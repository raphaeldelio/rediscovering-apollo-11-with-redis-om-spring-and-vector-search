package dev.raphaeldelio.redisapollo.photograph;

import com.redis.om.spring.repository.RedisEnhancedRepository;

public interface PhotographsRepository extends RedisEnhancedRepository<Photograph, String> {
}