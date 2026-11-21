# Вложенные классы Java 21: nested, inner, local и anonymous

В Java класс может быть объявлен внутри другого класса, метода или выражения создания объекта. Эти формы выглядят похоже, но по-разному относятся к **окружающему экземпляру**, именам и переменным. От этого зависят компиляция, конструирование и то, что означает `this`.

Ключевой вопрос собеседования — не «чем static inner отличается от обычного», а **какой именно внешний контекст существует у данного объявления и что разрешено из него использовать**. В терминологии [JLS 21 §8.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.3) `static nested class` — вложенный, **но не inner** класс. Выражение «static inner class» в разговорах встречается, но нормативно неточно.

Связанные темы: [классы и конструкторы](classes.md), [доступ и инкапсуляция](access.md), [наследование и вызовы методов](inheritance.md), [порядок инициализации](initialization.md), [типы и ссылки](types.md), [лямбды и method references](functions.md). Исходный перечень проверяемых случаев: [исследовательский план CORE-NESTED/FUNCTIONS](../../docs/plans/CORE-NESTED-FUNCTIONS.md).

## Четыре формы — какие у них свойства

| Форма | Где объявляется | Есть имя типа в исходном коде | Требуется охватывающий экземпляр | Характерная особенность |
|---|---|---|---|---|
| **Static nested member** | В теле класса с `static` | Да | Нет | Упоминается как `Outer.Nested` |
| **Non-static inner member** | В теле класса без `static` | Да | Да, при обычном создании | `outer.new Inner()`, `Outer.this` |
| **Local class** | В блоке, например внутри метода | Да, но область видимости локальная | Зависит от контекста объявления | Может захватывать effectively-final локальные переменные |
| **Anonymous class** | В выражении `new Parent(...) { ... }` | Нет собственного имени в исходном коде | Зависит от контекста и суперкласса | Одновременно определяет класс и создаёт объект |

Оговорка: не всякое лексически вложенное объявление автоматически несёт ссылку на объект `Outer`. В том числе статический контекст и неявно статические типы меняют правила. Здесь описываем **семантику языка**, а не обещаем определённую структуру полей компиляторного classfile.

## 1. Static nested: вложен по объявлению, независим от объекта

`static nested` удобно использовать для небольшой вспомогательной модели или реализации, логически относящейся к внешнему типу, но не нуждающейся в состоянии конкретного внешнего объекта.

```java
class Envelope {
    private int instanceValue = 67;
    private static int globalValue = 68;

    static class Nested {
        int readGlobal() {
            return globalValue; // разрешено
        }

        // int readInstance() { return instanceValue; }
        // Ошибка: нет неявного экземпляра Envelope
    }
}

class Test {
    public static void main(String[] args) {
        Envelope.Nested nested = new Envelope.Nested();
        System.out.println(nested.readGlobal()); // 68
    }
}
```

Создание `new Envelope.Nested()` не вызывает конструктор `Envelope` для нового охватывающего объекта. Доступ к закрытым членам внешнего класса может быть разрешён правилами вложенного доступа, но **доступность имени не создаёт объект**. Статическое поле доступно без внешнего экземпляра; instance-поле требует явной ссылки, например переданной в конструктор `Nested(Envelope owner)`.

**Контрпример NF-02 (N02):** попытка вернуть `instanceValue` из `Nested.readGlobal()` без receiver не компилируется. Это не ошибка `private`, а отсутствие неявного `Envelope.this`. [JLS §8.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.3).

## 2. Inner member: внешний экземпляр определён

Non-static member class — *inner member class*. В типичном случае новый `Inner` связан с конкретным экземпляром `Outer`; это не то же самое, что просто хранить в поле произвольный `Outer`.

```java
class Outer {
    private final int id;
    Outer(int id) { this.id = id; }

    class Inner {
        int idOfOwner() { return Outer.this.id; }
    }
}

class Test {
    public static void main(String[] args) {
        Outer a = new Outer(67);
        Outer b = new Outer(68);
        System.out.println(a.new Inner().idOfOwner()); // 67
        System.out.println(b.new Inner().idOfOwner()); // 68
    }
}
```

`Outer.this` — **квалифицированное `this`**, обращение к лексически охватывающему экземпляру соответствующего типа. Обычное `this` внутри `Inner` обозначает сам `Inner`. Поэтому две строки отличаются не потому, что значения `id` случайны, а потому, что два объекта `Inner` связаны с разными объектами `Outer`.

Из instance-метода `Outer` часто достаточно `new Inner()` — там присутствует подходящий неявный охватывающий экземпляр. В `static`-методе `Outer` такая запись без явного внешнего объекта не имеет основания.

```java
class CannotCreate {
    class Inner {}

    static void test() {
        // new Inner(); // N01: нет enclosing instance
        CannotCreate owner = new CannotCreate();
        owner.new Inner(); // теперь явно выбран owner
    }
}
```

Отдельный N09: `Outer.this` внутри `static class Nested` недопустим. N14: `new Outer.Inner()` не подменяет требуемую форму с объектом. Язык различает **имя member type** и **экземпляр, относительно которого создаётся inner**. См. [JLS §15.9.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9.2), [§15.8.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.8.4).

### Когда охватывающий экземпляр может стать проблемой

Если внутренний объект живёт дольше внешнего и ему требуется внешний контекст, можно непреднамеренно продлить время достижимости внешнего объекта. Это **проектный риск**, а не утверждение, что каждый компилятор создаёт и сохраняет конкретное синтетическое поле во всех случаях. Если связь с внешним состоянием не нужна, рассмотрите `static nested` и явно переданные зависимости. Про жизненный цикл объектов см. [CORE-OBJECT](object.md); о выборе делегирования — [CORE-COMPOSITION](composition.md).

## 3. В Java 21 inner/local/anonymous могут содержать static-члены

Частая ошибка — повторять правило старых выпусков Java: «внутреннему классу разрешены только static final compile-time constants». **Начиная с Java SE 16 это ограничение изменено.** В Java 21 внутри inner class допустимы обычные static-поля, методы и static initializers; аналогично — в local/anonymous classes.

```java
class Counters {
    class Inner {
        static int count = 69;
        static { count++; }
        static int current() { return count; }
    }

    void inspect() {
        class Local {
            static int marker = 70;
            static { marker++; }
        }
        Runnable anonymous = new Runnable() {
            static int marker = 72;
            static { marker++; }
            @Override public void run() {
                System.out.println(marker);
            }
        };

        System.out.println(Inner.current()); // 70
        System.out.println(Local.marker);    // 71
        anonymous.run();                     // 73
    }
}
```

Здесь три различных **типа**, у каждого свой class initialization. Это не означает, что локальному классу можно написать модификатор `static`:

```java
class Bad {
    void run() {
        // static class Local {} // N18: запрещён modifier у local class
        class Local {
            static int marker = 67; // допустимо в Java 21
        }
    }
}
```

Разделяйте две независимые вещи: статический **член внутри типа** и объявление **самого локального типа**. См. [JLS §8.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.3), [§14.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.3) и [инициализацию классов](initialization.md).

## 4. Local class: имя действительно только в локальной области

Объявление `class Reader { ... }` внутри метода создаёт **локальный класс**. Его нельзя использовать по имени за пределами области видимости объявления. Он может обращаться к подходящим членам внешнего контекста и к локальным переменным/параметрам, если они **final или effectively final** и определённо инициализированы.

```java
class LocalExample {
    void print() {
        int base = 67;
        class Reader {
            int read() { return base + 1; }
        }
        System.out.println(new Reader().read()); // 68
    }
}
```

`base` явно не помечен `final`, однако после присвоения его значение не меняется. Такой local является **effectively final** по [JLS §4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4). Внимание: если написать `base = 68` после объявления `Reader`, ссылка внутри `Reader` станет недопустимой **на этапе компиляции**; Java не поддерживает захват такой изменяемой локальной ячейки.

```java
class Reassign {
    void test() {
        int value = 67;
        value = 68;
        class Reader {
            // int read() { return value; } // N03: не effectively final
        }
    }
}
```

Требование **definite assignment** отдельно: `int x; Supplier<Integer> s = () -> x; x = 67;` не компилируется, даже если вызвать `s.get()` позднее. Компилятор проверяет определённость присваивания в контексте лямбды, а не угадывает будущий порядок вызовов. См. [JLS §16](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html) и [разбор lambda capture](functions.md).

### Захват ссылки не замораживает объект

```java
import java.util.function.Supplier;

class CaptureReference {
    void print() {
        int[] values = {67};
        Supplier<Integer> get = () -> values[0];
        values[0] = 68;
        System.out.println(get.get()); // 68
    }
}
```

Переменная `values` не переназначалась — ссылка effectively final. Объект массива при этом **изменяем**. Это не доказательство потокобезопасности: при конкурентном доступе появляются дополнительные вопросы JMM, здесь не рассматриваемые. Сохранённая ссылка и содержимое объекта — разные уровни состояния. Аналогичное правило захвата применяется к local/anonymous class и lambda, хотя их `this` различается.

## 5. Local record — не обычный захватывающий local class

`record`, объявленный локально, **неявно static**. Он может использовать свои record-компоненты, переданные при создании, но не получает тот же механизм захвата локальной переменной метода.

```java
class LocalRecordExample {
    void print() {
        int x = 67;
        record Result(int value) {
            int plusOne() { return value + 1; }
            // int fromOuter() { return x; } // N13: захват недопустим
        }
        System.out.println(new Result(x).plusOne()); // 68
    }
}
```

Передача `x` в `new Result(x)` — обычное вычисление аргумента конструктора; это **не скрытый захват**. Такое явное владение данными полезно, когда локальной модели не нужна связь с контекстом. См. [JLS §14.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.3), [§8.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.3).

## 6. Anonymous class: объект плюс объявление без имени

`new Runnable() { ... }` — не lambda. Это анонимное объявление класса и создание его экземпляра, реализующего интерфейс. Для наследования абстрактного класса используется форма `new AbstractParent(...) { ... }`. У anonymous class **свой `this`**, он может объявлять дополнительные поля, переопределять методы и использовать правила захвата локальных значений.

```java
import java.util.function.Supplier;

class AnonymousThis {
    private int id = 67;

    void show() {
        Supplier<Boolean> anonymous = new Supplier<>() {
            @Override public Boolean get() {
                return (Object) this == AnonymousThis.this;
            }
        };
        Supplier<Boolean> lambda = () -> this == AnonymousThis.this;
        System.out.println(anonymous.get() + "," + lambda.get());
        // false,true
    }
}
```

Здесь анонимная реализация `get()` вызывается **на анонимном объекте**; её `this` не равен внешнему объекту. Lambda сохраняет лексическую привязку `this`, то есть не вводит собственный `this`. Подробно target type и выполнение lambda разобраны в [CORE-FUNCTIONS](functions.md).

Из `static`-контекста ни сам по себе anonymous class, ни lambda не создают `Outer.this`. Но внутри anonymous class его собственный `this` остаётся допустимым. Поэтому ошибка «lambda не имеет `this` вообще» также неверна: внутри instance-метода lambda использует **внешний** `this`.

Анонимный класс нужен, когда у реализации есть собственная идентичность/состояние или она должна наследовать класс. Lambda допустима только в контексте functional interface; нельзя механически заменить ею каждое `new SomeAbstractClass(){...}`. Это различие архитектурное и нормативное. См. [JLS §15.9.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9.5), [§15.27.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.2).

## 7. Capture и область видимости: типовые ошибки компиляции

Ниже **отдельные** отрицательные случаи. Каждую программу компилируют независимо: не складывайте повторяющиеся объявления в один файл и не трактуйте ошибку `duplicate class` как подтверждение другой нормы.

| Случай G1 | Что неверно | Почему |
|---|---|---|
| N01 | `new Inner()` из `static`-метода без внешнего объекта | Нет enclosing instance |
| N02 | Обращение к instance-полю внешнего класса из `static nested` без receiver | Нет неявного `Outer.this` |
| N03 | Local class читает переназначенный local | Local уже не effectively final |
| N04 | Anonymous class захватывает local, изменённый после объявления | То же требование capture |
| N09 | `Outer.this` из `static nested` | В данном контексте не существует охватывающего экземпляра |
| N13 | Local record пытается захватить local | Локальный record неявно static |
| N14 | `new Outer.Inner()` без экземпляра `Outer` | Нарушена форма создания non-static inner |
| N18 | `static class Local` в теле метода | Модификатор объявления local class запрещён |

Полные самодостаточные тексты N01–N20 находятся в [G1-плане §6](../../docs/plans/CORE-NESTED-FUNCTIONS.md). Это **отдельные 20 отрицательных компиляций**, не один некомпилируемый проект. Авторские локальные прогоны будут фиксироваться отдельно от исследовательских проверок G1 и CI.

## 8. Что отвечать на собеседовании и чего не выводить из примера

**Почему inner class создаётся через внешний объект?** Потому что экземпляр non-static member class связан с охватывающим экземпляром, который нужен для разрешения квалифицированного `Outer.this` и доступа к instance-контексту. Простой тип `Outer.Inner` сам по себе не предоставляет объект.

**Почему ссылка на local должна быть effectively final?** Чтобы компилятор мог обеспечить определённые правила захвата значения переменной без изменяемой локальной ячейки, чей срок жизни привязан к стековому вызову метода. Но отсюда не следует неизменность самого объекта, на который переменная указывает.

**Почему у anonymous есть собственный `this`, а у lambda нет?** Anonymous создаёт новый класс и объект; lambda — выражение с functional target и лексической областью имён. Спецификация не требует для lambda конкретной реализации отдельным классом, конкретных имён или одинакового числа аллокаций. См. [JLS §15.27.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.4).

**Как выбрать форму?** Если нужен тип-«помощник» без доступа к экземпляру — static nested. Если поведение действительно принадлежит конкретному внешнему объекту — inner. Если реализация имеет смысл только внутри одного метода и захватывает локальную настройку — local. Если нужен безымянный одноразовый класс с собственным `this`/полями либо subclass — anonymous. Для лаконичной функциональной операции с SAM рассмотрите [lambda](functions.md), помня, что она не эквивалентна anonymous class во всём.

## 9. Карта покрытия NF и границы

| NF-ID | Приоритет | Где объясняется |
|---|---|---|
| NF-01 | P0 | Static nested против inner и enclosing instance, §1–2 |
| NF-02 | P0 | Qualified `Outer.this`, §2 |
| NF-03 | P0 | Local и anonymous, §4/§6 |
| NF-04 | P0 | Static members Java 21 vs запрет `static class Local`, §3 |
| NF-05 | P0 | Capture и definite assignment, §4/§7 |
| NF-06 | P0 | Ссылка effectively final, изменяемый объект, §4 |
| NF-07 | P0 | Anonymous `this` vs lambda lexical `this`, §6 |
| NF-16 | P0 | Local record implicitly static, §5 |
| NF-22 | P1 | Enclosing state, время достижимости и ограничения synthetic field, §2 |
| NF-24 | P1 | Почему anonymous class не всегда заменяется lambda, §6 |
| NF-25 | P2 | `invokedynamic`/hidden classes/JVMS — отдельная тема JVM |
| NF-29 | P2 | Thread safety/JMM не следует из capture |
| NF-30 | P2 | Performance/allocations требует специальных измерений |

**Остальные NF-08–NF-15 P0, NF-17–NF-21/NF-23 P1 и NF-26–NF-28 P2** раскрыты в [лямбдах и method references](functions.md) либо отмечены там границей. Матрица исследования полная: NF-01–NF-30, 16 P0 / 8 P1 / 6 P2. Статья не является реализацией Java Streams, serialization или JMM.

## 10. Источники и воспроизводимость

Нормативные источники — [JLS 21 §8.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.3), [§8.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.5), [§14.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.3), [§15.9.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9.2), [§15.9.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9.5), [§4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4), [§16](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html). Примеры вопросов: [реестр источников](../../docs/SOURCE_REGISTRY.md), но подборки интервью не устанавливают гарантии языка.

В [плане §5](../../docs/plans/CORE-NESTED-FUNCTIONS.md) описана самостоятельная Java-программа `NestedFunctionsProbes` (одна компиляция/один запуск; 17 строк stdout), а в §6 — 20 отдельных компиляционно отрицательных примеров. Это доказательства **G1**, а не автоматически выполненные тесты данной статьи. Новые проверки авторского G2, CI и `not run` фиксируются в PR отдельно. Визуальный просмотр ссылок Obsidian/IntelliJ, альтернативные JDK и параллельные сценарии не подразумеваются.
