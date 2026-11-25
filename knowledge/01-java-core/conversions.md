# CORE-CONVERSIONS — преобразования типов и выбор операций

**Java 21 · предпосылка:** [CORE-TYPES — типы, значения и переменные](types.md)

## Почему один и тот же литерал ведёт себя по-разному

Предположим, метод принимает `byte`. Почему переменную можно инициализировать числом `67` без приведения, а вызвать этот метод как `takeByte(67)` — нельзя?

```java
byte stored = 67; // компилируется

static void takeByte(byte value) { }
// takeByte(67);         // ошибка компиляции
takeByte((byte) 67);     // компилируется
```

В обоих случаях `67` — выражение типа **`int`**. Однако Java разрешает разные преобразования в разных **контекстах**: при присваивании, при вызове метода, при явном приведении и при вычислении выражений. Здесь работают разные правила, а не «компилятор то сам догадывается, то нет».

**Преобразование типа** — разрешённое языком использование значения выражения в другом типе. Оно может сохранить значение или изменить его, а некоторые допустимые преобразования могут завершиться исключением при выполнении. Если требуемая цепочка не разрешена, компилятор отвергает программу: это не выполненное преобразование.

## Пять учебных групп и шесть формальных контекстов преобразований

| Контекст | Пример | Основная идея |
|---|---|---|
| **Присваивание** (*assignment*) | `long x = 67;` | Значение выражения должно быть совместимо с типом переменной; константам доступны особые послабления |
| **Вызов метода или конструктора** (*invocation*) | `choose(67)` | Два формальных контекста: строгий допускает тождественное и расширяющие преобразования, менее строгий также упаковку, распаковку и разрешённые сочетания. |
| **Явное приведение** (*casting*) | `byte b = (byte) 300;` | Разрешает больше преобразований, но не обещает сохранения значения |
| **Числовой контекст** | `byte a = 40, b = 27; int sum = a + b;` | Операторы определяют числовой тип операндов по правилам продвижения. |
| **Строковый контекст** | `"count=" + 67` | Операнд преобразуется в текст при строковой конкатенации |

Это пять учебных групп. Во введении [JLS 21, глава 5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html) перечислены шесть формальных контекстов: присваивание (assignment), строгий вызов (strict invocation), менее строгий вызов (loose invocation), строковый контекст (string), явное приведение (casting), числовой контекст (numeric). Тождественное преобразование допустимо там, где оно удовлетворяет требованиям контекста: например, `int n = 67; int same = n;` сохраняет тип и значение. Таблица показывает основные преобразования; она не охватывает все правила обобщённых типов (generics), в том числе непроверяемые преобразования (unchecked conversion) и захват подстановочного типа (capture conversion).

Тип некоторых выражений зависит от контекста (poly expressions в JLS). Целевой тип (target type) может участвовать в выводе типа самого выражения: например, он задаёт функциональный тип лямбды, а при вызове обобщённого метода (generic method) компилятор может вывести параметр типа с учётом присваивания. Это не приведение уже типизированного объекта лямбды к другому интерфейсу. [JLS §15.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.2), [CORE-GENERICS](generics.md), [CORE-FUNCTIONS](functions.md). Сохраните `Target.java`:

~~~java
class Target {
    public static void main(String[] args) {
        java.util.function.IntUnaryOperator f = x -> x + 1;
        java.util.List<String> xs = java.util.Collections.emptyList();
        int n = 67; int same = n;
        System.out.println(f.applyAsInt(same) + ":" + xs.size());
    }
}
~~~

Вывод `68:0`. У `var f = x -> x + 1;` нет целевого функционального типа. Компилятор отклонил отдельный пример `BadTarget` (см. ниже) с диагностикой `cant.infer.local.var.type/local.lambda.missing.target`. Старые короткие блоки статьи — фрагменты для обсуждения в соответствующем контексте класса или метода, не все являются самостоятельными файлами.

## Расширение и сужение примитивных типов

**Расширяющее преобразование** (*widening*) обычно не требует явного cast. **Сужающее** (*narrowing*) часто требует его и может потерять информацию.

```java
int value = 67;
long unchanged = value;            // int → long: все значения int сохраняются
double floating = unchanged;      // long → double: не все большие long точны

long tooBig = 300;
byte truncated = (byte) tooBig;   // 44, а не 300
```

Здесь `300` в `byte` даёт `44`: при сужении целого к `byte` остаются младшие 8 бит. Наличие приведения означает лишь разрешение операции. Пример с `long` и `double` требует отдельной оговорки:

```java
long exact = 9_007_199_254_740_993L;
double rounded = exact;
System.out.println((long) rounded); // 9007199254740992: точность потеряна
```

По [JLS 21 §5.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.2) среди расширяющих преобразований примитивов потеря числовой точности возможна только при `int → float`, `long → float`, `long → double`. Остальные сохраняют числовое значение: в частности, `int → long`, `int → double`, `float → double`. Само расширение не выбрасывает исключение. Пример ниже с `Promotion.java` проверяет три пограничных преобразования. Детали представления чисел и округления — [CORE-NUMBERS](numbers.md).

### Сужение чисел с плавающей точкой до целых типов

Здесь важно различать две операции: переход от дробного числа к целому и последующее уменьшение разрядности. [JLS SE 21 §5.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.3) задаёт правило для обоих исходных типов — `float` и `double`.

**Первый шаг — преобразование в `int` или `long`.** Если конечный тип — `long`, промежуточным типом будет `long`; во всех остальных рассматриваемых случаях (`int`, `byte`, `short`, `char`) — `int`.

- `NaN` преобразуется в 0.
- Для конечного числа дробная часть отбрасывается в сторону нуля, а не округляется к ближайшему целому или вниз. Например, 3.9 превращается в 3, а −3.9 — в −3.
- Если полученное целое не помещается в промежуточный тип, результат ограничивается его минимальным или максимальным значением соответственно. Отрицательная и положительная бесконечности дают те же границы.

```java
System.out.println((int) 3.9);                       // 3
System.out.println((int) -3.9);                      // -3
System.out.println((int) Double.NaN);                // 0
System.out.println((int) Double.POSITIVE_INFINITY);  // 2147483647
System.out.println((long) Double.NEGATIVE_INFINITY); // -9223372036854775808
```

Для `int` и `long` на этом преобразование заканчивается. В частности, `(long) Double.NEGATIVE_INFINITY` даёт `Long.MIN_VALUE` непосредственно по правилам перехода к `long`, а не сначала `Integer.MIN_VALUE` с последующим расширением.

**Второй шаг — только для `byte`, `short` и `char`.** Из полученного на первом шаге `int` сохраняются младшие 8 бит для `byte` или младшие 16 бит для `short` и `char`. В `byte` и `short` эти биты интерпретируются как знаковое целое в дополнительном коде, а в `char` — как беззнаковое 16-битное значение.

Разберём `(byte) 300.0`:

```text
double 300.0
    → int 300 = 0x0000012C
    → младшие 8 бит: 0x2C = 00101100₂
    → byte 44
```

300 помещается в `int`, поэтому на первом шаге ограничения диапазона нет. На втором шаге старшие биты отбрасываются: 300 − 256 = 44. Это не ограничение до `Byte.MAX_VALUE` (127).

У бесконечностей первый шаг даёт границы `int`:

| Исходное значение | Промежуточный `int` | Младшие 8 бит | Младшие 16 бит |
|---|---|---|---|
| `Double.POSITIVE_INFINITY` | 2147483647 = `0x7FFFFFFF` | `0xFF` | `0xFFFF` |
| `Double.NEGATIVE_INFINITY` | −2147483648 = `0x80000000` | `0x00` | `0x0000` |
| `Double.NaN` | 0 = `0x00000000` | `0x00` | `0x0000` |

У положительной бесконечности младшие биты все единичные. В знаковых типах `byte` и `short` это −1, а в беззнаковом `char` — 65535. У отрицательной бесконечности младшие 16 бит нулевые, поэтому результат преобразования к `char` равен 0. Для `NaN` первый шаг даёт ноль, который сохраняется и на втором шаге.

```java
System.out.println((byte) 300.0);                         // 44
System.out.println((byte) Double.POSITIVE_INFINITY);      // -1
System.out.println((short) Double.POSITIVE_INFINITY);     // -1
System.out.println((int) (char) Double.NEGATIVE_INFINITY); // 0
System.out.println((int) (char) Double.POSITIVE_INFINITY); // 65535
System.out.println((byte) Double.NaN);                    // 0
```

Дополнительное приведение `char → int` в примерах нужно только для вывода числового значения вместо символа. Это расширяющее преобразование, сохраняющее беззнаковое значение `char` ([JLS §5.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.2)).

**Явное приведение и допустимость присваивания — разные вопросы.** Неявное сужение выражения типа `double` до `byte` при обычном присваивании недопустимо, даже если числовое значение могло бы поместиться. Явное приведение разрешено, но применяет описанное правило и не гарантирует сохранение значения:

```java
// byte x = 300.0;       // ошибка компиляции
// byte y = 67.0;        // тоже ошибка: выражение имеет тип double
byte x = (byte) 300.0;   // допустимо: 44
```

См. [JLS §5.2 — контекст присваивания](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2) и [§5.5 — контекст явного приведения](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.5). Само сужающее преобразование примитивов не бросает исключение во время выполнения из-за потери точности или диапазона. Ограничение до крайних значений **промежуточного** типа не означает ограничения до границ конечного `byte` или `short`.

Тонкости IEEE 754, `-0.0`, `BigDecimal` и доменной политики округления — в самостоятельной [CORE-NUMBERS](numbers.md).

## Константные выражения: где cast не требуется

В контексте **присваивания** компилятор может сузить константное выражение типа `byte`, `short`, `char` или `int` до `byte`, `short` или `char`, если его значение помещается в целевой тип.

```java
byte a = 67;               // допустимо, хотя 67 — int
byte b = 60 + 7;           // допустимо: константное выражение со значением 67
final int constant = 67;
byte c = constant;         // допустимо: final + константное выражение

int ordinary = 67;
// byte d = ordinary;      // ошибка: обычная переменная int не константное выражение
// byte e = 128;           // ошибка: 128 не помещается в byte
```

Но при передаче аргумента в метод аналогичного послабления **нет**. Поэтому `takeByte(67)` не компилируется, даже если число очевидно помещается в `byte`. Это различие `assignment` и `invocation` — один из самых полезных вопросов для понимания правил языка.

`final Integer n = 67;` не объявляет константную переменную: по [JLS §4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4) она должна иметь примитивный тип либо `String`, быть `final` и иметь константный инициализатор. Поэтому `byte b = n;` не получает особого разрешения на сужение после распаковки.

## Числовое продвижение и переполнение до присваивания

```java
byte first = 40;
byte second = 27;
// byte sum = first + second; // ошибка: результат сложения — int
int sum = first + second;    // 67

byte small = 60;
small += 7;                  // допустимо: составное присваивание включает приведение
// small = small + 7;        // ошибка: справа int, не byte
```

При обычных бинарных арифметических операциях Java сначала распаковывает операнды, если это необходимо, затем выбирает общий числовой тип: `double`, если хотя бы один операнд имеет тип `double`, иначе `float`, иначе `long`, иначе `int`. В частности, при сложении двух `byte` результат имеет тип `int`.

У сдвигов другое правило: каждый операнд проходит числовое продвижение отдельно, а тип результата определяет левый операнд после продвижения. В `1 << 32L` слева `int`; Java использует пять младших битов расстояния сдвига (здесь 0), поэтому результат равен `1`. Для `long` слева используются шесть младших битов.

У числового условного оператора `?:` тоже свои правила: например, переменная `byte small` и константа `68`, представимая в `byte`, дают тип `byte`. Сочетание `Integer` и `int` даёт `int`; если выбрана ссылка `null`, её распаковка вызывает `NullPointerException` (NPE). См. [JLS §5.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.6), [§15.19](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.19), [§15.25.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.25.2).

**Составное присваивание** `x += y` не всегда равнозначно записи `x = x + y` с точки зрения типов: оно выполняет предусмотренное языком приведение к типу левой переменной. Поэтому даже `byte b = 127; b += 1;` законно и даст `-128`. В соответствии с [§15.26.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.26.2) `E1 op= E2` подобно `E1 = (T)(E1 op E2)`, где T — тип E1, но E1 вычисляется только один раз. Поэтому при `a[i++] += 1` индекс увеличивается один раз, а не два.

```java
long wrong = Integer.MAX_VALUE + 1;
long correct = (long) Integer.MAX_VALUE + 1;
System.out.println(wrong);   // -2147483648
System.out.println(correct); // 2147483648
```

Почему `long` не спасает `wrong`? **Операция справа уже выполнилась как `int` и переполнилась.** Присваивание `long` расширяет полученный отрицательный результат, а не заново вычисляет выражение. В `correct` первый операнд заранее расширен до `long`.

```java
int count = 3;
double wrongAverage = count / 2; // 1.0: int / int = int
double rightAverage = count / 2.0; // 1.5: один из операндов double
```

Правило: **тип переменной-приёмника не обязан влиять на тип уже вычисляемого выражения**.

Сохраните `Promotion.java` отдельно, запустите без аргументов:

~~~java
class Promotion {
    static String type(byte x) { return "byte"; }
    static String type(int x) { return "int"; }
    public static void main(String[] args) {
        int i = 0; int[] a = {67, 68}; a[i++] += 1;
        System.out.println(i + ":" + a[0] + ":" + a[1]);
        byte small = 67; boolean flag = args.length == 0;
        System.out.println(type(flag ? small : 68) + ":" + type(small + 1) + ":" + (1 << 32L));
        Integer missing = null;
        try { int result = flag ? missing : 67; throw new AssertionError(result); }
        catch (NullPointerException e) { System.out.println("NPE"); }
        int x = 16_777_217; long y = 9_007_199_254_740_993L;
        System.out.println((int)(float)x + ":" + (long)(float)y + ":" + (long)(double)y);
    }
}
~~~

Фактический вывод:

~~~text
1:68:68
byte:int:1
NPE
16777216:9007199254740992:9007199254740992
~~~

В первой строке `1:68:68`: при `a[i++] += 1` индекс `i` увеличивается один раз, `a[0]` становится 68, а `a[1]` остаётся 68.

Во второй строке `byte:int:1`: числовой условный оператор даёт `byte`, поэтому компилятор выбирает `type(byte)`; сложение `small + 1` даёт `int`, поэтому выбирается `type(int)`. В `1 << 32L` расстояние сдвига маскируется до 0, а результат имеет тип `int`.

Третья строка `NPE`: при запуске без аргументов условие `flag` истинно, Java выбирает `missing` и пытается распаковать ссылку `null`. Обработчик перехватывает `NullPointerException`.

В последней строке `16777216:9007199254740992:9007199254740992` видно, что каждое из трёх расширяющих преобразований теряет единицу: `int → float` для 16 777 217, `long → float` и `long → double` для 9 007 199 254 740 993. Обратное приведение к целому типу используется только для наглядного вывода полученных значений. Принятые в B1 правила сужения чисел с плавающей точкой выше не меняются. [CORE-OPERATORS](operators.md) и [CORE-NUMBERS](numbers.md) раскрывают механизмы подробнее.

<a name="boxing-и-unboxing-превращение-числа-в-объект-и-обратно"></a>

## Упаковка и распаковка (boxing/unboxing): примитивные значения и объекты-обёртки

```java
Integer wrapped = 67;    // boxing: int → Integer
int restored = wrapped;  // unboxing: Integer → int

Integer missing = null;
// int n = missing;      // NullPointerException во время выполнения
```

В отличие от `int`, `Integer` — **ссылочный тип**, переменная которого может содержать `null`. У восьми примитивных типов свои классы-обёртки: `boolean → Boolean`, `byte → Byte`, `short → Short`, `char → Character`, `int → Integer`, `long → Long`, `float → Float`, `double → Double`.

Упаковка (boxing) даёт ссылку на объект соответствующего класса-обёртки: этот объект может уже существовать. Распаковка (unboxing) извлекает из обёртки примитивное значение; если ссылка равна `null`, возникает `NullPointerException`. При упаковке и распаковке существующий объект не превращается в объект другого класса. `Integer` и `Long` — разные классы, не взаимозаменяемые преобразованием ссылок. Равенство числового значения не делает их экземпляры равными по `equals`.

Не все на первый взгляд естественные цепочки преобразований допустимы:

```java
Number number = 67;      // int → Integer → Number: допустимо
Long correct = 67L;     // long → Long
// Long wrong = 67;     // ошибка: int → long → Long так не разрешено при присваивании
```


| Цепочка | Присваивание | Вызов (один параметр) |
|---|---|---|
| `int → long` | да | да, строгий вызов (strict) |
| `int → Integer → Number` | да | да, менее строгий вызов (loose) |
| `Integer → int → long` | да | да, менее строгий вызов (loose) |
| `int`-константа 67 → `byte` → `Byte` | да | нет |
| `int`-константа 67 → `short`/`char` → `Short`/`Character` | да | нет |
| обычный `int → byte → Byte` | нет | нет |
| `int → long → Long` | нет | нет |
| `Integer → int → byte` | нет | нет |
| `Integer → Long` | нет | нет |

Это частые сочетания, а не разрешение произвольно складывать любые преобразования. Правила [§5.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2) и [§5.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.3) задают допустимые цепочки. Для `Long` нужен `67L` либо явное предварительное `(long) n`, затем упаковка; запись `(Long)n` для `int n` не даёт такой цепочки. Сохраните `Composition.java`:

~~~java
class Composition {
    static void accept(Number n, long x) { System.out.println(n.getClass().getSimpleName() + ":" + x); }
    public static void main(String[] args) {
        Byte b = 67; Short s = 67; Character c = 67;
        long wide = Integer.valueOf(67); Number n = 67; Object o = 67;
        System.out.println(b + ":" + s + ":" + (int)c + ":" + wide + ":" + n.getClass().getSimpleName() + ":" + (long)(Integer)o);
        accept(67, Integer.valueOf(67));
    }
}
~~~

Вывод `67:67:67:67:Integer:67`, затем `Integer:67`. При инициализации первых трёх переменных компилятор разрешает сужение константы с последующей упаковкой. В вызове `accept` первый аргумент упаковывается в `Integer`, затем ссылка расширяется до `Number`; второй аргумент распаковывается в `int`, затем значение расширяется до `long`. Отдельные отрицательные примеры приведены ниже.
### Кэш `Integer` и граница 127

```java
Integer a = 127, b = 127;
System.out.println(a == b);      // true — гарантировано

Integer c = 128, d = 128;
System.out.println(c == d);      // идентичность не гарантирована

Integer e = 1000, f = 1000;
System.out.println(e == f);      // идентичность не гарантирована
System.out.println(e.equals(f)); // true — одинаковое числовое значение
```

[JLS 21 §5.1.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.7) гарантирует, что два преобразования упаковки одного и того же значения, полученного из **константного выражения**, дают идентичные ссылки в следующих случаях:

- `boolean`: `true` и `false`;
- `char`: символы U+0000…U+007F включительно;
- `byte`, `short`, `int`, `long`: целые значения от −128 до 127 включительно.

Для `float` и `double` такой гарантии нет. Она не распространяется в общем случае на упаковку значения неконстантной переменной, даже если значение попадает в указанный диапазон.

Отдельный контракт [Integer.valueOf(int)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Integer.html#valueOf(int)) гарантирует кэширование значений от −128 до 127 для любого переданного `int`, независимо от того, константа ли аргумент. Метод может кэшировать и другие значения.

**Для 128 и 1000 нельзя обещать ни `true`, ни `false` по спецификации.** Не используйте `==` для проверки равенства чисел в двух обёртках.

По [Java SE 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Integer.html) `Integer` — класс, основанный на значениях (value-based class). Не используйте идентичность его экземпляров для синхронизации. `parseInt("67")` возвращает примитив `int`, `valueOf("67")` — `Integer`, `intValue()` извлекает примитивное значение; при разборе некорректного числа возникает `NumberFormatException`. Не делайте общих выводов о производительности упаковки без измерений. Сохраните `CacheDemo.java`:

~~~java
class CacheDemo {
    public static void main(String[] args) {
        int runtime = args.length == 0 ? 127 : Integer.parseInt(args[0]);
        Integer x = runtime, y = runtime, a = 128, b = 128;
        Integer c = 1000, d = 1000;
        System.out.println((x == y) + ":" + (a == b) + ":" + (c == d) + ":" + (Integer.valueOf(127) == Integer.valueOf(127)));
        Boolean t = true, u = true; Byte e = 67, f = 67; Short g = 67, h = 67;
        Character j = 'A', k = 'A'; Long l = 67L, m = 67L;
        System.out.println((t == u) + ":" + (e == f) + ":" + (g == h) + ":" + (j == k) + ":" + (l == m));
        System.out.println(Integer.parseInt("67") + ":" + Integer.valueOf("67").intValue());
        try { Integer.parseInt("x"); throw new AssertionError(); }
        catch (NumberFormatException ex) { System.out.println("NumberFormatException"); }
    }
}
~~~

Запуск `java -cp out CacheDemo` (без аргументов) на Debian OpenJDK 21.0.12.1 дал `true:false:false:true`, `true:true:true:true:true`, `67:67`, `NumberFormatException` по строкам.

Четыре позиции первой строки — результаты `x == y` (упаковка неконстантной переменной `runtime` со значением 127), `a == b` (128), `c == d` (1000) и `Integer.valueOf(127) == Integer.valueOf(127)` соответственно. При повторном запуске `java -XX:AutoBoxCacheMax=1000 -cp out CacheDemo` первая строка стала `true:true:true:true`: изменились только две средние позиции, а другие три строки вывода остались прежними.

Эта JVM приняла флаг, и такой результат наблюдался на ней. Это не спецификация флага, не описание границ кэша для всех JVM и не гарантия упаковки неконстантных выражений. Для сравнения числовых значений используйте нужный примитивный тип либо `equals` у одинаковых типов обёрток.

### Один `Integer`, другой `int`: `==` уже другой

```java
Integer boxed = 1000;
int primitive = 1000;
System.out.println(boxed == primitive); // true: boxed распакован в int

Integer missing = null;
// System.out.println(missing == 0);   // NullPointerException при распаковке
System.out.println(missing == null);    // true: сравниваются ссылки
```

Здесь **не участвует кэш объектов**. Поскольку один операнд числовой примитив, Java выполняет числовое продвижение, при необходимости распаковывая `Integer`. Если в ссылке `null`, распаковка завершится `NullPointerException`.

Именно поэтому нельзя заучивать «`Integer ==` после 127 возвращает `false`»: сначала разберите **типы обоих операндов**.

## Как преобразования влияют на перегрузку

```java
static String choose(long x)     { return "long"; }
static String choose(Integer x) { return "Integer"; }

System.out.println(choose(67)); // long, не Integer
```

По [JLS §15.12.2–2.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2) компилятор ищет применимые методы в трёх фазах:

1. Строгий вызов с фиксированным числом аргументов (strict invocation).
2. Менее строгий вызов с фиксированным числом аргументов (loose invocation).
3. Вызов с переменным числом аргументов (variable arity invocation, varargs).

Преобразование `int → long` доступно уже в первой фазе, а `int → Integer` требует второй. Компилятор останавливает поиск на первой фазе, в которой найдены применимые методы, и выбирает наиболее специфичный метод по правилам JLS. Если выбор неоднозначен, он не переходит к следующей фазе ради разрешения неоднозначности. Для обобщённых методов (generic methods) и объявлений с эквивалентными для переопределения сигнатурами (override-equivalent) действуют дополнительные правила.

```java
static void accept(String s) { }
static void accept(Integer i) { }

// accept(null);           // ошибка компиляции: неоднозначность
accept((String) null);     // явное приведение выбирает нужную перегрузку
```

`null` совместим с обоими **несвязанными** ссылочными типами, и ни одна перегрузка не является более специфичной. Это ошибка **компиляции**, а не `NullPointerException`.

> [!note]- Углубление: точный выбор перегрузки
> JLS 21 §15.12 определяет применимость методов и «наиболее специфичный» вариант. Простая фраза «всегда widening перед boxing» — полезная подсказка для приведённого примера, но **не универсальный алгоритм** для всех generic, varargs, лямбд, методов и конструкторов. Вывод параметров типов и стирание разобраны в [CORE-GENERICS](generics.md) и [CORE-ERASURE](erasure.md); подробный разбор bridge-байткода остаётся задачей будущей `JVM-BYTECODE`.

В первых двух фазах компилятор рассматривает параметр `Object...` как `Object[]`: метод с переменным числом аргументов может оказаться применимым и без упаковки аргументов в новый массив. Сохраните `Phases.java`:

~~~java
class Phases {
    static String phase(Integer x) { return "Integer"; }
    static String phase(int... x) { return "varargs"; }
    static String fixed(Object x) { return "Object"; }
    static String fixed(Object... x) { return x == null ? "null array" : "array"; }
    static String ref(Object x) { return "Object"; }
    static String ref(int x) { return "int"; }
    static String related(Object x) { return "Object"; }
    static String related(String x) { return "String"; }
    public static void main(String[] args) {
        System.out.println(phase(67) + ":" + phase(67, 68) + ":" + fixed(new Object[0]) + ":" + fixed((Object[])null) + ":" + ref(Integer.valueOf(67)) + ":" + related(null));
    }
}
~~~

Вывод `Integer:varargs:array:null array:Object:String`. Для `phase(67)` компилятор выбирает перегрузку с упаковкой во второй фазе, не дойдя до третьей. Для `phase(67, 68)` нужна третья фаза, с переменным числом аргументов.

Вызовы `fixed(new Object[0])` и `fixed((Object[])null)` выбирают более специфичную перегрузку с параметром `Object[]` уже в первой фазе; второй передаёт именно ссылку `null` вместо массива. Для `ref(Integer.valueOf(67))` расширение ссылки до `Object` доступно раньше распаковки в `int`.

При `related(null)` параметр `String` специфичнее `Object`. Между несвязанными `String` и `Integer` выбор неоднозначен. В `BadAmbiguity` ниже два метода применимы во второй фазе, но ни один из них не является более специфичным. Наличие `m(int...)` не разрешает неоднозначность переходом к третьей фазе.

## Преобразования ссылок: приведение не превращает объект в другой

```java
Object value = "Java";
String text = (String) value; // разрешено: объект действительно String

Object unrelated = new Object();
// String wrong = (String) unrelated; // ClassCastException при выполнении
```

Расширение ссылочного типа (upcast), например присваивание `Object o = n;` после `Integer n = 67;`, не создаёт новый объект и не меняет его класс. Сужающее ссылочное преобразование (downcast) может потребовать проверки фактического типа объекта. Приведение ссылки `null` к ссылочному типу успешно, но `null instanceof String` даёт `false`. Явное приведение между несвязанными конечными классами, например `Integer` и `String`, компилятор отвергает. [JLS §5.1.5–6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.5), [§5.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.5), [CORE-INHERITANCE](inheritance.md), [CORE-PATTERNS](patterns.md).

Не путайте `ClassCastException` с `NullPointerException` при распаковке и с потерей числовой точности — это три разные причины проблем.

Особая ловушка: `(long)o`, где статический тип `o` — `Object`, разрешён как сужение ссылки к `Long` и распаковка. Если объект на самом деле `Integer`, проверка `Long` завершается `ClassCastException`. `(long)(Integer)o` сначала проверяет `Integer`, затем распаковывает и расширяет `int` до `long`. Сохраните `RefCast.java`:

~~~java
class RefCast {
    public static void main(String[] args) {
        Integer n = 67; Object o = n;
        System.out.println((o == n) + ":" + ((Integer)o == n) + ":" + (long)(Integer)o);
        try { long bad = (long)o; throw new AssertionError(bad); }
        catch (ClassCastException e) { System.out.println("CCE:Object(Integer) to long"); }
        Object absent = null; String s = (String)absent;
        System.out.println((s == null) + ":" + (absent instanceof String));
    }
}
~~~

Вывод по строкам: `true:true:67`, `CCE:Object(Integer) to long`, `true:false`. Первые два сравнения в первой строке подтверждают идентичность ссылки, а явное приведение к `Integer` с последующей распаковкой и расширением даёт 67. Во второй строке обработчик сообщает о `ClassCastException` при попытке проверить `Long`. Последняя строка показывает успешное приведение ссылки `null` и ложный результат `instanceof`. Непроверяемые приведения параметризованных типов (unchecked casts) разобраны в [CORE-GENERICS](generics.md) и [CORE-ERASURE](erasure.md); подробный разбор обобщённых типов и мостовых методов (bridge methods) выходит за границы этого раздела.

## Строковый контекст и String Pool

```java
System.out.println("count=" + 67); // count=67
System.out.println("value=" + (Object) null); // value=null
```

Оператор `+` со строкой преобразует другой операнд в текст. Это **не то же самое**, что преобразовать произвольный объект в `String` через cast.

Отдельная классическая ловушка — **пул интернированных строк**:

```java
String first = "Java";
String second = "Java";
String folded = "Ja" + "va";  // строковое константное выражение
String created = new String("Java");

System.out.println(first == second);            // true
System.out.println(first == folded);            // true
System.out.println(first == created);           // false
System.out.println(first == created.intern());  // true
System.out.println(first.equals(created));      // true
```

Литералы и строковые константные выражения используют интернированные экземпляры. `new String("Java")` создаёт отдельный объект; `intern()` возвращает каноническую строку. **Это не кэш `Integer`**.

[JLS §5.1.11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.11), [§15.18.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.18.1) и [§15.29](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.29) различают константную строку и неконстантную конкатенацию: результат последней по правилам языка является новой строкой. Это не означает, что реализация обязательно физически выделит память при каждой операции: она вправе устранять ненаблюдаемые промежуточные объекты. Здесь не исследуются ни способ преобразования конкатенации компилятором (lowering), ни оптимизации JIT.

Операции `+` группируются слева направо: `'A' + 67 + "!"` сначала даёт число 132, а `"!" + 'A' + 67` с самого начала выполняет строковую конкатенацию. Ссылка `null` преобразуется в текст `null`; для ненулевой ссылки используется строковое представление (`toString`, если результат равен `null` — текст `null`), а не явное приведение к `String`. [CORE-STRINGS и CORE-BUILDERS](strings.md). Сохраните `Concat.java`:

~~~java
class Concat {
    public static void main(String[] args) {
        String part = "Ja"; final String constant = "Ja";
        String r = part + "va";
        System.out.println((r == "Java") + ":" + (constant + "va" == "Java") + ":" + (r.intern() == "Java"));
        System.out.println('A' + 67 + "!");
        System.out.println("!" + 'A' + 67);
        System.out.println("value=" + (Object)null);
        Object o = new Object() { public String toString() { return "text"; } };
        System.out.println("" + o);
        try { String s = (String)o; throw new AssertionError(s); }
        catch (ClassCastException e) { System.out.println("CCE"); }
    }
}
~~~

Вывод по строкам: `false:true:true`, `132!`, `!A67`, `value=null`, `text`, `CCE`. Результат неконстантной конкатенации `part + "va"` не идентичен литералу, а результат константного выражения идентичен ему; `intern()` возвращает каноническую ссылку. Следующие две строки показывают порядок группировки `+`, четвёртая — преобразование `null` в текст. Вызов `toString()` объекта даёт `text`, но сам объект не становится `String`: попытка приведения завершается `ClassCastException`, которую перехватывает обработчик.

## Что спросить себя перед собеседованием

| Вопрос | Объяснение |
|---|---|
| Почему `byte b = 67` допустимо, а `takeByte(67)` — нет? | Контексты присваивания и вызова |
| Почему `final int x = 67; byte b = x` работает, а обычный `int x` — нет? | Константные выражения |
| Почему `byte + byte` даёт `int`, а `b += 1` компилируется? | Продвижение и составное присваивание |
| Почему `Integer == Integer` и `Integer == int` — разные ситуации? | Сравнение ссылок в первом случае, распаковка и сравнение числовых значений во втором. |
| Почему `choose(67)` попадает в `long`, а `accept(null)` неоднозначен? | Фазы выбора перегрузки |
| Почему `long sum = intA + intB` всё равно может переполниться? | Тип операции до присваивания |
| Что произойдёт при `(int)Double.NaN` или при неверном сужающем приведении ссылки (downcast)? | `NaN` преобразуется в 0; разрешённое приведение несовместимого объекта может вызвать `ClassCastException`. |
| Почему `(byte)300.0` даёт 44, а `(byte)Double.POSITIVE_INFINITY` — −1? | Сначала преобразование в int, затем сохранение младших 8 бит; это не ограничение диапазоном byte |
| Почему `"Ja" + "va" == "Java"`, но `new String("Java") == "Java"` — `false`? | Интернирование строк |
| Почему `phase(67)` и `phase(67,68)` выбирают разные методы? | Упаковка доступна во второй фазе, вызов с переменным числом аргументов — в третьей; массив как фиксированный параметр может участвовать ещё раньше. |
| Почему `flag ? small : 68` имеет тип `byte`, а `small + 1` — `int`? | Специальное правило числового условного оператора и обычное арифметическое продвижение. |
| Почему `(long)o` с `Object o = Integer.valueOf(67)` выбрасывает `ClassCastException` при выполнении, а `(long)(Integer)o` работает? | В первом случае проверяется `Long`, во втором — `Integer` с последующей распаковкой и расширением примитивного значения. |
| Почему `a[i++] += 1` не увеличивает индекс дважды? | Левый операнд составного присваивания вычисляется один раз. |


## Практика

Короткие примеры специально отделяют **ошибки компиляции** (помечены комментариями) от исключений при выполнении. Для проверки выбранных случаев используется [ConversionsDemo.java](../../examples/core/ConversionsDemo.java).

### Изолированные контрпримеры D-A02

Каждый из следующих файлов предназначен для отдельной компиляции, а не для совместной сборки. Номера N01–N16 в историческом аудите обозначают технические отрицательные пробы, а не findings JC-A02-N01–N09. Сужение при вызове, присваивании и арифметике (N01–05/N16):

`BadByteCall.java`:

~~~java
class BadByteCall { static void take(byte b) {} static void test() { take(67); } }
~~~

`BadOrdinary.java`:

~~~java
class BadOrdinary { void test() { int ordinary = 67; byte b = ordinary; } }
~~~

`BadRange.java`:

~~~java
class BadRange { void test() { byte b = 128; } }
~~~

`BadSum.java`:

~~~java
class BadSum { void test() { byte a = 40, b = 27; byte sum = a + b; } }
~~~

`BadAssignment.java`:

~~~java
class BadAssignment { void test() { byte small = 60; small = small + 7; } }
~~~

`BadOther.java`:

~~~java
class BadOther { void test() { byte small = 60; byte other = small + 1; } }
~~~

`BadByteCall` отвергается с `cant.apply.symbol` и `possible.loss.of.precision`; остальные пять — `prob.found.req/possible.loss.of.precision`. Во всех случаях причина — недопустимая цепочка к `byte`, а не отсутствие инициализации.

Упаковка, тип обёртки, явное приведение (N06/N08–14):

`BadLong.java`:

~~~java
class BadLong { Long n = 67; }
~~~

`BadVariableBox.java`:

~~~java
class BadVariableBox { void test() { int n = 67; Byte b = n; } }
~~~

`BadBoxCall.java`:

~~~java
class BadBoxCall { static void take(Byte b) {} static void test() { take(67); } }
~~~

`BadWrapper.java`:

~~~java
class BadWrapper { Integer a = 67; Long b = a; }
~~~

`BadFinal.java`:

~~~java
class BadFinal { final Integer n = 67; byte b = n; }
~~~

`BadLongCast.java`:

~~~java
class BadLongCast { int n = 67; Long a = (Long)n; }
~~~

`BadDisjoint.java`:

~~~java
class BadDisjoint { String s = (String)Integer.valueOf(67); }
~~~

`BadBoolean.java`:

~~~java
class BadBoolean { boolean b = (boolean)67; }
~~~

`BadBoxCall` даёт `cant.apply.symbol/inconvertible.types`; остальные — `prob.found.req/inconvertible.types`. `boolean` не числовой тип; `Integer` и `String` статически несовместимы. Обычный `int` не получает послабление для констант. Неоднозначность и целевой тип (N07/N15 и дополнительный poly-контраст):

`BadNull.java`:

~~~java
class BadNull { static void accept(String s) {} static void accept(Integer i) {} static void test() { accept(null); } }
~~~

`BadAmbiguity.java`:

~~~java
class BadAmbiguity { static void m(long a, Integer b) {} static void m(Integer a, long b) {} static void m(int... x) {} static void test() { m(67, 67); } }
~~~

`BadTarget.java`:

~~~java
class BadTarget { void test() { var f = x -> x + 1; } }
~~~

Первые два дают `ref.ambiguous`, третий — `cant.infer.local.var.type/local.lambda.missing.target`. Все 17 отдельных компиляций завершились кодом 1, пустым stdout и ровно одной ошибкой. Комментарии с отрицательными строками в центральном `ConversionsDemo` не являются запущенными компиляторными тестами CI.

**Историческое уточнение:** в [плане CORE-CONVERSIONS, L21](https://github.com/Melchiorius/backend-interview-labs/blob/f04f1eb5ee3a400a988757414bcae09113bd885b/docs/plans/CORE-CONVERSIONS.md#L21) написано `byte small = small + 1`. Это чтение собственного объявления, а не чистый контраст типов. Нужна уже объявленная переменная: `byte small = 60; small = small + 7;` (N05) либо `byte small = 60; byte other = small + 1;` (N16). Именно эти независимые пробы проверены выше. Исторический план и аудит не меняются.

### Фактические проверки автора D-A02

Роль 02 выполнила 24 отдельные компиляции временных файлов: 7 с кодом 0 и 17 с ожидаемым кодом 1. Восемь успешных запусков JVM включают второй запуск `CacheDemo` с флагом кэша. Среда — Debian OpenJDK/javac 21.0.12.1, `LC_ALL=C`.

Для каждого файла в отдельном временном каталоге использована команда `javac --release 21 -XDrawDiagnostics -d out Имя.java`; успешно скомпилированные примеры запускались командой `java -cp out Имя`. У успешных компиляций stdout и stderr пусты, у успешных запусков JVM stderr пуст; отрицательные диагностики перечислены выше.

Исходные тексты проверены и сопоставлены с листингами статьи на коммите `f913b68b0c3c507e5674c2a3ccb3e9f8f7547181`; их Git-хеши, команды, коды завершения, фактический вывод, диагностические ключи и ограничения приведены в [адресном отчёте роли 02 #7168942143](https://github.com/Melchiorius/backend-interview-labs/issues/302#issuecomment-7168942143). [#302](https://github.com/Melchiorius/backend-interview-labs/issues/302) — постановка задачи.

Это отдельные временные компиляции, а не 24 теста CI. Программы RefCast, Concat и Promotion перехватывают ожидаемые исключения. JIT, `javap` и производительность не проверялись; прежняя серия проб исторического аудита целиком не повторялась. Редакционные изменения не означают нового локального запуска.

### Сохранённый исполняемый пример B1

Запуск из корня репозитория с JDK 21:

```powershell
java examples/core/ConversionsDemo.java
```

Самостоятельные объяснения см. в [CORE-SYNTAX — области видимости и вывод локального типа](syntax.md) и [CORE-OPERATORS — вычисление выражений, `+=`, `?:` и числовое продвижение](operators.md). Эти темы не исчерпываются правилами контекстов преобразований.

Другие связанные материалы: [CORE-TYPES](types.md), [CORE-PARAMS](parameters.md), [CORE-GENERICS](generics.md), [CORE-ERASURE](erasure.md), [массивы и проверка ковариантности во время выполнения](arrays.md), [строки и разрешение перегрузок `String.valueOf(null)`](strings.md) с [примером из двух программ](../../examples/core/arrays-strings/README.md). [CORE-NUMBERS](numbers.md) — числа, точность и округление.

Роль 02 восстановила `ConversionsDemo.java` из исходного BASE `8ba59d828a2a9cb55c61614eaf289046c8665415` и итоговой авторской версии D-A02 в двух временных деревьях, не в полном клоне. Git-хеш файла `80f57177bc021d19fb88bccd8be3ae7129aff115` и режим `100644` совпали.

В каждом дереве были выполнены `javac --release 21 -Xlint:all -d out examples/core/ConversionsDemo.java`, `java examples/core/ConversionsDemo.java` и `java -cp out ConversionsDemo`. Все команды завершились с кодом 0. При компиляции stdout и stderr пусты, при запуске stderr пуст; четыре строки stdout при всех запусках совпали побайтно, включая `CHECK PASSED`. Это четыре строки вывода сохранённого примера B1, а не четыре теста. Подробный протокол локальных запусков приведён в адресном отчёте выше; B1 и все его проверки сохранены. Редакционные изменения не являются новым локальным запуском или результатом независимого G4.

## Первоисточники

- [JLS 21 §5 — Conversions and Contexts](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html)
- [JLS 21 §5.1.3 — Narrowing Primitive Conversion](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.3)
- [JLS 21 §15.12 — Method Invocation and Overload Resolution](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12)
- [JLS 21 §15.21 — Equality Operators](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21)
- [JLS 21 §15.26 — Assignment Operators](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.26)
- [JLS 21 §3.10.5 — String Literals](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.5)
- [Java SE 21 `Integer.valueOf`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Integer.html#valueOf(int))
- [Java SE 21 `String.intern`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#intern())
- [JLS 21 §15.19 — сдвиги](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.19), [§15.25.2 — числовой условный оператор](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.25.2), [§15.29 — константные выражения](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.29).

Интервью-источники — историческая выборка [A02](../../docs/audits/java-core-full-2026-11-24/A02-CORE-CONVERSIONS.md), не оценка частоты и не новый G1: [teamlead](https://github.com/teamlead/java-interview-questions/blob/4c0784024412904625e1455189d3c05cba07bebf/README.md) L98/146/148,478/479,781/1066,1187,1299; [Hruday](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/ced3cf4f91084e9b45366f7a81a70526e7a4be7d/README.md) L92–96,120–121,241–244,479–488,635–691 (в частности 664–678 — parse/valueOf; не воспроизводить предположение L653 об универсальном `false` для 200); [Chernyshov](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/597e08112ab6a57d6eac8e3c70c070da606dae73/eng/README.md) L59,263/278/280,288–289; [Baeldung Java Interview Questions](https://www.baeldung.com/java-interview-questions), beginners Q12/Q13, advanced Q2. Ответы устанавливаются по JLS/API, не по этим спискам.
