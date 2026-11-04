package dev.adriangm.tracker.search.application;

import dev.adriangm.tracker.search.domain.SearchProfile;

public interface ProfileRepository {
    SearchProfile load();

    void save(SearchProfile profile);
}
