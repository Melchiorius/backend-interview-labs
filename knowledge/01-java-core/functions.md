# Функциональные интерфейсы, лямбды и ссылки на методы в Java 21

Лямбда в Java — не «сокращённый анонимный класс» и не самостоятельный объект-функция с фиксированным типом. Это выражение, смысл которого определяется **целевым функциональным интерфейсом**. При компиляции важно знать его единственную абстрактную операцию, типы параметров и результата, допустимые checked exceptions и окружение, в котором записано выражение.

Такая модель объясняет сразу несколько собеседовательных случаев: почему `var f = x -> x + 1` не компилируется, почему `String::length` требует аргумент-строку, почему `missing::trim` бросает `NullPointerException` раньше `() -> missing.trim()` и почему `this` в lambda не равен `this` в anonymous class.

Перед чтением полезны [интерфейсные методы и SAM](interfaces.md), [типы и значения](types.md), [перегрузки и наследование](inheritance.md), [вложенные и анонимные классы](nested-classes.md) и [контракты Object](object.md). Полная матрица проверок NF-01–NF-30 приведена в [принятом исследовательском плане](../../docs/plans/CORE-NESTED-FUNCTIONS.md). **Общий Java 21 runnable:** [инструкция и разбор](../../examples/core/nested-functions/README.md), [полный исходник](../../examples/core/nested-functions/src/NestedFunctionsProbes.java), [17 строк эталона](../../examples/core/nested-functions/expected.stdout). P09–P14/P17 из него связывают target typing, четыре ссылки на методы, bound receiver, overload и checked exception. 17 строк — наблюдения **одного** запуска.

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

Метод invocation разрешён как statement expression, его результат может быть проигнорирован. Для этого не нужно менять `Runnable` на `Supplier<Integer>`. Точные правила: [JLS §15.27.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.2).

## 4. Захват переменных: effectively final и definite assignment

Lambda может читать local variable или parameter из охватывающей области только при соблюдении требований `final/effectively final` и **definite assignment**. Присваивание переменной нового значения нарушает effectively final; изменение объекта, на который она ссылается, — нет.

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

### Область имён: lambda не создаёт новую область в смысле затенения параметра

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

## 7. Method references: четыре основных формы и target type

Method reference — выражение с `::`, которое должно подходить целевому functional interface. Оно **не вызывает метод при самом объявлении**, если только вычисление *receiver expression* не вызывает побочный эффект. Для обычной функции выполнение метода происходит при вызове SAM.

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
        Supplier<String> boundRef = "hi"::toUpperCase;
        ToIntFunction<String> unboundRef = String::length;
        IntFunction<Box> constructorRef = Box::new;

        System.out.println(staticRef.applyAsInt("67")); // 67
        System.out.println(boundRef.get());            // HI
        System.out.println(unboundRef.applyAsInt("ab"));// 2
        System.out.println(constructorRef.apply(67).value()); // 67
    }
}
```

Это четыре **адаптации к соответствующим SAM**. `String::length` нельзя напрямую присвоить `Supplier<Integer>` — receiver-аргумента нет. Также `Function<String,String> wrong = String::length` не подходит, потому что `length()` возвращает `int`, не `String`. Это отдельные compile-time ошибки N17/N10, а не runtime.

Для конструктора `Box::new` экземпляр `Box` не создаётся в момент вычисления ссылки; он появляется при вызове `constructorRef.apply(67)`. Тип `IntFunction<Box>` ограничивает и количество аргументов, и их тип, и возвращаемый объект. Помимо этих четырёх форм у Java есть дополнительные нюансы массивов/супертипов/overloads; они не заменяют основных правил §15.13.

Нормы: [JLS §15.13.1–3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.13).

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

## 9. Перегрузки и неоднозначность

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
| NF-08 | P0 | SAM, Object methods, non-sealed, аннотация — §1 |
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
| NF-28 | P2 | Stream API/Collectors — отдельная тема |
| NF-29 | P2 | Java Memory Model/thread-safety — граница |
| NF-30 | P2 | Производительность/аллокации — benchmark, не JLS |

**Остальные P0 NF-01–NF-04 и NF-16**, включая static nested, inner/local/anonymous, `Outer.this` и local record, объяснены в [CORE-NESTED](nested-classes.md). В сумме план содержит **16 P0, 8 P1 и 6 P2**. P2 перечислены не ради притворного освоения JVM/Streams, а чтобы явно отделить границы.

## 13. Отрицательные случаи и как их воспроизводить

G1-план содержит 20 полных отдельных файлов N01–N20; к этой статье напрямую относятся N05–N08, N10–N12, N15–N17, N19–N20, а также N03/N04/N16 в совместном объяснении capture. **Каждый файл компилируется отдельно**, например:

```sh
mkdir -p out-negative
javac --release 21 -d out-negative N11_AmbiguousLambdaOverload.java
```

Ожидается ненулевой exit status по причине overload ambiguity, а не по случайной «duplicate class». У разных компиляторов формулировки диагностики отличаются, поэтому сравнивать её с одной буквальной строкой не нужно. Полные короткие исходники, включая недопустимые `return`, checked `throws`, `this` в static и несоответствия return/arity, доступны в [§6 исследовательского плана](../../docs/plans/CORE-NESTED-FUNCTIONS.md).

**Позитивное сопоставление:** [§5 исследовательского плана](../../docs/plans/CORE-NESTED-FUNCTIONS.md) содержит один исходник `NestedFunctionsProbes.java` и **17 строк stdout**; это *одна* compilation/run, не 17 отдельных тестов. Этот исходник включён в [общий запускаемый пример](../../examples/core/nested-functions/README.md), компилируемый отдельным `verify.sh` и CI-step; это проверка **позитивного сценария**, а не автоматическая компиляция двадцати отрицательных листингов.

## 14. Первоисточники, ограничения проверки и дальнейшие связи

Нормативные определения: [JLS 21 §9.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8), [§15.27.1–4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27), [§15.13.1–3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.13), [§15.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2), [§4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4), [§6.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.4), [§16](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html). API: [Runnable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runnable.html), [Supplier](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Supplier.html), [Function](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Function.html).

Для практического API-дизайна см. [интерфейсы](interfaces.md), [композицию/делегирование](composition.md), [передачу аргументов](parameters.md), [объекты и identity](object.md), [равенство](equality.md). Банки интервью-вопросов в [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) служат источником возможных уточняющих вопросов, а не доказательством правил Java.

**Границы проверки:** автоматическое извлечение и компиляция всех Java-фрагментов непосредственно из Markdown не заявляются; авторские проверки отдельных примеров и отрицательных сценариев зафиксированы в PR. Общий запускаемый пример проверяется отдельным скриптом и CI, но обычный Gradle workflow не компилирует Java-код из Markdown. Визуальный просмотр в Obsidian/IntelliJ, сравнительное поведение Java 8/16, альтернативные JVM, JIT/bytecode, сериализация, Stream API, concurrency и performance не считаются проверенными без отдельного отчёта.
