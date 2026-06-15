# Transactions API

REST API that stores transactions in memory and exposes three operations: create/replace a transaction, list IDs by type, and compute the transitive sum of a transaction and its descendants.

**Stack:** Java 21 · Spring Boot 3 · Gradle

## Prerequisites

- **JDK 21** for local development
- **Docker** (optional) for containerized runs

## Run locally

From the project root:

```bash
# Unix / macOS / Git Bash
./gradlew bootRun
```

```powershell
# Windows (PowerShell)
.\gradlew.bat bootRun
```

The API listens on **[http://localhost:8080](http://localhost:8080)**.

## Run with Docker

Build and run the image:

```bash
docker build -t transactions .
docker run -p 8080:8080 transactions
```

Or use Docker Compose:

```bash
docker compose up --build
```

## Run tests

```bash
# Unix / macOS / Git Bash
./gradlew test
```

```powershell
# Windows (PowerShell)
.\gradlew.bat test
```

Test reports are written to `build/reports/tests/test/index.html`.

## API examples

The examples below match the [challenge specification](docs/challenge-prd.md). Start the app first, then run:

```bash
# Create transactions (hierarchy: 10 → 11 → 12)
curl -s -X PUT http://localhost:8080/transactions/10 \
  -H "Content-Type: application/json" \
  -d '{"amount": 5000, "type": "cars"}'

curl -s -X PUT http://localhost:8080/transactions/11 \
  -H "Content-Type: application/json" \
  -d '{"amount": 10000, "type": "shopping", "parent_id": 10}'

curl -s -X PUT http://localhost:8080/transactions/12 \
  -H "Content-Type: application/json" \
  -d '{"amount": 5000, "type": "shopping", "parent_id": 11}'

# List IDs by type
curl -s http://localhost:8080/transactions/types/cars
# → [10]

curl -s http://localhost:8080/transactions/types/shopping
# → [11,12]

# Transitive sum (root + all descendants via parent_id)
curl -s http://localhost:8080/transactions/sum/10
# → {"sum":20000.0}

curl -s http://localhost:8080/transactions/sum/11
# → {"sum":15000.0}
```

### Endpoints


| Method | Path                         | Description                                                                       |
| ------ | ---------------------------- | --------------------------------------------------------------------------------- |
| `PUT`  | `/transactions/{id}`         | Create or replace a transaction → `200 OK` + `{ "status": "ok" }`                 |
| `GET`  | `/transactions/types/{type}` | JSON array of transaction IDs for the given type                                  |
| `GET`  | `/transactions/sum/{id}`     | `{ "sum": <double> }` — amount of the transaction plus all transitive descendants |


`type` is compared case-insensitively (e.g. `cars`, `CaRs`, and `CARS` are equivalent).

## Documentation

- [Challenge specification](docs/challenge-prd.md) — original requirements *(Spanish)*
- [Technical design](docs/design.md) — API rules, architecture, and algorithms
- [Execution plan](docs/plan.md) — implementation roadmap
- [Implementation conventions](docs/implementation-conventions.md) — TDD and endpoint patterns
- [In-memory repository concurrency](docs/technical/in-memory-repository-concurrency.md)

## Project layout

```
src/main/java/com/aleitox/transactions/
├── api/              # REST controllers, DTOs, exception handling
├── application/      # TransactionService
├── domain/           # Transaction model, repository interface, rules
└── infrastructure/   # InMemoryTransactionRepository
```

