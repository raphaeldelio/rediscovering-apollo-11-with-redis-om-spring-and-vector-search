package dev.raphaeldelio.redisapollo.photograph;

import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.SearchStream;
import com.redis.om.spring.tuple.Fields;
import com.redis.om.spring.tuple.Pair;
import com.redis.om.spring.vectorize.Embedder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    private final EntityStream entityStream;
    private final Embedder embedder;
    private final PhotographsRepository photographsRepository;

    public ImageService(EntityStream entityStream, Embedder embedder, PhotographsRepository photographsRepository) {
        this.entityStream = entityStream;
        this.embedder = embedder;
        this.photographsRepository = photographsRepository;
    }

    public String saveTmpImage(String imageBase64, String imagePath) {
        if (imageBase64 != null) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(imageBase64);
                Path tempFile = Files.createTempFile("uploaded-image-", ".jpg");
                Files.write(tempFile, decodedBytes);
                String savedImagePath = tempFile.toUri().toString();
                logger.info("Received Base64 image, saved to: {}", savedImagePath);
                return savedImagePath;
            } catch (IOException e) {
                throw new IllegalArgumentException("Error saving uploaded image", e);
            }
        } else if (imagePath != null) {
            logger.info("Received image path: {}", imagePath);
            return imagePath;
        } else {
            throw new IllegalArgumentException("No image provided");
        }
    }

    public List<ImageSearchResult> searchByImage(String tmpImagePath) {
        Photograph tmpPhoto = new Photograph("tmp", "tmp", null, null, null);
        tmpPhoto.setImagePath(tmpImagePath);
        tmpPhoto = photographsRepository.save(tmpPhoto);

        SearchStream<Photograph> stream = entityStream.of(Photograph.class);
        List<Pair<Photograph, Double>> photographsAndScores = stream
                .filter(Photograph$.EMBEDDED_IMAGE.knn(20, tmpPhoto.getEmbeddedImage()))
                .sorted(Photograph$._EMBEDDED_IMAGE_SCORE)
                .map(Fields.of(Photograph$._THIS, Photograph$._EMBEDDED_IMAGE_SCORE))
                .collect(Collectors.toList());

        return photographsAndScores.stream()
                .filter(pair -> !pair.getFirst().getName().equals("tmp"))
                .map(pair -> new ImageSearchResult(
                        pair.getFirst().getImagePath().replace("classpath:static", ""),
                        pair.getFirst().getDescription(),
                        pair.getSecond()))
                .toList();
    }

    public List<ImageSearchResult> searchByImageText(String query) {
        logger.info("Received query: {}", query);
        byte[] embedding = embedder.getTextEmbeddingsAsBytes(List.of(query), Photograph$.DESCRIPTION).getFirst();
        SearchStream<Photograph> stream = entityStream.of(Photograph.class);
        List<Pair<Photograph, Double>> photographsAndScores = stream
                .filter(Photograph$.EMBEDDED_DESCRIPTION.knn(3, embedding))
                .sorted(Photograph$._EMBEDDED_DESCRIPTION_SCORE)
                .map(Fields.of(Photograph$._THIS, Photograph$._EMBEDDED_DESCRIPTION_SCORE))
                .collect(Collectors.toList());

        return photographsAndScores.stream()
                .map(pair -> new ImageSearchResult(
                        pair.getFirst().getImagePath().replace("classpath:static", ""),
                        pair.getFirst().getDescription(),
                        pair.getSecond()))
                .toList();
    }
}