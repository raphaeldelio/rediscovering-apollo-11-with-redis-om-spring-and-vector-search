package dev.raphaeldelio.redisapollo.photograph

import com.redis.om.spring.annotations.*
import com.redis.om.spring.indexing.DistanceMetric
import com.redis.om.spring.indexing.VectorType
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.lang.NonNull
import redis.clients.jedis.search.schemafields.VectorField

@RedisHash
data class Photograph(
    @Id
    @NonNull
    var timestamp: String = "",

    @NonNull
    var name: String = "",

    var internalUrl: String? = null,

    var externalUrl: String? = null,

    @Vectorize(
        destination = "embeddedDescription",
        embeddingType = EmbeddingType.SENTENCE,
    )
    var description: String = "",

    @NonNull
    @Vectorize(
        destination = "embeddedImage",
        embeddingType = EmbeddingType.IMAGE
    )
    var imagePath: String = "",

    @Indexed(
        schemaFieldType = SchemaFieldType.VECTOR,
        algorithm = VectorField.VectorAlgorithm.HNSW,
        type = VectorType.FLOAT32,
        dimension = 512,
        distanceMetric = DistanceMetric.COSINE,
        initialCapacity = 10
    )
    var embeddedImage: ByteArray? = null,

    @Indexed(
        schemaFieldType = SchemaFieldType.VECTOR,
        algorithm = VectorField.VectorAlgorithm.HNSW,
        type = VectorType.FLOAT32,
        dimension = 384,
        distanceMetric = DistanceMetric.COSINE,
        initialCapacity = 10
    )
    var embeddedDescription: ByteArray? = null
) {
    constructor(
        timestamp: String,
        name: String,
        imagePath: String,
        externalUrl: String,
        description: String,
    ) : this(
        timestamp = timestamp,
        name = name,
        internalUrl = null,
        externalUrl = externalUrl,
        description = description,
        imagePath = imagePath,
        embeddedImage = null,
        embeddedDescription = null
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Photograph

        if (timestamp != other.timestamp) return false
        if (name != other.name) return false
        if (internalUrl != other.internalUrl) return false
        if (externalUrl != other.externalUrl) return false
        if (description != other.description) return false
        if (imagePath != other.imagePath) return false
        if (!embeddedImage.contentEquals(other.embeddedImage)) return false
        if (!embeddedDescription.contentEquals(other.embeddedDescription)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (internalUrl?.hashCode() ?: 0)
        result = 31 * result + (externalUrl?.hashCode() ?: 0)
        result = 31 * result + (description.hashCode())
        result = 31 * result + imagePath.hashCode()
        result = 31 * result + (embeddedImage?.contentHashCode() ?: 0)
        result = 31 * result + (embeddedDescription?.contentHashCode() ?: 0)
        return result
    }
}