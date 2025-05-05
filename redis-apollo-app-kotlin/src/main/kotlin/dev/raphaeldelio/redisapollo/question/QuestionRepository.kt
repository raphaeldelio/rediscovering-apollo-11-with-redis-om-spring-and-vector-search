package dev.raphaeldelio.redisapollo.question

import com.redis.om.spring.repository.RedisEnhancedRepository

interface QuestionRepository : RedisEnhancedRepository<Question, String>