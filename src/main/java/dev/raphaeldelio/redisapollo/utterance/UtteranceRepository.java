package dev.raphaeldelio.redisapollo.utterance;

import com.redis.om.spring.repository.RedisEnhancedRepository;

public interface UtteranceRepository extends RedisEnhancedRepository<Utterance, String> {
}