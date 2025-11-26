package dev.raphaeldelio.redisapollo.tableofcontents

import com.redis.om.spring.search.stream.EntityStream
import dev.raphaeldelio.redisapollo.dataloader.FileService
import dev.raphaeldelio.redisapollo.RedisService
import dev.raphaeldelio.redisapollo.utterance.Utterance
import dev.raphaeldelio.redisapollo.utterance.`Utterance$`.TIMESTAMP_INT
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class TOCService(
    private val tocDataRepository: TOCDataRepository,
    private val entityStream: EntityStream,
    private val fileService: FileService,
    private val redisService: RedisService
) {
    private val logger = LoggerFactory.getLogger(TOCService::class.java)

    fun loadTOCData(filePath: String) {
        logger.info("Loading TOC data from file: {}", filePath)
        fileService.readAndProcessFile(filePath, TOCData::class.java) { data ->
            data.filter(::isValidToc)
                .onEach { tocData ->
                    val startInt = fileService.asSeconds(tocData.startDate)
                    tocData.startDate = tocData.startDate.replace(":", ";")
                    tocData.startDateInt = startInt }
                .chunked(1000) { batch ->
                    tocDataRepository.saveAll(batch)
                }
        }
    }

    private fun isValidToc(tocData: TOCData): Boolean {
        return !tocData.description.startsWith("Video: ")
    }

    fun populateUtterances(overwrite: Boolean = false) {
        logger.info("Grouping utterances by TOC entries")
        val tocList = entityStream.of(TOCData::class.java)
            .sorted(`TOCData$`.START_DATE_INT)
            .collect(Collectors.toList())

        val toFilterOut = redisService.getZRangeByScore("utterances", 2.0, Double.MAX_VALUE)

        tocList.onEach { logger.info("Processing TOC entry: {}", it.startDateInt) }
            .filter { overwrite || it.concatenatedUtterances.isNullOrBlank() }
            .onEach {
                if (!overwrite) {
                    logger.info("Utterances already grouped for TOC entry: {}", it.startDate)
                }
            }
            .mapNotNull { toc ->
                val start = toc.startDateInt
                val end = tocList.getOrNull(tocList.indexOf(toc) + 1)?.startDateInt ?: Int.MAX_VALUE

                val utterances = entityStream.of(Utterance::class.java)
                    .filter(TIMESTAMP_INT.ge(start).and(TIMESTAMP_INT.le(end)))
                    .collect(Collectors.toList())
                    .filterNot { it.text in toFilterOut }

                if (utterances.isEmpty()) {
                    logger.info("No utterances found for TOC entry: {}", toc.startDate)
                    return@mapNotNull null
                }

                val grouped = utterances.joinToString("\n") { "${it.speaker}: ${it.text}" }

                toc.apply {
                    concatenatedUtterances = grouped
                    this.utterances = utterances
                }
            }
            .onEach {
                tocDataRepository.save(it)
                logger.info("Grouped utterances for TOC entry: {}", it.startDate)
            }

        logger.info("Grouping complete")
    }

    fun getAllWithoutQuestions(): List<TOCData> {
        return tocDataRepository.findAll()
            .filter {
                !it.concatenatedUtterances.isNullOrBlank() &&
                        it.questions.isNullOrEmpty()
            }
    }

    fun getAllWithQuestions(): List<TOCData> {
        return tocDataRepository.findAll().filter { !it.questions.isNullOrEmpty() }
    }

    fun getAllWithoutSummary(): List<TOCData> {
        return tocDataRepository.findAll()
            .filter {
                !it.concatenatedUtterances.isNullOrBlank() &&
                        it.summary.isNullOrEmpty()
            }
    }

    fun getAllWithSummary(): List<TOCData> {
        return tocDataRepository.findAll().filter { !it.summary.isNullOrEmpty() }
    }

    fun saveAll(tocData: List<TOCData>): List<TOCData> {
        tocDataRepository
        return tocDataRepository.saveAll(tocData)
    }

    fun updateSummary(tocData: List<TOCData>) {
        tocData.forEach { toc ->
            tocDataRepository.updateField(toc, `TOCData$`.SUMMARY, toc.summary)
        }
    }

    fun updateQuestions(tocData: List<TOCData>) {
        tocData.forEach { toc ->
            tocDataRepository.updateField(toc, `TOCData$`.QUESTIONS, toc.questions)
        }
    }
}