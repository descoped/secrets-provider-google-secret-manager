import no.ssb.dapla.secrets.api.SecretManagerClientInitializer;
import no.ssb.dapla.secrets.google.secretmanager.restapi.GoogleSecretManagerClientInitializer;

module dapla.secrets.provider.google.rest.api {

    requires java.net.http;

    requires no.ssb.service.provider.api;
    requires dapla.secrets.client.api;

    requires com.google.auth;
    requires com.google.auth.oauth2;
    requires com.fasterxml.jackson.databind;

    opens no.ssb.dapla.secrets.google.secretmanager.restapi to com.fasterxml.jackson.databind;

    provides SecretManagerClientInitializer with GoogleSecretManagerClientInitializer;

    exports no.ssb.dapla.secrets.google.secretmanager.restapi;
}
