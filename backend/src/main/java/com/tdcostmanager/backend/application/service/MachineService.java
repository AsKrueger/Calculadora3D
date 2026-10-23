package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.MachineCreateRequest;
import com.tdcostmanager.backend.application.dto.MachineResponse;
import com.tdcostmanager.backend.application.dto.MachineUpdateRequest;
import com.tdcostmanager.backend.domain.model.Machine;
import com.tdcostmanager.backend.domain.repository.MachineRepository;
import com.tdcostmanager.backend.infrastructure.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MachineService {

    private final MachineRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public MachineService(MachineRepository repository, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<MachineResponse> findAll() {
        return repository.findAllByOwnerId(currentUserProvider.getCurrentUserId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MachineResponse findById(Long id) {
        return mapToResponse(findOwned(id));
    }

    public MachineResponse create(MachineCreateRequest request) {
        Machine machine = new Machine();
        machine.setName(request.name());
        machine.setAcquisitionCost(request.acquisitionCost());
        machine.setUsefulLifeHours(request.usefulLifeHours());
        machine.setPowerWatts(request.powerWatts());
        machine.setMaintenanceCostPerHour(request.maintenanceCostPerHour());
        machine.setActive(true);
        machine.setOwner(currentUserProvider.getCurrentUser());

        return mapToResponse(repository.save(machine));
    }

    public MachineResponse update(Long id, MachineUpdateRequest request) {
        Machine machine = findOwned(id);

        machine.setName(request.name());
        machine.setAcquisitionCost(request.acquisitionCost());
        machine.setUsefulLifeHours(request.usefulLifeHours());
        machine.setPowerWatts(request.powerWatts());
        machine.setMaintenanceCostPerHour(request.maintenanceCostPerHour());
        machine.setActive(request.active());

        return mapToResponse(repository.save(machine));
    }

    public void deactivate(Long id) {
        Machine machine = findOwned(id);
        machine.setActive(false);
        repository.save(machine);
    }

    // Un recurso de otro usuario se trata como inexistente (404) para no revelar que existe
    private Machine findOwned(Long id) {
        return repository.findByIdAndOwnerId(id, currentUserProvider.getCurrentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Machine not found with id: " + id));
    }

    private MachineResponse mapToResponse(Machine machine) {
        return new MachineResponse(
                machine.getId(),
                machine.getName(),
                machine.getAcquisitionCost(),
                machine.getUsefulLifeHours(),
                machine.getPowerWatts(),
                machine.getMaintenanceCostPerHour(),
                machine.isActive()
        );
    }
}
