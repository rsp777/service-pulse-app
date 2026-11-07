package com.pawar.todo.amt.ssh.ca;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CertificateSigningOptions {

    private final List<String> validPrincipals;
    private final Duration ttl;
    private final String certType;
    private final String keyId;

    public CertificateSigningOptions(List<String> validPrincipals, Duration ttl, String certType, String keyId) {
        this.validPrincipals = validPrincipals != null ? Collections.unmodifiableList(new ArrayList<>(validPrincipals)) : Collections.emptyList();
        this.ttl = ttl != null ? ttl : Duration.ofHours(1);
        this.certType = certType != null ? certType : "user";
        this.keyId = keyId != null ? keyId : "service-pulse-app";
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<String> getValidPrincipals() {
        return validPrincipals;
    }

    public Duration getTtl() {
        return ttl;
    }

    public String getCertType() {
        return certType;
    }

    public String getKeyId() {
        return keyId;
    }

    public static class Builder {
        private List<String> validPrincipals = new ArrayList<>();
        private Duration ttl = Duration.ofMinutes(60);
        private String certType = "user";
        private String keyId = "service-pulse-app";

        public Builder validPrincipals(List<String> principals) {
            if (principals != null) {
                this.validPrincipals = new ArrayList<>(principals);
            }
            return this;
        }

        public Builder principal(String principal) {
            if (principal != null && !principal.isBlank()) {
                this.validPrincipals.add(principal);
            }
            return this;
        }

        public Builder ttl(Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        public Builder certType(String certType) {
            this.certType = certType;
            return this;
        }

        public Builder keyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        public CertificateSigningOptions build() {
            return new CertificateSigningOptions(validPrincipals, ttl, certType, keyId);
        }
    }
}
