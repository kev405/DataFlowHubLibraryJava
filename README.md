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

```

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

```text
my-app
├── pom.xml                     # POM raíz (packaging = pom)
├── core                        # módulo de dominio (épica E1)
│   ├── pom.xml                 # dependencias JaCoCo, EqualsVerifier, etc.
│   └── src
│       ├── main
│       │   └── java
│       │       └── com/practice/domain/...
│       └── test
│           └── java
│               └── com/practice/domain/...
└── lib                         # reservado para la épica E2 (utilidades)
    ├── pom.xml
    └── src/...

```

---

## 6. Cómo compilar y probar

```bash
# Compilar + tests + reporte de cobertura
./mvnw clean verify

# Abrir el reporte
open core/target/site/jacoco/index.html

```

---

### InMemoryCache – LRU

```java
import com.utils.cache.LruCache;
import java.util.Optional;
import java.util.UUID;

LruCache<UUID, User> cache = new LruCache<>(1_000);

cache.put(user.getId(), user);
Optional<User> maybe = cache.get(user.getId());

System.out.println("hits=" + cache.hitCount() + ", miss=" + cache.missCount());

```

Si insertas 1 001 usuarios, el más antiguo se descarta automáticamente.

---

## 7. Diseño interno

| Componente      | Elección                           | Razón                                                          |
| --------------- | ---------------------------------- | -------------------------------------------------------------- |
| Contenedor base | `LinkedHashMap` *access-order*     | Reordenamiento automático y `removeEldestEntry` para expulsión |
| Concurrencia    | `ReentrantReadWriteLock`           | Muchos lectores, un escritor                                   |
| Métricas        | `volatile long hitCount/missCount` | Lectura coherente sin bloqueo                                  |


---

## 8. ¿Para qué sirve?

| Caso de uso                                             | Beneficio                                    |
|---------------------------------------------------------|----------------------------------------------|
| Resolver **User** por **UUID** miles de veces en un job | Reduce I/O a BD o estructuras de gran tamaño |
| Guardar configuraciones repetidamente leídas            | Evita parseo / IO redundante                 |

---

## 9. LRU vs TTL (futuro)

| Estrategia | Ventaja principal                                      | Uso recomendado                                  |
| ---------- | ------------------------------------------------------ | ------------------------------------------------ |
| **LRU**    | Mantiene en memoria los elementos usados recientemente | Lecturas muy frecuentes con cache de tamaño fijo |
| **TTL**    | Expira elementos tras X tiempo, sin importar el uso    | Configuraciones que cambian periódicamente       |

(Se incluye TtlCache<K,V> como referencia, aún no productiva.)

---

### Resultados definitivos JMH (HU F1-10)

_Comando ejecutado_  
```bash
java -jar lib/target/benchmarks.jar \
     -rf CSV -rff bench.csv \
     -tu ms -f 1 -wi 2 -i 3 -w 2s -r 2s

```

| Operación (dataset = 1 000 000) | ArrayList<br>(ms / op)  | LinkedList<br>(ms / op) | Ganador        |
| ------------------------------- | ----------------------- | ----------------------- | -------------- |
| **addLast** `list.add(x)`       | **0.000020 ± 0.000120** | 0.000138 ± 0.000025     | **ArrayList**  |
| **addFirst** `list.add(0,x)`    | 0.078022 ± 0.061059     | **0.000134 ± 0.000031** | **LinkedList** |
| **random get** `list.get(rnd)`  | **0.000025 ± 0.000039** | 0.625891 ± 2.060440     | **ArrayList**  |
| **full iteration** `for-each`   | **1.582198 ± 4.929957** | 3.701233 ± 29.944820    | **ArrayList**  |


Interpretación rápida

ArrayList domina en acceso aleatorio (get), inserción al final y recorrido secuencial.

LinkedList solo gana en inserción al inicio de la lista (addFirst) con colecciones muy grandes.

Para la mayoría de casos de lectura y escritura al final, ArrayList es la opción recomendada.

---

### CollectionUtils – métodos con wildcards

| Método | Firma | PECS aplicado |
|--------|-------|--------------|
| `copy` | `<T> List<T> copy(List<? extends T> src)` | **Producer Extends** |
| `addAll` | `<T> void addAll(Collection<? super T> dst, Collection<? extends T> src)` | **Consumer Super / Producer Extends** |
| `deepUnmodifiable` | `<K,V> Map<K,V> deepUnmodifiable(Map<? extends K,? extends V> src)` | Ambos “extends” |

**Regla PECS**: *Producer Extends* (fuentes producen objetos → `? extends`), *Consumer Super* (destinos consumen objetos → `? super`).  
Esto permite una API flexible y segura, sin _casts_ ni _raw types_.

---

### Jerarquía de Excepciones

```mermaid
classDiagram
    Exception <|-- DataflowException
    DataflowException <|-- DomainException
    RuntimeException <|-- InfraException
    DomainException <|-- InvalidFileFormatException
    DomainException <|-- UserNotFoundException
    InfraException <|-- DatabaseUnavailableException

```

---

### Ejemplo de salida `ErrorHandler`

```text
[DB_DOWN] DatabaseUnavailableException: db down
com.dataflowhub.core.exception.DatabaseUnavailableException: db down
    at com.dataflowhub.core.repository.JobRepository.save(JobRepository.java:42)
    at com.dataflowhub.core.service.JobService.create(JobService.java:57)
    at ...

```

*Prefijo `[CODE]` permite dashboards rápidos en Kibana / Grafana.*  
Opción **verbose=false** limita a 5 líneas de stack para logs limpios.

---

### Tests de Excepciones (HU F1-14)

| Método bajo prueba | Excepción esperada | Test                                                                 |
|--------------------|--------------------|----------------------------------------------------------------------|
| `CsvParser.parse()`             | `InvalidFileFormatException` | `CsvParserStubTest`                                                 |
| `TtlCache.put()` (cuando falla executor) | `InfraException`              | `TtlCacheFailureTest`                                               |
| `ProcessingRequest` constructor | `NullPointerException` / `IllegalArgumentException` | `ProcessingRequestValidationTest` |

*Cada excepción declarada cuenta con al menos un test; la cobertura supera el 75 %.*  
La CI fallará si se cambia la excepción lanzada o se reduce la cobertura.

---

## E4 – Concurrency Playground

### HU F1-15 – NotificationService (ExecutorService)

```java
NotificationService svc = new NotificationService();          // 4 threads
Notification n = new Notification(user.getEmail(), "Job done");
Future<Boolean> ok = svc.send(n);

if (ok.get()) log.info("Email sent!");
svc.shutdown();
svc.awaitTermination(5, SECONDS);

```

| Criterio de aceptación | Resultado                                           |
| ---------------------- | --------------------------------------------------- |
| 100 envíos en paralelo | ✔️ completan < 5 s sin `RejectedExecutionException` |
| `shutdown()` ordenado  | ✔️ termina < 2 s                                    |
| Cobertura playground   | 80 % en paquete `concurrent.notification`           |

---

### HU F1-16 – WorkQueue (BlockingQueue)

```java
try (WorkQueue workQueue = new WorkQueue()) {
    workQueue.startWorkers(3);
    jobs.forEach(job -> workQueue.submit(() -> process(job)));
} // auto-close ⇒ stop()

```

| Ventaja          | Detalle                                                           |
| ---------------- | ----------------------------------------------------------------- |
| Distribuye carga | Productores delegan a consumidores concurrentes.                  |
| Back-pressure    | Si limitas la capacidad, `submit()` bloquea al llenar la cola.    |
| Shutdown limpio  | `stop()` envía POISON PILL + `join()` sin `InterruptedException`. |

---

### HU F1-17 – Race Condition & Fixes

| Versión | Primitiva | Resultado | Rendimiento |
|---------|-----------|-----------|-------------|
| Buggy   | `int` sin sincronización | Pierde incrementos | Rápido pero incorrecto |
| Fix #1  | `AtomicInteger` | Correcto | Mejor que lock bajo contención alta |
| Fix #2  | `ReentrantLock` | Correcto | Latencia mayor, pero permite operaciones compuestas |

**Modelo de memoria (simplificado)**  
* Escribir en un `int` no es atómico → dos hilos pueden leer-modificar-escribir simultáneamente.  
* `AtomicInteger` ofrece operación **CAS** -> _happens-before_ y visibilidad.  
* `ReentrantLock` establece un **monitor** → exclusión mutua + semántica _happens-before_ en `unlock()` / `lock()`.

---

### HU F1-18 – ReportAggregator (CompletableFuture)

```java
ReportAggregator ra = new ReportAggregator();
ra.generate("id-123")
  .thenAccept(r -> log.info("Ready: {}", r.summary()))
  .join();            // bloquea en demo; en producción, se encadena
```

| Ventaja               | Detalle                                                                     |
| --------------------- | --------------------------------------------------------------------------- |
| **Paralelismo**       | `supplyAsync` lanza tareas A, B, C en el *commonPool*; total ≤ máx(tareas). |
| **Composición**       | `thenCombine` + `thenApply` fusionan resultados sin *callback hell*.        |
| **Manejo de errores** | `exceptionally` registra con `ErrorHandler` y propaga causa unificada.      |

---

### HU F1-19 – KPI Streams Pipeline

```java
total = transactions.stream() // fuente
.filter(t -> t.status() == VALID) // interm. 1
.collect(groupingBy( // interm. 2 + terminal
Transaction::user, summingDouble(Transaction::amount)))
.entrySet().stream() // nuevo stream
.sorted(comparingByValue().reversed())// interm. 3
.collect(toMap(..., LinkedHashMap::new));
```

| Operación Stream | Tipo | Complejidad |
|------------------|------|-------------|
| `filter`         | intermedia | O(n) |
| `groupingBy + sum` | terminal (con cola intermedia) | O(n) |
| `sorted`         | intermedia | O(n log n) |
| `collect(toMap)` | terminal | O(n) |

**Complejidad total**: _O(n log n)_ debido a la fase de ordenación.

---

## Streams paralelos – Benchmark `stream()` vs `parallelStream()` (HU F1-20)

| Operación                               | Dataset                | Secuencial (ms/op) | Paralelo (ms/op) | Speed-up |
|-----------------------------------------|------------------------|--------------------|------------------|----------|
| **Suma de 10 000 000 doubles**          | 10 M elementos         | 30.554 ± 3.189     | **2.764 ± 0.184**| **× 11 ≈** |
| **Map + reduce 100 000 JobExecution**   | 100 k elementos        | **0.076 ± 0.014**  | 0.083 ± 0.008    | × 0.92 (peor) |

> *Tiempos promedio (modo **AverageTime**) tras 2 warm-ups + 3 mediciones; unidad = ms/op.*

### Conclusiones rápidas

* **Cálculo numérico masivo**  
  *La suma de 10 M doubles se acelera ≈ 11 ×* gracias al fork-join: cada hilo procesa unos 2,5 M elementos y la sobrecarga de división/combina se amortiza.

* **Datasets medianos o pipelines ligeros**  
  Map-reduce sobre 100 k objetos **empeora** en paralelo (× 0.92).  
  Cuando la operación por elemento es muy barata, la sobrecarga de *fork-join* y la fusión de resultados supera al trabajo útil.

* **Regla práctica**  
  - Usa `parallelStream()` para colecciones **muy grandes** (≈ > 1 M) o tareas CPU-bound costosas.  
  - Evítalo en datasets pequeños, operaciones I/O-bound o servidores donde el *commonPool* ya está saturado.

* **Fork-join pool**  
  `parallelStream()` utiliza el **ForkJoinPool.commonPool** (≈ nº de núcleos).  
  Puedes ajustar su tamaño con  
  ```java
  System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "8");
  ```
  o emplear tu propio ForkJoinPool si necesitas aislar cargas.

  ---

  ### HU F1-21 – Normalización de fechas (java.time)

```java
Instant start = job.getStartTime();           // almacenado en UTC
Duration d = TimeUtil.between(start, TimeUtil.nowUtc());
log.info("Duración: {} s", d.getSeconds());
```

Almacena en UTC (Instant) y convierte a zona de usuario en la vista.

No se usan Date / Calendar; la API moderna es inmutable y thread-safe.

Complejidad O(1) en todas las utilidades; solo cálculos aritméticos o acceso a campos.

---

## Optional Best Practices

| ✅ DO | ❌ DON’T |
|-------|---------|
| Retornar `Optional<T>` en vez de `null`. | Encadenar `Optional.get()` sin `isPresent()`. |
| Usar `map`, `flatMap`, `orElse`, `orElseThrow`. | Aceptar `Optional` como argumento público (mejor `@Nullable` parámetro o sobrecarga). |
| Emplear utilidades como `OptionalUtil.firstPresent(...)` para evitar cascadas de `ifPresent`. | Usar `Optional` en campos de entidad (incrementa coste de serialización). |

> **Nota:** Guarda valores ausentes como `Optional.empty()`, no como `null` dentro del `Optional`.

Ejemplo práctico:

```java
Optional<Report> maybe = reportRepo.findByRequestId(id);
String path = maybe.map(Report::getFilePath)
                   .orElse("/placeholder.txt");
```

---

## Serialización JSON ligera (HU F1-23)

```java
User u = …;
String json = JsonSerializer.toJson(u);          // pretty-printed, null-safe
User copy  = JsonSerializer.fromJson(json, User.class);
assert u.equals(copy);
```

| Ventaja Gson core                                 | Limitación vs Jackson                          |
| ------------------------------------------------- | ---------------------------------------------- |
| ≈ 240 kB JAR, sin reflection module opener        | No soporta filtros, *mix-ins* o `@JsonView`    |
| Tolerancia a campos desconocidos (forward-compat) | Sin autodetección de records en versiones < 17 |
| Rendimiento suficiente (≈ 50 MB/s)                | Sin streaming “pull” de bajo nivel             |

Regla adoptada: guardar JSON siempre en UTF-8, sin dependencias de Spring; los modelos evolucionan manteniendo compatibilidad porque los campos extra se ignoran.

---

### Serialización nativa (HU F1-24) — datos medidos

| Métrica sobre 10 000 objetos        | JSON (Gson) | Externalizable | Ventaja |
|-------------------------------------|-------------|----------------|---------|
| **Tiempo serializar** (promedio)    | 4,01 ms/op  | **2,63 ms/op** | **≈ 1,5 ×** más rápido |
| **Tiempo calcular tamaño**<br>(`size_*` benchmark) | **0,20 ms/op** | 2,69 ms/op | JSON obtiene longitud de `String`; llamada binaria necesita copiar buffer |
| **Tamaño total en disco** (previo)  | 250 KB      | **120 KB**     | 2,1 × más compacto |

> _Resultados obtenidos en la misma JVM y máquina (cuatro núcleos),  
> 3 warm-ups + 3 mediciones (`jmh` modo **AverageTime**, unidad = ms/op)._  

#### Observaciones

* **Externalizable** continúa duplicando la compresión (120 KB vs 250 KB) y ahora es ~50 % más veloz al serializar.  
* El benchmark `size_*` sólo mide el coste de calcular el tamaño, no el tamaño en sí; por eso JSON es más rápido ahí (simple `String.length`).  
* En deserialización (no mostrado) la tendencia es similar: binario evita parseo de texto.

#### Pros / Contras rápidos

| Externalizable (binario)                                | JSON (texto legible)                     |
|---------------------------------------------------------|------------------------------------------|
| ✔  Tamaño más pequeño y latencia menor                  | ✔  Humano-legible, diff-friendly         |
| ✔  Controlas qué campos escribes (versión 100 % manual) | ✔  Portabilidad entre lenguajes          |
| ❌  No legible / requiere versión explícita             | ❌  Mayor tamaño y parseo más costoso     |

> **Regla práctica** Utiliza serialización binaria sólo en caminos «hot-path» controlados enteramente por Java; mantén JSON para integración, logs o configuración donde la legibilidad y portabilidad pesan más.

---

### CSV utilitario (HU F1-25)

```java
Path csv = Path.of("requests.csv");
CsvUtil.writeRequests(csv, list, UTF_8, ';');   // guardar
List<ProcessingRequest> back =
        CsvUtil.readRequests(csv, UTF_8, ';');  // leer
```

java.nio (Files.newBufferedReader/Writer) evita librerías pesadas.

Manejo correcto de salto de línea (Windows/Linux) gracias a BufferedWriter.newLine().

Charset configurable; por defecto usamos UTF-8 para compatibilidad.

Lógica O(n) simple: dividir cadena + join. Para CSV complejo (citas, escapes) considerar OpenCSV / Univocity.

---

# Guía de Calidad

Herramienta | Propósito | Severidad que rompe build
------------|-----------|---------------------------
**SpotBugs** | Detecta bugs potenciales (NPE, concurrencia) | `High` o superior
**Checkstyle** | Consistencia de estilo (sangría, nombres) | `error`
**PMD** | Code smells, complejidad, duplicados | `error`

## Cómo suprimir un falso positivo

1. SpotBugs: añade un bloque `<Match>` en `config/quality/spotbugs-exclude.xml`.
2. PMD / Checkstyle: usa la anotación `@SuppressWarnings("PMD.RuleName")` o comentario `// CHECKSTYLE:OFF ... ON`.

> **Regla**: justificar la supresión en el PR; no silenciar globalmente.

Los reportes HTML se generan en `target/quality-reports/index.html` para cada módulo.

---

### 📚 API Javadoc  
La documentación de la API pública (≥ 80 % cubierta) está disponible en  
➡️ [docs/javadoc/index.html](docs/javadoc/index.html)

---

# Fase 1 – Resumen General

## Objetivos cumplidos
- ☑️ **Setup** multi-módulo (`core`, `lib`) con CI y cobertura ≥ 75 %.
- ☑️ **Dominio** modelado (User, DataFile, ProcessingRequest, …) aplicando OOP sólido.
- ☑️ **Colecciones & Genéricos**: `PagedResult<T>`, `InMemoryCache`, micro-benchmark ArrayList vs LinkedList.
- ☑️ **Concurrencia básica**: `NotificationService`, `WorkQueue`, demo de *race-condition* y `CompletableFuture`.
- ☑️ **Streams & Lambdas**: `KpiCalculator`, benchmark `parallelStream()`.
- ☑️ **I/O & Serialización**: JSON “ligero”, `Externalizable`, `CsvUtil` con `java.nio`.
- ☑️ **Calidad transversal**: análisis estático (SpotBugs, Checkstyle, PMD), JMH, Javadoc 80 %.

---

## Conceptos practicados
- **OOP** (encapsulación, inmutabilidad, patrón Builder/Factory).  
- **Streams & Collectors** (`groupingBy`, `mapping`, paralelos).  
- **Generics avanzados** (wildcards `? extends / super`).  
- **Concurrencia** (`ExecutorService`, `BlockingQueue`, `AtomicInteger`, `ReentrantLock`).  
- **java.time** (`Instant`, `LocalDate`, zonas).  
- **Optional** (best practices, utilidades).  
- **I/O / NIO.2** (Paths, Files, charsets).  
- **Benchmarking** (JMH).  
- **Calidad** (JaCoCo, SpotBugs, Checkstyle, PMD).  
- **Javadoc** generada y publicada.  

---

## Principales aprendizajes
1. **Trade-offs en estructuras de datos** – benchmark mostró cuándo `LinkedList` vence a `ArrayList` solo en inserciones al inicio con datasets grandes.  
2. **Overhead de `parallelStream()`** – speed-up real sólo con > 1 M elementos CPU-bound.  
3. **Externalizable vs JSON** – binario 2× más pequeño y rápido, pero pierde portabilidad/legibilidad.  
4. **Evitar *data races*** – `AtomicInteger` ofrece CAS barato; locks útiles para operaciones compuestas.  
5. **Calidad automatizada** – build falla temprano; menor tiempo de revisión manual.

---

## Gaps / dificultades detectadas
- Necesidad de profundizar en **JMH avanzado** (profiler, warm-up adecuado).  
- PMD marcó complejidad > 15 en algunos métodos; pendiente refactorizar.  
- Cobertura JaCoCo de *benchmarks* excluida; explorar integración con `jacoco:agent` para medición real.  
- Falta prueba de integración end-to-end (JSON ↔ CSV ↔ Report).

---

## Próximos pasos (Fase 2)
| Área | Acción | Enlace |
|------|--------|--------|
| **Refactor** | Reducir complejidad de `ProcessingRequest.Builder`. | #REF-TICKET-123 |
| **Performance** | Añadir benchmarks para *I/O* (Buffered vs NIO channels). | perf-board |
| **Observabilidad** | Integrar logs estructurados + metrics Micrometer. | board-fase-2 |
| **Persistencia** | Prototipo con Spring Batch + H2. | board-fase-2 |
| **Automatización** | Publicar *GitHub Pages* con Javadoc y JaCoCo badge. | PR #XYZ |

> **Cobertura actual:** 78 % (core) • 85 % (lib)  
> **Benchmarks clave:** *externalizable vs JSON* (×1.5 speed-up) / *parallelStream suma* (×11 speed-up).

---

_Fase 1 establecida como base sólida; lista para escalar a integración, observabilidad y optimización avanzada en la siguiente etapa._

---

# DataFlowHubLibraryJava – Fase 2 (Épica B0)

> **Boot Setup & Tooling**
> **Estado**: ✅ F2-01 • ✅ F2-02 · ✅ F2-03

Este documento resume los cambios y lineamientos aplicados en la **Fase 2** para preparar el andamiaje de Spring Boot y conectar la librería de la Fase 1 con un servicio API.

---

## 1) Estructura del repositorio (multi-módulo)

```
DataFlowHubLibraryJava/
├─ pom.xml                    # POM padre (packaging = pom)
├─ lib/                       # utilidades/demos (Fase 1)
│  └─ pom.xml
├─ core/                      # lógica reusable de dominio (Fase 1)
│  └─ pom.xml
└─ api-service/               # NUEVO: servicio REST Spring Boot 3
   └─ pom.xml
```

**Claves del POM padre**

* Propiedad centralizada `spring.boot.version`.
* Import del **BOM** de Spring Boot en `<dependencyManagement>`.
* Versión del **spring-boot-maven-plugin** fijada en `<pluginManagement>` para herencia en todos los módulos.

---

## 2) HU **F2-01 – Setup de Spring Boot**

**Objetivo:** Inicializar un servicio web mínimo con Spring Boot 3 (Web + Actuator) que compile y arranque.

**Entregables**

* Módulo **`api-service`** creado.
* Dependencias: `spring-boot-starter-web`, `spring-boot-starter-actuator`.
* Configuración de versión de Boot mediante BOM en el POM padre.

**Criterios de aceptación**

* `mvn -pl api-service test` compila sin errores.
* La aplicación arranca localmente.

**Comandos**

```bash
mvn -pl api-service test
mvn -pl api-service spring-boot:run
```

---

## 3) HU **F2-02 – Importar `core-lib` como dependencia**

**¿Qué se hizo?**

1. Se importó el módulo **`core`** (librería de Fase 1) dentro de **`api-service`**:

   ```xml
   <dependency>
     <groupId>com.practice</groupId>
     <artifactId>core</artifactId>
     <version>${project.version}</version>
   </dependency>
   ```
2. Se habilitó *component scan* cruzado desde la app web para detectar beans del `core`:

   ```java
   @SpringBootApplication(scanBasePackages = "com.practice")
   public class ApiServiceApplication { }
   ```
3. Se validó la inyección de un bean de la librería (ej. `ErrorHandler`) exponiendo un endpoint de prueba:

   ```java
   @RestController
   @RequiredArgsConstructor
   class HealthExtraController {
     private final ErrorHandler errorHandler; // viene de core
     @GetMapping("/ping") public String ping() { return "pong"; }
   }
   ```

**Criterios de aceptación**

* `mvn -pl api-service test` pasa sin errores.
* `ErrorHandler` (u otro bean de `core`) se inyecta y funciona en un endpoint expuesto.

**Probar rápidamente**

```bash
mvn clean install
java -jar api-service/target/api-service-*.jar
# En otra terminal
curl http://localhost:8080/actuator/health
curl http://localhost:8080/ping     # → "pong"
```

---

## 4) Configuración Maven aplicada (resumen)

**POM padre**

* Define:

    * `java.version`
    * `spring.boot.version`
* Importa el BOM de Spring Boot en `dependencyManagement`.
* Fija `spring-boot-maven-plugin` en `pluginManagement` (heredado por los módulos).
* Orden de módulos para un build determinista:

  ```xml
  <modules>
    <module>lib</module>
    <module>core</module>
    <module>api-service</module>
  </modules>
  ```

---

## 5) Cómo compilar y ejecutar

**Compilación completa**

```bash
mvn clean install
```

**Compilar solo `api-service` (con sus dependencias aguas arriba)**

```bash
mvn -pl api-service test -am
```

**Ejecutar**

```bash
java -jar api-service/target/api-service-*.jar
```

**Endpoints**

* `GET /actuator/health`
* `GET /ping`

---

# HU F2-03 – Perfiles `dev`, `test`, `prod`

> **Objetivo**: definir configuración por ambiente en `api-service`, aislando datasources y niveles de log, y documentar cómo activar perfiles.

---

## Archivos creados/actualizados

* `api-service/src/main/resources/application.yml` *(base)*
* `api-service/src/main/resources/application-dev.yml`
* `api-service/src/main/resources/application-test.yml`
* `api-service/src/main/resources/application-prod.yml`

**Dependencias relevantes (en ********************`api-service/pom.xml`********************):** `spring-boot-starter-jdbc`, `com.h2database:h2` *(runtime)*, `org.postgresql:postgresql` *(runtime)*.
**Tests** ejecutan con perfil `test` de forma automática (Surefire: `spring.profiles.active=test`).

---

## Configuración base (`application.yml`)

```yaml
spring:
  application:
    name: api-service
  profiles:
    default: dev    # si no se especifica, arranca en dev
server:
  port: 8080
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

---

## Perfil `dev` (`application-dev.yml`)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:dataflow;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true

logging:
  level:
    root: INFO
    com.practice: DEBUG
```

## Perfil `test` (`application-test.yml`)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop

logging:
  level:
    root: WARN
    org.springframework: WARN
```

## Perfil `prod` (`application-prod.yml`)

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://db:5432/dataflow}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate

server:
  port: ${PORT:8080}

logging:
  level:
    root: INFO
    org.springframework: INFO
  file:
    name: ${LOG_FILE:logs/api-service.log}
```

---

## Cómo activar perfiles

* **Por variable de entorno**

  ```bash
  # Linux/Mac
  SPRING_PROFILES_ACTIVE=prod mvn -pl api-service spring-boot:run

  # Windows PowerShell
  $Env:SPRING_PROFILES_ACTIVE="prod"; mvn -pl api-service spring-boot:run
  ```
* **Por línea de comandos al ejecutar el JAR**

  ```bash
  java -Dspring.profiles.active=dev -jar api-service/target/api-service-*.jar
  ```
* **Tests**: el plugin Surefire fija `spring.profiles.active=test` (no requiere anotaciones en los tests).

---

## Verificación rápida

```bash
mvn clean install
java -jar api-service/target/api-service-*.jar
# En otra terminal
curl http://localhost:8080/actuator/health
curl http://localhost:8080/ping   # si está habilitado el endpoint de ejemplo
```

**Smoke test (resumen):** se valida que con `test` el `DataSource` sea H2 en memoria y que el nivel de log sea `WARN`.

---

## Criterios de aceptación

* Cambiar perfil modifica datasource y nivel de log.
* Tests se ejecutan con perfil `test` automáticamente (`@ActiveProfiles("test")`).
* README sección “Perfiles” explica variables y ejemplos.

---

# B1 · HU F2-04 — Endpoint **POST /files** (simular upload de `DataFile`)

> **Objetivo**: exponer un endpoint que **registre metadatos** de un archivo (NO guarda binarios), valide con **Bean Validation** y responda **201 Created** con `Location` y un DTO de salida.

---

## Archivos creados/actualizados

* `api-service/src/main/java/.../files/FileController.java`
* `api-service/src/main/java/.../files/FileUploadRequest.java`
* `api-service/src/main/java/.../files/FileUploadResponse.java`
* `api-service/src/main/java/.../common/ApiErrorHandler.java`
* `api-service/src/main/java/.../files/FileTooLargeException.java`
* `api-service/src/test/java/.../files/FileControllerTest.java`

**Dependencias** (en `api-service/pom.xml`)

* `spring-boot-starter-validation` (activación de Bean Validation)

---

## Contrato del endpoint

**Ruta:** `POST /files`

**Request Body (JSON):**

```json
{
  "originalFilename": "ventas_julio.csv",
  "sizeBytes": 1048576,
  "storagePath": "/data/in/ventas_julio.csv",
  "checksumSha256": "9f2c...abcd", // OPCIONAL
  "uploadedByUserId": "8d3b6c3f-64db-4c1e-9d51-3b6f3d8e2a11"
}
```

**Validaciones (Bean Validation):**

* `originalFilename` → `@NotBlank` y `@Size(max=255)`
* `sizeBytes` → `@Positive` y **máx 50 MB** (`50 * 1024 * 1024`)
* `checksumSha256` → **opcional**, regex `^[a-fA-F0-9]{64}$`
* `storagePath` → `@NotBlank`
* `uploadedByUserId` → `@NotNull` (UUID)

**Response (201 Created):**

* **Headers:** `Location: /files/{id}`
* **Body:**

```json
{
  "id": "4c16cf2a-9a11-4a9b-a1e5-0c7b2a7d1234",
  "originalFilename": "ventas_julio.csv"
}
```

**Errores (400 Bad Request):**

* Cuerpo estándar: `{ "code": string, "message": string, "fields": [ {"field": string, "message": string} ] }`
* Tamaño excedido ⇒ `code = "FILE_TOO_LARGE"`
* Violaciones de Bean Validation ⇒ `code = "VALIDATION_ERROR"` con `fields[]`

---

## Mapeo al dominio

* Se reutiliza el `DataFile` **existente** en `core/domain`.
* Creación recomendada: **`DataFile.createForUpload(...)`** (fábrica en el dominio que genera `id`/`uploadedAt` y resuelve `uploadedBy` a partir de `uploadedByUserId`).
* Si no existe la fábrica, puede instanciarse el `DataFile` directamente respetando las invariantes actuales del dominio.

---

## Ejemplo con `curl`

```bash
# Éxito (201)
curl -i -X POST http://localhost:8080/files \
  -H 'Content-Type: application/json' \
  -d '{
        "originalFilename":"ventas_julio.csv",
        "sizeBytes":1048576,
        "storagePath":"/data/in/ventas_julio.csv",
        "uploadedByUserId":"8d3b6c3f-64db-4c1e-9d51-3b6f3d8e2a11"
      }'

# Error (400 FILE_TOO_LARGE)
curl -i -X POST http://localhost:8080/files \
  -H 'Content-Type: application/json' \
  -d '{
        "originalFilename":"big.bin",
        "sizeBytes":52428801,
        "storagePath":"/tmp/big.bin",
        "uploadedByUserId":"8d3b6c3f-64db-4c1e-9d51-3b6f3d8e2a11"
      }'

# Error (400 VALIDATION_ERROR) por filename vacío + checksum inválido
curl -i -X POST http://localhost:8080/files \
  -H 'Content-Type: application/json' \
  -d '{
        "originalFilename":" ",
        "sizeBytes":100,
        "checksumSha256":"XYZ",
        "storagePath":"/data/in/a.csv",
        "uploadedByUserId":"8d3b6c3f-64db-4c1e-9d51-3b6f3d8e2a11"
      }'
```

---

## Tests incluidos (`@WebMvcTest`)

* **Happy path:** 201 + `Location` + body `{id, originalFilename}`.
* **Tamaño > 50MB:** 400 con `code=FILE_TOO_LARGE`.
* **Payload inválido:** 400 `VALIDATION_ERROR`, lista de `fields` con `originalFilename` y `checksumSha256`.

> Los tests fuerzan la validación antes de llegar al dominio usando `@Valid` + `BindingResult`.

---

## Criterios de aceptación

* 202 con Location válido al crear correctamente.
* 400 con lista de errores de validación cuando falten campos.
* `title` recorta espacios; longitud > 140 → 400.
* Tests de mapeo DTO→dominio verifican campos obligatorios y opcionales.

---

## Notas

* El almacenamiento real del binario **no se implementa** en esta HU; solo se registran metadatos.
* En `prod` el comportamiento de logging y perfiles se hereda de la HU **F2-03**.

---

# HU F2-05 — Endpoint **POST /processings** (ACK 202)

> **Objetivo:** aceptar la solicitud de procesamiento y devolver un **acuse (ACK)** con `status=PENDING`, sin instanciar aún el agregado de dominio ni persistir. La validación de existencia de `DataFile`, `User` y `BatchJobConfig` se realizará en la siguiente HU/épica.

---

## Archivos creados/actualizados

* `api-service/src/main/java/.../processings/ProcessingController.java`
* `api-service/src/main/java/.../processings/dto/CreateProcessingRequest.java`
* `api-service/src/main/java/.../processings/dto/ProcessingCreatedResponse.java`
* `api-service/src/main/java/.../config/AppBatchProps.java`
* `api-service/src/test/java/.../processings/ProcessingControllerTest.java`
* `api-service/src/main/resources/application.yml` → propiedad `app.batch.default-config-id`

**Notas**

* Se reutiliza el `ApiErrorHandler` existente (`badRequestFrom(BindingResult)`) para responder **400 VALIDATION\_ERROR**.
* No se construyen objetos de dominio (`ProcessingRequest`, `DataFile`, `User`, `BatchJobConfig`) en esta HU.

---

## Contrato del endpoint

**Ruta:** `POST /processings`

**Request Body (JSON):**

```json
{
  "title": "ETL Ventas Julio",
  "dataFileId": "e4b7b32e-f93b-47b1-8a5d-6a0c3c8f1b0b",
  "requestedByUserId": "1b2b4d6e-9fa1-4f0f-8b12-33d4c9a0e111",
  "batchJobConfigId": "00000000-0000-0000-0000-000000000001", // OPCIONAL
  "parameters": { "delimiter": ";" }                          // OPCIONAL
}
```

**Validaciones:**

* `title` → se aplica `trim()` y luego se exige longitud **1..140**. (El DTO puede permitir hasta 400 para entrada, pero el controller recorta y valida el límite efectivo.)
* `dataFileId` → requerido (UUID)
* `requestedByUserId` → requerido (UUID)
* `batchJobConfigId` → **opcional**; si no llega se usa `app.batch.default-config-id`
* `parameters` → opcional; si viene, `Map<@NotBlank String, @NotBlank String>`

**Response (202 Accepted):**

* **Headers:** `Location: /processings/{id}`
* **Body:**

```json
{ "id": "4c16cf2a-9a11-4a9b-a1e5-0c7b2a7d1234", "status": "PENDING" }
```

**Errores (400 Bad Request):**

* Cuerpo estándar: `{ "code": "VALIDATION_ERROR", "message": "Invalid payload", "fields": [ {"field": "...", "message": "..."} ] }`
* Casos cubiertos: `title` vacío tras `trim`, `title` > 140, `dataFileId`/`requestedByUserId` nulos, claves/valores inválidos en `parameters`.

---

## Configuración

`application.yml` (o `application-dev.yml`):

```yaml
app:
  batch:
    default-config-id: "00000000-0000-0000-0000-000000000001"
```

Habilitar properties:

```java
@EnableConfigurationProperties(AppBatchProps.class)
```

---

## Ejemplos con `curl`

```bash
# Éxito (202)
curl -i -X POST http://localhost:8080/processings \
  -H 'Content-Type: application/json' \
  -d '{
        "title":"  ETL Ventas Julio  ",
        "dataFileId":"e4b7b32e-f93b-47b1-8a5d-6a0c3c8f1b0b",
        "requestedByUserId":"1b2b4d6e-9fa1-4f0f-8b12-33d4c9a0e111",
        "parameters": {"delimiter":";"}
      }'

# Error (400 VALIDATION_ERROR) por título inválido
d='{"title":"   ","dataFileId":"'$(uuidgen)'","requestedByUserId":"'$(uuidgen)'"}'
curl -i -X POST http://localhost:8080/processings -H 'Content-Type: application/json' -d "$d"
```

---

## Tests (`@WebMvcTest`)

* **Happy path:** 202 + `Location` + body `{id, "PENDING"}`.
* **Uso de default:** cuando `batchJobConfigId` no viene, se lee `app.batch.default-config-id`.
* **Validación:** `title` (trim y longitud) y requeridos → 400 `VALIDATION_ERROR` con `fields`.

---

## Criterios de aceptación

* 202 con Location válido al crear correctamente.
* 400 con lista de errores de validación cuando falten campos.
* `title` recorta espacios; longitud > 140 → 400.
* Tests de mapeo DTO→dominio verifican campos obligatorios y opcionales.

---

## Notas

* Esta HU no verifica la existencia de DataFile, User ni BatchJobConfig; tampoco instancia ProcessingRequest. Ese wiring (lookups/repos) se aborda en la siguiente HU/épica.

---

# HU F2-06 — Endpoint **GET /processings/{id}** (estado + métricas)

> **Objetivo:** exponer el estado de un *processing* combinando el **`ProcessingRequest`** y su \*\*última \*\***`JobExecution`** (si existe). Devuelve un DTO estable para el front y 404 si el id no existe.

---

## Archivos creados/actualizados

* `api-service/src/main/java/.../processings/ProcessingQueryController.java`
* `api-service/src/main/java/.../processings/dto/ProcessingStatusResponse.java`
* `api-service/src/main/java/.../processings/query/ProcessingStatusFinder.java` *(puerto de lectura)*
* `api-service/src/main/java/.../processings/query/InMemoryProcessingStatusFinder.java` *(adaptador in-memory)*
* `api-service/src/main/java/.../utils/error/ResourceNotFoundException.java` *(404)*
* `api-service/src/test/java/.../processings/ProcessingQueryControllerTest.java`
* *(opcional demo)* `api-service/src/main/java/.../config/DemoData.java` para “sembrar” un registro al arrancar.

**Dependencias:** se reutilizan las de `api-service`. No se modifican `core` ni `lib`.

---

## Contrato del endpoint

**Ruta:** `GET /processings/{id}`

**Response 200 (JSON):**

```json
{
  "id": "7e2a1d7c-39bb-4f1a-8a55-9a2f14c47788",
  "title": "ETL Ventas Julio",
  "status": "RUNNING",
  "createdAt": "2025-08-07T03:10:21Z",
  "dataFileId": "4c16cf2a-9a11-4a9b-a1e5-0c7b2a7d1234",
  "metrics": { "readCount": 12345, "writeCount": 12280, "skipCount": 145 },
  "lastExecution": {
    "startTime": "2025-08-07T03:10:22Z",
    "endTime": null,
    "exitStatus": null,
    "errorMessage": null
  }
}
```

**Response 404:**

```json
{ "code": "NOT_FOUND", "message": "processing id not found", "fields": [] }
```

**Notas de serialización:**

* Tiempos (`Instant`) en formato **ISO‑8601 UTC** (`Z`).
* `lastExecution` es **`null`** si nunca se ha ejecutado.
* Si existe al menos una ejecución, `metrics` refleja la última (`read/write/skip`).

---

## DTO de salida

```java
public record ProcessingStatusResponse(
    UUID id,
    String title,
    String status,          // RequestStatus del dominio → String
    Instant createdAt,
    UUID dataFileId,
    Metrics metrics,
    LastExecution lastExecution
) {
  public record Metrics(long readCount, long writeCount, long skipCount) {}
  public record LastExecution(Instant startTime, Instant endTime, String exitStatus, String errorMessage) {}
}
```

---

## Fuente de datos (read model)

* Se usa el puerto **`ProcessingStatusFinder`** que expone:

    * `findRequest(UUID id)` → `Optional<ProcessingRequest>`
    * `findLastExecution(UUID processingId)` → `Optional<JobExecution>`
* Implementación por defecto: `InMemoryProcessingStatusFinder` (mapas concurrentes) para permitir tests y demos sin persistencia.
* **Mapeo a DTO:**

    * `id`, `title`, `status`, `createdAt` y `dataFileId` vienen de `ProcessingRequest`.
    * Si hay última `JobExecution`: rellena `metrics` y `lastExecution` (con `exitStatus.name()`), si no, `metrics` = `0/0/0` y `lastExecution=null`.

---

## Ejemplos con `curl`

```bash
# Existe y está corriendo (200)
curl -s http://localhost:8080/processings/7e2a1d7c-39bb-4f1a-8a55-9a2f14c47788 | jq

# No existe (404)
curl -i http://localhost:8080/processings/00000000-0000-0000-0000-000000000000
```

---

## Tests (`@WebMvcTest`)

* **200 sin ejecución:** `lastExecution` no presente y `metrics` en `0`.
* **200 con ejecución:** `status` y métricas correctas; `lastExecution.startTime` en ISO‑8601.
* **404 inexistente:** error con `code=NOT_FOUND`.

> En los tests se **mockea** `ProcessingStatusFinder` y se construyen objetos de dominio reales (`ProcessingRequest`, `JobExecution`) para validar el mapeo.

---

## 4. Criterios de aceptación

* 200 con DTO completo; 404 cuando `id` no existe.
* No exponer stacktraces; errores van por `RestExceptionHandler` (B3).
* Campos de fecha en **ISO‑8601 UTC**.
* Tests cubren los tres escenarios.

---

## Notas

* Este read-model es **in-memory** y sirve como contrato estable. En épicas B5/B6 se conectará con **Spring Batch/Actuator** para poblar las ejecuciones reales y métricas.

---

# HU F2-07 — PostgreSQL + Flyway + Testcontainers

> **Objetivo:** configurar PostgreSQL en **dev** y **tests** con migraciones **Flyway**. En tests se usa **Testcontainers** (PG real en Docker). No se modifica `core` ni `lib`.

---

## Archivos creados/actualizados

* `api-service/pom.xml` → dependencias JPA, PostgreSQL, Flyway y Testcontainers
* `api-service/src/main/resources/application-dev.yml` → datasource PostgreSQL (por variables de entorno)
* `api-service/src/main/resources/application-test.yml` → sin datasource; JPA/Flyway mínimos para Testcontainers
* `api-service/src/test/java/.../db/DbSmokeTest.java` → prueba de arranque y conexión a BD
* *(opcional)* `api-service/docker-compose.yml` → servicio PostgreSQL local para desarrollo
* *(opcional)* `src/main/resources/db/migration/*.sql` → scripts de Flyway

**Dependencias añadidas (POM ********`api-service`********):**

* `spring-boot-starter-data-jpa`
* `org.postgresql:postgresql` *(runtime)*
* `org.flywaydb:flyway-core`
* `org.flywaydb:flyway-database-postgresql` *(requerido para PG 16+)*
* `org.testcontainers:junit-jupiter` *(test)*
* `org.testcontainers:postgresql` *(test)*

> Si tu BOM de Spring Boot no trae Flyway 10, fija `flyway.version` a **10.x** o usa Testcontainers con `postgres:15-alpine` (ver más abajo).

---

## Configuración por perfil

### `application-dev.yml` (PostgreSQL por variables de entorno)

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/dataflow}
    username: ${DB_USER:app}
    password: ${DB_PASSWORD:secret}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### `application-test.yml` (para Testcontainers)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none
  flyway:
    enabled: true
```

> **No** declares `spring.datasource.*` aquí: Testcontainers inyecta URL/usuario/clave automáticamente.

---

## Test de humo con Testcontainers

```java
@Testcontainers
@SpringBootTest
class DbSmokeTest {
  @Container
  @ServiceConnection // Spring Boot 3.1+: autoconfigura el DataSource desde el contenedor
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  // Fallback si tu Boot < 3.1
  @DynamicPropertySource
  static void pgProps(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired javax.sql.DataSource ds;

  @Test void contextLoads_andDatabaseIsReachable() throws Exception {
    try (var c = ds.getConnection()) { assert c.isValid(2); }
  }
}
```

**Nota Flyway:** Para PostgreSQL 16, agrega `flyway-database-postgresql`. Si usas Flyway < 10, cambia el contenedor a `postgres:15-alpine`.

---

## Ejecución en desarrollo (dev)

### Linux/Mac

```bash
export DB_URL=jdbc:postgresql://localhost:5432/dataflow
export DB_USER=app
export DB_PASSWORD=secret
export SPRING_PROFILES_ACTIVE=dev
mvn -pl api-service spring-boot:run
```

### Windows PowerShell

```powershell
$Env:DB_URL = "jdbc:postgresql://localhost:5432/dataflow"
$Env:DB_USER = "app"
$Env:DB_PASSWORD = "secret"
$Env:SPRING_PROFILES_ACTIVE = "dev"
mvn -pl api-service spring-boot:run
```

*(Opcional)* levanta PostgreSQL con Docker:

```yaml
# api-service/docker-compose.yml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: dataflow
      POSTGRES_USER: app
      POSTGRES_PASSWORD: secret
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]
volumes:
  pgdata:
```

---

## Migraciones Flyway

* Coloca scripts en `src/main/resources/db/migration` con prefijos `V1__*.sql`, `V2__*.sql`, etc.
* `baseline-on-migrate: true` permite inicializar una BD vacía sin errores.

---

## CI (GitHub Actions)

```yaml
- uses: actions/checkout@v4
- uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: '17'   # o 21 si tu POM lo requiere
    cache: maven
- run: mvn -B -DskipTests=false clean verify
```

> Testcontainers usa Docker del runner automáticamente; no requieres servicios extra.

---

## Criterios de aceptación

* La app arranca en `dev` y ejecuta migraciones Flyway sin errores.
* En `test`, el contexto usa Postgres de Testcontainers automáticamente.
* Logs muestran zona horaria **UTC** y `ddl-auto=validate`.


---

# HU F2-08 — Repositorios JPA e infraestructura de persistencia

> **Objetivo:** persistir los agregados del dominio **sin anotar \*\*\*\*\*\*\*\*`core`**, creando **entidades JPA** y **repos Spring Data** en `api-service` (capa infra). Guardar `parameters` como **JSONB** y exponer consultas básicas necesarias para B2.

---

## Archivos creados/actualizados

* `api-service/src/main/java/.../infra/db/converter/MapToJsonConverter.java`
* `api-service/src/main/java/.../infra/db/entity/`
  `UserEntity.java`, `DataFileEntity.java`, `BatchJobConfigEntity.java`, `ProcessingRequestEntity.java`, `JobExecutionEntity.java`, `ReportEntity.java`
* `api-service/src/main/java/.../infra/db/repo/`
  `UserRepository.java`, `DataFileRepository.java`, `BatchJobConfigRepository.java`, `ProcessingRequestRepository.java`, `JobExecutionRepository.java`, `ReportRepository.java`
* *(opcional)* `api-service/src/main/java/.../infra/db/mapper/` (MapStruct)
  `UserMapper.java`, `DataFileMapper.java`, `ProcessingRequestMapper.java`
* `api-service/src/test/java/.../infra/db/RepositoryIT.java` (tests de integración con Testcontainers)

**Dependencias**: se reutilizan las de HU F2-07 (JPA, PostgreSQL, Testcontainers). Si usas MapStruct, añade `mapstruct` + `mapstruct-processor` (annotation processor) en `api-service/pom.xml`.

---

## Estructura de paquetes

```
api-service/
 └─ src/main/java/com/practice/apiservice/infra/db/
    ├─ converter/MapToJsonConverter.java
    ├─ entity/
    │   ├─ UserEntity.java
    │   ├─ DataFileEntity.java
    │   ├─ BatchJobConfigEntity.java
    │   ├─ ProcessingRequestEntity.java
    │   ├─ JobExecutionEntity.java
    │   └─ ReportEntity.java
    ├─ repo/
    │   ├─ UserRepository.java
    │   ├─ DataFileRepository.java
    │   ├─ BatchJobConfigRepository.java
    │   ├─ ProcessingRequestRepository.java
    │   ├─ JobExecutionRepository.java
    │   └─ ReportRepository.java
    └─ mapper/ (opcional)
```

> Todo cuelga de `com.practice.apiservice` para que **component-scan** los detecte sin `@EnableJpaRepositories/@EntityScan`.

---

## Entidades JPA (resumen)

* **UserEntity** → tabla `users` (`id: uuid`, `name` opcional).
* **DataFileEntity** → `data_files` con: `originalFilename`, `storagePath`, `sizeBytes`, `checksumSha256`, `uploadedAt`, `uploadedBy (FK → users)`.
* **BatchJobConfigEntity** → `batch_job_configs` con: `name`, `description`, `chunkSize`, `readerType`, `writerType`, `allowRestart`, `createdAt`, `active`.
* **ProcessingRequestEntity** → `processing_requests` con: `title`, `dataFile (FK)`, `parameters: jsonb`, `status (enum)`, `createdAt`, `requestedBy (FK)`, `batchJobConfig (FK)`.
* **JobExecutionEntity** → `job_executions` con: `processingRequest (FK)`, `startTime`, `endTime`, `exitStatus (enum)`, `readCount`, `writeCount`, `skipCount`, `errorMessage`.
* **ReportEntity** → `reports` con: `processingRequest (FK)`, `storagePath`, `summaryJson (text)`, `generatedAt`, `generatedBy (FK)`.

> Todas las `@Id` son `UUID` (`columnDefinition = "uuid"`). `parameters` usa **JSONB** con el converter de abajo.

---

## Converter JSONB

`MapToJsonConverter` serializa `Map<String,String>` ⇄ `jsonb` usando Jackson. Se aplica en `ProcessingRequestEntity.parameters` con `@Convert(converter = MapToJsonConverter.class)` y `@Column(columnDefinition = "jsonb")`.

---

## Repositorios Spring Data (interfaz)

> No es obligatorio anotar con `@Repository` al extender `JpaRepository` (Spring Data registra el bean automáticamente). Puedes anotarlo si quieres hacerlo explícito.

* `UserRepository extends JpaRepository<UserEntity, UUID>`
* `DataFileRepository extends JpaRepository<DataFileEntity, UUID>`
* `BatchJobConfigRepository extends JpaRepository<BatchJobConfigEntity, UUID>`
* `ProcessingRequestRepository extends JpaRepository<ProcessingRequestEntity, UUID>`

    * `Page<ProcessingRequestEntity> findByStatus(RequestStatus status, Pageable pageable)`
* `JobExecutionRepository extends JpaRepository<JobExecutionEntity, UUID>`

    * `Optional<JobExecutionEntity> findTop1ByProcessingRequestIdOrderByStartTimeDesc(UUID processingRequestId)`
* `ReportRepository extends JpaRepository<ReportEntity, UUID>`

---

## Mappers (opcional, MapStruct)

Si necesitas llevar entidades a objetos de dominio (p. ej. para B1/F2-06):

* `UserMapper` → `User.ofId(entity.getId())`
* `DataFileMapper` → construye `DataFile` del core con sus campos
* `ProcessingRequestMapper` → crea `ProcessingRequest` del core y ajusta `status` aplicando sus transiciones (`markInProgress`, `markCompleted`, `markFailed`) según el enum almacenado.

> `BatchJobConfig` en el core usa builder con `logicalName` y genera ID; si solo necesitas el **id** en consultas, puedes omitir su reconstrucción completa hasta B5/B6.

---

## Tests de integración (`RepositoryIT`)

* **Tipo:** `@DataJpaTest` + **Testcontainers** (reutiliza lo de F2-07).
* **Esquema:** para esta HU, `spring.jpa.hibernate.ddl-auto=create-drop` y `spring.flyway.enabled=false` en el test. (En F2-09 migraremos a **Flyway**.)
* **Cobertura:**

    * Persistir `ProcessingRequestEntity` con `parameters` y leerlos de vuelta.
    * Crear 3 `JobExecutionEntity` con `startTime` distintos y comprobar que `findTop1ByProcessingRequestIdOrderByStartTimeDesc(...)` devuelve la última.
    * Consulta paginada `findByStatus(...)`.

**Ejemplo de aserción de “última ejecución”**

```java
var last = execs.findTop1ByProcessingRequestIdOrderByStartTimeDesc(prId).orElseThrow();
assertThat(last.getStartTime()).isEqualTo(t2).isAfter(t1).isAfter(t3);
```

---

## Configuración de test

* Mantén `application-test.yml` **sin** `spring.datasource.*` (Testcontainers inyecta).
* Si tu Boot < 3.1, añade `@DynamicPropertySource` en el test para mapear las props del contenedor.

---

## Criterios de aceptación

* CRUD básico para `User`, `DataFile`, `ProcessingRequest` pasa en tests de integración.
* Query `findTopByProcessingRequestIdOrderByStartTimeDesc` devuelve la ejecución correcta.
* Conversor JSONB funciona: inserta/lee `parameters` sin pérdida.

---

## Notas

* `@Repository` en interfaces de Spring Data es **opcional**.
* En B2-09 se introducirá **Flyway** para el esquema; por ahora los tests generan tablas con Hibernate (`create-drop`).

---

# F2-09 — Flyway baseline + JSONB y correcciones de test

> **Objetivo:** crear el **esquema inicial** con **Flyway**, mapear `JSONB` correctamente (sin acoplar `core`) y asegurar que los tests no ejecuten SQL de PostgreSQL en H2.

---

## Archivos creados/actualizados

* `api-service/src/main/resources/db/migration/postgresql/V1__baseline.sql` *(migración base vendorizada)*
* `api-service/src/main/resources/db/migration/postgresql/V2__alter_checksum_to_varchar.sql` *(ajuste de tipos)*
* `api-service/src/main/resources/application-test.yml` *(Flyway + validate, sin datasource)*
* **Entidades (ajustes)** en `api-service`:

    * `ProcessingRequestEntity.parameters` → `@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition = "jsonb"`
    * `UserEntity.role` → `@Enumerated(EnumType.STRING)`; `createdAt` con `@PrePersist` para default
* **Tests**: desactivar Flyway solo en smokes que no usan BD o vendorizar migraciones

---

## Esquema con Flyway (PostgreSQL)

**`V1__baseline.sql`** (ubicación: `db/migration/postgresql/`):

* Extensiones/Tipos Postgres
* Tablas y claves foráneas:

    * `users (id uuid PK, name varchar(140), email varchar(140) UNIQUE NOT NULL, role varchar(32) NOT NULL, created_at timestamptz NOT NULL DEFAULT now())`
    * `data_files` (`checksum_sha256 varchar(64)`, etc.)
    * `batch_job_configs`
    * `processing_requests (parameters jsonb NOT NULL DEFAULT '{}'::jsonb)`
    * `job_executions`
    * `reports`
* Índices:

    * `idx_pr_status_created (status, created_at DESC)`
    * `idx_je_pr_start_desc (processing_request_id, start_time DESC)`

**`V2__alter_checksum_to_varchar.sql`**:

```sql
ALTER TABLE data_files
  ALTER COLUMN checksum_sha256 TYPE varchar(64);
```

> **Nota ****`users`****:** se actualizó para reflejar tu `UserEntity` (`email` único, `role`, `created_at`).

---

## JSONB sin converter (Hibernate 6)

En `ProcessingRequestEntity`:

```java
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@JdbcTypeCode(SqlTypes.JSON)
@Column(columnDefinition = "jsonb", nullable = false)
private Map<String, String> parameters;
```

> El campo `parameters` se persiste como **jsonb** nativo; no hace falta `AttributeConverter` ni `PGobject`.

---

## Configuración por perfil

**`application-test.yml`** (tests con Testcontainers + Flyway):

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration/{vendor}
    baseline-on-migrate: false
  sql:
    init:
      mode: never
logging:
  level:
    root: WARN
    org.springframework: WARN
    org.flywaydb: INFO
```

**`application-dev.yml`** (resumen):

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/dataflow}
    username: ${DB_USER:app}
    password: ${DB_PASSWORD:secret}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration/{vendor}
    baseline-on-migrate: true
```

> Usar `{vendor}` permite que Flyway cargue `postgresql/` con Postgres y (si lo necesitas) `h2/` con H2.

---

## Estrategia de tests

* **Integración JPA/Repos:**

    * `@DataJpaTest` + Testcontainers (PostgreSQL 16) con Flyway habilitado (`ddl-auto=validate`).
    * Verifica: persistencia de `parameters` (jsonb), query de última `JobExecution`, paginación por `status`.

* **Smokes sin BD (web/actuator/config):**

    * Deshabilitar Flyway en el test:

      ```java
      @SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
      })
      class HealthExtraControllerIT { }
      ```

* **Opcional:** si necesitas H2, crea migraciones en `db/migration/h2/` (sin `CREATE EXTENSION` ni `jsonb`) y deja `locations: classpath:db/migration/{vendor}`.

---

## Errores típicos y solución

* **Duplicated version** (`Found more than one migration with version 1`):

    * Evita tener `V1__*.sql` en `src/main/resources` y `src/test/resources` a la vez.
    * Mantén todas las versionadas en `main` y usa `R__*.sql` para seeds de test.

* **H2 ejecutando SQL PG** (`CREATE EXTENSION`, `jsonb`):

    * Vendoriza migraciones (`{vendor}`) o desactiva Flyway en smokes.

* **Mismatch de tipos** (`char(64)` vs `varchar(64)`):

    * Aplica `V2__alter_checksum_to_varchar.sql` o ajusta `@Column(columnDefinition = "char(64)")` (no recomendado).

---

## Verificación

**Desarrollo (dev):**

```bash
export SPRING_PROFILES_ACTIVE=dev DB_URL=jdbc:postgresql://localhost:5432/dataflow DB_USER=app DB_PASSWORD=secret
mvn -pl api-service spring-boot:run
```

**Tests (PostgreSQL real):**

```bash
mvn -pl api-service -Dtest=RepositoryIT,DbSmokeTest test
```

Debes ver en logs: `Successfully applied V1` (+ `V2` si procede) y `ddl-auto=validate` sin errores.

---

## Criterios de aceptación

* Migraciones Flyway aplican limpias en PostgreSQL (V1 + V2).
* `ProcessingRequestEntity.parameters` persiste/lee como **jsonb**.
* Tests de integración pasan con Testcontainers; smokes no fallan por SQL de Postgres.
* `users` refleja `email UNIQUE`, `role` (string) y `created_at`.

---

# F2-10 — Manejador global de errores (REST)

> **Objetivo:** unificar el **formato JSON** de errores en toda la API con un `@RestControllerAdvice`, mapear correctamente las excepciones comunes (validación, negocio, infraestructura) y cubrirlo con tests de MVC.

---

## Archivos creados/actualizados

* `api-service/src/main/java/.../exception/RestExceptionHandler.java` *(nuevo handler global)*
* `api-service/src/test/java/.../RestExceptionHandlerTest.java` *(tests de mapeo 400/404/503/500)*
* **Elimina/sustituye** `ApiErrorHandler` previo para evitar dos `@RestControllerAdvice` en el contexto.

**Dependencias:** se reutilizan las de `api-service` (incl. `spring-boot-starter-validation`). No se tocan `core` ni `lib`.

---

## Formato de respuesta de error

```json
{
  "timestamp": "2025-08-11T03:15:29.123Z",
  "path": "/ruta",
  "code": "VALIDATION_ERROR",
  "message": "Invalid request",
  "fields": [ {"field": "title", "message": "must not be blank"} ],
  "traceId": "..."  // opcional si hay MDC/observabilidad
}
```

**Tipos auxiliares:**

* `FieldItem { field, message }`
* `ErrorResponse { timestamp, path, code, message, fields[], traceId? }`

---

## Mapeos implementados

| HTTP | code                      | Excepción manejada                                                                                                                               |
| ---- | ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| 400  | `VALIDATION_ERROR`        | `MethodArgumentNotValidException`, `BindException`, `ConstraintViolationException`, `HttpMessageNotReadableException` *(→ **`MALFORMED_JSON`**)* |
| 400  | `BUSINESS_RULE_VIOLATION` | `IllegalArgumentException` (mensajes de dominio mapeados a `fields` cuando aplica)                                                               |
| 400  | `FILE_TOO_LARGE`          | `FileTooLargeException`                                                                                                                          |
| 404  | `NOT_FOUND`               | `ResourceNotFoundException`                                                                                                                      |
| 503  | `SERVICE_UNAVAILABLE`     | `DataAccessException` (problemas de BD/infra)                                                                                                    |
| 500  | `UNEXPECTED_ERROR`        | `Exception` (catch‑all)                                                                                                                          |

> **Nota:** los métodos `@ExceptionHandler` son **públicos**. El handler obtiene `traceId` de `MDC` si está disponible.

---

## Integración con controladores

* No requieren cambios: cualquier excepción de las listadas será serializada con el formato anterior.
* Para reglas de dominio que hoy lanzan `IllegalArgumentException`, el handler responde **400 BUSINESS\_RULE\_VIOLATION** y, si reconoce el mensaje, rellena `fields` (por ejemplo: `originalFilename is blank` → `fields[originalFilename]`).

---

## Tests

### Opción A (recomendada): MockMvc standalone

Registra explícitamente el controller de prueba y el advice:

```java
MockMvc mvc = MockMvcBuilders
  .standaloneSetup(new DummyController())
  .setControllerAdvice(new RestExceptionHandler())
  .build();
```

Casos cubiertos en `RestExceptionHandlerTest`:

* 400 con `fields[]` (Bean Validation)
* 404 `NOT_FOUND`
* 503 `SERVICE_UNAVAILABLE` (simulando `DataAccessException`)
* 500 `UNEXPECTED_ERROR`

### Opción B: `@WebMvcTest`

```java
@WebMvcTest(controllers = DummyController.class)
@Import(RestExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false) // si seguridad interfiere
```

> Si ves 500 en lugar de 400/404/503: confirma que el advice está **importado** en el slice y que los `@ExceptionHandler` son **public**.

---

## Troubleshooting

* **500 en tests de validación:** faltaba importar el advice en `@WebMvcTest` o los `@ExceptionHandler` no eran `public`.
* **`HttpMessageNotReadableException`** por JSON malformado: se devuelve `code="MALFORMED_JSON"` con 400.
* **Tests que no usan BD fallan por Flyway/H2:** deshabilitar Flyway en esos tests (`spring.flyway.enabled=false`) o vendorizar migraciones (ver HU F2-09).

---

## Criterios de aceptación

* Todas las respuestas de error comparten el **mismo shape** y `Content-Type: application/json`.
* Validación → **400** con `fields[]`.
* Recurso inexistente → **404 NOT\_FOUND**.
* Problemas de BD → **503 SERVICE\_UNAVAILABLE**.
* Errores no controlados → **500 UNEXPECTED\_ERROR**.
* Tests de MVC cubren los cuatro escenarios anteriores.

---

## F2-12 — Pruebas de integración de seguridad

> **Objetivo:** verificar la configuración de seguridad de la API mediante pruebas de integración con usuarios en memoria y autenticación básica.

---

## Archivos creados/actualizados

* `api-service/src/test/java/.../SecurityIntegrationTest.java` *(nuevo test de integración)*
* `api-service/src/main/java/.../config/SecurityConfig.java` *(configuración de seguridad con usuarios en memoria y reglas de autorización)*

**Dependencias:** se utilizan las de `api-service` junto con H2 en memoria y `spring-security-test` para soporte de `MockMvc` con autenticación.

---

## Comportamiento probado

* Endpoints públicos (por ejemplo, `/ping`, `/actuator/health`) accesibles sin autenticación → **200 OK**.
* Endpoints protegidos requieren credenciales válidas → **401 Unauthorized** sin autenticación.
* Control de acceso por rol funciona correctamente (por ejemplo, endpoints solo para ADMIN → **403 Forbidden** para usuarios USER).
* Autenticación exitosa pero recurso inexistente → **404 Not Found**.
* Autenticación exitosa pero petición inválida → **400 Bad Request**.

---

## Implementación de pruebas

* **Frameworks**: `SpringBootTest`, `MockMvc`, `JUnit 5`.
* **Autenticación**: HTTP Basic con usuarios en memoria (`user` / `user123`, `admin` / `admin123`).
* **Base de datos**: H2 en memoria configurada para el perfil de pruebas, Flyway deshabilitado para acelerar la ejecución.

---

## Ejecución de las pruebas

```bash
mvn test -Dtest=SecurityIntegrationTest
```

---

## Criterios de aceptación

* `/actuator/health` responde **200** sin auth; `/processings/**` devuelve **401** sin credenciales.
* Con `user/user123` se accede a `GET /processings/{id}`; con `admin/admin123` se accede además a endpoints `ADMIN` si los hay.
* Tests de seguridad cubren: 401, 403 y acceso válido.

---

## Notas

* Para desarrollo local, CORS está habilitado para `http://localhost:3000`.
* Las contraseñas se almacenan usando `BCryptPasswordEncoder`.
* La configuración está definida en `SecurityConfig` y aplica tanto a entornos de producción como de prueba.
* En los tests de integración se usan códigos de estado **404** o **400** para confirmar que la autenticación pasó, aunque la lógica de negocio no encuentre el recurso o el body sea inválido.

---

# Seguridad — Comparativa Basic vs JWT (Bearer)

> **Objetivo:** documentar ventajas y desventajas de **Basic Auth** y **JWT (Bearer)** en la API, y proponer una **estrategia de migración** para fases siguientes.

---

## Archivos creados/actualizados

* `docs/security-authn.md` *(este documento)*
* (Opcional PoC) `api-service/src/main/resources/application-jwt.yml` *(perfil de pruebas para resource server JWT)*

**Dependencias (PoC JWT):** `spring-boot-starter-oauth2-resource-server`.

---

## Resumen ejecutivo

* **Fase actual (F2):** **Basic Auth** — simple, rápido para equipos internos y pruebas.
* **Fase siguiente (F3+):** **JWT (Bearer)** — mejor para microservicios, propagación entre servicios y escalabilidad.

---

## Comparativa por ejes

| Eje                     | Basic Auth                                                               | JWT (Bearer)                                                            |
| ----------------------- | ------------------------------------------------------------------------ | ----------------------------------------------------------------------- |
| **Estado**              | Envía credenciales en **cada** request.                                  | Token firmado, **stateless**, con expiración.                           |
| **Rotación/Revocación** | Cambiar contraseña del usuario.                                          | Compleja: listas negras, tokens **short‑lived**, **refresh tokens**.    |
| **Tamaño/latencia**     | Ligero, pero puede requerir lookups (si hay user service).               | Token más pesado (claims), evita lookups por request.                   |
| **Microservicios**      | Cada salto debe revalidar credenciales.                                  | Facilita propagación entre servicios con el mismo token.                |
| **Seguridad**           | Requiere **HTTPS**; credenciales sensibles en tránsito con cada request. | **HTTPS** + gestión de llaves (**JWK/Issuer**) y relojes sincronizados. |
| **CSRF**                | Puede requerir protección si se usa desde navegador.                     | APIs stateless con Bearer suelen quedar fuera de riesgo de CSRF.        |
| **Observabilidad**      | Sin claims; menos contexto.                                              | Claims (sub, scope, roles, tenant) mejoran trazabilidad.                |
| **Operación**           | Sin gestión de llaves.                                                   | Gestión de **JWKS**, rotación de firmas, reloj del cluster.             |

---

## Recomendación

1. **Mantener Basic Auth en F2** (interno + pruebas) por rapidez y menor superficie de cambio.
2. **Planificar migración a JWT en F3+** usando `spring-boot-starter-oauth2-resource-server` con validación de tokens desde un **Issuer** (Keycloak/Okta/issuer propio).

    * Tokens **cortos** (5–15 min).
    * Rotación de llaves (JWKS).
    * Scopes/roles en claims según dominios de negocio.

---

## Lineamientos de diseño para migrar a JWT

* **Boundary de seguridad**: validar el token en el **edge** (API gateway o cada microservicio) y propagar solo claims necesarios.
* **Autorización**: usar anotaciones (`@PreAuthorize`) basadas en authorities/scopes extraídos del token, o reglas en filtros.
* **Clock skew**: configurar tolerancia (±60s).
* **Errores**: mapear 401 por token inválido/expirado y 403 por falta de scopes/roles.

---

## PoC opcional (resource server)

### Encabezado Bearer

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

### `application-jwt.yml` (perfil de prueba)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Elige uno de los dos enfoques
          # 1) Validación por Issuer (OpenID Connect/Keycloak/Okta)
          issuer-uri: http://localhost:9000/realms/dataflow
          # 2) Validación por clave pública (RSA) local
          # jwk-set-uri: http://localhost:9000/realms/dataflow/protocol/openid-connect/certs

# Activar este perfil solo en el PoC para no interferir con Basic
spring:
  profiles:
    active: jwt
```

### Dependencia (PoC)

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### Config mínima Java (si no usas autoconfig por perfil)

```java
@Bean
SecurityFilterChain jwtFilterChain(HttpSecurity http) throws Exception {
  http
    .authorizeHttpRequests(a -> a
      .requestMatchers("/actuator/health", "/ping").permitAll()
      .anyRequest().authenticated())
    .oauth2ResourceServer(o -> o.jwt());
  return http.build();
}
```

### Tests sugeridos (PoC)

* **200** con token válido (firma OK y no expirado).
* **401** con token sin firma válida o expirado.
* **403** con token válido pero sin el scope/rol requerido.

---

## Criterios de aceptación

* Documento `docs/security-authn.md` con **tabla comparativa** y **recomendación clara** (Basic ahora, JWT luego).
* *(Si se realiza PoC)* existe `application-jwt.yml` y un test que demuestra **200** con token válido y **401** con token inválido.
* Configuración separada por **perfil** para no interferir con Basic en `dev`.

---

## Notas

* Para producción, planificar **rotación de llaves**, tiempos de expiración cortos y estrategia de **refresh tokens** (si aplica a clientes de confianza).
* Considerar un **Issuer** central (Keycloak/Okta/Cognito) para estandarizar *claims*, *scopes* y rotación de llaves.
* La migración debe ser **compatible**: convivir temporalmente **Basic** (interno) y **JWT** (clientes externos) detrás de paths o perfiles distintos.
* Documentar *error codes* de autenticación/autorización para los clientes (401 vs 403).

---

## F2-14 — Actuator y Métricas (Micrometer)

> **Objetivo:** habilitar y verificar métricas de Actuator, incluyendo métricas personalizadas para el conteo de ejecuciones de Job y pruebas de su exposición.

---

## Archivos creados/actualizados

* `api-service/src/main/resources/application.yml` *(configuración de exposición de endpoints Actuator y etiquetas de métricas)*
* `api-service/src/main/java/.../metrics/JobMetrics.java` *(registro de métricas personalizadas con Micrometer)*
* `api-service/src/test/java/.../HealthActuatorIT.java` *(test de integración para /actuator/health)*
* `api-service/src/test/java/.../MetricsActuatorIT.java` *(test de integración para /actuator/metrics y métricas personalizadas)*

**Dependencias:**

* `spring-boot-starter-actuator` (endpoints de observabilidad)
* `io.micrometer:micrometer-registry-prometheus` *(opcional para exposición en Prometheus)*

---

## Comportamiento probado

* `/actuator/health` expuesto públicamente y retorna **200 OK** con estado `UP`.
* `/actuator/metrics` accesible para usuarios autenticados.
* Métrica personalizada `dataflow.job.executions.total` presente con tags configurados (`result=success|fail`).
* Incremento del contador al simular la finalización de un Job.
* Validación opcional de `/actuator/prometheus` si está habilitado.

---

## Implementación de pruebas

* **Frameworks:** `SpringBootTest`, `MockMvc`, `JUnit 5`.
* **Base de datos:** H2 en memoria para pruebas, Flyway deshabilitado.
* **Flujo de test:**

    1. Llamar a `/actuator/health` y verificar estado `UP`.
    2. Consultar `/actuator/metrics/dataflow.job.executions.total` y validar presencia de `measurements`.
    3. Simular incremento de contador y verificar cambio.

---

## Ejecución de las pruebas

```bash
mvn test -Dtest=HealthActuatorIT,MetricsActuatorIT
```

---

## Criterios de aceptación

* `/actuator/metrics` y `/actuator/health` expuestos según perfil.
* `dataflow.job.executions.total` visible y se incrementa en pruebas.
* `/actuator/prometheus` devuelve series con prefijo `dataflow_` si está habilitado.
* Documentación breve en README sobre consulta y significado de métricas.

---

## Notas

* En desarrollo, configurar cuidadosamente Actuator para no exponer información sensible.
* Tag `application` definido como `dataflowhub-api` para dashboards.
* Con Prometheus habilitado, `/actuator/prometheus` puede ser consumido por Prometheus o APM compatible.

---

## F2-15 — Logs Estructurados (JSON) con traceId

> **Objetivo:** habilitar logs en formato JSON que incluyan `traceId` para mejorar la trazabilidad y facilitar la integración con herramientas de análisis de logs.

---

## Archivos creados/actualizados

* `api-service/src/main/resources/logback-spring.xml` *(configuración para salida JSON con traceId/spandId en cada evento)*
* `api-service/src/test/java/.../StructuredLoggingIT.java` *(test de integración para validar formato y campos clave)*
* Configuración por perfil en `application.yml` y `application-dev.yml` para niveles de logging.

**Dependencias:**

* Opción A: `net.logstash.logback:logstash-logback-encoder` para JSON estructurado.
* Opción B: Patrón JSON manual con `%mdc` (sin encoder externo).

---

## Comportamiento implementado

* Logs en formato JSON en entornos **dev** y **prod**.
* Inclusión automática de `traceId` y `spanId` en cada evento.
* Niveles de log configurables por perfil (`DEBUG` en dev, `INFO` en prod).
* Integración opcional con Micrometer Tracing para poblar automáticamente el `traceId`.
* Filtro `OncePerRequestFilter` para generar y propagar `traceId` cuando no exista.

---

## Ejemplo de log generado

```json
{
  "timestamp": "2025-08-11T05:25:10.111Z",
  "level": "INFO",
  "logger": "com.dataflowhub.api.web.ProcessingController",
  "traceId": "f9f1a2b3c4d5e67fa",
  "message": "ProcessingRequest created",
  "requestId": "7e2a1d7c-3b9b-4f1a-8a55-9a2f1e4c7788",
  "user": "user@acme.com"
}
```

---

## Ejecución de las pruebas

```bash
mvn test -Dtest=StructuredLoggingIT
```

---

## Criterios de aceptación

* Logs en JSON en **dev/prod** con `traceId` presente.
* Control de niveles por perfil (`DEBUG` en dev, `INFO` en prod).
* Tests validan presencia de campos clave (`traceId`, `message`).
* README de "Observabilidad" documenta cómo visualizar `/actuator/*` y ejemplo de log JSON.

---

## Notas

* No incluir datos sensibles en los logs.
* Se recomienda incluir campos de contexto como: `requestId`, `user`, `endpoint`.
* Niveles: `INFO` normal, `WARN` recuperable, `ERROR` con stacktrace.

---

## F2-16 — Lanzar Job Batch vía REST (`POST /jobs/{id}/run`)

> **Objetivo:** permitir la ejecución bajo demanda de un Job de Spring Batch vía un endpoint REST seguro y restringido a administradores.

---

## Archivos creados/actualizados

* `api-service/src/main/java/.../rest/JobController.java` *(nuevo controlador para ejecutar Jobs vía REST)*
* `api-service/src/test/java/.../JobControllerTest.java` *(tests unitarios)*
* Configuración de Spring Batch en perfiles `dev`, `test` y `prod` (`application-*.yml`).

**Dependencias:** `spring-boot-starter-batch` (solo en `api-service`).

---

## Comportamiento implementado

* Endpoint `POST /jobs/{configId}/run` protegido con rol **ADMIN**.
* Construcción de `JobParameters` con:

    * `processingRequestId` (UUID como String)
    * `configId` (desde el path)
    * `requestTime` (Instant en ISO-8601 o epoch para asegurar unicidad)
* Uso de `JobLauncher.run(job, params)` para iniciar el Job y retorno **202 Accepted** con:

    * Header `Location` apuntando a `/processings/{processingRequestId}`
    * Body con `jobInstanceId` y `jobExecutionId`.
* Control de concurrencia: si existe una ejecución `RUNNING` con los mismos parámetros clave, responde **409 Conflict** con código `JOB_ALREADY_RUNNING`.
* Registro de métricas personalizadas (`dataflow.job.executions.total`).
* Manejo de errores:

    * Config desconocido → **404 CONFIG\_NOT\_FOUND**
    * Error de infraestructura → **503**

---

## Ejecución de pruebas

* **Unitarias:** Mock de `JobLauncher` para verificar construcción de `JobParameters` y respuesta 202.
* **Integración:** con Spring Batch activo, ejecuta un Job dummy y verifica registros en tablas `BATCH_*`.

```bash
mvn test -Dtest=JobControllerTest
```

---

## Ejemplo de uso

**Request (mínimo):**

```http
POST /jobs/csv_to_ipa_v1/run
Content-Type: application/json

{
  "processingRequestId": "7e2a1d7c-3b9b-4f1a-8a55-9a2f1e4c7788",
  "parameters": { "delimiter": ";" }
}
```

**Response (202):**

```json
{
  "jobInstanceId": 123,
  "jobExecutionId": 456
}
```

**Conflicto si ya corre → 409:**

```json
{
  "code": "JOB_ALREADY_RUNNING"
}
```

---

## Criterios de aceptación

* Endpoint protegido (ADMIN) retorna 202 y `Location` válido.
* Si existe ejecución RUNNING con mismos parámetros clave → 409.
* Spring Batch crea registros en tablas `BATCH_*` o mock verificado en unit test.
* Métrica `dataflow.job.executions.total` incrementa al aceptar.

---

## Notas

* **Perfiles:** en `dev` y `test` usar H2; en `prod` conectar a base persistente.
* **Persistencia:** esquema JDBC de Spring Batch se autoinicializa en `dev/test`; en `prod` incluir en Flyway.
* **Seguridad:** restringido a rol `ADMIN` según configuración de `SecurityConfig`.
* **Métricas:** visibles vía `/actuator/metrics/dataflow.job.executions.total`.

---

## F2-17 — Scheduler diario con `@EnableScheduling` (02:00 en `prod`)

> **Objetivo:** Automatizar la ejecución diaria de Jobs Batch a las 02:00 en el entorno de producción, con control de concurrencia, métricas y logs estructurados.

---

## Archivos creados/actualizados

* `api-service/src/main/java/.../BatchScheduler.java` *(nuevo scheduler)*
* `application-prod.yml` *(propiedad `scheduling.enabled=true`)*
* `application-dev.yml` y `application-test.yml` *(propiedad `scheduling.enabled=false`)*

**Dependencias:** uso de `spring-context` para `@EnableScheduling` y `@Scheduled`.

---

## Comportamiento implementado

* El scheduler se activa únicamente en perfil **`prod`** gracias a la propiedad `scheduling.enabled=true`.
* A las **02:00** se ejecuta el método planificado (`@Scheduled(cron = "0 0 2 * * *")`).
* Consulta en la base de datos todas las solicitudes de procesamiento (`ProcessingRequest`) con estado **PENDING** (máx. 50 por ciclo).
* Para cada solicitud, invoca internamente el mismo servicio que expone el endpoint `/jobs/{configId}/run` de la HU F2-16.
* Verifica mediante `JobExplorer` que no exista ya una ejecución **RUNNING** para el mismo `processingRequestId`.
* Implementa control de back-pressure: si hay más de `N` ejecuciones en estado **RUNNING**, pospone el resto.
* Registra en métricas (`dataflow.scheduler.trigger{result=launched|skipped}`) y logs (`traceId`) el resultado de cada ejecución.

---

## Ejecución del Scheduler

* **Prod:** activo a las 02:00.
* **Dev/Test:** desactivado (`scheduling.enabled=false`).

---

## Ejemplos

**application-prod.yml**

```yaml
scheduling:
  enabled: true
```

**Log esperado (JSON)**

```json
{
  "message": "Scheduler launched 17 jobs",
  "result": "launched",
  "pending": 23,
  "launched": 17,
  "skipped": 6,
  "traceId": "f91fa2b3c4d5e67a"
}
```

---

## Criterios de aceptación

* Con `scheduling.enabled=true` en **prod**, el método se ejecuta a las **02:00**.
* No lanza un job si ya hay una ejecución **RUNNING** para el mismo `processingRequestId`.
* Métrica `dataflow.scheduler.trigger` visible en `/actuator/metrics`.
* Tests unitarios verifican el comportamiento en ambos perfiles.

---

## Notas

* En entornos locales, mantener `scheduling.enabled=false` para evitar ejecuciones involuntarias.
* La implementación actual está preparada para integrarse con el flujo real de ejecución de jobs definido en la HU F2-16.
* Los logs incluyen `traceId` para permitir trazabilidad completa de ejecuciones.

---

## F2-20 — Configuración de despliegue con Docker

> **Objetivo:** preparar la aplicación `api-service` para ser empaquetada y ejecutada en contenedores Docker, incluyendo configuración para PostgreSQL y perfiles de ejecución.

---

## Archivos creados/actualizados

* `Dockerfile` *(nuevo)* — Construcción multi-stage (JDK para compilación, JRE para ejecución) con empaquetado optimizado.
* `docker-compose.yml` *(nuevo)* — Orquestación de `api-service` y base de datos PostgreSQL.
* Configuración en `application.properties` para soportar variables de entorno y perfiles (`prod`, `dev`, `test`).

**Dependencias:**

* Imagen base `eclipse-temurin` (Java 21).
* PostgreSQL 16-alpine.

---

## Comportamiento esperado

* Construcción del JAR mediante **multi-stage build**:

    1. Etapa de compilación con Maven y JDK 21.
    2. Etapa final con JRE 21 y el `app.jar` listo para ejecutar.
* Ejecución en contenedor con variables de entorno para DB y perfil activo.
* Orquestación con `docker-compose` que levanta la app y PostgreSQL en red compartida.
* Posibilidad de usar `host.docker.internal` para conectar a PostgreSQL local en entornos sin Compose.

---

## Ejecución de pruebas

### 1) Solo la imagen de la app (sin DB)

```bash
docker build -t dataflowhub/api-service .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_FLYWAY_ENABLED=false \
  -e SPRING_DATASOURCE_URL='jdbc:h2:mem:testdb;MODE=PostgreSQL' \
  dataflowhub/api-service
```

### 2) App + DB con Docker Compose

```bash
docker compose up --build
```

Esto levanta PostgreSQL (`db`) y `api-service` conectados en la misma red Docker.

### 3) App conectando a DB local

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://host.docker.internal:5432/dataflow' \
  -e SPRING_DATASOURCE_USERNAME=app \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  dataflowhub/api-service
```

---

## Ejemplo de configuración `docker-compose.yml`

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: dataflow
      POSTGRES_USER: app
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"

  api:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/dataflow
      SPRING_DATASOURCE_USERNAME: app
      SPRING_DATASOURCE_PASSWORD: secret
    ports:
      - "8080:8080"
    depends_on:
      - db
```

---

## Criterios de aceptación

* Se puede construir la imagen del servicio con `docker build` sin errores.
* La aplicación inicia correctamente en contenedor usando DB en Compose.
* Se soporta conexión a DB local o en otro contenedor usando variables de entorno.
* El empaquetado multi-stage reduce el tamaño final de la imagen.

---

## Notas

* En Windows/Mac, `host.docker.internal` apunta al host; en Linux puede requerir configuración adicional.
* Flyway está habilitado en `prod` por defecto; deshabilitar (`SPRING_FLYWAY_ENABLED=false`) si no se desea migrar en contenedor.
* Para desarrollo, se recomienda mapear `application.properties` externos como volumen para ajustes rápidos sin reconstruir la imagen.

---

