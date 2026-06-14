# Implementation conventions

Project-specific rules for implementing REST endpoints with TDD.
Complements [`design.md`](design.md) (what) and [`plan.md`](plan.md) (when).

Use this document before starting phases 2.3–2.5 so each endpoint follows the same line.

---

## Layer responsibilities

| Layer | Package | Responsibility |
| --- | --- | --- |
| API | `api/` | HTTP mapping, request/response DTOs, `type` normalization, Bean Validation trigger |
| Application | `application/` | Use-case orchestration; delegates persistence to repository |
| Domain | `domain/` | `Transaction`, exceptions, `TransactionRepository` interface |
| Infrastructure | `infrastructure/` | Persistence and indexes (already implemented) |

**Controller:** HTTP only — no business logic.

**Service:** builds `Transaction` and calls `repository.save()` — hierarchy rules are enforced atomically inside `save()`.

**Repository:** persistence and indexes; runs `TransactionHierarchyRules` under the write lock before mutating state.

---

## JSON and DTO conventions

### Snake_case in JSON, camelCase in Java

The challenge spec uses `parent_id` in JSON. Use **explicit `@JsonProperty` per field** when the JSON name differs from Java — do not apply a global snake_case naming strategy.

```java
public record PutTransactionRequest(
    @NotNull @PositiveOrZero Double amount,
    @NotBlank String type,
    @JsonProperty("parent_id") Long parentId
) {}
```

**Why:** only `parent_id` differs today; explicit annotations stay readable and avoid surprising renames if new camelCase fields are added later.

### Wrapper types for required numeric fields

Use **`Double` (object) with `@NotNull`**, not primitive `double`, for `amount`.

**Why:** a missing `amount` in JSON deserializes to `null` → Bean Validation returns 400. A primitive would silently become `0.0`.

### Optional fields

Use nullable wrapper types (`Long parentId`). Absent field in JSON = `null` = no parent.

### Response DTOs

One small record per response shape, e.g. `StatusResponse(String status)` for `{ "status": "ok" }`.

---

## Validation split

| Concern | Where | Mechanism |
| --- | --- | --- |
| `amount` present and `>= 0` | DTO | `@NotNull @PositiveOrZero` |
| `type` non-blank before normalization | DTO | `@NotBlank` (raw value) |
| `type` empty after `trim()` | API boundary | Custom check after trim → `IllegalArgumentException` |
| `parent_id` exists | Repository (`save`) | `TransactionHierarchyRules` under write lock → `InvalidParentException` |
| Cycle in hierarchy | Repository (`save`) | Same as above |
| Self-parent (`parent_id == id`) | Repository (`save`) | Same as cycle check |

Normalize `type` **after** `@NotBlank` passes on the raw value, then reject if empty post-trim.

---

## `type` normalization (API boundary)

Always at the API layer on `PUT` (body) and `GET /types/{type}` (path segment):

```java
String normalized = rawType.trim().toLowerCase(Locale.ROOT);
if (normalized.isEmpty()) {
    throw new IllegalArgumentException("type must not be empty");
}
```

Pass `normalized` to the service. Domain and repository always store lowercase.

---

## HTTP error handling — Option A

**During endpoint phases (2.3–2.5):**

- Implement validation logic and throw domain/API exceptions.
- Do **not** add `@RestControllerAdvice` yet.
- Integration tests in each endpoint phase cover the **happy path only**.

**Phase 2.6:**

- Add centralized `@RestControllerAdvice`.
- Add integration tests for HTTP status codes and error JSON (400, 404).

| Exception | HTTP status (from 2.6) |
| --- | --- |
| `InvalidParentException` | 400 |
| `TransactionNotFoundException` | 404 |
| `IllegalArgumentException` | 400 |
| `MethodArgumentNotValidException` (Bean Validation) | 400 |

**Optional during 2.3–2.5:** unit tests on `TransactionService` with a mocked repository to verify parent/cycle rules without asserting HTTP status.

---

## Success responses

| Endpoint | Status | Body |
| --- | --- | --- |
| `PUT /transactions/{id}` | **200 OK** always (create and replace) | `{ "status": "ok" }` |
| `GET /transactions/types/{type}` | 200 | `[10, 11, ...]` |
| `GET /transactions/sum/{id}` | 200 | `{ "sum": 20000.0 }` |

Never return `201 Created` on PUT.

---

## TDD workflow per endpoint

Follow the order in [`plan.md`](plan.md) for each phase:

1. **Red** — integration test for the happy path (`@SpringBootTest` + `@AutoConfigureMockMvc`).
2. **Green** — minimal controller, DTOs, service method, repository call.
3. **Refactor** — normalization, validations, naming; keep tests green.
4. **Commit** — one commit per deliverable endpoint (not per bullet in the plan).

### Integration test conventions

- Location: `src/test/java/.../api/` (mirror the API package).
- Naming: `{Endpoint}{Verb}IntegrationTest` or `{Feature}IntegrationTest`.
- First test: reproduce the spec example for that endpoint.
- Assert HTTP status and response body only — do not assert cross-endpoint behavior (e.g. PUT tests do not call GET).

### Test isolation

The in-memory repository is a singleton Spring bean. For a single happy-path test that chains calls (e.g. 3 PUTs), one test method is enough.

When multiple integration tests need a clean store (phases 2.4–2.6), pick one strategy and stick to it:

- `@DirtiesContext` per test method, or
- unique IDs per scenario, or
- a test-only `clear()` on the repository.

Decide in phase 2.4 when GET tests are added.

---

## Hierarchy validation (domain + repository)

Rules live in `domain/TransactionHierarchyRules` (pure functions on a transaction snapshot).

`InMemoryTransactionRepository.save()` calls them **under the write lock before any index mutation** — validate-then-persist in one critical section (like FK checks inside a SQL transaction).

```java
lock.writeLock().lock();
try {
    TransactionHierarchyRules.validateParent(transactions, id, parentId);
    // index updates + put
} finally {
    lock.writeLock().unlock();
}
```

The service does not call `exists()` / `findById()` separately for validation.

---

## Cycle detection (domain)

When saving transaction `id` with `parentId`:

```
current = parentId
while current != null:
    if current == id → throw InvalidParentException
    current = transactions.get(current).parentId()
```

Implemented in `TransactionHierarchyRules`; invoked from `save()` under the write lock.

---

## Commit conventions

- One commit per plan phase (`feat: implement PUT /transactions/{id}`, etc.).
- TDD happens locally in red-green-refactor cycles; the published commit is the finished deliverable.
- Do not mix endpoints in one commit (e.g. no GET types inside the PUT commit).

---

## Checklist for a new endpoint

- [ ] Integration test (happy path) written first and failing.
- [ ] DTOs with conventions above (`@JsonProperty`, `Double`, `@NotNull`).
- [ ] Controller delegates to service; no business logic in controller.
- [ ] `type` normalized at API boundary if the endpoint accepts `type`.
- [ ] Business validations for hierarchy enforced via `repository.save()` (not separate read-then-write calls).
- [ ] No `@RestControllerAdvice` until phase 2.6.
- [ ] `./gradlew test` green.
- [ ] Single feature commit per plan phase.

---

## Related docs

| File | Role |
| --- | --- |
| [`design.md`](design.md) | Domain model, API contracts, architecture |
| [`plan.md`](plan.md) | Phases, acceptance criteria, commit messages |
| [`challenge-prd.md`](challenge-prd.md) | Original specification |
