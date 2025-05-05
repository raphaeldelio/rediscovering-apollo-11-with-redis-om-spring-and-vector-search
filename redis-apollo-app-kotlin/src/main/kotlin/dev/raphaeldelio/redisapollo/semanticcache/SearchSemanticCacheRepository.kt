package dev.raphaeldelio.redisapollo.semanticcache

import com.redis.om.spring.repository.RedisEnhancedRepository

interface SearchSemanticCacheRepository : RedisEnhancedRepository<SearchSemanticCache, String>