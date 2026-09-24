package com.notare.coderun;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// Colocated with the coderun feature package rather than a shared config/ package - this codebase
// doesn't have one; see SageConfig for the same pattern with the Anthropic client.
//
// Builds the RestClient via its own static factory rather than taking an injected
// RestClient.Builder - this app's Spring Boot 4.1 setup doesn't auto-configure that builder bean
// (found via a real production crash-loop, UnsatisfiedDependencyException on every boot), the same
// class of Spring Boot 4.1 auto-configuration gap already documented for Jackson's ObjectMapper
// bean in SageConfig.
@Configuration
public class CodeRunConfig {

    @Bean
    public RestClient codeRunRestClient(@Value("${code-run.service-url}") String serviceUrl) {
        return RestClient.builder().baseUrl(serviceUrl).build();
    }
}
