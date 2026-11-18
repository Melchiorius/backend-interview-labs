# Roadmap подготовки

План — рабочая гипотеза, а не жёсткая программа. Приоритеты можно менять по результатам разбора тем и пробных собеседований.

## Track A — многопоточность и конкурентность

Высокий приоритет: нужно перейти от знания названий механизмов к пониманию их семантики и практического применения.

1. Модель памяти Java: happens-before, visibility, ordering, safe publication.
2. Мониторы и intrinsic locks: synchronized, reentrancy, wait/notify.
3. Lock API: ReentrantLock, Condition, fairness, interruptible и timed acquisition.
4. volatile: гарантии видимости и упорядочивания, отсутствие составной атомарности.
5. Атомики и CAS: AtomicInteger, compareAndSet, ABA, contention, LongAdder.
6. Гонки данных, deadlock, livelock, starvation, lock convoy.
7. Thread pools: жизненный цикл, очередь, размеры, rejection policies, backpressure.
8. Concurrent collections: ConcurrentHashMap, блокировки и атомарные операции на ключе.
9. Interrupt и cancellation: кооперативная остановка, корректное завершение задач.
10. Диагностика: thread dump, deadlock detection, мониторинг пула.

## Track B — System Design

Высокий приоритет. Изучать параллельно с Track A, начиная с фундаментальных принципов.

1. Требования: функциональные, нефункциональные, ограничения и допущения.
2. Нагрузка: RPS, latency, throughput, concurrency, capacity estimation.
3. API design, идемпотентность, пагинация, версии контрактов.
4. Монолит, модульный монолит, микросервисы и границы декомпозиции.
5. Синхронное и асинхронное взаимодействие.
6. Кэширование, инвалидация, TTL и cache stampede.
7. Хранение данных: SQL/NoSQL, индексы, транзакции, репликация, шардинг.
8. Очереди и доставка сообщений: at-most-once, at-least-once, exactly-once как системное свойство.
9. Надёжность: retries, timeouts, circuit breaker, bulkhead, graceful degradation.
10. Согласованность, распределённые блокировки и конкурирующие обновления.
11. Наблюдаемость: logs, metrics, traces, SLI/SLO.
12. Разбор типовых систем от требований до компромиссов.

## Track C — JVM и Core Java

1. Heap, stack, metaspace, object layout и жизненный цикл объектов.
2. Class loading, linking, initialization и ClassLoader.
3. JIT, интерпретация, оптимизации и deoptimization.
4. Garbage Collection: поколения, паузы, throughput и latency.
5. Утечки памяти и диагностика OutOfMemoryError.
6. Исключения, generics, equals/hashCode и контракт API.

## Track D — асинхронное программирование

1. Executor и жизненный цикл задач.
2. Future и CompletableFuture.
3. thenApply, thenCompose, thenCombine и обработка исключений.
4. Таймауты, отмена, прерывание и распространение ошибок.
5. Ограничение параллелизма и композиция асинхронных операций.

## Track E — коллекции и структуры данных

Базовые знания уже есть; задача — систематизировать внутреннее устройство и закрыть уточняющие вопросы.

1. HashMap: hash, buckets, collisions, resize, tree bins.
2. equals/hashCode и неизменяемость ключей.
3. ArrayList, LinkedList, Queue, Deque.
4. ConcurrentHashMap и атомарные операции.
5. Итераторы, fail-fast и weakly consistent semantics.
6. Сложность операций и реальные компромиссы.

## Track F — backend-практика

Подключать после формирования прочной базы, а не ради перечисления технологий.

- Spring Core и Spring Boot: IoC, DI, lifecycle, proxies, transactions.
- JDBC, connection pools, транзакции и уровни изоляции.
- SQL, индексы и анализ планов запросов.
- HTTP, REST, сетевые таймауты и повторные запросы.
- Kafka или другой брокер — после понимания семантики доставки.
- Тестирование, контейнеризация и эксплуатация.

## Первый учебный цикл

1. Java Memory Model и happens-before.
2. synchronized и мониторы.
3. volatile против synchronized и atomic classes.
4. ReentrantLock и Condition.
5. Thread pools и отказные политики.
6. Практические сценарии: общий счётчик, резервирование ресурса, ограниченный пул потребителей.

Для каждой темы сначала создаётся связный конспект, затем разобранный код и тесты, после чего — вопросы на углубление.

## Критерий завершения темы

Тема не считается освоенной только потому, что пользователь может дать определение. Нужно уметь:
- объяснить механизм без подмены причин следствиями;
- назвать гарантии и ограничения;
- разобрать код и предсказать допустимые исходы;
- выбрать механизм под конкретную задачу и обосновать выбор;
- объяснить цену решения и возможные альтернативы.
