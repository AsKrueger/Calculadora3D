-- Proyectos editables y validaciones coherentes (Fase 5)

-- 1. Las líneas de un proyecto deben tener cantidad mayor que 0, igual que exige la API.
--    Una línea con cantidad 0 no aporta nada al coste, así que se elimina antes de añadir la restricción.
DELETE FROM project_materials WHERE quantity_used <= 0;
DELETE FROM project_machines WHERE estimated_hours <= 0;
DELETE FROM project_tools WHERE uses <= 0;

ALTER TABLE project_materials DROP CONSTRAINT IF EXISTS project_materials_quantity_used_check;
ALTER TABLE project_machines DROP CONSTRAINT IF EXISTS project_machines_estimated_hours_check;
ALTER TABLE project_tools DROP CONSTRAINT IF EXISTS project_tools_uses_check;

ALTER TABLE project_materials ADD CONSTRAINT project_materials_quantity_used_positive CHECK (quantity_used > 0);
ALTER TABLE project_machines ADD CONSTRAINT project_machines_estimated_hours_positive CHECK (estimated_hours > 0);
ALTER TABLE project_tools ADD CONSTRAINT project_tools_uses_positive CHECK (uses > 0);

-- 2. Densidad opcional del material, para convertir entre masa y volumen
--    (por ejemplo, resina comprada en litros y usada en gramos).
ALTER TABLE materials ADD COLUMN density_g_per_ml NUMERIC(19, 6) CHECK (density_g_per_ml > 0);
