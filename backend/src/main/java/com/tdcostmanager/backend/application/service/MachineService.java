package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.MachineCreateRequest;
import com.tdcostmanager.backend.application.dto.MachineResponse;
import com.tdcostmanager.backend.application.dto.MachineUpdateRequest;
import com.tdcostmanager.backend.domain.model.Machine;
import com.tdcostmanager.backend.domain.repository.MachineRepository;
import jakarta.persistence.EntityNotFoundException;
import com.tdcostmanager.backend.infrastructure.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MachineService {

    private final MachineRepository repository;
    private final CurrentUserProvider currentUser;

    public MachineService(MachineRepository repository, CurrentUserProvider currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<MachineResponse> findAll() {
        return repository.findAllByOwnerId(currentUser.getCurrentUserId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MachineResponse findById(Long id) {
        return repository.findByIdAndOwnerId(id, currentUser.getCurrentUserId())
                .map(this::mapToResponse)
                .orElseThrow(() -> new EntityNotFoundException("Machine not found with id: " + id));
    }

    public MachineResponse create(MachineCreateRequest request) {
        Machine machine = new Machine();
        machine.setName(request.name());
        machine.setAcquisitionCost(request.acquisitionCost());
        machine.setUsefulLifeHours(request.usefulLifeHours());
        machine.setPowerWatts(request.powerWatts());
        machine.setMaintenanceCostPerHour(request.maintenanceCostPerHour());
        machine.setActive(true);
        machine.setOwnerId(currentUser.getCurrentUserId());

        return mapToResponse(repository.save(machine));
    }

    public MachineResponse update(Long id, MachineUpdateRequest request) {
        Machine machine = repository.findByIdAndOwnerId(id, currentUser.getCurrentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Machine not found with id: " + id));

        machine.setName(request.name());
        machine.setAcquisitionCost(request.acquisitionCost());
        machine.setUsefulLifeHours(request.usefulLifeHours());
        machine.setPowerWatts(request.powerWatts());
        machine.setMaintenanceCostPerHour(request.maintenanceCostPerHour());
        machine.setActive(request.active());

        return mapToResponse(repository.save(machine));
    }

    public void deactivate(Long id) {
        Machine machine = repository.findByIdAndOwnerId(id, currentUser.getCurrentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Machine not found with id: " + id));
        machine.setActive(false);
        repository.save(machine);
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
