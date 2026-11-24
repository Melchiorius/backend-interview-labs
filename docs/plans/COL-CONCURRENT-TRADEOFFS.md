# C4 Collections G1 — COL-CONCURRENT + COL-TRADEOFFS (Java SE 21)

**Статус: G1 PROPOSED**, исследование до G2, не готовые статьи. [Issue #237](https://github.com/Melchiorius/backend-interview-labs/issues/237), программа [#213](https://github.com/Melchiorius/backend-interview-labs/issues/213). Исходный BASE `5dbf84fc8d64c34412c87a04de572f0a3c7488ed`. Java SE 21. P0 — обязательный собственный ответ/контрпример; P1 — важное уточнение или содержательный переход; P2 — узкая дальнейшая тема. Приоритеты не являются измеренной частотой интервью.

## Обещание и границы

COL-CONCURRENT: объяснять гарантии конкретной concurrent collection, атомарный переход одного mapping, границы publication и traversal, выбор очереди и обработку full/interruption; не путать thread-safe container с транзакцией и thread-safe payload. COL-TRADEOFFS: выбрать структуру по контракту и workload, отличить оценку сложности от latency и построить воспроизводимый эксперимент без придуманных результатов. Полные CON-JMM/CON-LOCK/CON-ATOM/CON-POOL/CON-THREAD, executor lifecycle, lock-free proofs, linearizability proof, jcstress и системное нагрузочное тестирование — отдельные будущие ID/работы; здесь только необходимая граница. Не повторять готовые C1–C3.

## Источники A/I/B/C

A — Java SE21 Javadoc:
- [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) (class description, putIfAbsent, replace, remove, computeIfAbsent/Present, compute, merge, keySet, bulk operations), [ConcurrentMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentMap.html), [Map](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html), [Collections](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#synchronizedMap(java.util.Map)).
- [CopyOnWriteArrayList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CopyOnWriteArrayList.html), [ConcurrentLinkedQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentLinkedQueue.html), [ConcurrentLinkedDeque](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentLinkedDeque.html), [java.util.concurrent package](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/package-summary.html), [JLS21 §17.4.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.4.5), [Thread](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html).
- [BlockingQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/BlockingQueue.html), [ArrayBlockingQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ArrayBlockingQueue.html), [LinkedBlockingQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/LinkedBlockingQueue.html), [SynchronousQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/SynchronousQueue.html), [PriorityBlockingQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/PriorityBlockingQueue.html), [Queue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Queue.html), [ConcurrentSkipListMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentSkipListMap.html), [LongAdder](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/atomic/LongAdder.html).
- Tradeoffs use [ArrayList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html), [LinkedList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedList.html), [ArrayDeque](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayDeque.html), [HashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html), [TreeMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html), [TreeSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html), [PriorityQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html), [List](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html). A class-specific complexity notes ≠ universal hard wall-time bounds. Official links are version-specific; primary retrieval checked for CHM/CM/Collections/COW/BQ/ABQ/CLQ/package and collection overview; other named method details remain subject to independent G1 source check, not claimed as individually opened.

I — pinned OpenJDK21 revision `890adb6410dab4606a4f26a942aed02fb2f55387`. Read through GitHub source connector:
- [ConcurrentHashMap.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/concurrent/ConcurrentHashMap.java): casTabAt and synchronized(f) found; inspect putVal/compute/transfer and serialization Segment separately. Empty-bin CAS does not prove lock-free updates overall.
- [CopyOnWriteArrayList.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/concurrent/CopyOnWriteArrayList.java): setArray, copied array publication, snapshot iterator model.
- [ArrayBlockingQueue.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/concurrent/ArrayBlockingQueue.java): ReentrantLock with fair option, conditions; [LinkedBlockingQueue.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/concurrent/LinkedBlockingQueue.java): putLock/takeLock algorithm; [ConcurrentLinkedQueue.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/concurrent/ConcurrentLinkedQueue.java): CAS links/items. These are implementation observations, not requirements for every JDK or performance measurements.

B — [official framework overview](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/doc-files/coll-overview.html), [concurrency package guidance](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/package-summary.html). [OpenJDK JMH](https://github.com/openjdk/jmh), observed source revision `a194eead0136bb66e5e59e4fdb2e18543e730929`. Exact methodology reading anchors: [02 BenchmarkModes](https://github.com/openjdk/jmh/blob/a194eead0136bb66e5e59e4fdb2e18543e730929/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_02_BenchmarkModes.java), [03 States](https://github.com/openjdk/jmh/blob/a194eead0136bb66e5e59e4fdb2e18543e730929/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_03_States.java), [05 StateFixtures](https://github.com/openjdk/jmh/blob/a194eead0136bb66e5e59e4fdb2e18543e730929/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_05_StateFixtures.java), [08 DeadCode](https://github.com/openjdk/jmh/blob/a194eead0136bb66e5e59e4fdb2e18543e730929/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_08_DeadCode.java), [09 Blackholes](https://github.com/openjdk/jmh/blob/a194eead0136bb66e5e59e4fdb2e18543e730929/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_09_Blackholes.java), [10 ConstantFold](https://github.com/openjdk/jmh/blob/a194eead0136bb66e5e59e4fdb2e18543e730929/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_10_ConstantFold.java), [12 Forking](https://github.com/openjdk/jmh/blob/a194eead0136bb66e5e59e4fdb2e18543e730929/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_12_Forking.java), [15 Asymmetric](https://github.com/openjdk/jmh/blob/a194eead0136bb66e5e59e4fdb2e18543e730929/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_15_Asymmetric.java), [27 Params](https://github.com/openjdk/jmh/blob/a194eead0136bb66e5e59e4fdb2e18543e730929/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_27_Params.java), [35 Profilers](https://github.com/openjdk/jmh/blob/a194eead0136bb66e5e59e4fdb2e18543e730929/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_35_Profilers.java). Public README and samples 08/09/10 surfaced during source search; other anchors are follow-up reading/checklist, not claim of executed examples or verified latest release. Choose and record actual JMH dependency version only if runnable benchmark is later approved.

C — from [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), independent question coverage, not guarantees:
- C1 [Baeldung Collections interview](https://www.baeldung.com/java-collections-interview-questions): Hashtable vs ConcurrentHashMap and iterator questions; detected comparison of synchronized Hashtable and CHM. Do not copy simplified claims about blocking every retrieval or fail-fast as universal.
- C2 [teamlead pinned README](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md): located questions on HashTable/ConcurrentHashMap differences and effectiveness, Hashtable problem, CopyOnWriteArrayList features; frequency is author-provided, not validated statistics.
- C3 [in28minutes CollectionHierarchy](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/src/main/java/com/in28minutes/java/collections/CollectionHierarchy.java): located HashMap vs Hashtable question and BlockingQueue/ArrayBlockingQueue/LinkedBlockingQueue declarations. Declarations are topic leads, not evidence of explicit queue interview questions.
- C GAP: no independently checked specific JMH methodology, multi-key atomicity or callback-recursion interview questions in this selection; cover them from A/B and engineering need. Chernyshov specific C4 questions not checked. No fabricated frequency or attribution. Repeat external C sampling against actual G2 articles and record new questions/coverage before publication.

## COL-CONCURRENT — матрица CCN

| ID | P | Вопрос и механизм | Контрпример / критерий ответа | Evidence |
|---|---|---|---|---|
| CCN-01 | P0 | ConcurrentHashMap: контракт ConcurrentMap, отличие от HashMap/Hashtable; null key/value запрещены | get отсутствующего ключа возвращает null; put(null,67) и put("x",null) дают NPE. Отдельно объяснить, что null из callback имеет специальный смысл | A CHM/CM; C1/C2 |
| CCN-02 | P0 | Check-then-act: отдельные get/containsKey/put не образуют атомарную операцию | Два клиента читают 0 и каждый пишет 1: потерянное обновление допустимо; это логическая трасса, не обещание конкретной гонки | A CHM |
| CCN-03 | P0 | putIfAbsent, conditional remove(k,v), replace(k,old,new), replace(k,v); атомарность конкретного перехода | putIfAbsent возвращает старое значение или null; remove с несовпадающим value возвращает false; replace отсутствующего ключа не создаёт mapping | A CM/CHM |
| CCN-04 | P0 | computeIfAbsent: функция вызывается при отсутствии значения, весь вызов атомарен; non-null result устанавливается | computeIfAbsent(k,f) при существующем значении не вызывает f; null result не создаёт запись; RuntimeException оставляет mapping неустановленным | A CHM |
| CCN-05 | P0 | computeIfPresent и compute: функция получает старое значение; compute работает также с отсутствующим ключом (old=null) | computeIfPresent отсутствующего ключа пропускает callback; null result удаляет существующую запись; exception оставляет mapping неизменённым | A CHM |
| CCN-06 | P0 | merge: non-null value, absent → value без callback, present → remapping(old,value); null result удаляет | merge("x",1,Integer::sum) для отсутствующего x даёт 1; повтор даёт 2; merge с null value запрещён | A CHM |
| CCN-07 | P0 | Callback constraints различаются по методам: короткие вычисления, потенциальная блокировка других updates; не менять карту внутри callback вопреки Javadoc конкретного метода | Детектируемый рекурсивный update, который иначе не завершился бы, может дать IllegalStateException; не обещать detection любого цикла; exception не откатывает внешний side effect | A CHM methods |
| CCN-08 | P0 | Per-key atomicity ≠ multi-key transaction; инвариант нескольких записей требует отдельного согласованного протокола для всех участников | Списание с A и зачисление B двумя compute допускают промежуточное нарушение суммы; внешний lock работает лишь при соблюдении его всеми читателями/писателями | A CHM/CM |
| CCN-09 | P0 | Публикация через ConcurrentMap и HB CHM: действия до размещения объекта предшествуют действиям после доступа/удаления этого объекта; чтение non-null updated value имеет связь с update данного ключа | После публикации mutable value произвольные последующие изменения его полей не становятся автоматически безопасными; два get не образуют глобального snapshot | A CM/CHM; JLS17 |
| CCN-10 | P0 | Weakly consistent traversal: CHM iterators/views, не CME и не snapshot; один iterator предназначен для использования одним потоком | Итератор может отражать изменения после создания; не проверять фиксированный набор при параллельных модификациях; keySet backed, add supported только у соответствующей key-set view | A CHM/package |
| CCN-11 | P0 | Aggregate size/isEmpty/containsValue при concurrent updates — мониторинг, не управляющий инвариант | if(map.isEmpty()) { ... } не резервирует пустое состояние; putAll/clear не предоставляют атомарный глобальный снимок | A CHM |
| CCN-12 | P0 | Collections.synchronizedMap: все обращения через wrapper; составная операция под тем же monitor; traversal views под monitor возвращённой map, не keySet | synchronized(m){ for(var k:m.keySet()) ... }; lock на backing map или view не заменяет этот протокол | A Collections; C1 |
| CCN-13 | P0 | CopyOnWriteArrayList: snapshot iterator, безопасное чтение при преобладании traversals; mutating iterator methods unsupported | Создать iterator, добавить 67, старый iterator не видит добавление; iterator.remove даёт UOE. Snapshot массива ссылок не deep-copy объектов | A COW; I COW; C2 |
| CCN-14 | P0 | ConcurrentLinkedQueue/Deque: thread-safe unbounded nonblocking FIFO queue / double-ended deque; null запрещён, weak iterator | poll пустой очереди null; CLQ size требует traversal и может быть неточным при concurrent modification; size+poll не атомарны | A CLQ/CLD |
| CCN-15 | P0 | BlockingQueue: четыре формы операций — exception, special value, blocking, timed; null запрещён | Для полной ArrayBlockingQueue: add → ISE, offer → false; для пустой: remove → NSE, poll → null; put/take могут ждать, timed offer/poll ограничены временем ожидания | A BQ/ABQ |
| CCN-16 | P0 | Capacity/backpressure: ArrayBlockingQueue fixed bound; LinkedBlockingQueue optional bound, default Integer.MAX_VALUE; producer must handle full policy | Bounded queue сама по себе не гарантирует end-to-end backpressure: вызывающий должен блокировать, отказывать, ограничивать поток или иначе явно обработать full | A ABQ/LBQ; B |
| CCN-17 | P0 | Interruption: put/take и timed offer/poll interruptible; не глотать InterruptedException; propagate или восстановить flag при выходе если контракт не позволяет throw | Отдельный worker на пустой bounded queue; контролируемый interrupt, bounded join; не требовать определённого момента входа в wait или sleep | A BQ/Thread |
| CCN-18 | P1 | ABQ fairness=true — FIFO доступ ожидающих потоков по документации класса; не обещает равное время исполнения/глобальную fairness | FIFO порядок элементов и FIFO доступ ожидающих producers/consumers — разные свойства; цена fairness зависит от workload | A ABQ |
| CCN-19 | P1 | SynchronousQueue rendezvous без внутренней ёмкости; PriorityBlockingQueue logically unbounded priority queue, null запрещён | SynchronousQueue.offer без получателя false; PBQ.put не ждёт свободного места в нормальной работе, но ресурсы ограничены; priority ties не FIFO | A SQ/PBQ |
| CCN-20 | P1 | CHM bulk forEach/search/reduce: допустимы concurrent updates, результат составной, порядок не гарантирован; reduction associative/commutative, basis identity | search может вернуть первый найденный non-null результат, не стабильный первый по порядку; threshold Long.MAX_VALUE подавляет параллелизм, 1 — максимальное разбиение | A CHM |
| CCN-21 | P1 | CHM OpenJDK21: таблица bins, CAS empty-bin insertion, synchronized nonempty-bin update, tree bins и cooperative transfer resize; retrieval обычно не блокирует | Не обещать lock-free все методы или независимость любых ключей: shared bin и callback могут задержать обновления; Segment присутствует для совместимости сериализации, не архитектура текущего hot path | I CHM; A CHM |
| CCN-22 | P1 | COW write-copy cost: изменение структуры обычно требует копирования массива ссылок O(n); не утверждать копирование при каждом no-op | Длительное удержание iterator сохраняет старый array; mutable elements остаются общими | A COW; I COW |
| CCN-23 | P1 | ABQ один lock и conditions, LBQ separate put/take locks, CLQ CAS — pinned implementation, не universal performance rank | Не выводить из числа locks победителя на всех workloads; allocations/contended operations измерять | I ABQ/LBQ/CLQ |
| CCN-24 | P1 | Memory consistency queue: передача элемента через concurrent collection публикует предшествующие действия, не все действия обоих потоков | Заполнить payload до offer/put, получить его через poll/take; последующая запись поля после передачи требует отдельной координации | A BQ/CLQ/package |
| CCN-25 | P2 | ConcurrentSkipListMap/Set как sorted concurrent alternative: отдельная граница API, expected logarithmic costs по классу | CHM не range index; не переносить на skip list внутренности TreeMap | A CSLM |
| CCN-26 | P2 | Frequency map LongAdder через computeIfAbsent: пример из CHM Javadoc; sum не атомарный snapshot при concurrent updates | После join можно проверить итог при отсутствии удаления/reset; не считать LongAdder multi-key transaction или точным счётчиком в момент updates | A CHM/LongAdder |
| CCN-27 | P2 | BlockingQueue не имеет встроенного close/shutdown; lifecycle protocol и bounded termination отдельно | Poison pill требует согласовать число consumers и политику full; executor lifecycle и cancellation остаются CON-POOL/CON-THREAD | A BQ |

## COL-TRADEOFFS — матрица CTR

| ID | P | Вопрос и механизм | Контрпример / критерий ответа | Evidence |
|---|---|---|---|---|
| CTR-01 | P0 | Начинать выбор с контракта: equality/order/null/duplicates/mutation, только затем cost | HashSet и TreeSet могут различно объединять BigDecimal 1.0/1.00; PQ не sorted iteration; не заменять одну структуру другой только по O(...) | A C1–C3 |
| CTR-02 | P0 | Workload: random indexed access vs sequential traversal, known cursor vs поиск позиции | ArrayList get(i) constant; LinkedList insert at known cursor не означает add(index) constant; deque для концов | A C2; I |
| CTR-03 | P0 | Hash lookup vs sorted range/nav: expected vs guaranteed comparison counts, hash/equals/comparator costs | Плохое распределение/дорогое equals меняют cost; TreeMap guaranteed log n базовых операций не обещает универсальный latency | A C1/C3 |
| CTR-04 | P0 | FIFO/deque/priority: операция, допустимые дубликаты и empty behavior | ArrayDeque для single-owner FIFO, PriorityQueue для head by priority; concurrent producer-consumer требует другой contract | A C2/C3/C4 |
| CTR-05 | P0 | Amortized vs expected vs worst-case: append/resize, hash distribution, heap growth | Редкая копия массива линейна; средняя стоимость последовательности не гарантирует p99 конкретной операции | A ArrayList/HashMap/PQ; I |
| CTR-06 | P0 | Capacity: size, initial capacity, load factor, growth, iteration overhead, queue bound | HashMap iteration cost capacity+size; ensureCapacity не добавляет элементы; слишком большой bound не заменяет admission control | A C1/C2/BQ |
| CTR-07 | P0 | Memory tradeoffs: contiguous reference array vs linked nodes, allocations, boxing, GC, locality; не обещать layout Java language | ArrayList<Integer> хранит ссылки, не плотный int[]; linked traversal может проиграть даже при одинаковой асимптотике; размер узла/ссылки зависит от JVM | A JLS; I; B |
| CTR-08 | P0 | Concurrency decision: ownership/immutable publication/external locking vs CHM/COW/queues; read/write ratio, contention, consistency needs | Для многоключевого инварианта CHM недостаточно; COW при частых updates дорого; thread-safe контейнер не делает mutable payload thread-safe | A C4 |
| CTR-09 | P0 | Backpressure и ресурсы: arrival/service rates, bounded vs unbounded, политика rejection/blocking/drop, deadline/interruption | Очередь с постоянным превышением arrival над service растёт или насыщается; bound ограничивает накопление, но не повышает throughput потребителя | A BQ; модель нагрузки |
| CTR-10 | P0 | Что измерять: end-to-end latency vs operation latency, throughput, distribution/percentiles; units, error/variance | Высокий ops/s не доказывает низкую p99; AverageTime не заменяет распределение; отдельно учитывать очередь, allocation и GC | B JMH modes |
| CTR-11 | P0 | JMH: warmup, measurement, forks, state scopes, params, setup; воспроизводимость версии JDK/JMH/hardware/OS/GC/heap/threads | Не сравнивать холодный первый запуск с прогретым; Scope.Thread не измеряет contention общего контейнера, shared state нужен по цели | B JMH samples |
| CTR-12 | P0 | Защита от DCE и constant folding, потребление результата/Blackhole, непостоянные входы, корректное состояние | Пустой неиспользованный результат может быть удалён JIT; заранее известные константы могут вычислиться до измеряемой операции | B JMH samples 08/09/10 |
| CTR-13 | P0 | Representative workload: размеры, распределение/коллизии ключей, hit/miss, read/write ratio, contention hot keys, threads, bounded growth, prefill/reset | Benchmark только get одного hot key не характеризует map при смешанной нагрузке; uncontrolled growth меняет n между измерениями | B JMH state/params/groups |
| CTR-14 | P1 | JMH allocation/GC profiling (-prof gc), environment noise, repeatability, reporting confidence/error; не выбирать лучший одиночный run | Сравнить allocation rate/bytes per op только для одинаковой полезной операции и одинаковой среды; GC profiler не измеряет автоматически retained heap | B JMH profiler samples |
| CTR-15 | P1 | Throughput contention: shared vs per-thread state, independent/hot keys, group read/write; correctness check вне measured path | Разные Scope меняют исследуемый вопрос; не выводить общий speedup из одного thread count | B JMH groups |
| CTR-16 | P1 | Cost model decision table: requirements → candidate → preconditions → risks → evidence needed; non-goals | Если нужен sorted range, PQ не подходит даже при быстром peek; если требуется stable tie order, добавить explicit tie-break key | A C1–C4 |
| CTR-17 | P1 | Defensive copy vs view vs snapshot iterator, shallow vs deep, consistency and retained references | List.copyOf не копирует mutable elements; COW snapshot сохраняет array; view может отражать backing state | A C1/C2/C4 |
| CTR-18 | P2 | Microbenchmark vs integration/load test: service-level latency, queues, saturation, GC pauses and tail behavior | Изолированный JMH put не моделирует network/DB/work queue; системный эксперимент отдельная задача | B JMH scope; model |
| CTR-19 | P2 | False sharing, CPU affinity, NUMA, JVM flags и profilers только при обоснованной задаче | Не объяснять любой плохой scaling false sharing без evidence; не публиковать invented performance ratios | B JMH; deferred COL/CON boundary |

## Проверяемая карта операций и выбора

| BlockingQueue | Exception | Special result | Blocking | Timed |
|---|---|---|---|---|
| Insert | add(e), ISE on full | offer(e), false on full | put(e) | offer(e,t,unit) |
| Remove | remove(), NSE on empty | poll(), null | take() | poll(t,unit) |
| Examine | element(), NSE on empty | peek(), null | — | — |

Timed calls are interruptible; timeout means bounded waiting policy, not strict wall-clock deadline of thread scheduling. Null is prohibited, capacity may vary by implementation. remainingCapacity is observation, not reservation. No atomicity promise for arbitrary bulk operations.

| Requirement | Candidate | Caveat |
|---|---|---|
| Random index / append | ArrayList | shifts and resize |
| FIFO or both ends, single owner | ArrayDeque | no null, no concurrent guarantee |
| Key lookup | HashMap | expected costs with distribution/equals assumptions |
| Range / nearest key | TreeMap | comparison cost and equivalence |
| Priority head | PriorityQueue | iteration unordered, ties not stable |
| Concurrent independent mappings | CHM | no multi-key transaction |
| Read-mostly traversals | COW list | writes, retained snapshots |
| Concurrent nonblocking FIFO | CLQ | no configured bound or backpressure |
| Bounded handoff | ABQ / bounded LBQ | interruption, saturation policy, contention |
| Sorted concurrent lookup | ConcurrentSkipListMap | separate class contract, not TreeMap internals |

## Контрпримеры и план практики

All examples below are **expected from API or logical reasoning**, not executed. G1 does not add Java files. G2 should build complete standalone sources only for justified cases.

R01: CHM transitions, distinguish absence/conditional outcomes and null callback:
```java
var m = new java.util.concurrent.ConcurrentHashMap<String,Integer>();
System.out.println(m.putIfAbsent("x", 67)); // null
System.out.println(m.putIfAbsent("x", 1));  // 67
System.out.println(m.computeIfAbsent("x", k -> { throw new AssertionError(); })); // 67
System.out.println(m.merge("x", 1, Integer::sum)); // 68
System.out.println(m.remove("x", 67)); // false
System.out.println(m.computeIfPresent("x", (k,v) -> null)); // null
System.out.println(m.containsKey("x")); // false
```
Additional isolated cases: compute on absent receives null; mappingFunction returns null on absent; RuntimeException thrown inside remapping leaves old mapping intact; null key/value and merge null input give NPE. Do not create a recursive callback that risks hang; explain documented ISE detection as conditional, not guaranteed. If G2 demonstrates one detected case, isolate with bounded process and label I/observed outcome.

R02: queue capacity one:
```java
var q = new java.util.concurrent.ArrayBlockingQueue<Integer>(1);
System.out.println(q.offer(67)); // true
System.out.println(q.offer(1)); // false
System.out.println(q.poll()); // 67
System.out.println(q.poll()); // null
```
Separate try/catch cases for full add, empty remove, null offer. Timed offer/poll with zero or bounded timeout: assert result, not duration. Interruption test only if useful: worker with initially empty queue, CountDownLatch signals readiness, interrupt and bounded join; catch InterruptedException in worker, communicate result with Future/atomic reference; assert termination and outcome, not precise wait-state/timing. Interrupt before take is also valid; no claim of having observed blocked state without evidence. Cleanup in finally with interrupt/cancel and bounded waits, surface timeout/failure.

R03: COW snapshot: create list [1,2], iterator, add 67, traverse old iterator -> [1,2]; list -> [1,2,67]. iterator.remove unsupported. For mutable payload explain shallow references; do not race mutable fields.

R04: compound lost update is a logical interleaving, not race test: both workers read 0 before either writes, then both write 1. If implementation chosen, use bounded barrier/latch coordination and failure cleanup; do not rely on sleep. Contrast one mapping merge(k,1,Integer::sum) by two workers with N increments each, start latch and bounded completion, assert 2*N after join (no deletion, no overflow); all worker exceptions propagate. One successful run is not proof of absence of races. Multi-key debit/credit example: intermediate sum after first operation can differ; no actual concurrent timing necessary.

N01 compiler-negative: Queue<Integer> q = new ArrayBlockingQueue<>(1); q.put(67); // put not declared by Queue. In G2 make complete isolated class with import and throws InterruptedException to distinguish method-not-found from checked exception.
N02 compiler-negative: method without throws/catch calls BlockingQueue.take(); unreported InterruptedException (ensure valid declared types). Separate source from N01.
R05: synchronizedMap traversal must synchronize on wrapper, not keySet; code illustration only, do not deliberately cause deadlock to demonstrate failure.
R06: CHM weak iterator and size under mutation: no exact concurrent stdout or fixed traversal set asserted; illustrate contract through documentation and deterministic stable-state traversal only.

### Benchmark design, not benchmark results

If approved, compare ArrayList/LinkedList indexed get vs traversal at multiple n (setup prefilled); HashMap/TreeMap lookups on equal key sets with documented equality/comparator; CHM vs synchronizedMap with matched workloads, distinct vs hot keys, shared state across threads, separate read/write groups and thread counts. COW case read-mostly vs write-heavy at controlled size, include copying/allocations. Never compare semantically different operations as equivalent. For bounded queues include admitted operations, failures, producer/consumer balance and queue occupancy; a nonblocking offer benchmark cannot measure end-to-end backpressure.

Record source SHA, exact JDK/JMH versions, CPU/OS, heap/GC, fork/warmup/measurement configuration, state scope and parameters, input distribution, units, error/distribution, allocation profiler configuration. Consume results and avoid constant-folded inputs; do not include unrelated setup in measured operation unless intentional. Avoid uncontrolled state growth, benchmark ordering bias and treating a single run as evidence. Report no universal ratio, memory bytes or latency. **JMH NOT RUN, no new module required.** A methodology table and one reasoned scenario can satisfy G2 without executable benchmark.

## Dependencies and scope

Actual published relative targets: [contracts](../../knowledge/02-collections/contracts.md), [hash](../../knowledge/02-collections/hash-collections.md), [sequential](../../knowledge/02-collections/sequential.md), [iteration](../../knowledge/02-collections/iteration.md), [sorted](../../knowledge/02-collections/sorted.md), [priority queue](../../knowledge/02-collections/priority-queue.md), [C1 plan](COL-CONTRACTS-HASH.md), [C2 plan](COL-SEQUENTIAL-ITER.md), [C3 plan](COL-SORTED-HEAP.md), [CORE arrays](../../knowledge/01-java-core/arrays.md), [CORE equality](../../knowledge/01-java-core/equality.md), [CORE immutability](../../knowledge/01-java-core/immutability.md), [CORE generics](../../knowledge/01-java-core/generics.md), [CORE utilities](../../knowledge/01-java-core/utilities.md), [semaphore](../../knowledge/03-concurrency/semaphore.md) only if useful for admission-control comparison, not as substitute for queue capacity or proof of CON coverage. Other CON IDs remain future IDs; do not invent links. Suggested future files (not links): knowledge/02-collections/concurrent.md and tradeoffs.md; actual names/split decided at G2.

Propose one compact standalone deterministic probe for per-key transitions, bounded queue outcomes, COW snapshot, plus N01/N02; optional coordinated invariant-based update/interruption cases only if useful. No mandatory Gradle/JUnit/JMH module. 02/03 own G2 implementation and technical contract after G1 acceptance.

## G1/G2 and final section audit risks

1. Every P0 receives own explanation and counterexample; each P1 gets sufficient explanation or real link; P2 explicit boundary. Repeat C sample against draft and document gaps.
2. Check exact Java21 method contracts for callback recursion, exceptions, null returns, HB, weak traversal and bulk operations; compare with I source, do not confuse Segment serialization with modern locking.
3. Cross-check C1–C4 consistency: no global snapshot implied by CHM, no deterministic fail-fast/tie/order assertion, no unconditional performance rank, correct range/equality/null/queue contracts, expected vs measured evidence. Check older links and status separately under authorized G2/G6.
4. Final 8-ID audit after C4 G5/G6 is a separate assignment of 00, not automatically DONE by this research or count. Include deferred CON boundaries, exact source/runtime evidence and residual GAP.
5. **NOT RUN:** local javac/java/Gradle/JUnit, negative diagnostics, expected stdout, JMH, GUI Obsidian/IDE. Source reads/search and Git diff are static evidence. Existing CI does not compile Markdown snippets. Record actual PR CI separately on exact HEAD; no invented result.
6. One research-only file. G1 review by independent 04 on exact HEAD → 00 acceptance/guarded merge → separate G2 assignments. No status changes or early Implemented claim.
