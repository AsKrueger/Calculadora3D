package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.MaterialCreateRequest;
import com.tdcostmanager.backend.application.dto.MaterialResponse;
import com.tdcostmanager.backend.application.dto.MaterialUpdateRequest;
import com.tdcostmanager.backend.domain.model.Material;
import com.tdcostmanager.backend.domain.model.MaterialCategory;
import com.tdcostmanager.backend.domain.model.UnitType;
import com.tdcostmanager.backend.domain.model.User;
import com.tdcostmanager.backend.domain.repository.MaterialRepository;
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
class MaterialServiceTest {

    private static final Long OWNER_ID = 7L;

    @Mock
    private MaterialRepository repository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private MaterialService materialService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(OWNER_ID);
    }

    @Test
    void findAllShouldOnlyReturnMaterialsOfCurrentUser() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findAllByOwnerId(OWNER_ID)).thenReturn(List.of(
                material(1L, "PLA negro", true),
                material(2L, "Resina gris", false)
        ));

        List<MaterialResponse> result = materialService.findAll();

        assertThat(result).extracting(MaterialResponse::name).containsExactly("PLA negro", "Resina gris");
        assertThat(result).extracting(MaterialResponse::active).containsExactly(true, false);
        verify(repository, never()).findAll();
    }

    @Test
    void findByIdShouldMapAllFieldsOfOwnedMaterial() {
        Material material = material(3L, "PETG", true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(3L, OWNER_ID)).thenReturn(Optional.of(material));

        MaterialResponse response = materialService.findById(3L);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("PETG");
        assertThat(response.description()).isEqualTo("Desc PETG");
        assertThat(response.purchasePrice()).isEqualByComparingTo("24.99");
        assertThat(response.quantity()).isEqualByComparingTo("1000");
        assertThat(response.unit()).isEqualTo(UnitType.G);
        assertThat(response.category()).isEqualTo(MaterialCategory.FILAMENT);
        assertThat(response.densityGPerMl()).isEqualByComparingTo("1.24");
        assertThat(response.active()).isTrue();
    }

    @Test
    void findByIdShouldThrowNotFoundWhenMaterialBelongsToAnotherUser() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(99L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Material not found with id: 99");
    }

    @Test
    void createShouldAssignCurrentUserAsOwnerAndStartActive() {
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(repository.save(any(Material.class))).thenAnswer(invocation -> {
            Material saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        MaterialCreateRequest request = new MaterialCreateRequest(
                "Resina estándar", "Gris, 1 litro",
                new BigDecimal("32.90"), new BigDecimal("1"), UnitType.L,
                MaterialCategory.RESIN, new BigDecimal("1.10")
        );

        MaterialResponse response = materialService.create(request);

        ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
        verify(repository).save(captor.capture());
        Material saved = captor.getValue();
        assertThat(saved.getOwner()).isSameAs(owner);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getName()).isEqualTo("Resina estándar");
        assertThat(saved.getPurchasePrice()).isEqualByComparingTo("32.90");
        assertThat(saved.getUnit()).isEqualTo(UnitType.L);
        assertThat(saved.getCategory()).isEqualTo(MaterialCategory.RESIN);
        assertThat(saved.getDensityGPerMl()).isEqualByComparingTo("1.10");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.active()).isTrue();
    }

    @Test
    void createShouldAllowMaterialsWithoutDensity() {
        when(currentUserProvider.getCurrentUser()).thenReturn(owner);
        when(repository.save(any(Material.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaterialCreateRequest request = new MaterialCreateRequest(
                "Lija 400", null,
                new BigDecimal("6"), new BigDecimal("20"), UnitType.UNIT,
                MaterialCategory.SANDPAPER, null
        );

        MaterialResponse response = materialService.create(request);

        assertThat(response.densityGPerMl()).isNull();
        assertThat(response.description()).isNull();
        assertThat(response.unit()).isEqualTo(UnitType.UNIT);
    }

    @Test
    void updateShouldOverwriteFieldsIncludingActiveFlag() {
        Material material = material(4L, "PLA negro", true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(4L, OWNER_ID)).thenReturn(Optional.of(material));
        when(repository.save(material)).thenReturn(material);

        MaterialUpdateRequest request = new MaterialUpdateRequest(
                "PLA blanco", null,
                new BigDecimal("19.50"), new BigDecimal("1"), UnitType.KG,
                MaterialCategory.FILAMENT, false, null
        );

        MaterialResponse response = materialService.update(4L, request);

        assertThat(response.name()).isEqualTo("PLA blanco");
        assertThat(response.description()).isNull();
        assertThat(response.purchasePrice()).isEqualByComparingTo("19.50");
        assertThat(response.quantity()).isEqualByComparingTo("1");
        assertThat(response.unit()).isEqualTo(UnitType.KG);
        assertThat(response.densityGPerMl()).isNull();
        assertThat(response.active()).isFalse();
        assertThat(material.getOwner()).isSameAs(owner);
    }

    @Test
    void updateShouldNotSaveWhenMaterialIsNotOwned() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(5L, OWNER_ID)).thenReturn(Optional.empty());

        MaterialUpdateRequest request = new MaterialUpdateRequest(
                "X", null, BigDecimal.ONE, BigDecimal.ONE, UnitType.G,
                MaterialCategory.OTHER, true, null
        );

        assertThatThrownBy(() -> materialService.update(5L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Material not found with id: 5");
        verify(repository, never()).save(any());
    }

    @Test
    void deactivateShouldMarkMaterialInactiveWithoutDeletingIt() {
        Material material = material(6L, "Pintura acrílica", true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(6L, OWNER_ID)).thenReturn(Optional.of(material));

        materialService.deactivate(6L);

        assertThat(material.isActive()).isFalse();
        verify(repository).save(material);
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deactivateShouldThrowNotFoundWhenMaterialIsNotOwned() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(repository.findByIdAndOwnerId(8L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.deactivate(8L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Material not found with id: 8");
        verify(repository, never()).save(any());
    }

    private Material material(Long id, String name, boolean active) {
        Material material = new Material();
        material.setId(id);
        material.setName(name);
        material.setDescription("Desc " + name);
        material.setPurchasePrice(new BigDecimal("24.99"));
        material.setQuantity(new BigDecimal("1000"));
        material.setUnit(UnitType.G);
        material.setCategory(MaterialCategory.FILAMENT);
        material.setDensityGPerMl(new BigDecimal("1.24"));
        material.setActive(active);
        material.setOwner(owner);
        return material;
    }
}
