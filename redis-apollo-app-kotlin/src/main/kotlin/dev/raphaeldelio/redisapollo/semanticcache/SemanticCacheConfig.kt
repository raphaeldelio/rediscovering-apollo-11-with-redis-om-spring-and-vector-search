package dev.raphaeldelio.redisapollo.semanticcache

import com.redis.vl.extensions.cache.SemanticCache
import com.redis.vl.utils.vectorize.BaseVectorizer
import com.redis.vl.utils.vectorize.SentenceTransformersVectorizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.UnifiedJedis

@Configuration
class SemanticCacheConfig {

    @Bean
    fun vectorizer(): SentenceTransformersVectorizer {
        return SentenceTransformersVectorizer("Xenova/all-MiniLM-L6-v2")
    }

    @Bean
    fun jedis(
        jedisConnectionFactory: JedisConnectionFactory,
        @Value("\${spring.data.redis.username:}") username: String?,
    ): UnifiedJedis {
        return JedisPooled(
            jedisConnectionFactory.hostName,
            jedisConnectionFactory.port,
            username,
            jedisConnectionFactory.password
        )
    }

    @Bean
    fun questionsSemanticCache(
        jedis: UnifiedJedis,
        vectorizer: BaseVectorizer
    ): SemanticCache {
        return SemanticCache.Builder()
            .name("questions-cache")
            .distanceThreshold(0.2F)
            .ttl(360)
            .redisClient(jedis)
            .vectorizer(vectorizer)
            .build()
    }

    @Bean
    fun summarySemanticCache(
        jedis: UnifiedJedis,
        vectorizer: BaseVectorizer
    ): SemanticCache {
        return SemanticCache.Builder()
            .name("summary-cache")
            .distanceThreshold(0.2F)
            .ttl(360)
            .redisClient(jedis)
            .vectorizer(vectorizer)
            .build()
    }
}