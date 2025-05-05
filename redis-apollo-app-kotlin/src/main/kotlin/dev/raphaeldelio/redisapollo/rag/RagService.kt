package dev.raphaeldelio.redisapollo.rag

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.stereotype.Service

@Service
class RagService(
    private val chatModel: OpenAiChatModel
) {
    private val logger = LoggerFactory.getLogger(RagService::class.java)

    fun enhanceWithRag(query: String, data: String): String {
        val prompt = Prompt(
            listOf(
                SystemMessage(
                    """
                    You are an expert assistant specializing in the Apollo missions. Your goal is to provide accurate,
                    detailed, and concise answers to user inquiries by utilizing the provided Apollo mission data.
                    Rely solely on the information given below and avoid introducing external information.
                    """.trimIndent()
                ),
                SystemMessage("Apollo mission data: $data"),
                UserMessage("User question: $query")
            )
        )

        val response: ChatResponse = chatModel.call(prompt)
        val enhancedAnswer = response.result.output.text
        logger.info("AI response: {}", enhancedAnswer)
        return enhancedAnswer
    }
}