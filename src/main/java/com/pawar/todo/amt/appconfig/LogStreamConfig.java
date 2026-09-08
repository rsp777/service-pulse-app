package com.pawar.todo.amt.appconfig;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class LogStreamConfig {

	private static final Logger logger = LoggerFactory.getLogger(LogStreamConfig.class);

	@Bean
	public ThreadPoolTaskExecutor logStreamExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(20);
		executor.setThreadNamePrefix("log-stream-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		executor.initialize();
		logger.info("Initialized log stream executor with core={}, max={}, queue={}", 2, 10, 20);
		return executor;
	}
}