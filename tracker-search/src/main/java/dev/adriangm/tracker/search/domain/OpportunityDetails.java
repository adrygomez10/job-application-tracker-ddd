package dev.adriangm.tracker.search.domain;

import java.net.URI;

/** Hiring company and agency are distinct roles; an unknown agency remains empty. */
public record OpportunityDetails(
    String employer,
    String agency,
    String role,
    String location,
    WorkMode workMode,
    SalaryRange salary,
    String url
) {
    public OpportunityDetails
    {
        employer = Text.required(employer, 160);
        agency = Text.optional(agency, 160);
        role = Text.required(role, 180);
        location = Text.optional(location, 160);
        url = Text.optional(url, 2000);
        if (workMode == null) {
            throw new IllegalArgumentException("Work mode is required");
        }
        if (!url.isEmpty() && !validUrl(url)) {
            throw new IllegalArgumentException("Expected an HTTP or HTTPS job URL");
        }
    }

    private static boolean validUrl(String url)
    {
        URI address = URI.create(url);
        return address.getHost() != null && ("https".equals(address.getScheme()) || "http".equals(address.getScheme()));
    }
}
