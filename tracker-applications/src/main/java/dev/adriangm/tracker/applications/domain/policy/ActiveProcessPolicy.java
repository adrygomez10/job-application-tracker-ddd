package dev.adriangm.tracker.applications.domain.policy;

import dev.adriangm.tracker.applications.domain.RuleViolation;

/** Confirmed opportunity identity is the scope; archiving does not close a process. */
public final class ActiveProcessPolicy {
    public void requireAvailable(long activeProcesses)
    {
        if (activeProcesses != 0) {
            throw new RuleViolation("activeOpportunity");
        }
    }
}
