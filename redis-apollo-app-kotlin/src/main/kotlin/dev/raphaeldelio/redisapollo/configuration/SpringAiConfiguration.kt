package dev.raphaeldelio.redisapollo.configuration

import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class SpringAiConfiguration {
    @Bean
    fun openAiChatModel(): OpenAiChatModel {
        val factory = SimpleClientHttpRequestFactory().apply {
            setReadTimeout(Duration.ofSeconds(60))
        }

        val openAiApi = OpenAiApi.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .restClientBuilder(RestClient.builder().requestFactory(factory))
            .build()

        val options = OpenAiChatOptions.builder()
            .model("gpt-4o-mini")
            .build()

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build()
    }
}