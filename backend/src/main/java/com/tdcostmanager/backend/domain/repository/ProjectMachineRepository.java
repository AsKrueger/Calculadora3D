package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.ProjectMachine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMachineRepository extends JpaRepository<ProjectMachine, Long> {
}
