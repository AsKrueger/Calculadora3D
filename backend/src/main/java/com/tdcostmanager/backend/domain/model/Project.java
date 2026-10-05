package com.tdcostmanager.backend.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "projects")
@EntityListeners(AuditingEntityListener.class)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProjectStatus status;

    @NotNull
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @NotNull
    @PositiveOrZero
    @Column(name = "labor_hours", nullable = false, precision = 19, scale = 4)
    private BigDecimal laborHours = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero
    @Column(name = "labor_cost_per_hour", nullable = false, precision = 19, scale = 4)
    private BigDecimal laborCostPerHour = BigDecimal.ZERO;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMachine> projectMachines = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMaterial> projectMaterials = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectTool> projectTools = new ArrayList<>();

    public Project() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public ProjectStatus getStatus() { return status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getLaborHours() { return laborHours; }

    public BigDecimal getLaborCostPerHour() { return laborCostPerHour; }

    public List<ProjectMachine> getProjectMachines() { return Collections.unmodifiableList(projectMachines); }

    public List<ProjectMaterial> getProjectMaterials() { return Collections.unmodifiableList(projectMaterials); }

    public List<ProjectTool> getProjectTools() { return Collections.unmodifiableList(projectTools); }

    // --- Domain Operations & Invariants ---

    public void updateDetails(String name, String description) {
        ensureNotArchived();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name cannot be null or blank");
        }
        this.name = name;
        this.description = description;
    }

    public void updateLabor(BigDecimal laborHours, BigDecimal laborCostPerHour) {
        ensureNotArchived();
        if (laborHours == null || laborHours.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Labor hours cannot be negative");
        }
        if (laborCostPerHour == null || laborCostPerHour.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Labor cost per hour cannot be negative");
        }
        this.laborHours = laborHours;
        this.laborCostPerHour = laborCostPerHour;
    }

    public void updateStatus(ProjectStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Project status cannot be null");
        }
        if (this.status == ProjectStatus.ARCHIVED && status == ProjectStatus.ARCHIVED) {
            return; // idempotent
        }
        ensureNotArchived();
        this.status = status;
    }

    public void archive() {
        this.status = ProjectStatus.ARCHIVED;
    }

    public boolean isArchived() {
        return this.status == ProjectStatus.ARCHIVED;
    }

    private void ensureNotArchived() {
        if (isArchived()) {
            throw new IllegalStateException("No se puede modificar un proyecto archivado");
        }
    }

    public void addProjectMaterial(ProjectMaterial pm) {
        ensureNotArchived();
        Objects.requireNonNull(pm, "Project material cannot be null");
        projectMaterials.add(pm);
        pm.setProject(this);
    }

    public void addProjectMachine(ProjectMachine pm) {
        ensureNotArchived();
        Objects.requireNonNull(pm, "Project machine cannot be null");
        projectMachines.add(pm);
        pm.setProject(this);
    }

    public void addProjectTool(ProjectTool pt) {
        ensureNotArchived();
        Objects.requireNonNull(pt, "Project tool cannot be null");
        projectTools.add(pt);
        pt.setProject(this);
    }
}
