import no.ssb.dapla.secrets.api.SecretManagerClientInitializer;

module secrets.provider.google.rest.api {

    requires java.net.http;

    requires io.descoped.service.provider.api;
    requires secrets.client.api;

    requires com.google.auth;
    requires com.google.auth.oauth2;
    requires com.fasterxml.jackson.databind;

    opens no.ssb.dapla.secrets.google.secretmanager.restapi to com.fasterxml.jackson.databind;

    provides SecretManagerClientInitializer with no.ssb.dapla.secrets.google.secretmanager.restapi.GoogleSecretManagerClientInitializer;

    exports no.ssb.dapla.secrets.google.secretmanager.restapi;
}
