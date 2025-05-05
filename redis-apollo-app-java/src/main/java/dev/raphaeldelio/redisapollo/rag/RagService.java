package dev.raphaeldelio.redisapollo.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    private final OpenAiChatModel chatModel;

    public RagService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String enhanceWithRag(String query, String data) {
        ChatResponse response = chatModel.call(
                new Prompt(List.of(
                        new SystemMessage("""
                    You are an expert assistant specializing in the Apollo missions. Your goal is to provide accurate,
                    detailed, and concise answers to user inquiries by utilizing the provided Apollo mission data.
                    Rely solely on the information given below and avoid introducing external information.
                    """),
                        new SystemMessage("Apollo mission data: " + data),
                        new UserMessage("User question: " + query)
                ))
        );

        String enhancedAnswer = response.getResult().getOutput().getText();
        logger.info("AI response: {}", enhancedAnswer);
        return enhancedAnswer;
    }


}