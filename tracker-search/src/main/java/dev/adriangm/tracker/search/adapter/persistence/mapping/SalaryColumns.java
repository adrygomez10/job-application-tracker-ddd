package dev.adriangm.tracker.search.adapter.persistence.mapping;

import dev.adriangm.tracker.search.domain.SalaryRange;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Currency;

public final class SalaryColumns {
    private SalaryColumns()
    {
    }

    public static SalaryRange read(ResultSet row) throws SQLException
    {
        var minimum = row.getBigDecimal("salary_min");
        return minimum == null
            ? null
            : new SalaryRange(
                minimum,
                row.getBigDecimal("salary_max"),
                Currency.getInstance(row.getString("currency")),
                SalaryRange.Period.valueOf(row.getString("salary_period"))
            );
    }

    public static Object[] values(SalaryRange salary)
    {
        return salary == null
            ? new Object[]{null, null, null, null}
            : new Object[]{salary.minimum(), salary.maximum(), salary.currency().getCurrencyCode(),
                salary.period().name()};
    }
}
