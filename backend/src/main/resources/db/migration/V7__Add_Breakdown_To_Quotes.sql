-- Presupuestos auditables (Fase 3)
-- Cada presupuesto guarda su desglose por partida, la energía consumida, el precio del kWh
-- aplicado y el instante para el que se calculó.

ALTER TABLE quotes
ADD COLUMN material_cost NUMERIC(19, 4) CHECK (material_cost >= 0),
ADD COLUMN machine_cost NUMERIC(19, 4) CHECK (machine_cost >= 0),
ADD COLUMN tool_cost NUMERIC(19, 4) CHECK (tool_cost >= 0),
ADD COLUMN labor_cost NUMERIC(19, 4) CHECK (labor_cost >= 0),
ADD COLUMN electricity_cost NUMERIC(19, 4) CHECK (electricity_cost >= 0),
ADD COLUMN energy_kwh NUMERIC(19, 4) CHECK (energy_kwh >= 0),
ADD COLUMN electricity_price_per_kwh NUMERIC(19, 8) CHECK (electricity_price_per_kwh >= 0),
ADD COLUMN safety_amount NUMERIC(19, 4) CHECK (safety_amount >= 0),
ADD COLUMN profit_amount NUMERIC(19, 4) CHECK (profit_amount >= 0),
ADD COLUMN calculated_at TIMESTAMP WITH TIME ZONE;

-- Presupuestos existentes: el margen de seguridad y el beneficio se deducen de forma exacta
-- de los importes guardados. El instante del cálculo no se guardaba; se usa el de creación.
-- El desglose por partida no se puede reconstruir y queda NULL.
UPDATE quotes
SET safety_amount = adjusted_cost - base_cost,
    profit_amount = final_price - adjusted_cost,
    calculated_at = created_at;

ALTER TABLE quotes
ALTER COLUMN safety_amount SET NOT NULL,
ALTER COLUMN profit_amount SET NOT NULL,
ALTER COLUMN calculated_at SET NOT NULL;
