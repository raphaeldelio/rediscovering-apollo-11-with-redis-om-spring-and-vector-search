package dev.raphaeldelio.redisapollo.workflow

import dev.raphaeldelio.redisapollo.question.Question
import dev.raphaeldelio.redisapollo.question.QuestionRepository
import dev.raphaeldelio.redisapollo.tableofcontents.TOCDataRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class QuestionGenerationWorkflow(
    private val tocDataRepository: TOCDataRepository,
    private val questionRepository: QuestionRepository,
    private val questionGenerationChatClient: ChatClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

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
                            val response = questionGenerationChatClient
                                .prompt()
                                .user(toc.concatenatedUtterances ?: "")
                                .call()
                                .chatResponse()

                            toc.questions = response?.result?.output?.text
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

    fun embedQuestions(overwrite: Boolean = false) {
        logger.info("Creating question embeddings")

        val tocDataList = tocDataRepository.findAll()
        val toSaveList = mutableListOf<Question>()

        tocDataList.forEach { toc ->
            val hasData = toc.concatenatedUtterances != null && toc.questions != null
            if (hasData) {
                toc.questions!!.forEachIndexed { index, questionText ->
                    val id = "${toc.startDate}-$index"
                    if (questionRepository.findById(id).isEmpty || overwrite) {
                        val question = Question(
                            id,
                            toc.concatenatedUtterances!!,
                            toc.utterances!!,
                            questionText
                        )
                        toSaveList.add(question)
                    }
                }
            }
        }

        questionRepository.saveAll(toSaveList)
        logger.info("Embedding questions created successfully")
    }
}