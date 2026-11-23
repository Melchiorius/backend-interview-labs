# CORE-DATETIME — Instant, локальное время, часовые пояса и DST в Java 21

**Практическая проблема.** Пользователь указал «31 марта 2024 года, 02:30, Берлин». Почему стандартное преобразование даёт 03:30? Почему «завтра в 12:00» может наступить через **23 часа**, а не через 24? Почему два расписания с одинаковым `LocalDateTime` иногда ссылаются на **разные** моменты? Ответ требует отделить **временную шкалу (instant)** от **настенного представления (local date/time)** и **правил региональной зоны (ZoneRules)**. Просто подставить `UTC` везде недостаточно для календаря конкретного региона.

**Нормативная версия:** Java SE **21**, без preview. Основные API: [Instant](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Instant.html), [Clock](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html), [LocalDate](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDate.html), [LocalTime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalTime.html), [LocalDateTime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDateTime.html), [OffsetDateTime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/OffsetDateTime.html), [ZonedDateTime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZonedDateTime.html), [ZoneRules](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/zone/ZoneRules.html), [Duration](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Duration.html), [Period](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Period.html). **Исходный проверенный G1** — [DateTimeProbe.java, 38 stdout-строк и команды, §5.2](../../docs/plans/CORE-NUMBERS-DATETIME.md); в его конкретной среде **OpenJDK 21.0.12.1, Debian 13, tzdb 2026b**.

**Покрытие:** **DT-01–DT-20 = 20 P0**, DT-21–DT-25 = **5 P1**, DT-26–DT-29 = **4 P2**. Вместе с [CORE-NUMBERS](numbers.md) — **42 P0 / 11 P1 / 8 P2 = 61 ID**, не 61 проверка JUnit. Связанные опубликованные материалы: [immutability](immutability.md), [строки](strings.md), [исключения](exceptions.md), [типы](types.md), [операторы](operators.md). Даты и интервалы здесь не заменяются числами секунд без понимания календаря.

## 1. Instant — момент, LocalDateTime — поля без временной зоны (DT-01–DT-03, DT-20)

`Instant` обозначает точку на временной шкале. Строка с `Z`, например `2024-03-30T12:34:56Z`, однозначно задаёт момент. `Instant.getEpochSecond()` отсчитывает секунды относительно эпохи `1970-01-01T00:00:00Z` по правилам Java time-scale. Это **не дата/час конкретного пользователя**.

`LocalDate` содержит год/месяц/число, `LocalTime` — час/минуту/секунду, `LocalDateTime` — оба набора полей, **но не зону и не offset**. Поэтому `LocalDateTime.of(2024, 3, 30, 12, 0)` само по себе не даёт уникальный `Instant`: для преобразования нужен offset или zone+правила разрешения. В G1 `DT36=1711802096` (epoch seconds конкретного `Instant`), а `DT37=2024-03-30T12:34:56` — локальная форма **при явно заданной UTC**.

~~~java
Instant moment = Instant.parse("2024-03-30T12:34:56Z");
LocalDateTime utcFields = LocalDateTime.ofInstant(moment, ZoneOffset.UTC);

System.out.println(moment);    // 2024-03-30T12:34:56Z
System.out.println(utcFields); // 2024-03-30T12:34:56
~~~

Две одинаково выглядящие временные цифры здесь имеют разные **типы и смысл**. При записи события нужен момент и, если требуется, исходная зона/контекст пользователя; при расписании «каждый день в 09:00 по Берлину» нужен локальный календарный контракт и ZoneId. Выбор зависит от того, нужен ли уникальный момент или локальное правило.

**Важное отрицательное доказательство:** вызов `LocalDateTime.toInstant()` **без** `ZoneOffset` не существует. Изолированный G1 `D_N01` дал ошибку `compiler.err.cant.apply.symbol`. Но корректный `LocalDateTime.toInstant(ZoneOffset)` **не проверяет историческую допустимость** offset для региональной зоны: вы явно передали фиксированное смещение. Это разные задачи.

## 2. Clock.fixed делает «сейчас» воспроизводимым (DT-02; DT-24 P1)

`Instant.now()` без аргументов использует источник времени окружения. Тест, зависящий от текущего момента/системной зоны, со временем меняет вывод. `Clock.fixed(moment, zone)` возвращает часы, которые **всегда выдают один Instant и фиксированную ZoneId**; все перегрузки `now(clock)` могут использовать одну точку.

~~~java
Instant moment = Instant.parse("2024-03-30T12:34:56Z");
Clock fixed = Clock.fixed(moment, ZoneOffset.UTC);

System.out.println(Instant.now(fixed));        // 2024-03-30T12:34:56Z
System.out.println(LocalDate.now(fixed));      // 2024-03-30
System.out.println(LocalTime.now(fixed));      // 12:34:56
System.out.println(LocalDateTime.now(fixed));  // 2024-03-30T12:34:56
~~~

G1 `DT01–DT04` именно такие; `DT34=true` для `fixed.getZone().equals(ZoneOffset.UTC)`. Clock **внедряют как зависимость** в код, вычисляющий текущую дату/срок, вместо жёстко вызванного `Instant.now()`. Это позволяет проверять границы суток и правила дедлайна. Но `Clock.fixed` — **не поток тиков**, он не двигает время и не гарантирует корректность планировщика/системных часов. `Clock.system`, `offset` и `tick` имеют другие контракты; реальный scheduler/NTP этим примером не проверялся (DT-24 P1).

## 3. ZoneId, ZoneOffset, ZonedDateTime, OffsetDateTime (DT-04–DT-06)

`ZoneOffset.ofHours(1)` — **фиксированный сдвиг** относительно UTC. `ZoneId.of("Europe/Berlin")` — **региональная зона** с правилами, которые могут менять offset в зависимости от даты. `Asia/Tokyo` в одном моменте G1 имел +09:00, Берлин +01:00. Неверно отождествлять `Europe/Berlin` и `+01:00` на все даты: летом Берлин использует другой offset.

~~~java
Instant moment = Instant.parse("2024-03-30T12:34:56Z");
ZoneId berlin = ZoneId.of("Europe/Berlin");
ZonedDateTime zoned = moment.atZone(berlin);
OffsetDateTime offset = OffsetDateTime.ofInstant(moment, berlin);

System.out.println(zoned); // 2024-03-30T13:34:56+01:00[Europe/Berlin]
System.out.println(offset); // 2024-03-30T13:34:56+01:00
System.out.println(zoned.toInstant().equals(moment)); // true
~~~

Это G1 `DT05`, `DT08`, `DT07=true`. `ZonedDateTime` сохраняет региональный ZoneId и локальное представление с offset, согласованным с правилами выбранного момента. `OffsetDateTime` содержит **offset**, но не региональный набор будущих переходов. Его значение однозначно интерпретируется как момент, однако само по себе не означает «все будущие записи по Berlin». Для долгоживущего регионального расписания обычно нужен ZoneId отдельно.

При том же `Instant`, но зоне Tokyo, G1 `DT06=2024-03-30T21:34:56+09:00[Asia/Tokyo]`. Это **другие настенные поля того же момента**, а не «изменившееся время события». Версии правил зоны следует сохранять в evidence: будущее обновление tzdb может изменить региональные данные; UTC offset не гарантирует неизменность будущего законодательства.

## 4. Как ZoneRules разрешает неоднозначное локальное время (DT-07–DT-11)

Для локальной пары дата+время у `ZoneRules.getValidOffsets(local)` обычно возможны **один offset**, при весеннем переходе **ноль**, при осеннем повторении **два**. Это не диагностика `javac`: локальные значения легальны как поля, проблема возникает при привязке к конкретной региональной зоне.

### Весна: gap — локальное время отсутствует (DT-07–DT-09)

В G1 для `Europe/Berlin`, **31 марта 2024 в 02:30**, по установленной **tzdb 2026b**:

~~~java
ZoneId berlin = ZoneId.of("Europe/Berlin");
LocalDateTime gap = LocalDateTime.of(2024, 3, 31, 2, 30);

System.out.println(berlin.getRules().getValidOffsets(gap).size()); // 0
System.out.println(gap.atZone(berlin));
// 2024-03-31T03:30+02:00[Europe/Berlin]

try {
    ZonedDateTime.ofStrict(gap, ZoneOffset.ofHours(1), berlin);
} catch (DateTimeException e) {
    System.out.println("DateTimeException:gap");
}
~~~

G1 `DT09=2024-03-31T02:30`, `DT10=0`, `DT11=2024-03-31T03:30+02:00[Europe/Berlin]`, `DT12=DateTimeException:gap`. Почему 03:30? Стандартное `atZone` разрешает gap **переносом локального времени вперёд на длину пропуска**. `ofStrict` при недопустимой паре (локальное время, offset, ZoneId) вместо коррекции **бросает runtime `DateTimeException`**. Нельзя говорить «любая несуществующая локальная дата — ошибка компиляции». Политика бронирования/планирования при gap — самостоятельное решение: сдвинуть, отклонить ввод или явно спросить пользователя.

### Осень: overlap — два настоящих момента с одними часами (DT-10, DT-11)

При обратном переходе `Europe/Berlin` **27 октября 2024 в 02:30** допустимы два offset:

~~~java
LocalDateTime repeated = LocalDateTime.of(2024, 10, 27, 2, 30);
System.out.println(berlin.getRules().getValidOffsets(repeated));
// [+02:00, +01:00]
ZonedDateTime first = repeated.atZone(berlin);
ZonedDateTime second = first.withLaterOffsetAtOverlap();
System.out.println(first);  // 2024-10-27T02:30+02:00[Europe/Berlin]
System.out.println(second); // 2024-10-27T02:30+01:00[Europe/Berlin]
System.out.println(Duration.between(first.toInstant(), second.toInstant()).toMinutes()); // 60
~~~

G1 `DT13=[+02:00, +01:00]`, `DT14/DT15` как выше, `DT16=60`. `atZone` при overlap по умолчанию предпочитает **более ранний offset, обычно летний**, а `withLaterOffsetAtOverlap` выбирает другой допустимый offset без смены настенных полей. Два значения с одинаковым `LocalDateTime` и ZoneId — **разные моменты**, здесь на один час. Если нужно точно сохранять время операции, храните moment/offset/идентификатор выбора, а не только `LocalDateTime`.

**Ограничение:** это конкретные переходы Берлина **2024** в одной tzdb. Не следует утверждать, что любой регион/год имеет ровно часовой переход или что у всех зон DST вообще существует (DT-26 P2).

## 5. Сменить зону — не то же самое, что сохранить настенные часы (DT-12, DT-13)

`withZoneSameInstant(otherZone)` сохраняет **момент** и пересчитывает локальную дату/время для новой зоны. `withZoneSameLocal(otherZone)` старается сохранить **локальную дату/время**, подбирая правила другой зоны — **момент обычно меняется** (при gap локальное время может быть скорректировано). Ни один метод не «просто редактирует строку зоны».

~~~java
ZonedDateTime berlinTime = Instant.parse("2024-03-30T12:34:56Z")
        .atZone(ZoneId.of("Europe/Berlin"));
ZoneId tokyo = ZoneId.of("Asia/Tokyo");

ZonedDateTime sameInstant = berlinTime.withZoneSameInstant(tokyo);
ZonedDateTime sameLocal = berlinTime.withZoneSameLocal(tokyo);
System.out.println(sameInstant); // 2024-03-30T21:34:56+09:00[Asia/Tokyo]
System.out.println(sameLocal);   // 2024-03-30T13:34:56+09:00[Asia/Tokyo]
~~~

G1 `DT17` и `DT18` как выше; `DT19=true` (sameInstant.toInstant == old Instant), `DT20=false` (sameLocal.toInstant != old Instant). Практический выбор зависит от смысла: событие с точным UTC-временем показываем в другом часовом поясе через *sameInstant*; пересаживаем локальное расписание «всегда 09:00» в другую зону — только с явными правилами *sameLocal* и gap/overlap политики.

## 6. «Один календарный день» не гарантирует 24 прошедших часа (DT-14–DT-16)

`Period` описывает **календарные годы/месяцы/дни**, `Duration` — **временную длительность в секундах и наносекундах**. Применять Period к дате и измерять Duration между двумя Instants — не взаимозаменяемые операции.

В G1 для Берлина:

~~~java
ZonedDateTime before = LocalDateTime.of(2024,3,30,12,0)
        .atZone(ZoneId.of("Europe/Berlin"));

ZonedDateTime plusDay = before.plusDays(1);
ZonedDateTime plusHours = before.plusHours(24);

System.out.println(before);    // 2024-03-30T12:00+01:00[Europe/Berlin]
System.out.println(plusDay);   // 2024-03-31T12:00+02:00[Europe/Berlin]
System.out.println(plusHours); // 2024-03-31T13:00+02:00[Europe/Berlin]
~~~

**Почему отличаются?** При переходе на летнее время стрелки часов перескочили один час. `plusDays(1)` у `ZonedDateTime` работает в **локальной календарной шкале**, сохраняя 12:00 и разрешая offset по зоне; между двумя Instants прошло **23 часа** (G1 `DT24=23`). `plusHours(24)` работает в **instant timeline**, проходит **24 часа**, поэтому местное время стало 13:00 (G1 `DT27=24`).

`Period.between(before.toLocalDate(), plusDay.toLocalDate())` даёт `P1D` (DT25), а `ChronoUnit.DAYS.between(LocalDate, LocalDate)` — **1 календарный день** (DT26). Это **не доказательство 24 часов**. Для SLA «в течение 24 часов» удобнее сравнивать моменты/Duration; для пользовательского «завтра в 12:00» требуется календарная семантика с региональной зоной. Разные продукты могут требовать другого выбора для gap/overlap.

## 7. Formatting/parsing, некорректные поля и исключения (DT-17–DT-19; DT-21 P1)

Форматирование — **текстовое представление**, не определение нового момента. Чтобы не зависеть от системной Locale и zone, используйте явные параметры:

~~~java
DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern("uuuu-MM-dd HH:mm XXX", Locale.ROOT)
        .withZone(ZoneId.of("Europe/Berlin"));
System.out.println(formatter.format(
        Instant.parse("2024-03-30T12:34:56Z")));
// 2024-03-30 13:34 +01:00
~~~

G1 `DT28=2024-03-30 13:34 +01:00`. `uuuu` здесь год по пролептическому ISO календарю, `yyyy` — год эры: различия важны при строгом разборе дат вне обычного положительного диапазона и с `ResolverStyle.STRICT` (DT-21 P1). Тема всех Locale/chronology/CLDR — самостоятельный CORE-I18N, не закрывается одним `Locale.ROOT`.

Корректный ISO текст `Instant.parse("2024-03-30T12:34:56Z")` возвращает соответствующий `Instant` (G1 DT29). А `LocalDate.parse("2024-02-30")` **синтаксически компилируется**, но во время исполнения бросает `DateTimeParseException` (G1 DT30). Это исключение конкретной операции разбора, а не неисправность класса LocalDate или компилятора.

Для отсутствующих полей/информации различаются:

~~~java
try {
    Instant.from(LocalDate.of(2024,3,30)); // только дата, недостаточно времени/offset
} catch (DateTimeException e) {
    System.out.println("DateTimeException:missingInstant");
}
try {
    LocalDate.of(2024,3,30).getLong(ChronoField.HOUR_OF_DAY);
} catch (UnsupportedTemporalTypeException e) {
    System.out.println("UnsupportedTemporalTypeException");
}
~~~

Это G1 `DT31=DateTimeException:missingInstant`, `DT32=UnsupportedTemporalTypeException`. Последнее — более специальное runtime-исключение API `TemporalAccessor`, а не checked exception из javac. Различайте **невалидный текст**, **недоступные поля** и **неоднозначное локальное время**: они требуют разных ответов приложения.

## 8. Неизменяемые значения и их жизненный цикл (DT-20; DT-22–DT-25 P1)

Экземпляры основных `java.time` классов являются **immutable value-based** объектами. Вызов `LocalDate.of(2024,3,30).plusDays(1)` создаёт результат для 31 марта и **не изменяет** исходный объект. G1 `DT33=2024-03-31`; код, который не присваивает/не использует результат `plusDays`, теряет вычисленное значение. Прочтите [общий механизм immutability](immutability.md): неизменяемость значения — **не** обещание корректного многопоточного протокола работы с внешним состоянием.

**DT-22 P1:** `LocalDate.atStartOfDay(zone)` ищет *самое раннее допустимое время* соответствующей даты — иногда это **не** 00:00 из-за исторического midnight gap. Переходы Берлина в §4 не проверяют все такие зоны. **DT-23 P1:** если версия tzdb обновилась, пересчёт будущих локальных расписаний через региональный `ZoneId` может дать иной offset/Instant; уже сохранённый `OffsetDateTime` не превращается автоматически в новую зону. Это граница хранения данных, не эксперимент G1.

**DT-24 P1:** `Clock.system` и обёртки `Clock.offset/tick` применяются при проектировании зависимостей, но точность hardware/OS scheduler в рамках этого примера не измерялась. **DT-25 P1:** `System.nanoTime()` пригоден для **относительного измерения интервалов в пределах одного источника**, а не для записи UTC timestamp; его численное значение не означает дату. Отдельный стенд для monotonic deadlines, NTP и distributed clock skew **NOT RUN**.

## 9. Компилятор и runtime — четыре изолированные ошибки типов

Принятый G1 отдельно проверил четыре компиляции, у каждой exit=1 и тематический код `compiler.err.*`:

| G1 case | Чего не хватает Java-типам | Тематический diagnostic |
|---|---|---|
| `D_N01_LocalDateTimeToInstant` | У `LocalDateTime.toInstant()` нет нулевой арности; нужен `ZoneOffset` | `compiler.err.cant.apply.symbol` |
| `D_N02_PeriodInstant` | `Period.between` требует две `LocalDate`, не два `Instant` | `compiler.err.cant.apply.symbol` |
| `D_N03_WrongZone` | `Instant.atZone` принимает `ZoneId`, а не результат `ZoneOffset.UTC.getId()` типа `String` | `compiler.err.cant.apply.symbol` |
| `D_N04_WrongFormatter` | `LocalDate.format` принимает `DateTimeFormatter`, не pattern-строку | `compiler.err.cant.apply.symbol` |

**Исторически:** 4 DT + 4 NM = **8/8 изолированных compiler-negative** проверок G1/04 с `javac --release 21 -XDrawDiagnostics`, **вне позитивной G2 CI**. Это не значит, что все примеры с ошибкой во времени «не компилируются»: `gap.atZone(berlin)`, `ZonedDateTime.ofStrict`, `LocalDate.parse`, `Instant.from(LocalDate)` и `Duration.between` — корректные Java-вызовы и либо дают значение, либо бросают **runtime** исключение.

Полные отдельные compiler-negative sources/команды и причины — [G1 §6](../../docs/plans/CORE-NUMBERS-DATETIME.md). НЕ заявлять, что штатный positive runner выполнял эти восемь отдельных компиляций, пока это не подтверждено отдельным CI.

## 10. Матрица покрытия CORE-DATETIME

| ID | P | Механизм, секция, G1 trace |
|---|---|---|
| DT-01 | P0 | §1: Instant/epoch vs local, DT01/DT36 |
| DT-02 | P0 | §2: Clock.fixed, DT01–04/DT34 |
| DT-03 | P0 | §1: LocalDate/Time/DateTime no zone, DT02–04/DT37 |
| DT-04 | P0 | §3: regional ZoneId vs fixed offset, DT05/06/35 |
| DT-05 | P0 | §3: ZonedDateTime/time zone, DT05/07 |
| DT-06 | P0 | §3: OffsetDateTime vs region, DT08 |
| DT-07 | P0 | §4: DST gap getValidOffsets=0, DT09/10 |
| DT-08 | P0 | §4: gap resolution, DT11 |
| DT-09 | P0 | §4: ofStrict runtime exception, DT12 |
| DT-10 | P0 | §4: DST overlap validOffsets=2, DT13 |
| DT-11 | P0 | §4: two distinct overlap Instants, DT14–16 |
| DT-12 | P0 | §5: withZoneSameInstant, DT17/19 |
| DT-13 | P0 | §5: withZoneSameLocal, DT18/20 |
| DT-14 | P0 | §6: Period vs Duration, DT24/25 |
| DT-15 | P0 | §6: plusDays/plusHours 23/24h, DT21–24/27 |
| DT-16 | P0 | §6: ChronoUnit.DAYS dates, DT26 |
| DT-17 | P0 | §7: explicit formatter zone/Locale, DT28 |
| DT-18 | P0 | §7: parsing and invalid date, DT29/30 |
| DT-19 | P0 | §7: missing temporal field, DT31/32 |
| DT-20 | P0 | §8: immutable java.time, DT33 |
| DT-21 | P1 | §7: resolver strict/smart / uuuu vs yyyy, no standalone run |
| DT-22 | P1 | §8: atStartOfDay midnight gap; NOT RUN |
| DT-23 | P1 | §3/8: tzdb update/offset persistence; NOT RUN |
| DT-24 | P1 | §2/8: Clock offset/tick/system/scheduler; NOT RUN |
| DT-25 | P1 | §8: nanoTime vs wall time; NOT RUN |
| DT-26 | P2 | §4/11: other tzdb/JDK/OS transitions NOT RUN |
| DT-27 | P2 | §8/11: distributed clocks/leap seconds NOT RUN |
| DT-28 | P2 | §11: DB TIMESTAMP/JDBC/ORM NOT RUN |
| DT-29 | P2 | §7/11: chronology/Locale/advanced formatting NOT RUN |

## 11. Нормативные A, интервью C и границы воспроизводимости

**Нормы Java 21:** [Instant](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Instant.html), [ZonedDateTime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZonedDateTime.html), [ZoneId](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZoneId.html), [ZoneRules](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/zone/ZoneRules.html), [Clock](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html), [Period](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Period.html), [Duration](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Duration.html), [ChronoUnit](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/temporal/ChronoUnit.html), [DateTimeFormatter](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html), [DateTimeParseException](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeParseException.html). Региональные правила **не часть вечной гарантии одной версии API**; они поступают из версионируемой базы зон.

**Проверенные реальные C:** [Baeldung, Java 8 Interview Questions, вопрос о новом Date/Time API](https://www.baeldung.com/java-8-interview-questions); [ViacheslavChernyshov README, вопрос об immutability LocalDate/LocalDateTime](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/eng/README.md). Это подтверждение **широких** интервью-вопросов, не Java21 спецификация и не частотность; отдельные проверенные формулировки про `getValidOffsets`, gap/overlap, `withZoneSameInstant/Local` и `Clock.fixed` **в проверенных банках не обнаружены (C GAP)**. Такие механизмы нужны для строгого ответа на уточняющие вопросы, не для имитации найденных источников.

**Историческая G1 фактическая проверка:** OpenJDK `21.0.12.1`, Debian 13, `ZoneRulesProvider.getVersions("Europe/Berlin").lastKey() == "2026b"`, полный `DateTimeProbe.java` с фиксированным Instant/Locale/Zone, stdout **38 строк**, SHA-256 `71a4bb9934201ec65ba8e7d89658dd19a9b815d40a6c2218b6dd125d61b4d908`; вместе с NumbersProbe **71 stdout строк из двух JVM, а не 71 JUnit-тест**. Исходник и full stdout в [принятом G1 §5.2](../../docs/plans/CORE-NUMBERS-DATETIME.md). Это **локальное историческое evidence G1**, не утверждение, что новый runnable уже опубликован в данной ветке. Отдельная реализация 03 — [Issue #187](https://github.com/Melchiorius/backend-interview-labs/issues/187).

**NOT RUN на стадии авторского G2 текста:** все короткие embedded snippets отдельно как compilation units; новые project runner/CI пока не заявлены; все иные JDK/vendor/OS/tzdb, будущие переходы и исторические timezone rule changes, нетипичные midnight-gaps, real parser ResolverStyle tests, полноценные Locale/CLDR/chronology, распределённые часы/NTP/leap seconds/monotonic deadline tests, JDBC/DB TIMESTAMP/ORM, scheduler timing, все P2, IDE/Obsidian GUI, HTTP status/anchors. **Не** обещаем воспроизводимость UTC/DST примера на бесконечно будущих версиях tzdb: фиксированы конкретный год, зона и реально наблюдавшаяся версия базы.

[CORE-NUMBERS](numbers.md) объясняет числовое представление; [immutability](immutability.md) — общий принцип value objects. Статусы [CURRICULUM](../../docs/CURRICULUM.md) и [COVERAGE](../../docs/COVERAGE.md) меняются только после G5/G6.
