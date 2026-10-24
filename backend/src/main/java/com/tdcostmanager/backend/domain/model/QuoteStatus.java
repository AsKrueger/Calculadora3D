package com.tdcostmanager.backend.domain.model;

import java.util.Set;

/**
 * Ciclo de vida de un presupuesto:
 * DRAFT -> SENT | REJECTED; SENT -> ACCEPTED | REJECTED | EXPIRED.
 * ACCEPTED, REJECTED y EXPIRED son estados finales.
 */
public enum QuoteStatus {
    DRAFT,
    SENT,
    ACCEPTED,
    REJECTED,
    EXPIRED;

    public Set<QuoteStatus> allowedTransitions() {
        return switch (this) {
            case DRAFT -> Set.of(SENT, REJECTED);
            case SENT -> Set.of(ACCEPTED, REJECTED, EXPIRED);
            case ACCEPTED, REJECTED, EXPIRED -> Set.of();
        };
    }

    public boolean canTransitionTo(QuoteStatus target) {
        return allowedTransitions().contains(target);
    }
}
