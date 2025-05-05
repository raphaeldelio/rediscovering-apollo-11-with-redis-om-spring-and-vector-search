package dev.raphaeldelio.redisapollo.photograph;

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

    public Photograph() {}

    public Photograph(@NonNull String timestamp, @NonNull String name, @NonNull String imagePath) {
        this.timestamp = timestamp;
        this.name = name;
        this.imagePath = imagePath;
    }

    public Photograph(@NonNull String timestamp, @NonNull String name, String internalUrl, String externalUrl, String description) {
        this.timestamp = timestamp;
        this.name = name;
        this.internalUrl = internalUrl;
        this.externalUrl = externalUrl;
        this.description = description;
    }

    public @NonNull String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(@NonNull String timestamp) {
        this.timestamp = timestamp;
    }

    public @NonNull String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public String getInternalUrl() {
        return internalUrl;
    }

    public void setInternalUrl(String internalUrl) {
        this.internalUrl = internalUrl;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public @NonNull String getImagePath() {
        return imagePath;
    }

    public void setImagePath(@NonNull String imagePath) {
        this.imagePath = imagePath;
    }

    public byte[] getEmbeddedImage() {
        return embeddedImage;
    }

    public void setEmbeddedImage(byte[] embeddedImage) {
        this.embeddedImage = embeddedImage;
    }

    public byte[] getEmbeddedDescription() {
        return embeddedDescription;
    }

    public void setEmbeddedDescription(byte[] embeddedDescription) {
        this.embeddedDescription = embeddedDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Photograph that = (Photograph) o;
        return Objects.equals(timestamp, that.timestamp) && Objects.equals(name, that.name) && Objects.equals(internalUrl, that.internalUrl) && Objects.equals(externalUrl, that.externalUrl) && Objects.equals(description, that.description) && Objects.equals(imagePath, that.imagePath) && Objects.deepEquals(embeddedImage, that.embeddedImage) && Objects.deepEquals(embeddedDescription, that.embeddedDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, name, internalUrl, externalUrl, description, imagePath, Arrays.hashCode(embeddedImage), Arrays.hashCode(embeddedDescription));
    }
}