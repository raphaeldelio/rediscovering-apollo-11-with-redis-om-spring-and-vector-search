package dev.raphaeldelio.redisapollo.dataloader

import dev.raphaeldelio.redisapollo.dataloader.workflow.QuestionGenerationWorkflow
import dev.raphaeldelio.redisapollo.dataloader.workflow.SummarizationWorkflow
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
        val dirPath = "./redis-apollo-app-kotlin/src/main/resources/Apollo11_Data"

        if (utteranceService.count() > 0) {
            println("Data already loaded")
            return@CommandLineRunner
        }

        val startTime = System.currentTimeMillis()

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