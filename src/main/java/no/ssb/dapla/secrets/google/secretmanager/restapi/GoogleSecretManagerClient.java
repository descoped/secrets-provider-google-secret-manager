package no.ssb.dapla.secrets.google.secretmanager.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ComputeEngineCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import no.ssb.dapla.secrets.api.SecretManagerClient;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

public class GoogleSecretManagerClient implements SecretManagerClient {

    static final ObjectMapper mapper = new ObjectMapper();
    private final String projectId;
    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    private final AccessToken accessToken;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    // uses compute-engine
    public GoogleSecretManagerClient(String projectId) {
        this.projectId = projectId;
        GoogleCredentials credentials = ComputeEngineCredentials.create();
        GoogleCredentials scopedCredentials = credentials.createScoped("https://www.googleapis.com/auth/cloud-platform");
        this.accessToken = getAccessToken(scopedCredentials);
    }

    // uses service-account
    public GoogleSecretManagerClient(String projectId, String serviceAccountKeyPath) {
        this.projectId = projectId;
        GoogleCredentials credentials = getServiceAccountCredentials(serviceAccountKeyPath);
        GoogleCredentials scopedCredentials = credentials.createScoped("https://www.googleapis.com/auth/cloud-platform");
        this.accessToken = getAccessToken(scopedCredentials);
    }

    GoogleCredentials getServiceAccountCredentials(String serviceAccountKeyPath) {
        try {
            return ServiceAccountCredentials.fromStream(Files.newInputStream(Paths.get(serviceAccountKeyPath), StandardOpenOption.READ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    AccessToken getAccessToken(GoogleCredentials scopedCredentials) {
        try {
            return scopedCredentials.refreshAccessToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] readBytes(String secretName, String secretVersion) {
        if (closed.get()) {
            throw new RuntimeException("Client is closed!");
        }
        try {
            String accessToken = String.format("Bearer %s", this.accessToken.getTokenValue());
            URI uri = URI.create(String.format("https://secretmanager.googleapis.com/v1/projects/%s/secrets/%s/versions/%s:access",
                    projectId, secretName, secretVersion == null ? "latest" : secretVersion));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", accessToken)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException(String.format("Error (%s) reading secret '%s' cause %s", response.statusCode(), secretName, response.body()));
            }

            try {
                JsonNode responseNode = mapper.readTree(response.body());
                if (!responseNode.has("payload")) {
                    throw new RuntimeException("Unable to resolve payload from " + response.body());
                }
                if (!responseNode.get("payload").has("data")) {
                    throw new RuntimeException("Unable to resolve payload.data from " + response.body());
                }
                return responseNode.get("payload").get("data").binaryValue();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String addVersion(String secretName, byte[] secretValue) {
        if (closed.get()) {
            throw new RuntimeException("Client is closed!");
        }
        try {
            HttpResponse<String> checkSecretResponse;
            {
                String accessToken = String.format("Bearer %s", this.accessToken.getTokenValue());
                URI uri = URI.create(String.format("https://secretmanager.googleapis.com/v1/projects/%s/secrets/%s",
                        projectId, secretName));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Authorization", accessToken)
                        .GET()
                        .build();

                checkSecretResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            }
            if (checkSecretResponse.statusCode() == 200) {
                // already exists, do nothing
            } else if (checkSecretResponse.statusCode() == 404) {
                ObjectNode secret = mapper.createObjectNode();
                ObjectNode replication = secret.putObject("replication");
                ObjectNode automatic = replication.putObject("automatic");
                ObjectNode customerManagedEncryption = automatic.putObject("customerManagedEncryption");
                customerManagedEncryption.put("kmsKeyName", "global");

                String accessToken = String.format("Bearer %s", this.accessToken.getTokenValue());
                URI uri = URI.create(String.format("https://secretmanager.googleapis.com/v1/projects/%s/secrets?secretId=%s",
                        URLEncoder.encode(projectId, StandardCharsets.UTF_8),
                        URLEncoder.encode(secretName, StandardCharsets.UTF_8)
                ));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Authorization", accessToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(secret.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new RuntimeException(String.format("Error (%s) creating secret '%s' cause %s", response.statusCode(), secretName, response.body()));
                }
            } else {
                throw new RuntimeException(String.format("Error (%s) checking secret '%s' cause %s", checkSecretResponse.statusCode(), secretName, checkSecretResponse.body()));
            }
            {
                ObjectNode secretVersion = mapper.createObjectNode();
                ObjectNode payload = secretVersion.putObject("payload");
                payload.put("data", secretValue);

                String accessToken = String.format("Bearer %s", this.accessToken.getTokenValue());
                URI uri = URI.create(String.format("https://secretmanager.googleapis.com/v1/projects/%s/secrets/%s:addVersion",
                        URLEncoder.encode(projectId, StandardCharsets.UTF_8),
                        URLEncoder.encode(secretName, StandardCharsets.UTF_8)
                ));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Authorization", accessToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(secretVersion.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new RuntimeException(String.format("Error (%s) creating secret '%s' cause %s", response.statusCode(), secretName, response.body()));
                }

                JsonNode responseSecretVersion = mapper.readTree(response.body());
                String version = responseSecretVersion.get("name").textValue();
                return version;
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        closed.compareAndSet(false, true);
    }
}
