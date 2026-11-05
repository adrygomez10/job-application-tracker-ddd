package dev.adriangm.tracker.search.application.query;

import dev.adriangm.tracker.search.domain.WorkMode;

public record OpportunityCriteria(String employer, String role, String mode, String source, Integer page) {
    public OpportunityCriteria
    {
        page = page == null ? 0 : page;
        employer = normalize(employer);
        role = normalize(role);
        mode = normalize(mode);
        source = normalize(source);
        if (!mode.isEmpty()) {
            WorkMode.valueOf(mode);
        }
        if (page < 0 || page > 1000000) {
            throw new IllegalArgumentException("Invalid page");
        }
    }

    private static String normalize(String value)
    {
        String result = value == null ? "" : value.strip();
        if (result.length() > 180) {
            throw new IllegalArgumentException("Search text too long");
        }
        return result;
    }
}
