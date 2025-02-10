package dev.raphaeldelio.redisapollo.service;

import dev.raphaeldelio.redisapollo.document.domain.TOCData;
import dev.raphaeldelio.redisapollo.document.repository.TOCDataRepository;
import dev.raphaeldelio.redisapollo.hash.domain.UtteranceSummaries;
import dev.raphaeldelio.redisapollo.hash.repository.UtteranceQuestionsRepository;
import dev.raphaeldelio.redisapollo.hash.repository.UtteranceSummariesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtteranceSummariesService {
    private static final Logger logger = LoggerFactory.getLogger(UtteranceSummariesService.class);
    private final TOCDataRepository tocDataRepository;
    private final UtteranceSummariesRepository utteranceSummariesRepository;

    public UtteranceSummariesService(UtteranceQuestionsRepository utteranceQuestionsRepository, TOCDataRepository tocDataRepository, UtteranceSummariesRepository utteranceSummariesRepository) {
        this.tocDataRepository = tocDataRepository;
        this.utteranceSummariesRepository = utteranceSummariesRepository;
    }

    public void embedSummaries() {
        embedSummaries(false);
    }

    public void embedSummaries(boolean overwrite) {
        logger.info("Creating utterance summaries");

        // Retrieve all TOC entries and utterances in chronological order
        List<TOCData> tocDataList = tocDataRepository.findAll();

        // Iterate through each TOC entry
        for (TOCData currentTOC : tocDataList) {
            boolean areUtterancesAndQuestionsPresent = currentTOC.getConcatenatedUtterances() != null && currentTOC.getSummary() != null;
            if (areUtterancesAndQuestionsPresent) {
                var id = currentTOC.getStartDate();
                if (utteranceSummariesRepository.findById(id).isEmpty() || overwrite) {
                    var grouped = UtteranceSummaries.of(
                            id,
                            currentTOC.getConcatenatedUtterances(),
                            currentTOC.getUtterances(),
                            currentTOC.getSummary()
                    );
                    utteranceSummariesRepository.save(grouped);
                    logger.info("Utterance summary saved: {}", id);
                }
            }
        }
        logger.info("Grouping complete");
    }
}
