# Конкурентные коллекции и выбор структуры: практические проверки Java 21

Целевые статьи C4: `knowledge/02-collections/concurrent.md` и `knowledge/02-collections/tradeoffs.md` (ссылки активировать после появления файлов).
Предпосылки: [контракты](../../../knowledge/02-collections/contracts.md), [hash-based коллекции](../../../knowledge/02-collections/hash-collections.md), [последовательные коллекции](../../../knowledge/02-collections/sequential.md), [итераторы](../../../knowledge/02-collections/iteration.md), [сортированные коллекции](../../../knowledge/02-collections/sorted.md), [приоритетная очередь](../../../knowledge/02-collections/priority-queue.md).
Основание: [план C4](../../../docs/plans/COL-CONCURRENT-TRADEOFFS.md).

## Запуск

[ConcurrentCollectionsProbe.java](src/ConcurrentCollectionsProbe.java) — самостоятельный пример без внешних зависимостей. check сравнивает значения, throwsType — ожидаемый тип исключения; расхождения вызывают AssertionError независимо от -ea. [Эталон stdout](expected.stdout) содержит 61 строку. Это наблюдения одного runnable, а не 61 JUnit-тест или полная проверка всей матрицы.

Нужны OpenJDK 21 (javac и java одного JDK), Bash, diff, grep, mktemp и стандартные Unix-утилиты. Из корня checkout:

```bash
bash examples/collections/concurrent-tradeoffs/verify.sh
```

Из каталога примера можно вызвать `./verify.sh`; отдельно позитивный пример — `java src/ConcurrentCollectionsProbe.java`. Runner компилирует позитивный исходник с --release 21 -Xlint:all, запускает JVM, сверяет stdout полностью через diff -u, затем компилирует два отрицательных файла по отдельности. Временные файлы удаляются; exit=0 означает прохождение проверок.

## Разбор сценариев

| Сценарий | Матрица | Что проверяется |
|---|---|---|
| R01 CHM | CCN-01/03–07 | Отсутствие и запрет null key/value; conditional replace/remove, putIfAbsent; условия вызова callback computeIfAbsent/Present, compute, merge; null result и исключение при неизменяемых значениях Integer. |
| R02 ABQ | CCN-15/16, CTR-04/06/09 | Очередь ёмкости один: empty poll=null, remove=NSE; full offer=false, add=ISE; запрет null, timed offer/poll с нулевым timeout при full/empty и доступном месте/элементе. |
| R03 COW | CCN-13/22, CTR-17 | Старый iterator сохраняет ссылки на a и b, хотя структура списка изменена. Изменение StringBuilder a в одном потоке видно через старую ссылку: [a67,b], тогда как новый список [a67,d,c]. remove итератора запрещён. |
| R04 atomicity | CCN-02/08, CTR-08 | Последовательная логическая трасса lost update; отдельные атомарные операции A/B не сохраняют общий инвариант между ними. Два реальных worker выполняют per-key merge; итог проверяется после Future.get обоих. |
| R05 wrapper | CCN-12 | Составная операция и обход keySet под монитором возвращённой synchronizedMap. Все обращения участников обязаны соблюдать единый протокол. |
| R06 views | CCN-10/11 | Обычный keySet запрещает add, keySet(mappedValue) поддерживает вставку соответствующего отображения. Membership и size проверяются только в стабильном состоянии, без утверждения порядка обхода. |

### Атомарность и callback

Пример относится к ConcurrentHashMap, а не ко всем реализациям ConcurrentMap. Для CHM при существующем non-null mapping computeIfAbsent не вызывает функцию; при отсутствии computeIfPresent не вызывает функцию; merge при отсутствии устанавливает переданное non-null value без вызова remapping function. В остальных показанных случаях callback получает соответствующее прежнее значение; null из compute/computeIfPresent/merge удаляет отображение, null из computeIfAbsent оставляет отсутствие. Исключения из показанных callback сохраняют прежнее отображение или отсутствие. Это не транзакционный rollback внешних side effects и не откат полей mutable value. Не изменяйте ту же карту из callback вопреки контракту метода; вычисление должно быть коротким. Рекурсивные обновления не запускаются; диагностика IllegalStateException не гарантирует обнаружение всех циклов. CHM может задерживать другие обновления во время вычисления.

### Логическая трасса и реальный запуск двух worker

Два логических клиента читают x=0 до любой записи, затем каждый пишет своё прочитанное значение плюс один. Итог 1 вместо 2; это специально последовательная модель допустимого interleaving, не случайно воспроизведённая гонка. Вторая модель: A=67, B=0, после списания единицы сумма 66, после зачисления — 67. Две per-key операции не являются многоключевой транзакцией; общий lock помогает только при соблюдении протокола всеми читателями и писателями.

В реальном примере фиксированный executor из двух worker и CountDownLatch разрешают общий старт, но не гарантируют одновременность или расписание. Каждый worker выполняет 1000 merge по одному ключу. При отсутствии удаления, reset и overflow после завершения обоих ожидается 2000. Future.get получает результат/исключение в пределах общего 10-секундного бюджета ожидания; проверка использует остаток deadline, а не по 10 секунд на каждый Future. finally открывает latch, отменяет незавершённые задачи, вызывает shutdownNow и ожидает завершения до 5 секунд, при неуспехе бросает исключение. Worker проверяет interruption; daemon threads — дополнительная защита standalone-процесса, не замена cleanup. Бюджеты являются пределами ожидания API, не измерениями скорости или гарантией жёсткого wall-clock deadline. Отдельный interruption probe не запускается. Один проход не доказывает race freedom.

### Границы гарантий коллекций

Timed queue operations с timeout=0 показывают доступность без проверки длительности. Блокирующие put/take и положительное ожидание здесь не запускаются; interruption необходимо передать вызывающему или восстановить флаг при выходе согласно контракту. Bound ограничивает накопление, но требует решения на стороне producer (blocking, отказ, drop, admission control); не повышает сам по себе throughput потребителя. Weakly consistent iterator CHM не даёт общего snapshot или фиксированного набора при обновлениях; aggregate size/isEmpty не являются резервированием состояния. Thread-safe контейнер публикует ссылки согласно контракту, но не делает произвольные последующие изменения payload безопасными. Snapshot COW — не deep copy; synchronizedMap требует монитор wrapper, не backing map или keySet. См. Java SE 21 [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html), [ConcurrentMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentMap.html), [BlockingQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/BlockingQueue.html), [ArrayBlockingQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ArrayBlockingQueue.html), [CopyOnWriteArrayList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CopyOnWriteArrayList.html), [Collections](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html).

## Compiler-negative

- [N01](negative/N01.java), CCN-15: статический тип Queue<Integer> не объявляет put, хотя объект — ArrayBlockingQueue. throws InterruptedException присутствует для изоляции ошибки разрешения метода.
- [N02](negative/N02.java), CCN-17: тип BlockingQueue корректен, take объявляет InterruptedException, который не пойман и не объявлен.

Фактически на javac 21.0.12.1 каждый завершился exit=1: N01 compiler.err.cant.resolve.location.args (put/int/Queue), N02 compiler.err.unreported.exception.need.to.catch.or.throw (java.lang.InterruptedException). Runner проверяет статус и категорию, не весь локализованный текст; другой компилятор может потребовать адаптации. Не включайте эти файлы в позитивную компиляцию.

## Tradeoffs / NOT RUN

Выбор начинается с контракта (порядок, равенство, атомарность, ограничение ресурса), затем workload и цена операций. COW полезен при подходящем соотношении чтений/изменений, CHM — при независимых отображениях, ABQ — при явном bound и политике обработки заполнения. Это условия выбора, не сравнительные измерения. Не измерялись throughput, latency, percentiles, память/GC, JMH, stress-тесты, fairness или результаты гонок. Для сравнений потребуются репрезентативная нагрузка, отдельные успешные/отклонённые операции, версии и параметры среды, warmup/forks/state/setup, защита от DCE/constant folding и статистическая обработка. Не выдавать один runtime probe за benchmark или доказательство реализации всех CCN/CTR. Теоретические вопросы JMH, pinned OpenJDK, bulk operations, skip lists, LongAdder и остальные tradeoffs остаются предметом статей.
