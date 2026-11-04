package dev.adriangm.tracker.applications.adapter.persistence;

import java.util.List;
import java.util.function.Function;

/** Keep unchanged embeddables attached; Hibernate can update only changed list positions. */
final class CollectionChanges {
    private CollectionChanges()
    {
    }

    static <R, D> void synchronize(List<R> rows, List<D> values, Function<R, D> read, Function<D, R> write)
    {
        int common = Math.min(rows.size(), values.size());
        for (int index = 0; index < common; index++) {
            if (!read.apply(rows.get(index)).equals(values.get(index))) {
                rows.set(index, write.apply(values.get(index)));
            }
        }
        while (rows.size() > values.size()) {
            rows.removeLast();
        }
        for (int index = common; index < values.size(); index++) {
            rows.add(write.apply(values.get(index)));
        }
    }
}
