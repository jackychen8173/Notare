package com.notare.sage;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SageConfig {

    @Bean
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.fromEnv();
    }

    // Spring Boot 4.1's own Jackson auto-configuration builds a Jackson 3.x
    // tools.jackson.databind.json.JsonMapper, not a com.fasterxml (Jackson 2.x) ObjectMapper,
    // so the classic ObjectMapper bean SageService needs is no longer auto-registered.
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
