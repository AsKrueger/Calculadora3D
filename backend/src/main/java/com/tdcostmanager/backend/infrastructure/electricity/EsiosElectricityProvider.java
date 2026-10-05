package com.tdcostmanager.backend.infrastructure.electricity;

import com.tdcostmanager.backend.domain.calculation.ElectricityPriceProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class EsiosElectricityProvider implements ElectricityPriceProvider {

    private final EsiosProperties properties;
    private final RestClient restClient;
    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    public EsiosElectricityProvider(EsiosProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public BigDecimal getPricePerKWh(LocalDateTime dateTime) {
        if (properties.getToken() == null || properties.getToken().isBlank()) {
            throw new IllegalStateException("ESIOS_API_TOKEN no está configurado en las variables de entorno");
        }

        String startDateStr = dateTime.toLocalDate().atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endDateStr = dateTime.toLocalDate().atTime(23, 59, 59).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        try {
            EsiosResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/indicators/{id}")
                            .queryParam("start_date", startDateStr)
                            .queryParam("end_date", endDateStr)
                            .queryParam("geo_ids[]", 8741)
                            .build(properties.getIndicatorId()))
                    .header("x-api-key", properties.getToken())
                    .retrieve()
                    .body(EsiosResponse.class);

            if (response == null || response.indicator() == null || response.indicator().values() == null) {
                throw new RuntimeException("Respuesta vacía o malformada de la API de ESIOS");
            }

            int targetHour = dateTime.getHour();
            BigDecimal pricePerMWh = response.indicator().values().stream()
                    .filter(pv -> {
                        try {
                            String dtStr = pv.datetime();
                            int tIndex = dtStr.indexOf('T');
                            if (tIndex != -1 && dtStr.length() >= tIndex + 3) {
                                int hour = Integer.parseInt(dtStr.substring(tIndex + 1, tIndex + 3));
                                return hour == targetHour;
                            }
                        } catch (Exception ignored) {}
                        return false;
                    })
                    .map(EsiosResponse.PriceValue::value)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No se encontró el precio de la electricidad para la hora: " + targetHour));

            if (pricePerMWh == null || pricePerMWh.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("El precio eléctrico devuelto por ESIOS es inválido o negativo");
            }

            return pricePerMWh.divide(THOUSAND, 8, RoundingMode.HALF_UP);

        } catch (RestClientException e) {
            throw new RuntimeException("Fallo en la comunicación técnica HTTP con ESIOS: " + e.getMessage(), e);
        }
    }
}
