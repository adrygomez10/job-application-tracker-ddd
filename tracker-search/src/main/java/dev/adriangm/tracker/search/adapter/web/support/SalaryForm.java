package dev.adriangm.tracker.search.adapter.web.support;

import dev.adriangm.tracker.search.domain.SalaryRange;
import java.math.BigDecimal;
import java.util.Currency;

public record SalaryForm(BigDecimal minimum, BigDecimal maximum, String currency, String period) {
    public SalaryRange toDomain()
    {
        if (minimum == null) {
            if (maximum != null) {
                throw new IllegalArgumentException("A maximum requires a minimum");
            }
            return null;
        }
        return new SalaryRange(minimum, maximum, Currency.getInstance(currency), SalaryRange.Period.valueOf(period));
    }
}
