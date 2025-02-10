package dev.raphaeldelio.redisapollo.hash.domain;

import com.redis.om.spring.annotations.*;
import com.redis.om.spring.indexing.DistanceMetric;
import com.redis.om.spring.indexing.VectorType;
import lombok.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import redis.clients.jedis.search.schemafields.VectorField;

@Data
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor
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
            dimension = 768,
            distanceMetric = DistanceMetric.COSINE,
            initialCapacity = 10
    )
    private byte[] embeddedText;

    @Indexed
    @NonNull
    private String speakerId;
}