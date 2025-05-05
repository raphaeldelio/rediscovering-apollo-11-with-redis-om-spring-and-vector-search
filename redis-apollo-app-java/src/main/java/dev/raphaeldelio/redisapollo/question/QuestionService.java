package dev.raphaeldelio.redisapollo.question;

import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.SearchStream;
import com.redis.om.spring.tuple.Fields;
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
public class QuestionService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);
    private final QuestionRepository questionRepository;
    private final TOCDataRepository tocDataRepository;
    private final Embedder embedder;
    private final EntityStream entityStream;

    public QuestionService(QuestionRepository questionRepository, TOCDataRepository tocDataRepository, Embedder embedder, EntityStream entityStream) {
        this.questionRepository = questionRepository;
        this.tocDataRepository = tocDataRepository;
        this.embedder = embedder;
        this.entityStream = entityStream;
    }

    public void embedQuestions() {
        embedQuestions(false);
    }

    public void embedQuestions(boolean overwrite) {
        logger.info("Creating question embeddings");

        // Retrieve all TOC entries and utterances in chronological order
        List<TOCData> tocDataList = tocDataRepository.findAll();

        // Iterate through each TOC entry
        List<Question> toSaveList = new ArrayList<>();
        for (TOCData currentTOC : tocDataList) {
            boolean areUtterancesAndQuestionsPresent = currentTOC.getConcatenatedUtterances() != null && currentTOC.getQuestions() != null;
            if (areUtterancesAndQuestionsPresent) {
                var count = 0;
                for (String question : currentTOC.getQuestions()) {
                    var id = currentTOC.getStartDate() + "-" + count++;
                    if (questionRepository.findById(id).isEmpty() || overwrite) {
                        var utteranceQuestion = new Question(
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
        questionRepository.saveAll(toSaveList);
        logger.info("Embedding questions created successfully");
    }

    public byte[] embedQuery(String query) {
        logger.info("Received question: {}", query);
        return embedder.getTextEmbeddingsAsBytes(List.of(query), Question$.QUESTION).getFirst();
    }

    public List<QuestionSearchResult> searchByQuestion(byte[] embedding) {
        SearchStream<Question> stream = entityStream.of(Question.class);
        return stream.filter(Question$.EMBEDDED_QUESTION.knn(3, embedding))
                .sorted(Question$._EMBEDDED_QUESTION_SCORE)
                .map(Fields.of(Question$._THIS, Question$._EMBEDDED_QUESTION_SCORE))
                .collect(Collectors.toList())
                .stream().map(pair -> new QuestionSearchResult(
                        pair.getFirst(),
                        pair.getSecond())
                ).toList();
    }
}
