package dev.adriangm.tracker.search.application;

import dev.adriangm.tracker.search.domain.SearchProfile;

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
