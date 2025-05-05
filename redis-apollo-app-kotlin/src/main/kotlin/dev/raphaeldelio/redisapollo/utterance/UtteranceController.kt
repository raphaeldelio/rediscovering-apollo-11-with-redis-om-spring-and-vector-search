package dev.raphaeldelio.redisapollo.utterance

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/utterance")
class UtteranceController(
    private val utteranceService: UtteranceService
) {

    @PostMapping("/search")
    fun searchByText(@RequestBody request: SearchRequest): Map<String, Any> {
        var start = System.currentTimeMillis()
        val embedding = utteranceService.embedUtterance(request.query)
        val embeddingTime = System.currentTimeMillis() - start

        start = System.currentTimeMillis()
        val matches = utteranceService.search(embedding)
        val searchTime = System.currentTimeMillis() - start

        return mapOf(
            "query" to request.query,
            "matchedTexts" to matches,
            "embeddingTime" to "${embeddingTime}ms",
            "searchTime" to "${searchTime}ms"
        )
    }

    data class SearchRequest(
        val query: String,
        val enableSemanticCache: Boolean,
        val enableRag: Boolean
    )
}