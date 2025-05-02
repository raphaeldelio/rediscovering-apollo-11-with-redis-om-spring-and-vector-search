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
        long start = System.currentTimeMillis();
        String tmpPath = imageService.saveTmpImage(request.imageBase64(), request.imagePath());
        return Map.of(
                "imagePath", tmpPath,
                "matchedPhotographs", imageService.searchByImage(tmpPath),
                "processingTime", (System.currentTimeMillis() - start) + "ms"
        );
    }

    @PostMapping("/search/by-description")
    public Map<String, Object> searchByImageText(@RequestBody SearchRequest request) {
        long start = System.currentTimeMillis();
        return Map.of(
                "query", request.query(),
                "matchedPhotographs", imageService.searchByImageText(request.query()),
                "processingTime", (System.currentTimeMillis() - start) + "ms"
        );
    }

    // ---------- DTOs ----------

    public record SearchRequest(String query, boolean enableSemanticCache, boolean enableRag) {}

    public record ImageSearchRequest(String imageBase64, String imagePath) {}
}