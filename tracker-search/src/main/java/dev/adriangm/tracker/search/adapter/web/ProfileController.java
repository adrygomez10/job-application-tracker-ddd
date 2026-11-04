package dev.adriangm.tracker.search.adapter.web;

import dev.adriangm.tracker.search.application.SaveProfile;
import dev.adriangm.tracker.search.application.SearchQueries;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ProfileController {
    private final SearchQueries store;
    private final SaveProfile save;

    public ProfileController(SearchQueries store, SaveProfile save)
    {
        this.store = store;
        this.save = save;
    }

    @GetMapping("/profile")
    String show(Model model)
    {
        model.addAttribute("modes", dev.adriangm.tracker.search.domain.WorkMode.values());
        model.addAttribute("profile", store.profile());
        return "profile";
    }

    @PostMapping("/profile")
    String update(@ModelAttribute ProfileForm form)
    {
        save.execute(form.profile());
        return "redirect:/profile";
    }
}
