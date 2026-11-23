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

Если размер числа должен расти, используйте `BigInteger` **до** переполняющейся операции:

~~~java
BigInteger larger = BigInteger.valueOf(Long.MAX_VALUE)
        .add(BigInteger.ONE);
System.out.println(larger); // 9223372036854775808
~~~

G1 `NM30=9223372036854775808`. Передать в `BigInteger.valueOf` уже переполненное значение `long` — **не восстановить** потерянные биты. Произвольная точность не означает неограниченные память, время или отсутствие исключений других методов.

**Граница компилятора:** `int x = 2147483648;` — слишком большой *decimal int literal*: отдельная G1 проверка `N_N03` дала `compiler.err.int.number.too.large`. А `int x = Integer.MAX_VALUE + 1;` **компилируется** и даёт runtime-значение с переполнением. Компилятор проверяет синтаксис и типы, а не обещает детектировать обычное переполнение.

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

## 3. Почему double не равно десятичной арифметике (NM-07–NM-10; NM-23, NM-24, NM-27 P1)

`double` хранит двоичное приближение, поэтому конечные десятичные дроби вроде 0.1 обычно не представимы точно. После сложения двух уже округлённых значений появляется новое округление. Это объясняет G1 `NM07=0.30000000000000004`:

~~~java
double result = 0.1 + 0.2;
System.out.println(result);                  // 0.30000000000000004
System.out.println(result == 0.3);           // false
~~~

**Не выводите «сравнение double через == всегда ошибка».** `==` уместно, когда нужен именно точный IEEE-контракт; для численных приближений допустимая ошибка и масштаб должны определяться задачей. В финансовом договоре «задать epsilon» тоже не означает автоматически корректную десятичную политику.

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

**P1 / границы:** начиная с Java 17 все floating-point выражения вычисляются в строгом режиме; устаревшие объяснения зависимости точности от старого `strictfp` нельзя выдавать за Java 21 [JLS §15.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.4). `Math.fma(a,b,c)` рассчитывает произведение с добавлением с единственным округлением на выходе в соответствующей операции; G1 `NM33=7.0` только проверяет простой случай, **не** доказывает его точность/performance относительно отдельного `a*b+c`. Различия Math/StrictMath, NaN payload, subnormals и JIT не исследовались (NM-24/27 и P2).

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

Рекомендация для точных десятичных входов: хранить и читать десятичный текст (или явные целые минимальные единицы с описанной шкалой), **не пропускать его незаметно через `double`**. При этом даже `BigDecimal` с точным входом может требовать округления при операции, результат которой имеет бесконечное десятичное разложение (см. §6).

## 5. Scale, precision, сравнение и коллекции (NM-14–NM-17; NM-25 P1)

Для `BigDecimal` верно соотношение

\[
\text{value} = \text{unscaledValue}\cdot 10^{-\text{scale}}.
\]

`new BigDecimal("2.0")` имеет *unscaled* 20, *scale* 1, *precision* 2; `new BigDecimal("2.00")` имеет unscaled 200, scale 2, precision 3. **Scale — количество десятичных позиций по степени десяти, precision — количество значащих цифр unscaled-значения**, это разные величины. G1 `NM17=1:2`, `NM18=2:3`, `NM21=20`.

~~~java
BigDecimal a = new BigDecimal("2.0");
BigDecimal b = new BigDecimal("2.00");
System.out.println(a.equals(b));        // false
System.out.println(a.compareTo(b));     // 0
System.out.println(new HashSet<>(List.of(a,b)).size()); // 2
System.out.println(new TreeSet<>(List.of(a,b)).size()); // 1
~~~

`equals` у `BigDecimal` учитывает и значение, и *scale*, `compareTo` сравнивает числовое значение (G1 `NM19=false:0`). Поэтому `HashSet` ориентируется на `equals/hashCode`, а `TreeSet` использует сравнение упорядочения: в G1 `NM20=2:1`. Это не доказательство поломки коллекций — это два **разных контракта эквивалентности**. [Общие гарантии equals/hashCode](equality.md) и [генерируемое равенство record](records.md) рассматриваются отдельно.

`stripTrailingZeros` удаляет десятичные нули **с изменением представления**. Для `new BigDecimal("1.00").stripTrailingZeros().toPlainString()` получено `NM22=1`. При `1000` результат может получить **отрицательный scale** и строковое научное представление `1E+3`, а `toPlainString` печатает `1000`. Нормализация текста, сравнение ключей и денежная шкала — это разные решения; нельзя считать, что `stripTrailingZeros` автоматически реализует бизнес-policy.

## 6. Divide и точность: ограничение результата должно быть явным (NM-18–NM-20; NM-26 P1)

Даже если оба `BigDecimal` точны, результат деления может иметь бесконечную десятичную запись. `BigDecimal.ONE.divide(new BigDecimal("3"))` без заданного округления бросает *runtime* `ArithmeticException` (G1 `NM23=ArithmeticException:nonterminating`). Напротив, `new BigDecimal("1.0").divide(new BigDecimal("8"))` даёт точное `0.125` (NM32). Это не compiler-negative.

~~~java
BigDecimal one = BigDecimal.ONE;
BigDecimal three = new BigDecimal("3");

System.out.println(one.divide(three, 2, RoundingMode.HALF_EVEN)); // 0.33
System.out.println(one.divide(three,
        new MathContext(4, RoundingMode.HALF_UP)));                // 0.3333
~~~

В первом случае **scale=2** означает две цифры после точки, во втором **precision=4** означает четыре *значащие цифры результата*, не обязательно четыре знака после точки. G1 `NM24=0.33`, `NM25=0.3333`. `setScale` задаёт новый scale существующего числа, `MathContext` ограничивает точность арифметических операций; это не две формы одной настройки. При вычислении цепочки операций важно, на каком шаге производить округление: промежуточное округление не обязано коммутировать с последующими действиями (NM-26 P1).

Режимы округления нельзя перепутать по одному примеру:

| Вход и операция | HALF_EVEN | HALF_UP |
|---|---|---|
| `2.5.setScale(0, …)` | 2 (чётная последняя цифра) | 3 |
| `3.5.setScale(0, …)` | 4 | 4 |

Это результаты NM26–28. `RoundingMode.HALF_EVEN` разрешает half tie в пользу чётной соседней цифры, `HALF_UP` в сторону от нуля. `RoundingMode.UNNECESSARY` не округляет «по умолчанию»: если без округления невозможно, бросает `ArithmeticException`. Пример `new BigDecimal("2.55").setScale(1, UNNECESSARY)` даёт G1 `NM29=ArithmeticException:roundingRequired`.

**Принцип проектирования:** выбор `scale`, времени округления, единиц измерения, налоговых ставок, FX-конверсий, нулевых значений и допустимой ошибки задаёт **доменный договор**. Java API предоставляет инструменты, но не универсальную норму «для денег всегда HALF_EVEN». Для части продуктов верно хранить сумму в целых минимальных единицах; для других нужны десятичные доли этих единиц — оба варианта требуют документированного контракта, инвариантов и тестов. G1 `NM31=6700` показывает лишь `new BigDecimal("67.00").movePointRight(2)`, **не** универсальную денежную схему.

## 7. Compiler-negative ≠ runtime failure и пределы экспериментального доказательства (NM-28 P1)

Принятый G1 отдельно проверил **четыре** изолированных `javac --release 21 -XDrawDiagnostics` ошибки:

| G1 case | Что отвергает компилятор | Ожидаемая тематическая диагностика |
|---|---|---|
| `N_N01_DoubleAsBigDecimal` | Прямое присвоение `0.1` переменной `BigDecimal` | `compiler.err.prob.found.req` |
| `N_N02_ExactDouble` | Несуществующая перегрузка `Math.addExact(1.0, 2.0)` | `compiler.err.cant.apply.symbols` |
| `N_N03_OverflowConstant` | Decimal int literal `2147483648` | `compiler.err.int.number.too.large` |
| `N_N04_Narrowing` | `short x = 32768;` вне диапазона | `compiler.err.prob.found.req` |

**Исторический результат G1/04:** 4/4 диагностических причины подтверждены в отдельных компиляциях, а не одним запуском с четырьмя ошибками. **Их не проверяет автоматически** текущий авторский Markdown. Полные исходники этих случаев и точные команды — в [принятом G1 §6](../../docs/plans/CORE-NUMBERS-DATETIME.md). Ни overflow у оператора с корректным литералом, ни `divide(1/3)`, ни `Math.addExact` при переполнении **не** являются «отрицательными компиляциями». С ними должна справляться runtime-обработка или дизайн данных, а не `javac`.

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
| NM-23 | P1 | §3: strictfp Java17+ and Java21 (warning NOT RUN) |
| NM-24 | P1 | §3: Math.fma minimal NM33, error/perf NOT RUN |
| NM-25 | P1 | §5: toString/toPlainString, negative scale |
| NM-26 | P1 | §6: MathContext vs setScale and repeated rounding |
| NM-27 | P1 | §3: Double.compare/Double.equals, NaN/-0.0 vs records |
| NM-28 | P1 | §1/7: constant overflow literal vs runtime/narrowing |
| NM-29 | P2 | §6/9: money/FX/tax/DB DECIMAL NOT RUN |
| NM-30 | P2 | §3/9: subnormals/JIT/error bounds NOT RUN |
| NM-31 | P2 | §9: vector/crypto/performance NOT RUN |
| NM-32 | P2 | §9: localized NumberFormat/CORE-I18N NOT RUN |

## 9. Sources A, реальные вопросы C и честные NOT RUN

**Нормативные A:** [JLS21 §5.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.6), [JLS21 §15.17](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.17), [JLS21 §4.2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.2.3), [JLS21 §15.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.4), [Math API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html), [BigDecimal API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html), [BigInteger API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigInteger.html), [RoundingMode API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/RoundingMode.html). Это **гарантии языка/API**, не универсальные бизнес-правила.

**Реальные C-формулировки:** [Q-HRUDAY, вопросы о integer overflow и floating-point equality](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md); [sudhir-tech, вопросы 149/150 BigDecimal и строкового конструктора](https://github.com/sudhir-tech/java-backend-interview-prep/blob/main/01-core-java/core-java-interview-questions.md). Эти источники подтверждают, что спрашивают *широкие темы*, но не статистическую частоту и не Java-норму. Для отдельных вопросов про `BigDecimal.divide(1/3)`, scale/rounding policy, `Double.doubleToRawLongBits` и `MathContext` **самостоятельное точное C-вопросы в проверенных банках отсутствуют (GAP)**; их разбираем как техническое продолжение.

**Наблюдение G1:** в OpenJDK21.0.12.1 Debian13 выполнены полные `NumbersProbe.java` и отдельные компиляционно-отрицательные кейсы; stdout **33 строки**, SHA-256 `9c9e8000292ee4face04197388feadb50ba8cdc1e7a9099c3b89ecbdd4b6ea83`. **33 строки — не 33 теста**. Это историческое подтверждение [G1 §5.1](../../docs/plans/CORE-NUMBERS-DATETIME.md). **Дополнительно интегрирован runnable G2:** [исходник](../../examples/core/numbers-datetime/src/NumbersProbe.java), [эталон](../../examples/core/numbers-datetime/numbers.expected.stdout), [README](../../examples/core/numbers-datetime/README.md). На техническом HEAD `d4dc693c67ac688682d39ab45dea8ed72ec9af85` [GitHub Actions #45140250500](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45140250500) успешно выполнил два Java 21 процесса и строгую проверку эталонов. **Отдельно подтверждён интегрированный author CI:** [Actions #45150568904](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45150568904) completed/SUCCESS на exact HEAD `4410bf7b374dbce6edafbb5d07328405125b3f3c`; технический [#45140250500](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45140250500) completed/SUCCESS только на `d4dc693c67ac688682d39ab45dea8ed72ec9af85`. Интегрированный workflow включает штатный Gradle/JUnit и отдельный позитивный runner: **1 clean javac, 2 JVM, 2 полных diff** и SHA-256 двух эталонов (33+38=71 строк stdout, не JUnit-тестов). **8 изолированных compiler-negative не входят в штатный CI**; все embedded snippets, иные JDK/OS/tzdb, финансовые политики, P2 и GUI/HTTP/anchors остаются NOT RUN. При любом последующем изменении HEAD необходим новый независимый CI на изменённом SHA.

**NOT RUN этим G2-текстом:** все embedded snippets как самостоятельные compilation units, компиляционно-отрицательные случаи в штатном CI, все альтернативные JDK/vendors/OS и hardware, реальная политика денежных единиц/округления/налогов/FX, JDBC/DB DECIMAL, `BigDecimal` performance/benchmark, strictfp compiler warning, NaN payload/subnormal error bounds, floating JIT/StrictMath benchmarking, vector/cryptographic constant time, CORE-I18N/NumberFormat, GUI Obsidian, внешние HTTP/anchors. Не приписывать P2 статус Implemented.

[CORE-DATETIME](datetime.md) — отдельная задача о *временной шкале и правилах часового пояса*. Она тоже показывает границы представления данных, но числовое округление и DST — разные механизмы. Статусы [CURRICULUM](../../docs/CURRICULUM.md) / [COVERAGE](../../docs/COVERAGE.md) переводит G6 после G5, не сам автор G2.

**Локализованное представление чисел:** [CORE-I18N](internationalization.md) показывает `NumberFormat`, `DecimalFormat`, `ParsePosition`, частичный разбор, округление и CLDR-зависимые разделители. Это граница UI, а не замена `BigDecimal` и правил арифметики из этой статьи.
