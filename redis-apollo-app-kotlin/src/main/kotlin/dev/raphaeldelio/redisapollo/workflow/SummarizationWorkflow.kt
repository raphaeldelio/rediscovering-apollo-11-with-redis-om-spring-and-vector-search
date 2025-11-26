package dev.raphaeldelio.redisapollo.workflow

import dev.raphaeldelio.redisapollo.summary.Summary
import dev.raphaeldelio.redisapollo.summary.SummaryRepository
import dev.raphaeldelio.redisapollo.tableofcontents.TOCDataRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class SummarizationWorkflow(
    private val tocDataRepository: TOCDataRepository,
    private val summaryRepository: SummaryRepository,
    private val summarizationChatClient: ChatClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

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

    fun embedSummaries(overwrite: Boolean = false) {
        logger.info("Creating utterance summaries")

        val tocDataList = tocDataRepository.findAll()

        val toSaveList = tocDataList
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

        summaryRepository.saveAll(toSaveList)
        logger.info("Utterance summaries embedded")
    }
}