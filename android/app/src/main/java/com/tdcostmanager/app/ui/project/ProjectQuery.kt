package com.tdcostmanager.app.ui.project

import com.tdcostmanager.app.data.remote.dto.project.ProjectResponse
import com.tdcostmanager.app.domain.model.ProjectStatus
import java.text.Collator
import java.text.Normalizer
import java.time.Instant
import java.util.Locale

enum class ProjectFilter { Active, Archived, All }

enum class ProjectSort(val label: String) {
    Recent("Más recientes"),
    Name("Nombre (A–Z)"),
    PriceHigh("Precio: de mayor a menor"),
    PriceLow("Precio: de menor a mayor")
}

private val spanishCollator: Collator = Collator.getInstance(Locale("es", "ES")).apply { strength = Collator.PRIMARY }

/** Minúsculas y sin tildes, para que «aerografo» encuentre «aerógrafo». */
private fun String.normalized(): String =
    Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

fun ProjectResponse.matches(filter: ProjectFilter): Boolean = when (filter) {
    ProjectFilter.Active -> status != ProjectStatus.ARCHIVED
    ProjectFilter.Archived -> status == ProjectStatus.ARCHIVED
    ProjectFilter.All -> true
}

fun ProjectResponse.matches(query: String): Boolean {
    val q = query.trim().normalized()
    if (q.isEmpty()) return true
    return name.normalized().contains(q) || description.orEmpty().normalized().contains(q)
}

/** Filtra por estado y texto y ordena. Los proyectos sin presupuesto van al final al ordenar por precio. */
fun List<ProjectResponse>.query(filter: ProjectFilter, text: String, sort: ProjectSort): List<ProjectResponse> {
    val visible = filter { it.matches(filter) && it.matches(text) }
    return when (sort) {
        ProjectSort.Recent -> visible.sortedByDescending { runCatching { Instant.parse(it.updatedAt) }.getOrDefault(Instant.EPOCH) }
        ProjectSort.Name -> visible.sortedWith { a, b -> spanishCollator.compare(a.name, b.name) }
        ProjectSort.PriceHigh -> visible.sortedWith(compareBy<ProjectResponse> { it.latestQuote == null }.thenByDescending { it.latestQuote?.finalPrice })
        ProjectSort.PriceLow -> visible.sortedWith(compareBy<ProjectResponse> { it.latestQuote == null }.thenBy { it.latestQuote?.finalPrice })
    }
}

/**
 * Resumen de líneas para el pie de la tarjeta. Con tres tipos de línea se abrevia para que quepa:
 * «1 material · 1 herram.» o «1 mat. · 1 máq. · 1 herram.».
 */
fun ProjectResponse.lineParts(): List<String> {
    val compact = listOf(materials, machines, tools).count { it.isNotEmpty() } >= 3
    fun count(n: Int, one: String, many: String, short: String) = "$n ${if (compact) short else if (n == 1) one else many}"
    return buildList {
        if (materials.isNotEmpty()) add(count(materials.size, "material", "materiales", "mat."))
        if (machines.isNotEmpty()) add(count(machines.size, "máquina", "máquinas", "máq."))
        if (tools.isNotEmpty()) add("${tools.size} herram.")
    }
}
