package dev.adriangm.tracker.search.configuration;

import dev.adriangm.tracker.applications.api.ApplicationsApi;
import dev.adriangm.tracker.search.adapter.feed.GreenhouseFeed;
import dev.adriangm.tracker.search.application.error.FeedUnavailable;
import dev.adriangm.tracker.search.application.feed.ImportFeed;
import dev.adriangm.tracker.search.application.opportunity.ManageOpportunities;
import dev.adriangm.tracker.search.application.port.OfferFeed;
import dev.adriangm.tracker.search.application.port.OpportunityRepository;
import dev.adriangm.tracker.search.application.port.SearchWork;
import java.net.URI;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class SearchConfiguration {
    @Bean
    SearchWork searchWork(PlatformTransactionManager manager)
    {
        var transaction = new TransactionTemplate(manager);
        return new SearchWork() {
            @Override
            public <T> T required(Supplier<T> action)
            {
                return transaction.execute(status -> action.get());
            }
        };
    }

    @Bean
    ManageOpportunities manageOpportunities(OpportunityRepository store, SearchWork work, ApplicationsApi api)
    {
        return new ManageOpportunities(store, work, api);
    }

    @Bean
    OfferFeed offerFeed(
        @Value("${tracker.greenhouse.board:}") String board,
        @Value("${tracker.greenhouse.employer:}") String employer
    )
    {
        if (board.isBlank()) {
            return new OfferFeed() {
                @Override
                public boolean configured()
                {
                    return false;
                }

                @Override
                public java.util.List<ImportedOffer> fetch()
                {
                    throw new FeedUnavailable("No Greenhouse board is configured");
                }
            };
        }
        if (!board.matches("[a-zA-Z0-9_-]{1,80}") || employer.isBlank()) {
            throw new IllegalArgumentException("Invalid Greenhouse board configuration");
        }
        return new GreenhouseFeed(
            URI.create("https://boards-api.greenhouse.io/v1/boards/" + board + "/jobs"),
            employer,
            board
        );
    }

    @Bean
    ImportFeed importFeed(
        OfferFeed feed,
        OpportunityRepository store,
        SearchWork work,
        dev.adriangm.tracker.search.application.port.ProfileRepository profiles,
        dev.adriangm.tracker.search.application.port.FeedRuns runs,
        dev.adriangm.tracker.notifications.api.NotificationRequests notifications,
        java.time.Clock clock
    )
    {
        return new ImportFeed(feed, store, work, profiles, runs, notifications, clock);
    }

    @Bean
    dev.adriangm.tracker.search.application.profile.SaveProfile saveProfile(
        dev.adriangm.tracker.search.application.port.ProfileRepository profiles
    )
    {
        return new dev.adriangm.tracker.search.application.profile.SaveProfile(profiles);
    }
}
