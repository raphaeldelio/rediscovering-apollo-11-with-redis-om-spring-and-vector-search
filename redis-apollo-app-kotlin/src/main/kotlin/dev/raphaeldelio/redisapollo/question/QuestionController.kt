package dev.raphaeldelio.redisapollo.question

import dev.raphaeldelio.redisapollo.rag.RagService
import dev.raphaeldelio.redisapollo.semanticcache.SearchSemanticCacheService
import dev.raphaeldelio.redisapollo.utterance.Utterance
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/question")
class QuestionController(
    private val questionService: QuestionService,
    private val searchSemanticCacheService: SearchSemanticCacheService,
    private val ragService: RagService
) {

    @PostMapping("/search/")
    fun searchByQuestion(@RequestBody request: SearchRequest): Map<String, Any> {
        var start = System.currentTimeMillis()
        val embedding = questionService.embedQuery(request.query)
        val embeddingTime = System.currentTimeMillis() - start

        if (request.enableSemanticCache) {
            start = System.currentTimeMillis()
            val cached = searchSemanticCacheService
                .getCacheResponse(embedding, true)
                .firstOrNull { it.second < 0.1 }

            val cacheSearchTime = System.currentTimeMillis() - start

            if (cached != null) {
                return mapOf(
                    "query" to request.query,
                    "ragAnswer" to cached.first.answer,
                    "cachedQuery" to cached.first.query,
                    "cachedScore" to cached.second,
                    "matchedQuestions" to "",
                    "embeddingTime" to "${embeddingTime}ms",
                    "cacheSearchTime" to "${cacheSearchTime}ms"
                )
            }
        }

        start = System.currentTimeMillis()
        val results = questionService.searchByQuestion(embedding)
        val searchTime = System.currentTimeMillis() - start

        val mapped = results.map {
            mapOf(
                "question" to it.question.question,
                "utterances" to formatUtterances(it.question.utterances),
                "score" to it.score.toString()
            )
        }

        if (request.enableRag) {
            val context = results.joinToString("\n") { it.question.utterancesConcatenated }

            start = System.currentTimeMillis()
            val answer = ragService.enhanceWithRag(request.query, context)
            val ragTime = System.currentTimeMillis() - start

            if (request.enableSemanticCache) {
                searchSemanticCacheService.cacheResponse(request.query, answer, true)
            }

            return mapOf(
                "query" to request.query,
                "ragAnswer" to answer,
                "matchedQuestions" to mapped,
                "embeddingTime" to "${embeddingTime}ms",
                "ragTime" to "${ragTime}ms",
                "searchTime" to "${searchTime}ms"
            )
        }

        return mapOf(
            "query" to request.query,
            "matchedQuestions" to mapped,
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