# CORE-MODULES + CORE-CLASSLOADER: воспроизводимый Java 21 runnable

Один набор исходников с **двумя самостоятельными учебными маршрутами**: [CORE-MODULES](../../../knowledge/01-java-core/modules.md) (JPMS-граф, `requires transitive`, `exports`/`opens`, `requires static`) и [CORE-CLASSLOADER](../../../knowledge/01-java-core/classloaders.md) (defining/initiating loader, class identity, `Class.forName`, TCCL и реальные ошибки загрузки). Исходники и эталоны — из [принятого G1 §6](../../../docs/plans/CORE-MODULES-CLASSLOADER.md); исполняемое тело не изменялось, в каждом Java-файле добавлены две начальные строки `// Theory:`.

**Всего 4 независимые компиляции, 6 JVM, 6 полных эталонов и 11+11+2+2+21+2 = 49 подписанных строк stdout. Это НЕ 49 JUnit-тестов.** Никакого отдельного Gradle/JUnit/Mockito-подпроекта.

## Запуск

Требуются JDK **21** (`javac` и `java` в PATH), Bash, `mktemp`, `diff`, `cp`, `rm`. Из корня репозитория:

~~~bash
bash examples/core/modules-classloader/verify.sh
~~~

Скрипт применяет `set -euo pipefail`, создаёт отдельный `mktemp -d` и удаляет его через `trap`. Все четыре `javac --release 21 -Xlint:all` обязаны завершиться с кодом 0, **без stderr и предупреждений**. Каждая из шести JVM запускается в **отдельном процессе**, exit=0 и stderr=0; весь stdout каждой JVM проверяется через `diff -u` относительно независимого fixture. Выводы `.class`, runtime-copy, модули и actual stdout существуют **только** в временном каталоге — Git не получает бинарных файлов. При первой ошибке выполнение прекращается.

## Модули: четыре запуска и три компиляции

| Режим | Исходное дерево | Запуск Java 21 | Эталон |
| --- | --- | --- | --- |
| JPMS base: `requires transitive`, `exports` | [modules/base/src](modules/base/src) | `java --module-path <mods-base> -m m.client/client.Main` | [base/expected.stdout](modules/base/expected.stdout) — JM01–JM11, 11 строк |
| JPMS opened: `opens lib.internal to m.client` | [modules/opened/src](modules/opened/src) | `java --module-path <mods-opened> -m m.client/client.Main` | [opened/expected.stdout](modules/opened/expected.stdout) — JM01–JM11, 11 строк |
| `requires static`, optional отсутствует | [modules/static/src](modules/static/src) | `java --module-path <runtime-only-consumer> -m m.consumer/consumer.StaticMain` | [absent.expected.stdout](modules/static/absent.expected.stdout) — JS01–JS02, 2 строки |
| `requires static`, optional разрешён | тот же [modules/static/src](modules/static/src) | `java --module-path <compiled-mods> --add-modules m.opt -m m.consumer/consumer.StaticMain` | [present.expected.stdout](modules/static/present.expected.stdout) — JS01–JS02, 2 строки |

Вместо имитации модульных путей на classpath скрипт действительно собирает `m.lib/m.mid/m.client` **два раза независимо** через `javac --module-source-path`. Полное дерево `base` из семи Java-файлов включает [m.lib descriptor](modules/base/src/m.lib/module-info.java), [Greeter](modules/base/src/m.lib/lib/api/Greeter.java), [Hidden](modules/base/src/m.lib/lib/internal/Hidden.java), [m.mid descriptor](modules/base/src/m.mid/module-info.java), [Bridge](modules/base/src/m.mid/mid/api/Bridge.java), [m.client descriptor](modules/base/src/m.client/module-info.java), [Main](modules/base/src/m.client/client/Main.java). У `opened` отдельная полная копия семи файлов, но [m.lib descriptor opened](modules/opened/src/m.lib/module-info.java) отличается **ровно директивой адресного opens**.

**JM01=bridge:hello-67**, **JM02=hello-67** и **JM03=true** показывают `requires transitive` и читаемость `m.lib` через `m.mid`; публичный `lib.api` exported. `JM05–JM09`: в `base` значения **false/false/false/false/blocked**, в `opened` — **true/true/true/true/67**. Критически важно: runtime `Module.isExported("lib.internal", m.client)` может стать **true из-за `opens`**, но это **не доказательство compile-time `exports` и права на `import lib.internal.Hidden`**. `JM10=true` в этих запусках также не означает «один JPMS-модуль = один ClassLoader».

Третий комплект `m.opt/m.consumer` состоит из [m.opt descriptor](modules/static/src/m.opt/module-info.java), [OptionalApi](modules/static/src/m.opt/opt/api/OptionalApi.java), [m.consumer descriptor](modules/static/src/m.consumer/module-info.java), [StaticMain](modules/static/src/m.consumer/consumer/StaticMain.java). **Одна компиляция** выдаёт оба named modules. В первом запуске временная копия содержит только `m.consumer`: **JS01=false/JS02=true**. Во втором `--add-modules m.opt` действительно разрешает optional module: **JS01=true/JS02=true**. **Наличие `m.opt` в каталоге само по себе не равнозначно его разрешению.** Программа специально *не вызывает* `OptionalApi`, когда модуль отсутствует: безопасный runtime-вызов отсутствующего API здесь НЕ доказан.

## ClassLoader: две отдельные JVM после четвёртой компиляции

Полный каталог из шести источников: [LoaderProbe](classloaders/src/LoaderProbe.java), [MissingProbe](classloaders/src/MissingProbe.java), [Token](classloaders/src/sample/Token.java), [Lazy](classloaders/src/sample/Lazy.java), [Parent](classloaders/src/sample/Parent.java), [Dependent](classloaders/src/sample/Dependent.java).

Первая classpath JVM читает байты только что собранного `Token.class` и создаёт два различных `ChildFirstTokenLoader`, **оба реально определяют** `sample.Token` в собственном namespace. Это не просто две ссылки на один Class: **CL09=true** (одинаковое binary name), **CL10=false** (разные `Class<?> `), **CL11=true** (разные defining loaders), **CL13=false** (`instanceof` с системно определённым `sample.Token`), **CL14=ClassCastException** при реальном `Class.cast`. Наблюдения `CL19=true` и `CL20=true` показывают, что прямой вызов `loadClass` на делегирующем загрузчике возвращает тот же родительский Class; эти наблюдения не проверяют регистрацию initiating loader в JVM. **CL04/CL05/CL06/CL07** наблюдают явную инициализацию `sample.Lazy` через `Class.forName(name,true,...)`; `CL15/CL16` — текущий контекстный загрузчик потока (TCCL), а не defining loader произвольного класса. **CL02/CL15** — наблюдения конкретного процесса, не универсальная гарантия всех сред. Программа и [эталон 21 строки](classloaders/loader.expected.stdout) сохраняют точный порядок, включая `CL06=initialized` между `CL05` и `CL07`.

Вторая JVM запускает `MissingProbe` из временной **runtime-копии** с существующим `Dependent.class`, но **без `Parent.class`**. Именно *после успешной компиляции* `Parent.class` удаляется **только внутри `mktemp`**, не из исходников, позитивного output и тем более не из Git. Получаем [эталон](classloaders/missing.expected.stdout) **CE01=ClassNotFoundException** для `absent.DoesNotExist` и **CE02=NoClassDefFoundError** для `sample.Dependent` с отсутствующей зависимостью. Это **runtime exceptions/errors**, не отрицательные тесты `javac`.

## Границы и доказательная дисциплина

В этот positive runner **не входят** 17 изолированных compiler-negative вариантов G1 §7 (14 полных JPMS дерева и 3 ClassLoader, роль 04 проверила отдельно по `-XDrawDiagnostics`/`compiler.err.*` вне обычного CI). Не выдавать `ClassCastException`, `ClassNotFoundException`, `NoClassDefFoundError` за compiler-negative; не смешивать `does.not.read` с `not.exported`. Не проверяются полные ServiceLoader discovery, ModuleLayer, `--add-opens/exports/reads`, альтернативные JDK/OS, иные module graphs, реальные classloader lock/concurrency, agents, OSGi/SpringBoot, JVM-LOADING internals, JIT/JMH/GC, отдельная компиляция каждого inline snippet двух статей, GUI Obsidian/IDE, HTTP/anchors и P2. Не переносить сюда как «новые» ранее созданные AN/RF `m.target/m.client` опыты. Патчи G1 N1/N2 и статусные COVERAGE/CURRICULUM — **отдельный scope**.
