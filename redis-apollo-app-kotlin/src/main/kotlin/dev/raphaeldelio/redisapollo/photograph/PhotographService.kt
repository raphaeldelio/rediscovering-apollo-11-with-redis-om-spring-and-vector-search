package dev.raphaeldelio.redisapollo.photograph

import dev.raphaeldelio.redisapollo.FileService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PhotographService(
    private val photographsRepository: PhotographsRepository,
    private val fileService: FileService
) {
    private val logger = LoggerFactory.getLogger(PhotographService::class.java)

    fun loadPhotographData(filePath: String, overwrite: Boolean = false) {
        logger.info("Loading photograph data from file: {}", filePath)
        val batchSize = 100
        val batch = mutableListOf<Photograph>()

        fileService.readAndProcessFile(filePath, Photograph::class.java) { data ->
            data.forEach { photograph ->
                if (!overwrite && photographsRepository.existsById(photograph.timestamp)) {
                    logger.info("Photograph with timestamp {} already exists, skipping", photograph.timestamp)
                    return@forEach
                }

                val imagePath = "classpath:static/images/apollo11/${photograph.timestamp.toInt()}.jpg"
                photograph.imagePath = imagePath
                batch.add(photograph)

                if (batch.size >= batchSize) {
                    val timestamps = batch.joinToString(", ") { it.timestamp }
                    logger.info("Saving batch of ${batch.size} photographs with timestamps: $timestamps")
                    photographsRepository.saveAll(batch)
                    batch.clear()
                }
            }
        }

        if (batch.isNotEmpty()) {
            val timestamps = batch.joinToString(", ") { it.timestamp }
            logger.info("Saving final batch of ${batch.size} photographs with timestamps: $timestamps")
            try {
                photographsRepository.saveAll(batch)
            } catch (e: Exception) {
                logger.error("Error saving final batch of photographs: ${e.message}", e)
                for (photograph in batch) {
                    logger.info("Saving photograph with timestamp ${photograph.timestamp} individually")
                    photographsRepository.save(photograph)
                }
            }
        }

        logger.info("Photograph data loaded successfully")
    }
}