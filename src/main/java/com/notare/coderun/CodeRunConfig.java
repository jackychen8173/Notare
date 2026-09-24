package com.notare.coderun;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// Colocated with the coderun feature package rather than a shared config/ package - this codebase
// doesn't have one; see SageConfig for the same pattern with the Anthropic client.
@Configuration
public class CodeRunConfig {

    @Bean
    public RestClient codeRunRestClient(
            RestClient.Builder builder,
            @Value("${code-run.service-url}") String serviceUrl
    ) {
        return builder.baseUrl(serviceUrl).build();
    }
}
