# CORE-MODULES — JPMS, модульный граф и границы доступа в Java 21

**Почему это спрашивают.** В обычном Java-коде класс может быть `public`, но клиент из другого модуля всё равно не сможет его импортировать. И наоборот: reflection в открытом пакете может прочитать приватное поле, хотя компилятор запрещает импорт даже публичного класса из того же пакета. Чтобы не путать эти случаи, различаем **readability модулей**, **compile-time exports** и **runtime opens**.

**Версия и статус:** Java SE 21, нормативные [JLS §7.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7), [JLS §6.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6), [Module API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Module.html), [javac 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html), [java launcher 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html). Принятый [план G1](../../docs/plans/CORE-MODULES-CLASSLOADER.md) содержит исходники, локальный transcript и изолированные negative; это учебная статья G2, **не** утверждение о новом CI самого файла.

**Адресное покрытие:** **JM-01–JM-17 = 17 P0**, **JM-18–JM-23 = 6 P1**, **JM-24–JM-27 = 4 P2**: **27/27 ID**. Вместе с [CORE-CLASSLOADER](classloaders.md) — **53/53 (33 P0, 12 P1, 8 P2)**. P1/P2 помечены границами, P2 не объявлены experimentally verified. Связанные существующие материалы: [правила доступа](access.md), [инспекция Reflection](reflection.md), [аннотации](annotations.md), [инициализация](initialization.md), [определяющий загрузчик](classloaders.md).

## 1. Что такое модуль: от исходников к графу (JM-01, JM-02, JM-11, JM-17)

*Пакет* группирует типы внутри пространства имён. **Named module** — единица конфигурации, обозначенная descriptor `module-info.java`: зависимости, предоставляемые наружу пакеты и специальные разрешения. Даже если оба проекта содержат пакет `api`, имя пакета само по себе не задаёт граф чтения.

В компиляции нескольких модулей каждая единица имеет собственный дескриптор. Возьмём три настоящих модуля: `m.lib` с библиотекой; `m.mid` — промежуточный API; `m.client` — клиент. Это не три Gradle-подпроекта: здесь обсуждается **JPMS**, а инструменты сборки могут лишь подготовить нужные каталоги.

~~~java
// m.lib/module-info.java
module m.lib {
    exports lib.api;
}

// m.mid/module-info.java
module m.mid {
    requires transitive m.lib;
    exports mid.api;
}

// m.client/module-info.java
module m.client {
    requires m.mid;
}
~~~

По этим директивам клиент явно **читает** `m.mid`; через transitive-ребро получает readability `m.lib`. При этом модульная читаемость — только первое условие. Для статического обращения к типу снаружи должны выполняться ещё правила доступности **пакета**, **класса** и **члена**. Например, `requires m.mid` не даёт права импортировать `lib.internal.Hidden`, когда `m.lib` вообще не экспортирует `lib.internal`.

Практическая компиляция и запуск из принятого G1 §6.1:

~~~bash
javac --release 21 -Xlint:all -d mods --module-source-path src -m m.lib,m.mid,m.client
java --module-path mods -m m.client/client.Main
~~~

Параметр `--module-source-path` описывает **раскладку источников named modules**; `-d mods` сохраняет результат компиляции; `--module-path mods` предоставляет готовые модули launcher; `-m m.client/client.Main` называет модуль и точку входа. Без настоящего `module-info.java`, `--module-path` и выбора main module нельзя считать опыт проверкой JPMS. [Полная файловая структура G1 §6.1](../../docs/plans/CORE-MODULES-CLASSLOADER.md) содержит **семь** исходников *в каждом* `base`/`opened` варианте, включая `lib.api.Greeter`, `mid.api.Bridge`, `client.Main` и `lib.internal.Hidden`.

## 2. requires, transitive и статическая доступность (JM-02, JM-03, JM-05, JM-07, JM-11, JM-14, JM-15)

**Обычный `requires m.lib`** задаёт читаемость библиотеки самому declaring module. **`requires transitive m.lib`** помимо этого транзитивно распространяет читаемость на модули, которые читают данный module; это особенно полезно для API, публичные типы которого ссылаются на типы другого модуля.

В нашем примере `client.Main` **напрямую импортирует** и `mid.api.Bridge`, и `lib.api.Greeter`. G1 stdout показал:

| Строка | Что реально напечатано | Объяснение |
|---|---|---|
| `JM01` | `bridge:hello-67` | Bridge вызывает Greeter |
| `JM02` | `hello-67` | Клиент статически использует публичный API библиотеки |
| `JM03` | `true` | `self.canRead(library)` на runtime graph |
| `JM04` | `true` | `lib.api` exported клиенту |
| `JM11` | `true` | `Main` находится в named module |

**Негативный контроль `JM_N01`:** заменить `requires transitive m.lib` на просто `requires m.lib` в `m.mid`. Тогда `m.client` по-прежнему читает `m.mid`, однако прямой импорт `lib.api.Greeter` больше не проходит. Изолированный `javac -XDrawDiagnostics` дал категорию `compiler.err.package.not.visible` с **причиной** `does.not.read: m.client,lib.api,m.lib`. Не объясняйте ошибку так, будто `Greeter` перестал быть public или пакет вдруг перестал экспортироваться: сломалось **ребро readability**.

**Другой отрицательный контроль `JM_N02`:** оставить модуль читаемым, но убрать `exports lib.api` (оставив лишь `opens lib.internal to m.client`). Диагностика верхнего уровня также `package.not.visible`, однако причина уже **not.exported**, то есть клиент читает модуль, но не получает пакет для компиляции. Это принципиально иное ограничение, требующее другого исправления.

Формальная структура директив тоже проверяется компилятором: нельзя дважды объявлять один `requires m.mid` (`JM_N05`: `duplicate.requires`), зависеть от себя (`JM_N06`: `cyclic.requires`), конфликтующим образом экспортировать тот же пакет (`JM_N08`: `conflicting.exports`), экспортировать отсутствующий пакет (`JM_N09`) или повторять адресата qualified export (`JM_N13`). Это не reflection ошибки, и `trySetAccessible` здесь неприменим.

**`java.base`** имеет особое нормативное положение: обычные named modules читают его неявно. Запись `requires static java.base` не поддерживается в исследованном Java 21 случае (`JM_N12`: `compiler.err.mod.not.allowed.here: static`). Но это не значит, что клиент не может использовать любые остальные `java.*` без `requires`: изолированный `JM_N14` с `import java.sql.Connection` без чтения `java.sql` получил `does.not.read: m.client,java.sql`. Существование класса в JDK не заменяет graph dependency. [JLS §7.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.1).

## 3. requires static: compile dependency без обязательного runtime resolution (JM-04, JM-17; JM-23 P1)

У `requires static m.opt` есть важная асимметрия: **для компиляции нужна наблюдаемая зависимость**, но при запуске модульная система не обязана разрешать optional module только из-за этой директивы. Это не «ленивая загрузка класса» и не аналог Java `Optional<T>`.

~~~java
// m.opt/module-info.java
module m.opt {
    exports opt.api;
}

// m.consumer/module-info.java
module m.consumer {
    requires static m.opt;
}

// m.consumer/consumer/StaticMain.java
package consumer;
public class StaticMain {
    public static void main(String[] args) {
        System.out.println("JS01=" + ModuleLayer.boot().findModule("m.opt").isPresent());
        System.out.println("JS02=" + StaticMain.class.getModule().isNamed());
    }
}
~~~

Две фактические команды **после отдельной компиляции обоих модулей** в G1 §6.2:

~~~bash
javac --release 21 -Xlint:all -d static/mods --module-source-path static/src -m m.opt,m.consumer
mkdir -p static/runtime
cp -R static/mods/m.consumer static/runtime/
java --module-path static/runtime -m m.consumer/consumer.StaticMain
java --module-path static/mods --add-modules m.opt -m m.consumer/consumer.StaticMain
~~~

| Вариант | `JS01` | `JS02` | Почему |
|---|---|---|---|
| Нет `m.opt` в runtime module path | `false` | `true` | Optional module не разрешён; consumer остаётся named |
| Модуль есть и явно добавлен через `--add-modules m.opt` | `true` | `true` | Дополнительный module включён в root set |

**Не делайте неверный вывод:** если вызвать реальный код `opt.api.OptionalApi.value()` при отсутствующем модуле, никакого обещания успешного выполнения нет. В проверенном `StaticMain` отсутствующий API *не вызывается*. Также «модуль присутствует в каталоге» ≠ «обязательно разрешён в boot layer»: второе отдельно контролируется условиями resolution и `--add-modules`.

## 4. exports, qualified exports, opens — три разных вопроса (JM-05–JM-11, JM-13; JM-19 P1)

**`exports lib.api`** делает пакет доступным для компиляции других читающих модулей. Публичный класс вне экспортированного пакета может быть законно загружен или найден по имени, но не обязательно доступен обычному статическому импорту. **`exports lib.api to m.mid`** — qualified export, он адресован конкретному модулю. Клиент `m.client` может читать `m.lib` и всё равно получить compile diagnostic, потому что он **не входит в список адресатов** (`JM_N03`: `not.exported.to.module: m.client`).

**`opens lib.internal to m.client`** решает другую задачу: позволяет **deep reflection** из указанного модуля согласно условиям Java 21 `AccessibleObject`. Это не разрешение `import lib.internal.Hidden;` в исходнике клиента.

~~~java
// base: m.lib/module-info.java
module m.lib {
    exports lib.api;
}
~~~

~~~java
// opened: альтернативный m.lib/module-info.java
module m.lib {
    exports lib.api;
    opens lib.internal to m.client;
}
~~~

Это **два разных исходных дерева**, а не два одновременных объявления одного модуля. В `client.Main` используется `Class.forName("lib.internal.Hidden")`, публичный конструктор, приватное `secret` и вызовы `Constructor.trySetAccessible()` / `Field.trySetAccessible()`, результат проверяется перед чтением поля. [Полный Java код G1 §6.1](../../docs/plans/CORE-MODULES-CLASSLOADER.md) нужен для реального повторения — краткая версия модуля не заменяет семь источников.

| Runtime наблюдение | `base`: только exports API | `opened`: плюс opens internal |
|---|---|---|
| `JM05=Module.isExported("lib.internal",client)` | `false` | **`true`** |
| `JM06=isOpen("lib.internal",client)` | `false` | `true` |
| `JM07=Constructor.trySetAccessible()` | `false` | `true` |
| `JM08=Field.trySetAccessible()` | `false` | `true` |
| `JM09=read secret` | `blocked` | `67` |
| `JM10=ClassLoader identity` | `true` | `true` |

**Центральная ловушка JM-10.** Метод [Module.isExported(String, Module)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Module.html#isExported(java.lang.String,java.lang.Module)) **во время выполнения** может вернуть `true` для **открытого** пакета из-за runtime семантики открытия. Это **не означает**, что package является `exports` для javac. G1 `JM_N04` заменяет декларацию на `opens lib.api to m.mid, m.client` **без exports**; компилятор отказывает на обычном `import lib.api.Greeter` с `package.not.visible / not.exported`. Следовательно, `Module.isExported(...)` и проверка compile-time `import` отвечают на разные вопросы.

**`open module m.lib { ... }`** по JLS открывает пакеты для глубокого доступа, но в нём нельзя одновременно прописывать `opens` — это отдельный compiler-negative `JM_N07`. Это не доказательство запуска реального positive `open module`: **он NOT RUN** в G1. Не распространяйте результат `opens lib.internal to m.client` на все прочие модули и пакеты.

Не следует путать успешный `Class.forName("lib.internal.Hidden")` с успешным `new lib.internal.Hidden()` из исходника: runtime lookup, статическая доступность и права на reflective invocation проверяются разными механизмами. См. [CORE-REFLECTION](reflection.md), [AccessibleObject 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AccessibleObject.html).

## 5. Named, unnamed, module path и загрузчик: ортогональные измерения (JM-01, JM-12, JM-13, JM-17)

Классы classpath-кода обычно принадлежат **unnamed module** того загрузчика, который их определил; их отражённый `Class.getModule().isNamed()` возвращает `false`. В нашем named `m.client` из реального `java --module-path ... -m ...` `JM11=true`. Однако «named module» **не означает** «обязательно собственный отдельный ClassLoader». В конкретном графе G1 `JM10=true`: `Main.class.getClassLoader()==Greeter.class.getClassLoader()`, хотя их `Class.getModule()` различаются. И наоборот, два отдельных custom loader могут определить **unnamed** классы одинакового имени, но разных runtime-типов, см. [CORE-CLASSLOADER §4](classloaders.md).

У `--class-path` и `--module-path` разные режимы конфигурации: путь до class-файла сам по себе не гарантирует, что тот будет найден в named module. Миграционные смешанные варианты automatic modules и module-path JAR здесь не воспроизводились (JM-22 P1). Не подменяйте реальный `-m` запуск класспасовым `java -cp ... client.Main` и не объявляйте два сценария эквивалентными.

**Типовая ошибка на интервью:** «У нас два модуля, значит JVM создала два classloader»; ответ — нет, JPMS регулирует граф модулей, а **defining loader + binary name** задают идентичность класса. Это две оси, не одно отношение.

## 6. uses/provides: descriptor проверяет контракты, но не запускает SPI (JM-16; JM-20 P1)

`uses service.Interface` декларирует зависимость от сервиса, `provides service.Interface with implementation.Type` — предложение поставщика по правилам JPMS. Но наличие этих директив **не вызывает само по себе** `ServiceLoader.load(...)` и не является доказательством обнаружения provider во время исполнения.

В G1 были изолированные статические проверки:
- `JM_N10` попытался объявить `uses lib.api.Importance`, где `Importance` — enum, и получил `compiler.err.service.definition.is.enum`;
- `JM_N11` объявил `provides lib.api.Contract with lib.api.BadProvider`, где `BadProvider` **не реализует** `Contract`, и получил `compiler.err.service.implementation.must.be.subtype.of.service.interface`.

Ни один из этих случаев **не является successful ServiceLoader discovery**. Для настоящей проверки понадобились бы корректные provider and consumer modules, runtime layer, вызов ServiceLoader и точный наблюдаемый результат — **NOT RUN**. [JLS §7.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7), [ServiceLoader API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html).

## 7. Разбор разных compiler-negative, одинаковой внешней ошибки (JM-02, JM-06–JM-11, JM-14–JM-16)

Взятые из [принятого G1 §7](../../docs/plans/CORE-MODULES-CLASSLOADER.md) **14/14** isolated JPMS negative tree, каждый с **семью исходниками base** и одной явно указанной заменой/добавлением, проверены независимо ролью 04 на Java21 с `javac --release 21 -XDrawDiagnostics`. Ошибка не засчитывалась только из-за отсутствия файла: полный деревообразный контекст восстановлен.

| Случай | Суть поломки | Наблюдаемая тематическая диагностика |
|---|---|---|
| `JM_N01` | `requires` вместо transitive | `package.not.visible / does.not.read` |
| `JM_N02` | Пакет существует, но не exported | `package.not.visible / not.exported` |
| `JM_N03` | Exports дан только `m.mid`, не `m.client` | `package.not.visible / not.exported.to.module` |
| `JM_N04` | `opens` используется без `exports` для import | `package.not.visible / not.exported` |
| `JM_N05`/`N06` | Duplicate requires / self dependency | `duplicate.requires` / `cyclic.requires` |
| `JM_N07` | `opens` внутри `open module` | `no.opens.unless.strong` |
| `JM_N08`/`N09` | Conflicting exports / missing package | `conflicting.exports` / `package.empty.or.not.found` |
| `JM_N10`/`N11` | Некорректный service type / provider | `service.definition.is.enum` / `service.implementation.must.be.subtype.of.service.interface` |
| `JM_N12`/`N13` | `requires static java.base` / duplicate qualified target | `mod.not.allowed.here` / `conflicting.exports.to.module` |
| `JM_N14` | Не читается `java.sql` | `package.not.visible / does.not.read` |

**Как диагностировать правильно:** сначала спросите, компилируется ли source; затем посмотрите модуль **клиента**, фактические `requires`/транзитивные readability edges, и только после этого — `exports` конкретного пакета нужному модулю. Проверка `opens` применима к правам reflection, не исправляет compile import. Для терминологически одинакового «package not visible» причины **does not read** и **not exported** разные.

**Граница доказательства:** это локальная независимая G1/04 проверка 17/17 negative (14 JM + 3 CL), а не 17 JUnit tests и не новые positive assertions. Runtime `JM07=false`/`JM09=blocked` — **корректно выполненный** Java-процесс, не compile-negative. Полный список изолированных исходников/фрагментов приведён в G1 §7, отдельно от обычного CI.

## 8. Матрица охвата CORE-MODULES 27/27

| ID | Приоритет | Что именно объяснено и где |
|---|---|---|
| JM-01 | P0 | §1/5: named module, package и descriptor |
| JM-02 | P0 | §1–2: requires/readability, not exported vs does not read |
| JM-03 | P0 | §2: requires transitive и JM_N01 |
| JM-04 | P0 | §3: requires static optional runtime |
| JM-05 | P0 | §2/4: exports и public contract |
| JM-06 | P0 | §4/7: qualified exports и JM_N03 |
| JM-07 | P0 | §2/4: unexported public class |
| JM-08 | P0 | §4: opens и reflective access |
| JM-09 | P0 | §4/7: opens ≠ compile-time exports |
| JM-10 | P0 | §4: Module.isExported runtime paradox |
| JM-11 | P0 | §1/2: readability vs accessibility |
| JM-12 | P0 | §5: named/unnamed и module/classpath |
| JM-13 | P0 | §5: module identity vs classloader identity |
| JM-14 | P0 | §2/4/7: module directive compiler-negative |
| JM-15 | P0 | §2: implicit java.base / java.sql readability |
| JM-16 | P0 | §6/7: static uses/provides typing |
| JM-17 | P0 | §1/3/5: реальные javac + java module-path |
| JM-18 | P1 | §4: open module; positive NOT RUN |
| JM-19 | P1 | §4: qualified exports/opens; beyond one graph NOT RUN |
| JM-20 | P1 | §6: ServiceLoader provider discovery NOT RUN |
| JM-21 | P1 | §9: add-exports/opens/reads flags NOT RUN |
| JM-22 | P1 | §5/9: automatic modules/migration NOT RUN |
| JM-23 | P1 | §3: module roots и add-modules m.opt verified in G1 |
| JM-24 | P2 | §9: ModuleLayer/multiple providers/jlink/JMOD NOT RUN |
| JM-25 | P2 | §9: security/agent/all layers NOT RUN |
| JM-26 | P2 | §9: multi-release JAR/module migration NOT RUN |
| JM-27 | P2 | §9: security-manager legacy/dynamic addOpens NOT RUN |

## 9. Источники A/C и пределы воспроизводимости

**Нормативные A, строго Java 21:** [JLS §7.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7), [§7.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.1), [§7.7.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.2), [§6.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6), [Module API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Module.html), [ModuleLayer API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ModuleLayer.html), [javac21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html), [java launcher21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html), [ServiceLoader21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html). [Реестр источников](../../docs/SOURCE_REGISTRY.md) определяет уровни доверия.

**Интервью C, реальные найденные формулировки:** [Q-IVAN: JPMS Q125/126](https://github.com/ivanjayasooriya/java-interview-prep/blob/main/docs/17-modules-jpms.md) (что такое JPMS и `module-info.java`). [Q-HRUDAY: ClassLoader/Modules](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md), [Q-TEAMLEAD](https://github.com/teamlead/java-interview-questions/blob/main/README.md), [Q-CHERNYSHOV-RU](https://github.com/Shell26/Java-Developer/blob/master/core.md) — широкие вопросы о class loading и окружении, **не** нормативные JPMS-источники. Для точных `Module.isExported` при `opens`, `requires static`, graph roots **нет доказанного самостоятельного интервью-вопроса** в изученных C (GAP); это обоснованное авторское углубление широкого Q-IVAN JPMS, не оценка частотности собеседований.

**G1/04 (историческая самостоятельная проверка):** на Debian OpenJDK21.0.12.1 проверены **18 уникальных исходников** (семь base JPMS + один отличающийся `module-info.java` в opened + четыре static JPMS + шесть classpath; при двух самостоятельных JPMS source trees всего **24 физических Java-файла**), 4 чистые компиляции и 6 JVM / 6 точных stdout diff = **11+11+2+2+21+2=49 строк**, не JUnit-тесты. **Теперь действительно опубликован** [полный Java21 modules/classloaders runner](../../examples/core/modules-classloader/README.md) с 24 файлами, [исполнимым `verify.sh`](../../examples/core/modules-classloader/verify.sh) и шестью fixtures ([JPMS base](../../examples/core/modules-classloader/modules/base/expected.stdout), [opened](../../examples/core/modules-classloader/modules/opened/expected.stdout), [optional absent](../../examples/core/modules-classloader/modules/static/absent.expected.stdout), [present](../../examples/core/modules-classloader/modules/static/present.expected.stdout), [LoaderProbe](../../examples/core/modules-classloader/classloaders/loader.expected.stdout), [MissingProbe](../../examples/core/modules-classloader/classloaders/missing.expected.stdout)). [Технический CI #45029310487](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45029310487) — **completed/SUCCESS только на technical SHA** `b7e7af3e8c44703cacf71e4f4e4343ecd60092e4`; **[итоговый CI #45030280851](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45030280851) — completed/SUCCESS только на интегрированном author SHA** `6eb170d8660a1f28fbf323198451faa84e68b040`. Оба запуска включали **Gradle/JUnit** и отдельный позитивный Java21 runner: **4 чистые `javac --release 21 -Xlint:all`**, **6 JVM с нулевым stderr** и **6 полных `diff -u`**; **11+11+2+2+21+2=49 строк stdout из шести JVM, а не 49 JUnit-тестов**. **17/17** isolated G1/04 compiler-negative с `-XDrawDiagnostics` проверялись отдельно **вне** обоих positive CI. Успех каждого запуска относится **только к его SHA**; для последующих авторских коммитов требуется собственная проверка CI (см. [PR #179](https://github.com/Melchiorius/backend-interview-labs/pull/179)).

**NOT RUN в опубликованном позитивном G2 runner:** работающий `ServiceLoader` discovery; positive `open module`; инструменты `--add-exports`, `--add-opens`, `--add-reads`; автоматические модули, migration JAR, dynamic `ModuleLayer`, alternate module graphs/JDK/OS/JMOD/jlink, code generated/agents/OSGi/Spring Boot, classloader concurrency/security, все inline code fences как самостоятельные компиляции, GUI/HTTP/anchors, JIT/JMH/GC, P2. К статусам [COVERAGE](../../docs/COVERAGE.md) и [CURRICULUM](../../docs/CURRICULUM.md) обращайтесь как к **отдельному gate G6**, а не меняйте их в G2. За G1 N1 (14 malformed JLS link labels) и N2 (18/19 links) отвечает отдельный erratum, **принятый G1-план не исправляем в этом PR**.
