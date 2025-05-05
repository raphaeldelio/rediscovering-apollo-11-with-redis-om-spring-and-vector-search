package dev.raphaeldelio.redisapollo.photograph;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/image")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/search/by-image")
    public Map<String, Object> searchByImage(@RequestBody ImageSearchRequest request) {
        String tmpPath = imageService.saveTmpImage(request.imageBase64(), request.imagePath());

        long start = System.currentTimeMillis();
        Photograph tmpPhoto = imageService.embedImage(tmpPath);
        long embeddingTime = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        var matchedPhotographs = imageService.searchByImage(tmpPhoto.getEmbeddedImage());
        long searchTime = System.currentTimeMillis() - start;

        return Map.of(
                "imagePath", tmpPath,
                "matchedPhotographs", matchedPhotographs,
                "embeddingTime", embeddingTime + "ms",
                "searchTime", searchTime + "ms"
        );
    }

    @PostMapping("/search/by-description")
    public Map<String, Object> searchByImageText(@RequestBody SearchRequest request) {
        long start = System.currentTimeMillis();
        byte[] embedding = imageService.embedDescription(request.query());
        long embeddingTime = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        var matchedPhotographs = imageService.searchByImageText(embedding);
        long searchTime = System.currentTimeMillis() - start;

        return Map.of(
                "query", request.query(),
                "matchedPhotographs", matchedPhotographs,
                "embeddingTime", embeddingTime + "ms",
                "searchTime", searchTime + "ms"
        );
    }

    // ---------- DTOs ----------

    public record SearchRequest(String query, boolean enableSemanticCache, boolean enableRag) {}

    public record ImageSearchRequest(String imageBase64, String imagePath) {}
}