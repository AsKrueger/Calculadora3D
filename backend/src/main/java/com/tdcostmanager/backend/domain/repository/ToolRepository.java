package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.Tool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {

    List<Tool> findAllByOwnerId(Long ownerId);

    Optional<Tool> findByIdAndOwnerId(Long id, Long ownerId);
}
