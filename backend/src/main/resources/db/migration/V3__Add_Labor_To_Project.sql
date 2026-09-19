-- Añadir campos de mano de obra a Proyectos
ALTER TABLE projects
ADD COLUMN labor_hours NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (labor_hours >= 0),
ADD COLUMN labor_cost_per_hour NUMERIC(19, 4) NOT NULL DEFAULT 0 CHECK (labor_cost_per_hour >= 0);
