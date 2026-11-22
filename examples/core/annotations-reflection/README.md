# CORE-ANNOTATIONS + CORE-REFLECTION — Java 21 runnable с настоящим JPMS

Это воспроизводимый комплект для двух самостоятельных статей: [Annotations](../../../knowledge/01-java-core/annotations.md) и [Reflection](../../../knowledge/01-java-core/reflection.md). Все positive Java-программы и stdout-эталоны взяты из [принятого G1, §6](../../../docs/plans/CORE-ANNOTATIONS-REFLECTION.md) без изменения исполняемой логики. В JPMS используются настоящие `module-info.java` и два named modules, а не псевдопример на classpath.

**Всего 23+34+3+3=63 подписанные строки stdout из четырёх отдельных JVM-процессов — не 63 unit/JUnit-теста.** Отдельного Gradle/JUnit/Mockito-модуля нет.

## Команда для самостоятельного запуска

Требуются JDK 21 (`java`, `javac`), Bash, `diff`, `mktemp`. Из корня репозитория:

~~~bash
bash examples/core/annotations-reflection/verify.sh
~~~

Скрипт создаёт временный каталог `mktemp -d` с `trap` на удаление. Он **трижды отдельно компилирует** исходники с `javac --release 21 -Xlint:all`: сначала два обычных source в `classes/`, затем независимо оба named-module trees в `mods.exportonly/` и `mods.opens/` через `--module-source-path`. Ошибки и любой stderr компилятора (включая warning) запрещены. Затем выполняются **четыре отдельные JVM**, две с `java -cp` и две с `java --module-path`; для каждого процесса требуется exit=0 и пустой stderr. Четыре полных stdout побайтово сравниваются с эталонами через **четыре `diff -u`**, после чего temp directory удаляется. `.class` и actual stdout в дерево репозитория не попадают.

| Что проверяется | Исходники | Точный эталон |
| --- | --- | --- |
| AN01–AN23 (23 строки) | [src/AnnotationsProbe.java](src/AnnotationsProbe.java) | [annotations.expected.stdout](annotations.expected.stdout) |
| RF01–RF32, включая RF17b/RF17c (34 строки) | [src/ReflectionProbe.java](src/ReflectionProbe.java), top-level `ReflectTarget` | [reflection.expected.stdout](reflection.expected.stdout) |
| JPMS `exports` (3 строки) | [m.target/module-info.java](jpms/exportonly/src/m.target/module-info.java), [api/Vault.java](jpms/exportonly/src/m.target/api/Vault.java), [m.client/module-info.java](jpms/exportonly/src/m.client/module-info.java), [client/Inspect.java](jpms/exportonly/src/m.client/client/Inspect.java) | [exportonly/expected.stdout](jpms/exportonly/expected.stdout) |
| JPMS `exports + opens` (3 строки) | [m.target/module-info.java](jpms/opens/src/m.target/module-info.java), [api/Vault.java](jpms/opens/src/m.target/api/Vault.java), [m.client/module-info.java](jpms/opens/src/m.client/module-info.java), [client/Inspect.java](jpms/opens/src/m.client/client/Inspect.java) | [opens/expected.stdout](jpms/opens/expected.stdout) |

## Что показывает AnnotationsProbe

- **AN01/AN02=parent, AN03/AN04=true:** `@Inherited` распространяется на Class-наследника через superclass, но не делает аннотацию *объявленной* на нём и не наследуется с interface на implementor.
- **AN05=true, AN06/AN07=2, AN08=true:** `@Repeatable` хранится с контейнером `Tags`; `getDeclaredAnnotationsByType(Tag.class)` раскрывает два экземпляра, в отличие от одиночного `getDeclaredAnnotation(Tag.class)`.
- **AN09/AN10=true:** SOURCE/CLASS retention недоступны через runtime `getAnnotation`. Это **не опыт с `javap -v`** и не запуск JSR 269.
- **AN11–AN14:** значения `@Details` по умолчанию — `default`, `6:7`, `String`, `HIGH`. **AN15=method**, **AN16=true**: `@Override` не является runtime-аннотацией. **AN17/AN18=true**: type-use `@TypeMark` на типе поля есть в `Field.getAnnotatedType()`, а не `Field.getAnnotation()`.
- **AN19=68** — лямбда функционального интерфейса, **AN20–AN23** — классовая аннотация, inherited/declared repeatable и `annotationType()`. `@Documented` используется в источнике, но `javadoc` runner **не запускает**.

## Что показывает ReflectionProbe

- **RF01–RF08:** `Foo.class`/`obj.getClass()`, `getMethods/getFields` (public/inherited) vs `getDeclaredMethods/getDeclaredFields` (declared включая private), видимость конструкторов. Это различия условий поиска, а не просто более длинные имена методов.
- **RF09=false / RF10=true / RF11=67:** `ReflectTarget` — **отдельный top-level тип, НЕ nestmate `ReflectionProbe`**. Первоначальный private access невозможен; в этом *unnamed module* `trySetAccessible()` допускает последующий `Field.getInt`. Перенос `ReflectTarget` во вложенный тип меняет учебный контракт.
- **RF12=72, RF13=68, RF14=true, RF15=1:** `Method.invoke` и вызовы public/private конструкторов через `Constructor.newInstance`. **RF16=IllegalArgumentException:boom:** исключение целевого метода обёрнуто в `InvocationTargetException`, вызывающий читает `getCause()`. **RF17=NoSuchMethodException** — иной тип неуспеха.
- **RF17b, RF18=static-init, RF17c=Lazy:** `Class.forName(name,false,loader)` не инициирует инициализацию, обычный `Class.forName(name)` — инициирует.
- **RF19=IllegalAccessException, RF20=true, RF21=hidden:67:** закрытый private метод доступен лишь после успешного `trySetAccessible()` в данном сценарии. Это не универсальное право reflective caller.
- **RF22=List / RF23=true / RF24=java.lang.String:** erased `getReturnType()` vs `getGenericReturnType()` и `ParameterizedType` декларации. **RF25–RF29** — primitives/arrays/metadata. **RF30=false/RF31=InaccessibleObjectException** — отказ deep reflection на `String.value` в JDK 21. **RF32** — `getDeclaredMethods` явно отсортированы по имени; исходный порядок API не специфицирован.

## JPMS: сравнительный опыт с двумя named modules

Обе конфигурации имеют `m.target` и `m.client`, обе позволяют публичный вызов `Vault.read()` (**`JM01=67`**) благодаря `exports api`. Но **публичный export не даёт права доступа к private**: в `exportonly` `trySetAccessible` возвращает **`JM02=false`**, `getInt` выдаёт **`JM03=IllegalAccessException`**.

В отдельном варианте `opens` объявлено **`opens api to m.client`**. Это разрешает deep reflection *именно этому именованному модулю*: **`JM02=true`, `JM03=67`**. В обоих деревьях сохранён `public Vault() {}`, чтобы `-Xlint:all` не выдавал `[missing-explicit-ctor]`. Запуски выполняются с `java --module-path mods -m m.client/client.Inspect`, без `--add-opens` и без classpath-подмены.

Такой fixture демонстрирует только **конкретную module topology**, а не все варианты `open module`, `--add-opens`, automatic modules, class loading или Java agents.

## Граница доказанного

Положительный runner/CI проверяет только эти **4 JVM-запуска и 4 полных stdout** (3 compilations). Изолированные **19 compiler-negative** из G1 проверены ранее ролью 04 с `-XDrawDiagnostics`, но **не включены в штатный CI**. `@Target(TYPE_USE)` на class — compile-positive Java21; runtime `NoSuchMethodException`, `IllegalAccessException`, `InvocationTargetException` — не javac diagnostics. Не выполняются отдельно JSR269 processor, `javap -v` для retention CLASS, `javadoc`, самостоятельные опыты `@Deprecated/@SuppressWarnings`, каждый inline snippet статей, другие JPMS graphs, JDK/OS, Spring/AOP/ORM/agents/ClassLoader internals, JIT/JMH/GC/performance, Obsidian/IDE GUI/anchor/HTTP и P2.
