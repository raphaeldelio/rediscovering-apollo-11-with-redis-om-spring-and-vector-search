package dev.raphaeldelio.redisapollo;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories;
import dev.raphaeldelio.redisapollo.photograph.PhotographService;
import dev.raphaeldelio.redisapollo.question.QuestionService;
import dev.raphaeldelio.redisapollo.summary.SummaryService;
import dev.raphaeldelio.redisapollo.tableofcontents.TOCDataRepository;
import dev.raphaeldelio.redisapollo.tableofcontents.TOCService;
import dev.raphaeldelio.redisapollo.utterance.UtteranceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;


@EnableRedisDocumentRepositories(basePackages = {"dev.raphaeldelio.redisapollo.tableofcontents*"})
@EnableRedisEnhancedRepositories(basePackages = {"dev.raphaeldelio.redisapollo.*"}, excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {TOCDataRepository.class})})
@SpringBootApplication
public class RedisApolloApplication {

    private static final Logger logger = LoggerFactory.getLogger(RedisApolloApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RedisApolloApplication.class, args);
    }

    @Bean
    CommandLineRunner loadData(
            UtteranceService utteranceService,
            TOCService tocService,
            PhotographService photographService,
            QuestionService questionService,
            SummaryService summaryService) {
        return args -> {
            long startTime = System.currentTimeMillis();
            String filePath = "./redis-apollo-app-java/src/main/resources/Apollo11_Data/";
            //utteranceService.loadUtteranceData(filePath + "gUtteranceData.json");

            //tocService.loadTOCData(filePath + "gTOCData.json");
            //tocService.populateUtterances();

            //tocService.summarize();
            //summaryService.embedSummaries();

            //tocService.generateQuestions();
            //questionService.embedQuestions();

            //photographService.loadPhotographData(filePath + "gPhotoData.json", true);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            logger.info("Data loaded in {} milliseconds", duration);
        };
    }

    @Bean
    OpenAiChatModel openAiChatModel() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(60));

        var openAiApi = OpenAiApi.builder()
                .apiKey(System.getenv("OPEN_AI_KEY"))
                .restClientBuilder(RestClient.builder().requestFactory(factory))
                .build();
        var openAiChatOptions = OpenAiChatOptions.builder()
                .model("gpt-4o-mini")
                .build();
        return OpenAiChatModel.builder().openAiApi(openAiApi).defaultOptions(openAiChatOptions).build();
    }
}
