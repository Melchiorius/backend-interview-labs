# Контракты коллекций Java 21: Collection, Map и упорядоченные представления

**Раздел:** `COL-CONTRACTS`. **Java:** 21. **Предварительное исследование:** [COL-CONTRACTS-HASH](../../docs/plans/COL-CONTRACTS-HASH.md). Следующая тема — [хеш-коллекции](hash-collections.md). **Проверяемая практика:** [Java 21 ContractsHashProbe, N01/N02 и команды запуска](../../examples/collections/contracts-hash/README.md).

Java Collections Framework задаёт **контракты хранения и доступа к группам объектов**. Интерфейс определяет, какие операции доступны и какие результаты допустимы, а реализация — дополнительные ограничения, стоимость и, иногда, порядок. Например, `Set` запрещает одинаковые по `equals` элементы; это не означает, что любой `Set` сохраняет порядок добавления или разрешает `null`.

## Иерархия интерфейсов: что от чего наследуется (CC-01)

Ниже стрелка `→` направлена **от дочернего интерфейса к его непосредственному родительскому интерфейсу**: запись `List → SequencedCollection` означает `List extends SequencedCollection`. Линии `├─` и `└─` только группируют дочерние интерфейсы для чтения схемы, не заменяют стрелки наследования и не изображают движение объектов. Перечислены основные интерфейсы Java 21, а не реализации.

```text
Collection → Iterable
├─ SequencedCollection → Collection
│  └─ List → SequencedCollection
├─ Set → Collection
│  └─ SequencedSet → Set, SequencedCollection
└─ Queue → Collection
   └─ Deque → Queue, SequencedCollection

Map (отдельная иерархия)
└─ SequencedMap → Map
```

`Map<K,V>` не расширяет `Collection`: у карты **отображения ключ → значение**, а не элементы одного типа. Методы `keySet()`, `values()`, `entrySet()` возвращают коллекционные представления, но сама карта от этого не становится `Collection`. В Java 21 `List` непосредственно расширяет `SequencedCollection`, а `Deque` — одновременно `Queue` и `SequencedCollection`. Непосредственные родительские интерфейсы `SequencedSet` — `Set` и `SequencedCollection`.

Код **не компилируется** (N01): тип `HashMap` не реализует `Collection`. Ошибка обнаруживается компилятором, не при запуске:

```java
import java.util.*;
class InvalidMapHierarchy {
    Collection<String> values = new HashMap<String, Integer>(); // ошибка типов
}
```

## Семантика интерфейсов и выбор реализации (CC-02, CC-07, CC-13)

| Контракт | Главная идея | Повторы / порядок | Пример и ограничения |
|---|---|---|---|
| `List<E>` | позиционная последовательность, `get(index)` | равные значения обычно допустимы, порядок позиций существенен | `ArrayList`; отдельные реализации могут ограничивать элементы, включая повторы |
| `Set<E>` | уникальность относительно `equals` | повтор равного элемента не добавляется; общего порядка нет | `HashSet` без гарантии порядка, `LinkedHashSet` с encounter order |
| `Queue<E>` | операции очереди `offer/poll/peek` | порядок определяется реализацией, **не всегда FIFO** | `PriorityQueue` выбирает по приоритету, а не по времени вставки |
| `Deque<E>` | операции с обоими концами | гарантирован первый/последний по собственной последовательности | `ArrayDeque`; стек и очередь на общем интерфейсе |
| `Map<K,V>` | соответствие уникальных ключей значениям | дубликатов ключей нет; общий порядок обхода не обещан | `HashMap`, `LinkedHashMap`, `TreeMap` с разными свойствами |

В этой таблице **не** утверждается единая политика `null` для интерфейса. Например, `HashMap` допускает `null`, `Map.of` запрещает, а `ArrayDeque` не принимает `null`. Если задача требует позиционного доступа, порядок или приоритет — сначала выбрать нужный контракт, затем оценить реализации. Устройство `ArrayList` и других последовательных коллекций уже раскрыто в [COL-SEQUENTIAL](sequential.md); [сортированные структуры](sorted.md) и [приоритетные очереди](priority-queue.md) раскрывают другие правила порядка.

```java
List<Integer> list = new ArrayList<>();
list.add(67);
list.add(67);
System.out.println(list);  // [67, 67]

Set<Integer> set = new HashSet<>();
System.out.println(set.add(67)); // true
System.out.println(set.add(67)); // false
System.out.println(set.size());  // 1
```

`SequencedCollection` (Java 21) задаёт **определённый порядок обхода элементов (encounter order)** и операции на концах (`getFirst`, `getLast`, `addFirst`, `removeLast` и т. п., с возможными необязательными мутациями). `SequencedSet` объединяет ограничения множества и порядок обхода; `SequencedMap` — упорядоченные пары и операции над первой/последней записью. `reversed()` возвращает **обратное представление**, а не новую независимую копию. Определённый порядок обхода **не означает сортировку**.

```java
SequencedMap<String, Integer> original = new LinkedHashMap<>();
original.put("first", 1);
original.put("second", 2);
SequencedMap<String, Integer> reversed = original.reversed();
System.out.println(reversed.firstEntry().getKey()); // second
original.put("third", 3);
System.out.println(reversed.firstEntry().getKey()); // third
```

Это допустимый пример backed view именно для `LinkedHashMap`; обычная `HashMap` не реализует `SequencedMap`. `firstEntry()` возвращает снимок пары, а не живую `Map.Entry` для изменения исходной карты; не путать её с записями из `entrySet()`.

## Равенство элементов и коллекций (CC-03, CC-09)

`Object.equals` должен быть рефлексивным, симметричным, транзитивным и согласованным при неизменном состоянии, а также возвращать `false` для `null`. Если `a.equals(b)`, то `a.hashCode() == b.hashCode()`; **обратное неверно**. Нарушение контрактов ломает поиск в хеш-структурах. Подробный механизм: [равенство и hashCode](../01-java-core/equality.md).

Равенство **самих коллекций** тоже определяется контрактом: для `List` существенны элементы и их позиции, для `Set` — множество элементов, для `Map` — соответствия ключей значениям. Тип конкретной реализации обычно не обязан совпадать.

```java
System.out.println(List.of(1, 2).equals(List.of(2, 1))); // false
System.out.println(Set.of(1, 2).equals(Set.of(2, 1)));   // true
System.out.println(Map.of("a", 1).equals(Map.of("a", 1))); // true
```

## Модификации, исключения и политика null (CC-04, CC-05, CC-13)

Операция изменения, объявленная интерфейсом, может не поддерживаться конкретной коллекцией: при её вызове возможен `UnsupportedOperationException`, иногда даже если состояние не изменилось. Сам вызов при этом допустим **с точки зрения компилятора**; исключение возникает **во время выполнения**, а не на этапе компиляции.

```java
List<Integer> fixed = List.of(67);
fixed.add(68); // при выполнении UnsupportedOperationException
```

`List.of`, `Set.of`, `Map.of` не допускают `null` и создают немодифицируемые коллекции. `HashMap` допускает один `null`-ключ и `null`-значения. У `Map.get(k)` результат `null` **не доказывает отсутствие ключа**:

```java
Map<String, Integer> map = new HashMap<>();
map.put("present", null);
System.out.println(map.get("present") == null);       // true
System.out.println(map.get("missing") == null);       // true
System.out.println(map.containsKey("present"));       // true
System.out.println(map.containsKey("missing"));       // false
```

Для `Queue` есть пары операций с разным способом сообщать о неуспехе. `remove()` и `element()` на пустой очереди бросают `NoSuchElementException`, тогда как `poll()` и `peek()` на пустой очереди возвращают `null` **по контракту `Queue`**. Допустимость `null` как элемента зависит от конкретной реализации: большинство очередей его запрещает, чтобы результат `null` не путали с отсутствием элемента. `ArrayDeque.add(null)` бросает `NullPointerException`. Возврат `false` от `offer` может означать отказ принять элемент при ограниченной вместимости — это не общий сценарий для всех очередей.

## Map views: наблюдаемые связи с исходной картой (CC-06, CC-11)

`map.keySet()`, `map.values()`, `map.entrySet()` — **представления, связанные с исходной картой (backed views)**. Это не независимые копии: изменения исходной карты видны в представлении, пока соблюдаются правила его использования. Удаление через поддерживаемую операцию представления изменяет карту. Добавление непосредственно в `keySet()` не предоставляется как способ создать неизвестное значение; `entrySet` предоставляет итерацию пар.

```java
Map<String, Integer> map = new HashMap<>();
map.put("a", 67);
Set<String> keys = map.keySet();
map.put("b", 68);
System.out.println(keys.contains("b")); // true
keys.remove("a");
System.out.println(map.containsKey("a")); // false
```

Запись `Map.Entry`, полученная из `entrySet()` при обходе, в общем случае связана с исходной картой **во время соответствующей итерации**; у изменяемой `HashMap` поддерживаемый `setValue` может изменить значение в карте. **Если исходную карту изменить после получения такой записи, поведение записи не определено**, кроме изменения через её собственный поддерживаемый `setValue` — ограничение не сводится только к структурным изменениям. Не храните полученную при обходе запись как независимый долгоживущий снимок. Не всякая `Map.Entry` изменяема: `Map.entry("a", 67).setValue(68)` бросает `UnsupportedOperationException`. Для независимого представления пары используйте `Map.entry` или `Map.Entry.copyOf`, учитывая запрет `null` у этих фабрик.

## Немодифицируемое представление, копия и изменяемые объекты (CC-08, CC-15)

`Collections.unmodifiableList(backing)` возвращает **немодифицируемое представление, связанное с исходным списком**: через обёртку список менять нельзя, но изменения через исходную ссылку остаются видны в представлении. `List.copyOf(backing)` создаёт немодифицируемый результат, который **не отражает последующие структурные изменения источника**; для уже подходящего немодифицируемого источника разрешено переиспользование экземпляра. `copyOf` — не глубокая копия объектов внутри. То же концептуальное различие важно для `Set.copyOf` и `Map.copyOf`; набор гарантий и поведение с `null` сверяют по конкретной фабрике.

```java
var source = new ArrayList<>(List.of(1, 2));
var view = Collections.unmodifiableList(source);
var copy = List.copyOf(source);
source.add(67);
System.out.println(view.size()); // 3
System.out.println(copy.size()); // 2
```

Результаты фабрик `List.of` / `Set.of` / `Map.of` относятся к **объектам, определяемым значением (value-based)**: не полагайтесь на идентичность ссылок (`==`) у разных вызовов фабрик и не используйте такие экземпляры как монитор синхронизации. Важны элементы и контракт коллекции, а не то, переиспользован ли объект. `Set.of(1, 1)` бросает `IllegalArgumentException`; `Set.copyOf` может выбрать один из дубликатов без обещания конкретного представителя. `Map.of` запрещает повтор ключа; `Map.copyOf` сохраняет независимость отображений от последующих модификаций исходной карты, но не глубокую неизменяемость значений. См. [иммутабельность](../01-java-core/immutability.md) и [утилиты/фабрики Java](../01-java-core/utilities.md).

## Generics и итераторы: необходимые границы (CC-10, CC-12)

`List<String>` **не** является подтипом `List<Object>`: иначе через переменную `List<Object>` можно было бы записать `Integer` в список строк. Пример N02 **не компилируется**:

```java
List<Object> objects = new ArrayList<String>(); // несовместимые параметры типа
```

`List<? extends Object>` позволяет читать элементы как `Object`, но в общем случае не разрешает добавлять произвольный ненулевой объект; `? super String` поддерживает запись `String` и чтение как `Object`. Подробности: [generics](../01-java-core/generics.md), [стирание типов](../01-java-core/erasure.md).

`Iterator.remove()` — необязательная операция, поддерживаемая не каждым итератором и только при выполнении его правил. Изменение коллекции в обход итератора может нарушить условия обхода. `ConcurrentModificationException` для fail-fast реализаций обнаруживается **по возможности**, это не замена синхронизации и не универсальная гарантия. Состояние итератора, представления подсписков и три модели обхода разобраны в [COL-ITER](iteration.md).

## Особые границы и самоссылка (CC-14)

По договору `Map` **не может содержать саму себя в качестве ключа**, но может содержать себя в качестве значения. Последний случай не делает безопасными рекурсивные `equals`, `hashCode` и `toString` для любых составных графов. Не предполагайте, что каждое нарушение запрета self-key обязано немедленно диагностироваться определённым исключением. Аналогично самоссылочные `Collection` имеют отдельные ограничения операций, которые явно предупреждаются в API.

## Проверка перед собеседованием: механизм, а не лозунг

- Почему `Map` не `Collection`, хотя у карты есть `keySet()`? Чем `SequencedMap` отличается от произвольного `Map`?
- Почему `get(k) == null` недостаточно, чтобы проверить отсутствие отображения?
- Какие операции запрещает `List.of`, и почему `Collections.unmodifiableList` не snapshot?
- Почему `Set` не гарантирует порядок, а `List` может иметь ограничения на дубликаты в конкретной реализации?
- Что происходит при удалении ключа через `keySet` и насколько долго можно хранить полученную `Map.Entry`?

**Продолжение:** [COL-SEQUENTIAL](sequential.md) — стоимость операций над списками и двусторонней очередью, [COL-ITER](iteration.md) — состояния итераторов и модели обхода. [COL-SORTED](sorted.md) раскрывает навигацию по ключам, а [COL-HEAP](priority-queue.md) — извлечение по приоритету. [COL-CONCURRENT](concurrent.md) объясняет конкурентные структуры, а [COL-TRADEOFFS](tradeoffs.md) — выбор и методику измерений.

## Источники и статус доказательств

Нормативные источники **A**: [Iterable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Iterable.html), [Collection](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collection.html), [List](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html), [Set](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Set.html), [Queue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Queue.html), [Deque](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Deque.html), [Map](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html), [SequencedCollection](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedCollection.html), [SequencedSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedSet.html), [SequencedMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedMap.html), [Object](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html) и [JLS §4.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.5).

Поясняющий источник **B**: [Oracle о Sequenced API](https://docs.oracle.com/en/java/javase/21/core/creating-sequenced-collections-sets-and-maps.html). Интервью-кандидаты **C**, повторная сверка 24.11.2026: [Baeldung](https://www.baeldung.com/java-collections-interview-questions), [teamlead — pinned](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md), [in28minutes](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/src/main/java/com/in28minutes/java/collections/CollectionHierarchy.java). Интервью-подборки не определяют корректность ответов. Дополнительная повторная выборка C4 (ранее GAP): [Q-CHERNYSHOV — закреплённая ревизия](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/597e08112ab6a57d6eac8e3c70c070da606dae73/eng/README.md), раздел «HashMap / equals / hashCode» с вопросами о бакетах, коллизиях, record и изменяемых ключах. Это **реальные интервью-вопросы**, не источник нормативных гарантий Java 21; прежний статус GAP в неизменённом историческом G1-плане не переписывается задним числом.

**Продолжение C2:** [ArrayList, LinkedList и ArrayDeque](sequential.md) раскрывают стоимость индексного доступа, связанных представлений и операций на концах; [итераторы Java 21](iteration.md) — курсор и три разные модели обхода. Оба раздела развивают контракты, не изменяя их.

**Фактическая проверка:** роль 03 создала и выполнила [самостоятельный пример `ContractsHashProbe` с N01/N02](../../examples/collections/contracts-hash/README.md): 57 строк вывода и две отдельные ожидаемо неуспешные компиляции на OpenJDK 21.0.12.1; это **не 57 JUnit-тестов**. Интегрированный [CI #45303677619](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45303677619) завершился успешно на HEAD `59456ce7f38480af7aed44612cd5bb52a5955bcd`, включая шаг COL. Эти доказательства относятся к указанной ревизии, **не** автоматически к последующим редакционным коммитам. Роль 02 не запускала локально все встроенные фрагменты статьи; отдельная компиляция всех фрагментов и визуальная проверка Obsidian/IDE — **NOT RUN**.
