# CORE-GENERICS — параметры типов, инвариантность, wildcards и вывод типов

**Java SE 21.** Исследовательская основа — [принятый план CORE-GENERICS/ERASURE](../../docs/plans/CORE-GENERICS-ERASURE.md), Issue [#82](https://github.com/Melchiorius/backend-interview-labs/issues/82). Эта статья объясняет, какие операции **разрешены компилятором** для параметризованных типов. Продолжение о том, что остаётся при выполнении программы, — [CORE-ERASURE](erasure.md). Предпосылки: [типы и ссылки](types.md), [классы и методы](classes.md), [преобразования](conversions.md), [наследование](inheritance.md).

## Зачем generics, если Java уже проверяет типы

Пусть сервис получает объект по ключу из контейнера. Без параметра типа клиенту пришлось бы самостоятельно приводить результат к нужному классу — и ошибка могла бы обнаружиться только при исполнении. С параметром `T` компилятор проверяет согласованность значения, хранящегося в контейнере, с типом результата.

~~~java
final class Box<T> {
    private final T value;
    Box(T value) { this.value = value; }
    T get() { return value; }
}

Box<Integer> box = new Box<>(67);
Integer result = box.get();          // корректно
// String wrong = box.get();         // ошибка компиляции: Integer не String
~~~

`T` — **переменная типа**, а не имя класса, создаваемого для каждого значения `T`. `Box<T>` — объявление обобщённого класса; `Box<Integer>` — параметризованный тип; `Integer` — фактический аргумент типа. Компилятор сопоставляет объявленное ограничение с употреблением. Это не означает, что при выполнении Java создаёт отдельный класс `Box<Integer>`: границы runtime описаны в [стирании типов](erasure.md). Нормы: [JLS 21 §8.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.2), [§4.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.5).

Классы `Box<Integer>` и `Box<String>` используют **одно объявление** с разными статическими контрактами. В частности, это помогает библиотекам возвращать строго типизированные значения и проверять вызовы на этапе компиляции. При использовании raw types и unchecked-преобразований часть гарантий можно обойти — это не опровержение цели generics.

## Параметр класса, параметр метода и границы (GE-01, GE-02)

В обобщённом **методе** параметр объявляется *до* типа результата; он не обязан совпадать с параметром окружающего класса.

~~~java
static <T> T first(T a, T b) {
    return a;
}

String s = first("a", "b");
Integer n = first(67, 68);
~~~

Если алгоритму нужны операции, которых нет у `Object`, задаём верхнюю границу (*bound*). Для двух и более ограничений используется `&`; если имеется класс, он должен стоять первым.

~~~java
static <T extends Number & Comparable<T>> int compare(T a, T b) {
    return a.compareTo(b);
}

int order = compare(67, 68); // отрицательное значение
// compare("67", "68");      // ошибка: String не Number
~~~

Компилятор знает, что `T` одновременно является `Number` и реализует `Comparable<T>`, поэтому допускает вызов `compareTo`. Порядок границ важен также при **erasure**: стиранием `T` станет первая граница (здесь `Number`), а не произвольный общий класс. Подробнее: [JLS §4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.4) и [erasure](erasure.md).

Граница `<T extends Number>` относится к *конкретной переменной типа T*: в одном вызове все позиции `T` связаны. Это не то же самое, что `List<? extends Number>`, где компилятор рассуждает о **неизвестном** аргументе типа.

**Статический член.** Параметр `T` обычного обобщённого класса нельзя использовать как тип статического поля, поскольку оно одно для класса, а `T` выбирается для параметризации:

~~~java
class Holder<T> {
    T instanceValue;        // корректно
    // static T shared;      // ошибка компиляции
    static <U> U identity(U value) { return value; } // свой U у метода
}
~~~

## Почему List<Integer> не является List<Number> (GE-03)

Хотя `Integer` — подтип `Number`, отсюда **не следует** отношение подтипов между `List<Integer>` и `List<Number>`. Параметризованные типы с разными определёнными аргументами в данном случае **инвариантны**. Рассмотрим, какую проблему предотвратил компилятор:

~~~java
List<Integer> integers = new ArrayList<>();
integers.add(67);
// List<Number> numbers = integers; // ошибка компиляции
~~~

Если бы присваивание разрешили, код мог бы затем вызвать `numbers.add(3.14)`. Это поместило бы `Double` в ту же коллекцию, которую другая ссылка считает списком целых. Ограничение защищает соглашение о типе элемента на этапе компиляции.

Но `Integer` по-прежнему можно использовать *там, где нужно отдельное значение Number*: `Number value = Integer.valueOf(67);`. Разница — в наличии **изменяемого контейнера** и разных возможных операций чтения/записи. Нормы: [JLS §4.10.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.10.2), [List API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html).

## Три разных контракта: List<Object>, List<?>, List<? extends Object> (GE-04)

`List<Object>` обещает возможность работать с элементами как с `Object` и, при поддержке изменяющих операций конкретной коллекцией, записывать объекты подходящих типов. Это **не** супертип для любого `List<String>`.

~~~java
List<String> strings = new ArrayList<>(List.of("a"));
List<?> unknown = strings;                // корректно
List<? extends Object> anyObjects = strings; // корректно
// List<Object> objects = strings;        // ошибка компиляции

Object read = unknown.get(0);
// unknown.add("b");                      // ошибка: неизвестен аргумент типа
~~~

`List<?>` означает «список некоторого неизвестного элементного типа», а не «список Object». Во многих обычных сценариях `List<?>` и `List<? extends Object>` дают сходные разрешения чтения/записи, однако запись второго типа явно задаёт верхнюю границу. Обобщения об их эквивалентности **во всех** языковых контекстах делать не нужно: правила capture и wildcard определяются конкретным выражением. Даже если статический тип допускает вызов `add`, конкретный список может не поддерживать изменение.

## Producer: ? extends Number (GE-05, GE-19)

~~~java
List<? extends Number> producer =
        new ArrayList<Integer>(List.of(67));

Number value = producer.get(0); // чтение безопасно
// producer.add(68);            // ошибка компиляции
producer.add(null);             // компилятор допускает
~~~

Почему нельзя добавить `Integer`? Неизвестный аргумент может оказаться `Integer`, `Double` или другим подтипом `Number`; запись конкретного ненулевого значения не безопасна для **каждого** такого типа. При чтении, напротив, любое значение — как минимум `Number`.

`null` совместим с любым ссылочным типом и потому особый случай. Но успешная **компиляция** вызова `add(null)` не обещает, что конкретная коллекция разрешает изменение или `null`: неизменяемый список выбросит `UnsupportedOperationException`, а некоторые коллекции запрещают `null`. Wildcard **не делает** исходную коллекцию неизменяемой; он лишь ограничивает операции через эту ссылку.

## Consumer: ? super Integer (GE-06, GE-19)

~~~java
List<? super Integer> consumer = new ArrayList<Number>();
consumer.add(67);               // корректно
Object item = consumer.get(0);  // известен только Object
// Integer x = consumer.get(0); // ошибка компиляции
~~~

Список может иметь аргумент `Integer`, `Number` или `Object`; все три способны принять `Integer`. Но вернуть `Integer` гарантированно нельзя: фактическая коллекция с элементами `Number` могла изначально содержать другие подклассы, а у `List<Object>` вообще нет обещания, что каждый элемент — Integer. Нельзя выводить тип чтения из того, что **сам вызывающий код только что добавил** конкретное число: статическая гарантия проверяет контракт ссылки независимо от этой истории.

Слово `super` здесь относится к ограничению аргумента типа, а не к вызову метода суперкласса. Сравнение с наследованием классов — в [CORE-INHERITANCE](inheritance.md).

## PECS в реальном API: Collections.copy (GE-07)

PECS (*Producer Extends, Consumer Super*) — удобная памятка, **не термин JLS**. Она помогает выбирать границы аргументов, когда один источник **производит** элементы, а другой их **принимает**.

~~~java
List<Integer> source = List.of(67, 68);
List<Number> destination =
        new ArrayList<>(List.of(0, 0));

Collections.copy(destination, source);
System.out.println(destination); // [67, 68]
~~~

Контракт метода библиотеки:

~~~java
public static <T> void copy(
        List<? super T> dest,
        List<? extends T> src)
~~~

`src.get(i)` отдаёт значение типа `T`; `dest.set(i, value)` его принимает. Поэтому размер `destination` должен быть **не меньше** размера источника: `Collections.copy` **не добавляет** элементы, а заменяет имеющиеся. Назначение также должно поддерживать `set`. Если целевой список слишком короткий — `IndexOutOfBoundsException`; если изменение запрещено — `UnsupportedOperationException`. Это реальные границы **API**, а не ограничения generics. [Java 21 Collections.copy](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#copy(java.util.List,java.util.List)).

Возможность принять один тип и возвратить другой зависит от **направления движения значений**. PECS не требует расставлять wildcard на каждом параметре публичного метода: когда значение и возвращаемый результат должны иметь один и тот же `T`, именованный параметр типа может выразить связь точнее.

## Захват неизвестного типа: wildcard capture (GE-08)

На первый взгляд кажется, что переставить элементы `List<?>` нельзя: мы же не знаем тип элемента для записи. Компилятор действительно отвергает прямую запись извлечённого `Object`:

~~~java
// void replace(List<?> list) {
//     list.set(0, list.get(0)); // ошибка: Object нельзя записать как захваченный тип
// }
~~~

Чтобы удержать **один и тот же неизвестный** тип для чтения и записи, передадим список в generic helper:

~~~java
static void duplicateFirst(List<?> values) {
    duplicateFirstHelper(values);
}

private static <T> void duplicateFirstHelper(List<T> values) {
    values.set(1, values.get(0));
}

List<String> words = new ArrayList<>(List.of("a", "b"));
duplicateFirst(words);
System.out.println(words); // [a, a]
~~~

При вызове helper компилятор захватывает неизвестный тип списка как некоторое `T`. Он не выясняет, что это обязательно `String`, но знает, что результат `get` и аргумент `set` относятся к **одному** `T`. Helper не создаёт возможности записать произвольный `Integer` в список строк. Нужны также два элемента и поддержка `set` — иначе возможна runtime-ошибка, не связанная с типизацией. [JLS §5.1.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.10).

## Почему массивы ведут себя иначе (GE-09)

Массивы Java **ковариантны**: `Integer[]` допустимо присвоить переменной `Number[]`. В отличие от параметризованных списков, компонентный тип массива проверяется также при записи во время выполнения.

~~~java
Integer[] integers = {67};
Number[] asNumbers = integers; // разрешено
try {
    asNumbers[0] = 3.14;
} catch (ArrayStoreException expected) {
    System.out.println("Нельзя поместить Double в Integer[]");
}
~~~

Проверяется **реальный** тип массива `Integer[]`, а не только статический `Number[]`. Generics с инвариантностью обычно отсекают похожую ошибку **раньше**, при компиляции. Не путайте `ArrayStoreException` с `ClassCastException`: это разные проверки. [JLS §10.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.5). Подробное устройство массивов — отдельная тема CORE-ARRAYS.

## Вывод аргументов типа, diamond и контекст (GE-17, GE-25)

~~~java
List<Integer> numbers = new ArrayList<>();
numbers.add(67);

List<Integer> empty = Collections.emptyList();
Integer result = first(67, 68); // T выводится по аргументам
~~~

`<>` (diamond) позволяет компилятору вывести часть аргументов типа из окружающего контекста, а `Collections.emptyList()` — generic method, где тип результата также связывается с ожидаемым типом. Это не значит, что Java «всегда сама угадывает» желаемый тип: overload resolution, ограничения bounds и контекст присваивания могут сделать программу неоднозначной или недопустимой.

~~~java
// List<int> primitives = new ArrayList<int>(); // ошибка: type argument — ссылочный тип
List<Integer> boxed = new ArrayList<>();        // Integer — ссылочная обёртка
~~~

`int` как аргумент типа недопустим; нужны ссылочные типы. Автоупаковка `int → Integer` при помещении числа в `List<Integer>` не отменяет этого ограничения. Граничные случаи inference и перегрузки не следует выводить из одного diamond-примера; конкретные правила — [JLS §15.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12), [§5.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2).

> [!note]- Generic constructors, явные аргументы метода и сложный вывод
> Конструктор может иметь **собственный** параметр типа, не совпадающий с параметром класса. У generic method иногда полезно явно задать `ClassName.<T>method(...)`, если контекст вывода не выражает нужной связи. Вывод типов с intersection types, overloaded methods и lambdas становится отдельным существенным механизмом, а не правилом «diamond всегда работает». См. [JLS §8.8.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.4) и [CORE-FUNCTIONS](functions.md).

## Что generics НЕ гарантируют

- `? extends Number` не доказывает неизменяемость или потокобезопасность коллекции; `? super Integer` не гарантирует тип `Integer` при чтении.
- Успешный `javac` означает соблюдение проверяемого статического контракта **для данного исходника**; raw/unchecked-код может нарушить обещания типов и обнаружиться позже.
- Нет отдельного runtime-класса `List<Integer>`, автоматически проверяющего при извлечении все аргументы типа: [что стирается, какие проверки сохраняются и почему возникает ClassCastException](erasure.md).
- PECS не выражает владение объектом, конкурентные гарантии или правила бизнес-валидации; это правило выбора generic-ограничений для движения значений.

## Карта покрытия G1 и границы

| ID | Где читатель получает ответ |
|---|---|
| GE-01, GE-02 | Обобщённые классы/методы и границы; runtime-часть GE-02 — [erasure](erasure.md) |
| GE-03, GE-04 | Инвариантность и различие `List<Object>`/`List<?>` |
| GE-05, GE-06, GE-07 | Чтение/запись через extends/super и `Collections.copy` |
| GE-08, GE-09 | Захват wildcard и контраст массивов |
| GE-17 | Inference, diamond, boxing |
| GE-19, GE-21, GE-25 (P1) | Особые случаи `null`, статический член, контекст вывода |
| GE-26, GE-30 (P2) | Recursive/F-bounds, intersection types и deep inference — обозначены как дальнейшее углубление, не как полностью реализованная теория |

> [!note]- P2: рекурсивные границы
> В `class Node<T extends Comparable<T>>` параметр `T` появляется внутри собственного ограничения. Это обычная техника для статической связи операций сравнения; строгая теория F-bounded polymorphism и взаимодействие intersection types с inference требуют отдельного материала. Нельзя по этому примеру делать общие выводы о всех recursive bounds — см. [JLS §4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.4).

**Источники:** [JLS 21 §4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html), [§5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html), [§8.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.2), [§15.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12), [Collections.copy API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#copy(java.util.List,java.util.List)). [Реестр источников](../../docs/SOURCE_REGISTRY.md) разделяет нормативные и интервью-материалы; подборки вопросов не служат доказательством языковых гарантий.

**Один воспроизводимый пример Java 21:** [CORE-GENERICS + CORE-ERASURE — README, исходник и полный stdout](../../examples/core/generics-erasure/README.md). Программа `GenericsProbes.java` воспроизводит **P01–P20 из §5 принятого G1-плана**: PECS/`Collections.copy`, `? extends` и `add(null)`, `? super`, capture-helper, bounds/inference, erasure и контролируемые runtime-исключения. Это **20 строк одной программы**, не двадцать самостоятельных тестов и не замена независимых негативных N01–N22 / lint W01–W04. Для P17 число reflection-методов зависит от компилятора; точное `2,1,child` подтверждено в принятой среде OpenJDK/javac 21, **не** является гарантией JLS. Результат и границы проверки приведены в README.

**Переход дальше:** [CORE-ERASURE](erasure.md) → [наследование](inheritance.md) → [функциональные интерфейсы и lambda](functions.md).
