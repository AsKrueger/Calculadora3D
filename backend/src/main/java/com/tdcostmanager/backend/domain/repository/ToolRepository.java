package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.Tool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {
}
