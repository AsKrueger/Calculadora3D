package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.MachineCreateRequest;
import com.tdcostmanager.backend.application.dto.MachineResponse;
import com.tdcostmanager.backend.application.dto.MachineUpdateRequest;
import com.tdcostmanager.backend.domain.model.Machine;
import com.tdcostmanager.backend.domain.model.User;
import com.tdcostmanager.backend.domain.repository.MachineRepository;
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
class MachineServiceTest {

    private static final Long OWNER_ID = 7L;

    @Mock
    private MachineRepository repository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private MachineService machineService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(OWNER_ID);
    }

    @Test
    void findAllShouldOnlyReturnMachinesOfCurrentUser() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findAllByOwnerId(OWNER_ID)).thenReturn(List.of(
                machine(1L, "Bambu Lab P1S", true),
                machine(2L, "Elegoo Saturn", false)
        ));

        List<MachineResponse> result = machineService.findAll();

        assertThat(result).extracting(MachineResponse::name).containsExactly("Bambu Lab P1S", "Elegoo Saturn");
        assertThat(result).extracting(MachineResponse::active).containsExactly(true, false);
        verify(repository, never()).findAll();
    }

    @Test
    void findByIdShouldMapAllFieldsOfOwnedMachine() {
        Machine machine = machine(3L, "Prusa MK4", true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(3L, OWNER_ID)).thenReturn(Optional.of(machine));

        MachineResponse response = machineService.findById(3L);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("Prusa MK4");
        assertThat(response.acquisitionCost()).isEqualByComparingTo("799.00");
        assertThat(response.usefulLifeHours()).isEqualByComparingTo("5000");
        assertThat(response.powerWatts()).isEqualByComparingTo("120");
        assertThat(response.maintenanceCostPerHour()).isEqualByComparingTo("0.05");
        assertThat(response.active()).isTrue();
    }

    @Test
    void findByIdShouldThrowNotFoundWhenMachineBelongsToAnotherUser() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> machineService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Machine not found with id: 99");
    }

    @Test
    void createShouldAssignCurrentUserAsOwnerAndStartActive() {
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(repository.save(any(Machine.class))).thenAnswer(invocation -> {
            Machine saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        MachineCreateRequest request = new MachineCreateRequest(
                "Creality K1", new BigDecimal("459.90"), new BigDecimal("4000"),
                new BigDecimal("350"), new BigDecimal("0.08")
        );

        MachineResponse response = machineService.create(request);

        ArgumentCaptor<Machine> captor = ArgumentCaptor.forClass(Machine.class);
        verify(repository).save(captor.capture());
        Machine saved = captor.getValue();
        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getName()).isEqualTo("Creality K1");
        assertThat(saved.getAcquisitionCost()).isEqualByComparingTo("459.90");
        assertThat(saved.getUsefulLifeHours()).isEqualByComparingTo("4000");
        assertThat(saved.getPowerWatts()).isEqualByComparingTo("350");
        assertThat(saved.getMaintenanceCostPerHour()).isEqualByComparingTo("0.08");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.active()).isTrue();
    }

    @Test
    void updateShouldOverwriteFieldsIncludingActiveFlag() {
        Machine machine = machine(4L, "Prusa MK4", true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(4L, OWNER_ID)).thenReturn(Optional.of(machine));
        when(repository.save(machine)).thenReturn(machine);

        MachineUpdateRequest request = new MachineUpdateRequest(
                "Prusa MK4S", new BigDecimal("899"), new BigDecimal("6000"),
                new BigDecimal("0"), new BigDecimal("0.10"), false
        );

        MachineResponse response = machineService.update(4L, request);

        assertThat(response.name()).isEqualTo("Prusa MK4S");
        assertThat(response.acquisitionCost()).isEqualByComparingTo("899");
        assertThat(response.usefulLifeHours()).isEqualByComparingTo("6000");
        assertThat(response.powerWatts()).isEqualByComparingTo("0");
        assertThat(response.maintenanceCostPerHour()).isEqualByComparingTo("0.10");
        assertThat(response.active()).isFalse();
        assertThat(machine.getOwner()).isSameAs(owner);
    }

    @Test
    void updateShouldNotSaveWhenMachineIsNotOwned() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(5L, OWNER_ID)).thenReturn(Optional.empty());

        MachineUpdateRequest request = new MachineUpdateRequest(
                "X", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        assertThatThrownBy(() -> machineService.update(5L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Machine not found with id: 5");
        verify(repository, never()).save(any());
    }

    @Test
    void deactivateShouldMarkMachineInactiveWithoutDeletingIt() {
        Machine machine = machine(6L, "Elegoo Saturn", true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(6L, OWNER_ID)).thenReturn(Optional.of(machine));

        machineService.deactivate(6L);

        assertThat(machine.isActive()).isFalse();
        verify(repository).save(machine);
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deactivateShouldThrowNotFoundWhenMachineIsNotOwned() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(8L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> machineService.deactivate(8L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Machine not found with id: 8");
        verify(repository, never()).save(any());
    }

    private Machine machine(Long id, String name, boolean active) {
        Machine machine = new Machine();
        machine.setId(id);
        machine.setName(name);
        machine.setAcquisitionCost(new BigDecimal("799.00"));
        machine.setUsefulLifeHours(new BigDecimal("5000"));
        machine.setPowerWatts(new BigDecimal("120"));
        machine.setMaintenanceCostPerHour(new BigDecimal("0.05"));
        machine.setActive(active);
        machine.setOwner(owner);
        return machine;
    }
}
