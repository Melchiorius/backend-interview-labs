# CORE-I18N + CORE-SERIAL — G1-план Java SE 21

**G1 PROPOSED / NOT ACCEPTED.** Роль 01 — Research & Coverage. Основание: [Issue #200](https://github.com/Melchiorius/backend-interview-labs/issues/200), [адресное 00→01 #7101031603](https://github.com/Melchiorius/backend-interview-labs/issues/200#issuecomment-7101031603), [Java Core #33](https://github.com/Melchiorius/backend-interview-labs/issues/33). **Исходный BASE поручения:** `a60add1898112f444eb1ebac6d1386a05ca7abca` после G5 [PR #197](https://github.com/Melchiorius/backend-interview-labs/pull/197). **Фактический BASE этой ветки:** `1ff21114742c13d5a4cf9817f388b1ea646e2a17` после статусного G6 [PR #201](https://github.com/Melchiorius/backend-interview-labs/pull/201). Exact HEAD/CI фиксируются в PR. Роль 01 не делает G2, G6, merge.

## 1. Цель и границы
Последние **2 незавершённых из 42 Java Core curricular ID** — CORE-I18N и CORE-SERIAL (40 ID реализованы через 39 статей по G5 #197). Исследование предлагает **две самостоятельные G2 статьи**, будущие `knowledge/01-java-core/internationalization.md` и `knowledge/01-java-core/serialization.md`; файлов ещё **нет**, поэтому Markdown-ссылок на них нет.

**I18N:** Locale language/script/region/BCP47, explicit vs default, NumberFormat/DecimalFormat/parse/rounding, DateTimeFormatter localized vs Instant/ZoneId, ResourceBundle messages, Unicode vs Charset, CLDR/provider/version drift. Не подменять CORE-NUMBERS, CORE-DATETIME, CORE-I/O или CORE-REGEX.

**SERIAL:** Serializable/UID/transient/static/native graph, OOS/OIS trusted local bytes only, private readObject validation, record/enum/compatibility, ObjectInputFilter boundaries и безопасная архитектура. Не создавать gadget payload, unsafe public deserializer, endpoints, внешние/недоверенные потоки. Native stream ≠ JSON/Avro/Protobuf.

**Рекомендуем два независимых Java21 runner** только при решении 00 о G2: `I18nProbe` и `SerializationProbe`, полный source и проверенный stdout в `6`. Отдельные отрицательные Java-компиляции — в `7`. До G1 acceptance не менять `knowledge/`, `examples/`, Gradle/CI, root README, `COVERAGE/CURRICULUM`.

### Уже опубликованные мосты (не засчитывать два ID)
| Статья | Мост к G1, не дублировать всю тему |
|---|---|
| [CORE-NUMBERS](../../knowledge/01-java-core/numbers.md) | BigDecimal и numeric rounding ≠ localized display |
| [CORE-DATETIME](../../knowledge/01-java-core/datetime.md) | Instant/ZoneId/tzdb ≠ текстовая locale |
| [CORE-STRINGS](../../knowledge/01-java-core/strings.md) | Unicode code points/UTF-16 ≠ locale и кодировка |
| [CORE-UTIL](../../knowledge/01-java-core/utilities.md) | Comparator; locale-aware Collator отдельно |
| [CORE-REGEX](../../knowledge/01-java-core/regex.md) | regex Unicode categories ≠ locale-aware display |
| [CORE-RECORDS](../../knowledge/01-java-core/records.md) | Canonical record constructor vs special deserialization |
| [CORE-IMMUTABLE](../../knowledge/01-java-core/immutability.md) | defensive copies ≠ безопасная native десериализация |
| [CORE-REFLECTION](../../knowledge/01-java-core/reflection.md) | Class/reflection ≠ валидация object input |
| [CORE-EXCEPTIONS](../../knowledge/01-java-core/exceptions.md) | checked/runtime distinctions |
| [CORE-RESOURCES](../../knowledge/01-java-core/resources.md) | try-with-resources with byte streams |

Прочитаны [AGENTS](../../AGENTS.md), [WORKFLOW](../WORKFLOW.md), [ROLE_MODEL](../ROLE_MODEL.md), [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md), [CURRICULUM](../CURRICULUM.md), [COVERAGE](../COVERAGE.md), [EXERCISE_AND_TESTING_STANDARD](../EXERCISE_AND_TESTING_STANDARD.md).

## 2. Нормативные A и фактические интервью C

**A (обязательно Java 21):** Javadoc и Java Object Serialization Specification; внешний банк вопросов C **не заменяет норму**.

| ID | Java SE 21 нормативный locator |
|---|---|
| L1 | [Locale](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Locale.html): BCP47/FORMAT/DISPLAY |
| L2 | [NumberFormat](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/NumberFormat.html): format/parse/ParsePosition/thread safety |
| L3 | [DecimalFormat](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/DecimalFormat.html), [DecimalFormatSymbols](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/DecimalFormatSymbols.html) |
| L4 | [RoundingMode](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/RoundingMode.html), [BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html) |
| L5 | [DateTimeFormatter](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html), [Instant](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Instant.html) |
| L6 | [String.toLowerCase(Locale)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html) |
| L7 | [ResourceBundle](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ResourceBundle.html), [PropertyResourceBundle](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PropertyResourceBundle.html) |
| L8 | [Charset](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/charset/Charset.html), [StandardCharsets](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/charset/StandardCharsets.html) |
| L9 | [JDK21 supported locales](https://www.oracle.com/java/technologies/javase/jdk21-supported-locales.html), [LocaleServiceProvider](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/spi/LocaleServiceProvider.html) |
| S1 | [Serializable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Serializable.html), [@Serial](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Serial.html) |
| S2 | [ObjectOutputStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ObjectOutputStream.html), [ObjectInputStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ObjectInputStream.html) |
| S3 | [Serialization spec — class descriptors](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/class.html), [versioning](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/version.html) |
| S4 | [Serialization architecture](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/serial-arch.html), [input](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/input.html) and [protocol](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/protocol.html) |
| S5 | [ObjectInputFilter](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ObjectInputFilter.html), [Config](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ObjectInputFilter.Config.html) |
| S6 | [Oracle secure coding guidelines](https://www.oracle.com/java/technologies/javase/seccodeguide.html) — untrusted input security |
| S7 | [Externalizable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Externalizable.html) |

**C — реально прочитанные интервью-вопросы:**
- **I1 / Q-TEAMLEAD** [README](https://github.com/teamlead/java-interview-questions/blob/main/README.md), прямой вопрос “What is localization” (около строки 1323); подтверждает область, НЕ отдельный DecimalFormat/CLDR/Locale API follow-up.
- **I2 / Q-CHERNYSHOV-RU** [core.md](https://github.com/Shell26/Java-Developer/blob/master/core.md), «Что такое интернационализация, локализация?» (около строки 1690). Никакой исходной точной формулировки о ParsePosition/ResourceBundle codec не установлено.
- **S1 / Q-HRUDAY** [README](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md), **Chapter 19: Serialization & Deserialization (66 вопросов)**: Serializable, serialVersionUID, transient/static, custom hooks, Externalizable, graphs/cycles, security и explicit DTO formats.
- **S2 / Q-TEAMLEAD** [README](https://github.com/teamlead/java-interview-questions/blob/main/README.md), буквальные вопросы о serialization, transient и секция о custom serialization/fields/serialVersionUID/validation (около строк 1449–1457).
- **S3 / Q-CHERNYSHOV** [English README](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/eng/README.md), №245 «What is Serializable?».
- **C GAP:** точных вопросов об ObjectInputFilter UNDECIDED, record canonical deserialization, CLDR drift, localized partial parse и Record UID waiver не найдено. Они взяты как **нормативно необходимые follow-ups**, не выдуманные интервью-цитаты.

## 3. CORE-I18N — отдельная P0-матрица

**L01–L29** — коды реального stdout I18nProbe; когда эксперимент отсутствует, это сказано прямо. C=GAP означает отсутствие подтверждённого узкого вопроса, не отсутствие Java21 нормы.

| ID | Механизм | A | C | Trace/контрпример или NOT RUN |
|---|---|---|---|---|
| `L-01` | BCP47 Locale: language/script/region vs charset | L1 | I1 | L01–L03 |
| `L-02` | Locale имеет default и FORMAT/DISPLAY categories; explicit Locale не зависит от host default | L1 | I1 | Положительные L04/05 используют explicit; default НЕ проверялся |
| `L-03` | NumberFormat.getNumberInstance(locale) отображает одну величину по-разному | L2 | GAP | L04=12,345.67; L05=12.345,67 |
| `L-04` | Decimal/group separators — конкретные Unicode code points, а не универсальная точка/запятая | L3 | GAP | L06=U+002C; L07=U+002E |
| `L-05` | NumberFormat форматирует представление, не меняет BigDecimal/исходное числовое значение | L2,L4 | GAP | L04/L05 |
| `L-06` | DecimalFormat явный pattern+DecimalFormatSymbols vs provider factory | L3 | GAP | L08/L11/L12 |
| `L-07` | setParseBigDecimal(true) возвращает BigDecimal; factory NumberFormat не универсально DecimalFormat | L3 | GAP | L08=BigDecimal:67.25 |
| `L-08` | ParsePosition показывает частичное потребление строки; нельзя молча принимать trailing junk | L2 | GAP | L09=12345.67:9:13 |
| `L-09` | Некорректный NumberFormat.parse → runtime ParseException, не compiler error | L2 | GAP | L10 |
| `L-10` | HALF_EVEN vs HALF_UP tie: 2.5 → 2/3; финансовая политика НЕ следует из JDK default | L3,L4 | GAP | L11/L12 |
| `L-11` | Fraction digits и rounding mutability требуют настройки отдельных NumberFormat instances | L2 | GAP | L13=3 после min=2 |
| `L-12` | DateTimeFormatter.ofLocalizedDate(style).withLocale — локализованное отображение даты | L5 | I1 | L14 vs L15 |
| `L-13` | Изменение locale не изменяет LocalDate/Instant/UTC момент | L5 | GAP | L16–L18 |
| `L-14` | Locale != ZoneId/Chronology/tzdb; разные часы — не часть перевода текста | L5 | GAP | L17; CORE-DATETIME bridge |
| `L-15` | Locale.ROOT vs Turkish-specific lowercase и Unicode dotless i | L1,L6 | GAP | L19–L21 |
| `L-16` | Charset.UTF_8/UTF_16BE переводят Unicode текст в байты независимо от Locale | L8 | I2 (шир.) | L22/L23 |
| `L-17` | ResourceBundle keyed locale candidates: перевод статических сообщений | L7 | I1 | L24/L25 |
| `L-18` | MissingResourceException для отсутствующего ключа — runtime | L7 | GAP | L26 |
| `L-19` | Locale language tag extensions u-nu влияют на locale properties/providers | L1 | GAP | L27 |
| `L-20` | CLDR/provider version drift делает конкретный localized output environment-dependent | L1,L9 | GAP | L04/L05/L14/L15 pinned Debian13 OpenJDK21, CLDR provider |
| `L-21` | getIntegerInstance для отображения 67.8 даёт 68, не int cast | L2 | GAP | L28 |
| `L-22` | locale-aware parse DE строки и plain Double.parseDouble — разные методы | L2 | GAP | L29=12345.67 |
| `L-23` | format ≠ guaranteed inverse of parse (rounding/truncation/partial) | L2,L3 | GAP | L09/L11/L12/L28 |
| `L-24` | NumberFormat/DecimalFormat mutable, concurrent use НЕ гарантированно safe | L2,L3 | GAP | Норма API; threaded race NOT RUN |
| `L-25` | ResourceBundle fallback and candidate selection: careful with default and missing resources | L7 | I1 (общ.) | L24–L26; другой default/fallback NOT RUN |
| `L-26` | UI localization vs machine wire protocol, Locale.ROOT for invariant case operations | L1,L2,L5 | I1 | L16/L19–L21 |
| `L-27` | Locale data selection and Unicode encoding are independent sources of nondeterminism | L8,L9 | I2 | Native stdout ASCII needed UTF-8 override, separately observed |

## 4. CORE-SERIAL — отдельная P0-матрица

**S01–S20** — строки реально запущенной SerializationProbe. Изолированный безопасный сценарий использует только локально созданные доверенные objects/bytes. Нормативные случаи без собственного trace не названы «проверенными».

| ID | Механизм и security boundary | A | C | Trace или NOT RUN |
|---|---|---|---|---|
| `S-01` | Serializable marker and default graph serialization | S1,S2 | S1,S2 | S01/S18 |
| `S-02` | ObjectOutputStream/ObjectInputStream on trusted local ByteArrayOutputStream/ByteArrayInputStream | S2 | S1 | S01/S03 |
| `S-03` | Explicit serialVersionUID long 67L, reflected by ObjectStreamClass | S1,S3 | S1,S2 | S02=67 |
| `S-04` | ordinary class serialVersionUID mismatch → InvalidClassException runtime | S3 | S1 | Normative; two-version class test NOT RUN |
| `S-05` | transient instance field excluded/restored as default null | S1,S2 | S1 | S04=true |
| `S-06` | static field not serialized; class value after write remains current | S1 | S1,S2 | S05=after; S20=local-session |
| `S-07` | Reachable object graph and member fields persisted | S4 | S1 | S06=child |
| `S-08` | Cyclic references restored without infinite recursion | S4 | S1 | S07=true |
| `S-09` | Stream handle sharing: same object written twice in same OOS restores same reference | S4 | S1 | S08=true |
| `S-10` | Reconstituted Java identity distinct from original object in memory | S2 | S1 (шир.) | S09=true |
| `S-11` | ObjectInputFilter resource limits and UNDECIDED/REJECTED are separate statuses | S5 | GAP | S03–S09 local-limits UNDECIDED |
| `S-12` | Rejecting even trusted locally produced Node triggers InvalidClassException runtime | S5 | GAP | S10=InvalidClassException:filter |
| `S-13` | NotSerializableException for unsupported graph/object is runtime, NOT javac error | S1,S2 | S1 | S11 |
| `S-14` | Private readObject hook+defaultReadObject, explicit object invariant validation | S1,S2 | S1,S2 | S12=InvalidObjectException:validation |
| `S-15` | Ordinary Serializable constructors not generally used on deserialization; nonserializable parent noarg contract | S1,S4 | S1 | Normative; constructor trace NOT RUN |
| `S-16` | Serializable record canonical constructor invoked during reading | S4 | GAP | S13=1 → S14=2 |
| `S-17` | Record components restored and new object identity | S4 | GAP | S15/S16 |
| `S-18` | Enum constant restored by name, preserved identity | S4 | S1 (шир.) | S17=true |
| `S-19` | Native serialization stream ≠ JSON/Avro/Protobuf stable cross-language DTO | S4 | S1,S2 | S01 local bytes; external format NOT RUN |
| `S-20` | Untrusted deserialization has code/data attack surface; never expose an untrusted OIS in probe | S5,S6 | S1 | Threat model, no gadgets or remote input |
| `S-21` | ObjectInputFilter not full security boundary; UNDECIDED not implicit reject | S5,S6 | GAP | S10; only local filter rejection |
| `S-22` | Nonserializable field reachable in graph can cause NotSerializableException | S1,S2 | S1 | Normative, separate nested-field test NOT RUN |
| `S-23` | Record serialVersionUID matching has special spec waiver; do not infer ordinary compatibility rules | S3,S4 | GAP | S19=0; UID mismatch NOT RUN |
| `S-24` | ObjectStreamClass lookup ordinary vs record UID | S3 | S1 | S02=67 vs S19=0 |
| `S-25` | private readObject/writeObject signature and @Serial compilation warnings vs compiler errors | S1,S7 | S1 | S12; invalid @Serial forms compile WARNINGS, not negative |
| `S-26` | Record-specific serialization ignores class-specific readObject/writeObject and invokes canonical | S4 | GAP | S13–S16, hook-ignore case NOT RUN |
| `S-27` | readObject returns Object; checking/casting and invariant handling mandatory | S2,S6 | S1 | S_N04 is actual compile error if assigned directly to String |

**Итого 54 P0: 27 I18N + 27 SERIAL.** Это требования к будущим двум статьям, не 54 JUnit-теста или 54 дословных интервью-вопроса.

## 5. P1/P2 — future research, не готовые статьи

| ID | Раздел | P | Отдельная возможность углубления / NOT RUN |
|---|---|---|---|
| `L28` | I18N | P1 | PropertyResourceBundle UTF-8 (JEP 226) and compatibility fallback rules, no property files run |
| `L29` | I18N | P1 | Collator vs String.compareTo, language-aware sorting; not run |
| `L30` | I18N | P1 | Locale.Builder malformed tags vs lenient forLanguageTag; not run |
| `L31` | I18N | P1 | DateTimeFormatter withZone/Chronology, localized ZonedDateTime; not run |
| `L32` | I18N | P1 | MessageFormat pluralization/quoting and security; not run |
| `L33` | I18N | P1 | Currency/Percent/CompactNumberFormat provider defaults, no money policy presumed |
| `L34` | I18N | P1 | CLDR/HOST/provider and vendor JDK drift cross-check, not run |
| `S28` | SERIAL | P1 | Externalizable public noarg, writeExternal/readExternal; test only compile-negative missing methods |
| `S29` | SERIAL | P1 | Serialization proxy writeReplace/readResolve; not run |
| `S30` | SERIAL | P1 | ObjectInputValidation callback, JEP415 filter factory; not run |
| `S31` | SERIAL | P1 | @Serial incorrect method/UID declarations compiled with warnings, not compiler-negative |
| `S32` | SERIAL | P1 | serialPersistentFields and versioning migrations, not run |
| `S33` | SERIAL | P1 | writeUnshared/readUnshared/reset handle rules, not run |
| `S34` | SERIAL | P1 | Nonserializable superclass constructor and hierarchy variant runnable, not run |
| `L35` | I18N | P2 | Full CLDR and JDK vendor drift across OS/version, not run |
| `L36` | I18N | P2 | Unicode normalization/graphemes/IDNA and spoofing, not run |
| `L37` | I18N | P2 | ICU message plural rules, not run |
| `L38` | I18N | P2 | Global Locale.setDefault mutation and concurrency, not run |
| `S35` | SERIAL | P2 | Real hostile gadget chains deliberately NOT constructed |
| `S36` | SERIAL | P2 | Public deserialization endpoints/untrusted files deliberately NOT constructed |
| `S37` | SERIAL | P2 | Cross-version build & classloader compatibility matrix, not run |
| `S38` | SERIAL | P2 | JSON/Avro/Protobuf or Jackson wire schema integration, not run |
| `S39` | SERIAL | P2 | Huge object graph stress/JMH/resource exhaustion, not run |

**Итого 54 P0 / 14 P1 / 9 P2.** Не засчитывать P1/P2 в implemented CORE-I18N или CORE-SERIAL.

## 6. Две локально проверенные Java21 программы — источник и stdout

**Фактическое окружение:** OpenJDK `java/javac 21.0.12.1+1-1-deb13u1`, Debian GNU/Linux 13; `javac --release 21 -Xlint:all` обеих программ **exit 0, stderr 0 bytes**. Отдельные запуски Java дали **29 + 20 = 49** stdout строк. Повторные `diff -u` **2/2 exit 0**. Время, система Locale и random не использованы.

Для I18N запуск: `java -Djava.locale.providers=CLDR -Dsun.stdout.encoding=UTF-8 I18nProbe`. Последнее свойство специфично реализации JDK, **не универсальная Java SE гарантия**: в здешнем окружении native stdout ASCII без него заменял нелатинские символы на `?`, что отдельно реально наблюдалось. Сами locale data зависят от JDK/vendor/CLDR version, их точный CLDR revision не измерялся. Нельзя копировать представление DE/US как инвариант всех JDK.

Для SERIAL запуск: `java -Dsun.stdout.encoding=UTF-8 SerializationProbe`. Весь ObjectInputStream читает **только байты, записанные в этой же программе** через ObjectOutputStream в ByteArrayOutputStream. Доверенный тест фильтра демонстрирует REJECTED, а не «защищённый десериализатор для внешнего ввода».

~~~sh
javac --release 21 -Xlint:all -d . I18nProbe.java SerializationProbe.java
java -Djava.locale.providers=CLDR -Dsun.stdout.encoding=UTF-8 I18nProbe > i18n.stdout
java -Dsun.stdout.encoding=UTF-8 SerializationProbe > serial.stdout
diff -u i18n.expected.stdout i18n.stdout
diff -u serial.expected.stdout serial.stdout
sha256sum I18nProbe.java i18n.stdout SerializationProbe.java serial.stdout
~~~

### 6.1 I18nProbe.java — полный проверенный источник

~~~java
