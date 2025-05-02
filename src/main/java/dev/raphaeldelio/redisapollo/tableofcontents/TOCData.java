package dev.raphaeldelio.redisapollo.tableofcontents;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import dev.raphaeldelio.redisapollo.utterance.Utterance;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor(force = true)
@Document
public class TOCData {

    @Id
    @NonNull
    private String startDate;

    @Indexed
    private int startDateInt;

    @Indexed
    @NonNull
    private String title;

    @Indexed
    @NonNull
    private String description;

    @Indexed
    private String summary;

    @Indexed
    private List<String> questions;

    @Indexed
    private String concatenatedUtterances;

    @Indexed
    private List<Utterance> utterances;
}


