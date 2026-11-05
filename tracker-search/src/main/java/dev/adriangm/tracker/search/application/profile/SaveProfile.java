package dev.adriangm.tracker.search.application.profile;

import dev.adriangm.tracker.search.application.port.ProfileRepository;
import dev.adriangm.tracker.search.domain.profile.SearchProfile;

public final class SaveProfile {
    private final ProfileRepository profiles;

    public SaveProfile(ProfileRepository profiles)
    {
        this.profiles = profiles;
    }

    public void execute(SearchProfile profile)
    {
        profiles.save(profile);
    }
}
