package dev.raphaeldelio.redisapollo.question;

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
public class Question {

    @NonNull
    @Id
    private String timestamp;

    @NonNull
    private String utterancesConcatenated;

    @NonNull
    private List<Utterance> utterances;

    @NonNull
    @Vectorize(
            destination = "embeddedQuestion",
            embeddingType = EmbeddingType.SENTENCE,
            provider = EmbeddingProvider.OPENAI,
            openAiEmbeddingModel = OpenAiApi.EmbeddingModel.TEXT_EMBEDDING_3_LARGE
    )
    private String question;

    @Indexed(
            schemaFieldType = SchemaFieldType.VECTOR,
            algorithm = VectorField.VectorAlgorithm.HNSW,
            type = VectorType.FLOAT32,
            dimension = 3072,
            distanceMetric = DistanceMetric.COSINE,
            initialCapacity = 10
    )
    private byte[] embeddedQuestion;

    public Question(@NonNull String timestamp, @NonNull String utterancesConcatenated, @NonNull List<Utterance> utterances, @NonNull String question) {
        this.timestamp = timestamp;
        this.utterancesConcatenated = utterancesConcatenated;
        this.utterances = utterances;
        this.question = question;
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

    public @NonNull String getQuestion() {
        return question;
    }

    public void setQuestion(@NonNull String question) {
        this.question = question;
    }

    public byte[] getEmbeddedQuestion() {
        return embeddedQuestion;
    }

    public void setEmbeddedQuestion(byte[] embeddedQuestion) {
        this.embeddedQuestion = embeddedQuestion;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Question question1 = (Question) o;
        return Objects.equals(timestamp, question1.timestamp) && Objects.equals(utterancesConcatenated, question1.utterancesConcatenated) && Objects.equals(utterances, question1.utterances) && Objects.equals(question, question1.question) && Objects.deepEquals(embeddedQuestion, question1.embeddedQuestion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, utterancesConcatenated, utterances, question, Arrays.hashCode(embeddedQuestion));
    }
}