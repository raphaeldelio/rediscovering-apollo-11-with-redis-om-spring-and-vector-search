package dev.raphaeldelio.redisapollo;

import com.redis.om.spring.client.RedisModulesClient;
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

    public RedisService(RedisModulesClient redisModulesClient) {
        this.redisModulesClient = redisModulesClient;
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
}
