package AiStudyHub.BE.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for OpenAI integration.
 * Sets up the ChatClient using Spring AI's auto-configured builder or a safe fallback.
 */
@Configuration
public class OpenAiConfig {

    @Bean
    public ChatClient chatClient(ObjectProvider<ChatClient.Builder> builderProvider, ObjectProvider<ChatModel> chatModelProvider) {
        ChatClient.Builder builder = builderProvider.getIfAvailable();
        if (builder != null) {
            return builder.build();
        }

        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel != null) {
            return ChatClient.create(chatModel);
        }

        ChatModel dummyModel = prompt -> new ChatResponse(List.of());
        return ChatClient.create(dummyModel);
    }
}
