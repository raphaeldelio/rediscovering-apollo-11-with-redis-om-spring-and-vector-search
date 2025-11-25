package dev.raphaeldelio.redisapollo.semanticcache

import com.redis.om.spring.search.stream.EntityStream
import com.redis.om.spring.search.stream.SearchStream
import com.redis.om.spring.tuple.Fields
import com.redis.om.spring.tuple.Pair
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class SearchSemanticCacheService(
    private val entityStream: EntityStream,
    private val searchSemanticCacheRepository: SearchSemanticCacheRepository
) {
    private val logger = LoggerFactory.getLogger(SearchSemanticCacheService::class.java)

    fun getCacheResponse(embedding: ByteArray, isQuestion: Boolean): List<Pair<SearchSemanticCache, Double>> {
        val stream: SearchStream<SearchSemanticCache> = entityStream.of(SearchSemanticCache::class.java)
        return stream
            .filter(`SearchSemanticCache$`.EMBEDDED_QUERY.knn(1, embedding))
            .filter(`SearchSemanticCache$`.QUESTION.eq(isQuestion))
            .sorted(`SearchSemanticCache$`._EMBEDDED_QUERY_SCORE)
            .map(Fields.of(`SearchSemanticCache$`._THIS, `SearchSemanticCache$`._EMBEDDED_QUERY_SCORE))
            .collect(Collectors.toList())
    }

    fun cacheResponse(query: String, answer: String, isQuestion: Boolean) {
        val cache = SearchSemanticCache(
            query = query,
            answer = answer,
            question = isQuestion,
        )
        searchSemanticCacheRepository.save(cache)
    }

    fun clearCache() {
        searchSemanticCacheRepository.deleteAll()
        logger.info("Cache cleared")
    }
}