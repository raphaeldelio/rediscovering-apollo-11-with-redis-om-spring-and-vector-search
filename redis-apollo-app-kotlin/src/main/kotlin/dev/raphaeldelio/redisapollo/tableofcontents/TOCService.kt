package dev.raphaeldelio.redisapollo.tableofcontents

import com.redis.om.spring.search.stream.EntityStream
import dev.raphaeldelio.redisapollo.FileService
import dev.raphaeldelio.redisapollo.RedisService
import dev.raphaeldelio.redisapollo.utterance.Utterance
import dev.raphaeldelio.redisapollo.utterance.`Utterance$`.TIMESTAMP_INT
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.stream.Collectors

@Service
class TOCService(
    private val tocDataRepository: TOCDataRepository,
    private val chatModel: OpenAiChatModel,
    private val entityStream: EntityStream,
    private val fileService: FileService,
    private val redisService: RedisService
) {
    private val logger = LoggerFactory.getLogger(TOCService::class.java)

    fun loadTOCData(filePath: String) {
        logger.info("Loading TOC data from file: {}", filePath)
        fileService.readAndProcessFile(filePath, TOCData::class.java) { data ->
            data.filter(::isValidToc)
                .onEach { tocData ->
                    val startInt = fileService.asSeconds(tocData.startDate)
                    tocData.startDate = tocData.startDate.replace(":", ";")
                    tocData.startDateInt = startInt }
                .chunked(100) { batch ->
                    tocDataRepository.saveAll(batch)
                }
        }
    }

    private fun isValidToc(tocData: TOCData): Boolean {
        return !tocData.description.startsWith("Video: ")
    }

    fun populateUtterances(overwrite: Boolean = false) {
        logger.info("Grouping utterances by TOC entries")
        val tocList = entityStream.of(TOCData::class.java)
            .sorted(`TOCData$`.START_DATE_INT)
            .collect(Collectors.toList())

        val toFilterOut = redisService.getZRangeByScore("utterances", 2.0, Double.MAX_VALUE)

        tocList.onEach { logger.info("Processing TOC entry: {}", it.startDateInt) }
            .filter { overwrite || it.concatenatedUtterances.isNullOrBlank() }
            .onEach {
                if (!overwrite) {
                    logger.info("Utterances already grouped for TOC entry: {}", it.startDate)
                }
            }
            .mapNotNull { toc ->
                val start = toc.startDateInt
                val end = tocList.getOrNull(tocList.indexOf(toc) + 1)?.startDateInt ?: Int.MAX_VALUE

                val utterances = entityStream.of(Utterance::class.java)
                    .filter(TIMESTAMP_INT.ge(start).and(TIMESTAMP_INT.le(end)))
                    .collect(Collectors.toList())
                    .filterNot { it.text in toFilterOut }

                if (utterances.isEmpty()) {
                    logger.info("No utterances found for TOC entry: {}", toc.startDate)
                    return@mapNotNull null
                }

                val grouped = utterances.joinToString("\n") { "${it.speaker}: ${it.text}" }

                toc.apply {
                    concatenatedUtterances = grouped
                    this.utterances = utterances
                }
            }
            .onEach {
                tocDataRepository.save(it)
                logger.info("Grouped utterances for TOC entry: {}", it.startDate)
            }

        logger.info("Grouping complete")
    }

    fun summarize(overwrite: Boolean = false) {
        logger.info("Summarizing TOC entries")
        val toProcess = tocDataRepository.findAll()
            .filter {
                !it.concatenatedUtterances.isNullOrBlank() &&
                (overwrite || it.summary == null)
            }

        if (toProcess.isEmpty()) {
            logger.info("No TOC entries need summaries generated")
            return
        }

        val batchSize = 300
        val totalBatches = (toProcess.size + batchSize - 1) / batchSize

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            for (batchIndex in 0 until totalBatches) {
                val batch = toProcess.drop(batchIndex * batchSize).take(batchSize)
                logger.info("Processing batch ${batchIndex + 1} of $totalBatches (${batch.size} entries)")

                val futures = batch.map { toc ->
                    CompletableFuture.supplyAsync({
                        try {
                            logger.info("Generating summary for TOC entry: {}", toc.startDate)
                            val response: ChatResponse = chatModel.call(
                                Prompt(
                                    listOf(
                                        SystemMessage("""
                                            You are a helpful assistant who summarizes utterances of the Apollo 11 mission.
                                            Make these summaries very dense with all curiosities included.
                                            Limit the summary to 512 words.
                                        """.trimIndent()),
                                        UserMessage(toc.concatenatedUtterances ?: "")
                                    )
                                )
                            )
                            toc.summary = response.result.output.text
                            logger.info("Successfully generated summary for TOC entry: {}", toc.startDate)
                            toc
                        } catch (e: Exception) {
                            logger.error("Error generating summary for TOC entry: {}", toc.startDate, e)
                            null
                        }
                    }, executor)
                }

                val processed = futures.mapNotNull { it.join() }
                if (processed.isNotEmpty()) {
                    tocDataRepository.saveAll(processed)
                    logger.info("Saved {} TOC entries in batch", processed.size)
                }

                logger.info("Completed batch ${batchIndex + 1} of $totalBatches")
            }
        }

        logger.info("Completed generating summaries for all TOC entries")
    }

    fun generateQuestions(overwrite: Boolean = false) {
        logger.info("Generating questions for TOC entries")
        val toProcess = tocDataRepository.findAll()
            .filter {
                !it.concatenatedUtterances.isNullOrBlank() &&
                (overwrite || it.questions == null)
            }

        if (toProcess.isEmpty()) {
            logger.info("No TOC entries need questions generated")
            return
        }

        val batchSize = 300
        val totalBatches = (toProcess.size + batchSize - 1) / batchSize

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            for (batchIndex in 0 until totalBatches) {
                val batch = toProcess.drop(batchIndex * batchSize).take(batchSize)
                logger.info("Processing batch ${batchIndex + 1} of $totalBatches (${batch.size} entries)")

                val futures = batch.map { toc ->
                    CompletableFuture.supplyAsync({
                        try {
                            logger.info("Generating questions for TOC entry: {}", toc.startDate)
                            val response: ChatResponse = chatModel.call(
                                Prompt(
                                    listOf(
                                        SystemMessage("""
                                            You are a helpful assistant that is helping me predict which questions can be asked by people who are trying to
                                            rediscover the Apollo 11 mission data. You will be given a number of utterances and you will predict the questions that
                                            can be asked by people who are trying to rediscover the Apollo 11 mission data. You will ONLY return the questions separate by breaklines,
                                            and nothing more. You will NEVER return more than 512 words.
                                        """.trimIndent()),
                                        UserMessage(toc.concatenatedUtterances ?: "")
                                    )
                                )
                            )

                            toc.questions = response.result.output.text
                                ?.lines()
                                ?.filter { it.isNotBlank() }

                            logger.info("Successfully generated {} questions for TOC entry: {}", toc.questions?.size, toc.startDate)
                            toc
                        } catch (e: Exception) {
                            logger.error("Error generating questions for TOC entry: {}", toc.startDate, e)
                            null
                        }
                    }, executor)
                }

                val processed = futures.mapNotNull { it.join() }
                if (processed.isNotEmpty()) {
                    tocDataRepository.saveAll(processed)
                    logger.info("Saved {} TOC entries in batch", processed.size)
                }

                logger.info("Completed batch ${batchIndex + 1} of $totalBatches")
            }
        }

        logger.info("Completed generating questions for all TOC entries")
    }
}