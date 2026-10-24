package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.QuoteCreateRequest;
import com.tdcostmanager.backend.application.dto.QuoteResponse;
import com.tdcostmanager.backend.application.dto.QuoteStatusUpdateRequest;
import com.tdcostmanager.backend.domain.calculation.CostCalculationInput;
import com.tdcostmanager.backend.domain.calculation.CostCalculationResult;
import com.tdcostmanager.backend.domain.calculation.CostCalculator;
import com.tdcostmanager.backend.domain.calculation.ElectricityPriceProvider;
import com.tdcostmanager.backend.domain.model.Project;
import com.tdcostmanager.backend.domain.model.ProjectStatus;
import com.tdcostmanager.backend.domain.model.Quote;
import com.tdcostmanager.backend.domain.repository.ProjectRepository;
import com.tdcostmanager.backend.domain.repository.QuoteRepository;
import com.tdcostmanager.backend.infrastructure.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuoteService {

    // La fecha y hora del cálculo llega sin zona; se interpreta en la hora peninsular, la que usa ESIOS
    private static final ZoneId CALCULATION_ZONE = ZoneId.of("Europe/Madrid");

    private final QuoteRepository quoteRepository;
    private final ProjectRepository projectRepository;
    private final ElectricityPriceProvider electricityPriceProvider;
    private final CurrentUserProvider currentUserProvider;

    public QuoteService(QuoteRepository quoteRepository,
                        ProjectRepository projectRepository,
                        ElectricityPriceProvider electricityPriceProvider,
                        CurrentUserProvider currentUserProvider) {
        this.quoteRepository = quoteRepository;
        this.projectRepository = projectRepository;
        this.electricityPriceProvider = electricityPriceProvider;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public QuoteResponse generateQuote(Long projectId, QuoteCreateRequest request) {
        Project project = findOwnedProject(projectId);

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("No se pueden generar presupuestos para un proyecto archivado");
        }

        List<CostCalculationInput.MaterialInput> materialInputs = project.getProjectMaterials().stream()
                .map(pm -> new CostCalculationInput.MaterialInput(
                        pm.getQuantityUsed(),
                        pm.getUnit(),
                        pm.getMaterial().getPurchasePrice(),
                        pm.getMaterial().getQuantity(),
                        pm.getMaterial().getUnit(),
                        pm.getMaterial().getName()
                ))
                .collect(Collectors.toList());

        List<CostCalculationInput.MachineInput> machineInputs = project.getProjectMachines().stream()
                .map(pm -> new CostCalculationInput.MachineInput(
                        pm.getEstimatedHours(),
                        pm.getMachine().getAcquisitionCost(),
                        pm.getMachine().getUsefulLifeHours(),
                        pm.getMachine().getPowerWatts(),
                        pm.getMachine().getMaintenanceCostPerHour(),
                        pm.getMachine().getName()
                ))
                .collect(Collectors.toList());

        List<CostCalculationInput.ToolInput> toolInputs = project.getProjectTools().stream()
                .map(pt -> new CostCalculationInput.ToolInput(
                        pt.getUses(),
                        pt.getTool().getAcquisitionCost(),
                        pt.getTool().getEstimatedUses(),
                        pt.getTool().getMaintenancePercentage(),
                        pt.getTool().getName()
                ))
                .collect(Collectors.toList());

        CostCalculationInput input = new CostCalculationInput(
                project.getLaborHours(),
                project.getLaborCostPerHour(),
                materialInputs,
                machineInputs,
                toolInputs
        );

        // Ejecutar motor de cálculo del dominio
        CostCalculationResult result = CostCalculator.calculate(
                input,
                request.calculationDateTime(),
                request.marginPercentage(),
                request.safetyPercentage(),
                electricityPriceProvider
        );

        Instant calculatedAt = request.calculationDateTime().atZone(CALCULATION_ZONE).toInstant();
        Quote quote = Quote.create(
                project,
                request.marginPercentage(),
                request.safetyPercentage(),
                result,
                calculatedAt
        );

        Quote saved = quoteRepository.save(quote);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<QuoteResponse> findByProjectId(Long projectId) {
        findOwnedProject(projectId);
        return quoteRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuoteResponse findById(Long projectId, Long quoteId) {
        return mapToResponse(findOwnedQuote(projectId, quoteId));
    }

    @Transactional
    public QuoteResponse updateStatus(Long projectId, Long quoteId, QuoteStatusUpdateRequest request) {
        Quote quote = findOwnedQuote(projectId, quoteId);
        quote.updateStatus(request.status());
        return mapToResponse(quoteRepository.save(quote));
    }

    private Quote findOwnedQuote(Long projectId, Long quoteId) {
        return quoteRepository.findByIdAndProjectIdAndProjectOwnerId(quoteId, projectId, currentUserProvider.getCurrentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + quoteId));
    }

    // Los presupuestos heredan el propietario de su proyecto; uno ajeno se trata como inexistente (404)
    private Project findOwnedProject(Long projectId) {
        return projectRepository.findByIdAndOwnerId(projectId, currentUserProvider.getCurrentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado: " + projectId));
    }

    private QuoteResponse mapToResponse(Quote q) {
        return new QuoteResponse(
                q.getId(),
                q.getProject().getId(),
                q.getMarginPercentage(),
                q.getSafetyPercentage(),
                q.getMaterialCost(),
                q.getMachineCost(),
                q.getToolCost(),
                q.getLaborCost(),
                q.getElectricityCost(),
                q.getEnergyKWh(),
                q.getElectricityPricePerKWh(),
                q.getBaseCost(),
                q.getSafetyAmount(),
                q.getAdjustedCost(),
                q.getProfitAmount(),
                q.getFinalPrice(),
                q.getStatus(),
                q.getCalculatedAt(),
                q.getCreatedAt()
        );
    }
}
