package dev.raphaeldelio.redisapollo.dataloader.workflow

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SummarizationWorkflowConfig {

    @Bean
    fun summarizationChatClient(
        chatModel: ChatModel,
    ): ChatClient {
        return ChatClient.builder(chatModel)
            .defaultSystem(DEFAULT_PROMPT)
            .build()
    }

    companion object {
        private val DEFAULT_PROMPT = """
            You are a helpful assistant who summarizes utterances of the Apollo 11 mission.
            Make these summaries very dense with all curiosities included.
            Limit the summary to 512 words.
            """.trimIndent()
    }
}
