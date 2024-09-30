package io.descoped.secrets.google.secretmanager.restapi;

import io.descoped.secrets.api.SecretManagerClient;
import io.descoped.secrets.api.SecretManagerClientInitializer;
import io.descoped.service.provider.api.ProviderName;

import java.util.Map;
import java.util.Set;

@ProviderName("google-secret-manager")
public class GoogleSecretManagerClientInitializer implements SecretManagerClientInitializer {

    @Override
    public String providerId() {
        return "google-secret-manager";
    }

    @Override
    public Set<String> configurationKeys() {
        return Set.of(
                "secrets.project-id"
        );
    }

    @Override
    public SecretManagerClient initialize(Map<String, String> map) {
        String projectId = map.get("secrets.project-id");
        String serviceAccountKeyPath = map.get("secrets.service-account-key-path");
        return serviceAccountKeyPath == null ? new GoogleSecretManagerClient(projectId) : new GoogleSecretManagerClient(projectId, serviceAccountKeyPath);
    }
}
