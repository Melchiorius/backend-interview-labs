# Каталог базы знаний Java Backend

Статус: карта знаний, не маршрут обучения; отсутствие файла статьи/примера обозначает отсутствие реализации, а не отсутствие темы. Базовая Java — 21. Темы могут пересекаться: категории — карта, не строгая последовательность. **Читатель сам выбирает очередность и глубину прохождения.** Для фактического состояния см. [COVERAGE.md](COVERAGE.md).

## 01 — Java Language Fundamentals
- CORE-TYPES: primitive/reference, assignment, conversions, boxing/unboxing, parameter passing.
- CORE-OOP: class/interface, inheritance, overriding/overloading, polymorphism, dynamic dispatch.
- CORE-CONTRACTS: equals/hashCode/compareTo, immutable objects, defensive copies.
- CORE-GENERICS: generic types, invariant types, PECS/wildcards, erasure, heap pollution.
- CORE-ERRORS: checked/unchecked, propagation, try-with-resources, suppressed exceptions.
- CORE-FUNCTIONAL: lambdas, method references, Optional, Stream API, laziness and side effects.
- CORE-MODERN: records, sealed types, pattern matching, switch expressions.
- CORE-REFLECTION: annotations, reflection, module access.

## 02 — Collections & Data Structures
- COL-CONTRACTS: Collection/List/Set/Map/Queue/Deque, invariants and equality.
- COL-SEQUENTIAL: arrays, ArrayList, LinkedList, ArrayDeque.
- COL-HASH: HashMap, HashSet, buckets, collisions, resize, tree bins.
- COL-SORTED: Comparable/Comparator, TreeMap/TreeSet.
- COL-HEAP: PriorityQueue, heap, complexity.
- COL-ITER: iterators, fail-fast vs weak consistency.
- COL-CONCURRENT: ConcurrentHashMap, concurrent queues, atomic per-key updates.
- COL-TRADEOFFS: complexity, memory locality, benchmarking caveats.

## 03 — Concurrency & Java Memory Model
- CON-THREAD: Thread lifecycle, start/run/join, interrupt, cancellation, shutdown.
- CON-RACE: shared mutable state, race conditions, data race, atomicity, visibility, ordering.
- CON-JMM: happens-before, synchronizes-with, publication, final fields, reordering.
- CON-MONITOR: synchronized, reentrancy, monitor ownership, wait/notify/notifyAll, condition loops.
- CON-VOLATILE: visibility/order guarantees, read-modify-write limits.
- CON-LOCK: Lock/ReentrantLock, Condition, fair/timed/interruptible acquisition.
- **CON-SEM: Semaphore**, permits, binary/counting, fairness, interrupt, leaks, mutex comparison.
- **CON-LATCH: CountDownLatch** and one-shot coordination.
- **CON-BARRIER: CyclicBarrier, Phaser**, cyclic phases, abort/failure cases.
- CON-RWLOCK: ReadWriteLock and StampedLock, optimistic reads and caveats.
- CON-ATOM: CAS, AtomicInteger, ABA, LongAdder, contention.
- CON-QUEUE: BlockingQueue, bounded buffers, producer-consumer.
- CON-EXEC: ExecutorService, ThreadPoolExecutor, queues, saturation, rejection, backpressure.
- CON-FORKJOIN: ForkJoinPool, work stealing, parallel streams interaction.
- CON-FUTURE: Future, CompletableFuture composition and failures.
- CON-TLOCAL: ThreadLocal, cleanup and leaks.
- CON-VIRTUAL: virtual threads, pinning/version-specific behavior, structured concurrency status.
- CON-FAILURES: deadlock, starvation, livelock, lock convoy, diagnostics.
- CON-PARK: LockSupport, park/unpark and permit semantics.

## 04 — JVM Internals & Performance
- JVM-BYTECODE: javac, bytecode, verification.
- JVM-LOADING: loaders, delegation, loading/linking/initialization.
- JVM-MEMORY: heap, stacks, metaspace, references, reachability, GC roots.
- JVM-GC: collectors, generational behavior, STW, latency/throughput.
- JVM-JIT: interpreter, tiers, JIT, escape analysis, deoptimization.
- JVM-DIAG: jcmd, JFR, thread/heap dumps, profiling, leaks.
- JVM-MODELS: runtime memory areas vs Java Memory Model.

## 05 — I/O, Networking & Asynchrony
- IO-BYTES: encodings, streams/readers/writers, buffering.
- IO-FILES: Files, channels, ByteBuffer, NIO.
- IO-NET: sockets, TCP, connection lifecycle and timeouts.
- IO-HTTP: HTTP semantics, status, caching, client/server.
- IO-ASYNC: blocking/non-blocking, selectors, async IO and cancellation.
- IO-PRESSURE: buffering, backpressure, resource limits.

## 06 — SQL, Databases & Persistence
- DB-REL: relational model, keys/constraints.
- DB-SQL: joins, grouping, subqueries, windows.
- DB-INDEX: indexes, explain plans, statistics.
- DB-TX: ACID, anomalies, isolation, MVCC, locks, deadlocks.
- DB-JDBC: JDBC, connection pools, resource lifetime.
- DB-ORM: JPA/Hibernate, entity lifecycle, persistence context.
- DB-PERF: lazy loading, N+1, caching, batching.

## 07 — Spring & Application Architecture
- SPR-DI: DI/IoC, bean scopes and lifecycle.
- SPR-BOOT: configuration/auto-configuration.
- SPR-AOP: proxying, interception, self-invocation.
- SPR-WEB: MVC, HTTP, validation, errors.
- SPR-TX: @Transactional, propagation, rollback.
- SPR-SEC: authentication/authorization.
- SPR-TEST: slicing, integration and unit tests.
- SPR-DESIGN: module boundaries, layering, dependency direction.

## 08 — Distributed Systems
- DIST-FAIL: partial failure, timeout/retry/jitter, idempotency.
- DIST-MSG: queues, at-most/at-least once, duplicates, Kafka partitions/offsets/groups.
- DIST-CONS: consistency, outbox, saga, transactions.
- DIST-SCALE: replication, partitioning, caching, capacity.
- DIST-OBS: logs, metrics, traces, SLO.
- DIST-PATTERNS: circuit breaker, bulkhead, rate limiting, graceful degradation.

## 09 — Testing, Tooling & Operations
- TEST-JUNIT: Jupiter, lifecycle, parametrized tests, assertions, assertions of exceptions.
- **TEST-MOCKITO: doubles, @Mock, MockitoExtension, stubbing, verifying behavior, misuse.**
- TEST-DESIGN: determinism, AAA, boundaries, contract/property tests, integration vs unit.
- TEST-CONCURRENT: latches, bounded waits, invariants, repeatability, limits of tests.
- TEST-BUILD: Gradle multi-project, focused test selection, caching, wrapper.
- TEST-IDE: IntelliJ shared .run configs, isolated execution.
- TEST-CI: focused CI jobs, matrix/sharding, all-reference verification.
- TEST-OPS: Git workflow, Docker, diagnostics, observability, recovery.

## 10 — Algorithms & System Design
- ALG-COMPLEXITY: asymptotics, time/space.
- ALG-STRUCT: arrays, lists, maps, heaps, trees, graphs.
- ALG-PATTERNS: binary search, two pointers, sliding window, recursion, DP.
- DESIGN-REQ: functional/nonfunctional requirements, capacity.
- DESIGN-API: contract, pagination, versioning, idempotency.
- DESIGN-ARCH: modular monolith/microservices, boundaries and tradeoffs.
- DESIGN-REL: caching, rate limiting, resilience, scalability.
- DESIGN-REVIEW: scenario analysis, architectural evolution and technical debt.

## Правила глубины
- Уровень указывать **на разделе/навыке внутри темы**, не на всей теме: `M` = обычный рабочий Middle, `M+` = Middle+ в производственных сценариях, `S` = Senior либо объективно специализированное углубление. Это не обязательные экзамены и не кадровая аттестация.
- Не завышать сложность: знакомство с Semaphore, CountDownLatch, JUnit, Mockito, JVM GC, транзакциями и большинством других распространённых API не является само по себе Senior-темой.
- Вносить и редкие механизмы в каталог; их наличие не делает чтение обязательным. Полнота дерева знаний важнее очередности занятий.
- Подробности предметного уровня раскрывать в дочерних подразделах и статьях; новый нюанс может быть добавлен без перестройки всей иерархии.

## Пример справочного графа предпосылок
`CORE-TYPES → CON-RACE → CON-JMM → {CON-MONITOR, CON-VOLATILE, CON-LOCK, CON-SEM} → {CON-QUEUE, CON-EXEC, CON-FUTURE}`.
`CORE-GENERICS → COL-CONTRACTS → COL-CONCURRENT`.
`TEST-JUNIT → TEST-DESIGN → TEST-CONCURRENT`; `TEST-MOCKITO` применяется при наличии внешних зависимостей, не для имитации реальной синхронизации.
`DB-TX → SPR-TX → DIST-CONS`.

## Проверка полноты
Перед публикацией статьи проверить: предпосылки; терминологию; гарантии; ошибочные случаи; альтернативы; код/тесты; ссылки; уровень глубины для отдельных разделов; статус материала в реестре. Вновь выявленный пробел сначала вносится сюда и в реестр, затем планируется реализация.
