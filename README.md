# DataFlowHubLibraryJava
project to practice Java without frameworks

# Dominio ‒ Epic E1 (OOP Sólido)

> **Estado**: ✔️ _Completado_  
> **Cobertura mínima garantizada**: **≥ 70 %** instrucciones (JaCoCo)  
> **Build**: ![CI](https://github.com/tu-org/tu-repo/actions/workflows/ci.yml/badge.svg)

---

## 1. Objetivo de la épica

Modelar las entidades centrales del sistema aplicando buenas prácticas OO:

* Encapsulación e **inmutabilidad**
* Contratos coherentes de `equals / hashCode / toString`
* Patrones de diseño apropiados (Builder)
* Cobertura automática de pruebas ≥ 70 %

---

## 2. Modelo de dominio

```mermaid
classDiagram
    direction TB

    class User {
        +UUID id
        +String name
        +String email
        +UserRole role
        +Instant createdAt
        +promoteTo()
    }

    class DataFile {
        +UUID id
        +String originalFilename
        +String storagePath
        +long sizeBytes
        +String checksumSha256
        +Instant uploadedAt
        +User uploadedBy
    }

    class BatchJobConfig {
        +UUID id
        +String name
        +String description
        +int chunkSize
        +ReaderType readerType
        +WriterType writerType
        +boolean allowRestart
        +Instant createdAt
        +boolean active
        +Builder builder()
    }

    class ProcessingRequest {
        +UUID id
        +String title
        +DataFile dataFile
        +Map\<String,String> parameters
        +RequestStatus status
        +Instant createdAt
        +User requestedBy
        +BatchJobConfig batchJobConfig
        +markRunning()/markCompleted()/markFailed()
    }

    class JobExecution {
        +UUID id
        +ProcessingRequest processingRequest
        +Instant startTime
        +Instant endTime
        +ExecutionStatus exitStatus
        +long readCount/writeCount/skipCount
        +String errorMessage
        +finish()
    }

    class Report {
        +UUID id
        +ProcessingRequest processingRequest
        +String storagePath
        +String summaryJson
        +Instant generatedAt
        +User generatedBy
    }

    User "1" --> "many" DataFile   : uploadedBy
    User "1" --> "many" ProcessingRequest : requestedBy
    DataFile "1" --> "many" ProcessingRequest : dataFile
    BatchJobConfig "1" --> "many" ProcessingRequest : template
    ProcessingRequest "1" --> "many" JobExecution  : retries
    ProcessingRequest "1" --> "0..1" Report        : result

---

## 3. Principios aplicados

| Área                 | Decisión                                                                                            |
|----------------------|------------------------------------------------------------------------------------------------------|
| **Identidad**        | Todas las entidades usan `UUID id`; igualdad y hash se basan solo en ese campo.                     |
| **Inmutabilidad**    | `DataFile` y `Report` son _records_ 100 % inmutables.<br>`User`, `ProcessingRequest`, `JobExecution` exponen solo los campos estrictamente mutables (`role`, `status`, métricas). |
| **Validaciones**     | Reglas de negocio comprobadas en constructores y métodos de transición (`assertThrows` cubierto en tests). |
| **Cobertura**        | JaCoCo con umbral ≥ 70 % (INSTRUCTION). El reporte HTML se publica como artefacto en GitHub Actions. |

---

## 4. Patrones usados

| Patrón   | Propósito principal | Pros clave | Contras clave | Dónde se aplica |
|----------|--------------------|------------|---------------|-----------------|
| **Builder** | Construir objetos con muchos parámetros opcionales manteniendo legibilidad. | Lectura fluida, evita telescoping constructors, facilita valores por defecto. | Algo de _boilerplate_ adicional. | `BatchJobConfig` (`builder(String)` + clase estática `Builder`). |
| **Factory** | Ocultar o centralizar la lógica de creación cuando existen varias implementaciones o decisiones condicionales. | Aísla la complejidad de instanciación; favorece SRP. | Puede dispersarse en múltiples métodos si crecen variantes. | **Previsto** para futuras estrategias de `ReaderType` / `WriterType` (no implementado aún, documentado para epic E2). |

---

## 5. Estructura de módulos

core
├─ src/main/java/com/practice/domain/...
├─ src/test/java/com/practice/domain/***Test.java
└─ pom.xml

---

## 6. Cómo compilar y probar

```bash
# Compilar + tests + reporte de cobertura
./mvnw clean verify

# Abrir el reporte
open core/target/site/jacoco/index.html