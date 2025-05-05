package dev.raphaeldelio.redisapollo.question;

import com.redis.om.spring.repository.RedisEnhancedRepository;

public interface QuestionRepository extends RedisEnhancedRepository<Question, String> {
}