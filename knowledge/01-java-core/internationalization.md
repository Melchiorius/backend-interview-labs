# CORE-I18N — локализация, форматы, языковые данные и кодировки в Java 21

**Задача.** В приложении одна величина — `12345.67` — может отображаться как `12,345.67` для англоязычного интерфейса и `12.345,67` для немецкого. При этом байты, отправляемые в API, не обязаны выглядеть как пользовательское представление. Изменение языка **не изменяет само число, момент времени или Unicode-текст**: оно выбирает правила отображения и разбора. Если перепутать эти уровни, данные можно незаметно обрезать при разборе, округлить не по предметному правилу или получить разные результаты на разных JDK.

Материал рассчитан на **Java SE 21**, без preview и без предположения, что система работает на одном языке, часовом поясе или версии CLDR. Нормативные источники: [Locale](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Locale.html), [NumberFormat](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/NumberFormat.html), [DecimalFormat](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/DecimalFormat.html), [DecimalFormatSymbols](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/DecimalFormatSymbols.html), [BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html), [RoundingMode](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/RoundingMode.html), [DateTimeFormatter](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html), [ResourceBundle](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ResourceBundle.html), [Charset](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/charset/Charset.html). Полные исторически проверенные эксперименты, точные нормативные ссылки и ограничения находятся в [принятом G1-плане](../../docs/plans/CORE-I18N-SERIAL.md) (§2, §3, §5–§7).

Карта темы: **L-01–L-27 = 27 P0**; **L28–L34 = 7 P1**; **L35–L38 = 4 P2**. Два соседних учебных блока: [числа и BigDecimal](numbers.md) и [Instant/ZoneId и DST](datetime.md). Там объясняется значение, здесь — *язык представления*. При написании использовалось опубликованное [исследование G1](../../docs/plans/CORE-I18N-SERIAL.md); теперь к статье интегрирован отдельный безопасный runnable G2. Исторический CI его технической ветки и проверка итогового авторского HEAD являются **разными** подтверждениями.

**Практика G2:** [полный I18nProbe.java](../../examples/core/i18n-serialization/src/I18nProbe.java), [UTF-8 эталон stdout (29 строк)](../../examples/core/i18n-serialization/i18n.expected.stdout) и [русская инструкция / verify.sh](../../examples/core/i18n-serialization/README.md). В том же комплекте находится [SerializationProbe.java](../../examples/core/i18n-serialization/src/SerializationProbe.java) с 20 строками: **29+20=49 наблюдений двух JVM, а не 49 JUnit-тестов**. Исполнимые тела и оба эталона совпадают с принятым G1 §6 после удаления одного `// Theory:` в каждом Java source. [Technical CI #45182432517](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45182432517) completed/SUCCESS строго на техническом SHA `648924427813af86c790ea4aaac32ba529b0f86d`; итоговый integrated CI фиксируется отдельно в [авторском PR #205](https://github.com/Melchiorius/backend-interview-labs/pull/205). Запуск проверяет одну чистую Java 21 компиляцию, две отдельные JVM и полные `diff -u` с SHA-256. I18N вывод использует `-Djava.locale.providers=CLDR` и implementation-specific `-Dsun.stdout.encoding=UTF-8`; чужие JDK/версии CLDR **NOT RUN**. 12 отдельных compiler-negative и 2 warning-only `@Serial` проверены ролью 03 отдельно, но **не входят** в позитивный CI.

## 1. Locale — метаданные о языковых предпочтениях, не кодировка (L-01, L-02, L-19, L-20)

`Locale` состоит из языкового тега и необязательных компонентов, таких как письменность, регион, варианты и Unicode-расширения. BCP 47 описывает структурированный язык тега: `sr-Latn-RS` означает сербский язык, латиницу и Сербию. Объект `Locale` **не содержит текст перевода**, не кодирует строку в байты и сам по себе не устанавливает часовой пояс.

~~~java
import java.util.Locale;

Locale sr = Locale.forLanguageTag("sr-Latn-RS");
System.out.println(sr.toLanguageTag()); // sr-Latn-RS
System.out.println(sr.getLanguage() + ":" + sr.getScript() + ":" + sr.getCountry());
// sr:Latn:RS
System.out.println(Locale.US.toLanguageTag() + ":" + Locale.GERMANY.toLanguageTag());
// en-US:de-DE
Locale digits = Locale.forLanguageTag("en-US-u-nu-latn");
System.out.println(digits.toLanguageTag()); // en-US-u-nu-latn
~~~

В теге `u-nu-latn` указывается Unicode-расширение с предпочтением системы записи цифр. **Поддержка и набор локальных символов зависят от поставщика данных**, поэтому сам тег не гарантирует, что любые фабрики на любом JDK нарисуют одинаковую строку. Явные `Locale.US` и `Locale.GERMANY` избавляют код от зависимости от выбранной языковой настройки пользователя, но **не фиксируют версию CLDR**.

Есть три контекста получения default: `Locale.getDefault()`, `Locale.getDefault(Locale.Category.FORMAT)` (форматы дат и чисел) и `Locale.getDefault(Locale.Category.DISPLAY)` (локализованные названия). Они могут различаться. Изменение глобальных defaults затрагивает другой код процесса; библиотечная функция должна принимать Locale явно, а не менять `Locale.setDefault` ради одного пользователя.

~~~java
// Иллюстрация зависимости, а не рекомендация менять default:
Locale userLocale = Locale.GERMANY;
String label = userLocale.getDisplayName(Locale.ENGLISH); // язык подписи выбирается отдельно
// Для форматирования явно передаём userLocale, а не Locale.getDefault().
~~~

**Контрпример:** сервер принимает `"12,345"` и без проверки выбирает формат по `Locale.getDefault()`. Один и тот же ввод может означать совсем разные числа. Для API-значения лучше использовать документированный машинный контракт, а локализованный ввод проверять по явному языку пользователя.

## 2. Форматирование чисел: те же данные, разные строки (L-03–L-07, L-11)

`NumberFormat.getNumberInstance(locale)` выбирает форматтер с правилами отображения для Locale. `DecimalFormat` позволяет явно установить шаблон и `DecimalFormatSymbols`; полученный фабричный объект **не обязан для всех Locale/провайдеров иметь конкретный класс DecimalFormat**, поэтому не следует вслепую приводить результат к нему. Форматтер **изменяемый**, в отличие от неизменяемого `BigDecimal`.

~~~java
import java.math.BigDecimal;
import java.text.*;
import java.util.*;

BigDecimal amount = new BigDecimal("12345.67");
NumberFormat us = NumberFormat.getNumberInstance(Locale.US);
NumberFormat de = NumberFormat.getNumberInstance(Locale.GERMANY);
System.out.println(us.format(amount)); // G1 OpenJDK21: 12,345.67
System.out.println(de.format(amount)); // G1 OpenJDK21: 12.345,67
// amount остаётся равным 12345.67.
DecimalFormatSymbols usSymbols = DecimalFormatSymbols.getInstance(Locale.US);
System.out.println((int) usSymbols.getGroupingSeparator()); // G1: 44 = U+002C
System.out.println((int) usSymbols.getDecimalSeparator());  // G1: 46 = U+002E
~~~

Это **результаты конкретной среды G1**, а не обещание одинаковых разделителей для всех JDK, языковых провайдеров и обновлений данных. Даже «пробел» как разделитель может оказаться не U+0020, а иным Unicode code point. Для диагностик полезно выводить символы как `U+XXXX`, а не судить о них только визуально.

~~~java
DecimalFormat fixed = new DecimalFormat(
        "0.00", DecimalFormatSymbols.getInstance(Locale.US));
fixed.setParseBigDecimal(true);
Number parsed = fixed.parse("67.25");
System.out.println(parsed.getClass().getSimpleName() + ":" + parsed);
// BigDecimal:67.25
~~~

`DecimalFormat.setParseBigDecimal(true)` выбирает результат типа `BigDecimal` для поддерживаемого числового ввода; метод принадлежит `DecimalFormat`, не интерфейсу `NumberFormat`. Фабричный форматтер без этой опции может вернуть другой подкласс `Number`. Для вычислений предпочтительнее хранить точное исходное значение, не делать round-trip через строку ради сложения или сравнения.

**Состояние форматтера:** `setMinimumFractionDigits(2)` и `setMaximumFractionDigits(3)` могут влиять друг на друга согласно контракту их границ. Настраивайте formatters для каждого запроса/контекста или используйте безопасную стратегию владения, а не общее изменяемое поле на множество потоков. `NumberFormat` не предоставляет безусловной гарантии потокобезопасности экземпляра.

## 3. Частичный разбор не равен валидации всего ввода (L-08, L-09, L-22, L-23)

`NumberFormat.parse(String)` может успешно распознать **начало** входа. Это не то же самое, что сказать: «вся строка корректна». Проверить полное потребление позволяет `ParsePosition`: её `getIndex()` показывает позицию окончания распознанного фрагмента.

~~~java
NumberFormat parser = NumberFormat.getNumberInstance(Locale.US);
String input = "12,345.67junk";
ParsePosition pos = new ParsePosition(0);
Number value = parser.parse(input, pos);
System.out.println(value + ":" + pos.getIndex() + ":" + input.length());
// G1: 12345.67:9:13
if (value == null || pos.getIndex() != input.length()) {
    throw new IllegalArgumentException("Некорректное числовое поле");
}
~~~

Вместо подтверждения валидности `"12,345.67junk"` мы получаем **явную ошибку**: после индекса 9 остался суффикс. В отдельной форме `parse("not-a-number")` выбрасывает проверяемое `ParseException` **во время выполнения**, а не при `javac`. Отрицательный компиляционный пример `L_N02_RoundingString.java` не связан с этим runtime-исключением.

Десятичная запятая в немецком интерфейсе **не означает**, что `Double.parseDouble("12.345,67")` становится локализованным парсером: у `Double.parseDouble` своя грамматика, не берущая Locale из системы. В G1 [I18nProbe §6.1](../../docs/plans/CORE-I18N-SERIAL.md) `NumberFormat` для `Locale.GERMANY` разобрал документированную немецкую форму в `12345.67`.

**Важная асимметрия:** `format(parse(text))` не обязательно восстанавливает исходный текст, а `parse(format(x))` — исходную точность x. Округление, удаление группирующих символов, локальная форма и разрешённое неполное потребление разрушают обратимость. Если формат используется в транзакционном API, отдельно задайте договорённость о точности и грамматике.

## 4. Округление — отдельная бизнес-политика (L-10, L-11, L-21, L-23)

Режим округления влияет на отображение; ограничение количества цифр после запятой не изменяет исходный `BigDecimal`. На половине между двумя целыми различаются `HALF_EVEN` (к ближайшему чётному) и `HALF_UP` (половина от нуля).

~~~java
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

DecimalFormat fmt = new DecimalFormat(
        "0", DecimalFormatSymbols.getInstance(Locale.US));
fmt.setRoundingMode(RoundingMode.HALF_EVEN);
System.out.println(fmt.format(2.5)); // 2
fmt.setRoundingMode(RoundingMode.HALF_UP);
System.out.println(fmt.format(2.5)); // 3
NumberFormat integers = NumberFormat.getIntegerInstance(Locale.US);
System.out.println(integers.format(67.8)); // G1: 68, не (int)67.8 == 67
~~~

`getIntegerInstance` в данном примере **форматирует** число для показа, а приведение `(int)` отбрасывает дробную часть по правилам Java. Это разные операции. Не выводите обязательную банковскую, налоговую или биржевую политику из default roundingMode JDK. [CORE-NUMBERS](numbers.md) подробно объясняет `BigDecimal.scale`, `RoundingMode` и точную арифметику.

## 5. Дата/время: Locale выбирает текст, ZoneId выбирает правила смещения (L-12–L-14, L-26)

`DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)` строит форматтер, используемый для вывода даты в выбранной локали. `withLocale` меняет язык/формат, **не изменяя LocalDate**. Это не перенос времени в другой часовой пояс.

~~~java
import java.time.*;
import java.time.format.*;
import java.util.*;

LocalDate day = LocalDate.of(2024, 3, 30);
DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);
System.out.println(fmt.withLocale(Locale.US).format(day));      // G1: March 30, 2024
System.out.println(fmt.withLocale(Locale.GERMANY).format(day)); // G1: 30. März 2024
Instant instant = Instant.parse("2024-03-30T12:34:56Z");
System.out.println(instant); // 2024-03-30T12:34:56Z
System.out.println(instant.atZone(ZoneId.of("Europe/Berlin")).toInstant().equals(instant));
// true: язык/зона не изменили момент на временной шкале
~~~

`Instant` — точка на UTC-временной шкале, `ZoneId` и `ZoneRules` определяют местные поля даты/времени с учётом DST, а Locale влияет на **локализованное представление**. При попытке форматировать Instant шаблоном локальной даты/времени может потребоваться явно задать зону через `withZone`: Locale **не подставляет её автоматически**. Полный разбор gap/overlap — в [CORE-DATETIME](datetime.md). Точные строки вида «30. März 2024» зависят от данных провайдера и версии JDK.

## 6. Locale-sensitive case conversion — не глобальная lowerCase (L-15, L-26)

Буква `I` ведёт себя не одинаково в англоязычном и турецком контексте: турецкая строчная форма может быть **dotless i** (`ı`, U+0131). Поэтому `String.toLowerCase()` без аргумента, зависящий от default Locale, опасен для технических идентификаторов, ключей и протоколов.

~~~java
import java.util.Locale;

System.out.println("TITLE".toLowerCase(Locale.ROOT));             // title
System.out.println("TITLE".toLowerCase(Locale.forLanguageTag("tr-TR")));
// tıtle — вторая буква U+0131
~~~

`Locale.ROOT` устраняет локаль-пользовательскую вариативность для простой технической операции над строкой, но **не является универсальным Unicode case folding или защитой от spoofing**. Для идентификаторов, предназначенных человеку, Unicode normalization, confusables и грамматику допустимых знаков надо определять отдельно.

## 7. ResourceBundle — выбор сообщения, отсутствующий ключ и fallback (L-17, L-18, L-25)

`ResourceBundle` связывает **ключ** с текстом перевода для Locale. Обычно поиск начинается с наиболее подходящей локали и может использовать кандидатов с меньшей спецификой и fallback; правила зависят от имени пакета, `Control`, default Locale и доступных ресурсов. **Нельзя обещать**, что `getBundle("x", locale)` всегда найдёт именно ресурс с таким суффиксом или что «нет языка» равнозначно `null`.

~~~java
import java.util.*;

public class MessagesDemo {
    public static class Messages_en extends ListResourceBundle {
        @Override protected Object[][] getContents() {
            return new Object[][] {{"welcome", "Welcome"}};
        }
    }
    public static class Messages_de extends ListResourceBundle {
        @Override protected Object[][] getContents() {
            return new Object[][] {{"welcome", "Willkommen"}};
        }
    }
    public static void main(String[] args) {
        System.out.println(ResourceBundle.getBundle(
                "MessagesDemo$Messages", Locale.ENGLISH).getString("welcome"));
        // Welcome
        System.out.println(ResourceBundle.getBundle(
                "MessagesDemo$Messages", Locale.GERMAN).getString("welcome"));
        // Willkommen
    }
}
~~~

При отсутствующем ключе `getString("missing")` бросит `MissingResourceException` **во время выполнения**. Наличие одного немецкого ресурса не доказывает конкретную цепочку fallback для любого default — полноценная матрица fallback в G1 не запускалась. `ListResourceBundle` показан ради проверяемого примера без внешних property files; в реальном проекте обычно используются именованные ресурсы. Для fallback/кодировки свойств существует отдельная P1-тема (§10).

## 8. Текст ≠ байты: Charset и Locale независимы (L-16, L-27)

Unicode задаёт абстрактные кодовые точки, `String` представляет текст внутри Java, а `Charset` задаёт отображение **текста в последовательность байтов**. Выбор языка форматирования не превращает Unicode в UTF-8.

~~~java
import java.nio.charset.StandardCharsets;

String value = "é";                         // U+00E9
System.out.println(value.getBytes(StandardCharsets.UTF_8).length);    // 2
System.out.println(value.getBytes(StandardCharsets.UTF_16BE).length); // 2
~~~

Одинаковое число байтов здесь **не означает одинаковые байты**. `UTF_8` и `UTF_16BE` не получают свой алгоритм из `Locale.GERMANY` или `Locale.US`. Для файлов и сетевого протокола указывайте кодировку явно, а для пользовательского отображения — нужную Locale. Не путать вывод `System.out` с кодировкой `String`: кодировка консоли/pipe тоже определяется окружением.

Граница наблюдений G1 особенно показательна: для `I18nProbe` на Debian13/OpenJDK21 пришлось запускать `-Dsun.stdout.encoding=UTF-8` вместе с `-Djava.locale.providers=CLDR`. Первое свойство **специфично реализации OpenJDK**, не нормативно для всех JVM. Отсутствие свойства в этой среде заменяло нелатинские знаки знаком `?`. Выводы о byte-for-byte совпадении fixture относятся к закреплённой среде/настройкам, не ко всем компьютерам.

## 9. Выбирайте явно: данные, представление или протокол (L-20, L-23, L-24, L-26, L-27)

При проектировании API полезно последовательно задать вопросы: *Что хранится?* (например, `BigDecimal`/`Instant`); *кто выбирает Locale?* (пользователь, запрос, ресурс); *какая zone?* (для местной даты/времени); *какая grammar и precision?* (для ввода и вывода); *какая charset?* (для bytes).

В обработчике запроса получение Locale из явной настройки клиента предпочтительнее скрытого глобального default. `NumberFormat` и `DecimalFormat` изменяемы, поэтому один общий экземпляр для параллельных запросов требует отдельной дисциплины синхронизации/владения; **данная статья не проводила concurrency stress**. Для технических токенов важно не сериализовать локализованное представление вместо документированного числа.

**Контрпример:** сервис отдаёт строку `"12.345,67"` и клиент считает, что это синтаксис JSON number. Она не является JSON number, а у разных пользователей она будет различаться. Для машинного контракта передавайте значение по его схеме (тип, точность, временная зона, charset); Locale применяйте на границе UI.

## 10. P1 — полезные уточнения, не выданные за выполненные эксперименты (L28–L34)

**L28. PropertyResourceBundle, UTF-8 и JEP 226.** Начиная с Java 9 чтение property resource bundles из стандартных способов загрузки использует UTF-8 с оговорённой совместимостью/поведением при ошибках декодирования. [JEP 226](https://openjdk.org/jeps/226) — источник механизма; отдельные .properties и тест fallback тут **NOT RUN**.

**L29. Collator и сортировка текста.** `Collator` определяет культурно-зависимое сравнение, отличное от `String.compareTo` (по UTF-16). Встроенный comparator или `String.CASE_INSENSITIVE_ORDER` не становится «правильным порядком слов для всех стран»; полный разбор деревьев/контрактов — [CORE-UTIL](utilities.md). Эксперимент с локальными правилами не проводился.

**L30. Проверка тегов.** `Locale.forLanguageTag` и `Locale.Builder` могут по-разному обрабатывать некорректные теги: lenient parsing и strict validation требуют разной политики API. Не принимайте произвольный language-tag как доказательство поддержки локали; негативный builder trace NOT RUN.

**L31. Formatter.withZone/Chronology.** Текст месяца и календарная система — не обязательно один и тот же выбор. `DateTimeFormatter.withZone` и `withChronology` управляют другими аспектами представления, чем `withLocale`. Экзотические календари NOT RUN.

**L32. MessageFormat.** Шаблон сообщения содержит собственный синтаксис, апострофы экранируются по правилам `MessageFormat`; наивная конкатенация опасна для полноты переводов и грамматического числа. Это **не** полный движок plural rules для всех языков; MessageFormat tests NOT RUN.

**L33. Деньги, проценты и compact numbers.** `NumberFormat.getCurrencyInstance`, `getPercentInstance` и `getCompactNumberInstance` имеют собственную семантику и правила из поставщика данных; ни один вызов не задаёт банковскую политику округления. Без раздельных инвариантов округлять деньги лишь для UI нельзя.

**L34. CLDR/provider drift.** Опция `java.locale.providers` выбирает порядок провайдеров в поддерживаемых реализациях; другой JDK/vendor/CLDR/OS способен выдать другое название месяца или символ. Здесь нет межпровайдерной/межверсионной сравнительной матрицы; нельзя заявлять её как завершённый эксперимент.

## 11. Матрица принятых вопросов: 27 P0 / 7 P1 / 4 P2

В каждой строке дана **точка объяснения**, а не «номер теста». Маркеры `L01–L29` относятся к stdout в [полном принятом G1 §6.1](../../docs/plans/CORE-I18N-SERIAL.md). Две программы будущего G2 после интеграции должны иметь отдельный подтверждённый SHA и CI.

| ID | Механизм, пример и граница |
|---|---|
| L-01 | §1 BCP47, script/region, `sr-Latn-RS` |
| L-02 | §1 FORMAT/DISPLAY/default и explicit Locale; изменение global default NOT RUN |
| L-03 | §2 тот же BigDecimal: US/DE вывод L04/L05 |
| L-04 | §2 code points разделителей U+002C/U+002E |
| L-05 | §2 format value ≠ изменение BigDecimal |
| L-06 | §2 DecimalFormat pattern+Symbols vs factory |
| L-07 | §2 parseBigDecimal и тип Number |
| L-08 | §3 ParsePosition частичный разбор 9/13 |
| L-09 | §3 ParseException runtime |
| L-10 | §4 HALF_EVEN 2 vs HALF_UP 3 |
| L-11 | §2, §9 mutable min/max fractional digits |
| L-12 | §5 localized DateTimeFormatter US/DE |
| L-13 | §5 LocalDate/Instant не меняются от языка |
| L-14 | §5 Locale != ZoneId/Chronology/tzdb |
| L-15 | §6 Turkish I/dotless i |
| L-16 | §8 UTF-8/UTF-16BE vs Locale |
| L-17 | §7 ResourceBundle ключи и ListResourceBundle |
| L-18 | §7 MissingResourceException runtime |
| L-19 | §1 BCP47 u-nu-latn |
| L-20 | §1, §5, §8, §9 provider/CLDR/version drift |
| L-21 | §4 integer number formatting 67.8 → 68 |
| L-22 | §3 German NumberFormat parse ≠ Double.parseDouble |
| L-23 | §3–§4 format/parse необратимость |
| L-24 | §2, §9 NumberFormat mutability / concurrent NOT RUN |
| L-25 | §7 ResourceBundle candidates и fallback NOT RUN |
| L-26 | §5, §6, §9 UI vs wire; Locale.ROOT |
| L-27 | §8 Locale-data != charset/console byte-encoding |
| L28 | §10 P1 JEP 226 PropertyResourceBundle UTF-8; NOT RUN |
| L29 | §10 P1 Collator vs UTF-16 comparator; NOT RUN |
| L30 | §10 P1 Locale.Builder validation; NOT RUN |
| L31 | §10 P1 formatter zone/chronology; NOT RUN |
| L32 | §10 P1 MessageFormat quoting/plural; NOT RUN |
| L33 | §10 P1 currency/percent/compact formatter; NOT RUN |
| L34 | §10 P1 CLDR/HOST provider drift; NOT RUN |
| L35 | P2 cross-JDK/OS/CLDR matrix **NOT VERIFIED** |
| L36 | P2 Unicode normalization/grapheme/IDNA/spoofing **NOT VERIFIED** |
| L37 | P2 ICU plural rules **NOT VERIFIED** |
| L38 | P2 глобальный setDefault/concurrency **NOT VERIFIED** |

## 12. Источники A/C и честные границы

**A (Java SE 21):** [Locale](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Locale.html), [NumberFormat](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/NumberFormat.html), [DecimalFormat](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/DecimalFormat.html), [DateTimeFormatter](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html), [ResourceBundle](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ResourceBundle.html), [StandardCharsets](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/charset/StandardCharsets.html), [JEP 226](https://openjdk.org/jeps/226). Показанные контракты API отделены от региональных строк *конкретного* OpenJDK.

**C — реально подтверждённые широкие интервью-вопросы:** [Q-TEAMLEAD](https://github.com/teamlead/java-interview-questions/blob/main/README.md) о локализации; [Q-CHERNYSHOV-RU](https://github.com/Shell26/Java-Developer/blob/master/core.md) об интернационализации/локализации. Источники не подтверждают, что на собеседованиях обязательно спрашивают `ParsePosition`, CLDR drift или `ResourceBundle`-кодировку; для этих точных вопросов отмечаем **C GAP**, но объясняем их по нормативному API.

**Исторический G1 evidence роли 01/04, НЕ авторский G2 runner:** Debian GNU/Linux13, OpenJDK 21.0.12.1, CLDR provider, фиксированная UTF-8 консоль; полный `I18nProbe.java` даёт **29 stdout-строк** [G1 §6.1](../../docs/plans/CORE-I18N-SERIAL.md), stdout SHA-256 `620ac75eb8c4baf0366e17ebd89384fd51b1b505007bceabf9b4c7cf9213b332`; source SHA-256 `f44886184d994bcc86842e51d8204619fc152ecb63aa6da435b9053d3706300d`. По G1 отдельно выполнялись **6 истинных javac-negative** (L_N01–L_N06); это не runtime ParseException и не часть stdout. Статус *локально проверено в G1* не означает, что новые G2 CI/fixtures уже опубликованы.

**NOT RUN в этой статье:** все embedded Java fragments как отдельные compilation units; 12 isolated compiler-negative и 2 `@Serial` warning-only в обычном позитивном CI; конкаррентное изменение defaults и NumberFormat; property bundle fallback/encoding, Collator, MessageFormat, все P2; другие JDK/vendors/CLDR/OS и точная версия CLDR; реальное банковское округление и production wire protocols; GUI Obsidian/IDE, внешние HTTP/anchors. Не расширять статус [CURRICULUM](../../docs/CURRICULUM.md) или [COVERAGE](../../docs/COVERAGE.md) до решения G5/G6.
