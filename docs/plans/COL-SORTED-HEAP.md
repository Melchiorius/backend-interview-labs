# C3 Collections G1 — COL-SORTED + COL-HEAP (Java SE 21)

**Статус:** G1 RESEARCH PROPOSED; [Issue #230](https://github.com/Melchiorius/backend-interview-labs/issues/230), программа [#213](https://github.com/Melchiorius/backend-interview-labs/issues/213). **Исходный main/BASE:** `86b7e8d745351b041ed2d43bf2c252ed5237dc71`. **Java:** Java SE 21. **Результат:** предварительная матрица исследований для 02/03/04; этот файл **не** является учебными статьями, проверенным runnable, G1 PASS или статусом Implemented.

## 1. Обещание и границы двух curricular ID

**COL-SORTED.** Читатель объясняет естественный и пользовательский порядок через `Comparable/Comparator`, его корректность и отличие от `equals/hashCode`, выбор `TreeMap/TreeSet`, соседние ключи (`lower/floor/ceiling/higher`), навигацию, включённые/исключённые границы диапазонов и backed views, стоимость операций с оговорками о цене сравнения. Java 21 `SequencedSet/SequencedMap` не означает поддержку произвольного изменения позиции сортированного элемента.

**COL-HEAP.** Читатель объясняет `PriorityQueue` как очередь с приоритетом, invariant «голова — минимальная по заданному порядку», различает `peek/poll` и итерацию, предсказывает исключения пустой очереди/null/type, сравнивает операции и их стоимость. Внутренний heap и `siftUp/siftDown/heapify` — модель конкретной OpenJDK 21; exact array layout и порядок равноприоритетных элементов — **не** портативный контракт.

**Не включать в C3 как готовое содержание:** полное JMM/atomicity, `PriorityBlockingQueue` и concurrent queues (`COL-CONCURRENT`), benchmark/требования к памяти конкретной JVM (`COL-TRADEOFFS`). Контракты hash/set и iterator/subList из опубликованных C1/C2 использовать только как предпосылки. P0 — собственное обязательное объяснение и контрпример будущего материала; P1 — существенная оговорка/подраздел; P2 — точное специализированное продолжение. P0/P1/P2 не являются рейтингом частот или градацией Middle/Senior.

## 2. Источники, версии, иерархия доверия

**A — нормативные контракты Java SE 21/JLS 21 (конкретные страницы):**
- [Comparable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Comparable.html), [Comparator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html), [SortedMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SortedMap.html), [NavigableMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/NavigableMap.html), [SortedSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SortedSet.html), [NavigableSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/NavigableSet.html).
- [TreeMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html), [TreeSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html), [SequencedMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedMap.html), [SequencedSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedSet.html). Отличать методы базового `Map/Set` от методов навигации.
- [Queue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Queue.html), [PriorityQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html), [PriorityBlockingQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/PriorityBlockingQueue.html) **только как boundary**.
- [BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html) (`compareTo` против `equals`), [Object.equals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#equals(java.lang.Object)), [JLS 21 §15](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html) (типы/выражения, при нужде для compiler-negative). Указывать точный метод, когда важны `NPE/CCE/IAE/UOE`; обобщённый `SortedMap` не определяет все внутренние детали `TreeMap`.

**I — конкретный OpenJDK 21 source (не Javadoc-гарантии):** pinned Git revision `890adb6410dab4606a4f26a942aed02fb2f55387`.
- [TreeMap.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/TreeMap.java): красно-чёрное дерево, `getEntry`, `rotateLeft/rotateRight`, `fixAfterInsertion`, range views. Подробности ребалансировки — I; публичная логарифмическая стоимость `containsKey/get/put/remove` подтверждается отдельно Javadoc TreeMap.
- [TreeSet.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/TreeSet.java): backing `NavigableMap`, `add`, `descendingSet`, `subSet`.
- [PriorityQueue.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/PriorityQueue.java): `queue[]`, `siftUp/siftDown`, `heapify`, `grow`; реализационная форма двоичной кучи и capacity growth **не** контракт `Queue`. При `heapify` рассмотреть bottom-up построение с O(n) **как анализ реализации/алгоритма**, а не обещание конкретного public constructor для всех Queue.
- Содержание файлов просмотрено через GitHub и расположение перечисленных методов подтверждено на указанном source SHA; **локальный JDK/байткод не собраны**.

**B — официальный объясняющий слой:** [dev.java Java Collections Framework](https://dev.java/learn/api/collections-framework/) и [Java SE21 API framework overview](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/doc-files/coll-reference.html). Пояснение не является альтернативной нормой Javadoc 21. Если страница dev.java говорит о более новой Java, сверять с A21.

**C — реально обнаруженные независимые интервью-вопросы, не авторитет для ответа:**
- **C1 / Baeldung:** [Java Collections Interview Questions](https://www.baeldung.com/java-collections-interview-questions), вопросы о `Comparable/Comparator`, различии HashSet/TreeSet, TreeSet-backed ordering; в старых примерах встречаются comparators через subtraction — не переносить их как безопасные.
- **C2 / teamlead:** [Java interview questions README на `main`](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md), точные вопросные формулировки: «What is the difference between Treeset and Hashset», «What restriction is to add to Treeset», «Can Treeset store null», «What tree lies in the implementation of Treeset», «What is the difficulty of searching for a key method in the Treemap collection», «What is the Comparable interface for», «Which allows you to make priorityqueue», «What is the “sorting” of SortedMap». Это фиксация вопросов, не проверенная статистика их частоты. Pinned source SHA `4c0784024412904625e1455189d3c05cba07bebf`.
- **C3 / in28minutes:** [CollectionHierarchy.java](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/src/main/java/com/in28minutes/java/collections/CollectionHierarchy.java), вопросы «How to decide between HashMap and TreeMap?» и «What is the difference between a HashSet and a TreeSet?», декларации `SortedMap/NavigableMap/SortedSet/NavigableSet/TreeSet/PriorityQueue`. Старое описание API не учитывает Java21 Sequenced*; исправлять через A21, а не перепечатывать.
- **C4 / Chernyshov:** [репозиторий Java interview questions](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers) — **адресная проверка конкретного C3-вопроса не выполнена; C GAP**, не приписывать этому источнику строки ниже.
- Независимая повторная C-выборка по **готовым статьям G2** обязательна; первоначальная матрица может пропустить вопросы и edge cases. Метки C1/C2/C3 ниже — provenance темы вопроса, не нормативное подтверждение.

## 3. COL-SORTED: матрица CT (Tree/comparison)

| ID | P | Вопрос и необходимый ответ будущей статьи | Контрпример / проверяемая граница | Evidence |
|---|---|---|---|---|
| CT-01 | P0 | `Comparable.compareTo` задаёт естественный порядок, `Comparator.compare` — переданный; отрицательный/нулевой/положительный **знак**, не обязательные -1/0/1 | `Integer.compare(a,b)` вместо `a-b`; `comparator()` возвращает null у natural-order TreeMap | A Comparable/Comparator/TreeMap; C1/C2 |
| CT-02 | P0 | Корректный comparator: антисимметрия знака, транзитивность, согласованность нулевой эквивалентности; для Comparable аналогично | `(a,b)->a-b` ломается при overflow (MAX_VALUE и -1); циклический comparator не задаёт корректный порядок | A Comparable/Comparator; C1 |
| CT-03 | P0 | Равенство по `compare==0` определяет уникальность TreeSet и эквивалентность ключей TreeMap, даже если `equals` различает объекты | `TreeSet<>(BigDecimal("1.0"), BigDecimal("1.00"))` имеет один элемент; в HashSet по equals — два | A TreeSet/TreeMap/BigDecimal; C1/C2 |
| CT-04 | P0 | `compare==0` при `equals==false`: TreeMap/TreeSet имеют well-defined собственную sorted semantics, но нарушают общий `Map/Set` equality contract; consistency-with-equals желательна | `Comparator.comparing(String::length)` объединяет разные строки одинаковой длины; не выдавать это за «поломку дерева» | A SortedSet/SortedMap/TreeMap |
| CT-05 | P0 | `TreeMap` и `TreeSet` — navigation ordered by key/element, не insertion order; отсутствие индексов и случайных позиций | `TreeSet.of` не существует; `TreeSet` не поддерживает доступ `get(0)`; `LinkedHashMap` сохраняет иной вид порядка | A TreeMap/TreeSet/SortedMap |
| CT-06 | P0 | `lower < key`, `floor <= key`, `ceiling >= key`, `higher > key`, границы `first/last`, `pollFirst/pollLast` | По {10,20,30} для 20: lower=10, floor=20, ceiling=20, higher=30; null/NoSuchElementException на empty различать по методу | A NavigableMap/NavigableSet |
| CT-07 | P0 | `headMap/tailMap/subMap`, `headSet/tailSet/subSet`: inclusive/exclusive bounds и backed views; ограничения `put/add` вне range | `subMap(10,true,30,false)` содержит 10,20, не 30; вставка 40 через range view — IllegalArgumentException | A NavigableMap/NavigableSet |
| CT-08 | P0 | `descendingMap/descendingSet` и `reversed()` Java21 — обратные views, не независимые копии, не новый comparator of equality; операции и порядок на backing | Изменение допустимого элемента через descending view видно исходной структуре; `TreeMap.putFirst/putLast` выбрасывают UOE (порядок определяется ключами) | A TreeMap/TreeSet/SequencedMap/SequencedSet |
| CT-09 | P0 | TreeMap Javadoc: гарантированно O(log n) для `containsKey/get/put/remove`, TreeSet — для `add/remove/contains`, но цена одного compare может быть нетривиальной | Comparator, сравнивающий длинные строки, имеет не O(1) сравнений; не объявлять любой `SortedMap` красно-чёрным деревом | A TreeMap/TreeSet; I |
| CT-10 | P0 | `null` и несовместимые типы: у natural-order TreeMap/TreeSet null запрещён (NPE), несовместимые элементы дают CCE; custom comparator может поддерживать null при совместимости | `new TreeSet<Integer>().add(null)` — NPE; `new TreeSet<>(Comparator.nullsFirst(...))` может включать null | A TreeMap/TreeSet/Comparator |
| CT-11 | P0 | Mutable ordering keys: изменение сравниваемых полей после вставки нарушает инварианты поиска, нельзя рассчитывать на корректный lookup/iteration | Изменить поле сортировки у уже сохранённого key; результат не обещать одинаковым для любой формы mutation | A TreeMap/TreeSet; I |
| CT-12 | P1 | Map.Entry: `firstEntry/lastEntry/lowerEntry/.../poll*` дают snapshots, тогда как iterator `entrySet()` имеет связанность с коллекцией и ограничения lifetime | `firstEntry().setValue(v)` UOE; не хранить итераторную entry как вечный live view после модификации карты | A TreeMap/Map.Entry |
| CT-13 | P1 | Заявленная red-black tree реализация OpenJDK21: rotation/recolor для балансировки и внутренний указатель parent; источник I | Не выдавать rotation count, конкретную форму дерева, память узла или путь поиска как Java21 API | I TreeMap.java |
| CT-14 | P1 | Comparator chaining, `thenComparing`, `naturalOrder/reverseOrder/nullsFirst`, consistency при выбранном ключе сортировки | `Comparator.comparing(Person::name)` не гарантирует уникальность разных Person с одинаковым name | A Comparator |
| CT-15 | P1 | Сравнение HashMap/TreeMap, HashSet/TreeSet и LinkedHashMap: порядок vs стоимость vs equality, диапазонные операции и null policy | Смешать `hashCode` и `compareTo` как одну политику уникальности — ошибка; не говорить «TreeMap всегда быстрее» | A classes; C2/C3 |
| CT-16 | P2 | Range-view composition, reverse bounds, пустые виды, вложенные views, bad endpoints и неожиданные IAE | `subMap` from>to — IAE; различить bound validity и отсутствие фактических элементов | A NavigableMap |
| CT-17 | P2 | Red-black tree details, serialization, traversal internals и iterator fail-fast best effort | Не считать точный цвет узлов или memory footprint переносимым контрактом; итераторы не заменяют синхронизацию | I TreeMap; A TreeMap |
| CT-18 | P2 | Типы `Comparable` и raw comparators, компиляция vs runtime ClassCastException | Неверный generic type в `TreeMap<Integer,String>.put("x","y")` — compiler-negative; raw map может отложить проблему до runtime | A JLS/TreeMap |

**Условия для G2:** для каждого CT P0 нужен раздел с механизмом + маленький контрпример (не только таблица API). Особое внимание consistent-with-equals, bounded view и Java21 Sequenced*. У `TreeMap` прямое позиционное `putFirst/putLast` не поддерживается, хотя она `SequencedMap`.

## 4. COL-HEAP: матрица CP (PriorityQueue)

| ID | P | Вопрос и необходимый ответ будущей статьи | Контрпример / проверяемая граница | Evidence |
|---|---|---|---|---|
| CP-01 | P0 | `PriorityQueue` — очередь приоритетов: head — наименьший элемент по natural order или Comparator; это **не** FIFO и не полностью отсортированный список | `offer(67),offer(2),offer(5)` -> `peek()==2`; порядок iterator не определён | A PriorityQueue/Queue; C1/C2 |
| CP-02 | P0 | Natural vs custom comparator, reverse order для max-priority, осторожно с overflow | `new PriorityQueue<>(Comparator.reverseOrder())` -> максимум первым; `(a,b)->b-a` опасен | A PriorityQueue/Comparator |
| CP-03 | P0 | `offer/add` добавляют, `poll/remove()` удаляют голову, `peek/element()` смотрят голову; на пустой очереди пары null/exception | `new PriorityQueue<>().peek()==null`; `element()` throws NoSuchElementException, `poll()==null`, `remove()` throws | A Queue/PriorityQueue |
| CP-04 | P0 | `PriorityQueue` не допускает null; при natural order элементы должны быть взаимно сравнимы; ошибочный comparator или тип — исключения согласно API | `q.offer(null)` -> NPE; incompatible raw type может привести к CCE | A PriorityQueue |
| CP-05 | P0 | Итератор и `spliterator` не гарантируют sorted traversal; последовательность `poll()` идёт по приоритетам | Нельзя сравнить `new ArrayList<>(pq)` с отсортированным эталоном; poll-loop с уникальными приоритетами — другой сценарий | A PriorityQueue |
| CP-06 | P0 | Дубликаты допустимы, tie priority не задаёт стабильный порядок; Comparator sign 0 не обязан означать equals | В очередь можно добавить два различных по equals объекта с одинаковым приоритетом; их относительный poll order не гарантирован | A PriorityQueue |
| CP-07 | P0 | Javadoc PriorityQueue: `offer/add/poll/remove()` O(log n), `peek/element/size` O(1), `contains/remove(Object)` O(n); явно различать две перегрузки `remove` | `pq.remove(67)` — поиск конкретного значения, не логарифмическое удаление головы | A PriorityQueue |
| CP-08 | P0 | Heap invariant: родитель не больше детей (для min-heap) в реализации OpenJDK21; `siftUp/siftDown` восстанавливают invariant после offer/poll | Форма `queue[]` и tie placement меняются и не документированы публичным API | I PriorityQueue.java |
| CP-09 | P0 | `PriorityQueue` не thread-safe; concurrent `PriorityBlockingQueue` — отдельный контракт (C4), не переносить туда bounded capacity по умолчанию | `PriorityQueue` в двух потоках без защиты не даёт guarantees; race-тест не доказывает ни безопасность, ни отсутствие | A PriorityQueue/PriorityBlockingQueue |
| CP-10 | P0 | Выбор PQ/TreeSet/TreeMap: PQ для повторяющейся головы и допуска duplicates, дерево для ordered membership/range navigation; HashMap для lookup | `contains` и remove-by-object в PQ линейны, TreeSet contains логарифмичен; PQ не заменяет NavigableSet | A PriorityQueue/TreeSet |
| CP-11 | P1 | `heapify` и построение из коллекции: OpenJDK21 bottom-up построение O(n) при обычных сравнениях vs n последовательных вставок O(n log n) | Не утверждать все публичные конструкторы или generic Queue имеют идентичный путь и стоимость; comparator cost отдельно | I PriorityQueue.java |
| CP-12 | P1 | Top-k как сценарий с ограниченным heap размером k; поддержание k лучших за O(n log k) при сравнении O(1) | PQ сама по себе не bounded; ограничение размера и критерий выбрасывания реализует алгоритм пользователя, не сам PriorityQueue | A PriorityQueue; алгоритм |
| CP-13 | P1 | Изменение поля priority у уже добавленного объекта нарушает порядок heap до явного удаления/переустановки; автоматической reheapify нет | Mutable task.priority: `peek` не обязан отражать новую приоритетность без повторного построения/обновления | A PriorityQueue; I |
| CP-14 | P1 | `toArray`, `iterator.remove` и fail-fast best effort; sorted representation только отдельной сортировкой | `Arrays.sort(pq.toArray())` — сортирует копию, не меняя priority order самой очереди | A PriorityQueue |
| CP-15 | P2 | Сложность роста capacity, memory overhead и comparator exceptions; спецификация не фиксирует capacity-growth policy | Не утверждать «на 50% всегда» или строгое время/байты под любой JVM | I PriorityQueue; A |
| CP-16 | P2 | Удаление произвольного элемента в I: поиск позиции, sift-down/up; secondary costs, fail-fast и duplicate tie placement | `remove(Object)` нельзя считать O(log n), даже если восстановление heap потом логарифмично | A PriorityQueue; I |
| CP-17 | P2 | Контроль сложных comparator, consistency, non-transitive policies, comparator throws | Неправильный comparator может нарушить ожидаемую политику; без корректного полного порядка heap guarantee не применим как ожидается | A Comparator/PriorityQueue |

**Критический teaching pitfall:** нельзя писать «PriorityQueue сортирует элементы» без разделения head-order, poll-order и iterator-order. В Java21 Javadoc прямо указано отсутствие гарантии порядка iterator.

## 5. Сложность и точные границы обещаний

| Структура/операция | Обоснованная оценка | Предпосылки и ограничения |
|---|---|---|
| TreeMap get/put/remove/containsKey | O(log n) по TreeMap API | Количество сравнений; сравнение ключей может быть дорогим; не заявлять O(log n) реального wall time при любом comparator |
| TreeSet add/remove/contains | O(log n) по TreeSet API | Уникальность по compare==0, а не обязательно Object.equals |
| TreeMap/TreeSet range navigation | Навигационный поиск в сбалансированном дереве (O(log n) при стандартном comparator) | Для views итерация/обход диапазона зависит от числа посещённых элементов; не переносить bound на все методы `SortedMap` |
| PriorityQueue offer/add/poll/remove() | O(log n) в implementation note API21 | При росте массива отдельная операция может иметь copy-cost; различать амортизированную/указанную в Javadoc сложность и worst-case конкретного запуска |
| PriorityQueue peek/element/size | O(1) по API21 | Не включает пользовательский comparator |
| PriorityQueue contains/remove(Object) | O(n) по API21 | В отличие от удаления головы; может сравнивать элементы |
| PriorityQueue heapify | O(n) для bottom-up construction в OpenJDK21 при O(1) compare | I/алгоритм, не обещание для всех Queue/всех constructors и не benchmark |
| Хранение/latency | Без универсальной оценки в байтах/нс | Узлы TreeMap, heap array, GC/JIT/cache locality и варианты VM требуют отдельного measurement |

## 6. Исследовательские контрпримеры: expected, не результаты запуска

**Все следующие фрагменты предназначены для G1 reasoning; локальные `javac --release 21`, `java`, результаты stdout и negative diagnostics — NOT RUN.** Некоторые фрагменты используют top-level JShell-style statements; для G2 03 должен оформить самодостаточные классы/сценарии с запуском и expected files. Compiler-negative отделены от runtime.

**N01 — compiler-negative (generic API):**
```java
import java.util.TreeMap;
class N01 {
  void test() {
    TreeMap<Integer,String> m = new TreeMap<>();
    m.put("wrong key type", "value"); // incompatible types at compile time
  }
}
```

**N02 — compiler-negative (нет позиционного доступа в TreeSet):**
```java
import java.util.TreeSet;
class N02 {
  int test() {
    return new TreeSet<Integer>().get(0); // cannot find symbol get(int)
  }
}
```

**R01 — compareTo/equals BigDecimal:**
```java
var a = new java.math.BigDecimal("1.0");
var b = new java.math.BigDecimal("1.00");
System.out.println(a.equals(b)); // expected false
System.out.println(a.compareTo(b)); // expected 0
var tree = new java.util.TreeSet<java.math.BigDecimal>();
tree.add(a);
tree.add(b);
System.out.println(tree.size()); // expected 1
```

**R02 — overflow comparator trap (демонстрировать на отдельных входах):**
```java
java.util.Comparator<Integer> bad = (a,b) -> a - b;
java.util.Comparator<Integer> good = Integer::compare;
System.out.println(bad.compare(Integer.MAX_VALUE, -1)); // expected negative (overflow)
System.out.println(good.compare(Integer.MAX_VALUE, -1)); // expected positive
```

**R03 — Navigation и bounded view:**
```java
var m = new java.util.TreeMap<Integer,String>();
m.put(10,"a"); m.put(20,"b"); m.put(30,"c");
System.out.println(m.lowerKey(20));  // expected 10
System.out.println(m.floorKey(20));  // expected 20
System.out.println(m.ceilingKey(20));// expected 20
System.out.println(m.higherKey(20)); // expected 30
var range = m.subMap(10,true,30,false);
range.remove(20);
System.out.println(m.containsKey(20)); // expected false, backed view
// range.put(40,"out"); // expected IllegalArgumentException (runtime)
```

**R04 — Java21 SequencedMap limitation:**
```java
java.util.SequencedMap<Integer,String> sorted = new java.util.TreeMap<>();
sorted.put(10, "a");
// sorted.putFirst(5, "b"); // expected UnsupportedOperationException (runtime)
System.out.println(sorted.firstEntry().getKey()); // expected 10
```
Не путать snapshot `firstEntry` с итераторными entries; запретить в объяснении `setValue()` на returned snapshot.

**R05 — PriorityQueue, head vs iteration:**
```java
var q = new java.util.PriorityQueue<Integer>();
q.offer(67); q.offer(2); q.offer(5);
System.out.println(q.peek()); // expected 2
System.out.println(q.poll()); // expected 2
System.out.println(q.poll()); // expected 5
System.out.println(q.poll()); // expected 67
// for(var x:q) { ... } -- order NOT specified, do not assert exact stdout
```

**R06 — PriorityQueue null/empty and duplicates:**
```java
var q = new java.util.PriorityQueue<Integer>();
System.out.println(q.poll()); // expected null
// q.remove(); // expected NoSuchElementException
// q.offer(null); // expected NullPointerException
q.offer(67); q.offer(67);
System.out.println(q.size()); // expected 2
```

**R07 — custom order as equivalence:**
```java
var s = new java.util.TreeSet<String>(java.util.Comparator.comparingInt(String::length));
s.add("aa"); s.add("bb");
System.out.println(s.size()); // expected 1 (compare==0 despite !equals)
```
Дать отдельно объяснение, почему это не эквивалент HashSet, и что comparator должен удовлетворять математическим ограничениям порядка.

**R08 — mutable ordering key:** намеренно изменить поле comparator-visible объекта после вставки в TreeSet/TreeMap, но **не** закреплять единственный stdout на всех изменениях. Объяснить нарушение инварианта и безопасные альтернативы immutable key / remove + reinsert.

**I01 — исходник heapify/rotations:** аналитический walkthrough `PriorityQueue.siftUp/siftDown/heapify` и `TreeMap.rotateLeft/rotateRight/fixAfterInsertion` с контрастом к A. Не использовать private reflection для измерения структуры как «Java contract» и не рассчитывать на цвет дерева/конкретное расположение элементов в array.

## 7. Реальные зависимости, планируемые файлы, практика

**Реальные локальные Markdown-ссылки из `docs/plans/`:** [COL-CONTRACTS](../../knowledge/02-collections/contracts.md), [COL-HASH](../../knowledge/02-collections/hash-collections.md), [COL-SEQUENTIAL](../../knowledge/02-collections/sequential.md), [COL-ITER](../../knowledge/02-collections/iteration.md), [C1 G1](COL-CONTRACTS-HASH.md), [C2 G1](COL-SEQUENTIAL-ITER.md); [CORE-EQUALITY](../../knowledge/01-java-core/equality.md), [CORE-GENERICS](../../knowledge/01-java-core/generics.md), [CORE-IMMUTABLE](../../knowledge/01-java-core/immutability.md), [CORE-ARRAYS](../../knowledge/01-java-core/arrays.md), [CORE-UTIL](../../knowledge/01-java-core/utilities.md). Существование этих файлов проверено в исходном `main`. Повторить проверку при G2 после перемещения/синхронизации.

**Будущие цели — ТОЛЬКО ID / НЕ ссылки на готовые файлы:** `knowledge/02-collections/sorted.md`, `knowledge/02-collections/priority-queue.md`, при педагогической пользе `examples/collections/sorted-heap/`, негативные `N01.java/N02.java`. Полное содержимое `COL-CONCURRENT`, `COL-TRADEOFFS`, `CON-JMM` и `TEST-JUNIT` не считается опубликованным по факту C3.

**Предложение для 03 после решения 00:** один компактный `SortedHeapProbe` на Java21, если он даёт пользу сверх короткого inline-кода: `TreeMap` navigation/backed range, `BigDecimal` equality, comparator overflow, `PriorityQueue` poll vs iterator-order non-guarantee, пустая очередь и ошибочные типы/null; изолированные negative `javac` случаи N01/N02. Проверять **инварианты, не случайный tie order, не конкретную форму красно-чёрного дерева, не nanoseconds**. `README` со standalone command/expected stdout, возможно `verify.sh`; не требовать Gradle/JUnit или отдельного упражнения формально. Соответствие формата реальным CI и методам сборки согласовывают 02/03/06 на G2, не 01 в G1.

## 8. Приёмка, честные доказательства и вопросы независимому reviewer

1. Пройти каждую CT/CP строку: P0 действительно закрывает соответствующий читательский вопрос, ограничения Comparator и `compare==0` не подменяются `equals`. У Java21 `Sequenced*` показывать конкретные поддерживаемые/неподдерживаемые методы. Для PQ не обещать порядок `iterator` и tie stability.
2. **Evidence A/I/C:** Java SE21 Javadoc/JLS со ссылкой на конкретные классы, source `openjdk/jdk21` pinned `890adb6410dab4606a4f26a942aed02fb2f55387`; три проверенных C sources, четвёртый C GAP. После G2 обязательно повторить независимый C coverage search и отметить найденные новые P0/P1, если есть.
3. **NOT RUN:** локальные `javac --release 21`, `java`, `gradle`, negative compile diagnostics, JUnit, benchmark, GUI Obsidian/IDE. Исследовательские expected outputs выведены из A/I и **не подтверждены запуском**. Плановая CI на PR HEAD отдельно проверяется в GitHub: зелёная Java build CI проекта **не компилирует Markdown snippets**.
4. **Git scope:** один research-only `docs/plans/COL-SORTED-HEAP.md`, точный исходный/живой BASE и HEAD, без редактирования `COVERAGE/CURRICULUM`, Java-кода, статей, Gradle/CI или прошлых C1/C2 планов. Не заявлять `COL-SORTED/COL-HEAP Implemented`.
5. **Gate:** независимый 04 G1 exact HEAD `PASS/CHANGES` с проверкой источников и контрпримеров → 04 адресно передаёт 00 → только 00 G1 acceptance/guarded merge и отдельные поручения 02/03 G2.

**Отдельные проверки для 04:** comparator sign/antisymmetry, BigDecimal `compareTo`/equals, исключения null/natural ordering и comparator-specific exceptions; snapshot vs live Entry, range-view bounds, Java21 SequencedMap `putFirst/putLast`, PQ `remove()` vs `remove(Object)`, точные предпосылки heapify/complexity и validity of C provenance.
