package dev.raphaeldelio.redisapollo

import com.redis.om.spring.client.RedisModulesClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import redis.clients.jedis.params.ZAddParams

@Service
class RedisService(private val redisModulesClient: RedisModulesClient) {

    private val logger = LoggerFactory.getLogger(RedisService::class.java)

    fun incrementZSet(key: String, member: String) {
        try {
            redisModulesClient.jedis.get().use { jedis ->
                jedis.zaddIncr(key, 1.0, member, ZAddParams.zAddParams())
            }
        } catch (e: Exception) {
            logger.error("Failed to increment ZSet: {} for member: {}", key, member, e)
        }
    }

    fun getZRangeByScore(key: String, min: Double, max: Double): List<String> {
        return try {
            redisModulesClient.jedis.get().use { jedis ->
                jedis.zrangeByScore(key, min, max)
            }
        } catch (e: Exception) {
            logger.error("Failed to get ZRangeByScore: {} for min: {} and max: {}", key, min, max, e)
            emptyList()
        }
    }
}