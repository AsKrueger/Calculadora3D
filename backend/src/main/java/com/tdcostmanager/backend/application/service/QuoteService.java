package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.QuoteCreateRequest;
import com.tdcostmanager.backend.application.dto.QuoteResponse;
import com.tdcostmanager.backend.domain.calculation.CostCalculationResult;
import com.tdcostmanager.backend.domain.calculation.CostCalculator;
import com.tdcostmanager.backend.domain.calculation.ElectricityPriceProvider;
import com.tdcostmanager.backend.domain.model.Project;
import com.tdcostmanager.backend.domain.model.ProjectStatus;
import com.tdcostmanager.backend.domain.model.Quote;
import com.tdcostmanager.backend.domain.model.QuoteStatus;
import com.tdcostmanager.backend.domain.repository.ProjectRepository;
import com.tdcostmanager.backend.domain.repository.QuoteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final ProjectRepository projectRepository;
    private final ElectricityPriceProvider electricityPriceProvider;

    public QuoteService(QuoteRepository quoteRepository,
                        ProjectRepository projectRepository,
                        ElectricityPriceProvider electricityPriceProvider) {
        this.quoteRepository = quoteRepository;
        this.projectRepository = projectRepository;
        this.electricityPriceProvider = electricityPriceProvider;
    }

    @Transactional
    public QuoteResponse generateQuote(Long projectId, QuoteCreateRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado: " + projectId));

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("No se pueden generar presupuestos para un proyecto archivado");
        }

        // Ejecutar motor de cálculo del dominio
        CostCalculationResult result = CostCalculator.calculate(
                project,
                request.calculationDateTime(),
                request.marginPercentage(),
                request.safetyPercentage(),
                electricityPriceProvider
        );

        // Mapear resultado a entidad persistible
        Quote quote = new Quote();
        quote.setProject(project);
        quote.setMarginPercentage(request.marginPercentage());
        quote.setSafetyPercentage(request.safetyPercentage());
        quote.setBaseCost(result.baseCost());
        quote.setAdjustedCost(result.adjustedCost());
        quote.setFinalPrice(result.finalPrice());
        quote.setStatus(QuoteStatus.DRAFT);

        Quote saved = quoteRepository.save(quote);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<QuoteResponse> findByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new EntityNotFoundException("Proyecto no encontrado: " + projectId);
        }
        return quoteRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuoteResponse findById(Long quoteId) {
        return quoteRepository.findById(quoteId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + quoteId));
    }

    private QuoteResponse mapToResponse(Quote q) {
        return new QuoteResponse(
                q.getId(),
                q.getProject().getId(),
                q.getMarginPercentage(),
                q.getSafetyPercentage(),
                q.getBaseCost(),
                q.getAdjustedCost(),
                q.getFinalPrice(),
                q.getStatus(),
                q.getCreatedAt()
        );
    }
}
