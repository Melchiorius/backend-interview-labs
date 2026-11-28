# CORE-DATETIME — Instant, локальное время, часовые пояса и DST в Java 21

**Практическая проблема.** Пользователь указал «31 марта 2024 года, 02:30, Берлин». Почему стандартное преобразование даёт 03:30? Почему «завтра в 12:00» может наступить через **23 часа**, а не через 24? Почему два расписания с одинаковым `LocalDateTime` иногда ссылаются на **разные** моменты? Ответ требует отделить **временную шкалу (instant)** от **настенного представления (local date/time)** и **правил региональной зоны (ZoneRules)**. Просто подставить `UTC` везде недостаточно для календаря конкретного региона.

**Нормативная версия:** Java SE **21**, без preview. Основные API: [Instant](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Instant.html), [Clock](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html), [LocalDate](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDate.html), [LocalTime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalTime.html), [LocalDateTime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/LocalDateTime.html), [OffsetDateTime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/OffsetDateTime.html), [ZonedDateTime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZonedDateTime.html), [ZoneRules](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/zone/ZoneRules.html), [Duration](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Duration.html), [Period](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Period.html). **Исходный проверенный G1** — [DateTimeProbe.java, 38 stdout-строк и команды, §5.2](../../docs/plans/CORE-NUMBERS-DATETIME.md); в его конкретной среде **OpenJDK 21.0.12.1, Debian 13, tzdb 2026b**. **Запускаемая проверка G2:** [DateTimeProbe.java](../../examples/core/numbers-datetime/src/DateTimeProbe.java), [stdout-эталон](../../examples/core/numbers-datetime/datetime.expected.stdout) и [README с командами](../../examples/core/numbers-datetime/README.md). Историческая версия tzdb не гарантирует одинаковые результаты на всех будущих JDK.

**Покрытие:** **DT-01–DT-20 = 20 P0**, DT-21–DT-25 = **5 P1**, DT-26–DT-29 = **4 P2**. Вместе с [CORE-NUMBERS](numbers.md) — **42 P0 / 11 P1 / 8 P2 = 61 ID**, не 61 проверка JUnit. Связанные опубликованные материалы: [immutability](immutability.md), [строки](strings.md), [исключения](exceptions.md), [типы](types.md), [операторы](operators.md). Даты и интервалы здесь не заменяются числами секунд без понимания календаря.

## 1. Instant — момент, LocalDateTime — поля без временной зоны (DT-01–DT-03, DT-20)

`Instant` обозначает точку на временной шкале. Строка с `Z`, например `2024-03-30T12:34:56Z`, однозначно задаёт момент. `Instant.getEpochSecond()` отсчитывает секунды относительно эпохи `1970-01-01T00:00:00Z` по правилам Java time-scale. Это **не дата/час конкретного пользователя**.

Внутреннее представление — целые секунды эпохи (`long`) и доля секунды в наносекундах (`int` от 0 до 999 999 999). Дробь нормализуется даже до эпохи: `Instant.ofEpochMilli(-1)` имеет `getEpochSecond() == -1` и `getNano() == 999000000`, то есть −1 + 0,999 = −0,001 секунды. Обратное `toEpochMilli()` даёт −1. При наличии доли миллисекунды точность теряется: `Instant.ofEpochSecond(-1, 999999999).toEpochMilli()` также даёт −1 (отрицательный момент на одну наносекунду до эпохи); для положительной доли меньше миллисекунды результат 0. Слишком далёкий момент, например `Instant.MAX`, не помещается в `long` миллисекунд — `ArithmeticException`. Java time-scale задаёт собственную шкалу; это не гарантия измерения физических високосных секунд, монотонности или точности системных часов.

`LocalDate` содержит год/месяц/число, `LocalTime` — час/минуту/секунду и наносекунды, `LocalDateTime` — оба набора полей, **но не зону и не offset**. Поэтому `LocalDateTime.of(2024, 3, 30, 12, 0)` само по себе не даёт уникальный `Instant`: для преобразования нужен offset или zone+правила разрешения. В G1 `DT36=1711802096` (epoch seconds конкретного `Instant`), а `DT37=2024-03-30T12:34:56` — локальная форма **при явно заданной UTC**.

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

G1 `DT01–DT04` именно такие; `DT34=true` для `fixed.getZone().equals(ZoneOffset.UTC)`. Clock **внедряют как зависимость** в код, вычисляющий текущую дату/срок, вместо жёстко вызванного `Instant.now()`. Это позволяет проверять границы суток и правила дедлайна. Но `Clock.fixed` — **не поток тиков**, он не двигает время и не гарантирует корректность планировщика/системных часов.

Зона часов влияет на локальные поля, а не на сам момент. Для `Clock.fixed(Instant.parse("2024-03-30T23:30:00.987654321Z"), ZoneOffset.UTC)` вызов `withZone(ZoneId.of("Asia/Tokyo"))` сохраняет instant, но `LocalDate.now` возвращает уже 31 марта вместо 30-го. Наносекунды момента при преобразовании в локальные поля не теряются.

`Clock.offset(fixed, Duration.ofSeconds(67))` выдаёт 23:31:07.987654321Z, а `Clock.tick(offset, Duration.ofSeconds(1))` — 23:31:07Z: tick усекает момент до границы шага, не округляет к ближайшему. Нулевая длительность и 1 нс возвращают базовые часы; отрицательная длительность или шаг с дробной миллисекундой, не делящий секунду нацело (например 3 нс), недопустимы. Слишком большая длительность, не представимая в наносекундах, может вызвать `ArithmeticException`. Реализация tick может кэшировать значения, поэтому момент видимого тика может запаздывать относительно базовых часов.

`Clock.system(zone)` опирается на доступный системный источник; API не гарантирует монотонность или точность физического времени. Если сервису нужен лишь текущий `Instant`, можно внедрить [InstantSource](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/InstantSource.html), не навязывая зону. Если требуется локальная дата, нужен явный выбор зоны. Проверки fixed/offset/tick здесь детерминированы, не испытание планировщика или NTP.

## 3. ZoneId, ZoneOffset, ZonedDateTime, OffsetDateTime (DT-04–DT-06)

`ZoneOffset.ofHours(1)` — **фиксированный сдвиг** относительно UTC. `ZoneId.of("Europe/Berlin")` — **региональная зона** с правилами, которые могут менять offset в зависимости от даты. `Asia/Tokyo` в одном моменте G1 имел +09:00, Берлин +01:00. Неверно отождествлять `Europe/Berlin` и `+01:00` на все даты: летом Берлин использует другой offset.

[ZoneOffset](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZoneOffset.html) наследует [ZoneId](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZoneId.html). У ZoneId возможны фиксированные и региональные правила, а сам региональный ID не гарантирует DST.

- `ZoneOffset (+01:00) → ZoneRules`: один постоянный offset.
- `ZoneId (Europe/Berlin) → ZoneRules провайдера → offset для конкретного Instant`: правила могут содержать переходы, разные ID могут использовать одинаковые правила.
- `LocalDateTime + ZoneRules → допустимые offsets → выбранный offset → Instant`: в отличие от преобразования готового момента, может понадобиться разрешение неоднозначности (§4).

`getOffset(Instant)` однозначен для данных правил, а `getOffset(LocalDateTime)` не является проверкой допустимости (§4).

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

**Равенство значений и равенство моментов.** Два `ZonedDateTime` одного момента в Берлине и Токио имеют разные локальные поля/зоны, поэтому `equals` возвращает false, а `isEqual` и сравнение `toInstant().equals(...)` — true. У `OffsetDateTime` равенство значений учитывает локальные поля и offset; в этом же примере `equals` false, `isEqual` true. Естественный `compareTo` у обоих типов не ограничивается моментом: при одинаковом instant учитываются дополнительные поля. В фактической проверке сравнение Berlin с Tokyo у ZonedDateTime дало −1; это не универсальный знак сравнения. Выбирайте контракт сравнения по назначению ключа/сортировки; см. [equals/hashCode](equality.md) и [immutability](immutability.md).

На 26 октября 2024, 12:00 в Берлине offset +02:00. При `plusDays(1)` у регионального значения получится 27 октября 12:00+01:00, а у соответствующего OffsetDateTime — 27 октября 12:00+02:00. Моменты различаются: фиксированное смещение не выполняет переход по правилам региона.

При том же `Instant`, но зоне Tokyo, G1 `DT06=2024-03-30T21:34:56+09:00[Asia/Tokyo]`. Это **другие настенные поля того же момента**, а не «изменившееся время события». Версии правил зоны следует сохранять в evidence: будущее обновление tzdb может изменить региональные данные; UTC offset не гарантирует неизменность будущего законодательства. Идентификатор версии из [ZoneRulesProvider.getVersions](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/zone/ZoneRulesProvider.html#getVersions(java.lang.String)) зависит от провайдера. Для сохранённого события и будущего расписания действуют разные требования (§8).

## 4. Как ZoneRules разрешает неоднозначное локальное время (DT-07–DT-11)

Для локальной пары дата+время у `ZoneRules.getValidOffsets(local)` обычно возможны **один offset**, при весеннем переходе **ноль**, при осеннем повторении **два**. Это не диагностика `javac`: локальные значения легальны как поля, проблема возникает при привязке к конкретной региональной зоне.

| Число элементов `getValidOffsets(local)` | Что делать с результатом |
|---|---|
| 1 | Использовать единственный offset; `getTransition(local)` обычно null. |
| 0 (gap) | Получить переход через `getTransition(local)`, проверить его длину, выбрать политику отказа или сдвига. |
| 2 (overlap) | Выбрать один из двух допустимых offset по договору приложения, а не по произвольному числовому минимуму. |

API допускает теоретически более двух offset при нескольких быстрых обратных переводах часов; это не наблюдалось в проверенных примерах. `getOffset(local)` при gap/overlap возвращает offset до перехода (лучшее приближение), а не доказательство его допустимости. Для проверки пары используйте `getValidOffsets` или `isValidOffset`; [ZoneOffsetTransition](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/zone/ZoneOffsetTransition.html) описывает границы и длительность перехода.

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

В отдельном D-A22b Probe для того же Berlin gap получены пустой список допустимых offset, `getOffset(gap)` вернул +01:00 (равенство с `ZoneOffset.ofHours(1)` проверено через `equals`), длительность перехода PT1H. При `Australia/Lord_Howe`, 6 октября 2024 в 02:15, длина пропуска PT30M, результат `atZone` — 02:45+11:00. Следовательно, прибавлять ровно час вручную нельзя.

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

«Более ранний» относится к первому появлению локального времени на временной шкале, а не к численно меньшему offset: в Берлине +02:00 раньше +01:00. `ZonedDateTime.ofLocal(repeated, berlin, ZoneOffset.ofHours(1))` выбирает поздний момент, поскольку предпочитаемый offset допустим. Если предпочитаемый offset не подходит, выбирается ранний по обычному правилу; `withEarlierOffsetAtOverlap` и `withLaterOffsetAtOverlap` позволяют явно сменить выбор. `ofStrict` принимает обе допустимые пары по отдельности, но отклоняет, например, UTC для этого overlap. В авторской проверке 02 D-A22b проверены ранний и поздний выбор, допустимый предпочитаемый offset, обе допустимые пары `ofStrict` и отказ для UTC. В проверке 02 поведение при недопустимом предпочитаемом offset и контракт `withEarlierOffsetAtOverlap` были приведены по API, без отдельного запуска. Независимая проверка 03 затем подтвердила выбор раннего offset при недопустимом предпочитаемом offset и действие `withEarlierOffsetAtOverlap` (см. §11).

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

G1 `DT17` и `DT18` как выше; `DT19=true` (`sameInstant.toInstant().equals(instant)`), `DT20=false` (`sameLocal.toInstant().equals(instant)`). Практический выбор зависит от смысла: событие с точным UTC-временем показываем в другом часовом поясе через *sameInstant*; пересаживаем локальное расписание «всегда 09:00» в другую зону — только с явными правилами *sameLocal* и gap/overlap политики.

Для `withZoneSameInstant` исходный instant уже определён, поэтому не требуется выбирать между двумя локальными кандидатами целевой зоны. При `withZoneSameLocal` предыдущий offset используется как предпочитаемый, если допустим: `repeated.atZone(ZoneOffset.ofHours(1)).withZoneSameLocal(berlin)` даёт 02:30+01:00, а не ранние 02:30+02:00. Если исходные поля равны 31 марта 02:30 и исходный offset +01:00, смена зоны на Berlin методом sameLocal сдвигает локальное время на 03:30 — обещания всегда сохранить поля нет.

## 6. «Один календарный день» не гарантирует 24 прошедших часа (DT-14–DT-16)

`Period` описывает **календарные годы/месяцы/дни**, `Duration` — **временную длительность в секундах и наносекундах**. Применять Period к дате и измерять Duration между двумя Instants — не взаимозаменяемые операции. `Duration.ofDays(1)` — ровно 86 400 секунд шкалы Java, а `Period.ofDays(1)` — один календарный день при применении к подходящему temporal. Месяц не имеет фиксированной длительности: `LocalDate.of(2024, 1, 31).plusMonths(1)` корректируется до 29 февраля.

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

Осенью картина обратная. Для Berlin 2024-10-26 12:00+02:00 прибавление `Period.ofDays(1)` даёт 27 октября 12:00+01:00 (25 прошедших часов); `Duration.ofDays(1)` — 27 октября 11:00+01:00 (24 часа). `ChronoUnit.DAYS.between` между исходным значением Berlin 2024-10-26 12:00+02:00 и результатом прибавления `Duration.ofDays(1)`, 2024-10-27 11:00+01:00 в той же зоне, возвращает 0 полных локальных дней (ещё не наступило 12:00 следующей даты). Для тех же двух значений после преобразования каждого через `toLocalDate()` результат `ChronoUnit.DAYS.between` равен 1. Проверять надо именно требуемый вид интервала.

Не все единицы поддерживаются всеми типами: `Instant.EPOCH.plus(Period.ofMonths(1))` и `LocalDate.of(2024,1,1).plus(1, ChronoUnit.HOURS)` дают `UnsupportedTemporalTypeException`. Календарная коррекция в конце месяца не гарантирует обратимость любой пары plus/minus.

## 7. Formatting/parsing, некорректные поля и исключения (DT-17–DT-19; DT-21 P1)

Форматирование — **текстовое представление**, не определение нового момента. Чтобы не зависеть от системной Locale и zone, используйте явные параметры. DateTimeFormatter неизменяем и потокобезопасен: его можно повторно использовать, но это не делает само преобразование без потерь:

~~~java
DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern("uuuu-MM-dd HH:mm XXX", Locale.ROOT)
        .withZone(ZoneId.of("Europe/Berlin"));
System.out.println(formatter.format(
        Instant.parse("2024-03-30T12:34:56Z")));
// 2024-03-30 13:34 +01:00
~~~

G1 `DT28=2024-03-30 13:34 +01:00`. `uuuu` здесь год по пролептическому ISO календарю, `yyyy` — год эры: различия важны при строгом разборе дат вне обычного положительного диапазона и с `ResolverStyle.STRICT` (DT-21 P1). Тема всех Locale/chronology/CLDR — самостоятельный CORE-I18N, не закрывается одним `Locale.ROOT`.

`withZone` задаёт переопределение зоны: при форматировании момента он переводится в поля этой зоны; это не метод разрешения произвольного LocalDateTime. Если локальному значению не хватает offset для `XXX`, то даже formatter с региональным withZone может выбросить `UnsupportedTemporalTypeException` (проверено здесь). При разборе явно прочитанный из текста ZoneId имеет приоритет над override. Подробнее — [withZone](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html#withZone(java.time.ZoneId)).

Шаблон выше не выводит секунды, наносекунды или региональный ID. Для момента 2024-03-30T23:30:00.987654321Z и момента на 1 мс позже получен один и тот же текст 2024-03-31 00:30 +01:00. Это отображение с потерей информации, а не формат для точного восстановления исходного момента. Для roundtrip момента подходит `DateTimeFormatter.ISO_INSTANT` вместе с `Instant.parse` (проверено); исходный регион при этом отдельно не восстанавливается.

Корректный ISO текст `Instant.parse("2024-03-30T12:34:56Z")` возвращает соответствующий `Instant` (G1 DT29). Вызов `LocalDate.parse("2024-02-30")` компилируется как корректный Java-код, но при исполнении разбор недопустимой даты завершается исключением `DateTimeParseException` (G1 DT30). Это исключение конкретной операции разбора, а не неисправность класса LocalDate или компилятора.

**Две стадии разбора.** Сначала formatter читает символы и извлекает поля, затем согласует их в дату/время. `LocalDate.parse(text)` использует ISO_LOCAL_DATE со строгим разрешением, а `DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ROOT)` по умолчанию SMART. Для строки `2023-02-29`:

| [ResolverStyle](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/ResolverStyle.html) | Фактический результат LocalDate.parse |
|---|---|
| STRICT | DateTimeParseException |
| SMART | 2023-02-28 |
| LENIENT | 2023-03-01 |

`yyyy-MM-dd` со STRICT может не разрешить даже корректную дату 2024-03-30: получено поле YEAR_OF_ERA без эры. Для строгого ISO года здесь нужен `uuuu-MM-dd` (та же дата успешно разобрана). ResolverStyle управляет разрешением уже извлечённых полей. Например, в независимой проверке 03 строка `2023/02/29` при разборе через `LocalDate.parse` с formatter по шаблону `uuuu-MM-dd` и даже с `ResolverStyle.LENIENT` вызвала `DateTimeParseException`: разделители не соответствуют шаблону ещё на стадии чтения текста. ResolverStyle не является универсальной проверкой всей предметной семантики; строгий разбор даты и `ZonedDateTime.ofStrict(local, offset, zone)` — разные операции.

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

| [ChronoField](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/temporal/ChronoField.html) | LocalDate | LocalTime | LocalDateTime | Instant |
|---|---|---|---|---|
| HOUR_OF_DAY | нет | да | да | нет |
| NANO_OF_SECOND | нет | да | да | да |
| INSTANT_SECONDS | нет | нет | нет | да |

Это таблица возможностей по API, а не все комбинации экспериментально перебраны. [TemporalAccessor](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/temporal/TemporalAccessor.html) позволяет спросить `isSupported(field)` и `range(field)`: поддержка поля ещё не гарантирует, что его диапазон помещается в int. Для Instant `get(INSTANT_SECONDS)` отклоняется даже при поддерживаемом поле; `getLong(INSTANT_SECONDS)` работает. `get(NANO_OF_SECOND)` возвращает долю секунды. `Instant.from` требует INSTANT_SECONDS и NANO_OF_SECOND, поэтому произвольных локальных полей недостаточно; из ZonedDateTime извлечение работает.

Различайте причины: отсутствие поля или слишком широкий диапазон для get — `UnsupportedTemporalTypeException`; значение 24 для HOUR_OF_DAY при проверке диапазона — `DateTimeException`; ошибка разбора — `DateTimeParseException`; неизвестный регион — `ZoneRulesException`; недопустимый offset (например +19:00) — `DateTimeException`. Все эти конкретные отказы проверены отдельно по точному классу исключения, не общий catch-all рецепт.

## 8. Неизменяемые значения и их жизненный цикл (DT-20; DT-22–DT-25 P1)

Экземпляры основных `java.time` классов являются **immutable value-based** объектами. Вызов `LocalDate.of(2024,3,30).plusDays(1)` возвращает значение результата для 31 марта и **не изменяет** исходный объект. G1 `DT33=2024-03-31`; код, который не присваивает/не использует результат `plusDays`, теряет вычисленное значение. Прочтите [общий механизм immutability](immutability.md): неизменяемость значения — **не** обещание корректного многопоточного протокола работы с внешним состоянием.

В D-A22b проверен исходный `d = LocalDate.of(2024,3,30)`, результат `next = d.plusDays(1)` и проигнорированный вызов `d.plusDays(67)`: d остаётся 2024-03-30, next — 2024-03-31. `d.plusDays(0).equals(d)` истинно; отдельной гарантии новой ссылочной идентичности нет. Используйте `equals` для значений, а не `==`; [value-based контракт](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/doc-files/ValueBased.html) не допускает зависимости от идентичности и синхронизации на таких экземплярах.

**DT-22 P1: начало дня не обязано принадлежать запрошенной дате.** `LocalDate.atStartOfDay()` без зоны возвращает локальные поля даты в 00:00. `atStartOfDay(zone)` разрешает начало дня по правилам зоны: при overlap выбирает первое появление полуночи, при gap — момент сразу после пропуска. Если пропущена целая календарная дата, результат может относиться к следующей дате.

| Вход (выбранные правила tzdb 2026b) | Фактический результат atStartOfDay(zone) |
|---|---|
| 2018-11-04, America/Sao_Paulo | 2018-11-04T01:00-02:00[America/Sao_Paulo] |
| 2011-12-30, Pacific/Apia | 2011-12-31T00:00+14:00[Pacific/Apia] |

В Apia пропуск длится 24 часа. Для сравнения: `LocalDate.of(2011,12,30).atTime(12,0).atZone(ZoneId.of("Pacific/Apia"))` переносит поля на 31 декабря 12:00+14:00, а не на начало следующего дня: обычный atZone сдвигает на длину gap. Прикладной контракт «дата должна сохраниться» требует проверить `result.toLocalDate().equals(requestedDate)` и отклонить результат, если это условие нарушено; не следует молча принимать другую дату. Это конкретные переходы, а не универсальная политика или правило часового пропуска.

**DT-23 P1: что хранить.** Для произошедшего события хранится Instant, при необходимости — исходный регион и offset/представление. Для будущего локального расписания нужен локальный календарный замысел, ZoneId и явная политика gap/overlap; при обновлении правил может понадобиться пересчёт будущих моментов. OffsetDateTime сохраняет offset, но не приобретает автоматически региональные правила. Версию использованных правил следует фиксировать в доказательствах и учитывать при выборе политики пересчёта.

В фактической проверке D-A22b `ZoneRulesProvider.getVersions(id).keySet()` вернул [2026b] для Europe/Berlin, Asia/Tokyo, Australia/Lord_Howe, America/Sao_Paulo и Pacific/Apia. Это результат установленного провайдера, не утверждение о глобально последней версии; имитация смены tzdb и контракт хранения в DB не проверялись.

**DT-24 P1:** примеры внедрения часов и композиции fixed/offset/tick приведены в §2. Реальные hardware/OS scheduler и точность системных часов не измерялись.

**DT-25 P1:** [System.nanoTime()](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#nanoTime()) возвращает показание источника времени в наносекундах относительно произвольного фиксированного начала, общего для вызовов в пределах одной JVM. Прошедший интервал получают вычитанием двух показаний. Это не UTC timestamp, значения разных JVM сравнивать напрямую нельзя. Единица представления — наносекунда; это не гарантия наносекундного разрешения (способности различать близкие показания) или точности измерения интервала (близости измеренной длительности к действительной). Для неотрицательного timeout в наносекундах сравнивайте `System.nanoTime() - start >= timeout`, а не `System.nanoTime() >= start + timeout`. Разность корректна при интервале меньше 2^63 нс (около 292 лет), иначе переполнение мешает измерению.

Синтетическая проверка: start = Long.MAX_VALUE − 10, now = Long.MAX_VALUE − 5, timeout = 20. Прошло 5 нс, поэтому сравнение разности ложно; сравнение абсолютного дедлайна истинно из-за переполнения суммы. После условного перехода через границу, now = Long.MIN_VALUE + 10, разность равна 21 и порог достигнут. Это проверка арифметики (см. [CORE-NUMBERS](numbers.md)), не наблюдение реального переполнения nanoTime, NTP, scheduler или distributed clock skew.

## 9. Компилятор и runtime — четыре изолированные ошибки типов

Принятый G1 отдельно проверил четыре компиляции, у каждой exit=1 и тематический код `compiler.err.*`:

| G1 case | Чего не хватает Java-типам | Тематический diagnostic |
|---|---|---|
| `D_N01_LocalDateTimeToInstant` | У `LocalDateTime.toInstant()` нет нулевой арности; нужен `ZoneOffset` | `compiler.err.cant.apply.symbol` |
| `D_N02_PeriodInstant` | `Period.between` требует две `LocalDate`, не два `Instant` | `compiler.err.cant.apply.symbol` |
| `D_N03_WrongZone` | `Instant.atZone` принимает `ZoneId`, а не результат `ZoneOffset.UTC.getId()` типа `String` | `compiler.err.cant.apply.symbol` |
| `D_N04_WrongFormatter` | `LocalDate.format` принимает `DateTimeFormatter`, не pattern-строку | `compiler.err.cant.apply.symbol` |

**Исторически:** 4 DT + 4 NM = **8/8 изолированных compiler-negative** проверок G1/04 с `javac --release 21 -XDrawDiagnostics`, **вне позитивной G2 CI**. Ошибки разрешения времени следует отличать от ошибок типов: `gap.atZone(berlin)`, `ZonedDateTime.ofStrict`, `LocalDate.parse`, `Instant.from(LocalDate)` и `Duration.between` — корректные Java-вызовы и либо дают значение, либо бросают **runtime** исключение.

Полные отдельные compiler-negative sources/команды и причины — [G1 §6](../../docs/plans/CORE-NUMBERS-DATETIME.md). Эти восемь отдельных компиляций не входят в штатный positive runner. Известная описка о количестве в G1 §5 пояснена в [README](../../examples/core/numbers-datetime/README.md). Новые проверки runtime не являются повтором исторических compiler-negative.

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
| DT-21 | P1 | §7: resolver strict/smart / uuuu vs yyyy, исторически no standalone run; D-A22b выбранные STRICT/SMART/LENIENT и yyyy/uuuu |
| DT-22 | P1 | §8: atStartOfDay midnight gap; G1 NOT RUN; D-A22b Sao_Paulo/Apia |
| DT-23 | P1 | §3/8: tzdb update/offset persistence; смена версии NOT RUN; D-A22b версия провайдера и fixed vs regional |
| DT-24 | P1 | §2/8: Clock offset/tick/system/scheduler; G1 NOT RUN; D-A22b fixed/offset/tick; scheduler NOT RUN |
| DT-25 | P1 | §8: nanoTime vs wall time; G1 NOT RUN; D-A22b синтетическое переполнение, не реальный таймер |
| DT-26 | P2 | §4/11: other tzdb/JDK/OS transitions NOT RUN |
| DT-27 | P2 | §8/11: distributed clocks/leap seconds NOT RUN |
| DT-28 | P2 | §11: DB TIMESTAMP/JDBC/ORM NOT RUN |
| DT-29 | P2 | §7/11: chronology/Locale/advanced formatting NOT RUN |

## 11. Нормативные A, интервью C и границы воспроизводимости

**Нормы Java 21:** [Instant](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Instant.html), [ZonedDateTime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZonedDateTime.html), [ZoneId](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZoneId.html), [ZoneRules](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/zone/ZoneRules.html), [Clock](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html), [Period](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Period.html), [Duration](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Duration.html), [ChronoUnit](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/temporal/ChronoUnit.html), [DateTimeFormatter](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html), [DateTimeParseException](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeParseException.html). Региональные правила **не часть вечной гарантии одной версии API**; они поступают из версионируемой базы зон.

**Проверенные реальные C:** [Baeldung, Java 8 Interview Questions, вопрос о новом Date/Time API](https://www.baeldung.com/java-8-interview-questions); [ViacheslavChernyshov README, вопрос об immutability LocalDate/LocalDateTime](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/eng/README.md). Это подтверждение **широких** интервью-вопросов, не Java21 спецификация и не частотность; отдельные проверенные формулировки про `getValidOffsets`, gap/overlap, `withZoneSameInstant/Local` и `Clock.fixed` **в проверенных банках не обнаружены (C GAP)**. Такие механизмы нужны для строгого ответа на уточняющие вопросы, не для имитации найденных источников.

**Историческая G1 фактическая проверка:** OpenJDK `21.0.12.1`, Debian 13, `ZoneRulesProvider.getVersions("Europe/Berlin").lastKey()` вернул строку `2026b`, полный `DateTimeProbe.java` с фиксированным Instant/Locale/Zone, stdout **38 строк**, SHA-256 `71a4bb9934201ec65ba8e7d89658dd19a9b815d40a6c2218b6dd125d61b4d908`; вместе с NumbersProbe **71 stdout строк из двух JVM, а не 71 JUnit-тест**. Исходник и full stdout в [принятом G1 §5.2](../../docs/plans/CORE-NUMBERS-DATETIME.md). Это **локальное историческое evidence G1**. В авторскую ветку интегрирована отдельная реализация роли 03: [DateTimeProbe.java](../../examples/core/numbers-datetime/src/DateTimeProbe.java), [38-строчный эталон](../../examples/core/numbers-datetime/datetime.expected.stdout), [README](../../examples/core/numbers-datetime/README.md). [CI технического HEAD `d4dc693c67ac688682d39ab45dea8ed72ec9af85`](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45140250500) успешно завершился с двумя JVM и проверкой полных эталонов. **Отдельно подтверждён интегрированный author CI:** [Actions #45150568904](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45150568904) completed/SUCCESS на exact HEAD `4410bf7b374dbce6edafbb5d07328405125b3f3c`; ранее названный технический [#45140250500](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45140250500) относится только к `d4dc693c67ac688682d39ab45dea8ed72ec9af85`. Интегрированный workflow включает штатный Gradle/JUnit и позитивный Java 21 runner: **1 clean javac, 2 JVM, 2 полных diff**, SHA-256 для 33+38=71 stdout-строк (не JUnit-тестов). **8 isolated compiler-negative в штатном CI не исполняются**; все inline snippets, иные JDK/OS/tzdb, будущие правила DST, P2 и GUI/HTTP/anchors остаются NOT RUN. Эти CI относятся только к указанным историческим SHA.

**Исторические NOT RUN первоначального интегрированного G2 (не описание всех последующих проверок):** все короткие embedded snippets отдельно как compilation units; восемь compiler-negative в штатном CI; все иные JDK/vendor/OS/tzdb, будущие переходы и исторические timezone rule changes, нетипичные midnight-gaps, real parser ResolverStyle tests, полноценные Locale/CLDR/chronology, распределённые часы/NTP/leap seconds/monotonic deadline tests, JDBC/DB TIMESTAMP/ORM, scheduler timing, все P2, IDE/Obsidian GUI, HTTP status/anchors. **Не** обещаем воспроизводимость UTC/DST примера на бесконечно будущих версиях tzdb: фиксированы конкретный год, зона и реально наблюдавшаяся версия базы. В D-A22b отдельно проверены выбранные случаи midnight gap и ResolverStyle; их результаты описаны ниже, исторический объём G2 не меняется.

**Отдельная фактическая проверка D-A22b (02).** Временный `Probe.java` на OpenJDK 21.0.12.1, Debian 13, скомпилирован командой `javac --release 21 -encoding UTF-8 -Xlint:all -Werror -XDrawDiagnostics` и запущен в отдельной JVM. Компиляция и запуск завершились с exit 0; stdout и stderr компилятора, а также stderr JVM пусты. JVM выдала 35 строк stdout (17 меток ожидаемых runtime-исключений с точным совпадением класса и 18 других строк наблюдений/меток), SHA-256 `62d15b9ed766f9cf92bcaa1e855e626a3c8eb6076412a949631f420e3563e025`. Это ограниченные новые эксперименты §§1–8, не новые tracked tests и не доказательство исполнения всех учебных фрагментов. Полный исходник, команды, наблюдения и версии — [фактическое приложение 02](https://github.com/Melchiorius/backend-interview-labs/issues/372#issuecomment-7238801187).

В проверке 02 общий runner без изменений выполнен на пяти восстановленных из репозитория входных файлах (не полный клон): один исходный запуск без инструментирования и три запуска с временным перехватчиком diff (копирование только фактического stdout программ до очистки, затем реальный diff). Во всех трёх инструментированных запусках захваченные 33/38 строк совпали с историческими эталонами побайтно; все четыре запуска завершились с exit 0, пустым внешним stderr и только итоговым маркером во внешнем stdout. Проверены исходное окружение, LC_ALL=C/TZ=Pacific/Honolulu и LC_ALL=C.utf8/TZ=Asia/Tokyo; в отдельном Probe при тех же окружениях наблюдались en_US/Etc/UTC/UTF-8, en_US/Pacific/Honolulu/UTF-8, en/Asia/Tokyo/UTF-8. Доступны только C, C.utf8, POSIX, не полноценная матрица национальных локалей; effective settings внутри runner JVM отдельно не измерялись. Исторические NOT RUN выше относятся к G2; выбранные новые проверки не снимают границ по всему tzdb/OS/JDK/Locale/chronology, реальным часам и таймерам, DB, P2, GUI/HTTP/anchors.

**Независимая фактическая проверка D-A22b (03).** Адаптированные временные `Boundary.java` и `Extra.java` на Java 21 прошли ту же строгую компиляцию и отдельные запуски JVM с exit 0; оба потока компилятора и stderr JVM пусты. Получены 38 и 5 строк stdout соответственно: 21 метка ожидаемых runtime-исключений с точным совпадением класса (18 + 3) и 22 другие строки наблюдений/меток. Дополнительно проверены переполнение длительности tick, выбор раннего offset при недопустимом предпочитаемом offset и withEarlierOffsetAtOverlap, отказ при неверном разделителе даже в LENIENT, отклонение смены даты Apia и принятие Sao_Paulo функцией проверки сохранения даты, синтетическое переполнение при измерении интервала. Новых изолированных отрицательных компиляций не было. Исходники, команды и результаты — [фактический отчёт 03](https://github.com/Melchiorius/backend-interview-labs/pull/373#issuecomment-7238928572).

Роль 03 независимо восстановила те же пять файлов (не полный клон) и выполнила три инструментированных запуска неизменённого runner в исходном окружении и двух вариантах LC_ALL/TZ, указанных выше. Её перехватчик копировал фактические stdout и внутренние stderr до очистки, затем вызывал реальный diff. Во всех трёх запусках NM 33 строки/580 байт и DT 38 строк/964 байта побайтно совпали с эталонами, внешние stderr и захваченные stderr компилятора и обеих JVM пусты; внешний stdout — итоговый маркер. Отдельного запуска без инструментирования роль 03 не заявляла. Эффективные настройки JVM измерены в отдельном Boundary при тех же окружениях, не внутри runner JVM; доступные локали C, C.utf8, POSIX не образуют полной матрицы. CI и внешний stdout неинструментированного запуска содержат маркер, не 71 строку трасс. Исторические 4 DT + 4 NM отрицательные компиляции, 24 маркера EXPECTED REJECTION полного CI и новые проверки runtime-исключений — разные свидетельства, их количества не суммируются.

[CORE-NUMBERS](numbers.md) объясняет числовое представление; [immutability](immutability.md) — общий принцип value objects. [CURRICULUM](../../docs/CURRICULUM.md) и [COVERAGE](../../docs/COVERAGE.md) содержат границы покрытия и статусы курса.

**Локализованные названия дат:** [CORE-I18N](internationalization.md) объясняет `DateTimeFormatter.withLocale`, различие языка, `ZoneId` и `Instant`, Unicode/CLDR drift. Правила DST и временная шкала остаются предметом этой статьи.

## 12. Самопроверка: какие именно часы и дата?

Сначала предскажите результат, затем сравните с объяснением. Это учебные задачи, не новые доказанные формулировки работодателей.

1. Berlin, 27 октября 2024, 02:30: чем отличаются `atZone(berlin)` и `ofLocal(local, berlin, ZoneOffset.ofHours(1))`?
2. Почему `LocalDate.parse("2023-02-29", formatter)` может завершиться успешно, если formatter создан через `DateTimeFormatter.ofPattern("uuuu-MM-dd")`, хотя дата не существует? Как отклонить её?
3. Berlin, 26 октября 2024, 12:00+02:00: одинаковы ли «завтра в 12:00» и «через 24 часа»?
4. Clock.fixed для 2024-03-30T23:30:00.987654321Z: какие даты получат LocalDate.now в UTC и Tokyo? Изменится ли Instant?

> [!note]- Ответы
> 1. Первый способ выбирает +02:00, второй +01:00; локальные поля совпадают, моменты различаются на час (§4).
> 2. Formatter, созданный через `ofPattern`, по умолчанию использует SMART и корректирует дату до 28 февраля. `LocalDate.parse("2023-02-29")` с ISO-форматом или разбор с formatter, настроенным на STRICT, отвергает её (§7).
> 3. Нет: календарный результат 27 октября 12:00+01:00 наступает через 25 часов, через 24 часа будет 11:00+01:00 (§6).
> 4. UTC — 30 марта, Tokyo — 31 марта; Instant остаётся тем же, зона задаёт локальные поля (§2).
