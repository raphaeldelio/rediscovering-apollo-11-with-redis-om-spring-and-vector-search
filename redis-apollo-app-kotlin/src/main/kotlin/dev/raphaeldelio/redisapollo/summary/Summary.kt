package dev.raphaeldelio.redisapollo.summary

import com.redis.om.spring.annotations.*
import com.redis.om.spring.indexing.DistanceMetric
import com.redis.om.spring.indexing.VectorType
import dev.raphaeldelio.redisapollo.utterance.Utterance
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.lang.NonNull
import redis.clients.jedis.search.schemafields.VectorField

@RedisHash
data class Summary(
    @Id
    @NonNull
    var timestamp: String = "",

    @NonNull
    var utterancesConcatenated: String = "",

    @NonNull
    var utterances: List<Utterance> = emptyList(),

    @NonNull
    @Vectorize(
        destination = "embeddedSummary",
        embeddingType = EmbeddingType.SENTENCE,
        provider = EmbeddingProvider.OPENAI,
        openAiEmbeddingModel = OpenAiApi.EmbeddingModel.TEXT_EMBEDDING_3_LARGE
    )
    var summary: String = "",

    @Indexed(
        schemaFieldType = SchemaFieldType.VECTOR,
        algorithm = VectorField.VectorAlgorithm.HNSW,
        type = VectorType.FLOAT32,
        dimension = 3072,
        distanceMetric = DistanceMetric.COSINE,
        initialCapacity = 10
    )
    var embeddedSummary: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Summary

        if (timestamp != other.timestamp) return false
        if (utterancesConcatenated != other.utterancesConcatenated) return false
        if (utterances != other.utterances) return false
        if (summary != other.summary) return false
        if (!embeddedSummary.contentEquals(other.embeddedSummary)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + utterancesConcatenated.hashCode()
        result = 31 * result + utterances.hashCode()
        result = 31 * result + summary.hashCode()
        result = 31 * result + (embeddedSummary?.contentHashCode() ?: 0)
        return result
    }
}