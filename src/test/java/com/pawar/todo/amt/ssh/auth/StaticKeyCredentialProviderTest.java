package com.pawar.todo.amt.ssh.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.pawar.todo.amt.ssh.SshProperties;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

class StaticKeyCredentialProviderTest {

    private SshProperties properties;
    private StaticKeyCredentialProvider provider;
    private SSHClient sshClient;

    @BeforeEach
    void setUp() {
        properties = new SshProperties();
        provider = new StaticKeyCredentialProvider(properties);
        sshClient = mock(SSHClient.class);
    }

    @Test
    void isReady_whenKeyPathUnset_returnsFalse() {
        properties.setPrivateKeyPath(null);
        assertFalse(provider.isReady());

        properties.setPrivateKeyPath("");
        assertFalse(provider.isReady());
    }

    @Test
    void isReady_whenFileDoesNotExist_returnsFalse() {
        properties.setPrivateKeyPath("non-existent-key-file.pem");
        assertFalse(provider.isReady());
    }

    @Test
    void isReady_whenFileExists_returnsTrue(@TempDir File tempDir) throws IOException {
        File keyFile = new File(tempDir, "id_rsa");
        assertTrue(keyFile.createNewFile());

        properties.setPrivateKeyPath(keyFile.getAbsolutePath());
        assertTrue(provider.isReady());
    }

    @Test
    void getMetadata_returnsStaticAuthMode() {
        properties.setPrivateKeyPath(null);
        SshCredentialMetadata metadata = provider.getMetadata();

        assertNotNull(metadata);
        assertEquals(SshAuthMode.STATIC, metadata.getAuthMode());
        assertFalse(metadata.isValid());
    }

    @Test
    void getKeyProvider_whenPathMissing_throwsIOException() {
        properties.setPrivateKeyPath("");
        assertThrows(IOException.class, () -> provider.getKeyProvider(sshClient));
    }

    @Test
    void getKeyProvider_withoutPassphrase_callsClientLoadKeysWithoutPassphrase(@TempDir File tempDir) throws IOException {
        File keyFile = new File(tempDir, "id_rsa");
        assertTrue(keyFile.createNewFile());
        properties.setPrivateKeyPath(keyFile.getAbsolutePath());
        properties.setPrivateKeyPassphrase(null);

        KeyProvider mockKeyProvider = mock(KeyProvider.class);
        when(sshClient.loadKeys(keyFile.getAbsolutePath())).thenReturn(mockKeyProvider);

        KeyProvider result = provider.getKeyProvider(sshClient);
        assertEquals(mockKeyProvider, result);
        verify(sshClient).loadKeys(keyFile.getAbsolutePath());
    }

    @Test
    void getKeyProvider_withPassphrase_callsClientLoadKeysWithPassphrase(@TempDir File tempDir) throws IOException {
        File keyFile = new File(tempDir, "id_rsa");
        assertTrue(keyFile.createNewFile());
        properties.setPrivateKeyPath(keyFile.getAbsolutePath());
        properties.setPrivateKeyPassphrase("secret-passphrase");

        KeyProvider mockKeyProvider = mock(KeyProvider.class);
        when(sshClient.loadKeys(keyFile.getAbsolutePath(), "secret-passphrase")).thenReturn(mockKeyProvider);

        KeyProvider result = provider.getKeyProvider(sshClient);
        assertEquals(mockKeyProvider, result);
        verify(sshClient).loadKeys(keyFile.getAbsolutePath(), "secret-passphrase");
    }
}
