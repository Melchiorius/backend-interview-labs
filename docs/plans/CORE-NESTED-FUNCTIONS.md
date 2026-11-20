# CORE-NESTED + CORE-FUNCTIONS — исследовательский план Java SE 21

**Статус:** G1, роль 01; **не принят координатором 00**. **Основание:** [Issue #72](https://github.com/Melchiorius/backend-interview-labs/issues/72), родитель #33. **Исходный main:** `9fa271902ff6a3b9fb487f85e39d8682e8304113`, 20.11.2026. **Версия:** JLS / API Java SE 21. **Локальные проверки:** OpenJDK 21.0.12.1, `javac 21.0.12.1 --release 21`. HEAD и CI фиксировать по GitHub после коммита. Это **план ДО статьи**, не обновление `COVERAGE` и не публикация учебных материалов.

## 1. Вопрос читателя и границы двух материалов

Почему `static nested`, `inner member`, `local` и `anonymous` — разные сущности, какую внешнюю переменную/экземпляр они могут использовать, чем `this` анонимного класса отличается от `this` в лямбде и как functional target type определяет допустимость lambda / method reference?

**Предлагаю две самостоятельные статьи, объединённые одной исследовательской картой:**

- **CORE-NESTED:** четыре вида вложенных классов, enclosing instance, `Outer.this`, local/anonymous, capture/effectively-final, scope и обновлённая с Java 16 допустимость static members в inner/local/anonymous. Дать только мост к lambda `this`/capture, **не** повторять все SAM и method references.
- **CORE-FUNCTIONS:** functional interface/`@FunctionalInterface`, target typing, lambda expressions/blocks, lexical `this`, effectively-final capture, static/bound/unbound/constructor method references, receiver evaluation, checked exceptions и overload ambiguity. Не переписывать классификацию вложенных классов.
- **Общий проверяемый мост:** `NestedFunctionsProbes.java` в §5 — **одна самостоятельная Java 21 программа с 17 утверждениями об stdout**, а не 17 независимых компиляций. Негативные примеры — 20 отдельных файлов. После G1 роль 03 согласует с 02, достаточно ли inline-примеров или нужен **один** автономный multi-file example; никакого обязательного упражнения/Mockito ради формата.

**Терминологическая точность:** `static nested class` **не** является `inner class` (§8.1.3), несмотря на распространённый интервью-термин «static inner». Inner class имеет право объявлять static поля, методы и static initializers в Java 21; старое ограничение существовало до Java SE 16. Однако local class нельзя объявить модификатором `static` — это другой запрет. Lambda не вводит новое значение `this` и не гарантирует уникальный runtime object при каждом вычислении.

**Вне подробного scope:** generics/erasure, Stream API, threading/JMM, сериализация лямбд, framework DI, JVM classfile/invokedynamic/hidden-class internals, performance/JIT. Scope G1 CORE-INTERFACES включает SAM только обзорно; этот план отвечает за лямбды и method references. Предыдущие G2 #70/#71 и G6 #69 продолжаются параллельно.

## 2. Нормативные источники категории A (только Java 21)

Официальные `OFF-JLS21` и `OFF-API21` из [реестра источников](../SOURCE_REGISTRY.md). Интервью-сайты **не** заменяют эти гарантии.

- [JLS §8.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.3): nested/inner/enclosing instance; возможность **static members и static initializers внутри inner class** в Java 21, примечание об изменении с Java 16; local/member record/enum и member interface могут быть implicitly static.
- [§8.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.5), [§15.9.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9.2): member classes и enclosing instance при создании non-static inner.
- [§14.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.3): local class declaration и его модификаторы, local record/enum implicitly static.
- [§15.9.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9.5): anonymous class — отдельная декларация и объект, собственный `this`, отсутствие имени конструктора.
- [§4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4), [§6.5.6.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.5.6.1), [§16](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html): captured local/parameter `final`/`effectively final` и definite assignment. Переназначение локальной переменной ≠ мутация объекта по неизменной локальной ссылке.
- [§9.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8): functional interface, Object methods и функциональный abstract-method contract. `@FunctionalInterface` — проверка, а не условие функциональности.
- [§15.27.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.1), [§15.27.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.2): lambda parameters, expression/block body, lexical `this`/`super` и capture.
- [§15.27.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.3), [§15.27.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.4): poly expression, target typing, lambda runtime evaluation. **Не гарантируются** выделение нового объекта при каждом вычислении, стабильная identity/class name или скорость.
- [§15.13.1–3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.13): method references: `Type::staticMethod`, `expression::instanceMethod`, `Type::instanceMethod`, `Type::new`, overload и receiver evaluation. Bound reference вычисляет receiver **при создании** ссылки и бросает NPE для null немедленно; лямбда с `()->receiver.method()` может вычислять receiver позднее.
- [§15.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2), [§5.2–3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2): overload resolution и functional target contexts. `var f = x -> x+1` без функционального target не компилируется.
- [API Runnable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runnable.html), [Supplier](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Supplier.html), [Function](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Function.html), [IntUnaryOperator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/IntUnaryOperator.html): реальные SAM и `throws`. `Runnable.run()` не допускает неотловленного checked `IOException`.

**Не переносить утверждение об алгоритме JVM на язык:** JLS описывает поведение; конкретная генерация `invokedynamic`/synthetic captures/JIT зависит от реализации. Захват значения и необходимость enclosing instance — правила языка; точное наличие synthetic field в каждом classfile — не универсальное утверждение.

## 3. Независимые подборки вопросов C

По [SOURCE_REGISTRY](../SOURCE_REGISTRY.md) проверены существующие реестровые записи и фактические списки вопросов:

- **Q-IN28:** [JavaInterviewQuestionsAndAnswers](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/readme.md), Q60–63 о nested/«static inner»/local/anonymous; Q213–216 о method references/lambdas и функциональных интерфейсах. Термин «static inner» не принимать за формальное определение JLS.
- **Q-TEAMLEAD:** [java-interview-questions](https://github.com/teamlead/java-interview-questions), local/anonymous/nested классы, lambda, scope/захват; повторяющиеся вопросы в банке не доказывают частотность.
- **Q-HRUDAY:** [Core Java Interview Question Bank](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md), Chapter 16 (84 вопроса), lambda vs anonymous, `this`, effectively-final, checked exceptions, method references, производительность/serialization как границы.
- **Q-CHERNYSHOV:** [русский перечень](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/ru/README.md) просмотрен, но отдельные вопросы про capture и виды method references в открытом оглавлении **не подтверждены**. Не считать его четвёртым независимым доказательством частной полноты.

Списки вопросов — лишь обнаружение пробелов, **не** источник Java 21 гарантий. Не проверены методика составления, статистика частот, все ответы и независимость авторов. После статьи 02 нужен ретроспективный аудит ещё по Q-BAELDUNG-CORE.

## 4. Матрица ожидаемых интервью-ответов P0/P1/P2

P0 — обязательный механизм и ответ; P1 — ограничения; P2 — специальные границы. Это **не** соответствие Middle/Senior. `P01–P17` — наблюдения §5, `N01–N20` — изолированные негативные компиляции §6.

| ID | Приоритет и проверяемый вопрос → ожидаемый ответ | Норма | Сценарий; статья |
|---|---|---|---|
| NF-01 | **P0**. Static nested vs non-static inner: только для второго нужен enclosing instance | §8.1.3, §15.9.2 | P01/P02, N01/N02; CORE-NESTED |
| NF-02 | **P0**. `Outer.this` в правильном inner context обращается к enclosing instance, не работает из обычного static nested | §8.1.3, §15.8.4 | P02, N09/N14; CORE-NESTED |
| NF-03 | **P0**. Local/anonymous class — отличие от member declaration; anonymous создаёт собственный объект/`this` | §14.3, §15.9.5 | P04/P05/P07; CORE-NESTED |
| NF-04 | **P0**. С Java 16 static members в inner/local/anonymous разрешены; `static class Local` запрещён | §8.1.3, §14.3 | P03–P05, N18; CORE-NESTED |
| NF-05 | **P0**. Captured local/parameter должен быть effectively final и definitely assigned | §4.12.4, §6.5.6.1, §16 | P06/P16, N03–N05/N16; обе |
| NF-06 | **P0**. Capture ссылки не равен immutability объекта; можно изменить элемент массива | §4.12.4, §15.27.2 | P08, N05; обе |
| NF-07 | **P0**. Anonymous `this` обозначает anonymous object, lambda `this` — внешний лексический `this`, в static context недоступен | §15.9.5, §15.27.2 | P07, N06; обе |
| NF-08 | **P0**. SAM/`@FunctionalInterface`; Object.equals не новая abstract operation | §9.8 | P17, N15; CORE-FUNCTIONS |
| NF-09 | **P0**. Lambda требует functional target type, просто `var` его не задаёт | §15.27.3, §5.2 | P09, N07; CORE-FUNCTIONS |
| NF-10 | **P0**. Expression/block lambda и checked exception должны быть совместимы с SAM; Runnable не бросает checked IOException | §15.27.2–3 | P14, N08/N12; CORE-FUNCTIONS |
| NF-11 | **P0**. Static method reference разрешается по target SAM, не вычисляется сразу | §15.13.1–3 | P10 static; CORE-FUNCTIONS |
| NF-12 | **P0**. Bound object::method vs unbound Type::method: во втором случае receiver — аргумент SAM | §15.13 | P10/P11, N17; CORE-FUNCTIONS |
| NF-13 | **P0**. Type::new — constructor reference, создание объекта при вызове SAM | §15.13 | P10 constructor; CORE-FUNCTIONS |
| NF-14 | **P0**. Bound receiver вычисляется сразу; null вызывает NPE при reference creation, lambda разыменовывает позже | §15.13.3, §15.27.4 | P11/P12; CORE-FUNCTIONS |
| NF-15 | **P0**. Lambda в overloaded call может быть ambiguous, cast или типизированный контекст решает выбор | §15.12.2 | P13, N11; CORE-FUNCTIONS |
| NF-16 | **P0**. Local record implicitly static и не захватывает локальную переменную метода | §14.3, §8.1.3 | P15, N13; CORE-NESTED |
| NF-17 | **P1**. Scope и shadowing параметров lambda отличаются от anonymous class | §6.4, §15.27.1 | N19; CORE-FUNCTIONS |
| NF-18 | **P1**. Definite assignment нужно до lambda expression, даже если вызов позже | §16 | N16; обе |
| NF-19 | **P1**. Lambda в static context не даёт доступ к instance field без receiver | §15.27.2 | N06/N20; CORE-FUNCTIONS |
| NF-20 | **P1**. Method reference return/arity должны быть совместимы с target | §15.13.1–2 | N10/N17; CORE-FUNCTIONS |
| NF-21 | **P1**. Lambda identity и выделение объектов не гарантируются языком | §15.27.4 | Не тестировать `lambda1==lambda2`; граница |
| NF-22 | **P1**. Enclosing/captured state и удержание объектов: проектный риск, не гарантия конкретного synthetic field | §8.1.3, §15.27.4 | P02/P07; CORE-NESTED |
| NF-23 | **P1**. Разные generic targets и пересекающиеся overloads требуют отдельной inference | §9.8, §15.12/13 | N11, CORE-GENERICS |
| NF-24 | **P1**. Anonymous class нельзя механически заменить lambda при необходимости собственного this или class inheritance | §15.9.5, §15.27 | P07; обе |
| NF-25 | **P2**. JVM invokedynamic/hidden classes — не языковая гарантия | JLS §15.27.4, JVMS | JVM-BYTECODE |
| NF-26 | **P2**. Generics, erasure, intersection types и bridge methods | §9.8, §15.27 | CORE-GENERICS/ERASURE |
| NF-27 | **P2**. Lambda serialization и стабильность бинарных имён | §15.27.4 | CORE-SERIAL |
| NF-28 | **P2**. Stream API/Collectors — отдельные вычислительные гарантии | Stream API 21 | CORE-STREAMS |
| NF-29 | **P2**. Effectively-final capture не доказывает thread safety объекта | JLS §17 | CON-JMM/CON-RACE |
| NF-30 | **P2**. Lambda vs anonymous speed/allocations требуют benchmark | JLS §15.27.4, JMH | JVM-JIT |

## 5. Positive: один полный исходник, 17 точных stdout observations

Сохранить в `NestedFunctionsProbes.java`. На OpenJDK 21.0.12.1 автором реально выполнено `javac --release 21 -d out NestedFunctionsProbes.java` и `java -cp out NestedFunctionsProbes`, stdout побайтно совпал с приведённым ниже (`diff -u` без различий). Это **один** positive compilation/run, а не 17 отдельных компиляций. Файл не включён в исходники Java репозитория.

```java
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

public class NestedFunctionsProbes {
    private int value = 67;
    static class Nested { int get() { return 68; } }
    class Inner {
        static int count = 69;
        static { count++; }
        int getOuter() { return NestedFunctionsProbes.this.value; }
    }
    record Widget(int id) { }
    @FunctionalInterface interface Transform { int apply(int x); boolean equals(Object obj); }
    @FunctionalInterface interface Checked { void run() throws IOException; }
    static String choose(IntUnaryOperator op) { return "int"; }
    static String choose(LongUnaryOperator op) { return "long"; }
    static String receiver(AtomicInteger calls) { calls.incrementAndGet(); return "abc"; }

    void check() {
        System.out.println("P01=" + new Nested().get());
        Inner inner = this.new Inner();
        System.out.println("P02=" + inner.getOuter());
        System.out.println("P03=" + Inner.count);
        int captured = 67;
        class Local {
            static int marker = 70;
            static { marker++; }
            int read() { return captured + marker; }
        }
        System.out.println("P04=" + Local.marker);
        Runnable anonymousStatic = new Runnable() {
            static int marker = 72;
            static { marker++; }
            @Override public void run() { System.out.println("P05=" + marker); }
        };
        anonymousStatic.run();
        System.out.println("P06=" + new Local().read());
        Supplier<Boolean> anonymousThis = new Supplier<>() {
            @Override public Boolean get() { return (Object)this == NestedFunctionsProbes.this; }
        };
        Supplier<Boolean> lambdaThis = () -> this == NestedFunctionsProbes.this;
        System.out.println("P07=" + anonymousThis.get() + "," + lambdaThis.get());
        int[] box = {67};
        Supplier<Integer> capturedRef = () -> box[0];
        box[0] = 68;
        System.out.println("P08=" + capturedRef.get());
        IntUnaryOperator add = x -> x + 1;
        System.out.println("P09=" + add.applyAsInt(67));
        IntFunction<String> staticRef = String::valueOf;
        Supplier<String> boundRef = "hi"::toUpperCase;
        ToIntFunction<String> unboundRef = String::length;
        IntFunction<Widget> ctorRef = Widget::new;
        System.out.println("P10=" + staticRef.apply(67) + "," + boundRef.get() + "," + unboundRef.applyAsInt("ab") + "," + ctorRef.apply(67).id());
        String missing = null;
        boolean immediate = false, delayed = false;
        try { Supplier<String> badRef = missing::trim; }
        catch (NullPointerException e) { immediate = true; }
        Supplier<String> later = () -> missing.trim();
        try { later.get(); } catch (NullPointerException e) { delayed = true; }
        System.out.println("P11=" + immediate + "," + delayed);
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> eager = receiver(calls)::toUpperCase;
        eager.get(); eager.get(); int afterEager = calls.get();
        Supplier<String> deferred = () -> receiver(calls).toUpperCase();
        deferred.get(); deferred.get();
        System.out.println("P12=" + afterEager + "," + calls.get());
        System.out.println("P13=" + choose((IntUnaryOperator)(x -> x + 1)));
        Checked checked = () -> { throw new IOException("io"); };
        String result;
        try { checked.run(); result = "missed"; }
        catch (IOException e) { result = e.getMessage(); }
        System.out.println("P14=" + result);
        record LocalRecord(int n) { }
        System.out.println("P15=" + new LocalRecord(67).n());
        Supplier<Integer> anonymousCapture = new Supplier<>() {
            @Override public Integer get() { return captured; }
        };
        Supplier<Integer> lambdaCapture = () -> captured;
        System.out.println("P16=" + anonymousCapture.get() + "," + lambdaCapture.get());
        Transform transform = x -> x + 1;
        System.out.println("P17=" + transform.apply(67));
    }
    public static void main(String[] args) { new NestedFunctionsProbes().check(); }
}
```

```text
P01=68
P02=67
P03=70
P04=71
P05=73
P06=138
P07=false,true
P08=68
P09=68
P10=67,HI,2,67
P11=true,true
P12=1,3
P13=int
P14=io
P15=67
P16=67,67
P17=68
```

**Команды повторения:**

```sh
mkdir -p out
javac --release 21 -d out NestedFunctionsProbes.java
java -cp out NestedFunctionsProbes > actual.stdout
# Сохранить 17 строк выше в expected.stdout
diff -u expected.stdout actual.stdout
```

Не проверять `this==null` или `lambda1==lambda2` вместо указанных тестов — такие примеры не объясняют нормативный момент захвата/типизации. P11/P12 особенно важны: у bound method reference receiver вычисляется **при создании reference**, у приведённой lambda он вычисляется при каждом `get()`.
