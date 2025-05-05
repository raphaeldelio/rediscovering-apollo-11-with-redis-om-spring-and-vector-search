package dev.raphaeldelio.redisapollo.photograph

import com.redis.om.spring.search.stream.EntityStream
import com.redis.om.spring.search.stream.SearchStream
import com.redis.om.spring.tuple.Fields
import com.redis.om.spring.vectorize.Embedder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.util.*
import java.util.stream.Collectors

@Service
class ImageService(
    private val entityStream: EntityStream,
    private val embedder: Embedder,
    private val photographsRepository: PhotographsRepository
) {
    private val logger = LoggerFactory.getLogger(ImageService::class.java)

    fun saveTmpImage(imageBase64: String?, imagePath: String?): String {
        return when {
            imageBase64 != null -> {
                try {
                    val decodedBytes = Base64.getDecoder().decode(imageBase64)
                    val tempFile = Files.createTempFile("uploaded-image-", ".jpg")
                    Files.write(tempFile, decodedBytes)
                    val savedImagePath = tempFile.toUri().toString()
                    logger.info("Received Base64 image, saved to: {}", savedImagePath)
                    savedImagePath
                } catch (e: Exception) {
                    throw IllegalArgumentException("Error saving uploaded image", e)
                }
            }
            imagePath != null -> {
                logger.info("Received image path: {}", imagePath)
                imagePath
            }
            else -> throw IllegalArgumentException("No image provided")
        }
    }

    fun embedImage(tmpImagePath: String): Photograph {
        val tmpPhoto = Photograph("tmp", "tmp", null, null, null).apply {
            imagePath = tmpImagePath
        }
        return photographsRepository.save(tmpPhoto)
    }

    fun embedDescription(description: String): ByteArray {
        return embedder.getTextEmbeddingsAsBytes(listOf(description), `Photograph$`.DESCRIPTION).first()
    }

    fun searchByImage(imageEmbedding: ByteArray): List<ImageSearchResult> {
        val stream: SearchStream<Photograph> = entityStream.of(Photograph::class.java)
        return stream
            .filter(`Photograph$`.EMBEDDED_IMAGE.knn(20, imageEmbedding))
            .sorted(`Photograph$`._EMBEDDED_IMAGE_SCORE)
            .map(Fields.of(`Photograph$`._THIS, `Photograph$`._EMBEDDED_IMAGE_SCORE))
            .collect(Collectors.toList())
            .filter { it.first.name != "tmp" }
            .map {
                ImageSearchResult(
                    imagePath = it.first.imagePath.replace("classpath:static", ""),
                    description = it.first.description,
                    score = it.second
                )
            }
    }

    fun searchByImageText(descriptionEmbedding: ByteArray): List<ImageSearchResult> {
        val stream: SearchStream<Photograph> = entityStream.of(Photograph::class.java)
        return stream
            .filter(`Photograph$`.EMBEDDED_DESCRIPTION.knn(3, descriptionEmbedding))
            .sorted(`Photograph$`._EMBEDDED_DESCRIPTION_SCORE)
            .map(Fields.of(`Photograph$`._THIS, `Photograph$`._EMBEDDED_DESCRIPTION_SCORE))
            .collect(Collectors.toList())
            .map {
                ImageSearchResult(
                    imagePath = it.first.imagePath.replace("classpath:static", ""),
                    description = it.first.description,
                    score = it.second
                )
            }
    }
}