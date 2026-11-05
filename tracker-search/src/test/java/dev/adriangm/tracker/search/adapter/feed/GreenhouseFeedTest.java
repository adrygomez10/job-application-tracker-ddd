package dev.adriangm.tracker.search.adapter.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import dev.adriangm.tracker.search.application.error.FeedUnavailable;
import dev.adriangm.tracker.search.domain.WorkMode;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GreenhouseFeedTest {
    private HttpServer server;
    private int status = 200;
    private String body;

    @BeforeEach
    void startServer() throws Exception
    {
        body = """
            {"jobs":[{"id":42,"title":"Java Developer","location":{"name":"Madrid"},
              "absolute_url":"https://example.test/jobs/42","content":"<script>bad</script>"}]}
            """;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jobs", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer()
    {
        server.stop(0);
    }

    @Test
    void translatesPostIdentityWithoutGuessingWorkModeOrSalary()
    {
        var offers = feed().fetch();
        assertThat(offers).hasSize(1);
        assertThat(offers.getFirst().sourceKey()).isEqualTo("greenhouse:example:42");
        assertThat(offers.getFirst().details().employer()).isEqualTo("Example employer");
        assertThat(offers.getFirst().details().workMode()).isEqualTo(WorkMode.UNSPECIFIED);
        assertThat(offers.getFirst().details().salary()).isNull();
    }

    @Test
    void malformedBodyAndProviderRejectionAreFailures()
    {
        body = """
            {"jobs":[{"title":"Missing identity"}]}
            """;
        assertThatThrownBy(() -> feed().fetch()).isInstanceOf(FeedUnavailable.class);
        status = 429;
        assertThatThrownBy(() -> feed().fetch()).isInstanceOf(FeedUnavailable.class);
    }

    @Test
    void rejectsOversizedBodiesBeforeParsing()
    {
        body = " ".repeat(2 * 1024 * 1024 + 1);
        assertThatThrownBy(() -> feed().fetch()).isInstanceOf(FeedUnavailable.class);
    }

    private GreenhouseFeed feed()
    {
        return new GreenhouseFeed(
            URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/jobs"),
            "Example employer",
            "example"
        );
    }
}
