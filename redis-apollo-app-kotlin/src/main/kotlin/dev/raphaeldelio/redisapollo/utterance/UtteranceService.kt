package dev.raphaeldelio.redisapollo.utterance

import com.redis.om.spring.search.stream.EntityStream
import com.redis.om.spring.search.stream.SearchStream
import com.redis.om.spring.tuple.Fields
import com.redis.om.spring.tuple.Pair
import com.redis.om.spring.vectorize.Embedder
import dev.raphaeldelio.redisapollo.dataloader.FileService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class UtteranceService(
    private val utteranceRepository: UtteranceRepository,
    private val fileService: FileService,
    private val embedder: Embedder,
    private val entityStream: EntityStream
) {

    private val logger = LoggerFactory.getLogger(UtteranceService::class.java)

    fun loadUtteranceData(filePath: String) {
        logger.info("Loading utterance data from file: {}", filePath)

         fileService.readAndProcessFile(filePath, Utterance::class.java) { data ->
             val utterances = data.filter { isValidUtterance(it) }.map { utterance ->
                 utterance.timestampInt = fileService.toHMSToSeconds(utterance.timestamp)
                 utterance
             }

             utterances.chunked(1000).forEach { batch ->
                 utteranceRepository.saveAll(batch)
             }
         }

        logger.info("Utterance data loaded successfully")
    }

    fun count() = utteranceRepository.count()

    fun embedUtterance(text: String): ByteArray {
        logger.info("Creating utterance embedding for text: {}", text)
        return embedder.getTextEmbeddingsAsBytes(listOf(text), `Utterance$`.TEXT).first()
    }

    fun search(embedding: ByteArray): List<UtteranceSearchResult> {
        logger.info("Received utterance embedding: {}", embedding)

        val stream: SearchStream<Utterance> = entityStream.of(Utterance::class.java)
        val results: List<Pair<Utterance, Double>> = stream
            .filter(`Utterance$`.EMBEDDED_TEXT.knn(3, embedding))
            .sorted(`Utterance$`._EMBEDDED_TEXT_SCORE)
            .map(Fields.of(`Utterance$`._THIS, `Utterance$`._EMBEDDED_TEXT_SCORE))
            .collect(Collectors.toList())

        return results.map { UtteranceSearchResult(it.first.text, it.second) }
    }

    private fun isValidUtterance(utterance: Utterance): Boolean {
        return utterance.speaker.isNotBlank() &&
                utterance.speakerId.isNotBlank() &&
                utterance.text.isNotBlank() &&
               utterance.text != "..."
    }
}