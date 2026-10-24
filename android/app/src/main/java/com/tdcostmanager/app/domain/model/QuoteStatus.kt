package com.tdcostmanager.app.domain.model

// Mismas transiciones que QuoteStatus en el backend, que es quien las valida
enum class QuoteStatus(val label: String) {
    DRAFT("Borrador"),
    SENT("Enviado"),
    ACCEPTED("Aceptado"),
    REJECTED("Rechazado"),
    EXPIRED("Caducado");

    val allowedTransitions: List<QuoteStatus>
        get() = when (this) {
            DRAFT -> listOf(SENT, REJECTED)
            SENT -> listOf(ACCEPTED, REJECTED, EXPIRED)
            ACCEPTED, REJECTED, EXPIRED -> emptyList()
        }
}
