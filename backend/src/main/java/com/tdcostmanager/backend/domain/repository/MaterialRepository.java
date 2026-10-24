package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findAllByOwnerId(Long ownerId);
    Optional<Material> findByIdAndOwnerId(Long id, Long ownerId);
}
