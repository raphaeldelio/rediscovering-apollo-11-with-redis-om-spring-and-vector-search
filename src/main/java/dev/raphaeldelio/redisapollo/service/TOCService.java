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
        for (TOCData tocData : tocDataList) {
            boolean isUtterancesPopulated = tocData.getConcatenatedUtterances() != null && !tocData.getConcatenatedUtterances().isBlank();
            boolean shouldOverwrite = overwrite || tocData.getSummary() == null;
            if (isUtterancesPopulated && shouldOverwrite) {
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
                tocDataRepository.save(tocData);
                logger.info("Summarized TOC entry: {}", tocData.getStartDate());
            }
        }
        logger.info("Summarized TOC entries");
    }

    public void generateQuestions() {
        generateQuestions(false);
    }

    public void generateQuestions(boolean overwrite) {
        logger.info("Generating questions for TOC entries");
        List<TOCData> tocDataList = tocDataRepository.findAll();
        for (TOCData tocData : tocDataList) {
            boolean isUtterancesPopulated = tocData.getConcatenatedUtterances() != null && !tocData.getConcatenatedUtterances().isBlank();
            boolean shouldOverwrite = overwrite || tocData.getQuestions() == null;
            if (isUtterancesPopulated && shouldOverwrite) {
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

                var questions = Arrays.stream(response.getResult().getOutput().getText().split("\n")).filter(q -> !q.isBlank()).toList();
                tocData.setQuestions(questions);
                tocDataRepository.save(tocData);
                logger.info("Generated questions for TOC entry: {}", tocData.getStartDate());
            }
        }
        logger.info("Generated questions for TOC entries");
    }
}
