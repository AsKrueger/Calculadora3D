package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.ProjectTool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectToolRepository extends JpaRepository<ProjectTool, Long> {
}
