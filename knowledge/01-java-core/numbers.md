# CORE-NUMBERS — представление чисел, переполнение, IEEE 754 и точная десятичная арифметика Java 21

**Ситуация на собеседовании.** Суммирование `Integer.MAX_VALUE + 1` неожиданно даёт отрицательное число, `0.1 + 0.2` не равно математическим 0.3, а два `BigDecimal` с одним числовым значением могут быть не равны по `equals`. Это **три разных контракта**: арифметика фиксированной разрядности, представление двоичной плавающей точки и десятичная арифметика со *scale*. Выбор типа определяется требованиями задачи, а не обещанием, что один класс «исправляет все числа».

**Версия и доказательства.** Java SE **21**, без preview. Нормы: [JLS §4.2.2–4.2.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.2.2), [§5.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.6), [§15.17](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.17), [§15.18](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.18), [§15.21.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21.1), [BigDecimal API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html), [MathContext](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/MathContext.html), [RoundingMode](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/RoundingMode.html), [Math](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html), [BigInteger](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigInteger.html). **Воспроизведённый исторический G1**: [полный исходник `NumbersProbe.java`, 33 строки stdout, команды и SHA-256 в §5.1](../../docs/plans/CORE-NUMBERS-DATETIME.md). Принятие [G1 PR #185](https://github.com/Melchiorius/backend-interview-labs/pull/185) подтверждает исследовательский baseline. **Запускаемая проверка G2:** [NumbersProbe.java](../../examples/core/numbers-datetime/src/NumbersProbe.java), [полный stdout-эталон](../../examples/core/numbers-datetime/numbers.expected.stdout) и [README с самостоятельными командами и проверкой](../../examples/core/numbers-datetime/README.md). Один общий `verify.sh` компилирует оба исходника Java 21 и проверяет две отдельные JVM; точная проверка относится к зафиксированным результатам, а не ко всем возможным числовым случаям.

**Охват:** **NM-01–NM-22: 22 P0**, NM-23–NM-28: **6 P1**, NM-29–NM-32: **4 P2**, без пропусков. Вместе с [CORE-DATETIME](datetime.md) исследовательская матрица **61 ID: 42 P0 / 11 P1 / 8 P2**. Смежное чтение: [типы](types.md), [преобразования](conversions.md), [операторы](operators.md), [равенство](equality.md), [исключения](exceptions.md), [immutability](immutability.md). Эта статья добавляет **числовую модель и последствия выбора**, а не повторяет их целиком.

## 1. Фиксированная ширина: обычное переполнение не исключение (NM-01–NM-03, NM-21)

`int` — 32-битное знаковое целое, `long` — 64-битное. Их арифметика не означает произвольную математическую точность. Для обычных `+`/`*` результат при переполнении по определённым JLS правилам теряет старшие биты: после максимального `int` снова получается минимальный. Это **нормативное поведение**, а не произвольный сбой одного процессора.

~~~java
int max = Integer.MAX_VALUE;
System.out.println(max + 1);                   // -2147483648
try {
    Math.addExact(max, 1);
} catch (ArithmeticException e) {
    System.out.println("ArithmeticException");  // выполнится
}
try {
    Math.multiplyExact(Long.MAX_VALUE, 2L);
} catch (ArithmeticException e) {
    System.out.println("ArithmeticException");  // выполнится
}
~~~

В G1 это строки `NM01=-2147483648`, `NM02=ArithmeticException:addExact` и `NM03=ArithmeticException:multiplyExact`. Ключевая разница: **`Math.addExact` и `multiplyExact` обнаруживают переполнение и бросают *runtime* `ArithmeticException`**; обычные операторы его не бросают. Это *не* compile-time failure. Наличие `Math.addExact` не делает другие операции автоматически проверяемыми. Для [границ integer promotion](conversions.md) и [составного присваивания](operators.md) действуют дополнительные правила типов.

**Почему тип переменной результата не спасает от переполнения.** Умножение двух int выполняется как int, только затем результат преобразуется к long. Чтобы изменить тип вычисления, нужно расширить хотя бы один операнд до операции.

~~~java
int count = 1_000_000, price = 3_000;
long bad = count * price;          // -1294967296
long alsoBad = (long) (count * price);
long good = (long) count * price;  // 3000000000
long checked = Math.multiplyExact((long) count, price);
~~~

Последнее выражение выбирает перегрузку с результатом long; Math.multiplyExact(count, price) вместо этого проверяет диапазон int и выбрасывает ArithmeticException. Даже long переполняется: Long.MAX_VALUE + 1 == Long.MIN_VALUE. Math.addExact(Long.MAX_VALUE - 1, 1) возвращает Long.MAX_VALUE, но Math.addExact(Long.MAX_VALUE, 1) и Math.toIntExact(3000000000L) отклоняют выход за диапазон. Аналогично, Math.abs(Integer.MIN_VALUE) и Math.abs(Long.MIN_VALUE) возвращают сами отрицательные минимумы: положительного аналога в соответствующем типе нет. Math.absExact для этих аргументов выбрасывает ArithmeticException. Преобразование должно происходить до потери информации; даже расширение одного операнда не делает произвольное выражение безопасным, если промежуточные операции уже переполнились. См. [преобразования](conversions.md) и [операторы](operators.md).

Если размер числа должен расти, используйте `BigInteger` **до** переполняющейся операции:

~~~java
BigInteger larger = BigInteger.valueOf(Long.MAX_VALUE)
        .add(BigInteger.ONE);
System.out.println(larger); // 9223372036854775808
~~~

G1 `NM30=9223372036854775808`. Передать в `BigInteger.valueOf` уже переполненное значение `long` — **не восстановить** потерянные биты. Произвольная точность не означает неограниченные память, время или отсутствие исключений других методов.

Для выхода из диапазона long можно также выполнить BigInteger.valueOf(count).multiply(BigInteger.valueOf(price)) до умножения. По [контракту BigInteger](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigInteger.html) поддерживается как минимум открытый диапазон от −2 в степени Integer.MAX_VALUE до +2 в той же степени; реализация вправе иметь больший диапазон. Выход за поддерживаемый диапазон приводит к ArithmeticException, а доступная память ограничивает практически достижимый размер. Обратное преобразование не обязано быть точным: для larger из примера longValue() возвращает Long.MIN_VALUE (младшие 64 бита), а longValueExact() выбрасывает ArithmeticException. Для int действуют аналогичные различия intValue()/intValueExact().

**Граница компилятора:** `int x = 2147483648;` — слишком большой *decimal int literal*: отдельная G1 проверка `N_N03` дала `compiler.err.int.number.too.large`. А `int x = Integer.MAX_VALUE + 1;` **компилируется**: `Integer.MAX_VALUE + 1` — допустимое константное выражение, значение которого после переполнения равно `Integer.MIN_VALUE`. Компилятор проверяет синтаксис и типы, а не обещает детектировать обычное переполнение.

## 2. Promotion, деление и ноль — тип результата решается до вычисления (NM-04–NM-06)

При `byte + int` работает **binary numeric promotion**, результат имеет тип `int`; не путайте с boxing — автоматическим преобразованием примитива в объект при соответствующем контексте.

~~~java
byte small = 67;
int promoted = small + 1;
Object boxed = small + 1;
System.out.println(boxed.getClass().getSimpleName()); // Integer

System.out.println(5 / 2);      // 2
System.out.println(5.0 / 2);    // 2.5
~~~

G1 `NM06=Integer` и `NM04=2:2.5`. Деление целых **усекает к нулю**, не округляет по HALF_UP или floor: например, `-5 / 2 == -2`. Замена только литерала `5` на `5.0` меняет тип вычисления и допустимые результаты.

При целочисленном делении на ноль выбрасывается `ArithmeticException` во время исполнения; при `double` деление ненулевого конечного на ±0.0 приводит к ±Infinity, а 0.0/0.0 — к NaN. В G1 `NM05=ArithmeticException:divisionByZero` и `NM09=true:Infinity`. Это **не** один и тот же механизм. При делении `Integer.MIN_VALUE / -1` результат `Integer.MIN_VALUE` (особый случай целочисленного переполнения), а не исключение; вопрос о защитном расчёте — задача выбора API/валидации.

Для ненулевого целого делителя выполняется (a / b) * b + a % b == a в арифметике соответствующего Java-типа. Остаток ненулевого значения имеет знак делимого. Math.floorDiv выбирает частное с округлением вниз, Math.floorMod согласован с ним, поэтому знак ненулевого остатка совпадает со знаком делителя.

| a, b | a / b | a % b | floorDiv(a,b) | floorMod(a,b) |
|---|---:|---:|---:|---:|
| −5, 2 | −2 | −1 | −3 | 1 |
| 5, −2 | −2 | 1 | −3 | −1 |

У особого случая MIN_VALUE / −1 результат снова MIN_VALUE, остаток 0; равенство сохраняется с учётом переполнения Java, не в неограниченной математической арифметике. Для int и long Math.divideExact (Java 18+) отклоняет именно это переполнение и нулевой делитель. Деление и остаток с целым нулевым делителем независимо выбрасывают ArithmeticException при исполнении, тогда как 5.0 / 0 даёт Infinity, 0.0 / 0 — NaN. Выбор truncation или floor зависит от контракта задачи; floor не универсальное правило денежных расчётов.

## 3. Почему double не равно десятичной арифметике (NM-07–NM-10; NM-23, NM-24, NM-27 P1)

`double` хранит двоичное приближение, поэтому конечные десятичные дроби вроде 0.1 обычно не представимы точно. После сложения двух уже округлённых значений появляется новое округление. Это объясняет G1 `NM07=0.30000000000000004`:

~~~java
double result = 0.1 + 0.2;
System.out.println(result);                  // 0.30000000000000004
System.out.println(result == 0.3);           // false
~~~

**Не выводите «сравнение double через == всегда ошибка».** `==` уместно, когда нужен именно точный IEEE-контракт; для численных приближений допустимая ошибка и масштаб должны определяться задачей. В финансовом договоре «задать epsilon» тоже не означает автоматически корректную десятичную политику.

**Откуда берётся погрешность.** Несократимая дробь имеет конечную двоичную запись тогда и только тогда, когда её знаменатель — степень двойки. Например, 0.5 = 1/2, а 0.1 = 1/10 содержит множитель 5 в знаменателе. Даже конечная двоичная запись должна помещаться в точность и диапазон формата: у нормального double 53 значащих двоичных разряда (включая неявную ведущую единицу), у float — 24. При обычном сложении и умножении результат округляется к ближайшему представимому; на середине выбирается значение с чётным младшим битом (roundTiesToEven). Так, 1.0 + 0x1.0p-53 == 1.0. У преобразований к целому и остатка свои правила; см. [JLS §15.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.4).

Наблюдение Java 21: Double.toHexString(0.1) = 0x1.999999999999ap-4; для 0.3 — 0x1.3333333333333p-2, а для суммы — 0x1.3333333333334p-2. Сумма равна Math.nextUp(0.3); разность равна Math.ulp(0.3) = 5.551115123125783E-17. ULP (unit in the last place) здесь описывает шаг между соседними представимыми значениями, а не универсальную допустимую ошибку.

Для приближённого сравнения измерений сначала определите допустимую ошибку. Абсолютный порог `absTol` особенно полезен около нуля, относительный `relTol` — при сравнении величин большого масштаба. Оба порога должны быть конечными и неотрицательными; их выбирают с учётом единиц и масштаба задачи.

1. Если NaN и бесконечности означают ошибку входных данных, сначала отвергните любой аргумент, для которого `Double.isFinite` возвращает `false`, и только потом проверяйте `a == b`. Если договор явно допускает одинаковые бесконечности, можно сначала принять равные по `==` значения, затем отвергнуть оставшиеся нечисловые и бесконечные входы. NaN при этом не принимается.
2. Для конечных аргументов достаточно выполнения одного из условий: `Math.abs(a - b) <= absTol` либо, при `m = Math.max(Math.abs(a), Math.abs(b))` и `m != 0`, `Math.abs(a / m - b / m) <= relTol`.

Вычитание `a - b` может переполниться до бесконечности, а нормирование тоже округляется. Это приближённый критерий для ограниченной задачи, а не транзитивное равенство ключей или доказательство строгой универсальной границы ошибки. В проверке роли 03 использовался первый вариант политики: нечисловые и бесконечные входы отвергались до проверки равенства.

NaN нарушает обычную интуицию математического равенства: `Double.NaN == Double.NaN` — `false`, а `Double.isNaN` возвращает `true`. В G1 `NM08=true:false`. Примитивные сравнения `<`, `>` с NaN также ложны; `!=` с NaN истинно. `Double.equals` и `Double.compare` как API **не совпадают по контракту** с примитивным `==` для всех значений: для NaN используются специальные правила, а знаковый ноль различается. Не переносите отсюда неоговорённые правила для [HashMap или record equality](records.md).

Положительная бесконечность `Double.POSITIVE_INFINITY` больше любого конечного `double`, а `Double.isInfinite(67.0 / 0.0)` возвращает `true` (G1 NM09/NM12). Это корректные IEEE-состояния, но не признак успешного бизнес-расчёта.

Два нуля имеют одинаковый результат при примитивном равенстве, но не одинаковые биты:

~~~java
System.out.println(0.0 == -0.0); // true
System.out.println(Double.compare(-0.0, 0.0)); // -1
System.out.println(Double.doubleToRawLongBits(-0.0)
        == Double.doubleToRawLongBits(0.0));   // false
~~~

G1 `NM10=true:-1`, `NM11=false`. Знак нуля может влиять на деление на него: `1.0 / -0.0` даёт `-Infinity`.

Уточним три отношения, которые полезно не смешивать. Для примитивных float/double действует численное сравнение, у объектов Float/Double — контракт [Float](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Float.html) / [Double](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Double.html):

| Пара значений одного типа | Примитивное == | equals обёрток | compare(левое, правое) |
|---|---|---|---|
| NaN, NaN (включая разные payload) | false | true | 0 |
| −0.0, +0.0 | true | false | отрицательный |
| NaN, +Infinity | false | false | положительный |
| 0.5, 0.5 | true | true | 0 |

**Битовое представление NaN.** Канонические методы `Double.doubleToLongBits` и `Float.floatToIntBits` сводят NaN к единому битовому представлению, а raw-варианты позволяют наблюдать разные представления. В отдельном эксперименте два quiet NaN с double-битами `0x7ff8000000000001L` и `0x7ff8000000000002L` (для float — `0x7fc00001` и `0x7fc00002`) сохранили различие raw-битов, но совпали по `equals`, `hashCode` и `compare`. Не утверждается сохранение всех signalling NaN на любом оборудовании.

**Объектное равенство и порядок.** Равные по `equals` обёртки имеют одинаковый `hashCode`. В естественном порядке обёрток `-0.0` предшествует `+0.0`, NaN следует за положительной бесконечностью; этот порядок согласован с `equals`. При этом `HashSet` использует объектное равенство и хэш, а `TreeSet` — порядок. Произвольный `Comparator` может изменить отношение эквивалентности в `TreeSet`.

**Равенство record.** В [record с примитивным компонентом](records.md) генерируемый `equals` соответствует нулевому результату `compare` соответствующей обёртки, а не примитивному `==` или сравнению raw-битов. Это контракт, а не требование фактического boxing или конкретного алгоритма хэширования. См. [контракты равенства](equality.md).

**Строгая арифметика и библиотечные функции.** Начиная с Java 17 все floating-point выражения вычисляются строго, поэтому strictfp в Java 21 избыточен ([JLS §15.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.4)). В D-A22a отдельный исходник strictfp class Warning {} с javac --release 21 -encoding UTF-8 -Xlint:all -XDrawDiagnostics дал exit 0 и compiler.warn.strictfp (без -Werror). Это предупреждение, не ошибка компиляции. Строгость операторов не означает, что любые функции [Math](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html) и [StrictMath](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/StrictMath.html) битово одинаковы: для некоторых функций StrictMath задаёт семантику fdlibm, а Math допускает реализации в пределах описанных для метода гарантий точности. Проверяйте контракт конкретного метода.

**FMA: одна операция округления вместо двух.** Math.fma и StrictMath.fma для конечных аргументов складывают точное произведение с третьим аргументом и округляют итог один раз к ближайшему с ties-to-even. Для x = 1 + 0x1.0p-27, y = 1 - 0x1.0p-27 точное произведение равно 1 − 2⁻⁵⁴. При обычном x*y-1 произведение округляется до 1 (середина между 1 и предшествующим double), вычитание даёт 0. Оба вызова fma(x,y,-1) сохраняют −2⁻⁵⁴. Это проверено в отдельном Java 21 Probe. Исторический G1 NM33=7.0 проверяет лишь простой случай.

Особые случаи по API: любой NaN, бесконечность, умноженная на ноль, или бесконечное точное произведение и противоположная бесконечность в третьем аргументе дают NaN; конечное точное произведение с бесконечным третьим аргументом даёт эту бесконечность. Для знака нулевого результата действуют правила сложения точного произведения и третьего аргумента (например, fma(-0.0,+0.0,-0.0) даёт -0.0). Отдельная проверка охватывает два NaN-контрпримера и cancellation, не все особые значения. Это объяснение результата, не утверждение о скорости, JIT или полном анализе погрешности.

## 4. BigDecimal: важен способ создания (NM-11–NM-13)

`BigDecimal` опирается на **целочисленное unscaled value и десятичный scale**. Он не знает, «какая десятичная строка была задумана», если ему передали уже аппроксимированное значение `double`.

~~~java
BigDecimal text = new BigDecimal("0.1");
BigDecimal binary = new BigDecimal(0.1);
BigDecimal viaValueOf = BigDecimal.valueOf(0.1);
System.out.println(text);       // 0.1
System.out.println(binary);     // 0.1000000000000000055511151231257827021181583404541015625
System.out.println(viaValueOf); // 0.1
~~~

Таков точный G1 вывод `NM13`/`NM14`/`NM15`. Конструктор `new BigDecimal(double)` **точно сохраняет уже заданное двоичное значение**, отчего вывод длинный. `BigDecimal.valueOf(double)` использует каноническое десятичное представление `Double.toString(double)`, дающее здесь 0.1. `new BigDecimal("0.1")` интерпретирует **исходный десятичный текст**. Отсюда G1 `NM16=true:false`: `text.equals(viaValueOf)` истинен, `text.equals(binary)` ложен.

Новый пример: BigDecimal.valueOf(0.1 + 0.2) даёт 0.30000000000000004, а не 0.3. valueOf получает уже вычисленный double и не знает первоначальных десятичных операндов. Оба способа конвертации double отклоняют NaN и обе бесконечности с NumberFormatException; у конечного −0.0 знак теряется (signum() == 0), хотя scale двух способов может различаться. Если значение пришло в виде строки или целых единиц, BigDecimal(String) либо BigDecimal.valueOf(long, scale) позволяют не вводить промежуточное двоичное приближение.

Рекомендация для точных десятичных входов: хранить и читать десятичный текст (или явные целые минимальные единицы с описанной шкалой), **не пропускать его незаметно через `double`**. При этом даже `BigDecimal` с точным входом может требовать округления при операции, результат которой имеет бесконечное десятичное разложение (см. §6).

## 5. Scale, precision, сравнение и коллекции (NM-14–NM-17; NM-25 P1)

Для `BigDecimal` верно соотношение

\[
\text{value} = \text{unscaledValue}\cdot 10^{-\text{scale}}.
\]

`new BigDecimal("2.0")` имеет *unscaled* 20, *scale* 1, *precision* 2; `new BigDecimal("2.00")` имеет unscaled 200, scale 2, precision 3. Scale — знаковый 32-битный показатель в приведённой формуле: отрицательный scale означает умножение на степень десяти, а не отрицательное число цифр после точки. Precision — число цифр абсолютного unscaled-значения; для нуля оно равно 1 независимо от scale. Например, у `new BigDecimal("0.000")` scale равен 3, unscaled value — 0, precision — 1: нули после точки не делают precision равным 3. Это не обязательно количество значащих цифр измерения в предметной области. В G1 NM17=1:2 — оба scale; NM18=2:3 — обе precision; NM21=20 — a.unscaledValue().

~~~java
BigDecimal a = new BigDecimal("2.0");
BigDecimal b = new BigDecimal("2.00");
System.out.println(a.equals(b));        // false
System.out.println(a.compareTo(b));     // 0
System.out.println(new HashSet<>(List.of(a,b)).size()); // 2
System.out.println(new TreeSet<>(List.of(a,b)).size()); // 1
~~~

`equals` у `BigDecimal` учитывает и значение, и *scale*, `compareTo` сравнивает числовое значение (G1 `NM19=false:0`). Поэтому `HashSet` ориентируется на `equals/hashCode`, а `TreeSet` использует сравнение упорядочения: в G1 `NM20=2:1`. Это не доказательство поломки коллекций — это два **разных контракта эквивалентности**. [Общие гарантии equals/hashCode](equality.md) и [генерируемое равенство record](records.md) рассматриваются отдельно.

Поэтому BigDecimal нельзя подставлять в любую арифметическую операцию только на основании compareTo == 0. В перегрузке divide(divisor, RoundingMode) результат имеет scale делимого: a.divide(new BigDecimal("3"), RoundingMode.HALF_UP) даёт 0.7, b — 0.67. Это различие представления влияет на последующее округление. У TreeSet с естественным порядком BigDecimal поведение определено, но из-за несогласованности compareTo с equals такой набор не удовлетворяет общему контракту Set, сформулированному через equals (см. [TreeSet API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html)). Выбор ключа и нормализации должен отражать доменную эквивалентность.

`stripTrailingZeros` удаляет десятичные нули **с изменением представления**. Для `new BigDecimal("1.00").stripTrailingZeros().toPlainString()` получено `NM22=1`. При `1000` результат может получить **отрицательный scale** и строковое научное представление `1E+3`, а `toPlainString` печатает `1000`. Нормализация текста, сравнение ключей и денежная шкала — это разные решения; нельзя считать, что `stripTrailingZeros` автоматически реализует бизнес-policy.

Для z = new BigDecimal("1000").stripTrailingZeros() получаем z.toString() = "1E+3", z.scale() = −3, z.toPlainString() = "1000". Каноническая строка toString() позволяет через BigDecimal(String) восстановить и числовое значение, и scale: new BigDecimal(z.toString()).equals(z) — true. Для toPlainString() обратное преобразование может потерять отрицательный scale: compareTo(z) == 0, но equals(z) — false. У нуля stripTrailingZeros() возвращает BigDecimal.ZERO со scale 0. Выбор канонического представления для восстановления значения и форматирование для показа человеку — отдельные задачи; о локализованном выводе см. [CORE-I18N](internationalization.md).

## 6. Divide и точность: ограничение результата должно быть явным (NM-18–NM-20; NM-26 P1)

Даже если оба `BigDecimal` точны, результат деления может иметь бесконечную десятичную запись. `BigDecimal.ONE.divide(new BigDecimal("3"))` без заданного округления бросает *runtime* `ArithmeticException` (G1 `NM23=ArithmeticException:nonterminating`). Напротив, `new BigDecimal("1.0").divide(new BigDecimal("8"))` даёт точное `0.125` (NM32). Это не compiler-negative.

Почему 1/8 заканчивается, а 1/3 нет? После сокращения рациональной дроби её десятичная запись конечна тогда и только тогда, когда знаменатель содержит только простые множители 2 и 5 (включая знаменатель 1). Это математический критерий; в BigDecimal дополнительно действуют ограничения диапазона scale и ресурсов. При точном divide предпочтительный scale равен scale делимого минус scale делителя; при необходимости точного результата он может увеличиться. Например, new BigDecimal("2.00").divide(new BigDecimal("2")) даёт 1.00, но 1/8 требует scale 3. MathContext.UNLIMITED (precision 0) не разрешает округление бесконечной дроби, так что 1/3 всё равно бросает ArithmeticException. Деление на ноль — отдельная причина ArithmeticException, даже если задан режим округления. Не диагностируйте все такие исключения как бесконечную дробь.

~~~java
BigDecimal one = BigDecimal.ONE;
BigDecimal three = new BigDecimal("3");

System.out.println(one.divide(three, 2, RoundingMode.HALF_EVEN)); // 0.33
System.out.println(one.divide(three,
        new MathContext(4, RoundingMode.HALF_UP)));                // 0.3333
~~~

В первом случае **scale=2** означает две цифры после точки, во втором **precision=4** ограничивает количество значащих цифр результата четырьмя, не обязательно четырьмя знаками после точки. G1 `NM24=0.33`, `NM25=0.3333`. `setScale` задаёт новый scale существующего числа, `MathContext` ограничивает точность арифметических операций; это не две формы одной настройки.

Режимы округления нельзя перепутать по одному примеру:

| Вход и операция | HALF_EVEN | HALF_UP |
|---|---|---|
| `2.5.setScale(0, …)` | 2 (чётная последняя цифра) | 3 |
| `3.5.setScale(0, …)` | 4 | 4 |

Это результаты NM26–28. `RoundingMode.HALF_EVEN` разрешает half tie в пользу чётной соседней цифры, `HALF_UP` в сторону от нуля. `RoundingMode.UNNECESSARY` не округляет «по умолчанию»: если без округления невозможно, бросает `ArithmeticException`. Пример `new BigDecimal("2.55").setScale(1, UNNECESSARY)` даёт G1 `NM29=ArithmeticException:roundingRequired`.

Предпочтительные scale точных операций: для сложения — максимум scale операндов, для умножения — сумма, для деления — разность (с возможной корректировкой для точного частного). Например, 2.0 + 2.00 даёт scale 2, их произведение — scale 3. MathContext с precision p > 0 ограничивает число значащих цифр сверху, но не дописывает нули ради достижения p. Precision 0 означает отсутствие ограничения значащих цифр; это не обещание неограниченной памяти. Перенос при округлении может поменять scale: new BigDecimal("999.9").round(new MathContext(3)) даёт 1.00E+3 (scale −1), а setScale(3) — 999.900. BigDecimal неизменяем, поэтому присваивайте возвращаемый результат; вызов метода не меняет исходный объект.

| Значение −2.5, scale 0 | Результат |
|---|---:|
| HALF_UP | −3 |
| CEILING (к +Infinity) | −2 |
| FLOOR (к −Infinity) | −3 |

При HALF_UP точная середина округляется от нуля; UP также от нуля, DOWN — к нулю, но не только на середине. Двойное округление: 1.249 при HALF_UP сначала до scale 2 даёт 1.25, затем до scale 1 — 1.3. Если сразу округлить исходное значение до scale 1, получится 1.2. В цепочке вычислений контракт должен явно определять промежуточное округление и конечную шкалу; ни один из вариантов не следует выбирать автоматически.

**Принцип проектирования:** выбор `scale`, времени округления, единиц измерения, налоговых ставок, FX-конверсий, нулевых значений и допустимой ошибки задаёт **доменный договор**. Java API предоставляет инструменты, но не универсальную норму «для денег всегда HALF_EVEN». Для части продуктов верно хранить сумму в целых минимальных единицах; для других нужны десятичные доли этих единиц — оба варианта требуют документированного контракта, инвариантов и тестов. G1 `NM31=6700` показывает лишь `new BigDecimal("67.00").movePointRight(2)`, **не** универсальную денежную схему.

Если иллюстративный договор требует ровно целого количества сотых долей единицы и диапазон `long`, нужно проверять оба условия. У `new BigDecimal("67.009").movePointRight(2)` получается 6700.9; `longValue()` молча отбросит дробную часть (6700), `longValueExact()` выбросит `ArithmeticException`. По контракту API обычный `longValue()` при выходе целой части за диапазон `long` сохраняет только младшие 64 бита; `longValueExact()` отклоняет и ненулевую дробную часть, и выход за диапазон. Для `"67.00"` exact возвращает 6700, а для `"92233720368547758.08"` после сдвига exact отклонит выход за `Long.MAX_VALUE`. Последний пример проверял именно отказ exact; обычное преобразование числа за пределами диапазона в этих экспериментах проверялось для BigInteger, не для BigDecimal.

`movePointRight` сам по себе не проверяет, что значение целое и помещается в `long`. Если домен допускает округление, сначала применяют явно выбранный режим, затем проверяют диапазон. Это пример технического контракта, не правило расчёта для любой валюты.

## 7. Compiler-negative ≠ runtime failure и пределы экспериментального доказательства (NM-28 P1)

Принятый G1 отдельно проверил **четыре** изолированных `javac --release 21 -XDrawDiagnostics` ошибки:

| G1 case | Что отвергает компилятор | Ожидаемая тематическая диагностика |
|---|---|---|
| `N_N01_DoubleAsBigDecimal` | Прямое присвоение `0.1` переменной `BigDecimal` | `compiler.err.prob.found.req` |
| `N_N02_ExactDouble` | Несуществующая перегрузка `Math.addExact(1.0, 2.0)` | `compiler.err.cant.apply.symbols` |
| `N_N03_OverflowConstant` | Decimal int literal `2147483648` | `compiler.err.int.number.too.large` |
| `N_N04_Narrowing` | `short x = 32768;` вне диапазона | `compiler.err.prob.found.req` |

**Исторический результат G1/04:** 4/4 диагностических причины подтверждены в отдельных компиляциях, а не одним запуском с четырьмя ошибками. **Их не проверяет автоматически** текущий авторский Markdown. Полные исходники этих случаев и точные команды — в [принятом G1 §6](../../docs/plans/CORE-NUMBERS-DATETIME.md). Ни overflow у оператора с корректным литералом, ни `divide(1/3)`, ни `Math.addExact` при переполнении **не** являются «отрицательными компиляциями». С ними должна справляться runtime-обработка или дизайн данных, а не `javac`.

По [JLS §15.29](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.29) константные выражения примитивного типа или `String` состоят из разрешённых операций и константных переменных; обычное переполнение при их вычислении допустимо.

- `int folded = Integer.MAX_VALUE + 67;` — константное выражение, вычисляемое на этапе компиляции; результат равен −2147483582.
- `int v = 67; int r = v + Integer.MAX_VALUE;` — неконстантное выражение, вычисляемое по семантике языка при исполнении, с тем же результатом −2147483582. Оба значения получены в ранее выполненной проверке роли 03.

`final int c = 67; byte b = c;` компилируется, потому что `c` — константная переменная и значение входит в `byte`. `int v = 67; byte b = v;` не компилируется: обычная переменная не даёт такого разрешения на сужение (явный cast имеет другую семантику). Десятичный литерал `2147483648` разрешён только непосредственным операндом унарного минуса, поэтому `int min = -2147483648;` корректно; аналогично `long min = -9223372036854775808L;` (см. [JLS §3.10.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.1), [§5.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2)). Это правила языка, а не обещание конкретного байткода или оптимизации JIT.

В отдельной проверке D-A22a корректный `Probe` с этими границами компилировался и выполнялся. Два изолированных исходника `class BadLiteral { int x = 2147483648; }` и `class BadNarrow { int v=67; byte b=v; }` проверены раздельно командой `javac --release 21 -encoding UTF-8 -Xlint:all -Werror -XDrawDiagnostics`: exit 1, пустой stdout, соответственно `compiler.err.int.number.too.large` и `compiler.err.prob.found.req` с причиной `compiler.misc.possible.loss.of.precision: int, byte`. Это отдельные проверки роли 02, не повтор всех четырёх исторических NM или восьми NM/DT; результаты дополнительных проверок роли 03 приведены в §9.

## 8. Адресная матрица CORE-NUMBERS

| ID | P | Где раскрыто / ключевой trace |
|---|---|---|
| NM-01 | P0 | §1: ordinary int/long overflow, NM01 |
| NM-02 | P0 | §1: addExact throws runtime, NM02 |
| NM-03 | P0 | §1: multiplyExact long, NM03 |
| NM-04 | P0 | §2: integer vs double division, NM04 |
| NM-05 | P0 | §2: integer /0 vs IEEE /0, NM05/NM09 |
| NM-06 | P0 | §2: promotion of byte+int, NM06 |
| NM-07 | P0 | §3: binary double sum, NM07 |
| NM-08 | P0 | §3: NaN and isnan, NM08 |
| NM-09 | P0 | §3: infinity, NM09/NM12 |
| NM-10 | P0 | §3: signed zero, NM10/NM11 |
| NM-11 | P0 | §4: BigDecimal(String), NM13 |
| NM-12 | P0 | §4: BigDecimal(double), NM14 |
| NM-13 | P0 | §4: BigDecimal.valueOf, NM15/NM16 |
| NM-14 | P0 | §5: scale/precision/unscaled, NM17/18/21 |
| NM-15 | P0 | §5: equals vs compareTo, NM19 |
| NM-16 | P0 | §5: Set contracts, NM20 |
| NM-17 | P0 | §5: stripTrailingZeros, NM22 |
| NM-18 | P0 | §6: exact divide failing and terminating, NM23/NM32 |
| NM-19 | P0 | §6: divide scale vs MathContext, NM24/25 |
| NM-20 | P0 | §6: HALF_EVEN/HALF_UP/UNNECESSARY, NM26–29 |
| NM-21 | P0 | §1: BigInteger arbitrary precision, NM30 |
| NM-22 | P0 | §6: decimal units/domain policy, NM31 |
| NM-23 | P1 | §3: strictfp Java17+; отдельное предупреждение Java21 |
| NM-24 | P1 | §3: FMA cancellation, NM33; performance NOT RUN |
| NM-25 | P1 | §5: toString/toPlainString, negative scale |
| NM-26 | P1 | §6: MathContext vs setScale and repeated rounding |
| NM-27 | P1 | §3: Float/Double equals, compare, NaN payload, signed zero и record |
| NM-28 | P1 | §1/7: constant overflow literal vs runtime/narrowing |
| NM-29 | P2 | §6/9: money/FX/tax/DB DECIMAL NOT RUN |
| NM-30 | P2 | §3/9: subnormals/JIT/error bounds NOT RUN |
| NM-31 | P2 | §9: vector/crypto/performance NOT RUN |
| NM-32 | P2 | §9: localized NumberFormat/CORE-I18N NOT RUN |

## 9. Sources A, реальные вопросы C и честные NOT RUN

**Нормативные A:** [JLS21 §5.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.6), [JLS21 §15.17](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.17), [JLS21 §4.2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.2.3), [JLS21 §15.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.4), [Math API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html), [BigDecimal API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html), [BigInteger API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigInteger.html), [RoundingMode API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/RoundingMode.html). Это **гарантии языка/API**, не универсальные бизнес-правила.

**Реальные C-формулировки:** [Q-HRUDAY, вопросы о integer overflow и floating-point equality](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md); [sudhir-tech, вопросы 149/150 BigDecimal и строкового конструктора](https://github.com/sudhir-tech/java-backend-interview-prep/blob/main/01-core-java/core-java-interview-questions.md). Эти источники подтверждают, что спрашивают *широкие темы*, но не статистическую частоту и не Java-норму. Для отдельных вопросов про `BigDecimal.divide(1/3)`, scale/rounding policy, `Double.doubleToRawLongBits` и `MathContext` **самостоятельные точные C-вопросы в проверенных банках отсутствуют (GAP)**; их разбираем как техническое продолжение.

**Наблюдение G1:** в OpenJDK21.0.12.1 Debian13 выполнены полные `NumbersProbe.java` и отдельные компиляционно-отрицательные кейсы; stdout **33 строки**, SHA-256 `9c9e8000292ee4face04197388feadb50ba8cdc1e7a9099c3b89ecbdd4b6ea83`. **33 строки — не 33 теста**. Это историческое подтверждение [G1 §5.1](../../docs/plans/CORE-NUMBERS-DATETIME.md). **Дополнительно интегрирован runnable G2:** [исходник](../../examples/core/numbers-datetime/src/NumbersProbe.java), [эталон](../../examples/core/numbers-datetime/numbers.expected.stdout), [README](../../examples/core/numbers-datetime/README.md). На техническом HEAD `d4dc693c67ac688682d39ab45dea8ed72ec9af85` [GitHub Actions #45140250500](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45140250500) успешно выполнил два Java 21 процесса и строгую проверку эталонов. **Отдельно подтверждён интегрированный author CI:** [Actions #45150568904](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45150568904) completed/SUCCESS на exact HEAD `4410bf7b374dbce6edafbb5d07328405125b3f3c`; технический [#45140250500](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45140250500) completed/SUCCESS только на `d4dc693c67ac688682d39ab45dea8ed72ec9af85`. Интегрированный workflow включает штатный Gradle/JUnit и отдельный позитивный runner: **1 clean javac, 2 JVM, 2 полных diff** и SHA-256 двух эталонов (33+38=71 строк stdout, не JUnit-тестов). **8 изолированных compiler-negative не входят в штатный CI**; все embedded snippets, иные JDK/OS/tzdb, финансовые политики, P2 и GUI/HTTP/anchors остаются NOT RUN. При любом последующем изменении HEAD необходим новый независимый CI на изменённом SHA.

**Исторические ограничения G1/G2 и отдельные проверки D-A22a.** G1 не запускал проверку предупреждения `strictfp`. Исторические 8 отрицательных компиляций (4 NM + 4 DT) не входят в общий runner; известная описка о количестве в G1 §5 уже пояснена в [README](../../examples/core/numbers-datetime/README.md). Последующие эксперименты не изменяют историческое свидетельство.

**Авторская проверка 02.** На OpenJDK 21.0.12.1, Debian 13, основной временный `Probe` с `javac --release 21 -encoding UTF-8 -Xlint:all -Werror -XDrawDiagnostics` и `java -cp classes Probe` завершился с exit 0 и пустым stderr; 21 строка stdout (20 подписанных проверок и одна строка числовых наблюдений), SHA-256 `cec34eedd9a36c54fb96a46c77f8b9cef6eef63342a7beb5c0461cdc05ed40f9`. Проверены 20 ожидаемых исключений с точным совпадением класса. Отдельно, не в основном `Probe`, скомпилированы `Warning.java` (exit 0, предупреждение `strictfp`, без `-Werror`) и два изолированных некорректных исходника (exit 1). Три запуска неизменённого runner на пяти восстановленных из репозитория входных файлах (не полный клон) прошли без инструментирования; внешний stdout содержит только итоговый маркер, а не 71 строку трасс. См. [исходники, команды и фактические результаты 02](https://github.com/Melchiorius/backend-interview-labs/issues/369#issuecomment-7237820464).

**Независимая практическая проверка 03.** Отдельные адаптированные `Check` и `Extra` прошли строгую компиляцию Java 21 и запуск JVM с exit 0, stdout — 35 и 3 строки соответственно; проверены 25 ожидаемых исключений с точным совпадением класса. Отдельно получены предупреждение `strictfp` (exit 0) и три отказа компиляции (exit 1). В `Extra` проверены выбранные конечные входы и отказ для нечисловых и бесконечных входов, `StrictMath.fma` и отрицательный ноль. Три запуска общего runner на восстановленных пяти файлах выполнены с временным перехватчиком `diff`, который копировал фактические трассы до очистки и затем вызывал настоящий `diff`; сравнения с эталонами успешны. В обеих проверках доступны только локали C, C.utf8, POSIX; использованы доступные варианты окружения, фактические locale/charset JVM и версия tzdb не измерялись. См. [исходники и фактические результаты 03](https://github.com/Melchiorius/backend-interview-labs/pull/370#issuecomment-7238038306). Это ограниченные временные эксперименты, не новые тесты в репозитории и не проверка всех фрагментов статьи; их отказы компиляции не суммируются с историческими 8 или 24 отказами в общем CI.

**Остаются NOT RUN:** все embedded snippets как самостоятельные compilation units, все альтернативные JDK/vendors/OS и hardware, реальная политика денежных единиц/округления/налогов/FX, JDBC/DB DECIMAL, BigDecimal performance/benchmark, полный перебор NaN payload/subnormals и строгие границы ошибки, floating JIT/StrictMath benchmarking, vector/cryptographic constant time, CORE-I18N/NumberFormat, GUI Obsidian, внешние HTTP/anchors всех ссылок. Не приписывать P2 статус Implemented.

[CORE-DATETIME](datetime.md) — отдельная задача о *временной шкале и правилах часового пояса*. Она тоже показывает границы представления данных, но числовое округление и DST — разные механизмы. Состояние опубликованного материала и границы проверки отражены в [CURRICULUM](../../docs/CURRICULUM.md) и [COVERAGE](../../docs/COVERAGE.md).

**Локализованное представление чисел:** [CORE-I18N](internationalization.md) показывает `NumberFormat`, `DecimalFormat`, `ParsePosition`, частичный разбор, округление и CLDR-зависимые разделители. Это граница UI, а не замена `BigDecimal` и правил арифметики из этой статьи.

## 10. Самопроверка: почему числа расходятся?

Сервис получает целые count = 1_000_000 и price = 3_000, а также строки десятичных сумм "2.0", "2.00", "1.249" и "67.009". Предскажите результат long total = count * price; сравните первые две суммы через equals и compareTo; объясните, какой результат даст округление "1.249" с HALF_UP до одного знака напрямую и через два знака; решите, можно ли принять "67.009" в договоре целых сотых долей в long. Сначала объясните каждый ответ без запуска, затем проверьте фрагменты из §§1, 5–6.

> [!note]- Ответ и диагностика
> 1. total = −1294967296: умножение выполнено как int, присваивание long уже не исправляет ошибку. Расширение операнда до операции даёт 3000000000; для проверки диапазона используют соответствующий exact-метод.
> 2. equals — false, compareTo — 0: представления различаются по scale, числовое значение одинаковое. Это влияет на HashSet/TreeSet и на операции, сохраняющие scale делимого.
> 3. Прямое округление даёт 1.2, промежуточное — 1.25, затем 1.3. Нужно определить момент округления, а не выбирать результат задним числом.
> 4. 67.009 × 100 = 6700.9; longValue() потеряет дробную часть, longValueExact() отклонит значение. Для договора точных сотых это отказ, если заранее не разрешено округление.
