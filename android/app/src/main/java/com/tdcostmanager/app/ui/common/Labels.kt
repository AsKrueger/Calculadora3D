package com.tdcostmanager.app.ui.common

import com.tdcostmanager.app.domain.model.MaterialCategory
import com.tdcostmanager.app.domain.model.ProjectStatus
import com.tdcostmanager.app.domain.model.QuoteStatus
import com.tdcostmanager.app.domain.model.UnitType
import com.tdcostmanager.app.ui.theme.Tone

// Textos y tonos de los enums de la API. Los nombres en inglés no llegan nunca a la pantalla.

val ProjectStatus.label: String
    get() = when (this) {
        ProjectStatus.DRAFT -> "Borrador"
        ProjectStatus.IN_PROGRESS -> "En curso"
        ProjectStatus.COMPLETED -> "Completado"
        ProjectStatus.CANCELLED -> "Cancelado"
        ProjectStatus.ARCHIVED -> "Archivado"
    }

val ProjectStatus.tone: Tone
    get() = when (this) {
        ProjectStatus.DRAFT -> Tone.Neutral
        ProjectStatus.IN_PROGRESS -> Tone.Progress
        ProjectStatus.COMPLETED -> Tone.Success
        ProjectStatus.CANCELLED -> Tone.Danger
        ProjectStatus.ARCHIVED -> Tone.Neutral
    }

val QuoteStatus.tone: Tone
    get() = when (this) {
        QuoteStatus.DRAFT -> Tone.Neutral
        QuoteStatus.SENT -> Tone.Info
        QuoteStatus.ACCEPTED -> Tone.Success
        QuoteStatus.REJECTED -> Tone.Danger
        QuoteStatus.EXPIRED -> Tone.Warning
    }

val MaterialCategory.label: String
    get() = when (this) {
        MaterialCategory.FILAMENT -> "Filamento"
        MaterialCategory.RESIN -> "Resina"
        MaterialCategory.PAINT -> "Pintura"
        MaterialCategory.GLUE -> "Adhesivo"
        MaterialCategory.SANDPAPER -> "Lija"
        MaterialCategory.OTHER -> "Otro"
    }

/** Símbolo corto para acompañar a una cantidad: 145 g. */
val UnitType.symbol: String
    get() = when (this) {
        UnitType.G -> "g"
        UnitType.KG -> "kg"
        UnitType.ML -> "ml"
        UnitType.L -> "L"
        UnitType.UNIT -> "ud."
        UnitType.H -> "h"
    }

/** Nombre completo para los selectores. */
val UnitType.label: String
    get() = when (this) {
        UnitType.G -> "Gramos (g)"
        UnitType.KG -> "Kilogramos (kg)"
        UnitType.ML -> "Mililitros (ml)"
        UnitType.L -> "Litros (L)"
        UnitType.UNIT -> "Unidades"
        UnitType.H -> "Horas (h)"
    }
