package dev.raphaeldelio.redisapollo.summary;

import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.SearchStream;
import com.redis.om.spring.tuple.Fields;
import com.redis.om.spring.tuple.Pair;
import com.redis.om.spring.vectorize.Embedder;
import dev.raphaeldelio.redisapollo.tableofcontents.TOCData;
import dev.raphaeldelio.redisapollo.tableofcontents.TOCDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SummaryService {
    private static final Logger logger = LoggerFactory.getLogger(SummaryService.class);
    private final TOCDataRepository tocDataRepository;
    private final SummaryRepository summaryRepository;
    private final Embedder embedder;
    private final EntityStream entityStream;

    public SummaryService(TOCDataRepository tocDataRepository, SummaryRepository summaryRepository, Embedder embedder, EntityStream entityStream) {
        this.tocDataRepository = tocDataRepository;
        this.summaryRepository = summaryRepository;
        this.embedder = embedder;
        this.entityStream = entityStream;
    }

    public void embedSummaries() {
        embedSummaries(false);
    }

    public void embedSummaries(boolean overwrite) {
        logger.info("Creating utterance summaries");

        // Retrieve all TOC entries and utterances in chronological order
        List<TOCData> tocDataList = tocDataRepository.findAll();

        List<Summary> toSaveList = new ArrayList<>();
        // Iterate through each TOC entry
        for (TOCData currentTOC : tocDataList) {
            boolean areUtterancesAndQuestionsPresent = currentTOC.getConcatenatedUtterances() != null && currentTOC.getSummary() != null;
            if (areUtterancesAndQuestionsPresent) {
                var id = currentTOC.getStartDate();
                if (summaryRepository.findById(id).isEmpty() || overwrite) {
                    var summary = Summary.of(
                            id,
                            currentTOC.getConcatenatedUtterances(),
                            currentTOC.getUtterances(),
                            currentTOC.getSummary()
                    );
                    toSaveList.add(summary);
                }
            }
        }
        summaryRepository.saveAll(toSaveList);
        logger.info("Utterance summaries embedded");
    }

    public byte[] embedQuery(String query) {
        logger.info("Received question: {}", query);
        return embedder.getTextEmbeddingsAsBytes(List.of(query), Summary$.SUMMARY).getFirst();
    }

    public List<SummarySearchResult> searchBySummary(byte[] embedding) {
        SearchStream<Summary> stream = entityStream.of(Summary.class);
        List<Pair<Summary, Double>> summaries = stream.filter(Summary$.EMBEDDED_SUMMARY.knn(3, embedding))
                .sorted(Summary$._EMBEDDED_SUMMARY_SCORE)
                .map(Fields.of(Summary$._THIS, Summary$._EMBEDDED_SUMMARY_SCORE))
                .collect(Collectors.toList());

        return summaries.stream()
                .map(pair -> new SummarySearchResult(
                        pair.getFirst(),
                        pair.getSecond()))
                .toList();
    }
}
