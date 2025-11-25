package dev.raphaeldelio.redisapollo.question

import com.redis.om.spring.annotations.*
import com.redis.om.spring.indexing.DistanceMetric
import com.redis.om.spring.indexing.VectorType
import dev.raphaeldelio.redisapollo.utterance.Utterance
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import redis.clients.jedis.search.schemafields.VectorField.VectorAlgorithm

@RedisHash
data class Question(
    @Id
    var timestamp: String,

    var utterancesConcatenated: String,

    var utterances: List<Utterance>,

    @Vectorize(
        destination = "embeddedQuestion",
        embeddingType = EmbeddingType.SENTENCE,
        provider = EmbeddingProvider.OPENAI,
        openAiEmbeddingModel = OpenAiApi.EmbeddingModel.TEXT_EMBEDDING_3_LARGE
    )
    var question: String,

    @Indexed(
        schemaFieldType = SchemaFieldType.VECTOR,
        algorithm = VectorAlgorithm.HNSW,
        type = VectorType.FLOAT32,
        dimension = 3072,
        distanceMetric = DistanceMetric.COSINE,
        initialCapacity = 10
    )
    var embeddedQuestion: ByteArray? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Question

        if (timestamp != other.timestamp) return false
        if (utterancesConcatenated != other.utterancesConcatenated) return false
        if (utterances != other.utterances) return false
        if (question != other.question) return false
        if (!embeddedQuestion.contentEquals(other.embeddedQuestion)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + utterancesConcatenated.hashCode()
        result = 31 * result + utterances.hashCode()
        result = 31 * result + question.hashCode()
        result = 31 * result + (embeddedQuestion?.contentHashCode() ?: 0)
        return result
    }
}
