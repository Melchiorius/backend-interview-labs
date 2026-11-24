# Последовательные коллекции и итераторы: практические проверки Java 21

Теория: [COL-SEQUENTIAL](../../../knowledge/02-collections/sequential.md). [COL-ITER](../../../knowledge/02-collections/iteration.md).
Предпосылки: [контракты коллекций](../../../knowledge/02-collections/contracts.md), [hash-based коллекции](../../../knowledge/02-collections/hash-collections.md), [массивы](../../../knowledge/01-java-core/arrays.md), [generics](../../../knowledge/01-java-core/generics.md).
Основание: [план C2](../../../docs/plans/COL-SEQUENTIAL-ITER.md).

## Что запускается

[SequentialIteratorProbe.java](src/SequentialIteratorProbe.java) не требует входных данных или зависимостей. Каждый check сравнивает ожидаемое значение с фактическим, при расхождении бросает AssertionError независимо от -ea; throwsType проверяет ожидаемый тип исключения. Программа печатает 52 строки, целиком сверяемые с [эталоном](expected.stdout). Это наблюдения одного примера, не 52 JUnit-теста и не полное покрытие плана.

## Воспроизведение

Нужны OpenJDK 21 (javac и java из одного JDK), Bash, diff, grep, mktemp и стандартные Unix-утилиты. Из корня репозитория:

```bash
bash examples/collections/sequential-iter/verify.sh
```

Из каталога примера можно запустить `./verify.sh`; для одного позитивного примера — `java src/SequentialIteratorProbe.java`. Runner компилирует позитивный исходник с --release 21 -Xlint:all, запускает JVM, сверяет весь stdout через diff -u, затем отдельно компилирует N01/N02. Временные файлы удаляются при выходе. Код 0 означает успех всех проверок. Негативные файлы намеренно не компилируются: не включать их в общий вызов javac.

## Разбор

| Сценарий | Строки плана | Механизм и граница |
|---|---|---|
| Размер, вставка, массив | CS-01/03/12 | ensureCapacity(67) не меняет size. Вставка по индексу меняет позиции элементов списка; длина исходного массива остаётся фиксированной. Это не измерение времени или внутренней ёмкости. |
| R01 subList | CS-08, CI-14 | set через view меняет parent; nonstructural set в parent виден через view; clear через view удаляет диапазон в parent. Внешняя структурная правка parent вне view не проверяется: семантика view в этом случае не определена контрактом. |
| R02 Arrays.asList | CS-09/14 | set пишет в backing array, запись в массив видна в fixed-size list; add запрещён. ArrayList copy имеет отдельную изменяемую структуру, не deep copy элементов. |
| R06 Deque | CS-05/06/07 | poll/peek на пустой ArrayDeque возвращают null; remove/element бросают NSE; null запрещён. Операции на концах, offer/poll (FIFO), push/pop (LIFO) показаны на одном объекте. Не все Queue имеют FIFO или одинаковую null policy. |
| R03 и R08 Iterator | CI-01/02/04/08/09 | hasNext не потребляет элемент; remove до next и повторный remove — ISE для ArrayList; допустимый remove обновляет список и не мешает продолжению. forEachRemaining начинает с оставшихся элементов; после исчерпания next бросает NSE. У List.of remove не поддерживается (UOE). |
| R04 ListIterator | CI-03/10, CS-04 | LinkedList cursor изначально перед первым элементом (nextIndex=0, previousIndex=-1). После next(1) и add(67) список [1,67,2], курсор после 67 (nextIndex=2). set/remove до нового next/previous запрещены. previous возвращает 67, set заменяет его на 68, remove удаляет его; nextIndex снова 1, следующий next возвращает 2. |
| enhanced for | CI-06 | Одинаковая сумма для массива и Iterable, но разные механизмы по JLS: индексный обход и Iterator. |
| R05 snapshot | CI-07/13 | Итератор CopyOnWriteArrayList создан до add/set и видит старую последовательность [1,2]; текущий список [68,2,67]. Изменение через iterator.remove запрещено. Snapshot относится к ссылкам в массиве, не к глубокому состоянию объектов. |
| R07 fail-fast | CI-04/05/08/12 | На OpenJDK 21 после создания ArrayList iterator выполняется один внешний add, затем next. Наблюдается CME. Это конкретный implementation probe с best-effort detection, не гарантия для всех изменений, реализаций или гонок. |

## Отрицательная компиляция

- [N01](negative/N01.java), CI-06: HashMap не является Iterable, enhanced for требует массив или Iterable. Обходить можно, например, entrySet().
- [N02](negative/N02.java), CS-05: у ArrayDeque нет get(int); работа идёт через операции Deque, не индекс List.

На javac 21.0.12.1 наблюдаются exit=1, соответственно compiler.err.foreach.not.applicable.to.type и compiler.err.cant.resolve.location.args (get, int). Runner проверяет статус и категорию, не весь локализованный текст. На другом компиляторе или версии диагностики могут отличаться.

## Границы

ConcurrentHashMap имеет weakly consistent итераторы: они могут видеть часть обновлений, не обещают единого snapshot и не бросают CME из-за конкурентного изменения. Это API-объяснение, а не race-test. Snapshot и weak consistency не обеспечивают атомарность произвольной бизнес-операции. CME не является средством синхронизации. См. Java SE 21 [Iterator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Iterator.html), [ArrayList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html), [CopyOnWriteArrayList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CopyOnWriteArrayList.html), [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html). Пример не измеряет latency, allocations, GC, capacity growth или memory layout; не использует reflection и не претендует на проверку всех строк CS/CI.
