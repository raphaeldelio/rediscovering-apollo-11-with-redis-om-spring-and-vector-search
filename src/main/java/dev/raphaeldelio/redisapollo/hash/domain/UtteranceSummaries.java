package dev.raphaeldelio.redisapollo.hash.domain;

import com.redis.om.spring.annotations.*;
import com.redis.om.spring.indexing.DistanceMetric;
import com.redis.om.spring.indexing.VectorType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import redis.clients.jedis.search.schemafields.VectorField;

import java.util.List;

@Data
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor
@RedisHash
public class UtteranceSummaries {

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
}