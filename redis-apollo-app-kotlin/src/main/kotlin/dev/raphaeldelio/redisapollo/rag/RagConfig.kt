package dev.raphaeldelio.redisapollo.rag

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RagConfig {

    @Bean
    fun ragChatClient(
        chatModel: ChatModel,
    ): ChatClient {
        return ChatClient.builder(chatModel)
            .defaultSystem(DEFAULT_PROMPT)
            .build()
    }

    companion object {
        private val DEFAULT_PROMPT = """
                    You are an expert assistant specializing in the Apollo missions. Your goal is to provide accurate,
                    detailed, and concise answers to user inquiries by utilizing the provided Apollo mission data.
                    Rely solely on the information given below and avoid introducing external information.
            """.trimIndent()
    }
}
