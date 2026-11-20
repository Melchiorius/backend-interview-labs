# CORE-INTERFACES — интерфейсы Java 21, контракты и выбор методов

**Java SE 21 · G2 / Issue [#70](https://github.com/Melchiorius/backend-interview-labs/issues/70).** Основа: принятый [исследовательский план](../../docs/plans/CORE-INTERFACES-COMPOSITION.md) (IC-01–IC-30); решение [G1 ACCEPTED 00](https://github.com/Melchiorius/backend-interview-labs/issues/64#issuecomment-7049387322). Статья о **правилах языка**; о замене наследования делегированием и стоимости такого выбора — самостоятельная [CORE-COMPOSITION](composition.md).

## Что обещает интерфейс

Представим сервис, который получает число 67 и применяет к нему одну из политик расчёта. Код сервиса должен знать, **какую операцию вправе потребовать**, а не **какой конкретный класс её исполняет**. Интерфейс задаёт ссылочный тип и набор условий, которым обязуются соответствовать реализации. Интерфейс нельзя создать через `new Policy()`; создаются экземпляры классов-реализаторов.

Java 21 interface — не «класс без реализации»: интерфейс может объявлять abstract, default, static и private методы, но не может хранить instance fields или иметь конструкторы экземпляров. Правила: [JLS 21 §9.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.1), [§9.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.3), [§9.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4).

## Центральный проверяемый пример: кто исполняет метод

Ниже отдельный **диагностический листинг** для правил интерфейсов. Роль 03 в [Issue #71](https://github.com/Melchiorius/backend-interview-labs/issues/71) разрабатывает **один общий многофайловый пример композиции и интерфейса**; не выдавайте этот независимый листинг за файлы её проекта.

Сохраните в `InterfaceMechanics.java`:

```java
import java.io.IOException;

public class InterfaceMechanics {
    interface Policy {
        int apply(int value);

        default String label() {
            return helper() + "/" + apply(67);
        }

        private String helper() {
            return "policy";
        }

        static String kind() {
            return "interface";
        }
    }

    static class Standard implements Policy {
        @Override public int apply(int n) { return n + 1; }
    }

    interface Left { default String name() { return "left"; } }
    interface Right { default String name() { return "right"; } }
    static class Both implements Left, Right {
        @Override public String name() {
            return Left.super.name() + "+" + Right.super.name();
        }
    }

    interface Welcome { default String hello() { return "interface"; } }
    static class Parent { public String hello() { return "class"; } }
    static class Descendant extends Parent implements Welcome { }

    interface A { default String value() { return "parent"; } }
    interface B extends A {
        @Override default String value() { return "child"; }
    }
    static class Specific implements A, B { }

    interface AbstractStatus { String status(); }
    interface DefaultStatus {
        default String status() { return "default"; }
    }
    static class Resolved implements AbstractStatus, DefaultStatus {
        @Override public String status() {
            return DefaultStatus.super.status();
        }
    }

    @FunctionalInterface
    interface Operation {
        int calculate(int n);
        boolean equals(Object other);
        default int twice(int n) { return 2 * calculate(n); }
        static int identity(int n) { return n; }
    }

    interface NumberSource { Number get() throws Exception; }
    static class IntegerSource implements NumberSource {
        @Override public Integer get() throws IOException { return 67; }
    }

    public static void main(String[] args) throws Exception {
        Policy policy = new Standard();
        System.out.println(policy.apply(67));
        System.out.println(policy.label());
        System.out.println(Policy.kind());
        System.out.println(new Both().name());
        System.out.println(new Descendant().hello());
        System.out.println(new Specific().value());
        System.out.println(new Resolved().status());
        Operation operation = n -> n + 1;
        System.out.println(operation.twice(67));
        System.out.println(new IntegerSource().get());
    }
}
```

Запуск из папки файла с JDK 21:

```sh
javac --release 21 InterfaceMechanics.java
java InterfaceMechanics
```

Ожидаемый полный stdout:

```text
68
policy/68
interface
left+right
class
child
default
136
67
```

**Порядок девяти строк:**

1. `68` — компилятор допускает `policy.apply(int)` по статическому типу `Policy`, а во время выполнения по фактическому классу объекта `Standard` выбирается его реализация. Компиляция и runtime dispatch — разные этапы ([JLS 21 §15.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12)).
2. `policy/68` — унаследованный экземплярный default `label()` вызывает private helper и виртуальную операцию `apply(67)`. Default имеет тело, но это не означает статическое связывание всех вызовов внутри него.
3. `interface` — метод `kind()` статический, поэтому вызывается через `Policy.kind()`, а не через `Standard.kind()` и не через переменную.
4. `left+right` — при двух независимых default класс `Both` **явно разрешает конфликт** и вызывает две допустимые реализации через `InterfaceName.super`.
5. `class` — конкретный метод, унаследованный от класса `Parent`, имеет приоритет перед default из `Welcome`.
6. `child` — подинтерфейс `B extends A` более специфичен, чем `A`; переопределив default, он выбирает своё поведение. Порядок перечисления интерфейсов не является алгоритмом «первый побеждает».
7. `default` — independent abstract/default конфликт разрешён объявлением собственного метода `Resolved.status()`; внутри него автор явно вызывает default.
8. `136` — functional interface имеет один логический abstract method `calculate`. Объявление `equals(Object)`, соответствующее публичному методу Object, и default/static members не создают вторую SAM-операцию. Лямбда даёт 68, default удваивает результат.
9. `67` — реализация `NumberSource.get()` сужает return type с `Number` до `Integer` и checked throws с `Exception` до `IOException`. Это допустимый случай, а не разрешение менять сигнатуры как угодно.

Общая механика dispatch/override — [CORE-INHERITANCE](inheritance.md); в этой статье рассматривается именно **дополнительный конфликт нескольких интерфейсных контрактов**.

## Как расширяются типы: `extends` и `implements`

`interface Extended extends First, Second` может наследовать несколько интерфейсов, если их методы совместимы. `class Worker extends Base implements First, Second` наследует **один** непосредственный класс и реализует несколько интерфейсов. Класс не может перечислить два прямых superclass. Интерфейсное множественное наследование **не** копирует множество полей состояния разных классов. См. [JLS 21 §9.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.1.3) и [§8.1.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.5).

Одна реализация удовлетворяет двум совместимым abstract требованиям:

```java
interface First { String name(); }
interface Second { String name(); }

class Employee implements First, Second {
    @Override public String name() { return "employee"; }
}
```

Но наличие общего имени **не означает** совместимость произвольных возвращаемых типов, уровня доступа и checked exceptions.

## Поля интерфейса — не поля каждого объекта

Любое поле интерфейса неявно `public static final` и обязано иметь инициализатор. Например:

```java
interface Settings {
    int LIMIT = 67;           // public static final int
    int[] VALUES = {67};      // public static final int[] reference
}
```

`Settings.LIMIT = 68` — ошибка компиляции: поле final. `Settings.VALUES[0] = 68` — **допустимое присваивание элементу массива** внутри метода: final запрещает переназначить ссылку, но не делает содержимое массива неизменяемым. Публичный изменяемый массив — потенциально плохой контракт API, даже когда формально соответствует JLS. В интерфейсе нет отдельных для каждого объекта полей, `private int state = 67` тоже нельзя ([JLS 21 §9.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.3)).

Compile-time constant `int LIMIT = 67` можно прочитать без запуска инициализации интерфейса, но `int[] VALUES` не является constant variable. Class initialization и связь default methods с инициализацией — отдельная точная тема [CORE-INIT](initialization.md) ([JLS 21 §12.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.1)).

## Четыре вида методов

| Декларация | Непосредственный смысл | Как используется |
|---|---|---|
| `void run();` | Неявный `public abstract`, тела нет | Конкретный класс обязан удовлетворить требование |
| `default void run() { ... }` | Публичный экземплярный метод с телом | В наследовании, override и dispatch |
| `static void help() { ... }` | Метод самого интерфейса, не наследуется реализацией | `Api.help()` |
| `private void help() { ... }` или `private static ...` | Внутренний helper с телом | Только код внутри данного интерфейса |

Отсутствие ключевого слова `public` перед обычным абстрактным методом интерфейса **не делает метод package-private**. Попытка `class X implements Api { void run(){} }` при `interface Api { void run(); }` — ошибка компиляции: реализация снижает видимость public-контракта. Не компилируются и `protected void run();` в интерфейсе, `private abstract void run();`, `static default void run() {}` и `default void run();` без тела.

Вызов `Standard.kind()` из центрального примера отвергается не из-за недоступности, а потому что static-методы интерфейсов **не наследуются классами**. Вызов `Policy.helper()` из клиента отвергается из-за `private`. Эти два ограничения нельзя смешивать. Основание — [JLS 21 §9.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4), [§9.4.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4.3).

## Разрешение default/abstract: пять разных ситуаций

### 1. Метод класса и default интерфейса

В центральном примере `Descendant extends Parent implements Welcome` получает `Parent.hello()`, возвращающий `"class"`. Конкретный метод класса имеет приоритет; интерфейсный default не подменяет существующую реализацию в иерархии классов.

**Опасное упрощение:** «любой метод суперкласса, даже abstract, уже реализует default». Нет: абстрактный метод класса не даёт готового тела.

```java
abstract class Parent {
    public abstract String text();
}
interface Welcome {
    default String text() { return "default"; }
}
class Child extends Parent implements Welcome { } // N03: ошибка javac
```

Несмотря на `Welcome.text()`, конкретный `Child` обязан предоставить реализацию унаследованного абстрактного метода суперкласса. Если `Child` сам абстрактный, такое обязательство может остаться невыполненным, но его тогда нельзя инстанцировать. Причина именно в наследовании и правилах классов, а не в «малой приоритетности» abstract ([JLS 21 §8.4.8.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.4)).

### 2. Два независимых default

```java
interface Left  { default int count() { return 67; } }
interface Right { default int count() { return 68; } }
class Broken implements Left, Right { } // N01: конфликт defaults
```

Класс должен объявить собственный override. Он может вызвать `Left.super.count()`, `Right.super.count()`, обе реализации либо написать новый алгоритм. В центральном примере `Both` вызывает обе. Никакого правила «победит первый/последний в implements» нет.

Иное положение, если два пути **ведут к одному исходному default**:

```java
interface Root { default int count() { return 67; } }
interface First extends Root { }
interface Second extends Root { }
class Diamond implements First, Second { }
// new Diamond().count() -> 67
```

Одна реализация унаследована двумя путями — это не два независимо объявленных default. Ср. [JLS 21 §9.4.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4.1.3), [§8.4.8.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.4).

### 3. Независимые abstract и default — тоже конфликт

```java
interface Required { String label(); }
interface Provided { default String label() { return "default"; } }
class Broken implements Required, Provided { } // N02: нужен override
```

В `Resolved` центрального примера этот случай разрешён явно: `public String status() { return DefaultStatus.super.status(); }`. Default не удовлетворяет автоматически несовместимому **параллельному** наследованию абстрактного метода другого интерфейса. Это другая причина ошибки, чем N03 с абстрактным методом **суперкласса**. Нельзя сводить оба механизма к одной схеме приоритетов.

### 4. Более специфичный подинтерфейс

`B extends A` может переопределить default из `A`. Поэтому `new Specific().value()` возвращает `"child"`: класс реализует более специфичный `B`.

Подинтерфейс также может переобъявить унаследованный default **абстрактным**:

```java
interface Base { default String token() { return "base"; } }
interface MustImplement extends Base {
    @Override String token();
}
class Concrete implements MustImplement {
    @Override public String token() { return "implemented"; }
}
```

У `Concrete` реализация обязательна: более специфичный контракт `MustImplement` действительно требует её. Это не конфликт двух независимых defaults ([JLS 21 §9.4.1.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4.1.1)).

### 5. Квалифицированный `InterfaceName.super.method()`

Такая форма вызывает конкретную default-реализацию **подходящей непосредственно указанной superinterface**. Она не равна приведению типа: `((Left) this).name()` выполняет обычный виртуальный вызов и может снова попасть в переопределение класса.

Два отдельных отрицательных фрагмента:

```java
interface A { default void run() {} }
interface B extends A {}
class N11_NonDirectSuper implements B {
    void test() { A.super.run(); } // N11: A не direct superinterface
}
```

```java
interface A { default void run() {} }
interface B extends A {}
class N12_RedundantSuper implements A, B {
    void test() { A.super.run(); } // N12: A redundant через B
}
```

Во втором случае `A` указана напрямую, но одновременно унаследована более специфичной `B` — такой квалификатор тоже запрещён. Каждую отрицательную программу компилируйте в **отдельном файле**, чтобы повторные объявления `A/B` не скрыли истинную причину отказа. См. [JLS 21 §15.12.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.1).

## Return types и checked exceptions: ещё одно измерение конфликтов

Подстановка реализации должна соблюдать return-type-substitutability, объявленные checked exceptions и достаточную видимость. **Допустимое сужение**:

```java
import java.io.IOException;
interface Source { Number get() throws Exception; }
class Good implements Source {
    @Override public Integer get() throws IOException { return 67; }
}
```

`Integer` — подтип `Number`, `IOException` — подтип `Exception`. Это соответствует последней строке центральной программы.

**Недопустимое расширение возвращаемого типа:**

```java
interface Source { Integer get(); }
class Bad implements Source {
    @Override public Number get() { return 67; } // compile-time error
}
```

**Недопустимое расширение checked throws:**

```java
import java.io.IOException;
interface Source { int get() throws IOException; }
class Bad implements Source {
    @Override public int get() throws Exception { return 67; } // compile-time error
}
```

Эти фрагменты намеренно самостоятельны, их типы не должны смешиваться с другими примерами в одной компиляции. Невозможно разрешить всякий конфликт только совпадением имени и параметров: несовместимые возвращаемые типы и checked throws способны сделать реализацию незаконной. Точная основа: [JLS 21 §8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3), [§8.4.8.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.4), [§9.4.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4.1.2). Это отдельный контрпример для IC-21, не только reabstract из плана G1.

## Интерфейс и методы `Object`

Интерфейс не наследует `java.lang.Object` как класс. Но JLS задаёт специальные правила для публичных instance methods `Object` ([§9.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.2)). Поэтому **запрещён** default-метод, override-equivalent non-private методу `Object`:

```java
interface Broken {
    default String toString() { return "bad"; } // N04
}
```

Аналогично нельзя объявить `default boolean equals(Object other)` (N17). Однако **абстрактное** `boolean equals(Object other);` допустимо: тип может требовать этот контракт, и такая декларация не добавляет вторую абстрактную операцию для SAM. Утверждение «в интерфейсах вообще нельзя объявлять equals» неверно. Подробный контракт identity и value-equality — [CORE-OBJECT](object.md), [CORE-EQUALITY](equality.md). Правила: [JLS 21 §9.4.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4.1.2).

## Functional interface, marker и abstract class

**Functional interface (SAM)** имеет одну логическую abstract-операцию. Не считаются самостоятельными SAM-операциями его `default`, `static` и `private` методы, а также abstract-декларации, совпадающие с публичными методами `Object`. В центральном примере `Operation` допускает lambda, хотя содержит `equals(Object)`, `twice()` и `identity()`. Аннотация `@FunctionalInterface` проверяет правильность объявления, но не изменяет его типовую семантику.

```java
@FunctionalInterface
interface Broken {
    void first();
    void second(); // N13: две независимые abstract-операции
}
```

Интерфейс может быть вообще без методов — *marker interface*:

```java
interface Marker {}
class Tagged implements Marker {}
// new Tagged() instanceof Marker -> true
```

Это метка, которую умеет интерпретировать конкретный API/код, но сама по себе она не даёт объекту нового алгоритма. См. [JLS 21 §9.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8). Лямбды, method references и intersection types полностью относятся к будущей CORE-FUNCTIONS.

**Интерфейс и абстрактный класс — не взаимозаменяемые декларации.**

| Характеристика | Интерфейс | Абстрактный класс |
|---|---|---|
| Непосредственное наследование | Несколько superinterfaces | Один superclass |
| Instance fields / конструктор | Нет | Да |
| Реализация | Default, static, private при ограничениях §9 | Обычные public/protected/private/final методы, abstract и шаблонные методы |
| Shared invariant / защищённый шаг | Нет собственного instance state | Может иметь protected state и final template |
| Типовой контракт | Многие несвязанные классы могут его реализовать | Участвует в одной цепочке наследования классов |

```java
abstract class Counter {
    protected final int initial;
    Counter(int initial) { this.initial = initial; }
    protected abstract int delta();
    public final int value() { return initial + delta(); }
}
class PlusOne extends Counter {
    PlusOne(int initial) { super(initial); }
    @Override protected int delta() { return 1; }
}
// new PlusOne(67).value() -> 68
```

Здесь состояние и конструктор являются частью реализации абстрактного класса. Интерфейс не хранит поле `initial` для каждого объекта. Но это не означает, что абстрактный класс *всегда лучше*: когда требуется комбинировать независимые политики, бывает удобнее [CORE-COMPOSITION](composition.md). Основание: [JLS 21 §8.1.1.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.1.1), [§8.1.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.5), [§9.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.3).

## Как правильно проверять отрицательные примеры

Все отрицательные фрагменты сохраняются в **разных** файлах (например, `N02.java` или `N03.java`). У них могут повторяться имена `A/Left/Bad`. Выполняйте:

```sh
javac --release 21 -d out N02.java
```

Ожидается **ненулевой exit code именно по нужной причине**. Ошибка «duplicate class» после склейки фрагментов не является доказательством abstract/default conflict. Текст диагностики зависит от компилятора; нормативен механизм JLS, не конкретные слова сообщения. Полный исследовательский комплект N01–N19 приведён в [плане G1 §5.2](../../docs/plans/CORE-INTERFACES-COMPOSITION.md), где роль 01 зафиксировала 19 независимых отказов. Это не означает, что автор данной статьи прогнал все 19 случаев повторно на её HEAD.

## Матрица CORE-INTERFACES

| ID | P | Раскрытие |
|---|---|---|
| IC-01 | P0 | «Что обещает интерфейс», центральный пример |
| IC-02 | P0 | «extends и implements» |
| IC-03 | P0 | «Четыре вида методов» |
| IC-04 | P0 | «Поля интерфейса» |
| IC-05 | P0 | Центральный пример, строка 1; CORE-INHERITANCE |
| IC-06 | P0 | «Метод класса и default»; N03 |
| IC-07 | P0 | «Два независимых default»; N01 |
| IC-08 | P0 | «Abstract и default»; N02 |
| IC-09 | P0 | «Более специфичный подинтерфейс» |
| IC-10 | P0 | «InterfaceName.super.method»; N11/N12 |
| IC-11 | P0 | «Интерфейс и методы Object»; N04/N17 |
| IC-12 | P0 | «Интерфейс и абстрактный класс» |
| IC-13 | P0 | «Functional interface, marker» |
| IC-17 | P1 | «Четыре вида методов», static/private |
| IC-18 | P1 | Совместимые abstract contracts, marker |
| IC-19 | P1 | Выбор метода, class/default/specific override |
| IC-20 | P1 | Инициализация и CORE-INIT |
| IC-21 | P1 | Return-type и checked throws; два отрицательных контрпримера |
| IC-25 | P2 | Sealed interfaces — граница CORE-SEALED |
| IC-26 | P2 | Generics, erasure, bridge — граница CORE-GENERICS |
| IC-27 | P2 | Lambdas, method refs, intersection — граница CORE-FUNCTIONS |
| IC-28 | P2 | Source/binary API compatibility — отдельное исследование |

Оставшиеся **IC-14–IC-16 P0** (strategy/delegation/decorator), **IC-22–IC-24 P1** и **IC-29–IC-30 P2** раскрыты в [CORE-COMPOSITION](composition.md). Не нужно переписывать содержание соседней статьи здесь.

## Ограничения и источники

- **Языковые гарантии:** [JLS 21 §9.1–§9.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html), [§9.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8), [§8.4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8), [§15.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12), [§12.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.1).
- **Java SE 21 API:** [Runnable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runnable.html), [Comparator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html), [Function](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Function.html).
- **Независимые интервью-вопросы:** [in28minutes](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers), [Hruday-Boppa](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List), [Chernyshov RU](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers), [teamlead](https://github.com/teamlead/java-interview-questions). Это источники формулировок вопросов, **не** доказательство гарантий JLS или частоты на рынке.

**Границы P2.** Sealed/permits (IC-25), generics/erasure (IC-26), продвинутые SAM/lambda (IC-27) и бинарная совместимость API при добавлении default/abstract (IC-28) — отдельные темы. В частности, добавление default к существующему интерфейсу **не** обещает отсутствия source-level конфликтов при множественном наследовании. Полная модель class initialization — в [CORE-INIT](initialization.md), composition/LSP/design — в [CORE-COMPOSITION](composition.md). Успешная компиляция одного сценария не доказывает отсутствие конфликтов во всех реализациях.
