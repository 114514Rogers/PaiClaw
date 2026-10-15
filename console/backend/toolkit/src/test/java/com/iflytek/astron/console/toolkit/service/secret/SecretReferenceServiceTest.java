package com.iflytek.astron.console.toolkit.service.secret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.iflytek.astron.console.commons.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class SecretReferenceServiceTest {

    @Test
    void normalizeAcceptsVariableNameAndReference() {
        SecretReferenceService service = new SecretReferenceService(new MockEnvironment());

        assertEquals("env:PAIFLOW_MODEL_API_KEY", service.normalize("PAIFLOW_MODEL_API_KEY"));
        assertEquals("env:PAIFLOW_MODEL_API_KEY", service.normalize("env:PAIFLOW_MODEL_API_KEY"));
    }

    @Test
    void resolveReadsEnvironmentWithoutExposingItInReference() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("PAIFLOW_MODEL_API_KEY", "secret-value");
        SecretReferenceService service = new SecretReferenceService(environment);

        assertEquals("secret-value", service.resolve("env:PAIFLOW_MODEL_API_KEY"));
        assertEquals("env:PAIFLOW_MODEL_API_KEY", service.displayReference("env:PAIFLOW_MODEL_API_KEY"));
    }

    @Test
    void invalidOrMissingReferencesFailClosed() {
        SecretReferenceService service = new SecretReferenceService(new MockEnvironment());

        assertThrows(BusinessException.class, () -> service.normalize("actual-secret"));
        assertThrows(BusinessException.class, () -> service.resolve("env:PAIFLOW_MISSING_API_KEY"));
        assertEquals("", service.displayReference("legacy-plaintext-secret"));
    }

    @Test
    void localSecretFileIsReadOnlyAtServiceStartup(@TempDir Path tempDir) throws IOException {
        Path secretFile = tempDir.resolve(".env.local");
        Files.writeString(secretFile, "PAIFLOW_STARTUP_ONLY_KEY=initial-value\n");
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("PAIFLOW_SECRETS_FILE", secretFile.toString());
        SecretReferenceService service = new SecretReferenceService(environment);

        Files.writeString(secretFile, "PAIFLOW_STARTUP_ONLY_KEY=changed-on-disk\n");

        assertEquals("initial-value", service.resolve("env:PAIFLOW_STARTUP_ONLY_KEY"));
    }
}
