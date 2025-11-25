package dev.raphaeldelio.redisapollo.tableofcontents

import com.redis.om.spring.annotations.Document
import com.redis.om.spring.annotations.Indexed
import dev.raphaeldelio.redisapollo.utterance.Utterance
import org.springframework.data.annotation.Id
import org.springframework.lang.NonNull

@Document
data class TOCData(
    @Id
    @NonNull
    var startDate: String = "",

    @Indexed
    var startDateInt: Int = 0,

    @Indexed
    @NonNull
    var title: String = "",

    @Indexed
    @NonNull
    var description: String = "",

    @Indexed
    var summary: String? = null,

    @Indexed
    var questions: List<String>? = null,

    @Indexed
    var concatenatedUtterances: String? = null,

    @Indexed
    var utterances: List<Utterance>? = null
) {
    constructor(
        startDate: String,
        title: String,
        description: String
    ) : this(
        startDate = startDate,
        startDateInt = 0,
        title = title,
        description = description,
        summary = null,
        questions = null,
        concatenatedUtterances = null,
        utterances = null
    )
}