# CORE-PARAMS — как Java передаёт аргументы в методы

**Java 21 · предпосылки:** [CORE-TYPES](types.md), [CORE-CONVERSIONS](conversions.md)

## Что именно получает метод

Допустим, один метод вызывает другой и передаёт ему объект с настройками. Сможет ли вызываемый метод заменить объект, на который ссылается переменная вызывающего кода? А изменить настройки внутри этого объекта?

Чтобы ответить, нужно различать **аргумент** и **параметр**:

- **Аргумент** — выражение в месте вызова метода, например `save(config)`. Перед выполнением метода это выражение вычисляется.
- **Формальный параметр** — переменная в объявлении метода, например `void save(Config received)`. При вызове ей присваивается значение аргумента с необходимыми допустимыми преобразованиями типов.

**Java всегда передаёт аргументы по значению (*pass-by-value*).** Если аргумент содержит число, копируется число. Если аргумент содержит ссылку на объект, копируется **значение ссылки**, а не объект целиком. Никакой возможности напрямую переназначить исходную переменную вызывающего кода через параметр это не даёт.

**О фрагментах кода:** примеры в разделах независимы друг от друга. Для каждого показан только существенный участок; объявления членов (методов, полей и вспомогательного вложенного класса) следует поместить в класс-обёртку, а выполняемые строки — в метод. В разделе «Практика» находится самостоятельная демонстрация основных механизмов, но не всех контрпримеров статьи.

## Примитив: переменная вызывающего кода остаётся прежней

```java
static void change(int number) {
    number = 100;
}

int value = 67;
change(value);
System.out.println(value); // 67
```

Параметр `number` — самостоятельная переменная вызванного метода. Присваивание `number = 100` меняет только её; переменная `value` вызывающего кода по-прежнему равна `67`.

Это же правило действует для `boolean`, `char`, `double` и других примитивных типов — не только для `int`.

## Ссылка: два имени одного объекта

```java
static void append(StringBuilder target) {
    target.append("B");
}

StringBuilder text = new StringBuilder("A");
append(text);
System.out.println(text); // AB
```

В начале вызова и `text`, и `target` содержат ссылки на **один и тот же объект**. Вызов `target.append("B")` изменяет объект, поэтому через `text` виден результат.

Но **переназначение параметра** не меняет ссылку в переменной вызывающего кода:

```java
static void replace(StringBuilder target) {
    target = new StringBuilder("C");
    target.append("D");
}

StringBuilder text = new StringBuilder("A");
replace(text);
System.out.println(text); // A
```

После `target = new ...` только параметр `target` указывает на новый объект. Переменная `text` осталась без изменений. Это главный аргумент против распространённого, но ошибочного выражения «объекты в Java передаются по ссылке»: **по значению передаются сами ссылки**.

### Почему функция swap не переставляет ссылки у вызывающего кода

```java
static void swap(StringBuilder left, StringBuilder right) {
    StringBuilder temp = left;
    left = right;
    right = temp;
}

StringBuilder first = new StringBuilder("A");
StringBuilder second = new StringBuilder("B");
swap(first, second);
System.out.println(first);  // A
System.out.println(second); // B
```

Метод поменял местами только свои **локальные параметры**, а не две переменные `first` и `second`. Для замены значений вызывающему коду нужен явный результат метода либо изменение согласованного контейнера/структуры данных.

### Два параметра могут ссылаться на один объект

Копирование ссылки не гарантирует, что два параметра обозначают разные объекты. Если передать один объект дважды, изменение через один параметр сразу видно через другой в том же потоке:

```java
static void aliases(StringBuilder a, StringBuilder b) {
    a.append("B");
    System.out.println(a == b); // true
    System.out.println(b);     // AB
    a = new StringBuilder("C");
    b.append("D");
}

StringBuilder value = new StringBuilder("A");
aliases(value, value);
System.out.println(value); // ABD
```

После переназначения `a` только эта локальная переменная указывает на другой объект; `b` и `value` всё ещё указывают на исходный. Для обмена элементами общего контейнера, в отличие от `swap(first, second)`, можно менять его ячейки:

```java
static void swapSlots(StringBuilder[] slots) {
    StringBuilder tmp = slots[0];
    slots[0] = slots[1];
    slots[1] = tmp;
}

StringBuilder first = new StringBuilder("A");
StringBuilder second = new StringBuilder("B");
StringBuilder[] slots = {first, second};
swapSlots(slots);
System.out.println(slots[0] == second); // true
System.out.println(first);              // A: сама переменная first не изменена
```

Метод изменил содержимое массива, а не локальные переменные вызывающего кода. Это пример изменения общей структуры данных, а не особого способа передачи аргументов.

## Массивы тоже передаются по значению

`int[]` — **ссылочный тип**, хотя его элементы имеют тип `int`. Поэтому массив подчиняется ровно тем же правилам, что и любой другой объект:

```java
static void changeElement(int[] numbers) {
    numbers[0] = 67;
}

static void replaceArray(int[] numbers) {
    numbers = new int[] {100};
}

int[] values = {1, 2};
changeElement(values);
System.out.println(values[0]); // 67: изменён общий массив

replaceArray(values);
System.out.println(values[0]); // 67: переменная values не переназначена
```

Путаница часто возникает из-за фразы «массив передаётся по ссылке». Точнее: **передаётся копия ссылки на общий массив**. Устройство массивов, ковариантность и копирование разобраны в [CORE-ARRAYS](arrays.md).

## Неизменяемые String и Integer: объект не изменился

Сравните изменяемый `StringBuilder` с `String`:

```java
static void changeText(String value) {
    value += "B";
}

String original = "A";
changeText(original);
System.out.println(original); // A
```

Объекты `String` неизменяемы. Оператор `+=` вычисляет другую строку и присваивает **её ссылку локальному параметру** `value`. Переменная `original` осталась прежней.

```java
static void increase(Integer number) {
    number++;
}

Integer original = 67;
increase(original);
System.out.println(original); // 67
```

Объекты `Integer` тоже неизменяемы. При `number++` происходит **распаковка**, увеличение числа на единицу и **повторная упаковка** результата; полученная ссылка присваивается только локальному параметру `number`. Сам объект `Integer` не изменяется, ссылка в переменной вызывающего кода остаётся прежней. Повторная упаковка не обязательно создаёт новый объект: подробности кэширования — в [CORE-CONVERSIONS](conversions.md).

Важно различать **неизменяемость объекта** и **запрет менять ссылку**: `final StringBuilder` остаётся изменяемым объектом, тогда как `String` не становится изменяемым из-за отсутствия `final` у переменной. Объяснение `final` — в [CORE-TYPES](types.md); String Pool и кэширование обёрток — в [CORE-CONVERSIONS](conversions.md).

## Что происходит с null

`null` — допустимое значение аргумента, если параметр имеет подходящий **ссылочный тип**. Само копирование `null` не выбрасывает исключение.

```java
static void replaceNull(StringBuilder target) {
    target = new StringBuilder("N"); // допустимо, даже если аргумент null
}

static void append(StringBuilder target) {
    target.append("N"); // здесь через null будет NullPointerException
}

replaceNull(null);
// append(null); // при выполнении метода выбросит NullPointerException
```

Нужно смотреть, **на каком этапе возникает проблема**. Проверка `target == null` не вызывает исключение. В `append(null)` тело уже началось, а исключение возникает при вызове метода экземпляра `target.append("N")` через `null`.

### Распаковка может завершить вызов до входа в метод

```java
static void primitive(int number) {
    System.out.println("body");
}

Integer missing = null;
try {
    primitive(missing); // требуется unboxing Integer → int
} catch (NullPointerException e) {
    System.out.println("unbox NPE");
}
```

Будет напечатано только `unbox NPE`: преобразование аргумента в `int` завершается исключением до входа в `primitive`. Это отличается от передачи `null` параметру ссылочного типа и от обращения к объекту внутри тела. Основания: [JLS §5.1.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.8), [§5.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.3), [§15.12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4.2).

Если `null` — сохранённая ссылка на получателя вызова метода экземпляра, проверка на `null` происходит **после нормального вычисления аргументов**. Исключение при вычислении аргумента может возникнуть раньше и не дать дойти до проверки получателя. Разбор с трассировкой — в разделе о порядке вычисления аргументов ниже.

### Неоднозначность перегрузки — ошибка компиляции

```java
static void m(String value) { System.out.println("String"); }
static void m(Integer value) { System.out.println("Integer"); }

// m(null);          // ошибка компиляции: неоднозначная перегрузка
m((String) null);     // String
m((Integer) null);    // Integer
```

В этом наборе перегрузок литерал `null` подходит обоим параметрам, но ни один из этих двух типов не является более специфичным по отношению к другому. Компилятор не выбирает метод; никакого выполнения и `NullPointerException` у неоднозначного вызова нет. Явное приведение задаёт тип аргумента и разрешает неоднозначность. Если же типы параметров связаны наследованием, результат может быть другим: см. [CORE-CONVERSIONS](conversions.md), [JLS §15.12.2.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2.5).

## Varargs: за многоточием скрывается массив

Объявление `int... values` позволяет вызывать метод с переменным числом аргументов. **Внутри метода параметр имеет тип `int[]`**.

```java
static void changeFirst(int... numbers) {
    if (numbers.length > 0) numbers[0] = 67;
}

changeFirst(1, 2);                 // для этого вызова создаётся массив
int[] input = {1, 2};
changeFirst(input);                // передана ссылка на существующий массив
System.out.println(input[0]);     // 67: изменён массив вызывающего кода
```

Таким образом, `varargs` не вводит «передачу по ссылке»: **в параметр копируется значение ссылки на массив**, а не сам массив. Разница в том, откуда массив взялся. При вызове с перечисленными элементами компилятор организует массив аргументов; при вызове с явно переданным подходящим массивом метод использует его.

В списке формальных параметров разрешён только один параметр с многоточием, и он должен быть последним. `m(int...)` и `m(int[])` имеют одну сигнатуру, поэтому объявить их как две перегрузки нельзя ([JLS §8.4.1–8.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.1)). Вызов `changeFirst()` создаёт пустой массив. `changeFirst((int[]) null)` передаёт `null` вместо массива; в приведённом теле обращение к `numbers.length` приведёт к NPE. Массив не создаётся автоматически для каждого вызова: он создаётся при вызове в режиме переменной арности, а подходящий массив при вызове с фиксированной арностью передаётся без такого создания.

### `Object...`: пустой массив, `null` и массив как один элемент

```java
static void shape(Object... values) {
    if (values == null) {
        System.out.println("null-array");
    } else {
        System.out.println(values.length + ":" + (values.length == 0
                ? "empty" : values[0] == null ? "null-element"
                : values[0].getClass().getSimpleName()));
    }
}

shape();                           // 0:empty
shape((Object[]) null);             // null-array
shape((Object) null);               // 1:null-element
shape(new int[]{67, 68});            // 1:int[]
String[] strings = {"A", "B"};
shape((Object[]) strings);           // 2:String
shape((Object) strings);             // 1:String[]
// shape(null);                     // javac 21: предупреждение; null-array
```

При `shape((Object[]) null)` параметр равен `null`; при `shape((Object) null)` создаётся массив из одного элемента `null`. Вызов без приведения `shape(null)` применим с фиксированной арностью и передаёт `null` как массив; `javac` 21 выдаёт предупреждение о неточном вызове varargs (`inexact.non-varargs.call`). При `-Werror` предупреждение препятствует успешной компиляции; это не неоднозначность двух перегрузок. Для ясности используйте явное приведение.

`int[]` является объектом, но не подтипом `Object[]`, поэтому `shape(new int[]{67, 68})` получает один элемент — сам массив `int[]`. Напротив, `String[]` можно передать как `Object[]` в силу ковариантности массивов; изменение элемента в теле может привести к `ArrayStoreException`, если записываемое значение несовместимо с фактическим типом массива. См. [CORE-ARRAYS](arrays.md).

Обобщённые методы и границы параметров типов разобраны в [CORE-GENERICS](generics.md), а `@SafeVarargs`, стирание и предупреждения компилятора — в [CORE-ERASURE](erasure.md). Фазы выбора перегрузки (в том числе применение varargs-метода с фиксированной арностью раньше режима переменной арности) разобраны в [CORE-CONVERSIONS](conversions.md). Нормативная основа: [JLS §15.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2), [§15.12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4.2).

## В каком порядке вычисляются аргументы

Java вычисляет аргументы **слева направо**, до входа в тело вызываемого метода. Это гарантированное правило JLS, а не наблюдение за конкретной JVM.

```java
static void pair(int first, int second) {
    System.out.println(first + ", " + second);
}

int counter = 0;
pair(counter++, counter++); // печатает 0, 1; теперь counter == 2
```

В этом статическом вызове сначала вычисляется первый аргумент (его значение 0), затем второй (значение 1). Лишь после этого управление переходит в `pair`.

### Ссылка на получателя вызова вычисляется раньше аргументов

Для обычного вызова метода экземпляра через выражение сначала вычисляется выражение, определяющее получателя вызова (receiver), и сохраняется полученная ссылка, затем аргументы вычисляются слева направо с необходимыми преобразованиями. Если вычисление выражения-получателя завершилось исключением, аргументы не вычисляются; если ранний аргумент завершился исключением, следующие аргументы и тело пропускаются. Уже произошедшие побочные эффекты при этом не отменяются.

```java
static class Target {
    final String name;
    Target(String name) { this.name = name; }
    void call(int a, int b) { trace.append(name); }
}
static Target receiver;
static StringBuilder trace = new StringBuilder();
static Target target() { trace.append("R"); return receiver; }
static int switchReceiver() {
    trace.append("1"); receiver = new Target("B"); return 67;
}
static int later() { trace.append("2"); return 68; }
static int fail() { trace.append("!"); throw new IllegalStateException(); }

receiver = new Target("A");
target().call(switchReceiver(), later());
System.out.println(trace); // R12A

receiver = null;
trace.setLength(0);
try {
    target().call(later(), later());
} catch (NullPointerException e) {
    System.out.println(trace + ":NPE"); // R22:NPE
}

trace.setLength(0);
try {
    target().call(fail(), later());
} catch (IllegalStateException e) {
    System.out.println(trace + ":ISE"); // R!:ISE
}
```

- `R12A`: сначала сохраняется ссылка на объект `A` (`R`), затем аргумент заменяет поле `receiver` ссылкой на `B` (`1`), вычисляется второй аргумент (`2`), и метод вызывается у исходного `A`.
- `R22:NPE`: вычисление получателя нормально завершилось значением `null`; оба аргумента вычислены, только затем вызов метода экземпляра приводит к NPE. Тело не выполняется.
- `R!:ISE`: первый аргумент дописывает `!` и выбрасывает `IllegalStateException`; второй аргумент и тело не выполняются. Исключение аргумента опережает NPE от получателя, а уже записанная трасса сохраняется.

Это порядок наблюдаемого поведения Java, а не утверждение о физическом порядке всех машинных инструкций или запрет оптимизаций. Для статического метода объект-получатель не нужен: если статический метод вызван через выражение, его результат отбрасывается, а `null` сам по себе не вызывает NPE. Не следует писать статические вызовы через объект. См. [JLS §15.7.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7.4), [§15.12.4.1–4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4) и [CORE-OPERATORS](operators.md).

## Как выбирается перегрузка: объявленный тип или реальный класс?

```java
static String describe(Object value) { return "Object"; }
static String describe(String value) { return "String"; }

Object argument = "Java";
System.out.println(describe(argument)); // Object
System.out.println(describe("Java"));   // String
```

**Перегрузку** выбирает компилятор. Он определяет тип, в котором ищет метод, по форме вызова и типу получателя, проверяет применимость кандидатов по выражениям аргументов, допустимым преобразованиям и, где нужно, выводу типов. Фактический класс объекта, переданного в качестве аргумента, не участвует в выборе перегрузки. Здесь `argument` имеет статический тип `Object`.

**Переопределение** — другой этап: после выбора сигнатуры при обычном виртуальном или интерфейсном вызове реализация может определяться фактическим классом объекта-получателя. Например:

```java
class Base {
    String describe(Object value) { return "Base:Object"; }
}
class Child extends Base {
    @Override String describe(Object value) { return "Child:Object"; }
    String describe(String value) { return "Child:String"; }
}

Base receiver = new Child();
Object argument = "Java";
System.out.println(receiver.describe(argument)); // Child:Object
System.out.println(receiver.describe("Java"));   // Child:Object
System.out.println(((Child) receiver).describe("Java")); // Child:String
```

В первых двух вызовах компилятор видит `Base` и выбирает `describe(Object)`, затем переопределённый метод выполняется у объекта `Child`. В третьем вызове приведение меняет статический тип получателя, поэтому становится доступна более специфичная перегрузка `describe(String)`. Статические методы скрываются, а не переопределяются; закрытый (`private`) метод не переопределяется; вызов через `super` не выполняет обычный виртуальный выбор переопределения в текущем подклассе. Подробности — [CORE-INHERITANCE](inheritance.md), [CORE-CLASSES](classes.md), [JLS §15.12.1–2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.1), [§15.12.4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4.4).

Выбор перегрузки, widening/boxing и ситуация `m(null)` разобраны в [CORE-CONVERSIONS](conversions.md).

## Как действительно изменить значение вызывающего кода

Если цель — обновить число или заменить объект, вернуть новое значение явно обычно яснее, чем надеяться на изменение параметра:

```java
static int increment(int number) {
    return number + 1;
}

int count = 67;
count = increment(count); // теперь 68
```

Когда метод **намеренно изменяет общий объект**, важно документировать, какие данные он меняет, сохраняет ли ссылку и кто вправе изменять объект после вызова. Если входные данные должны остаться нетронутыми, можно создать защитную копию и явно вернуть результат:

```java
static int[] changedCopy(int[] source) {
    int[] copy = source.clone();
    copy[0] = 67; // контракт: source не пуст
    return copy;
}

int[] original = {1, 2};
int[] result = changedCopy(original);
System.out.println(original[0]); // 1
System.out.println(result[0]);   // 67
```

Для `changedCopy` требуется ненулевая ссылка `source` на непустой массив. Для массива примитивов такой копии достаточно, чтобы изменения его элементов не затронули исходный массив, но копирование массива ссылок создаёт новый контейнер с теми же ссылками на элементы. Защитное копирование должно соответствовать требуемой независимости состояния; возвращаемая ссылка сама по себе не гарантирует владение объектом. См. [CORE-ARRAYS](arrays.md), [CORE-IMMUTABLE](immutability.md) и [CORE-COMPOSITION](composition.md), а также [модель полей и методов класса](classes.md).

### Может ли final защитить переданный объект?

```java
static void append(final StringBuilder target) {
    target.append("B");             // допустимо
    // target = new StringBuilder("C"); // ошибка компиляции
}
```

`final` у параметра запрещает **повторное присваивание значения параметру**, но не изменение объекта. Локальная переменная или параметр, захватываемые лямбдой, должны быть объявлены `final` либо удовлетворять правилам effectively final (фактически финальной переменной): существенны присваивания самой переменной, а не изменение состояния объекта по её ссылке. Подробный разбор дан в [CORE-FUNCTIONS](functions.md).

> [!note]- Углубление: вызовы и память JVM
> Параметры существуют отдельно для конкретных вызовов, но из этого **не следует**, что «все примитивы всегда лежат в стеке, а все объекты всегда в куче» — Java Language Specification не обещает конкретного размещения локальных значений в памяти реализации. Создание кадров, оптимизации, escape analysis и GC — будущая `JVM-MEMORY`.

> [!note]- Углубление: параллельный доступ
> Общая ссылка позволяет нескольким потокам получить доступ к одному объекту, но **не делает операции с ним потокобезопасными**. Какие изменения гарантированно видны и как не допустить гонок, разберут `CON-RACE` и `CON-JMM`.

## Вопросы, по которым стоит проверить понимание

| Вопрос | Что важно в ответе |
|---|---|
| Почему `change(int x)` не меняет значение в вызывающем коде? | Копируется значение, параметры самостоятельны |
| Почему `append(StringBuilder)` меняет объект, а `replace(StringBuilder)` — нет? | Общая ссылка vs переназначение параметра |
| Почему `swap(a, b)` не меняет местами переменные вызывающего кода? | Метод обменивает только локальные копии ссылок |
| Почему `changeElement(int[] numbers)` может изменить элемент массива? | Массив — объект, ссылка скопирована |
| Почему `s += "x"` для String и `n++` для Integer не меняют объект вызывающего кода? | Неизменяемость и присваивание локальной переменной |
| Почему `replaceNull(null)` завершается нормально, а при `append(null)` исключение возникает уже внутри тела метода? | Копирование `null` допустимо, NPE возникает при разыменовании в теле |
| Почему `primitive(missing)` при `Integer missing = null` не печатает `body`? | Распаковка аргумента завершается NPE до тела |
| Почему вызов `m(null)` при перегрузках `m(String)` и `m(Integer)` не компилируется? Почему здесь нельзя говорить об исключении при выполнении? | Ошибка компиляции: две несравнимые по специфичности перегрузки, до выполнения дело не доходит |
| Как `varargs` связан с массивом? | При вызове переменной арности массив создаётся, при фиксированной арности готовый массив передаётся без копирования |
| Чем отличаются `shape()`, `shape((Object[]) null)`, `shape((Object) null)` и `shape(new int[]{67})`? | Пустой массив, отсутствующий массив, один null-элемент, один элемент-массив |
| Можно ли перегрузить `m(int...)` методом `m(int[])`? | Нет, одна сигнатура; varargs только один и последний |
| Почему `Object value = "Java"; describe(value)` компилятор выбирает `describe(Object)`? | Статические типы и перегрузка |
| Каким будет порядок `call(i++, i++)`? | Аргументы вычисляются слева направо |
| Почему `target().call(switchReceiver(), later())` даёт `R12A`, а не `R12B`? | Ссылка на получателя сохранена до вычисления аргументов |
| Почему при `receiver == null` трассы `R22:NPE` и `R!:ISE` различаются? | Проверка получателя после нормального вычисления аргументов; раннее исключение прерывает вызов |
| Почему при `Base receiver = new Child()` вызов `receiver.describe("Java")` возвращает `Child:Object`, а не `Child:String`? Какую сигнатуру выбирает компилятор и какая реализация выполняется? | Компилятор выбирает `describe(Object)`, доступный через `Base`; при виртуальном вызове выполняется переопределённый `Child.describe(Object)` |
| Что изменится при `aliases(value, value)` и при обмене ячеек массива? | Общий объект, локальное переназначение и мутация контейнера — разные операции |

**Разбор ответа:** пусть `Integer missing = null`, а метод `primitive(int number)` при входе в тело печатает строку `body`. Вызов `primitive(missing)` не является передачей `null` в тело: тип параметра — `int`. Компилятор допускает вызов с распаковкой, но при выполнении распаковка завершается NPE, поэтому строка `body` не печатается. Для `m(null)` с несравнимыми ссылочными перегрузками ситуация иная: компилятор отклоняет вызов, и до выполнения программа не доходит.

## Практика

[ParametersDemo.java](../../examples/core/ParametersDemo.java) содержит самопроверки сохранения исходного примитива, мутации общего объекта и массива, локального переназначения, `swap`, неизменяемых типов, изменения явно переданного массива в `varargs`, выбора перегрузки `Object`/`String` и трассы `12` при вычислении аргументов. Вызовы `acceptNull(null)` и `mutateVarargs(1, 2)` проверены только нормальным завершением: для них нет отдельной проверки состояния или идентичности массива.

**Уточнение к историческому плану [CORE-PARAMS, L22](../../docs/plans/CORE-PARAMS.md#L22).** Утверждение, что все перечисленные контрпримеры воспроизводятся в `ParametersDemo.java`, шире фактического покрытия. В исходнике есть `choose(Object)` / `choose(String)` (строки 18–19, 70–72), но нет пары несвязанных перегрузок `m(String)` / `m(Integer)` и вызова `m(null)`. Закомментированный неоднозначный вызов в статье — иллюстрация ошибки компиляции; он не является исполняемым тестом `ParametersDemo`. В рамках D-A03 неоднозначность отдельно подтверждена временной компиляционной пробой Java 21; она не добавлена в репозиторий и не запускается обычным CI. Исторический план оставлен без изменений.

**Запуск из корня репозитория**, JDK 21:

```powershell
java examples/core/ParametersDemo.java
```

[ValuesAndReferencesDemo.java](../../examples/core/ValuesAndReferencesDemo.java) остаётся более коротким примером для первого знакомства с копированием значения и ссылки. Отдельный Gradle-модуль для этих демонстраций не требуется.

**Связь с вычислением выражений:** порядок аргументов слева направо иллюстрируется здесь при вызове методов; [CORE-OPERATORS](operators.md) отдельно объясняет этот порядок как гарантию JLS для операндов/аргументов, побочные эффекты и составное присваивание. Лексическая область имён разобрана в [CORE-SYNTAX](syntax.md), а захват локальной переменной — в [CORE-FUNCTIONS](functions.md).

## Первоисточники

- [JLS 21 §4.12 — Variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12)
- [JLS 21 §8.4.1 — Formal Parameters](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.1)
- [JLS 21 §15.7.4 — Argument Lists are Evaluated Left-to-Right](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.7.4)
- [JLS 21 §15.12 — Method Invocation Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12)
- [JLS 21 §5.3 — Invocation Contexts](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.3)
- [JLS 21 §5.1.8 — Unboxing Conversion](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.8)
- [JLS 21 §8.4.2 — Method Signature](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.2)
- [JLS 21 §15.12.4 — Run-Time Evaluation of Method Invocation](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4)
