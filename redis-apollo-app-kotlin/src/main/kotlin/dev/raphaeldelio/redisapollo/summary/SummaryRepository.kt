package dev.raphaeldelio.redisapollo.summary

import com.redis.om.spring.repository.RedisEnhancedRepository

interface SummaryRepository : RedisEnhancedRepository<Summary, String>