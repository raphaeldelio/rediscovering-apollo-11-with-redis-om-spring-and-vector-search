package dev.raphaeldelio.redisapollo

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories
import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories
import dev.raphaeldelio.redisapollo.photograph.PhotographService
import dev.raphaeldelio.redisapollo.tableofcontents.TOCDataRepository
import dev.raphaeldelio.redisapollo.tableofcontents.TOCService
import dev.raphaeldelio.redisapollo.utterance.UtteranceService
import dev.raphaeldelio.redisapollo.dataloader.workflow.QuestionGenerationWorkflow
import dev.raphaeldelio.redisapollo.dataloader.workflow.SummarizationWorkflow
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType


@EnableRedisDocumentRepositories(basePackages = ["dev.raphaeldelio.redisapollo.tableofcontents"])
@EnableRedisEnhancedRepositories(
    basePackages = ["dev.raphaeldelio.redisapollo"],
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = [TOCDataRepository::class]
    )]
)
@SpringBootApplication
class RedisApolloApplication

fun main(args: Array<String>) {
    runApplication<RedisApolloApplication>(*args)
}