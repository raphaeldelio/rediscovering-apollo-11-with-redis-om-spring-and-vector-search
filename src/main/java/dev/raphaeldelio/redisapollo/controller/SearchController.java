package dev.raphaeldelio.redisapollo.controller;

import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.SearchStream;
import com.redis.om.spring.tuple.Fields;
import com.redis.om.spring.tuple.Pair;
import com.redis.om.spring.vectorize.Embedder;
import dev.raphaeldelio.redisapollo.hash.domain.*;
import dev.raphaeldelio.redisapollo.hash.repository.PhotographsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private final EntityStream entityStream;
    private final Embedder embedder;
    private final OpenAiChatModel chatModel;
    private final PhotographsRepository photographsRepository;

    public SearchController(EntityStream entityStream, Embedder embedder, OpenAiChatModel chatModel, PhotographsRepository photographsRepository) {
        this.entityStream = entityStream;
        this.embedder = embedder;
        this.chatModel = chatModel;
        this.photographsRepository = photographsRepository;
    }

    @PostMapping("/search-by-text")
    public Map<String, Object> searchByText(@RequestBody Map<String, String> requestBody) {
        String queryText = requestBody.get("query");
        logger.info("Received question: {}", queryText);

        byte[] embedding = embedder.getTextEmbeddingsAsBytes(List.of(queryText), Utterance$.TEXT).get(0);
        SearchStream<Utterance> stream = entityStream.of(Utterance.class);
        List<Pair<Utterance, Double>> textsAndScores = stream
                .filter(Utterance$.EMBEDDED_TEXT.knn(3, embedding))
                .sorted(Utterance$._EMBEDDED_TEXT_SCORE)
                .map(Fields.of(Utterance$._THIS, Utterance$._EMBEDDED_TEXT_SCORE)) //
                .collect(Collectors.toList());

        List<Map<String, String>> matchedUtterances = textsAndScores.stream()
                .map(pair -> {
                    Utterance utterance = pair.getFirst();
                    return Map.of(
                            "text", utterance.getText(),
                            "score", pair.getSecond().toString()
                    );
                })
                .collect(Collectors.toList());

        return Map.of(
                "query", queryText,
                "matchedTexts", matchedUtterances
        );
    }

    @PostMapping("/search-by-summary")
    public Map<String, Object> searchBySummary(@RequestBody Map<String, String> requestBody) {
        String queryText = requestBody.get("query");
        logger.info("Received question: {}", queryText);

        byte[] embedding = embedder.getTextEmbeddingsAsBytes(List.of(queryText), UtteranceSummaries$.SUMMARY).get(0);
        SearchStream<UtteranceSummaries> stream = entityStream.of(UtteranceSummaries.class);
        List<Pair<UtteranceSummaries, Double>> summariesAndScores = stream
                .filter(UtteranceSummaries$.EMBEDDED_SUMMARY.knn(3, embedding))
                .sorted(UtteranceSummaries$._EMBEDDED_SUMMARY_SCORE)
                .map(Fields.of(UtteranceSummaries$._THIS, UtteranceSummaries$._EMBEDDED_SUMMARY_SCORE)) //
                .collect(Collectors.toList());

        List<Map<String, String>> matchedSummaries = summariesAndScores.stream()
                .map(pair -> {
                    UtteranceSummaries summary = pair.getFirst();
                    return Map.of(
                            "summary", summary.getSummary(),
                            "utterances", summary.getUtterances().stream()
                                    .map(utt -> utt.getTimestamp() + " - "
                                            + utt.getSpeaker() + ": "
                                            + utt.getText())
                                    .collect(Collectors.joining("\n")),
                            "score", pair.getSecond().toString()
                    );
                })
                .collect(Collectors.toList());

        ChatResponse response = chatModel.call(
                new Prompt(List.of(
                        new SystemMessage("""
                        You are an expert assistant specializing in the Apollo missions. Your goal is to provide accurate, 
                        detailed, and concise answers to user inquiries by utilizing the provided Apollo mission data. 
                        Rely solely on the information given below and avoid introducing external information.
                        """),
                        new SystemMessage("Apollo mission data: " + summariesAndScores.stream()
                                .map(it -> it.getFirst().getUtterancesConcatenated())
                                .collect(Collectors.joining("\n"))),
                        new UserMessage("User question: " + queryText)
                ))
        );

        logger.info("AI response: {}", response.getResult().getOutput().getContent());
        return Map.of(
                "query", queryText,
                "answer", response.getResult().getOutput().getContent(),
                "matchedSummaries", matchedSummaries
        );
    }

    @PostMapping("/search-by-question")
    public Map<String, Object> searchByQuestion(@RequestBody Map<String, String> requestBody) {
        String queryText = requestBody.get("query");
        logger.info("Received question: {}", queryText);

        byte[] embedding = embedder.getTextEmbeddingsAsBytes(List.of(queryText), UtteranceQuestions$.QUESTION).get(0);
        SearchStream<UtteranceQuestions> stream = entityStream.of(UtteranceQuestions.class);
        List<Pair<UtteranceQuestions, Double>> questionsAndScores = stream
                .filter(UtteranceQuestions$.EMBEDDED_QUESTION.knn(3, embedding))
                .sorted(UtteranceQuestions$._EMBEDDED_QUESTION_SCORE)
                .map(Fields.of(UtteranceQuestions$._THIS, UtteranceQuestions$._EMBEDDED_QUESTION_SCORE))
                .collect(Collectors.toList());

        List<Map<String, String>> matchedQuestions = questionsAndScores.stream()
                .map(pair -> {
                    UtteranceQuestions question = pair.getFirst();
                    return Map.of(
                            "question", question.getQuestion(),
                            "utterances", question.getUtterances().stream()
                                    .map(utt -> utt.getTimestamp() + " - "
                                            + utt.getSpeaker() + ": "
                                            + utt.getText())
                                    .collect(Collectors.joining("\n")),
                            "score", pair.getSecond().toString()
                    );
                })
                .collect(Collectors.toList());

        ChatResponse response = chatModel.call(
                new Prompt(List.of(
                        new SystemMessage("""
                    You are an expert assistant specializing in the Apollo missions. Your goal is to provide accurate, 
                    detailed, and concise answers to user inquiries by utilizing the provided Apollo mission data. 
                    Rely solely on the information given below and avoid introducing external information.
                    """),
                        new SystemMessage("Apollo mission data: " + questionsAndScores.stream()
                                .map(it -> it.getFirst().getUtterancesConcatenated())
                                .collect(Collectors.joining("\n"))),
                        new UserMessage("User question: " + queryText)
                ))
        );

        logger.info("AI response: {}", response.getResult().getOutput().getContent());
        return Map.of(
                "query", queryText,
                "answer", response.getResult().getOutput().getContent(),
                "matchedQuestions", matchedQuestions
        );
    }

    @PostMapping("/search-by-image")
    public Map<String, Object> searchByImage(@RequestBody Map<String, String> requestBody) {
        String imageBase64 = requestBody.get("imageBase64");
        String imagePath = requestBody.get("imagePath"); // Fallback for old behavior
        String savedImagePath = null;

        if (imageBase64 != null) {
            try {
                // Decode Base64 and save as a temporary file
                byte[] decodedBytes = Base64.getDecoder().decode(imageBase64);
                Path tempFile = Files.createTempFile("uploaded-image-", ".jpg");
                Files.write(tempFile, decodedBytes);
                savedImagePath = tempFile.toUri().toString();

                logger.info("Received Base64 image, saved to: {}", savedImagePath);
            } catch (IOException e) {
                logger.error("Error saving uploaded image", e);
                return Map.of("error", "Failed to process image");
            }
        } else if (imagePath != null) {
            savedImagePath = imagePath; // Use provided image path if no Base64 image
            logger.info("Received image path: {}", imagePath);
        } else {
            return Map.of("error", "No image provided");
        }

        // Create temporary Photograph object for searching
        Photograph tmpPhoto = new Photograph("tmp", "tmp", null, null, null);
        tmpPhoto.setImagePath(savedImagePath);
        tmpPhoto = photographsRepository.save(tmpPhoto);

        // Perform KNN search
        SearchStream<Photograph> stream = entityStream.of(Photograph.class);
        List<Pair<Photograph, Double>> photographsAndScores = stream
                .filter(Photograph$.EMBEDDED_IMAGE.knn(20, tmpPhoto.getEmbeddedImage()))
                .sorted(Photograph$._EMBEDDED_IMAGE_SCORE)
                .map(Fields.of(Photograph$._THIS, Photograph$._EMBEDDED_IMAGE_SCORE))
                .collect(Collectors.toList());

        photographsAndScores = photographsAndScores.stream()
                .filter(pair -> !pair.getFirst().getName().equals("tmp"))
                .toList();

        // Format the response
        List<Map<String, String>> matchedPhotographs = photographsAndScores.stream()
                .map(pair -> {
                    Photograph photograph = pair.getFirst();
                    return Map.of(
                            "imagePath", photograph.getImagePath().replace("classpath:static", ""),
                            "description", photograph.getDescription(),
                            "score", pair.getSecond().toString()
                    );
                })
                .toList();

        return Map.of(
                "imagePath", savedImagePath,
                "matchedPhotographs", matchedPhotographs
        );
    }

    @PostMapping("/search-by-image-text")
    public Map<String, Object> searchByImageText(@RequestBody Map<String, String> requestBody) {
        String queryText = requestBody.get("query");
        logger.info("Received query: {}", queryText);

        byte[] embedding = embedder.getTextEmbeddingsAsBytes(List.of(queryText), Photograph$.DESCRIPTION).get(0);
        SearchStream<Photograph> stream = entityStream.of(Photograph.class);
        List<Pair<Photograph, Double>> photographsAndScores = stream
                .filter(Photograph$.EMBEDDED_DESCRIPTION.knn(3, embedding))
                .sorted(Photograph$._EMBEDDED_DESCRIPTION_SCORE)
                .map(Fields.of(Photograph$._THIS, Photograph$._EMBEDDED_DESCRIPTION_SCORE))
                .collect(Collectors.toList());

        List<Map<String, String>> matchedPhotographs = photographsAndScores.stream()
                .map(pair -> {
                    Photograph photograph = pair.getFirst();
                    return Map.of(
                            "imagePath", photograph.getImagePath().replace(
                                    "classpath:static",
                                    ""
                            ),
                            "description", photograph.getDescription(),
                            "score", pair.getSecond().toString()
                    );
                })
                .toList();

        return Map.of(
                "query", queryText,
                "matchedPhotographs", matchedPhotographs
        );
    }
}
