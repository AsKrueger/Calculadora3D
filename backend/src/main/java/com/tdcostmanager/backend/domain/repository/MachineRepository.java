package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {
    List<Machine> findAllByOwnerId(Long ownerId);
    Optional<Machine> findByIdAndOwnerId(Long id, Long ownerId);
}
