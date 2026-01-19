package com.taumaturgo.infrastructure.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taumaturgo.application.dto.ProfilePhotoJobStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class WebhookNotifier {
    private final HttpClient client;
    private final ObjectMapper mapper;

    @Inject
    public WebhookNotifier(ObjectMapper mapper) {
        this(HttpClient.newHttpClient(), mapper);
    }

    WebhookNotifier(HttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public void notify(String callbackUrl, ProfilePhotoJobStatus status) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            return;
        }
        try {
            var body = mapper.writeValueAsString(status);
            var request = HttpRequest.newBuilder()
                                     .uri(URI.create(callbackUrl))
                                     .header("Content-Type", "application/json")
                                     .POST(HttpRequest.BodyPublishers.ofString(body))
                                     .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                  .exceptionally(throwable -> {
                      Logger.getLogger(getClass())
                            .warnf(throwable, "Failed to notify callback %s", callbackUrl);
                      return null;
                  });
        } catch (Exception exception) {
            Logger.getLogger(getClass()).warnf(exception, "Failed to build callback request for %s", callbackUrl);
        }
    }
}
