# CORE-INIT — Порядок инициализации в Java 21

**Версия:** Java SE 21. **Исследовательская спецификация:** принятый 00 план `docs/plans/CORE-INIT.md` из PR #50, HEAD `0614e5722f5f89cbaa4425fc707f1a3aa192368d`. **Задача:** [Issue #51](https://github.com/Melchiorius/backend-interview-labs/issues/51).

## Главная идея

Между `new Child()` и возвратом ссылки на готовый объект Java выполняет несколько разных процессов. Их нельзя называть одним словом «загрузка» или «инициализация»:

1. **Loading (загрузка)** — JVM получает бинарное представление класса и создаёт runtime-представление типа.
2. **Linking (связывание)** — проверка бинарного представления, подготовка статических полей, разрешение символических ссылок. Разрешение может быть отложенным.
3. **Class initialization (инициализация класса)** — исполнение инициализаторов `static`-полей и `static`-блоков класса; у интерфейса — инициализаторов его полей.
4. **Создание экземпляра** — выделение памяти под объект и присвоение **всем** его полям, включая поля подкласса, значений по умолчанию.
5. **Instance initialization и constructor body** — цепочка вызовов конструкторов от суперкласса к подклассу; после возврата конструктора суперкласса выполняются инициализаторы текущего класса в текстовом порядке, затем оставшаяся часть его конструктора.

Инициализация класса отвечает за статическое состояние типа. Создание экземпляра — за состояние конкретного объекта. Важнейшее следствие: конструктор родителя может вызвать переопределённый метод подкласса, когда поля подкласса ещё содержат default values.

Нормативная основа — [JLS 21, глава 12: Execution](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html), прежде всего §§12.2–12.5. В статье используются только правила Java SE 21.

## Центральный пример: статическая инициализация и создание объекта

Пример соединяет class initialization, интерфейсы с `default`-методами, compile-time constant, default values, `this(...)`, неявный `super()` и динамическую диспетчеризацию в конструкторе родителя.

Сохраните листинг в `InitializationOrderDemo.java`:

```java
final class Trace {
    static int value(String label, int value) {
        System.out.println(label);
        return value;
    }

    static void event(String label) {
        System.out.println(label);
    }
}

interface RootFeature {
    int INITIALIZED = Trace.value("static: RootFeature", 1);
    default void rootFeature() { }
}

interface MiddleFeature extends RootFeature {
    int INITIALIZED = Trace.value("static: MiddleFeature", 1);
    default void middleFeature() { }
}

interface NoDefaultFeature {
    int INITIALIZED = Trace.value("static: NoDefaultFeature", 1);
}

class Parent {
    static int state = Trace.value("static: Parent field", 1);
    static {
        Trace.event("static: Parent block");
    }

    int parentValue = Trace.value("instance: Parent field", 10);
    {
        Trace.event("instance: Parent block");
    }

    Parent() {
        Trace.event("constructor: Parent body");
        describe();
    }

    void describe() {
        Trace.event("Parent.describe");
    }
}

class Child extends Parent implements MiddleFeature, NoDefaultFeature {
    static final int CONSTANT = 67;
    static final Integer BOXED = Trace.value("static: Child boxed field", 68);
    static {
        Trace.event("static: Child block");
    }

    int value = Trace.value("instance: Child field", 68);
    {
        Trace.event("instance: Child block");
    }

    Child() {
        this(67);
        Trace.event("constructor: Child() remainder");
    }

    Child(int argument) {
        Trace.event("constructor: Child(int) body, argument=" + argument);
    }

    @Override
    void describe() {
        Trace.event("override: Child.describe sees value=" + value);
    }
}

public class InitializationOrderDemo {
    public static void main(String[] args) {
        System.out.println("constant=" + Child.CONSTANT);
        System.out.println("before boxed");
        System.out.println("boxed=" + Child.BOXED);
        System.out.println("before new Child");

        Child child = new Child();
        System.out.println("final state=" + child.parentValue + "/" + child.value);
        System.out.println("direct interface field=" + NoDefaultFeature.INITIALIZED);
    }
}
```

Запуск на JDK 21:

```sh
javac --release 21 InitializationOrderDemo.java
java InitializationOrderDemo
```

Полный ожидаемый вывод:

```text
constant=67
before boxed
static: Parent field
static: Parent block
static: RootFeature
static: MiddleFeature
static: Child boxed field
static: Child block
boxed=68
before new Child
instance: Parent field
instance: Parent block
constructor: Parent body
override: Child.describe sees value=0
instance: Child field
instance: Child block
constructor: Child(int) body, argument=67
constructor: Child() remainder
final state=10/68
static: NoDefaultFeature
direct interface field=1
```

### Пошаговый разбор

**1. `Child.CONSTANT` не запускает инициализацию `Child`.** Поле имеет тип `int`, объявлено `static final` и инициализировано constant expression `67`. Это *constant variable* по [JLS 21 §4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4). Использование такой переменной не является active use; компилятор может встроить её значение в клиентский код. Поэтому строка `constant=67` появляется до любых static-маркеров.

**2. `Child.BOXED` запускает class initialization.** Тип `Integer` — ссылочный, а не примитивный или `String`; поле не является constant variable. Чтение неконстантного static-поля, объявленного в `Child`, — active use, поэтому Java должна инициализировать класс до чтения.

**3. Сначала инициализируется `Parent`.** Инициализация класса начинается с его суперкласса. Внутри `Parent` сначала выполняется инициализатор поля `state`, затем static block. Это единая последовательность в порядке исходного текста.

**4. Затем идут superinterfaces, объявляющие default methods.** `Child implements MiddleFeature, NoDefaultFeature`, а `MiddleFeature extends RootFeature`. У `RootFeature` и `MiddleFeature` есть default methods, поэтому при инициализации `Child` Java обходит эту ветвь в postorder: сначала `RootFeature`, затем `MiddleFeature`. `NoDefaultFeature` не объявляет default method, поэтому не инициализируется только из-за `implements`.

**5. После суперкласса и нужных интерфейсов выполняются static initializers `Child`.** Сначала вычисляется `BOXED`, затем static block — в текстовом порядке. Constant variable `CONSTANT` имеет отдельный ранний шаг подробной процедуры: её значение присваивается до инициализации суперкласса. Этот шаг не печатает строку и потому не виден в stdout.

**6. Перед конструированием все поля объекта уже имеют default values.** Когда выполняется `new Child()`, class initialization завершена. Память выделяется под один объект с полями `Parent.parentValue` и `Child.value`. Сначала оба равны `0`; выражения инициализаторов ещё не выполнялись.

**7. `Child()` делегирует через `this(67)`.** Его первый оператор вызывает другой конструктор того же класса — `Child(int)`. В нём нет явного вызова `this(...)` или `super(...)`, поэтому Java 21 применяет неявный `super()`. Цепочка доходит до `Object`; у `Object` нет суперкласса.

**8. После возврата конструктора `Object` выполняются instance initializers `Parent`.** Сначала `parentValue` получает `10`, затем выполняется instance block. Только после этого начинается тело `Parent()`.

**9. `Parent()` вызывает `describe()`, но исполняется override из `Child`.** Динамическая диспетчеризация действует и во время construction. Фактический объект — `Child`, поэтому вызывается `Child.describe()`. Но инициализатор `Child.value = 68` ещё не выполнялся: поле всё ещё равно `0`. В этот момент поле родителя уже равно `10`, а поле подкласса ещё равно `0`.

**10. После возврата `Parent()` выполняются инициализаторы `Child`.** Поле `value` получает `68`, затем выполняется instance block. После этого выполняется тело `Child(int)`.

**11. Затем продолжается тело `Child()`.** Делегированный конструктор вернулся, поэтому выполняется оставшаяся часть исходного конструктора. Инициализаторы `Child` второй раз не запускаются.

**12. Теперь объект имеет итоговое состояние `10/68`.** Только после завершения всей цепочки `new Child()` возвращает ссылку вызывающему коду.

**13. `NoDefaultFeature` инициализируется при прямом чтении своего поля.** Последняя строка обращается к неконстантному static-полю интерфейса. Это самостоятельное active use, поэтому теперь появляется `static: NoDefaultFeature`. Тем самым пример показывает: `implements` само по себе не запускает интерфейс без default method, но чтение его собственного неконстантного static-поля запускает.

## Loading, linking и class initialization — разные этапы

**Loading** — JVM получает бинарное представление типа и создаёт runtime-представление. Загрузка сама по себе не запускает static initializers. См. [JLS 21 §12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.2) и [§12.2.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.2.1).

**Linking** включает verification, preparation и resolution:

- **Verification** — проверка корректности бинарного представления.
- **Preparation** — создание static-полей и присвоение им default values; исходный код инициализаторов ещё не исполняется.
- **Resolution** — разрешение символических ссылок на типы, поля и методы. Оно может выполняться по мере необходимости, а не обязательно целиком до первого active use.

Нормативные ссылки: [JLS 21 §12.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.3), [§12.3.2 — Preparation](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.3.2), [§12.3.3 — Resolution](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.3.3).

**Class initialization** — выполнение инициализаторов static-полей и static-блоков класса; у интерфейса — инициализаторов его полей. В терминах JVMS код этого процесса связан с initialization method `<clinit>`, но это не метод, который программист объявляет в исходном коде. См. [JLS 21 §12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4) и [JVMS 21 §5.5](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.5).

**Instance initialization** — выполнение field initializers и instance initializer blocks конкретного объекта в процедуре его создания. **Constructor body** — оставшаяся часть конструктора после необходимых вызовов `this(...)`/`super(...)` и инициализаторов текущего класса. Полная последовательность задана [JLS 21 §12.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.5).

При запуске приложения initial class инициализируется до вызова `main`: [JLS 21 §12.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.1.3).

## Когда класс инициализируется

[JLS 21 §12.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.1) требует инициализировать класс непосредственно перед первым из соответствующих активных использований:

- создание экземпляра `new C(...)`;
- вызов static-метода, объявленного в `C`;
- присваивание static-полю, объявленному в `C`;
- чтение static-поля, объявленного в `C`, если оно не является constant variable;
- некоторые reflective operations.

При инициализации класса также инициализируются его суперклассы и подходящие superinterfaces. Подробный порядок приведён ниже.

### Что само по себе не запускает инициализацию

- объявление переменной типа `C` или получение `C.class`;
- создание массива ссылок `new C[10]` — создаётся массив, а не экземпляр `C`;
- чтение constant variable;
- вызов `ClassLoader.loadClass(name)`;
- вызов `Class.forName(name, false, loader)`.

Класс при этом может быть загружен; важно не смешивать loading и initialization.

Для контраста, `Class.forName(name)` инициирует инициализацию. Вариант с флагом позволяет отделить загрузку от инициализации:

```java
Class<?> first = Class.forName("example.SomeType", false, loader);
// Класс загружен, но его инициализаторы не запускаются этим вызовом.

Class<?> second = Class.forName("example.SomeType", true, loader);
// Инициализация выполняется, если она ещё не происходила.
```

Это контракт Java SE 21 API: [Class.forName(String)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#forName(java.lang.String)), [Class.forName(String, boolean, ClassLoader)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#forName(java.lang.String,boolean,java.lang.ClassLoader)) и [ClassLoader.loadClass(String)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html#loadClass(java.lang.String)).

### Инициализируется тип, который объявил static-член

```java
class Base {
    static int VALUE = initialize();
    static int initialize() {
        System.out.println("Base initialized");
        return 67;
    }
}

class Sub extends Base {
    static {
        System.out.println("Sub initialized");
    }
}

class Trigger {
    public static void main(String[] args) {
        System.out.println(Sub.VALUE);
    }
}
```

Вывод:

```text
Base initialized
67
```

Поле `VALUE` объявлено в `Base`. Выражение `Sub.VALUE` использует имя подкласса как квалификатор, но разрешённое поле принадлежит `Base`; поэтому инициализируется `Base`, а не `Sub`. Квалификатор выражения сам по себе не определяет инициализируемый тип. Это показано в [JLS 21 §12.4.1, Example 12.4.1-2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.1).

## Constant variables: почему `static final` недостаточно

*Constant variable* — переменная примитивного типа или `String`, объявленная `final` и инициализированная constant expression ([JLS 21 §4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4)).

| Объявление | Constant variable? | Чтение само по себе запускает class initialization? |
|---|---:|---:|
| `static final int LIMIT = 67;` | Да | Нет |
| `static final String NAME = "Java";` | Да | Нет |
| `static final Integer LIMIT = 67;` | Нет | Да, если поле объявлено в этом классе |
| `static final String NAME = new String("Java");` | Нет | Да, если поле объявлено в этом классе |
| `static final int LIMIT = calculate();` | Нет | Да, если поле объявлено в этом классе |

Критерий — не наличие `static final`, а тип переменной и constant expression. `Integer` — ссылочный тип, поэтому даже значение `67` после boxing не превращает поле в constant variable.

В подробной процедуре [JLS 21 §12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2) есть тонкость: при инициализации класса его constant variables получают значения на отдельном раннем шаге **до инициализации суперкласса**. Обычные static field initializers выполняются позже — после суперкласса и нужных интерфейсов. В центральном примере `Child.CONSTANT` — constant variable, а `Child.BOXED` — нет.

Значение constant variable может быть встроено компилятором в клиентский класс. Если библиотека изменит `public static final int LIMIT = 67` на `68`, старый клиентский `.class` может продолжить использовать `67`, пока его не перекомпилируют. Это тонкость бинарной совместимости, а не механизм обновления статического состояния во время выполнения ([JLS 21 §§13.1, 13.4.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html)).

## Порядок class initialization

### Суперкласс, затем superinterfaces с default methods, затем класс

При инициализации класса `C` подробная процедура выполняет следующие действия:

1. Отмечает, что инициализация `C` начата текущим потоком.
2. Присваивает значения его constant variables.
3. Инициализирует суперкласс `C`, если он ещё не инициализирован.
4. Инициализирует superinterfaces `C`, объявляющие хотя бы один `default`-метод, в порядке специального рекурсивного обхода.
5. Выполняет class variable initializers и static blocks самого `C` в текстовом порядке.

Для обычного класса сначала идёт цепочка классов-суперклассов, затем нужные интерфейсы, затем собственные non-constant static initializers класса.

### Почему интерфейсы обходятся в postorder

Для каждого непосредственно реализованного интерфейса Java рекурсивно рассматривает его superinterfaces слева направо, прежде чем вернуться к самому интерфейсу. В инициализацию класса включаются только интерфейсы этого обхода, объявляющие хотя бы один `default`-метод.

В центральном примере `Child implements MiddleFeature, NoDefaultFeature`, а `MiddleFeature extends RootFeature`. Поэтому порядок: `Parent` → `RootFeature` → `MiddleFeature` → `Child`. `NoDefaultFeature` не входит в этот список.

Компилятор может создать *synthetic default methods*. Такие методы тоже могут повлиять на то, считается ли интерфейс объявляющим default method для целей инициализации, хотя этого не видно в исходном коде. Это оговорка спецификации, а не основание строить повседневный код на предположениях о синтетических методах. См. [JLS 21 §12.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.1) и [§12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2).

### Прямая инициализация интерфейса не инициализирует его предков

```java
interface ParentI {
    int P = log("ParentI");
}

interface ChildI extends ParentI {
    int C = log("ChildI");

    static int log(String name) {
        System.out.println(name);
        return 1;
    }
}

public class InterfaceInit {
    public static void main(String[] args) {
        System.out.println(ChildI.C);
    }
}
```

Будет напечатано `ChildI`, затем `1`; `ParentI` не инициализируется. Это контрпример к фразе «инициализация интерфейса всегда сначала инициализирует его родителей». Исключение для superinterfaces с default methods относится к инициализации **класса**, а не к прямой инициализации интерфейса.

### Static fields и blocks — одна последовательность

```java
class Ordered {
    static int first = log("first field");
    static { log("first block"); }
    static int second = log("second field");
    static { log("second block"); }

    static int log(String text) {
        System.out.println(text);
        return 67;
    }
}
```

Порядок: `first field` → `first block` → `second field` → `second block`. Java не сортирует отдельно все поля и все блоки.

Для простых имён действуют ограничения на *forward references* ([JLS 21 §8.3.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.3)):

```java
class BadForwardReference {
    static int first = later; // compile-time error: illegal forward reference
    static int later = 67;
}
```

Квалифицированное обращение — другой случай:

```java
class QualifiedForwardReference {
    static int first = QualifiedForwardReference.later;
    static int later = 67;
}
```

Этот вариант компилируется, но `first` получает `0`: при выполнении первого инициализатора `later` ещё содержит default value. Это показывает, почему наличие default value не отменяет правил текстового порядка и forward references.

## Значения полей по умолчанию

[JLS 21 §4.12.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.5) определяет начальные значения полей и элементов массивов.

| Тип | Значение по умолчанию |
|---|---|
| `byte`, `short`, `int`, `long` | числовой ноль |
| `float`, `double` | положительный ноль |
| `char` | `'\u0000'` |
| `boolean` | `false` |
| Любой ссылочный тип | `null` |

Правило относится к полям и компонентам массивов. **Локальные переменные не получают автоматического значения по умолчанию**: до чтения локальная переменная должна быть определённо присвоена.

Для объекта важно: все поля всего нового объекта — включая унаследованные и поля самого нижнего подкласса — получают default values до запуска конструкторов и instance initializers. Это прямо указано в [JLS 21 §12.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.5).

## Instance initialization и цепочка конструкторов

Для `new Child(...)` после необходимой class initialization Java выполняет следующую последовательность:

1. Выделяет память под объект со всеми instance fields всех классов его иерархии.
2. Присваивает каждому полю default value.
3. Вызывает выбранный конструктор.
4. Если конструктор начинает с `this(...)`, рекурсивно выполняет другой конструктор того же класса. Инициализаторы текущего класса на этом шаге ещё не выполняются.
5. Конструктор, который не делегирует через `this(...)`, вызывает конструктор прямого суперкласса — явно через `super(...)` или неявно через `super()`.
6. Когда конструктор суперкласса и его цепочка завершились, выполняются instance field initializers и instance initializer blocks текущего класса в текстовом порядке.
7. Выполняется оставшаяся часть тела текущего конструктора.
8. Если текущий конструктор был вызван через `this(...)`, после его завершения продолжается оставшаяся часть тела делегирующего конструктора.

Так повторяется для каждого класса от базового к производному.

### `this(...)` выполняет инициализаторы класса один раз

```java
class Example {
    int value = log("field initializer");

    Example() {
        this(67);
        log("Example() remainder");
    }

    Example(int value) {
        log("Example(int) body");
    }

    static int log(String text) {
        System.out.println(text);
        return 67;
    }
}
```

При `new Example()` сначала вызывается `Example(int)`; после возврата конструктора `Object` выполняется инициализатор `value`. Затем выполняется тело `Example(int)`. После возврата делегированного конструктора исполняется остаток `Example()`. Инициализатор поля появляется в выводе один раз.

### Неявный `super()` и Java 21

В Java 21, если конструктор не начинается с явного вызова `this(...)` или `super(...)` и это не конструктор `Object`, он неявно начинает работу с `super()` — вызова no-args конструктора прямого суперкласса.

Если у суперкласса нет доступного конструктора без аргументов, подкласс не скомпилируется:

```java
class Parent {
    Parent(int value) { }
}

class Child extends Parent {
    Child() { } // compile-time error: implicit super() is not applicable
}
```

Исправление — явно выбрать существующий конструктор родителя, например `Child() { super(67); }`.

Не путайте *default constructor* и *no-args constructor*. Default constructor компилятор объявляет только тогда, когда класс вообще не объявил ни одного конструктора. Ручной конструктор без параметров — обычный явно написанный конструктор. Подробное введение уже дано в [CORE-CLASSES](classes.md); здесь важна иерархическая последовательность.

В Java 21 явный вызов `this(...)` или `super(...)` должен быть первым оператором тела конструктора. Следующий код не компилируется:

```java
class Child {
    Child() {
        System.out.println("before super");
        super(); // compile-time error in Java 21
    }
}
```

Не переносите в Java 21 более поздний синтаксис гибких тел конструкторов или preview-возможности других версий. См. [JLS 21 §8.8.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7) и [§8.8.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7.1).

Циклическая цепочка `this(...)` — ошибка компиляции:

```java
class Cyclic {
    Cyclic() { this(67); }
    Cyclic(int value) { this(); } // compile-time error: recursive constructor invocation
}
```

### Instance field initializers и blocks идут в текстовом порядке

Для каждого класса отдельно, после возврата конструктора суперкласса, Java выполняет field initializers и instance initializer blocks в порядке их расположения в исходном тексте.

```java
class OrderedInstance {
    int first = log("first field");
    { log("first block"); }
    int second = log("second field");
    { log("second block"); }

    int log(String text) {
        System.out.println(text);
        return 67;
    }
}
```

Порядок: `first field` → `first block` → `second field` → `second block`. Тело конструктора выполняется после них. Нормативная опора — [JLS 21 §8.3.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.2), [§8.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.6) и [§12.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.5).

