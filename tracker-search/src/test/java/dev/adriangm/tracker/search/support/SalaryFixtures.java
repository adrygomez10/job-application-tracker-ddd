package dev.adriangm.tracker.search.support;

import dev.adriangm.tracker.search.domain.SalaryRange;
import java.math.BigDecimal;
import java.util.Currency;

/** Test data shared by salary and compatibility scenarios. */
public final class SalaryFixtures {
    private SalaryFixtures()
    {
    }

    public static SalaryRange salary(String minimum, String maximum, String currency, SalaryRange.Period period)
    {
        return new SalaryRange(
            new BigDecimal(minimum),
            maximum == null ? null : new BigDecimal(maximum),
            Currency.getInstance(currency),
            period
        );
    }
}
