package dev.raphaeldelio.redisapollo.semanticcache

import com.redis.om.spring.annotations.*
import com.redis.om.spring.indexing.DistanceMetric
import com.redis.om.spring.indexing.VectorType
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.lang.NonNull
import redis.clients.jedis.search.schemafields.VectorField

@RedisHash
data class SearchSemanticCache(
    @Id
    var id: String? = null,

    @NonNull
    @Vectorize(
        destination = "embeddedQuery",
        embeddingType = EmbeddingType.SENTENCE,
        provider = EmbeddingProvider.OPENAI,
        openAiEmbeddingModel = OpenAiApi.EmbeddingModel.TEXT_EMBEDDING_3_LARGE
    )
    var query: String = "",

    @Indexed(
        schemaFieldType = SchemaFieldType.VECTOR,
        algorithm = VectorField.VectorAlgorithm.HNSW,
        type = VectorType.FLOAT32,
        dimension = 3072,
        distanceMetric = DistanceMetric.COSINE,
        initialCapacity = 10
    )
    var embeddedQuery: ByteArray? = null,

    var answer: String = "",

    @Indexed
    var question: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchSemanticCache

        if (question != other.question) return false
        if (id != other.id) return false
        if (query != other.query) return false
        if (!embeddedQuery.contentEquals(other.embeddedQuery)) return false
        if (answer != other.answer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = question.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + query.hashCode()
        result = 31 * result + (embeddedQuery?.contentHashCode() ?: 0)
        result = 31 * result + (answer.hashCode())
        return result
    }
}