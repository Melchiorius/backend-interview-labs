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

## Центральный пример: class initialization и создание экземпляра

Роль 03 подготовила один автономный файл [InitializationDemo.java](../../examples/core/initialization/src/InitializationDemo.java), README с командами и разбором, а также сохранённый [expected.stdout](../../examples/core/initialization/expected.stdout). В статье ниже приведён тот же исходник, чтобы теория и runnable-пример не расходились. Автоматическая проверка точного stdout описана в [README примера](../../examples/core/initialization/README.md).

Сохраните исходник в файл `InitializationDemo.java`:

```java
/**
 * CORE-INIT: one Java SE 21 run through class and instance initialization.
 * Theory: knowledge/01-java-core/initialization.md (class/instance initialization),
 * knowledge/01-java-core/classes.md (fields/constructors) and
 * knowledge/01-java-core/inheritance.md (overriding/dynamic dispatch).
 */
public final class InitializationDemo {
    private InitializationDemo() {
    }

    private static void log(String event) {
        System.out.println(event);
    }

    private static int markInt(String event, int value) {
        log(event);
        return value;
    }

    private static Integer markInteger(String event, int value) {
        log(event);
        return value;
    }

    private static String markString(String event, String value) {
        log(event);
        return value;
    }

    interface RootDefault {
        int MARK = InitializationDemo.markInt("RootDefault.<clinit>", 1);

        default void rootDefault() {
        }
    }

    interface LeafDefault extends RootDefault {
        int MARK = InitializationDemo.markInt("LeafDefault.<clinit>", 1);

        default void leafDefault() {
        }
    }

    interface QuietParent {
        int MARK = InitializationDemo.markInt("QuietParent.<clinit>", 1);
    }

    interface QuietChild extends QuietParent {
        int MARK = InitializationDemo.markInt("QuietChild.<clinit>", 1);
    }

    static class Parent {
        static final int CONSTANT = 67;
        static int staticValue = markInt("Parent.static field", 67);
        static final Integer BOXED = markInteger("Parent.BOXED field", 67);

        static {
            log("Parent.static block");
        }

        int parentValue = markInt("Parent.instance field", 67);

        {
            log("Parent.instance block");
        }

        Parent() {
            log("Parent.constructor: before virtual call");
            observe();
            log("Parent.constructor: after virtual call");
        }

        void observe() {
        }
    }

    static final class Child extends Parent implements LeafDefault, QuietChild {
        static int OWN = markInt("Child.static field", 68);

        static {
            log("Child.static block");
        }

        int childValue = markInt("Child.instance field", 68);
        String childLabel = markString("Child.instance reference field", "ready");

        {
            log("Child.instance block");
        }

        Child() {
            this(67);
            log("Child() constructor body");
        }

        Child(int seed) {
            super();
            log("Child(int) constructor body: seed=" + seed);
        }

        @Override
        void observe() {
            log("Child.override: childValue=" + childValue + ", childLabel=" + childLabel);
        }
    }

    public static void main(String[] args) {
        log("== 1. Compile-time constant ==");
        log("Child.CONSTANT = " + Child.CONSTANT);

        log("== 2. Inherited non-constant static field ==");
        log("Child.BOXED = " + Child.BOXED);

        log("== 3. Child class initialization ==");
        log("Child.OWN = " + Child.OWN);

        log("== 3a. Direct interface initialization ==");
        log("QuietChild.MARK = " + QuietChild.MARK);

        log("== 4. Instance construction ==");
        Child child = new Child();

        log("== 5. Final object state ==");
        log("state=" + child.parentValue + "/" + child.childValue + "/" + child.childLabel);
    }
}
```

Команды из корня репозитория (JDK 21):

```sh
javac --release 21 -d examples/core/initialization/build/classes examples/core/initialization/src/InitializationDemo.java
java -cp examples/core/initialization/build/classes InitializationDemo
```

Для полной проверки, включая точное сравнение с сохранённым stdout:

```sh
bash examples/core/initialization/verify.sh
```

Полный ожидаемый вывод:

```text
== 1. Compile-time constant ==
Child.CONSTANT = 67
== 2. Inherited non-constant static field ==
Parent.static field
Parent.BOXED field
Parent.static block
Child.BOXED = 67
== 3. Child class initialization ==
RootDefault.<clinit>
LeafDefault.<clinit>
Child.static field
Child.static block
Child.OWN = 68
== 3a. Direct interface initialization ==
QuietChild.<clinit>
QuietChild.MARK = 1
== 4. Instance construction ==
Parent.instance field
Parent.instance block
Parent.constructor: before virtual call
Child.override: childValue=0, childLabel=null
Parent.constructor: after virtual call
Child.instance field
Child.instance reference field
Child.instance block
Child(int) constructor body: seed=67
Child() constructor body
== 5. Final object state ==
state=67/68/ready
```

### Пошаговый разбор центрального примера

**1. `Child.CONSTANT` читается без инициализации ни `Parent`, ни `Child`.**

Поле `CONSTANT` объявлено в `Parent` как `static final int` и инициализировано constant expression `67`. `Child` наследует это поле, поэтому обращение `Child.CONSTANT` допустимо. Это constant variable по [JLS 21 §4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4); значение может быть встроено в клиентский код. Поэтому сначала появляется только `Child.CONSTANT = 67` — ни один инициализатор родителя или подкласса ещё не запускался.

**2. `Child.BOXED` запускает именно `Parent`.**

Поле `BOXED` также объявлено в `Parent`, но имеет тип `Integer` и не является constant variable. Имя `Child` здесь лишь квалификатор унаследованного поля; фактический declaring type — `Parent`. Поэтому перед чтением инициализируется `Parent`, но не `Child`. Внутри `Parent` в текстовом порядке исполняются `staticValue`, `BOXED` и static block. Именно поэтому видны три строки с префиксом `Parent`, а строки `Child.static...` пока нет.

**3. `Child.OWN` запускает инициализацию самого `Child`.**

Теперь используется static-поле, объявленное непосредственно в `Child`. Суперкласс `Parent` уже инициализирован на предыдущем шаге. При инициализации класса Java рассматривает superinterfaces, объявляющие default methods: рекурсивный обход выполняется в postorder, поэтому сначала печатается `RootDefault.<clinit>`, затем `LeafDefault.<clinit>`. После интерфейсов исполняются собственные static field initializer и static block `Child`.

`QuietParent` и `QuietChild` не объявляют default methods. Сам факт `implements QuietChild` не запускает их инициализацию, поэтому их маркеров здесь нет.

**4. Прямое чтение `QuietChild.MARK` инициализирует `QuietChild`, но не `QuietParent`.**

Это отдельное active use поля интерфейса. Инициализация интерфейса сама по себе не инициирует его superinterfaces. Поэтому появляется `QuietChild.<clinit>`, но не `QuietParent.<clinit>`.

**5. Перед конструкторами все instance fields уже имеют default values.**

К моменту `new Child()` инициализация класса завершена. Память выделяется под объект, содержащий поля и `Parent`, и `Child`. До запуска конструкторов `childValue == 0`, а `childLabel == null`. Поле `parentValue` тоже сначала получает `0`; позднее его initializer присвоит `67`.

**6. `Child()` делегирует через `this(67)`, а `Child(int)` вызывает `super()`.**

Первый оператор `Child()` вызывает другой конструктор того же класса. Конструктор `Child(int)` явно вызывает конструктор суперкласса `Parent`. Затем цепочка доходит до `Object`, у которого нет суперкласса. Инициализаторы текущего класса не запускаются повторно из-за `this(...)`.

**7. После возврата `Object` выполняются инициализаторы экземпляра `Parent`.**

Сначала выполняется initializer поля `parentValue`, затем instance initializer block. После этого начинается тело конструктора `Parent()`.

**8. Конструктор `Parent()` вызывает виртуальный метод, и срабатывает override `Child.observe()`.**

Динамическая диспетчеризация не отключается во время создания объекта. Фактический объект — `Child`, поэтому вызывается его реализация. Но инициализаторы `Child` ещё не выполнялись: override видит `childValue=0` и `childLabel=null`. Это обычный виртуальный вызов на частично сконструированном объекте, а не специальный режим конструкторов.

**9. После возврата `Parent()` выполняются инициализаторы `Child`.**

Поле `childValue` получает `68`, затем `childLabel` получает `"ready"`, после чего выполняется instance initializer block `Child`. Только затем продолжается тело `Child(int)`.

**10. После возврата `Child(int)` продолжается тело `Child()`.**

Выполняется остаток делегирующего конструктора. Все инициализаторы полей и блоки `Child` уже выполнены ровно один раз.

**11. Готовый объект имеет состояние `67/68/ready`.**

Суперклассовое поле `parentValue` равно `67`, поле `childValue` — `68`, ссылочное поле `childLabel` — `"ready"`. Теперь создание завершено и ссылка возвращается вызывающему коду.

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

Для контраста, `Class.forName(name)` инициирует инициализацию. Вариант с флагом позволяет отделить загрузку от инициализации. Ниже фрагмент: предполагается, что переменная `loader` уже содержит нужный `ClassLoader`; обработка `ClassNotFoundException` опущена.

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
final class InitLog {
    static int log(String name) {
        System.out.println(name);
        return 1;
    }
}

interface ParentI {
    int P = InitLog.log("ParentI");
}

interface ChildI extends ParentI {
    int C = InitLog.log("ChildI");
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
public class QualifiedForwardReference {
    static int first = QualifiedForwardReference.later;
    static int later = 67;

    public static void main(String[] args) {
        System.out.println(first);
    }
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

## Опасность виртуального вызова из конструктора

Java не отключает динамическую диспетчеризацию при создании объекта. Если конструктор суперкласса вызывает переопределяемый экземплярный метод, JVM выбирает реализацию по фактическому классу объекта.

В этот момент:

- все поля объекта уже имеют default values;
- инициализаторы и initializer blocks суперкласса уже выполнились;
- выполняется тело конструктора суперкласса;
- instance initializers и тело конструктора подкласса **ещё не выполнялись**.

В центральном примере `Parent()` вызывает `describe()`. Фактический объект — `Child`, поэтому запускается `Child.describe()`. `Parent.parentValue` уже равно `10`, но `Child.value` всё ещё равно `0`. После возврата `Parent()` инициализатор подкласса присвоит полю `68`.

Это обычная динамическая диспетчеризация, применённая к объекту, который ещё создаётся. Общее правило overriding разобрано в [CORE-INHERITANCE](inheritance.md); здесь важен момент времени и доступное состояние объекта. См. [JLS 21 §12.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.5).

Практическое правило: **не вызывайте из конструктора методы, которые подкласс может переопределить**, если контракт явно не рассчитан на частично сконструированный объект. Вынесите такую работу в метод, вызываемый после создания объекта, либо используйте `private`/`final` helper там, где это соответствует дизайну. Само объявление helper как `final` не исправляет любой дефект порядка: метод всё ещё может читать неустановленное состояние.

### `this` escape во время construction

Риск усиливается, если override публикует ссылку `this` наружу:

```java
class Parent {
    Parent() {
        register();
    }
    void register() { }
}

class Child extends Parent {
    int value = 67;
    static volatile Child escaped;

    @Override
    void register() {
        escaped = this;
    }
}
```

При `new Child()` вызов `register()` из `Parent()` попадёт в `Child.register()`. В момент записи в `escaped` поле `Child.value` ещё содержит `0`; инициализатор `= 67` выполнится позже.

`volatile` здесь не «достраивает» объект. Запись ссылки в volatile-поле происходит **до** присваивания `Child.value = 67` и не публикует эту будущую запись как уже завершённую. Другой поток может получить ссылку на частично сконструированный объект. Точный результат конкурентного чтения зависит от синхронизации и момента чтения; нельзя обещать один детерминированный вывод.

Same-thread dispatch из конструктора и межпоточный `this` escape — разные вопросы. Первый сам по себе не является data race. Второй создаёт возможность наблюдать объект до завершения construction и затрагивает Java Memory Model; полная модель безопасной публикации относится к CON-JMM.

## Ошибка инициализации класса

Если class initialization завершается ошибкой, JVM запоминает это состояние. При следующем обращении класс не запускает static initializers повторно.

```java
class Broken {
    static {
        fail();
    }

    static void fail() {
        throw new IllegalStateException("boom");
    }

    static int VALUE = 67;
}

public class InitFailureDemo {
    public static void main(String[] args) {
        try {
            System.out.println(Broken.VALUE);
        } catch (ExceptionInInitializerError error) {
            System.out.println("first: "
                    + error.getClass().getSimpleName()
                    + ", cause="
                    + error.getCause().getClass().getSimpleName());
        }

        try {
            System.out.println(Broken.VALUE);
        } catch (NoClassDefFoundError error) {
            System.out.println("second: "
                    + error.getClass().getSimpleName());
        }
    }
}
```

Ожидаемый результат:

```text
first: ExceptionInInitializerError, cause=IllegalStateException
second: NoClassDefFoundError
```

Первое чтение `Broken.VALUE` — active use. Static block вызывает `fail()`, который выбрасывает `IllegalStateException`. Это исключение не является `Error`; JVM заменяет его на `ExceptionInInitializerError` с исходным исключением в качестве причины. Инициализация `Broken` завершается неуспешно, и тип помечается как *erroneous*.

При следующем active use JVM не выполняет static block повторно. Инициализация невозможна, поэтому возникает `NoClassDefFoundError`.

Если из инициализатора выброшен объект класса `Error` или его подкласса, он передаётся без обязательной упаковки в `ExceptionInInitializerError`. Например, `AssertionError` остаётся `AssertionError` при первом неуспешном active use; последующее использование всё равно встретит erroneous state и приведёт к `NoClassDefFoundError`.

Если инициализация суперкласса или требуемого superinterface завершилась ошибкой, инициализируемый класс также помечается erroneous. Сообщение `NoClassDefFoundError` и текст stack trace не являются надёжным способом определить первопричину: ищите исходную ошибку первой попытки.

Это правило относится к class initialization. Оно не означает, что любое исключение из любого метода навсегда «ломает класс»: если класс уже успешно инициализирован, исключение из обычного static-метода не меняет его состояние инициализации.

Нормативный алгоритм — [JLS 21 §12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2). API: [ExceptionInInitializerError](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ExceptionInInitializerError.html) и [NoClassDefFoundError](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/NoClassDefFoundError.html).

## Исключение при создании экземпляра — другая ситуация

Исключение из instance initializer или конструктора прерывает создание **конкретного объекта**, но не переводит класс в erroneous state. Если class initialization прошла успешно, следующая попытка `new` может пройти нормально.

```java
class Retryable {
    static int attempts;

    {
        if (++attempts == 1) {
            throw new IllegalStateException("first attempt");
        }
    }
}

public class InstanceFailureDemo {
    public static void main(String[] args) {
        try {
            new Retryable();
        } catch (IllegalStateException error) {
            System.out.println("first: " + error.getMessage());
        }

        new Retryable();
        System.out.println("second: success");
    }
}
```

Вывод:

```text
first: first attempt
second: success
```

При первом `new` инициализация самого класса `Retryable` проходит успешно, затем instance initializer бросает исключение. Объект не возвращается вызывающему коду, но класс остаётся инициализированным. При втором `new` блок выполняется снова; счётчик уже равен двум, поэтому создание завершается успешно.

| Где возникло исключение | Что не удалось | Что будет при следующем использовании |
|---|---|---|
| `static` field initializer / `static` block | Инициализация класса | Класс erroneous; active use приводит к `NoClassDefFoundError` |
| Instance field initializer / instance block | Создание одного объекта | Класс не становится erroneous только из-за этой ошибки; следующий `new` может пройти |
| Тело конструктора | Создание одного объекта | Класс не становится erroneous только из-за ошибки конструктора |
| Обычный метод после успешной class initialization | Вызов метода | Состояние class initialization не меняется |

## Рекурсивная инициализация в одном потоке

Инициализатор одного класса может обратиться к другому классу, а тот — снова к первому. Если повторный запрос приходит **из того же потока**, который уже инициализирует класс, JVM не запускает инициализатор повторно и не считает ситуацию ошибкой сама по себе.

```java
class A {
    static int x = B.y + 1;
}

class B {
    static int y = A.x + 1;
}

public class RecursiveInitDemo {
    public static void main(String[] args) {
        System.out.println(A.x + "/" + B.y);
    }
}
```

Вывод:

```text
2/1
```

Если первым active use становится чтение `A.x`:

1. Начинается инициализация `A`; до выполнения инициализатора `A.x` имеет default value `0`.
2. Вычисление `A.x` читает `B.y`, поэтому начинается инициализация `B`.
3. Вычисление `B.y` читает `A.x`. `A` уже инициализируется тем же потоком.
4. Рекурсивный запрос на инициализацию `A` завершается нормально, без повторного выполнения инициализатора `A`.
5. `B.y` получает `0 + 1`, то есть `1`.
6. Инициализация `B` заканчивается; вычисление `A.x` продолжается и получает `1 + 1`, то есть `2`.

Это допустимая, но неочевидная конструкция. В реальном коде циклические зависимости статических инициализаторов лучше устранять: значение может быть прочитано до присваивания, которое программист ожидал увидеть первым.

## Синхронизация class initialization и безопасная публикация

Для каждого класса или интерфейса JLS задаёт уникальную блокировку инициализации. Если один поток уже инициализирует тип, другой поток, которому требуется инициализировать тот же тип, ожидает завершения процедуры. Если тот же поток рекурсивно запрашивает инициализацию типа, который он уже инициализирует, повторный запуск не выполняется.

**Как JVM отображает тип на lock — деталь реализации.** JLS не требует, чтобы блокировкой был именно объект `C.class`. Нельзя считать, что JVM обязательно выполняет `synchronized (C.class)`. Реализация может оптимизировать проверки уже инициализированного класса, но должна сохранить требуемые happens-before отношения. См. [JLS 21 §12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2), [JVMS 21 §5.5](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.5) и [JLS 21 §17.4.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.4.5).

Из этого следуют две границы:

- Успешная class initialization обеспечивает корректную публикацию статического состояния, созданного в инициализаторах, для последующего использования типа. Поэтому статическая инициализация часто применяется для создания неизменяемого singleton.
- Это **не** означает, что все последующие операции с изменяемыми `static`-полями автоматически потокобезопасны. После завершения class initialization обычные записи/чтения должны использовать подходящую синхронизацию, `volatile`, атомарные классы и т. п.

Не следует смешивать это с *final-field semantics*. Для `final`-полей объекта существуют отдельные гарантии Java Memory Model при корректном завершении конструктора ([JLS 21 §17.5.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.5.1)). Если `this` утекает из конструктора до завершения создания объекта, нельзя безоговорочно применять к такому объекту рассуждение о корректно сконструированном состоянии. Полная модель безопасной публикации, happens-before и data races относится к CON-JMM.

## Что не следует смешивать с class initialization

### `ClassCircularityError` — не рекурсивный static initializer

Рекурсивный запрос `A → B → A` не означает циклическую иерархию классов. Это разные механизмы.

`ClassCircularityError` связан с **загрузкой и derivation**: загружаемый класс или интерфейс оказывается собственным прямым или косвенным superclass/superinterface. Основание — [JLS 21 §12.2.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.2.1) и [JVMS 21 §5.3.5](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3.5). Хотя `ClassCircularityError` относится к семейству `LinkageError`, обнаружение циклической иерархии относится к loading/derivation, а не к фазе linking.

Обычный исходный код:

```java
class A extends B { }
class B extends A { }
```

отвергается `javac` на этапе компиляции. Runtime-сценарий `ClassCircularityError` требует циклического графа в загружаемом class file, например при работе собственного class loader. Не следует изображать его как обычный пример с двумя static-блоками.

### Возможный deadlock между потоками — отдельный P2-случай

Same-thread recursion распознаётся процедурой инициализации. Но теоретически два потока могут образовать цикл ожиданий:

- поток A начал инициализировать `ClassA` и в ходе `<clinit>` обращается к `ClassB`;
- поток B начал инициализировать `ClassB` и в ходе `<clinit>` обращается к `ClassA`;
- каждый ждёт завершения инициализации другого типа.

Это может привести к взаимной блокировке. Нельзя обещать детерминированный вывод или точный тайминг: результат зависит от расписания потоков и конкретных зависимостей. Стресс-тест такого deadlock в рамках статьи не заявляется.

### Разные class loaders — разные определения классов

Идентичность runtime-класса зависит не только от binary name, но и от defining class loader. Два загрузчика могут определить классы с одинаковым именем как разные runtime-типы; у таких определений будет отдельное состояние инициализации и отдельные static fields. Это граница JVM-LOADING, а не повод подробно разбирать delegation, модули и class unloading. Нормативные отправные точки: [JLS 21 §12.2.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.2.2), [JVMS 21 §5.3](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3).

## Частые ошибочные формулировки

| Ошибочно | Точнее |
|---|---|
| «Класс инициализируется при загрузке» | Loading, linking и class initialization — разные действия. Загрузка сама по себе не запускает static initializers. |
| «Все static-поля выполняются до static-блоков» | Non-constant static field initializers и static blocks выполняются в общем текстовом порядке. Constant variables имеют отдельный ранний шаг. |
| «При инициализации класса запускаются все его интерфейсы» | Для инициализации класса учитываются superinterfaces, объявляющие default methods, в рекурсивном postorder. |
| «Если класс реализует интерфейс, интерфейс уже инициализирован» | Сам факт `implements` не запускает интерфейс без default methods. Прямое active use его поля/метода — отдельный триггер. |
| «Подкласс начинает инициализировать свои поля до конструктора родителя» | Все поля сначала получают default values; инициализаторы подкласса выполняются после возврата конструктора суперкласса. |
| «В конструкторе родителя вызывается версия метода родителя» | Переопределяемый метод использует обычную динамическую диспетчеризацию и может попасть в override подкласса. |
| «`this(...)` повторно запускает все field initializers» | Инициализаторы класса выполняются один раз на цепочке делегирования; делегирующий конструктор продолжает тело после возврата `this(...)`. |
| «Любое исключение из конструктора делает класс unusable» | Ошибка instance initializer/constructor прерывает создание объекта, но сама по себе не делает класс erroneous. |
| «Рекурсия static initializers — это `ClassCircularityError`» | Same-thread recursive initialization и циклическая иерархия при загрузке — разные случаи. |
| «Class init lock — это `synchronized (C.class)`» | JVM выбирает mapping типа на initialization lock; JLS не требует использовать монитор объекта `Class`. |

## Карта обязательного покрытия CORE-INIT

P0/P1/P2 — приоритеты исследовательского плана, а не рейтинг частоты на реальных интервью.

| ID плана | Приоритет | Где дан ответ |
|---|---|---|
| CI-01 | P0 | «Фазы жизненного цикла класса» |
| CI-02 | P0 | «Когда класс инициализируется» |
| CI-03 | P0 | «Constant variables» и центральный пример |
| CI-04 | P0 | «Инициализируется тип, который объявил static-член» |
| CI-05 | P0 | Центральный пример; порядок class initialization |
| CI-06 | P0 | Центральный пример; интерфейсный postorder |
| CI-07 | P0 | Default values и разбор центрального примера |
| CI-08 | P0 | Instance initialization и цепочка конструкторов |
| CI-09 | P0 | Виртуальный вызов из конструктора |
| CI-10 | P0 | Ошибка инициализации класса |
| CI-11 | P1 | Рекурсивная инициализация в одном потоке |
| CI-12 | P1 | Синхронизация class initialization |
| CI-13 | P1 | Исключение при создании экземпляра |
| CI-14 | P1 | API `Class.forName` / `ClassLoader.loadClass` |
| CI-15 | P1 | `this` escape во время construction |
| CI-16 | P1 | Forward references |
| CI-17 | P2 | `ClassCircularityError` |
| CI-18 | P2 | Deadlock между потоками |
| CI-19 | P2 | Разные class loaders |
| CI-20 | P2 | Constant variables и бинарная совместимость |

## Границы темы

- **CORE-CLASSES:** здесь не переписывается базовое определение конструктора, default constructor, перегрузка конструкторов и простая последовательность одного класса. Эти понятия изложены в [CORE-CLASSES](classes.md); здесь добавлена иерархическая последовательность.
- **CORE-INHERITANCE:** общий механизм overriding не пересказывается. Используется только момент вызова override до инициализаторов подкласса; общая модель — в [CORE-INHERITANCE](inheritance.md).
- **CORE-INTERFACES:** здесь нужна только граница инициализации superinterfaces с default methods. Разрешение конфликтов default methods остаётся за отдельной темой.
- **JVM-LOADING:** loading/linking описаны ровно настолько, чтобы не путать их с initialization. Delegation, modules, bytecode, metaspace, unloading и особенности HotSpot не являются целью статьи.
- **CON-JMM:** дано только необходимое объяснение синхронизации и escape при конструировании. Полная модель happens-before, data races, safe publication и final-field semantics требует отдельного материала.
- **Не заявляется:** runtime воспроизведение `ClassCircularityError`, стресс-тест межпоточного deadlock, полная проверка разных class loaders и визуальная проверка Obsidian/IntelliJ.

## Источники

### Нормативные источники — Java SE 21

- [JLS 21 §4.12.4 — final variables и constant variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4).
- [JLS 21 §4.12.5 — initial values of variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.5).
- [JLS 21 §8.3.2 — field initialization](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.2) и [§8.3.3 — restrictions on field references in initializers](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.3).
- [JLS 21 §8.6 — instance initializers](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.6) и [§8.7 — static initializers](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.7).
- [JLS 21 §8.8.7 — constructor body](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7), [§8.8.7.1 — explicit constructor invocations](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7.1), [§8.8.9 — default constructor](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.9).
- [JLS 21 §9.3 — interface fields](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.3), [§9.4.3 — interface default methods](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4.3).
- [JLS 21 §12.1.3 — initialize initial class](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.1.3); [§12.2 — loading](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.2); [§12.2.1 — loading process](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.2.1); [§12.3 — linking](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.3).
- [JLS 21 §12.4.1 — when initialization occurs](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.1), [§12.4.2 — detailed initialization procedure](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2), [§12.5 — creation of new class instances](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.5).
- [JLS 21 §16.8 — Definite Assignment and Static Initializers](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html#jls-16.8) и [§16.9 — Definite Assignment, Constructors, and Instance Initializers](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html#jls-16.9).
- [JLS 21 §17.4.5 — happens-before order](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.4.5) и [§17.5.1 — final field semantics](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.5.1).
- [JLS 21 §13.1 — binary form](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.1) и [§13.4.9 — final fields and constant variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.4.9).

### Граница виртуальной машины и API

- [JVMS 21 §5.3 — creation and loading](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3), [§5.3.5 — derivation](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3.5), [§5.4 — linking](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.4), [§5.5 — initialization](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.5).
- Java SE 21 API: [Class.forName](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#forName(java.lang.String)), [ClassLoader.loadClass](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html#loadClass(java.lang.String)), [ExceptionInInitializerError](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ExceptionInInitializerError.html), [NoClassDefFoundError](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/NoClassDefFoundError.html).

JLS определяет языковые гарантии; JVMS — требования к виртуальной машине и class files; API — контракт библиотечных методов. Наблюдения конкретной реализации HotSpot не выдаются за универсальные правила языка.

### Интервью-подборки — только для проверки пробелов

Эти источники помогают увидеть формулировки вопросов и контрпримеры, но не являются нормативными и не доказывают статистическую частоту:

- [Baeldung — Java Class Structure and Initialization Interview Questions](https://www.baeldung.com/java-classes-initialization-questions): constructor chaining, instance/static initializer blocks, static-field Singleton. В тексте встречается упрощённое «during class loading»; в статье оно заменено точными терминами JLS loading и class initialization.
- [Baeldung — Java Interview Questions](https://www.baeldung.com/java-interview-questions): смежные вопросы о ClassLoader и загрузке классов; не используется как нормативное объяснение инициализации.
- [teamlead/java-interview-questions](https://github.com/teamlead/java-interview-questions): prompts об ошибках в class initialization block, исключениях конструктора, static blocks, порядке конструкторов и default values. Заявленная авторами статистика публичных интервью не считается независимо валидированной частотностью.
- [Viacheslav Chernyshov — Java Interview Questions and Answers (RU)](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/ru/README.md): смежные prompts о Singleton, thread safety и double-checked locking; прямой вопрос о полном порядке class initialization в доступном списке не обнаружен.

## Что статья не утверждает

- Успешная компиляция центрального примера не доказывает полноту всей JLS.
- Успешная class initialization не делает последующие изменения всех `static`-полей потокобезопасными.
- Наличие `static final` не означает, что поле — constant variable.
- Сам факт `implements` не означает инициализацию всех интерфейсов.
- Вызов override из конструктора не означает, что поля подкласса уже прошли свои initializers.
- Same-thread recursive initialization не равна `ClassCircularityError`.
- `ClassCircularityError`, несмотря на имя `LinkageError`, не следует относить к ошибке linking-фазы.
- Не утверждается, что конкретная JVM использует объект `Class` как initialization lock.
- Результаты одного запуска не дают детерминированного вывода для межпоточных циклов и deadlock.
- Синтаксис Java после версии 21 не переносится в эту статью без отдельной проверки и изменения целевой версии.
