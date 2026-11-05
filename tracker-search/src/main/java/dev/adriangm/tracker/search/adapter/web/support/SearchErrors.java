package dev.adriangm.tracker.search.adapter.web.support;

import dev.adriangm.tracker.search.application.error.FeedUnavailable;
import dev.adriangm.tracker.search.application.error.SearchConflict;
import jakarta.servlet.http.HttpServletResponse;
import java.util.NoSuchElementException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackages = "dev.adriangm.tracker.search.adapter.web")
public class SearchErrors {
    @ExceptionHandler({IllegalArgumentException.class, java.time.DateTimeException.class})
    String invalid(HttpServletResponse response, Model model)
    {
        return error(400, "invalid", response, model);
    }

    @ExceptionHandler(SearchConflict.class)
    String conflict(HttpServletResponse response, Model model)
    {
        return error(409, "conflict", response, model);
    }

    @ExceptionHandler(NoSuchElementException.class)
    String missing(HttpServletResponse response, Model model)
    {
        return error(404, "missing", response, model);
    }

    @ExceptionHandler(FeedUnavailable.class)
    String provider(HttpServletResponse response, Model model)
    {
        return error(502, "provider", response, model);
    }

    private static String error(int status, String code, HttpServletResponse response, Model model)
    {
        response.setStatus(status);
        model.addAttribute("errorStatus", status);
        model.addAttribute("errorCode", code);
        return "error";
    }
}
