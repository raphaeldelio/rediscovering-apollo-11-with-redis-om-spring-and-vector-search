package dev.raphaeldelio.redisapollo.summary

import com.redis.om.spring.search.stream.EntityStream
import com.redis.om.spring.search.stream.SearchStream
import com.redis.om.spring.tuple.Fields
import com.redis.om.spring.vectorize.Embedder
import com.redis.vl.extensions.cache.CacheHit
import com.redis.vl.extensions.cache.SemanticCache
import dev.raphaeldelio.redisapollo.tableofcontents.TOCDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional
import java.util.stream.Collectors

@Service
class SummaryService(
    private val embedder: Embedder,
    private val entityStream: EntityStream,
    private val summarySemanticCache: SemanticCache
) {
    private val logger = LoggerFactory.getLogger(SummaryService::class.java)

    fun embedQuery(query: String): ByteArray {
        logger.info("Received question: {}", query)
        return embedder.getTextEmbeddingsAsBytes(listOf(query), `Summary$`.SUMMARY).first()
    }

    fun searchBySummary(embedding: ByteArray): List<SummarySearchResult> {
        val stream: SearchStream<Summary> = entityStream.of(Summary::class.java)
        val summaries = stream
            .filter(`Summary$`.EMBEDDED_SUMMARY.knn(3, embedding))
            .sorted(`Summary$`._EMBEDDED_SUMMARY_SCORE)
            .map(Fields.of(`Summary$`._THIS, `Summary$`._EMBEDDED_SUMMARY_SCORE))
            .collect(Collectors.toList())

        return summaries.map { SummarySearchResult(it.first, it.second) }
    }

    fun getCacheResponse(question: String): Optional<CacheHit> {
        return summarySemanticCache.check(question)
    }

    fun cacheResponse(query: String, answer: String, isQuestion: Boolean) {
        summarySemanticCache.store(query, answer)
    }
}