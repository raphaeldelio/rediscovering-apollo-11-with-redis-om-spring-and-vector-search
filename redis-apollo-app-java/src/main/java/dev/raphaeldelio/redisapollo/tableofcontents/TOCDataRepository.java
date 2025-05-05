package dev.raphaeldelio.redisapollo.tableofcontents;

import com.redis.om.spring.repository.RedisDocumentRepository;

public interface TOCDataRepository extends RedisDocumentRepository<TOCData, String> {
}