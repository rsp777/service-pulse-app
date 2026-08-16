package com.pawar.todo.amt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;


@SpringBootApplication(scanBasePackages = {"com.pawar.todo", "com.pawar.sop"})
@EnableScheduling
public class ServicePulseApp implements ServletContextInitializer{

	public static void main(String[] args) {
		SpringApplication.run(ServicePulseApp.class, args);
	}
	
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
//		servletContext.setInitParameter("org.apache.tomcat.websocket.textBuffer	Size", "700000");
	}

}
