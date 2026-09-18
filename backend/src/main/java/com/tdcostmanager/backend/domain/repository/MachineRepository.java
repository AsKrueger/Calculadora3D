package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {
}
