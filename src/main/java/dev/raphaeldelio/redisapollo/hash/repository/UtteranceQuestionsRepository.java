package dev.raphaeldelio.redisapollo.hash.repository;

import com.redis.om.spring.repository.RedisEnhancedRepository;
import dev.raphaeldelio.redisapollo.hash.domain.UtteranceQuestions;

public interface UtteranceQuestionsRepository extends RedisEnhancedRepository<UtteranceQuestions, String> {
}