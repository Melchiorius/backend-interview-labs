# Итераторы Java 21: состояние, изменения и модели обхода

**Проверяемый самостоятельный пример Java 21:** [SequentialIteratorProbe, вывод и два случая ожидаемой ошибки компиляции N01/N02](../../examples/collections/sequential-iter/README.md). **Тема:** `COL-ITER`. **Платформа:** Java SE 21. **Принятый план:** [C2 — COL-SEQUENTIAL + COL-ITER](../../docs/plans/COL-SEQUENTIAL-ITER.md). Предпосылки: [контракты коллекций](contracts.md), [последовательные коллекции](sequential.md), [массивы](../01-java-core/arrays.md) и [обобщённые типы](../01-java-core/generics.md).

Итератор — не просто синтаксическая замена цикла. Это **объект с состоянием обхода и правилами допустимых изменений**. Нельзя одинаково рассуждать о `ArrayList.iterator()`, `CopyOnWriteArrayList.iterator()` и итераторе представления `ConcurrentHashMap`: они имеют разные договоры о наблюдении изменений. Ниже **A** обозначает Java 21 API/JLS; **I** — подробности реализации OpenJDK 21.

## Iterator: получение, проверка и исчерпание (CI-01)

`Iterable<T>` предоставляет `iterator()`. Тип `Iterator<T>` объявляет `hasNext()` — возможность получить очередной элемент, `next()` — фактическое получение и перемещение вперёд, а также необязательную операцию `remove()`. **Вызов `hasNext()` сам не потребляет элемент**. После исчерпания `next()` выбрасывает `NoSuchElementException`.

~~~java
Iterator<Integer> it = List.of(67).iterator();
System.out.println(it.hasNext()); // true
System.out.println(it.next());    // 67
System.out.println(it.hasNext()); // false
// it.next(); // NoSuchElementException во время выполнения
~~~

Наличие `Iterator` ещё **не означает** поддержку модификаций, fail-fast обнаружение или потокобезопасность. Даже `Iterable` с собственным итератором может иметь другие свойства обхода; выводить реализацию через `modCount` из одного интерфейса нельзя.

## Правила Iterator.remove и исключения (CI-02, CI-08, CI-15)

Если `Iterator.remove()` поддерживается, он удаляет **последний элемент, возвращённый `next()`**. Вызвать его до первого `next()` или повторно без нового успешного `next()` нельзя: у поддерживающего удаление итератора возникает `IllegalStateException`. Однако `remove` — **необязательная операция**: реализация, не поддерживающая её, может бросить `UnsupportedOperationException` даже до первого `next()`. Не следует утверждать, что в каждой коллекции возможна одна и та же последовательность исключений.

~~~java
var values = new ArrayList<>(List.of(1, 2, 67));
Iterator<Integer> it = values.iterator();
// it.remove(); // IllegalStateException у этого изменяемого итератора
System.out.println(it.next()); // 1
it.remove();                   // корректное удаление через сам итератор
System.out.println(values);   // [2, 67]
// it.remove(); // IllegalStateException: новый next() не вызывался

Iterator<Integer> fixed = List.of(1).iterator();
// fixed.remove(); // UnsupportedOperationException: изменение не поддерживается
~~~

Четыре важные категории не взаимозаменяемы: `NoSuchElementException` — вызов `next()` за концом; `IllegalStateException` — недопустимая последовательность поддерживаемых операций изменения; `UnsupportedOperationException` — изменение вообще не поддерживается; `ConcurrentModificationException` — **обнаруженная**, но не гарантированно обнаруживаемая несовместимая модификация в реализациях с соответствующим механизмом.

## ListIterator: курсор между элементами (CI-03, CI-10)

`ListIterator<E>` движется вперёд и назад. В отличие от «указателя на текущий узел» его **курсор стоит между элементами**. Для списка `[1, 2]` `listIterator(1)` указывает на промежуток между 1 и 2: `nextIndex() == 1`, `previousIndex() == 0`; **если из этого исходного положения вызвать только** `next()`, будет возвращено 2; **если вместо него вызвать только** `previous()`, будет возвращено 1. Это альтернативные действия, а не последовательные вызовы. Схема: `1 | 2`, где `|` — курсор. На начале списка `previousIndex() == -1`, на конце `nextIndex() == size()`.

~~~java
var list = new LinkedList<>(List.of(1, 2));
ListIterator<Integer> it = list.listIterator(1);
System.out.println(it.nextIndex());     // 1
System.out.println(it.previousIndex()); // 0
it.add(67);
System.out.println(list);               // [1, 67, 2]
System.out.println(it.previous());      // 67
it.set(68);                             // заменить элемент после previous()
System.out.println(list);               // [1, 68, 2]
it.remove();                            // удалить последний возвращённый элемент
System.out.println(list);               // [1, 2]
~~~

**Состояния важнее имён методов.** `set(e)` и `remove()` допустимы только после успешного `next()` или `previous()` и при отсутствии промежуточного `add()` или `remove()`, сбрасывающего такую возможность. `add(e)` вставляет значение непосредственно перед курсором; последующий `previous()` возвращает добавленный элемент, а `next()` продолжает со следующим прежним элементом. `add` также сбрасывает возможность `set/remove` до следующего перемещения. На самом начале `previous()` бросает `NoSuchElementException`, на конце — `next()`.

~~~java
var list = new ArrayList<>(List.of(1, 2));
var it = list.listIterator();
// it.set(67); // IllegalStateException у поддерживающего set итератора
it.add(67);
// it.remove(); // IllegalStateException: после add не было next/previous
System.out.println(it.previous()); // 67
~~~

Это **состояние итератора**, а не вызов `List.set(index,e)`. Некоторые `ListIterator` не поддерживают необязательные методы изменения вообще, даже если тип интерфейса позволяет их вызвать.

## Изменения через итератор и в обход него (CI-04, CI-05)

При поддерживаемом `it.remove()` итератор согласует своё состояние с изменением коллекции и может продолжить обход. Если после создания итератора выполнить **структурное изменение исходного `ArrayList` в обход итератора**, OpenJDK 21 обычно замечает его при последующем обращении к итератору и бросает `ConcurrentModificationException`. Но спецификация `ArrayList` прямо оговаривает **обнаружение по возможности (best effort)**, поэтому CME нельзя использовать как способ обеспечения правильности программы или синхронизации.

~~~java
var list = new ArrayList<>(List.of(1, 2));
var it = list.iterator();
System.out.println(it.next()); // 1
list.add(67);
// it.next(); // типичное CME в OpenJDK 21, НЕ универсальная гарантия
~~~

`modCount` и ожидаемое значение счётчика у итератора видны в исходниках [OpenJDK21 `AbstractList`](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/AbstractList.java) и [`ArrayList`](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/ArrayList.java). **Это механизм реализации**, не обязательный алгоритм всех реализаций `Iterable`. Изменение `set` без изменения структуры и изменение ёмкости списка могут по-разному влиять на фактический механизм модификаций; гарантировать отсутствие CME на основе одного `size()` нельзя.

Для `subList` ситуация ещё тоньше: [контракт List](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#subList(int,int)) объявляет поведение представления **неопределённым**, когда исходный список структурно изменён **не через это представление**. Искомое следствие — **не опираться на такое представление после нарушения условий**, а не писать тест, требующий конкретного исключения от каждой версии JDK (CI-14).

### Почему ConcurrentModificationException может не возникнуть (N5)

Следующий фрагмент намеренно нарушает протокол итератора:

~~~java
var list = new ArrayList<>(List.of(1, 67));
var it = list.iterator();
System.out.println(it.next()); // 1
list.remove(0);                // изменение в обход итератора
System.out.println(it.hasNext()); // false в наблюдении аудита
~~~

После `next()` курсор равен 1. После удаления первого элемента размер также равен 1. В [закреплённом ArrayList.Itr.hasNext](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/ArrayList.java#L1043-L1060) проверяется `cursor != size`, а не счётчик модификаций. Поэтому `hasNext()` возвращает false, а следующий `next()`, в котором есть проверка модификации, в обычном цикле уже не вызывается. В [независимом аудите §6.2](../../docs/audits/COLLECTIONS_FINAL_COVERAGE_2026-11-24.md) это наблюдалось на Java 21.0.12.1. Это объяснение конкретной реализации, **не гарантия API** и не рекомендуемый алгоритм. Отдельно автором не запускалось; пример не добавлен в обязательный runner.

Корректные варианты при поддерживаемом изменении: удалить последний возвращённый элемент самим итератором или применить предикат без внешнего изменения коллекции.

~~~java
var list = new ArrayList<>(List.of(1, 67));
for (var it = list.iterator(); it.hasNext(); ) {
    if (it.next() == 1) it.remove();
}
// list: [67]
// Альтернатива на исходном списке: list.removeIf(v -> v == 1);
~~~

Не путайте отсутствие исключения с корректностью. В `LinkedHashMap` с порядком доступа даже `get` может переставить запись и нарушить протокол созданного ранее итератора. Подробности и условия — [COL-HASH](hash-collections.md); корректность конкурентного обхода требует отдельного протокола, а не обработки CME.

## Enhanced for: массивы и Iterable — разные пути (CI-06)

По JLS §14.14.2 расширенный цикл `for (T element : expression)` поддерживает **массивы** и выражения, реализующие `Iterable`. Для массива компилятор использует индексы и длину, **не вызывает `Iterator`**. Для `Iterable` цикл использует полученный итератор и повторяет `hasNext/next` (с учётом типовой трансляции JLS).

~~~java
int sum = 0;
for (int x : new int[]{1, 67}) sum += x;
System.out.println(sum); // 68

for (Integer x : List.of(1, 67)) sum += x;
System.out.println(sum); // 136
~~~

**N01 — отрицательная компиляция:** `for (var e : new HashMap<String,Integer>())` некорректен, потому что `Map` не реализует `Iterable`. При этом `map.entrySet()`, `map.keySet()` и `map.values()` возвращают пригодные для обхода коллекционные представления. Это правило **языка и интерфейсов**, а не свойство `HashMap`-бакетов.

## Три модели обхода: fail-fast, snapshot, weakly consistent (CI-07, CI-13)

**1. ArrayList / LinkedList — обнаружение несовместимых изменений по возможности.** Итератор обычно проходит состояние списка, а неподдерживаемая структурная модификация в обход итератора может завершиться CME. Обнаружение best-effort — **не** гарантия реакции при гонке и не защита общих данных от конкурентного изменения.

**2. CopyOnWriteArrayList — снимок для итератора.** Итератор использует ссылки на элементы из массива **на момент создания**, поэтому последующие изменения списка не появляются в этом обходе. У полученного `Iterator` не поддерживается `remove()`; операции `set()` и `add()` относятся к `ListIterator`, а не к обычному `Iterator`, и у итератора списка `CopyOnWriteArrayList` тоже не поддерживаются.

~~~java
var list = new java.util.concurrent.CopyOnWriteArrayList<>(List.of(1, 2));
Iterator<Integer> before = list.iterator();
list.add(67);
while (before.hasNext()) System.out.print(before.next()); // 12, не 1267
// before.remove(); // UnsupportedOperationException
~~~

**3. ConcurrentHashMap — слабо согласованный обход представлений.** Итераторы `keySet/values/entrySet` могут отражать **часть** изменений, произошедших после их создания; не обязаны формировать единый фиксированный снимок и не бросают `ConcurrentModificationException` по модели fail-fast. Ни «обязательно увидит каждую новую запись», ни «не увидит ни одной» — не переносимые утверждения. Одним многопоточным тестом нельзя доказать эти границы. *Слабая согласованность* также **не гарантирует атомарность многошаговой бизнес-операции**: это тема [COL-CONCURRENT](concurrent.md).

Слово **fail-safe** встречается в подборках вопросов, но не является единым нормативным контрактом Java 21. Обход по **снимку (snapshot)** и **слабосогласованный обход (weakly consistent)** имеют разные гарантии; объединять их под названием «без `ConcurrentModificationException`» неправильно.

## forEachRemaining и действия с побочными эффектами (CI-09)

`Iterator.forEachRemaining(action)` применяет действие ко **всем ещё не пройденным** элементам в порядке, установленном данным итератором, вплоть до конца или исключения. Если `action` выбрасывает исключение, оно передаётся вызывающему коду. После уже выполненного `next()` обработка начнётся с **следующего**, а не с первого элемента списка:

~~~java
Iterator<Integer> it = List.of(1, 67, 2).iterator();
System.out.println(it.next()); // 1
it.forEachRemaining(System.out::println); // 67, затем 2
~~~

**Точные границы Java SE 21 `Iterator`:** если действие `action` изменяет исходную коллекцию, поведение итератора не определено, **если только переопределяющий класс не задал собственную политику конкурентных изменений**. Это касается и попытки вызвать `remove()` из действия. Если `action` выбрасывает исключение, дальнейшее поведение итератора не определено; само исключение передаётся вызывающему коду. Кроме того, поведение `Iterator.remove()` **после вызова `forEachRemaining()`** не определено. Речь не о запрете любых побочных эффектов действия и не о гарантированном `ConcurrentModificationException`: ограничения относятся к изменению источника и последующему использованию итератора.

## Spliterator: минимум для понимания параллельного обхода (CI-11)

`Spliterator` поддерживает `tryAdvance`, `forEachRemaining` и попытку разделения работы через `trySplit()`. Его **характеристики** (`ORDERED`, `SIZED`, `IMMUTABLE`, `CONCURRENT` и другие) описывают источник, но не эквивалентны гарантиям атомарности операций над бизнес-состоянием. Некоторые разделяемые итераторы связываются с источником *отложенно* (late-binding), что влияет на момент получения данных и обнаружение изменений. У `ArrayList` детали late-binding и fail-fast требуется понимать по документации/реализации **конкретного класса**, а не переносить на любой `Spliterator`. Стримы и параллельные операции подробнее рассмотрены в [CORE-STREAMS](../01-java-core/streams.md); точные результаты гонок здесь не проверяются.

Порядок обхода зависит от контракта источника: например, [PriorityQueue](priority-queue.md) гарантирует выбор головы по приоритету, но не отсортированный порядок итератора.

## Где заканчивается данный материал (CI-12, CI-16)

Счётчик `modCount` в `AbstractList` — защищённая точка расширения (`protected`) для реализаций списков, а не часть контракта всякого `Iterable`. Для `ConcurrentHashMap` количество увиденных итератором записей при конкурентной модификации не обязано совпадать ни со снимком до, ни со снимком после; `Spliterator` и параллельные потоки обработки (`Stream`) также требуют отдельного разговора об источнике, действиях и потокобезопасности. **Нельзя делать вывод об атомарности алгоритма из свойства итератора.** Гонки и Java Memory Model, поведение конкурентных очередей, измерения производительности и выбор по профилю нагрузки относятся к другим темам программы.

## Готовимся отвечать на уточняющие вопросы

1. Почему `Iterator.remove()` перед первым `next()` у `ArrayList` — `IllegalStateException`,  а у `List.of` — `UnsupportedOperationException`? Каковы состояния?
2. Где стоит курсор `ListIterator` и что происходит с ним при `add(67)`?
3. Почему обновление списка «снаружи» может нарушить обход, а `it.remove()` — допустимо?
4. Чем `ArrayList` fail-fast отличается от обхода по снимку `CopyOnWriteArrayList` и слабосогласованного обхода `ConcurrentHashMap`, и почему слово *fail-safe* недостаточно?
5. Почему `for-each` по массиву не требует `Iterable`, а по `HashMap` напрямую не компилируется?
6. Что делает `forEachRemaining` после одного `next()`? Когда результат действия нельзя предсказывать без дополнительных предпосылок?

## Матрица покрытия: CI-01..CI-16

| ID | Приоритет | Где представлен |
|---|---|---|
| CI-01 | P0 | Iterator/hasNext/next/исчерпание |
| CI-02 | P0 | Состояние Iterator.remove |
| CI-03 | P0 | Курсор ListIterator и add/set/remove |
| CI-04 | P0 | Мутация через итератор и в обход него |
| CI-05 | P0 | Fail-fast best-effort |
| CI-06 | P0 | Enhanced for array vs Iterable; N01 |
| CI-07 | P0 | Три модели обхода |
| CI-08 | P0 | NSE/ISE/UOE/CME и условия |
| CI-09 | P0 | forEachRemaining |
| CI-10 | P1 | ListIterator cursor после add/previous |
| CI-11 | P1 | Spliterator и характеристики |
| CI-12 | P1 | AbstractList.modCount — I |
| CI-13 | P1 | обход по снимку, слабосогласованный и fail-fast |
| CI-14 | P1 | представление `subList` и нарушение условий его использования |
| CI-15 | P2 | Необязательные методы изменения |
| CI-16 | P2 | Ограничения конкурентного обхода и stream |

## Источники, доказательства и NOT RUN

**Нормативные A:** [Iterator Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Iterator.html), [ListIterator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ListIterator.html), [Iterable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Iterable.html), [List.subList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html), [ArrayList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html), [LinkedList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedList.html), [CopyOnWriteArrayList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CopyOnWriteArrayList.html), [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html), [Spliterator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Spliterator.html), [JLS 21 §14.14.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.14.2). **Реализация I:** [AbstractList.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/AbstractList.java), [ArrayList.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/ArrayList.java), [LinkedList.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/LinkedList.java), закреплённый SHA `890adb6410dab4606a4f26a942aed02fb2f55387`. **C — вопросы, не источник контрактов:** [Baeldung](https://www.baeldung.com/java-collections-interview-questions), [teamlead pinned](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md), [in28minutes](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/src/main/java/com/in28minutes/java/collections/CollectionHierarchy.java), [ViacheslavChernyshov — fail-fast/fail-safe question](https://viacheslavchernyshov.github.io/java-interview-questions-and-answers/questions/04-collections/26-what-are-fail-fast-and-fail-safe-iterators/).

**Повторная выборка C (24.11.2026):** [Baeldung](https://www.baeldung.com/java-collections-interview-questions) содержит вопрос о fail-fast/fail-safe; [teamlead (фиксированная ревизия)](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md) — вопросы об итераторах и `CopyOnWriteArrayList`; [in28minutes](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/src/main/java/com/in28minutes/java/collections/CollectionHierarchy.java) — об `Iterator` и `ListIterator`; [Q-CHERNYSHOV (фиксированная ревизия)](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/597e08112ab6a57d6eac8e3c70c070da606dae73/eng/README.md) — о различии `Iterator`/`ListIterator` и fail-fast/fail-safe. Четыре банка подтверждают **вопросы**, но не нормативную классификацию: `ConcurrentHashMap` слабо согласован, а не обязан создавать снимок; статистика интервью не установлена.

Авторский текст представляет **объяснение API и ограничений, а не воспроизведённый набор локальных запусков**. Отдельная компиляция всех фрагментов кода внутри Markdown, визуальная проверка в Obsidian/IDE, локальный запуск Java-программы и проверка на чистой копии репозитория ролью 02 — **NOT RUN**. Самостоятельный проверочный скрипт и два отдельных сценария ожидаемых ошибок компиляции роли 03 **интегрированы** из технического PR #228; локальное выполнение роли 03 на OpenJDK 21.0.12.1 и технический CI #45358515688 (HEAD `a3c14cb67a2ffee026e335e507af8ebf1e610077`) успешны. Они не доказывают отдельную компиляцию всех Markdown-фрагментов и не заменяют CI нового интегрированного HEAD.