package dev.raphaeldelio.redisapollo.tableofcontents;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import dev.raphaeldelio.redisapollo.utterance.Utterance;
import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Objects;

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

    public TOCData() {}

    public TOCData(@NonNull String startDate, @NonNull String title, @NonNull String description) {
        this.startDate = startDate;
        this.title = title;
        this.description = description;
    }

    public @NonNull String getStartDate() {
        return startDate;
    }

    public void setStartDate(@NonNull String startDate) {
        this.startDate = startDate;
    }

    public int getStartDateInt() {
        return startDateInt;
    }

    public void setStartDateInt(int startDateInt) {
        this.startDateInt = startDateInt;
    }

    public @NonNull String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public @NonNull String getDescription() {
        return description;
    }

    public void setDescription(@NonNull String description) {
        this.description = description;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }

    public String getConcatenatedUtterances() {
        return concatenatedUtterances;
    }

    public void setConcatenatedUtterances(String concatenatedUtterances) {
        this.concatenatedUtterances = concatenatedUtterances;
    }

    public List<Utterance> getUtterances() {
        return utterances;
    }

    public void setUtterances(List<Utterance> utterances) {
        this.utterances = utterances;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TOCData tocData = (TOCData) o;
        return startDateInt == tocData.startDateInt && Objects.equals(startDate, tocData.startDate) && Objects.equals(title, tocData.title) && Objects.equals(description, tocData.description) && Objects.equals(summary, tocData.summary) && Objects.equals(questions, tocData.questions) && Objects.equals(concatenatedUtterances, tocData.concatenatedUtterances) && Objects.equals(utterances, tocData.utterances);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, startDateInt, title, description, summary, questions, concatenatedUtterances, utterances);
    }
}


