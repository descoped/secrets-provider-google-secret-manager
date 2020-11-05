package no.ssb.dapla.secrets.google.secretmanager.restapi;

import no.ssb.dapla.secrets.api.SecretManagerClient;
import no.ssb.dapla.secrets.api.SecretManagerClientInitializer;
import no.ssb.service.provider.api.ProviderName;

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
                "secrets.projectId"
        );
    }

    @Override
    public SecretManagerClient initialize(Map<String, String> map) {
        String projectId = map.get("secrets.projectId");
        String serviceAccountKeyPath = map.get("secrets.serviceAccountKeyPath");
        return serviceAccountKeyPath == null ? new GoogleSecretManagerClient(projectId) : new GoogleSecretManagerClient(projectId, serviceAccountKeyPath);
    }
}
