# Каталог базы знаний Java Backend

Статус: карта знаний, не маршрут обучения; отсутствие файла статьи/примера обозначает отсутствие реализации, а не отсутствие темы. Базовая Java — 21. Темы могут пересекаться: категории — карта, не строгая последовательность. **Читатель сам выбирает очередность и глубину прохождения.** Для фактического состояния см. [COVERAGE.md](COVERAGE.md).

## 01 — Java Core / Language & Standard APIs

Развёрнутый перечень **42 возможных самостоятельных статей**, конкретные примеры, направления углубления и связи с другими разделами: [JAVA_CORE_PLAN.md](JAVA_CORE_PLAN.md). Ниже — **карта групп**, а не список готовых файлов и не обещание создавать ровно 42 Markdown-файла: короткие близкие вопросы можно объяснить внутри одной содержательной статьи.

- **Язык и значения:** **[CORE-SYNTAX](../knowledge/01-java-core/syntax.md)**, **[CORE-TYPES](../knowledge/01-java-core/types.md)**, **[CORE-OPERATORS](../knowledge/01-java-core/operators.md)**, **[CORE-CONVERSIONS](../knowledge/01-java-core/conversions.md)**, **[CORE-PARAMS](../knowledge/01-java-core/parameters.md)**, **[CORE-CONTROL](../knowledge/01-java-core/control-flow.md)**.

По [принятому G1-плану SY/OP](plans/CORE-SYNTAX-OPERATORS.md) через PR #156 опубликованы две самостоятельные статьи [CORE-SYNTAX](../knowledge/01-java-core/syntax.md) и [CORE-OPERATORS](../knowledge/01-java-core/operators.md), а также [общий Java 21 runnable](../examples/core/syntax-operators/README.md) с четырьмя источниками: [SyntaxProbe.java](../examples/core/syntax-operators/src/SyntaxProbe.java), [OperatorsProbe.java](../examples/core/syntax-operators/src/OperatorsProbe.java), [p/Helper.java](../examples/core/syntax-operators/src/p/Helper.java) и [demo/PackageProbe.java](../examples/core/syntax-operators/src/demo/PackageProbe.java). Три эталона — [syntax.expected.stdout](../examples/core/syntax-operators/syntax.expected.stdout), [operators.expected.stdout](../examples/core/syntax-operators/operators.expected.stdout) и [package.expected.stdout](../examples/core/syntax-operators/package.expected.stdout); [verify.sh](../examples/core/syntax-operators/verify.sh). **14+27+3 подписанные stdout-строки трёх JVM-процессов, не 44 JUnit-теста**. После G5 #156 были опубликованы **29 самостоятельных статей / 30 из 42 curricular ID**, а **CORE-ASSERT и CORE-OPTIONAL впоследствии опубликованы отдельным G5 #164** (см. ссылочную секцию ниже). Неопубликованные ID, P2/GUI и иные среды не повышены автоматически.

По [принятому G1-плану EC](plans/CORE-ENUMS-CONTROL.md) и опубликованному PR #148 доступны самостоятельные [CORE-ENUMS](../knowledge/01-java-core/enums.md) и [CORE-CONTROL](../knowledge/01-java-core/control-flow.md). Их [общий Java 21 runnable](../examples/core/enums-control/README.md) содержит [EnumProbe.java](../examples/core/enums-control/src/EnumProbe.java) (E01–E30) и [ControlProbe.java](../examples/core/enums-control/src/ControlProbe.java) (C01–C21), два эталона [enum.expected.stdout](../examples/core/enums-control/enum.expected.stdout) и [control.expected.stdout](../examples/core/enums-control/control.expected.stdout), [verify.sh](../examples/core/enums-control/verify.sh). **30+21 строка stdout — два отдельных JVM-процесса, не 51 JUnit-тест**. После G5 #148 были опубликованы **27 самостоятельных Java Core статей / 28 из 42 ID**; **CORE-SYNTAX и CORE-OPERATORS впоследствии опубликованы отдельно в PR #156** (ссылки выше), а CORE-ASSERT/CORE-OPTIONAL отдельно опубликованы через G5 #164; P2 по-прежнему не повышаются автоматически до Verified.
- **Объекты и классы:** **[CORE-CLASSES](../knowledge/01-java-core/classes.md)**, **[CORE-ACCESS](../knowledge/01-java-core/access.md)**, **[CORE-OBJECT](../knowledge/01-java-core/object.md)**, **[CORE-INHERITANCE](../knowledge/01-java-core/inheritance.md)**, **[CORE-INTERFACES](../knowledge/01-java-core/interfaces.md)**, **[CORE-COMPOSITION](../knowledge/01-java-core/composition.md)**, **[CORE-NESTED](../knowledge/01-java-core/nested-classes.md)**, **[CORE-INIT](../knowledge/01-java-core/initialization.md)**.

Для [CORE-INTERFACES](../knowledge/01-java-core/interfaces.md) и [CORE-COMPOSITION](../knowledge/01-java-core/composition.md) опубликованы самостоятельные статьи и [общий Java 21 runnable](../examples/core/interfaces-composition/README.md) через PR #75; [G1-план IC](plans/CORE-INTERFACES-COMPOSITION.md) сохранён. Для CORE-NESTED/FUNCTIONS опубликованы [вложенные классы](../knowledge/01-java-core/nested-classes.md), [функциональные выражения](../knowledge/01-java-core/functions.md) и [общий runnable](../examples/core/nested-functions/README.md) через PR #87; [G1-план NF](plans/CORE-NESTED-FUNCTIONS.md) сохранён. По принятому [G1-плану GE](plans/CORE-GENERICS-ERASURE.md) через PR #92 опубликованы [CORE-GENERICS](../knowledge/01-java-core/generics.md), [CORE-ERASURE](../knowledge/01-java-core/erasure.md) и [runnable (18 строк)](../examples/core/generics-erasure/README.md). По принятому [G1-плану ER](plans/CORE-EXCEPTIONS-RESOURCES.md) через PR #105 опубликованы [CORE-EXCEPTIONS](../knowledge/01-java-core/exceptions.md), [CORE-RESOURCES](../knowledge/01-java-core/resources.md) и [runnable (20 строк)](../examples/core/exceptions-resources/README.md). Публикации не означают, что соседние разделы Spring/JDBC/JVM уже реализованы. По [принятому G1-плану ST](plans/CORE-STREAMS-ADV.md) через PR #116 опубликованы [CORE-STREAMS](../knowledge/01-java-core/streams.md), [CORE-STREAM-ADV](../knowledge/01-java-core/stream-advanced.md) и [общий Java 21 runnable с двумя процессами и 44 строками stdout](../examples/core/streams/README.md). Параллелизм, порядок и типы исключений описаны в рамках Java SE 21; GUI/бенчмарки и P2 не утверждаются проверенными.
- **Строки, массивы и объекты-значения:** **[CORE-ARRAYS](../knowledge/01-java-core/arrays.md)**, **[CORE-STRINGS](../knowledge/01-java-core/strings.md)**, **[CORE-BUILDERS (часть CORE-STRINGS)](../knowledge/01-java-core/strings.md)**, **[CORE-ENUMS](../knowledge/01-java-core/enums.md)**, **[CORE-RECORDS](../knowledge/01-java-core/records.md)**, **[CORE-IMMUTABLE](../knowledge/01-java-core/immutability.md)**.

По [принятому G1-плану R/I](plans/CORE-RECORDS-IMMUTABLE.md) и опубликованному PR #132 подготовлены две самостоятельные статьи: [CORE-RECORDS](../knowledge/01-java-core/records.md) и [CORE-IMMUTABLE](../knowledge/01-java-core/immutability.md). [Общий Java 21 runnable](../examples/core/records-immutability/README.md) запускает две программы и сверяет два эталона (**16+10 подписанных строк stdout, не 26 JUnit-тестов**). `record` сам по себе не гарантирует глубокую неизменяемость; real-world JMM/ORM/serialization и benchmark остаются границами P2.

По [принятому G1-плану AR/TX/BL](plans/CORE-ARRAYS-STRINGS.md) и интегрированному PR #124 опубликованы **две**, а не три статьи: [CORE-ARRAYS](../knowledge/01-java-core/arrays.md) и [CORE-STRINGS с самостоятельным разделом CORE-BUILDERS](../knowledge/01-java-core/strings.md). [Один общий Java 21 runnable](../examples/core/arrays-strings/README.md) содержит два источника, два эталона (**24+28 stdout-строк двух процессов**) и один проверочный скрипт. Три curricular ID раскрываются в двух файлах; P2, реальные Unicode/I18N и все возможные режимы JVM не объявляются проверенными.
- **Контракты и система типов:** **[CORE-EQUALITY](../knowledge/01-java-core/equality.md)**, **[CORE-GENERICS](../knowledge/01-java-core/generics.md)**, **[CORE-ERASURE](../knowledge/01-java-core/erasure.md)**, **[CORE-SEALED](../knowledge/01-java-core/sealed.md)**, **[CORE-PATTERNS](../knowledge/01-java-core/patterns.md)**.

По [принятому G1-плану SP](plans/CORE-SEALED-PATTERNS.md) через PR #140 опубликованы две самостоятельные статьи [CORE-SEALED](../knowledge/01-java-core/sealed.md) и [CORE-PATTERNS](../knowledge/01-java-core/patterns.md) и [общий Java 21 runnable](../examples/core/sealed-patterns/README.md) с двумя программами, отдельными эталонами и **11+20 подписанными строками stdout двух процессов, не 31 JUnit-тестом**. CORE-ENUMS впоследствии опубликован отдельно в PR #148 (см. ссылки выше); P2 про JPMS runtime, binary evolution и производительность не считаются выполненными.
- **Ошибки и ресурсы:** **[CORE-EXCEPTIONS](../knowledge/01-java-core/exceptions.md)**, **[CORE-RESOURCES](../knowledge/01-java-core/resources.md)**, **[CORE-ASSERT](../knowledge/01-java-core/assertions.md)**, **[CORE-OPTIONAL](../knowledge/01-java-core/optional.md)**.

По [принятому G1-плану AS/OT](plans/CORE-ASSERT-OPTIONAL.md) после G5 PR #164 опубликованы две самостоятельные статьи [CORE-ASSERT](../knowledge/01-java-core/assertions.md) и [CORE-OPTIONAL](../knowledge/01-java-core/optional.md) и [общий Java 21 runnable](../examples/core/assert-optional/README.md) с двумя исходниками [AssertProbe.java](../examples/core/assert-optional/src/AssertProbe.java) и [OptionalProbe.java](../examples/core/assert-optional/src/OptionalProbe.java), тремя эталонами [AssertProbe.default.expected.stdout](../examples/core/assert-optional/AssertProbe.default.expected.stdout), [AssertProbe.ea.expected.stdout](../examples/core/assert-optional/AssertProbe.ea.expected.stdout), [OptionalProbe.expected.stdout](../examples/core/assert-optional/OptionalProbe.expected.stdout) и [verify.sh](../examples/core/assert-optional/verify.sh). **15 (default) + 15 (`-ea`) + 36 (Optional) = 66 подписанных stdout-строк трёх JVM-процессов, не 66 JUnit-тестов**. После PR #164 опубликована **31 самостоятельная Java Core статья / 32 из 42 curricular ID с адресным Implemented-содержанием / 10 ID без Implemented**. CORE-ANNOTATIONS/REFLECTION/CLASSLOADER/MODULES и P2 соседних тем не повышаются автоматически.
- **Функциональный стиль:** **[CORE-FUNCTIONS](../knowledge/01-java-core/functions.md)**, **[CORE-STREAMS](../knowledge/01-java-core/streams.md)**, **[CORE-STREAM-ADV](../knowledge/01-java-core/stream-advanced.md)**.
- **Стандартные API:** **[CORE-NUMBERS](../knowledge/01-java-core/numbers.md)**, **[CORE-DATETIME](../knowledge/01-java-core/datetime.md)**, **[CORE-REGEX](../knowledge/01-java-core/regex.md)**, **[CORE-UTIL](../knowledge/01-java-core/utilities.md)**, **[CORE-I18N](../knowledge/01-java-core/internationalization.md)**.
- **Метаданные и динамика:** **[CORE-ANNOTATIONS](../knowledge/01-java-core/annotations.md)**, **[CORE-REFLECTION](../knowledge/01-java-core/reflection.md)**, **[CORE-MODULES](../knowledge/01-java-core/modules.md)**, **[CORE-SERIAL](../knowledge/01-java-core/serialization.md)**, **[CORE-CLASSLOADER](../knowledge/01-java-core/classloaders.md)**.

По [принятому G1-плану AN/RF](plans/CORE-ANNOTATIONS-REFLECTION.md) и опубликованному G5 [PR #172](https://github.com/Melchiorius/backend-interview-labs/pull/172) доступны две самостоятельные статьи [CORE-ANNOTATIONS](../knowledge/01-java-core/annotations.md) и [CORE-REFLECTION](../knowledge/01-java-core/reflection.md) и [общий Java 21 runnable](../examples/core/annotations-reflection/README.md) с двумя plain Java sources, восемью физическими JPMS Java source (включая четыре настоящих `module-info.java`), четырьмя stdout fixtures и [verify.sh](../examples/core/annotations-reflection/verify.sh). **3 отдельные компиляции, 4 JVM (2 classpath + 2 module-path), 4 полных stdout-сравнения; 23+34+3+3=63 строки, не 63 JUnit-теста.** После G5 #172 фактически опубликованы **33 самостоятельные Java Core статьи / 34 из 42 curricular ID с адресным Implemented-содержанием / 8 ID без Implemented**. На момент G5 #172 CORE-MODULES, CORE-CLASSLOADER, CORE-SERIAL и пять Standard API ID ещё оставались без Implemented; исследовательский [G1 PR #176](https://github.com/Melchiorius/backend-interview-labs/pull/176) не является публикацией этих статей. Статус файлов не доказывает полное P2/интервью-покрытие, другие JPMS графы или проверку Obsidian/IDE.

По [принятому G1 JM/CL](plans/CORE-MODULES-CLASSLOADER.md) и опубликованному G5 [PR #179](https://github.com/Melchiorius/backend-interview-labs/pull/179) теперь существуют две самостоятельные статьи [CORE-MODULES](../knowledge/01-java-core/modules.md) и [CORE-CLASSLOADER](../knowledge/01-java-core/classloaders.md), а также [общий Java21/JPMS runnable](../examples/core/modules-classloader/README.md) с **24 Java sources (6 classpath + 7+7 named-module + 4 `requires static`), шестью stdout fixtures** и [verify.sh](../examples/core/modules-classloader/verify.sh). **4 независимые clean-компиляции, 6 JVM (4 module-path + 2 classpath), 6 `diff -u`, 11+11+2+2+21+2=49 подписанных stdout-строк, не 49 JUnit-тестов.** После G5 #179 **35 самостоятельных опубликованных Java Core статей / 36 из 42 curricular ID с адресным Implemented-содержанием / 6 ID без Implemented**: CORE-NUMBERS, CORE-DATETIME, CORE-REGEX, CORE-UTIL, CORE-I18N, CORE-SERIAL. Наличие статей/runner не повышает P2 и не доказывает ServiceLoader discovery, dynamic ModuleLayer или все варианты ClassLoader/JVM; отрицательные 17 javac-кейсов проверены 04 отдельно и не входят в штатный CI.

По принятому [G1-плану NM/DT](plans/CORE-NUMBERS-DATETIME.md) и опубликованному G5 [PR #188](https://github.com/Melchiorius/backend-interview-labs/pull/188) теперь доступны две самостоятельные статьи [CORE-NUMBERS](../knowledge/01-java-core/numbers.md) и [CORE-DATETIME](../knowledge/01-java-core/datetime.md) и [общий Java21 runnable](../examples/core/numbers-datetime/README.md) с [NumbersProbe.java](../examples/core/numbers-datetime/src/NumbersProbe.java), [DateTimeProbe.java](../examples/core/numbers-datetime/src/DateTimeProbe.java), двумя [Numbers stdout](../examples/core/numbers-datetime/numbers.expected.stdout) и [DateTime stdout](../examples/core/numbers-datetime/datetime.expected.stdout) fixtures, [verify.sh](../examples/core/numbers-datetime/verify.sh). **1 clean компиляция, 2 JVM, 2 `diff -u`, 33+38=71 stdout-строка, не 71 JUnit-тест.** После G5 #188 фактический `main` содержит **37 опубликованных самостоятельных статей / 38 из 42 curricular ID Implemented / 4 ID без Implemented**: CORE-REGEX, CORE-UTIL, CORE-I18N и CORE-SERIAL. Для NM/DT матрица **42 P0 / 11 P1 / 8 P2**, отрицательные 8 javac-кейсов проверены 04 отдельно от CI; иные версии tzdb/JDK, финансовые политики и P2 не становятся Verified по самому факту публикации.

После принятого [G1 REGEX/UTIL](plans/CORE-REGEX-UTIL.md), [G3 PASS 05](https://github.com/Melchiorius/backend-interview-labs/pull/197#pullrequestreview-6021127097), [G4 PASS 04](https://github.com/Melchiorius/backend-interview-labs/pull/197#pullrequestreview-6021541068) и G5 [PR #197](https://github.com/Melchiorius/backend-interview-labs/pull/197) опубликованы самостоятельные статьи [CORE-REGEX](../knowledge/01-java-core/regex.md) и [CORE-UTIL](../knowledge/01-java-core/utilities.md), [общий Java21 runner](../examples/core/regex-util/README.md) с [RegexProbe.java](../examples/core/regex-util/src/RegexProbe.java), [UtilProbe.java](../examples/core/regex-util/src/UtilProbe.java), двумя stdout-эталонами [Regex](../examples/core/regex-util/regex.expected.stdout) и [Util](../examples/core/regex-util/util.expected.stdout), [verify.sh](../examples/core/regex-util/verify.sh). **1 clean javac, 2 JVM, 2 stdout diff, 33+38=71 stdout-строка, не 71 JUnit-тест.** Текущий `main` после G5 #197 имеет **39 самостоятельных Java Core статей / 40 из 42 curricular ID Implemented / 2 без Implemented: CORE-I18N и CORE-SERIAL**. Матрица G1 60 P0/13 P1/9 P2; 14 isolated compiler-negative проверены 04 отдельно вне positive CI, ReDoS worst-case/timeout и P2 не подтверждены публикацией.

По [принятому G1 I18N/SERIAL](plans/CORE-I18N-SERIAL.md), [G3 PASS](https://github.com/Melchiorius/backend-interview-labs/pull/205#pullrequestreview-6024210263), [G4 PASS](https://github.com/Melchiorius/backend-interview-labs/pull/205#pullrequestreview-6026683840) и опубликованному G5 [PR #205](https://github.com/Melchiorius/backend-interview-labs/pull/205) появились самостоятельные статьи [CORE-I18N](../knowledge/01-java-core/internationalization.md), [CORE-SERIAL](../knowledge/01-java-core/serialization.md) и [общий Java21 trusted-only runnable](../examples/core/i18n-serialization/README.md) с [I18nProbe.java](../examples/core/i18n-serialization/src/I18nProbe.java), [SerializationProbe.java](../examples/core/i18n-serialization/src/SerializationProbe.java), эталонами [i18n stdout](../examples/core/i18n-serialization/i18n.expected.stdout) и [serial stdout](../examples/core/i18n-serialization/serial.expected.stdout), [verify.sh](../examples/core/i18n-serialization/verify.sh). **1 clean javac, 2 JVM, 2 exact stdout diff, 29+20=49 stdout-строк, не 49 JUnit-тестов.** После G5 #205 текущий `main` имеет **41 самостоятельную Java Core статью / 42 из 42 curricular ID Implemented / 0 без Implemented**; `CORE-BUILDERS` остаётся внутри CORE-STRINGS. G1 — **77 ID = 54 P0 / 14 P1 / 9 P2**, P2 не автоматически Verified; 12 compiler-negative и 2 `@Serial` warnings проверены 04 вне CI. Сериализация проверяется только на локально созданных доверенных bytes с `ObjectInputFilter`, `UNDECIDED` не равнозначен отказу/allowlist и не даёт доказательства безопасности недоверенных данных. **Следующий обязательный этап — финальный содержательный аудит Java Core по [Issue #33](https://github.com/Melchiorius/backend-interview-labs/issues/33), не автоматическое закрытие программы из-за счётчика 42/42.**

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
