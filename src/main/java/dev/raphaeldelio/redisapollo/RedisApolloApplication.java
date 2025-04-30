package dev.raphaeldelio.redisapollo;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories;
import dev.raphaeldelio.redisapollo.service.*;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;


@EnableRedisDocumentRepositories(basePackages = {"dev.raphaeldelio.redisapollo.document.*"})
@EnableRedisEnhancedRepositories(basePackages = {"dev.raphaeldelio.redisapollo.hash.*"})
@SpringBootApplication
public class RedisApolloApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisApolloApplication.class, args);
    }

    @Bean
    CommandLineRunner loadData(
            UtteranceService utteranceService,
            TOCService tocService,
            PhotographsService photographsService,
            UtteranceQuestionsService utteranceQuestionsService,
            UtteranceSummariesService utteranceSummariesService) {
        return args -> {
            String filePath = "./src/main/resources/Apollo11_Data/";
            //utteranceService.loadUtteranceData(filePath + "gUtteranceData.json");

            //tocService.loadTOCData(filePath + "gTOCData.json");
            //tocService.populateUtterances();

            //tocService.summarize();
            //utteranceSummariesService.embedSummaries(true);

            tocService.generateQuestions(true);
            //utteranceQuestionsService.embedQuestions();

            //photographsService.loadPhotographData(filePath + "gPhotoData.json");
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
