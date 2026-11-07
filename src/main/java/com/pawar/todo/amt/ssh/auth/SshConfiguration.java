package com.pawar.todo.amt.ssh.auth;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pawar.todo.amt.ssh.SshProperties;
import com.pawar.todo.amt.ssh.ca.SshCertificateAuthorityClient;
import com.pawar.todo.amt.ssh.ca.vault.VaultSshCertificateAuthorityClient;
import com.pawar.todo.amt.ssh.key.EphemeralKeyPairGenerator;
import com.pawar.todo.amt.ssh.rotation.SshCertificateRotationEngine;

@Configuration
public class SshConfiguration {

    @Bean
    @ConditionalOnProperty(name = "ssh.auth-mode", havingValue = "CA_CERTIFICATE")
    public CaCertificateCredentialProvider caCertificateCredentialProvider() {
        return new CaCertificateCredentialProvider();
    }

    @Bean
    @ConditionalOnProperty(name = "ssh.auth-mode", havingValue = "CA_CERTIFICATE")
    @ConditionalOnMissingBean(SshCertificateAuthorityClient.class)
    public SshCertificateAuthorityClient sshCertificateAuthorityClient(SshProperties properties) {
        return new VaultSshCertificateAuthorityClient(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "ssh.auth-mode", havingValue = "CA_CERTIFICATE")
    public SshCertificateRotationEngine sshCertificateRotationEngine(
            SshProperties properties,
            EphemeralKeyPairGenerator keyPairGenerator,
            SshCertificateAuthorityClient caClient,
            CaCertificateCredentialProvider credentialProvider,
            ApplicationEventPublisher eventPublisher) {
        return new SshCertificateRotationEngine(properties, keyPairGenerator, caClient, credentialProvider, eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean(SshCredentialProvider.class)
    public SshCredentialProvider sshCredentialProvider(SshProperties properties,
                                                       Optional<CaCertificateCredentialProvider> caProvider) {
        if (properties.getAuthMode() == SshAuthMode.CA_CERTIFICATE && caProvider.isPresent()) {
            return caProvider.get();
        }
        return new StaticKeyCredentialProvider(properties);
    }
}
