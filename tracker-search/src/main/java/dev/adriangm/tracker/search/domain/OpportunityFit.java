package dev.adriangm.tracker.search.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Explainable hints, never an automatic rejection or a currency conversion. */
public final class OpportunityFit {
    public List<String> assess(OpportunityDetails offer, SearchProfile profile)
    {
        List<String> reasons = new ArrayList<>();
        if (profile.preferredMode() != WorkMode.UNSPECIFIED && offer.workMode() != profile.preferredMode()) {
            reasons.add("Revisar modalidad");
        }
        if (
            !profile.location().isEmpty()
                && !offer.location().toLowerCase(Locale.ROOT).contains(profile.location().toLowerCase(Locale.ROOT))
        ) {
            reasons.add("Revisar ubicación");
        }
        if (
            !profile.targetRole().isEmpty()
                && !offer.role().toLowerCase(Locale.ROOT).contains(profile.targetRole().toLowerCase(Locale.ROOT))
        ) {
            reasons.add("Revisar puesto");
        }
        assessSalary(offer.salary(), profile.targetSalary(), reasons);
        return List.copyOf(reasons);
    }

    private static void assessSalary(SalaryRange offered, SalaryRange target, List<String> reasons)
    {
        if (target == null) {
            return;
        }
        if (offered == null || !offered.comparableTo(target)) {
            reasons.add("Salario sin datos comparables");
        } else if (offered.maximum() != null && offered.maximum().compareTo(target.minimum()) < 0) {
            reasons.add("Salario por debajo del objetivo");
        } else if (offered.minimum().compareTo(target.minimum()) < 0) {
            reasons.add("Objetivo salarial pendiente de confirmar");
        }
    }
}
