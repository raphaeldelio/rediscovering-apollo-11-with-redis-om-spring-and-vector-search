package dev.raphaeldelio.redisapollo.photograph

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/image")
class ImageController(
    private val imageService: ImageService
) {

    @PostMapping("/search/by-image")
    fun searchByImage(@RequestBody request: ImageSearchRequest): Map<String, Any> {
        val tmpPath = imageService.saveTmpImage(request.imageBase64, request.imagePath)

        val embeddingStart = System.currentTimeMillis()
        val tmpPhoto = imageService.embedImage(tmpPath)
        val embeddingTime = System.currentTimeMillis() - embeddingStart

        val searchStart = System.currentTimeMillis()
        val matchedPhotographs = imageService.searchByImage(tmpPhoto.embeddedImage!!)
        val searchTime = System.currentTimeMillis() - searchStart

        return mapOf(
            "imagePath" to tmpPath,
            "matchedPhotographs" to matchedPhotographs,
            "embeddingTime" to "${embeddingTime}ms",
            "searchTime" to "${searchTime}ms"
        )
    }

    @PostMapping("/search/by-description")
    fun searchByImageText(@RequestBody request: SearchRequest): Map<String, Any> {
        val embeddingStart = System.currentTimeMillis()
        val embedding = imageService.embedDescription(request.query)
        val embeddingTime = System.currentTimeMillis() - embeddingStart

        val searchStart = System.currentTimeMillis()
        val matchedPhotographs = imageService.searchByImageText(embedding)
        val searchTime = System.currentTimeMillis() - searchStart

        return mapOf(
            "query" to request.query,
            "matchedPhotographs" to matchedPhotographs,
            "embeddingTime" to "${embeddingTime}ms",
            "searchTime" to "${searchTime}ms"
        )
    }

    data class SearchRequest(
        val query: String,
        val enableSemanticCache: Boolean,
        val enableRag: Boolean
    )

    data class ImageSearchRequest(
        val imageBase64: String,
        val imagePath: String?
    )
}