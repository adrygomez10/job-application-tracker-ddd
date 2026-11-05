package dev.adriangm.tracker.search.adapter.web.integration;

import dev.adriangm.tracker.search.application.port.FeedRuns;
import dev.adriangm.tracker.search.application.port.OfferFeed;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class IntegrationController {
    private final FeedRuns runs;
    private final OfferFeed feed;

    public IntegrationController(FeedRuns runs, OfferFeed feed)
    {
        this.runs = runs;
        this.feed = feed;
    }

    @GetMapping("/integrations")
    String show(@RequestParam(defaultValue = "0") int page, Model model)
    {
        if (page < 0 || page > 1000000) {
            throw new IllegalArgumentException("Invalid page");
        }
        model.addAttribute("runs", runs.history(page));
        model.addAttribute("page", page);
        model.addAttribute("configured", feed.configured());
        return "integrations";
    }
}
