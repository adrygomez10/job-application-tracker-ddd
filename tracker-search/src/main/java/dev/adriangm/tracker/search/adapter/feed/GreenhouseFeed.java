package dev.adriangm.tracker.search.adapter.feed;

import dev.adriangm.tracker.search.application.error.FeedUnavailable;
import dev.adriangm.tracker.search.application.port.OfferFeed;
import dev.adriangm.tracker.search.domain.WorkMode;
import dev.adriangm.tracker.search.domain.opportunity.OpportunityDetails;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Anti-corruption layer: no remote HTML, guessed salary, or inferred remote-work policy. */
public final class GreenhouseFeed implements OfferFeed {
    private final URI endpoint;
    private final String employer;
    private final String board;
    private final HttpClient client;

    public GreenhouseFeed(URI endpoint, String employer, String board)
    {
        this.endpoint = endpoint;
        this.employer = employer;
        this.board = board;
        this.client = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    @Override
    public List<ImportedOffer> fetch()
    {
        var request = HttpRequest
            .newBuilder(endpoint)
            .timeout(Duration.ofSeconds(8))
            .header("Accept", "application/json")
            .GET()
            .build();
        try {
            var response = client.send(request, new LimitedJsonBody());
            if (response.statusCode() != 200) {
                throw new FeedUnavailable("The job provider rejected the request");
            }
            return decode(response.body());
        } catch (IOException failure) {
            throw new FeedUnavailable("The job provider could not be read", failure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new FeedUnavailable("The job provider request was interrupted", interrupted);
        }
    }

    private List<ImportedOffer> decode(byte[] body)
    {
        try {
            return translate(JsonMapper.builder().build().readTree(body));
        } catch (tools.jackson.core.JacksonException | IllegalArgumentException invalid) {
            throw new FeedUnavailable("The job provider returned invalid data", invalid);
        }
    }

    private List<ImportedOffer> translate(JsonNode response)
    {
        if (response == null) {
            throw new FeedUnavailable("Empty provider response");
        }
        JsonNode jobs = response.path("jobs");
        if (!jobs.isArray() || jobs.size() > 1000) {
            throw new FeedUnavailable("Expected at most 1000 job posts");
        }
        List<ImportedOffer> offers = new ArrayList<>();
        for (JsonNode job : jobs) {
            offers.add(translateJob(job));
        }
        return List.copyOf(offers);
    }

    private ImportedOffer translateJob(JsonNode job)
    {
        long externalId = job.path("id").asLong(0);
        if (!job.path("id").isIntegralNumber() || externalId <= 0) {
            throw new FeedUnavailable("Missing job post identity");
        }
        var details = new OpportunityDetails(
            employer,
            "",
            job.path("title").asString(""),
            job.path("location").path("name").asString(""),
            WorkMode.UNSPECIFIED,
            null,
            job.path("absolute_url").asString("")
        );
        return new ImportedOffer("greenhouse:" + board + ":" + externalId, details);
    }
}
