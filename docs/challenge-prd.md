# Java Code Challenge

Nos gustaría tener un servicio web RESTful que almacene transacciones (en memoria está bien) y devuelva información sobre esas transacciones.

Las transacciones a almacenar tienen un tipo y un monto. El servicio debe admitir la devolución de todas las transacciones para su tipo. Además, las transacciones se pueden vincular entre sí (usando un `parent_id`) y, por otro lado, necesitamos saber el monto total involucrado para todas las transacciones vinculadas a una transacción en particular.

1. Por favor, completar en Spring Boot (Java o Kotlin) y en no más de 3 días consecutivos.
2. Completar el proyecto en Bitbucket o Github, para que podamos revisar el código.
3. No usar SQL.

### Requerido

- Tests de integración.
- Aplicación dockerizada.
- Java 11 o superior.
- Claridad del código.
- Correctitud en diseño de arquitectura.

### Se valorará positivamente

- Uso de TDD.
- Desarrollo incremental de la solución mediante el uso de commits.
- Aplicación de los principios SOLID.
- Documentación.

## Especificación del servicio

### `PUT /transactions/$transaction_id`

**Body:**

```json
{
  "amount": double,
  "type": string,
  "parent_id": long
}
```

En dónde:

- `transaction_id` — Es de tipo `long`, identificador de una nueva transacción.
- `amount` — Es de tipo `double`, especificando el monto.
- `type` — Es un `string` que identifica el tipo de la transacción.
- `parent_id` — Es de tipo `long`, opcional. Especifica el id de la transacción padre.

### `GET /transactions/types/$type`

**Returns:** `[ long, long, .... ]`

Una lista JSON de todos los ids de las transacciones para el tipo especificado.

### `GET /transactions/sum/$transaction_id`

**Returns:** `{ "sum": double }`

La suma de todas las transacciones que están transitivamente conectadas por su `parent_id` a `$transaction_id`.

## Algunos ejemplos simples podrian ser:

```
PUT /transactions/10 { "amount": 5000, "type": "cars" } => { "status": "ok" }
PUT /transactions/11 { "amount": 10000, "type": "shopping", "parent_id": 10 }
PUT /transactions/12 { "amount": 5000, "type": "shopping", "parent_id": 11 }

GET /transactions/types/cars => [10]
GET /transactions/sum/10 => {"sum":20000}
GET /transactions/sum/11 => {"sum":15000}
```

