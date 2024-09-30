package no.ssb.dapla.secrets.google.secretmanager.restapi;

import io.descoped.config.DynamicConfiguration;
import io.descoped.config.StoreBasedDynamicConfiguration;
import no.ssb.dapla.secrets.api.SecretManagerClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GoogleSecretManagerRestApiTest {

    @Disabled
    @Test
    public void readGoogleSecret() {
        DynamicConfiguration configuration = new StoreBasedDynamicConfiguration.Builder()
                .propertiesResource("application-override.properties")
                .build();

        Map<String, String> providerConfiguration = Map.of(
                "secrets.provider", "google-secret-manager",
                "secrets.project-id", "ssb-team-dapla",
                "secrets.service-account-key-path", getServiceAccountFile(configuration)
        );

        try (GoogleSecretManagerClient client = (GoogleSecretManagerClient) SecretManagerClient.create(providerConfiguration)) {
            assertEquals("42\n", client.readString("AN_ANSWER"));
            assertArrayEquals("42\n".getBytes(), client.readBytes("AN_ANSWER"));
        }
    }

    String getServiceAccountFile(DynamicConfiguration configuration) {
        String path = configuration.evaluateToString("gcp.service-account.file");
        if (path == null || !Files.isReadable(Paths.get(path))) {
            throw new RuntimeException("Missing 'application-override.properties'-file with required property 'gcp.service-account.file'");
        }
        return path;
    }

    @Disabled
    @Test
    public void writeThenReadSecret() {
        DynamicConfiguration configuration = new StoreBasedDynamicConfiguration.Builder()
                .propertiesResource("application-override.properties")
                .build();

        Map<String, String> providerConfiguration = Map.of(
                "secrets.provider", "google-secret-manager",
                "secrets.project-id", "ssb-team-dapla",
                "secrets.service-account-key-path", getServiceAccountFile(configuration)
        );

        try (SecretManagerClient client = SecretManagerClient.create(providerConfiguration)) {
            assertEquals("1", client.addVersion("question", "42".getBytes(StandardCharsets.UTF_8)));
            assertEquals("42", new String(client.readBytes("question"), StandardCharsets.UTF_8));
            assertEquals("2", client.addVersion("question", "43".getBytes(StandardCharsets.UTF_8)));
            assertEquals("43", new String(client.readBytes("question"), StandardCharsets.UTF_8));
            assertEquals("42", new String(client.readBytes("question", "1"), StandardCharsets.UTF_8));
            assertEquals("43", new String(client.readBytes("question", "2"), StandardCharsets.UTF_8));
        }
    }

}
