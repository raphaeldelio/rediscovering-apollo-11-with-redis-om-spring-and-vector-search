package dev.raphaeldelio.redisapollo.document.repository;

import com.redis.om.spring.repository.RedisDocumentRepository;
import dev.raphaeldelio.redisapollo.document.domain.TOCData;

public interface TOCDataRepository extends RedisDocumentRepository<TOCData, String> {
}