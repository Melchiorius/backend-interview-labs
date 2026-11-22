# CORE-CLASSLOADER — определяющий загрузчик, идентичность класса и инициализация Java 21

**Практическая проблема.** Две библиотеки могут содержать байтово одинаковый `sample.Token.class`, с тем же именем, полями и методами, но приведение объектов может завершиться `ClassCastException`: **имя — только половина runtime identity**. Вторая половина — загрузчик, который *определил* класс. Это не erasure, не JPMS readability и не только текст package.

**Версия:** Java SE 21; нормы [JVMS §5.3](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3), [§5.3.2](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3.2), [JLS §12.2–12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.2), [ClassLoader API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html) и [Class API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html). Исторические исходники и доказательная трасса — [G1 §6.3–6.4](../../docs/plans/CORE-MODULES-CLASSLOADER.md). Теперь исходники `LoaderProbe`/`MissingProbe` действительно опубликованы в [G2 runnable](../../examples/core/modules-classloader/README.md); новая автоматическая проверка относится к точному SHA коммита, а не к одному лишь Markdown.

**Адресное покрытие:** CL-01–CL-16 = **16 P0**, CL-17–CL-22 = **6 P1**, CL-23–CL-26 = **4 P2**: **26/26**. Совместно с [CORE-MODULES](modules.md) — **53/53 (33 P0,12 P1,8 P2)**. Смежные статьи: [инициализация](initialization.md), [Reflection](reflection.md), [классы](classes.md), [исключения](exceptions.md), [стирание](erasure.md), [JPMS](modules.md).

## 1. Class, ClassLoader и binary name (CL-01, CL-05, CL-06)

`Class<?>` отражает конкретный runtime type. `ClassLoader` участвует в поиске и/или определении классов. У reference-класса есть binary name (например, `sample.Token`), но одинаковое имя не гарантирует один runtime type в разных namespaces. По [JVMS §5.3](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3) идентичность reference class/interface задаёт **пара binary name + defining loader**: `(sample.Token, loaderA)` не тот же тип, что `(sample.Token, loaderB)`, когда оба loader независимо определили один binary name.

~~~java
// sample/Token.java
package sample;
public class Token {
    public int value() { return 67; }
}
~~~

`Token.class` обращается к Class статически названного типа; `new Token()` создаёт объект; `Token.class.getClassLoader()` возвращает loader, определивший соответствующий Class согласно Javadoc. Это не произвольный «последний loader, через который кто-то искал имя».

## 2. Loading ≠ linking ≠ initialization (CL-02, CL-03, CL-04)

**Loading** находит/создаёт runtime class; **linking** включает verification, preparation, resolution (допускаются разные стратегии времени разрешения символов); **initialization** исполняет static initialization согласно JLS §12.4. Простое получение объекта Class не всегда запускает static block.

~~~java
package sample;
public class Lazy {
    static {
        System.out.println("CL06=initialized");
    }
}
~~~

В полном G1 `LoaderProbe.java`:

~~~java
ClassLoader app = LoaderProbe.class.getClassLoader();
Class<?> loaded = app.loadClass("sample.Lazy");
System.out.println("CL04=" + loaded.getName());
System.out.println("CL05=" + (Class.forName("sample.Lazy", false, app) == loaded));
System.out.println("CL07=" + Class.forName("sample.Lazy", true, app).getSimpleName());
~~~

**Реальный порядок:** `CL04=sample.Lazy`, `CL05=true`, затем `CL06=initialized` и `CL07=Lazy`. Почему `CL06` между CL05 и CL07? Третий вызов `Class.forName(...,true,...)` требует инициализации, поэтому статический блок печатает строку прежде внешнего `println`. `Class.forName(...,false,loader)` **не запрашивает** инициализацию; это не доказательство, что класс не мог быть инициализирован раньше по иной причине. `ClassLoader.loadClass(name)` не эквивалентен созданию экземпляра и гарантированному запуску static block. Повторный `forName(...,true,...)` для уже инициализированного того же Class не повторяет успешный static initializer. [JLS §12.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.1).

## 3. Bootstrap, platform и application без Java 8 легенды (CL-10, CL-11; CL-18 P1)

Java21 публично предоставляет `ClassLoader.getSystemClassLoader()` и `ClassLoader.getPlatformClassLoader()`; реальный `ClassLoader.getParent()` показывает связь конкретного объекта-загрузчика. Bootstrap loader не обязательно представлен обычным Java объектом: для bootstrap-defined `String` в G1 `String.class.getClassLoader()==null` → **CL01=true**. Это не значит, что String «не загружен».

В G1 также **CL02=true** для `LoaderProbe.class.getClassLoader()==getSystemClassLoader()` и **CL03=true** для `getPlatformClassLoader()!=null`. Это наблюдение конкретного OpenJDK21 процесса, а не универсальные class implementation names, заданные спецификацией. Java 8 extension loader, `rt.jar`, `lib/ext` и `java.ext.dirs` нельзя выдавать за действующую Java21 архитектуру; современный **platform class loader** — API, а не обещание старого extension mechanism. [ClassLoader API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html).

## 4. Два defining loader одного binary name и настоящий ClassCastException (CL-05–CL-07, CL-09, CL-20 P1)

Учебный `ChildFirstTokenLoader` перехватывает **только** `sample.Token`, остальные имена делегирует parent. Для этого имени делает собственный `defineClass`. Полный исходник и сборочные команды — [G1 §6.3](../../docs/plans/CORE-MODULES-CLASSLOADER.md):

~~~java
static final class ChildFirstTokenLoader extends ClassLoader {
    private final byte[] bytes;
    ChildFirstTokenLoader(byte[] bytes) {
        super(ClassLoader.getSystemClassLoader());
        this.bytes = bytes.clone();
    }
    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        if (!name.equals("sample.Token")) return super.loadClass(name, resolve);
        synchronized (getClassLoadingLock(name)) {
            Class<?> defined = findLoadedClass(name);
            if (defined == null) {
                defined = defineClass(name, bytes, 0, bytes.length);
            }
            if (resolve) resolveClass(defined);
            return defined;
        }
    }
}
~~~

Здесь реально читаются байты **уже скомпилированного** `sample/Token.class` через `Files.readAllBytes(Path.of(args[0]))`, а затем два новых loader определяют тип независимо:

~~~java
Class<?> a = new ChildFirstTokenLoader(bytes).loadClass("sample.Token");
Class<?> b = new ChildFirstTokenLoader(bytes).loadClass("sample.Token");

System.out.println(a.getName().equals(b.getName())); // true
System.out.println(a == b);                           // false
System.out.println(a.getClassLoader() != b.getClassLoader()); // true

try {
    b.cast(a.getConstructor().newInstance());
} catch (ClassCastException expected) {
    System.out.println("ClassCastException");
}
~~~

**G1 stdout:** `CL09=true` — строки одинаковы; `CL10=false` — два разных `Class`; `CL11=true` — два определяющих loader; `CL13=false` — экземпляр class `a` не является обычным `sample.Token` из application loader; **CL14=ClassCastException** при `b.cast(instanceOfA)`. Ошибка **runtime**, а не compiler-negative; байтовая идентичность не объединяет namespaces. Этот упрощённый loader не является production-ready security/plugin isolation, см. §5/11.

## 5. Parent delegation: initiating loader может не определять Class (CL-06, CL-08, CL-09, CL-20 P1)

Стандартная реализация `ClassLoader.loadClass` обычно сначала делегирует родителю (parent-first), а затем обращается к `findClass`. **Но это не абсолютная обязанность любого custom subclass:** §4 умышленно делает child-first для одного имени. Родительский loader всё ещё является родителем объекта, просто политика в этом отдельном случае меняется.

~~~java
ClassLoader app = LoaderProbe.class.getClassLoader();
ClassLoader delegating = new ClassLoader(app) {};
Class<?> shared = delegating.loadClass("sample.Token");
System.out.println(shared == sample.Token.class);   // true
System.out.println(shared.getClassLoader() == app); // true
~~~

**G1 CL19=true / CL20=true:** поиск инициирован через `delegating`, но возвращён родительский Class, определённый `app`. Это объясняет различие **initiating** и **defining** loader в JVMS; «вызывал метод на этом загрузчике» не значит «этот загрузчик определил тип». В child-first примере §4 наоборот этот объект сам вызвал `defineClass`. `CL17=true` подтвердил у него родителя app, что не помешало ему перехватить имя.

**NOT RUN** как отдельная дисциплина: параллельное выполнение loadClass несколькими потоками, loader deadlocks/locks, resource loading, package sealing/signatures, hot reload, unload и consistency в сложных графах. Строка `synchronized(getClassLoadingLock(name))` сама по себе не является доказательством полной production-безопасности.

## 6. TCCL — mutable context потока, не изменение Class identity (CL-12; CL-21 P1)

`Thread.currentThread().getContextClassLoader()` — контекст, который инфраструктурный код может использовать для поиска. Он не переписывает loader уже определённого Class.

~~~java
Thread thread = Thread.currentThread();
ClassLoader original = thread.getContextClassLoader();
ClassLoader candidate = a.getClassLoader();
try {
    thread.setContextClassLoader(candidate);
    System.out.println(thread.getContextClassLoader() == candidate); // true
    System.out.println(a.getClassLoader() == candidate);              // true
} finally {
    thread.setContextClassLoader(original);
}
~~~

В G1 **CL15=true** для начального TCCL == app и **CL16=true** после setter == loaderA. Первое — **не инвариант всех потоков/контейнеров/launchers**, второе — фактическая смена TCCL **без изменения `a==b`**. Настоящий поиск provider через ServiceLoader и контекстный загрузчик **не запускался**. [Thread API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html).

## 7. ClassNotFoundException vs NoClassDefFoundError: два разных runtime механизма (CL-13, CL-14, CL-15)

`ClassNotFoundException` — checked failure при явном lookup имени через `Class.forName`/`loadClass`. `NoClassDefFoundError` — Error при проблеме определения нужного типа во время загрузки/связывания; это не общий синоним CNFE.

~~~java
public class MissingProbe {
    public static void main(String[] args) {
        try {
            Class.forName("absent.DoesNotExist");
        } catch (ClassNotFoundException e) {
            System.out.println("CE01=ClassNotFoundException");
        }
        try {
            Class.forName("sample.Dependent", false,
                    MissingProbe.class.getClassLoader());
        } catch (NoClassDefFoundError e) {
            System.out.println("CE02=NoClassDefFoundError");
        } catch (ClassNotFoundException e) {
            System.out.println("CE02=unexpected-CNFE");
        }
    }
}
~~~

В этом реальном G1 кейсе **Dependent существует и сначала успешно скомпилирован** вместе с `Parent`:

~~~java
package sample;
public class Parent {}
~~~

~~~java
package sample;
public class Dependent extends Parent {}
~~~

Затем после компиляции код копирует output в **отдельный временный каталог**, где удаляет `Parent.class`, не удаляя исходники/нормальную сборку. Запуск `MissingProbe` через `java -cp classloader/bad MissingProbe` печатает **CE01=ClassNotFoundException** для имени отсутствующего top-level класса и **CE02=NoClassDefFoundError** для найденного Dependent с отсутствующим superclass. Это **две успешные JVM-программы с обработанными исключениями**, не два compiler-negative. [CNFE API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassNotFoundException.html), [NCDFE API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/NoClassDefFoundError.html).

**Контрпример compile-time:** записать `Class.forName("absent.DoesNotExist");` без `catch/throws` — static checked exception compiler-negative `CL_N01`; так же необработанный `loader.loadClass(name)` — `CL_N02`. Но обработанный G1 MissingProbe компилируется и завершается exit=0.

## 8. JPMS membership и ClassLoader identity независимы (CL-01, CL-12, CL-16)

В [CORE-MODULES §5](modules.md) классы `m.client` и `m.lib` принадлежат разным named modules, но в одном наблюдаемом графе **JM10=true** — у них одинаковый ClassLoader. У G1 LoaderProbe, запущенного через classpath, **CL21=true** означает `!LoaderProbe.class.getModule().isNamed()`, а **CL12=false** — child-defined Token тоже unnamed, хотя два Class не совпадают.

То есть «один модуль = один ClassLoader» неверно, и «unnamed = один runtime type для всех» тоже. JPMS регулирует readability/encapsulation; **defining loader + binary name** — идентичность типов. Реальный CL14 ClassCastException возник **без named modules**, а JPMS `JM_N01/03` являются **ошибками статической видимости**, не ClassCastException.

## 9. Три независимых compiler-negative и границы их интерпретации (CL-13–CL-15)

В G1/04 отдельно с `javac --release 21 -XDrawDiagnostics` проверены:

| Кейс | Смысл | Тематическая диагностика |
|---|---|---|
| `CL_N01_UnhandledForName` | `Class.forName(...)` без обработки checked exception | `compiler.err.unreported.exception.need.to.catch.or.throw` |
| `CL_N02_UnhandledLoadClass` | `loader.loadClass(...)` без обработки checked exception | `compiler.err.unreported.exception.need.to.catch.or.throw` |
| `CL_N03_OverrideFinalGetParent` | Переопределить final `ClassLoader.getParent()` | `compiler.err.override.meth` |

Итого исторически **3 CL + 14 JM = 17/17** отдельных compiler-negative G1/04, **вне общего CI**. Результаты CE01/CE02/CL14 являются **runtime observations**, а не ещё тремя отрицательными компиляциями.

## 10. Матрица покрытия CORE-CLASSLOADER 26/26

| ID | P | Секция / статус |
|---|---|---|
| CL-01 | P0 | §1/8: Class vs loader и runtime Type |
| CL-02 | P0 | §2: load/link/init |
| CL-03 | P0 | §2: forName(false/true) |
| CL-04 | P0 | §2: loadClass не запускает static block автоматически |
| CL-05 | P0 | §1/4: binary name + defining loader |
| CL-06 | P0 | §4/5: независимые child-first |
| CL-07 | P0 | §4: ClassCastException runtime |
| CL-08 | P0 | §5: initiating vs defining |
| CL-09 | P0 | §5: parent-first vs override |
| CL-10 | P0 | §3: bootstrap/null |
| CL-11 | P0 | §3: platform/system API |
| CL-12 | P0 | §6/8: TCCL и identity |
| CL-13 | P0 | §7/9: missing-name lookup CNFE |
| CL-14 | P0 | §7: missing superclass NCDFE |
| CL-15 | P0 | §7/9: checked vs Error |
| CL-16 | P0 | §8: module vs loader |
| CL-17 | P1 | §11: array loader/component type NOT RUN |
| CL-18 | P1 | §3: platform loader/TCCL environment-sensitive |
| CL-19 | P1 | §2/11: verifier/linking constraints NOT RUN |
| CL-20 | P1 | §4/5: limited child-first implementation |
| CL-21 | P1 | §6: TCCL/SPI discovery NOT RUN |
| CL-22 | P1 | §11: Class.forName(Module,String) NOT RUN |
| CL-23 | P2 | §5/11: locks/deadlock/parallel capable NOT RUN |
| CL-24 | P2 | §5/11: hot reload/unloading/metaspace/JIT/JMH NOT RUN |
| CL-25 | P2 | §11: instrumentation/agents/hidden classes NOT RUN |
| CL-26 | P2 | §2/11: JVMS resolution/loader constraints/JMM NOT RUN |

## 11. Нормы A, интервью C и ограничения

**Нормы Java21:** [JVMS §5.3](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3), [§5.3.2](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3.2), [JLS §12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.2), [§12.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.3), [§12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4), [ClassLoader API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html), [Class API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html), [Thread API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html), [NoClassDefFoundError](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/NoClassDefFoundError.html). Не подменять нормы поведением HotSpot одного процесса.

**Реальные вопросы C:** [Q-HRUDAY Chapter12](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md) — parent delegation, custom loaders, phases loading; [Q-TEAMLEAD](https://github.com/teamlead/java-interview-questions/blob/main/README.md) — «What is classloader», dynamic loading; [Q-CHERNYSHOV-RU](https://github.com/Shell26/Java-Developer/blob/master/core.md) — вопрос о загрузчиках/динамической загрузке; [Q-IVAN JPMS](https://github.com/ivanjayasooriya/java-interview-prep/blob/main/docs/17-modules-jpms.md) — модульная тема. Это **широкие вопросы**, не частотная статистика и не норматив. Для initiating/defining, TCCL setter и Class.forName(Module,String) в этих C самостоятельных подтверждённых вопросов **нет (GAP)**. Устаревшие ответы про Java8 extension loader/`rt.jar` не применять к Java21.

**G1/04 (исторически вне GitHub):** 18 уникальных Java21-исходников в четырёх компиляционных контекстах (7 base + 1 изменённый opened descriptor + 4 static + 6 classpath; два полных модульных варианта дают 24 физических файла), **4 чистых `javac`**, **6 JVM / 6 строгих stdout diff**, **49 строк** (11+11+2+2+21+2), не 49 JUnit-тестов. **17/17** compiler-negative с `-XDrawDiagnostics` — отдельное независимое G1/04 evidence, **не штатный positive CI**. После integration технического [PR #180](https://github.com/Melchiorius/backend-interview-labs/pull/180) уже существуют [шесть runnable-сценариев](../../examples/core/modules-classloader/README.md), [LoaderProbe.java](../../examples/core/modules-classloader/classloaders/src/LoaderProbe.java), [MissingProbe.java](../../examples/core/modules-classloader/classloaders/src/MissingProbe.java) и [verify.sh](../../examples/core/modules-classloader/verify.sh). [Технический CI #45029310487](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45029310487) — **completed/SUCCESS на technical SHA** `b7e7af3e8c44703cacf71e4f4e4343ecd60092e4`; **[итоговый интегрированный CI #45030280851](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45030280851) — completed/SUCCESS на другом, author SHA** `6eb170d8660a1f28fbf323198451faa84e68b040`. В каждом отдельно успешны **Gradle/JUnit** и позитивный Java21 runnable: **4 компиляции `javac --release 21 -Xlint:all` без warnings/stderr, 6 JVM и 6 строгих `diff -u`**; **49 stdout строк шести JVM, не 49 JUnit-тестов**. Все **17/17** compiler-negative G1/04 с `-XDrawDiagnostics` остались **за пределами** обоих positive CI; runtime `ClassCastException`/CNFE/NCDFE не являются неудачными компиляциями. Каждый CI подтверждает **только свой SHA**; для последующих изменений нужен собственный CI ([PR #179](https://github.com/Melchiorius/backend-interview-labs/pull/179)).

**P1 NOT RUN:** array loader по component type и primitive arrays (CL17); environment-dependent defaults (CL18); JVMS linking constraints (CL19); production custom child-first safety (CL20); ServiceLoader через TCCL (CL21); [Class.forName(Module,String)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#forName(java.lang.Module,java.lang.String)) (CL22). **P2 NOT RUN:** parallel-capable loader/locking/deadlocks, unloading/metaspace/hot reload, agents/hidden classes, JVM loading full course, JMH/JIT/GC/JMM. Не проверены все отдельные inline Java snippets, альтернативные JDK vendors/OS, IDE/Obsidian GUI и внешние HTTP/anchors. Не закрывайте CORE-SERIAL или JVM-LOADING этой статьёй.

[CORE-MODULES](modules.md) объясняет readability/exports/opens, а эта статья — defining loader и runtime identity. Статусы [COVERAGE](../../docs/COVERAGE.md) и [CURRICULUM](../../docs/CURRICULUM.md) меняются только отдельным G6 после G5; 14 malformed link labels G1 и N2 по link count — отдельный erratum, **принятый G1-план не изменяется**.
