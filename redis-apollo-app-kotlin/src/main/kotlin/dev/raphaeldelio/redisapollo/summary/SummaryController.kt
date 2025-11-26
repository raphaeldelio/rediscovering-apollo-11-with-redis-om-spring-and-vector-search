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
    fun search(@RequestBody request: SearchRequest): SummaryResponse {
        var start = System.currentTimeMillis()
        val embedding = summaryService.embedQuery(request.query)
        val embeddingTime = System.currentTimeMillis() - start

        if (request.enableSemanticCache) {
            start = System.currentTimeMillis()
            val cached = summaryService.getCacheResponse(request.query)
            val cacheSearchTime = System.currentTimeMillis() - start

            if (cached.isPresent) {
                val hit = cached.get()
                return CachedSummaryResponse(
                    query = request.query,
                    ragAnswer = hit.response,
                    cachedQuery = hit.prompt,
                    cachedScore = hit.distance,
                    cacheSearchTime = cacheSearchTime,
                    embeddingTime = embeddingTime
                )
            }
        }

        start = System.currentTimeMillis()
        val results = summaryService.searchBySummary(embedding)
        val searchTime = System.currentTimeMillis() - start

        val matched = results.map {
            MatchedSummary(
                summary = it.summary.summary,
                utterances = formatUtterances(it.summary.utterances),
                score = it.score
            )
        }

        if (request.enableRag) {
            val context = results.joinToString("\n") { it.summary.utterancesConcatenated }

            start = System.currentTimeMillis()
            val answer = ragService.enhanceWithRag(request.query, context)
            val ragTime = System.currentTimeMillis() - start

            if (request.enableSemanticCache) {
                summaryService.cacheResponse(request.query, answer)
            }

            return RagSummaryResponse(
                query = request.query,
                ragAnswer = answer,
                matchedSummaries = matched,
                embeddingTime = embeddingTime,
                searchTime = searchTime,
                ragTime = ragTime
            )
        }

        return SearchOnlySummaryResponse(
            query = request.query,
            matchedSummaries = matched,
            embeddingTime = embeddingTime,
            searchTime = searchTime
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

    // -------------------- Response Models --------------------

    data class MatchedSummary(
        val summary: String,
        val utterances: String,
        val score: Double
    )

    /** Base response type */
    open class SummaryResponse(
        open val query: String,
        open val embeddingTime: Long,
        open val searchTime: Long? = null,
        open val ragTime: Long? = null
    )

    /** Cache hit */
    data class CachedSummaryResponse(
        val ragAnswer: String,
        val cachedQuery: String,
        val cachedScore: Float,
        val cacheSearchTime: Long,
        override val query: String,
        override val embeddingTime: Long
    ) : SummaryResponse(query, embeddingTime)

    /** RAG-enhanced result */
    data class RagSummaryResponse(
        override val query: String,
        val ragAnswer: String,
        val matchedSummaries: List<MatchedSummary>,
        override val embeddingTime: Long,
        override val searchTime: Long,
        override val ragTime: Long
    ) : SummaryResponse(query, embeddingTime, searchTime, ragTime)

    /** Search-only result */
    data class SearchOnlySummaryResponse(
        override val query: String,
        val matchedSummaries: List<MatchedSummary>,
        override val embeddingTime: Long,
        override val searchTime: Long
    ) : SummaryResponse(query, embeddingTime, searchTime)
}