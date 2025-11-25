package dev.raphaeldelio.redisapollo.question

import dev.raphaeldelio.redisapollo.rag.RagService
import dev.raphaeldelio.redisapollo.utterance.Utterance
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/question")
class QuestionController(
    private val questionService: QuestionService,
    private val ragService: RagService,
) {

    @PostMapping("/search/")
    fun searchByQuestion(
        @RequestBody request: SearchRequest
    ): SearchResponse {
        var start = System.currentTimeMillis()
        val embedding = questionService.embedQuery(request.query)
        val embeddingTime = System.currentTimeMillis() - start

        if (request.enableSemanticCache) {
            start = System.currentTimeMillis()
            val cached = questionService.getCacheResponse(request.query)
            val cacheSearchTime = System.currentTimeMillis() - start

            if (cached.isPresent) {
                val c = cached.get()
                return CachedResponse(
                    query = request.query,
                    ragAnswer = c.response,
                    cachedQuery = c.prompt,
                    cachedScore = c.distance,
                    embeddingTime = embeddingTime,
                    cacheSearchTime = cacheSearchTime
                )
            }
        }

        start = System.currentTimeMillis()
        val results = questionService.searchByQuestion(embedding)
        val searchTime = System.currentTimeMillis() - start

        val matched = results.map {
            MatchedQuestion(
                question = it.question.question,
                utterances = formatUtterances(it.question.utterances),
                score = it.score
            )
        }

        if (request.enableRag) {
            val context = results.joinToString("\n") { it.question.utterancesConcatenated }

            start = System.currentTimeMillis()
            val answer = ragService.enhanceWithRag(request.query, context)
            val ragTime = System.currentTimeMillis() - start

            if (request.enableSemanticCache) {
                questionService.cacheResponse(request.query, answer, true)
            }

            return RagResponse(
                query = request.query,
                ragAnswer = answer,
                matchedQuestions = matched,
                embeddingTime = embeddingTime,
                searchTime = searchTime,
                ragTime = ragTime
            )
        }

        return SearchOnlyResponse(
            query = request.query,
            matchedQuestions = matched,
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

    data class MatchedQuestion(
        val question: String,
        val utterances: String,
        val score: Double
    )

    open class SearchResponse(
        open val query: String,
        open val embeddingTime: Long,
        open val searchTime: Long? = null,
        open val ragTime: Long? = null
    )

    data class CachedResponse(
        val ragAnswer: String,
        val cachedQuery: String,
        val cachedScore: Float,
        val cacheSearchTime: Long,
        override val query: String,
        override val embeddingTime: Long
    ) : SearchResponse(query, embeddingTime)

    data class RagResponse(
        val ragAnswer: String,
        val matchedQuestions: List<MatchedQuestion>,
        override val query: String,
        override val embeddingTime: Long,
        override val searchTime: Long,
        override val ragTime: Long
    ) : SearchResponse(query, embeddingTime, searchTime, ragTime)

    data class SearchOnlyResponse(
        override val query: String,
        val matchedQuestions: List<MatchedQuestion>,
        override val embeddingTime: Long,
        override val searchTime: Long
    ) : SearchResponse(query, embeddingTime, searchTime)
}