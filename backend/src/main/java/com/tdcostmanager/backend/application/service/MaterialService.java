package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.MaterialCreateRequest;
import com.tdcostmanager.backend.application.dto.MaterialResponse;
import com.tdcostmanager.backend.application.dto.MaterialUpdateRequest;
import com.tdcostmanager.backend.domain.model.Material;
import com.tdcostmanager.backend.domain.repository.MaterialRepository;
import jakarta.persistence.EntityNotFoundException;
import com.tdcostmanager.backend.infrastructure.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MaterialService {

    private final MaterialRepository repository;
    private final CurrentUserProvider currentUser;

    public MaterialService(MaterialRepository repository, CurrentUserProvider currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> findAll() {
        return repository.findAllByOwnerId(currentUser.getCurrentUserId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaterialResponse findById(Long id) {
        return repository.findByIdAndOwnerId(id, currentUser.getCurrentUserId())
                .map(this::mapToResponse)
                .orElseThrow(() -> new EntityNotFoundException("Material not found with id: " + id));
    }

    public MaterialResponse create(MaterialCreateRequest request) {
        Material material = new Material();
        material.setName(request.name());
        material.setDescription(request.description());
        material.setPurchasePrice(request.purchasePrice());
        material.setQuantity(request.quantity());
        material.setUnit(request.unit());
        material.setCategory(request.category());
        material.setActive(true);
        material.setOwnerId(currentUser.getCurrentUserId());

        return mapToResponse(repository.save(material));
    }

    public MaterialResponse update(Long id, MaterialUpdateRequest request) {
        Material material = repository.findByIdAndOwnerId(id, currentUser.getCurrentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Material not found with id: " + id));

        material.setName(request.name());
        material.setDescription(request.description());
        material.setPurchasePrice(request.purchasePrice());
        material.setQuantity(request.quantity());
        material.setUnit(request.unit());
        material.setCategory(request.category());
        material.setActive(request.active());

        return mapToResponse(repository.save(material));
    }

    public void deactivate(Long id) {
        Material material = repository.findByIdAndOwnerId(id, currentUser.getCurrentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Material not found with id: " + id));
        material.setActive(false);
        repository.save(material);
    }

    private MaterialResponse mapToResponse(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getName(),
                material.getDescription(),
                material.getPurchasePrice(),
                material.getQuantity(),
                material.getUnit(),
                material.getCategory(),
                material.isActive()
        );
    }
}
