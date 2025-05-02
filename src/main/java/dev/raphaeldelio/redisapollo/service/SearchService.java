package dev.raphaeldelio.redisapollo.service;

import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.SearchStream;
import com.redis.om.spring.tuple.Fields;
import com.redis.om.spring.tuple.Pair;
import com.redis.om.spring.vectorize.Embedder;
import dev.raphaeldelio.redisapollo.hash.domain.SearchSemanticCache;
import dev.raphaeldelio.redisapollo.hash.domain.SearchSemanticCache$;
import dev.raphaeldelio.redisapollo.hash.domain.*;
import dev.raphaeldelio.redisapollo.hash.repository.PhotographsRepository;
import dev.raphaeldelio.redisapollo.hash.repository.SearchSemanticCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private final EntityStream entityStream;
    private final Embedder embedder;
    private final OpenAiChatModel chatModel;
    private final PhotographsRepository photographsRepository;
    private final SearchSemanticCacheRepository searchSemanticCacheRepository;

    public SearchService(EntityStream entityStream, Embedder embedder, OpenAiChatModel chatModel, PhotographsRepository photographsRepository, SearchSemanticCacheRepository searchSemanticCacheRepository) {
        this.entityStream = entityStream;
        this.embedder = embedder;
        this.chatModel = chatModel;
        this.photographsRepository = photographsRepository;
        this.searchSemanticCacheRepository = searchSemanticCacheRepository;
    }

    public List<Map<String, String>> searchByText(String query) {
        logger.info("Received text: {}", query);
        byte[] embedding = embedder.getTextEmbeddingsAsBytes(List.of(query), Utterance$.TEXT).getFirst();

        SearchStream<Utterance> stream = entityStream.of(Utterance.class);
        List<Pair<Utterance, Double>> textsAndScores = stream
                .filter(Utterance$.EMBEDDED_TEXT.knn(3, embedding))
                .sorted(Utterance$._EMBEDDED_TEXT_SCORE)
                .map(Fields.of(Utterance$._THIS, Utterance$._EMBEDDED_TEXT_SCORE)) //
                .collect(Collectors.toList());

        return textsAndScores.stream()
                .map(pair -> {
                    Utterance utterance = pair.getFirst();
                    return Map.of(
                            "text", utterance.getText(),
                            "score", pair.getSecond().toString()
                    );
                })
                .collect(Collectors.toList());
    }

    public List<Pair<UtteranceSummaries, Double>> searchBySummary(String query) {
        logger.info("Received question: {}", query);

        byte[] embedding = embedder.getTextEmbeddingsAsBytes(List.of(query), UtteranceSummaries$.SUMMARY).getFirst();

        SearchStream<UtteranceSummaries> stream = entityStream.of(UtteranceSummaries.class);
        return stream.filter(UtteranceSummaries$.EMBEDDED_SUMMARY.knn(3, embedding))
                .sorted(UtteranceSummaries$._EMBEDDED_SUMMARY_SCORE)
                .map(Fields.of(UtteranceSummaries$._THIS, UtteranceSummaries$._EMBEDDED_SUMMARY_SCORE)) //
                .collect(Collectors.toList());
    }

    public List<Pair<UtteranceQuestions, Double>> searchByQuestion(String query) {
        logger.info("Received question: {}", query);

        byte[] embedding = embedder.getTextEmbeddingsAsBytes(List.of(query), UtteranceQuestions$.QUESTION).getFirst();

        SearchStream<UtteranceQuestions> stream = entityStream.of(UtteranceQuestions.class);
        return stream.filter(UtteranceQuestions$.EMBEDDED_QUESTION.knn(3, embedding))
                .sorted(UtteranceQuestions$._EMBEDDED_QUESTION_SCORE)
                .map(Fields.of(UtteranceQuestions$._THIS, UtteranceQuestions$._EMBEDDED_QUESTION_SCORE))
                .collect(Collectors.toList());
    }

    public String enhanceWithRag(String query, String data) {
        ChatResponse response = chatModel.call(
                new Prompt(List.of(
                        new SystemMessage("""
                    You are an expert assistant specializing in the Apollo missions. Your goal is to provide accurate,
                    detailed, and concise answers to user inquiries by utilizing the provided Apollo mission data.
                    Rely solely on the information given below and avoid introducing external information.
                    """),
                        new SystemMessage("Apollo mission data: " + data),
                        new UserMessage("User question: " + query)
                ))
        );

        String enhancedAnswer = response.getResult().getOutput().getText();
        logger.info("AI response: {}", enhancedAnswer);
        return enhancedAnswer;
    }

    public String saveTmpImage(String imageBase64, String imagePath) {
        String savedImagePath;
        if (imageBase64 != null) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(imageBase64);
                Path tempFile = Files.createTempFile("uploaded-image-", ".jpg");
                Files.write(tempFile, decodedBytes);
                savedImagePath = tempFile.toUri().toString();

                logger.info("Received Base64 image, saved to: {}", savedImagePath);
            } catch (IOException e) {
                throw new IllegalArgumentException("Error saving uploaded image", e);
            }
        } else if (imagePath != null) {
            savedImagePath = imagePath;
            logger.info("Received image path: {}", imagePath);
        } else {
            throw new IllegalArgumentException("No image provided");
        }
        return savedImagePath;
    }

    public List<Map<String, String>> searchByImage(String tmpImagePath) {
        Photograph tmpPhoto = new Photograph("tmp", "tmp", null, null, null);
        tmpPhoto.setImagePath(tmpImagePath);
        tmpPhoto = photographsRepository.save(tmpPhoto);

        SearchStream<Photograph> stream = entityStream.of(Photograph.class);
        List<Pair<Photograph, Double>> photographsAndScores = stream
                .filter(Photograph$.EMBEDDED_IMAGE.knn(20, tmpPhoto.getEmbeddedImage()))
                .sorted(Photograph$._EMBEDDED_IMAGE_SCORE)
                .map(Fields.of(Photograph$._THIS, Photograph$._EMBEDDED_IMAGE_SCORE))
                .collect(Collectors.toList());

        photographsAndScores = photographsAndScores.stream()
                .filter(pair -> !pair.getFirst().getName().equals("tmp"))
                .toList();

        return photographsAndScores.stream()
                .map(pair -> {
                    Photograph photograph = pair.getFirst();
                    return Map.of(
                            "imagePath", photograph.getImagePath().replace("classpath:static", ""),
                            "description", photograph.getDescription(),
                            "score", pair.getSecond().toString()
                    );
                })
                .toList();
    }

    public List<Map<String, String>> searchByImageText(String query) {
        logger.info("Received query: {}", query);

        byte[] embedding = embedder.getTextEmbeddingsAsBytes(List.of(query), Photograph$.DESCRIPTION).getFirst();
        SearchStream<Photograph> stream = entityStream.of(Photograph.class);
        List<Pair<Photograph, Double>> photographsAndScores = stream
                .filter(Photograph$.EMBEDDED_DESCRIPTION.knn(3, embedding))
                .sorted(Photograph$._EMBEDDED_DESCRIPTION_SCORE)
                .map(Fields.of(Photograph$._THIS, Photograph$._EMBEDDED_DESCRIPTION_SCORE))
                .collect(Collectors.toList());

        return photographsAndScores.stream()
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
    }

    public List<Pair<SearchSemanticCache, Double>> getCacheResponse(String query, boolean isQuestion) {
        float[] embedding = embedder.getTextEmbeddingsAsFloats(List.of(query), SearchSemanticCache$.QUERY).getFirst();
        SearchStream<SearchSemanticCache> stream = entityStream.of(SearchSemanticCache.class);
        return stream
                .filter(SearchSemanticCache$.EMBEDDED_QUERY.knn(1, embedding))
                .filter(SearchSemanticCache$.IS_QUESTION.eq(isQuestion))
                .sorted(SearchSemanticCache$._EMBEDDED_QUERY_SCORE)
                .map(Fields.of(SearchSemanticCache$._THIS, SearchSemanticCache$._EMBEDDED_QUERY_SCORE))
                .collect(Collectors.toList());
    }

    public void cacheResponse(String query, String answer, boolean isQuestion) {
        SearchSemanticCache cache = new SearchSemanticCache();
        cache.setId(UUID.randomUUID().toString());
        cache.setQuery(query);
        cache.setAnswer(answer);
        cache.setQuestion(isQuestion);
        searchSemanticCacheRepository.save(cache);
    }

    public void clearCache() {
        searchSemanticCacheRepository.deleteAll();
        logger.info("Cache cleared");
    }
}
