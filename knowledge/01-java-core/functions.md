# Функциональные интерфейсы, лямбды и ссылки на методы в Java 21

Лямбда в Java — не «сокращённый анонимный класс» и не самостоятельный объект-функция с фиксированным типом. Это выражение, смысл которого определяется **целевым функциональным интерфейсом**. При компиляции важно знать его единственную абстрактную операцию, типы параметров и результата, допустимые checked exceptions и окружение, в котором записано выражение.

Такая модель объясняет сразу несколько собеседовательных случаев: почему `var f = x -> x + 1` не компилируется, почему `String::length` требует аргумент-строку, почему при `missing == null` выражение `missing::trim` бросает `NullPointerException` раньше `() -> missing.trim()` и почему `this` в lambda не равен `this` в anonymous class.

Перед чтением полезны [интерфейсные методы и SAM](interfaces.md), [типы и значения](types.md), [перегрузки и наследование](inheritance.md), [вложенные и анонимные классы](nested-classes.md) и [контракты Object](object.md). Полная матрица проверок NF-01–NF-30 приведена в [принятом исследовательском плане](../../docs/plans/CORE-NESTED-FUNCTIONS.md). **Общий Java 21 runnable:** [инструкция и разбор](../../examples/core/nested-functions/README.md), [полный исходник](../../examples/core/nested-functions/src/NestedFunctionsProbes.java), [17 строк эталона](../../examples/core/nested-functions/expected.stdout). P09–P14/P17 из него связывают target typing, четыре ссылки на методы, bound receiver, overload и checked exception. 17 строк — наблюдения **одного** запуска.

**Воспроизведение.** Из корня репозитория: `bash examples/core/nested-functions/verify.sh` (JDK 21, Bash). Новые фрагменты относятся к отдельному временному [Probe.java, его main, исходникам отрицательных файлов и фактическим командам](https://github.com/Melchiorius/backend-interview-labs/issues/354#issuecomment-7224888344); они не добавлены в общий runner. Вспомогательные типы/методы Probe объявлены вне main, операторы проверки находятся в main; не склеивайте все фрагменты в один файл.

## 1. Что означает функциональный интерфейс (SAM)

**Functional interface** — интерфейс Java, который **не объявлен `sealed`** и имеет одну абстрактную операцию в смысле [JLS 21 §9.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8). Считаются именно совместимые унаследованные abstract-методы после специальных правил override-equivalent; `default`, `static`, `private` и подходящие public-методы `Object` не образуют второй функциональной операции.

```java
@FunctionalInterface
interface Rule {
    int apply(int value);
    boolean equals(Object other); // не вторая SAM-операция

    default int twice(int value) {
        return apply(apply(value));
    }
}

class UseRule {
    public static void main(String[] args) {
        Rule plusOne = x -> x + 1;
        System.out.println(plusOne.apply(67)); // 68
        System.out.println(plusOne.twice(67)); // 69
    }
}
```

`@FunctionalInterface` просит компилятор проверять корректность объявления; сама по себе аннотация не является условием функциональности. Интерфейс с двумя независимыми abstract-методами не функционален — даже если программист поставит аннотацию.

```java
@FunctionalInterface
interface Broken {
    void first();
    void second(); // N15: две независимые abstract-операции
}
```

И ещё одна менее очевидная ошибка: объявленный `sealed interface` с одной abstract-операцией **не является functional interface**. Назначить ему lambda невозможно. Подробный пример `ClosedPolicy` и нормативный квалификатор разобраны в [CORE-INTERFACES](interfaces.md), [JLS §9.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8).

В библиотеке Java 21 готовые SAM: [`Runnable`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runnable.html) (`void run()`), [`Supplier<T>`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Supplier.html) (`T get()`), [`Function<T,R>`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Function.html) (`R apply(T)`), [`IntUnaryOperator`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/IntUnaryOperator.html) (`int applyAsInt(int)`). Это **разные контракты**, а не просто четыре имени для произвольной функции.

Краткая карта стандартных SAM ([java.util.function](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html)):

| Тип | Функциональный метод |
|---|---|
| `Supplier<T>` | `T get()` |
| `Consumer<T>` | `void accept(T t)` |
| `Predicate<T>` | `boolean test(T t)` |
| `Function<T,R>` | `R apply(T t)` |
| `UnaryOperator<T>` | `T apply(T t)` (Function с одинаковыми входом/выходом) |
| `BiConsumer<T,U>` / `BiPredicate<T,U>` | `void accept(T t,U u)` / `boolean test(T t,U u)` |
| `BiFunction<T,U,R>` / `BinaryOperator<T>` | `R apply(T t,U u)` / `T apply(T t,T u)` |
| `IntSupplier` / `IntConsumer` / `IntPredicate` | `int getAsInt()` / `void accept(int)` / `boolean test(int)` |
| `IntFunction<R>` / `ToIntFunction<T>` / `IntUnaryOperator` | `R apply(int)` / `int applyAsInt(T)` / `int applyAsInt(int)` |

Специализации уменьшают необходимость boxing на соответствующей границе вызова; не обещают универсальную производительность. У Function `compose(before)` сначала вызывает before, затем текущую функцию; `andThen(after)` — наоборот. В Probe для plus=x→x+1, twice=x→x*2 на 33 получены 67 и 68 соответственно. [Function API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Function.html).

## 2. Target typing: откуда lambda получает тип

Lambda — *poly expression*: её допустимость и интерпретация зависят от целевого типа в контексте присваивания, аргумента вызова или приведения. Целевой тип должен быть функциональным интерфейсом, совместимым по параметрам, результату и исключениям.

```java
import java.util.function.*;

class Targets {
    public static void main(String[] args) {
        IntUnaryOperator increment = x -> x + 1;
        Function<String, Integer> length = s -> s.length();

        System.out.println(increment.applyAsInt(67)); // 68
        System.out.println(length.apply("abc"));     // 3

        // var unknown = x -> x + 1; // N07: нет target type
        var known = (IntUnaryOperator) (x -> x + 1);
        System.out.println(known.applyAsInt(67));     // 68
    }
}
```

`var` выводит тип из уже типизированного initializer, но не создаёт функциональный тип для lambda «из ничего». Приведение в последнем примере **предоставило** target type, после чего `var` может вывести тип всей конструкции. Подробности: [JLS §15.27.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.3), [§5.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2).

Различайте functional interface declaration и конкретный **functional target type** с function type (параметры, возвращаемый тип, throws). Интерфейс может иметь обобщённый SAM: в Probe `interface Generic { <T> T id(T t); }`, `static <T> T identity(T t){return t;}`, `Generic g = Probe::identity;` — компилируется и g.id(67) вернул 67. Lambda не может реализовать function type с собственными type parameters; отдельный GenericLambda с `G g=x->x;` отклонён invalid.generic.lambda.target. Параметризованный интерфейс с уже выбранным T — иной случай. Intersection cast `Runnable r=(Runnable & Serializable)()->{};` допустим, если пересечение индуцирует функциональный интерфейс; Probe проверил вызов и принадлежность Serializable, не сериализацию/совместимость. См. [JLS §9.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.9), [§15.27.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.3), [generics](generics.md).

Нельзя судить о lambda только по записи `x -> x + 1`: здесь тип `x` и допустимое значение результата задаёт контекст. Перегрузка методов с разными функциональными параметрами потому и требует отдельного анализа.

## 3. Expression body, block body и возвращаемое значение

Выражение справа от `->` может иметь вид *expression body* либо *block body*. Для value-совместимого SAM результат должен удовлетворять его возвращаемому типу; для `void`-SAM допустимо соответствующее statement expression или block, который завершается по правилам void-совместимости.

```java
import java.util.function.IntUnaryOperator;

class Bodies {
    public static void main(String[] args) {
        IntUnaryOperator shortForm = x -> x + 1;
        IntUnaryOperator block = x -> {
            int result = x + 1;
            return result;
        };
        Runnable task = () -> System.out.println("run");

        System.out.println(shortForm.applyAsInt(67)); // 68
        System.out.println(block.applyAsInt(67));     // 68
        task.run();                                   // run
    }
}
```

Отрицательные случаи полезнее заученного синтаксиса:

```java
class WrongResult {
    Runnable task = () -> 67; // N12: литерал 67 — не statement expression для void SAM
}
```

Но такой вариант корректен, хотя вызванный метод возвращает значение:

```java
import java.util.List;
class IgnoredResult {
    Runnable task = () -> List.of(67).size();
}
```

Block body void-compatible, если все return имеют вид `return;`; value-compatible, если блок не может завершиться нормально и все return имеют выражение `return expression;`. Блок `{ throw new IllegalStateException(); }` подходит обоим: в Probe присвоен Runnable и IntSupplier, каждый вызов бросил исключение. Блок `x -> { if(x>0) return x; }` для IntUnaryOperator отклонён missing.ret.val: существует путь нормального завершения. Не путайте с expression body: method invocation — допустимое statement expression, простой литерал для void SAM — нет.

Метод invocation разрешён как statement expression, его результат может быть проигнорирован. Для этого не нужно менять `Runnable` на `Supplier<Integer>`. Точные правила: [JLS §15.27.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.2).

## 4. Захват переменных: effectively final и definite assignment

Lambda может читать local variable или parameter из охватывающей области только при соблюдении требований `final/effectively final` и **definite assignment**. Даже присваивание того же значения может нарушать effectively final; точные правила для инициализированных/неинициализированных locals, parameters, ++/-- и DA, включая ветви и enhanced-for, в [CORE-NESTED §4](nested-classes.md). Изменение состояния объекта через захваченную ссылку само по себе не нарушает effectively final: это не присваивание самой локальной переменной.

```java
import java.util.function.Supplier;

class Capture {
    public static void main(String[] args) {
        int base = 67;
        Supplier<Integer> fixed = () -> base;

        int[] box = {67};
        Supplier<Integer> fromArray = () -> box[0];
        box[0] = 68;

        System.out.println(fixed.get());     // 67
        System.out.println(fromArray.get()); // 68
    }
}
```

`fixed` захватывает значение переменной `base`; `fromArray` использует неизменную локальную ссылку `box`, но наблюдает новое состояние самого массива.

```java
import java.util.function.Supplier;
class InvalidCapture {
    void bad() {
        int value = 67;
        Supplier<Integer> get = () -> value; // N05: захват переменной
        value++;                             // делает её не effectively final
    }

    void alsoBad() {
        int missing;
        Supplier<Integer> get = () -> missing; // N16: missing ещё не definitely assigned
        missing = 67;
    }
}
```

Оба примера **специально не компилируются**. Для проверки каждой ошибки используйте отдельный файл — иначе одна диагностика может замаскировать другую. Аналогичные ограничения есть у local/anonymous class; различия контекста разобраны в [CORE-NESTED](nested-classes.md). Нормы: [JLS §4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4), [§6.5.6.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.5.6.1), [§16](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html).

**Это не потокобезопасность.** `effectively final` ограничивает переприсваивание локальной переменной; оно не защищает изменяемый массив, объект, поле или другой поток от data race.

## 5. Значение `this` в lambda — лексическое

У lambda нет нового `this`, отличного от охватывающего контекста. В instance-методе `this` обозначает экземпляр охватывающего класса; из `static`-контекста не возникает доступ к instance state.

```java
import java.util.function.Supplier;

class WhichThis {
    private final int id = 67;

    void show() {
        Supplier<Boolean> lambda = () -> this.id == 67;
        Supplier<Boolean> anonymous = new Supplier<>() {
            @Override public Boolean get() {
                return (Object) this == WhichThis.this;
            }
        };

        System.out.println(lambda.get());    // true
        System.out.println(anonymous.get()); // false
    }
}
```

Anonymous class создаёт собственный экземпляр и тем самым собственный `this`. Lambda наследует лексический смысл `this`/`super` и имён из окружающего контекста. Это не «передача this как скрытого аргумента» в нормативной модели языка. [JLS §15.27.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.2), [§15.9.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9.5).

```java
class StaticContext {
    private int value = 67;

    static Runnable wrong = () -> {
        // System.out.println(this);  // N06: this отсутствует
        // System.out.println(value); // N20: нет implicit receiver
    };
}
```

Если нужен доступ к экземпляру, явно передайте ссылку на него или используйте instance context.

### Область имён и синтаксис параметров lambda

В отличие от метода анонимного класса lambda не может переобъявить параметром имя локальной переменной из охватывающей области:

```java
import java.util.function.Function;
class Shadow {
    void wrong() {
        int x = 67;
        // Function<Integer,Integer> f = x -> x + 1; // N19
        Function<Integer,Integer> valid = input -> input + x;
    }
}
```

Допустимы `x -> x+1`, `(x) -> x+1`, `(int x) -> x+1` и `(var x) -> x+1` для подходящего IntUnaryOperator. Только одиночный inferred identifier допускает отсутствие скобок; ноль и несколько параметров требуют скобок. Для нескольких нельзя смешивать inferred, explicit types и var; в Probe `(var x,var y)->x+y` с BiFunction дал 67, MixedVar `(var x,y)->x+y` отдельно отклонён. Lambda параметры и locals в block body имеют свою область видимости, но не могут переобъявлять имена охватывающих locals/parameters в их scope, кроме границы вложенного класса; имя поля можно затенить, поле доступно через this в instance context. Пример трёх одноимённых значений — [CORE-NESTED §4](nested-classes.md).

Имена параметров lambda должны не конфликтовать с именами локальных переменных в соответствующих областях видимости. Подробнее [JLS §6.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.4) и [§15.27.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.1).

## 6. Checked exceptions — часть совместимости с SAM

В checked exceptions важно не то, что lambda «функция», а то, что её тело реализует конкретный метод функционального интерфейса. `Runnable.run()` не объявляет checked `IOException`, поэтому бросить его из lambda без обработки нельзя.

```java
import java.io.IOException;

class CheckedExample {
    @FunctionalInterface
    interface CheckedTask {
        void run() throws IOException;
    }

    public static void main(String[] args) {
        CheckedTask task = () -> { throw new IOException("io"); };
        try {
            task.run();
        } catch (IOException e) {
            System.out.println(e.getMessage()); // io
        }
    }
}
```

```java
import java.io.IOException;
class WrongThrows {
    Runnable task = () -> {
        throw new IOException("io"); // N08: Runnable.run() не throws IOException
    };
}
```

Обратный пример `CheckedTask` компилируется не благодаря «обёртке лямбды», а потому что *functional method* разрешает данное исключение. Дальнейшие правила вызова метода всё равно требуют обработку/объявление checked exception. Дополнительный разбор checked/unchecked и распространения исключений — в [CORE-EXCEPTIONS](exceptions.md). Нормы: [JLS §11.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2), [§15.27.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.3).

Для method reference checked exceptions выбранного метода/конструктора должны быть разрешены function type. В Probe `static int checked() throws IOException`, `interface Checked {int get() throws IOException;}`, `Checked c=Probe::checked;` — вызов c.get() бросил IOException; `IntSupplier r=CheckedRef::f;` для f throws IOException отклонён incompatible.thrown.types.in.mref. У объединённого SAM throws вычисляется по [JLS §9.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.9): допустимые checked типы должны удовлетворять throws всех соответствующих методов после адаптации, а не быть их произвольным объединением. В Probe Merged extends Wide (get throws Exception) и Narrow (get throws IOException) допускает IOException, вызов m.get() проверен. Отдельно receiver expression вычисляется при создании bound reference; `IntSupplier unused=factory()::read;` с factory throws IOException потребовал обработку на месте создания и фактически бросил IOException ещё до появления SAM, хотя IntSupplier сам не объявляет checked throws. См. [JLS §11.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2), [JLS §15.13.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.13.2) и [интерфейсы](interfaces.md).

## 7. Method references: четыре основных формы и target type

Method reference — выражение с `::`, которое должно подходить целевому functional interface. Выбранный целевой метод или конструктор выполняется при вызове SAM. Выражение-получатель bound reference вычисляется сразу при вычислении самой ссылки, может вызвать другие методы, иметь побочные эффекты или бросить исключение; это не вызов выбранного целевого метода.

| Форма | Пример | Откуда берётся receiver |
|---|---|---|
| **Static** | `Integer::parseInt` | Receiver instance не требуется |
| **Bound** | `someString::toUpperCase` | Указанный объект вычисляется при создании reference |
| **Unbound** | `String::length` | Первый аргумент SAM становится receiver |
| **Constructor** | `Box::new` | При вызове SAM создаётся новый объект |

```java
import java.util.function.*;

class References {
    record Box(int value) {}

    public static void main(String[] args) {
        ToIntFunction<String> staticRef = Integer::parseInt;
        Supplier<String> boundRef = "abc"::toUpperCase;
        ToIntFunction<String> unboundRef = String::length;
        IntFunction<Box> constructorRef = Box::new;

        System.out.println(staticRef.applyAsInt("67")); // 67
        System.out.println(boundRef.get());            // ABC
        System.out.println(unboundRef.applyAsInt("ab"));// 2
        System.out.println(constructorRef.apply(67).value()); // 67
    }
}
```

Это четыре **адаптации к соответствующим SAM**. `String::length` нельзя напрямую присвоить `Supplier<Integer>` — receiver-аргумента нет. Также `Function<String,String> wrong = String::length` не подходит, потому что `length()` возвращает `int`, не `String`. Это отдельные compile-time ошибки N17/N10, а не runtime.

Для конструктора `Box::new` экземпляр `Box` не создаётся в момент вычисления ссылки; он появляется при вызове `constructorRef.apply(67)`. Тип `IntFunction<Box>` ограничивает и количество аргументов, и их тип, и возвращаемый объект. Помимо этих четырёх форм у Java есть дополнительные нюансы массивов/супертипов/overloads; они не заменяют основных правил §15.13.

Дополнительные формы: `IntFunction<int[]> array = int[]::new;` создаёт массив при apply(67), длина 67 проверена. В Probe Child extends Parent переопределяет f() (68 вместо 67), но его instance-метод `IntSupplier parent(){return super::f;}` связывает вызов с текущим объектом и реализацией родителя: parent().getAsInt() вернул 67. В instance-методе Probe `Supplier<Inner> innerCtor(){return Inner::new;}` имеет подходящий лексический enclosing instance; p.innerCtor().get().owner() вернул 67. В static context без такого экземпляра InnerCtor отдельно отклонён cant.access.inner.cls.constr. Для inner-конструктора это отдельное условие, а не передача произвольного owner как первого SAM аргумента. См. [CORE-NESTED §2](nested-classes.md), [JLS §15.13.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.13.1).

### Локаль и выбор перегрузки `toUpperCase`

Здесь для ссылки на метод конкретного объекта (bound) выбрана строка `"abc"`: в проверяемых конфигурациях JVM она даёт `ABC`. Это позволяет сохранить пример с `Supplier<String>` (его метод `get()` не принимает аргументов) и воспроизводимый технический вывод. Сам метод [`String.toUpperCase()`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#toUpperCase()) от этого не становится независимым от локали: он применяет правила `Locale.getDefault()` **при вызове метода**. Например, для `"hi"` при `en_US` результат — `HI`, а при `tr_TR` — `Hİ` (последний символ U+0130, заглавная I с точкой). Это различие правил преобразования регистра, а не ошибка кодировки.

Если правила регистра не должны зависеть от текущей локали (например, при нормализации технического ключа), используйте [`toUpperCase(Locale)`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#toUpperCase(java.util.Locale)) с [`Locale.ROOT`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Locale.html#ROOT). Для текста, предназначенного человеку, выбирайте локаль согласно требованиям языка и интерфейса. `Locale.ROOT` задаёт нейтральную локаль, а не неизменность результата для любых версий Unicode/JDK.

Выбор перегрузки с параметром `Locale` влияет на число параметров единственного абстрактного метода целевого интерфейса (SAM). В ссылке `text::toUpperCase` на метод конкретного объекта (bound) получатель уже задан, поэтому `Function<Locale, String>` позволяет передать локаль единственным аргументом при вызове `apply`. Для ссылки с передаваемым получателем (unbound) `String::toUpperCase` и перегрузки с локалью нужны два аргумента — строка-получатель и локаль (`BiFunction<String, Locale, String>`). Если требуется именно `Supplier<String>` с заранее выбранной локалью, используйте лямбду: ссылка на метод сама по себе не подставляет произвольные значения аргументов.

Сохраните в отдельном файле `LocaleCaseProbe.java`:

~~~java
import java.util.Locale;
import java.util.function.*;

class LocaleCaseProbe {
    public static void main(String[] args) {
        String text = "hi";
        Supplier<String> original = text::toUpperCase;
        Supplier<String> sample = "abc"::toUpperCase;
        Supplier<String> technical = () -> text.toUpperCase(Locale.ROOT);
        Function<Locale, String> boundWithLocale = text::toUpperCase;
        BiFunction<String, Locale, String> unboundWithLocale = String::toUpperCase;

        System.out.println("locale=" + Locale.getDefault());
        System.out.println("stdout=" + System.out.charset());
        System.out.println("stdout.encoding=" + System.getProperty("stdout.encoding"));
        System.out.println("original=" + original.get());
        System.out.println("sample=" + sample.get());
        System.out.println("technical=" + technical.get());
        System.out.println("bound-root=" + boundWithLocale.apply(Locale.ROOT));
        System.out.println("unbound-root=" + unboundWithLocale.apply(text, Locale.ROOT));
    }
}
~~~

Для сопоставления запустите три отдельных процесса JVM (команды ниже для Bash; локаль системы и `Locale.setDefault` не меняем):

~~~sh
export LC_ALL=C
mkdir -p out
javac --release 21 -encoding UTF-8 -XDrawDiagnostics -d out LocaleCaseProbe.java
java -Dstdout.encoding=UTF-8 -cp out LocaleCaseProbe
java -Duser.language=en -Duser.country=US -Dstdout.encoding=UTF-8 -cp out LocaleCaseProbe
java -Duser.language=tr -Duser.country=TR -Dstdout.encoding=UTF-8 -cp out LocaleCaseProbe
~~~

**Фактическая локальная проверка роли 02:** [отчёт о трёх компиляциях и девяти запусках](https://github.com/Melchiorius/backend-interview-labs/issues/284#issuecomment-7159833708) для авторской версии `1fb987d7956df70611bf87d3908ab0138ad3b816`, Debian OpenJDK/javac 21.0.12.1. Для `LocaleCaseProbe.java`: Компиляция — код 0, stdout/stderr пусты; все три запуска — код 0, stderr пуст. Фактическая `System.out.charset()` и свойство `stdout.encoding` равны `UTF-8` во всех трёх процессах.

| Конфигурация JVM | Фактическая `Locale.getDefault()` | `original` | `sample` | `technical`, `bound-root`, `unbound-root` |
|---|---|---|---|---|
| По умолчанию (без `user.language` и `user.country`) | `en_US` | `HI` | `ABC` | `HI` |
| Явная `en_US` | `en_US` | `HI` | `ABC` | `HI` |
| Явная `tr_TR` | `tr_TR` | `Hİ` | `ABC` | `HI` |

Проверены кодовые точки и байты UTF-8: `HI` — U+0048 U+0049 (`48 49`), `Hİ` — U+0048 U+0130 (`48 C4 B0`). Отдельно скомпилирован и запущен верхний листинг `References.java` под теми же тремя конфигурациями: во всех случаях четыре строки `67`, `ABC`, `2`, `67`; неизменённый `ReceiverTiming.java` из §8 — строки `1`, `3`. Компиляции и запуски завершились с кодом 0, без stderr. Это самостоятельные учебные проверки, не запуск общего `verify.sh` и не доказательство для всех локалей, ОС или версий JDK.

Нормы: [JLS §15.13.1–3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.13). При создании ссылки на метод конкретного объекта вычисляется выражение-получатель, а выбранный метод выполняется при вызове единственного абстрактного метода целевого интерфейса. Перегрузка определяется целевым типом. Подробности момента вычисления — в следующем разделе.

## 8. Bound receiver вычисляется сразу — не путать с вызовом метода

Эта тонкость легко обнаруживается на примере null:

```java
import java.util.function.Supplier;

class NullReference {
    public static void main(String[] args) {
        String missing = null;

        boolean referenceFailed = false;
        try {
            Supplier<String> early = missing::trim;
        } catch (NullPointerException expected) {
            referenceFailed = true;
        }

        Supplier<String> late = () -> missing.trim(); // пока допустимо
        boolean lambdaFailed = false;
        try {
            late.get();
        } catch (NullPointerException expected) {
            lambdaFailed = true;
        }

        System.out.println(referenceFailed + "," + lambdaFailed); // true,true
    }
}
```

**У `missing::trim` NPE возникает при вычислении method-reference expression.** До вызова `get()` программа даже не дошла. Lambda в этом примере создаётся; разыменование `missing` происходит при вызове её `get()`. Это не компиляционная ошибка — обе формы типизируются.

Есть и вариант без null, различающий *сколько раз* вычисляется receiver:

```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

class ReceiverTiming {
    static String receiver(AtomicInteger counter) {
        counter.incrementAndGet();
        return "abc";
    }

    public static void main(String[] args) {
        AtomicInteger calls = new AtomicInteger();

        Supplier<String> bound = receiver(calls)::toUpperCase;
        bound.get();
        bound.get();
        System.out.println(calls.get()); // 1: receiver() был вычислен один раз

        Supplier<String> lambda = () -> receiver(calls).toUpperCase();
        lambda.get();
        lambda.get();
        System.out.println(calls.get()); // 3: ещё два вызова receiver()
    }
}
```

Даже если итоговая строка у двух форм одинакова, побочные эффекты и момент исключения **различаются**. Это сильная причина не объявлять каждую ссылку на метод механически эквивалентной определённой lambda. Норма: [JLS §15.13.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.13.3).

Bound reference сохраняет вычисленный receiver, а lambda может при вызове читать поле заново. В Probe у p поле field сначала содержит Box(1), затем присваивается Box(67). `IntSupplier bound=p.field::read, late=()->p.field.read(); p.field=new Box(67);` — bound.getAsInt() дал 1, late.getAsInt() дал 67. Локальная ссылка p effectively final; instance/static поля не подпадают под правило EF для local variables/parameters. Это не гарантия thread safety и не снимок состояния самого Box, только выбор объекта receiver.

## 9. Перегрузки и неоднозначность

Для формы Type::name и function type с n параметрами JLS §15.13.1 рассматривает две возможности: static метод с n аргументами или instance метод с n−1 аргументами и первым SAM параметром как receiver (с применимыми правилами overload/varargs). В Probe Over содержит static int f(Over x) и int f(); `ToIntFunction<Over> r=Over::f;` неоднозначна, отрицательный RefAmbiguous дал ref.ambiguous. Вместо неё `ToIntFunction<Over> explicitStatic=x->Over.f(x), explicitInstance=x->x.f();` даёт 1 и 67; явные типы lambda могут помочь при выборе применимой перегрузки, но не гарантируют универсального устранения ambiguity. Результат method reference адаптируется по правилам совместимости, в том числе boxing/unboxing; `Function<Over,Integer> boxed=explicitInstance::applyAsInt;` вернул boxed 67, `Consumer<Integer> add=out::add;` для `ArrayList<Integer> out` отбросил boolean List.add и добавил 67. Не всякий return-тип преобразуется в произвольный тип результата.

Две перегрузки могут принимать разные функциональные интерфейсы, каждый из которых на первый взгляд подходит к одному lambda expression. Нельзя выбрать метод, только читая математическое выражение справа от стрелки.

```java
import java.util.function.*;

class Overloads {
    static String choose(IntUnaryOperator f) { return "int"; }
    static String choose(LongUnaryOperator f) { return "long"; }

    public static void main(String[] args) {
        // choose(x -> x + 1); // N11: ambiguous invocation
        System.out.println(choose((IntUnaryOperator) (x -> x + 1))); // int

        IntUnaryOperator typed = x -> x + 1;
        System.out.println(choose(typed)); // int
    }
}
```

Явное приведение или уже типизированная переменная делают ожидаемый контракт известным. Но универсальное правило «всегда можно добавить cast» неверно: типы параметров, checked exceptions и return-совместимость должны соответствовать назначению. Подробный алгоритм overload resolution относится к [JLS §15.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2); эта статья показывает практический частный случай, не реконструируя весь алгоритм специфичности.

Сочетания generic overloads, intersection types, inference и raw types способны сделать ситуацию сложнее. Их границы обозначены в матрице ниже; за базовыми правилами параметризации и стирания обращайтесь к [CORE-GENERICS](generics.md) и [CORE-ERASURE](erasure.md); сложный вывод типов остаётся отдельным углублением, а не «эвристикой компилятора».

## 10. Identity lambda и детали JVM: что НЕ обещает JLS

Наблюдение `lambda1 == lambda2` на одной JVM **не является переносимым контрактом**. JLS [§15.27.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.4) оставляет реализациям свободу в выборе runtime-объекта, повторном использовании и его представлении. Нельзя надёжно строить business logic на уникальности каждого lambda object, его конкретном class name, synthetic field, реализации через `invokedynamic` или сравнительной скорости против anonymous class.

Из `effectively final` не следует, что callback безопасно публиковать между потоками. Для этого потребуются отдельные правила Java Memory Model и синхронизации. Аналогично сериализуемость и совместимость после перекомпиляции — отдельные вопросы. Не путайте **семантическую гарантию языка** с тем, что однажды показал `javap` на конкретном JDK.

## 11. Разбор типовых неверных ответов

| Упрощение | Что неверно | Правильная проверка |
|---|---|---|
| «Lambda — анонимный класс» | У lambda lexical `this`; anonymous объявляет класс со своим `this` | Сравните §5 и [CORE-NESTED](nested-classes.md) |
| «`@FunctionalInterface` делает любой интерфейс SAM» | Аннотация проверяет, но не исправляет две abstract-операции или `sealed` | N15 и JLS §9.8 |
| «`var` выведет тип lambda» | Для `var x = lambda` нет нужного target type | N07 и §2 |
| «В lambda можно менять захваченный счётчик» | Переназначаемый local не effectively final | N05; можно изменять отдельный объект, но это другой контракт |
| «Method reference всегда просто синтаксический сахар» | У bound receiver и lambda отличается момент вычисления receiver | §8, NPE/side effects |
| «`String::length` работает как `Supplier<Integer>`» | Unbound receiver должен прийти аргументом SAM | N17 |
| «Lambda всегда создаёт новый объект» | Identity/кэширование не гарантируются | §10 |
| «Lambda быстрее anonymous class» | Нужны корректные performance-измерения | За пределами JLS |

На собеседовании полезно назвать тип SAM, конкретный тип аргумента, момент вычисления receiver и только после этого говорить о результате. Это работает и при дополнительном вопросе «почему код не скомпилируется».

## 12. Карта покрытия NF-08–15 и P1/P2

| NF-ID | P | Проверяемое объяснение |
|---|---|---|
| NF-05, NF-06, NF-07 | P0 | Capture, мутация объекта, lexical `this` — §4–5 и CORE-NESTED |
| NF-08 | P0 | SAM, Object methods, интерфейс не sealed, аннотация — §1 |
| NF-09 | P0 | Target typing, `var` и cast — §2 |
| NF-10 | P0 | Expression/block bodies, checked throws — §3/§6 |
| NF-11 | P0 | Static method reference — §7 |
| NF-12 | P0 | Bound и unbound receiver — §7/§8 |
| NF-13 | P0 | Constructor reference — §7 |
| NF-14 | P0 | Немедленная оценка bound receiver, null — §8 |
| NF-15 | P0 | Неоднозначные overloads — §9 |
| NF-17 | P1 | Shadowing параметров lambda — §5 |
| NF-18 | P1 | Definite assignment к месту захвата — §4 |
| NF-19 | P1 | Static context не даёт instance `this` — §5 |
| NF-20 | P1 | Совместимость арности и результата reference — §7 |
| NF-21 | P1 | Lambda identity не гарантируется — §10 |
| NF-23 | P1 | Generic overloads и inference — граница §9 |
| NF-24 | P1 | Anonymous и lambda не взаимозаменяемы — §5 и CORE-NESTED |
| NF-25 | P2 | `invokedynamic`/hidden classes — граница JVM |
| NF-26 | P2 | Основы [generics](generics.md)/[erasure](erasure.md) разобраны; intersection и сложный inference — граница углубления |
| NF-27 | P2 | Serialization и стабильность бинарного имени — граница |
| NF-28 | P2 | Stream API и Collectors — самостоятельные материалы: [pipeline и функциональные операции](streams.md), [reduce и Collector](stream-advanced.md); здесь NF-28 остаётся P2-границей именно статьи о лямбдах |
| NF-29 | P2 | Java Memory Model/thread-safety — граница |
| NF-30 | P2 | Производительность/аллокации — benchmark, не JLS |

**Остальные P0 NF-01–NF-04 и NF-16**, включая static nested, inner/local/anonymous, `Outer.this` и local record, объяснены в [CORE-NESTED](nested-classes.md). В сумме план содержит **16 P0, 8 P1 и 6 P2**. P2 обозначают границы этой статьи.

## 13. Отрицательные случаи и как их воспроизводить

G1-план содержит 20 полных отдельных файлов N01–N20; к этой статье напрямую относятся N05–N08, N10–N12, N15–N17, N19–N20, а также N03/N04/N16 в совместном объяснении capture. **Каждый файл компилируется отдельно**, например:

```sh
mkdir -p out-negative
javac --release 21 -d out-negative N11_AmbiguousLambdaOverload.java
```

Ожидается ненулевой exit status по причине overload ambiguity, а не по случайной «duplicate class». У разных компиляторов формулировки диагностики отличаются, поэтому сравнивать её с одной буквальной строкой не нужно. Полные короткие исходники, включая недопустимые `return`, checked `throws`, `this` в static и несоответствия return/arity, доступны в [§6 исследовательского плана](../../docs/plans/CORE-NESTED-FUNCTIONS.md).

**Позитивное сопоставление:** [§5 исследовательского плана](../../docs/plans/CORE-NESTED-FUNCTIONS.md) содержит один исходник `NestedFunctionsProbes.java` и **17 строк stdout**; это *одна* compilation/run, не 17 отдельных тестов. На этом историческом исходнике основан текущий код: в B4a для P10 исходная строка hi и ожидаемое HI заменены на abc и ABC (см. §7, локаль); текущая версия включена в [общий запускаемый пример](../../examples/core/nested-functions/README.md), компилируемый отдельным `verify.sh` и CI-step; это проверка **позитивного сценария**, а не автоматическая компиляция двадцати отрицательных листингов.

## 14. Первоисточники, ограничения проверки и дальнейшие связи

Нормативные определения: [JLS 21 §9.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8), [§15.27.1–4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27), [§15.13.1–3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.13), [§15.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2), [§4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4), [§6.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.4), [§16](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html). API: [Runnable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runnable.html), [Supplier](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Supplier.html), [Function](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Function.html).

Для практического API-дизайна см. [интерфейсы](interfaces.md), [композицию/делегирование](composition.md), [передачу аргументов](parameters.md), [объекты и identity](object.md), [равенство](equality.md). Применение `Predicate` и `Function` в `filter`/`map`, а также запрет на прямой checked `IOException` в `Function.apply` разобраны в [Stream API](streams.md); накопление результатов — в [reduce и Collector](stream-advanced.md). Банки интервью-вопросов в [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) служат источником возможных уточняющих вопросов, а не доказательством правил Java.

**Границы проверки:** автоматическое извлечение и компиляция всех Java-фрагментов непосредственно из Markdown не заявляются; авторские проверки отдельных примеров и отрицательных сценариев зафиксированы в PR. Общий запускаемый пример проверяется отдельным скриптом и CI, но обычный Gradle workflow не компилирует Java-код из Markdown. Визуальный просмотр в Obsidian/IntelliJ, сравнительное поведение Java 8/16, альтернативные JVM, JIT/bytecode, сериализация, интеграция лямбд со Stream API внутри этой статьи, concurrency и performance не считаются проверенными без отдельного отчёта. Отдельный [Stream runnable](../../examples/core/streams/README.md) не означает автоматической компиляции примеров из этого Markdown.

**Применение функциональных интерфейсов к порядку:** [CORE-UTIL](utilities.md) объясняет `Comparator.comparing`/`thenComparingInt`, ленивый `Supplier<String>` для `Objects.requireNonNull` и отличие null-ключа от null-элемента.

**Дополнительное фактическое исполнение 02.** [Полное приложение](https://github.com/Melchiorius/backend-interview-labs/issues/354#issuecomment-7224888344): OpenJDK/javac 21.0.12.1 Debian, одна компиляция Probe с `--release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror` exit 0, обе streams пусты. Три JVM с явными file.encoding/stdout.encoding UTF-8, default en_US и explicit en_US/tr_TR: exit 0, stderr пуст, фактический stdout charset UTF-8, по 15 строк/237 байт. Четырнадцать изолированных отрицательных файлов с -XDrawDiagnostics дали exit 1 по ожидаемым причинам, LocalStatic две parser diagnostics, остальные по одной. Исторические 20 G1 negatives, прежние B4a локальные проверки, новые 14 и whole-suite CI — разные доказательства.

Отдельно полный неизменённый runner на трёх восстановленных входах с проверенными Git blob SHA/mode, не полном клоне. Для трёх конфигураций default/en_US/tr_TR временный PATH launcher передал JVM flags file.encoding/stdout.encoding UTF-8 и при необходимости user.language/user.country; временный javaagent до main записал фактические locale/default charset/file.encoding/stdout charset в отдельный файл, без изменения stdout. Все три runner exit 0, stderr пуст, actual.stdout 17 строк/149 байт, SHA-256 cb62ae550f243ae46665102bed14fc8b9807c04e627f6d5b404f56b73ca2c0c0, внешний stdout 18 строк/194 байта с marker, SHA-256 4e98c26a91a6a09c8ffd475e7b54a914edff17d952b1d29199307d4c68de2f1c; cmp с fixture и между конфигурациями успешен. Во всех трёх P10=67,ABC,2,67; P11=true,true; P12=1,3. Это три instrumented JVM-конфигурации, не native OS en_US/tr_TR: доступны только C/C.utf8/POSIX. Полные исходники launcher/agent, команды, хеши и наблюдения в приложении. Не заявляется полный clone, все Markdown snippets, альтернативные JDK/OS, serialization, GUI, concurrency или benchmark.

**Независимое фактическое исполнение 03.** [Boundary.java, четыре отрицательных файла, команды и хеши](https://github.com/Melchiorius/backend-interview-labs/pull/355#issuecomment-7225079256): отдельная строгая компиляция `javac --release 21 -encoding UTF-8 -Xlint:all -Werror -XDrawDiagnostics Boundary.java` exit 0, stdout/stderr пусты. Три JVM с явным UTF-8 и временным LocaleAgent проверили фактические locale/default charset/file.encoding/stdout charset: default/en_US — en_US, tr_TR — tr_TR; exit 0, stderr пуст, stdout 4 строки/142, 142 и 143 байта соответственно. В Boundary проверены разные owner (67/68), blank local с двумя допустимыми ветвями присваивания (выполнена только ветвь без аргументов, обе ветви фактически проверены у 02), generic SAM method reference с int (boxing) и String, bound receiver длина 3 против lambda field reread 1, счётчик receiver 1 после двух bound вызовов и 3 после двух deferred, отдельно checked exception при вычислении receiver и при вызове target. Для локали наблюдалось hi → HI/Hİ, Locale.ROOT → HI и abc → ABC во всех трёх конфигурациях; проверены bound/unbound формы с явной Locale. Четыре изолированных отрицательных компиляции (BadGeneric, BadCapture, BadEval, BadAmbiguous) дали exit 1 по ожидаемым причинам и пустой stdout.

Отдельно роль 03 повторила текущий неизменённый runner на тех же трёх точных восстановленных входах с временными PATH launcher и LocaleAgent (не полный клон). Все три instrumented JVM-конфигурации default/en_US/tr_TR — exit 0, stderr пуст; девять cmp дали exit 0 (для каждой конфигурации actual.stdout с fixture, внешний stdout с fixture + marker, перечень путей/Git blob SHA/mode до и после). Actual.stdout 17 строк/149 байт, SHA-256 cb62ae550f243ae46665102bed14fc8b9807c04e627f6d5b404f56b73ca2c0c0; внешний stdout 18 строк/194 байта, SHA-256 4e98c26a91a6a09c8ffd475e7b54a914edff17d952b1d29199307d4c68de2f1c — те же байты, что у 02, включая P10–P12. Runner выполняет одну компиляцию и один JVM-запуск, не 17 тестов; строгие флаги компиляции временных проб не являются флагами runner. Не проверялись native OS en_US/tr_TR (они отсутствуют), uninstrumented default в этих локальных опытах или повтор старого B4a runner с hi/HI. Исторические 20 G1 negatives, 14 новых проверок 02, четыре независимые проверки 03 и 24 EXPECTED REJECTION всего CI на принятом G2 HEAD `c6ef11ffe9609da6c5b7dd6654fe5670e319e937` — разные наборы; временные пробы и агенты в CI не входят. Нового локального исполнения при этой редакционной правке не проводилось.
