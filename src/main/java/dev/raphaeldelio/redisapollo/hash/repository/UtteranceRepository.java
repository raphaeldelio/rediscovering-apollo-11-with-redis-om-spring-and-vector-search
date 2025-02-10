package dev.raphaeldelio.redisapollo.hash.repository;

import com.redis.om.spring.repository.RedisEnhancedRepository;
import dev.raphaeldelio.redisapollo.hash.domain.Utterance;

public interface UtteranceRepository extends RedisEnhancedRepository<Utterance, String> {
}