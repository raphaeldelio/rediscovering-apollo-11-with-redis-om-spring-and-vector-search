package dev.raphaeldelio.redisapollo.controller;

import com.redis.om.spring.tuple.Pair;
import dev.raphaeldelio.redisapollo.hash.domain.SearchSemanticCache;
import dev.raphaeldelio.redisapollo.hash.domain.*;
import dev.raphaeldelio.redisapollo.service.SearchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/search-by-text")
    public Map<String, Object> searchByText(@RequestBody Map<String, String> requestBody) {
        String query = requestBody.get("query");

        List<Map<String, String>> matchedUtterances = searchService.searchByText(query);

        return Map.of(
                "query", query,
                "matchedTexts", matchedUtterances
        );
    }

    @PostMapping("/search-by-summary")
    public Map<String, Object> searchBySummary(@RequestBody Map<String, String> requestBody) {
        String query = requestBody.get("query");
        List<Pair<UtteranceSummaries, Double>> summariesAndScores = searchService.searchBySummary(query);

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
                }).toList();

        List<Pair<SearchSemanticCache, Double>> queriesAndScores = searchService.getCacheResponse(query, false);
        Optional<Pair<SearchSemanticCache, Double>> cacheResponse = queriesAndScores.stream().filter(queryAndScore -> queryAndScore.getSecond() < 0.1).findFirst();
        if (cacheResponse.isPresent()) {
            return Map.of(
                    "query", query,
                    "answer", cacheResponse.get().getFirst().getAnswer(),
                    "matchedSummaries", matchedSummaries,
                    "cachedQuery", cacheResponse.get().getFirst().getQuery(),
                    "cachedScore", cacheResponse.get().getSecond()
            );
        }

        String enhancedAnswer = searchService.enhanceWithRag(
                query,
                summariesAndScores.stream()
                        .map(it -> it.getFirst().getUtterancesConcatenated())
                        .collect(Collectors.joining("\n"))
        );

        searchService.cacheResponse(query, enhancedAnswer, false);


        return Map.of(
                "query", query,
                "answer", enhancedAnswer,
                "matchedSummaries", matchedSummaries
        );
    }

    @PostMapping("/search-by-question")
    public Map<String, Object> searchByQuestion(@RequestBody Map<String, String> requestBody) {
        String query = requestBody.get("query");

        List<Pair<UtteranceQuestions, Double>> questionsAndScores = searchService.searchByQuestion(query);
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
                }).toList();

        List<Pair<SearchSemanticCache, Double>> queriesAndScores = searchService.getCacheResponse(query, true);
        Optional<Pair<SearchSemanticCache, Double>> cacheResponse = queriesAndScores.stream().filter(
                queryAndScore -> queryAndScore.getSecond() < 0.1
        ).findFirst();

        if (cacheResponse.isPresent()) {
            return Map.of(
                    "query", query,
                    "answer", cacheResponse.get().getFirst().getAnswer(),
                    "matchedQuestions", matchedQuestions,
                    "cachedQuery", cacheResponse.get().getFirst().getQuery(),
                    "cachedScore", cacheResponse.get().getSecond()
            );
        }

        String enhancedAnswer = searchService.enhanceWithRag(
                query,
                questionsAndScores.stream()
                        .map(it -> it.getFirst().getUtterancesConcatenated())
                        .collect(Collectors.joining("\n"))
        );

        searchService.cacheResponse(query, enhancedAnswer, true);

        return Map.of(
                "query", query,
                "answer", enhancedAnswer,
                "matchedQuestions", matchedQuestions
        );
    }

    @PostMapping("/search-by-image")
    public Map<String, Object> searchByImage(@RequestBody Map<String, String> requestBody) {
        String imageBase64 = requestBody.get("imageBase64");
        String imagePath = requestBody.get("imagePath");

        String tmpImagePath = searchService.saveTmpImage(imageBase64, imagePath);
        List<Map<String, String>> matchedPhotographs = searchService.searchByImage(tmpImagePath);

        return Map.of(
                "imagePath", tmpImagePath,
                "matchedPhotographs", matchedPhotographs
        );
    }

    @PostMapping("/search-by-image-text")
    public Map<String, Object> searchByImageText(@RequestBody Map<String, String> requestBody) {
        String query = requestBody.get("query");
        List<Map<String, String>> matchedPhotographs = searchService.searchByImageText(query);

        return Map.of(
                "query", query,
                "matchedPhotographs", matchedPhotographs
        );
    }
}
