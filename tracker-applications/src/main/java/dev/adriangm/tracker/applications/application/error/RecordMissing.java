package dev.adriangm.tracker.applications.application.error;

public final class RecordMissing extends RuntimeException {
    public RecordMissing()
    {
        super("Application not found.");
    }
}
