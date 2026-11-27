# CORE-MODULES — JPMS, модульный граф и границы доступа в Java 21

**Почему это спрашивают.** В обычном Java-коде класс может быть `public`, но клиент из другого модуля всё равно не сможет его импортировать. И наоборот: reflection в открытом пакете может прочитать приватное поле, хотя компилятор запрещает импорт даже публичного класса из того же пакета. Чтобы не путать эти случаи, различаем **readability модулей**, **compile-time exports** и **runtime opens**.

**Версия:** Java SE 21, нормативные [JLS §7.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7), [JLS §6.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6), [Module API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Module.html), [javac 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html), [java launcher 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html). Принятый [план G1](../../docs/plans/CORE-MODULES-CLASSLOADER.md) содержит исходники, локальный transcript и изолированные negative; история проверок и дополнительные эксперименты разобраны в §9.

**Как воспроизводить примеры.** Каждый `module-info.java` и каждый Java-файл с указанным путём размещайте отдельно, в каталоге соответствующего модуля под `--module-source-path`. Не вставляйте многофайловый листинг целиком в один файл. Для исходной лаборатории используйте полные деревья G1 и общий runner; дополнительные графы, альтернативные дескрипторы и фрагменты Main/Query относятся к отдельным временным вариантам. Полные исходники, пути, замены и команды этих проб приведены в [приложении A/B к отчёту 02](https://github.com/Melchiorius/backend-interview-labs/issues/342#issuecomment-7217861072).

**Адресное покрытие:** **JM-01–JM-17 = 17 P0**, **JM-18–JM-23 = 6 P1**, **JM-24–JM-27 = 4 P2**: **27/27 ID**. Вместе с [CORE-CLASSLOADER](classloaders.md) — **53/53 (33 P0, 12 P1, 8 P2)**. P1/P2 помечены границами, P2 не объявлены experimentally verified. Связанные существующие материалы: [правила доступа](access.md), [инспекция Reflection](reflection.md), [аннотации](annotations.md), [инициализация](initialization.md), [определяющий загрузчик](classloaders.md).

## 1. Что такое модуль: от исходников к графу (JM-01, JM-02, JM-11, JM-17)

*Пакет* группирует типы в пространстве имён. **Модуль JPMS** объединяет пакеты и задаёт границы зависимостей и доступа. Именованный (named) модуль бывает явным или автоматическим. У явного модуля есть явно заданный дескриптор, обычно полученный компиляцией `module-info.java` в `module-info.class`. Для запуска уже скомпилированного именованного модуля исходный файл не нужен; автоматический получает дескриптор при обнаружении подходящего JAR без явной декларации. В обоих случаях `ModuleDescriptor` описывает конфигурацию, а `java.lang.Module` представляет уже созданный runtime-модуль. Неназванный (unnamed) модуль не имеет имени и дескриптора, подробнее §5. Имя пакета само по себе не является глобальной идентичностью типа или указанием на модуль. См. [ModuleDescriptor API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/module/ModuleDescriptor.html), [CORE-CLASSLOADER](classloaders.md).

Не смешивайте четыре понятия:
- **Наблюдаемые (observable) модули:** кандидаты, доступные системе для поиска дескрипторов, с учётом режима и путей. Файл в произвольном каталоге не автоматически кандидат.
- **Корни (roots):** стартовые имена, от которых разрешаются зависимости; в приведённом запуске есть корень m.client, дополнительные корни можно задать --add-modules.
- **Resolution:** построение конфигурации из корней и зависимостей; вычисляется граф readability. При неразрешимой обязательной зависимости или конфликте resolution может завершиться ошибкой до main, это не поиск конкретного класса.
- **Runtime-модуль:** объект Module создаётся при определении конфигурации в слое JVM; наличие дескриптора, даже найденного ModuleFinder, не доказывает наличие этого объекта в boot layer. Загрузка/связывание/инициализация класса — отдельные события.

Пример ниже связывает три явных модуля. В схеме стрелка означает объявленную зависимость, а не загрузку класса; java.base и другие системные модули опущены ради ясности.

~~~text
наблюдаемые: m.client, m.mid, m.lib, ...
корень:      m.client
requires:    m.client -> m.mid -> m.lib
                               (transitive)
разрешены:   m.client, m.mid, m.lib, ...
readability: m.client -> m.mid
             m.client -> m.lib (подразумевается)
             m.mid    -> m.lib
~~~

В этой конкретной конфигурации ребро transitive добавляет читаемость клиенту, не превращая все его зависимости в экспортируемые пакеты. Полные правила включают дополнительные условия; см. [java.lang.module — resolution](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/module/package-summary.html). 

В компиляции нескольких модулей каждая единица имеет собственный дескриптор. Возьмём три настоящих модуля: `m.lib` с библиотекой; `m.mid` — промежуточный API; `m.client` — клиент. Это не три Gradle-подпроекта: здесь обсуждается **JPMS**, а инструменты сборки могут лишь подготовить нужные каталоги. В следующем листинге показаны три отдельных файла-дескриптора, по одному на модуль.

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

Параметр `--module-source-path` описывает **раскладку источников named modules**; `-d mods` сохраняет результат компиляции; `--module-path mods` предоставляет готовые модули launcher; `-m m.client/client.Main` называет модуль и точку входа. Именно для воспроизведения **этой лаборатории с тремя явно объявленными модулями** нужны показанная структура дескрипторов, module path и выбор main module. Это не универсальные условия любого опыта с JPMS: можно обнаружить автоматический модуль через ModuleFinder из обычной classpath-программы, исследовать Module API без -m, а также программно создавать конфигурации и слои. Два первых контрпримера фактически проверены в §5; программное создание слоя здесь не проверялось. [Полная файловая структура G1 §6.1](../../docs/plans/CORE-MODULES-CLASSLOADER.md) содержит **семь** исходников *в каждом* `base`/`opened` варианте, включая `lib.api.Greeter`, `mid.api.Bridge`, `client.Main` и `lib.internal.Hidden`.

## 2. requires, transitive и статическая доступность (JM-02, JM-03, JM-05, JM-07, JM-11, JM-14, JM-15)

**Обычный `requires m.lib`** задаёт читаемость библиотеки самому declaring module. **`requires transitive m.lib`** помимо этого даёт подразумеваемые рёбра чтения зависимого модуля для читающих его модулей согласно правилам resolution; это особенно полезно для API, публичные типы которого ссылаются на типы другого модуля.

В нашем примере `client.Main` **напрямую импортирует** и `mid.api.Bridge`, и `lib.api.Greeter`. G1 stdout показал:

| Строка | Что реально напечатано | Объяснение |
|---|---|---|
| `JM01` | `bridge:hello-67` | Bridge вызывает Greeter |
| `JM02` | `hello-67` | Клиент статически использует публичный API библиотеки |
| `JM03` | `true` | `self.canRead(library)` на runtime graph |
| `JM04` | `true` | `lib.api` exported клиенту |
| `JM11` | `true` | `Main` находится в named module |

**Негативный контроль `JM_N01`:** заменить `requires transitive m.lib` на просто `requires m.lib` в `m.mid`. Тогда `m.client` по-прежнему читает `m.mid`, однако прямой импорт `lib.api.Greeter` больше не проходит. Изолированный `javac -XDrawDiagnostics` дал категорию `compiler.err.package.not.visible` с **причиной** `does.not.read: m.client,lib.api,m.lib`. Не объясняйте ошибку так, будто `Greeter` перестал быть public или пакет вдруг перестал экспортироваться: сломалось **ребро readability**.

### Когда transitive нужен самому API

В историческом примере Bridge возвращает String: он не демонстрирует тип из другого модуля в объявлении возвращаемого типа. Рассмотрим отдельный граф lab.client → lab.bridge → lab.model. Модуль lab.model экспортирует model.api, lab.bridge экспортирует bridge.api. Клиент использует публичный метод Bridge.greeter(), тип результата объявлен в другом модуле.

~~~java
// lab.model/model/api/Greeter.java
package model.api;
public record Greeter(int value) {}
// lab.bridge/bridge/api/Bridge.java (отдельный файл)
package bridge.api;
import model.api.Greeter;
public final class Bridge {
    private Bridge() {}
    public static Greeter greeter() { return new Greeter(67); }
}
// lab.client/client/Main.java (отдельный файл)
package client;
import bridge.api.Bridge;
import model.api.Greeter;
public class Main {
    public static void main(String[] args) {
        Greeter g = Bridge.greeter();
        System.out.println("api=" + g.value());
    }
}
~~~

~~~java
// lab.model/module-info.java
module lab.model { exports model.api; }
// lab.bridge/module-info.java
module lab.bridge { requires transitive lab.model; exports bridge.api; }
// lab.client/module-info.java
module lab.client { requires lab.bridge; }
~~~

В Java21 отдельная строгая компиляция трёх модулей и запуск клиента дали api=67. С обычным requires lab.model в bridge импорт Greeter клиентом отвергнут (does.not.read); отдельно javac предупредил, что публичный API экспортируемого пакета использует возвращаемый тип из модуля, зависимость от которого не объявлена через requires transitive (leaks.not.accessible.not.required.transitive). Это предупреждение о контракте API bridge, а не ошибка отсутствия readability именно у client. Возвращаемый тип не входит в сигнатуру метода в смысле [JLS §8.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.2). Два решения: transitive у bridge, если чужой тип — сознательная часть публичного API; либо явный requires lab.model у client, если клиент сам принимает прямую зависимость. Второй вариант в пробе компилировался без -Werror с предупреждением о прежнем API bridge, затем также напечатал api=67. Когда же Greeter используется только внутри bridge, метод возвращает int, а клиент не импортирует Greeter, обычный requires достаточен: строгая компиляция и вывод internal=67. Нет основания делать транзитивной каждую внутреннюю зависимость: это расширяет контракт и связывает клиентов с её типами. См. [CORE-INTERFACES](interfaces.md).

**Другой отрицательный контроль `JM_N02`:** оставить модуль читаемым, но убрать `exports lib.api` (оставив лишь `opens lib.internal to m.client`). Диагностика верхнего уровня также `package.not.visible`, однако причина уже **not.exported**, то есть клиент читает модуль, но не получает пакет для компиляции. Это принципиально иное ограничение, требующее другого исправления.

Формальная структура директив тоже проверяется компилятором: нельзя дважды объявлять один `requires m.mid` (`JM_N05`: `duplicate.requires`), зависеть от себя (`JM_N06`: `cyclic.requires`), конфликтующим образом экспортировать тот же пакет (`JM_N08`: `conflicting.exports`), экспортировать отсутствующий пакет (`JM_N09`) или повторять адресата qualified export (`JM_N13`). Это не reflection ошибки, и `trySetAccessible` здесь неприменим.

**`java.base`** имеет особое нормативное положение: у явного модуля, отличного от java.base, зависимость от java.base подразумевается, если не объявлена явно. Явное `requires java.base;` допустимо; любые модификаторы requires для java.base запрещены в Java 21. Запись `requires static java.base` отвергнута в историческом G1 (`JM_N12`: `compiler.err.mod.not.allowed.here: static`). Но это не значит, что клиент не может использовать любые остальные `java.*` без `requires`: изолированный `JM_N14` с `import java.sql.Connection` без чтения `java.sql` получил `does.not.read: m.client,java.sql`. Существование класса в JDK не заменяет graph dependency. [JLS §7.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.1).

Повторная проверка правил на отдельном модуле lab.rules с пакетом rules:
- requires java.base; exports rules; opens rules; скомпилировалось строго: экспорт и открытие одного пакета — две независимые директивы.
- requires transitive java.base и requires static java.base дали отдельные ошибки (modifier.not.allowed.here и mod.not.allowed.here). Дубли exports и opens дали conflicting.exports и conflicting.opens; opens внутри open module — no.opens.unless.strong.
- Отсутствующий пакет в exports — ошибка; в opens по JLS допустим (javac здесь предупредил package.empty.or.not.found). Недоступный при компиляции модуль-адресат в to тоже допустим по JLS, в этой пробе два module.not.found warning, выход 0 без -Werror. Не превращайте предупреждение в языковой запрет.
- Дубли requires и циклические зависимости — разные причины отказа. Цикл может быть косвенным; исходные G1 negatives проверяют повтор и самозависимость, не все косвенные циклы. Повтор адресата запрещён отдельно внутри exports/opens. См. [JLS §7.7.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.2).

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

### Три состояния optional-модуля и два этапа отказа

Уточним старую лабораторию новой временной парой lab.app (requires static lab.opt) и lab.opt (exports opt.api). Main сравнивает обнаружение дескриптора в переданном каталоге и наличие runtime-модуля в boot layer. В классе OptionalApi есть public static int value(), возвращающий 67. Ниже фрагмент внутри метода main класса Main, а не самостоятельный исходный файл:

~~~java
var self = Main.class.getModule();
var opt = ModuleLayer.boot().findModule("lab.opt");
System.out.println("observable=" +
    java.lang.module.ModuleFinder.of(java.nio.file.Path.of(args[0]))
        .find("lab.opt").isPresent() +
    ",resolved=" + opt.isPresent() +
    ",reads=" + opt.map(self::canRead).orElse(false));
if (args.length > 1) {
    System.out.println("before-call");
    System.out.println("value=" + OptionalApi.value()); // import opt.api.OptionalApi;
}
~~~

В действительном Main импортирован opt.api.OptionalApi. Ниже фактические результаты, а не вывод из гипотезы:

| Runtime module path / roots | observable в данном finder | resolved в boot | reads (если найден) | С аргументом use |
|---|---|---|---|---|
| Только lab.app | false | false | false (нет Module) | before-call, затем NoClassDefFoundError, exit 1 |
| Оба, корень lab.app | true | false | false (нет Module) | тот же тип ошибки после before-call, exit 1 |
| Оба, дополнительный корень lab.opt | true | true | true | value=67, exit 0 |

Без use все три JVM завершились с кодом 0. Само поле observable измеряет поиск именно по переданному каталогу, не все системные модули. Отдельно javac с отсутствующим lab.opt отказал module.not.found даже при requires static. При обычном requires lab.opt, скомпилированном при наличии зависимости, запуск без неё остановился до main: Error occurred during initialization of boot layer / FindException. В отличие от этого optional-вариант дошёл до конкретного обращения к классу и получил NoClassDefFoundError. Это наблюдение конечного графа, не универсальное обещание порядка загрузки/связывания классов; см. [CORE-CLASSLOADER](classloaders.md) и [CORE-EXCEPTIONS](exceptions.md).

Директива `requires static transitive lab.opt;` тоже допустима по грамматике Java 21: compile-time зависимость обязательна, transitive readability вычисляется для разрешённого target; runtime static само по себе не добавляет его к корням. Совместную форму здесь не компилировали отдельно. Наличие типа из optional-модуля в публичном контракте API не делает безопасным его вызов при отсутствии модуля.

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

**`open module m.lib { ... }`** по JLS открывает пакеты для глубокого доступа, но в нём нельзя одновременно прописывать `opens` — это отдельный compiler-negative `JM_N07`. Это не доказательство запуска positive open module: он **NOT RUN в G1**. Новый ограниченный опыт ниже восполняет именно этот пробел. Не распространяйте результат `opens lib.internal to m.client` на все прочие модули и пакеты.

Не следует путать успешный `Class.forName("lib.internal.Hidden")` с успешным `new lib.internal.Hidden()` из исходника: runtime lookup, статическая доступность и права на reflective invocation проверяются разными механизмами. См. [CORE-REFLECTION](reflection.md), [AccessibleObject 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AccessibleObject.html).

### Как читать запросы к Module

У двух форм isExported и isOpen разные параметры: одна проверяет безусловное предоставление пакета, другая — предоставление конкретному модулю. Ни та, ни другая не проверяет, читает ли получатель владельца пакета; для этого отдельно вызывают `recipient.canRead(owner)`. Для собственных пакетов результат двухаргументных запросов к себе true; для unnamed module контракт API возвращает true в этих запросах. Объект Module отражает состояние runtime, а ModuleDescriptor — декларацию; динамически добавленные права не обязаны отражаться в descriptor. См. [Module API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Module.html).

Проверим независимые условия на новом графе: lab.friend requires lab.lib; lab.client не объявляет requires lab.lib. Модуль friend добавлен к корням при запуске клиента, поэтому библиотека разрешена, но клиент её не читает.

~~~java
// lab.lib/module-info.java
module lab.lib {
    exports lib.api to lab.friend;
    opens lib.hidden to lab.friend;
}
// lab.friend/module-info.java
module lab.friend { requires lab.lib; }
// lab.client/module-info.java
module lab.client {}
~~~

В lib.api.PublicApi есть public static value() → 67; в lib.hidden.Hidden — публичный конструктор и приватное поле secret=67. Query в клиенте получает три runtime-модуля через ModuleLayer.boot(), вызывает оба overload с нужным получателем, затем проверяет Field.trySetAccessible() на поле Hidden. В альтернативном исходном дереве библиотека объявлена `open module lab.lib { exports lib.api to lab.friend; }`.

| Фактический запрос | Qualified exports/opens | open module |
|---|---|---|
| client.canRead(lib), friend.canRead(lib) | false,true | false,true |
| isExported(lib.api): без адресата / friend / client | false,true,false | true,true,true |
| isExported(lib.hidden): без адресата / friend / client | false,true,false | true,true,true |
| isOpen(lib.hidden): без адресата / friend / client | false,true,false | true,true,true |
| isExported(lib.hidden,lib) / isOpen(lib.hidden,lib) | true,true | true,true |
| descriptor.isOpen / exports.size / opens.size | false,1,1 | true,1,0 |
| trySetAccessible поля из client | false | true, чтение 67 |

Строки отражают только конкретные пакеты и модули. В открытом модуле все его пакеты открыты во время выполнения, хотя дескриптор не содержит opens и qualified exports остаётся единственным compile-time export. В friend отдельный запуск подтвердил public API и чтение приватного поля: friend=67,true,67. Для компиляции двух отрицательных вариантов клиенту явно добавили requires lab.lib: импорт lib.api из неадресованного клиента отвергнут not.exported.to.module, а импорт lib.hidden из open module отвергнут not.exported. Core reflection по контракту [AccessibleObject](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AccessibleObject.html) предполагает readability при проверках доступа; этим объясняется чтение поля из не читающего lab.lib клиента в open-варианте. Это не обещание произвольного отражательного доступа (другие проверки сохраняются) и не общее правило для MethodHandles или обычного Java-кода. Сначала различайте readability, экспорт/открытие, затем конкретный тип/член и используемый API.

## 5. Named, unnamed, module path и загрузчик: ортогональные измерения (JM-01, JM-12, JM-13, JM-17)

Классы classpath-кода обычно принадлежат **unnamed module** того загрузчика, который их определил; их отражённый `Class.getModule().isNamed()` возвращает `false`. В нашем named `m.client` из реального `java --module-path ... -m ...` `JM11=true`. Однако «named module» **не означает** «обязательно собственный отдельный ClassLoader». В конкретном графе G1 `JM10=true`: `Main.class.getClassLoader()==Greeter.class.getClassLoader()`, хотя их `Class.getModule()` различаются. И наоборот, два отдельных custom loader могут определить **unnamed** классы одинакового имени, но разных runtime-типов, см. [CORE-CLASSLOADER §4](classloaders.md).

У `--class-path` и `--module-path` разные режимы конфигурации: путь до class-файла сам по себе не гарантирует, что тот будет найден в named module. Полная миграция и смешанный production deployment не воспроизводились ни в G1, ни в новой пробе; ниже отдельно проверены обнаружение automatic JAR и classpath-доступ. Не подменяйте реальный `-m` запуск класспасовым `java -cp ... client.Main` и не объявляйте два сценария эквивалентными.

| Свойство | Явный named | Automatic named | Unnamed |
|---|---|---|---|
| Имя и descriptor | задано декларацией | синтезируется для подходящего JAR | имя и descriptor отсутствуют |
| Readability | по разрешённому графу и runtime adjustments | читает все другие модули конфигурации; в JVM также unnamed modules | читает все модули по контракту Module API |
| Exports/opens | по descriptor и runtime adjustments, включая open module | descriptor не перечисляет exports/opens; в JVM все пакеты считаются exported/open | пакеты exported/open по контракту API |

Неназванный модуль принадлежит определяющему загрузчику, а не всей JVM как единый глобальный объект. Явный модуль не может объявить requires неназванного модуля по имени. Automatic — средство миграции, не эквивалент classpath; его широкие права во время выполнения описаны [ModuleDescriptor API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/module/ModuleDescriptor.html), здесь они не проверены полной смешанной конфигурацией. Отдельный modular JAR, помещённый на classpath, используется как обычный classpath-код: в пробе svc.api.Service из service.jar имел unnamed module и null name.

### Проверка JM-17 без явного дескриптора и без -m

Для небольшого JAR достаточно такого файла (не module-info.java):

~~~java
// auto/src/auto/api/Lib.java
package auto.api;
public final class Lib {
    private Lib() {}
    public static int value() { return 67; }
}
~~~

Из каталога временных проб выполнены команды. Файл auto/manifest.mf содержит Manifest-Version: 1.0 и Automatic-Module-Name: stable.example, затем пустую строку.

~~~bash
javac --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror -d auto/classes auto/src/auto/api/Lib.java
jar --create --file auto/demo-lib-1.0.jar -C auto/classes .
jar --create --file auto/renamed-2.0.jar --manifest auto/manifest.mf -C auto/classes .
jar --describe-module --file auto/demo-lib-1.0.jar
jar --describe-module --file auto/renamed-2.0.jar
~~~

Обе команды describe-module в авторской пробе 02 завершились с кодом 0. Ниже показаны содержательные строки stdout по очереди; завершающая пустая строка каждого вывода опущена. Точные выводы содержат соответственно 125 и 131 байт, с двумя LF в конце каждого. SHA-256: для имени из файла — `5abebda05e43322480783e2e769e6c1525e6bf94695819c275ac5ee4af8e19fa`, для имени из manifest — `69905c2c1fac8974011355fcb00851aa4ac6de0658e4d5bd7d6628a6cdf41ca3`. Полные байтовые представления и контрольные суммы приведены в [приложении C к отчёту 02](https://github.com/Melchiorius/backend-interview-labs/issues/342#issuecomment-7217883834).

~~~text
No module descriptor found. Derived automatic module.

demo.lib@1.0 automatic
requires java.base mandated
contains auto.api
~~~

~~~text
No module descriptor found. Derived automatic module.

stable.example@2.0 automatic
requires java.base mandated
contains auto.api
~~~

[ModuleFinder.of](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/module/ModuleFinder.html) позволяет найти дескриптор такого JAR даже из classpath-процесса. Следующая проба не объявляет модуля и не использует -m.

~~~java
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
public class Discover {
    public static void main(String[] args) throws Exception {
        Module self = Discover.class.getModule();
        System.out.println("classpath=" + self.isNamed() + "," +
            self.getName() + "," + (self.getDescriptor() == null));
        System.out.println("java.base=" + String.class.getModule().getName());
        for (String path : args) {
            var d = ModuleFinder.of(Path.of(path)).findAll()
                .iterator().next().descriptor();
            System.out.println("descriptor=" + d.name() +
                ",automatic=" + d.isAutomatic() + ",exports=" +
                d.exports().size() + ",boot=" +
                ModuleLayer.boot().findModule(d.name()).isPresent());
        }
        Class<?> c = Class.forName("auto.api.Lib");
        System.out.println("ordinary classpath=" + c.getModule().isNamed() +
            "," + (c.getModule() == self));
    }
}
~~~

~~~bash
javac --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror -d auto/probe auto/Discover.java
java -cp auto/probe:auto/demo-lib-1.0.jar Discover auto/demo-lib-1.0.jar auto/renamed-2.0.jar
~~~

~~~text
classpath=false,null,true
java.base=java.base
descriptor=demo.lib,automatic=true,exports=0,boot=false
descriptor=stable.example,automatic=true,exports=0,boot=false
ordinary classpath=false,true
~~~

Это реальный контрпример универсальному требованию явного module-info.java, --module-path и -m для любого опыта с JPMS. Оба JAR обнаружены по отдельности, их automatic descriptors найдены, но named runtime-модули не созданы в boot layer и Lib на classpath относится к unnamed module. Не делайте из этого вывод, что запуск named consumer с этими JAR проверен, что любой JAR годится для automatic module или что слои создаются автоматически при одном поиске. Имя из manifest стабильно относительно переименования файла при неизменном manifest, но не гарантирует неизменность API. Без него имя и версия выводятся из имени JAR; возможны некорректные имена, class-файлы в неназванном пакете, проблемы с service declarations и конфликтами. Нормативное правило ModuleFinder и его FindException, не универсальный эксперимент над всеми JAR.

**Учебная ловушка:** «У нас два модуля, значит JVM создала два classloader»; ответ — нет, JPMS регулирует граф модулей, а **defining loader + binary name** задают идентичность класса. Это две оси, не одно отношение.

## 6. uses/provides: descriptor проверяет контракты, но не запускает SPI (JM-16; JM-20 P1)

`uses service.Interface` декларирует зависимость от сервиса, `provides service.Interface with implementation.Type` — предложение поставщика по правилам JPMS. Но наличие этих директив **не вызывает само по себе** `ServiceLoader.load(...)` и не является доказательством обнаружения provider во время исполнения. Сервис — доступный класс или интерфейс, но не enum. Для явного named модуля поставщик должен быть публичным top-level или static вложенным типом, объявленным в самом модуле поставщика. Если он не имеет специального provider method, нужен subtype сервиса и публичный конструктор без параметров. Альтернатива — явно объявленный метод `public static provider()` без параметров. Его возвращаемый тип должен быть подтипом сервиса и удовлетворять одному из двух условий: быть объявленным в самом модуле поставщика либо быть объявленным в другом модуле и доступным коду модуля поставщика. Сам тип поставщика при этом не обязан быть подтипом сервиса, но требования к его объявлению сохраняются ([JLS §7.7.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.4)). Например, в независимой пробе [роли 04](https://github.com/Melchiorius/backend-interview-labs/pull/343#issuecomment-7218666119) публичная фабрика `Factory` объявила метод `public static Impl provider()`, где `Impl` — её приватный вложенный класс, реализующий сервис. Строгая компиляция и запуск `ServiceLoader` в именованных модулях завершились успешно; отдельная компиляция класса `Other` из того же пакета и модуля с обращением к `Factory.Impl` была отвергнута из-за `private`. Следовательно, тип результата, объявленный в модуле поставщика, не обязан быть доступен произвольному коду этого модуля. Обычные правила доступа в месте объявления метода и требование подтипа сервиса сохраняются. Пакет реализации экспортировать не нужно. Эти правила относятся к explicit named modules, а не ко всем способам развёртывания (для automatic modules и classpath действует режим provider constructor).

В G1 были изолированные статические проверки:
- `JM_N10` попытался объявить `uses lib.api.Importance`, где `Importance` — enum, и получил `compiler.err.service.definition.is.enum`;
- `JM_N11` объявил `provides lib.api.Contract with lib.api.BadProvider`, где `BadProvider` **не реализует** `Contract`, и получил `compiler.err.service.implementation.must.be.subtype.of.service.interface`.

Ни один из этих случаев **не является successful ServiceLoader discovery**. В G1 positive discovery был **NOT RUN**; ниже — отдельная проверка после первоначальной публикации. В JM_N11 не было подходящего provider method, поэтому правило subtype действительно применялось. [JLS §7.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7), [ServiceLoader API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html).

### Реальный SPI из трёх явных модулей

Стрелки зависимостей: lab.consumer → lab.service, lab.provider → lab.service. У клиента нет requires lab.provider. Система при service binding может разрешить provider по uses/provides, но это ещё не создание экземпляра.

~~~java
// lab.service/module-info.java
module lab.service { exports svc.api; }
// lab.provider/module-info.java
module lab.provider {
    requires lab.service;
    provides svc.api.Service with impl.Factory;
}
// lab.consumer/module-info.java
module lab.consumer { requires lab.service; uses svc.api.Service; }
~~~

~~~java
// lab.service/svc/api/Service.java
package svc.api;
public interface Service { int value(); }
// lab.provider/impl/Factory.java (отдельный файл)
package impl;
import svc.api.Service;
public final class Factory {
    private Factory() {}
    public static Service provider() {
        System.out.println("factory-called");
        return () -> 67;
    }
}
// lab.consumer/consumer/Main.java (отдельный файл)
package consumer;
import java.util.ServiceLoader;
import java.util.ServiceConfigurationError;
import svc.api.Service;
public class Main {
    public static void main(String[] args) {
        System.out.println("provider-resolved=" +
            ModuleLayer.boot().findModule("lab.provider").isPresent());
        try {
            var loader = ServiceLoader.load(Service.class);
            System.out.println("loader-created");
            Service service = loader.iterator().next();
            System.out.println("service=" + service.value() +
                ",module=" + service.getClass().getModule().getName());
        } catch (ServiceConfigurationError e) {
            System.out.println("missing-uses=" + e.getClass().getSimpleName());
        }
    }
}
~~~

Файлы размещаются под spi/src/<имя-модуля>/ согласно комментариям; команды фактической пробы:

~~~bash
javac --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror -d spi/mods --module-source-path spi/src -m lab.service,lab.provider,lab.consumer
java --module-path spi/mods -m lab.consumer/consumer.Main
~~~

~~~text
provider-resolved=true
loader-created
factory-called
service=67,module=lab.provider
~~~

Factory не реализует Service, имеет private constructor и неэкспортированный пакет impl; provider() возвращает реализацию Service. Строгая компиляция и JVM завершились 0, stderr пуст. Вариант без uses в descriptor consumer строго скомпилирован отдельно, запущен с дополнительным корнем lab.provider; наблюдение:

~~~text
provider-resolved=true
missing-uses=ServiceConfigurationError
~~~

Второй процесс завершился 0, потому что Main обработал ошибку; отсутствие uses не делает программу некомпилируемой само по себе, но ServiceLoader.load из явного модуля требует декларации uses. Наличие разрешённого provider не означает, что вызвана фабрика; loader лениво находит/создаёт поставщиков при использовании. Порядок нескольких providers и другие layer/configuration не проверялись. Consumer должен иметь доступ к service type; в данном графе requires lab.service и exports svc.api обеспечивают его. Тонкости classpath discovery и TCCL относятся к [CORE-CLASSLOADER](classloaders.md).

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

### Самопроверка: предскажите ошибку и исправьте её

Пусть из обязательных зависимостей приложения модуль app объявляет только requires bridge, bridge обычным requires зависит от model и экспортирует свой API, который возвращает model.api.Greeter. model экспортирует model.api и открывает model.hidden только bridge. В Main клиент импортирует Greeter и хочет прочитать приватное поле Hidden через reflection. У app есть также requires static addon; addon наблюдаем при компиляции, поэтому эта зависимость может быть проверена компилятором. При запуске addon лежит на module path, но не входит в корни, а кроме static-ребра нет никакой причины для его разрешения.

1. Компилируется ли импорт Greeter? Если нет, какой минимальный ремонт? Какие два способа отличаются контрактом API?
2. После исправления читаемости разрешено ли из app deep reflection в model.hidden? Достаточно ли простого exports?
3. Будет ли addon разрешён только из-за присутствия JAR? Что если Main вызывает его API?

> [!note]- Разбор
> 1. Нет: при обычном requires bridge→model отсутствует readability app→model. Явный requires model у app или requires transitive model у bridge добавляет нужную читаемость. Если тип из другого модуля — часть публичного контракта API bridge, transitive выражает этот контракт, а не скрытую реализацию; model.api уже экспортирован.
> 2. Нет: qualified opens адресован bridge, не app. Нужен соответствующий opens для app при соблюдении остальных условий reflection; exports сам по себе не открывает приватные члены. При обычном Java import одного opens недостаточно.
> 3. Нет: при запуске при описанных корнях и отсутствии иной причины resolution static-ребра недостаточно. Для конкретного вызова нужно обеспечить доступность и разрешение addon, например явно включить его в roots, либо предусмотреть корректное поведение без него. Отсутствие runtime-модуля может привести к ошибке связывания при обращении; static не гарантирует успешного вызова.

Это самостоятельная учебная задача с разбором, не приписываемый внешнему интервью-источнику вопрос и не ещё один фактически запущенный тест.

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
| JM-18 | P1 | §4: open module; positive NOT RUN в G1, новая ограниченная проба |
| JM-19 | P1 | §4: qualified exports/opens, новый граф friend/client; не все конфигурации |
| JM-20 | P1 | §6: ServiceLoader discovery NOT RUN в G1, новая SPI-проба |
| JM-21 | P1 | §9: флаги доступа; NOT RUN в G1, ограниченная проверка собственных модулей |
| JM-22 | P1 | §5/9: automatic descriptor discovery и classpath; полная миграция NOT RUN |
| JM-23 | P1 | §3: module roots и add-modules m.opt verified in G1 |
| JM-24 | P2 | §9: ModuleLayer/multiple providers/jlink/JMOD NOT RUN |
| JM-25 | P2 | §9: security/agent/all layers NOT RUN |
| JM-26 | P2 | §9: multi-release JAR/module migration NOT RUN |
| JM-27 | P2 | §9: security-manager legacy/dynamic addOpens NOT RUN |

## 9. Источники A/C и пределы воспроизводимости

### Временные настройки графа для собственных модулей

Иногда для теста нужно проверить одну границу, не меняя дескриптор. Флаги инструмента не заменяют архитектуру публичного API. Для лаборатории lab.client и lab.lib из §4:

| Флаг | Что изменяет | Где применяется |
|---|---|---|
| --add-reads lab.client=lab.lib | дополнительное ребро чтения из client | javac и java отдельно |
| --add-exports lab.lib/lib.api=lab.client | экспорт пакета для конкретного получателя | javac и java отдельно |
| --add-opens lab.lib/lib.hidden=lab.client | открытие пакета для runtime reflection | java; не разрешает обычный import при компиляции |

ALL-UNNAMED в позиции получателя относится ко всем unnamed modules; это более широкий охват, не рекомендуемая настройка по умолчанию. --add-modules добавляет корни, в отличие от изменения рёбер. Эти флаги применяют к конкретной фазе, а не к исходному descriptor навсегда. При javac --release 21 нельзя с помощью --add-exports расширить экспорт пакетов Java SE/JDK/стандартных модулей указанного релиза; наша проверка касается только собственных учебных модулей. См. документацию javac/launcher из §9. Не следует открывать внутренние пакеты JDK ради устранения случайной ошибки доступа.

Фактическая проверка: Call в отдельном lab.client без requires импортирует lib.api.PublicApi и печатает flags=67 при вызове value(). На этапе компиляции добавлены корень lab.lib, чтение и экспорт, strict javac прошёл. На этапе запуска без флагов чтения/экспорта, но с lab.lib как корнем, получен IllegalAccessError (does not read); с одним add-reads — IllegalAccessError (does not export). С обоими — exit 0 и flags=67. Это новые JVM, а не изменение графа после уже случившейся ошибки связывания. В отдельном Query из §4 добавлен только --add-opens для lib.hidden к client, где lab.friend — дополнительный корень: reflect=true,67 при прежнем client.canRead(lib)=false и descriptor.opens.size=1. Это подтверждает разницу между runtime-доступом и декларацией, но не гарантию любого reflection. ALL-UNNAMED и ограничения экспорта JDK здесь изложены по документации, не проверены отдельными запусками.

**Нормативные A, строго Java 21:** [JLS §7.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7), [§7.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.1), [§7.7.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7.2), [§6.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6), [Module API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Module.html), [ModuleLayer API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ModuleLayer.html), [javac21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html), [java launcher21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html), [ServiceLoader21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html). [Реестр источников](../../docs/SOURCE_REGISTRY.md) определяет уровни доверия.

**Интервью C, реальные найденные формулировки:** [Q-IVAN: JPMS Q125/126](https://github.com/ivanjayasooriya/java-interview-prep/blob/main/docs/17-modules-jpms.md) (что такое JPMS и `module-info.java`). [Q-HRUDAY: ClassLoader/Modules](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md), [Q-TEAMLEAD](https://github.com/teamlead/java-interview-questions/blob/main/README.md), [Q-CHERNYSHOV-RU](https://github.com/Shell26/Java-Developer/blob/master/core.md) — широкие вопросы о class loading и окружении, **не** нормативные JPMS-источники. Для точных `Module.isExported` при `opens`, `requires static`, graph roots **нет доказанного самостоятельного интервью-вопроса** в изученных C (GAP); это обоснованное авторское углубление широкого Q-IVAN JPMS, не оценка частотности собеседований.

**G1/04 (историческая самостоятельная проверка):** на Debian OpenJDK21.0.12.1 проверены **18 уникальных исходников** (семь base JPMS + один отличающийся `module-info.java` в opened + четыре static JPMS + шесть classpath; при двух самостоятельных JPMS source trees всего **24 физических Java-файла**), 4 чистые компиляции и 6 JVM / 6 точных stdout diff = **11+11+2+2+21+2=49 строк**, не JUnit-тесты. **Теперь действительно опубликован** [полный Java21 modules/classloaders runner](../../examples/core/modules-classloader/README.md) с 24 файлами, [исполнимым `verify.sh`](../../examples/core/modules-classloader/verify.sh) и шестью fixtures ([JPMS base](../../examples/core/modules-classloader/modules/base/expected.stdout), [opened](../../examples/core/modules-classloader/modules/opened/expected.stdout), [optional absent](../../examples/core/modules-classloader/modules/static/absent.expected.stdout), [present](../../examples/core/modules-classloader/modules/static/present.expected.stdout), [LoaderProbe](../../examples/core/modules-classloader/classloaders/loader.expected.stdout), [MissingProbe](../../examples/core/modules-classloader/classloaders/missing.expected.stdout)). [Технический CI #45029310487](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45029310487) — **completed/SUCCESS только на technical SHA** `b7e7af3e8c44703cacf71e4f4e4343ecd60092e4`; **[итоговый CI #45030280851](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45030280851) — completed/SUCCESS только на интегрированном author SHA** `6eb170d8660a1f28fbf323198451faa84e68b040`. Оба запуска включали **Gradle/JUnit** и отдельный позитивный Java21 runner: **4 чистые `javac --release 21 -Xlint:all`**, **6 JVM с нулевым stderr** и **6 полных `diff -u`**; **11+11+2+2+21+2=49 строк stdout из шести JVM, а не 49 JUnit-тестов**. **17/17** isolated G1/04 compiler-negative с `-XDrawDiagnostics` проверялись отдельно **вне** обоих positive CI. Исторические контрольные SHA-256 шести выводов находятся в [G1 L505–510](../../docs/plans/CORE-MODULES-CLASSLOADER.md#L505-L510). Успех каждого запуска относится **только к его SHA** (см. [PR #179](https://github.com/Melchiorius/backend-interview-labs/pull/179)).

**NOT RUN в опубликованном позитивном G2 runner:** работающий `ServiceLoader` discovery; positive `open module`; инструменты `--add-exports`, `--add-opens`, `--add-reads`; автоматические модули, migration JAR, dynamic `ModuleLayer`, alternate module graphs/JDK/OS/JMOD/jlink, code generated/agents/OSGi/Spring Boot, classloader concurrency/security, все inline code fences как самостоятельные компиляции, GUI/HTTP/anchors, JIT/JMH/GC, P2. Этот список относится к исходному позитивному runner, а не к новым временным опытам. За G1 N1 (14 malformed JLS link labels) и N2 (18/19 links) отвечает отдельный erratum; исходный план сохранён. [COVERAGE](../../docs/COVERAGE.md) фиксирует принятые статусы, [CURRICULUM](../../docs/CURRICULUM.md) — состав курса.

**Новые ограниченные проверки D-A21a (роль 02, первоначальная доработка статьи).** В [задаче #342](https://github.com/Melchiorius/backend-interview-labs/issues/342) опубликованы [основной авторский отчёт](https://github.com/Melchiorius/backend-interview-labs/issues/342#issuecomment-7217830566), [приложение A/B с полными исходниками, вариантами, командами и кодами завершения](https://github.com/Melchiorius/backend-interview-labs/issues/342#issuecomment-7217861072) и [приложение C с фактическими stdout/stderr и SHA-256](https://github.com/Melchiorius/backend-interview-labs/issues/342#issuecomment-7217883834). Debian OpenJDK 21.0.12.1+1-1-deb13u1-Debian, javac 21.0.12.1, LC_ALL=C.UTF-8; отключены переменные инъекции Java-опций (JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS, _JAVA_OPTIONS, JDK_JAVAC_OPTIONS) и CLASSPATH.

- 26 вызовов javac: 13 строгих положительных компиляций с параметрами `--release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror`, все exit 0, stdout/stderr пусты; 3 успешные компиляции с намеренными предупреждениями и 10 изолированных отрицательных компиляций (exit 1), те же параметры без -Werror. При explicit client requires и plain bridge осталось одно предупреждение, при отсутствующем friend — два, при отсутствующем opens package — одно; это успешные компиляции без -Werror.
- 21 вызов java: 16 exit 0, включая обработанный ServiceConfigurationError, и 5 ожидаемых runtime-отказов с exit 1.
- 5 вызовов jar: все exit 0. Всего 52 команды, 37 exit 0 и 15 exit 1; точные наблюдения приведены в §1–6 и §9.

Совместная форма requires static transitive рассмотрена только по грамматике и правилам resolution; ALL-UNNAMED и ограничение javac --release на изменение экспорта пакетов JDK — только по документации. Запуск named consumer с automatic JAR и программное создание слоя не проверялись. Новые временные исходники не входят в репозиторий и не исполняются общим runner или CI.

**Повторный запуск неизменённого общего runner (роль 02, первоначальная доработка D-A21a).** Восстановлены 31 точный входной файл (24 Java,6 fixtures,verify.sh), не полный клон; сверены Git SHA и mode до и после. В стандартном окружении с POSIX locale и в LC_ALL=C.UTF-8, без JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS, _JAVA_OPTIONS, JDK_JAVAC_OPTIONS и CLASSPATH, `bash verify.sh` завершился 0 в обоих случаях, внешний stderr пуст, сравнение двух выводов cmp завершилось с кодом 0. Скрипт выполняет четыре javac с --release 21 -Xlint:all, шесть JVM, проверяет отсутствие stderr компилятора/JVM и шесть полных diff -u. В отличие от строгих временных компиляций, скрипт не использует -Werror и не проверяет SHA внутри; 49 наблюдений из 6 JVM не означают 49 независимых тестов. Он печатает одну строку (104 байта с LF), а не все эталоны:

~~~text
CHECK PASSED: Java 21 JPMS/ClassLoader; 4 compilations, 6 JVM processes, 11+11+2+2+21+2=49 stdout lines
~~~

SHA-256 обеих строковых трасс 2f40eb643683b422fe9e7efe1392aa7424ea44f948d828189cbcbc4f00b1dfb8; все шесть fixture SHA-256 совпали с историческим G1 L505–510. Это предыдущие фактические запуски 02, а не новые локальные проверки при редактуре.

**Независимая практическая проверка JM17 (роль 03).** В [отдельном отчёте 03 #7217992291](https://github.com/Melchiorius/backend-interview-labs/pull/343#issuecomment-7217992291) приведены другие временные исходники, команды и точные потоки. На OpenJDK/javac/jar 21.0.12.1 из двух обычных JAR без module-info обнаружены автоматические дескрипторы number.box (версия 2.1 из имени файла) и lab.fixed (имя из manifest). Для обоих automatic=true, boot=false; Discovery и NumberBox принадлежат одному unnamed module, value=67, Object относится к именованному java.base. Семь первоначальных команд (две строгие компиляции, четыре вызова jar и одна JVM) завершились с кодом 0 и пустым stderr; ещё два вызова jar --list успешно подтвердили содержимое архивов. Вывод JVM — 5 строк, 193 байта с LF; два вывода describe-module — 130 и 125 байт с двумя LF в конце, соответственно. Полные потоки и их SHA-256 — в отчёте 03. Отрицательная компиляция для этого ограниченного положительного контрпримера не требовалась. Это не повтор всех 52 команд 02 и не запуск named consumer с automatic JAR или динамическим слоем.

Роль 03 независимо восстановила те же 31 защищённый входной файл (не полный клон), проверила их Git SHA и режимы до и после и запустила исходный runner в default POSIX и LC_ALL=C.UTF-8. Оба запуска exit 0, внешний stderr пуст, cmp exit 0. Тот же маркер из 104 байт и тот же SHA-256; все шесть fixture hashes совпали с G1 L505–510, шесть полных diff в каждом запуске прошли. Это отдельная проверка исходного runner, а не дополнительные JM/CL-строки или повтор новых проб в CI. CI исходного runner печатает только маркер, не все 49 строк.

**Границы новых проверок 02/03.** en_US.UTF-8/tr_TR.UTF-8 в локальных системах отсутствовали, NOT RUN. Не проверены другие ОС/JDK, полная миграция с automatic modules, динамическое создание слоёв, широкие вопросы security/agents/unloading/concurrency, JMOD/jlink, все inline code fences как самостоятельные компиляции, GUI/HTTP/anchors и P2. Эти ограничения не отменяют конкретных новых наблюдений и не означают полное покрытие CORE-CLASSLOADER.
