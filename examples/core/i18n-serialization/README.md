# CORE-I18N + CORE-SERIAL — два независимых Java 21 runnable

Лаборатория дополняет самостоятельные статьи [Интернационализация и локализация](../../../knowledge/01-java-core/internationalization.md) и [Сериализация Java](../../../knowledge/01-java-core/serialization.md). Оба полных Java source и UTF-8 stdout-эталона взяты из [принятого G1 §6](../../../docs/plans/CORE-I18N-SERIAL.md) **без изменения исполняемых тел**: к каждому Java-файлу добавлена одна начальная root-relative строка `// Theory:` на свою статью.

**Два процесса JVM, 29 + 20 = 49 подписанных строк stdout, НЕ 49 JUnit-тестов.** Отдельный Gradle/JUnit/Mockito-подпроект не создаётся. Доверенный сценарий сериализации **не принимает никаких внешних, сетевых, файловых или пользовательских сериализованных данных**.

## Запуск и проверка

Требуются JDK **21** (`javac`, `java`), Bash, `mktemp`, `diff`, `wc`, `sha256sum`, `cut`. Из корня репозитория:

~~~bash
bash examples/core/i18n-serialization/verify.sh
~~~

Исполняемый `verify.sh` использует `set -euo pipefail`, `mktemp -d` и `trap` с удалением временных артефактов. Одна `javac --release 21 -encoding UTF-8 -Xlint:all` компилирует **оба** источника с exit 0, **пустым stderr и нулём warnings**. Затем **два отдельных JVM-процесса** должны завершиться с exit 0 и пустым stderr; каждый полный stdout сверяется через `diff -u`, точное число строк (29 / 20) и два SHA-256 (actual и fixture) с нормативным G1. В Git не сохраняются `.class`, сериализованные bytes и временные stdout.

Самостоятельное чтение каждого вывода:

~~~bash
tmp="$(mktemp -d)"
trap 'rm -rf -- "$tmp"' EXIT
javac --release 21 -encoding UTF-8 -Xlint:all -d "$tmp" \
  examples/core/i18n-serialization/src/I18nProbe.java \
  examples/core/i18n-serialization/src/SerializationProbe.java
java -Djava.locale.providers=CLDR -Dsun.stdout.encoding=UTF-8 -cp "$tmp" I18nProbe
# Вторая JVM, отдельно:
java -Dsun.stdout.encoding=UTF-8 -cp "$tmp" SerializationProbe
~~~

| Учебная программа | Полный исходник | Полный эталон |
| --- | --- | --- |
| I18N: L01–L29 (29 строк) | [I18nProbe.java](src/I18nProbe.java) | [i18n.expected.stdout](i18n.expected.stdout) |
| SERIAL: S01–S20 (20 строк) | [SerializationProbe.java](src/SerializationProbe.java) | [serial.expected.stdout](serial.expected.stdout) |

## I18nProbe: наблюдение и механизм

**L01–L03:** `Locale.forLanguageTag("sr-Latn-RS")` отдельно содержит язык, письменность и регион, а `Locale.US`/`Locale.GERMANY` — не сам формат чисел, а выбор правил локализации. `Locale.Category.FORMAT` и `DISPLAY` нужны для разных задач: **этот пример явно передаёт Locale и не тестирует изменение default categories**. Связывайте его с объяснением в [статье](../../../knowledge/01-java-core/internationalization.md).

**L04–L07:** один `12345.67` отображается как `12,345.67` (US) и `12.345,67` (DE); `DecimalFormatSymbols` определяет значащие символы разделителей, L06/L07 специально печатают Unicode code points. Это **наблюдаемые CLDR-данные установленного JDK**, не вечные правила Java SE. L08 использует `setParseBigDecimal(true)` и действительно получает `BigDecimal:67.25`, а L09 через `ParsePosition` частично читает **9 из 13** UTF-16 единиц строки, не выдавая исключение; L10 — реальный **runtime** `ParseException`. L11/L12 показывают отличие `RoundingMode.HALF_EVEN` (2) и `HALF_UP` (3) для 2.5, **не универсальный выбор денежной политики**.

**L13–L18:** ограничение дробных разрядов форматтера, два локализованных длинных представления фиксированного `LocalDate` и ISO-формат `Instant` — разные контракты. `DateTimeFormatter.withLocale` не меняет абсолютное мгновение времени; этот trace не доказывает независимость всех форматтеров от `ZoneId`.

**L19–L23:** `"TITLE".toLowerCase(Locale.ROOT)` и с турецкой `tr-TR` дают разные результаты (L20 содержит U+0131); длина байтов `é` в UTF-8 и UTF-16BE — **по 2**, но это не длина `String` и не выбор `Locale`. **L24–L26** читают только вложенные в `I18nProbe` локальные `ListResourceBundle` (EN/DE) и наблюдают `MissingResourceException` для отсутствующего ключа; никакого скачивания ресурсов нет. L27–L29: Unicode extension тега Locale, целочисленное форматирование с округлением и parse немецкого числа.

**Воспроизводимость:** в рамках принятого G1 наблюдения получены на Debian GNU/Linux 13 / **OpenJDK 21.0.12.1** с `-Djava.locale.providers=CLDR` и `-Dsun.stdout.encoding=UTF-8`. `sun.stdout.encoding` — **implementation-specific параметр OpenJDK**, не часть Java SE. **Ревизия CLDR отдельно не измерялась**. У других поставщиков JDK, конфигураций provider, обновлений CLDR или ОС строки L04/L05/L14/L15 и иные локализованные данные **могут отличаться**; строгая проверка должна честно показать diff, а не молча менять эталон.

## SerializationProbe: доверенные байты и пределы фильтрации

Весь граф строится **самой Java-программой**: `Node root` и `Node child` содержат взаимный цикл, `Receipt` — локально созданный record, `Kind.ACTIVE` — enum. Метод `write(Object...)` сериализует их через `ObjectOutputStream` **в локальный `ByteArrayOutputStream`**. Каждый `ObjectInputStream` в исходнике получает исключительно `new ByteArrayInputStream(...)` из этого метода: **никаких сетей, файлов, argv, public deserializer, внешних объектов или exploit/gadget payloads**.

**S01–S09:** запись существует; `ObjectStreamClass.lookup(Node.class).getSerialVersionUID()` возвращает заявленный UID **67**; `transient session` после чтения стал `null`, тогда как `static Node.category` имеет актуальное значение `after` и не является частью object stream. `next.next == restored` показывает восстановление циклических ссылок, а два последовательных `readObject` одного ранее дважды записанного экземпляра дают тот же restored instance (stream handles). Это **не** демонстрация совместимости двух разных версий класса/UID.

**S10:** второй stream **на тех же собственных доверенных bytes** ставит filter, возвращающий `REJECTED` для `Node.class`, поэтому `InvalidClassException:filter` означает **отказ фильтра**, а **не несовпадение serialVersionUID**. Более ранний filter с ограничением depth/references/bytes может возвращать `UNDECIDED`, и **UNDECIDED не является автоматическим отклонением или allowlist**. Ни такой пример, ни один successful filter не доказывают безопасности произвольных недоверенных потоков Java native serialization.

**S11/S12:** `NotSerializableException` возникает при попытке записать `new Object()`, а `InvalidObjectException:validation` — когда private `Node.readObject` отвергает пустое `name` **после чтения собственноручно записанных байтов**. Это **runtime**, не `javac` compiler-negative. **S13–S16:** record `Receipt` использует canonical constructor при создании и повторно при восстановлении (`canonicalCalls`: 1 → 2), значения полей сохраняются, а экземпляр новый. S17 показывает enum identity; S18 — marker interface `Serializable`; S19 — наблюдаемый UID record, S20 — исходный transient value до сериализации.

**Граница безопасности:** пример не должен применяться к неуправляемому пользовательскому или сетевому `ObjectInputStream`. У него нет production-grade политики фильтрации, защиты от DoS, доказательства sandbox/allowlist и эксперимента с двумя сборками/несовместимыми UID. Для реальных API отдельно решают формат контракта, DTO, эволюцию схем, проверку входа и модель угроз.

## Compiler-negative, warning-only и runtime — три независимых класса

В [G1 §7](../../../docs/plans/CORE-I18N-SERIAL.md) приведены **12 самостоятельных Java21 compiler-negative**: L_N01–L_N06 и S_N01–S_N06. Их источник — несоответствие типов/перегрузок/marker или `Externalizable`:
- **L_N01–L_N06:** `String` вместо `Locale`, `String` вместо `RoundingMode` и `FormatStyle`, передача `Locale` в `LocalDate.format`, числовой ключ `ResourceBundle.getString`, `Locale` вместо имени `Charset`.
- **S_N01–S_N06:** незавершённая реализация `Externalizable`, ошибочный `@Override` метода для marker `Serializable`, неверный `writeObject` return type, неверный тип результата `readObject`, `String` вместо `ObjectInputFilter` и `ByteArrayOutputStream` вместо `InputStream`.

Роль 03 самостоятельно проверила все **12/12** в отдельных каталогах на `javac --release 21 -XDrawDiagnostics`: каждый **exit 1 с ожидаемым смысловым `compiler.err.*`**. Ещё **2 неправильных объявления `@Serial` при `-Xlint:serial` дали warnings, но `javac exit 0`**; это не настоящие compiler-negative. **Ни один из 14 случаев не исполняется автоматически в данном двух-JVM positive `verify.sh`/CI**: это отдельные локальные свидетельства, а не мнимые JUnit tests. `InvalidClassException`, `NotSerializableException`, `InvalidObjectException`, `ParseException` и `MissingResourceException` из trace — именно **runtime** события.

## NOT RUN

Никаких испытаний внешних/недоверенных сериализованных байтов, gadget chains, production filter/DoS, two-build UID mismatch, иных JDK/vendors/OS/CLDR revisions, широких Unicode/Locale providers, всех inline snippets статей, P1/P2, benchmark/JMH/JIT/GC, реального UI Obsidian/IDE, Markdown anchors и внешних HTTP. Положительный CI доказывает только две трассы в своей фактической Java21 среде, а не безопасность public deserialization или одинаковые локализованные символы на всех JDK.
