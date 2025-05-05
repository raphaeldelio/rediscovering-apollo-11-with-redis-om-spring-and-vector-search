package dev.raphaeldelio.redisapollo.tableofcontents

import com.redis.om.spring.repository.RedisDocumentRepository

interface TOCDataRepository : RedisDocumentRepository<TOCData, String>