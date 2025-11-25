package dev.raphaeldelio.redisapollo.utterance

import com.redis.om.spring.annotations.*
import com.redis.om.spring.indexing.DistanceMetric
import com.redis.om.spring.indexing.VectorType
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.lang.NonNull
import redis.clients.jedis.search.schemafields.VectorField

@RedisHash
data class Utterance(
    @Id
    @NonNull
    var timestamp: String,

    @Indexed
    var timestampInt: Int = 0,

    @Indexed
    @NonNull
    var speaker: String,

    @NonNull
    @Vectorize(
        destination = "embeddedText",
        embeddingType = EmbeddingType.SENTENCE
    )
    var text: String,

    @Indexed(
        schemaFieldType = SchemaFieldType.VECTOR,
        algorithm = VectorField.VectorAlgorithm.HNSW,
        type = VectorType.FLOAT32,
        dimension = 384,
        distanceMetric = DistanceMetric.COSINE,
        initialCapacity = 10
    )
    var embeddedText: ByteArray? = null,

    @Indexed
    @NonNull
    var speakerId: String = ""
) {

    constructor(
        timestamp: String,
        speaker: String,
        text: String,
        speakerId: String
    ) : this(
        timestamp = timestamp,
        timestampInt = 0,
        speaker = speaker,
        text = text,
        embeddedText = null,
        speakerId = speakerId
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Utterance

        if (timestampInt != other.timestampInt) return false
        if (timestamp != other.timestamp) return false
        if (speaker != other.speaker) return false
        if (text != other.text) return false
        if (!embeddedText.contentEquals(other.embeddedText)) return false
        if (speakerId != other.speakerId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestampInt
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + speaker.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + (embeddedText?.contentHashCode() ?: 0)
        result = 31 * result + speakerId.hashCode()
        return result
    }
}