package com.tdcostmanager.backend.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceSecretValidationTest {

    private static String base64OfBytes(int length) {
        return Base64.getEncoder().encodeToString(new byte[length]);
    }

    @Test
    void falla_si_el_secreto_no_esta_definido() {
        assertThatThrownBy(() -> JwtService.buildSigningKey(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> JwtService.buildSigningKey("   "))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void falla_si_el_secreto_no_es_base64() {
        assertThatThrownBy(() -> JwtService.buildSigningKey("escribe_aqui_un_secreto_seguro_y_largo_de_32_caracteres"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    void falla_si_el_secreto_tiene_menos_de_256_bits() {
        assertThatThrownBy(() -> JwtService.buildSigningKey(base64OfBytes(31)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("248 bits");
    }

    @Test
    void acepta_un_secreto_base64_de_256_bits() {
        assertThat(JwtService.buildSigningKey(base64OfBytes(32))).isNotNull();
    }

    @Test
    void acepta_el_secreto_del_env_example() {
        assertThat(JwtService.buildSigningKey("bG9jYWwtZGV2LW9ubHktY2hhbmdlLW1lLTMyLWJ5dGVzLWtleSEh")).isNotNull();
    }
}
