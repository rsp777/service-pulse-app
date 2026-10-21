package com.pawar.todo.amt.ssh.ca.vault;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pawar.todo.amt.ssh.SshProperties;
import com.pawar.todo.amt.ssh.ca.CertificateSigningOptions;
import com.pawar.todo.amt.ssh.ca.SshCaException;
import com.pawar.todo.amt.ssh.ca.SshCertificateAuthorityClient;
import com.pawar.todo.amt.ssh.ca.SshSignedCertificate;
import com.pawar.todo.amt.ssh.cert.OpenSshCertificate;
import com.pawar.todo.amt.ssh.cert.OpenSshCertificateParser;

/**
 * HashiCorp Vault SSH Secrets Engine client for signing OpenSSH user certificates.
 */
public class VaultSshCertificateAuthorityClient implements SshCertificateAuthorityClient {

    private static final Logger logger = LoggerFactory.getLogger(VaultSshCertificateAuthorityClient.class);

    private final SshProperties properties;
    private final RestTemplate restTemplate;
    private final Gson gson = new Gson();

    public VaultSshCertificateAuthorityClient(SshProperties properties) {
        this(properties, new RestTemplate());
    }

    public VaultSshCertificateAuthorityClient(SshProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public SshSignedCertificate sign(String openSshPublicKey, CertificateSigningOptions options) throws SshCaException {
        SshProperties.CaProperties ca = properties.getCa();
        String endpoint = ca.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new SshCaException("Vault endpoint is not configured");
        }

        String mount = StringUtils.hasText(ca.getMount()) ? ca.getMount() : "ssh";
        String role = StringUtils.hasText(ca.getRole()) ? ca.getRole() : "servicepulse-runner";
        String url = endpoint.replaceAll("/+$", "") + "/v1/" + mount + "/sign/" + role;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(ca.getToken())) {
            headers.set("X-Vault-Token", ca.getToken());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("public_key", openSshPublicKey);
        payload.put("cert_type", options != null ? options.getCertType() : "user");

        if (options != null && !options.getValidPrincipals().isEmpty()) {
            payload.put("valid_principals", String.join(",", options.getValidPrincipals()));
        } else if (ca.getPrincipals() != null && !ca.getPrincipals().isEmpty()) {
            payload.put("valid_principals", String.join(",", ca.getPrincipals()));
        }

        Duration ttl = (options != null && options.getTtl() != null)
                ? options.getTtl()
                : Duration.ofMinutes(ca.getCertTtlMinutes() > 0 ? ca.getCertTtlMinutes() : 60);
        payload.put("ttl", ttl.toMinutes() + "m");

        HttpEntity<String> request = new HttpEntity<>(gson.toJson(payload), headers);

        try {
            logger.info("Requesting SSH User Certificate from Vault (role={}, endpoint={})", role, endpoint);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new SshCaException("Vault returned non-2xx response: " + response.getStatusCode(), response.getStatusCode().value());
            }

            JsonObject json = JsonParser.parseString(response.getBody()).getAsJsonObject();
            if (!json.has("data") || !json.getAsJsonObject("data").has("signed_key")) {
                throw new SshCaException("Vault response missing 'data.signed_key' payload");
            }

            String signedKey = json.getAsJsonObject("data").get("signed_key").getAsString();
            OpenSshCertificate certificate = OpenSshCertificateParser.parse(signedKey);
            logger.info("Successfully received signed SSH certificate (serial={}, validBefore={})",
                    certificate.getSerial(), certificate.getValidBefore());

            return new SshSignedCertificate(signedKey, certificate);

        } catch (HttpStatusCodeException e) {
            logger.error("Vault SSH signing failed with status {}", e.getStatusCode());
            throw new SshCaException("Vault SSH signing failed: " + e.getStatusText(), e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            logger.error("Vault SSH CA communication error: {}", e.getMessage());
            throw new SshCaException("Failed to communicate with Vault SSH CA: " + e.getMessage(), e);
        }
    }
}
