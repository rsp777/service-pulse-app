package com.pawar.todo.amt.ssh.ca;

import com.pawar.todo.amt.ssh.cert.OpenSshCertificate;

public class SshSignedCertificate {

    private final String rawCertificate;
    private final OpenSshCertificate parsedCertificate;

    public SshSignedCertificate(String rawCertificate, OpenSshCertificate parsedCertificate) {
        this.rawCertificate = rawCertificate;
        this.parsedCertificate = parsedCertificate;
    }

    public String getRawCertificate() {
        return rawCertificate;
    }

    public OpenSshCertificate getParsedCertificate() {
        return parsedCertificate;
    }

    public long getSerial() {
        return parsedCertificate != null ? parsedCertificate.getSerial() : 0L;
    }

    @Override
    public String toString() {
        return "SshSignedCertificate{" +
                "serial=" + getSerial() +
                ", parsedCertificate=" + parsedCertificate +
                '}';
    }
}
