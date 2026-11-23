# CORE-NUMBERS + CORE-DATETIME: два запускаемых примера Java 21

Лаборатория дополняет две самостоятельные статьи: [числовые типы и точность](../../../knowledge/01-java-core/numbers.md) и [дата, время и часовые пояса](../../../knowledge/01-java-core/datetime.md). Полные Java-программы и ожидаемый вывод взяты из [принятого G1 §5](../../../docs/plans/CORE-NUMBERS-DATETIME.md) без изменения исполняемой логики. У каждого source есть короткий комментарий `// Theory:` к соответствующей статье.

**33 + 38 = 71 подписанная строка stdout из двух отдельных JVM. Это не 71 JUnit-тест.** Здесь нет искусственного Gradle/JUnit/Mockito-подпроекта.

## Команды

Требуются JDK 21 (`java` и `javac`), Bash, `mktemp`, `diff`, `wc`, `cut`, `sha256sum`. Из корня репозитория:

~~~bash
bash examples/core/numbers-datetime/verify.sh
~~~

Скрипт использует `set -euo pipefail`, временный каталог с `mktemp -d` и `trap` для очистки. Одна компиляция `javac --release 21 -Xlint:all` должна иметь exit 0 и **пустой stderr без warnings**. Затем каждый класс запускается в **своей JVM**, с exit 0 и пустым stderr. Для каждой трассы выполняются **полный `diff -u`, проверка числа строк и точный SHA-256**, сопоставленный с принятым G1 и файлом эталона. Скомпилированные классы сохраняются только во временном каталоге.

Чтобы отдельно изучить один из двух примеров (из корня репозитория):

~~~bash
tmp="$(mktemp -d)"
trap 'rm -rf -- "$tmp"' EXIT
javac --release 21 -Xlint:all -d "$tmp" \
  examples/core/numbers-datetime/src/NumbersProbe.java \
  examples/core/numbers-datetime/src/DateTimeProbe.java
java -cp "$tmp" NumbersProbe
# Или отдельно:
java -cp "$tmp" DateTimeProbe
~~~

| Тема | Исходник | Точный эталон |
| --- | --- | --- |
| Числа, IEEE 754 и BigDecimal | [src/NumbersProbe.java](src/NumbersProbe.java) | [numbers.expected.stdout](numbers.expected.stdout) — NM01–NM33, 33 строки |
| java.time, зоны и DST | [src/DateTimeProbe.java](src/DateTimeProbe.java) | [datetime.expected.stdout](datetime.expected.stdout) — DT01–DT38, 38 строк |

## NumbersProbe: наблюдение и механизм

**NM01–NM06:** переполнение целого `int` не бросает исключение, в отличие от `Math.addExact` и `multiplyExact`. Деление целых отбрасывает дробную часть; деление целого на ноль бросает `ArithmeticException`. При сложении `byte` с `int` результат продвигается до `int`.

**NM07–NM12:** `0.1 + 0.2` вычисляется с двоичной погрешностью; NaN, бесконечность и знаковый ноль имеют разные правила сравнения. **NM13–NM16:** строковое `new BigDecimal("0.1")`, `new BigDecimal(0.1)` и `BigDecimal.valueOf(0.1)` — **разные операции**. NM14 печатает длинное точное десятичное представление двоичного `double`, а не сокращённое `0.1`.

**NM17–NM22:** `scale` и `precision` отличаются; `BigDecimal.equals` учитывает масштаб, `compareTo` сравнивает числовые значения. Разные размеры `HashSet` и `TreeSet` соответствуют их различным правилам эквивалентности. **NM23–NM29:** неточное десятичное деление без заданного правила приводит к исключению; `RoundingMode` и `MathContext` задают конкретные правила. **NM30–NM33:** произвольная точность `BigInteger`, масштабирование BigDecimal и один демонстрационный результат `Math.fma`. Этот запуск **не проверяет финансовые политики и точность FMA на трудных наборах входных данных**.

## DateTimeProbe: переходы часовых поясов

Первые строки DT01–DT08 используют **`Clock.fixed`**, явные `Europe/Berlin` и `Asia/Tokyo`. Здесь не используются текущее системное время, `ZoneId.systemDefault()`, locale по умолчанию или случайные значения.

**Весенний DST gap:** 31 марта 2024 г. `02:30` в Berlin не существует (`DT10=0`), обычная привязка к зоне сдвигает его к `03:30` (DT11), а строгая отвергает указанный неправильный offset (DT12). **Осенний overlap:** 27 октября 2024 г. `02:30` имеет два допустимых смещения (DT13); DT14/DT15 обозначают два разных момента времени с разницей 60 минут (DT16).

**DT17–DT20:** `withZoneSameInstant` сохраняет `Instant`, `withZoneSameLocal` сохраняет местное время, но меняет момент. **DT21–DT27:** `plusDays(1)` через переход к летнему времени даёт **23 прошедших часа**, а `plusHours(24)` — ровно **24**; `Period` и `ChronoUnit.DAYS` показывают календарный день. DT28–DT38 — форматирование с `Locale.ROOT`, парсинг, конкретные runtime-исключения и иммутабельность результата.

## Воспроизводимость и границы доказанного

Наблюдения G1 получены на **OpenJDK 21.0.12.1 (Debian 13), tzdb 2026b**. Версия правил `Europe/Berlin` и `Asia/Tokyo` в исполняющем JDK может поменяться. Поэтому stdout с DST не следует обещать побайтово одинаковым для всех будущих tzdb, JDK, поставщиков и ОС. При изменении правил программа обнаружит отличие через `diff`/SHA, а не молча подменит эталон.

**8 отдельных compiler-negative из G1 §6** (4 Numbers + 4 DateTime) проверялись отдельно с `javac -XDrawDiagnostics`, но **не входят в этот positive runner/CI**. Опечатка «(6)» в G1 §5 — известная неблокирующая документарная ошибка; принятый G1-план не изменён. Runtime `ArithmeticException`, `DateTimeParseException`, `DateTimeException` — не compiler-negative.

**NOT RUN этим набором:** каждый встроенный Java-фрагмент статей, иные версии tzdb/JDK/OS и zone provider, финансовые соглашения/DB/I18N, все календарные системы, производительность/JMH/JIT/GC, P2 и проверка Obsidian/IDE GUI, якорей Markdown и внешних HTTP-ссылок. Успешный CI подтверждает две конкретные трассы в своей среде, но не универсальность всех временных правил.
