package dev.raphaeldelio.redisapollo.service;

import com.redis.om.spring.search.stream.EntityStream;
import dev.raphaeldelio.redisapollo.document.domain.TOCData;
import dev.raphaeldelio.redisapollo.document.domain.TOCData$;
import dev.raphaeldelio.redisapollo.hash.domain.Utterance;
import dev.raphaeldelio.redisapollo.hash.domain.Utterance$;
import dev.raphaeldelio.redisapollo.document.repository.TOCDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class TOCService {

    private static final Logger logger = LoggerFactory.getLogger(TOCService.class);

    private final TOCDataRepository tocDataRepository;
    private final OpenAiChatModel chatModel;
    private final EntityStream entityStream;
    private final FileService fileService;
    private final RedisService redisService;

    public TOCService(TOCDataRepository tocDataRepository, OpenAiChatModel chatModel, EntityStream entityStream, FileService fileService, RedisService redisService) {
        this.tocDataRepository = tocDataRepository;
        this.chatModel = chatModel;
        this.entityStream = entityStream;
        this.fileService = fileService;
        this.redisService = redisService;
    }

    public void loadTOCData(String filePath) {
        logger.info("Loading TOC data from file: {}", filePath);
        fileService.readAndProcessFile(filePath, TOCData.class, data -> {
            for (TOCData tocData : data) {
                if (!isValidToc(tocData)) {
                    continue;
                }
                int utteranceSeconds = fileService.asSeconds(tocData.getStartDate());
                tocData.setStartDate(tocData.getStartDate().replace(":", ";"));
                tocData.setStartDateInt(utteranceSeconds);
                tocDataRepository.save(tocData);
            }
        });
    }

    private boolean isValidToc(TOCData tocData) {
        return !tocData.getDescription().startsWith("Video: ");
    }

    public void populateUtterances() {
        populateUtterances(false);
    }

    public void populateUtterances(boolean overwrite) {
        logger.info("Grouping utterances by TOC entries");

        List<TOCData> tocDataList = entityStream.of(TOCData.class)
                .sorted(TOCData$.START_DATE_INT)
                .collect(Collectors.toList());

        List<String> utterancesToFilterOut = redisService.getZRangeByScore("utterances", 2, Double.MAX_VALUE);

        // Iterate through each TOC entry
        for (int i = 0; i < tocDataList.size(); i++) {
            TOCData currentTOC = tocDataList.get(i);
            logger.info("Processing TOC entry: {}", currentTOC.getStartDateInt());

            if (!overwrite && currentTOC.getConcatenatedUtterances() != null && !currentTOC.getConcatenatedUtterances().isBlank()) {
                logger.info("Utterances already grouped for TOC entry: {}", currentTOC.getStartDate());
                continue;
            }

            int startDate = currentTOC.getStartDateInt();
            int endDate = (i < tocDataList.size() - 1) ? tocDataList.get(i + 1).getStartDateInt() : Integer.MAX_VALUE;

            // Filter utterances that fall within the current TOC's time range
            //utterancesToFilterOut.contains(utt.getText())
            List<Utterance> tocUtterances = entityStream.of(Utterance.class)
                    .filter(Utterance$.TIMESTAMP_INT.ge(startDate)
                            .and(Utterance$.TIMESTAMP_INT.le(endDate))
                    ).collect(Collectors.toList());

            tocUtterances = tocUtterances.stream().filter(utt ->
                    !utterancesToFilterOut.contains(utt.getText())
            ).toList();

            var groupedUtterances = tocUtterances.stream()
                    .map(utt -> utt.getSpeaker() + ":" + utt.getText())
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse(null);

            if (groupedUtterances != null) {
                currentTOC.setConcatenatedUtterances(groupedUtterances);
                currentTOC.setUtterances(tocUtterances);
                tocDataRepository.save(currentTOC);
                logger.info("Grouped utterances for TOC entry: {}", currentTOC.getStartDate());
            } else {
                logger.info("No utterances found for TOC entry: {}", currentTOC.getStartDate());
            }
        }
        logger.info("Grouping complete");
    }

    public void summarize() {
        summarize(false);
    }

    public void summarize(boolean overwrite) {
        logger.info("Summarizing TOC entries");
        List<TOCData> tocDataList = tocDataRepository.findAll();

        // Filter TOC entries that need summaries generated
        List<TOCData> tocDataToProcess = tocDataList.stream()
                .filter(tocData -> {
                    boolean isUtterancesPopulated = tocData.getConcatenatedUtterances() != null && !tocData.getConcatenatedUtterances().isBlank();
                    boolean shouldOverwrite = overwrite || tocData.getSummary() == null;
                    return isUtterancesPopulated && shouldOverwrite;
                })
                .collect(Collectors.toList());

        logger.info("Found {} TOC entries that need summaries generated", tocDataToProcess.size());

        if (tocDataToProcess.isEmpty()) {
            logger.info("No TOC entries need summaries generated");
            return;
        }

        // Process in batches of 300
        int batchSize = 300;
        int totalBatches = (tocDataToProcess.size() + batchSize - 1) / batchSize; // Ceiling division

        logger.info("Processing TOC entries in {} batches of up to {} entries each", totalBatches, batchSize);

        // Create a virtual thread per task executor for better scalability with I/O-bound operations
        // Virtual threads are lightweight and don't consume OS resources like platform threads
        // This is ideal for I/O-bound tasks like API calls to OpenAI
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {

            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                int startIndex = batchIndex * batchSize;
                int endIndex = Math.min(startIndex + batchSize, tocDataToProcess.size());
                List<TOCData> currentBatch = tocDataToProcess.subList(startIndex, endIndex);

                logger.info("Processing batch {} of {} ({} entries)", batchIndex + 1, totalBatches, currentBatch.size());

                // Process each batch asynchronously
                List<CompletableFuture<TOCData>> futures = currentBatch.stream()
                        .map(tocData -> CompletableFuture.supplyAsync(() -> {
                            try {
                                logger.info("Generating summary for TOC entry: {}", tocData.getStartDate());

                                ChatResponse response = chatModel.call(
                                        new Prompt(List.of(
                                                new SystemMessage("""
                                                        You are a helpful assistant who summarizes utterances of the Apollo 11 mission.
                                                        Make these summaries very dense with all curiosities included.
                                                        Limit the summary to 512 words.
                                                        """),
                                                new UserMessage(tocData.getConcatenatedUtterances())
                                        ))
                                );

                                tocData.setSummary(response.getResult().getOutput().getText());
                                logger.info("Successfully generated summary for TOC entry: {}", tocData.getStartDate());
                                return tocData;
                            } catch (Exception e) {
                                logger.error("Error generating summary for TOC entry: {}", tocData.getStartDate(), e);
                                return null;
                            }
                        }, executorService))
                        .toList();

                // Wait for all futures in the current batch to complete before processing the next batch
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // Collect all processed TOCData objects and filter out nulls (from failed futures)
                List<TOCData> processedBatch = futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .toList();

                // Save all processed TOCData objects in batch
                if (!processedBatch.isEmpty()) {
                    tocDataRepository.saveAll(processedBatch);
                    logger.info("Saved {} TOC entries in batch", processedBatch.size());
                }

                logger.info("Completed batch {} of {}", batchIndex + 1, totalBatches);
            }
        }

        logger.info("Completed generating summaries for all TOC entries");
    }

    public void generateQuestions() {
        generateQuestions(false);
    }

    public void generateQuestions(boolean overwrite) {
        logger.info("Generating questions for TOC entries");
        List<TOCData> tocDataList = tocDataRepository.findAll();

        // Filter TOC entries that need questions generated
        List<TOCData> tocDataToProcess = tocDataList.stream()
                .filter(tocData -> {
                    boolean isUtterancesPopulated = tocData.getConcatenatedUtterances() != null && !tocData.getConcatenatedUtterances().isBlank();
                    boolean shouldOverwrite = overwrite || tocData.getQuestions() == null;
                    return isUtterancesPopulated && shouldOverwrite;
                })
                .collect(Collectors.toList());

        logger.info("Found {} TOC entries that need questions generated", tocDataToProcess.size());

        if (tocDataToProcess.isEmpty()) {
            logger.info("No TOC entries need questions generated");
            return;
        }

        // Process in batches of 300
        int batchSize = 300;
        int totalBatches = (tocDataToProcess.size() + batchSize - 1) / batchSize; // Ceiling division

        logger.info("Processing TOC entries in {} batches of up to {} entries each", totalBatches, batchSize);

        // Create a virtual thread per task executor for better scalability with I/O-bound operations
        // Virtual threads are lightweight and don't consume OS resources like platform threads
        // This is ideal for I/O-bound tasks like API calls to OpenAI
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {

            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                int startIndex = batchIndex * batchSize;
                int endIndex = Math.min(startIndex + batchSize, tocDataToProcess.size());
                List<TOCData> currentBatch = tocDataToProcess.subList(startIndex, endIndex);

                logger.info("Processing batch {} of {} ({} entries)", batchIndex + 1, totalBatches, currentBatch.size());

                // Process each batch asynchronously
                List<CompletableFuture<TOCData>> futures = currentBatch.stream()
                        .map(tocData -> CompletableFuture.supplyAsync(() -> {
                            try {
                                logger.info("Generating questions for TOC entry: {}", tocData.getStartDate());

                                ChatResponse response = chatModel.call(
                                        new Prompt(List.of(
                                                new SystemMessage("""
                                                        You are a helpful assistant that is helping me predict which questions can be asked by people who are trying to
                                                        rediscover the Apollo 11 mission data. You will be given a number of utterances and you will predict the questions that
                                                        can be asked by people who are trying to rediscover the Apollo 11 mission data. You will ONLY return the questions separate by breaklines,
                                                        and nothing more. You will NEVER return more than 512 words.
                                                        """),
                                                new UserMessage(tocData.getConcatenatedUtterances())
                                        ))
                                );

                                var questions = Arrays.stream(response.getResult().getOutput().getText().split("\n"))
                                        .filter(q -> !q.isBlank())
                                        .toList();

                                tocData.setQuestions(questions);
                                logger.info("Successfully generated {} questions for TOC entry: {}", questions.size(), tocData.getStartDate());
                                return tocData;
                            } catch (Exception e) {
                                logger.error("Error generating questions for TOC entry: {}", tocData.getStartDate(), e);
                                return null;
                            }
                        }, executorService))
                        .toList();

                // Wait for all futures in the current batch to complete before processing the next batch
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // Collect all processed TOCData objects and filter out nulls (from failed futures)
                List<TOCData> processedBatch = futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .toList();

                // Save all processed TOCData objects in batch
                if (!processedBatch.isEmpty()) {
                    tocDataRepository.saveAll(processedBatch);
                    logger.info("Saved {} TOC entries in batch", processedBatch.size());
                }

                logger.info("Completed batch {} of {}", batchIndex + 1, totalBatches);
            }
        }

        logger.info("Completed generating questions for all TOC entries");
    }
}
