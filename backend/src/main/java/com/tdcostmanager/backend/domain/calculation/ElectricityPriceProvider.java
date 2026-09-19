package com.tdcostmanager.backend.domain.calculation;

import java.math.BigDecimal;

/**
 * Functional interface to provide the price per kilowatt-hour (kWh).
 * Isolated from infrastructure details like ESIOS or HTTP calls.
 */
@FunctionalInterface
public interface ElectricityPriceProvider {
    BigDecimal getPricePerKWh();
}
