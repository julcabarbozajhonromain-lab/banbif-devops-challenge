# BanBif DevOps Technical Challenge - Spring Boot 3 & AWS ECS Fargate

Solución integral de infraestructura como código (IaC), contenedorización y CI/CD automatizado para el despliegue de un microservicio en **Java 17 / Spring Boot 3** sobre **Amazon ECS (AWS Fargate)** con alta disponibilidad y buenas prácticas bancarias.

---

## 1. Arquitectura de la Solución

El flujo completo de integración y despliegue continuo (CI/CD) orquesta la construcción de la aplicación, el aprovisionamiento de recursos modulares en AWS mediante Terraform y la exposición del servicio a través de un Application Load Balancer (ALB).

```mermaid
flowchart TD
    subgraph CI_CD["GitHub Actions Pipeline"]
        GHA[Push to main / workflow_dispatch]
        JOB1["Job 1: build-and-push"]
        JOB2["Job 2: deploy-ecs"]
        GHA --> JOB1
        JOB1 -->|outputs.image_uri| JOB2
    end

    subgraph AWS_ECR["Amazon ECR"]
        ECR_REPO[("ECR Repository: banbif-java-app")]
    end

    subgraph AWS_INFRA["AWS Cloud (Sandbox us-east-1)"]
        subgraph VPC["VPC 10.0.0.0/16"]
            IGW["Internet Gateway"]
            subgraph SUBNETS["Public Subnets (us-east-1a & us-east-1b)"]
                ALB["Application Load Balancer (Port 80)"]
                TG["Target Group (Port 8080)"]
                ECS_SVC["ECS Service (Fargate desired=1)"]
                TASK["Task: banbif-java-app (appuser:8080)"]
            end
        end
        CW["CloudWatch Logs: /ecs/banbif-java-app"]
        S3_BACKEND[("S3 Remote State")]
    end

    JOB1 -->|Terraform Apply| ECR_REPO
    JOB1 -->|Docker Build & Push| ECR_REPO
    JOB2 -->|Terraform Apply| VPC
    JOB2 -->|Pull Image| ECR_REPO
    ALB --> TG
    TG -->|HealthCheck /actuator/health| TASK
    ECS_SVC --> TASK
    TASK --> CW
    JOB2 -->|cURL Validate HTTP 200| ALB
```

---

## 2. Justificación Técnica de Decisiones de Arquitectura

| Componente | Decisión Técnica | Justificación Técnica & Buenas Prácticas |
| :--- | :--- | :--- |
| **Cómputo** | **AWS ECS Fargate** frente a EKS (Kubernetes) | Para microservicios desacoplados y cuentas sandbox, Fargate elimina el *overhead* operativo de administrar el plano de control (Control Plane de EKS cuesta ~$73/mes fijo) y la gestión de nodos EC2, parches del SO y agentes. Ofrece aislamiento a nivel de kernel por tarea y menor *time-to-market*. |
| **Contenedor** | **Multi-Stage Build** (Eclipse Temurin 17 JRE Alpine) | Separa el entorno de compilación pesado (`maven:3.9`) del runtime ligero de producción. Reduce el tamaño de la imagen final a < 180MB y minimiza la superficie de ataque CVE al omitir herramientas de compilación y shell innecesarios. |
| **Seguridad** | **Usuario no privilegiado (`appuser`)** | Ejecutar contenedores como `root` es un riesgo crítico. En el Dockerfile se crea `appuser` con UID sin privilegios y permisos restringidos exclusivamente a la lectura del artefacto. |
| **Estado IaC** | **S3 Backend dinámico** con bloques vacíos | Desacopla la infraestructura en dos capas independientes (`01-ecr` y `02-ecs-cluster`), permitiendo que el estado remoto se configure mediante `-backend-config` en el pipeline sin quemar nombres de buckets sensibles en el código. |
| **Redes** | **Seguridad por capas (Security Groups)** | El contenedor ECS no está expuesto directamente al mundo; su Security Group únicamente autoriza tráfico TCP entrante en el puerto `8080` originado desde el Security Group del ALB. |

---

## 3. Configuración de Secretos en GitHub Actions

En el repositorio de GitHub, diríjase a **Settings > Secrets and variables > Actions > New repository secret** y configure las siguientes variables:

| Secret Name | Descripción | Ejemplo / Valor |
| :--- | :--- | :--- |
| `AWS_ACCESS_KEY_ID` | Identificador de acceso IAM con permisos para ECR, ECS, VPC, IAM, ALB y CloudWatch. | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | Clave secreta asociada al usuario IAM. | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |
| `AWS_REGION` | Región AWS designada para el despliegue. | `us-east-1` |
| `TF_STATE_BUCKET` | Nombre del bucket S3 previamente creado para almacenar los estados de Terraform. | `banbif-tfstate-sandbox-bucket` |

> **Nota:** El bucket S3 especificado en `TF_STATE_BUCKET` debe existir previamente en la cuenta y región correspondiente con versionado y cifrado habilitado.

---

## 4. Endpoints y Guía de Verificación

Una vez finalizado el pipeline de GitHub Actions, el output expondrá el DNS público del Load Balancer.

### 4.1. Verificación de Salud (Healthcheck)
Valida el estado operativo y los probes del microservicio:
```bash
curl -i http://<ALB_DNS_NAME>/actuator/health
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### 4.2. Endpoint de Bienvenida
```bash
curl -i http://<ALB_DNS_NAME>/api/v1/hello
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "status": "OK",
  "message": "Bienvenido al API de BanBif DevOps Challenge",
  "timestamp": "2026-11-12T16:45:00.000Z"
}
```

### 4.3. Listado de Ítems (GET)
```bash
curl -i http://<ALB_DNS_NAME>/api/v1/items
```
**Respuesta esperada (HTTP 200 OK):**
```json
[
  {
    "id": 1,
    "name": "Cuenta Digital BanBif",
    "category": "Productos Bancarios",
    "createdAt": "2026-11-12T16:45:00.000Z"
  },
  {
    "id": 2,
    "name": "Tarjeta de Crédito Signature",
    "category": "Tarjetas",
    "createdAt": "2026-11-12T16:45:00.000Z"
  }
]
```

### 4.4. Creación de un Ítem (POST)
```bash
curl -i -X POST http://<ALB_DNS_NAME>/api/v1/items \
  -H "Content-Type: application/json" \
  -d '{"name": "Préstamo Efectivo BanBif", "category": "Créditos"}'
```
**Respuesta esperada (HTTP 201 Created):**
```json
{
  "id": 3,
  "name": "Préstamo Efectivo BanBif",
  "category": "Créditos",
  "createdAt": "2026-11-12T16:46:12.123Z"
}
```

---

## 5. Limpieza y Destrucción de Recursos (Ahorro de Costos Sandbox)

Para evitar consumos no deseados de saldo tras la evaluación, se ha provisto el workflow manual **`destroy.yml`**:

1. Vaya a la pestaña **Actions** en GitHub.
2. Seleccione el workflow **`Cleanup / Destroy Sandbox Infrastructure`**.
3. Haga clic en **Run workflow**.
4. El pipeline ejecutará `terraform destroy` ordenadamente:
   - Primero destruye `infra/02-ecs-cluster` (Servicio ECS, Tareas, Load Balancer, VPC y networking).
   - Seguidamente destruye `infra/01-ecr` (Repositorio ECR y sus imágenes contenidas gracias a `force_delete = true`).
