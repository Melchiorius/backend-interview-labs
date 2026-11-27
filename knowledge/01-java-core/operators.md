# CORE-OPERATORS — вычисления и побочные эффекты в Java SE 21

**Область:** Java 21 **без preview**. [Принятый план G1](../../docs/plans/CORE-SYNTAX-OPERATORS.md), [Issue #154](https://github.com/Melchiorius/backend-interview-labs/issues/154). **OP-01–OP-30: 20 P0 / 6 P1 / 4 P2**. Здесь мы разбираем, *какой результат и какие изменения состояния* производят выражения. [CORE-SYNTAX](syntax.md) разбирает компиляционные единицы, объявления и области имён. [Общий runnable Java 21](../../examples/core/syntax-operators/README.md) содержит [OperatorsProbe](../../examples/core/syntax-operators/src/OperatorsProbe.java), [эталон OP01–OP27](../../examples/core/syntax-operators/operators.expected.stdout) и [verify.sh](../../examples/core/syntax-operators/verify.sh). Более широкие контракты типов, [преобразований](conversions.md), [передачи аргументов](parameters.md), [равенства](equality.md), [строк](strings.md) и [управления потоком](control-flow.md) остаются отдельными самостоятельными статьями.

Вопрос «что напечатает этот код» труден, когда в одной строке перемешаны приоритет, изменение переменной, вычисление аргументов и неявные преобразования. У Java есть **точные гарантии порядка вычисления**, и ответ следует строить пошагово: сначала определить группировку выражения, затем типы и конкретные обращения к переменным, потом порядок побочных эффектов и только после — итоговое значение. Не приписывайте «случайный» порядок HotSpot, который определён спецификацией языка.

**Воспроизведение.** Из корня репозитория с JDK 21 и Bash: `bash examples/core/syntax-operators/verify.sh`. Новые наблюдения ниже относятся к отдельным временным [Probe.java, Zero.java и 14 отрицательным файлам — исходники, команды и фактические результаты](https://github.com/Melchiorius/backend-interview-labs/issues/360#issuecomment-7226882959). Они не входят в общий runner. Вспомогательные методы и типы Probe объявлены вне main; фрагменты main не являются самостоятельными файлами. Номера OP-06/OP-10 в карте вопросов отличаются от подписей строк OP06/OP10 в эталоне.

## 1. Приоритет, ассоциативность и реальный порядок исполнения (OP-01, OP-02, OP-17)

Три разных вопроса:

- **Приоритет** (`precedence`): какая операция к чему относится при разборе выражения. `1 + 2 * 3` группируется как `1 + (2 * 3)` и даёт `7`; `(1 + 2) * 3` даёт `9`.
- **Ассоциативность**: как группируются последовательности операторов равного уровня. Например, `10 - 3 - 2` группируется слева: `(10 - 3) - 2 = 5`. Присваивание связывается справа: `a = b = d` — это `a = (b = d)`.
- **Порядок вычисления**: *когда* читается каждый операнд и выполняется вызов с побочным эффектом. Java гарантирует наблюдаемое вычисление операндов и аргументов **слева направо**, а не «в удобном порядке компилятора» ([JLS §15.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7)).

~~~java
class OrderDemo {
    static int part(StringBuilder out, String label, int value) {
        out.append(label);
        return value;
    }
    static int combine(int a, int b) { return a * 10 + b; }
    static void run() {
        StringBuilder trace = new StringBuilder();
        int result = combine(part(trace, "L", 6), part(trace, "R", 7));
        System.out.println(result + ":" + trace); // 67:LR
    }
}
~~~

Сначала вычисляется аргумент с `L`, затем с `R`; только после этого вызывается `combine(6,7)`. **`67:LR` не результат неопределённого порядка**, а воспроизводимое следствие языка. Ассоциативность не означает, что правый операнд выполнится первым; можно иметь правую группировку и левосторонний порядок наблюдаемых вычислений. Не смешивайте это с передачей примитивных значений и ссылок по значению — отдельная [CORE-PARAMS](parameters.md).

Гарантия относится к наблюдаемым результатам и побочным эффектам, а не к порядку машинных инструкций. При исключении в левом операнде дальнейшее вычисление правого не происходит; при исключении в более раннем аргументе следующие аргументы не вычисляются. В Probe вспомогательный fail() добавляет X к trace и бросает IllegalStateException, mark("Y",1) добавляет Y. Оба фрагмента `fail()+mark("Y",1)` и `sum(fail(),mark("Y",1))` оставили trace X. При обычном instance-вызове сначала вычисляется receiver expression, затем аргументы слева направо, а проверка null получателя происходит после их вычисления: receiver() добавляет R и возвращает null, `receiver().call(mark("A",1),mark("B",2))` оставил RAB и бросил NPE. Это не утверждение о любой форме вызова (static вызов через expression, например, не требует ненулевого receiver). См. [JLS §15.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4). Уже совершённые эффекты не откатываются; JMM из этого не выводится.

## 2. Префикс, постфикс и `x = x++` (OP-03; OP-23 P1)

`x++` изменяет переменную на единицу, но **значение выражения** — старое значение `x`. `++x` тоже увеличивает переменную, но значение выражения уже новое. Поэтому в правой части присваивания может оказаться старое значение, хотя переменная успела измениться.

~~~java
class IncrementDemo {
    static void run() {
        int x = 67;
        int old = x++;               // old=67; x=68
        int now = ++x;               // now=69; x=69
        System.out.println(old + ":" + now + ":" + x); // 67:69:69

        x = 4;
        x = x++;
        System.out.println(x);       // 4, НЕ 5
    }
}
~~~

Во втором фрагменте левая часть `x` обозначает переменную назначения; справа `x++` возвращает `4`, увеличив `x` до `5`; внешнее простое присваивание записывает возвращённое **4** обратно. Это не «ошибка компилятора» и не неустойчивый результат. Контрпример OP-N13 `++(a+b)` отвергается **до выполнения**, потому что результат суммы не обозначает переменную, которой можно присвоить новое значение. Изменять `final int n` посредством `n++` тоже нельзя (OP-N03). [JLS §15.14.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.14.2).

Префиксные и постфиксные ++/-- требуют изменяемой переменной числового типа, в том числе подходящего boxed-типа. При необходимости выполняются unboxing, числовое продвижение, затем сужение и/или boxing обратно в тип переменной. Тип выражения — исходный тип переменной; результат является значением, не переменной. -- симметрично вычитает единицу. В Probe `byte b=127; byte old=b++;` дал old=127, b=-128; `char c=0; char now=--c;` дал now=c=65535; `Integer boxed=67; Integer previous=boxed++;` дал 67 и 68. Вызовы перегруженного type непосредственно с testB++, --testC, testI++ выбрали byte, char, Integer. При `Integer nil=null; nil++;` наблюдался NPE при распаковке, nil осталась null. Никакой гарантии атомарности ++ из этих правил нет. [JLS §15.14.2–3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.14.2), [§15.15.1–2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.15.1).

На собеседовании важно сначала записать **значение выражения** и **новое содержимое переменной** в две разные колонки; только так можно объяснить `x = x++`, а не угадать ответ.

## 3. Простое присваивание, составное `+=` и сохранённое значение слева (OP-04, OP-16, OP-18; OP-23 P1)

В `a += rhs` Java однократно вычисляет адресуемую левую переменную, **сохраняет её старое значение до вычисления rhs** и затем использует старое значение и вычисленный правый операнд при сложении. Это ключевой механизм, который невозможно восстановить простым текстовым «`a = a + rhs`» при наличии побочных эффектов.

~~~java
class AssignmentDemo {
    static void run() {
        int a = 1;
        a += (a = 3);
        System.out.println(a); // 4, не 6

        int x = 1, y = 2, z = 3;
        x = y = z;
        System.out.println(x + ":" + y); // 3:3

        short s = Short.MAX_VALUE;
        s += 1;
        System.out.println(s); // -32768
    }
}
~~~

**Трассировка первого выражения:** (1) левая часть означает переменную `a`, запоминается старое `1`; (2) вычисляется `(a=3)`: переменная теперь содержит `3`, выражение присваивания возвращает `3`; (3) сохранённое `1` складывается с `3` → `4`, и `a` получает `4`. Это конкретно описано [JLS §15.26.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.26.2). Простое `=` и составное `+=` имеют разную семантику чтения левого значения, в особенности с полями/массивами.

**Неявное сужение:** `short s = 32767; s += 1;` эквивалентно операции с явным приведением результата к `short` после вычисления суммы как `int`. Получается `-32768`. Но `s = s + 1` без явного сужения **не компилируется**, поскольку `s + 1` имеет тип `int` (OP-N02). `+=` не означает безопасную арифметику без переполнения. И ещё: `int n; n += 1;` без инициализации — ошибка definite assignment OP-N10, потому что составное присваивание читает **старое значение**.

Формальная эквивалентность `E1 op= E2` — `E1 = (T)((E1) op (E2))`, где T — тип E1, **с исключением, что E1 вычисляется один раз**; это не текстовая подстановка и не обещание допустимости любой комбинации boxing/cast. Например, отдельный BoxedCompound с `Byte b=1; b+=1;` отклонён inconvertible.types int, Byte. При массиве вычисляются array reference и index; если они завершились нормально, в простом присваивании RHS вычисляется до проверок null и границ, в составном — после этих проверок и сохранения старого элемента. В Probe index() и rhs() каждый увеличивают hits, возвращают 0 и 67 соответственно. При absent=null и empty={} простое `absent[index()]=rhs();` / `empty[index()]=rhs();` оставляло hits=2 перед NPE/ArrayIndexOutOfBoundsException, составное += оставляло hits=1. На `int[] a={1}; a[index()]+=rhs();` получены a[0]=68 и hits=2 (один index и один rhs). В случае исключения при вычислении самой ссылки или индекса следующие части не выполняются. [JLS §15.26.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.26.1), [§15.26.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.26.2).

Правоассоциативная запись `a=b=d` показывает, что выражение присваивания само имеет значение: сначала `d` присваивается `b`, затем это значение присваивается `a` (G1 OP25: `3:3`). Для объявлений типов и DA см. [CORE-SYNTAX](syntax.md).

Тип выражения присваивания — тип левой переменной после capture conversion; значение — уже присвоенное значение после преобразования, а не исходный RHS произвольного типа. В Probe `byte dest; int assigned=(dest=67);` дал 67, перегрузка type(dest=67) выбрала byte. Результат не является переменной: (a=b)++ в отдельном NonVariableAssignment отклонён unexpected.type variable/value. Правоассоциативность задаёт дерево, не переставляет вычисление выражений слева; исключение не отменяет предыдущие эффекты.

## 4. Числовые типы, расширение и переполнение (OP-05, OP-07; OP-26 P1)

**Углубление о числовой модели:** [CORE-NUMBERS](numbers.md) отдельно объясняет fixed-width overflow vs `Math.addExact`, promotion, binary IEEE754, BigDecimal constructors, scale/precision/rounding. Эта статья остаётся о семантике самих операторов.

В бинарной арифметике операнды меньших целочисленных типов обычно подвергаются **binary numeric promotion**: например, `byte + byte` и `short + int` вычисляются как минимум в `int`. Последующее присваивание результата обратно в `short` уже отдельный conversion context. [JLS §5.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.6).

~~~java
class PromotionDemo {
    static int promoted(short left, short right) {
        return left + right; // результат int, не short
    }
    static void run() {
        System.out.println(Integer.MAX_VALUE + 1);  // -2147483648
        byte small = true ? 1 : 2; // обе константы помещаются в byte
        System.out.println(small);                // 1
    }
}
~~~

Для обычных бинарных арифметических операторов после необходимого unboxing: если один операнд double — оба double, иначе при float — оба float, иначе при long — оба long, иначе int. Это тип самой операции, а не переменной назначения. В Probe `int n=67000; long late=n*100000, early=(long)n*100000;` дал late=-1889934592 (сначала переполнение int, затем widening) и early=6700000000 (приведение до умножения). Сдвиги и условный оператор имеют свои правила (§6/§8). См. [CORE-CONVERSIONS](conversions.md) и [CORE-NUMBERS](numbers.md).

У `int` конечная фиксированная разрядность; `Integer.MAX_VALUE + 1` **переполняется с переносом по модулю 2^32**, результат `Integer.MIN_VALUE`. Автоматического `ArithmeticException` из-за целочисленного переполнения **нет**. Это не связано с floating NaN и не случайность оптимизатора. Существуют API с явным контролем переполнения, например `Math.addExact`, но они не меняют семантику обычного оператора `+`.

**Константное сужение — отдельное правило:** `byte b = 127;` компилируется, потому что константа представима в `byte`; `byte b = 127 + 1;` отвергается (OP-N15), поскольку результат 128 уже не входит в диапазон. Нельзя объяснять допустимость `byte b = 1;` тем, будто вся арифметика `byte` выполняется в `byte`. В G1 OP18 `true ? 1 : 2` тоже обладает правилами типизации условного выражения, описанными в §8 ниже.

## 5. Целочисленное деление, остаток и деление на ноль (OP-06, OP-20)

Деление двух `int` выполняет **целочисленную** операцию, результат которой усекается к нулю (кроме специального переполнения MIN_VALUE / -1, см. ниже); она не даёт дробное `2.333...`. Ненулевой остаток при ненулевом делителе имеет знак делимого, нулевой остаток равен 0. Поэтому:

~~~java
class DivisionDemo {
    static void run() {
        System.out.println(7 / 3 + ":" + 7 % 3);     // 2:1
        System.out.println(-7 / 3 + ":" + -7 % 3);   // -2:-1
        try {
            int divisor = Integer.parseInt("0");
            int ignored = 67 / divisor;
            System.out.println(ignored);
        } catch (ArithmeticException ex) {
            System.out.println("ArithmeticException");
        }
    }
}
~~~

**OP-06: особый случай переполнения.** Для int и long минимальное представимое число, делённое на -1, даёт то же минимальное число, а не положительное число за границей диапазона, и исключения нет. Остаток в обоих случаях равен 0. В Probe переменные im=Integer.MIN_VALUE, lm=Long.MIN_VALUE, minus=-1: im/minus=-2147483648, lm/minus=-9223372036854775808L, im%minus=0, lm%minus=0. Обычные 7/-3=-2, 7%-3=1, -7/3=-2, -7%3=-1; Math.floorDiv(-7,3)=-3 (округление вниз, не усечение к нулю). Для целочисленных / и % с нулевым делителем ArithmeticException: отдельно выполнены четыре случая int/long. [JLS §15.17.2–3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.17.2), [Math.floorDiv](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html#floorDiv(int,int)). Это обязательный контракт, не вывод из конкретных строк OP06/OP10 runner.

**Ключевой ответ:** даже полный фрагмент `class Zero { int n = 1 / 0; }` **успешно компилируется в Java 21**, и при *вычислении* такого инициализатора возникает `ArithmeticException`. Знак `/` и операция `%` подчиняются соответствующим правилам JLS, а не обязательному округлению вниз. Проверяемый отрицательный случай `case dynamicVariable` относится к **требованию constant expression метки**, а не к запрету `1/0` на этапе компиляции. См. [JLS §15.17](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.17) и §11.

В G1 OP27 при вычислении `side[0]++ / zero` сначала срабатывает `side[0]++`, а только затем обнаруживается нулевой делитель. Поэтому после перехвата исключения программа пишет `OP27=ArithmeticException:1`. Это важно: исключение арифметики не «отменяет» уже совершённый побочный эффект. Деление значений `double` на ноль ведёт себя иначе (бесконечности/NaN); полный курс floating-point вынесен в P2.

## 6. Сдвиги и отбрасывание старших битов расстояния (OP-08; OP-22 P1)

Для сдвига `int` эффективное расстояние определяется **младшими пятью битами** правого операнда (то есть расстояние маскируется как `& 0x1f`); для `long` — **младшими шестью битами** (`& 0x3f`). Поэтому `1 << 33` для `int` ведёт себя как `1 << 1` → `2`, а `1L << 65` как `1L << 1` → `2L`. Это не универсальная «нормализация любого числа бит», а точное правило типов.

~~~java
class ShiftDemo {
    static void run() {
        System.out.println(1 << 33);  // 2
        System.out.println(1L << 65); // 2
        System.out.println(-1 >>> 1); // 2147483647
        System.out.println(-1 >> 1);  // -1
    }
}
~~~

Каждый операнд отдельно проходит unary numeric promotion (не binary promotion), оба результата должны быть целочисленными. Тип и маска сдвига определяются продвинутым типом **левого** операнда, даже если правый long. В Probe `1 << 33L` дал int 2; `1 << -1` использует расстояние 31, даёт Integer.MIN_VALUE; byte one=1, one<<1 имеет тип int (проверено перегрузкой type). При << старшие вытесненные биты теряются: Integer.MIN_VALUE<<1 равно 0. Проверены также 1L<<65=2L, -1>>1=-1 и -1>>>1=Integer.MAX_VALUE. >>> не вводит отдельный unsigned int-тип.

`>>` — сдвиг вправо с сохранением знакового разряда, `>>>` заполняет освободившиеся старшие биты нулями. `boolean << 1` не является операцией над целочисленным типом и не компилируется (OP-N06). `long` результат `1L << 67` нельзя присвоить `int` без сужения (OP-N16) — это уже типовая ошибка, а не «ошибка расстояния сдвига». [JLS §15.19](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.19).

## 7. Короткое замыкание и полное вычисление булевых операндов (OP-09–OP-11)

`&&` выполняет правый операнд **только если левый true**; `||` — **только если левый false**. В отличие от них, булевы `&`, `|` и `^` не пропускают правый операнд по логическому значению левого, **если вычисление и необходимый unboxing левого завершились нормально**. Итоговое булево значение может совпасть, но побочные эффекты будут другими.

~~~java
class BooleanEval {
    static boolean mark(int[] hits, boolean value) {
        hits[0]++;
        return value;
    }
    static void run() {
        int[] hits = {0};
        boolean first = false && mark(hits, true);
        System.out.println(first + ":" + hits[0]); // false:0
        hits[0] = 0;
        boolean second = false & mark(hits, true);
        System.out.println(second + ":" + hits[0]); // false:1
        hits[0] = 0;
        boolean third = true || mark(hits, false);
        System.out.println(third + ":" + hits[0]); // true:0
        hits[0] = 0;
        boolean fourth = true | mark(hits, false);
        System.out.println(fourth + ":" + hits[0]); // true:1
    }
}
~~~

Это тот же механизм, который G1 фиксирует OP12–OP15. Он влияет на проверки `obj != null && obj.isReady()`: благодаря левому условию при `null` метод на объекте не вызывается; заменив `&&` на булев `&`, можно получить `NullPointerException`. Но это не означает, что `&` всегда запрещён: он также определён для целочисленных побитовых операций. Смешивать `boolean` с `int` в выражении `true & 1` нельзя (OP-N07), `int` как условие `if (67)` тоже не допускается (OP-N14). Подробнее [JLS §15.22–15.24](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.22.2).

**OP-10: eager не означает «всегда оба».** Если failBool() бросает исключение слева в `failBool() & hit()`, hit() не выполняется (hits=0). Если Boolean nb=null, то `nb | hit()` и `nb ^ hit()` бросают NPE при распаковке слева, hits также 0. При нормальном левом операнде false & hit(), true | hit(), false ^ hit() вызывают hit() по одному разу; результаты false/true/true. Короткое замыкание false&&nb и true||nb не распаковывает правый null, результаты false/true; аналогичные false&&hit()/true||hit() не вызывают hit(). Условие if(nb) с null бросает NPE, а if(67) вообще не компилируется: целые не имеют truthiness. Все указанные проверки выполнены Probe или отдельным IntCondition. [JLS §15.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7.1), [§15.22.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.22.2), [§14.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.9). См. [управление потоком](control-flow.md).

## 8. Условный оператор `?:` выбирает одну ветвь, но имеет общий тип (OP-12; OP-25 P1)

Условный оператор сначала вычисляет условие типа `boolean` или `Boolean` (с распаковкой), а затем **только выбранный** операнд. Второй, невыбранный, *не выполняется*, но при **компиляции** тип всего выражения определяется с учётом правил типов обеих ветвей.

~~~java
class ConditionalDemo {
    static void run() {
        int[] hits = {0};
        int result = true ? 67 : hits[0]++;
        System.out.println(result + ":" + hits[0]); // 67:0

        Number number = true ? 1 : 2.5;
        System.out.println(number + ":" + number.getClass().getSimpleName()); // 1.0:Double

        byte small = true ? 1 : 2;
        System.out.println(small); // 1
    }
}
~~~

Почему `number` — `Double`, хотя выбрано целое `1`? Числовой conditional expression с ветвями `int` и `double` имеет результирующий тип `double` по правилам [JLS §15.25](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.25); выбранное `1` преобразуется в `1.0`, затем упаковывается при присваивании `Number`. В первом фрагменте правая ветка `hits[0]++` не запускается: это отдельный контроль *вычисления*, не *типизации*.

Boolean и numeric conditional expressions — standalone, их тип не определяется целевой переменной. `true ? 1 : 2` имеет тип int, а `byte small = true ? 1 : 2;` допустим благодаря representable constant narrowing при присваивании. Для `boolean flag; byte v=67; flag ? v : 2` (flag должен быть присвоен) тип byte по специальному правилу numeric conditional. Probe проверил выбор перегрузок type(int/byte/double). Если Integer missing=null и flag=true, `flag ? missing : 67` имеет тип int и при выборе missing бросает NPE. Reference conditional expression в assignment/invocation context — poly expression, обе ветви проверяются с соответствующим target type; вне этих контекстов standalone. В Probe `IntSupplier fn=flag?()->67:()->68;` и `invoke(flag?()->67:()->68)` с перегрузками invoke(IntSupplier)/invoke(String) успешно выбрали функциональный тип, вызовы дали 67; flag=true (запуск без аргументов), ветвь 68 не исполнялась. См. [CORE-FUNCTIONS](functions.md) и [JLS §15.25.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.25.3). Детали контекстных преобразований — [CORE-CONVERSIONS](conversions.md). Контрпример OP-N05 `int n = true ? "x" : 67;` нарушает ожидаемый тип `int`; не путать его с «обе ветви обязательно выполняются».

## 9. Строковый `+`, группировка и допустимые операторы (OP-13, OP-19; OP-21, OP-24 P1)

`+` перегружен **встроенным правилом языка** для конкатенации строк. Это **не** означает, что пользовательский класс может переопределить произвольный оператор `+`. Если операнды до встречи со строкой числовые, выполняется числовое сложение; если хотя бы один операнд данного бинарного + имеет статический тип String, действует строковая конкатенация. Статический, а не runtime-тип определяет этот выбор.

~~~java
class ConcatDemo {
    static void run() {
        System.out.println(1 + 2 + "x"); // 3x
        System.out.println("x" + 1 + 2); // x12
        System.out.println("value=" + (1 + 2)); // value=3
        String ref = null;
        System.out.println("ref=" + ref); // ref=null
    }
}
~~~

При обычной левосторонней группировке первый вариант выполняет числовое `1+2=3` и затем дописывает `"x"`. Второй уже начинает со строки и последовательно дописывает `1` и `2`. Исходные строки при этом не меняются — `String` неизменяем. Обычные выражения `1+2` сами по себе не становятся `String`, поэтому `String s = 1+2;` не компилируется (OP-N11). Унарный `+"x"` недопустим (OP-N04), а `67 + true` тоже недопустимо (OP-N08). [JLS §15.18.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.18.1), [CORE-STRINGS](strings.md).

Два Object с runtime-строками не становятся допустимыми операндами +: ObjectConcat с Object a="a", b="b"; return a+b; отклонён operator.cant.be.applied. При String conversion null даёт "null", boolean преобразуется в "true"/"false", ненулевая ссылка преобразуется по toString(), даже возвращённая им null превращается в строку "null". Probe с Object str="x", obj=new NullText() (toString возвращает null), `""+str+true+null+obj` дал xtruenullnull. [JLS §5.1.11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.11). Не гарантируется lowering в StringBuilder или конкретная аллокация; для повторного накопления в цикле StringBuilder удобен как изменяемый накопитель, но не универсальная гарантия скорости. Подробнее [CORE-STRINGS](strings.md).

Отдельно: сравнение `String` через `==` проверяет **ссылочную идентичность**, а `equals` — равенство по контракту типа. Конкатенация и пул строк не превращают `==` в универсальную проверку значения. См. [CORE-EQUALITY](equality.md).

## 10. `==`, `equals`, NaN и знаковый ноль (OP-14, OP-15; OP-27 P2)

Для числового equality, когда оба операнда имеют примитивные числовые типы или один имеет такой тип, а другой допускает unboxing в числовой тип, применяется binary numeric promotion (с необходимым unboxing); для boolean/Boolean, если хотя бы один boolean, применяется boolean equality с unboxing. Две ссылки сравниваются по reference equality, если допускается casting conversion хотя бы в одном направлении (включая null); true, если обе null или обозначают один объект/массив. Два разных boxed-типа Integer и Long нельзя сравнить напрямую через == (IncompatibleWrappers). Probe проверил Integer wrap=67; wrap==67, Boolean yes=true; yes==true, две null Object ссылки, а Integer missing=null; missing==67 бросило NPE. Это не сравнение адресов памяти; `equals` — вызов метода, поведение которого определяет класс. Контрпример `"x" == 67` отвергается по несовместимым типам (OP-N12); это не ложный `false` при выполнении.

Плавающая точка имеет особые значения, поэтому простая аналогия «`==` означает одинаковые биты» неверна:

~~~java
class FloatingEquality {
    static void run() {
        System.out.println(Double.NaN == Double.NaN); // false
        System.out.println(-0.0 == 0.0);              // true
        System.out.println(Double.compare(-0.0, 0.0)); // -1
    }
}
~~~

Любой `NaN` не равен даже себе через `==`; `-0.0 == 0.0` возвращает `true`, хотя `Double.compare(-0.0,0.0)` упорядочивает эти значения по-разному. `Double.compare` — **конкретный контракт Java API**, не «иная реализация оператора `==`». Упрощать эти две разные функции до одного отношения нельзя. Полный анализ бесконечностей, underflow/subnormal и строгой floating-point семантики — **OP-27 P2, не проверено**. [JLS §15.21](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21), [CORE-EQUALITY](equality.md).

API Double.equals отличается от примитивного ==: два boxed NaN равны, а boxed -0.0 и +0.0 не равны. Probe подтвердил Double.valueOf(Double.NaN).equals(Double.NaN) true и Double.valueOf(-0.0).equals(0.0) false, одновременно primitive NaN != NaN и -0.0==0.0. Это контракт [Double.equals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Double.html#equals(java.lang.Object)), не свойство кэша обёрток. См. [CORE-EQUALITY](equality.md).

## 11. Требования к операндам и граница «константа» / runtime (OP-19, OP-20; OP-26 P1)

Некоторые выражения неверны **по типу** ещё до выполнения. Например, арифметический унарный `+` ожидает числовой тип; сдвиг требует целочисленные операнды; `if` требует `boolean`, а не `int`; типы несовместимы у `String == int`. Поэтому OP-N04, OP-N06–OP-N08, OP-N12, OP-N14 относятся к компиляционным ошибкам. Знание отдельного успешного выражения типа `1 + 2` не доказывает работоспособность `1 + true`.

Карта областей применимости: целочисленные & ^ | после binary numeric promotion выполняют операции над битами, булевы формы — над boolean/Boolean с unboxing, без смешивания двух областей; ~ принимает целочисленный операнд после unary promotion, ! — boolean/Boolean; унарные + и - принимают числовые операнды с unary promotion. `<`, `<=`, `>`, `>=` — числовые сравнения после promotion, а не сравнение произвольных ссылок. ==/!= имеют отдельные правила §10. Практическая маска: `int flags=0; flags|=4;` затем (flags&4)!=0 и (flags^4)==0; последнее выражение вычисляет результат переключения, не присваивает его. Probe также проверил ~0=-1, !Boolean.FALSE=true, тип int для +one/-one (byte one), one<67. Отдельно отклонены MixedBoolean, FloatBitwise, BoolNotInt, ReferenceRelational, UnaryString, IntCondition. [JLS §15.15](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.15), [§15.22](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.22).

Другой случай — метка `case` в соответствующем `switch`: для числовой/строковой constant-label формы требуется подходящее *константное выражение*; enum-константы, pattern labels и case null имеют отдельные правила Java 21, не сводятся к этому требованию. Поэтому `int key = 67; case key -> ...` с обычной изменяемой локальной не допускается (OP-N09). Для примеров и Java21 switch-expression см. [CORE-CONTROL](control-flow.md), [JLS §15.29](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.29).

Constant expression по JLS §15.29 — выражение примитивного типа или String из разрешённых форм, которое не завершается резко; значение, известное человеку при выполнении, ещё не константное выражение. Constant variable — final переменная примитивного типа или String, инициализированная constant expression. В контексте присваивания представимое constant expression типа byte/short/char/int допускает narrowing в byte/short/char, в том числе narrowing с последующим boxing в Byte/Short/Character; не long и не произвольное runtime-значение. В Probe final int k=67; Byte narrow=k; byte w=k; switch(k) с case k — допустимы, результат ok. ConstLong (byte b=67L), ConstNonFinal (byte b=n при int n=67), ConstInvoke (вызов f(byte) с 67 без cast) отклонены: invocation context не имеет этого constant narrowing. ConstZeroCase (case 1/0) отклонён const.expr.req, поскольку 1/0 не constant expression. См. [JLS §5.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2), [§4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4), [§14.11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11).

**Неожиданный контраст:** `int n = 1 / 0;` — **не запрещённый синтаксис, не invalid constant-expression program, не compile-negative**. JLS допускает компиляцию соответствующей целочисленной операции, а если это выражение действительно вычисляется, оно бросает `ArithmeticException`. [G1 §7](../../docs/plans/CORE-SYNTAX-OPERATORS.md) отдельно подтверждает успешную компиляцию полноценного файла `class CheckDivisionZeroCompile { int n=1/0; }`. Вывод «всё, что содержит `1/0`, нельзя скомпилировать» неверен. Для случая OP27 (с побочным эффектом в делимом) см. §5. Не включайте OP27 в счётчик 33 compile-negative.

Отдельный временный Zero.java с main и int n=1/0 скомпилирован с --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all: exit 0, stderr содержит compiler.warn.div.zero, затем JVM exit 1 с ArithmeticException. Предупреждение -Xlint не является ошибкой языка; -Werror намеренно не применялся к этому примеру.

## 12. Что находится за границей P0/P1 (OP-27–OP-30 P2)

- **OP-27 P2:** подробная арифметика `NaN`, Infinity, overflow/underflow для floating point, subnormal и строгие гарантии — **не реализован полный набор опытов**.
- **OP-28 P2:** Java Memory Model, межпоточная видимость, `happens-before`, `volatile`, reorderings — **не** следуют из порядка вычисления JLS §15.7 в одном потоке. Это самостоятельная [область concurrency](../03-concurrency/semaphore.md) с более узким имеющимся материалом; весь JMM не закрыт.
- **OP-29 P2:** bytecode/JIT/GC/JMH, оптимизация и измерение производительности — **NOT RUN**. Логическая семантика языка не требует конкретной машинной последовательности инструкций, если наблюдаемое поведение соблюдено.
- **OP-30 P2:** исчерпывающие побочные эффекты сложных `array access`, boxing/unboxing NPE и большие expression chains — исчерпывающее исследование отложено; отдельные граничные сценарии §2/§3/§7/§8 проверены, но не закрывают этот P2.

Даже успешный CI конкретных трёх программ не подтвердил бы эти P2 автоматически.

## 13. Все 27 строк `OperatorsProbe`: что реально подтверждено G1

Полный проверенный исходник и эталон приведены в [принятом G1 §6.2](../../docs/plans/CORE-SYNTAX-OPERATORS.md), опубликованы как [Java-файл](../../examples/core/syntax-operators/src/OperatorsProbe.java), [эталон](../../examples/core/syntax-operators/operators.expected.stdout) и [README runnable](../../examples/core/syntax-operators/README.md). Следующая таблица — **ожидаемые результаты именно этих исходников** на JDK 21; исходные исследования 01 и проверка 04 локально выполнили `javac --release 21 -Xlint:all`, запуск Java и `diff -u`. Встроенные выше отдельные фрагменты не выдаются за самостоятельные исполненные тесты.

| Строка | Точный результат | Механизм |
|---|---|---|
| OP01 / OP02 | `7` / `9` | приоритет `*` и круглые скобки (§1) |
| OP03 | `67:68` | postincrement: old vs current (§2) |
| OP04 | `69:69` | preincrement: new vs current (§2) |
| OP05 | `4` | `x=x++` возвращает старое (§2) |
| OP06 | `-32768` | `short += 1` и narrowing (§3) |
| OP07 | `-2147483648` | переполнение `int` (§4) |
| OP08 | `-2:-1` | деление с усечением к нулю и знак остатка (§5) |
| OP09 / OP11 | `2` / `2` | masked shift distances `int`/`long` (§6) |
| OP10 | `2147483647` | unsigned right shift `-1 >>> 1` (§6) |
| OP12 / OP13 | `false:0` / `false:1` | короткое замыкание `&&` vs полное `&` (§7) |
| OP14 / OP15 | `true:0` / `true:1` | короткое `||` vs полное `|` (§7) |
| OP16 / OP17 | `3x` / `x12` | группировка и строковое сложение (§9) |
| OP18 | `1` | conditional для представимой `byte` константы (§8) |
| OP19 | `1.0:Double` | `int/double` conditional, boxing (§8) |
| OP20 | `4` | сохранённое `a` при `a+=(a=3)` (§3) |
| OP21 | `67:LR` | порядок аргументов слева направо (§1) |
| OP22 | `false` | `NaN == NaN` (§10) |
| OP23 | `-1` | `Double.compare(-0.0,0.0)` (§10) |
| OP24 | `67:0` | невыбранная ветвь `?:` не выполняется (§8) |
| OP25 | `3:3` | правоассоциативное присваивание (§3) |
| OP26 | `2:1` | целочисленные деление/остаток (§5) |
| OP27 | `ArithmeticException:1` | деление на ноль **при выполнении** после postincrement (§5) |

**44 stdout-строки полного G1** — 14 `SY`, 27 `OP` и 3 `PK` из трёх JVM-процессов. **Это 44 наблюдения, не 44 независимых unit/JUnit-теста.** Особо важно не заменять `SY-N15` двухфайловую отрицательную проверку одним неполным фрагментом — это привело бы к другому классу ошибки.

### Самопроверка: смешанный пример и перенос

В Probe проверено: `int x=6; byte y=7; int res=x++ + (y+=(byte)x);`. Сначала x++ даёт 6, x=7. Составное присваивание сохраняет y=7, вычисляет (byte)x=7, складывает как int и сужает до byte 14. Значение присваивания 14, после продвижения при внешнем сложении res=20. Итог x=7, y=14. Для реального кода предпочтительны отдельные выражения: они яснее показывают эффекты.

Предскажите самостоятельно для двух независимых фрагментов: (1) `int x=6; byte y=7; int res=++x + (y+=(byte)x);` — res, x, y; (2) `Boolean b=null; int[] n={0}; boolean r=false && (b | (++n[0]>0));` — будет ли исключение, чему равны r и n[0]?

### Ответы к самопроверке

(1) ++x даёт 7, y становится 14, res=21; x=7. (2) false && не вычисляет всю правую часть, поэтому b не распаковывается, исключения нет, r=false, n[0]=0. Это задачи на перенос правил; эти два точных фрагмента отдельно не запускались.

## 14. Карта 30/30 вопросов OP-01–OP-30

Приоритет P0 означает «подробно объяснять», а не уже пройденный независимый G3/G4; семь P2 двух новых статей не становятся Implemented лишь потому, что есть соседние статьи.

| ID | P | Ключевой механизм и адресная проверка |
|---|---|---|
| OP-01 | P0 | §1: precedence grouping, OP01/02 |
| OP-02 | P0 | §1: association ≠ execution order, OP21, OP25 |
| OP-03 | P0 | §2: postfix/prefix и `x=x++`, OP03–05 |
| OP-04 | P0 | §3: implicit narrowing `short+=1`, OP06 vs OP-N02 |
| OP-05 | P0 | §4: integer overflow wrap, OP07 |
| OP-06 | P0 | §5: MIN/-1 для int/long, остаток и zero runtime, OP08/26/27 |
| OP-07 | P0 | §4, §8: numeric promotion, OP18/19, OP-N01/02 |
| OP-08 | P0 | §6: `int` 5/`long` 6 mask bits, OP09–11 |
| OP-09 | P0 | §7: `&&` skip rhs, OP12 |
| OP-10 | P0 | §7: булевы eager-операторы после нормального вычисления и распаковки слева, OP13/15 |
| OP-11 | P0 | §7: `||` skip rhs, OP14 |
| OP-12 | P0 | §8: selected branch only, typing both branches, OP18/19/24 |
| OP-13 | P0 | §9: String `+` grouping, OP16/17 |
| OP-14 | P0 | §10: `==` numeric/reference semantics vs equals, OP22, OP-N12 |
| OP-15 | P0 | §10: NaN vs signed-zero compare, OP22/23 |
| OP-16 | P0 | §3: saved LHS before RHS side effects, OP20 |
| OP-17 | P0 | §1: Java left-to-right arguments OP21 |
| OP-18 | P0 | §3: value/right association of `=`, OP25; unassigned compound OP-N10 |
| OP-19 | P0 | §9, §11: type restrictions unary/boolean/shifts OP-N04/06–08/14 |
| OP-20 | P0 | §5, §11: constant label OP-N09 vs runtime `1/0`/OP27 |
| OP-21 | P1 | §9: no custom overloaded operators |
| OP-22 | P1 | §6: shift masking, arithmetic vs logical right shift |
| OP-23 | P1 | §2, §3: ++ / compound traps, OP05/20 |
| OP-24 | P1 | §9: String conversion `null` vs StringBuilder, ссылка |
| OP-25 | P1 | §8: conditional numeric typing vs poly expressions |
| OP-26 | P1 | §4: representable byte constant vs OP-N15 |
| OP-27 | P2 | §10, §12: floating extremes NOT RUN |
| OP-28 | P2 | §12: JMM/reordering/concurrency NOT RUN |
| OP-29 | P2 | §12: JIT/JMH/GC/performance NOT RUN |
| OP-30 | P2 | §12: array side effects/boxing long chains NOT RUN |

## Компиляционные ошибки и фактические границы

В исследовании G1 §7 отдельно сохранены **16 полных OP source файлов**, каждый компилировался **изолированно**, а ошибка сверялась по тематическому `javac -XDrawDiagnostics` ключу, не просто ненулевому exit code. Таблица для самостоятельной проверки:

| Отрицательный случай | Причина |
|---|---|
| OP-N01 / OP-N02 | long нельзя присвоить int без сужения / `short = short+1` возвращает int |
| OP-N03 / OP-N13 | increment у `final` / increment у не-переменной |
| OP-N04 / OP-N08 | унарный `+` над String / `int + boolean` |
| OP-N05 | несовместимое значение `?:` для целевой переменной |
| OP-N06 / OP-N07 | shift boolean / boolean `&` с int |
| OP-N09 / OP-N10 | не-константный `case` / чтение неинициализированного `n+=1` |
| OP-N11 / OP-N12 | `String s=1+2` / сравнение `String == int` |
| OP-N14 / OP-N15 / OP-N16 | `if(67)` / константа byte=128 / long result в int |

Вместе с **16 SY single-source** и **одним полноценным two-file SY-N15** это **33/33**, проверенные локально в G1/04, **не** стандартный Java CI репозитория. `int n=1/0` является *положительной компиляционной пробой*, а при выполнении выражения ведёт к `ArithmeticException`; не превращайте его в «34-й compiler-negative».

**Нормативные источники:** [JLS Java21 §5.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.6), [JLS §15.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7), [JLS §15.17–15.19](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.17), [JLS §15.21–15.26](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21), [JLS §15.29](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.29). Интервью-банки C1/C2, зафиксированные в [G1 §2](../../docs/plans/CORE-SYNTAX-OPERATORS.md), используются только как перечень реальных вопросов, не как нормативные доказательства. Строки `OP` и `SY/PK` были получены исследователем и независимо повторно проверены 04 на OpenJDK 21.0.12.1 **вне проектного CI**. Авторская статья сама по себе не компилирует inline-фрагменты. [CI #45006826872](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45006826872) проверил технический HEAD `9d377ccccfec54153e2ee62a49ee9198b45ba120`, [CI #45007263735](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45007263735) — интегрированный author HEAD `aa668e5ac39a4b5b320f13ffd135e38b7fbae8f1`, а [CI #45007640817](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45007640817) завершился **SUCCESS** на следующем авторском HEAD `b898bfae718844f394d56401c1e9f0637b575798`. В каждом случае CI выполнял четыре committed positive Java source, три JVM-процесса и сверку 14+27+3 stdout-строк; 33 compiler-negative и все отдельные inline snippets не входят в эту проверку, а её успех не означает G3/G4/G5 PASS. [PR #156](https://github.com/Melchiorius/backend-interview-labs/pull/156) — историческая публикация; результаты нового SHA фиксируются в соответствующем PR, не наследуются от прежних CI.

**NOT RUN / не выводить из этих проверок:** произвольные inline Java-фрагменты как отдельные программы, 33 compile-negatives в CI, P2/JMM/JPMS/classloading/JIT/GC/JMH, разные версии JDK/ОС, Obsidian/IntelliJ GUI или anchors. Актуальная навигация: [COVERAGE](../../docs/COVERAGE.md) и [CURRICULUM](../../docs/CURRICULUM.md); эта статья сама по себе не подтверждает итоговую приёмку.

**Отдельный синтаксис после Java-строки:** [CORE-REGEX](regex.md) показывает, как Java string literal превращается в аргумент `Pattern.compile`, почему `"\\d+"` и недопустимое `"\d+"` различаются на стадии компиляции и чем это отличается от runtime regex-ошибки.

**Новые фактические проверки 02.** [Временные исходники, команды, stdout/stderr, диагностики и хеши](https://github.com/Melchiorius/backend-interview-labs/issues/360#issuecomment-7226882959) на Debian OpenJDK/javac 21.0.12.1: итоговый Probe с --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror compile exit 0, stdout/stderr пусты, JVM без аргументов exit 0, stderr пуст, 32 строки/537 байт с assertion labels. Значения в разборе — проверенные условия этих assertions, не дословные строки вывода. Первоначальная попытка y+=x (int→byte) вызвала предупреждение possible.loss.of.precision, -Werror превратил его в failure; class-not-found после неё не считается самостоятельной runtime-пробой. После явного (byte)x временный пример успешно скомпилирован и исполнен. 14 изолированных отрицательных компиляций exit 1/stdout пуст; Zero compile exit 0 с div.zero warning, JVM exit 1 ArithmeticException. Исторические 33 отрицательных G1, новые 14 и 24 ожидаемых отклонения всего CI — разные наборы. Не проверялись все ветви Probe или все Markdown-фрагменты.

Отдельно полный неизменённый runner на восьми восстановленных входах с проверенными Git blob SHA/mode (не полный клон), default POSIX и LC_ALL=C.utf8 с временной обёрткой diff, сохраняющей actual.stdout до очистки и делегирующей неизменённые аргументы /usr/bin/diff. Оба exit 0, stderr пуст, инвентарь до/после совпал. Одна компиляция четырёх Java-исходников с --release 21 -Xlint:all, отказ при stderr компилятора, три JVM с проверкой stderr, три diff -u и очистка trap. Во втором запуске сохранены SY14/130, OP27/295, PK3/29 байт, все три cmp с fixture exit 0; SHA-256 в приложении. Внешний stdout обоих запусков — одна строка/91 байт, SHA-256 f70c4dd9462d21b082a7720352501eab01528985d36f2dfcad9a567f63c9275a: `CHECK PASSED: Java 21 syntax/operators/package; 14+27+3 stdout lines (three JVM processes)`. Это 44 наблюдения, не 44 unit-теста и не печать 44 строк. В скрипте нет Werror или явного encoding. OS locales C/C.utf8/POSIX, фактический charset JVM runner не измерялся; альтернативные JVM/OS, полный clone, GUI, JMM/JIT/performance не проверялись. Все исходники, README, эталоны и syntax.md сохранены.
