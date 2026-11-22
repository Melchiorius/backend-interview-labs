# Каталог базы знаний Java Backend

Статус: карта знаний, не маршрут обучения; отсутствие файла статьи/примера обозначает отсутствие реализации, а не отсутствие темы. Базовая Java — 21. Темы могут пересекаться: категории — карта, не строгая последовательность. **Читатель сам выбирает очередность и глубину прохождения.** Для фактического состояния см. [COVERAGE.md](COVERAGE.md).

## 01 — Java Core / Language & Standard APIs

Развёрнутый перечень **42 возможных самостоятельных статей**, конкретные примеры, направления углубления и связи с другими разделами: [JAVA_CORE_PLAN.md](JAVA_CORE_PLAN.md). Ниже — **карта групп**, а не список готовых файлов и не обещание создавать ровно 42 Markdown-файла: короткие близкие вопросы можно объяснить внутри одной содержательной статьи.

- **Язык и значения:** **[CORE-SYNTAX](../knowledge/01-java-core/syntax.md)**, **[CORE-TYPES](../knowledge/01-java-core/types.md)**, **[CORE-OPERATORS](../knowledge/01-java-core/operators.md)**, **[CORE-CONVERSIONS](../knowledge/01-java-core/conversions.md)**, **[CORE-PARAMS](../knowledge/01-java-core/parameters.md)**, **[CORE-CONTROL](../knowledge/01-java-core/control-flow.md)**.

По [принятому G1-плану SY/OP](plans/CORE-SYNTAX-OPERATORS.md) через PR #156 опубликованы две самостоятельные статьи [CORE-SYNTAX](../knowledge/01-java-core/syntax.md) и [CORE-OPERATORS](../knowledge/01-java-core/operators.md), а также [общий Java 21 runnable](../examples/core/syntax-operators/README.md) с четырьмя источниками: [SyntaxProbe.java](../examples/core/syntax-operators/src/SyntaxProbe.java), [OperatorsProbe.java](../examples/core/syntax-operators/src/OperatorsProbe.java), [p/Helper.java](../examples/core/syntax-operators/src/p/Helper.java) и [demo/PackageProbe.java](../examples/core/syntax-operators/src/demo/PackageProbe.java). Три эталона — [syntax.expected.stdout](../examples/core/syntax-operators/syntax.expected.stdout), [operators.expected.stdout](../examples/core/syntax-operators/operators.expected.stdout) и [package.expected.stdout](../examples/core/syntax-operators/package.expected.stdout); [verify.sh](../examples/core/syntax-operators/verify.sh). **14+27+3 подписанные stdout-строки трёх JVM-процессов, не 44 JUnit-теста**. Теперь фактически опубликованы **29 самостоятельных статей / 30 из 42 curricular ID с Implemented-содержанием / 12 ID без Implemented**; CORE-ASSERT/CORE-OPTIONAL и остальные незакрытые ID не повышены, P2/GUI и иные среды не объявлены проверенными.

По [принятому G1-плану EC](plans/CORE-ENUMS-CONTROL.md) и опубликованному PR #148 доступны самостоятельные [CORE-ENUMS](../knowledge/01-java-core/enums.md) и [CORE-CONTROL](../knowledge/01-java-core/control-flow.md). Их [общий Java 21 runnable](../examples/core/enums-control/README.md) содержит [EnumProbe.java](../examples/core/enums-control/src/EnumProbe.java) (E01–E30) и [ControlProbe.java](../examples/core/enums-control/src/ControlProbe.java) (C01–C21), два эталона [enum.expected.stdout](../examples/core/enums-control/enum.expected.stdout) и [control.expected.stdout](../examples/core/enums-control/control.expected.stdout), [verify.sh](../examples/core/enums-control/verify.sh). **30+21 строка stdout — два отдельных JVM-процесса, не 51 JUnit-тест**. После G5 #148 были опубликованы **27 самостоятельных Java Core статей / 28 из 42 ID**; **CORE-SYNTAX и CORE-OPERATORS впоследствии опубликованы отдельно в PR #156** (ссылки выше), тогда как CORE-ASSERT/CORE-OPTIONAL и P2 не повышаются автоматически до Implemented/Verified.
- **Объекты и классы:** **[CORE-CLASSES](../knowledge/01-java-core/classes.md)**, **[CORE-ACCESS](../knowledge/01-java-core/access.md)**, **[CORE-OBJECT](../knowledge/01-java-core/object.md)**, **[CORE-INHERITANCE](../knowledge/01-java-core/inheritance.md)**, **[CORE-INTERFACES](../knowledge/01-java-core/interfaces.md)**, **[CORE-COMPOSITION](../knowledge/01-java-core/composition.md)**, **[CORE-NESTED](../knowledge/01-java-core/nested-classes.md)**, **[CORE-INIT](../knowledge/01-java-core/initialization.md)**.

Для [CORE-INTERFACES](../knowledge/01-java-core/interfaces.md) и [CORE-COMPOSITION](../knowledge/01-java-core/composition.md) опубликованы самостоятельные статьи и [общий Java 21 runnable](../examples/core/interfaces-composition/README.md) через PR #75; [G1-план IC](plans/CORE-INTERFACES-COMPOSITION.md) сохранён. Для CORE-NESTED/FUNCTIONS опубликованы [вложенные классы](../knowledge/01-java-core/nested-classes.md), [функциональные выражения](../knowledge/01-java-core/functions.md) и [общий runnable](../examples/core/nested-functions/README.md) через PR #87; [G1-план NF](plans/CORE-NESTED-FUNCTIONS.md) сохранён. По принятому [G1-плану GE](plans/CORE-GENERICS-ERASURE.md) через PR #92 опубликованы [CORE-GENERICS](../knowledge/01-java-core/generics.md), [CORE-ERASURE](../knowledge/01-java-core/erasure.md) и [runnable (18 строк)](../examples/core/generics-erasure/README.md). По принятому [G1-плану ER](plans/CORE-EXCEPTIONS-RESOURCES.md) через PR #105 опубликованы [CORE-EXCEPTIONS](../knowledge/01-java-core/exceptions.md), [CORE-RESOURCES](../knowledge/01-java-core/resources.md) и [runnable (20 строк)](../examples/core/exceptions-resources/README.md). Публикации не означают, что соседние разделы Spring/JDBC/JVM уже реализованы. По [принятому G1-плану ST](plans/CORE-STREAMS-ADV.md) через PR #116 опубликованы [CORE-STREAMS](../knowledge/01-java-core/streams.md), [CORE-STREAM-ADV](../knowledge/01-java-core/stream-advanced.md) и [общий Java 21 runnable с двумя процессами и 44 строками stdout](../examples/core/streams/README.md). Параллелизм, порядок и типы исключений описаны в рамках Java SE 21; GUI/бенчмарки и P2 не утверждаются проверенными.
- **Строки, массивы и объекты-значения:** **[CORE-ARRAYS](../knowledge/01-java-core/arrays.md)**, **[CORE-STRINGS](../knowledge/01-java-core/strings.md)**, **[CORE-BUILDERS (часть CORE-STRINGS)](../knowledge/01-java-core/strings.md)**, **[CORE-ENUMS](../knowledge/01-java-core/enums.md)**, **[CORE-RECORDS](../knowledge/01-java-core/records.md)**, **[CORE-IMMUTABLE](../knowledge/01-java-core/immutability.md)**.

По [принятому G1-плану R/I](plans/CORE-RECORDS-IMMUTABLE.md) и опубликованному PR #132 подготовлены две самостоятельные статьи: [CORE-RECORDS](../knowledge/01-java-core/records.md) и [CORE-IMMUTABLE](../knowledge/01-java-core/immutability.md). [Общий Java 21 runnable](../examples/core/records-immutability/README.md) запускает две программы и сверяет два эталона (**16+10 подписанных строк stdout, не 26 JUnit-тестов**). `record` сам по себе не гарантирует глубокую неизменяемость; real-world JMM/ORM/serialization и benchmark остаются границами P2.

По [принятому G1-плану AR/TX/BL](plans/CORE-ARRAYS-STRINGS.md) и интегрированному PR #124 опубликованы **две**, а не три статьи: [CORE-ARRAYS](../knowledge/01-java-core/arrays.md) и [CORE-STRINGS с самостоятельным разделом CORE-BUILDERS](../knowledge/01-java-core/strings.md). [Один общий Java 21 runnable](../examples/core/arrays-strings/README.md) содержит два источника, два эталона (**24+28 stdout-строк двух процессов**) и один проверочный скрипт. Три curricular ID раскрываются в двух файлах; P2, реальные Unicode/I18N и все возможные режимы JVM не объявляются проверенными.
- **Контракты и система типов:** **[CORE-EQUALITY](../knowledge/01-java-core/equality.md)**, **[CORE-GENERICS](../knowledge/01-java-core/generics.md)**, **[CORE-ERASURE](../knowledge/01-java-core/erasure.md)**, **[CORE-SEALED](../knowledge/01-java-core/sealed.md)**, **[CORE-PATTERNS](../knowledge/01-java-core/patterns.md)**.

По [принятому G1-плану SP](plans/CORE-SEALED-PATTERNS.md) через PR #140 опубликованы две самостоятельные статьи [CORE-SEALED](../knowledge/01-java-core/sealed.md) и [CORE-PATTERNS](../knowledge/01-java-core/patterns.md) и [общий Java 21 runnable](../examples/core/sealed-patterns/README.md) с двумя программами, отдельными эталонами и **11+20 подписанными строками stdout двух процессов, не 31 JUnit-тестом**. CORE-ENUMS впоследствии опубликован отдельно в PR #148 (см. ссылки выше); P2 про JPMS runtime, binary evolution и производительность не считаются выполненными.
- **Ошибки и ресурсы:** **[CORE-EXCEPTIONS](../knowledge/01-java-core/exceptions.md)**, **[CORE-RESOURCES](../knowledge/01-java-core/resources.md)**, `CORE-ASSERT`, `CORE-OPTIONAL`.
- **Функциональный стиль:** **[CORE-FUNCTIONS](../knowledge/01-java-core/functions.md)**, **[CORE-STREAMS](../knowledge/01-java-core/streams.md)**, **[CORE-STREAM-ADV](../knowledge/01-java-core/stream-advanced.md)**.
- **Стандартные API:** `CORE-NUMBERS`, `CORE-DATETIME`, `CORE-REGEX`, `CORE-UTIL`, `CORE-I18N`.
- **Метаданные и динамика:** `CORE-ANNOTATIONS`, `CORE-REFLECTION`, `CORE-MODULES`, `CORE-SERIAL`, `CORE-CLASSLOADER`.

Прежние восемь крупных пунктов (`CORE-TYPES`, `CORE-OOP`, `CORE-CONTRACTS`, `CORE-GENERICS`, `CORE-ERRORS`, `CORE-FUNCTIONAL`, `CORE-MODERN`, `CORE-REFLECTION`) были **рубриками**. Старые ID `CORE-OOP`, `CORE-CONTRACTS`, `CORE-ERRORS`, `CORE-FUNCTIONAL`, `CORE-MODERN` больше не назначаются отдельным статьям: их содержание распределено по новым точным ID. `CORE-TYPES`, `CORE-GENERICS` и `CORE-REFLECTION` сохранены. При работе с ранее созданными ссылками учитывать эту миграцию; не создавать пустые файлы-«алиасы».

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
- **[CON-SEM: Semaphore](../knowledge/03-concurrency/semaphore.md)**, permits, binary/counting, fairness, interrupt, leaks, mutex comparison.
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
