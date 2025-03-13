package dev.raphaeldelio.redisapollo;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories;
import dev.raphaeldelio.redisapollo.service.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


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
            //photographsService.loadPhotographData(filePath + "gPhotoData.json");
            //tocService.summarize();
            //tocService.generateQuestions();
            //utteranceQuestionsService.embedQuestions();
            //utteranceSummariesService.embedSummaries();
        };
    }
}
