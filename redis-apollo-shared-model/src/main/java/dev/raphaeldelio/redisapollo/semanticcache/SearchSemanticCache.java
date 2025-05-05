package dev.raphaeldelio.redisapollo.semanticcache;

import com.redis.om.spring.annotations.*;
import com.redis.om.spring.indexing.DistanceMetric;
import com.redis.om.spring.indexing.VectorType;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.lang.NonNull;
import redis.clients.jedis.search.schemafields.VectorField;

import java.util.Arrays;
import java.util.Objects;

@RedisHash
public class SearchSemanticCache {
    @Id
    private String id;

    @NonNull
    @Vectorize(
            destination = "embeddedQuery",
            embeddingType = EmbeddingType.SENTENCE,
            provider = EmbeddingProvider.OPENAI,
            openAiEmbeddingModel = OpenAiApi.EmbeddingModel.TEXT_EMBEDDING_3_LARGE
    )
    private String query;

    @Indexed(
            schemaFieldType = SchemaFieldType.VECTOR,
            algorithm = VectorField.VectorAlgorithm.HNSW,
            type = VectorType.FLOAT32,
            dimension = 3072,
            distanceMetric = DistanceMetric.COSINE,
            initialCapacity = 10
    )
    private byte[] embeddedQuery;

    private String answer;

    @Indexed
    private boolean question;

    public SearchSemanticCache(@NonNull String query, String answer, boolean question) {
        this.query = query;
        this.answer = answer;
        this.question = question;
    }

    public @NonNull String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public @NonNull String getQuery() {
        return query;
    }

    public void setQuery(@NonNull String query) {
        this.query = query;
    }

    public byte[] getEmbeddedQuery() {
        return embeddedQuery;
    }

    public void setEmbeddedQuery(byte[] embeddedQuery) {
        this.embeddedQuery = embeddedQuery;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isQuestion() {
        return question;
    }

    public void setQuestion(boolean question) {
        this.question = question;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SearchSemanticCache that = (SearchSemanticCache) o;
        return question == that.question && Objects.equals(id, that.id) && Objects.equals(query, that.query) && Objects.deepEquals(embeddedQuery, that.embeddedQuery) && Objects.equals(answer, that.answer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, query, Arrays.hashCode(embeddedQuery), answer, question);
    }
}
