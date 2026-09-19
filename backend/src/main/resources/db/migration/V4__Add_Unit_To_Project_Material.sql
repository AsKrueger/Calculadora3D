-- Añadir unidad a la relación proyecto-material para permitir conversiones
ALTER TABLE project_materials
ADD COLUMN unit VARCHAR(20) NOT NULL DEFAULT 'G';
