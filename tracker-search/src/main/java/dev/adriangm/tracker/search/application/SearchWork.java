package dev.adriangm.tracker.search.application;

import java.util.function.Supplier;

public interface SearchWork {
    <T> T required(Supplier<T> action);
}
