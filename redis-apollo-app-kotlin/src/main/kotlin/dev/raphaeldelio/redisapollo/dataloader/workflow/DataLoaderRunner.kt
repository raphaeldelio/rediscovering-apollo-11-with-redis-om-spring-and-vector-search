package dev.raphaeldelio.redisapollo.dataloader.workflow

import dev.raphaeldelio.redisapollo.photograph.PhotographService
import dev.raphaeldelio.redisapollo.tableofcontents.TOCService
import dev.raphaeldelio.redisapollo.utterance.UtteranceService
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class DataLoaderRunner {
    @Bean
    fun loadData(
        utteranceService: UtteranceService,
        tocService: TOCService,
        photographService: PhotographService,
        summarizationWorkflow: SummarizationWorkflow,
        questionGenerationWorkflow: QuestionGenerationWorkflow
    ): CommandLineRunner = CommandLineRunner {
        val startTime = System.currentTimeMillis()

        val dirPath = "./redis-apollo-app-kotlin/src/main/resources/Apollo11_Data"

        utteranceService.loadUtteranceData("$dirPath/gUtteranceData.json")

        tocService.loadTOCData("$dirPath/gTOCData.json")
        tocService.populateUtterances()

        runBlocking {
            summarizationWorkflow.run()
            questionGenerationWorkflow.run()
        }

        photographService.loadPhotographData("$dirPath/gPhotoData.json")

        val endTime = System.currentTimeMillis()
        println("Data loaded in ${endTime - startTime} milliseconds")
    }
}