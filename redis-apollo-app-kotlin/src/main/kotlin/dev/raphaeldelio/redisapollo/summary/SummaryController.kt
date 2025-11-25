package dev.raphaeldelio.redisapollo.summary

import dev.raphaeldelio.redisapollo.rag.RagService
import dev.raphaeldelio.redisapollo.utterance.Utterance
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/summary")
class SummaryController(
    private val summaryService: SummaryService,
    private val ragService: RagService,
) {

    @PostMapping("/search")
    fun search(@RequestBody request: SearchRequest): Map<String, Any> {
        var start = System.currentTimeMillis()
        val embedding = summaryService.embedQuery(request.query)
        val embeddingTime = System.currentTimeMillis() - start

        if (request.enableSemanticCache) {
            start = System.currentTimeMillis()
            val cached = summaryService.getCacheResponse(request.query)
            val cacheSearchTime = System.currentTimeMillis() - start

            if (cached.isPresent) {
                val hit = cached.get()
                return mapOf(
                    "query" to request.query,
                    "ragAnswer" to hit.response,
                    "cachedQuery" to hit.prompt,
                    "cachedScore" to hit.distance,
                    "matchedSummaries" to "",
                    "embeddingTime" to "${embeddingTime}ms",
                    "cacheSearchTime" to "${cacheSearchTime}ms"
                )
            }
        }

        start = System.currentTimeMillis()
        val results = summaryService.searchBySummary(embedding)
        val searchTime = System.currentTimeMillis() - start

        val mapped = results.map { result ->
            mapOf(
                "summary" to result.summary.summary,
                "utterances" to formatUtterances(result.summary.utterances),
                "score" to result.score.toString()
            )
        }

        if (request.enableRag) {
            val context = results.joinToString("\n") { it.summary.utterancesConcatenated }

            start = System.currentTimeMillis()
            val answer = ragService.enhanceWithRag(request.query, context)
            val ragTime = System.currentTimeMillis() - start

            if (request.enableSemanticCache) {
                summaryService.cacheResponse(request.query, answer, false)
            }

            return mapOf(
                "query" to request.query,
                "ragAnswer" to answer,
                "matchedSummaries" to mapped,
                "embeddingTime" to "${embeddingTime}ms",
                "searchTime" to "${searchTime}ms",
                "ragTime" to "${ragTime}ms"
            )
        }

        return mapOf(
            "query" to request.query,
            "matchedSummaries" to mapped,
            "embeddingTime" to "${embeddingTime}ms",
            "searchTime" to "${searchTime}ms"
        )
    }

    private fun formatUtterances(utterances: List<Utterance>): String =
        utterances.joinToString("\n") {
            "${it.timestamp} - ${it.speaker}: ${it.text}"
        }

    data class SearchRequest(
        val query: String,
        val enableSemanticCache: Boolean,
        val enableRag: Boolean
    )
}