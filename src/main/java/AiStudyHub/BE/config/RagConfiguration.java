package AiStudyHub.BE.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for RAG components.
 * Exposes text splitter beans.
 */
@Configuration
public class RagConfiguration {

    /**
     * Exposes the TokenTextSplitter bean.
     * Splitting is done based on token count using CL100K_BASE encoding.
     * Default chunk size is set to 800 tokens.
     *
     * @return the TokenTextSplitter bean
     */
    @Bean
    public TokenTextSplitter textSplitter() {
        // Constructor: defaultChunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator
        return new TokenTextSplitter(800, 100, 5, 10000, true);
    }
}
