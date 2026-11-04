package dev.adriangm.tracker.search.domain;

final class Text {
    private Text()
    {
    }

    static String required(String text, int limit)
    {
        String value = optional(text, limit);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Required text is empty");
        }
        return value;
    }

    static String optional(String text, int limit)
    {
        String value = text == null ? "" : text.strip();
        if (value.length() > limit) {
            throw new IllegalArgumentException("Text exceeds maximum length");
        }
        return value;
    }
}
