package dev.adriangm.tracker.notifications.adapter.http;

import dev.adriangm.tracker.notifications.application.NotificationChannel;
import dev.adriangm.tracker.notifications.api.NotificationMessage;
import dev.adriangm.tracker.notifications.domain.DeliveryState;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Generic content only: the provider never receives employer, notes, profile, or interview data.
 */
public final class NtfyChannel implements NotificationChannel {
    private final URI topic;
    private final String token;
    private final HttpClient client;

    public NtfyChannel(URI topic, String token)
    {
        this.topic = topic;
        this.token = token;
        this.client = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    @Override
    public boolean configured()
    {
        return true;
    }

    @Override
    public DeliveryState send(NotificationMessage message)
    {
        var request = HttpRequest
            .newBuilder(topic)
            .timeout(Duration.ofSeconds(8))
            .header("Content-Type", "text/plain; charset=utf-8")
            .header("Title", "Job Tracker")
            .POST(HttpRequest.BodyPublishers.ofString(message.text()));
        if (!token.isBlank()) {
            request.header("Authorization", "Bearer " + token);
        }

        try {
            int status = client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
            return classify(status);
        } catch (IOException failure) {
            return DeliveryState.UNCERTAIN;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return DeliveryState.UNCERTAIN;
        }
    }

    private static DeliveryState classify(int status)
    {
        if (status >= 200 && status < 300) {
            return DeliveryState.SENT;
        }
        return status >= 400 && status < 500 ? DeliveryState.REJECTED : DeliveryState.UNCERTAIN;
    }
}
