# CORE-INIT — Порядок инициализации в Java 21

**Версия:** Java SE 21. **Исследовательская спецификация:** принятый 00 план [CORE-INIT.md](../../docs/plans/CORE-INIT.md) из PR #50, HEAD `0614e5722f5f89cbaa4425fc707f1a3aa192368d`. **Задача:** [Issue #51](https://github.com/Melchiorius/backend-interview-labs/issues/51).

## Главная идея

При обычном внешнем первом использовании `new Child()` инициализация класса предшествует созданию экземпляра. Это разные процессы; повторный запрос из того же потока при уже начатой инициализации может завершиться до её окончания (см. раздел «Рекурсивная инициализация в одном потоке»).

1. **Loading (загрузка)** — JVM получает бинарное представление класса и создаёт runtime-представление типа.
2. **Linking (связывание)** — проверка бинарного представления, подготовка статических полей, разрешение символических ссылок. Разрешение может быть отложенным.
3. **Class initialization (инициализация класса)** — исполнение инициализаторов `static`-полей и `static`-блоков класса; у интерфейса — инициализаторов его полей.
4. **Создание экземпляра** — выделение памяти под объект и присвоение **всем** его полям, включая поля подкласса, значений по умолчанию.
5. **Instance initialization и constructor body** — после вычисления аргументов вызова выполняется цепочка конструкторов; после нормального возврата конструктора суперкласса выполняются инициализаторы текущего класса в текстовом порядке, затем оставшаяся часть его конструктора.

Инициализация класса отвечает за статическое состояние типа. Создание экземпляра — за состояние конкретного объекта. Конструктор родителя может вызвать переопределённый метод подкласса до выполнения инициализаторов подкласса: его поля могут ещё содержать значения по умолчанию, но предыдущий код мог уже их изменить.

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

Поле `CONSTANT` объявлено в `Parent` как `static final int` и инициализировано constant expression `67`. `Child` наследует это поле, поэтому обращение `Child.CONSTANT` допустимо. Это constant variable по [JLS 21 §4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4); ссылка на такое поле разрешается при компиляции Java-клиента в значение (JLS §13.1). Поэтому сначала появляется только `Child.CONSTANT = 67` — ни один инициализатор родителя или подкласса ещё не запускался.

**2. `Child.BOXED` запускает именно `Parent`.**

Поле `BOXED` также объявлено в `Parent`, но имеет тип `Integer` и не является constant variable. Имя `Child` здесь лишь квалификатор унаследованного поля; фактический declaring type — `Parent`. Поэтому перед чтением инициализируется `Parent`, но не `Child`. Внутри `Parent` в текстовом порядке исполняются `staticValue`, `BOXED` и static block. Именно поэтому видны три строки с префиксом `Parent`, а строки `Child.static...` пока нет.

**3. `Child.OWN` запускает инициализацию самого `Child`.**

Теперь используется static-поле, объявленное непосредственно в `Child`. Суперкласс `Parent` уже инициализирован на предыдущем шаге. При инициализации класса Java рассматривает superinterfaces, объявляющие default methods: рекурсивный обход выполняется в postorder, поэтому сначала печатается `RootDefault.<clinit>`, затем `LeafDefault.<clinit>`. После интерфейсов исполняются собственные static field initializer и static block `Child`.

`QuietParent` и `QuietChild` не объявляют default methods. Сам факт `implements QuietChild` не запускает их инициализацию, поэтому их маркеров здесь нет.

**4. Прямое чтение `QuietChild.MARK` инициализирует `QuietChild`, но не `QuietParent`.**

Это отдельное active use поля интерфейса. Инициализация интерфейса сама по себе не инициирует его superinterfaces. Поэтому появляется `QuietChild.<clinit>`, но не `QuietParent.<clinit>`. Это отсутствие автоматической инициализации предков: код самого инициализатора может явно обратиться к другому типу и запустить его инициализацию.

**5. Перед конструкторами все instance fields уже имеют default values.**

К моменту `new Child()` инициализация класса завершена. Память выделяется под объект, содержащий поля и `Parent`, и `Child`. До запуска конструкторов `childValue == 0`, а `childLabel == null`. Поле `parentValue` тоже сначала получает `0`; позднее его initializer присвоит `67`.

**6. `Child()` делегирует через `this(67)`, а `Child(int)` вызывает `super()`.**

Первый оператор `Child()` вызывает другой конструктор того же класса. Конструктор `Child(int)` явно вызывает конструктор суперкласса `Parent`. Затем цепочка доходит до `Object`, у которого нет суперкласса. Инициализаторы текущего класса не запускаются повторно из-за `this(...)`.

**7. После возврата `Object` выполняются инициализаторы экземпляра `Parent`.**

Сначала выполняется initializer поля `parentValue`, затем instance initializer block. После этого начинается тело конструктора `Parent()`.

**8. Конструктор `Parent()` вызывает виртуальный метод, и срабатывает override `Child.observe()`.**

Динамическая диспетчеризация не отключается во время создания объекта. Фактический объект — `Child`, поэтому вызывается его реализация. Но инициализаторы `Child` ещё не выполнялись: override видит `childValue=0` и `childLabel=null`. Это обычный виртуальный вызов на частично сконструированном объекте, а не специальный режим конструкторов. Здесь до вызова никто не присваивал этим полям другие значения; это не гарантия сохранения нуля и null для любого конструктора.

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
- стандартная реализация `ClassLoader.loadClass(name)`, эквивалентная `loadClass(name, false)`;
- вызов `Class.forName(name, false, loader)`.

Класс при этом может быть загружен. Перечень относится к самой операции, а не ко всем вычислениям внутри выражения: вычисление квалификатора или аргументов может вызвать методы и обратиться к другим статическим членам. Стандартный `loadClass` не является активным использованием, но переопределённый метод пользовательского загрузчика может исполнить дополнительный код, в частности вызвать `Class.forName` с `true`.

Для контраста, `Class.forName(name)` для обычного имени класса или интерфейса инициирует инициализацию. Вариант с флагом позволяет отделить загрузку от инициализации. Ниже фрагмент: предполагается, что переменная `loader` уже содержит нужный `ClassLoader`; обработка `ClassNotFoundException` опущена.

```java
Class<?> first = Class.forName("example.SomeType", false, loader);
// Класс загружен, но его инициализаторы не запускаются этим вызовом.

Class<?> second = Class.forName("example.SomeType", true, loader);
// Инициализация выполняется, если она ещё не происходила.
```

Это контракт Java SE 21 API: [Class.forName(String)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#forName(java.lang.String)), [Class.forName(String, boolean, ClassLoader)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#forName(java.lang.String,boolean,java.lang.ClassLoader)) и [ClassLoader.loadClass(String)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html#loadClass(java.lang.String)).

Имя массива для `Class.forName` задаётся в форме `Class.getName()`, например `[Ljava.lang.String;`; загрузка массива не инициализирует тип его элементов даже при `initialize=true`. Получение метаданных (`getDeclaredField`, `getDeclaredMethod`) не равнозначно доступу к значению поля или вызову метода. При допустимом отражательном чтении статического поля `Field.get(null)` или вызове статического метода `Method.invoke(null)` инициализируется объявивший член тип. См. [Field.get](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Field.html#get(java.lang.Object)), [Method.invoke](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#invoke(java.lang.Object,java.lang.Object...)), [CORE-CLASSLOADER](classloaders.md).

Сохраните `LoaderReflection.java` в отдельном каталоге. Это три разных типа, чтобы первое активное обращение к одному не скрывало эффект следующего.

~~~java
class LoaderReflection {
    public static class F { public static int x = mark("F"); }
    public static class M { static int x = mark("M"); public static int call() { return x; } }
    public static class L { static int x = mark("L"); }
    static int mark(String s) { System.out.println(s); return 67; }
    public static void main(String[] args) throws Exception {
        ClassLoader loader = LoaderReflection.class.getClassLoader();
        Class<?> f = loader.loadClass(F.class.getName());
        var field = f.getDeclaredField("x");
        var method = Class.forName(M.class.getName(), false, loader).getDeclaredMethod("call");
        Class.forName(L[].class.getName(), true, loader);
        System.out.println("metadata/array");
        System.out.println(field.get(null));
        System.out.println(method.invoke(null));
        ClassLoader custom = new ClassLoader(loader) {
            @Override public Class<?> loadClass(String name) throws ClassNotFoundException {
                return Class.forName(name, true, getParent());
            }
        };
        custom.loadClass(L.class.getName());
    }
}
~~~

Вывод: `metadata/array`, `F`, `67`, `M`, `67`, `L` (каждая запись на отдельной строке). Получение метаданных и класса массива не запустило инициализаторы; чтение поля и вызов метода запустили разные типы. Последний маркер появляется потому, что переопределённый `loadClass` явно вызвал `Class.forName` с `true` через родительский загрузчик; пример не определяет новые классы и не проверяет изоляцию пространств имён.

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

То же правило относится к вызову статического метода: инициализируется объявивший его класс. В `ActiveUse.java` выражение с квалификатором и аргументом вычисляет их до вызова метода.

~~~java
class ActiveUse {
    static class Base {
        static int value = mark("Base", 67);
        static void call(int n) { System.out.println("call=" + n); }
    }
    static class Sub extends Base { static int own = mark("Sub", 68); }
    static class Side { static int x = mark("Side", 67); }
    static int mark(String s, int n) { System.out.println(s); return n; }
    static Sub qualifier() { System.out.println("qualifier"); return null; }
    public static void main(String[] args) {
        Class<?> c = Sub.class;
        Sub[] array = new Sub[1];
        System.out.println("literal/array");
        qualifier().call(Side.x);
        System.out.println(Sub.own);
    }
}
~~~

Вывод: `literal/array`, `qualifier`, `Side`, `Base`, `call=67`, `Sub`, `68` — отдельные строки. Получение класса и создание массива не инициализируют `Sub`. Вызов `qualifier()` выполняется, хотя возвращает `null`: статическому методу не нужен экземпляр. Чтение `Side.x` инициализирует `Side`, а вызов `call` — объявивший метод `Base`. Только чтение `Sub.own` инициирует `Sub`. Это различие между собственным триггером и побочными действиями при вычислении выражения.

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

По [JLS 21 §13.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.1) ссылка на поле — константную переменную — разрешается при компиляции в значение её инициализатора; для статической константной переменной ссылка на поле в бинарном коде отсутствует. Это не факультативная оптимизация JIT. Если библиотека изменит `public static final int LIMIT = 67` на `68`, прежний Java-клиент, скомпилированный с прежним значением, не увидит новое значение при таком использовании до перекомпиляции ([§13.4.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.4.9)). Обычное неконстантное `static final` поле, например `Integer LIMIT`, читается как поле; правило подстановки к нему не применяется. Это нормативное объяснение; эксперимент с двумя версиями библиотеки здесь не выполнялся.

## Порядок class initialization

### Суперкласс, затем superinterfaces с default methods, затем класс

При инициализации класса `C` подробная процедура выполняет следующие действия:

1. Отмечает, что инициализация `C` начата текущим потоком.
2. Присваивает значения его constant variables.
3. Инициализирует суперкласс `C`, если он ещё не инициализирован.
4. Инициализирует superinterfaces `C`, объявляющие хотя бы один `default`-метод, в порядке специального рекурсивного обхода.
5. Выполняет class variable initializers и static blocks самого `C` в текстовом порядке.

Для обычного класса сначала идёт цепочка классов-суперклассов, затем нужные интерфейсы, затем собственные non-constant static initializers класса. Это нормальный порядок при завершении процедуры, а не утверждение о том, что рекурсивный запрос в том же потоке ждёт окончания всех инициализаторов.

### Почему интерфейсы обходятся в postorder

Для каждого непосредственно реализованного интерфейса Java рекурсивно рассматривает его superinterfaces слева направо, прежде чем вернуться к самому интерфейсу. В инициализацию класса включаются только интерфейсы этого обхода, объявляющие хотя бы один `default`-метод.

В центральном примере `Child implements LeafDefault, QuietChild`, а `LeafDefault extends RootDefault`. Суперкласс `Parent` уже инициализирован; среди интерфейсов с `default`-методами порядок — `RootDefault` → `LeafDefault`, затем выполняются статические инициализаторы `Child`. Ни `QuietParent`, ни `QuietChild` не объявляют `default`-методов, поэтому одно лишь `implements QuietChild` не запускает их инициализацию.

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

Будет напечатано `ChildI`, затем `1`; `ParentI` не инициализируется. Это контрпример к фразе «инициализация интерфейса всегда сначала инициализирует его родителей». Исключение для superinterfaces с default methods относится к инициализации **класса**, а не к прямой инициализации интерфейса. Речь именно об автоматическом обходе: собственный код инициализатора может прочитать неконстантное поле предка и инициировать его явно.

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

Правило [JLS 21 §8.3.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.3) ограничивает ссылку по простому имени на поле, объявленное в данном типе, в его собственном инициализаторе или раньше декларатора: для статического поля — в статическом инициализаторе поля или блоке, для экземплярного — в экземплярном инициализаторе поля или блоке. Ошибка возникает при выполнении всех условий правила, включая ближайший объемлющий тип (для статического — класс или интерфейс) и то, что ссылка не является левой частью присваивания. Простое имя может быть в области видимости и всё же быть запрещено.

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

В `ForwardDemo.java` объединены допустимые контрасты: квалифицированное чтение, чтение в методе, присваивание по простому имени слева и экземплярное поле.

~~~java
class ForwardDemo {
    static int qualified = ForwardDemo.later;
    static int viaMethod = read();
    static { later = 68; }
    static int assigned = ForwardDemo.later;
    static int later = 67;
    int before = this.after;
    int after = 67;
    static int read() { return later; }
    public static void main(String[] args) {
        System.out.println("forward=" + qualified + ":" + viaMethod + ":" + new ForwardDemo().before + ":" + assigned + ":" + later);
    }
}
~~~

Вывод `forward=0:0:0:68:67`: первые два чтения видят 0 до присваивания, экземплярное `this.after` также видит 0; присваивание `later = 68` разрешено, затем объявленный инициализатор `later = 67` перезаписывает значение. Не следует переносить разрешённость квалификации на все поля: для blank final действует анализ определённого присваивания (definite assignment, DA), а локальная переменная вообще не получает автоматического начального значения.

Сохраните каждый отрицательный контраст в отдельном файле, не объединяя классы:

`BadStaticForward.java`:

~~~java
class BadStaticForward { static int a = b; static int b = 67; }
~~~

`BadInstanceForward.java`:

~~~java
class BadInstanceForward { int a = b; int b = 67; }
~~~

`BadBlankFinal.java`:

~~~java
class BadBlankFinal { final int n; BadBlankFinal() { System.out.println(this.n); n = 67; } }
~~~

`BadLocal.java`:

~~~java
class BadLocal { void f() { int n; System.out.println(n); } }
~~~

Первые два компилятор отвергает за `illegal.forward.ref`, остальные два — за `var.might.not.have.been.initialized`: квалификация `this.n` не отменяет проверку DA. См. [JLS 21 §16.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html#jls-16.8), [§16.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html#jls-16.9).

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

Для обычного неквалифицированного `new Child(...)` после необходимой инициализации класса при нормальном завершении Java выполняет следующую последовательность ([JLS 21 §15.9.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9.4), [§12.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.5)):

1. Выделяет память под объект со всеми instance fields всех классов его иерархии.
2. Присваивает каждому полю default value.
3. Вычисляет фактические аргументы `new` слева направо, затем вызывает выбранный конструктор.
4. Если конструктор начинает с `this(...)`, сначала вычисляет аргументы этого вызова слева направо, затем рекурсивно выполняет другой конструктор того же класса. Инициализаторы текущего класса на этом шаге ещё не выполняются.
5. Конструктор, который не делегирует через `this(...)`, вычисляет аргументы явного `super(...)` слева направо и вызывает конструктор прямого суперкласса либо выполняет неявный `super()`. У `Object` суперкласса нет; этот шаг для него отсутствует.
6. Когда конструктор суперкласса и его цепочка завершились, выполняются instance field initializers и instance initializer blocks текущего класса в текстовом порядке.
7. Выполняется оставшаяся часть тела текущего конструктора.
8. Если текущий конструктор был вызван через `this(...)`, после его завершения продолжается оставшаяся часть тела делегирующего конструктора.

Так повторяется для каждого класса от базового к производному. При резком завершении вычисления аргумента следующие аргументы и соответствующий вызов не выполняются; при ошибке вызванного конструктора или инициализатора последующие стадии не выполняются. Уже произошедшие побочные эффекты не откатываются автоматически. Для квалифицированного `new` сначала проверяется объемлющий экземпляр; квалифицированный `super` также имеет особые правила ([§8.8.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7.1)). Специальные правила [enum](enums.md), [record](records.md), [вложенных классов](nested-classes.md), [клонирования](object.md) и [десериализации](serialization.md) нельзя выводить из этой последовательности.

Сохраните `ArgumentOrder.java` отдельно:

~~~java
class ArgumentOrder {
    static int arg(String label, int n) { System.out.println(label); return n; }
    static int fail() { System.out.println("fail"); throw new IllegalArgumentException(); }
    static class Parent { Parent(int n) { System.out.println("parent"); } }
    static class Child extends Parent {
        int x = arg("field", 67);
        Child(int a, int b) { this(arg("this argument", a)); System.out.println("outer body"); }
        Child(int a) { super(arg("super argument", a)); System.out.println("inner body"); }
    }
    public static void main(String[] args) {
        new Child(arg("new left", 67), arg("new right", 68));
        try { new Child(fail(), arg("skipped", 67)); }
        catch (IllegalArgumentException e) { System.out.println("caught"); }
    }
}
~~~

Вывод по строкам: `new left`, `new right`, `this argument`, `super argument`, `parent`, `field`, `inner body`, `outer body`, `fail`, `caught`. При второй попытке исключение первого аргумента оставляет его маркер `fail`, но не выполняет `skipped` и не запускает конструкторы. Сам порядок выделения памяти и начальных значений перед аргументами установлен нормой, а не выводится из этих маркеров.

Отдельный отрицательный пример `EarlyThis.java`:

~~~java
class EarlyThis {
    EarlyThis() { this(read()); }
    EarlyThis(int n) {}
    int read() { return 67; }
}
~~~

В Java 21 в аргументах явного вызова другого конструктора нельзя обращаться к экземплярному методу создаваемого объекта таким способом: `javac` отвергает вызов `read()` (`cant.ref.before.ctor.called`). Это не запрет всех вычислений аргументов: статические методы, как в `ArgumentOrder`, допустимы.

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

В Java 21 у обычного класса явно объявленный конструктор, в начале которого нет вызова другого конструктора через `this(...)` или `super(...)`, неявно вызывает `super()` (кроме конструктора `Object`). В скобках нет **фактических аргументов**, но это не означает, что у выбранного конструктора прямого суперкласса непременно пустой список формальных параметров.

Для допустимого вызова нужен доступный и применимый к этому списку аргументов конструктор, однозначно выбранный по правилам разрешения перегрузок; должны выполняться и правила проверки checked exceptions. Конструктор с единственным параметром переменной арности, например `Parent(int... xs)`, применим при нуле аргументов: при таком вызове он получает пустой массив `int[]`. Если же у родителя есть только `Parent(int value)`, неявный вызов неприменим:

```java
class Parent {
    Parent(int value) { }
}

class Child extends Parent {
    Child() { } // compile-time error: implicit super() is not applicable
}
```

Исправление — явно выбрать существующий конструктор родителя, например `Child() { super(67); }`.

**Три разных понятия.** Вызов с нулём аргументов (`super()`) относится к выражениям, передаваемым при вызове. Конструктор без параметров (`Parent() {}`) относится к объявлению. *Default constructor* — конструктор, неявно объявляемый, если класс вообще не объявил конструкторов; для обычного верхнеуровневого класса он не имеет формальных параметров и секции `throws`, а его доступность соответствует доступности класса. Явный `Child() {}` — не default constructor, даже если тело пустое. Подробности — [CORE-CLASSES](classes.md), [CORE-ACCESS](access.md).

#### Контраст: переменная арность и неявный вызов

Сохраните в `Main.java` (в отдельном каталоге):

~~~java
class Parent {
    Parent(int... xs) { System.out.println(xs.length); }
}
class Explicit extends Parent { Explicit() {} }
class Implicit extends Parent {}
class Main {
    public static void main(String[] args) {
        new Explicit();
        new Implicit();
    }
}
~~~

В `Parent` нет конструктора с пустым списком формальных параметров. У `Explicit` конструктор объявлен явно, а у `Implicit` — неявно. В наблюдаемом запуске на Java 21 обе цепочки вызывают `Parent(int... xs)` и печатают:

~~~text
0
0
~~~

**Нормативное правило и граница наблюдения.** [JLS 21 §8.8.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7) и [§8.8.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7.1) задают вызов конструктора; при разрешении перегрузок учитываются доступность ([§6.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6)), применимость в том числе при переменной арности и выбор наиболее специфичного кандидата ([§15.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2), в частности §15.12.2.4–5). Для явно объявленного конструктора без явного вызова это объясняет применимость `super()` к `Parent(int... xs)`. При вызове с переменной арностью формируется массив аргументов ([§15.12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4.2)); при нуле передаваемых элементов он пуст.

Для **неявно объявленного** default constructor нужно отдельно читать [JLS 21 §8.8.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.9): там сформулировано условие ошибки при отсутствии у суперкласса доступного конструктора, принимающего ноль аргументов и не имеющего секции `throws`. Ноль аргументов не следует механически отождествлять с нулём формальных параметров: выше `javac` принимает и подкласс без объявленного конструктора при единственном конструкторе родителя с переменной арностью. Однако формулировка об отсутствии `throws` требует отдельной оговорки ниже.

#### Доступность, неоднозначность и исключения

Каждый фрагмент — отдельный `Main.java` в собственном каталоге. Не объединяйте одноимённые классы из разных примеров.

Даже если применимые конструкторы существуют, выбор может оказаться неоднозначным:

~~~java
class Parent {
    Parent(String... xs) {}
    Parent(Integer... xs) {}
}
class Child extends Parent { Child() {} }
~~~

Конструктор с пустым списком формальных параметров тоже не гарантирует допустимость вызова. Здесь классы верхнеуровневые и разные, поэтому `private`-конструктор родителя недоступен:

~~~java
class Parent { private Parent() {} }
class Child extends Parent { Child() {} }
~~~

Checked exceptions проверяются отдельно от применимости конструктора ([JLS 21 §11.2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.3)). Объявление `throws IOException` у родителя делает следующий явно объявленный конструктор подкласса недопустимым:

~~~java
import java.io.IOException;
class Parent { Parent() throws IOException {} }
class Child extends Parent { Child() {} }
~~~

Если у такого подкласса вообще нет объявлений конструкторов (`class Child extends Parent {}` вместо последней строки), неявный default constructor также отвергается. У него нет секции `throws`. У явно объявленного конструктора можно указать подходящее исключение; вызывающий код обязан обработать его или объявить в своей секции `throws`:

~~~java
import java.io.IOException;
class Parent {
    Parent() throws IOException { System.out.println("checked=67"); }
}
class Child extends Parent { Child() throws IOException {} }
class Main {
    public static void main(String[] args) {
        try {
            new Child();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
~~~

Вывод при нормальном завершении: `checked=67`. Вызов `super()` по-прежнему неявный. Нельзя обернуть его в `try/catch` внутри тела конструктора в Java 21: явный вызов конструктора имеет специальное место в начале тела, а не является обычным оператором вызова метода.

**Отдельный диагностический контраст к §8.8.9:**

~~~java
class Parent {
    Parent() throws IllegalStateException { System.out.println("unchecked=67"); }
}
class Child extends Parent {}
class Main {
    public static void main(String[] args) {
        new Child();
    }
}
~~~

Секция `throws` у родителя есть, но указано только unchecked exception. В фактическом запуске OpenJDK/javac 21.0.12.1 с `--release 21` этот подкласс без собственного конструктора скомпилирован, вывод `unchecked=67`. Это согласуется с тем, что §11.2.3 требует объявления именно checked exceptions, но при буквальном чтении условие §8.8.9 об отсутствии секции `throws` строже наблюдаемого результата. Здесь не подменяем текст спецификации наблюдением: не утверждаем, что §8.8.9 разрешает любую секцию `throws`, что спецификация ошибочна, или что результат гарантирован для любого компилятора. Нужна независимая проверка этого пограничного случая. Для явно объявленного конструктора и unchecked exceptions применяется обычное правило проверки исключений; сама секция `throws` не означает, что исключение действительно будет выброшено.

**Как проверить.** В каждом отдельном каталоге с `Main.java`:

~~~sh
mkdir -p out
javac --release 21 -XDrawDiagnostics -d out Main.java
# Только для трёх положительных примеров после успешной компиляции:
java -cp out Main
~~~

Фактическая локальная проверка роли 02: Debian OpenJDK/javac 21.0.12.1, `LC_ALL=C`, изолированные каталоги. Три положительных примера — компиляция и запуск с кодом 0, stdout соответственно `0\n0\n`, `checked=67`, `unchecked=67`. Для отрицательных примеров по одному отказу с кодом 1, пустым stdout и следующими ключами диагностики `javac`:

| Случай | Причина отказа | Ключ диагностики |
|---|---|---|
| Только `Parent(int)` | Нет применимого конструктора | `compiler.err.cant.apply.symbol` (arg.length.mismatch) |
| `String...` и `Integer...` | Неоднозначность выбора | `compiler.err.ref.ambiguous` |
| `private Parent()` в другом верхнеуровневом классе | Нет доступа | `compiler.err.report.access` |
| `Parent() throws IOException`, явный `Child() {}` | Не объявлено проверяемое исключение | `compiler.err.unreported.exception.need.to.catch.or.throw` |
| Тот же родитель, `Child` без объявлений конструкторов | Checked exception в default constructor | `compiler.err.unreported.exception.default.constructor` |

Это результаты отдельных учебных проб, а не проверка всех конструкторов Java или полного проекта. Диагностические ключи относятся к конкретному компилятору, не являются частью гарантии JLS. Сценарии относятся к обычным классам; правила `Object`, record, enum и квалифицированного вызова конструктора внутреннего суперкласса нельзя выводить из них без отдельных условий. В частности, не переносите применимость переменной арности при обычном `super()` на специальные требования конструкторов при [десериализации](serialization.md).

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

- все поля объекта первоначально получили значения по умолчанию, но часть из них могла уже измениться;
- инициализаторы и initializer blocks суперкласса уже выполнились;
- выполняется тело конструктора суперкласса;
- instance initializers и тело конструктора подкласса **ещё не выполнялись**.

В центральном примере `Parent()` вызывает `observe()`. Фактический объект — `Child`, поэтому выполняется `Child.observe()`. `Parent.parentValue` к этому моменту уже равно `67`, но `Child.childValue` всё ещё равно `0`, а `Child.childLabel` — `null`. После возврата из `Parent()` инициализаторы подкласса присвоят этим полям `68` и `"ready"` соответственно.

Контраст `ParentWrite.java`: суперкласс записывает поле подкласса перед виртуальным вызовом.

~~~java
class ParentWrite {
    static class Parent {
        Parent() { ((Child) this).value = 68; observe(); }
        void observe() {}
    }
    static class Child extends Parent {
        int value = 67;
        @Override void observe() { System.out.println("during=" + value); }
    }
    public static void main(String[] args) {
        System.out.println("after=" + new Child().value);
    }
}
~~~

Вывод `during=68`, затем `after=67`. Вызванный `observe()` видит значение, присвоенное в конструкторе родителя; позднее собственный инициализатор поля подкласса заменяет его. Это иллюстрация риска, а не рекомендация приводить `this` к типу подкласса.

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

При `new Child()` вызов `register()` из `Parent()` попадёт в `Child.register()`. В момент записи в `escaped` поле `Child.value` ещё содержит `0`; инициализатор `= 67` выполнится позже. Именно в этом примере иных записей до публикации нет.

`volatile` здесь не «достраивает» объект. Запись ссылки в volatile-поле происходит **до** присваивания `Child.value = 67` и не публикует эту будущую запись как уже завершённую. Другой поток может получить ссылку на частично сконструированный объект. Точный результат конкурентного чтения зависит от синхронизации и момента чтения; нельзя обещать один детерминированный вывод.

Same-thread dispatch из конструктора и межпоточный `this` escape — разные вопросы. Первый сам по себе не является data race. Второй создаёт возможность наблюдать объект до завершения construction и затрагивает Java Memory Model; полная модель безопасной публикации относится к CON-JMM.

## Ошибка инициализации класса

Если процедура инициализации типа завершается ошибкой, соответствующее runtime-определение класса или интерфейса помечается ошибочным. Следующий запрос на его инициализацию не запускает инициализаторы повторно.

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

Первое чтение `Broken.VALUE` — активное использование. Статический блок вызывает `fail()`, который выбрасывает `IllegalStateException`. По §12.4.2, шаг 11, если исключение не является `Error`, процедура создаёт `ExceptionInInitializerError` с исходным исключением. Если создание обёртки само не удалось из-за `OutOfMemoryError`, вместо неё передаётся `OutOfMemoryError`. Это нормативная оговорка, искусственно исчерпывать память для её проверки не требуется. В показанном случае создаётся обёртка; определение `Broken` помечается ошибочным (*erroneous*).

При следующем запросе на инициализацию того же runtime-определения возникает `NoClassDefFoundError`. Это не запрет любого использования имени типа: литерал класса или чтение константной переменной сами по себе не требуют такой инициализации. Кроме того, `NoClassDefFoundError` возможна и по другим причинам, например когда при загрузке не найдено определение класса.

Если из инициализатора выброшен объект класса `Error` или его подкласса, он передаётся без обязательной упаковки в `ExceptionInInitializerError`. Например, `AssertionError` остаётся `AssertionError` при первом неуспешном active use; последующий запрос инициализации встретит erroneous state и приведёт к `NoClassDefFoundError`.

Если инициализация суперкласса или требуемого superinterface завершилась ошибкой, инициализируемый класс также помечается erroneous. Сообщение `NoClassDefFoundError` и текст stack trace не являются надёжным способом определить первопричину: ищите исходную ошибку первой попытки.

В отдельном `FailurePropagation.java` показаны сбой суперкласса и сбой требуемого интерфейса с методом по умолчанию:

~~~java
class FailurePropagation {
    static int fail() { throw new IllegalStateException("boom"); }
    static class Parent { static int x = fail(); }
    static class Child extends Parent { static final int K = 67; static int own = 68; }
    interface Required { int X = fail(); default void f() {} }
    static class Impl implements Required { static int own = 67; }
    static void request(String name, Runnable action) {
        try { action.run(); throw new AssertionError("expected failure"); }
        catch (LinkageError e) { System.out.println(name + "=" + e.getClass().getSimpleName()); }
    }
    public static void main(String[] args) {
        request("child first", () -> System.out.println(Child.own));
        request("parent later", () -> System.out.println(Parent.x));
        request("child later", () -> System.out.println(Child.own));
        System.out.println(Child.class.getSimpleName() + ":" + Child.K);
        request("impl first", () -> System.out.println(Impl.own));
        request("interface later", () -> System.out.println(Required.X));
        request("impl later", () -> System.out.println(Impl.own));
    }
}
~~~

Фактические строки:

~~~text
child first=ExceptionInInitializerError
parent later=NoClassDefFoundError
child later=NoClassDefFoundError
Child:67
impl first=ExceptionInInitializerError
interface later=NoClassDefFoundError
impl later=NoClassDefFoundError
~~~

Ошибка зависимости распространяется в первую попытку и помечает также зависимый класс; повторные запросы завершаются отказом. `Child.class` и `Child.K` доступны без нового запроса инициализации. Отдельные определения того же бинарного имени у разных defining loaders имеют отдельное состояние.

Это правило относится к class initialization. Оно не означает, что любое исключение из любого метода навсегда «ломает класс»: если класс уже успешно инициализирован, исключение из обычного static-метода не меняет его состояние инициализации.

Нормативный алгоритм — [JLS 21 §12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2). API: [ExceptionInInitializerError](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ExceptionInInitializerError.html) и [NoClassDefFoundError](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/NoClassDefFoundError.html).

### Проверяемые исключения и нормальное завершение блоков

[JLS 21 §11.2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.3) запрещает выход проверяемого исключения из статического инициализатора или инициализатора статического поля именованного класса: у блока нет `throws`. Исключение нужно обработать либо изменить контракт вызываемой операции. Для инициализатора экземпляра или его поля именованного класса проверяемое исключение допустимо, если есть хотя бы один явно объявленный конструктор и каждый конструктор объявляет это исключение или его суперкласс в `throws`. Для анонимных классов действует отдельное правило. Блоки по [§8.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.6) и [§8.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.7) должны быть способны завершиться нормально по правилам компилятора; это не гарантия отсутствия исключения в конкретном запуске. Поэтому вызов `fail()` с непроверяемым исключением в `Broken` допустим.

Положительный `CheckedInstance.java`:

~~~java
class CheckedInstance {
    { check(); }
    CheckedInstance() throws java.io.IOException {}
    static void check() throws java.io.IOException { System.out.println("checked"); }
    public static void main(String[] args) throws java.io.IOException { new CheckedInstance(); }
}
~~~

Вывод `checked`; вызывающий `main` тоже объявляет `IOException`. Отрицательные контрасты в разных файлах:

`BadCheckedStatic.java`:

~~~java
class BadCheckedStatic { static { fail(); } static void fail() throws java.io.IOException {} }
~~~

`BadCheckedInstance.java`:

~~~java
class BadCheckedInstance { { check(); } static void check() throws java.io.IOException {} }
~~~

`BadCompletion.java`:

~~~java
class BadCompletion { static { throw new IllegalStateException(); } }
~~~

Первые два отвергаются из-за необработанного проверяемого исключения (второй класс не объявляет конструктора); третий — `initializer.must.be.able.to.complete.normally`. См. [CORE-EXCEPTIONS](exceptions.md).

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

При первом `new` инициализация самого класса `Retryable` проходит успешно, затем instance initializer бросает исключение. Объект не возвращается вызывающему коду, но класс остаётся инициализированным. При втором `new` блок выполняется снова; счётчик уже равен двум, поэтому создание завершается успешно. При неудачной попытке уже выполненный инкремент `attempts` не откатывается.

| Где возникло исключение | Что не удалось | Что будет при следующем использовании |
|---|---|---|
| `static` field initializer / `static` block | Инициализация класса | Класс erroneous; active use приводит к `NoClassDefFoundError` |
| Instance field initializer / instance block | Создание одного объекта | Класс не становится erroneous только из-за этой ошибки; следующий `new` может пройти |
| Тело конструктора | Создание одного объекта | Класс не становится erroneous только из-за ошибки конструктора |
| Обычный метод после успешной class initialization | Вызов метода | Состояние class initialization не меняется |

## Рекурсивная инициализация в одном потоке

Инициализатор одного класса может обратиться к другому классу, а тот — снова к первому. Если повторный запрос приходит **из того же потока**, который уже инициализирует класс, JVM не запускает инициализатор повторно и не считает ситуацию ошибкой сама по себе. По шагу 3 §12.4.2 тот же поток освобождает блокировку и завершает рекурсивный запрос нормально, хотя первоначальная инициализация ещё идёт. Поэтому создание экземпляра внутри собственного статического инициализатора может опередить последующие присваивания.

Отдельный `RecursiveNew.java`:

~~~java
class RecursiveNew {
    static class C {
        static C instance = new C();
        static int value = 67;
        C() { System.out.println("during=" + value); }
    }
    public static void main(String[] args) {
        System.out.println("after=" + C.value);
    }
}
~~~

Вывод `during=0`, затем `after=67`: конструктор исполняется в ходе первого инициализатора, прежде чем `C.value` получит 67. Это исключение из обычной внешней последовательности, не повторная инициализация.

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

В шаге 6 §12.4.2 поток отмечает состояние «инициализируется этим потоком» и освобождает `LC` **до** инициализации зависимостей и выполнения собственных инициализаторов. Другой поток, увидев состояние «в процессе», освобождает `LC` и ожидает уведомления; завершивший процедуру поток вновь захватывает `LC`, меняет состояние и уведомляет ожидающих. Поэтому модель ожидания основана на состоянии процедуры, а не на обязательном удержании двух мониторов на всё время выполнения кода.

**Как JVM отображает тип на lock — деталь реализации.** JLS не требует, чтобы блокировкой был именно объект `C.class`. Нельзя считать, что JVM обязательно выполняет `synchronized (C.class)`. Реализация может оптимизировать проверки уже инициализированного класса, но должна сохранить требуемые happens-before отношения. См. [JLS 21 §12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2), [JVMS 21 §5.5](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.5) и [JLS 21 §17.4.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.4.5).

Из этого следуют две границы:

- Успешная инициализация обеспечивает необходимые отношения happens-before для последующего использования уже успешно инициализированного типа (с сохранением этих отношений и при оптимизации проверки). Это не гарантия для ссылки, которая стала доступна другому потоку до завершения процедуры через другой канал публикации. Статическая инициализация часто применяется для создания неизменяемого singleton.
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

Историческое уточнение к [плану CORE-INIT, CI-18, L137](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-INIT.md#L137): слова о том, что потоки «держат разные initialization locks», не описывают обязательный механизм JLS. Корректный цикл — A ждёт завершения инициализации B, B ждёт завершения A; `LC` освобождается до выполнения инициализаторов. Исторический план и аудит сохранены без изменений. Здесь нет утверждения о фактически выполненном тесте взаимной блокировки.

### Разные class loaders — разные определения классов

Подробный отдельный механизм — [CORE-CLASSLOADER](classloaders.md): loader namespaces, class identity, делегация, TCCL и runtime failures; [CORE-MODULES](modules.md) отдельно разъясняет readability и module graph, которые не следует отождествлять с загрузчиками.

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
| CI-01 | P0 | «Loading, linking и class initialization — разные этапы» |
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

## Проверка дополнительных примеров D-A07

Каждый новый файл компилировался отдельно в собственном временном каталоге с `javac --release 21 -XDrawDiagnostics -d out Имя.java`; положительные — `java -cp out Имя`. Debian OpenJDK/javac 21.0.12.1, `LC_ALL=C`. В 16 отдельных компиляциях: восемь успешных, восемь ожидаемых отказов с кодом 1; восемь запусков JVM с кодом 0. У успешных компиляций stdout/stderr пусты, у успешных JVM stderr пуст; каждый отказ дал пустой stdout и одну указанную ошибку. Это не число тестов общего скрипта. Новые файлы временные, не добавлены в проект. Точные результаты и хеши — в отчёте по [Issue #299](https://github.com/Melchiorius/backend-interview-labs/issues/299), отдельно от ранее принятых проб B3. Полный общий скрипт проверяется отдельно; см. README примера.

## Границы темы

- **CORE-CLASSES:** здесь не переписывается базовое определение конструктора, default constructor, перегрузка конструкторов и простая последовательность одного класса. Эти понятия изложены в [CORE-CLASSES](classes.md); здесь добавлена иерархическая последовательность.
- **CORE-INHERITANCE:** общий механизм overriding не пересказывается. Используется только момент вызова override до инициализаторов подкласса; общая модель — в [CORE-INHERITANCE](inheritance.md).
- **[CORE-INTERFACES](interfaces.md):** здесь рассматривается только инициализация суперинтерфейсов с `default`-методами; правила разрешения конфликтов таких методов вынесены в самостоятельную статью.
- **JVM-LOADING:** loading/linking описаны ровно настолько, чтобы не путать их с initialization. Delegation, modules, bytecode, metaspace, unloading и особенности HotSpot не являются целью статьи.
- **CON-JMM:** дано только необходимое объяснение синхронизации и escape при конструировании. Полная модель happens-before, data races, safe publication и final-field semantics требует отдельного материала.
- **Не заявляется:** runtime воспроизведение `ClassCircularityError`, стресс-тест межпоточного deadlock, полная проверка разных class loaders и визуальная проверка Obsidian/IntelliJ. Двухверсионный эксперимент с бинарной константой и принудительный отказ выделения памяти под ExceptionInInitializerError не выполнялись.

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
- [JLS 21 §15.9.4 — run-time evaluation of class instance creation](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9.4), [§11.2.3 — exception checking](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.3).
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
- [teamlead, зафиксированная версия](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md#L610): L610/715,657/730,736/739,777/782 — статическая/экземплярная инициализация, ошибки, порядок, значения по умолчанию. Историческая выборка A07; заявления о частоте не считаются независимо подтверждёнными.
- [Chernyshov RU, зафиксированная версия](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/597e08112ab6a57d6eac8e3c70c070da606dae73/ru/README.md#L90): L90–93 Singleton и DCL, смежные вопросы, не полный порядок/ошибки инициализации.
- [Hruday-Boppa, зафиксированная версия](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/ced3cf4f91084e9b45366f7a81a70526e7a4be7d/README.md#L312): L312–317, в том числе вопрос о проверяемых исключениях статического блока (L316); L171,293,1604–1612,1933,2820 — дополнительные вопросы. Исторические локаторы взяты из [A07, раздел источников](../../docs/audits/java-core-full-2026-11-24/A07-CORE-INIT.md); новый поиск банков вопросов не заявляется.

**Историческая опечатка индекса A07.** [Строка 163 индекса](https://github.com/Melchiorius/backend-interview-labs/blob/d088d4cf4155414b2b519111a3664125783188b4/docs/audits/java-core-full-2026-11-24/README.md#L163) ссылается на README примера, но содержит blob `a6af428dcee3ff7e4c7c794ce080055ab7f76ee9` исторического `verify.sh`, а не README (его исторический blob `22c3ec516a4a35af14f4da55997726080a96f791`). См. A07, раздел 1. Это уточнение исторического указателя, не изменение замороженного аудита.

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
