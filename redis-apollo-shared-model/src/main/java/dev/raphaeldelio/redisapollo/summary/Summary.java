package dev.raphaeldelio.redisapollo.summary;

import com.redis.om.spring.annotations.*;
import com.redis.om.spring.indexing.DistanceMetric;
import com.redis.om.spring.indexing.VectorType;
import dev.raphaeldelio.redisapollo.utterance.Utterance;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.lang.NonNull;
import redis.clients.jedis.search.schemafields.VectorField;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RedisHash
public class Summary {

    @NonNull
    @Id
    private String timestamp;

    @NonNull
    private String utterancesConcatenated;

    @NonNull
    private List<Utterance> utterances;

    @NonNull
    @Vectorize(
            destination = "embeddedSummary",
            embeddingType = EmbeddingType.SENTENCE,
            provider = EmbeddingProvider.OPENAI,
            openAiEmbeddingModel = OpenAiApi.EmbeddingModel.TEXT_EMBEDDING_3_LARGE
    )
    private String summary;

    @Indexed(
            schemaFieldType = SchemaFieldType.VECTOR,
            algorithm = VectorField.VectorAlgorithm.HNSW,
            type = VectorType.FLOAT32,
            dimension = 3072,
            distanceMetric = DistanceMetric.COSINE,
            initialCapacity = 10
    )
    private byte[] embeddedSummary;

    public Summary(@NonNull String timestamp, @NonNull String utterancesConcatenated, @NonNull List<Utterance> utterances, @NonNull String summary) {
        this.timestamp = timestamp;
        this.utterancesConcatenated = utterancesConcatenated;
        this.utterances = utterances;
        this.summary = summary;
    }

    public @NonNull String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(@NonNull String timestamp) {
        this.timestamp = timestamp;
    }

    public @NonNull String getUtterancesConcatenated() {
        return utterancesConcatenated;
    }

    public void setUtterancesConcatenated(@NonNull String utterancesConcatenated) {
        this.utterancesConcatenated = utterancesConcatenated;
    }

    public @NonNull List<Utterance> getUtterances() {
        return utterances;
    }

    public void setUtterances(@NonNull List<Utterance> utterances) {
        this.utterances = utterances;
    }

    public @NonNull String getSummary() {
        return summary;
    }

    public void setSummary(@NonNull String summary) {
        this.summary = summary;
    }

    public byte[] getEmbeddedSummary() {
        return embeddedSummary;
    }

    public void setEmbeddedSummary(byte[] embeddedSummary) {
        this.embeddedSummary = embeddedSummary;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Summary summary1 = (Summary) o;
        return Objects.equals(timestamp, summary1.timestamp) && Objects.equals(utterancesConcatenated, summary1.utterancesConcatenated) && Objects.equals(utterances, summary1.utterances) && Objects.equals(summary, summary1.summary) && Objects.deepEquals(embeddedSummary, summary1.embeddedSummary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, utterancesConcatenated, utterances, summary, Arrays.hashCode(embeddedSummary));
    }
}