# JobParameters – Especificación (C0 / HU F3-04)

Contrato canónico para el job principal (`csvToJpaJob`). Define claves, tipos, si son **identificantes** de `JobInstance`, obligatoriedad, validaciones y descripción.

| Clave                | Tipo Batch | Identif. | Oblig. | Validación / Dominio                  | Descripción                                                     |
|----------------------|-----------:|:--------:|:------:|---------------------------------------|-----------------------------------------------------------------|
| `processingRequestId`| String     |   Sí     |  Sí    | UUID válido                            | Id del pedido/lote a procesar. Fija la identidad del JobInstance. |
| `configId`           | String     |   Sí     |  Sí    | `BatchJobConfig` existente             | Plantilla/config de job a usar.                                |
| `storagePath`        | String     |   No     |  Sí    | Ruta legible/existente                 | Origen del archivo a procesar.                                 |
| `delimiter`          | String     |   No     |  No    | Uno de `,` `;`                         | Separador CSV (default `,`).                                   |
| `requestTime`        | Date       |   Sí*    |  No    | ISO‑8601 o epoch ms                    | Marca única para **forzar nueva instancia** (*ver reglas*).    |
| `chunkSize`          | Long       |   No     |  No    | Rango `100..10_000`                    | Tamaño del chunk (tuning).                                     |

> **Identificante** = participa en la identidad de `JobInstance`. Dos ejecuciones con los **mismos** parámetros *identificantes* pertenecen al **mismo** `JobInstance` (útil para `restart`). Si cambia **alguno**, se crea **otro** `JobInstance` (re‑ejecución/otra versión).

## Reglas de identidad
- `processingRequestId` y `configId` → **siempre identificantes**.
- `requestTime` → **identificante solo si se suministra**: úsalo para **forzar nueva instancia** (HU F3‑06).
- `storagePath` y `delimiter` → **no identificantes** (pueden variar sin cambiar la identidad si son parte del mismo pedido).
- Documentar toda decisión de identidad; cualquier cambio requiere migración/rollback plan.

## Mapeo REST → tipos Spring Batch
Spring Batch permite tipos: **String, Long, Double, Date**.
Ejemplo de payload REST y cómo se mapea:

```json
{
  "processingRequestId": "7e2a7c1e-9a37-49a0-9b2e-6c4a3a2f8f10",
  "configId": "csv_to_jpa_v1",
  "storagePath": "/data/in/ventas_julio.csv",
  "delimiter": ";",
  "chunkSize": 500,
  "requestTime": "2025-08-12T00:15:00Z"
}
