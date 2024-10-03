import io.descoped.secrets.api.SecretManagerClientInitializer;

module io.descoped.secrets.provider.google.rest.api {

    requires java.net.http;

    requires io.descoped.service.provider.api;
    requires io.descoped.secrets.client.api;

    requires com.google.auth;
    requires com.google.auth.oauth2;
    requires com.fasterxml.jackson.databind;

    opens io.descoped.secrets.google.secretmanager.restapi to com.fasterxml.jackson.databind;

    provides SecretManagerClientInitializer with io.descoped.secrets.google.secretmanager.restapi.GoogleSecretManagerClientInitializer;

    exports io.descoped.secrets.google.secretmanager.restapi;
}
