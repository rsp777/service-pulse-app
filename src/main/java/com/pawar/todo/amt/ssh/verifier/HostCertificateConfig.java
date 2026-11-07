package com.pawar.todo.amt.ssh.verifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HostCertificateConfig {

    private final String trustedHostCaPublicKey;
    private final List<String> allowedDomains;
    private final boolean strictChecking;

    public HostCertificateConfig(String trustedHostCaPublicKey, List<String> allowedDomains, boolean strictChecking) {
        this.trustedHostCaPublicKey = trustedHostCaPublicKey;
        this.allowedDomains = allowedDomains != null ? Collections.unmodifiableList(new ArrayList<>(allowedDomains)) : Collections.emptyList();
        this.strictChecking = strictChecking;
    }

    public String getTrustedHostCaPublicKey() {
        return trustedHostCaPublicKey;
    }

    public List<String> getAllowedDomains() {
        return allowedDomains;
    }

    public boolean isStrictChecking() {
        return strictChecking;
    }
}
