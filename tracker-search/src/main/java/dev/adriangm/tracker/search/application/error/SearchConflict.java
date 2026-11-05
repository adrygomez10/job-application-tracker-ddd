package dev.adriangm.tracker.search.application.error;

public final class SearchConflict extends RuntimeException {
    public SearchConflict(Throwable cause)
    {
        super("The record changed or its identity is already in use", cause);
    }

    public SearchConflict()
    {
        super("The record changed or its identity is already in use");
    }
}
