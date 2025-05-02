package dev.raphaeldelio.redisapollo.semanticcache;

import com.redis.om.spring.repository.RedisEnhancedRepository;

public interface SearchSemanticCacheRepository extends RedisEnhancedRepository<SearchSemanticCache, String> {
}