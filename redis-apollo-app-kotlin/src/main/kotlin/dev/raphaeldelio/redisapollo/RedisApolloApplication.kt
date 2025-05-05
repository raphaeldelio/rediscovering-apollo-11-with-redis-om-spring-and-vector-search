package dev.raphaeldelio.redisapollo

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories
import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories
import dev.raphaeldelio.redisapollo.photograph.PhotographService
import dev.raphaeldelio.redisapollo.question.QuestionService
import dev.raphaeldelio.redisapollo.summary.SummaryService
import dev.raphaeldelio.redisapollo.tableofcontents.TOCDataRepository
import dev.raphaeldelio.redisapollo.tableofcontents.TOCService
import dev.raphaeldelio.redisapollo.utterance.UtteranceService
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

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
        summaryService: SummaryService
    ): CommandLineRunner = CommandLineRunner {
        val startTime = System.currentTimeMillis()

        val filePath = "./redis-apollo-app-kotlin/src/main/resources/Apollo11_Data"
        utteranceService.loadUtteranceData("$filePath/gUtteranceData.json")
        tocService.loadTOCData("$filePath/gTOCData.json")
        tocService.populateUtterances()
        tocService.summarize()
        summaryService.embedSummaries()
        tocService.generateQuestions()
        questionService.embedQuestions()
        photographService.loadPhotographData("$filePath/gPhotoData.json")

        val endTime = System.currentTimeMillis()
        println("Data loaded in ${endTime - startTime} milliseconds")
    }

    @Bean
    fun openAiChatModel(): OpenAiChatModel {
        val factory = SimpleClientHttpRequestFactory().apply {
            setReadTimeout(Duration.ofSeconds(60))
        }

        val openAiApi = OpenAiApi.builder()
            .apiKey(System.getenv("OPEN_AI_KEY"))
            .restClientBuilder(RestClient.builder().requestFactory(factory))
            .build()

        val options = OpenAiChatOptions.builder()
            .model("gpt-4o-mini")
            .build()

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build()
    }
}

fun main(args: Array<String>) {
    runApplication<RedisApolloApplication>(*args)
}