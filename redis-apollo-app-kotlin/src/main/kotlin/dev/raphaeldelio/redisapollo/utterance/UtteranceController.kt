package dev.raphaeldelio.redisapollo.utterance

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/utterance")
class UtteranceController(
    private val utteranceService: UtteranceService
) {

    @PostMapping("/search")
    fun searchByText(@RequestBody request: SearchRequest): UtteranceSearchResponse {
        var start = System.currentTimeMillis()
        val embedding = utteranceService.embedUtterance(request.query)
        val embeddingTime = System.currentTimeMillis() - start

        start = System.currentTimeMillis()
        val matches = utteranceService.search(embedding)
        val searchTime = System.currentTimeMillis() - start

        return UtteranceSearchResponse(
            query = request.query,
            matchedTexts = matches,
            embeddingTime = embeddingTime,
            searchTime = searchTime
        )
    }

    data class SearchRequest(
        val query: String,
        val enableSemanticCache: Boolean,
        val enableRag: Boolean
    )

    data class UtteranceSearchResponse(
        val query: String,
        val matchedTexts: List<UtteranceSearchResult>,
        val embeddingTime: Long,
        val searchTime: Long
    )
}