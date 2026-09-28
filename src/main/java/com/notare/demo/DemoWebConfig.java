package com.notare.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DemoWebConfig implements WebMvcConfigurer {

    private final DemoRestrictionInterceptor demoRestrictionInterceptor;

    public DemoWebConfig(DemoRestrictionInterceptor demoRestrictionInterceptor) {
        this.demoRestrictionInterceptor = demoRestrictionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(demoRestrictionInterceptor)
                .addPathPatterns(DemoRestrictionInterceptor.PAID_ENDPOINTS);
    }
}
