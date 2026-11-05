package dev.adriangm.tracker.notifications.adapter.web;

import dev.adriangm.tracker.notifications.application.collection.SendReminder;
import dev.adriangm.tracker.notifications.application.port.NotificationChannel;
import dev.adriangm.tracker.notifications.application.port.NotificationQueue;
import dev.adriangm.tracker.reminders.api.ReminderNotifications;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificationController {
    private final NotificationQueue store;
    private final ReminderNotifications reminders;
    private final SendReminder send;
    private final NotificationChannel channel;

    public NotificationController(
        NotificationQueue store,
        ReminderNotifications reminders,
        SendReminder send,
        NotificationChannel channel
    )
    {
        this.store = store;
        this.reminders = reminders;
        this.send = send;
        this.channel = channel;
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    String stale(jakarta.servlet.http.HttpServletResponse response, Model model)
    {
        response.setStatus(400);
        model.addAttribute("errorStatus", 400);
        model.addAttribute("errorCode", "invalid");
        return "error";
    }

    @GetMapping("/notifications")
    String show(Model model, @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page)
    {
        if (page < 0 || page > 1000000) {
            throw new IllegalArgumentException("Invalid page");
        }
        model.addAttribute("page", page);
        model.addAttribute("deliveries", store.history(page));
        model.addAttribute("due", reminders.active());
        model.addAttribute("configured", channel.configured());
        return "notifications";
    }

    @PostMapping("/notifications/{id}/send")
    String send(@PathVariable UUID id, RedirectAttributes flash)
    {
        flash.addFlashAttribute("deliveryResult", send.execute(id));
        return "redirect:/notifications";
    }
}
