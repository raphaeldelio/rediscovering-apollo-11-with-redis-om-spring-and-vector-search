package dev.raphaeldelio.redisapollo.dataloader.workflow

import dev.raphaeldelio.redisapollo.summary.Summary
import dev.raphaeldelio.redisapollo.summary.SummaryRepository
import dev.raphaeldelio.redisapollo.tableofcontents.TOCData
import dev.raphaeldelio.redisapollo.tableofcontents.TOCService
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class SummarizationWorkflow(
    private val tocService: TOCService,
    private val summaryRepository: SummaryRepository,
    private val summarizationChatClient: ChatClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun run() {
        logger.info("Summarizing TOC entries")
        val toGenerate = tocService.getAllWithoutSummary()

        if (toGenerate.isEmpty()) {
            logger.info("No TOC entries need summaries generated")
            return
        }

        summarize(toGenerate)

        val toEmbed = tocService.getAllWithSummary()
            .filter { toc -> summaryRepository.findById(toc.startDate).isEmpty }

        embedSummaries(toEmbed)
    }

    private suspend fun summarize(toGenerate: List<TOCData>) = coroutineScope {
        logger.info("Processing ${toGenerate.size} TOC entries")

        val processed = toGenerate.map { toc ->
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
            tocService.updateSummary(processed)
            logger.info("Saved {} TOC entries", processed.size)
        }

        logger.info("Completed generating summaries for all TOC entries")
    }

    private suspend fun embedSummaries(toEmbed: List<TOCData>) = coroutineScope {
        logger.info("Creating utterance summaries")

        val summaries = toEmbed.map { toc ->
                Summary(
                    toc.startDate,
                    toc.concatenatedUtterances!!,
                    toc.utterances ?: emptyList(),
                    toc.summary!!
                )
            }

        if (summaries.isEmpty()) {
            logger.info("No new summaries to embed")
            return@coroutineScope
        }

        val batchSize = 100
        summaries.chunked(batchSize)
            .map { batch ->
                async(Dispatchers.IO) {
                    summaryRepository.saveAll(batch)
                }
            }.awaitAll()

        logger.info("Utterance summaries embedded - total: ${summaries.size}")
    }
}