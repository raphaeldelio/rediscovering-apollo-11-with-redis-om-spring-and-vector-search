package dev.raphaeldelio.redisapollo.utterance;

import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.SearchStream;
import com.redis.om.spring.tuple.Fields;
import com.redis.om.spring.tuple.Pair;
import com.redis.om.spring.vectorize.Embedder;
import dev.raphaeldelio.redisapollo.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UtteranceService {

    private static final Logger logger = LoggerFactory.getLogger(UtteranceService.class);

    private final UtteranceRepository utteranceRepository;
    private final FileService fileService;
    private final Embedder embedder;
    private final EntityStream entityStream;

    public UtteranceService(UtteranceRepository utteranceRepository, FileService fileService, Embedder embedder, EntityStream entityStream) {
        this.utteranceRepository = utteranceRepository;
        this.fileService = fileService;
        this.embedder = embedder;
        this.entityStream = entityStream;
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

    public byte[] embedUtterance(String text) {
        logger.info("Creating utterance embedding for text: {}", text);
        return embedder.getTextEmbeddingsAsBytes(List.of(text), Utterance$.TEXT).getFirst();
    }

    public List<UtteranceSearchResult> search(byte[] embedding) {
        logger.info("Received utterance: {}", embedding);
        SearchStream<Utterance> stream = entityStream.of(Utterance.class);
        List<Pair<Utterance, Double>> textsAndScores = stream
                .filter(Utterance$.EMBEDDED_TEXT.knn(3, embedding))
                .sorted(Utterance$._EMBEDDED_TEXT_SCORE)
                .map(Fields.of(Utterance$._THIS, Utterance$._EMBEDDED_TEXT_SCORE))
                .collect(Collectors.toList());

        return textsAndScores.stream()
                .map(pair -> new UtteranceSearchResult(pair.getFirst().getText(), pair.getSecond()))
                .toList();
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
