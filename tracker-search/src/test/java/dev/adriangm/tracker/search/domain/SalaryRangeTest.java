package dev.adriangm.tracker.search.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class SalaryRangeTest {
    @Test
    void normalizesDecimalScaleForValueEquality()
    {
        assertThat(salary("40000.00", null, "EUR", SalaryRange.Period.YEAR))
            .isEqualTo(salary("40000", null, "EUR", SalaryRange.Period.YEAR));
    }

    @Test
    void rejectsNegativeInvertedAndFractionalCentRanges()
    {
        assertThatThrownBy(() -> salary("-1", null, "EUR", SalaryRange.Period.YEAR))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> salary("50000", "40000", "EUR", SalaryRange.Period.YEAR))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> salary("1.001", null, "EUR", SalaryRange.Period.YEAR))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void neverConvertsCurrencyOrPeriodImplicitly()
    {
        var annual = salary("40000", "50000", "EUR", SalaryRange.Period.YEAR);
        assertThat(annual.comparableTo(salary("40000", null, "USD", SalaryRange.Period.YEAR))).isFalse();
        assertThat(annual.comparableTo(salary("3000", null, "EUR", SalaryRange.Period.MONTH))).isFalse();
        assertThat(annual.comparableTo(salary("42000", null, "EUR", SalaryRange.Period.YEAR))).isTrue();
    }

    static SalaryRange salary(String minimum, String maximum, String currency, SalaryRange.Period period)
    {
        return new SalaryRange(
            new BigDecimal(minimum),
            maximum == null ? null : new BigDecimal(maximum),
            Currency.getInstance(currency),
            period
        );
    }
}
