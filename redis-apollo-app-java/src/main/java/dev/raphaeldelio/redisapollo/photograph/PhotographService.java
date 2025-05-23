package dev.raphaeldelio.redisapollo.photograph;

import dev.raphaeldelio.redisapollo.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PhotographService {

    private static final Logger logger = LoggerFactory.getLogger(PhotographService.class);

    private final PhotographsRepository photographsRepository;
    private final FileService fileService;

    public PhotographService(PhotographsRepository photographsRepository, FileService fileService) {
        this.photographsRepository = photographsRepository;
        this.fileService = fileService;
    }

    public void loadPhotographData(String filePath) {
        loadPhotographData(filePath, false);
    }

    public void loadPhotographData(String filePath, boolean overwrite) {
        logger.info("Loading photograph data from file: {}", filePath);
        List<Photograph> toBeSaved = new ArrayList<>();
        fileService.readAndProcessFile(filePath, Photograph.class, data -> {
            for (Photograph photograph : data) {
                if (!overwrite) {
                    if (photographsRepository.existsById(photograph.getTimestamp())) {
                        logger.info("Photograph with timestamp {} already exists, skipping", photograph.getTimestamp());
                        continue;
                    }
                }

                String imagePath = "classpath:static/images/apollo11/" + Integer.parseInt(photograph.getTimestamp()) + ".jpg";
                photograph.setImagePath(imagePath);
                toBeSaved.add(photograph);
            }
        });
        photographsRepository.saveAll(toBeSaved);
        logger.info("Photograph data loaded successfully");
    }
}
