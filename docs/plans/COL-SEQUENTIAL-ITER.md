# Collections C2 — COL-SEQUENTIAL + COL-ITER: исследовательская карта Java 21

**Статус:** G1 PROPOSED; **Issue:** [#223](https://github.com/Melchiorius/backend-interview-labs/issues/223); **программа:** [#213](https://github.com/Melchiorius/backend-interview-labs/issues/213). **Исходный BASE/main:** `49c6d281005edac614d6d3a66f868f6132337b66`. **Цель:** исследование до G2, **не** готовые статьи, исполняемые тесты или G1 acceptance. **Целевая платформа:** Java SE 21; implementation evidence — OpenJDK 21 repository HEAD `890adb6410dab4606a4f26a942aed02fb2f55387`. 

## Обещание и границы

По `COL-SEQUENTIAL` читатель сможет обоснованно сопоставлять массив, `ArrayList`, `LinkedList`, `ArrayDeque`: управление размером и ёмкостью, доступ к позиции, добавление/удаление с учётом поиска позиции, очередь и стек, порядок, nullability, особые views и условия сложности. По `COL-ITER` — объяснять состояние `Iterator/ListIterator`, разрешённые мутации, причины `IllegalStateException`, `ConcurrentModificationException` и `NoSuchElementException`, отличие fail-fast best effort от snapshot/weakly-consistent обхода; предсказывать результат, а не запоминать лозунг «все итераторы fail-safe».

**P0** — обязательное собственное объяснение и предметный контрпример будущей статьи; **P1** — существенная оговорка, либо содержательный переход к существующей статье; **P2** — конкретный «хвост». Не приравнивать P0/P1/P2 к Middle/Senior. Два curricular ID не требуют ровно двух статей, но позволяют две логические матрицы `CS-xx`/`CI-xx`. **За пределами C2:** полноценные `COL-SORTED` (деревья и comparator), `COL-HEAP`, `COL-CONCURRENT` (memory consistency, atomicity) и `COL-TRADEOFFS` (измерение и реальные нагрузки). `ConcurrentHashMap` и `CopyOnWriteArrayList` используются **только** для сравнения контрактов обхода.

## Источники: уровень доверия и точные версии

### A — Java SE 21 API и JLS

- [`List`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html), [`Collection`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collection.html), [`Iterable`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Iterable.html), [`SequencedCollection`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedCollection.html) — interfaces, encounter order, `subList`, optional mutation, generic contracts.
- [`ArrayList`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html), [`LinkedList`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedList.html), [`ArrayDeque`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayDeque.html), [`Deque`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Deque.html), [`Queue`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Queue.html), [`Arrays.asList`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html#asList(T...)).
- [`Iterator`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Iterator.html), [`ListIterator`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ListIterator.html), [`Spliterator`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Spliterator.html), [`AbstractList`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/AbstractList.html) — iterator state, cursor, modCount contract for subclasses, traversals.
- [`CopyOnWriteArrayList`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CopyOnWriteArrayList.html), [`ConcurrentHashMap`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) — snapshot и weakly-consistent согласно документации **конкретных классов**, не общей метке «fail-safe».
- [JLS 21 §14.14.2 enhanced for](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.14.2), [JLS 21 §4.5 generics](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.5) — трансляция for-each и compile-time ограничение типов.

### I — исходники OpenJDK 21, НЕ универсальный API

Фиксация исходника: commit `890adb6410dab4606a4f26a942aed02fb2f55387` в `openjdk/jdk21`. Переносить из него **только** модель реализации с явно указанной версией, не обещать такой же рост таблиц/узлов другим JDK:
- [`ArrayList.java`](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/ArrayList.java): `grow`, `System.arraycopy`, `SubList`, `modCount`; политика расширения внутреннего массива — **I**, не специфицированная формула роста Java `List`.
- [`LinkedList.java`](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/LinkedList.java): `linkLast`, `linkBefore`, `node(int)` с движением от ближнего конца, `ListItr`, узлы/связи.
- [`ArrayDeque.java`](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/ArrayDeque.java): индексируемый циклический массив, `head/tail`, `grow`, `DeqIterator`; конкретная начальная/последующая capacity — **I**, не публичное обязательство.
- [`AbstractList.java`](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/AbstractList.java): `modCount`, проверка ожидаемой модификации у `Itr`.
- [`CopyOnWriteArrayList.java`](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/concurrent/CopyOnWriteArrayList.java) — создание `COWIterator` поверх состояния массива.
  
**Прямо проверено:** чтение этих файлов из GitHub на указанном исходном SHA и поиск перечисленных методов/полей. Точная byte-level сборка/локальный запуск исходников **NOT RUN**.

### B — пояснения

- [dev.java Learn](https://dev.java/learn/) — официальный учебный обзор (при использовании конкретной страницы сверять её версию с A21), [Java SE21 collection framework overview](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/doc-files/coll-reference.html). B не подменяет гарантии Java21 Javadoc.

### C — подтверждённые вопросы из независимых банков, не источник API-гарантий

- **C1:** [Baeldung Java Collections Interview Questions](https://www.baeldung.com/java-collections-interview-questions): конкретные группы «Difference Between LinkedList and ArrayList», итерация/fail-fast, `Iterator` vs `ListIterator`. Их формулировки (например «always throws CME») проверять по A, не переносить как гарантию.
- **C2:** [teamlead/java-interview-questions README, commit-pinned source SHA `4c0784024412904625e1455189d3c05cba07bebf`](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md): вопросы «What is the difference between Linkedlist and Arraylist», «What is an iterator and why it is needed», «What is Failfast», «What is Deque», «Is adding always to ArrayList the complexity O (1)», «What is the speed of access to the element in Linkedlist by index», «What is the feature of CopyonWritearraylist». Частоты автора не подтверждены как статистика.
- **C3:** [in28minutes `CollectionHierarchy.java`](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/src/main/java/com/in28minutes/java/collections/CollectionHierarchy.java): фактически найдены «What is difference between ArrayList and LinkedList», «What is difference between Array and ArrayList», `Iterator/ListIterator` API, `Deque/ArrayDeque` в группе вопросов. Последние объявления в файле — пример старой модели; уточнить Java 21 `SequencedCollection`.
- **C4:** [ViacheslavChernyshov: fail-fast/fail-safe](https://viacheslavchernyshov.github.io/java-interview-questions-and-answers/questions/04-collections/26-what-are-fail-fast-and-fail-safe-iterators/) — конкретная страница C; «fail-safe» не нормативная категория Javadoc, а слишком широкая бытовая метка для двух разных моделей.
  
**Границы C:** это обнаруженные формулировки вопросов, не верифицированные интервью-частоты и не автоматически корректные ответы. Повторный поиск/выборка перед завершением G2 обязателен; если вопрос не найден, отмечать C GAP, не изобретать источник. Дальнейший G4 должен независимо оспаривать спорные утверждения.

## Матрица COL-SEQUENTIAL (CS)

| ID | Приоритет | Содержание ответа и механизм | Контрпример / предсказание / граница | Evidence |
|---|---|---|---|---|
| CS-01 | P0 | Java array: фиксированная длина и индекс; `ArrayList` — изменяемый `size` с внутренней ёмкостью; `size` не `capacity` | Массив `int[2]` не `add`; `ArrayList.ensureCapacity` не увеличивает `size`; `get(size)` бросает IOOBE | A List/ArrayList; C3 |
| CS-02 | P0 | `ArrayList` произвольный индекс — constant time; добавление в конец амортизированно O(1) при обычном расширении, единичный grow+copy — O(n) | `add` не всегда O(1); конкретный коэффициент роста — I, не API; возможные resource limits | A ArrayList; I; C2 |
| CS-03 | P0 | Вставка/удаление в середине `ArrayList`: сдвиг элементов O(n), даже когда индекс известен | `add(0,x)`, `remove(0)` сдвигают ссылки; `set(i,x)` не меняет структуру и обычно O(1) | A ArrayList; I |
| CS-04 | P0 | `LinkedList`: двусвязные узлы (I); позиционный `get(index)` — обход O(n); добавление/удаление через уже известную позицию/итератор без поиска — O(1) изменения связей | `list.add(list.size()/2,x)` не O(1), поскольку надо найти узел; `ListIterator.add` при известном cursor — другая цена | A LinkedList/ListIterator; I; C1/C2 |
| CS-05 | P0 | `ArrayDeque` — `Deque`, кольцевой массив (I), операции с обоих концов (амортизированно O(1) при расширении); не `List`, не индексированный `get(i)` | `Deque<Integer> dq = new ArrayDeque<>(); dq.get(0)` compiler-negative; `addFirst/addLast/removeFirst/pollLast` отдельно | A Deque/ArrayDeque; I; C2 |
| CS-06 | P0 | Семантика queue/stack: `offer/poll/peek` vs `add/remove/element`, `push/pop`, FIFO для ArrayDeque при addLast/removeFirst; fail modes пустоты | `new ArrayDeque<>().pollFirst()` возвращает null, `removeFirst()` throws NoSuchElementException | A Queue/Deque/ArrayDeque |
| CS-07 | P0 | `null`: `ArrayList` и `LinkedList` допускают null, `ArrayDeque` запрещает; значение null смешивалось бы с обозначением пустоты `poll/peek` | `new ArrayDeque<String>().add(null)` — NPE; LinkedList null допустим; не обобщать на все Queue | A ArrayList/LinkedList/ArrayDeque |
| CS-08 | P0 | `List.subList(from,to)` — backed view полуинтервала, изменения через view отражаются в исходном списке и наоборот в пределах согласованного контракта | `subList(...).clear()` меняет parent; структурная правка parent **вне view** даёт undefined semantics view, не обещать всегда CME | A List/ArrayList |
| CS-09 | P0 | `Arrays.asList(array)` — fixed-size list backed by array; `List.of` — unmodifiable; `new ArrayList<>(...)` — отдельная modifiable copy | `Arrays.asList(a).set(0,x)` меняет массив; `add` UOE; `new ArrayList<>(...)` независим от последующих перестановок массива | A Arrays/List; C3 |
| CS-10 | P0 | Выбор структуры по профилю доступа и mutation, не по лозунгу «LinkedList быстрее для вставок»: locality, allocations и GC могут перевесить сложность ссылочных операций | Без известного узла в LinkedList поиск O(n); без замеров нельзя утверждать универсальную latency/память | A + I; C1/C2 |
| CS-11 | P1 | Java21 Sequenced API: `List`/LinkedList/ArrayDeque имеют предписанный encounter order; `reversed()` — view, не deep copy | `ArrayDeque.reversed()` меняет направление encounter order, `List.reversed()` не сортирует | A SequencedCollection/List/Deque |
| CS-12 | P1 | `ensureCapacity` vs `trimToSize` (ArrayList), structural modification и cost iterators | `trimToSize` может менять modCount даже без изменения size в реализации; не полагаться на сохранность iterator после capacity operations | A ArrayList; I |
| CS-13 | P1 | `LinkedList` как List и Deque: двунаправленные итераторы и `descendingIterator`; не путать список и очередь | `LinkedList.add(null)` возможен, но `Deque` API советует избегать null как маркера пустоты | A LinkedList/Deque |
| CS-14 | P1 | Fixed-size vs unmodifiable vs immutable structure, shallow elements, source-array backing; `subList` не snapshot | `Collections.unmodifiableList(parent)` может отражать изменения parent; `List.copyOf` не deep-copy элементов | A List/Collections; C1 |
| CS-15 | P2 | Runtime overhead объектов и reference locality, реальные memory-layout/GC, JIT, boxing | Не обещать «LinkedList всегда 3 ссылки на элемент» как реальные байты; измерение — COL-TRADEOFFS | I; граница |
| CS-16 | P2 | Специальные случаи `removeIf`, `toArray`, `sort`, `RandomAccess`, parallel stream spliterator | Сложность/оптимизация зависит от конкретного метода и реализации, не выводить универсальную из общего List | A List/RandomAccess |

## Матрица COL-ITER (CI)

| ID | Приоритет | Содержание ответа и механизм | Контрпример / предсказание / граница | Evidence |
|---|---|---|---|---|
| CI-01 | P0 | `Iterable.iterator` создаёт итератор; `hasNext` проверяет доступность, `next` выдаёт элемент и двигает cursor; исчерпание -> NoSuchElementException | `List.of(67).iterator().next()` выдаёт 67, следующий `next()` throws; `hasNext` сам не потребляет | A Iterable/Iterator |
| CI-02 | P0 | `Iterator.remove` — optional и разрешён не более одного раза на один успешный `next()`; до первого next/повтор — IllegalStateException, если remove поддерживается | `ArrayList` iterator: `remove` перед `next` = ISE; `List.of` iterator.remove = UOE; не объявлять UOE/ISE одинаковыми во всех реализациях | A Iterator/ArrayList |
| CI-03 | P0 | `ListIterator` двунаправленный, cursor **между** элементами, индексы nextIndex/previousIndex; `add`, `set`, `remove` с точными preconditions | `set` до next/previous throws ISE; `add` сбрасывает допустимость set/remove до следующего next/previous; `previous()` в начале throws NSE | A ListIterator |
| CI-04 | P0 | Модификация через **собственный** iterator.remove/ListIterator.add vs внешняя structural modification; фиксация expected modCount — I | После `it.next(); it.remove()` поддерживаемый iterator корректно продолжает; `list.add(...)` после создания iterator может вызвать CME | A ArrayList/Iterator; I |
| CI-05 | P0 | Fail-fast — best-effort detection, **не** контракт для корректности алгоритма, не защита от гонок | Не утверждать, что каждое `list.add` гарантированно вызовет CME и что `try/catch CME` обеспечивает thread safety | A ArrayList/LinkedList; C1/C2 |
| CI-06 | P0 | Enhanced `for(T x : iterable)` использует iterator по JLS §14.14.2; при array используется индексный traversal, а не вызов `iterator()` | `for (var x : new int[]{1,2})` допустим; `for (var x : new HashMap<>())` compiler-negative (Map не Iterable) | A JLS/Iterable |
| CI-07 | P0 | Конкретные модели: ArrayList/LinkedList fail-fast best-effort; `ConcurrentHashMap` views weakly-consistent; `CopyOnWriteArrayList` snapshot iterator | Итератор COW видит snapshot при создании, `iterator.remove` UOE; CHM может увидеть часть concurrent updates, не promise полного/свежего snapshot | A ArrayList/LinkedList/CHM/COW; C4 |
| CI-08 | P0 | Различать exceptions: `NoSuchElementException` (конец), `IllegalStateException` (неверная последовательность), `UnsupportedOperationException` (optional mutation), `ConcurrentModificationException` (best-effort detection) | Прогонить изолированные positive/negative cases; не обещать exact CME в race или при каждом внешнем изменении | A Iterator/ListIterator/ArrayList |
| CI-09 | P0 | `forEachRemaining` выполняет action для каждого оставшегося элемента, передаёт исключения action; postconditions iterator, изменения из action | `it.next(); it.forEachRemaining(action)` начинает после выданного элемента; `action` не должен модифицировать source, если это не разрешено iterator contract | A Iterator |
| CI-10 | P1 | `ListIterator.add/remove/set` и порядок элементов при перемещении cursor, операции независимы от `List.set` | Пример `[1,2]`, cursor индекс 1, add(67) -> `[1,67,2]`, subsequent `previous()` -> 67; уточнить exact cursor rules | A ListIterator |
| CI-11 | P1 | `Spliterator`: `tryAdvance/forEachRemaining/trySplit`, characteristics (ORDERED/SIZED/CONCURRENT/IMMUTABLE), late-binding и fail-fast у конкретного источника | `Spliterator.CONCURRENT` не равен `Iterator` snapshot; `ArrayList` spliterator late-binding/ fail-fast best-effort | A Spliterator/List/ArrayList |
| CI-12 | P1 | `AbstractList.modCount` — protected implementation hook, не спецификация для каждого Iterable/Iterator | Пользовательский Iterable может обходиться без modCount; `ConcurrentModificationException` не следует из одного `Iterable` | A AbstractList; I |
| CI-13 | P1 | Weak consistency vs snapshot vs fail-fast — три разных модели; корректность конкурентной логики отдельно в COL-CONCURRENT и CON-JMM | Ни weakly-consistent, ни snapshot не гарантируют бизнес-атомарность совместных операций | A CHM/COW; C4 |
| CI-14 | P1 | `List.subList` и iterator: изменения backing, view-driven structural modification и undefined behavior outside view | Не превращать undefined view semantics в гарантированное CME; конкретное наблюдение на OpenJDK21 — лишь I | A List |
| CI-15 | P2 | `Iterator.remove` default throws UOE, implementation override; `ListIterator` необязательно поддерживает all mutation operations | Нельзя просто вывести поддержку mutation из интерфейса ListIterator | A Iterator/ListIterator |
| CI-16 | P2 | Concurrent iterator best-effort traversal counts и параллельные Stream/Spliterator; не измерять outcome на одной гонке | `forEach` CHM может наблюдать часть updates, без гарантий полного consistent snapshot | A CHM/Spliterator; граница C4 |

## Условия сложности: контракт, реализация, неизвестное

| Сценарий | Корректное утверждение | Ошибочное обещание |
|---|---|---|
| `ArrayList.get(i)` и `set(i,v)` | constant time по Javadoc реализации при допустимом i | `add(i,v)` тоже всегда constant |
| `ArrayList.add(e)` | амортизированная постоянная стоимость при росте; отдельное расширение/копирование — линейная операция | каждая вставка за строгий O(1) |
| `ArrayList.add(0,e)` | линейный сдвиг в текущей реализации | O(1), если известен индекс |
| `LinkedList.get(i)` | обход O(n) через `node(int)` (I), даже при известном индексе | random access как у массива |
| `LinkedList` insertion at **known cursor** | локальные изменения связей O(1), поиск позиции оплачивается отдельно | `list.add(n/2,x)` строго O(1) |
| `ArrayDeque` операции с концами | амортизированно O(1), возможен resize; `remove(Object)`/поиск требуют traversal | произвольный индексный доступ или строго O(1) на любое удаление |
| Итерация и `ListIterator` | последовательный обход O(n) для n посещений, учитывая cost пользовательского action | точное время/байты/GC без измерений |

**Performance caveat:** реальные размер узла, cache locality, GC pressure, boxing и JIT не определены интерфейсом; одна микропроверка не демонстрирует универсальную скорость. Бенчмарки и рекомендация выбора по нагрузке — область `COL-TRADEOFFS`.

## Контрпримеры: планируемые isolated cases (не выдавать за выполненные)

**N01 — compiler-negative, Map vs Iterable (JLS):**
```java
import java.util.HashMap;
class N01 {
  void probe() {
    for (var x : new HashMap<String,Integer>()) { } // compiler-negative: Map is not Iterable
  }
}
```

**N02 — compiler-negative, ArrayDeque vs indexed List:**
```java
import java.util.ArrayDeque;
class N02 {
  int probe() { return new ArrayDeque<Integer>().get(0); } // no get(int)
}
```

**R01 — mutable subList backing (API-defined):**
```java
var parent = new java.util.ArrayList<>(java.util.List.of(1, 2, 3));
var view = parent.subList(1,3);
view.set(0,67);
System.out.println(parent); // expected [1, 67, 3]
view.clear();
System.out.println(parent); // expected [1]
```

**R02 — fixed-size array-backed vs mutable copy:**
```java
String[] data = {"a","b"};
var fixed = java.util.Arrays.asList(data);
fixed.set(0,"z");
System.out.println(data[0]); // expected z
// fixed.add("c"); // expected runtime UnsupportedOperationException
var copy = new java.util.ArrayList<>(fixed);
copy.add("c"); // allowed; data.length unchanged
```

**R03 — Iterator state (использовать self-contained main при G2):**
```java
var list = new java.util.ArrayList<>(java.util.List.of(1,2,3));
var it = list.iterator();
// it.remove(); // IllegalStateException (for this mutable iterator)
it.next();
it.remove();
System.out.println(list); // expected [2, 3]
```

**R04 — ListIterator cursor:**
```java
var list = new java.util.ArrayList<>(java.util.List.of(1,2));
var cursor = list.listIterator(1);
cursor.add(67);
System.out.println(list); // expected [1, 67, 2]
System.out.println(cursor.previous()); // expected 67
```

**R05 — snapshot iterator (одно-поточное наблюдение):**
```java
var xs = new java.util.concurrent.CopyOnWriteArrayList<>(java.util.List.of(1,2));
var it = xs.iterator();
xs.add(67);
while(it.hasNext()) System.out.print(it.next()); // expected "12" (not 67)
```

**R06 — ArrayDeque empty and null:**
```java
var dq = new java.util.ArrayDeque<Integer>();
System.out.println(dq.pollFirst()); // expected null
// dq.add(null); // runtime NullPointerException
// dq.removeFirst(); // runtime NoSuchElementException
```

**R07 — fail-fast caveat:** взять `ArrayList`, создать `Iterator`, структурно изменить список вне него, затем `next()`: конкретная реализация обычно диагностирует CME. **Не** считать это безусловным обещанием API или правильной стратегией синхронизации; не создавать race-test с ожидаемым строгим stdout.

**R08 — unsupported iterator remove:** `List.of(1).iterator().remove()` — `UnsupportedOperationException`; проверить одновременно различие с `IllegalStateException` собственного итератора изменяемого `ArrayList`.

**Состояние evidence:** snippets встроены в Markdown, часть использует JShell-style top-level statements и не является самостоятельно компилируемым `javac File.java` классом. Это **план контрпримеров**, а не готовый G2 runner. До публикации complete examples / expected output: локальный `javac --release 21`, `java`, диагностические категории negative cases — **NOT RUN**. CI репозитория не компилирует код внутри Markdown.

## Связи, target paths и педагогическая практика

**Действующие локальные Markdown-ссылки из `docs/plans/`:** [COL-CONTRACTS](../../knowledge/02-collections/contracts.md), [COL-HASH](../../knowledge/02-collections/hash-collections.md), [план C1](COL-CONTRACTS-HASH.md), [CORE-ARRAYS](../../knowledge/01-java-core/arrays.md), [CORE-GENERICS](../../knowledge/01-java-core/generics.md), [CORE-EQUALITY](../../knowledge/01-java-core/equality.md), [CORE-UTIL](../../knowledge/01-java-core/utilities.md), [CORE-IMMUTABLE](../../knowledge/01-java-core/immutability.md), [CORE-STREAMS](../../knowledge/01-java-core/streams.md), [уже опубликованный C1 runnable](../../examples/collections/contracts-hash/README.md). Эти пути проверены в опубликованном `main` на исходном BASE.

**Предлагаемые, не существующие и поэтому без фиктивных ссылок:** `knowledge/02-collections/sequential.md`, `knowledge/02-collections/iteration.md`; при пользе `examples/collections/sequential-iter/`, отдельные `negative/N01.java`, `negative/N02.java`. Будущие связки — ID `COL-SORTED`, `COL-HEAP`, `COL-CONCURRENT`, `COL-TRADEOFFS`, `CON-JMM`, `TEST-JUNIT`; пока не утверждать об их готовности.

**Предложение роли 03 после принятого G1:** один компактный `SequentialIteratorProbe` с детерминированными проверками `subList`, `Arrays.asList`, `ArrayDeque` edge cases, `ListIterator` cursor и COW snapshot, без измерения nanos и без теста на конкретный HashMap порядок. Изолировать 2 compiler-negative source. Если результаты не требуют отдельного runner, держать короткий код в статье; отдельный Gradle-модуль, обязательное упражнение и JUnit по числу вопросов **не нужны**. Техническая структура и контракт согласуются 02/03, не назначаются ролью 01.

## G1 acceptance: что необходимо проверить и кому передать

1. Проверить P0 строк CS/CI по Java21 API; для спорных методов указать предусловия, результаты и exceptions; для I-подробностей подтвердить pinned source revision. Поддержка `null`, `Sequenced*`, `subList` и тип итерации **по конкретному классу**, не по одному общему ярлыку.
2. Обязательная ретроспективная проверка независимых C-вопросов после написания G2; перед G1 проверить, что C1–C4 не приписывают спецификации несуществующую гарантию. Вынести спорную «fail-safe» терминологию как проблему C-источников.
3. **Git/CI:** G1 отдельная ветка/PR с изменением **только** этого `docs/plans/COL-SEQUENTIAL-ITER.md`; BASE и HEAD подтвердить, exact CI/NOT RUN и Git diff. `COVERAGE/CURRICULUM`, Java код, workflow и предыдущие C1 документы **не изменять**.
4. **Gates:** независимый 04 G1 review на точном HEAD, затем 04 → 00 для ACCEPTED/BLOCKED и guarded merge. Только 00 открывает G2 поручения 02 и 03. Не отмечать `COL-SEQUENTIAL` или `COL-ITER` Implemented по одному исследовательскому PR.

### Отдельно помеченные неопределённости

- Конкретные байты на linked node, коэффициент роста массива, cost под определённой VM/GC, weakly-consistent exact outcome при гонке — **не установлены** и не нужны для базового G1.
- Непосредственная проверка точных stdout/diagnostics для N01–N02 и R01–R08 — **NOT RUN**. Javadoc по Java 21 и просмотр I не равны локальному выполнению.
- GUI-проверка Obsidian, Mermaid и отдельная локальная Gradle-сборка документа — **NOT RUN**. Реальные CI свидетельства после создания PR проверять отдельно.
