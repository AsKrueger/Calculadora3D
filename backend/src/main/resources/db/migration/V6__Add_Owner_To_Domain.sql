-- Issue #42: separación de datos por usuario.
-- Cada material, máquina, herramienta y proyecto pertenece a un usuario.
-- Los presupuestos heredan el propietario a través de su proyecto.

ALTER TABLE materials ADD COLUMN owner_id BIGINT REFERENCES users(id);
ALTER TABLE machines  ADD COLUMN owner_id BIGINT REFERENCES users(id);
ALTER TABLE tools     ADD COLUMN owner_id BIGINT REFERENCES users(id);
ALTER TABLE projects  ADD COLUMN owner_id BIGINT REFERENCES users(id);

-- Datos existentes: se asignan al usuario más antiguo.
-- Si hay datos pero ningún usuario, la migración se detiene con un mensaje claro.
DO $$
DECLARE
    first_user BIGINT;
    orphan_rows BIGINT;
BEGIN
    SELECT MIN(id) INTO first_user FROM users;

    IF first_user IS NOT NULL THEN
        UPDATE materials SET owner_id = first_user WHERE owner_id IS NULL;
        UPDATE machines  SET owner_id = first_user WHERE owner_id IS NULL;
        UPDATE tools     SET owner_id = first_user WHERE owner_id IS NULL;
        UPDATE projects  SET owner_id = first_user WHERE owner_id IS NULL;
    END IF;

    SELECT (SELECT COUNT(*) FROM materials WHERE owner_id IS NULL)
         + (SELECT COUNT(*) FROM machines  WHERE owner_id IS NULL)
         + (SELECT COUNT(*) FROM tools     WHERE owner_id IS NULL)
         + (SELECT COUNT(*) FROM projects  WHERE owner_id IS NULL)
      INTO orphan_rows;

    IF orphan_rows > 0 THEN
        RAISE EXCEPTION 'V6: hay % registros sin propietario y ningún usuario al que asignarlos. Registra un usuario y vuelve a desplegar.', orphan_rows;
    END IF;
END $$;

ALTER TABLE materials ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE machines  ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE tools     ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE projects  ALTER COLUMN owner_id SET NOT NULL;

CREATE INDEX idx_materials_owner ON materials(owner_id);
CREATE INDEX idx_machines_owner  ON machines(owner_id);
CREATE INDEX idx_tools_owner     ON tools(owner_id);
CREATE INDEX idx_projects_owner  ON projects(owner_id);
