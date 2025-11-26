package dev.raphaeldelio.redisapollo.photograph

import dev.raphaeldelio.redisapollo.dataloader.FileService
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

        fileService.readAndProcessFile(filePath, Photograph::class.java) { data ->
            data.filter { overwrite || !photographsRepository.existsById(it.timestamp) }
                .onEach { photograph ->
                    photograph.imagePath = "classpath:static/images/apollo11/${photograph.timestamp.toInt()}.jpg"
                }
                .chunked(batchSize)
                .forEachIndexed { index, batch ->
                    val timestamps = batch.joinToString(", ") { it.timestamp }
                    logger.info("Saving batch ${index + 1} (${batch.size} photographs): $timestamps")
                    photographsRepository.saveAll(batch)
                }
        }

        logger.info("Photograph data loaded successfully")
    }
}