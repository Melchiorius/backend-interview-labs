# CORE-STREAMS — Stream API в Java 21: как собирается и выполняется pipeline

**Версия:** Java SE 21, без preview. **Контракт темы:** принятый [G1 CORE-STREAMS + CORE-STREAM-ADV](../../docs/plans/CORE-STREAMS-ADV.md), [Issue #114](https://github.com/Melchiorius/backend-interview-labs/issues/114). Эта статья объясняет, **какие участники создают поток вычислений, когда вызываются функции и какой тип результата можно обещать**. Отдельная [CORE-STREAM-ADV](stream-advanced.md) раскрывает редукции, коллекторы, порядок при параллельном выполнении и ресурсы. Предпосылки: [лямбды и функциональные интерфейсы](functions.md), [обобщённые типы](generics.md), [исключения](exceptions.md), [equals/hashCode](equality.md).

**Сквозная исполняемая демонстрация:** [Stream API Java 21 — два исходника, полный stdout и verify.sh](../../examples/core/streams/README.md). Первая программа показывает P01–P33 (pipeline, collectors, закрытие, порядок), вторая — C01–C11 (формы `reduce`, `Optional` и `*Match`). Это **44 подписанных наблюдения двух запусков**, не 44 самостоятельных теста. Проще сначала понять механизм в разделах ниже, затем сверить его с эталоном.

## 1. Stream — описание обработки данных, а не ещё одна коллекция (ST-01)

`List<Integer>` хранит элементы и предоставляет операции доступа к ним. `Stream<Integer>` представляет **последовательность вычислений над элементами из источника**, которую можно обработать **один раз**. Поток не добавляет элементы в исходный список и не хранит автоматически копию всех результатов.

Участники одного pipeline:

- **Источник**: `List`, массив, генератор, диапазон `IntStream` или другой источник элементов.
- **Промежуточная стадия**: возвращает Stream (не обязательно новый объект), описывая дополнительное преобразование или фильтрацию, например `filter`, `map`, `flatMap`.
- **Терминальная операция**: запрашивает итог `List`, `Optional`, число, логическое значение либо выполняет действие, например `toList`, `count`, `findFirst`, `forEach`.

~~~text
List<Integer> source: [1, 2, 3, 4]
                 │
                 ▼
Stream<Integer> -- filter(x -> x % 2 == 0) --> Stream<Integer>
                                                       │
                                                       ▼
                              map(x -> x * 10) --> Stream<Integer>
                                                       │
                                                       ▼
                                      toList() --> List<Integer> [20, 40]
~~~

Стрелки означают **логические стадии преобразования элементов**, не отдельные потоки ОС, задачи executor или гарантированный цикл для каждой стрелки. Источник поставляет элементы, операция terminal запрашивает результат, реализация Stream решает, как выполнить этапы с учётом допустимых оптимизаций. [Stream API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html), [package description](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html).

~~~java
import java.util.List;

class FirstPipeline {
    public static void main(String[] args) {
        List<Integer> source = List.of(1, 2, 3, 4);
        List<Integer> result = source.stream()
            .filter(x -> x % 2 == 0)
            .map(x -> x * 10)
            .toList();
        System.out.println(result); // [20, 40]
        System.out.println(source); // [1, 2, 3, 4] — исходный List не изменён
    }
}
~~~

**Зачем:** когда в сервисе нужно получить преобразованный список из DTO, `stream()` описывает вычисление результата без изменения исходной коллекции. Для обновления самих DTO или выполнения внешнего побочного действия API Stream не даёт автоматически транзакционности, потокобезопасности или повторяемости.

### Как выбрать фабрику

| Вход | Операция и смысл |
|---|---|
| Коллекция | collection.stream() или parallelStream() |
| Массив ссылок | Arrays.stream(array) или Stream.of(array) с выводом типа элементов (varargs); для потока самих массивов тип задают явно |
| int[] | Arrays.stream(array) даёт IntStream по значениям; Stream.of(array) даёт Stream<int[]> с одним элементом — самим массивом |
| Одно значение, в том числе null | Stream.of(value) — один элемент (для null задайте тип/приведение, чтобы выбрать одноаргументную форму) |
| Возможно отсутствующее значение | Stream.ofNullable(value) — ноль элементов при null, иначе один |
| Диапазон, вычисляемая последовательность | IntStream.range / rangeClosed, Stream.iterate / generate (§9) |

В отдельной Java 21 пробе (здесь и далее результаты новых временных опытов перечислены в конце статьи) проверены следующие выражения. Сами фрагменты можно поместить в main с импортами java.util.Arrays и java.util.stream.Stream.

~~~java
int[] data = {67, 68};
System.out.println(Stream.of(data).count());    // 1
System.out.println(Arrays.stream(data).count()); // 2
System.out.println(Stream.of((String) null).count()); // 1
System.out.println(Stream.ofNullable(null).count()); // 0
~~~

Массив примитивов нельзя разложить в Object... по отдельным int без преобразования: int[] сам является ссылочным объектом. Поэтому тип потока и число элементов не всегда очевидны из вида вызова. См. [CORE-ARRAYS](arrays.md), [CORE-GENERICS](generics.md).

## 2. Отложенное выполнение: map не вызывается при построении (ST-02, ST-09)

Промежуточные операции ленивы: построение цепочки не начинает обход элементов источника и применение mapper/predicate к элементам. Но обычные выражения Java вычисляются при вызове метода: аргумент `map(makeMapper())` сначала требует вызова makeMapper(), даже если возвращённая функция пока не применяется к данным. Вызов map возвращает Stream; API не гарантирует иной объект для каждой операции. Терминальная операция обычно инициирует вычисление, но iterator() и spliterator() — специальные терминальные операции, передающие управление обходом вызывающему коду. Из ленивости **не** следует «каждая lambda обязательно выполняется один раз для каждого элемента» или «сначала полностью filter, потом полностью map»: реализация вправе устранять стадии, если результат не меняется, и потреблять лишь нужные элементы.

~~~java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class LazyExample {
    public static void main(String[] args) {
        AtomicInteger calls = new AtomicInteger();
        Stream<Integer> pipeline = Stream.of(1, 2, 3)
            .map(x -> {
                calls.incrementAndGet();
                return x * 2;
            });
        System.out.println(calls.get()); // 0: пока только построили pipeline
        System.out.println(pipeline.toList()); // [2, 4, 6]
        System.out.println(calls.get()); // 3: наблюдение этого конкретного выполнения
    }
}
~~~

`AtomicInteger` здесь позволяет **наблюдать** вызовы, а не является обязательным механизмом Stream. Не превращайте значение счётчика в переносимую гарантию для произвольной цепочки. Спецификация прямо позволяет устранять вызовы behavioral parameters, если вычисленный результат не меняется.

Например, factory увеличивает первый счётчик при создании функции, а сама функция увеличивает второй счётчик при обработке элемента. Фрагмент из временной последовательной пробы (factory — метод класса, остальные строки внутри main):

~~~java
static java.util.function.Function<Integer,Integer> factory(int[] c) {
    c[0]++;
    return x -> { c[1]++; return x * 2; };
}
// Внутри main:
int[] c = {0, 0};
var p = Stream.of(67, 68).map(factory(c));
System.out.println(java.util.Arrays.toString(c)); // [1, 0]
var it = p.iterator();
System.out.println(java.util.Arrays.toString(c)); // [1, 0]
System.out.println(it.next());                    // 134
System.out.println(java.util.Arrays.toString(c)); // [1, 1]
~~~

Последние два значения в фактической пробе напечатаны одной строкой 134,[1, 1]. Другой новый поток map(x -> x + 1) передан через spliterator(); вызов tryAdvance для одного элемента напечатал 68 и вернул true. Это наблюдения конкретного обхода, не обещание числа вызовов для любого источника, буферизации или параллельного исполнения. Полученный iterator/spliterator используют по их контракту; старый Stream не становится доступным для второго обхода. [BaseStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/BaseStream.html), [Iterator/Spliterator](../02-collections/iteration.md).

Контрпример против «`peek` гарантированно проверяет все записи»:

~~~java
import java.util.List;

class PeekCount {
    static long count() {
        return List.of("a", "b", "c").stream()
            .peek(System.out::println)
            .count();
    }
}
~~~

Для источника с известным размером `count()` **может** вычислить 3 без обхода и вообще не вызвать `peek`; так ведёт себя исследованный JDK 21. Но это **не обещание**, что `peek` не сработает на любом источнике или при любой цепочке. Аналогично `map(x -> sendEmail(x))` — ошибочная попытка обеспечивать обязательное действие через промежуточную стадию: обработка может быть сокращена или оптимизирована. Если требуются эффекты, нужно отдельно проектировать их семантику; в обычной бизнес-логике часто лучше явно выраженный цикл.

Норма: [Stream.count() API note](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#count()), [stream package — side effects](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html).

## 3. filter, map и flatMap: разные типы функций (ST-03, ST-04)

Точные объявления в Stream<T>: `Stream<T> filter(Predicate<? super T> predicate)` и `<R> Stream<R> map(Function<? super T, ? extends R> mapper)`. filter оставляет элементы, для которых предикат возвращает true, сохраняя тип T. map заменяет каждый потреблённый элемент результатом функции типа R. `? super T` позволяет передать функцию, принимающую T либо его супертип; `? extends R` позволяет функции возвращать R или его подтип. Параметр R определяется при выводе типов или явно. Это два различных SAM-контракта из [CORE-FUNCTIONS](functions.md); правила wildcard и инвариантности — [CORE-GENERICS](generics.md).

~~~java
import java.util.List;
import java.util.Locale;

class Transform {
    static List<String> names() {
        return List.of("Al", "Boris", "Vi").stream()
            .filter(s -> s.length() > 2)    // Predicate<String> -> boolean
            .map(s -> s.toUpperCase(Locale.ROOT)) // Function<String,String> -> String
            .toList();                       // [BORIS]
    }
}
~~~

В первоначальном фрагменте стояло `map(String::toUpperCase)`. Вызов без аргумента использует JVM locale по умолчанию. При цели примера — получить одинаковое преобразование независимо от языка среды — нужен явно переданный Locale.ROOT. Если же преобразуется естественный язык для пользователя, выбирают соответствующую языковую локаль, а не автоматически ROOT. Регистровое преобразование не заменяет полную нормализацию протокольных ключей/Unicode. См. [String.toUpperCase()](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#toUpperCase()), [CORE-STRINGS](strings.md); будущая тема A24 I18N.

Фактический контроль на Debian OpenJDK 21.0.12.1: обе функции использовали тот же список Al, Boris, Vi и тот же filter; отличается только mapper. JVM запускалась с UTF-8; свойствами user.language/user.country были заданы две локали и проверены Locale.getDefault(), язык, страна, file.encoding и фактическая кодировка System.out.

| Фактическая JVM locale | Исходный mapper | Locale.ROOT | Кодовая точка I в исходном / исправленном |
|---|---|---|---|
| en (страна пустая, без флагов locale) | [BORIS] | [BORIS] | U+0049 / U+0049 |
| en_US | [BORIS] | [BORIS] | U+0049 / U+0049 |
| tr_TR | [BORİS] | [BORIS] | U+0130 / U+0049 |

Все три процесса exit 0, stderr пуст. stdout соответственно 136/141/142 байта, везде 5 строк; в tr_TR символ İ занимает два байта UTF-8. Локали JVM не требуют установки одноимённых локалей ОС. Проверка не меняет глобальный Locale.setDefault внутри программы и не доказывает, что исходный общий runner содержал эту ошибку.

**Типы функций:** допустимы не только функции, принимающие ровно Integer. В фактической строгой компиляции:

~~~java
java.util.function.Predicate<Number> pred = x -> x.intValue() > 67;
java.util.function.Function<Number,String> fmt = x -> "v" + x;
List<String> result = Stream.of(67, 68).filter(pred).map(fmt).toList();
System.out.println(result); // [v68]
~~~

В этом случае T = Integer, R = String. Явно заданный аргумент String у mapper потока Integer несовместим, тогда как Number подходит. Отдельные negative-компиляции `Stream.of(67).map((String s) -> s.length())` и `Stream.of("a").filter(s -> s.length())` отвергнуты: первая — несовместимый тип функции, вторая — int вместо boolean. Исторические compile-negative N01/N11 зафиксированы в G1 §7; это новые независимые от них исходники автора.

Если один внешний элемент содержит **несколько** внутренних, обычный `map(List::stream)` создаёт `Stream<Stream<T>>`. Чтобы получить единый плоский поток элементов, используют `flatMap`, чей mapper возвращает Stream:

~~~java
import java.util.List;

class Flat {
    public static void main(String[] args) {
        List<List<String>> groups = List.of(
            List.of("a", "b"), List.of("c"));
        System.out.println(groups.stream()
            .flatMap(List::stream)
            .toList());                    // [a, b, c]
        // .flatMap(group -> group)        // НЕ компилируется: требуется Stream
    }
}
~~~

`flatMap` разворачивает элементы вложенных потоков в итоговый Stream; созданный mapper'ом внутренний Stream закрывается после использования согласно API. В параллельном pipeline это **не** означает последовательный порядок исполнения mapper или фиксированный порядок потоков ОС. Не обещайте, что `flatMap` автоматически закроет **внешний** Stream или произвольные ресурсы, захваченные lambda. Дополнительный разбор закрытия — в [CORE-STREAM-ADV](stream-advanced.md). [Stream.flatMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#flatMap(java.util.function.Function)).

Точная форма: `<R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper)`. Если mapper возвращает null вместо Stream, по API это пустой поток для данного элемента. Это не то же самое, что Stream.of((Integer)null) внутри mapper — он содержит один null. Передача null вместо самого mapper недопустима. Фактически получены [67] для mapper x -> x == 0 ? null : Stream.of(x) на входе 0,67; [null] для mapper, создающего поток одного null; NullPointerException для flatMap(null) в исследованном JDK.

Пример разделения ответственности за закрытие (java.util.ArrayList, java.util.stream.Stream):

~~~java
var log = new java.util.ArrayList<String>();
try (var outer = Stream.of(1, 2).onClose(() -> log.add("outer"))) {
    var first = outer.flatMap(x -> {
        log.add("create" + x);
        return Stream.of(x, x + 10).onClose(() -> log.add("close" + x));
    }).findFirst();
    System.out.println(first + "," + log);
}
System.out.println(log);
~~~

Фактический вывод (без подписей временной пробы):

~~~text
Optional[1],[create1, close1]
[create1, close1, outer]
~~~

Первый внутренний поток закрыт после использования, включая short-circuit; внешнее закрытие произошло при выходе из try. В данном последовательном запуске второй внутренний поток вообще не создавался. Нельзя переносить это число созданных потоков на любую цепочку, особенно parallel, или считать обработчики onClose доказательством физического освобождения произвольного ресурса. Внешним ресурсом владеет вызывающий код; см. также [CORE-RESOURCES](resources.md).

## 4. distinct, sorted и limit: состояние, цена и сокращение (ST-05, ST-06)

У `filter` для решения об одном элементе достаточно его текущего значения. `distinct` должен различать ранее встреченные элементы, а `sorted` — упорядочивать множество элементов; это **stateful** intermediate operations. Упорядоченному `sorted` обычно нужно получить все конечные элементы, прежде чем начать выдавать отсортированный результат, поэтому нельзя утверждать, что любая цепочка «обязательно работает с O(1) памяти». `distinct` определяет совпадения по `Object.equals`; согласованный `hashCode` необходим для корректного контракта объектов, но API не предписывает конкретную хеш-таблицу или алгоритм.

~~~java
import java.util.List;

class Stateful {
    public static void main(String[] args) {
        System.out.println(List.of(3, 1, 3, 2, 2).stream()
            .distinct()
            .sorted()
            .limit(2)
            .toList());                    // [1, 2]
    }
}
~~~

Здесь `distinct` устраняет дубликаты, `sorted` устанавливает порядок, `limit(2)` ограничивает итог двумя значениями. **Порядок операций меняет смысл**: `limit(2).sorted()` сперва ограничивает исходную последовательность, а затем сортирует только её первые два значения. При parallel/ordered `distinct` и `limit` могут быть заметно дороже из-за необходимости соблюдать encounter order; гарантии асимптотики и speedup по одному примеру не выводятся.

Для ordered stream distinct сохраняет **первый по encounter order** представитель среди равных элементов; sorted сохраняет относительный порядок элементов, для которых сравнение даёт 0 (стабильная сортировка). Для unordered потока обе гарантии стабильности отсутствуют. Равенство для distinct и равенство по Comparator для sorted — разные отношения. Например, два объекта Item равны по key, но имеют разные label. В отдельной пробе класс record Item(int key, String label) переопределил equals и hashCode согласованно по key. Для входа (1,first),(1,second),(2,other) distinct с последующим map(Item::label) дал [first, other]. Stream.of("bb","a","aa").sorted(Comparator.comparingInt(String::length)).toList() дал [a, bb, aa]. bb и aa равны по длине, поэтому сохранён их относительный порядок.

Без Comparator sorted использует естественный порядок: элементы должны быть взаимно сравнимы. В вызове Stream.of(new Object(), new Object()).sorted() тип не отвергается на этапе компиляции; в отдельном запуске ClassCastException возникла при toList(), когда понадобилась сортировка. Не обещайте, что каждая неверная цепочка обязательно дойдёт до этого сравнения (возможна оптимизация). См. [CORE-EQUALITY](equality.md) и [CORE-STREAM-ADV](stream-advanced.md).

skip(n) отбрасывает первые n элементов при наличии порядка (или весь поток, если их меньше), limit(n) ограничивает результат n элементами. Для unordered потока не задавайте конкретные сохраняемые/отбрасываемые элементы. n должен быть неотрицательным: в фактической пробе оба отрицательных аргумента отвергнуты IllegalArgumentException при построении стадии, limit(0) дал [], skip(0) — [67] для одноэлементного входа. В ordered parallel skip/limit могут быть дороже из-за согласования префикса. В наблюдении Stream.of(67,68).map(x -> { calls[0]++; return x; }).skip(1).count() вернул 1, а счётчик остался 0: размер был известен, mapper не понадобился. Это допустимая оптимизация, не обязательный исход для всех pipeline. [Stream.skip](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#skip(long)).

~~~java
import java.util.stream.Stream;

class ShortCircuit {
    static long firstThreeCounted() {
        return Stream.iterate(0, x -> x + 1)
            .limit(3)
            .count();                       // 3
    }
}
~~~

`limit` — short-circuiting *intermediate* операция: downstream может перестать требовать элементы после границы. Для **бесконечного** источника такой предел часто делает запрос конечным, но не обещает завершение *любой* pipeline: если перед `limit` поставить операцию, которой требуется бесконечно много данных (например, полную сортировку неограниченной последовательности), вычисление может не завершиться. [Stream.limit](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#limit(long)), [Stream.sorted](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#sorted()).

## 5. findFirst, findAny и *Match: Optional — не boolean (ST-07, ST-19)

Терминальная `findFirst()` возвращает `Optional<T>`: он либо содержит найденный объект, либо пуст. Если **есть encounter order**, первый означает первый по этому порядку. `findAny()` вправе выбрать **любой** элемент: даже если в конкретном sequential запуске он совпал с первым, не надо записывать это как гарантированный выбор. Для `IntStream` результат аналогично имеет тип `OptionalInt`; он предоставляет `getAsInt()`/`orElse(int)`, не `Optional<T>.get()`.

~~~java
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class FindExamples {
    public static void main(String[] args) {
        Optional<String> first = Stream.of("first", "second").findFirst();
        OptionalInt number = IntStream.of(67, 68).findFirst();
        System.out.println(first.orElse("missing"));       // first
        System.out.println(number.orElse(-1));              // 67
        System.out.println(Stream.<String>empty().findFirst()); // Optional.empty
    }
}
~~~

Важно не смешивать `Optional<Integer>` и `OptionalInt`: первый оборачивает Integer, второй специализирован для `int`. Попытка присвоить `IntStream.of(1).findFirst()` в `Optional<Integer>` — ошибка типов. Если выбранный элемент равен `null`, `findFirst/findAny` выбрасывают `NullPointerException` по контракту; неопределённость может относиться к выбору элемента, а не к реакции на выбранный null. В новой пробе оба вызова на Stream.of((String)null) дали NPE, поэтому такие операции не следует представлять как универсальную замену проверке null.

Другие три terminal-операции возвращают **примитивный `boolean`**, не Optional:

~~~java
import java.util.stream.Stream;

class Match {
    public static void main(String[] args) {
        System.out.println(Stream.of(1, 2, 3).anyMatch(x -> x > 2)); // true
        System.out.println(Stream.<Integer>empty().anyMatch(x -> true));  // false
        System.out.println(Stream.<Integer>empty().allMatch(x -> false)); // true
        System.out.println(Stream.<Integer>empty().noneMatch(x -> true)); // true
    }
}
~~~

Для пустого множества `anyMatch` ложен, а `allMatch` и `noneMatch` истинны: **нет контрпримера** к «все подходят» или «никто не подходит». Это вакуумная истинность, а не вызов `Predicate` на каком-то «пустом» элементе. `Optional.orElse` применим только к Optional, не к `boolean`. [Stream.findFirst](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#findFirst()), [Stream.anyMatch](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#anyMatch(java.util.function.Predicate)), [OptionalInt](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/OptionalInt.html).

## 6. Stream — одноразовый; терминальная операция не обязана его закрывать (ST-08)

~~~java
import java.util.stream.Stream;

class UseOnce {
    public static void main(String[] args) {
        Stream<Integer> source = Stream.of(1, 2, 3);
        System.out.println(source.count()); // 3
        // source.toList(); // запрещено: повторное использование pipeline
    }
}
~~~

API требует, чтобы stream **использовался только однажды**. После терминальной операции дальнейшее использование того же pipeline может приводить к `IllegalStateException`; реализация обнаруживает некоторые случаи повторного использования уже раньше. Если нужно два разных результата, создайте **два новых stream из повторно доступного источника**; не пытайтесь «перезапустить» старый объект. Генератор или сетевой источник может быть неповторяемым — это не лечится переприсваиванием ссылки.

Один pipeline нельзя разветвлять на две цепочки от одной стадии. Например:

~~~java
var source = Stream.of(67);
var child = source.map(x -> x + 1);
// Отдельная попытка source.filter(x -> true) — повторное использование.
System.out.println(child.toList()); // [68]
~~~

В новой пробе попытка source.filter(...) после source.map(...) была выполнена в отдельном try/catch, ещё до terminal. Получены IllegalStateException и затем [68] из child. Это наблюдение диагностики, не гарантия сохранности child после любых ошибочных действий; API допускает, что не все повторные использования будут обнаружены. Два новых Stream.of(67) независимо дали count=1 и [67]. Закрытый stream использовать нельзя: отдельный вызов count после close дал IllegalStateException. Методы sequential()/parallel() могут вернуть самого получателя; в пробе обе проверки через == дали true на одном новом Stream, затем он закрыт. Это не способ создать две независимые цепочки и не гарантия тождества объектов. [BaseStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/BaseStream.html).

И отдельно: «terminal запущена» **не равно** «`close()` вызван». Обычные streams из List не нуждаются во внешней очистке, но stream из IO-источника может удерживать ресурс. Правильные `try-with-resources` и `onClose` разобраны в [CORE-STREAM-ADV](stream-advanced.md) и [CORE-RESOURCES](resources.md). [Stream overview](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html).

## 7. Три похожие операции — три разных контракта List (ST-11, ST-12, ST-13)

| Операция | Что именно гарантировано | Null и изменяемость |
|---|---|---|
| `stream.toList()`, с Java 16 | Итоговый List **unmodifiable**, encounter order сохраняется при наличии; конкретный класс не обещан | На OpenJDK 21 в обычной пробе `Stream.of((String)null).toList()` содержит null; API этой операции не устанавливает запрет на null |
| `stream.collect(Collectors.toList())` | List с элементами в encounter order, без гарантий типа, изменяемости, сериализуемости и потокобезопасности | Нельзя считать результат гарантированно изменяемым или гарантированно null-отвергающим |
| `stream.collect(Collectors.toUnmodifiableList())` | **Unmodifiable** List с элементами в encounter order; null запрещён | При встрече с null — `NullPointerException` |

~~~java
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ListResults {
    public static void main(String[] args) {
        List<Integer> fixed = Stream.of(1, 2, 3).toList();
        try {
            fixed.add(67);
        } catch (UnsupportedOperationException e) {
            System.out.println("unmodifiable");
        }
        List<Integer> unspecified = Stream.of(1, 2, 3)
            .collect(Collectors.toList());
        System.out.println(unspecified);   // [1, 2, 3]
        try {
            Stream.of("x", (String) null)
                .collect(Collectors.toUnmodifiableList());
        } catch (NullPointerException e) {
            System.out.println("null rejected");
        }
    }
}
~~~

Старайтесь проектировать **нужный контракт**, а не угадывать текущую реализацию `ArrayList`. Если нужен гарантированно изменяемый `ArrayList`, используйте `collect(Collectors.toCollection(java.util.ArrayList::new))`. Для Stream.toList() все методы изменения **самого списка** выбрасывают UnsupportedOperationException, включая замену элемента через set и операции без фактического изменения (например, addAll(empty) или removeIf с всегда ложным предикатом). Это не глубокая неизменяемость объектов внутри. Не утверждайте, что `Collections.unmodifiableList`, `List.copyOf` и `Stream.toList` имеют одинаковую политику null. [Stream.toList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#toList()), [Collectors API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collectors.html).

В отдельном запуске set(0,68), addAll(List.of()) и removeIf(x -> false) на результате Stream.of(67).toList() дали три UnsupportedOperationException. У Arrays.asList(67) размер фиксирован, но set(0,68) разрешён и дал [68], а add(69) — UnsupportedOperationException. Это иной контракт. Если элементом unmodifiable list является StringBuilder, его собственные методы остаются доступны: builder.append(68) после Stream.of(builder).toList() изменил видимый элемент с 67 на 6768. См. [CORE-IMMUTABILITY](immutability.md), [CORE-ARRAYS](arrays.md).

Результат Stream.toList() может быть value-based объектом: не полагайтесь на тождество ссылок, identityHashCode и монитор этого списка. Не обещаны конкретный класс или сериализуемость. Если поток имеет encounter order, результат соблюдает его. Контракт Collectors.toList() также сохраняет encounter order, но не гарантирует изменяемости; для явного выбора реализации используют toCollection. Наблюдение [null] на Java21 не следует превращать в общий запрет null для всех способов построения списков.

**Вывод типов — ещё одно отличие:** `Stream<T>.toList()` возвращает List<T> без собственного параметра типа; `collect(Collector<? super T,A,R>)` может вывести совместимый тип коллектора из контекста. В строгой компиляции прошли обе строки:

~~~java
List<Number> viaCollector = Stream.of(67).collect(Collectors.toList());
List<Number> viaWitness = Stream.<Number>of(67).toList();
~~~

В первой T самого потока — Integer, коллектор может принимать Number и вернуть List<Number>. Во второй тип потока задан явно. Обе напечатали [67]. В отдельной отрицательной компиляции `List<Number> wrong = Stream.of(67).toList();` получен несовместимый List<Integer> и List<Number>; присваивание не расширяет инвариантный List. [CORE-GENERICS](generics.md).

## 8. IntStream — не Stream<Integer> (ST-18)

`Stream<Integer>` переносит ссылки на boxed Integer; `IntStream` работает с примитивными `int` и предлагает специальные терминальные операции `sum()`, `average()` (`OptionalDouble`), `summaryStatistics()`. Автобоксинг может возникать при переходе обратно к объектной форме, но Stream API не обещает конкретного выигрыша по времени в любой программе.

~~~java
import java.util.List;
import java.util.stream.IntStream;

class Primitive {
    public static void main(String[] args) {
        int total = IntStream.rangeClosed(1, 3).sum(); // 6
        List<Integer> values = IntStream.range(1, 4).boxed().toList();
        int charCount = List.of("a", "bb", "ccc").stream()
            .mapToInt(String::length)
            .sum();                              // 6
        System.out.println(total + "," + values + "," + charCount);
    }
}
~~~

`Stream<Integer>.sum()` **не существует**; нужно `mapToInt(Integer::intValue).sum()`, `collect(summingInt(...))` или `reduce(0, Integer::sum)`. У IntStream **нет** перегрузки collect(Collector); его метод имеет форму `<R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R,R> combiner)`. В новой отрицательной компиляции IntStream.of(67).collect(Collectors.toList()) отвергнут, а трёхаргументный collect(ArrayList<Integer>::new, ArrayList::add, ArrayList::addAll) на IntStream.of(67,68) скомпилирован и дал [67, 68]. Для обычного Collector можно сначала вызвать boxed(). Аналоги `LongStream` и `DoubleStream` имеют собственные примитивные операции/Optional-специализации. [IntStream API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/IntStream.html).

### Преобразования и границы арифметики

Из IntStream можно получить Stream<R> через mapToObj, LongStream через mapToLong или asLongStream (расширение int до long), DoubleStream через mapToDouble или asDoubleStream. В проверке IntStream.of(67) отдельные потоки дали: mapToObj(x -> "v" + x).toList() — [v67]; mapToLong(x -> x + 1L).sum() — 68; asLongStream().sum() — 67; asDoubleStream().sum() — 67.0. Не нужно сначала boxing для примитивной арифметики; однако число реально выделенных объектов и скорость зависят от реализации/задачи, не от одного имени метода.

~~~java
int wrapped = IntStream.of(Integer.MAX_VALUE, 1).sum();
long widened = IntStream.of(Integer.MAX_VALUE, 1).asLongStream().sum();
System.out.println(wrapped); // -2147483648
System.out.println(widened); // 2147483648
~~~

Расширять нужно **до** накопления: (long) intStream.sum() расширяет уже переполненный int. long тоже имеет ограниченный диапазон. При пустом IntStream sum() возвращает 0, average() — OptionalDouble.empty; summaryStatistics() имеет count=0, sum=0, min=Integer.MAX_VALUE, max=Integer.MIN_VALUE, average=0.0. Это значения пустой статистики, не измеренные экстремумы — сначала проверяйте count. Фактически проверены average и все пять полей; преобразование null из Stream<Integer> через mapToInt(Integer::intValue) при потреблении дало NPE. См. [CORE-NUMBERS](numbers.md), [IntSummaryStatistics](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/IntSummaryStatistics.html); более глубокая оптимизация/измерения — будущая A22, здесь не проверяются.

## 9. Префикс, бесконечные источники и mapMulti (ST-26 / ST-27 / ST-28 · P1)

**`takeWhile` и `dropWhile`** на *ordered* stream работают с префиксом: первая возвращает наибольший начальный участок подходящих элементов, вторая удаляет такой начальный участок. Они **не** равны `filter`, который отбирает все подходящие элементы где бы они ни находились:

~~~java
import java.util.List;

class Prefix {
    public static void main(String[] args) {
        List<Integer> v = List.of(1, 2, 8, 3, 4);
        System.out.println(v.stream().takeWhile(x -> x < 5).toList()); // [1, 2]
        System.out.println(v.stream().dropWhile(x -> x < 5).toList()); // [8, 3, 4]
        System.out.println(v.stream().filter(x -> x < 5).toList());    // [1, 2, 3, 4]
    }
}
~~~

Для unordered stream API не требует префикса по encounter order. Точный контракт для обоих вариантов при конечном входе:

| Условие | takeWhile | dropWhile |
|---|---|---|
| Все элементы подходят | Все элементы | Пусто |
| Ни один не подходит | Пусто | Все элементы |
| Подходят некоторые, но не все | Любое подмножество подходящих, включая пустое | Удаляет любое подмножество подходящих, включая пустое; все неподходящие сохраняются |

В новой последовательной unordered пробе для входа [1,2] и предиката x < 5 оба count соответственно 2,0, для [8,9] — 0,2. Для смешанного [1,8,2] фактически получились [1] и [8,2]; это лишь допустимый выбор, не гарантированный результат и не две взаимодополняющие операции в общем случае. На ordered stream обе операции stateful; takeWhile — также short-circuiting, dropWhile не имеет такой характеристики. Ordered parallel может требовать значительной координации для определения длиннейшего префикса. Не убирайте порядок только ради предполагаемого ускорения, если бизнес-смысл зависит от префикса. [Stream.takeWhile](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#takeWhile(java.util.function.Predicate)), [Stream.dropWhile](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#dropWhile(java.util.function.Predicate)). Доступны с Java 9.

**Источники:** двухаргументный Stream.iterate(seed,next) создаёт последовательный ordered неограниченный поток; Stream.generate(supplier) — последовательный unordered неограниченный поток. Их потребление обычно ограничивают, если нужен конечный итог. Stream.ofNullable(value) даёт ноль или один элемент. Трёхаргументный iterate(seed,hasNext,next) (Java 9+) подобен циклу с проверкой условия перед выдачей очередного элемента: если условие уже ложно на seed, поток пуст, если оно позже становится ложным, поток конечен. Он не обязательно завершается, если условие никогда не станет ложным. В новой пробе `Stream.iterate(1, x -> x < 4, x -> x + 1).toList()` дал [1, 2, 3]; отдельные запросы spliterator().hasCharacteristics(Spliterator.ORDERED) для двухаргументного iterate и generate дали true и false, без обхода бесконечного потока. Ниже исходная демонстрация конечного потребления:

~~~java
import java.util.stream.Stream;

class Sources {
    public static void main(String[] args) {
        System.out.println(Stream.ofNullable(null).count()); // 0
        System.out.println(Stream.iterate(1, x -> x + 1)
            .limit(3).toList());                             // [1, 2, 3]
        System.out.println(Stream.generate(() -> 67)
            .limit(2).toList());                             // [67, 67]
    }
}
~~~

**`mapMulti`** (Java 16+) позволяет для одного входного элемента передать **ноль, один или несколько** выходных элементов непосредственно в предоставленный `Consumer`; автор mapper не обязан создавать отдельный Stream на каждый вход:

~~~java
import java.util.List;

class MapMulti {
    static List<String> words() {
        return List.of("a,b", "c").stream()
            .<String>mapMulti((s, out) -> {
                for (String word : s.split(",")) out.accept(word);
            })
            .toList();                           // [a, b, c]
    }
}
~~~

В этом примере это альтернатива `flatMap(s -> java.util.Arrays.stream(s.split(",")))`, а не обещание, что любое `mapMulti` быстрее. Сигнатура, вывод типов и правила работы с `Consumer` указаны в [Stream.mapMulti](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html#mapMulti(java.util.function.BiConsumer)).

Точная форма: `<R> Stream<R> mapMulti(BiConsumer<? super T, ? super Consumer<R>> mapper)`. Вызов consumer.accept передаёт элемент результата; consumer допустимо использовать только в пределах текущего вызова mapper, нельзя сохранять его для последующего или асинхронного вызова (результат вне области вызова не определён). В данном выражении с цепочкой toList контекст не вынуждает компилятор вывести R=String, и без witness получается List<Object>: фактическая отрицательная компиляция `Stream.of("a").mapMulti((s,out) -> out.accept(s)).toList()` в контексте возврата List<String> отвергнута. Варианты `.<String>mapMulti((s,out) -> ...)` и явно типизированная lambda `.mapMulti((String s, java.util.function.Consumer<String> out) -> ...)` на входе "a,b","c" строго скомпилированы и дали [a, b, c]. Не утверждайте, что witness обязателен во всех контекстах.

API рекомендует mapMulti, когда для каждого элемента нужно выдать небольшое число результатов, или императивное построение удобнее, чем вложенный поток. По контракту default-реализации mapper накапливает элементы во внутреннем буфере, из которого создаётся Stream для flatMap. Конкретная реализация может оптимизировать это, но гарантии отсутствия буферизации, аллокаций или ускорения нет. Эксперимент с выходом Consumer за область mapper не проводился.

## 10. Checked IOException в map(Function) и ошибки во время terminal (ST-31, P1)

`Stream.map` принимает `Function<? super T, ? extends R>`; метод `Function.apply` **не** объявляет checked `IOException`. Поэтому объявление `throws IOException` у **внешнего** метода не делает следующую lambda допустимой:

~~~java
import java.io.IOException;
import java.util.stream.Stream;

class CheckedMapperWrong {
    // static void broken() throws IOException {
    //     Stream.of("x").map(s -> { throw new IOException("IO"); }).toList();
    // }
    // НЕ компилируется: checked IOException не разрешён контрактом Function.apply
}
~~~

Это **самостоятельный compile-negative**: если раскрыть строки без комментариев, `javac --release 21` отвергнет checked exception в lambda. Это не ошибка `onClose(Runnable)` и не тождественно N15 из исследовательского G1. Контракт SAM и правила lambda — [Function API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Function.html), [JLS §15.27](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27), [CORE-FUNCTIONS](functions.md).

Если преобразование действительно может завершиться IO-сбоем, можно обработать checked исключение **внутри** lambda и выразить результат в согласованном контракте, например через `UncheckedIOException`:

~~~java
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

class CheckedMapperHandled {
    static String read(String name) throws IOException {
        if (name.isEmpty()) throw new IOException("empty");
        return name.toUpperCase();
    }
    public static void main(String[] args) {
        Stream<String> pipeline = Stream.of("", "ok").map(name -> {
            try {
                return read(name);
            } catch (IOException e) {
                throw new UncheckedIOException("read failed", e);
            }
        });
        // Создание pipeline ещё не требует вызова read().
        try {
            pipeline.toList();
        } catch (UncheckedIOException e) {
            System.out.println(e.getCause().getMessage()); // empty
        }
    }
}
~~~

В этом заданном случае ошибка проявляется во время terminal, когда mapper реально потреблён. **Не** гарантируется, что любой mapper с побочным эффектом будет выполнен перед каждым terminal (`count` может оптимизировать), и обёртка unchecked сама по себе не решает retry/transaction semantics. [CORE-EXCEPTIONS](exceptions.md), [CORE-RESOURCES](resources.md).

## 11. Как выбрать инструмент для задачи и чего статья не обещает

Обычно начинают с вопроса об итоговом результате: нужен список — `map/filter/toList`; нужно установить существование — `anyMatch`; найти значение или зафиксировать отсутствие — `findFirst`/`Optional`; сумма по `int` — `mapToInt().sum()`; группировка и политика дублирующихся ключей — [CORE-STREAM-ADV](stream-advanced.md). Для существенных побочных эффектов, внешнего IO или мутации общего состояния **цикл нередко яснее и безопаснее**, чем скрытая цепочка lambda.

**Не обещается:** что Stream «быстрее любого цикла», что каждое `map/peek` обязательно вызовется, что `findAny` вернёт конкретный элемент, что parallel изменит только скорость, что терминальная операция закроет ресурсы или что `Stream.toList` вернёт ArrayList. Всё это либо противоречит контракту, либо зависит от конкретной реализации и workload.

## Карта ST-01–ST-36 для самостоятельного чтения

| ID / уровень | Где объяснён и где граница |
|---|---|
| ST-01–ST-02 · P0 | §§1–2: source/intermediate/terminal, deferred pipeline и оптимизация |
| ST-03–ST-04 · P0 | §3: filter/map/flatMap, Predicate/Function и отрицательные типовые примеры |
| ST-05–ST-06 · P0 | §4: stateful distinct/sorted, short-circuit limit |
| ST-07–ST-09 · P0 | §§5–6 и §2: findFirst/findAny/Optional, одно использование, peek/count |
| ST-10 · P0 | [CORE-STREAM-ADV](stream-advanced.md): encounter order и forEachOrdered |
| ST-11–ST-13 · P0 | §7: три контракта List, null/mutability |
| ST-14–ST-17 · P0 | [CORE-STREAM-ADV](stream-advanced.md): toMap и grouping |
| ST-18–ST-19 · P0 | §§8, 5: IntStream, OptionalInt, *Match boolean, пустая последовательность |
| ST-20–ST-22 · P0 | [CORE-STREAM-ADV](stream-advanced.md): reduce, parallel, close/onClose |
| ST-23–ST-25 · P1 | [CORE-STREAM-ADV](stream-advanced.md): Collector, Spliterator, onClose errors |
| ST-26 / ST-27 / ST-28 · P1 | §9: takeWhile/dropWhile, generators и mapMulti |
| ST-29–ST-30 · P1 | [CORE-STREAM-ADV](stream-advanced.md): mutable reduction и изменение источника |
| ST-31 · P1 | §10: checked Function и deferred unchecked failure |
| ST-32 · P1 | [CORE-STREAM-ADV](stream-advanced.md): concurrent grouping |
| ST-33–ST-36 · P2 | [CORE-STREAM-ADV](stream-advanced.md): внешние IO, scheduler, custom Collector, API после 21 — только границы |

**Связь с результатами поиска:** [CORE-OPTIONAL](optional.md) подробно объясняет `Optional.empty/of/ofNullable`, `get/orElseThrow`, `orElse` vs `orElseGet`, `map/flatMap/filter`, value-based equality и `Optional.stream()` как поток нуля или одного элемента. Здесь же остаётся основной материал о Stream pipeline, терминальных операциях и `findFirst`.

## Источники и границы воспроизводимости

**Официальные нормы:** [Stream API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html), [java.util.stream package](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html), [Collectors](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collectors.html), [IntStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/IntStream.html), [Optional](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html), [JLS §15.27](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27). Независимые интервью-источники и матрица — [G1-план](../../docs/plans/CORE-STREAMS-ADV.md), [реестр источников](../../docs/SOURCE_REGISTRY.md). **JLS не задаёт внутренний планировщик Stream**; библиотечные правила — в Java SE API.

**Статус авторских inline-примеров:** демонстрационные классы предназначены для отдельного копирования в собственные файлы, а закомментированные строки специально показывают compile-negative. Приведённые комментарии с результатами описывают ожидаемые результаты для показанных входов, а не доказательство запуска **всех** snippets из Markdown. Для сопоставления с теорией доступен [общий запускаемый пример с двумя программами](../../examples/core/streams/README.md), соответствующими G1 §6 и §5a: `StreamPipelineProbes.java` P01–P33 и `StreamG1Clarifications.java` C01–C11. Роль 03 на Debian OpenJDK 21.0.12.1 реально выполнила `bash examples/core/streams/verify.sh`: обе программы скомпилированы (`javac --release 21`), запущены и прошли общий `diff -u` (33+11 строк). **Это проверка внешних Java-файлов, не автоматическая компиляция всех snippets внутри статей.** Автор 02 отдельно проверил 17/17 compile-negative N01–N17 и `CheckedFunctionProbe` по ожидаемым категориям javac на OpenJDK 21.0.12.1; эти исходники временные и **не включены в runnable/CI**. Проверка общего runnable в GitHub Actions завершилась успешно на [зафиксированном коммите `35f85f8…`](https://github.com/Melchiorius/backend-interview-labs/actions/runs/44940716537); в CI запускаются две Java-программы и сравнивается эталонный вывод, **но не** все блоки кода из этих статей и не negative-пробы. **NOT RUN до отдельной проверки:** автоматическое извлечение/компиляция всех блоков Markdown, GUI Obsidian/IntelliJ, другие JDK/JVM, JMH, IO, параллельные гонки.

**Новые авторские проверки D-A13a (02, Java 21).** Полные временные исходники, команды, фактические потоки и хеши — в [задаче #345](https://github.com/Melchiorius/backend-interview-labs/issues/345). Debian OpenJDK 21.0.12.1+1-1-deb13u1-Debian, javac 21.0.12.1, LC_ALL=C.UTF-8; переменные JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS, _JAVA_OPTIONS, JDK_JAVAC_OPTIONS, CLASSPATH удалены. LocaleProbe и Probe вместе скомпилированы один раз командой `javac --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror LocaleProbe.java Probe.java` (exit 0, оба потока пусты). Четыре JVM с `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8`: LocaleProbe без явных флагов локали и с -Duser.language=en -Duser.country=US / tr/TR, плюс Probe. Все exit 0/stderr пуст; LocaleProbe проверяет фактические язык, страну (для en_US/tr_TR) и кодировки; 3 вывода по 5 строк, 136/141/142 байта. Probe — 35 строк, 1078 байт; пойманные исключения (например NPE/UOE) выведены как наблюдения, не ненулевые коды JVM. Пять отдельных отрицательных исходников компилировались с теми же флагами javac, но без -Werror; все exit 1, stdout пуст, JVM не запускалась. Итого 10 вызовов (одна строгая компиляция, четыре JVM, пять negative), отдельно от исторических 17. Кодовые фрагменты статьи иллюстрируют эти проверки, но не все fences автоматически извлекались и компилировались.

**Новый запуск прежнего runner (02, без изменений).** Восстановлены четыре точных входных файла (два Java, expected.stdout, verify.sh), не полный клон. Git SHA и mode сверены до и после каждого запуска. В default POSIX окружении без перечисленных переменных, затем отдельно с JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8" и аналогично tr/TR исходный bash verify.sh завершился 0 во всех трёх случаях; внешний stderr базового запуска пуст, в двух вариантах с инъекцией — по три строки Picked up JAVA_TOOL_OPTIONS (336 байт), это не строгая проверка пустого stderr. locale -a: C, C.utf8, POSIX; одноимённые en_US/tr_TR локали ОС отсутствуют, но JVM locale проверена отдельным LocaleProbe, а неизменённый runner сам свойства не печатает. Скрипт выполняет одну компиляцию двух исходников, две JVM, diff -u общего stdout, cat и маркер; не добавляет -Xlint/-Werror/encoding, не отвергает непустой stderr и не проверяет хеши внутри. Все три build/actual.stdout совпали с expected.stdout (cmp exit 0), 44 строки/650 байт, SHA-256 42c1854ebf93d1d3b6e79571a8086c9e6d96dfb81a2daeaa4e8ffece21f77f82. Внешний stdout каждого запуска — эти же 44 строки и маркер CHECK PASSED: stdout matches expected.stdout (45 строк/695 байт), SHA-256 f97f7d5661d666a7bb63a67a6cb56b569bf18bfdd894ece3e0287dec6869dfec, cmp с эталоном и маркером exit 0. Это 44 наблюдения из двух JVM за запуск, не 44 теста. Не утверждается, что прежняя ошибка Transform была в общем runner: его собственный вывод сохранился. Временные пробы не входят в CI. Не проверялись другие JDK/ОС, все inline fences, недопустимый escaped Consumer, зависающие бесконечные цепочки, benchmark, стресс параллельного выполнения, GUI/HTTP/anchors, дальнейшие A13b/A22/A24 критерии.

**Связанный C2:** [Iterator/ListIterator и Spliterator](../02-collections/iteration.md) сопоставлены по контрактам состояния, `forEachRemaining` и моделям обхода; это не доказательство атомарности параллельного `Stream`. Для `ArrayList` и `ArrayDeque` см. [последовательные коллекции](../02-collections/sequential.md).

**Дальше:** [CORE-STREAM-ADV — reduce/Collector/parallel/resources](stream-advanced.md); при необходимости вернитесь к [CORE-FUNCTIONS](functions.md), [CORE-EQUALITY](equality.md) и [CORE-EXCEPTIONS](exceptions.md).
