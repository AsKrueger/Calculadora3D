# Configuración de Amazon RDS PostgreSQL — 3D Cost Manager

Este documento detalla la configuración, arquitectura de seguridad y parámetros operativos de la base de datos administrada **Amazon RDS PostgreSQL** para el backend de **3D Cost Manager**.

---

## 1. Especificaciones de la Instancia

| Parámetro | Valor Configurado | Justificación |
| :--- | :--- | :--- |
| **Motor** | PostgreSQL | Alineado con la base de datos de desarrollo y Flyway. |
| **Versión** | PostgreSQL 16 | Versión moderna, estable y compatible con características avanzadas. |
| **Clase de Instancia** | `db.t4g.micro` (o `db.t3.micro`) | Coste mínimo, apto para entorno de portfolio y pruebas. |
| **Almacenamiento** | 20 GB (gp2) con autoscale hasta 50 GB | Suficiente para el volumen inicial de datos del gestor de costes. |
| **Multi-AZ** | `false` (Single-AZ) | Desactivado intencionalmente para evitar duplicar costes en infraestructura de portfolio. |
| **Acceso Público** | `false` (No) | Obligatorio. La base de datos no tiene IP pública. |

---

## 2. Red y Seguridad

### 2.1 DB Subnet Group
La instancia RDS se despliega dentro de un **DB Subnet Group** que abarca las dos **Subnets Privadas** creadas en la Issue #21 (`10.0.10.0/24` y `10.0.11.0/24`), garantizando redundancia en distintas Availability Zones (AZ) sin exponerse a Internet.

### 2.2 Security Group (`3d-cost-manager-rds-sg`)
* **Puerto:** `5432` (PostgreSQL).
* **Regla de Entrada:** Permitido **exclusivamente** desde el Security Group de la instancia EC2 (`3d-cost-manager-ec2-sg`).
* **Regla de Entrada desde Internet:** Cero (`0.0.0.0/0` bloqueado).

---

## 3. Conectividad y Migraciones (Flyway)

La aplicación Spring Boot se conectará a RDS mediante la URL JDBC privada proporcionada por el endpoint de CloudFormation/RDS:

```text
jdbc:postgresql://<RDS-ENDPOINT>:5432/costmanager
```

* **Responsabilidad de esquema:** **Flyway** gestionará la creación y evolución de las tablas en producción mediante las migraciones SQL almacenadas en `src/main/resources/db/migration/`.
* **Hibernate:** Operará estrictamente en modo validación (`spring.jpa.hibernate.ddl-auto=validate`), impidiendo que el ORM modifique la estructura de base de datos en producción.

---

## 4. Backups y Retención

* **Retención de Backups Automáticos:** 7 días.
* **Cifrado:** Almacenamiento cifrado mediante AWS KMS (`StorageEncrypted: true`).
* **Actualizaciones menores:** Automatizadas (`AutoMinorVersionUpgrade: true`) durante ventanas de mantenimiento de bajo impacto.

---

## 5. Control de Costes y Recomendaciones Operativas

* **No garantizar gratuidad permanente:** RDS `db.t4g.micro` o `db.t3.micro` puede estar cubierto parcialmente por créditos o Free Tier según la antigüedad de la cuenta AWS, pero el almacenamiento EBS gp2 y posibles transferencias de red no son exentos indefinidamente.
* **Política de apagado:** Para cuentas de portfolio sin uso continuo, se recomienda **detener** o **eliminar** la instancia RDS cuando no esté en demostración, realizando un snapshot final previo si se requiere persistir los datos.
