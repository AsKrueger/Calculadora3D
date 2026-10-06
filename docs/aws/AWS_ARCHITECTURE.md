# Arquitectura de Infraestructura AWS — 3D Cost Manager

Este documento detalla la arquitectura de red, topología, seguridad y control de costes en AWS para el despliegue del backend de **3D Cost Manager**.

---

## 1. Estado de Implementación

| Componente | Estado | Descripción |
| :--- | :--- | :--- |
| **Región AWS** | **IMPLEMENTADO (Diseño)** | `eu-west-1` (Irlanda) |
| **AWS Budgets** | **IMPLEMENTADO (Diseño)** | Alertas automáticas al 85% y 100% |
| **Amazon ECR** | **IMPLEMENTADO (Diseño)** | Repositorio privado `3d-cost-manager-backend` |
| **IAM Instance Profile** | **IMPLEMENTADO (Diseño)** | Mínimo privilegio (ECR pull + Parameter Store read) |
| **VPC & Subnets** | **IMPLEMENTADO (Diseño)** | VPC aislada con Subnet Pública (EC2) y Subnets Privadas (RDS) |
| **Security Groups** | **IMPLEMENTADO (Diseño)** | EC2 expone 8080; RDS accesible exclusivamente desde el SG de EC2 en puerto 5432 |
| **Amazon RDS PostgreSQL** | **PLANIFICADO (Issue #22)** | Base de datos gestionada en subred privada |
| **Amazon EC2** | **PLANIFICADO (Issue #23)** | Instancia compute con Docker y Spring Boot |
| **Parameter Store** | **PLANIFICADO (Issue #24)** | Configuración y secretos (`SecureString`) |

---

## 2. Topología de Red (VPC)

Se ha diseñado una VPC dedicada (`10.0.0.0/16`) en la región `eu-west-1` que separa estrictamente los componentes expuestos a Internet de los recursos de persistencia internos.

```text
VPC (10.0.0.0/16)
│
├── Public Subnet (10.0.1.0/24)
│      └── Internet Gateway (IGW)
│      └── Instancia EC2 (Spring Boot Docker) [Puerto 8080 / 443]
│
└── Private Subnets (10.0.10.0/24, 10.0.11.0/24)
       └── Amazon RDS PostgreSQL [Puerto 5432 (Cerrado a Internet)]
```

### Tabla de Rutas (Routing)
* **Public Route Table:** Enruta el tráfico hacia `0.0.0.0/0` a través del Internet Gateway (IGW).
* **Private Route Table:** Sin salida directa a Internet para las subnets privadas de RDS, maximizando la seguridad de la base de datos.

---

## 3. Security Groups y Reglas de Firewall

### 3.1 EC2 Security Group (`3d-cost-manager-ec2-sg`)
* **Inbound (Entrada):**
  * `TCP 8080` (o `443`) desde `0.0.0.0/0` (tráfico HTTP/HTTPS para la API REST).
  * `TCP 22` (Opcional/Restringido) o uso exclusivo de AWS Systems Manager Session Manager (recomendado para evitar abrir el puerto SSH).
* **Outbound (Salida):**
  * Todo el tráfico permitido (`0.0.0.0/0`) para permitir llamadas a la API de ESIOS, descarga de imágenes desde ECR, lectura de Parameter Store y envío de logs a CloudWatch.

### 3.2 RDS Security Group (`3d-cost-manager-rds-sg`)
* **Inbound (Entrada):**
  * `TCP 5432` (**Exclusivamente** desde el Security Group de EC2 `3d-cost-manager-ec2-sg`). No se permite ninguna regla desde `0.0.0.0/0`.
* **Outbound (Salida):**
  * Tráfico estándar de base de datos.

---

## 4. Consideraciones de Costes y Control

* **Uso de NAT Gateway:** Se ha prescindido de NAT Gateway en la arquitectura inicial para evitar costes fijos innecesarios (un NAT Gateway ronda los ~$32/mes más transferencia). Al no requerir salida a Internet iniciada desde RDS (las actualizaciones se gestionan por mantenimiento administrado de AWS), se eliminan costes fijos de red.
* **Presupuesto:** AWS Budgets vigilará el consumo total de la cuenta.
* **Instancias:** Uso previsto de tipos orientados a capa gratuita o instancias económicas (`t4g.micro` / `db.t4g.micro`).
