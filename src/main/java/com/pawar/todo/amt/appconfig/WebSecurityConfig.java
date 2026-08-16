package com.pawar.todo.amt.appconfig;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties
public class WebSecurityConfig {

	 @Bean
	 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	        http
	            .csrf(csrf -> csrf.disable())
	            .authorizeRequests(authz -> authz
	                .requestMatchers("/api/register").permitAll()
	                .requestMatchers("/api/roles").permitAll()
	                .requestMatchers("/api/roles/{role_id}").permitAll()
	                .requestMatchers("/api/users").permitAll()
	                .requestMatchers("/api/users/{userId}").permitAll()
	                .requestMatchers("/api/user_roles/user/{userId}").permitAll()
	                .anyRequest().permitAll())
	            .formLogin(form -> form.loginPage("/login").permitAll())
	            .logout(logout -> logout.permitAll());
	        return http.build();
	    }

	    @Bean
	    PasswordEncoder passwordEncoder() {
	        return new BCryptPasswordEncoder();
	    }
	
}
