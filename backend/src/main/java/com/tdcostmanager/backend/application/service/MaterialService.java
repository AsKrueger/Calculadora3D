package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.MaterialCreateRequest;
import com.tdcostmanager.backend.application.dto.MaterialResponse;
import com.tdcostmanager.backend.application.dto.MaterialUpdateRequest;
import com.tdcostmanager.backend.domain.model.Material;
import com.tdcostmanager.backend.domain.repository.MaterialRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MaterialService {

    private final MaterialRepository repository;

    public MaterialService(MaterialRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> findAll() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MaterialResponse findById(Long id) {
        return repository.findById(id)
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

        return mapToResponse(repository.save(material));
    }

    public MaterialResponse update(Long id, MaterialUpdateRequest request) {
        Material material = repository.findById(id)
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
        Material material = repository.findById(id)
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
