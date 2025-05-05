package dev.raphaeldelio.redisapollo.question;

import com.redis.om.spring.tuple.Pair;
import dev.raphaeldelio.redisapollo.rag.RagService;
import dev.raphaeldelio.redisapollo.semanticcache.SearchSemanticCache;
import dev.raphaeldelio.redisapollo.semanticcache.SearchSemanticCacheService;
import dev.raphaeldelio.redisapollo.utterance.Utterance;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/question")
public class QuestionController {

    private final QuestionService questionService;
    private final SearchSemanticCacheService searchSemanticCacheService;
    private final RagService ragService;

    public QuestionController(QuestionService questionService, SearchSemanticCacheService searchSemanticCacheService, RagService ragService) {
        this.questionService = questionService;
        this.searchSemanticCacheService = searchSemanticCacheService;
        this.ragService = ragService;
    }

    @PostMapping("/search/")
    public Map<String, Object> searchByQuestion(@RequestBody SearchRequest request) {
        long start = System.currentTimeMillis();

        byte[] embedding = questionService.embedQuery(request.query());

        long embeddingTime = System.currentTimeMillis() - start;

        if (request.enableSemanticCache()) {
            start = System.currentTimeMillis();
            Optional<Pair<SearchSemanticCache, Double>> cached = searchSemanticCacheService
                    .getCacheResponse(embedding, true)
                    .stream()
                    .filter(pair -> pair.getSecond() < 0.1)
                    .findFirst();
            long cacheSearchTime = System.currentTimeMillis() - start;

            if (cached.isPresent()) {
                var hit = cached.get();
                return Map.of(
                        "query", request.query(),
                        "ragAnswer", hit.getFirst().getAnswer(),
                        "cachedQuery", hit.getFirst().getQuery(),
                        "cachedScore", hit.getSecond(),
                        "matchedQuestions", "",
                        "embeddingTime", embeddingTime + "ms",
                        "cacheSearchTime", cacheSearchTime + "ms"
                );
            }
        }

        start = System.currentTimeMillis();
        var results = questionService.searchByQuestion(embedding);
        long searchTime = System.currentTimeMillis() - start;

        List<Map<String, String>> mapped = results.stream()
                .map(result -> Map.of(
                        "question", result.question().getQuestion(),
                        "utterances", formatUtterances(result.question().getUtterances()),
                        "score", result.score().toString()
                )).toList();

        if (request.enableRag()) {
            String context = results.stream()
                    .map(result -> result.question().getUtterancesConcatenated())
                    .collect(Collectors.joining("\n"));

            start = System.currentTimeMillis();
            String answer = ragService.enhanceWithRag(request.query(), context);
            long ragTime = System.currentTimeMillis() - start;

            if (request.enableSemanticCache()) {
                searchSemanticCacheService.cacheResponse(request.query(), answer, true);
            }

            return Map.of(
                    "query", request.query(),
                    "ragAnswer", answer,
                    "matchedQuestions", mapped,
                    "embeddingTime", embeddingTime + "ms",
                    "ragTime", ragTime + "ms",
                    "searchTime", searchTime + "ms"
            );
        }

        return Map.of(
                "query", request.query(),
                "matchedQuestions", mapped,
                "embeddingTime", embeddingTime + "ms",
                "searchTime", searchTime + "ms"
        );
    }

    private String formatUtterances(List<Utterance> utterances) {
        return utterances.stream()
                .map(u -> u.getTimestamp() + " - " + u.getSpeaker() + ": " + u.getText())
                .collect(Collectors.joining("\n"));
    }

    // ---------- DTOs ----------

    public record SearchRequest(String query, boolean enableSemanticCache, boolean enableRag) {}
}