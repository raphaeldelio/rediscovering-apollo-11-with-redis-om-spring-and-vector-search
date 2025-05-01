package dev.raphaeldelio.redisapollo.service;

import dev.raphaeldelio.redisapollo.document.domain.TOCData;
import dev.raphaeldelio.redisapollo.hash.domain.UtteranceQuestions;
import dev.raphaeldelio.redisapollo.document.repository.TOCDataRepository;
import dev.raphaeldelio.redisapollo.hash.repository.UtteranceQuestionsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UtteranceQuestionsService {
    private static final Logger logger = LoggerFactory.getLogger(UtteranceQuestionsService.class);
    private final UtteranceQuestionsRepository utteranceQuestionsRepository;
    private final TOCDataRepository tocDataRepository;

    public UtteranceQuestionsService(UtteranceQuestionsRepository utteranceQuestionsRepository, TOCDataRepository tocDataRepository) {
        this.utteranceQuestionsRepository = utteranceQuestionsRepository;
        this.tocDataRepository = tocDataRepository;
    }

    public void embedQuestions() {
        embedQuestions(false);
    }

    public void embedQuestions(boolean overwrite) {
        logger.info("Creating utterance questions");

        // Retrieve all TOC entries and utterances in chronological order
        List<TOCData> tocDataList = tocDataRepository.findAll();

        // Iterate through each TOC entry
        List<UtteranceQuestions> toSaveList = new ArrayList<>();
        for (TOCData currentTOC : tocDataList) {
            boolean areUtterancesAndQuestionsPresent = currentTOC.getConcatenatedUtterances() != null && currentTOC.getQuestions() != null;
            if (areUtterancesAndQuestionsPresent) {
                var count = 0;
                for (String question : currentTOC.getQuestions()) {
                    var id = currentTOC.getStartDate() + "-" + count++;
                    if (utteranceQuestionsRepository.findById(id).isEmpty() || overwrite) {
                        var utteranceQuestion = UtteranceQuestions.of(
                                id,
                                currentTOC.getConcatenatedUtterances(),
                                currentTOC.getUtterances(),
                                question
                        );
                        toSaveList.add(utteranceQuestion);
                    }
                }
            }
        }
        utteranceQuestionsRepository.saveAll(toSaveList);
        logger.info("Utterance questions created successfully");
    }
}
