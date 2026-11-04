package dev.adriangm.tracker.search.domain;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/** Gross salary; an absent maximum means no published upper bound. */
public record SalaryRange(BigDecimal minimum, BigDecimal maximum, Currency currency, Period period) {
    public enum Period {
        YEAR,
        MONTH
    }

    public SalaryRange
    {
        Objects.requireNonNull(minimum);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(period);

        minimum = minimum.stripTrailingZeros();
        maximum = maximum == null ? null : maximum.stripTrailingZeros();

        if (minimum.signum() < 0 || invalidAmount(minimum)) {
            throw new IllegalArgumentException("Invalid salary minimum");
        }
        if (maximum != null && (maximum.compareTo(minimum) < 0 || invalidAmount(maximum))) {
            throw new IllegalArgumentException("Invalid salary maximum");
        }
    }

    private static boolean invalidAmount(BigDecimal amount)
    {
        return amount.scale() > 2 || amount.precision() - amount.scale() > 12;
    }

    public boolean comparableTo(SalaryRange other)
    {
        return currency.equals(other.currency) && period == other.period;
    }

    public String label()
    {
        return minimum.toPlainString() + "–" + (maximum == null ? "…" : maximum.toPlainString()) + " " + currency + "/"
            + period;
    }
}
