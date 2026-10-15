package com.iflytek.astron.console.toolkit.service.secret;

import com.iflytek.astron.console.commons.constant.ResponseEnum;
import com.iflytek.astron.console.commons.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SecretReferenceService {
    private static final String PREFIX = "env:";
    private static final Pattern ENV_NAME = Pattern.compile("[A-Z][A-Z0-9_]{2,127}");
    private static final String SECRETS_FILE_ENV = "PAIFLOW_SECRETS_FILE";

    private final Environment environment;
    private final Map<String, String> localSecrets;

    public SecretReferenceService(Environment environment) {
        this.environment = environment;
        this.localSecrets = Collections.unmodifiableMap(loadLocalSecrets());
    }

    public String normalize(String value) {
        if (StringUtils.isBlank(value)) {
            throw invalidReference();
        }
        String normalized = value.trim();
        if (!normalized.startsWith(PREFIX)) {
            normalized = PREFIX + normalized;
        }
        variableName(normalized);
        return normalized;
    }

    public String resolve(String reference) {
        String variableName = variableName(reference);
        String value = environment.getProperty(variableName);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }
        value = localSecrets.get(variableName);
        if (StringUtils.isBlank(value)) {
            throw new BusinessException(
                    ResponseEnum.RESPONSE_FAILED,
                    "Secret environment variable is not configured: " + variableName);
        }
        return value;
    }

    public boolean isReference(String value) {
        if (StringUtils.isBlank(value) || !value.startsWith(PREFIX)) {
            return false;
        }
        try {
            variableName(value);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    public String displayReference(String value) {
        return isReference(value) ? value : StringUtils.EMPTY;
    }

    private String variableName(String reference) {
        if (StringUtils.isBlank(reference) || !reference.startsWith(PREFIX)) {
            throw invalidReference();
        }
        String variableName = reference.substring(PREFIX.length());
        if (!ENV_NAME.matcher(variableName).matches()) {
            throw invalidReference();
        }
        return variableName;
    }

    private Map<String, String> loadLocalSecrets() {
        Map<String, String> secrets = new HashMap<>();
        for (Path candidate : secretFileCandidates()) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try {
                for (String line : Files.readAllLines(candidate)) {
                    SecretEntry entry = parseLine(line);
                    if (entry != null) {
                        secrets.putIfAbsent(entry.name(), entry.value());
                    }
                }
            } catch (IOException e) {
                throw new BusinessException(
                        ResponseEnum.RESPONSE_FAILED,
                        "Failed to read local secrets file: " + candidate);
            }
        }
        return secrets;
    }

    private List<Path> secretFileCandidates() {
        List<Path> candidates = new ArrayList<>();
        String configured = environment.getProperty(SECRETS_FILE_ENV);
        if (StringUtils.isNotBlank(configured)) {
            candidates.add(Path.of(configured).toAbsolutePath().normalize());
        }
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 8 && current != null; i++) {
            candidates.add(current.resolve(".env.local"));
            current = current.getParent();
        }
        return candidates;
    }

    private SecretEntry parseLine(String line) {
        String normalized = line.trim();
        if (normalized.isEmpty() || normalized.startsWith("#")) {
            return null;
        }
        if (normalized.startsWith("export ")) {
            normalized = normalized.substring(7).trim();
        }
        int separator = normalized.indexOf('=');
        if (separator <= 0) {
            return null;
        }
        String name = normalized.substring(0, separator).trim();
        if (!ENV_NAME.matcher(name).matches()) {
            return null;
        }
        String value = normalized.substring(separator + 1).trim();
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
            value = value.substring(1, value.length() - 1);
        }
        return new SecretEntry(name, value);
    }

    private BusinessException invalidReference() {
        return new BusinessException(
                ResponseEnum.RESPONSE_FAILED,
                "Secret must use an environment reference such as env:PAIFLOW_DEEPSEEK_API_KEY");
    }

    private record SecretEntry(String name, String value) {}
}
