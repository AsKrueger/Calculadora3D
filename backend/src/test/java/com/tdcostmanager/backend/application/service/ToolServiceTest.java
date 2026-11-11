package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.ToolCreateRequest;
import com.tdcostmanager.backend.application.dto.ToolResponse;
import com.tdcostmanager.backend.application.dto.ToolUpdateRequest;
import com.tdcostmanager.backend.domain.model.Tool;
import com.tdcostmanager.backend.domain.model.User;
import com.tdcostmanager.backend.domain.repository.ToolRepository;
import com.tdcostmanager.backend.infrastructure.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    private static final Long OWNER_ID = 7L;

    @Mock
    private ToolRepository repository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ToolService toolService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(OWNER_ID);
    }

    @Test
    void findAllShouldOnlyReturnToolsOfCurrentUser() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findAllByOwnerId(OWNER_ID)).thenReturn(List.of(
                tool(1L, "Lijadora", true),
                tool(2L, "Aerógrafo", false)
        ));

        List<ToolResponse> result = toolService.findAll();

        assertThat(result).extracting(ToolResponse::name).containsExactly("Lijadora", "Aerógrafo");
        assertThat(result).extracting(ToolResponse::active).containsExactly(true, false);
        verify(repository, never()).findAll();
    }

    @Test
    void findByIdShouldMapAllFieldsOfOwnedTool() {
        Tool tool = tool(3L, "Dremel", true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(3L, OWNER_ID)).thenReturn(Optional.of(tool));

        ToolResponse response = toolService.findById(3L);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("Dremel");
        assertThat(response.description()).isEqualTo("Desc Dremel");
        assertThat(response.acquisitionCost()).isEqualByComparingTo("120.00");
        assertThat(response.estimatedUses()).isEqualByComparingTo("500");
        assertThat(response.maintenancePercentage()).isEqualByComparingTo("10");
        assertThat(response.active()).isTrue();
    }

    @Test
    void findByIdShouldThrowNotFoundWhenToolBelongsToAnotherUser() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Tool not found with id: 99");
    }

    @Test
    void createShouldAssignCurrentUserAsOwnerAndStartActive() {
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(repository.save(any(Tool.class))).thenAnswer(invocation -> {
            Tool saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ToolCreateRequest request = new ToolCreateRequest(
                "Pistola de calor", "Para curvar PLA",
                new BigDecimal("35.50"), new BigDecimal("200"), new BigDecimal("5")
        );

        ToolResponse response = toolService.create(request);

        ArgumentCaptor<Tool> captor = ArgumentCaptor.forClass(Tool.class);
        verify(repository).save(captor.capture());
        Tool saved = captor.getValue();
        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getName()).isEqualTo("Pistola de calor");
        assertThat(saved.getAcquisitionCost()).isEqualByComparingTo("35.50");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.active()).isTrue();
    }

    @Test
    void updateShouldOverwriteFieldsIncludingActiveFlag() {
        Tool tool = tool(4L, "Lijadora", true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(4L, OWNER_ID)).thenReturn(Optional.of(tool));
        when(repository.save(tool)).thenReturn(tool);

        ToolUpdateRequest request = new ToolUpdateRequest(
                "Lijadora orbital", null,
                new BigDecimal("80"), new BigDecimal("1000"), new BigDecimal("12.5"), false
        );

        ToolResponse response = toolService.update(4L, request);

        assertThat(response.name()).isEqualTo("Lijadora orbital");
        assertThat(response.description()).isNull();
        assertThat(response.acquisitionCost()).isEqualByComparingTo("80");
        assertThat(response.estimatedUses()).isEqualByComparingTo("1000");
        assertThat(response.maintenancePercentage()).isEqualByComparingTo("12.5");
        assertThat(response.active()).isFalse();
        assertThat(tool.getOwner()).isSameAs(owner);
    }

    @Test
    void updateShouldNotSaveWhenToolIsNotOwned() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(5L, OWNER_ID)).thenReturn(Optional.empty());

        ToolUpdateRequest request = new ToolUpdateRequest(
                "X", null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, true
        );

        assertThatThrownBy(() -> toolService.update(5L, request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deactivateShouldMarkToolInactiveWithoutDeletingIt() {
        Tool tool = tool(6L, "Aerógrafo", true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(6L, OWNER_ID)).thenReturn(Optional.of(tool));

        toolService.deactivate(6L);

        assertThat(tool.isActive()).isFalse();
        verify(repository).save(tool);
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deactivateShouldThrowNotFoundWhenToolIsNotOwned() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(8L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolService.deactivate(8L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Tool not found with id: 8");
        verify(repository, never()).save(any());
    }

    private Tool tool(Long id, String name, boolean active) {
        Tool tool = new Tool();
        tool.setId(id);
        tool.setName(name);
        tool.setDescription("Desc " + name);
        tool.setAcquisitionCost(new BigDecimal("120.00"));
        tool.setEstimatedUses(new BigDecimal("500"));
        tool.setMaintenancePercentage(new BigDecimal("10"));
        tool.setActive(active);
        tool.setOwner(owner);
        return tool;
    }
}
