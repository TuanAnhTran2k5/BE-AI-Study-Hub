package AiStudyHub.BE.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAI integration.
 * Sets up the ChatClient using Spring AI's auto-configured builder.
 */
@Configuration
public class OpenAiConfig {

    @Bean
    public ChatClient chatClient(ObjectProvider<ChatClient.Builder> builderProvider) {
        ChatClient.Builder builder = builderProvider.getIfAvailable();
        if (builder != null) {
            return builder.build();
        }
        return ChatClient.builder(prompt -> null).build();
    }
}
