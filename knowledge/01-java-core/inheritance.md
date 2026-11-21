# CORE-INHERITANCE — наследование, перегрузка и переопределение в Java 21

**Java SE 21.** Исследовательская основа: [матрица CORE-INHERITANCE](../../docs/plans/CORE-INHERITANCE.md) (IH-01–IH-25). Для понимания достаточно знать классы, объекты и ссылочные типы: [CORE-CLASSES](classes.md), [CORE-TYPES](types.md).

## Зачем нам наследование и что именно оно обещает

Представим, что сервис работает с объектами разных видов документов. Общий код может принимать базовый тип `Document`, а конкретные объекты по-разному реализуют операцию `render()`. Наследование позволяет определить отношение *подтип — надтип*: там, где контракт требует `Document`, можно передать подходящий `PdfDocument`. Но этот факт сам по себе **не** означает, что любой метод одноимённого подкласса будет вызван автоматически.

Особенно важно различить три вопроса:

1. **Доступно ли объявление и какую перегрузку выбирает компилятор?** Это зависит от типов выражений и аргументов, правил доступа и поиска методов.
2. **Если выбран метод экземпляра, какую переопределяющую реализацию исполнить?** Здесь обычно имеет значение фактический класс объекта во время выполнения.
3. **А если это поле или `static` метод?** Для них нет такого виртуального переопределения: статическая типизация определяет выбранный член.

Смешение этих этапов и порождает ошибочные ответы вроде «Java всегда вызывает метод по классу реального объекта». Точное языковое описание — [JLS 21 §15.12](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12).

## Иерархия, extends и границы наследования

`extends` у класса объявляет его непосредственный суперкласс. Обычный класс может непосредственно расширять **не более одного класса**, но может реализовывать несколько интерфейсов через `implements`:

```java
interface Exportable { void export(); }
interface Auditable { void audit(); }

class Document { }

class Report extends Document implements Exportable, Auditable {
    @Override public void export() { }
    @Override public void audit() { }
}
```

`Report` является подтипом `Document` и реализует два интерфейсных контракта. Запись `class Report extends Document, Other {}` не допускается: в Java нет двух прямых *классов*-супертипов через `extends`. Правила множественного наследования интерфейсов и их `default`-методов подробно разобраны в [CORE-INTERFACES](interfaces.md).

**Исключение, которое часто забывают:** у самого `java.lang.Object` **нет суперкласса**. Если у обычного объявления класса нет `extends` (и это не `Object`), его непосредственным суперклассом считается `Object`. Следовательно, фраза «каждый класс явно расширяет другой класс» неверна. Например, у `class Document {}` непосредственный суперкласс — `Object` без явной записи `extends Object` ([JLS 21 §8.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.4)).

При наследовании класс получает доступные для наследования члены в соответствии с правилами JLS §8.2: важны модификаторы, пакет и объявления самого подкласса. `private` член родителя **не наследуется**, а package-private член из чужого пакета не становится наследуемым доступным API. **Конструкторы не наследуются вообще**: у подкласса собственные конструкторы, которые вызывают конструктор суперкласса по правилам цепочки создания. Не следует отождествлять наследование и простое копирование текста родительского класса.

Отдельные понятия создания экземпляра и инициализации подробно рассматриваются в [CORE-CLASSES](classes.md); правила `private`, `protected` и пакетов — в [CORE-ACCESS](access.md).

## Одна переменная — два важных типа

```java
Document ref = new Report();
```

**Статический (объявленный) тип** переменной `ref` — `Document`. **Фактический класс** созданного объекта — `Report`. Присваивание ссылки на экземпляр подкласса переменной типа родителя — допустимое расширяющее ссылочное преобразование (*upcast*); оно не создаёт копию объекта.

Статический тип ограничивает **какие члены можно выбрать и вызвать из исходного кода**. Например, если только `Report` объявляет `specialReport()`, строка `ref.specialReport()` не скомпилируется. Компилятор не выводит наличие этого метода из выражения `new Report()`, записанного справа при первоначальном присваивании: значение переменной впоследствии может стать ссылкой на другой допустимый подтип.

Явное приведение `((Report) ref).specialReport()` позволяет компилятору рассматривать выражение как `Report`. Оно допустимо к исполнению лишь тогда, когда фактический объект совместим с `Report`; иначе возможен `ClassCastException`. Подробнее о преобразованиях — [CORE-CONVERSIONS](conversions.md).

## Главный пример: сначала выбирается метод, потом — реализация

Ниже — **законченное самостоятельное** демо. Сохраните листинг в файл `Dispatch.java`. Здесь показан компактный вариант механизма: в **отдельном многофайловом примере** [CORE-INHERITANCE](../../examples/core/inheritance/README.md) роль 03 использует те же имена `Base`/`Derived`, метод `describe`, статический `kind()` и поле `code`, но размещает классы в двух пакетах и добавляет проверки других правил. Оба примера самостоятельны: не копируйте их классы в одну директорию для общей компиляции.

```java
public class Dispatch {
    static class Base {
        String describe(Object arg) { return "base:object"; }
        static String kind() { return "base:static"; }
        int code = 67;
    }

    static class Derived extends Base {
        @Override
        String describe(Object arg) { return "derived:object"; }

        String describe(String arg) { return "derived:string"; }

        static String kind() { return "derived:static"; }
        int code = 68;

        String throughSuper() { return super.describe("X"); }
        String throughCast() { return ((Base) this).describe("X"); }
        int parentCode() { return super.code; }
    }

    public static void main(String[] args) {
        Base ref = new Derived();
        System.out.println(ref.describe("X"));
        System.out.println(((Derived) ref).describe("X"));
        System.out.println(Base.kind() + "/" + Derived.kind());
        System.out.println(ref.code + "/" + ((Derived) ref).code);
        System.out.println(((Derived) ref).throughSuper()
                + "/" + ((Derived) ref).throughCast());
        System.out.println(((Derived) ref).parentCode());
    }
}
```

Вывод:

```text
derived:object
derived:string
base:static/derived:static
67/68
base:object/derived:object
67
```

После сохранения `Dispatch.java` выполните в каталоге с этим файлом на JDK 21:

```sh
javac --release 21 Dispatch.java
java Dispatch
```

**Почему первая строка — `derived:object`?** Компилятор видит `ref` типа `Base`. Среди доступных ему методов с именем `describe` есть только `describe(Object)`, значит сигнатура выбрана **при компиляции**. Переданный `String` допустимо преобразуется к `Object`. Затем во время выполнения для выбранного экземплярного метода находится переопределение в фактическом объекте `Derived`: возвращается `"derived:object"`.

**Почему вторая — `derived:string`?** Выражение `((Derived) ref)` имеет статический тип `Derived`. Теперь при поиске перегрузок доступны `describe(Object)` и `describe(String)`. Для аргумента `"X"` наиболее специфичен `describe(String)`, поэтому компилятор выбирает **другую сигнатуру**. Искомая реализация уже находится в `Derived`.

Это не два конкурирующих способа виртуального выбора. Последовательность одна: **статический тип → поиск и выбор перегрузки → допустимость вызова → исполнение выбранной реализации**. Простейшие преобразования аргументов и этапы перегрузки изложены в [CORE-CONVERSIONS](conversions.md); подробная спецификация — [JLS 21 §§15.12.1–15.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12).

## Overloading и overriding — различие на уровне объявлений

**Перегрузка (overloading)** — несколько методов с одним именем, но различающимися списками параметров / сигнатурами. Она может существовать даже без наследования. Метод `describe(String)` не переопределяет `describe(Object)`, а образует другую перегрузку. Тип результата сам по себе не является новым списком параметров:

```java
// НЕ КОМПИЛИРУЕТСЯ, отдельный самостоятельный пример:
class Wrong {
    int code() { return 67; }
    String code() { return "67"; } // одна и та же сигнатура
}
```

**Переопределение (overriding)** — объявление экземплярного метода в подклассе, соответствующего наследуемому методу по правилам сигнатуры (*subsignature*) и совместимости результата, доступа и исключений. `Derived.describe(Object)` — переопределение. Аннотация `@Override` помогает обнаружить ошибку соответствия.

Сигнатура в терминах Java не определяется только именем и типом возвращаемого значения. Для сложных обобщённых методов действует понятие subsignature из JLS §8.4.2; глубокие случаи стирания типов остаются в [CORE-ERASURE](erasure.md).

## Почему static методы не переопределяются

В примере из `Dispatch` объявления `Base.kind()` и `Derived.kind()` являются `static`. Они **скрывают** одноимённый статический метод (*static method hiding*), но не создают виртуального override.

Правильная, читаемая форма вызова — через имя класса:

```java
System.out.println(Base.kind());    // base:static
System.out.println(Derived.kind()); // derived:static
```

Java допускает и выражение `ref.kind()`, однако выбор статического метода не зависит от фактического объекта `Derived`. Для `Base ref` это будет `Base.kind()`, что легко ошибочно принять за динамическую диспетчеризацию. Предпочитайте `Base.kind()`.

Смешение статического и экземплярного метода с override-эквивалентной сигнатурой **не становится другим вариантом hiding**, а отвергается компилятором:

```java
// НЕ КОМПИЛИРУЕТСЯ; отдельный файл:
class A { static void act() {} }
class B extends A { void act() {} } // instance вместо static
```

И обратный случай — попытка объявить `static void act()` в подклассе поверх доступного экземплярного `void act()` — также ошибка. `static` методы разрешается **перегружать** другими параметрами, но не виртуально переопределять ([JLS 21 §8.4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8)).

## Поля тоже не переопределяются

У `Base` и `Derived` в примере есть собственные поля `code`. При обращении `ref.code` компилятор разрешает имя поля через **статический тип** `Base` и получает `Base.code = 67`. После приведения к `Derived` разрешается другое объявление: `Derived.code = 68`. Здесь нет «виртуального выбора поля» по фактическому классу объекта.

Эти поля хранятся в одном объекте, но представляют **два разных члена**. Скрытие работает даже тогда, когда поля имеют разные типы. Java также допускает скрытие `static` поля экземплярным полем и наоборот — в отличие от запрета на такую же подмену формы у методов:

```java
class ParentField { static int code = 67; }
class ChildField extends ParentField { int code = 68; }

ParentField ref = new ChildField();
System.out.println(ref.code);                // 67
System.out.println(((ChildField) ref).code); // 68
```

Этот фрагмент следует помещать внутрь метода, а классы объявить отдельно: он показывает правило выбора поля, а не законченный файл программы. Для чтения статических полей лучше использовать имя объявляющего типа; `ref.code` здесь используется только для контраста.

Итого: **экземплярные методы могут переопределяться, поля — скрываются, статические методы — скрываются при совместимых объявлениях**. Основание: [JLS 21 §8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3), [§15.11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.11).

## this, super и приведение к родителю — не одно и то же

В `Derived` из основного примера:

- `super.describe("X")` вызывает реализацию метода суперкласса `Base`, поэтому возвращает `base:object`.
- `((Base) this).describe("X")` **не отключает** динамическую диспетчеризацию; выбранная через тип `Base` сигнатура `describe(Object)` исполняется в объекте `Derived` и возвращает `derived:object`.
- `super.code` обращается к **полю Base**, поэтому возвращает 67; приведение `((Base) this).code` для поля тоже разрешило бы имя по `Base`, но для метода это не эквивалент `super`.

`super` разрешено использовать только в подходящем контексте экземпляра подкласса. Например, `super.describe("X")` внутри `static` метода невозможно. Нельзя вызывать `super` через произвольную внешнюю ссылку как `other.super.describe()`. Это языковая форма обращения из тела текущего класса, а не способ выбрать любой объект «на уровень выше».

Конструкторы применяют `super(...)` иначе: это обращение к конструктору родителя, а не переопределение. Сами конструкторы не наследуются. Порядок инициализации здесь не раскрываем целиком — он разобран в [CORE-INIT](initialization.md).

## private и package-private: одно имя ещё не значит override

Не каждое совпадающее имя метода образует полиморфное переопределение. Например, родительский `private` метод не наследуется подклассом, поэтому одноимённый метод `public` в дочернем типе **не подменяет** вызовы родителя:

```java
class PrivateBase {
    private String step() { return "base-private"; }
    String execute() { return step(); }
}
class PrivateChild extends PrivateBase {
    public String step() { return "child-public"; }
}

// из main:
PrivateChild child = new PrivateChild();
System.out.println(child.execute()); // base-private
System.out.println(child.step());    // child-public
```

При выполнении `execute()` код класса `PrivateBase` вызывает **свой private метод** `step()`; у него нет виртуального override в `PrivateChild`.

Аналогичная ловушка возникает с package-private методом, объявленным в **другом пакете**:

```java
// api/Base.java
package api;
public class Base {
    String internal() { return "base"; } // package-private
    public String run() { return internal(); }
}
```

```java
// client/Child.java
package client;
public class Child extends api.Base {
    public String internal() { return "child"; } // НЕ override Base.internal()
}
```

Вызов `new Child().run()` вернёт `"base"`: в `api.Base.run` выбирается собственный пакетный метод `internal()`, а одноимённый метод из другого пакета его не переопределяет. Это не ошибка при объявлении `Child.internal`. Но если добавить к нему `@Override`, компилятор **отвергнет аннотацию**, обнаружив, что override отсутствует.

Почему типы и пакеты имеют значение, подробно раскрыто в [CORE-ACCESS](access.md), включая ограничения `protected` в другом пакете.

## @Override: проверка, а не переключатель

Аннотация `@Override` сообщает компилятору, что метод должен действительно переопределять метод надтипа или реализовывать подходящий интерфейсный метод по применимым правилам. Если это не так, будет **ошибка компиляции**.

```java
class Base { void act(Object arg) {} }
class Derived extends Base {
    @Override void act(Object arg) {} // корректное переопределение
}
```

А следующий пример — **отдельный некомпилируемый вариант**:

```java
class Base { void act(Object arg) {} }
class Derived extends Base {
    @Override void act(String arg) {} // НЕ компилируется: это новая перегрузка
}
```

Без `@Override` такое объявление `act(String)` было бы допустимо как перегрузка. Поэтому аннотация ловит ошибку намерения автора. Она **не меняет** правила динамической диспетчеризации; корректный override работает и без неё. Кроме того, `@Override` допускается при реализации метода интерфейса; полный разбор интерфейсных контрактов приведён в [CORE-INTERFACES](interfaces.md). Источник — [Java SE 21 API Override](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Override.html).

## Ограничения переопределения: final, доступ, результат, исключения

### final — запрещает дальнейшее наследование или переопределение

`final class` нельзя расширить. `final` метод экземпляра нельзя переопределить, `final static` метод нельзя скрыть. Это **разные правила** от `final` у поля или локальной переменной, где запрет касается переназначения. При этом наследовать `final` метод как вызываемую операцию можно.

Следующие варианты **не компилируются по отдельности**:

```java
final class Fixed {}
class Child extends Fixed {} // ошибка: final superclass
```

```java
class Base { final void run() {} }
class Child extends Base {
    @Override void run() {} // ошибка: final instance method
}
```

```java
class Base { static final void run() {} }
class Child extends Base {
    static void run() {} // ошибка: нельзя скрыть final static
}
```

Это ограничения языка, **не** обещание конкретного вида байткода или гарантированного JIT-inlining.

### Нельзя уменьшать доступность

Если родительский метод `protected`, переопределение не может стать package-private или `private`. Допустимо расширить доступ до `public`.

```java
class Base { protected void run() {} }
class Good extends Base {
    @Override public void run() {} // допустимо
}
```

А это **другой, некомпилируемый вариант**:

```java
class Base { protected void run() {} }
class Wrong extends Base {
    @Override void run() {} // ошибка: доступ сужен
}
```

### Ковариантный возвращаемый тип

Для ссылочных результатов подкласс при соблюдении правил [JLS 21 §8.4.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.5) может возвращать более конкретный совместимый тип:

```java
class NumberSource {
    Number read() { return 67; }
}
class IntegerSource extends NumberSource {
    @Override Integer read() { return 68; } // Number → Integer допустимо
}
```

Если переменная объявлена `NumberSource`, статический тип выражения `source.read()` остаётся `Number`, хотя реальный вызов `IntegerSource.read()` вернёт `Integer`. Это ещё одна причина различать *объявленный результат* и *реальный объект*.

Обратный переход `Number → Object` в переопределении при родительском возвращаемом `Number` недопустим:

```java
// НЕ КОМПИЛИРУЕТСЯ:
class Base { Number read() { return 67; } }
class Child extends Base {
    @Override Object read() { return "wrong"; } // более широкий результат
}
```

При примитивных результатах такие «ковариантные» послабления не действуют. И нельзя перегрузить методы **только** различием результата.

### Checked throws не разрешается произвольно расширять

Пусть родитель объявляет проверяемое исключение `IOException`. Подкласс может сузить объявленное checked исключение до `FileNotFoundException` или вообще не объявлять проверяемых исключений:

```java
import java.io.FileNotFoundException;
import java.io.IOException;

class NumberSource {
    Number read() throws IOException { return 67; }
}
class IntegerSource extends NumberSource {
    @Override Integer read() throws FileNotFoundException { return 68; }
}
```

Но обратная замена **`throws FileNotFoundException` в родителе на `throws IOException` в override** не компилируется:

```java
// НЕ КОМПИЛИРУЕТСЯ; отдельный файл:
import java.io.*;
class Base { void read() throws FileNotFoundException {} }
class Child extends Base {
    @Override void read() throws IOException {} // новый более широкий checked throws
}
```

Ограничение касается **checked exceptions**. Можно объявлять подходящие unchecked исключения без того же ограничения; это не освобождает от проектного контракта и не означает, что исключение будет обязательно выброшено. Вызвавший метод через ссылку `NumberSource` компилятор проверяет согласно объявлению метода, выбранному **по статическому типу**: даже если конкретный override не бросает `IOException`, вызывающему коду может потребоваться обработать или объявить его. Иерархия исключений и статическая проверка `throws` подробно разобраны в [CORE-EXCEPTIONS](exceptions.md).

## Перегрузки в наследовании и неоднозначный null

Подкласс может одновременно **переопределить** одну унаследованную сигнатуру и **добавить** новую перегрузку, не отменяя остальных. Компилятор ищет наиболее подходящий метод среди допустимых кандидатов. Поэтому важно не только родство классов, но и статические типы параметров:

```java
class Printer {
    void print(String value) {}
    void print(Integer value) {}
    void demo() {
        // print(null); // НЕ компилируется: неоднозначность двух несравнимых типов
        print((String) null); // выбирается print(String)
    }
}
```

`String` и `Integer` не находятся в отношении подтипов друг к другу; литерал `null` совместим с обоими. Ни одна перегрузка не становится однозначно более специфичной. Это **ошибка на этапе компиляции**, не `NullPointerException` в процессе вызова.

Полному алгоритму выбора перегрузки, упаковке и преобразованиям аргументов посвящена [CORE-CONVERSIONS](conversions.md); здесь достаточно видеть, что *runtime-класс объекта-получателя не выбирает новый список параметров*.

## abstract и интерфейсы: краткие границы

`abstract class` может содержать реализованные методы, поля и конструкторы, но нельзя напрямую создать его обычным `new AbstractBase()`. `abstract` метод объявляет контракт без реализации; конкретный подкласс должен обеспечить реализацию применимых абстрактных методов либо сам остаться абстрактным.

```java
abstract class Formatter {
    abstract String format(int value);
}
class DecimalFormatter extends Formatter {
    @Override String format(int value) { return Integer.toString(value); }
}
```

Сочетание `abstract final class` не компилируется: класс одновременно должен требовать наследования ради реализации и запрещать наследование. Это не эквивалент `final` метода. Методы `private abstract` также несовместимы по смыслу и правилам языка.

Если у интерфейса есть `default` метод с той же сигнатурой, что унаследованный подходящий метод класса, обычно **метод класса имеет приоритет**:

```java
interface Named { default String label() { return "interface"; } }
class Parent { public String label() { return "class"; } }
class Child extends Parent implements Named {}

// new Child().label() возвращает "class"
```

А два конфликтующих унаследованных `default` метода разных интерфейсов без разрешения конфликта вызывают ошибку компиляции. Возможны тонкие дополнительные ограничения, поэтому эту тему не заменяем здесь упрощённой формулой «интерфейсы никогда не конфликтуют». Подробности — в [CORE-INTERFACES](interfaces.md).

## Опасность виртуального вызова из конструктора

Динамическая диспетчеризация действует даже тогда, когда конструирование подкласса **ещё не завершено**. Это важное предостережение:

```java
class EarlyBase {
    EarlyBase() { show(); }
    void show() {}
}
class EarlyChild extends EarlyBase {
    int code = 67;
    @Override void show() { System.out.println(code); }
}
class InitDemo {
    public static void main(String[] args) { new EarlyChild(); }
}
```

Вывод — **`0`**, а не `67`. Конструктор `EarlyBase` вызывает переопределённый `EarlyChild.show()` до того, как инициализатор поля `code = 67` в подклассе был выполнен. На момент вызова поле имеет значение по умолчанию `0`. Это пример риска, а не рекомендация так писать.

Для конструктора безопаснее не вызывать методы, которые могут быть переопределены ещё неинициализированным подклассом. Подробная последовательность создания объектов, риски публикации `this` и порядок инициализации разобраны в [CORE-INIT](initialization.md); спецификация — [JLS 21 §12.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.5).

## Наследование, LSP и хрупкий базовый класс — вопрос дизайна

Законность `extends` **не гарантирует** корректную подстановку объекта подкласса в бизнес-логике. Принцип подстановки Лисков (*LSP*) — проектный критерий: если код ожидает обещанное поведение базового типа, подходящий подкласс не должен неожиданно ломать это обещание. Это **не дополнительная норма компиляции JLS**.

Конкретная ситуация: договоримся, что успешный вызов `MessageStore.save(message)` увеличивает число сохранённых сообщений на один. Следующий наследник формально **компилируется**, но нарушает контракт:

```java
import java.util.ArrayList;
import java.util.List;

class MessageStore {
    private final List<String> messages = new ArrayList<>();
    void save(String message) { messages.add(message); }
    int count() { return messages.size(); }
}

class SilentStore extends MessageStore {
    @Override void save(String message) {
        // тихо игнорирует запись — нарушает обещание базового типа
    }
}
```

Если клиент работает с `MessageStore store = new SilentStore();` и после успешного `save("67")` ожидает увеличения `count()`, его бизнес-предположение окажется неверным. Компилятор этого не обнаружит: сигнатура подходит, но **поведенческий** контракт нарушен.

Композиция позволяет явно **отделить другую политику** от контракта хранения:

```java
final class OptionalSaver {
    private final MessageStore delegate;

    OptionalSaver(MessageStore delegate) {
        this.delegate = delegate;
    }

    boolean trySave(String message, boolean enabled) {
        if (!enabled) return false; // клиент видит, что записи не было
        delegate.save(message);     // контракт MessageStore сохраняется
        return true;
    }
}
```

`OptionalSaver` **содержит** `MessageStore`, но **не выдаёт себя** за такой же подтип; клиент вызывает отдельную операцию `trySave` и проверяет результат. Смысл не в том, что композиция всегда лучше наследования, а в том, что у разных контрактов должны быть разные явные границы.

Связанная проблема — *fragile superclass*: подкласс переопределяет `protected` метод, на частоту/порядок вызова которого полагается. Если родитель позже начинает вызывать этот hook дважды вместо одного раза, поведение наследника неожиданно меняется, хотя его исходник не трогали. При композиции взаимодействия с внедрённым зависимым компонентом формулируются отдельным интерфейсом/контрактом, и изменения внутренней реализации родителя не становятся скрытым расширением точек вызова подкласса. Документированные template-method контракты могут, напротив, оправдывать наследование. Подробный выбор между подходами — в [CORE-COMPOSITION](composition.md).

## Какие ошибки проверить отдельно

Ниже каждый пример — **намеренно некомпилируемый**, и его нужно проверять в отдельном файле/изолированном каталоге, не объединяя все блоки с одинаковыми именами классов:

- `extends` от `final class`; override `final` метода; скрытие `final static` метода.
- `static` метод вместо экземплярного или наоборот с override-эквивалентной сигнатурой.
- `@Override` на новый overload `act(String)` при единственном `Base.act(Object)`.
- Переопределение с более широким ссылочным результатом, суженным уровнем доступа или расширенным checked `throws`.
- Два метода только с различным return type.
- Два класса в `extends A, B`, `abstract final class`, конфликт интерфейсных `default`.
- Вызов `super.method()` из статического контекста.
- Неоднозначный вызов `print(null)` при `print(String)` и `print(Integer)`.

Это **разные причины отказа** на этапе компиляции; нельзя трактовать любой ненулевой код завершения `javac` как доказательство нужного правила. Полный рабочий пример роли 03 уже включён в эту интеграционную ветку: [README, исходники и 12 отдельных negative cases](../../examples/core/inheritance/README.md), работа — [Issue #39](https://github.com/Melchiorius/backend-interview-labs/issues/39) / [PR #44](https://github.com/Melchiorius/backend-interview-labs/pull/44). Входная точка — `client.InheritanceDemo`, файлы находятся в `examples/core/inheritance/` и **не входят** в текстовые одноклассовые фрагменты выше.

На JDK 21 из корня репозитория можно запустить все положительные и отрицательные проверки:

```sh
bash examples/core/inheritance/verify.sh
```

Скрипт должен вывести `CHECK PASSED`, затем 12 строк `EXPECTED REJECTION` с диагностикой компилятора и закончить `ALL EXPECTED OUTCOMES OBSERVED`. Каждый отрицательный файл компилируется отдельно. Команды для одиночного положительного запуска без Bash, точный stdout и пояснения каждого отрицательного файла даны в [README](../../examples/core/inheritance/README.md). Результаты CI **для объединённого HEAD** необходимо сверять отдельно: старый зелёный CI PR #44 относится к другой версии дерева.

## Как отвечать на нестандартный фрагмент кода

Сначала определите **статический тип ссылки** и **типы аргументов**; затем найдите доступные методы и **сигнатуру, выбранную компилятором**. После этого спросите, является ли выбранный член экземплярным виртуально вызываемым методом, `static` методом, полем, `private` методом или особым вызовом `super`. Только для допускающего виртуальную диспетчеризацию вызова переходите к фактическому классу объекта и его override.

Вопрос «что выведет?» не равен вопросу «скомпилируется ли?». Сначала докажите корректность объявления, доступность и выбор сигнатуры; только затем вычисляйте результат. Помните, что `@Override` помогает обнаружить неверное намерение автора, а отсутствие этой аннотации не отменяет переопределение.

## Границы и дальнейшие темы

- **Действующие локальные материалы:** [классы и конструкторы](classes.md), [доступность членов](access.md), [статический и фактический типы](types.md), [преобразования и перегрузки](conversions.md), [передача аргументов](parameters.md), [порядок инициализации](initialization.md).
- **Объект и равенство:** [CORE-OBJECT](object.md), [CORE-EQUALITY](equality.md).
- **Интерфейсы и композиция:** [CORE-INTERFACES](interfaces.md), [CORE-COMPOSITION](composition.md).
- **Обобщённые типы и стирание:** [CORE-GENERICS](generics.md), [CORE-ERASURE](erasure.md).
- **Исключения и управление ресурсами:** [CORE-EXCEPTIONS](exceptions.md), [CORE-RESOURCES](resources.md).
- **Будущие темы, пока без локальных ссылок:** `CORE-SEALED`, `CORE-RECORDS`, `JVM-BYTECODE`, `JVM-JIT` и особенности фреймворков/прокси.

В этой статье не раскрывается подробно механизм стирания и bridge-методов при переопределении: он разобран в [CORE-ERASURE](erasure.md); не раскрываются подробно sealed/record ограничения (`record` как класс является `final`); не измеряется работа JIT. **JLS 21 не требует конкретной универсальной таблицы vtable или обязательной формы байткода для каждого вызова**: реализацию JVM не подменяем правилами языка. Контракты `Object.equals/hashCode/toString` и взаимодействие proxy/ORM с наследованием требуют отдельного обсуждения.

## Первоисточники Java 21

- [JLS §8.1.4 — superclass](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.4), [§8.2 — members/inheritance](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.2), [§8.3 — field hiding](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3).
- [JLS §8.4.2 — method signatures](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.2), [§8.4.8 — override/hide](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8), [§8.4.8.3 — returns/access/throws](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3).
- [JLS §15.11 — fields](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.11), [§15.12 — method invocation](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12), [§12.5 — object construction](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.5).
- [Java SE 21 @Override](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Override.html). Нормативная матрица с внешними интервью-подборками — [план исследования](../../docs/plans/CORE-INHERITANCE.md).

Проверки на конкретном JDK помогают ловить ошибки примеров, но не заменяют спецификацию и независимое ревью.
