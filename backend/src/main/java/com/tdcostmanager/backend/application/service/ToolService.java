package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.ToolCreateRequest;
import com.tdcostmanager.backend.application.dto.ToolResponse;
import com.tdcostmanager.backend.application.dto.ToolUpdateRequest;
import com.tdcostmanager.backend.domain.model.Tool;
import com.tdcostmanager.backend.domain.repository.ToolRepository;
import com.tdcostmanager.backend.infrastructure.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ToolService {

    private final ToolRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public ToolService(ToolRepository repository, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<ToolResponse> findAll() {
        return repository.findAllByOwnerId(currentUserProvider.getCurrentUserId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ToolResponse findById(Long id) {
        return mapToResponse(findOwned(id));
    }

    public ToolResponse create(ToolCreateRequest request) {
        Tool tool = new Tool();
        tool.setName(request.name());
        tool.setDescription(request.description());
        tool.setAcquisitionCost(request.acquisitionCost());
        tool.setEstimatedUses(request.estimatedUses());
        tool.setMaintenancePercentage(request.maintenancePercentage());
        tool.setActive(true);
        tool.setOwner(currentUserProvider.getCurrentUser());

        return mapToResponse(repository.save(tool));
    }

    public ToolResponse update(Long id, ToolUpdateRequest request) {
        Tool tool = findOwned(id);

        tool.setName(request.name());
        tool.setDescription(request.description());
        tool.setAcquisitionCost(request.acquisitionCost());
        tool.setEstimatedUses(request.estimatedUses());
        tool.setMaintenancePercentage(request.maintenancePercentage());
        tool.setActive(request.active());

        return mapToResponse(repository.save(tool));
    }

    public void deactivate(Long id) {
        Tool tool = findOwned(id);
        tool.setActive(false);
        repository.save(tool);
    }

    // Un recurso de otro usuario se trata como inexistente (404) para no revelar que existe
    private Tool findOwned(Long id) {
        return repository.findByIdAndOwnerId(id, currentUserProvider.getCurrentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Tool not found with id: " + id));
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
