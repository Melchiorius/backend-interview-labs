# Последовательные коллекции Java 21: ArrayList, LinkedList и ArrayDeque

**Проверяемый самостоятельный пример Java 21:** [SequentialIteratorProbe, эталонный вывод и два случая ожидаемых ошибок компиляции N01/N02](../../examples/collections/sequential-iter/README.md). **Тема:** `COL-SEQUENTIAL`. **Платформа:** Java SE 21. **Исследовательский план:** [C2 — COL-SEQUENTIAL + COL-ITER](../../docs/plans/COL-SEQUENTIAL-ITER.md). Связанные темы: [контракты коллекций](contracts.md), [массивы](../01-java-core/arrays.md), [равенство](../01-java-core/equality.md), [обход и итераторы](iteration.md). В тексте различаются **A — контракт Java 21 API**, **I — исходники конкретного OpenJDK 21** и **условия рабочей нагрузки**.

При выборе коллекции недостаточно спросить «что быстрее?». Важно, требуется ли индексный доступ, где выполняются вставки и удаления, известна ли позиция, сохраняется ли порядок, разрешён ли `null`, как происходит обход и сколько памяти допускается потратить. В частности, «вставка в `LinkedList` за O(1)» справедлива лишь **после нахождения узла/позиции**.

## Массив, размер и вместимость ArrayList (CS-01)

Массив `T[]` имеет фиксированную `length`: можно заменить элемент, но нельзя увеличить длину **того же** объекта массива. `ArrayList<E>` — изменяемый список, чья `size()` равна фактическому числу элементов. *Capacity* — вместимость внутреннего хранилища, а не логический размер. В частности, `ensureCapacity` резервирует место, но не создаёт доступных по индексам элементов.

~~~java
int[] fixed = new int[2];
fixed[0] = 67;
System.out.println(fixed.length); // 2

ArrayList<Integer> list = new ArrayList<>();
list.ensureCapacity(100);
System.out.println(list.size()); // 0
list.add(67);
System.out.println(list.size()); // 1
// list.get(1); // IndexOutOfBoundsException: индекс равен size()
~~~

Индекс `i` при чтении/замене должен удовлетворять `0 <= i < size()`, а при вставке `add(i, value)` разрешён также `i == size()`. `List<Integer>` хранит ссылки на объекты `Integer`, а не примитивы `int`. `ArrayList` допускает `null`, но необязательная поддержка отдельных операций/элементов у других реализаций `List` не следует из её поведения.

## ArrayList: доступ, амортизация и сдвиги (CS-02, CS-03)

`get(i)` и `set(i, value)` в `ArrayList` выполняются за постоянное время при допустимом индексе. Для добавления в конец при достаточной вместимости достаточно записать ссылку; при заполнении хранилища `ArrayList` расширяет его и копирует прежние ссылки. В длинной серии `add(e)` стоимость **амортизированно O(1)**, но отдельный вызов с расширением может быть O(n). Это не противоречие: амортизированная оценка распределяет редкую дорогую работу на множество вызовов.

В [исходнике OpenJDK 21 `ArrayList.java`](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/ArrayList.java) механизм виден в `grow` и копировании массива. **Конкретная формула увеличения capacity — реализационная деталь**, не нормативная гарантия `List` или всех JDK.

Вставка в начало/середину `add(index, e)` сдвигает элементы вправо, а `remove(index)` — влево для закрытия промежутка: O(n) в худшем случае, даже если индекс известен. `set(index, e)` лишь заменяет элемент, без изменения `size`.

~~~java
var a = new ArrayList<>(List.of(1, 2, 3));
a.add(0, 67);
System.out.println(a); // [67, 1, 2, 3]
System.out.println(a.remove(2)); // 2
System.out.println(a); // [67, 1, 3]
System.out.println(a.set(1, 68)); // 1, старое значение
~~~

У `List<Integer>` перегрузки `remove(int index)` и `remove(Object value)` ведут себя по-разному: `remove(1)` удалит позицию 1, а `remove(Integer.valueOf(1))` ищет равное значение. Это частая ошибка при сочетании перегрузок и автоупаковки.

## LinkedList: поиск узла не бесплатен (CS-04)

В OpenJDK 21 `LinkedList` — двусвязные узлы. `node(index)` начинает поиск с ближайшего конца, однако `get(index)` всё равно может требовать O(n) переходов. **Если узел уже найден** (например, итератор стоит в нужной позиции), перенастройка соседних ссылок для вставки/удаления может занимать O(1) в этой реализации. Вызов `list.add(list.size() / 2, x)` включает **поиск позиции** и потому не получает автоматического O(1).

~~~java
LinkedList<Integer> linked = new LinkedList<>(List.of(1, 2, 3));
var cursor = linked.listIterator(1); // позиция между 1 и 2
cursor.add(67);                      // вставка по известному курсору
System.out.println(linked);          // [1, 67, 2, 3]
System.out.println(linked.get(2));   // 2; поиск по индексу
~~~

Связные узлы выделяются как отдельные объекты и создают дополнительные переходы по ссылкам; непрерывный массив `ArrayList` обычно выгоден с точки зрения локальности доступа. Но **универсальное утверждение о наносекундах, количестве байтов на узел и частоте GC без замеров некорректно**: существенны JVM, распределение операций, размер данных и нагрузка. Сравнивайте *поиск позиции + изменение*, а не только стоимость перепривязки ссылок.

## ArrayDeque: два конца, очередь и стек (CS-05, CS-06)

`ArrayDeque` реализует `Deque`, **не `List`**: `get(int)` у него нет. В закреплённом [OpenJDK 21 `ArrayDeque.java`](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/ArrayDeque.java) элементы размещаются в **циклическом массиве** с позициями головы и хвоста. При переполнении хранилище расширяется. Операции с обоих концов обычно имеют **амортизированную O(1)** стоимость; поиск определённого элемента и его удаление из середины — другая задача. Точный начальный размер массива/способ роста — **I**, не обязательство `Deque`.

Для FIFO добавляем в хвост и извлекаем из головы; для LIFO используем `push/pop` на одном конце:

~~~java
Deque<Integer> q = new ArrayDeque<>();
q.addLast(1);
q.addLast(67);
System.out.println(q.removeFirst()); // 1: FIFO
System.out.println(q.removeFirst()); // 67

Deque<Integer> stack = new ArrayDeque<>();
stack.push(1);
stack.push(67);
System.out.println(stack.pop()); // 67: LIFO
~~~

При пустой очереди `poll()/peek()` возвращают `null` по контракту `Queue`, а `remove()/element()` бросают `NoSuchElementException`. Аналогично для концов `Deque`: `pollFirst()` возвращает `null`, `removeFirst()` выбрасывает исключение. `offer` при ограничении вместимости может сообщить об отказе через `false`, но у `ArrayDeque` нет фиксированного пользовательского предела ёмкости — остаются ресурсные ограничения. Извлечение пустой очереди — не ошибка компиляции.

~~~java
Deque<Integer> empty = new ArrayDeque<>();
System.out.println(empty.pollFirst()); // null
// empty.removeFirst(); // NoSuchElementException при выполнении
~~~

**N02 — пример, намеренно не проходящий компиляцию**: `new ArrayDeque<Integer>().get(0)` — метода `get(int)` нет. Это не то же, что выход за границы у индексного `ArrayList`.

## Политика null принадлежит реализации (CS-07)

`ArrayList` и `LinkedList` допускают `null` как элемент, `ArrayDeque` — **запрещает**: `add(null)` приводит к `NullPointerException`. Так `null` в результате `poll/peek` не путается с извлечённым элементом. Для `LinkedList`, используемого через `Deque`, API также рекомендует избегать `null` из-за этой неоднозначности: сам класс его принимает, но в очереди такой элемент затрудняет интерпретацию результата `poll`.

~~~java
List<String> list = new LinkedList<>();
list.add(null); // допустимо
Deque<String> deque = new ArrayDeque<>();
// deque.add(null); // NullPointerException во время выполнения
~~~

## subList: связанное представление, а не копия (CS-08)

`List.subList(fromIndex, toIndex)` возвращает **представление диапазона [fromIndex, toIndex)** исходного списка. Операции через него действуют на исходный список, включая структурное изменение. Нормативный Java 21 `List` оговаривает важную границу: семантика полученного представления становится **неопределённой**, если исходный список **структурно изменён не через это представление** (включая обходную модификацию другой части списка). Это не означает «обязательно случится CME» — наблюдаемое исключение в конкретном OpenJDK не надо превращать в контракт.

~~~java
var parent = new ArrayList<>(List.of(1, 2, 3));
List<Integer> view = parent.subList(1, 3);
view.set(0, 67);
System.out.println(parent); // [1, 67, 3]
view.clear();
System.out.println(parent); // [1]
~~~

В показанном `ArrayList` изменение **через представление** (`view.set`, `view.clear`) непосредственно изменяет исходный список `parent`: первая операция заменяет элемент, вторая удаляет весь выделенный диапазон. Если же исходный список не меняет размер и **через `parent.set()` заменяется элемент внутри выделенного диапазона**, соответствующий элемент представления также становится новым (это видно в проверяемом примере). Такая замена не является структурной модификацией. Напротив, изменение структуры исходного списка **в обход представления** нарушает условия `subList`; после него поведение представления не определено. Не сохраняйте подсписок как независимый снимок, если исходный список может независимо изменяться. Вопросы итераторов представления — в [COL-ITER](iteration.md).

## Arrays.asList, List.of и настоящая копия (CS-09, CS-14)

`Arrays.asList(array)` создаёт список **фиксированного размера, связанный с исходным массивом**. `set` заменяет элемент массива и наоборот, но `add/remove` меняют размер и поэтому вызывают `UnsupportedOperationException`. `List.of` возвращает немодифицируемую коллекцию, не принимающую `null`; `new ArrayList<>(fixed)` создаёт отдельную изменяемую структуру со своими слотами. **Копирование списка не означает глубокого клонирования содержащихся объектов.**

~~~java
String[] words = {"a", "b"};
List<String> fixed = Arrays.asList(words);
fixed.set(0, "z");
System.out.println(words[0]); // z
words[1] = "y";
System.out.println(fixed.get(1)); // y

List<String> independent = new ArrayList<>(fixed);
independent.add("c");
System.out.println(words.length); // 2
// fixed.add("c"); // UnsupportedOperationException
~~~

`Collections.unmodifiableList(backing)` возвращает немодифицируемое представление исходного списка `backing`: его нельзя изменять через обёртку, однако изменения через исходную ссылку видны при чтении обёртки. `List.copyOf(source)` — немодифицируемый результат, не отражающий последующие структурные изменения исходного списка (при корректных ненулевых элементах), но без глубокой копии изменяемых объектов. Сопоставление — [контракты коллекций](contracts.md) и [CORE-IMMUTABLE](../01-java-core/immutability.md).

## Java 21 Sequenced API и двойная роль LinkedList (CS-11, CS-13)

В Java 21 `List extends SequencedCollection`, `Deque extends Queue, SequencedCollection`. Поэтому у `List`/ `LinkedList` / `ArrayDeque` есть осмысленный **порядок обхода** и операции над первым/последним элементом. `reversed()` задаёт обратное **представление**, не сортировку и не глубокую копию.

`LinkedList` одновременно `List` и `Deque`: поддерживает позиционные операции, операции с концами, `ListIterator` для движения в обе стороны и `descendingIterator()` для обратного обхода. Вызов `get(i)` при этом **не становится O(1)**. Обратите внимание: не каждая реализация `List` поддерживает все необязательные модификации через `reversed()`.

## ensureCapacity, trimToSize и обход (CS-12)

`ArrayList.ensureCapacity(min)` используется, когда известен предполагаемый рост; `trimToSize()` пытается привести внутреннюю вместимость к текущему размеру. В конкретном OpenJDK 21 операции с ёмкостью могут влиять на механизм обнаружения изменений `modCount`, хотя число элементов не меняется. Нельзя строить корректность сохранённого итератора на предположении «если size не изменился, значит любой внутренний модификатор безвреден». Подробный `modCount` — деталь реализации, не гарантия общего интерфейса `Iterable`.

## Специализированные операции и границы стоимости (CS-15, CS-16)

`removeIf`, `toArray`, `sort`, `RandomAccess` и `Spliterator` имеют собственные предусловия и контракт. Маркер `RandomAccess` сообщает о поддержке быстрого произвольного доступа; **не** заставляет каждую реализацию обещать одинаковое фактическое время в любых условиях. У `ArrayList` и `LinkedList` разные особенности преобразования, обхода и массовых изменений. Подробный `Spliterator` — в [обходе коллекций](iteration.md); реальные измерения времени, объём памяти узлов, локальность доступа и влияние сборщика мусора (GC) — будущая тема `COL-TRADEOFFS`, а не установленные измерения C2.

## Сводка: задача важнее привычного названия структуры (CS-10)

| Задача | Обычно подходящий контракт/реализация | Обязательная оговорка |
|---|---|---|
| Частый индексный доступ | `ArrayList` | наличие элементов определяется size, а не capacity |
| Добавление преимущественно в конец | `ArrayList` | O(1) амортизированно; отдельное расширение внутреннего массива может быть дорогим |
| Вставки через уже известный курсор | `LinkedList` + `ListIterator` | поиск нужной позиции оплачивается отдельно; расположение узлов в памяти тоже имеет значение |
| FIFO или LIFO через концы | `ArrayDeque` | не поддерживает индексный доступ и `null` |
| Связанный диапазон | `subList` | после структурного изменения исходного списка не через представление поведение представления не определено |
| Изменяемая копия массива | `new ArrayList<>(Arrays.asList(array))` | структура независима, элементы копируются неглубоко |

## Вопросы для собеседования

1. Почему `ensureCapacity(100)` не разрешает `get(67)` при `size() == 0`?
2. Чем амортизированная O(1) для `add(e)` отличается от худшей стоимости отдельного расширения?
3. Почему `LinkedList.add(n/2, x)` нельзя считать O(1), а изменение связей на известной позиции можно?
4. Что произойдёт с `subList`, если менять исходный список снаружи? Почему ответ «CME гарантирован» неверен?
5. Как различить фиксированный размер `Arrays.asList`, немодифицируемую фабрику `List.of` и отдельную копию `ArrayList`?
6. Каким способом `ArrayDeque` реализует FIFO/LIFO и почему у него нет `get(int)`?

## Матрица покрытия C2: 16 из 16 CS

| G1 | Приоритет | Где раскрыто |
|---|---|---|
| CS-01 | P0 | Массив, size и capacity |
| CS-02 | P0 | Рост ArrayList и амортизация |
| CS-03 | P0 | Сдвиги add/remove |
| CS-04 | P0 | LinkedList: поиск против известной позиции |
| CS-05 | P0 | Deque и циклический массив как I; N02 |
| CS-06 | P0 | Queue/stack, пустота и исключения |
| CS-07 | P0 | Различия допуска null |
| CS-08 | P0 | subList, backed и undefined semantics |
| CS-09 | P0 | Arrays.asList vs List.of vs ArrayList copy |
| CS-10 | P0 | Выбор по нагрузке и границы реальных замеров |
| CS-11 | P1 | SequencedCollection/reversed |
| CS-12 | P1 | ensureCapacity/trimToSize/modCount |
| CS-13 | P1 | LinkedList как List/Deque |
| CS-14 | P1 | unmodifiable view vs copy |
| CS-15 | P2 | Memory/locality/GC — явно не измерены |
| CS-16 | P2 | removeIf/toArray/sort/RandomAccess/Spliterator |

## Источники и состояние доказательств

**A — Java SE21:** [ArrayList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html), [LinkedList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedList.html), [ArrayDeque](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayDeque.html), [List](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html), [Deque](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Deque.html), [Arrays.asList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html#asList(T...)), [SequencedCollection](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedCollection.html). **I — OpenJDK21:** исходники [ArrayList](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/ArrayList.java), [LinkedList](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/LinkedList.java), [ArrayDeque](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/ArrayDeque.java), revision `890adb6410dab4606a4f26a942aed02fb2f55387`. **C — темы интервью, не нормативное доказательство:** [Baeldung](https://www.baeldung.com/java-collections-interview-questions), [teamlead (pinned)](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md), [in28minutes](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/src/main/java/com/in28minutes/java/collections/CollectionHierarchy.java).

**Повторная выборка C (24.11.2026):** в [Baeldung](https://www.baeldung.com/java-collections-interview-questions) обнаружен вопрос о `ArrayList` против `LinkedList`; в [teamlead (фиксированная ревизия)](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md) — о скорости доступа, вставки и `Deque`; в [in28minutes](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/src/main/java/com/in28minutes/java/collections/CollectionHierarchy.java) — о массивах и списках; в [Q-CHERNYSHOV (фиксированная ревизия)](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/597e08112ab6a57d6eac8e3c70c070da606dae73/eng/README.md) — об операциях `ArrayList`/`LinkedList` и `Deque`. Это подтверждённые **темы вопросов**, не статистика собеседований и не нормативный источник; тезис «вставка `LinkedList` всегда быстрее» не учитывает поиск позиции.

Встроенные Java-фрагменты в этой статье — иллюстрации поведения, **не самостоятельные проверенные файлы**. Автор 02 отдельно не запускал `javac/java` для каждого фрагмента, проверку Obsidian GUI или JMH: **NOT RUN**. Самостоятельный проверочный скрипт роли 03 из PR #228 **интегрирован в авторскую ветку**. Роль 03 сообщила об успешном запуске на OpenJDK 21.0.12.1: 52 строки стандартного вывода и два отдельных примера, намеренно не проходящих компиляцию. [Технический CI #45358515688](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45358515688) успешен на HEAD `a3c14cb67a2ffee026e335e507af8ebf1e610077`; [интегрированный CI #45359330761](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45359330761) успешен на **предыдущем авторском HEAD** `0c05193c60bdce0f49258ac6be0a4c559a23a0a6`. Эти результаты не означают, что все фрагменты кода из статьи компилировались отдельно. Для нового редакционного HEAD нужно проверить новый запуск CI.