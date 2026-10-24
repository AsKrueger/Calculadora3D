package com.tdcostmanager.app.ui.project

import com.tdcostmanager.app.data.remote.dto.project.ProjectResponse
import com.tdcostmanager.app.domain.model.ProjectStatus
import com.tdcostmanager.app.domain.model.QuoteStatus
import com.tdcostmanager.app.domain.model.UnitType
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectQueryTest {

    private fun project(
        id: Long,
        name: String,
        status: ProjectStatus = ProjectStatus.DRAFT,
        description: String? = null,
        price: Double? = null,
        updatedAt: String = "2026-10-24T09:00:00Z",
        materials: Int = 0,
        machines: Int = 0,
        tools: Int = 0
    ) = ProjectResponse(
        id = id, name = name, description = description, status = status,
        laborHours = 1.0, laborCostPerHour = 10.0,
        materials = List(materials) { ProjectResponse.MaterialItem(it.toLong(), "m", 1.0, UnitType.G) },
        machines = List(machines) { ProjectResponse.MachineItem(it.toLong(), "k", 1.0) },
        tools = List(tools) { ProjectResponse.ToolItem(it.toLong(), "t", 1.0) },
        createdAt = updatedAt, updatedAt = updatedAt,
        latestQuote = price?.let { ProjectResponse.LatestQuote(id, it, QuoteStatus.DRAFT) }
    )

    private val projects = listOf(
        project(1, "Soporte de auriculares", price = 14.5, updatedAt = "2026-10-20T09:00:00Z"),
        project(2, "Carcasa Raspberry Pi", ProjectStatus.COMPLETED, price = 28.0, updatedAt = "2026-10-24T09:00:00Z"),
        project(3, "Figura de colección", description = "Pintada con aerógrafo", updatedAt = "2026-10-22T09:00:00Z"),
        project(4, "Prototipo viejo", ProjectStatus.ARCHIVED, price = 5.0)
    )

    private fun List<ProjectResponse>.ids() = map { it.id }

    @Test
    fun filtersByArchivedState() {
        assertEquals(listOf(2L, 3L, 1L), projects.query(ProjectFilter.Active, "", ProjectSort.Recent).ids())
        assertEquals(listOf(4L), projects.query(ProjectFilter.Archived, "", ProjectSort.Recent).ids())
        assertEquals(4, projects.query(ProjectFilter.All, "", ProjectSort.Recent).size)
    }

    @Test
    fun searchIgnoresCaseAndAccentsAndLooksInTheDescription() {
        assertEquals(listOf(3L), projects.query(ProjectFilter.All, "AEROGRAFO", ProjectSort.Recent).ids())
        assertEquals(listOf(3L), projects.query(ProjectFilter.All, "coleccion", ProjectSort.Recent).ids())
        assertEquals(emptyList<Long>(), projects.query(ProjectFilter.Active, "prototipo", ProjectSort.Recent).ids())
    }

    @Test
    fun sortsByNameAndPriceWithUnquotedProjectsLast() {
        assertEquals(listOf(2L, 3L, 1L), projects.query(ProjectFilter.Active, "", ProjectSort.Name).ids())
        assertEquals(listOf(2L, 1L, 3L), projects.query(ProjectFilter.Active, "", ProjectSort.PriceHigh).ids())
        assertEquals(listOf(1L, 2L, 3L), projects.query(ProjectFilter.Active, "", ProjectSort.PriceLow).ids())
    }

    @Test
    fun lineSummaryIsAbbreviatedOnlyWithThreeKinds() {
        assertEquals(listOf("1 material", "1 herram."), project(1, "a", materials = 1, tools = 1).lineParts())
        assertEquals(listOf("2 materiales", "1 máquina"), project(1, "a", materials = 2, machines = 1).lineParts())
        assertEquals(listOf("1 mat.", "1 máq.", "1 herram."), project(1, "a", materials = 1, machines = 1, tools = 1).lineParts())
        assertEquals(emptyList<String>(), project(1, "a").lineParts())
    }
}
