package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    List<Quote> findByProjectId(Long projectId);
    Optional<Quote> findByIdAndProjectIdAndProjectOwnerId(Long id, Long projectId, Long ownerId);

    Optional<Quote> findFirstByProjectIdOrderByIdDesc(Long projectId);

    // El último presupuesto de cada proyecto del usuario, en una sola consulta para todo el listado
    @Query("""
            select q from Quote q
            where q.project.owner.id = :ownerId
              and q.id = (select max(q2.id) from Quote q2 where q2.project = q.project)
            """)
    List<Quote> findLatestPerProjectByOwnerId(@Param("ownerId") Long ownerId);
}
