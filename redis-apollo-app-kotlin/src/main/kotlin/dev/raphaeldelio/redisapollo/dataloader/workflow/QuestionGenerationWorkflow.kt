package dev.raphaeldelio.redisapollo.dataloader.workflow

import dev.raphaeldelio.redisapollo.question.Question
import dev.raphaeldelio.redisapollo.question.QuestionRepository
import dev.raphaeldelio.redisapollo.tableofcontents.TOCData
import dev.raphaeldelio.redisapollo.tableofcontents.TOCService
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class QuestionGenerationWorkflow(
    private val tocService: TOCService,
    private val questionRepository: QuestionRepository,
    private val questionGenerationChatClient: ChatClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun run() {
        logger.info("Creating questions for TOC entries")
        val toGenerate = tocService.getAllWithoutQuestions()

        if (toGenerate.isEmpty()) {
            logger.info("No TOC entries need questions generated")
            return
        }

        generateQuestions(toGenerate)

        val toEmbed = tocService.getAllWithQuestions()

        if (toEmbed.isEmpty()) {
            logger.info("No TOC entries with questions to embed")
            return
        }

        embedQuestions(toEmbed)
    }

    private suspend fun generateQuestions(toGenerate: List<TOCData>) = coroutineScope {
        logger.info("Processing ${toGenerate.size} TOC entries")

        val processed = toGenerate.map { toc ->
            async(Dispatchers.IO) {
                runCatching {
                    logger.info("Generating questions for TOC entry: {}", toc.startDate)

                    val response = questionGenerationChatClient
                        .prompt()
                        .user(toc.concatenatedUtterances.orEmpty())
                        .call()
                        .chatResponse()

                    toc.questions = response?.result?.output?.text
                        ?.lines()
                        ?.filter { it.isNotBlank() }

                    logger.info(
                        "Successfully generated {} questions for TOC entry: {}",
                        toc.questions?.size,
                        toc.startDate
                    )

                    toc
                }.onFailure { e ->
                    logger.error("Error generating questions for TOC entry: {}", toc.startDate, e)
                }.getOrNull()
            }
        }.mapNotNull { it.await() }

        if (processed.isNotEmpty()) {
            tocService.updateQuestions(processed)
            logger.info("Saved {} TOC entries", processed.size)
        }

        logger.info("Completed generating questions for all TOC entries")
    }

    private suspend fun embedQuestions(toEmbed: List<TOCData>) = coroutineScope {
        logger.info("Creating question embeddings")

        val allQuestions = toEmbed.flatMap { toc ->
            toc.questions!!.mapIndexedNotNull { index, questionText ->
                val id = "${toc.startDate}-$index"
                if (questionRepository.findById(id).isEmpty) {
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
            }.awaitAll()

        logger.info("Embedding questions created successfully - total: ${allQuestions.size}")
    }
}