package dev.raphaeldelio.redisapollo.summary

import com.redis.om.spring.search.stream.EntityStream
import com.redis.om.spring.search.stream.SearchStream
import com.redis.om.spring.tuple.Fields
import com.redis.om.spring.vectorize.Embedder
import dev.raphaeldelio.redisapollo.tableofcontents.TOCDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class SummaryService(
    private val tocDataRepository: TOCDataRepository,
    private val summaryRepository: SummaryRepository,
    private val embedder: Embedder,
    private val entityStream: EntityStream
) {
    private val logger = LoggerFactory.getLogger(SummaryService::class.java)

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
                    toc.utterances!!,
                    toc.summary!!
                )
            }

        summaryRepository.saveAll(toSaveList)
        logger.info("Utterance summaries embedded")
    }

    fun embedQuery(query: String): ByteArray {
        logger.info("Received question: {}", query)
        return embedder.getTextEmbeddingsAsBytes(listOf(query), `Summary$`.SUMMARY).first()
    }

    fun searchBySummary(embedding: ByteArray): List<SummarySearchResult> {
        val stream: SearchStream<Summary> = entityStream.of(Summary::class.java)
        val summaries = stream
            .filter(`Summary$`.EMBEDDED_SUMMARY.knn(3, embedding))
            .sorted(`Summary$`._EMBEDDED_SUMMARY_SCORE)
            .map(Fields.of(`Summary$`._THIS, `Summary$`._EMBEDDED_SUMMARY_SCORE))
            .collect(Collectors.toList())

        return summaries.map { SummarySearchResult(it.first, it.second) }
    }
}