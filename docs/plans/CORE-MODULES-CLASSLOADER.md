# CORE-MODULES + CORE-CLASSLOADER — исследовательский план G1 (Java SE 21)

**Статус:** G1 PROPOSED / NOT ACCEPTED. **Роль:** 01 — Research & Coverage. **G0:** [Issue #175](https://github.com/Melchiorius/backend-interview-labs/issues/175), [поручение 00 #7079490849](https://github.com/Melchiorius/backend-interview-labs/issues/175#issuecomment-7079490849), программа #33. **Исходный BASE/main:** `aa183b3b1bdba3c5eed8163e4b10aee82a043092` (G5 PR #172). Параллельный status-only G6 [Issue #174](https://github.com/Melchiorius/backend-interview-labs/issues/174) выполняет 06; если он переместит `main`, ветку следует перенести на новый BASE **до адресного review**. **Точный PR HEAD** — GitHub commit и отдельный адресный комментарий для 04. **Нормативная версия:** JLS/JVMS/Javadoc/JDK tools **Java SE 21 без preview**; **проверяемая среда:** OpenJDK 21.0.12.1+1-1-deb13u1, Debian GNU/Linux 13 (trixie).

## 1. Архитектура знаний: не смешивать два разных измерения

**CORE-MODULES** отвечает на вопрос: какие *модули читает* модуль-потребитель, какие пакеты ему *экспортированы* для статического использования, какие пакеты *открыты* для reflection, как модульный граф собирается из `module-info.java`? **CORE-CLASSLOADER** отвечает на другой вопрос: какая `ClassLoader`-система *определила* конкретный `Class`, почему одинаковое бинарное имя ещё не даёт одинакового runtime-типа, когда происходит class initialization?

**Рекомендация G1:** **две отдельные связные статьи**, будущие `knowledge/01-java-core/modules.md` (CORE-MODULES) и `knowledge/01-java-core/classloaders.md` (CORE-CLASSLOADER). Эти файлы **ещё не существуют, поэтому мы НЕ создаём Markdown-ссылки на них**. G2 код `examples/core/modules-classloader/` возможен в одном учебном наборе с раздельными `modules` и `classloaders` runners, но только при подтверждённой педагогической необходимости. 01 не пишет G2/source/Gradle/CI. Для объяснения нужны **отдельные** трассы JPMS и class identity, а затем проверяемый мост: ``JM10`/`JM11` и `CL12`/`CL21` показывают, что модульность ≠ отдельный classloader.

**Границы:** CORE-MODULES и обзор CORE-CLASSLOADER не закрывают `CORE-SERIAL`, `JVM-LOADING`, JVM linkage constraints/class unloading как самостоятельный курс, JDK image/JMOD internals, agent/bytecode tooling, классы с бинарной эволюцией, dynamic ModuleLayer graphs, сложную security architecture, full ServiceLoader SPI/Provider discovery, Spring Boot nested JAR launchers/OSGi, parallel capable loaders/locking, JIT/JMH/GC и полный JPMS deployment. Пункты вне реально запущенных экспериментов отмечены `P1`/`P2`/`not run`.

### 1.1 Фактический main: уже опубликовано и чем это не заменяет новые P0

На исходном G5 #172 **33 самостоятельные Java Core статьи**, материал есть для **34 из 42 curricular ID**, **8 без Implemented**. В реестрах [COVERAGE](../COVERAGE.md) и [CURRICULUM](../CURRICULUM.md) в момент постановки могли оставаться старые значения до G6 #174. Перечень существующих файлов сверён по **полному GitHub recursive tree**, а не по историческому счётчику; не изменять реестры в G1.

| Реально существующая статья / ресурс | Покрыто уже | Отдельная недостающая глубина JM/CL |
|---|---|---|
| [CORE-ACCESS](../../knowledge/01-java-core/access.md) | public/private/protected/package, объявление доступных типов | модульная readability, exported vs opened packages, qualified exports |
| [CORE-INIT](../../knowledge/01-java-core/initialization.md) | JLS12 loading/linking/init, Class.forName(false) и true, lifecycle/errors | defining vs initiating loader, две независимые Class, TCCL, CLI/classpath traces; не переписывать всё об инициализации |
| [CORE-REFLECTION](../../knowledge/01-java-core/reflection.md) | `Class`, `trySetAccessible`, 2 named-module `exports/opens` runs | `requires transitive/static`, module graph, qualified exports, module-path/classpath, classloader identity |
| [CORE-ANNOTATIONS](../../knowledge/01-java-core/annotations.md) | annotation retention, runtime inspection | metadata сама по себе не JPMS/loader identity |
| [CORE-NESTED](../../knowledge/01-java-core/nested-classes.md) | вложенные классы, private nestmates | runtime class identity по defining loader, не отношения nesting |
| [CORE-GENERICS](../../knowledge/01-java-core/generics.md) | type parameters/variance/inference | одинаковое имя после стирания не доказывает равенство runtime Class |
| [CORE-ERASURE](../../knowledge/01-java-core/erasure.md) | bridge/runtime erasure/metadata | loader constraints/JVMS class identity — только адресный мост |
| [CORE-EXCEPTIONS](../../knowledge/01-java-core/exceptions.md) | Throwable и цепочки ошибок | CNFE vs NCDFE и ошибки module compile/runtime по конкретной стадии |
| [CORE-CLASSES](../../knowledge/01-java-core/classes.md) | классы, поля, конструктор, `new` | классы определяются загрузчиком, а не только исходным текстом |
| [принятый G1 AN/RF](CORE-ANNOTATIONS-REFLECTION.md) | 2 реальных JPMS варианта `m.target/m.client`, `exports/opens`, stdout `3+3` | использовать как **ранее опубликованный мост**, НЕ считать эти шесть строк «новыми» G1 экспериментами |
| [опубликованный runner AN/RF](../../examples/core/annotations-reflection/README.md) | source tree `exports-only` и `exports+opens` | новые `m.lib/m.mid/m.client` эксперименты здесь **самостоятельно** запускаются; источник AN/RF не копируется в G1 PR |

Нормы workflow: [AGENTS](../../AGENTS.md), [ROLE_MODEL](../ROLE_MODEL.md), [WORKFLOW](../WORKFLOW.md), [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md), [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), [EXERCISE_AND_TESTING_STANDARD](../EXERCISE_AND_TESTING_STANDARD.md).

## 2. Нормативность A и независимые C, только Java 21

| ID | Первоисточник A, точный раздел/назначение |
|---|---|
| A1 | [JLS21 `7.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7), [`7.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.1), [`7.7.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.2): module-info, requires/transitive/static, exports/opens/qualified/open module |
| A2 | [JLS21 `7.7.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.3), [`7.7.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.4), [`7.7.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.5), [`7.7.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.6): service directive boundary, unnamed module, observability |
| A3 | [JLS21 `7.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.3), [`6.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6): visible compilation units, compile accessibility/readability |
| A4 | [Module API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Module.html): `canRead/isNamed/isExported/isOpen/getLayer` и тонкость: открытый пакет может быть отмечен `isExported=true` **во время исполнения**, не обретая compile-time `exports` |
| A5 | [ModuleLayer API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ModuleLayer.html), [java.lang.module API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/module/package-summary.html): модульная конфигурация, resolution/readability; границы программного graph creation |
| A6 | [javac 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html), [java launcher 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html): `--module-source-path`, `--module-path`, `-m`, `--add-modules`, `--class-path`, diagnostics |
| A7 | [JLS21 `12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.2), [`12.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.3), [`12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4): load, link, initialization; `Class.forName` не эквивалентен `new` |
| A8 | [JVMS21 `5.3](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3), [`5.3.2](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3.2): class identity = binary name + **defining** loader; initiating loader может отличаться от defining |
| A9 | [ClassLoader API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html): `loadClass/defineClass/findLoadedClass/getParent/getPlatformClassLoader/getSystemClassLoader`; default parent delegation, не абсолютное правило всех subclasses |
| A10 | [Class API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html), [Class.forName(String,boolean,ClassLoader)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#forName(java.lang.String,boolean,java.lang.ClassLoader)): init flag, Class.getClassLoader, binary name |
| A11 | [Thread API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html): context class loader getter/setter — отдельный выбор для инфраструктуры, не меняет defining loader существующего Class |
| A12 | [ClassNotFoundException21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassNotFoundException.html), [NoClassDefFoundError21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/NoClassDefFoundError.html), [ClassCastException21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassCastException.html): checked lookup failure / runtime linkage / runtime cast |
| A13 | [AccessibleObject API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AccessibleObject.html), [ServiceLoader API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html): reflection opening and SPI (SPI remains P1/not run) |

**Подтверждённые реальные независимые интервью-вопросы C из [SOURCE_REGISTRY](../SOURCE_REGISTRY.md):**
- **C1 / `Q-IVAN`** — [Modules (JPMS), Q125/Q126](https://github.com/ivanjayasooriya/java-interview-prep/blob/main/docs/17-modules-jpms.md): *“What is the Java Platform Module System (JPMS), introduced in Java 9?”* и *“What is module-info.java?”* Это действительно открытый текст исходника; **два** общих вопроса, **не** доказательство, что каждое условие `requires static`, `open module` или TCCL — отдельный распространённый вопрос.
- **C2 / `Q-HRUDAY`** — [README Chapter 12 ``12.2/12.3](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md): вопросы *“Explain Parent Delegation Model”*, *“Can Parent Delegation be broken?”*, *“What are custom Class Loaders?”*, *“What are the phases of class loading?”*, *“Difference between Loading and Initialization”*. В Chapter 18 ``18.2/18.6 — `Class.forName` и module restrictions reflection. Не принимать термин “Platform (Extension)” как утверждение, что в JDK21 всё ещё существует старый Java 8 extension mechanism.
- **C3 / `Q-TEAMLEAD`** — [README](https://github.com/teamlead/java-interview-questions/blob/main/README.md): “What is classloader”, “Tell me about the classes-loaders and about dynamic class loading”. Эти формулировки **не** доказательство специфики Java21.
- **C4 / `Q-CHERNYSHOV-RU`** — [core.md](https://github.com/Shell26/Java-Developer/blob/master/core.md): реальный вопрос «Расскажите про классы-загрузчики и про динамическую загрузку классов». **В его ответе присутствует устаревший extension loader/`rt.jar`/`java.ext.dirs`**; применять как сигнал о типовом интервью-вопросе, **не** как описание Java21.
- **GAP:** для точных `requires static`, `Module.isExported` при `opens`, `--add-modules`, разницы initiating/defining loader и particular TCCL setter в просмотренных C **нет доказанного самостоятельного вопроса**. В P0 это авторские углубления проверенных широких C1/C2, **не** мнимые цитаты. A определяет гарантию, а лабораторная проба — контрпример.

## 3. CORE-MODULES — 17 P0 с доказательными следами

**JMxx** — строка `client.Main`, `JSxx` — `StaticMain`, `JM_Nxx` — отдельный комплект исходников, отрицательный javac. В колонке C указан **зонтичный** реально найденный вопрос; где это уточнение, оно не выдается за дословную формулировку автора.

| ID | Вопрос, который должен быть объяснён механизмом | A/C | Positive/контрпример |
|---|---|---|---|
| `JM-01` | Модуль — именованная единица с descriptor и пакетами; package != module | A1,A3 / C1 | полные `m.lib/m.mid/m.client` и `JM11` |
| `JM-02` | `requires` добавляет прямую зависимость для readable graph, не открывает package автоматически | A1,A3 / C1 | `JM03=true` при `requires transitive`, `JM_N01` |
| `JM-03` | `requires transitive` позволяет клиенту читать transitive dependency; plain requires — нет | A1,A5 / C1 | `JM02` vs `JM_N01` (does.not.read) |
| `JM-04` | `requires static` — compile dependency, допускающая отсутствие target module при runtime | A1,A6 / C1 (углубление) | `JS01=false/true` в двух module-path запусках |
| `JM-05` | `exports` и `public` нужны для обычного доступа из читающего module | A1,A3 / C1 | `JM02,JM04=true`; `JM_N02` |
| `JM-06` | Qualified `exports api to friend` не делает API доступным остальным | A1 / C1 (углубление) | `JM_N03`: m.mid разрешён, m.client нет |
| `JM-07` | Public class в НЕ export package остаётся закрытым для static import клиента | A1,A3 / C1 | `JM05=false` base, `JM_N02` |
| `JM-08` | `opens` допускает deep reflection, даже если package не объявлен exported | A1,A4,A13 / C1 | base `JM06..09=false/blocked`, opened `true/67` |
| `JM-09` | `opens` не заменяет compile-time `exports` | A1,A3 / C1 (углубление) | `JM_N04`, реальная package.not.visible |
| `JM-10` | `Module.isExported(pkg,other)` при runtime opens может быть true, хотя static compilation без exports невозможна | A4 / C1 (GAP точного вопроса) | `JM05` base=false, opened=true; `JM_N04` |
| `JM-11` | Readability и package accessibility — две независимые проверки | A1,A3,A4 / C1 | `JM03=true,JM05=false` base |
| `JM-12` | Classpath код принадлежит unnamed module; module-path named module, разные способы запуска | A2,A6 / C1 | `JM11=true` vs `CL21=true` |
| `JM-13` | ClassLoader identity и JPMS module identity не тождественны | A4,A8 / C1,C2 | `JM10=true` при разных named modules |
| `JM-14` | Запрет duplicate directives, open module + opens directive, self-dependency | A1 / C1 (углубление) | `JM_N05..JM_N09,JM_N12..JM_N13` |
| `JM-15` | Неявная читаемость `java.base`, и другие JDK-модули требуют подходящей зависимости | A1,A3 / C1 | `JM_N12` запрещает static java.base; `JM_N14` java.sql not read |
| `JM-16` | `uses/provides` имеют статический типовой контракт; не значит, что SPI discovery исполнен | A2,A13 / C1 (GAP точного вопроса) | `JM_N10` enum service, `JM_N11` wrong provider; ServiceLoader runtime **not run** |
| `JM-17` | `javac --module-source-path` и `java --module-path -m` действительно создают/запускают named graph | A6 / C1 | 4 отдельных JPMS JVM запусka ``6.1,6.2`, `JM11` |

## 4. CORE-CLASSLOADER — 16 P0

**CLxx** — stdout `LoaderProbe`, **CExx** — runtime `MissingProbe`, `CL_Nxx` — изолированный compiler-negative.

| ID | Вопрос/механизм | A/C | Положительный/отрицательный контроль |
|---|---|---|---|
| `CL-01` | `ClassLoader` и `Class` — разные роли: загрузчик определяет тип, Class представляет runtime type | A8,A9,A10 / C2,C3 | `CL04/CL09..11` |
| `CL-02` | load/link/initialize — не один шаг; выполнение `static{}` отделено от load | A7,A8 / C2 | `CL04,CL05` до `CL06` init |
| `CL-03` | `Class.forName(name,false,loader)` не требует инициализации, `true` требует | A7,A10 / C2 | `CL05=true`, затем `CL06=initialized`/`CL07` |
| `CL-04` | `ClassLoader.loadClass(name)` по стандартному API не равно вызову static initialization | A7,A9 / C2 | `CL04`, `CL06` позже |
| `CL-05` | Binary name + defining loader задают runtime identity, одинаковых имён мало | A8 / C2,C3 (углубление) | `CL09=true,CL10=false,CL11=true` |
| `CL-06` | Два экземпляра `ClassLoader` вправе определить один bytecode как несовместимые типы | A8,A9 / C2 | полный `ChildFirstTokenLoader` и `CL13/CL18` |
| `CL-07` | Вызов `b.cast(aInstance)` даёт `ClassCastException` при разных defining loaders | A8,A12 / C2 | `CL14=ClassCastException` runtime |
| `CL-08` | Defining loader отличается от initiating: делегирующий loader может вернуть parent's Class | A8,A9 / C2 (GAP exact wording) | `CL19=true,CL20=true` через `new ClassLoader(app) {}` |
| `CL-09` | Parent-first — типичная модель `loadClass`, не абсолютное требование любого переопределённого subclass | A9 / C2 | `CL19` delegate vs явный child-first `CL10=false` |
| `CL-10` | Bootstrap-defined `String.class.getClassLoader()==null` в этом JDK — не ошибка/отсутствие класса | A9,A10 / C2 | `CL01=true`, ограничение Java21 API/среда |
| `CL-11` | Platform/system loader API и observed classloader, не обещать имена implementation/одинаковую иерархию всех JDK | A9 / C2 | `CL02=true,CL03=true` на указанном JDK |
| `CL-12` | Thread context class loader выбирается на потоке, не переписывает уже существующую identity Class | A11 / C2 (GAP direct) | `CL15/CL16/CL17` и `CL10` неизменен |
| `CL-13` | Missing binary-name lookup `Class.forName` → checked `ClassNotFoundException` | A10,A12 / C2,C3 | `CE01` runtime, `CL_N01` unhandled checked compile error |
| `CL-14` | Missing linked superclass после успешной компиляции → `NoClassDefFoundError`, не CNFE | A7,A8,A12 / C2 | `CE02` с удалённым `Parent.class` из runnable copy |
| `CL-15` | `ClassNotFoundException` — checked, `NoClassDefFoundError` — Error; compiler negatives другие | A7,A12 / C2 | `CL_N01/CL_N02` vs `CE01/CE02` |
| `CL-16` | Named module и ClassLoader независимы: несколько модулей могут иметь один defining loader, unnamed classes — другой случай | A4,A8 / C1,C2 | `JM10=true`, `CL12=false`, `CL21=true` |

**Итого 33 P0 (17 JM + 16 CL)**. Каждая строка имеет нормативный A, *не обязательно дословный* широкий C и конкретный trace/neg. Reviewer должен отдельно проверить, что G2-статья **объяснит механизм**, а не просто сошлётся на `JM03`.

## 5. P1/P2 и обязательные пределы

| ID | Приоритет | Задача и статус |
|---|---|---|
| JM-18 | P1 | `open module` делает пакеты открытыми, но сам `opens` внутри `open module` запрещён, `JM_N07`; positive open-module launch **not run** |
| JM-19 | P1 | `exports api to friend` и `opens pkg to friend`, их graph semantics и не-объявленный friend; qualified export negative `JM_N03` |
| JM-20 | P1 | `ServiceLoader uses/provides` со стабильным SPI API, но настоящий named-module provider-discovery **not run**; `JM_N10/11` проверяют только compile |
| JM-21 | P1 | `--add-exports/--add-opens/--add-reads` для инструментов, отличие runtime и compilation flags; **not run** |
| JM-22 | P1 | Automatic modules / module path JAR naming/migration и unnamed module access — **not run**, не весь CORE-MODULES |
| JM-23 | P1 | Runtime module roots, `--add-modules m.opt` vs absent; `JS01=false/true` verified |
| CL-17 | P1 | Array `Class` loader follows component type; primitive array bootstrap/null; A9, **not run** |
| CL-18 | P1 | Platform loader implementation/JDK runtime modules and TCCL defaults differ; observed CL02/03/15 not universal |
| CL-19 | P1 | `LinkageError` family, verifier/preparation/resolution, loader constraints — scoped JVM-LOADING, **not run** |
| CL-20 | P1 | Custom loader policy can be child-first; code proves one such policy, not ALL Java loaders |
| CL-21 | P1 | TCCL usage by SPI/service discovery API; TCCL setter checked, actual `ServiceLoader` discovery **not run** |
| CL-22 | P1 | `Class.forName(Module,String)` returns null for class absent in module under API contract; **not run** |
| JM-24 | P2 | Runtime ModuleLayer graph resolution, conflicts/split packages, services with multiple providers, jlink/JMOD/JDK images — **not run** |
| JM-25 | P2 | Full JPMS security, agents, reflective opens of JDK internals, actual cross-layer readable graph — **not run** |
| JM-26 | P2 | Multi-release JAR/automatic module names/classpath migration production — **not run** |
| JM-27 | P2 | Signed JAR/security manager legacy, dynamic `addOpens/addExports` mutators — **not run** |
| CL-23 | P2 | JDK bootstrap internals, load lock/deadlock, parallel-capable ClassLoaders, custom parent delegation edge conditions — **not run** |
| CL-24 | P2 | Hot reload/unloading/metaspace pressure, GC/JIT/performance/JMH — **not run** |
| CL-25 | P2 | Java agents/instrumentation, hidden classes, custom bytecode generation in frameworks — **not run** |
| CL-26 | P2 | JVM-LOADING complete JVMS resolution/linking lifecycle, detailed loader constraint violations, concurrency/JMM — **not run** |

**Итого 33 P0 / 12 P1 / 8 P2.** Ни один соседний curricular ID не получает Implemented из-за одной дополнительной пробы.

## 6. Действительно исполненные Java21 положительные сценарии — полные исходники

Запуски выполнены **в чистом временном каталоге вне репозитория**, OpenJDK21.0.12.1/Debian13. В каждом необходимом `javac` использовали `--release 21 -Xlint:all`: **exit 0 и пустой stderr**. Все шесть отдельных JVM-результатов повторены, все **6/6 `diff -u` exit 0**, код и stdout приведены ниже; **49 stdout lines ≠ 49 unit tests**. Java-файлы и ожидаемые stdout остаются **в Markdown G1**, не коммитятся как G2 artifacts.

### 6.1 Три настоящих JPMS-модуля: requires transitive, exports и opens (два запуска)

**Полный `base` tree — семь исходников:**
~~~text
base/src/m.lib/module-info.java
base/src/m.lib/lib/api/Greeter.java
base/src/m.lib/lib/internal/Hidden.java
base/src/m.mid/module-info.java
base/src/m.mid/mid/api/Bridge.java
base/src/m.client/module-info.java
base/src/m.client/client/Main.java
~~~

`base/src/m.lib/module-info.java`:
~~~java
module m.lib { exports lib.api; }
~~~

`base/src/m.lib/lib/api/Greeter.java`:
~~~java
package lib.api;
public final class Greeter {
    private Greeter() {}
    public static String hello() { return "hello-67"; }
}
~~~

`base/src/m.lib/lib/internal/Hidden.java`:
~~~java
package lib.internal;
public class Hidden {
    public Hidden() {}
    private final int secret = 67;
}
~~~

`base/src/m.mid/module-info.java`:
~~~java
module m.mid { requires transitive m.lib; exports mid.api; }
~~~

`base/src/m.mid/mid/api/Bridge.java`:
~~~java
package mid.api;
import lib.api.Greeter;
public final class Bridge {
    private Bridge() {}
    public static String bridge() { return "bridge:" + Greeter.hello(); }
}
~~~

`base/src/m.client/module-info.java`:
~~~java
module m.client { requires m.mid; }
~~~

`base/src/m.client/client/Main.java`:
~~~java
package client;
import lib.api.Greeter;
import mid.api.Bridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
public class Main {
    public static void main(String[] args) throws Exception {
        Module self = Main.class.getModule();
        Module library = Greeter.class.getModule();
        System.out.println("JM01=" + Bridge.bridge());
        System.out.println("JM02=" + Greeter.hello());
        System.out.println("JM03=" + self.canRead(library));
        System.out.println("JM04=" + library.isExported("lib.api", self));
        System.out.println("JM05=" + library.isExported("lib.internal", self));
        System.out.println("JM06=" + library.isOpen("lib.internal", self));
        Class<?> hidden = Class.forName("lib.internal.Hidden");
        Constructor<?> constructor = hidden.getConstructor();
        Field field = hidden.getDeclaredField("secret");
        boolean ctorOpen = constructor.trySetAccessible();
        boolean fieldOpen = field.trySetAccessible();
        System.out.println("JM07=" + ctorOpen);
        System.out.println("JM08=" + fieldOpen);
        if (ctorOpen && fieldOpen) System.out.println("JM09=" + field.getInt(constructor.newInstance()));
        else System.out.println("JM09=blocked");
        System.out.println("JM10=" + (Main.class.getClassLoader() == Greeter.class.getClassLoader()));
        System.out.println("JM11=" + self.isNamed());
    }
}
~~~

`opened` — **полная копия всех семи исходников `base`, с ровно одной заменой** `opened/src/m.lib/module-info.java`:
~~~java
module m.lib { exports lib.api; opens lib.internal to m.client; }
~~~

**Команды выполнены отдельно для каждого `base` и `opened`, из соответствующего каталога:**
~~~sh
javac --release 21 -Xlint:all -d mods --module-source-path src -m m.lib,m.mid,m.client
java --module-path mods -m m.client/client.Main > actual.stdout
diff -u expected.stdout actual.stdout
~~~

**Фактический base stdout (11 строк):**
~~~text
JM01=bridge:hello-67
JM02=hello-67
JM03=true
JM04=true
JM05=false
JM06=false
JM07=false
JM08=false
JM09=blocked
JM10=true
JM11=true
~~~

**Фактический opened stdout (11 строк):**
~~~text
JM01=bridge:hello-67
JM02=hello-67
JM03=true
JM04=true
JM05=true
JM06=true
JM07=true
JM08=true
JM09=67
JM10=true
JM11=true
~~~

**Критический Java21 контрпример:** в `opened` `JM05=true` по **runtime** `Module.isExported("lib.internal",client)` при `opens`, но ``JM_N04` показывает, что **opens даже на публичном api package без exports недостаточен для compile-time import**. Нельзя объяснять `isExported()` как абсолютный «снимок директивы exports из module-info». `JM10=true` демонстрирует, что **разные named модули могут иметь один определяющий загрузчик** в наблюдавшемся графе, а не «один module — один ClassLoader». Эти два новых запущенных module graphs **не** являются историческими `m.target/m.client` опытами [RF](../../knowledge/01-java-core/reflection.md).

### 6.2 requires static: модуль нужен при compile, может отсутствовать в runtime (два запуска)

**Полное дерево `static`:**
~~~text
static/src/m.opt/module-info.java
static/src/m.opt/opt/api/OptionalApi.java
static/src/m.consumer/module-info.java
static/src/m.consumer/consumer/StaticMain.java
~~~

`static/src/m.opt/module-info.java`:
~~~java
module m.opt { exports opt.api; }
~~~

`static/src/m.opt/opt/api/OptionalApi.java`:
~~~java
package opt.api;
public final class OptionalApi {
    private OptionalApi() {}
    public static int value() { return 67; }
}
~~~

`static/src/m.consumer/module-info.java`:
~~~java
module m.consumer { requires static m.opt; }
~~~

`static/src/m.consumer/consumer/StaticMain.java`:
~~~java
package consumer;
public class StaticMain {
    public static void main(String[] args) {
        System.out.println("JS01=" + ModuleLayer.boot().findModule("m.opt").isPresent());
        System.out.println("JS02=" + StaticMain.class.getModule().isNamed());
    }
}
~~~

**Команды реально выполнены:**
~~~sh
javac --release 21 -Xlint:all -d static/mods --module-source-path static/src -m m.opt,m.consumer
mkdir -p static/runtime
cp -R static/mods/m.consumer static/runtime/
java --module-path static/runtime -m m.consumer/consumer.StaticMain > static/runtime.stdout
java --module-path static/mods --add-modules m.opt -m m.consumer/consumer.StaticMain > static/present.stdout
diff -u static/runtime.expected.stdout static/runtime.stdout
diff -u static/present.expected.stdout static/present.stdout
~~~

**Без `m.opt` в runtime (2 строки):**
~~~text
JS01=false
JS02=true
~~~

**С доступным и явно добавленным `m.opt` (2 строки):**
~~~text
JS01=true
JS02=true
~~~

**Ограничение:** этот source не исполняет код `OptionalApi` после удаления модуля. Продемонстрировано **отсутствие обязательного разрешения optional dependency при старте**; не следует заключать, что попытка *использовать* отсутствующий тип `m.opt` успешно выполнится. `--add-modules m.opt` специально делает его root и не эквивалентен просто наличию в папке. В `m.opt` нет отношения к Java `Optional<T>` — это название тестового модуля.

### 6.3 ClassLoader — два определения одного binary name, parent delegation и class init (21 строка)

**Полное дерево `classloader`:**
~~~text
classloader/src/LoaderProbe.java
classloader/src/MissingProbe.java
classloader/src/sample/Token.java
classloader/src/sample/Lazy.java
classloader/src/sample/Parent.java
classloader/src/sample/Dependent.java
~~~

`classloader/src/sample/Token.java`:
~~~java
package sample;
public class Token { public int value() { return 67; } }
~~~

`classloader/src/sample/Lazy.java`:
~~~java
package sample;
public class Lazy {
    static { System.out.println("CL06=initialized"); }
}
~~~

`classloader/src/sample/Parent.java`:
~~~java
package sample;
public class Parent {}
~~~

`classloader/src/sample/Dependent.java`:
~~~java
package sample;
public class Dependent extends Parent {}
~~~

`classloader/src/LoaderProbe.java` — полноценная программа с **осознанно child-first только для `sample.Token`**, а прочие классы делегируются родителю:
~~~java
import java.nio.file.Files;
import java.nio.file.Path;
public class LoaderProbe {
    static final class ChildFirstTokenLoader extends ClassLoader {
        private final byte[] bytes;
        ChildFirstTokenLoader(byte[] bytes) {
            super(ClassLoader.getSystemClassLoader());
            this.bytes = bytes.clone();
        }
        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals("sample.Token")) return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> defined = findLoadedClass(name);
                if (defined == null) defined = defineClass(name, bytes, 0, bytes.length);
                if (resolve) resolveClass(defined);
                return defined;
            }
        }
    }
    public static void main(String[] args) throws Exception {
        ClassLoader app = LoaderProbe.class.getClassLoader();
        System.out.println("CL01=" + (String.class.getClassLoader() == null));
        System.out.println("CL02=" + (app == ClassLoader.getSystemClassLoader()));
        System.out.println("CL03=" + (ClassLoader.getPlatformClassLoader() != null));
        Class<?> loaded = app.loadClass("sample.Lazy");
        System.out.println("CL04=" + loaded.getName());
        System.out.println("CL05=" + (Class.forName("sample.Lazy", false, app) == loaded));
        System.out.println("CL07=" + Class.forName("sample.Lazy", true, app).getSimpleName());
        System.out.println("CL08=" + (Class.forName("java.lang.String") == String.class));
        byte[] bytes = Files.readAllBytes(Path.of(args[0]));
        Class<?> a = new ChildFirstTokenLoader(bytes).loadClass("sample.Token");
        Class<?> b = new ChildFirstTokenLoader(bytes).loadClass("sample.Token");
        System.out.println("CL09=" + a.getName().equals(b.getName()));
        System.out.println("CL10=" + (a == b));
        System.out.println("CL11=" + (a.getClassLoader() != b.getClassLoader()));
        System.out.println("CL12=" + (a.getModule().isNamed()));
        System.out.println("CL13=" + (a.getConstructor().newInstance() instanceof sample.Token));
        try {
            b.cast(a.getConstructor().newInstance());
            System.out.println("CL14=unexpected");
        } catch (ClassCastException e) {
            System.out.println("CL14=ClassCastException");
        }
        System.out.println("CL15=" + (Thread.currentThread().getContextClassLoader() == app));
        Thread.currentThread().setContextClassLoader(a.getClassLoader());
        System.out.println("CL16=" + (Thread.currentThread().getContextClassLoader() == a.getClassLoader()));
        Thread.currentThread().setContextClassLoader(app);
        System.out.println("CL17=" + (a.getClassLoader().getParent() == app));
        System.out.println("CL18=" + (sample.Token.class != a));
        ClassLoader delegating = new ClassLoader(app) {};
        Class<?> shared = delegating.loadClass("sample.Token");
        System.out.println("CL19=" + (shared == sample.Token.class));
        System.out.println("CL20=" + (shared.getClassLoader() == app));
        System.out.println("CL21=" + (!LoaderProbe.class.getModule().isNamed()));
    }
}
~~~

**Фактический stdout, 21 строка, порядок `CL06` вставлен между `CL05` и `CL07` из static initializer:**
~~~text
CL01=true
CL02=true
CL03=true
CL04=sample.Lazy
CL05=true
CL06=initialized
CL07=Lazy
CL08=true
CL09=true
CL10=false
CL11=true
CL12=false
CL13=false
CL14=ClassCastException
CL15=true
CL16=true
CL17=true
CL18=true
CL19=true
CL20=true
CL21=true
~~~

**Что здесь на самом деле доказано:** `CL19/CL20` — *delegating* загрузчик инициировал поиск, но получил `Class`, определённый родителем; `CL09/CL10/CL11` — два отдельных **определяющих** загрузчика создали два разных runtime-типа с одним binary name; `CL13=false` показывает, что даже системно определённый `sample.Token` не является тем же типом; `CL14` — runtime cast error. `CL12=false` и `CL21=true` отражают unnamed module этого classpath runner и **не заменяют** модульный граф. `CL15` — observed default TCCL этого процесса, **не универсальный invariant всех потоков**. `CL16` после setter показывает смену TCCL, а **не** смену a.getClassLoader(). Классы `ChildFirstTokenLoader` не предлагаются как production-ready plugin isolation/security boundary: ресурсная загрузка, пакеты, security/linkage, concurrent loading, disposal и агенты здесь не исследованы.

### 6.4 ClassNotFoundException vs NoClassDefFoundError (2 строки)

`classloader/src/MissingProbe.java`:
~~~java
public class MissingProbe {
    public static void main(String[] args) {
        try { Class.forName("absent.DoesNotExist"); }
        catch (ClassNotFoundException e) { System.out.println("CE01=ClassNotFoundException"); }
        try { Class.forName("sample.Dependent", false, MissingProbe.class.getClassLoader()); }
        catch (NoClassDefFoundError e) { System.out.println("CE02=NoClassDefFoundError"); }
        catch (ClassNotFoundException e) { System.out.println("CE02=unexpected-CNFE"); }
    }
}
~~~

**Команды действительно исполнены:** `Parent.java` и `Dependent.java` **сначала оба компилируются** вместе с другими файлами, затем `Parent.class` удаляется **только из отдельной runtime-копии** `classloader/bad`, не из исходников или positive `out`. JVM в новом процессе видит `Dependent.class` с отсутствующим нужным superclass.

~~~sh
javac --release 21 -Xlint:all -d classloader/out \
    classloader/src/LoaderProbe.java classloader/src/MissingProbe.java \
    classloader/src/sample/Token.java classloader/src/sample/Lazy.java \
    classloader/src/sample/Parent.java classloader/src/sample/Dependent.java
java -cp classloader/out LoaderProbe classloader/out/sample/Token.class > classloader/actual.stdout
cp -R classloader/out classloader/bad
rm classloader/bad/sample/Parent.class
java -cp classloader/bad MissingProbe > classloader/missing.stdout
diff -u classloader/expected.stdout classloader/actual.stdout
diff -u classloader/missing.expected.stdout classloader/missing.stdout
~~~

**Фактический stdout (2 строки):**
~~~text
CE01=ClassNotFoundException
CE02=NoClassDefFoundError
~~~

Важно: это **разные исполняемые ситуации**. `CE01` — имя отсутствует при явном reflective lookup, `CE02` — `Dependent` существует, но не удаётся загрузить отсутствующего `Parent` во время связывания/загрузки. Ни один из этих runtime outcomes не включён в `javac` negatives.

### 6.5 Протокол и хэши всех шести outputs

| JVM запуск | Exit | Строк stdout | SHA-256 stdout (UTF-8 + LF) |
|---|---:|---:|---|
| `base, -m m.client/client.Main` | 0 | 11 | `baa6e5df7ae0d5785ddb69dbfff909e1cf0ea72363d48e427b606d456f83c165` |
| `opened, -m m.client/client.Main` | 0 | 11 | `f7888ae22d0fb4a32adf6caff2a0ca024ab1b4c32eca64a9908952c0a8da724d` |
| `static/runtime, -m m.consumer/consumer.StaticMain` | 0 | 2 | `76f7a07110fa295756acffbe733743c9c83717562ec3702b440c62bab2ee146f` |
| `static/mods --add-modules m.opt, -m m.consumer/consumer.StaticMain` | 0 | 2 | `fbf6bc72c2cae2d8132f7d331663d8e8047c764f295101c74e725397ee1b02fe` |
| `classloader/out LoaderProbe` | 0 | 21 | `bc14f1d669ab8f992831fcc584d62ef1dd45c3ff941c8a1eeab1d7572eeed73c` |
| `classloader/bad MissingProbe` | 0 | 2 | `32aada973ec545bca57ecb6a49a714053ee784c5a07900e221990d35eafbeb0d` |

**Реальная проверка:** шесть повторных `java` запущены, их output сравнен через `diff -u` со stdout исходных запусков — **6/6 exit 0**, ни один не стал «ожидаемым» без запуска. Все четыре позитивные компиляции (`base`, `opened`, `static`, `classloader`) `--release 21 -Xlint:all` → exit 0, stderr **0 байт**. Запуск GitHub Actions на будущем PR — **другая проверка**, fenced Java внутри G1 Markdown автоматически не компилируется.

## 7. Изолированные compiler-negative: полные reconstructible source trees, реальные категории javac

**17/17** завершённых изолированных кейсов, OpenJDK21 `javac --release 21 -XDrawDiagnostics`, каждый exit **1**, зафиксирован тематический `compiler.err.*`. Это **14 самостоятельных multi-module исходных деревьев** плюс **3 отдельных ClassLoader Java файла**; они **не** соединялись в один общий компиляционный запуск. Отрицательное multi-module дерево однозначно воспроизводится так:

~~~sh
# Для каждого JM_Nxx независимо, из лабораторной корневой папки:
mkdir -p negative/JM_Nxx
cp -R base/src negative/JM_Nxx/src
# Затем ЗАМЕНИТЬ/ДОБАВИТЬ файлы ровно по таблице ниже.
javac --release 21 -XDrawDiagnostics -d negative/JM_Nxx/mods \
    --module-source-path negative/JM_Nxx/src -m m.lib,m.mid,m.client
~~~

Т.е. все **семь исходников** базового дерева `6.1` действительно присутствуют в каждой вариации; таблица описывает исчерпывающий diff: полное заменяющее содержимое файла/новый файл, а не недостающую скрытую зависимость. В случаях с добавлением файла в таблице указан **полный source**.

| Кейс | Единственная замена/добавление относительно семи файлов `base/src` | Реальная первая тематическая диагностика javac |
|---|---|---|
| `JM_N01_RequiresNonTransitive` | `m.mid/module-info.java` = `module m.mid { requires m.lib; exports mid.api; }` | `compiler.err.package.not.visible` / `does.not.read: m.client,lib.api,m.lib` |
| `JM_N02_NotExported` | `m.lib/module-info.java` = `module m.lib { opens lib.internal to m.client; }` | `compiler.err.package.not.visible` / `not.exported` |
| `JM_N03_QualifiedExport` | `m.lib/module-info.java` = `module m.lib { exports lib.api to m.mid; }` | `compiler.err.package.not.visible` / `not.exported.to.module: m.client` |
| `JM_N04_OpensNotExports` | `m.lib/module-info.java` = `module m.lib { opens lib.api to m.mid, m.client; }` | `compiler.err.package.not.visible` / `not.exported` |
| `JM_N05_DuplicateRequires` | `m.client/module-info.java` = `module m.client { requires m.mid; requires m.mid; }` | `compiler.err.duplicate.requires` |
| `JM_N06_SelfRequires` | `m.client/module-info.java` = `module m.client { requires m.client; requires m.mid; }` | `compiler.err.cyclic.requires` |
| `JM_N07_OpenModuleOpens` | `m.lib/module-info.java` = `open module m.lib { exports lib.api; opens lib.internal to m.client; }` | `compiler.err.no.opens.unless.strong` |
| `JM_N08_DuplicateExports` | `m.lib/module-info.java` = `module m.lib { exports lib.api; exports lib.api to m.mid; }` | `compiler.err.conflicting.exports` |
| `JM_N09_ExportMissingPackage` | `m.lib/module-info.java` = `module m.lib { exports lib.missing; exports lib.api; }` | `compiler.err.package.empty.or.not.found` |
| `JM_N10_UsesEnum` | `m.lib/module-info.java` = `module m.lib { exports lib.api; uses lib.api.Importance; }`; **добавить** `m.lib/lib/api/Importance.java` = `package lib.api; public enum Importance { HIGH }` | `compiler.err.service.definition.is.enum` |
| `JM_N11_BadProvider` | `m.lib/module-info.java` = `module m.lib { exports lib.api; provides lib.api.Contract with lib.api.BadProvider; }`; **добавить** `m.lib/lib/api/Contract.java` = `package lib.api; public interface Contract { int value(); }` и `m.lib/lib/api/BadProvider.java` = `package lib.api; public class BadProvider { public BadProvider() {} public int value(){return 67;} }` | `compiler.err.service.implementation.must.be.subtype.of.service.interface` |
| `JM_N12_StaticJavaBase` | `m.client/module-info.java` = `module m.client { requires static java.base; requires m.mid; }` | `compiler.err.mod.not.allowed.here: static` |
| `JM_N13_DuplicateQualifiedTarget` | `m.lib/module-info.java` = `module m.lib { exports lib.api to m.mid, m.mid; }` | `compiler.err.conflicting.exports.to.module` |
| `JM_N14_UnreadableJavaSql` | `m.client/module-info.java` остаётся `module m.client { requires m.mid; }`; **добавить** `m.client/client/SqlProbe.java` = `package client; import java.sql.Connection; class SqlProbe { Connection connection; }` | `compiler.err.package.not.visible` / `does.not.read: m.client,java.sql` |

**Полные отдельные ClassLoader negatives:** сохранить исходный текст каждого независимо в указанный файл `negative/<filename>` и выполнять `javac --release 21 -XDrawDiagnostics -d negative/out negative/<filename>`:

| Кейс | Полный source | Содержательная диагностика |
|---|---|---|
| `CL_N01_UnhandledForName.java` | `class CL_N01_UnhandledForName { void f() { Class.forName("missing.DoesNotExist"); } }` | `compiler.err.unreported.exception.need.to.catch.or.throw: java.lang.ClassNotFoundException` |
| `CL_N02_UnhandledLoadClass.java` | `class CL_N02_UnhandledLoadClass { void f(ClassLoader loader) { loader.loadClass("missing.DoesNotExist"); } }` | `compiler.err.unreported.exception.need.to.catch.or.throw: java.lang.ClassNotFoundException` |
| `CL_N03_OverrideFinalGetParent.java` | `class CL_N03_OverrideFinalGetParent extends ClassLoader { public ClassLoader getParent() { return null; } }` | `compiler.err.override.meth` / final superclass method |

**Реально получено:** 14/14 module-graph javac errors + 3/3 отдельных .java errors = **17/17**; у `JM_N01` и `JM_N03` разные причины **not readable** vs **not exported to client**, хотя общий top-level key одинаков `package.not.visible` — записана точная `compiler.misc.*`-причина. `JM_N04` показывает открытый для runtime package, не экспортируемый для компиляции. `JM_N11` содержит реальный интерфейс и поставщика-неподтип, а не missing dependency. `JM_N14` содержит существующий Java21 module `java.sql`, но `m.client` его не читает. Ни один отказ не засчитан только потому, что где-то забыли файл.

**Runtime НЕ compiler-negative:** `CE01=ClassNotFoundException`, `CE02=NoClassDefFoundError`, `CL14=ClassCastException`; ``JM07–09` при `opens` false не компиляционная ошибка — программа корректно выполняется и возвращает false/blocked. Для `requires static` отсутствие optional module при запуске допустимо в `StaticMain`, но это не универсальное обещание использования отсутствующего API.

## 8. Передача G2, критерии review и ограничения

**02 Knowledge Author:** две самостоятельные статьи с полноценным объяснением каждого P0; JPMS читать как причинную цепочку dependency → readability → exports vs opens → compile/public vs runtime reflection → errors → classpath unnamed. ClassLoader читать как name → initiating/defining → load/link/init → delegation/custom definition → identity → runtime errors → TCCL. Точно указать (1) наблюдаемое `JM05=true` при opens в Java21 API, (2) initiating≠defining, (3) `Class.getClassLoader()==null` bootstrap случая, (4) Java21 **не** имеет старого extension-loader механизма из устаревшего C4. Не переписывать весь `CORE-INIT`/`CORE-REFLECTION`.

**03 Examples & Exercises:** только если 00 назначит G2 и runnable учебно оправдан: 3-module 7-source `base` + `opened` variant, 4-source `static` variant, 6-source `classloader`, 6 уникальных stdout-эталонов (`11/11/2/2/21/2`). Java21 `--release 21 -Xlint:all` для positives, отдельный `java --module-path` (не compile-only), negative test runner должен проверять не только ненулевой exit, но **правильную диагностику** и причину module readability/export. Не копировать уже опубликованные AN/RF `exports/opens` тесты как якобы новые 01.

**05 Knowledge Editor:** естественный русский текст, схема **«readability / compile accessibility / deep reflection / defining loader / module membership»** — пять НЕэквивалентных понятий; link audit только после появления будущих статей. Не оставлять непроверенные таблицы JVM vendor-specific names и заявления «всё private доступно через setAccessible».

**04 Independent Reviewer:** независимо оценить 33 P0 по A/C + actual trace, устранить confounding (например compile error из-за file missing), различать `Module.isExported` runtime от JLS compile visibility и `open module` от qualified `opens`, поймать ошибку default parent-first claims, проверить 6 outputs/17 negatives, ClassCastException и runtime linkage. **PASS или CHANGES строго на exact HEAD; CHANGES → новый SHA, повторная независимая проверка.**

**Not run, а не «подтверждено»:** ServiceLoader actual provider discovery, dynamic ModuleLayer, standalone `open module` runtime positive, `--add-opens/exports/reads` flags, `Class.forName(Module,String)`, loaders с параллельными загрузками/ошибками security, class unloading, JMOD/multi-release JAR, Java agents, Spring Boot/OSGi/AOP/JPA, alternate JVM/JDK vendor/OS, IDE/Obsidian rendered navigation и anchors, все HTTP проверочные переходы, JMH/JIT/GC/performance, JMM/concurrent classloader deadlocks, benchmark. **Full-source Java21 snippets в G1 Markdown не входят в автоматически выполняемые Actions CI проекта**, проектная CI проверяет *старые опубликованные runnable*, а локальная серия Java21 в документе — независимая.

**Static relative link audit:** по **полной** recursive GitHub tree исходного BASE/main (обязательно `truncated=false`) разрешить реальные `./../../../` file links из `docs/plans/`, сообщить количественный итог. Это только проверка существования file-targets, **не** HTTP/anchors/Obsidian GUI. Никаких fake Markdown-ссылок на ещё не опубликованные `modules.md` и `classloaders.md`.

## 9. Фактические gate-статусы и передача эстафеты

| Объект | Подтверждение |
|---|---|
| Исследование/матрица 33 P0 / 12 P1 / 8 P2 | `G1 PROPOSED / NOT ACCEPTED` |
| 6 real JVM launches, 49 stdout, 6/6 diff, 4 positive javac | Verified local OpenJDK 21.0.12.1 Debian13, **не** GitHub CI |
| 17/17 independently compiled semantic-negative, 14 module + 3 classloader | Verified local `-XDrawDiagnostics` |
| CORE-MODULES/CORE-CLASSLOADER публикуемые статьи | **Not Implemented** |
| Java source, G2 articles/examples, Gradle/CI, COVERAGE/CURRICULUM | **не изменялись** в исследовательском PR |
| GitHub Actions exact PR HEAD | проверять **после открытия PR**, не объявлять успех до фактического run |
| P1/P2/другие JDK/OS, сложный JPMS/loader infra, IDE/GUI/performance | **not run** |

### Передача эстафеты

Кому: **04 — Independent Reviewer**.
Что сделать: независимый **G1-review исследовательского PR на exact HEAD**: version-pinned A/JLS/JVMS/API21, C-вопросы vs GAP, 33 P0, 6 настоящих positive JVM запуска включая `java --module-path` и два разных `requires static` outcomes, правильные 49 stdout, 17 отдельных compiler errors и precise причины, no false claims по ClassLoader/module identity,  static relative links, честные `not run`. Вердикт `G1 PASS` или `G1 CHANGES` с конкретными строками/нормативными обоснованиями; изменение HEAD → повторный review. После PASS адресно передать **00** для формального `G1 ACCEPTED/BLOCKED` и решения о merge; только 00 запускает G2 02/03.
Передать: оператор пересылает **прямой permalink отдельного адресного GitHub PR-комментария** к роли 04, а не URL общего PR/Issue.
