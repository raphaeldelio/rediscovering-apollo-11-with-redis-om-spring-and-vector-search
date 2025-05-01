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

    public UtteranceService(UtteranceRepository utteranceRepository, FileService fileService, RedisService redisService) {
        this.utteranceRepository = utteranceRepository;
        this.fileService = fileService;
    }

    public void loadUtteranceData(String filePath) {
        logger.info("Loading utterance data from file: {}", filePath);
        fileService.readAndProcessFile(filePath, Utterance.class, data -> {
            for (Utterance utterance : data) {
                if (!isValidUtterance(utterance)) {
                    continue;
                }
                int utteranceSeconds = fileService.toHMSToSeconds(utterance.getTimestamp());
                utterance.setTimestampInt(utteranceSeconds);
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
        if ("...".equals(utterance.getText())) return false;
        return true;
    }

}
