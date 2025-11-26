package dev.raphaeldelio.redisapollo.workflow

import dev.raphaeldelio.redisapollo.question.Question
import dev.raphaeldelio.redisapollo.question.QuestionRepository
import dev.raphaeldelio.redisapollo.tableofcontents.TOCDataRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class QuestionGenerationWorkflow(
    private val tocDataRepository: TOCDataRepository,
    private val questionRepository: QuestionRepository,
    private val questionGenerationChatClient: ChatClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun generateQuestions(overwrite: Boolean = false) = coroutineScope {
        logger.info("Generating questions for TOC entries")
        val toProcess = tocDataRepository.findAll()
            .filter {
                !it.concatenatedUtterances.isNullOrBlank() &&
                        (overwrite || it.questions == null)
            }

        if (toProcess.isEmpty()) {
            logger.info("No TOC entries need questions generated")
            return@coroutineScope
        }

        logger.info("Processing ${toProcess.size} TOC entries")

        val processed = toProcess.map { toc ->
            async(Dispatchers.IO) {
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
            }
        }.mapNotNull { it.await() }

        if (processed.isNotEmpty()) {
            tocDataRepository.saveAll(processed)
            logger.info("Saved {} TOC entries", processed.size)
        }

        logger.info("Completed generating questions for all TOC entries")
    }

    suspend fun embedQuestions(overwrite: Boolean = false) = coroutineScope {
        logger.info("Creating question embeddings")

        val tocDataList = tocDataRepository.findAll()
            .filter { it.concatenatedUtterances != null && it.questions != null }

        if (tocDataList.isEmpty()) {
            logger.info("No TOC entries with questions to embed")
            return@coroutineScope
        }

        val allQuestions = tocDataList.flatMap { toc ->
            toc.questions!!.mapIndexedNotNull { index, questionText ->
                val id = "${toc.startDate}-$index"
                if (questionRepository.findById(id).isEmpty || overwrite) {
                    Question(
                        id,
                        toc.concatenatedUtterances!!,
                        toc.utterances!!,
                        questionText
                    )
                } else null
            }
        }

        if (allQuestions.isEmpty()) {
            logger.info("No new questions to embed")
            return@coroutineScope
        }

        val batchSize = 100

        allQuestions.chunked(batchSize)
            .map { batch ->
                async(Dispatchers.IO) {
                    questionRepository.saveAll(batch)
                }
            }
            .awaitAll()

        logger.info("Embedding questions created successfully - total: ${allQuestions.size}")
    }
}