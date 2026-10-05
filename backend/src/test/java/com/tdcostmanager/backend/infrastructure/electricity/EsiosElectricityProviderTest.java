package com.tdcostmanager.backend.infrastructure.electricity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import static org.hamcrest.Matchers.containsString;

class EsiosElectricityProviderTest {

    private EsiosProperties properties;
    private EsiosElectricityProvider provider;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        properties = new EsiosProperties();
        properties.setUrl("https://api.esios.ree.es");
        properties.setToken("mock-valid-token");
        properties.setIndicatorId(1001);

        RestClient.Builder builder = RestClient.builder();
        // El orden es CRÍTICO: bindTo inyecta el MockClientHttpRequestFactory en el builder.
        mockServer = MockRestServiceServer.bindTo(builder).build();
        
        // El RestClient para el test se construye DESPUÉS del bindTo.
        RestClient restClient = builder.baseUrl(properties.getUrl()).build();
        
        provider = new EsiosElectricityProvider(properties, restClient);
    }

    @Test
    void shouldThrowExceptionIfTokenIsMissing() {
        properties.setToken("");
        LocalDateTime dateTime = LocalDateTime.of(2026, 9, 17, 14, 0);

        assertThatThrownBy(() -> provider.getPricePerKWh(dateTime))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ESIOS_API_TOKEN no está configurado");
    }

    @Test
    void shouldParseValidEsiosJsonAndReturnConvertedPricePerKWh() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 9, 17, 14, 30);

        String mockResponseJson = """
                {
                  "indicator": {
                    "name": "PVPC energía activa",
                    "values": [
                      {
                        "value": 150.5000,
                        "datetime": "2026-09-17T14:00:00.000+02:00",
                        "geo_id": 8741
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo(containsString("/indicators/1001")))
                .andExpect(header("x-api-key", "mock-valid-token"))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        BigDecimal resultPrice = provider.getPricePerKWh(dateTime);

        assertThat(resultPrice).isEqualByComparingTo("0.15050000");
        mockServer.verify();
    }

    @Test
    void shouldHandleUnauthorizedHttpErrorControladamente() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 9, 17, 14, 0);

        mockServer.expect(requestTo(containsString("/indicators/1001")))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> provider.getPricePerKWh(dateTime))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fallo en la comunicación técnica HTTP con ESIOS");
    }
}
