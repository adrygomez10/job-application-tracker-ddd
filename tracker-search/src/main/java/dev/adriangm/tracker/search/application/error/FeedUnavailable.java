package dev.adriangm.tracker.search.application.error;

public final class FeedUnavailable extends RuntimeException {
    public FeedUnavailable(String message)
    {
        super(message);
    }

    public FeedUnavailable(String message, Throwable cause)
    {
        super(message, cause);
    }
}
