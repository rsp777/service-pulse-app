package com.pawar.todo.amt.appconfig;

import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

	 @Override
     public void addCorsMappings(CorsRegistry registry) {
         registry.addMapping("/api/**") // Adjust the path as needed
                 .allowedOrigins("*") // Replace with your client origin
                 .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                 .allowedHeaders("*")
                 .allowCredentials(false);
		 logger.info("Configured API CORS policy for all origins with credentials disabled");
     }
}
