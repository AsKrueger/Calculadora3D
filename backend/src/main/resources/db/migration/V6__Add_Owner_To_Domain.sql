-- Separación de datos por usuario (Issue #42)
-- Cada material, máquina, herramienta y proyecto pertenece a un usuario.
-- Los presupuestos heredan el propietario a través de su proyecto.

ALTER TABLE materials ADD COLUMN owner_id BIGINT REFERENCES users(id);
ALTER TABLE machines  ADD COLUMN owner_id BIGINT REFERENCES users(id);
ALTER TABLE tools     ADD COLUMN owner_id BIGINT REFERENCES users(id);
ALTER TABLE projects  ADD COLUMN owner_id BIGINT REFERENCES users(id);

-- Los registros anteriores a esta migración se asignan al usuario más antiguo.
-- Si hay datos pero ningún usuario, se crea un usuario deshabilitado que los conserva
-- (no puede iniciar sesión: su hash no es un BCrypt válido) para no perder información.
DO $$
DECLARE
    legacy_owner_id BIGINT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM materials WHERE owner_id IS NULL)
       AND NOT EXISTS (SELECT 1 FROM machines WHERE owner_id IS NULL)
       AND NOT EXISTS (SELECT 1 FROM tools WHERE owner_id IS NULL)
       AND NOT EXISTS (SELECT 1 FROM projects WHERE owner_id IS NULL) THEN
        RETURN;
    END IF;

    SELECT id INTO legacy_owner_id FROM users ORDER BY id LIMIT 1;

    IF legacy_owner_id IS NULL THEN
        INSERT INTO users (email, password_hash, role, enabled, created_at, updated_at)
        VALUES ('legacy-owner@localhost', '!disabled', 'USER', FALSE, NOW(), NOW())
        RETURNING id INTO legacy_owner_id;
    END IF;

    UPDATE materials SET owner_id = legacy_owner_id WHERE owner_id IS NULL;
    UPDATE machines  SET owner_id = legacy_owner_id WHERE owner_id IS NULL;
    UPDATE tools     SET owner_id = legacy_owner_id WHERE owner_id IS NULL;
    UPDATE projects  SET owner_id = legacy_owner_id WHERE owner_id IS NULL;
END $$;

ALTER TABLE materials ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE machines  ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE tools     ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE projects  ALTER COLUMN owner_id SET NOT NULL;

CREATE INDEX idx_materials_owner_id ON materials(owner_id);
CREATE INDEX idx_machines_owner_id  ON machines(owner_id);
CREATE INDEX idx_tools_owner_id     ON tools(owner_id);
CREATE INDEX idx_projects_owner_id  ON projects(owner_id);
