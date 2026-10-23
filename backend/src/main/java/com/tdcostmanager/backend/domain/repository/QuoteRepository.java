package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    List<Quote> findByProjectId(Long projectId);
    Optional<Quote> findByIdAndProjectIdAndProjectOwnerId(Long id, Long projectId, Long ownerId);
}
