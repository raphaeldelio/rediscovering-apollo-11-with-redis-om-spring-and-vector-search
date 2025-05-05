package dev.raphaeldelio.redisapollo.utterance

import com.redis.om.spring.repository.RedisEnhancedRepository

interface UtteranceRepository : RedisEnhancedRepository<Utterance, String>