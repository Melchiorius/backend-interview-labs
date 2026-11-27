# CORE-CLASSLOADER — определяющий загрузчик, идентичность класса и инициализация Java 21

**Практическая проблема.** Две библиотеки могут содержать байтово одинаковый `sample.Token.class`, с тем же именем, полями и методами, но приведение объектов может завершиться `ClassCastException`: **имя — только половина runtime identity**. Вторая половина — загрузчик, который *определил* класс. Это не erasure, не JPMS readability и не только текст package.

**Версия:** Java SE 21; нормы [JVMS §5.3](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3), [§5.3.2](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3.2), [JLS §12.2–12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.2), [ClassLoader API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html) и [Class API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html). Исторические исходники и доказательная трасса — [G1 §6.3–6.4](../../docs/plans/CORE-MODULES-CLASSLOADER.md). Теперь исходники `LoaderProbe`/`MissingProbe` действительно опубликованы в [G2 runnable](../../examples/core/modules-classloader/README.md); новая автоматическая проверка относится к точному SHA коммита, а не к одному лишь Markdown.

**Как воспроизвести примеры.** Из корня клонированного репозитория при установленном JDK 21 и Bash выполните `bash examples/core/modules-classloader/verify.sh`. README и G1 по ссылкам выше содержат дерево исходников и эталоны. Исторические `sample/Token.java`, `sample/Lazy.java`, `sample/Parent.java`, `sample/Dependent.java` — отдельные файлы в пакете `sample`; `LoaderProbe` и `MissingProbe` — отдельные классы с main. `ChildFirstTokenLoader` — вложенный класс LoaderProbe вне main, остальные операторы демонстрации выполняются в main, байты берутся из скомпилированного `sample/Token.class`, путь к которому передаётся аргументом. Новые фрагменты §5–7/11 относятся к другой временной программе Probe (Token без package); используйте её полные исходники и команды по ссылке в §5, а не объединяйте все блоки статьи в один файл.

**Адресное покрытие:** CL-01–CL-16 = **16 P0**, CL-17–CL-22 = **6 P1**, CL-23–CL-26 = **4 P2**: **26/26**. Совместно с [CORE-MODULES](modules.md) — **53/53 (33 P0,12 P1,8 P2)**. Смежные статьи: [инициализация](initialization.md), [Reflection](reflection.md), [классы](classes.md), [исключения](exceptions.md), [стирание](erasure.md), [JPMS](modules.md). Эти числа показывают навигационное соответствие идентификаторам плана, а не итоговую проверенность ответов или завершённость независимого аудита.

## 1. Class, ClassLoader и binary name (CL-01, CL-05, CL-06)

`Class<?>` отражает конкретный runtime type. `ClassLoader` участвует в поиске и/или определении классов. У обычного класса или интерфейса (не массива и не hidden class) есть binary name (например, `sample.Token`), но одинаковое имя не гарантирует один runtime type в разных namespaces. По [JVMS §5.3](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3) идентичность такого класса или интерфейса задаёт **пара binary name + defining loader**: `(sample.Token, loaderA)` не тот же тип, что `(sample.Token, loaderB)`, когда оба loader независимо определили один binary name.

Примитивы и `void` также представлены объектами `Class`, но не определяются пользовательским загрузчиком. Классы массивов создаёт JVM (§11); hidden class, созданный через `Lookup.defineHiddenClass`, нельзя обнаружить обычным поиском по binary name. Runtime package определяется именем пакета и defining loader: одинаковый текст package в двух загрузчиках не даёт взаимного package-private доступа. У каждого загрузчика свой unnamed module. Общий интерфейс плагина обычно должен определяться общим родителем, иначе одинаковый текст интерфейса не гарантирует совместимости. Это нормативное объяснение, не эксперимент с package-private доступом. См. [JVMS §5.3](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3).

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

Verification проверяет структуру class-файла, байткод и типовую безопасность; preparation создаёт статические поля со значениями по умолчанию, не выполняя Java-инициализаторы. Значения constant variables из атрибута `ConstantValue` устанавливаются в ходе инициализации до `<clinit>` (JVMS §5.5); компилятор также может встроить константу в код клиента. Resolution связывает символические ссылки с конкретными сущностями; допустимо раннее или ленивое разрешение, не обязательно всё откладывается до первого использования.

Loading constraints нужны при пересечении пространств имён: например, дескриптор метода общего API, принимающего `sample.Token`, должен обозначать согласованный runtime type в участвующих загрузчиках. Независимые одноимённые типы допустимы, но при наложении ограничения JVM требует их тождества. Нарушение — `LinkageError`, не обычный checked CNFE. [JVMS §5.3.4](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3.4), [§5.4](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.4), [§5.5](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.5), [инициализация](initialization.md). Эксперимент с нарушением constraint не выполнялся; JVM-LOADING — отдельная будущая тема.

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

Bootstrap загружает фундаментальные классы платформы, platform loader — классы платформы, в том числе не определённые bootstrap; system loader обычно загружает приложение, но может быть пользовательским (`java.system.class.loader`). Схема application → platform → bootstrap показывает типичный путь делегации, а не универсальное дерево JPMS: встроенные загрузчики могут обращаться к другим загрузчикам при поиске классов модулей. У стандартного ClassLoader с null parent поиск идёт через bootstrap, что не гарантирует видимости platform-defined классов. См. [JPMS](modules.md).

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

Стандартный `loadClass(name, resolve)` сначала вызывает `findLoadedClass(name)`, затем parent.loadClass (bootstrap при null parent), при отсутствии — `findClass`, при успехе и resolve=true — `resolveClass`. Одноаргументный loadClass передаёт false. findLoadedClass — не только кеш собственных определений. Обычно переопределяют findClass, чтобы сохранить делегацию. **Но это не абсолютная обязанность любого custom subclass:** §4 умышленно делает child-first для одного имени. Родительский loader всё ещё является родителем объекта, просто политика в этом отдельном случае меняется.

~~~java
ClassLoader app = LoaderProbe.class.getClassLoader();
ClassLoader delegating = new ClassLoader(app) {};
Class<?> shared = delegating.loadClass("sample.Token");
System.out.println(shared == sample.Token.class);   // true
System.out.println(shared.getClassLoader() == app); // true
~~~

**Исторические stdout CL19=true / CL20=true:** прямой вызов на delegating вернул тот же родительский Class, определённый app. Это доказывает делегированную идентичность, **не регистрацию initiating loader**. Исходное требование CL-08 не равно метке stdout CL08 или CL19/20.

Различайте Java-объект, на котором вызван метод поиска, defining loader и initiating loader, зарегистрированный JVM для имени и класса. JVMS §5.3.2 описывает регистрацию при загрузке по запросу JVM; промежуточные делегирующие загрузчики не регистрируются автоматически. [findLoadedClass API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html#findLoadedClass(java.lang.String)) проверяет регистрацию.

Новая проба автора 02: два свежих делегирующих загрузчика одного родителя. Token.java содержит `public class Token {}` в default package, компилируется рядом с Probe. [Полные фактически использованные исходники и команды 02](https://github.com/Melchiorius/backend-interview-labs/issues/348#issuecomment-7222438659) позволяют повторить эксперимент в отдельном временном каталоге. Вложенные Delegating, Own, Broken и Lazy объявляются внутри Probe вне main; операторы выполняются в `main(String[] args) throws Exception`. Own.define читает через вызывающий код байты скомпилированного Token.class (`Files.readAllBytes(Path.of(args[0]))`) и определяет класс; используйте свежие загрузчики и переменные именно этой программы. Имена a/b исторического LoaderProbe не следует смешивать с переменными новых массивов. Ниже упрощённый вывод значений, тогда как полная программа проверяет условия и печатает именованные результаты только после успешной проверки. Вложенный класс Probe и фрагмент main:

~~~java
static class Delegating extends ClassLoader {
    Delegating(ClassLoader parent) { super(parent); }
    Class<?> recorded(String name) { return findLoadedClass(name); }
}
~~~

~~~java
ClassLoader app = Probe.class.getClassLoader();
String name = "Token";
Class<?> parent = Class.forName(name, false, app);
Delegating direct = new Delegating(app), vm = new Delegating(app);
System.out.println(direct.recorded(name) == null); // true
Class<?> d = direct.loadClass(name);
System.out.println(d == parent && d.getClassLoader() == app); // true
System.out.println(direct.recorded(name) == null); // true
System.out.println(vm.recorded(name) == null); // true
Class<?> v = Class.forName(name, false, vm);
System.out.println(v == parent && v.getClassLoader() == app); // true
System.out.println(vm.recorded(name) == parent); // true
~~~

Фактическая проба 02 проверяла эти условия с метками и AssertionError при расхождении: direct.before.empty, direct.parent.identity, direct.after.empty, vm.before.empty, vm.parent.identity, vm.after.recorded, loaders.distinct — все true. В этом опыте прямой вызов вернул Class без регистрации direct; запрос через JVM зарегистрировал vm. Defining loader остался app. Это не значит, что любой прямой loadClass исключает регистрацию: при собственном defineClass загрузчик становится defining и initiating.

Исторический CL17 подтвердил parent app у child-first. Новый Probe с отдельным Own loader, вызывающим defineClass для байтов Token, подтвердил vm.repeat=true, own.identity=true, own.repeat=true: повторный lookup возвращает прежний Class; повторный define тех же байтов в том же loader дал duplicate=LinkageError. Один общий API должен определяться общим загрузчиком; упрощённый пример не доказывает production security или concurrency.

**Независимая проверка 03.** В [отдельном эксперименте Registration/sample.Value](https://github.com/Melchiorius/backend-interview-labs/pull/349#issuecomment-7222597083) три свежих загрузчика имели одного родителя; до поиска у всех отсутствовала запись findLoadedClass. Прямой loadClass вернул родительский Class без записи; Class.forName через другой загрузчик вернул тот же Class и зарегистрировал его, при этом direct остался незарегистрированным даже после VM-вызова. Собственный defineClass третьего загрузчика дал отличный Class, собственный defining loader, запись и тот же Class при повторном loadClass. В выводе before/after обозначают наличие записи, same — тождество родительскому Class. Это отдельная ограниченная проверка, не универсальная невозможность регистрации при прямом вызове, не повтор всех проб 02 и не историческая метка CL08.

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

В G1 CL15=true для исходного TCCL == app и CL16=true после setter == loaderA. CL16 проверяет только setter, а не повторное a==b. Исторический runner восстанавливает app (наблюдавшееся начальное значение); переиспользуемый код восстанавливает сохранённый original в finally.

По умолчанию контекст наследуется от создающего потока, но может быть переустановлен. TCCL используется только явно выбирающими его API; одноаргументный Class.forName использует defining loader вызывающего класса, app.loadClass — политику app. Null context трактуется конкретным API, например ServiceLoader при null loader использует system loader или bootstrap при его отсутствии. [Thread API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html). В новом Probe обе формы поиска Token до и после установки TCCL provider вернули parent (tccl.before.lookup=true, tccl.after.lookup=true); после установки TCCL на Own, определивший другой Token, Class.forName(name) и app.loadClass(name) всё ещё вернули parent, own.loadClass(name) — child (tccl.own.lookup=true). Исходный TCCL восстановлен.

**ServiceLoader — реальный потребитель TCCL.** В classpath-пробе общий api.Svc в app, реализация impl.Provider и ресурс META-INF/services/api.Svc только в отдельном provider-каталоге дочернего URLClassLoader. В ресурсе одна строка impl.Provider. ServiceLoader.load(service) выбирает текущий TCCL в момент создания ServiceLoader, а discovery и инстанцирование происходят лениво при обходе/findFirst; явная перегрузка принимает loader. [ServiceLoader API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html#load(java.lang.Class)).

~~~java
// api/Svc.java
package api;
public interface Svc { int value(); }
~~~
~~~java
// impl/Provider.java
package impl;
public class Provider implements api.Svc {
    public int value() { return 67; }
}
~~~

Фрагмент main (imports java.net.URL, java.net.URLClassLoader, java.nio.file.Path, java.util.ServiceLoader; provider не включён в app classpath). В структуре из полного приложения 02 сохраните интерфейс как `src/api/Svc.java`, реализацию как `provider-src/impl/Provider.java`. Компиляция приложения помещает классы в `app`; отдельно компилируйте Provider с `-cp app -d provider`. В `provider/META-INF/services/api.Svc` запишите `impl.Provider` и LF. Из рабочего каталога `/tmp/a21b` или эквивалентной структуры с каталогами app и provider выполните `java -cp app Probe app/Token.class provider`; точные команды создания и компиляции приведены в [приложении 02](https://github.com/Melchiorius/backend-interview-labs/issues/348#issuecomment-7222438659). Фрагмент ниже использует фиксированный относительный путь `provider`, полный Probe получает его аргументом. Не запускайте этот фрагмент как самостоятельный Java-файл:

~~~java
Thread thread = Thread.currentThread();
ClassLoader original = thread.getContextClassLoader();
ClassLoader app = Probe.class.getClassLoader();
try (URLClassLoader provider = new URLClassLoader(
        new URL[]{Path.of("provider").toUri().toURL()}, app)) {
    try {
        thread.setContextClassLoader(app);
        ServiceLoader<api.Svc> before = ServiceLoader.load(api.Svc.class);
        thread.setContextClassLoader(provider);
        System.out.println(before.iterator().hasNext()); // false
        System.out.println(ServiceLoader.load(api.Svc.class, app)
                .iterator().hasNext()); // false
        System.out.println(ServiceLoader.load(api.Svc.class)
                .findFirst().orElseThrow().value()); // 67
        System.out.println(ServiceLoader.load(api.Svc.class, provider)
                .findFirst().orElseThrow().value()); // 67
    } finally { thread.setContextClassLoader(original); }
}
~~~

Фактический Probe подтвердил tccl.provider.absent.from.app, tccl.explicit.parent.empty, tccl.provider (значение 67 и defining loader provider), tccl.explicit.child, tccl.restored — все true. Не кешируйте один ServiceLoader глобально между приложениями: он сохраняет контекст поиска и кеш поставщиков. Это classpath SPI, отличный от named-module SPI из [CORE-MODULES](modules.md); не тест утечек или потокобезопасности.

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

Затем после компиляции код копирует output в **отдельный временный каталог**, где удаляет `Parent.class`, не удаляя исходники/нормальную сборку. Запуск `MissingProbe` через `java -cp classloader/bad MissingProbe` печатает **CE01=ClassNotFoundException** для имени отсутствующего top-level класса и **CE02=NoClassDefFoundError** для найденного Dependent с отсутствующим superclass. Это **один успешный JVM-процесс с двумя обработанными исключениями**, не два compiler-negative. [CNFE API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassNotFoundException.html), [NCDFE API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/NoClassDefFoundError.html).

При определённых загрузочных запросах JVM преобразует CNFE отсутствующей зависимости в NCDFE с причиной CNFE (JVMS §5.3.2). Но NCDFE возможен и при существующем class-файле: после неудачной инициализации Class становится ошибочным. По [JLS §12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2) первое исключение, не являющееся Error, обычно оборачивается в ExceptionInInitializerError; исходный Error может передаваться напрямую. Повторная попытка инициализации того же Class приводит к NCDFE.

~~~java
static class Broken {
    static final int VALUE = fail();
    static int fail() { throw new IllegalStateException("first failure"); }
}
~~~

~~~java
for (int i = 0; i < 2; i++) {
    try { System.out.println(Broken.VALUE); throw new AssertionError("broken"); }
    catch (LinkageError e) {
        System.out.println("broken." + i + "=" + e.getClass().getSimpleName()
                + ":" + (e.getCause() == null ? "null" : e.getCause().getClass().getSimpleName()));
    }
}
~~~

Фактически: broken.0=ExceptionInInitializerError:IllegalStateException, затем broken.1=NoClassDefFoundError:ExceptionInInitializerError. Вторая cause-цепочка — наблюдение данной JVM. В диагностике ищите первую ошибку и причину, а не только последний NCDFE. Перехват LinkageError ограничен демонстрацией, не совет продолжать после Error.

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

Матрица — указатель на объяснения по 26 идентификаторам плана, а не вердикт о полноте или окончательных dispositions.

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
| CL-17 | P1 | §11: array loader/component type, фактическая проба |
| CL-18 | P1 | §3: platform loader/TCCL environment-sensitive |
| CL-19 | P1 | §2: linking constraints, отдельный эксперимент NOT RUN |
| CL-20 | P1 | §4/5: limited child-first implementation |
| CL-21 | P1 | §6: TCCL/SPI discovery, фактическая проба |
| CL-22 | P1 | §11: Class.forName(Module,String), фактическая проба |
| CL-23 | P2 | §5/11: locks/deadlock/parallel capable NOT RUN |
| CL-24 | P2 | §5/11: hot reload/unloading/metaspace/JIT/JMH NOT RUN |
| CL-25 | P2 | §11: instrumentation/agents/hidden classes NOT RUN |
| CL-26 | P2 | §2/11: JVMS resolution/loader constraints/JMM NOT RUN |

## 11. Нормы A, интервью C и ограничения

### Массивы (CL-17)

Класс массива создаёт JVM; defining loader reference-массива совпадает с defining loader компонента, рекурсивно для многомерных массивов. У массивов примитивов bootstrap identity, getClassLoader()==null. Тип определяется элементом и размерностью, не длиной. [JVMS §5.3.3](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3.3). Фрагмент Probe с двумя независимо определёнными Token: Для этого фрагмента импортируйте `java.lang.reflect.Array`; parent и child — два Class объекта из новой пробы §5, app и own — их определяющие загрузчики (см. полный Probe 02). Здесь a/b обозначают Class массивов, а не исторические Class объектов Token.

~~~java
Class<?> a = Array.newInstance(parent, 0).getClass();
Class<?> b = Array.newInstance(child, 0).getClass();
Class<?> bb = Array.newInstance(child, 0, 0).getClass();
~~~

Проверены a != b, loader a == app и b == own; bb != b, bb.getComponentType()==b и loader bb==own; у int[].class и int[][].class loader null и компонент второго int[].class. Фактические array.identity, array.dimensions, primitive.arrays — true. Это не полная матрица covariance.

### Class.forName(Module,String) (CL-22)

Модульная перегрузка ищет binary name в заданном модуле, возвращает null при отсутствии или принадлежности найденного класса другому модулю. Не связывает и не инициализирует этим вызовом, не проверяет доступность класса вызывающему коду, не поддерживает имена массивов. Это не обещание отсутствия любых исключений (например, LinkageError или NullPointerException). Перегрузка name/initialize/loader при отсутствии бросает CNFE. [Class API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#forName(java.lang.Module,java.lang.String)).

В Probe вложенный `static class Lazy { static { System.out.println("module:init"); } }`. Class.forName(Object.class.getModule(), "Probe$Lazy") и Class.forName(Probe.class.getModule(), "absent.Nope") вернули null; Class.forName(Probe.class.getModule(), "Probe$Lazy") вернул Class без initializer. Class.forName("absent.Nope", false, app) дал CNFE. Затем Class.forName("Probe$Lazy", true, app) запустил initializer:

~~~text
module.wrong=true
module.absent=true
module.found=true
lookup=CNFE
module:before-explicit-init
module:init
module:after-explicit-init
~~~

Проверены unnamed module приложения и отрицательный поиск в java.base; custom layer и случай двух named modules одного loader отдельно не проверялись.

**Нормы Java21:** [JVMS §5.3](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3), [§5.3.2](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3.2), [JLS §12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.2), [§12.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.3), [§12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4), [ClassLoader API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html), [Class API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html), [Thread API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html), [NoClassDefFoundError](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/NoClassDefFoundError.html). Не подменять нормы поведением HotSpot одного процесса.

**Реальные вопросы C:** [Q-HRUDAY Chapter12](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md) — parent delegation, custom loaders, phases loading; [Q-TEAMLEAD](https://github.com/teamlead/java-interview-questions/blob/main/README.md) — «What is classloader», dynamic loading; [Q-CHERNYSHOV-RU](https://github.com/Shell26/Java-Developer/blob/master/core.md) — вопрос о загрузчиках/динамической загрузке; [Q-IVAN JPMS](https://github.com/ivanjayasooriya/java-interview-prep/blob/main/docs/17-modules-jpms.md) — модульная тема. Это **широкие вопросы**, не частотная статистика и не норматив. Для initiating/defining, TCCL setter и Class.forName(Module,String) в этих C самостоятельных подтверждённых вопросов **нет (GAP)**. Устаревшие ответы про Java8 extension loader/`rt.jar` не применять к Java21.

**G1/04 (исторически вне GitHub):** 18 уникальных Java21-исходников в четырёх компиляционных контекстах (7 base + 1 изменённый opened descriptor + 4 static + 6 classpath; два полных модульных варианта дают 24 физических файла), **4 чистых `javac`**, **6 JVM / 6 строгих stdout diff**, **49 строк** (11+11+2+2+21+2), не 49 JUnit-тестов. **17/17** compiler-negative с `-XDrawDiagnostics` — отдельное независимое G1/04 evidence, **не штатный positive CI**. После integration технического [PR #180](https://github.com/Melchiorius/backend-interview-labs/pull/180) уже существуют [шесть runnable-сценариев](../../examples/core/modules-classloader/README.md), [LoaderProbe.java](../../examples/core/modules-classloader/classloaders/src/LoaderProbe.java), [MissingProbe.java](../../examples/core/modules-classloader/classloaders/src/MissingProbe.java) и [verify.sh](../../examples/core/modules-classloader/verify.sh). [Технический CI #45029310487](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45029310487) — **completed/SUCCESS на technical SHA** `b7e7af3e8c44703cacf71e4f4e4343ecd60092e4`; **[итоговый интегрированный CI #45030280851](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45030280851) — completed/SUCCESS на другом, author SHA** `6eb170d8660a1f28fbf323198451faa84e68b040`. В каждом отдельно успешны **Gradle/JUnit** и позитивный Java21 runnable: **4 компиляции `javac --release 21 -Xlint:all` без warnings/stderr, 6 JVM и 6 строгих `diff -u`**; **49 stdout строк шести JVM, не 49 JUnit-тестов**. Все **17/17** compiler-negative G1/04 с `-XDrawDiagnostics` остались **за пределами** обоих positive CI; runtime `ClassCastException`/CNFE/NCDFE не являются неудачными компиляциями. Каждый CI подтверждает **только свой SHA**; для последующих изменений нужен собственный CI ([PR #179](https://github.com/Melchiorius/backend-interview-labs/pull/179)). Точные исторические хеши шести эталонных потоков указаны в [G1, строки 505–510](https://github.com/Melchiorius/backend-interview-labs/blob/2bfd950caac8fba900094a969dd060682c0bc921/docs/plans/CORE-MODULES-CLASSLOADER.md#L505-L510). Эти 17 отрицательных компиляций относятся к исторической проверке, не к новым временным пробам и не к 24 ожидаемым отклонениям всей последующей CI-сборки.

**Исторически P1 NOT RUN** включало массивы, SPI и модульную перегрузку; новые ограниченные пробы выше не подменяют историческое evidence. По-прежнему не проверены отдельное нарушение loading constraint, все environment-dependent defaults, production custom loader safety. **P2 NOT RUN:** parallel-capable loader/locking/deadlocks, unloading/metaspace/hot reload, agents/hidden classes, JVM loading full course, JMH/JIT/GC/JMM. Не проверены все inline snippets, альтернативные JDK/OS, IDE/Obsidian GUI и внешние HTTP/anchors.

### Диагностическое упражнение

Два плагина определили одноимённый Token; после смены TCCL приведение объекта к Token приложения не прошло. Второй класс присутствует в JAR, но сначала дал ExceptionInInitializerError, затем NCDFE. Третий найденный класс не загружается из-за отсутствующего superclass. Что проверять?

**Разбор:** сравнить реальные Class, getClassLoader и getModule, не только имя. TCCL не меняет defining loader уже созданного объекта; общий API должен быть определён общим родителем. CNFE при явном поиске отсутствующего имени отличать от NCDFE при загрузке зависимости и NCDFE при повторной ошибочной инициализации. Искать первый throwable и cause; наличие class-файла не гарантирует наличие зависимостей и успешную инициализацию. При отсутствии SPI provider проверить TCCL в момент создания ServiceLoader, явно выбранный loader, ресурс и общий тип сервиса.

### Воспроизводимость новых проб

Полные временные исходники и команды в адресном отчёте [#348](https://github.com/Melchiorius/backend-interview-labs/issues/348), непосредственно в [приложении 02](https://github.com/Melchiorius/backend-interview-labs/issues/348#issuecomment-7222438659). Фактическая проверка 02: OpenJDK 21.0.12.1 Debian 13, две компиляции `javac --release 21 -encoding UTF-8 -Xlint:all -Werror`, обе exit 0, stdout/stderr пусты. Provider компилируется с `-cp app -d provider`, его каталог отсутствует в app classpath. Команда `java -cp app Probe app/Token.class provider` запущена шесть раз: исходная среда (default en/US, default charset UTF-8, stdout US-ASCII, затем явные JVM en/US и tr/TR), затем `LC_ALL=C.utf8` (default en/empty, default charset и stdout UTF-8, затем явные JVM `-Duser.language=en -Duser.country=US` и tr/TR). Все exit 0, stderr пуст; 32 строки/777 байт в каждом из первых трёх запусков, 32 строки/772, 774, 774 байта соответственно в UTF-8. Последняя строка сообщает наблюдаемые locale и charset, не универсальные defaults и не наличие системных локалей.

Фактическая независимая проверка [03](https://github.com/Melchiorius/backend-interview-labs/pull/349#issuecomment-7222597083): отдельные Registration и sample.Value, одна строгая компиляция `javac --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror` exit 0, stdout/stderr пусты; три JVM exit 0, stderr пуст, по 4 строки/227 байт. Фактические default en/US и явные JVM en/US, tr/TR; в каждом запуске проверены default charset, file.encoding и stdout charset UTF-8. Точные исходники, команды, вывод и хеши в отчёте 03. Новые отрицательные компиляции не требовались; это 1+3 команды для CL08, а не 2+6 команд более широкой проверки 02.

Отдельные проверки неизменённого shared runner: 02 дважды выполняла `LC_ALL=C.utf8 bash verify.sh` на 31 восстановленном входе (24 Java-файла, 6 fixtures, verify.sh; Git blob SHA совпали), не полный клон. Независимая 03 восстановила те же 31 вход и выполнила runner в default POSIX и `LC_ALL=C.UTF-8`; инвентаризация path/blob/mode до и после совпала (`cmp` exit 0), шесть измеренных хешей эталонных файлов совпали с [G1, строки 505–510](https://github.com/Melchiorius/backend-interview-labs/blob/2bfd950caac8fba900094a969dd060682c0bc921/docs/plans/CORE-MODULES-CLASSLOADER.md#L505-L510). Это хеши ожидаемых байтов, не отдельно сохранённых фактических stdout каждого JVM: временные actuals удаляются trap, шесть `diff -u` подтверждают равенство эталонам. Все четыре запуска runner exit 0, внешние stdout одна строка marker (104 байта), stderr пуст; 03 также проверила stdout `cmp` exit 0. По отчётам 02/03 общий SHA-256 marker `2f40eb643683b422fe9e7efe1392aa7424ea44f948d828189cbcbc4f00b1dfb8`. Скрипт использует `-Xlint:all`, отклоняет ненулевой exit и непустой compiler/JVM stderr, не содержит собственного Werror или проверки хешей. Четыре компиляции, шесть JVM и шесть diff сверяют 49 внутренних stdout наблюдений, не печатают их; внешний marker один, не 49 строк CI и не 49 JUnit-тестов. Полные OS locale en_US/tr_TR отсутствовали, runner в них NOT RUN; отдельные JVM-флаги проверены в пробах, JAVA_TOOL_OPTIONS с banner в strict-stderr runner не вводилась. Исторические 17 negatives, новые команды 02 (2+6), 03 (1+3) и 24 EXPECTED REJECTION всей CI-сборки — разные наборы доказательств. Временные пробы не входят в CI runner. В этой редакционной правке новые локальные запуски не выполнялись. Полный clone, GUI, package access, concurrency и другие JDK/OS не проверены.

[CORE-MODULES](modules.md) объясняет readability/exports/opens, а эта статья — defining loader и runtime identity. Для навигации по учебным темам и вопросам используйте [COVERAGE](../../docs/COVERAGE.md) и [CURRICULUM](../../docs/CURRICULUM.md).
