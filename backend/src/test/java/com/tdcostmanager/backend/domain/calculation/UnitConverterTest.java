package com.tdcostmanager.backend.domain.calculation;

import com.tdcostmanager.backend.domain.model.UnitType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnitConverterTest {

    @ParameterizedTest
    @CsvSource({
        "1000, G, KG, 1.00000000",
        "1, KG, G, 1000.00000000",
        "500, G, KG, 0.50000000",
        "0.25, KG, G, 250.00000000",
        "0, G, KG, 0.00000000",
        "123.456, G, KG, 0.12345600",
        "1000, ML, L, 1.00000000",
        "1, L, ML, 1000.00000000",
        "250, ML, L, 0.25000000",
        "0.75, L, ML, 750.00000000",
        "10, UNIT, UNIT, 10.00000000",
        "5, H, H, 5.00000000"
    })
    void shouldConvertUnits(String value, UnitType from, UnitType to, String expected) {
        BigDecimal result = UnitConverter.convert(new BigDecimal(value), from, to);
        assertThat(result).isEqualByComparingTo(expected);
        assertThat(result.scale()).isEqualTo(8);
    }

    @Test
    void shouldMaintainValueForSameUnit() {
        BigDecimal value = new BigDecimal("10.5");
        BigDecimal result = UnitConverter.convert(value, UnitType.UNIT, UnitType.UNIT);
        assertThat(result).isEqualByComparingTo("10.5");
        assertThat(result.scale()).isEqualTo(8);
        
        result = UnitConverter.convert(value, UnitType.ML, UnitType.ML);
        assertThat(result).isEqualByComparingTo("10.5");
    }

    @Test
    void shouldThrowExceptionForIncompatibleUnits() {
        assertThatThrownBy(() -> UnitConverter.convert(BigDecimal.ONE, UnitType.G, UnitType.ML))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Para convertir g a ml el material necesita una densidad (g/ml)");

        assertThatThrownBy(() -> UnitConverter.convert(BigDecimal.ONE, UnitType.KG, UnitType.UNIT, BigDecimal.ONE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No se puede convertir kg a unidades");
    }

    @Test
    void shouldConvertBetweenMassAndVolumeWithDensity() {
        BigDecimal resinDensity = new BigDecimal("1.1");

        // 110 g de resina a 1,1 g/ml son 100 ml, es decir, 0,1 L
        assertThat(UnitConverter.convert(new BigDecimal("110"), UnitType.G, UnitType.ML, resinDensity)).isEqualByComparingTo("100");
        assertThat(UnitConverter.convert(new BigDecimal("110"), UnitType.G, UnitType.L, resinDensity)).isEqualByComparingTo("0.1");
        // 0,5 L a 1,1 g/ml son 550 g, es decir, 0,55 kg
        assertThat(UnitConverter.convert(new BigDecimal("0.5"), UnitType.L, UnitType.G, resinDensity)).isEqualByComparingTo("550");
        assertThat(UnitConverter.convert(new BigDecimal("0.5"), UnitType.L, UnitType.KG, resinDensity)).isEqualByComparingTo("0.55");
    }

    @Test
    void canConvertReportsWhetherDensityIsNeeded() {
        assertThat(UnitConverter.canConvert(UnitType.G, UnitType.KG, null)).isTrue();
        assertThat(UnitConverter.canConvert(UnitType.G, UnitType.ML, null)).isFalse();
        assertThat(UnitConverter.canConvert(UnitType.G, UnitType.ML, new BigDecimal("1.24"))).isTrue();
        assertThat(UnitConverter.canConvert(UnitType.UNIT, UnitType.G, new BigDecimal("1.24"))).isFalse();
    }

    @Test
    void shouldThrowExceptionForNullInputs() {
        assertThatThrownBy(() -> UnitConverter.convert(null, UnitType.G, UnitType.KG))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UnitConverter.convert(BigDecimal.ONE, null, UnitType.KG))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UnitConverter.convert(BigDecimal.ONE, UnitType.G, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldHandleHighPrecision() {
        BigDecimal value = new BigDecimal("0.000123456789");
        BigDecimal result = UnitConverter.convert(value, UnitType.G, UnitType.KG);
        // 0.000000123456789 rounded to 8 decimals -> 0.00000012
        assertThat(result).isEqualByComparingTo("0.00000012");
    }
}
