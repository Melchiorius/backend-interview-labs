# CORE-ASSERT + CORE-OPTIONAL — Java 21 runnable

Два **самостоятельных учебных материала**, но один воспроизводимый комплект примеров: [CORE-ASSERT](../../../knowledge/01-java-core/assertions.md) (переключаемая семантика утверждений JVM) и [CORE-OPTIONAL](../../../knowledge/01-java-core/optional.md) (допустимое отсутствие результата, eager/lazy fallback, null-контракты). Код и эталоны взяты из [принятого G1 §6](../../../docs/plans/CORE-ASSERT-OPTIONAL.md). Примеры предназначены для наблюдения семантики Java 21, не для реализации бизнес-валидации через `assert`.

**Всего 15 + 15 + 36 = 66 строк stdout трёх независимых JVM-процессов. Это не 66 JUnit-тестов.**

## Запуск

Требуется **JDK 21** (`java`, `javac` в `PATH`), Bash, `mktemp`, `diff`. Из корня репозитория:

~~~bash
bash examples/core/assert-optional/verify.sh
~~~

Ожидается строка `CHECK PASSED: Java 21 assertions/Optional; default 15 + -ea 15 + Optional 36 stdout lines (3 JVM processes)`.

Скрипт компилирует **два** файла с `javac --release 21 -Xlint:all -d` (ошибки, любые warnings/stderr → FAIL), создавая `.class` только во временном каталоге `mktemp -d` (обязательный cleanup `trap`). Затем он **три раза запускает отдельную JVM**: `AssertProbe` с default assertion policy, `AssertProbe` с `-ea` и `OptionalProbe` в обычном режиме. Каждый процесс должен закончиться успешно с пустым stderr; его полный stdout сравнивается `diff -u` с **собственным** эталоном. Никакие `.class` или actual stdout не записываются в репозиторий.

| Исходник | JVM-конфигурация | Эталон | Строк |
| --- | --- | --- | --- |
| [src/AssertProbe.java](src/AssertProbe.java) | `java AssertProbe` (default, без `-ea`) | [AssertProbe.default.expected.stdout](AssertProbe.default.expected.stdout) | AS01–AS15 (15) |
| [src/AssertProbe.java](src/AssertProbe.java) | `java -ea AssertProbe` | [AssertProbe.ea.expected.stdout](AssertProbe.ea.expected.stdout) | AS01–AS15 (15) |
| [src/OptionalProbe.java](src/OptionalProbe.java) | `java OptionalProbe` | [OptionalProbe.expected.stdout](OptionalProbe.expected.stdout) | OT01–OT36 (36) |

## Почему нужны две JVM для одного AssertProbe

При **отключённых** утверждениях `AS01=false`, `AS02=0:0`, `AS03=skipped`, `AS05=0`, `AS12=skipped`: JVM не вычисляет даже условие выражения `assert`. При **`-ea`** `AS01=true`, `AS02=1:0`, `AS03=AssertionError:expected-67`, `AS04=2:1`, `AS05=1`, `AS12=AssertionError:true`. Детальное сообщение `: detail()` вычисляется лишь тогда, когда enabled условие оказалось ложным.

`AS05` — **намеренно неправильный код**, демонстрирующий потерю обязательного побочного эффекта, когда JVM отключит assertions. Так делать нельзя. Проверки входных данных и требования безопасности должны работать независимо от JVM policy: `AS09=NPE:value`, `AS10=IAE:negative-age`, `AS11=ISE:not-ready` одинаковы в обоих режимах. `AS06=1` показывает eager вычисление выражения `text()` в String-аргументе `Objects.requireNonNull`, `AS07=1` — отсутствие вызова message-Supplier для non-null, `AS08=NPE:required-message:2` — вызов Supplier при null.

**Дополнительные JVM-флаги** (не входят в три основных запуска runner): после компиляции тех же классов `-da`, `-dsa`, `-esa` и `-ea -da:AssertProbe` для пользовательского `AssertProbe` соответствуют default-эталону A0; `-ea:AssertProbe`, `-da -ea:AssertProbe`, `-ea -dsa` — enabled-эталону A1. Проверять такие конфигурации необходимо **каждой отдельной JVM** с полным сравнением с A0/A1. Флаг **`-esa` не включает assertions пользовательского `AssertProbe`** сам по себе; выполнение операторов `assert` внутри классов JDK этот пакет не проверяет. Особый случай ранней инициализации класса из JLS §14.10 также здесь не воспроизводится.

## Что наблюдать в OptionalProbe

- `OT01–OT04`: present/empty, `ofNullable(null)` даёт empty, а `Optional.of(null)` бросает runtime `NullPointerException`.
- `OT05–OT08`: `get()` и пустой `orElseThrow()` дают runtime `NoSuchElementException`; `orElseThrow(supplier)` создаёт выбранное exception.
- `OT09=Java:1`: `orElse(fallback(counter))` **eager** — fallback вызван, хотя значение присутствует. `OT10=Java:1`: `orElseGet` **lazy** — не вызывает supplier для present. `OT11=fallback:2`: вызывает supplier для empty.
- `OT12–OT14, OT17`: `map` может изменить тип; `map` с возвращённым `null` создаёт empty, `flatMap` возвращает Optional; **OT17 — именно `absent.map`, не `empty.filter`**. Отдельный реальный `empty.filter` с нулём вызовов predicate описан в [статье Optional, §6](../../../knowledge/01-java-core/optional.md); он **не добавлен** в неизменный 36-строчный G1 stdout.
- `OT15/OT16`: `filter` на **present** с отклонённым и принятым predicate. `OT18–OT21`: `or(Supplier<Optional>)` и случаи runtime NPE, если поставщик возвращает null; `flatMap` c null результатом тоже runtime NPE.
- `OT22–OT24`: null Supplier для empty `orElseGet` приводит к NPE, но для present допускается без вызова; `orElse(null)` возвращает null, **не** хранит null внутри Optional.
- `OT25–OT27`: `Optional.stream()` даёт 0/1 элементов, `flatMap(Optional::stream)`, `ifPresent`/ `ifPresentOrElse`.
- `OT28–OT35`: примитивные `OptionalInt/Long/Double`, `getAsX`, `stream`, отсутствие generic `map`; равенство value-based `Optional` проверяется через `equals`, а не гарантированную identity. `OT36`: present `orElseThrow()` возвращает значение.

## Граница доказанного

Обычный runner и CI **не запускают** семь дополнительных launcher-конфигураций, 11 самостоятельных compiler-negative (проверены ранее G1/04 по diagnostic keys), все inline snippets из Markdown, реальные assertions системных классов при `-esa`, special class-initialization rule, другие JDK/vendors/OS, JPMS, GUI Obsidian/IDE, внешние HTTP и Markdown anchors, ORM/JSON, JVM internals/JMM/JIT/GC/JMH/benchmarks. Объект runtime `AssertionError`/NPE/NSEE/IAE/ISE — **не ошибка компилятора**. P2 не выдаётся за проверенное.
