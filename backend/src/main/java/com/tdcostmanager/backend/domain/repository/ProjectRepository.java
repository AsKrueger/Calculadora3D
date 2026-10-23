package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOwnerId(Long ownerId);
    Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);
}
