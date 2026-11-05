package dev.adriangm.tracker.search.application.port;

import dev.adriangm.tracker.search.domain.profile.SearchProfile;

public interface ProfileRepository {
    SearchProfile load();

    void save(SearchProfile profile);
}
