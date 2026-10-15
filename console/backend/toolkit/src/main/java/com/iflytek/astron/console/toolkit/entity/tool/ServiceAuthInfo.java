package com.iflytek.astron.console.toolkit.entity.tool;

import lombok.Data;

@Data
public class ServiceAuthInfo {
    String location;
    String parameterName;
    String secretRef;
    String serviceToken;

    public String getSecretReference() {
        return secretRef != null && !secretRef.isBlank() ? secretRef : serviceToken;
    }
}
