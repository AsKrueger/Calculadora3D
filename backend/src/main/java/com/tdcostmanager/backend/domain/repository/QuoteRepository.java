package com.tdcostmanager.backend.domain.repository;

import com.tdcostmanager.backend.domain.model.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
}
