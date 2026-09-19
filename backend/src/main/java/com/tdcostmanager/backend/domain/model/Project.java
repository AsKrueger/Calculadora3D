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
import java.util.List;

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
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getLaborHours() { return laborHours; }
    public void setLaborHours(BigDecimal laborHours) { this.laborHours = laborHours; }

    public BigDecimal getLaborCostPerHour() { return laborCostPerHour; }
    public void setLaborCostPerHour(BigDecimal laborCostPerHour) { this.laborCostPerHour = laborCostPerHour; }

    public List<ProjectMachine> getProjectMachines() { return projectMachines; }
    public void addProjectMachine(ProjectMachine pm) {
        projectMachines.add(pm);
        pm.setProject(this);
    }

    public List<ProjectMaterial> getProjectMaterials() { return projectMaterials; }
    public void addProjectMaterial(ProjectMaterial pm) {
        projectMaterials.add(pm);
        pm.setProject(this);
    }

    public List<ProjectTool> getProjectTools() { return projectTools; }
    public void addProjectTool(ProjectTool pt) {
        projectTools.add(pt);
        pt.setProject(this);
    }
}
