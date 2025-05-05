package dev.raphaeldelio.redisapollo.summary;

import com.redis.om.spring.repository.RedisEnhancedRepository;

public interface SummaryRepository extends RedisEnhancedRepository<Summary, String> {
}