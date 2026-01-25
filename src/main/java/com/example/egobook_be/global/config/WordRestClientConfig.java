package com.example.egobook_be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WordRestClientConfig {

    @Bean
    public RestClient wordRestClient(
            RestClient.Builder builder,
            @Value("${word.ai.base-url}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }
}
