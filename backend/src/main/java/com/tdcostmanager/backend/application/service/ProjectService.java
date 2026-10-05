package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.*;
import com.tdcostmanager.backend.domain.model.*;
import com.tdcostmanager.backend.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MaterialRepository materialRepository;
    private final MachineRepository machineRepository;
    private final ToolRepository toolRepository;

    public ProjectService(ProjectRepository projectRepository,
                          MaterialRepository materialRepository,
                          MachineRepository machineRepository,
                          ToolRepository toolRepository) {
        this.projectRepository = projectRepository;
        this.materialRepository = materialRepository;
        this.machineRepository = machineRepository;
        this.toolRepository = toolRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll() {
        return projectRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        return projectRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado con ID: " + id));
    }

    @Transactional
    public ProjectResponse create(ProjectCreateRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setLaborHours(request.laborHours());
        project.setLaborCostPerHour(request.laborCostPerHour());
        project.setStatus(ProjectStatus.DRAFT);

        if (request.materials() != null) {
            for (ProjectMaterialRequest req : request.materials()) {
                Material material = materialRepository.findById(req.materialId())
                        .orElseThrow(() -> new EntityNotFoundException("Material no encontrado: " + req.materialId()));
                ProjectMaterial pm = new ProjectMaterial();
                pm.setMaterial(material);
                pm.setQuantityUsed(req.quantityUsed());
                pm.setUnit(req.unit());
                project.addProjectMaterial(pm);
            }
        }

        if (request.machines() != null) {
            for (ProjectMachineRequest req : request.machines()) {
                Machine machine = machineRepository.findById(req.machineId())
                        .orElseThrow(() -> new EntityNotFoundException("Máquina no encontrada: " + req.machineId()));
                ProjectMachine pm = new ProjectMachine();
                pm.setMachine(machine);
                pm.setEstimatedHours(req.estimatedHours());
                project.addProjectMachine(pm);
            }
        }

        if (request.tools() != null) {
            for (ProjectToolRequest req : request.tools()) {
                Tool tool = toolRepository.findById(req.toolId())
                        .orElseThrow(() -> new EntityNotFoundException("Herramienta no encontrada: " + req.toolId()));
                ProjectTool pt = new ProjectTool();
                pt.setTool(tool);
                pt.setUses(req.uses());
                project.addProjectTool(pt);
            }
        }

        Project saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectUpdateRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado con ID: " + id));

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("No se puede modificar un proyecto archivado");
        }

        project.setName(request.name());
        project.setDescription(request.description());
        project.setStatus(request.status());
        project.setLaborHours(request.laborHours());
        project.setLaborCostPerHour(request.laborCostPerHour());

        return mapToResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado con ID: " + id));
        
        project.setStatus(ProjectStatus.ARCHIVED);
        projectRepository.save(project);
    }

    private ProjectResponse mapToResponse(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getStatus(),
                p.getLaborHours(),
                p.getLaborCostPerHour(),
                p.getProjectMaterials().stream()
                        .map(pm -> new ProjectResponse.MaterialItem(pm.getMaterial().getId(), pm.getMaterial().getName(), pm.getQuantityUsed(), pm.getUnit()))
                        .collect(Collectors.toList()),
                p.getProjectMachines().stream()
                        .map(pm -> new ProjectResponse.MachineItem(pm.getMachine().getId(), pm.getMachine().getName(), pm.getEstimatedHours()))
                        .collect(Collectors.toList()),
                p.getProjectTools().stream()
                        .map(pt -> new ProjectResponse.ToolItem(pt.getTool().getId(), pt.getTool().getName(), pt.getUses()))
                        .collect(Collectors.toList()),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
