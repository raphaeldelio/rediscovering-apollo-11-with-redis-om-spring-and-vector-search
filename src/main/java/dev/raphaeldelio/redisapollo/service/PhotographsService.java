package dev.raphaeldelio.redisapollo.service;

import dev.raphaeldelio.redisapollo.hash.domain.Photograph;
import dev.raphaeldelio.redisapollo.hash.repository.PhotographsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PhotographsService {

    private static final Logger logger = LoggerFactory.getLogger(PhotographsService.class);

    private final PhotographsRepository photographsRepository;
    private final FileService fileService;

    public PhotographsService(PhotographsRepository photographsRepository, FileService fileService) {
        this.photographsRepository = photographsRepository;
        this.fileService = fileService;
    }

    public void loadPhotographData(String filePath) {
        loadPhotographData(filePath, false);
    }

    public void loadPhotographData(String filePath, boolean overwrite) {
        logger.info("Loading photograph data from file: {}", filePath);
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
                photographsRepository.save(photograph);
            }
        });
        logger.info("Photograph data loaded successfully");
    }
}
