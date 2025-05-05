package dev.raphaeldelio.redisapollo.photograph

import com.redis.om.spring.repository.RedisEnhancedRepository

interface PhotographsRepository : RedisEnhancedRepository<Photograph, String>