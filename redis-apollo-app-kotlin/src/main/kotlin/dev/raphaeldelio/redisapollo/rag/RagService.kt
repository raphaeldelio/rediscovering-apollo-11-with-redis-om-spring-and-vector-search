package dev.raphaeldelio.redisapollo.rag

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class RagService(
    private val ragChatClient: ChatClient
) {
    private val logger = LoggerFactory.getLogger(RagService::class.java)

    fun enhanceWithRag(query: String, data: String): String {
        val response = ragChatClient
            .prompt()
            .system("Apollo mission data: $data")
            .user("User question: $query")
            .call()
            .chatResponse()

        val enhancedAnswer = response?.result?.output?.text ?: ""
        logger.info("AI response: {}", enhancedAnswer)
        return enhancedAnswer
    }
}