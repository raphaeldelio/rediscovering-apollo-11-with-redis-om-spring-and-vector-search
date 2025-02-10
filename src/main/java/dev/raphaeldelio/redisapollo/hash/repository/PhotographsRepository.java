package dev.raphaeldelio.redisapollo.hash.repository;

import com.redis.om.spring.repository.RedisEnhancedRepository;
import dev.raphaeldelio.redisapollo.hash.domain.Photograph;

public interface PhotographsRepository extends RedisEnhancedRepository<Photograph, String> {
}