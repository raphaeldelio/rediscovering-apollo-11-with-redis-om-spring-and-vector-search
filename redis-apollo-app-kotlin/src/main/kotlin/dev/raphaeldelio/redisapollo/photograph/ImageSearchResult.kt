package dev.raphaeldelio.redisapollo.photograph

data class ImageSearchResult(
    val imagePath: String,
    val description: String,
    val score: Double
)