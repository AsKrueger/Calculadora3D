package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.ProjectMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMaterialRepository extends JpaRepository<ProjectMaterial, Long> {
}
