package dev.raphaeldelio.redisapollo.service;

import dev.raphaeldelio.redisapollo.hash.domain.Utterance;
import dev.raphaeldelio.redisapollo.hash.repository.UtteranceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UtteranceService {

    private static final Logger logger = LoggerFactory.getLogger(UtteranceService.class);

    private final UtteranceRepository utteranceRepository;
    private final FileService fileService;
    private final RedisService redisService;

    public UtteranceService(UtteranceRepository utteranceRepository, FileService fileService, RedisService redisService) {
        this.utteranceRepository = utteranceRepository;
        this.fileService = fileService;
        this.redisService = redisService;
    }

    public void loadUtteranceData(String filePath) {
        logger.info("Loading utterance data from file: {}", filePath);
        //redisService.initializeCountMinSketch("wordCount", 1000, 5);
        fileService.readAndProcessFile(filePath, Utterance.class, data -> {
            for (Utterance utterance : data) {
                if (!isValidUtterance(utterance)) {
                    continue;
                }
                int utteranceSeconds = fileService.toHMSToSeconds(utterance.getTimestamp());
                utterance.setTimestampInt(utteranceSeconds);

                redisService.incrementZSet("utterances", utterance.getText());
                utteranceRepository.save(utterance);
            }
        });
        logger.info("Utterance data loaded successfully");
    }

    private boolean isValidUtterance(Utterance utterance) {
        if (utterance.getSpeaker() == null) return false;
        if (utterance.getSpeakerId() == null) return false;
        if (utterance.getText() == null) return false;
        if (utterance.getSpeaker().isBlank()) return false;
        //if ("SC".equals(utterance.getSpeaker())) return false;
        //if ("P".equals(utterance.getSpeakerId())) return false;
        if ("...".equals(utterance.getText())) return false;
        return true;
    }

}
