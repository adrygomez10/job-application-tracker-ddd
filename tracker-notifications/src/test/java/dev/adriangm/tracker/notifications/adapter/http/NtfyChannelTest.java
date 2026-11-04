package dev.adriangm.tracker.notifications.adapter.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import dev.adriangm.tracker.notifications.domain.DeliveryState;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NtfyChannelTest {
    @Test
    void publishesOnlyGenericContentAndClassifiesOutcomes() throws Exception
    {
        var body = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/topic", exchange -> {
                body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                int status = Integer.parseInt(exchange.getRequestURI().getQuery());
                if (status != 0) {
                    exchange.sendResponseHeaders(status, -1);
                }
                exchange.close();
            });
            server.start();
            assertThat(channel(server, 200).send(message())).isEqualTo(DeliveryState.SENT);
            assertThat(body.get()).isEqualTo("You have a pending reminder in Job Tracker.");
            assertThat(channel(server, 401).send(message())).isEqualTo(DeliveryState.REJECTED);
            assertThat(channel(server, 503).send(message())).isEqualTo(DeliveryState.UNCERTAIN);
            assertThat(channel(server, 0).send(message())).isEqualTo(DeliveryState.UNCERTAIN);
            var summary = dev.adriangm.tracker.notifications.api.NotificationMessage
                .summary(java.util.UUID.randomUUID(), 7, java.time.Instant.EPOCH);
            assertThat(channel(server, 200).send(summary)).isEqualTo(DeliveryState.SENT);
            assertThat(body.get()).isEqualTo("Job Tracker found 7 new opportunities.");
        } finally {
            server.stop(0);
        }
    }

    private static dev.adriangm.tracker.notifications.api.NotificationMessage message()
    {
        return dev.adriangm.tracker.notifications.api.NotificationMessage
            .reminder(java.util.UUID.randomUUID(), "TASK", java.time.Instant.EPOCH, false, java.time.Instant.EPOCH);
    }

    private static NtfyChannel channel(HttpServer server, int status)
    {
        return new NtfyChannel(
            URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/topic?" + status),
            "test-token"
        );
    }
}
