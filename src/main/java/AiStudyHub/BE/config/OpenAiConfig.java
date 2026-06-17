package AiStudyHub.BE.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAI integration.
 * Sets up the ChatClient using Spring AI's auto-configured builder.
 */
@Configuration
public class OpenAiConfig {

    /**
     * Configures the ChatClient bean.
     *
     * @param builder the auto-configured ChatClient.Builder
     * @return the configured ChatClient
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
