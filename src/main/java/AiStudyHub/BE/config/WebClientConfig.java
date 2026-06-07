package AiStudyHub.BE.config;

import reactor.netty.resources.ConnectionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;


import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {

        ConnectionProvider provider =
                ConnectionProvider.builder("ai-study-hub-pool")
                                  .maxConnections(50)
                                  .maxIdleTime(Duration.ofSeconds(20))
                                  .maxLifeTime(Duration.ofMinutes(1))
                                  .pendingAcquireTimeout(Duration.ofSeconds(60))
                                  .build();

        HttpClient httpClient =
                HttpClient.create(provider)
                          .responseTimeout(Duration.ofMinutes(2))
                          .followRedirect(true);

        return WebClient.builder()
                        .clientConnector(
                                new ReactorClientHttpConnector(httpClient)
                        );
    }
}