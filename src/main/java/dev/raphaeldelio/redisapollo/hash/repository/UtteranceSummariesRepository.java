package dev.raphaeldelio.redisapollo.hash.repository;

import com.redis.om.spring.repository.RedisEnhancedRepository;
import dev.raphaeldelio.redisapollo.hash.domain.UtteranceQuestions;
import dev.raphaeldelio.redisapollo.hash.domain.UtteranceSummaries;

public interface UtteranceSummariesRepository extends RedisEnhancedRepository<UtteranceSummaries, String> {
}