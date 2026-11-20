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
