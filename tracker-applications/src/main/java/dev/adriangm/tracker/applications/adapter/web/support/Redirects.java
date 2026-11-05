package dev.adriangm.tracker.applications.adapter.web.support;

import java.util.UUID;

/** Post/Redirect/Get: after a successful change, reloading the page does not repeat it. */
public final class Redirects {
    private Redirects()
    {
    }

    public static String toDetail(UUID id)
    {
        return "redirect:/applications/" + id;
    }
}
