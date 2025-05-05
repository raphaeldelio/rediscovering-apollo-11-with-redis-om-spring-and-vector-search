package dev.raphaeldelio.redisapollo.utterance;

import com.redis.om.spring.annotations.EmbeddingType;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.SchemaFieldType;
import com.redis.om.spring.annotations.Vectorize;
import com.redis.om.spring.indexing.DistanceMetric;
import com.redis.om.spring.indexing.VectorType;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.lang.NonNull;
import redis.clients.jedis.search.schemafields.VectorField;

import java.util.Arrays;
import java.util.Objects;


@RedisHash
public class Utterance {

    @Id
    @NonNull
    private String timestamp;

    @Indexed
    private int timestampInt;

    @Indexed
    @NonNull
    private String speaker;

    @NonNull
    @Vectorize(
            destination = "embeddedText",
            embeddingType = EmbeddingType.SENTENCE
    )
    private String text;

    @Indexed(
            schemaFieldType = SchemaFieldType.VECTOR,
            algorithm = VectorField.VectorAlgorithm.HNSW,
            type = VectorType.FLOAT32,
            dimension = 384,
            distanceMetric = DistanceMetric.COSINE,
            initialCapacity = 10
    )
    private byte[] embeddedText;

    @Indexed
    @NonNull
    private String speakerId;

    public Utterance() {}

    public Utterance(@NonNull String timestamp, @NonNull String speaker, @NonNull String text, @NonNull String speakerId) {
        this.timestamp = timestamp;
        this.speaker = speaker;
        this.text = text;
        this.speakerId = speakerId;
    }

    public @NonNull String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(@NonNull String timestamp) {
        this.timestamp = timestamp;
    }

    public int getTimestampInt() {
        return timestampInt;
    }

    public void setTimestampInt(int timestampInt) {
        this.timestampInt = timestampInt;
    }

    public @NonNull String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(@NonNull String speaker) {
        this.speaker = speaker;
    }

    public @NonNull String getText() {
        return text;
    }

    public void setText(@NonNull String text) {
        this.text = text;
    }

    public byte[] getEmbeddedText() {
        return embeddedText;
    }

    public void setEmbeddedText(byte[] embeddedText) {
        this.embeddedText = embeddedText;
    }

    public @NonNull String getSpeakerId() {
        return speakerId;
    }

    public void setSpeakerId(@NonNull String speakerId) {
        this.speakerId = speakerId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Utterance utterance = (Utterance) o;
        return timestampInt == utterance.timestampInt && Objects.equals(timestamp, utterance.timestamp) && Objects.equals(speaker, utterance.speaker) && Objects.equals(text, utterance.text) && Objects.deepEquals(embeddedText, utterance.embeddedText) && Objects.equals(speakerId, utterance.speakerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, timestampInt, speaker, text, Arrays.hashCode(embeddedText), speakerId);
    }
}