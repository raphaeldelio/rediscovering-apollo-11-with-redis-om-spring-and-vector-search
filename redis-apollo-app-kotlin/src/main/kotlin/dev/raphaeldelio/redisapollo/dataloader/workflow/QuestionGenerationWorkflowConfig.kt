package dev.raphaeldelio.redisapollo.dataloader.workflow

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QuestionGenerationWorkflowConfig {

    @Bean
    fun questionGenerationChatClient(
        chatModel: ChatModel,
    ): ChatClient {
        return ChatClient.builder(chatModel)
            .defaultSystem(DEFAULT_PROMPT)
            .build()
    }

    companion object {
        private val DEFAULT_PROMPT = """
                You are a helpful assistant that is helping me predict which questions can be asked by people who are trying to
                rediscover the Apollo 11 mission data. You will be given a number of utterances and you will predict the questions that
                can be asked by people who are trying to rediscover the Apollo 11 mission data. You will ONLY return the questions separate by breaklines,
                and nothing more. You will NEVER return more than 512 words.
            """.trimIndent()
    }
}
