package com.pawar.todo.amt.appconfig;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	 @Override
     public void addCorsMappings(CorsRegistry registry) {
         registry.addMapping("/api/**") // Adjust the path as needed
                 .allowedOrigins("*") // Replace with your client origin
                 .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                 .allowedHeaders("*")
                 .allowCredentials(false);
     }
}
