package com.tdcostmanager.backend.domain.calculation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Functional interface to provide the price per kilowatt-hour (kWh) for a given instant.
 * Isolated from infrastructure details like ESIOS or HTTP calls.
 */
@FunctionalInterface
public interface ElectricityPriceProvider {
    BigDecimal getPricePerKWh(LocalDateTime dateTime);
}
