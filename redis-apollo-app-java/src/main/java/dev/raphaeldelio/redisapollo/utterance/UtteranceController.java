package dev.raphaeldelio.redisapollo.utterance;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/utterance")
public class UtteranceController {

    private final UtteranceService utteranceService;

    public UtteranceController(UtteranceService utteranceService) {
        this.utteranceService = utteranceService;
    }

    @PostMapping("/search")
    public Map<String, Object> searchByText(@RequestBody SearchRequest request) {
        long start = System.currentTimeMillis();
        byte[] embedding = utteranceService.embedUtterance(request.query());
        long embeddingTime = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        List<UtteranceSearchResult> matches = utteranceService.search(embedding);
        long searchTime = System.currentTimeMillis() - start;

        return Map.of(
                "query", request.query(),
                "matchedTexts", matches,
                "embeddingTime", embeddingTime + "ms",
                "searchTime", searchTime + "ms"
        );
    }

    // ---------- DTOs ----------

    public record SearchRequest(String query, boolean enableSemanticCache, boolean enableRag) {}
}