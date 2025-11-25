package dev.raphaeldelio.redisapollo.question

import com.redis.om.spring.search.stream.EntityStream
import com.redis.om.spring.search.stream.SearchStream
import com.redis.om.spring.tuple.Fields
import com.redis.om.spring.vectorize.Embedder
import com.redis.vl.extensions.cache.CacheHit
import com.redis.vl.extensions.cache.SemanticCache
import dev.raphaeldelio.redisapollo.tableofcontents.TOCDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional
import java.util.stream.Collectors

@Service
class QuestionService(
    private val questionRepository: QuestionRepository,
    private val tocDataRepository: TOCDataRepository,
    private val embedder: Embedder,
    private val entityStream: EntityStream,
    private val questionsSemanticCache: SemanticCache,
) {
    private val logger = LoggerFactory.getLogger(QuestionService::class.java)

    fun embedQuestions(overwrite: Boolean = false) {
        logger.info("Creating question embeddings")

        val tocDataList = tocDataRepository.findAll()
        val toSaveList = mutableListOf<Question>()

        tocDataList.forEach { toc ->
            val hasData = toc.concatenatedUtterances != null && toc.questions != null
            if (hasData) {
                toc.questions!!.forEachIndexed { index, questionText ->
                    val id = "${toc.startDate}-$index"
                    if (questionRepository.findById(id).isEmpty || overwrite) {
                        val question = Question(
                            id,
                            toc.concatenatedUtterances!!,
                            toc.utterances!!,
                            questionText
                        )
                        toSaveList.add(question)
                    }
                }
            }
        }

        questionRepository.saveAll(toSaveList)
        logger.info("Embedding questions created successfully")
    }

    fun embedQuery(query: String): ByteArray {
        logger.info("Received question: {}", query)
        return embedder.getTextEmbeddingsAsBytes(listOf(query), `Question$`.QUESTION).first()
    }

    fun searchByQuestion(embedding: ByteArray): List<QuestionSearchResult> {
        val stream: SearchStream<Question> = entityStream.of(Question::class.java)
        return stream
            .filter(`Question$`.EMBEDDED_QUESTION.knn(3, embedding))
            .sorted(`Question$`._EMBEDDED_QUESTION_SCORE)
            .map(Fields.of(`Question$`._THIS, `Question$`._EMBEDDED_QUESTION_SCORE))
            .collect(Collectors.toList())
            .map { QuestionSearchResult(it.first, it.second) }
    }

    fun getCacheResponse(question: String): Optional<CacheHit> {
        return questionsSemanticCache.check(question)
    }

    fun cacheResponse(query: String, answer: String, isQuestion: Boolean) {
        questionsSemanticCache.store(query, answer)
    }
}