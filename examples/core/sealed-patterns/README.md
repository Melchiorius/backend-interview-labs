# CORE-SEALED + CORE-PATTERNS — два воспроизводимых примера Java 21

Это **один учебный runnable с двумя самостоятельными программами**, а не отдельный Gradle-модуль, задание с автопроверкой или набор из 31 JUnit-теста. Он дополняет статьи [CORE-SEALED](../../../knowledge/01-java-core/sealed.md) и [CORE-PATTERNS](../../../knowledge/01-java-core/patterns.md). Исходная исследовательская постановка и отрицательные контрпримеры описаны в [принятом плане G1](../../../docs/plans/CORE-SEALED-PATTERNS.md), §7–8.

## Запуск

Нужен **JDK 21**, доступные в `PATH` команды `javac` и `java`, Bash, `mktemp` и `diff`. Из корня репозитория:

```bash
bash examples/core/sealed-patterns/verify.sh
```

Или из любого каталога — указать путь к `verify.sh`. Скрипт компилирует оба исходника с `javac --release 21 -Xlint:all` **без допустимых предупреждений**, запускает **две разные JVM-программы**, отдельно сверяет полный stdout каждой через `diff -u` и удаляет временный каталог с `.class` после выхода. Расхождение эталона, ошибка компиляции, диагностическое предупреждение или исключение завершает проверку ненулевым кодом. На успехе печатается 11 строк S01–S11, 20 строк P01–P20 и `CHECK PASSED`.

Исходники и эталоны: [SealedProbe.java](src/SealedProbe.java) → [sealed.expected.stdout](sealed.expected.stdout); [PatternsProbe.java](src/PatternsProbe.java) → [patterns.expected.stdout](patterns.expected.stdout). Эталоны — точные UTF-8 файлы с завершающим LF.

## Сценарий 1. Разрешённые наследники и исчерпывающий switch

Начните с `SealedProbe` и сравните `S01–S03`: `Result` разрешает напрямую только `Ok`, `Err` и `Pending`; `Later` — **косвенный** потомок через `non-sealed Pending`. Метка `case Pending p` покрывает и `Later` (`S03=pending`), поэтому `switch` по `Result` исчерпывающий без `default`. `S04–S05` показывают implicit `permits`, `S06–S10` — допустимые типы-наследники и generic-вариант; `S11=1` демонстрирует, что `sealed` **не означает** неизменяемое состояние.

## Сценарий 2. Pattern matching, null и runtime-ошибки

В `PatternsProbe` проследите `P01–P06`: порядок `case`, `when` и `case null`; `P07–P09`: вложенные record patterns с guard; `P10–P12` и `P19`: область видимости pattern variable при `instanceof`, раннем `return` и `&&`.

Особенно важно **не смешивать три разных случая**:

- `P13=not-point`: компонент `Envelope.point()` равен `null`, вложенный `instanceof Envelope(Point(...))` **не совпадает**; это обычный `false`, не исключение.
- `P16=NPE`: `default` в `switch` **не обрабатывает null selector** без `case null`; в `P18=caught` явно используется `case null, default`.
- `P17=MatchException:IllegalStateException`: record accessor `Broken.number()` бросает исключение при деконструкции; pattern matching оборачивает его в `MatchException` с исходной причиной. `P20=MatchException`: при exhaustive `switch` значение `Yes(null)` не совпадает ни с одной вложенной record pattern, несмотря на исчерпываемость по sealed-типу `Answer`.

`P17`/`P20` — **runtime**-ситуации, а не отрицательные примеры компиляции. `when` — корректный guard в Java 21; старую preview-форму `case ... && ...` применять нельзя.

## Границы проверки

Обычный `verify.sh` проверяет только **две позитивные Java-программы и их 31 строку stdout**, а не 31 независимый unit-тест. Изолированные compiler-negative **N01–N24** и положительная компиляция named-module **M01** исследованы отдельно в G1 и не входят в этот runner/CI. Данный пример не подтверждает JPMS module-path runtime, binary evolution, JMH/JIT/GC, компиляцию всех inline snippets статей, работу в Obsidian/IDE и поведение других JDK/OS.
