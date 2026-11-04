package dev.adriangm.tracker.search.adapter.web;

import dev.adriangm.tracker.search.application.ImportFeed;
import dev.adriangm.tracker.search.application.ManageOpportunities;
import dev.adriangm.tracker.search.application.SearchQueries;
import dev.adriangm.tracker.search.domain.Opportunity;
import dev.adriangm.tracker.search.domain.OpportunityFit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SearchController {
    private final SearchQueries store;
    private final ManageOpportunities manage;
    private final ImportFeed feed;

    public SearchController(SearchQueries store, ManageOpportunities manage, ImportFeed feed)
    {
        this.store = store;
        this.manage = manage;
        this.feed = feed;
    }

    @GetMapping("/opportunities")
    String list(
        Model model,
        @ModelAttribute("criteria") dev.adriangm.tracker.search.application.OpportunityCriteria criteria,
        @RequestParam(defaultValue = "") String processQuery,
        @RequestParam(defaultValue = "0") int processPage
    )
    {
        model.addAttribute("criteria", criteria);
        model.addAttribute("hasNext", store.hasNext(criteria));
        model.addAttribute("processQuery", processQuery);
        model.addAttribute("processPage", processPage);
        var processes = manage.linkableProcesses(processQuery, processPage);
        model.addAttribute("processHasNext", processes.hasNext());
        var profile = store.profile();
        var fit = new OpportunityFit();
        model
            .addAttribute(
                "items",
                store
                    .search(criteria)
                    .stream()
                    .map(item -> new Assessed(item, fit.assess(item.details(), profile), UUID.randomUUID()))
                    .toList()
            );
        model.addAttribute("modes", dev.adriangm.tracker.search.domain.WorkMode.values());
        model.addAttribute("newId", UUID.randomUUID());
        model.addAttribute("processes", processes.items());
        return "opportunities";
    }

    @PostMapping("/opportunities")
    String create(@ModelAttribute OpportunityForm form)
    {
        if (form.id() == null) {
            throw new IllegalArgumentException("Opportunity identity is required");
        }
        manage.capture(form.id(), form.details());
        return "redirect:/opportunities";
    }

    @PostMapping("/opportunities/{id}/revise")
    String revise(@PathVariable UUID id, @ModelAttribute OpportunityForm form)
    {
        if (form.version() == null) {
            throw new IllegalArgumentException("Version is required");
        }
        manage.revise(id, form.version(), form.details());
        return "redirect:/opportunities";
    }

    @PostMapping("/opportunities/{id}/start")
    String start(@PathVariable UUID id, @RequestParam UUID processId, @RequestParam String channel)
    {
        UUID registered = manage.start(id, processId, channel);
        return "redirect:/applications/" + registered;
    }

    @PostMapping("/opportunities/{id}/relate")
    String relate(@PathVariable UUID id, @RequestParam String processKey)
    {
        String[] parts = processKey.split(":", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid process reference");
        }
        manage.relate(id, UUID.fromString(parts[0]), Long.parseLong(parts[1]));
        return "redirect:/opportunities";
    }

    @PostMapping("/opportunities/import-greenhouse")
    String importGreenhouse(RedirectAttributes flash)
    {
        var result = feed.execute();
        flash.addFlashAttribute("importResult", result);
        return "redirect:/opportunities";
    }

    public record Assessed(Opportunity opportunity, List<String> reasons, UUID processId) {
    }
}
