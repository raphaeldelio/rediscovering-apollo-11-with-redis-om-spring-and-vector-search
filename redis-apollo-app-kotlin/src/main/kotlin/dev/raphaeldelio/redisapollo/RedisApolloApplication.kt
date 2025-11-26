package dev.raphaeldelio.redisapollo

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories
import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories
import dev.raphaeldelio.redisapollo.photograph.PhotographService
import dev.raphaeldelio.redisapollo.question.QuestionService
import dev.raphaeldelio.redisapollo.summary.SummaryService
import dev.raphaeldelio.redisapollo.tableofcontents.TOCDataRepository
import dev.raphaeldelio.redisapollo.tableofcontents.TOCService
import dev.raphaeldelio.redisapollo.utterance.UtteranceService
import dev.raphaeldelio.redisapollo.workflow.QuestionGenerationWorkflow
import dev.raphaeldelio.redisapollo.workflow.SummarizationWorkflow
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
class RedisApolloApplication {

    @Bean
    fun loadData(
        utteranceService: UtteranceService,
        tocService: TOCService,
        photographService: PhotographService,
        questionService: QuestionService,
        summaryService: SummaryService,
        summarizationWorkflow: SummarizationWorkflow,
        questionGenerationWorkflow: QuestionGenerationWorkflow
    ): CommandLineRunner = CommandLineRunner {
        val startTime = System.currentTimeMillis()

        val filePath = "./redis-apollo-app-kotlin/src/main/resources/Apollo11_Data"
        //utteranceService.loadUtteranceData("$filePath/gUtteranceData.json")
        tocService.loadTOCData("$filePath/gTOCData.json")
        tocService.populateUtterances()
        summarizationWorkflow.summarize()
        summarizationWorkflow.embedSummaries()
        questionGenerationWorkflow.generateQuestions()
        questionGenerationWorkflow.embedQuestions()
        //photographService.loadPhotographData("$filePath/gPhotoData.json")

        val endTime = System.currentTimeMillis()
        println("Data loaded in ${endTime - startTime} milliseconds")
    }
}

fun main(args: Array<String>) {
    runApplication<RedisApolloApplication>(*args)
}