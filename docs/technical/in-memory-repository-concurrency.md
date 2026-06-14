# In-memory repository concurrency

Concurrency design for `InMemoryTransactionRepository`.
Overview in [`design.md`](../design.md).

---

## 1. Problem

Spring Boot serves HTTP requests concurrently. The repository keeps:

- a primary map of transactions
- auxiliary indexes (`byType`, `childrenByParent`) for fast lookups

Every `PUT` and `GET` can run in parallel across threads. Without coordination, shared mutable state would be unsafe and query results could be inconsistent.

---

## 2. Risks

| Risk | Cause |
| --- | --- |
| **Corrupted indexes** | `save` updates multiple structures in steps (remove old index entries, put transaction, add new index entries). Concurrent writes could leave indexes out of sync with the primary map. |
| **`ConcurrentModificationException`** | Index values use `HashSet` and `ArrayList`, which are not thread-safe. A read that iterates while `save` mutates the same collection can fail or return garbage. |
| **Inconsistent snapshots** | If each repository method acquires and releases its own lock, a caller composing several reads (e.g. list child IDs, then load each transaction) can observe a mix of old and new state when a `PUT` interleaves between calls. |

`ConcurrentHashMap` alone does not fix this: it only protects the map structure, not the non-thread-safe collections stored as values.

---

## 3. Solution

A single `ReentrantReadWriteLock` guards all access to the three maps:

| Operation | Lock | Methods |
| --- | --- | --- |
| **Write** | `writeLock()` | `save` |
| **Read** | `readLock()` | `findById`, `exists`, `findIdsByType`, `findChildrenIds`, `sumTransitive` |

- **Writes are exclusive** — one `save` at a time; the full upsert (index cleanup + put + index update) is atomic.
- **Reads can run in parallel** — multiple `GET` handlers do not block each other unless a `save` holds the write lock.
- **Reads never overlap with writes** — a query always sees a consistent snapshot.

Maps use plain `HashMap`; the lock provides the concurrency guarantee.

Read methods return `List.copyOf(...)` so callers cannot mutate internal index collections.

`sumTransitive` performs BFS over `childrenByParent` and sums `amount` values under **one** read lock, avoiding multiple lock cycles and mixed snapshots for `GET /transactions/sum/{id}`.

---

## 4. Optimizations

**Conditional index updates on upsert.** When only `amount` changes, `save` skips index work entirely. When `type` or `parentId` changes, only the affected index is updated. This shortens write-lock hold time and improves throughput.

**No precomputed subtree sums.** Maintaining a running sum per node would make reads O(1) but complicates every `PUT` (propagate amount changes up the parent chain, handle `parent_id` moves). For this challenge, on-demand BFS under a read lock is simpler and still O(descendants).

---

## 5. Trade-offs

| Choice | Benefit | Cost |
| --- | --- | --- |
| `ReadWriteLock` vs `synchronized` on all methods | Parallel reads | Slightly more complex than a single mutex |
| `HashMap` + lock vs `ConcurrentHashMap` | One clear correctness model; no false sense of safety on index values | All access goes through the lock |
| Write lock on `save` | Correct compound updates | Writes are serialized; heavy write load would queue |
| `sumTransitive` in the repository | Single snapshot, one lock for the sum endpoint | Traversal logic lives in infrastructure (acceptable for an in-memory adapter tied to `childrenByParent`) |

### What the repository guarantees — and what it does not

**Each repository method is atomic.** While `save`, `findById`, or `sumTransitive` runs, it holds the appropriate lock for the full method body. No other thread can write in the middle, and no read sees a half-updated map or index. When the method returns, the store is internally consistent.

**Several repository calls in a row are not one atomic operation.** If a caller invokes the repository multiple times, another HTTP request can interleave between those calls. Example of a problematic pattern:

```
findChildrenIds(10)   → read lock, then release
  … another thread runs PUT here …
findById(11)          → read lock, then release
```

Each call sees a valid snapshot on its own, but the two results may come from different moments in time. That is why `sumTransitive` does the full traversal in a single call.

**Business rules live in the service layer.** A `PUT` may need to check that `parent_id` exists or that the hierarchy would not form a cycle before calling `save`. Those checks are separate repository calls, for example:

```
exists(parentId)   → read lock, then release
save(transaction)  → write lock
```

Another request could change the store between `exists` and `save`. Preventing that kind of race is the service’s responsibility (or a future composite operation), not the repository’s. The repository is deliberately limited to thread-safe persistence; validation stays in the service by design (SRP).
