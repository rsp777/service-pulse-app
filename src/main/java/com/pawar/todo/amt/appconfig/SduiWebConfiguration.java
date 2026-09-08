package com.pawar.todo.amt.appconfig;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.pawar.todo.amt.controller.SduiDriftInterceptor;

@Configuration
public class SduiWebConfiguration implements WebMvcConfigurer {

    private final SduiDriftInterceptor driftInterceptor;

    public SduiWebConfiguration(SduiDriftInterceptor driftInterceptor) {
        this.driftInterceptor = driftInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(driftInterceptor).addPathPatterns("/dashboard/**");
    }
}