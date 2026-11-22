# CORE-PATTERNS — pattern matching, record patterns и switch в Java 21

**Задача статьи:** по конкретному Java-коду определять, компилируется ли он, какие значения связываются pattern variables, какая ветвь выбирается и когда возникает *исключение во время выполнения*. Используется **Java SE 21 без preview**. Рекомендуется знать [ссылочные типы и null](types.md), [наследование](inheritance.md), [record](records.md), [равенство](equality.md), [sealed hierarchy](sealed.md). Приведённые snippets служат учебными примерами; их не следует механически склеивать в один Java-файл.

**Исследовательская база:** [принятый план CORE-SEALED + CORE-PATTERNS](../../docs/plans/CORE-SEALED-PATTERNS.md), **PM-01–PM-28: 18 P0 / 6 P1 / 4 P2**. Нормативные правила — [JLS 21 §14.11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11), [§14.30](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.30), [§6.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3), [§15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2), [§15.28](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.28) и [MatchException API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/MatchException.html). Не используйте документацию preview Java 17–20 как спецификацию финального синтаксиса 21.

## 1. От instanceof и явного cast к связыванию значения (PM-01, PM-02)

Раньше тип проверяли выражением `value instanceof String`, а затем повторно приводили ссылку к `String`. **Type pattern** `value instanceof String s` объединяет проверку и *binding*: только если значение не null и подходит по типу, переменная `s` получает корректно типизированную ссылку. Компилятор не разрешает использовать `s` в местах, где совпадение не установлено.

~~~java
class InstanceofDemo {
    static String oldStyle(Object value) {
        if (value instanceof String) {
            String s = (String) value;
            return "len:" + s.length();
        }
        return "other";
    }

    static String pattern(Object value) {
        if (!(value instanceof String s)) {
            return "not-string";
        }
        return "string:" + s.length(); // s гарантированно связан после return
    }

    public static void main(String[] args) {
        System.out.println(pattern("seven")); // string:5
        System.out.println(pattern(67));      // not-string
        System.out.println(pattern(null));    // not-string
    }
}
~~~

**Трассировка:** для строки Java проверяет соответствие String и связывает `s`; отрицательная ветка не выполняется, поэтому после неё вызов `s.length()` безопасен с точки зрения типовой проверки. Для `67` или null отрицательная ветка возвращает ответ, до обращения к `s` дело не доходит.

В примере именно **ранний return** обеспечивает область видимости после проверки. Это не означает, что любые строки ниже любого `if (x instanceof T t)` могут свободно обращаться к `t`. Компилятор рассуждает о путях потока управления и о том, где pattern **definitely matches**.

## 2. Flow scoping, &&, || и недоступные binding variables (PM-03, PM-17)

Логическое `&&` вычисляет правый операнд только при истинном левом. Поэтому корректно обращаться к `s` справа, если левый `instanceof String s` установил совпадение:

~~~java
class FlowAnd {
    static boolean longString(Object value) {
        return value instanceof String s && s.length() > 3;
    }
    public static void main(String[] args) {
        System.out.println(longString("abcde")); // true
        System.out.println(longString(67));      // false
    }
}
~~~

Обратный фрагмент **не компилируется**:

~~~java
class InvalidOrScope {
    // Замените false на реальную проверку; пример специально отвергается javac:
    static boolean broken(Object v) {
        return v instanceof String s || s.length() > 3; // s не в scope справа
    }
}
~~~

Это **compiler error**, а не `NullPointerException`: при ложном левом результате type pattern *не* связал `s`, следовательно нельзя читать `s.length()`. Аналогично, повторное объявление одного binding имени в одной вложенной record pattern либо повторяющаяся метка null нарушают компиляционные ограничения (G1 N18/N11). Области pattern bindings зависят от control-flow, а не просто от фигурных скобок.

**Обратная логика:** выражение `!(x instanceof String s)` и последующий гарантированный выход из отрицательной ветви могут сделать `s` доступной после неё. Для `||` допустимость обращений требует другой логики definitely matches, а не общей догмы «pattern variable всегда запрещена при `||`». Конкретный G1 N15 запрещён, потому что справа `s` используется в ветви, где левый `instanceof` ложен.

## 3. Switch-expression и switch-statement — разные формы (PM-04, PM-19)

**Switch-expression** вычисляет результат; его необходимо сделать *exhaustive*: для каждого допустимого значения selector должен быть путь выбора label. **Enhanced switch-statement** с pattern labels также обязан быть exhaustive по правилам Java 21. Но старый legacy statement над допустимым традиционным selector с обычными constant labels **не обязан** перечислять все возможные значения.

~~~java
class SwitchForms {
    static int expression(int n) {
        return switch (n) {
            case 1 -> 10;
            default -> 67;
        };
    }

    static void legacyStatement(int n) {
        switch (n) {
            case 1:
                System.out.println("one");
                break;
            // нет default: законно для legacy statement
        }
    }

    static String patternStatement(Object value) {
        StringBuilder b = new StringBuilder();
        switch (value) {
            case null -> b.append("null");
            case Integer n when n > 0 -> b.append("positive");
            case Integer n -> b.append("nonpositive");
            default -> b.append("other");
        }
        return b.toString();
    }
}
~~~

**Почему это важно:** начинающий часто переносит правило exhaustiveness выражения на любое устаревшее `switch(int)` либо считает, что statement с pattern labels может остаться неполным. G1 N17 специально проверяет enhanced pattern switch-statement над Object **без default**: javac выдаёт `not.exhaustive.statement`. Наличие стрелочных меток само по себе не является достаточным объяснением исчерпываемости; нужно учитывать вид switch, selector и labels.

**Возможные selector types:** Java21 не разрешает напрямую `switch(long)` без специальных preview/новых языковых возможностей. G1 N19 изолированно подтверждает соответствующую compiler diagnostic. Нельзя импортировать из новых Java-версий primitive patterns и примитивные selector enhancements как стандарт SE 21.

## 4. Sealed hierarchy и полный switch без default (PM-05, PM-06)

В [CORE-SEALED](sealed.md) введено правило: sealed ограничивает **прямые** subtype. Компилятор может доказать исчерпываемость switch по разрешённым вариантам. Если разрешённая ветка объявлена `non-sealed`, это **не отменяет** exhaustiveness: достаточно одного pattern, охватывающего тип этой ветви **и все его косвенные подтипы**.

~~~java
sealed interface Decision permits Accepted, Rejected, Pending {}
record Accepted(int value) implements Decision {}
record Rejected(String reason) implements Decision {}
non-sealed class Pending implements Decision {}
final class Later extends Pending {}

class DecisionSwitch {
    static String describe(Decision d) {
        return switch (d) {
            case Accepted(int value) -> "yes:" + value;
            case Rejected(String why) -> "no:" + why;
            case Pending p -> "pending"; // поймает и Later
        };
    }

    public static void main(String[] args) {
        System.out.println(describe(new Accepted(67))); // yes:67
        System.out.println(describe(new Later()));       // pending
    }
}
~~~

**Трассировка `Later`:** объект не имеет прямого permitted relationship с `Decision`, но наследует `Pending`. Поэтому ветка `case Pending p` покрывает его без default. Если удалить Pending-ветку, G1 N09 получает **ошибку компиляции switch-expression**, а не runtime `MatchException` (последнее возможно при совсем других условиях — §9).

Важная граница: exhaustiveness по sealed типам доказывается на основе компиляторно доступной иерархии на момент компиляции. Исследование старого клиента после отдельной бинарной эволюции sealed tree — **PM-25 P2 / NOT RUN**, не автоматическое следствие одного примера.

## 5. Dominance: порядок case labels имеет семантику (PM-07, PM-22)

Pattern для более общего типа может поглотить более узкий вариант, если стоит раньше. Например, когда selector имеет тип Object, первая негардированная ветка `case Object o` способна обработать любой ненулевой Object, поэтому следующая `case String s` **dominated**, и javac её отвергнет:

~~~java
class DominanceExample {
    static String valid(Object o) {
        return switch (o) {
            case String s -> "text:" + s;
            case Integer n -> "number:" + n;
            default -> "other";
        };
    }

    // Нельзя сделать так (G1 N08):
    // switch(o) { case Object all -> "all"; case String s -> "text"; }
}
~~~

Это **статическая ошибка**, а не «вторая ветка никогда не срабатывает во время выполнения». Компилятор проверяет соотношение pattern applicability/dominance до запуска программы.

**Record patterns имеют собственные правила applicability и coverage.** Нельзя заменять их произвольным cast или считать, что один типовый pattern равен любой возможной деконструкции. Generic type restrictions, unchecked conversion, unconditional patterns и сложная вложенная применимость требуют отдельной сверки по [JLS 21 §14.30](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.30); для P1 здесь важно усвоить правило «широкая негардированная ветвь выше узкой часто ошибка», а не обещать полный решатель generic pattern dominance.

## 6. Guard when: фильтр после успешного type match (PM-10, PM-11, PM-20)

**Java21 stable syntax:** `case String s when s.length() > 3 -> ...`. Сначала проверяется type pattern и связывается `s`, затем вычисляется guard. Если guard ложный, поиск подходящей ветви продолжается. В финальном Java 21 используется ключевое слово **`when`**, а старый preview-синтаксис `case String s && ...` **отвергается** (G1 N12 — *намеренная* синтаксическая negative, не ошибка недописанного источника).

~~~java
class GuardsDemo {
    static String classify(Object v) {
        return switch (v) {
            case String s when s.length() > 3 -> "long:" + s;
            case String s -> "short:" + s;
            case Integer n when n > 0 -> "positive:" + n;
            case Integer n -> "nonpositive:" + n;
            default -> "other";
        };
    }
    public static void main(String[] args) {
        System.out.println(classify("abcd")); // long:abcd
        System.out.println(classify("ab"));   // short:ab
        System.out.println(classify(67));     // positive:67
        System.out.println(classify(-1));     // nonpositive:-1
    }
}
~~~

**Почему две String-ветки законны:** у первой есть guard, который может быть ложным; поэтому негардированная следующая ветка не является заведомо недостижимой. Если поставить общую `case String s` перед специализированной, она доминирует и делает поздний guarded String case недопустимым.

**Guarded-only не означает exhaustive.** `case String s when s.length() > 0` не охватывает пустую строку и вообще не охватывает другие Object. Даже в sealed hierarchy guard, который не гарантирован истинным, может сделать покрытие неполным — G1 N10/N23. Константное `when true` имеет специальные правила JLS, поэтому нельзя упрощённо заявлять «любая guarded ветвь никогда не считается для exhaustiveness».

### 6.1 Side effects и порядок вычисления (PM-20)

Selector вычисляется один раз; затем происходят проверки labels по правилам switch, и guard вычисляется **лишь после соответствующего pattern match**. Побочные эффекты в guard технически возможны, но создают скрытое изменение состояния во время выбора ветви:

~~~java
class GuardOrder {
    static int guards;
    static String choose(Object o) {
        return switch (o) {
            case String s when ++guards > 0 && s.length() > 2 -> "long";
            case String s -> "short";
            default -> "other";
        };
    }
    public static void main(String[] args) {
        System.out.println(choose(67) + ":" + guards);   // other:0
        System.out.println(choose("abcd") + ":" + guards); // long:1
    }
}
~~~

Здесь для 67 type pattern String ложный, поэтому guard **вообще не вычисляется** и счётчик остаётся 0. Для "abcd" тип совпал, `++guards` выполняется, затем оценивается длина. Это **ожидаемый trace по правилам языка**, а не доказательство, что любые side effects внутри guard хороши для читаемого кода. Практически guard лучше оставлять чистой проверкой условия.

## 7. Record patterns: разбор структуры без ручных accessors (PM-12, PM-13)

`record pattern` сначала проверяет, что объект соответствует указанному record-type, а затем деконструирует компоненты через его accessors и сопоставляет вложенные patterns. Это **не** сравнение двух записей методом `equals` и не глубокое копирование. Компоненты продолжают иметь собственные типы и свою семантику null.

~~~java
record Point(int x, int y) {}
record Envelope(Point point) {}

class Deconstruction {
    static String inspect(Object o) {
        if (o instanceof Envelope(Point(var x, var y))) {
            return "sum:" + (x + y);
        }
        return "no-point";
    }

    public static void main(String[] args) {
        System.out.println(inspect(new Envelope(new Point(6, 7)))); // sum:13
        System.out.println(inspect(new Envelope(null)));           // no-point
    }
}
~~~

Срабатывание проходит несколько шагов: вход проверяется как Envelope; извлекается компонент point; вложенный `Point(var x, var y)` проверяет его runtime тип и связывает x и y. В примере числа 6 и 7 складываются в 13. У record pattern должна быть **правильная арность**: при `record Point(int x,int y)` попытка написать `Point(int x)` — compiler error N13. Неверный тип компонента вроде `Point(String x, int y)` — compiler error N14.

**Вложенность и var:** `var x` здесь означает inference типа из record component, а не неявное преобразование во всё что угодно. Например, x/y объявлены как int; нельзя объявить внутренний String pattern для int. Вложенные record patterns можно комбинировать с отдельными bindings и guards, но generic сложные комбинации стоят отдельной проверки PM-26/P2.

## 8. Null selector, null component, default и объединённая метка (PM-08, PM-09, PM-14, PM-21)

Эта тема требует разделить **два совершенно разных null**.

**Null selector:** выражение в `switch(selector)` само равно null. Если нет подходящей `case null`, Java 21 бросает **`NullPointerException`**, даже при наличии `default`. Сам `default` не считается обработчиком null. Если требуется обработать и null, и все остальные случаи одним результатом, Java21 допускает объединённую метку `case null, default`.

~~~java
class NullSelectorDemo {
    static String explicitly(Object v) {
        return switch (v) {
            case null -> "null";
            case String s -> s;
            default -> "other";
        };
    }
    static String nullOrDefault(Object v) {
        return switch (v) {
            case null, default -> "caught";
        };
    }
    static String defaultDoesNotCatch(Object v) {
        return switch (v) {
            case String s -> s;
            default -> "default";
        };
    }
    public static void main(String[] args) {
        System.out.println(explicitly(null));   // null
        System.out.println(nullOrDefault(null)); // caught
        try {
            defaultDoesNotCatch(null);
        } catch (NullPointerException ex) {
            System.out.println("NPE");          // NPE
        }
    }
}
~~~

**Null record component:** selector в целом **не null**, например `new Envelope(null)`. Но внутри него компонент `point` равен null; вложенная record pattern `Point(var x,var y)` **не совпадает**, и `instanceof` даёт false. Это G1 **P13=not-point**. Такое несовпадение **не обязано** бросать MatchException: простой `instanceof` законно возвращает false.

**Нельзя обобщать «любой null-компонент запрещён»:** record может содержать null в String-поле, и другой pattern способен его обработать. Конкретное несовпадение связано с nested pattern, требующей **объекта Point**, а не с всеобщим запретом null в любом record.

## 9. Два разных runtime MatchException — и почему это НЕ compiler error (PM-15, PM-16, PM-23)

Есть ситуации, когда весь исходный switch/record pattern **успешно компилируется**, но в процессе поиска pattern происходит специальное исключение.

### 9.1 Accessor бросил исключение: причина сохраняется (G1 P17)

~~~java
record Broken(int number) {
    @Override public int number() {
        throw new IllegalStateException("broken-accessor");
    }
}

class AccessorFailure {
    static String extract(Object o) {
        return switch (o) {
            case Broken(int n) -> "number:" + n;
            default -> "other";
        };
    }
    public static void main(String[] args) {
        try {
            extract(new Broken(1));
        } catch (MatchException ex) {
            System.out.println(ex.getClass().getSimpleName()
                + ":" + ex.getCause().getClass().getSimpleName());
            // MatchException:IllegalStateException
        }
    }
}
~~~

Проверка типа Broken сначала проходит. Затем record matching вызывает accessor `number()`, потому что компонент нужен для деконструкции. Accessor бросает IllegalStateException; язык требует завершить matching с **`MatchException`, сохранив исходную причину**. Это не делает каждый accessor исключением: при нормальном accessor деконструкция продолжается обычным способом. И это не статическая ошибка `javac` — сигнатуры и patterns корректны.

### 9.2 Исчерпывающий статически switch не нашёл runtime match (G1 P20)

~~~java
record Point(int x, int y) {}

sealed interface Answer permits Yes, No {}
record Yes(Point point) implements Answer {}
record No(String cause) implements Answer {}

class ExhaustiveRuntimeMismatch {
    static String answer(Answer input) {
        return switch (input) {
            case Yes(Point(int x, int y)) when x > 0 -> "positive";
            case Yes(Point(int x, int y)) -> "yes";
            case No(String reason) -> "no";
        };
    }

    public static void main(String[] args) {
        try {
            answer(new Yes(null));
        } catch (MatchException ex) {
            System.out.println("MatchException"); // MatchException
        }
    }
}
~~~

**Подробный trace P20:** тип Answer sealed, разрешённые прямые реализации — Yes и No. Java-компилятор видит обе ветви и признаёт switch exhaustive. В runtime объект — `Yes`, но его поле `point` равно null. Обе ветви `Yes(Point(...))` требуют реального Point и не подходят. Ветка No тоже не подходит. Получается исчерпывающий **по типам** switch, который не нашёл matching pattern для конкретной композиции значений, — поэтому он бросает **runtime `MatchException`**.

Сравните три принципиально разных результата:

| Случай | Что произойдёт | Почему |
|---|---|---|
| `new Envelope(null) instanceof Envelope(Point(...))` | `false`, без exception | Nested record pattern не совпала (P13) |
| `Broken.number()` выбросил IllegalStateException при record pattern | **MatchException с cause** | Ошибка accessor во время deconstruction (P17) |
| Exhaustive `switch(Answer)` получил `Yes(null)` | **MatchException** | Не нашлось runtime match (P20) |
| `switch((Object)null)` без `case null` | **NullPointerException** | Это *null selector* (P16) |
| Не хватает sealed subtype branch в исходнике | **javac error** | Исходный switch не exhaustive (N09/N23) |

Нельзя отвечать на интервью «MatchException — это компиляционная ошибка», «default ловит null» или «вложенная null record pattern всегда бросает исключение». Все три утверждения неверны. Ситуации бинарной эволюции старого switch и нового permitted subtype исследованы только теоретически (PM-25 P2), **не как отдельный JVM experiment**.

## 10. Ограничения связывания, применимости и отдельные negative cases (PM-17, PM-18, PM-22)

Дублирование `case null` (G1 N11), вторичное объявление того же pattern variable имени в одной binding scope (N18), неверная арность record pattern (N13), не-convertible component type (N14), dominated case (N08) — всё это **ошибки компиляции**. В частности, `instanceof` над primitive `int` в форме `int v; v instanceof String s` запрещён (G1 N16): не путайте его с `String value; value instanceof String s`, который в Java21 допустим.

**Guard только для части значений:** `case Success s when s.value() > 0` не покрывает Success(0), если следующей ветви нет. G1 N23 — non-exhaustive switch-expression с guarded branch. Конкретные diagnostic keys зависят от реализации/версии javac; норма JLS устанавливает некорректность программы, не фиксированный текст диагностического сообщения.

**Предел PM-22:** generics и record patterns проверяют applicability и erasure по детальным правилам; нельзя объявить любой произвольный вложенный type pattern «всегда применимым». Глубокая теория wildcard capture и nested generic inference сверх примеров этой статьи выделена в PM-26/P2.

**Отдельная ветвь case null:** если есть unconditional type pattern, нужно учитывать порядок и dominance; label `case null, default` — специальная синтаксическая форма, а не два произвольных `default`. Не следует писать несколько null labels и ожидать runtime выбора: это ошибка компиляции.

## 11. Как читать принятый stdout G1 без подмены факта запуском всех snippets

У принятого исследования есть **полный `PatternsProbe.java`** с 20 подписанными наблюдениями. Вместо механического переписывания исходника по одной строке проследим цепочки:

| G1 метки | Наблюдаемый результат | Почему |
|---|---|---|
| P01 | `null` | `case null` |
| P02–P03 | `long:abcd` / `short:ab` | guarded String → fallback String |
| P04–P05 | `positive:67` / `nonpositive:-1` | guarded Integer → fallback Integer |
| P06 | `other` | selector Double, default |
| P07–P09 | `yes+:9` / `yes:5` / `no:down` | nested Point record + guarded Yes + No |
| P10–P11 | `string:5` / `not-string` | отрицательная ветка с ранним return |
| P12–P13 | `point:13` / `not-point` | Envelope(Point) vs Envelope(null) |
| P14–P15 | `positive` / `null` | enhanced switch-statement |
| P16 | `NPE` | default не перехватил null selector |
| P17 | `MatchException:IllegalStateException` | accessor failure с cause |
| P18 | `caught` | `case null, default` |
| P19 | `true` | `instanceof String s && s.length()>3` |
| P20 | `MatchException` | Yes(null) не совпал с nested Point |

Все 20 строк и исходный код целиком приведены в [G1 §7.2](../../docs/plans/CORE-SEALED-PATTERNS.md). Они реально проверены G1/G4 на OpenJDK21.0.12.1 с `javac --release 21 -Xlint:all` без предупреждений и двумя полными процессными stdout после совместной сборки с `SealedProbe.java`. Это **20 строк одного Java-процесса и 11 строк второго — 31 наблюдение, не 31 независимый тест**. С ролью 03 согласован [один автономный runnable для двух статей](https://github.com/Melchiorius/backend-interview-labs/issues/139); прямая локальная ссылка появится только после публикации этих файлов в авторской ветке.

## Карта PM-01–PM-28

| ID | Приоритет | Где раскрыт механизм, отрицательный пример или граница |
|---|---|---|
| PM-01 | P0 | §1 instanceof binding и cast, P10/P11 |
| PM-02 | P0 | §1 flow после `if (!(...)) return` |
| PM-03 | P0 | §2 short circuit &&, G1 N15 для || |
| PM-04 | P0 | §3 expression vs enhanced/legacy statement, N17 |
| PM-05 | P0 | §4 exhaustive sealed switch без default, N09 |
| PM-06 | P0 | §4 non-sealed Pending/Later и covering case |
| PM-07 | P0 | §5 dominance Object прежде String, N08 |
| PM-08 | P0 | §8 case null и duplicate N11 |
| PM-09 | P0 | §8 default vs case null, default, P16/P18 |
| PM-10 | P0 | §6 when guard после match, N12 old syntax |
| PM-11 | P0 | §6 guarded-only не exhaustive, N10/N23 |
| PM-12 | P0 | §7 record deconstruction, P07/P08/P09 |
| PM-13 | P0 | §7 nested Point(var x,var y), arity/type N13/N14 |
| PM-14 | P0 | §8/9 null nested component, P13 false vs P20 exception |
| PM-15 | P0 | §9.1 accessor failure → MatchException cause, P17 |
| PM-16 | P0 | §9.2 runtime mismatch vs javac non-exhaustive, P20/N09 |
| PM-17 | P0 | §2/10 pattern binding scope, duplicate N18/null N11 |
| PM-18 | P0 | §3/10 selector long и primitive instanceof N19/N16 |
| PM-19 | P1 | §3 legacy statement vs enhanced statement |
| PM-20 | P1 | §6.1 order/side effects of guard: own trace |
| PM-21 | P1 | §8 combined case null, default |
| PM-22 | P1 | §5/10 type vs record applicability/dominance, generic boundary |
| PM-23 | P1 | §9.1 accessor exception cause и предел |
| PM-24 | P1 | §4/9/12 sealed exhaustiveness vs enum boundary |
| PM-25 | P2 | §12 binary evolution/old exhaustive switch — NOT RUN |
| PM-26 | P2 | §10 nested generic inference/wildcard applicability — not exhausted |
| PM-27 | P2 | §12 JEP 443 preview unnamed patterns/other-release primitives — outside Java21 |
| PM-28 | P2 | §12 JPMS/JIT/GC/JMH/GUI/other JVM — NOT RUN |

## 12. Риск эволюции и непокрытые темы PM-24–PM-28

Для enum и sealed типов у исчерпывающего switch есть общая идея: компилятор понимает допустимые варианты по статически доступной модели. Но **CORE-ENUMS остаётся отдельной незавершённой curriculum-темой**: наличие таблицы здесь не означает изучения enum API, synthetic members или их всех правил эволюции.

**PM-25 P2:** полноценный эксперимент со старым отдельно скомпилированным exhaustive switch, новой permitted ветвью и тем, что реально делает JVM после смены binary, **не выполнялся**. Нельзя использовать P20 с Yes(null) как доказательство binary compatibility failure: P20 — иной runtime mechanism.

**PM-26 P2:** nested generic record inference, wildcard capture и полная математика applicability — отдельная глубокая тема. Простые Point и Envelope не заменяют её.

**PM-27 P2:** unnamed patterns/variables JEP 443 — **preview JDK21**, не часть стандарта `javac --release 21` без `--enable-preview`. Primitive switch/pattern enhancements новых релизов также не переносятся назад в Java21. В code snippets здесь используется стабильное `when`.

**PM-28 P2:** named module runtime, bytecode/JVMS, профилирование JVM, JIT/GC/JMH, GUI Obsidian/IntelliJ и проверки на альтернативных JDK/OS не выполнены; из 20 наблюдений `PatternsProbe` нельзя делать вывод о производительности и кросс-JVM timing.

## Нормативные источники и честность evidence

Первоисточники: [JLS Java21 §14.11 switch](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11), [§14.30 patterns](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.30), [§6.3 flow scope](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3), [§15.20.2 instanceof](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2), [§15.28 switch expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.28), [MatchException Java21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/MatchException.html). [JEP 440](https://openjdk.org/jeps/440)/[JEP 441](https://openjdk.org/jeps/441) — история; независимые C1/C2/C3 источники из [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) — материал для интервью-вопросов, не гарантии языка.

**Границы доказанного:** G1/G4 проверяли два полных source, **11+20** строк ожидаемого stdout, **24 изолированных compile-negative** по javac diagnostic keys и **один named-module positive M01 только компиляцией**. Эти результаты пока относятся к G1, не являются CI этой новой авторской ветки и **не означают компиляции всех отдельных inline-фрагментов выше**. Для новой ветки нужны отдельные проверки actual HEAD.

**NOT RUN:** N01–N24 в GitHub CI, независимая компиляция всех Markdown snippets, real JPMS module-path, binary evolution after separate compilation, JMH/JIT/GC, расширенный CORE-ENUMS, другие JDK/OS, GUI и ссылочные якоря.
