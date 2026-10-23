package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.Tool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {
    List<Tool> findAllByOwnerId(Long ownerId);
    Optional<Tool> findByIdAndOwnerId(Long id, Long ownerId);
}
