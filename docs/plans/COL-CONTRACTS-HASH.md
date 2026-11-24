# C1 Collections G1: COL-CONTRACTS + COL-HASH — исследовательский план до статей

**Статус:** G1 RESEARCH PROPOSED; **Issue:** [#214](https://github.com/Melchiorius/backend-interview-labs/issues/214); **программа:** [#213](https://github.com/Melchiorius/backend-interview-labs/issues/213). **Целевая версия:** Java SE 21; детали реализации — исходники OpenJDK 21 (ветка \`jdk21\`, не API-контракт). **Исходный BASE исследовательской ветки:** \`b0ad3976e3872a13c28ee24828ceb5e745cee6be\`. Исходный документ является **планом**, а не готовой статьёй, готовым упражнением или результатом независимого ревью.

## 1. Обещание читателю и границы

После связанных материалов читатель сможет: (1) правильно выбрать контракт \`Collection/List/Set/Queue/Deque/Map\`, отличить API и отдельные реализации; (2) предсказать допускаемые дубликаты, порядок, \`null\`, результат модификаций, исключения и поведение views; (3) объяснить поиск в \`HashMap/HashSet\` через \`hashCode\` и \`equals\`, коллизии и загрузку, причины нестабильного поиска по изменённому ключу; (4) оценить сложность **при сформулированных предпосылках**, а не повторить «всегда O(1)»; (5) отделить Java SE 21 API от конкретного устройства OpenJDK 21 и корректно сравнить обычную map с concurrent-map на границе темы.

**Состав C1:** \`COL-CONTRACTS\` (иерархия, контракты, мутации, views, sequenced API) и \`COL-HASH\` (hash-based map/set, коллизии, capacity/load factor, ошибки ключей). Не пытаться превратить план в энциклопедию всех коллекций.

**Отдельные curricular ID, без обещания полного покрытия здесь:** \`COL-SEQUENTIAL\` (ArrayList/LinkedList/ArrayDeque и locality), \`COL-ITER\` (полное устройство итераторов и consistency), \`COL-SORTED\` (TreeMap/TreeSet, comparator), \`COL-HEAP\` (PriorityQueue), \`COL-CONCURRENT\` (CHM, concurrent queues, memory model), \`COL-TRADEOFFS\` (workload/benchmark). Здесь даётся лишь необходимая граница, без ссылки на отсутствующий Markdown-файл.

### Уровни важности

**P0** — ответ обязателен в основной статье, с проверяемым механизмом и примером. **P1** — существенная оговорка или отдельный подраздел/переход к реально существующей статье; не скрывать пробел. **P2** — точный хвост/специальная тонкость. Это не ярлыки Middle/Senior и не статистика частот собеседований. Обозначения доказательств: **A** — Java SE 21 Javadoc/JLS; **I** — конкретный исходник OpenJDK 21 (реализация); **B** — официальный поясняющий материал; **C** — интервью-подборка, лишь кандидат вопроса.

## 2. Источники и provenance

### A — нормативные гарантии версии 21

- [Java SE 21 \`Iterable\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Iterable.html), [\`Collection\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collection.html), [\`List\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html), [\`Set\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Set.html), [\`Queue\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Queue.html), [\`Deque\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Deque.html), [\`Map\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html) и [\`Map.Entry\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.Entry.html).
- Java 21: [\`SequencedCollection\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedCollection.html), [\`SequencedSet\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedSet.html), [\`SequencedMap\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedMap.html). Не предполагать, что произвольный \`Set\` или \`Map\` автоматически имеет encounter order.
- Реализации: [\`HashMap\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html), [\`HashSet\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashSet.html), [\`LinkedHashMap\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedHashMap.html), [\`Collections\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html), [\`Arrays\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html). Фабрики [\`List.of\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#of()), [\`Set.of\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Set.html#of()) и [\`Map.of\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html#of()) рассматривать по API конкретной версии.
- [\`Object.equals/hashCode\`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html), [JLS 21 §4.5 parameterized types](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.5), [§4.10.2 subtyping](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.10.2), [§5.1.5 widening reference conversion](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.5). Объяснение инвариантности — норма языка, не поведение HashMap.

### I — OpenJDK 21, НЕ обещание всем JDK

- [Исходник \`HashMap.java\` в OpenJDK jdk21](https://github.com/openjdk/jdk21/blob/master/src/java.base/share/classes/java/util/HashMap.java): \`hash(key)\` (распределение через \`h ^ (h >>> 16)\`, специальный случай \`null\`), \`resize()\`, \`treeifyBin()\`, \`TREEIFY_THRESHOLD=8\`, \`UNTREEIFY_THRESHOLD=6\`, \`MIN_TREEIFY_CAPACITY=64\`. Эти конкретные константы и реализация бакетов **не предписаны контрактом \`Map\`**. Порог 8 не означает «ровно на восьмой вставке всегда дерево»: в малой таблице может произойти resize; treeification зависит от фактической структуры бакета и ёмкости.
- [Исходник \`HashSet.java\`](https://github.com/openjdk/jdk21/blob/master/src/java.base/share/classes/java/util/HashSet.java). Javadoc тоже подтверждает, что текущий HashSet backed by HashMap; не выводить из этого нормативный контракт любого Set.

**Важная оговорка о provenance:** ссылка на \`jdk21/master\` задаёт ветку линейки JDK 21, а не immutable source commit. Перед финальным G1 REVIEW/публикацией при необходимости закрепить exact Git commit SHA исходника для побайтовой воспроизводимости. Проверка исходника выше проведена чтением содержимого через GitHub; локальная сборка JDK не запускалась.

### B — официальные пояснения

- [Oracle Java 21: Creating Sequenced Collections, Sets, and Maps](https://docs.oracle.com/en/java/javase/21/core/creating-sequenced-collections-sets-and-maps.html). Использовать для рисунка порядка и обратного view, но обязательный контракт брать из Java SE 21 API.
- [dev.java Learn](https://dev.java/learn/) — дополнительная навигация, не замена версии 21.

### C — независимые вопросы, НЕ подтверждение правильности ответов

- [Baeldung Java Collections Interview Questions](https://www.baeldung.com/java-collections-interview-questions): вопросы о hierarchy, ArrayList/LinkedList, HashMap buckets/resize, equality, fail-fast, complexity. **Контрольная ловушка:** формулировку «fail-fast сразу выбросит CME» сверять с Javadoc: best-effort detection, а не гарантия.
- [teamlead/java-interview-questions, README](https://github.com/teamlead/java-interview-questions/blob/master/README.md): фактически найдены вопросы «How Hashmap is organized», «Why MAP stands apart in the hierarchy», «What is Failfast», «What is Deque», «How Hashmap is related to SET». Заявленные составителями частоты не считать независимо подтверждёнными.
- [in28minutes — \`CollectionHierarchy.java\`](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/src/main/java/com/in28minutes/java/collections/CollectionHierarchy.java): конкретные вопросы «Does Map implement Collection interface? Why?», «difference between HashMap and Hashtable?», «difference between a Set and a List», «difference between HashSet and TreeSet», «Generics in Collections Framework?». Их примеры написаны до Sequenced API; не переносить модель и детали реализации без Java 21 сверки.
- [ViacheslavChernyshov/java-interview-questions-and-answers](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers) — дополнительный кандидат C, **не выполнена адресная проверка конкретного Collections-вопроса: C GAP**, не приписывать источник отдельным строкам.

Первая выборка C проверена по трём независимым источникам, выборочные вопросы в таблицах помечены C1/C2/C3: **C1=Baeldung, C2=teamlead, C3=in28minutes**. Повторная внешняя выборка перед готовой статьёй обязательна (исходная матрица может оказаться неполной).

## 3. Матрица COL-CONTRACTS

| ID | P | Вопрос и необходимый ответ в статье | Проверяемый контрпример / граница | Основание |
|---|---|---|---|---|
| CC-01 | P0 | Строгая карта \`Iterable → Collection → List/Set/Queue → Deque\` с отдельной ветвью \`Map<K,V>\`; \`Map\` не наследует \`Collection\` | \`Map<?,?> m = new HashMap<>(); Collection<?> c = m;\` не компилируется; views map — коллекции | A Collection/Map; C2/C3 |
| CC-02 | P0 | \`List\` допускает повторы и позиционный доступ; \`Set\` уникален относительно \`equals\`, \`Queue\` и \`Deque\` задают операции с концами, но не одинаковый порядок всех реализаций | List \`[67,67]\` vs Set \`{67}\`; не обещать FIFO для \`PriorityQueue\` | A List/Set/Queue/Deque; C1/C3 |
| CC-03 | P0 | Договор \`Object.equals/hashCode\`; симметрия, транзитивность, согласованность, равные объекты имеют равный hash | Одинаковый hash **не** означает равенство, \`==\` — не то же, что \`equals\`; связь с CORE-EQUALITY | A Object/Set/Map; C1 |
| CC-04 | P0 | Optional mutator: \`add/put/remove/clear\` могут быть unsupported; \`null\` допускается не везде; даже неизменяющая операция может выдавать UOE по контракту | \`List.of(67).add(1)\` — UOE; \`List.of((Integer)null)\` — NPE; не путать compile/runtime | A Collection/List/Map; C1 |
| CC-05 | P0 | \`Map.put/get/containsKey\`: отсутствие ключа и отображение на \`null\` нельзя всегда различить по \`get\` | \`new HashMap<String,Integer>()\`: \`put("x",null)\`, \`get("x")==null\` и \`containsKey("x")==true\` | A Map/HashMap; C1 |
| CC-06 | P0 | \`keySet/values/entrySet\` — views карты, не независимые snapshot; документировать поддерживаемые операции и последствия модификаций | Удаление через \`keySet().remove(k)\` меняет карту; \`entrySet().iterator().remove()\` и \`setValue()\` имеют реализационные ограничения | A Map |
| CC-07 | P0 | Java 21 SequencedCollection/Set/Map: encounter order, доступ к концам, reversed view; не путать с индексами и сортировкой | \`new LinkedHashMap<>().reversed()\` — обратный view; \`HashMap\` не становится SequencedMap | A Sequenced*; B |
| CC-08 | P0 | \`List.of/Set.of/Map.of\`, \`copyOf\` и \`Collections.unmodifiable*\`: unmodifiable view vs snapshot-like unmodifiable result, ограничения элементов/ключей и null | \`Collections.unmodifiableList(mutable)\` отражает правку backing; \`List.copyOf(mutable)\` не отражает последующую правку списка, но не обеспечивает deep immutability | A List/Set/Map/Collections |
| CC-09 | P0 | Collection equality: List зависит от порядка и элементов, Set от состава, Map от key-value mappings; одинаковая реализация не обязательна | \`List.of(1,2).equals(List.of(2,1)) == false\`; \`Set.of(1,2).equals(Set.of(2,1)) == true\` | A List/Set/Map |
| CC-10 | P1 | Generics: инвариантность \`List<String>\` относительно \`List<Object>\`; wildcard \`? extends / ? super\` и типы ключей | \`List<Object> x = new ArrayList<String>();\` compiler-negative, корректный \`List<? extends Object>\` read-only в общем случае | A JLS §4/§5; C3 |
| CC-11 | P1 | \`Map.Entry\`, \`setValue\`, \`entrySet\`: жизненный цикл записи, допуски mutable/immutable entries, ограничения при структурных изменениях | \`Map.entry("x",67).setValue(68)\` UOE; не обещать безопасное использование Entry после изменения backing map | A Map.Entry/Map |
| CC-12 | P1 | Итерация, structural modification, \`Iterator.remove\`; fail-fast **best effort** и невзаимозаменяемость с thread safety | \`ConcurrentModificationException\` возможно, но не обязательное средство синхронизации; полный цикл — COL-ITER | A HashMap/Collection |
| CC-13 | P1 | Null policy по конкретным реализациям, \`Queue\` special return values vs exceptions (\`poll/remove\`, \`peek/element\`) | \`new ArrayDeque<>().add(null)\` NPE; \`poll()\` пустой queue возвращает null, \`remove()\` бросает NoSuchElementException | A Queue/Deque |
| CC-14 | P2 | Self-reference, mutable graph и возможная рекурсия \`equals/hashCode/toString\`; запрет на самоссылающиеся структуры как общее правило неправилен | Self-containing map может порождать рекурсию или исключение в некоторых операциях; конкретный метод/реализацию проверять отдельно | A Map/Collection |
| CC-15 | P2 | Условия итерации и immutable/value-based factory instances; identity и сериализация не должны становиться частью бизнес-контракта | \`List.of(...)\` value-based: не синхронизироваться по экземпляру; \`==\` разных фабрик не является содержательным тестом | A List |

### Какие P0 обязательны в будущей COL-CONTRACTS статье

Иерархию изложить с Java 21 \`Sequenced*\` (обновить обычные старые схемы), показать конкретную матрицу **тип интерфейса → допустимые повторы → порядок/доступ → типичные реализации → \`null\` не у интерфейса вообще, а у реализации**. Отдельно короткий код с map views, фабриками и ошибками разных стадий выполнения. CC-10/12 — связь с готовыми CORE-GENERICS и будущим COL-ITER, но минимум для понимания — здесь.

## 4. Матрица COL-HASH

| ID | P | Вопрос и необходимый ответ в статье | Проверяемый контрпример / граница | Основание |
|---|---|---|---|---|
| CH-01 | P0 | Вставка и поиск: \`hashCode → hash/spread → bucket → equals\` при совпадении кандидатов, индекс в текущей таблице | \`hashCode\` одинаков, \`equals\` различен: оба ключа должны сосуществовать | A HashMap/Object; I; C1/C2 |
| CH-02 | P0 | Ключи и инвариант равенства/hash: значение hash для ключа должно оставаться совместимым с поиском, пока он хранится | Mutable key после вставки: \`containsKey\` может стать false; не превращать наблюдение в гарантию поведения для всех мутаций | A Map/Object; C1 |
| CH-03 | P0 | \`HashMap\` не даёт гарантий порядка; \`HashSet\` backed by HashMap, уникальность основана на equality | \`entrySet\` порядок не проверять по фиксированной строке stdout; \`LinkedHashMap\` — отдельный порядок | A HashMap/HashSet |
| CH-04 | P0 | Null: HashMap позволяет **один** null-key и null values; различать \`get\` и \`containsKey\` | \`Map.of("a",null)\` — NPE; \`new HashMap<>().put(null,null)\` допустим | A HashMap/Map |
| CH-05 | P0 | Capacity, load factor, threshold и resize — компромисс lookup/memory/iteration; объяснить условия дефолтных параметров | Javadoc: итерация HashMap пропорциональна capacity + size, а не строго O(size); \`initialCapacity\` не size | A HashMap; I |
| CH-06 | P0 | Collision chain vs tree bins; thresholds и функция spread в OpenJDK 21, а не обещание любой JDK | \`TREEIFY_THRESHOLD=8, MIN_TREEIFY_CAPACITY=64\`: при меньшей таблице возможен resize вместо treeify; тип бакета не observable contract | I HashMap.java |
| CH-07 | P0 | Оценка ожидаемого \`get/put/remove\` при хорошей hash distribution; реальные предпосылки и худшие случаи | \`O(1)\` в среднем **при условии хорошего распределения**, не жёсткая worst-case граница; pathologies и cost \`equals\` | A HashMap; I |
| CH-08 | P0 | Замена значения существующего ключа, отличие отсутствия от mapping to null, логика \`putIfAbsent/computeIfAbsent/merge\` | \`computeIfAbsent\` вызывается и при mapped-to-null; возвращение null не устанавливает значение; \`merge\` требует non-null argument value | A Map/HashMap |
| CH-09 | P0 | Обычная \`HashMap\` не thread-safe; default \`Map.compute*\` не обещает атомарность | Два потока с check-then-act — не атомарная операция; не выдавать \`compute\` обычной HashMap за CHM; полный вопрос COL-CONCURRENT | A HashMap/Map; C2 |
| CH-10 | P1 | Качество hashCode, pathological collisions, значительная стоимость \`equals\`; balanced tree — не абсолютная защита | Tree bins зависят от key comparison/tie break в конкретной реализации, не гарантируют любой attacker-controlled workload | I HashMap.java |
| CH-11 | P1 | Fail-fast iterator/structural vs value replacement: best-effort CME, без thread safety | Изменение value на существующем ключе обычно не structural в HashMap; интерпретировать по конкретной реализации, не делать универсальный тест | A HashMap; I |
| CH-12 | P1 | HashSet.add возвращает boolean и скрывает представление values; не смешивать map keySet view и независимый HashSet | \`new HashSet<>(...).add(67)\` повторно false; \`map.keySet().remove(k)\` меняет map, но отдельный HashSet нет | A HashSet/Map |
| CH-13 | P1 | HashMap vs Hashtable/LinkedHashMap/TreeMap/IdentityHashMap: отличать sync, order, equality и null policy | Hashtable не допускает null; LinkedHashMap задаёт encounter order; IdentityHashMap использует reference equality; подробности соседних ID | A class Javadoc; C1/C3 |
| CH-14 | P1 | Исключения от user code в \`hashCode/equals/mappingFunction\`, ограничения модификации Map из callback | Callback, меняющий саму HashMap при \`computeIfAbsent\`, может привести к CME; не обещать транзакционный rollback | A Map/HashMap |
| CH-15 | P2 | Точная стоимость tree bins, \`Comparable\` и tie breaks, memory footprint у Node/TreeNode; версия/архитектура JDK | Не давать абсолютные байты/пороги latency без конкретной VM+GC+layout/измерения | I HashMap; граница COL-TRADEOFFS |
| CH-16 | P2 | Serialization, cloning, iteration after resize и попытки предсказывать порядок бакетов по текущему source | Даже совпадение порядка в одном OpenJDK 21 запуске не становится переносимым контрактом | A HashMap; I |

### Сложность: утверждение только с предпосылками

| Операция | Формулировка для будущей статьи | Ограничения |
|---|---|---|
| HashMap lookup/insert/remove | Ожидаемо близко к O(1) при достаточно равномерном hash distribution и разумной стоимости hashCode/equals | Ни API, ни единичный тест не гарантируют O(1) на **всех** ключах и нагрузках; resizing даёт отдельные дорогие операции |
| HashMap resize | В OpenJDK 21 перераспределение элементов таблицы; отдельный resize зависит от текущей ёмкости и количества узлов | Амортизацию обсуждать при росте и фиксированном load factor, не обещать постоянную стоимость каждой вставки |
| HashMap iteration | По Javadoc: время пропорционально capacity + size | Большая capacity может замедлить обход даже небольшой map |
| HashSet basic operations | По Javadoc постоянное ожидаемое время при хорошем распределении hash function | Использует backing HashMap; плохие ключи меняют картину |
| Tree bins | В OpenJDK 21 улучшают обработку значительных коллизий через дерево в подходящих случаях | Не объявлять строгие универсальные O(log n) для любого adversarial key и всех JDK; учитываются comparator/tie-break и equals |

**Никаких непроверенных точных цифр в наносекундах и байтах**. Для доказательства производительности нужен отдельный продуманный JMH/production workload с warmup, распределениями ключей, платформой, JDK и error bars — область COL-TRADEOFFS.

## 5. Контрпримеры G1 — планируемые и различимые по типу доказательства

**Все сниппеты ниже — исследовательские иллюстрации.** Автоматическая компиляция Markdown snippets, local \`javac --release 21\`, Java runtime и ожидаемый stdout **NOT RUN** в этой ветке. Именованные expected results взяты из API/JLS, не из непроведённого запуска.

**N01 compiler-negative, hierarchy:**
\`\`\`java
import java.util.*;
class N01 {
  Collection<String> c = new HashMap<String, Integer>(); // incompatible types
}
\`\`\`

**N02 compiler-negative, invariance:**
\`\`\`java
import java.util.*;
class N02 {
  List<Object> objects = new ArrayList<String>(); // incompatible types
}
\`\`\`

**R01 runtime, collision не равен equality:**
\`\`\`java
record Key(int id) {
  @Override public int hashCode() { return 67; }
}
var m = new java.util.HashMap<Key,String>();
m.put(new Key(1), "one");
m.put(new Key(2), "two");
System.out.println(m.size());              // expected 2
System.out.println(m.get(new Key(1)));       // expected one
\`\`\`
\`record\` синтезирует \`equals\` по полю \`id\`; hashCode переопределён намеренно. Вывод не проверен запуском.

**R02 runtime, null и отсутствие:**
\`\`\`java
var m = new java.util.HashMap<String,Integer>();
m.put("x", null);
System.out.println(m.get("x") == null);       // expected true
System.out.println(m.containsKey("x"));      // expected true
System.out.println(m.containsKey("missing"));// expected false
\`\`\`

**R03 runtime, mutable key:**
\`\`\`java
final class MutableKey {
  int id;
  MutableKey(int id) { this.id = id; }
  @Override public int hashCode() { return id; }
  @Override public boolean equals(Object o) {
    return o instanceof MutableKey other && id == other.id;
  }
}
var key = new MutableKey(1);
var map = new java.util.HashMap<MutableKey,String>();
map.put(key, "saved");
key.id = 67;
System.out.println(map.get(key)); // typical OpenJDK21 observation: null; not universal mutation guarantee
\`\`\`
Описать как нарушение предпосылки стабильного hash/equality. Важнее принцип, чем конкретное значение при всех вариантах изменения hash.

**R04 runtime, unmodifiable view vs copy:**
\`\`\`java
var src = new java.util.ArrayList<>(java.util.List.of(1,2));
var view = java.util.Collections.unmodifiableList(src);
var copy = java.util.List.copyOf(src);
src.add(67);
System.out.println(view.size()); // expected 3
System.out.println(copy.size()); // expected 2
\`\`\`

**R05 runtime, optional mutations:**
\`\`\`java
java.util.List.of(67).add(68);    // expected UnsupportedOperationException
// new java.util.HashMap<String,Integer>().put(null,null); // allowed
// java.util.Map.of("x", (Integer)null); // expected NullPointerException
\`\`\`
Ошибки компиляции N01/N02 нельзя считать результатом R01–R05 и наоборот.

**R06 reverse order:** использовать \`LinkedHashMap\` как \`SequencedMap\`; проверить, что \`reversed()\` — view, а не independent copy. Избегать проверки случайного порядка \`HashMap\`.

**R07 callback:** сравнить \`HashMap.computeIfAbsent\` при \`null\`/absent и \`ConcurrentHashMap.computeIfAbsent\` по документации; **многопоточные трассы не считать воспроизводимо доказанными**.

**I01 implementation probe (не обязательный тест):** намеренно создать больше восьми коллизий при ёмкости меньше 64 и отдельно при ёмкости не меньше 64, показать причину отсутствия/появления дерева через исходник \`treeifyBin\`, не использовать private reflection, illegal-access flags или разбор layout как публичный API. Поскольку визуальное внутреннее состояние не является API, prefer source walkthrough вместо «волшебного» stdout.

## 6. Карта существующих связей и будущая структура

Действующие локальные пути относительно **этого плана** (файлы существовали в \`main\` при предварительной проверке; при G2 сверить повторно):

- [CORE-EQUALITY](../../knowledge/01-java-core/equality.md) — \`equals/hashCode\`; [CORE-GENERICS](../../knowledge/01-java-core/generics.md) и [CORE-ERASURE](../../knowledge/01-java-core/erasure.md) — generics и runtime erasure.
- [CORE-IMMUTABLE](../../knowledge/01-java-core/immutability.md) — shallow/deep immutability; [CORE-UTIL](../../knowledge/01-java-core/utilities.md) — helpers/factories; [CORE-ARRAYS](../../knowledge/01-java-core/arrays.md) — массивы; [CORE-STREAMS](../../knowledge/01-java-core/streams.md) — streaming API.
- [Collections scaffold](../../labs/collections/README.md) — перечень тем, **не** реализация C1.

Предлагаемые будущие статьи/практика как **ID и target path**, без кликабельных ссылок на несуществующие файлы: \`knowledge/02-collections/contracts.md\`, \`knowledge/02-collections/hash-collections.md\`, при пользе \`examples/collections/contracts-hash/\`. Наличие двух curricular ID **не предписывает** ровно две статьи. Действующие будущие связи: \`COL-SEQUENTIAL\`, \`COL-ITER\`, \`COL-SORTED\`, \`COL-HEAP\`, \`COL-CONCURRENT\`, \`COL-TRADEOFFS\`, \`CON-JMM\`, \`TEST-JUNIT\`. Их реализацию подтверждать отдельно.

## 7. Практика: оценка необходимости, без G2-реализации

**Педагогически обосновано** предложить 03 один небольшой standalone Java21 \`ContractsHashProbe\` (или два, если независимые цели яснее) для deterministic checks: collisions+equals, null vs absent, views, unmodifiable copy/view, Sequenced view, mutable key с **точно обозначенными предпосылками**. Для негативных случаев полезны отдельные изолированные \`javac --release 21\` source cases; ожидание конкретной категории compiler error — после настоящего запуска и сохранения диагностик. README с честной командой \`javac/java\` или \`java <source>\`, expected stdout и \`verify.sh\` только если он упрощает проверяемость.

**Не требуется по умолчанию:** новый Gradle subproject, запуск реального race как «доказательство атомарности», JUnit на каждую строку, сравнение HashMap encounter order, benchmark на одном запуске, внедрение в общий CI до согласования 03/06. Отдельная задача/упражнение — только если помогает удержать конкретный инвариант без дублирования теории. Будущий runnable принадлежит G2 роли 03; этот PR его не создаёт.

## 8. G1 acceptance и честные границы evidence

- До написания статьи для каждого CC/CH вопроса указаны желаемый ответ и контрпример; \`P0\` не формален. После черновика 02 сопоставить текст и runnable с **каждой** строкой, дополнить актуальным C-search и отправить независимому 04.
- **Исследование источников:** Java SE 21 Javadoc/JLS и OpenJDK21 source (A/I) отделены от C. C-подборки 3 кандидатов проверены на реальные Collections-вопросы. Дополнительный C4 — GAP; внешняя ретроспективная проверка полноты текста ещё не проведена.
- **Команды:** локальные \`javac\`, \`java\`, \`gradle\`, тесты, Obsidian/IntelliJ preview — **NOT RUN**. Markdown code fences в текущем CI не компилируются автоматически. Не заявлять проверки N/R по фактическому runtime.
- **CI:** результат проверять по exact PR HEAD через GitHub Actions отдельно от успешного CI исходного \`main\`. До появления PR run — **NOT RUN/NOT OBSERVED**.
- **Содержательный gate:** 04 — independent G1 review exact SHA; затем 00 — ACCEPTED/BLOCKED/guarded merge. Роли 02/03 приступают к G2 лишь по отдельным поручениям 00. \`COVERAGE\` остаётся без \`COL-*\` Implemented до публикации G5/G6.

### Незакрытые вопросы для ревью 04

1. Достаточно ли P0 пояснено различие \`Seq*\` и контрактов существующих JDK21 интерфейсов, нет ли ложной общей гарантии порядка?
2. Нужны ли конкретные Java21 isolated compiler-negative и runtime traces как предварительное evidence до G1 acceptance? Пока честно NOT RUN.
3. Какие ограничения по \`Map.Entry\`, callback structural modification и complexity надо усилить в будущей статье? Сверить по конкретному Javadoc методу.
4. Source commit \`openjdk/jdk21\` для фрагментов hash/treeification желательно закрепить exact SHA перед окончательным принятием.
