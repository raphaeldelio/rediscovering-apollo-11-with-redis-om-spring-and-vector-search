package dev.raphaeldelio.redisapollo.workflow

import dev.raphaeldelio.redisapollo.summary.Summary
import dev.raphaeldelio.redisapollo.summary.SummaryRepository
import dev.raphaeldelio.redisapollo.tableofcontents.TOCDataRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class SummarizationWorkflow(
    private val tocDataRepository: TOCDataRepository,
    private val summaryRepository: SummaryRepository,
    private val summarizationChatClient: ChatClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun summarize(overwrite: Boolean = false) = coroutineScope {
        logger.info("Summarizing TOC entries")
        val toProcess = tocDataRepository.findAll()
            .filter {
                !it.concatenatedUtterances.isNullOrBlank() &&
                        (overwrite || it.summary == null)
            }

        if (toProcess.isEmpty()) {
            logger.info("No TOC entries need summaries generated")
            return@coroutineScope
        }

        logger.info("Processing ${toProcess.size} TOC entries")

        val processed = toProcess.map { toc ->
            async(Dispatchers.IO) {
                try {
                    logger.info("Generating summary for TOC entry: {}", toc.startDate)
                    val response = summarizationChatClient
                        .prompt()
                        .user(toc.concatenatedUtterances ?: "")
                        .call()
                        .chatResponse()

                    toc.summary = response?.result?.output?.text
                    logger.info("Successfully generated summary for TOC entry: {}", toc.startDate)
                    toc
                } catch (e: Exception) {
                    logger.error("Error generating summary for TOC entry: {}", toc.startDate, e)
                    null
                }
            }
        }.mapNotNull { it.await() }

        if (processed.isNotEmpty()) {
            tocDataRepository.saveAll(processed)
            logger.info("Saved {} TOC entries", processed.size)
        }

        logger.info("Completed generating summaries for all TOC entries")
    }

    suspend fun embedSummaries(overwrite: Boolean = false) = coroutineScope {
        logger.info("Creating utterance summaries")

        val allSummaries = tocDataRepository.findAll()
            .filter { it.concatenatedUtterances != null && it.summary != null }
            .filter { toc ->
                overwrite || summaryRepository.findById(toc.startDate).isEmpty
            }
            .map { toc ->
                Summary(
                    toc.startDate,
                    toc.concatenatedUtterances!!,
                    toc.utterances ?: emptyList(),
                    toc.summary!!
                )
            }

        if (allSummaries.isEmpty()) {
            logger.info("No new summaries to embed")
            return@coroutineScope
        }

        val batchSize = 100

        allSummaries.chunked(batchSize)
            .map { batch ->
                async(Dispatchers.IO) {
                    summaryRepository.saveAll(batch)
                }
            }.awaitAll()

        logger.info("Utterance summaries embedded - total: ${allSummaries.size}")
    }
}