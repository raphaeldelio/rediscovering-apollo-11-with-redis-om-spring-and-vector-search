package dev.raphaeldelio.redisapollo.hash.repository;

import com.redis.om.spring.repository.RedisEnhancedRepository;
import dev.raphaeldelio.redisapollo.hash.domain.SearchSemanticCache;

public interface SearchSemanticCacheRepository extends RedisEnhancedRepository<SearchSemanticCache, String> {
}