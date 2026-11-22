# CORE-REFLECTION — динамическая инспекция, вызов и границы доступа в Java 21

**Задача:** понять, какие сведения `Class` и `java.lang.reflect` действительно предоставляют во время выполнения, как они соотносятся с исходным кодом и почему «вижу private Method» ≠ «могу его вызвать». Читатель должен уметь пройти цепочку *получение Class → поиск члена → проверка доступа → вызов/создание → причина исключения*, включая узкую проверку JPMS. Основание — [принятый G1-план](../../docs/plans/CORE-ANNOTATIONS-REFLECTION.md), [Class API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html), [Method](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html) и [AccessibleObject](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AccessibleObject.html), все **Java SE 21**.

**Адресная матрица:** RF-01–RF-18 — 18 P0, RF-19–RF-23 — 5 P1, RF-24–RF-28 — 5 P2, всего **28/28**. Вместе с [аннотациями](annotations.md) — **53/53 (35 P0/10 P1/8 P2)**. Точно разграничиваем темы: [классы](classes.md), [видимость](access.md), [инициализация](initialization.md), [generics](generics.md), [erasure](erasure.md), [nested classes](nested-classes.md), [records](records.md). Здесь нет нового полного курса ClassLoader, JPMS, Security Manager, JMM или GC.

## 1. Получение Class — ещё не создание объекта и не обязательно инициализация (RF-01, RF-02, RF-16)

Объект `Class<?>` отражает runtime-класс или interface/primitive/array тип. Он не означает «создан новый объект нужного класса», а ссылка на `Class` — не универсальное доказательство, что выполнен static initializer.

~~~java
class Account {
    Account(int id) { this.id = id; }
    final int id;
}
class ClassLookup {
    void demo() throws ClassNotFoundException {
        Account account = new Account(67);
        Class<?> a = Account.class;
        Class<?> b = account.getClass();
        System.out.println(a == b);                    // true
        Class<?> primitive = int.class;
        Class<?> array = String[].class;
        System.out.println(primitive.isPrimitive());   // true
        System.out.println(array.isArray());           // true
        System.out.println(array.getComponentType());  // class java.lang.String
    }
}
~~~

`Foo.class` — class literal, `obj.getClass()` — класс *реального объекта* (может быть subclass объявленного типа переменной), `Class.forName(String)` — поиск класса по имени с инициализацией в условиях указанной перегрузки. Перегрузка `Class.forName(name, false, loader)` позволяет загрузить/связать класс **без запроса его инициализации**; инициализация может произойти позднее по другой причине. Не смешивайте **loading**, **linking** и **initialization** (JLS [§12.2–12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4)).

В G1 `RF17b=true` говорит о совпадении Class после `Class.forName(Lazy.class.getName(), false, loader)`; лишь следующий активный запрос через `Class.forName(name)` инициировал печать **RF18=static-init**, затем **RF17c=Lazy**. Это реальный stdout-порядок, а не предположение, что вызов `Class.forName(false)` «всегда обходится без loading». Границы: уже инициализированный класс не запускает статический блок повторно, пользовательские ClassLoader и сложная динамическая загрузка тут не исследованы.

Для примитивов `int.class.isPrimitive()==true` (RF25), для массива `String[].class.isArray()==true` (RF26), `String[].class.getComponentType().getSimpleName()=="String"` (RF27), а `Class.forName("java.lang.String")==String.class` (RF28). Получение Class для массива не означает создание всех его элементов.

## 2. «Public/inherited» и «declared» — разные наборы (RF-03, RF-04, RF-05, RF-06)

Отражение типов отвечает на разные вопросы, поэтому методы поиска нельзя использовать взаимозаменяемо:

| API Class | Какие элементы включает | Что не обещает |
|---|---|---|
| `getMethods()` / `getMethod(name, types...)` | Public methods, в том числе публичные inherited по правилам API | Не содержит произвольные private methods из superclass |
| `getDeclaredMethods()` / `getDeclaredMethod(...)` | Методы, **объявленные в этом классе**, независимо от public/private | Не включает просто унаследованные методы |
| `getFields()` / `getField(...)` | Public поля, включая допустимые inherited | Не получает все declared private поля |
| `getDeclaredFields()` / `getDeclaredField(...)` | Поля, объявленные именно классом | Не собирает поля всех superclasses |
| `getConstructors()` / `getConstructor(...)` | Public constructors этого класса | Конструкторы не наследуются |
| `getDeclaredConstructors()` / `getDeclaredConstructor(...)` | Конструкторы всех видимостей, объявленные классом | Наличие `Constructor` ещё не разрешает `newInstance` |

~~~java
import java.lang.reflect.*;

class Base {
    public int inheritedValue = 7;
    public void inherited() {}
}
class Child extends Base {
    private int secret = 67;
    private void hidden() {}
    public Child(int value) { secret = value; }
}

class Lookups {
    static void inspect() throws ReflectiveOperationException {
        Class<?> c = Child.class;
        Method publicInherited = c.getMethod("inherited");   // найден
        Method declaredPrivate = c.getDeclaredMethod("hidden"); // найден
        Field privateField = c.getDeclaredField("secret");
        System.out.println(publicInherited.getDeclaringClass()); // class Base
        System.out.println(declaredPrivate.getModifiers()); // флаги private (числовой бит)
        System.out.println(Modifier.isPrivate(privateField.getModifiers())); // true
    }
}
~~~

Реальная G1 `ReflectionProbe` содержала **отдельный top-level ReflectTarget**, наследующий Parent. Вывод: `RF02=true` унаследованный public method через `getMethods`; `RF03=false` тот же метод **не объявлен** в ReflectTarget; `RF04=true` private hidden присутствует в `getDeclaredMethods`. `RF05=true / RF06=false / RF07=true` аналогично разделяют inherited public field и собственный private field. `RF08=1:2` — один public constructor против двух declared; это **счёт для этого конкретного класса**, не универсальная формула.

**Важное:** `getDeclaredMethod("hidden")` успешно возвращает объект `Method`; право вызвать его проверяется отдельно, см. §3. `getMethod("missing")` — checked `NoSuchMethodException` при исполнении (RF17), а не compiler-negative, если исключение обработано.

## 3. Наличие private члена, caller access и nestmates (RF-06, RF-07, RF-13, RF-18; RF-23 P1)

Обычные проверки доступа не исчезают, когда мы получаем `Field` или `Method`:

~~~java
import java.lang.reflect.*;

class Vault {
    private int secret = 67;
    private String hidden() { return "hidden"; }
}

class ExternalReader {
    static void inspect() throws ReflectiveOperationException {
        Vault vault = new Vault();
        Field field = Vault.class.getDeclaredField("secret");
        System.out.println(field.canAccess(vault));  // false в этом top-level примере
        System.out.println(field.trySetAccessible()); // true для этих двух классов в unnamed module
        System.out.println(field.getInt(vault));     // 67

        Method m = Vault.class.getDeclaredMethod("hidden");
        // m.invoke(vault); // без открытия здесь IllegalAccessException
        if (m.trySetAccessible()) {
            System.out.println(m.invoke(vault));   // hidden
        }
    }
}
~~~

`canAccess(receiver)` спрашивает, способен ли конкретный caller законно получить доступ при текущем режиме (для non-static поле нужен корректный instance receiver; для static — null). `trySetAccessible()` пытается подавить стандартные Java access checks и возвращает `true`/**`false`** в зависимости от правил доступа и модулей. `setAccessible(true)` на запрещённом deep access может выбросить runtime `InaccessibleObjectException`. **Не** путайте `IllegalAccessException` при вызове неразрешённого члена с `InaccessibleObjectException` при попытке сделать его доступным. [AccessibleObject API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AccessibleObject.html).

**Критический контрпример G1 RF_NEST:** первоначально target был static nested class внутри ReflectionProbe. Тогда caller и target оказались **nestmates**: private access оказался допустим, `canAccess=true`, ожидаемой `IllegalAccessException` не было. Это **реально проверенная отрицательная проверка гипотезы**, а не теория. Из-за неё финальный G1 вынес `ReflectTarget` в **отдельный top-level класс** (в том же Java source, но не в том же nest). Это обеспечило в данном сценарии `RF09=false` и **RF19=IllegalAccessException** до открытия, после `trySetAccessible` — `RF10=true`, `RF20=true` и `RF21=hidden:67`.

Поэтому фраза «private всегда недоступен из reflection без setAccessible» **неверна**. Более того, «trySetAccessible всегда откроет private» тоже неверна, особенно при named modules.

## 4. Настоящая модульная граница: exports против opens (RF-08, RF-13, RF-18; RF-23 P1)

Начиная с модульной Java, простое наличие `Method`/`Field` не даёт обхода инкапсуляции. В узком, **фактически проверенном G1** сценарии два named modules: `m.target` с публичным классом `api.Vault` и `m.client` с reflective caller `client.Inspect`. В обоих вариантах клиент требует target: `module m.client { requires m.target; }`. Отличается **только** дескриптор target.

~~~java
// exports-only: m.target/module-info.java
module m.target {
    exports api;
}
~~~

В отдельном дереве исходников, для эксперимента с deep reflection, target объявлен так:

~~~java
// exports+opens: m.target/module-info.java
module m.target {
    exports api;
    opens api to m.client;
}
~~~

Это **альтернативные module-info.java**, а не два объявления одного module в одном source tree. Клиент вызывает публичный метод `Vault.read()` и пытается получить доступ к приватному полю `secret` через `trySetAccessible`. Отдельные компиляции:

~~~bash
javac --release 21 -Xlint:all -d mods --module-source-path src -m m.target,m.client
java --module-path mods -m m.client/client.Inspect
~~~

| Вывод отдельного JVM запуска | Только exports | exports + opens api to m.client |
|---|---|---|
| JM01 (публичный read) | `67` | `67` |
| JM02 (trySetAccessible private) | `false` | `true` |
| JM03 (field.getInt) | `IllegalAccessException` | `67` |

В G1 **оба варианта действительно скомпилированы и запущены** с `--module-path`: отдельный output 3+3 и сравнение с двумя эталонами, а не «module-info написан — значит работает». `exports api` разрешил публичный API клиента; `opens api to m.client` в конкретной графовой конфигурации разрешил deep reflection. У `api.Vault` был добавлен явный public constructor, потому что исходная версия порождала `[missing-explicit-ctor]` при `-Xlint:all` — этот warning был реально устранён. [JLS §7.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7), [AccessibleObject Java21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AccessibleObject.html).

**Это не завершённый курс JPMS:** не тестировали `open module`, `--add-opens`, другие module graphs, автоматические модули, custom classloaders, dynamic readability, agents/security. Практический вывод — не «откройте все пакеты в production», а «по публичному API и deep reflective access действуют разные контракты».

## 5. Метод и конструктор: динамический вызов, типы и причинившее исключение (RF-09, RF-10, RF-11, RF-12; RF-21 P1)

~~~java
import java.lang.reflect.*;
import java.util.List;

class Calculator {
    public Calculator(int start) { this.start = start; }
    private final int start;
    public int add(int x) { return start + x; }
    public void fail() { throw new IllegalArgumentException("boom"); }
    public List<String> names() { return List.of("A", "B"); }
}

class ReflectCall {
    static void run() throws ReflectiveOperationException {
        Calculator value = Calculator.class.getConstructor(int.class).newInstance(67);
        Method add = Calculator.class.getMethod("add", int.class);
        Object result = add.invoke(value, 5);  // boxing → Object(Integer)
        System.out.println(result);            // 72
        try {
            Calculator.class.getMethod("fail").invoke(value);
        } catch (InvocationTargetException ex) {
            System.out.println(ex.getCause().getClass().getSimpleName()); // IllegalArgumentException
        }
    }
}
~~~

`Method.invoke(receiver, args...)` возвращает `Object` (или `null` для void), применяет документированные dynamic invocation conversions; при target exception он обычно **оборачивает именно исключение вызываемого метода** в checked `InvocationTargetException`, причину ищите в `getCause()`. Ошибка доступа или подбора аргументов возникает на **другом этапе** и не обязана превращаться в `InvocationTargetException`. G1 `RF12=72`, `RF16=IllegalArgumentException:boom`. Не утверждайте, что unchecked target exception пробрасывается напрямую из `Method.invoke`.

`Constructor.newInstance(args...)` действительно создаёт объект (если access checks/аргументы допустимы), и тоже может оборачивать исключения constructor. В G1 public constructor `newInstance(67)` + `add(1)` дал **RF13=68**; private constructor стал вызываем после `trySetAccessible()=true` (**RF14=true, RF15=1**). Проверка доступа, достижимость конструктора и его фактический вызов — разные операции.

Контрпример с исключениями:
- `getMethod("missing")` на существующем Class при выполнении → checked `NoSuchMethodException` (G1 **RF17=NoSuchMethodException**), если обработано.
- Тот же вызов в исходнике **без `catch` или `throws`** → статическая ошибка компиляции **RF_N17**, потому что checked exception необработан.
- Аналогично `Method.invoke` без обработки checked `IllegalAccessException` → **RF_N18** с `compiler.err.unreported.exception.need.to.catch.or.throw`.

В обычном CI это **не два JUnit failure tests**: G1/04 проверял отдельные compiler-negative через изолированные javac запуски, вне общего positive runner.

## 6. Raw Class против generic Type: метаданные декларации (RF-14, RF-15)

Метод может объявить `List<String> names()`, но его runtime return-Class после erasure — просто `List.class`. Декларация generic signature часто доступна через отдельный reflection API:

~~~java
import java.lang.reflect.*;
import java.util.List;

class GenericRepository {
    public List<String> names() { return List.of("Java"); }
}

class GenericIntrospection {
    static void run() throws ReflectiveOperationException {
        Method m = GenericRepository.class.getMethod("names");
        Class<?> erased = m.getReturnType();
        Type declared = m.getGenericReturnType();
        System.out.println(erased.getSimpleName());           // List
        System.out.println(declared instanceof ParameterizedType); // true
        if (declared instanceof ParameterizedType p) {
            System.out.println(p.getActualTypeArguments()[0].getTypeName()); // java.lang.String
        }
    }
}
~~~

G1 **RF22=List, RF23=true, RF24=java.lang.String**. Это говорит о **сохранённом объявленном generic return type**, но не доказывает, что у произвольного `List` при выполнении «все элементы String» или что JVM reifies `List<String>`. [Type](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Type.html), [ParameterizedType](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/ParameterizedType.html), [erasure](erasure.md).

Проверка `getGenericReturnType()` не гарантирует, что всякий result — `ParameterizedType`: возможны `Class`, `TypeVariable`, `GenericArrayType`, `WildcardType` и ошибки metadata. Дальнейшие generic bounds и inference — в [generics](generics.md), [erasure](erasure.md), а не притворно «полностью разобраны reflection».

## 7. Нестабильный порядок членов, modifers, параметры (RF-17; RF-19 P1)

Методы `getDeclaredMethods` возвращаются **в неоговорённом порядке**; нельзя строить тест, сравнивающий прямой `Arrays.toString(getDeclaredMethods())` с одним фиксированным порядком. Явно сортируйте по нужному стабильному ключу:

~~~java
import java.lang.reflect.Method;
import java.util.Arrays;

class StableList {
    static void print(Class<?> type) {
        System.out.println(Arrays.stream(type.getDeclaredMethods())
                .map(Method::getName)
                .sorted()
                .toList());
    }
}
~~~

G1 **RF32=[add, fail, hidden, names]**, причём **порядок создан явной `sorted()`**, а не задан Java Reflection. При перегруженных методах одного имени сортировка только по `getName()` не различает сигнатуры; для полноценного deterministic dump нужен ключ *name + parameter types + return type* и контроль synthetic/bridge members (RF-19 P1, без отдельного эксперимента).

`getModifiers()` возвращает битовую маску; `Modifier.isPrivate(mask)` более осмысленно, чем hardcoded число (G1 RF29=true). `Method.getParameters()` даёт `Parameter` объекты, но **реальные source names** могут не сохраниться без `javac -parameters`. Нельзя обещать `"userId"` вместо `"arg0"`, не проверив flags и соответствующий class-файл. Bridge и synthetic методы связаны с компиляцией generics и могут появиться в declared arrays; отсутствие проверки не даёт права утверждать, что рефлексия всегда показывает «только написанные руками методы». [Parameter Java21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Parameter.html).

## 8. JDK strong encapsulation: две разные неудачи (RF-13, RF-18; RF-23 P1)

Для конкретной версии G1 на OpenJDK21 поле `String.value` существует, но попытка deep reflection из unnamed module наблюдала:

~~~java
import java.lang.reflect.Field;

class JdkPrivate {
    static void probe() throws NoSuchFieldException {
        Field field = String.class.getDeclaredField("value");
        System.out.println(field.trySetAccessible()); // false в проверенной конфигурации
        try {
            field.setAccessible(true);
        } catch (java.lang.reflect.InaccessibleObjectException ex) {
            System.out.println(ex.getClass().getSimpleName()); // InaccessibleObjectException
        }
    }
}
~~~

Фактические G1 **RF30=false / RF31=InaccessibleObjectException**. Это контрпример к убеждению, что `setAccessible(true)` гарантированно может открыть любую private реализацию JDK. Но нельзя обещать наличие внутреннего поля `String.value` у абсолютно всех Java vendors/versions — это **деталь выбранной JDK21 реализации**, а не обязательная спецификация внутренней структуры String. В отличие от того, `IllegalAccessException` в RF19 возникло при **вызове** private метода top-level-класса до открытия.

Не предлагайте «отключать encapsulation» как общую практику production: публичный API, инъекция зависимостей и явные contracts обычно лучше неустойчивого доступа к внутренностям JDK. Корректность reflection зависит от модуля вызывающего кода, целевого пакета, вида члена и действующих access rules.

## 9. API-соседи: AnnotatedElement, records/sealed, исключения (RF-20 P1, RF-22 P1)

`Class`, `Method`, `Field`, `Constructor` и `Parameter` предоставляют metadata, но разные контрактные методы. `getAnnotationsByType` может разворачивать повторяемый контейнер, тогда как `getDeclaredAnnotation` возвращает только directly present метку: подробно — [CORE-ANNOTATIONS §5](annotations.md). Из `Class.getDeclaredAnnotationsByType` не следует generic policy для получения методов, и наоборот. Отдельная P1 матрица inherited repeatable/canonical container не запускалась (RF-22).

Java21 также содержит `Class.isRecord()`, `getRecordComponents()`, `isSealed()`, `getPermittedSubclasses()`. Они полезны для introspection, но запись «существуют в API» **не равна** исполнению всех record/sealed cases. Их собственные ограничения разбираются в [records](records.md) и [sealed](sealed.md). `MethodHandles`, `VarHandles`, dynamic proxies, bytecode tools — другие API с иными permissions/performance models, не закрываются этим введением.

## 10. G1 positive trace: четыре отдельных JVM, 63 наблюдения

Принятый [G1 §6](../../docs/plans/CORE-ANNOTATIONS-REFLECTION.md) содержит два полных исходника и два отдельных JPMS source trees. **Независимая роль 04 действительно собрала и выполнила** их на Debian OpenJDK21.0.12.1 с `javac --release 21 -Xlint:all` без предупреждений, затем сверила stdout:

| Программа | JVM запусков | Подписанный stdout | Основные наблюдения |
|---|---:|---:|---|
| AnnotationsProbe | 1 | **23** | RUNTIME, @Inherited, repeatable, TYPE_USE |
| ReflectionProbe | 1 | **34** | declared/inherited, caller access, invoke, generics, ordered members |
| JPMS exports-only | 1 | **3** | JM01=67, JM02=false, JM03=IllegalAccessException |
| JPMS exports+opens | 1 | **3** | JM01=67, JM02=true, JM03=67 |
| **Итого** | **4** | **63** | **Не 63 JUnit-теста** |

**Важно:** у ReflectionProbe есть не только RF01–RF32, но и RF17b, RF17c, RF18 (порядок статической инициализации). Всего по эталону именно **34 stdout строки**. Результат RF32 сортировался явно; top-level ReflectTarget не был nestmate.

**19 compiler-negative** из G1 проверены отдельно через независимые source и компиляции с `-XDrawDiagnostics`, а не как штатный GitHub Actions JUnit negative runner. Ненадёжный тест «private всегда недоступен» был опровергнут реально исполненной промежуточной RF_NEST версией. Исторический G1 workflow **не запускал** эти исходники из Markdown в CI. Теперь роль 03 опубликовала [ReflectionProbe.java](../../examples/core/annotations-reflection/src/ReflectionProbe.java) и [stdout-эталон 34 строки](../../examples/core/annotations-reflection/reflection.expected.stdout) в [общем G2 runner](../../examples/core/annotations-reflection/README.md), который интегрирован **только в авторскую ветку**, не в `main`. Отдельные 19 compiler-negative не добавлены в positive runner.

## 11. Адресная матрица 28/28

| ID | P | Раздел и evidence / граничный случай |
|---|---|---|
| RF-01 | P0 | §1: Foo.class vs obj.getClass, RF01 |
| RF-02 | P0 | §1: Class.forName(name,false,loader) vs initialization, RF17b/RF18/RF17c |
| RF-03 | P0 | §2: getMethods/getDeclaredMethods, RF02–04 |
| RF-04 | P0 | §2: getFields/getDeclaredFields, RF05–07 |
| RF-05 | P0 | §2: getConstructors/getDeclaredConstructors, RF08/13–15 |
| RF-06 | P0 | §2–3: finding private Method ≠ right to invoke, RF04/RF19 |
| RF-07 | P0 | §3: canAccess/trySetAccessible + RF_NEST counterexample, RF09/10/20 |
| RF-08 | P0 | §4: named JPMS exports vs opens, two real module-path runs JM01–03 |
| RF-09 | P0 | §5: Method.invoke conversions/receiver/result, RF12 |
| RF-10 | P0 | §5: Constructor.newInstance and access, RF13–15 |
| RF-11 | P0 | §5: InvocationTargetException.getCause, RF16 |
| RF-12 | P0 | §5: NoSuchMethodException runtime vs unhandled checked compiler negative |
| RF-13 | P0 | §3/8: IllegalAccessException vs InaccessibleObjectException |
| RF-14 | P0 | §6: getReturnType vs getGenericReturnType, RF22/23 |
| RF-15 | P0 | §6: Type/ParameterizedType is declaration metadata, RF24 |
| RF-16 | P0 | §1: primitive/array Class and component type, RF25–28 |
| RF-17 | P0 | §7: method order unspecified; explicit sorted RF32 |
| RF-18 | P0 | §8: JDK21 strong encapsulation String.value, RF30/31 |
| RF-19 | P1 | §7: Parameter/-parameters and synthetic/bridge — not independently run |
| RF-20 | P1 | §9: record/sealed reflection API, own courses elsewhere |
| RF-21 | P1 | §5: varargs/invoke conversions & receiver edge — beyond example NOT RUN |
| RF-22 | P1 | §9: declaredAnnotationsByType vs annotationsByType complex inheritance NOT RUN |
| RF-23 | P1 | §3–4/8: setAccessible vs trySetAccessible, specific cases observed, no full graph |
| RF-24 | P2 | §1/4: classloader identities, service loaders, JPMS full graph NOT RUN |
| RF-25 | P2 | §8: JIT/JMH/GC reflection performance internals NOT RUN |
| RF-26 | P2 | §8: agents/instrumentation/security historical access NOT RUN |
| RF-27 | P2 | §9: proxies/MethodHandles/VarHandles/framework integration NOT RUN |
| RF-28 | P2 | §9: concurrency/JMM/serialization independent topics NOT RUN |

## 12. Источники и пределы проверки

**Нормативные A (Java SE 21):** [Class](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html), [AccessibleObject](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AccessibleObject.html), [Field](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Field.html), [Method](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html), [Constructor](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Constructor.html), [Parameter](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Parameter.html), [Type/ParameterizedType](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Type.html), [InvocationTargetException](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/InvocationTargetException.html), [JLS §12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4), [§6.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6), [§7.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7). **Интервью C:** [Q-HRUDAY](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md), [Q-TEAMLEAD](https://github.com/teamlead/java-interview-questions/blob/main/README.md), [Q-CHERNYSHOV-RU](https://github.com/Shell26/Java-Developer/blob/master/core.md) — подтверждают постановку вопросов, **не** технические нормы или frequency.

**Опубликованный G2 runnable:** [обе Java21-программы и два самостоятельных JPMS-сценария](../../examples/core/annotations-reflection/README.md), [verify.sh](../../examples/core/annotations-reflection/verify.sh), два [реальных module descriptors exports-only](../../examples/core/annotations-reflection/jpms/exportonly/src/m.target/module-info.java) и [exports+opens](../../examples/core/annotations-reflection/jpms/opens/src/m.target/module-info.java). [Технический GitHub CI #45020360437](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45020360437) завершился **SUCCESS** на SHA `5c1bb8cbf2fc46d7c9229df9989ce73ca1f4a048` (3 компиляции, 4 JVM, 4 stdout diff); **итоговый авторский SHA должен иметь собственный подтверждённый CI**, см. [PR #172](https://github.com/Melchiorius/backend-interview-labs/pull/172). **NOT RUN:** отдельные 19 compiler-negative в общем CI, компиляция всех inline snippet, dynamic proxy/ClassLoader/Agent/JSR269, другие JPMS graphs, `--add-opens`, `javadoc`/`javap -v`, другие JDK/vendors/OS, Spring/AOP/JPA/ORM, JIT/GC/JMH, IDE/Obsidian GUI/anchors/HTTP, P2. Не переносить успех G1 и technical CI на непроверенные условия. Статусы [CURRICULUM](../../docs/CURRICULUM.md) и [COVERAGE](../../docs/COVERAGE.md) обновляются в G6 после G5, не здесь.
