package com.pawar.todo.amt.ssh.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pawar.todo.amt.ssh.SshProperties;

@Configuration
public class SshConfiguration {

    @Bean
    @ConditionalOnMissingBean(SshCredentialProvider.class)
    public SshCredentialProvider sshCredentialProvider(SshProperties properties) {
        return new StaticKeyCredentialProvider(properties);
    }
}
