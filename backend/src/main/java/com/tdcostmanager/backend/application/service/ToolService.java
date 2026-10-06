package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.ToolCreateRequest;
import com.tdcostmanager.backend.application.dto.ToolResponse;
import com.tdcostmanager.backend.application.dto.ToolUpdateRequest;
import com.tdcostmanager.backend.domain.model.Tool;
import com.tdcostmanager.backend.domain.repository.ToolRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ToolService {

    private final ToolRepository repository;

    public ToolService(ToolRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ToolResponse> findAll() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ToolResponse findById(Long id) {
        return repository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new EntityNotFoundException("Tool not found with id: " + id));
    }

    public ToolResponse create(ToolCreateRequest request) {
        Tool tool = new Tool();
        tool.setName(request.name());
        tool.setDescription(request.description());
        tool.setAcquisitionCost(request.acquisitionCost());
        tool.setEstimatedUses(request.estimatedUses());
        tool.setMaintenancePercentage(request.maintenancePercentage());
        tool.setActive(true);

        return mapToResponse(repository.save(tool));
    }

    public ToolResponse update(Long id, ToolUpdateRequest request) {
        Tool tool = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tool not found with id: " + id));

        tool.setName(request.name());
        tool.setDescription(request.description());
        tool.setAcquisitionCost(request.acquisitionCost());
        tool.setEstimatedUses(request.estimatedUses());
        tool.setMaintenancePercentage(request.maintenancePercentage());
        tool.setActive(request.active());

        return mapToResponse(repository.save(tool));
    }

    public void deactivate(Long id) {
        Tool tool = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tool not found with id: " + id));
        tool.setActive(false);
        repository.save(tool);
    }

    private ToolResponse mapToResponse(Tool tool) {
        return new ToolResponse(
                tool.getId(),
                tool.getName(),
                tool.getDescription(),
                tool.getAcquisitionCost(),
                tool.getEstimatedUses(),
                tool.getMaintenancePercentage(),
                tool.isActive()
        );
    }
}
