package dev.raphaeldelio.redisapollo.service;

import com.redis.om.spring.client.RedisModulesClient;
import com.redis.om.spring.ops.RedisModulesOperations;
import com.redis.om.spring.ops.pds.CountMinSketchOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ZAddParams;

import java.util.List;

@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);
    private final RedisModulesClient redisModulesClient;
    private final RedisModulesOperations<String> redisModulesOperations;
    private final CountMinSketchOperations<String> countMinSketchOperations;

    public RedisService(RedisModulesClient redisModulesClient, RedisModulesOperations<String> redisModulesOperations) {
        this.redisModulesClient = redisModulesClient;
        this.redisModulesOperations = redisModulesOperations;
        this.countMinSketchOperations = redisModulesOperations.opsForCountMinSketch();
    }

    public void incrementZSet(String key, String member) {
        try (Jedis jedis = redisModulesClient.getJedis().get()) {
            jedis.zaddIncr(key, 1.0, member, ZAddParams.zAddParams());
        } catch (Exception e) {
            logger.error("Failed to increment ZSet: {} for member: {}", key, member, e);
        }
    }

    public List<String> getZRangeByScore(String key, double min, double max) {
        try (Jedis jedis = redisModulesClient.getJedis().get()) {
            return jedis.zrangeByScore(key, min, max);
        } catch (Exception e) {
            logger.error("Failed to get ZRangeByScore: {} for min: {} and max: {}", key, min, max, e);
            return List.of();
        }
    }

    public void incrementWordCount(String word) {
        try {
            countMinSketchOperations.cmsIncrBy("wordCount", word, 1);
        } catch (Exception e) {
            logger.error("Failed to increment word count for: {}", word, e);
        }
    }

    public void initializeCountMinSketch(String key, long width, long depth) {
        try {
            countMinSketchOperations.cmsInitByDim(key, width, depth);
        } catch (Exception e) {
            logger.error("Failed to initialize CountMinSketch for key: {}", key, e);
        }
    }
}
