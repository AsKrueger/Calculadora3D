package com.tdcostmanager.backend.infrastructure.electricity;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test de integración controlado para verificación manual contra la API real de ESIOS (Fase 9).
 * Deshabilitado por defecto para no romper la suite automática de compilación si no hay token.
 */
class EsiosElectricityProviderRealIntegrationTest {

    @Test
    @Disabled("Deshabilitado por defecto. Activar manualmente para pruebas reales con un token válido configurado.")
    void verifyRealEsiosConnectionManual() {
        EsiosProperties properties = new EsiosProperties();
        properties.setUrl("https://api.esios.ree.es");
        properties.setToken(System.getenv("ESIOS_API_TOKEN"));
        properties.setIndicatorId(1001);
        properties.setConnectTimeoutMs(5000);
        properties.setReadTimeoutMs(5000);

        // En prueba real, usamos un RestClient real
        RestClient realClient = RestClient.builder().baseUrl(properties.getUrl()).build();
        EsiosElectricityProvider provider = new EsiosElectricityProvider(properties, realClient);
        
        LocalDateTime todayAtNoon = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        System.out.println("--- INICIANDO VERIFICACIÓN MANUAL CONTRA LA API REAL DE ESIOS ---");
        System.out.println("Fecha y Hora de la consulta: " + todayAtNoon);
        
        try {
            BigDecimal priceKWh = provider.getPricePerKWh(todayAtNoon);
            System.out.println("RESULTADO OBTENIDO CON ÉXITO: " + priceKWh + " €/kWh");
        } catch (Exception e) {
            System.err.println("FALLO CONTROLADO EN LA PRUEBA REAL: " + e.getMessage());
            throw e;
        }
    }
}
