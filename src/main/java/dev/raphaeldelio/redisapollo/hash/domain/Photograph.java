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

@Data
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor
@RedisHash
public class Photograph {
    @NonNull
    @Id
    private String timestamp;

    @NonNull
    private String name;

    private String internalUrl;

    private String externalUrl;

    @Vectorize(
            destination = "embeddedDescription",
            embeddingType = EmbeddingType.SENTENCE,
            provider = EmbeddingProvider.OPENAI,
            openAiEmbeddingModel = OpenAiApi.EmbeddingModel.TEXT_EMBEDDING_3_LARGE
    )
    private String description;

    @NonNull
    @Vectorize(
            destination = "embeddedImage",
            embeddingType = EmbeddingType.IMAGE
    )
    private String imagePath;

    @Indexed(
            schemaFieldType = SchemaFieldType.VECTOR,
            algorithm = VectorField.VectorAlgorithm.HNSW,
            type = VectorType.FLOAT32,
            dimension = 512,
            distanceMetric = DistanceMetric.COSINE,
            initialCapacity = 10
    )
    private byte[] embeddedImage;

    @Indexed(
            schemaFieldType = SchemaFieldType.VECTOR,
            algorithm = VectorField.VectorAlgorithm.HNSW,
            type = VectorType.FLOAT32,
            dimension = 3072,
            distanceMetric = DistanceMetric.COSINE,
            initialCapacity = 10
    )
    private byte[] embeddedDescription;

    public Photograph(@NonNull String timestamp, @NonNull String name, String internalUrl, String externalUrl, String description) {
        this.timestamp = timestamp;
        this.name = name;
        this.internalUrl = internalUrl;
        this.externalUrl = externalUrl;
        this.description = description;
    }
}