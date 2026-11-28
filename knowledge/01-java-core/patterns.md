# CORE-PATTERNS — pattern matching, record patterns и switch в Java 21

**Задача статьи:** по конкретному Java-коду определять, компилируется ли он, какие значения связываются pattern variables, какая ветвь выбирается и когда возникает *исключение во время выполнения*. Используется **Java SE 21 без preview**. Рекомендуется знать [ссылочные типы и null](types.md), [наследование](inheritance.md), [record](records.md), [равенство](equality.md), [sealed hierarchy](sealed.md). Приведённые snippets служат учебными примерами; их не следует механически склеивать в один Java-файл. Switch-expression стал постоянной возможностью языка в Java 14, pattern matching для instanceof — в Java 16, record patterns и pattern matching для switch — в Java 21. См. [таблицу изменений языка по версиям](https://docs.oracle.com/en/java/javase/21/language/java-language-changes-release.html).

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

По [JLS §15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2) проверяется применимость pattern к статическому типу выражения, в частности checked cast compatibility для type pattern; это не произвольное непроверяемое приведение. В Java 21 даже `String s; s instanceof String t` допустимо (если s уже инициализирована): ненулевая строка даёт true, null — false. Временная проверка D-A16b подтвердила оба результата. Ограничение раннего preview на проверку того же типа здесь не действует.

Связанная переменная является обычной локальной переменной: её можно переназначить, если она не объявлена final и этому не мешают дополнительные правила контекста. Например, `if (o instanceof String s) { s = "67"; return Integer.parseInt(s); }` даёт 67 для ненулевой строки. При `instanceof final String s` присваивание s отклонено компилятором. Ограничение effectively final при использовании binding в guard — отдельное правило (§6). См. [преобразования типов](conversions.md) и [JLS §4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4).

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
    // Отрицательный пример: javac отклоняет обращение к s справа от ||.
    static boolean broken(Object v) {
        return v instanceof String s || s.length() > 3; // s не в scope справа
    }
}
~~~

Это **compiler error**, а не `NullPointerException`: при ложном левом результате type pattern *не* связал `s`, следовательно нельзя читать `s.length()`. Аналогично, повторное объявление одного binding имени в одной вложенной record pattern либо повторяющаяся метка null нарушают компиляционные ограничения (G1 N18/N11). Области pattern bindings зависят от control-flow, а не просто от фигурных скобок.

**Обратная логика:** выражение `!(x instanceof String s)` и последующий гарантированный выход из отрицательной ветви могут сделать `s` доступной после неё. Для `||` допустимость обращений требует другой логики definitely matches, а не общей догмы «pattern variable всегда запрещена при `||`». Конкретный G1 N15 запрещён, потому что справа `s` используется в ветви, где левый `instanceof` ложен.

Контраст — законное выражение, проверенное отдельно:

~~~java
static boolean negOr(Object o) {
    return !(o instanceof String s) || s.isEmpty();
}
~~~

| o | Левый операнд | Вычисляется ли справа s.isEmpty() | Результат |
|---|---|---|---|
| 67 | true | нет, s не связана | true |
| "" | false | да, s — пустая строка | true |
| "x" | false | да, s — "x" | false |

При ложном левом операнде отрицание означает, что исходный instanceof дал true, поэтому справа binding определён. По той же причине в `if (o instanceof String s) ... else if (o instanceof Integer s) ...` имя s можно повторить в двух непересекающихся областях (проверено: ветка Integer для 67 вернула 67). Это не разрешение повторного объявления имени в одной области; см. [JLS §6.3.1–6.3.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3.1).

## 3. Switch-expression и switch-statement — разные формы (PM-04, PM-19)

**Switch-expression** вычисляет результат, и его блок должен быть исчерпывающим на этапе компиляции. Это проверка покрытия, а не гарантия успешного выполнения для любого значения: null без подходящей метки или вложенный record с null могут привести к исключению (§8–9).

**Enhanced switch-statement** в Java 21 — statement, у которого либо тип селектора не относится к традиционным типам (char, byte, short, int, Character, Byte, Short, Integer, String, enum), либо хотя бы одна case-метка содержит pattern или null. Такой statement тоже обязан быть исчерпывающим. Legacy statement использует только традиционный селектор и не имеет pattern/null labels; он может не покрывать все значения. Форма стрелок или двоеточий сама по себе классификацию не определяет.

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

В отдельной проверке `switch (n) { case 1 -> x[0]++; }` при n=67 оставил счётчик x[0] равным 0: legacy statement может не выполнить ни одной ветви. После стрелки statement допускает statement expression (например, вызов метода или присваивание), блок либо throw; голое `case 1 -> 67;` в statement отклонено с compiler.err.not.stmt. В expression допустим результат 67; в блоке нормальное получение значения осуществляется через yield. См. [CORE-CONTROL](control-flow.md) и [JLS §14.11.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.2).

**Возможные selector types:** Java 21 поддерживает все ссылочные типы, в том числе Long (в проверке `case Long x -> x.intValue()` при 67L вернул 67), и примитивы char, byte, short, int. Примитивный long, float, double, boolean не допускаются. Возможность использовать Long как ссылочный селектор не делает `case 67L` допустимой константной меткой: constant expression labels ограничены char/byte/short/int и String и должны быть совместимы с селектором; enum constants проверяются отдельно (§12). Проверены отдельные ошибки primitive long, case 67L для Long и case null для int. Не переносите primitive patterns и расширенные примитивные селекторы из более новых релизов в стандарт SE 21.

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

Важная граница: exhaustiveness по sealed типам доказывается на основе компиляторно доступной иерархии на момент компиляции. Исследование старого клиента после отдельной бинарной эволюции sealed tree — **PM-25 P2 / NOT RUN**, не автоматическое следствие одного примера. Для конкретного sealed-класса необходимо покрыть и экземпляр самого класса; при параметризованном селекторе некоторые разрешённые объявления могут быть несовместимы с его статическим типом. Оба контраста PM-05 с практическими примерами уже раскрыты в [CORE-SEALED, §5–6](sealed.md); здесь они нужны для понимания применимости, а не повторного исследования sealed.

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

Для константной метки действуют отдельные правила финальной [JLS §14.11.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.1): если выше стоит негардированный pattern, безусловный для типа константы (для примитивной константы — её wrapper), константа доминируется. Guard с постоянным true учитывается как отсутствие guard. Обычный неконстантный guard не даёт такого статического доминирования над константой, даже если для некоторого значения он вернёт true. Например, в отдельной проверке:

~~~java
static int guarded(Integer n) {
    return switch(n) {
        case Integer i when i > 0 -> 1;
        case 67 -> 2;
        case Integer i -> 3;
    };
}
~~~

При n=67 выбрана первая ветвь (1), а не константная (2). Если удалить guard из первой ветви, отдельный вариант с последующей case 67 отклоняется с pattern.dominated. Также отклоняется повторная String-ветвь после `case String s when true`. Это не правило «константы всегда раньше patterns»: важно фактическое покрытие и наличие guard.

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

**Guarded-only не означает exhaustive.** `case String s when s.length() > 0` не охватывает пустую строку и вообще не охватывает другие Object. Даже в sealed hierarchy guard, который не гарантирован истинным, может сделать покрытие неполным — G1 N10/N23. Константное `when true` имеет специальные правила JLS, поэтому нельзя упрощённо заявлять «любая guarded ветвь никогда не считается для exhaustiveness». В отдельной проверке единственная ветвь `case Object x when true -> 67` была исчерпывающей для Object. Guard с constant expression false, напротив, запрещён при компиляции (guard.has.constant.expression.false).

### 6.1 Side effects и порядок вычисления (PM-20)

Selector вычисляется один раз; затем происходят проверки labels по правилам switch, и guard вычисляется **лишь после соответствующего pattern match**. Побочные эффекты в guard технически возможны, но создают скрытое изменение состояния во время выбора ветви:

Guard должен иметь тип boolean или Boolean. Во втором случае Java распаковывает ссылку; если метод nil() возвращает Boolean null, `case String s when nil()` при совпавшей строке бросает NullPointerException. Это не ложный guard и не MatchException.

По финальной JLS §14.11.1 локальные переменные, формальные параметры и параметры исключений, используемые, но не объявленные в guard, должны быть final или effectively final. Это относится и к binding из case, если он используется в guard. Например, если `case String s when s.isEmpty() -> { s = "x"; yield 1; }`, присваивание в теле лишает s effectively final и весь фрагмент отклоняется. Отдельно проверены изменяемые локальная переменная и параметр. Blank final должен быть definitely assigned до использования в guard: `final int k; k = 67;` перед switch допустимо, чтение k без этого присваивания — ошибка. В Probe с k и неизменяемым параметром p условие `i == k && i == p` дало 67 при обоих значениях 67.

~~~java
class GuardOrder {
    static int guards;
    static boolean checkLength(String s) {
        guards++;                  // побочный эффект внутри вызванного метода
        return s.length() > 2;
    }
    static String choose(Object o) {
        return switch (o) {
            case String s when checkLength(s) -> "long";
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

Здесь для 67 type pattern String ложный, поэтому guard **вообще не вычисляется** и счётчик остаётся 0. Для "abcd" тип совпал, вызывается `checkLength(s)`: он увеличивает счётчик, проверяет длину и возвращает true. Это исторический trace восьмистрочного локального smoke, а не рекомендация использовать побочные эффекты при выборе ветви.

В отдельной фактической проверке D-A16b селектор select(o) увеличивает счётчик selects, а check(s,len) — guards. Для 67 получены selects=1, guards=0; затем для "x" selects=2, guards=2, первая проверка длины >3 ложна, вторая >0 истинна, выбрана вторая ветвь. Итог `order=2:2:second`: каждый вызов switch вычислил селектор один раз, guard выполнялся лишь после совпадения pattern и в порядке поиска ветви.

**Граница спецификации и наблюдения javac.** В отдельных исходниках OpenJDK javac 21.0.12.1 отклонил `when ++n > 0`, `when ++QualifiedInc.n > 0`, `when (n = 67) > 0`, `when (QualifiedAssign.n = 67) > 0` при статическом поле n, объявленном вне guard. Во всех четырёх случаях exit 1 и compiler.err.cannot.assign.not.declared.guard. При начальных n=0, a[0]=0 выражение `when ++a[0] > 0` успешно скомпилировалось и дало результат 67 при n=0, a[0]=1; после сброса a[0] отдельная ветвь `when bump()` с методом, увеличивающим n, дала 67 при n=1, a[0]=0. Это реальные контрасты, а не запрет всех записей.

Финальная [JLS §14.11.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.1) перечисляет ограничения типа, final/effectively final, definite assignment и constant false, но в её абзаце о guard нет общего запрета присваивания/инкремента внешних полей, который был сформулирован в черновике от апреля 2023 года (сравнение текстов приведено в [A16.3 §4](../../docs/audits/java-core-full-2026-11-24/A16.3-SEALED-PATTERNS.md#L90); сам черновик в рамках D-A16b заново не проверялся). Черновик не является окончательной спецификацией; подтверждённого erratum или установленной ошибки компилятора здесь нет. Не выводите из результата этого javac универсальный запрет мутации или гарантию для всех реализаций. Практически guard лучше оставлять чистой проверкой условия.

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

Проверка использует accessor, а не читает поле записи напрямую. Например, в отдельном Probe у record Trace(Object a,Object b) метод a() дописывал "a" в журнал, b() — "b". Для `new Trace(67, "x") instanceof Trace(String x,var y)` результат false, журнал "a": первый компонент не совпал, второй accessor в этом запуске не вызван. У Count(int n) accessor n() дописывал "n". Switch с `case Count(int n) when n < 0` и затем `case Count(int n)` для Count(67) вернул 67, журнал "n", а не "nn". Это измерение на OpenJDK 21.0.12.1, не гарантия ровно одного вызова на каждый текстовый case. Не рассчитывайте на число вызовов accessor при повторном сопоставлении; для устойчивой семантики предпочтительны стабильные значения компонентов и accessors без побочных эффектов. См. [record](records.md).

**Вложенность и var:** `var x` здесь означает inference типа из record component, а не неявное преобразование во всё что угодно. Например, x/y объявлены как int; нельзя объявить внутренний String pattern для int. Вложенные record patterns можно комбинировать с отдельными bindings и guards, но generic сложные комбинации стоят отдельной проверки PM-26/P2.

**Применимость и вывод типа.** В Java 21 внешний type pattern (в instanceof или case) объявляет переменную ссылочного типа, не var; var разрешён как компонент record pattern. Вложенный pattern с примитивным типом должен иметь ровно тип соответствующего компонента: для int законно int n, но long n отклонён, несмотря на обычное widening; бокса или распаковки для такой применимости не предполагается. Record pattern требует record-класс и число patterns, равное числу компонентов. Отдельные отрицательные компиляции подтвердили top-level var, long для int, неверную арность и String(var s), где String не record.

~~~java
record Box<T>(T value) {}
static String infer(Box<String> b) {
    if (b instanceof Box(var s)) {
        return s == null ? "null" : s.toUpperCase();
    }
    return "no";
}
~~~

В Probe для Box<String>("x") получено X, для Box<String>(null) — строка "null". Отсутствие аргументов типа у Box в record pattern запускает вывод типа, здесь Box<String>, поэтому s имеет тип String; это не обычное объявление raw Box. В отдельном Extra тот же вывод с `case Box(var s)` в switch по Box<String> дал X. Напротив, `Object o; o instanceof List<String> s` отклоняется из-за непроверяемого приведения (если o инициализирована), а `Box<String> b; b instanceof Box<Integer>(var i)` — из-за несовместимых типов. См. [JLS §14.30.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.30.1), [§14.30.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.30.3), [generics](generics.md) и [erasure](erasure.md).

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

Точное условие по [JLS §14.30.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.30.1) (определение совпадения с null) и [JLS §14.30.2–3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.30.2): type pattern может совпасть с null, только если непосредственно находится в списке компонентов record pattern и безусловен (unconditional) для объявленного типа соответствующего компонента. Для ссылок безусловность type pattern определяется отношением подтипов после erasure; сам внешний type pattern не совпадает с null, как и любой record pattern. Поэтому результат true ещё не обещает, что каждый binding ненулевой.

| Фрагмент (R(String s), Pair(Object a,Object b)) | Фактический результат |
|---|---|
| new R(null) instanceof R(String s) | true, s == null |
| new Pair(null,67) instanceof Pair(String s,var b) | false |
| new Pair(null,67) instanceof Pair(var s,var b) | true, s == null |
| null как селектор instanceof String s или R(String s) | false |

Все четыре контраста проверены в Probe (в последнем случае два отдельных вызова); для Pair(String s,var b) сужение Object до String не безусловно, а var s выводит Object. Не путайте совпадение с доступностью binding или гарантией безопасного вызова метода на нём.

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

Если accessor завершается аварийно, правило распространяется и на Error. В отдельном Probe accessor Broken.n() бросает заранее созданный AssertionError ACCESS. И instanceof с Broken(int x), и switch с таким case завершились MatchException, причём cause — именно тот же объект ACCESS (проверены точный класс и identity). Прямой вызов new Broken(1).n() бросил сам AssertionError без обёртки. Отдельно выражение селектора, guard и тело выбранной ветви бросали заранее созданный IllegalStateException GUARD; каждый раз наружу вышел тот же объект, а не MatchException. Guard типа Boolean со значением null дал точный NullPointerException (§6). Проверки требуют ожидаемый класс/причину и отклоняют отсутствие или другой вид исключения. Контракт [MatchException](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/MatchException.html) относится к сбою matching, а не ко всем исключениям в switch.

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

При colon labels запрещён fallthrough, при котором управление входит в группу с pattern binding, не установив этот binding. Например, в `case String s: System.out.println(s); case Integer i: System.out.println(i); break;` путь из String-ветви не связал i: отдельная компиляция отклонена с flows.through.to.pattern. При отдельных arrow rules имя s можно повторно использовать в разных ветвях (проверено в Probe). Java 21 также не допускает произвольный список binding patterns `case String s, Integer i -> ...` (flows.through.from.pattern).

Это не общий запрет colon patterns. В отдельном Warn проверен record Empty() без компонентов и следующий фрагмент:

~~~java
static int f(Object o) {
    int n = 0;
    switch(o) {
        case String s: n++;
        case Empty(): n++; break;
        default: break;
    }
    return n;
}
~~~

Вызовы f("x") и f(new Empty()) дали 2:1. При javac --release 21 -Xlint:all (без -Werror) компиляция завершилась с exit 0 и предупреждением compiler.warn.possible.fall-through.into.case; JVM exit 0. Здесь Empty() не объявляет binding, который нужно установить перед входом. Предупреждение не является ошибкой языка. Для ясности обычно лучше избегать намеренного fallthrough.

**Guard только для части значений:** `case Success s when s.value() > 0` не покрывает Success(0), если следующей ветви нет. G1 N23 — non-exhaustive switch-expression с guarded branch. Конкретные diagnostic keys зависят от реализации/версии javac; норма JLS устанавливает некорректность программы, не фиксированный текст диагностического сообщения.

**Предел PM-22:** применимость и dominance — разные проверки. Например, при селекторе Box<String> pattern Box(var s) выводит Box<String> (§7) и применим, а Box<Integer>(var i) при том же типе входа неприменим. Если в switch по Box<String> сначала стоит `case Box<String> x`, а затем `case Box(var s)`, первая негардированная type-ветвь безусловна для типа записи и доминирует над record pattern. Такой отдельный вариант отклонён с pattern.dominated. Сам record pattern не является unconditional: null ему не соответствует. Для generic sealed селектора контраст несовместимой разрешённой ветви приведён в [CORE-SEALED](sealed.md), §5 (ранее проверенный пример Generic<Integer>, Text реализует Generic<String>). Глубокая теория wildcard capture и nested generic inference сверх примеров этой статьи выделена в PM-26/P2.

**Отдельная ветвь case null:** безусловный негардированный type pattern для статического типа селектора нельзя сочетать с default или case null, default (оба варианта отклонены с unconditional.pattern.and.default). При этом `case Object x -> 67; case null -> 0;` в switch по Object допустим: фактически для null получен 0, для строки — 67. Default перед case null или pattern label запрещён; объединённый case null, default обязан быть последней меткой. В двух отрицательных компиляциях неверный порядок дал pattern.dominated. Обычный default перед традиционной константой допустим: `switch(i) { default -> 0; case 67 -> 67; }` при i=67 дал 67. Последнее — switch-expression по int. Не следует писать несколько null labels и ожидать runtime выбора: это ошибка компиляции.

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

Все 20 строк и исходный код целиком приведены в [G1 §7.2](../../docs/plans/CORE-SEALED-PATTERNS.md). Они реально проверены исследованием G1 и независимой проверкой **плана G1** ролью 04 на OpenJDK 21.0.12.1 с `javac --release 21 -Xlint:all` без предупреждений и двумя полными процессными stdout после совместной сборки с `SealedProbe.java`. Это доказательства именно G1, а не более позднего G4 интегрированного PR #140; фактическая последующая хронология приведена в разделе об источниках. Это **20 строк одного Java-процесса и 11 строк второго — 31 наблюдение, не 31 независимый тест**. Теперь оба исходника физически опубликованы в [общем runnable Sealed + Patterns](../../examples/core/sealed-patterns/README.md): [PatternsProbe.java](../../examples/core/sealed-patterns/src/PatternsProbe.java) содержит P01–P20, а [SealedProbe.java](../../examples/core/sealed-patterns/src/SealedProbe.java) — S01–S11. Скрипт `verify.sh` компилирует их с Java 21, запускает отдельно и сравнивает полные stdout с эталонами. **31 подписанная строка двух JVM-процессов не является 31 unit-тестом.**

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

Для enum и sealed типов у исчерпывающего switch есть общая идея: компилятор понимает допустимые варианты по статически доступной модели. Подробные конструкторы, identity, `EnumSet`/`EnumMap` и enum-методы описаны в отдельной [CORE-ENUMS](enums.md), а базовые [условия, циклы и `switch`/`yield` — в CORE-CONTROL](control-flow.md). Эта таблица о patterns **не заменяет** соответствующие самостоятельные статьи или изучение всех правил бинарной эволюции enum; это пояснение относится к содержанию статьи, а не к историческим шагам её публикации.

Традиционный enum switch-statement с одними константами может быть неполным: для enum E{A,B} и единственной ветви `case A -> n[0]++` отдельный Extra при E.B вернул 0. Добавление case null делает statement enhanced; вариант с case null и case A, но без покрытия B, отклонён с not.exhaustive.statement. Switch-expression в обоих случаях обязан быть исчерпывающим.

В Java 21 enum constant может быть квалифицированным именем даже при селекторе общего ссылочного типа, если константа assignment-compatible с ним. В Probe:

~~~java
interface Marker {}
enum G implements Marker { A }
enum H implements Marker { B }
static String enumCase(Marker m) {
    return switch(m) {
        case G.A -> "A";
        case H.B -> "B";
        default -> "other";
    };
}
~~~

Вызовы с G.A/H.B дали A/B. Здесь G.A и H.B — константы, не patterns; default нужен для остальных ненулевых реализаций Marker. При селекторе String метка G.A из несовместимого enum отклонена отдельно. Если enum расширится новыми константами, как и при изменении sealed-иерархии, проверку покрытия при перекомпиляции нужно выполнить заново; поведение старого отдельно собранного клиента здесь не измерено.

**PM-25 P2:** полноценный эксперимент со старым отдельно скомпилированным exhaustive switch, новой permitted ветвью и тем, что реально делает JVM после смены binary, **не выполнялся**. Нельзя использовать P20 с Yes(null) как доказательство binary compatibility failure: P20 — иной runtime mechanism.

**PM-26 P2:** nested generic record inference, wildcard capture и полная математика applicability — отдельная глубокая тема. Простые Point и Envelope не заменяют её.

**PM-27 P2:** unnamed patterns/variables JEP 443 — **preview JDK21**, не часть стандарта `javac --release 21` без `--enable-preview`. Primitive switch/pattern enhancements новых релизов также не переносятся назад в Java21. В code snippets здесь используется стабильное `when`.

**PM-28 P2:** named module runtime, bytecode/JVMS, профилирование JVM, JIT/GC/JMH, GUI Obsidian/IntelliJ и проверки на альтернативных JDK/OS не выполнены; из 20 наблюдений `PatternsProbe` нельзя делать вывод о производительности и кросс-JVM timing.

## Нормативные источники и честность evidence

Первоисточники: [JLS Java21 §14.11 switch](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11), [§14.30 patterns](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.30), [§6.3 flow scope](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3), [§15.20.2 instanceof](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2), [§15.28 switch expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.28), [MatchException Java21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/MatchException.html). [JEP 440](https://openjdk.org/jeps/440)/[JEP 441](https://openjdk.org/jeps/441) — история; независимые C1/C2/C3 источники из [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) — материал для интервью-вопросов, не гарантии языка.

**Границы доказанного и хронология:** в исследовании **G1 и его независимой проверке ролью 04** были проверены два исходника с **11+20** строками stdout, **24 изолированных compile-negative** по категориям javac и положительная компиляция **named-module M01** (без запуска через `java --module-path`). Отдельно [GitHub Actions #44968020486](https://github.com/Melchiorius/backend-interview-labs/actions/runs/44968020486) завершился **completed/SUCCESS на точном HEAD `07d0a83bf4dce1f6a59952109bbee6acb2766640`** авторского [PR #140](https://github.com/Melchiorius/backend-interview-labs/pull/140): Gradle/JUnit и положительный runnable `SealedProbe.java` / `PatternsProbe.java` с двумя процессами и полными сравнениями **11+20 строк stdout** прошли. Это историческое свидетельство относится именно к названному SHA: **после любых изменений необходим CI уже нового HEAD**. В этот CI не входят N01–N24, проверка M01/JVM module-path и автоматическая компиляция всех отдельных Java-фрагментов статьи. На старом HEAD зафиксирован [G3 CHANGES #6009272129](https://github.com/Melchiorius/backend-interview-labs/pull/140#pullrequestreview-6009272129). Позднее на другом точном HEAD `b4b77f7fb155b64e003fa77379766817c084e52b` получены [повторный G3 PASS #6009305995](https://github.com/Melchiorius/backend-interview-labs/pull/140#pullrequestreview-6009305995), [G4 PASS #6009330818](https://github.com/Melchiorius/backend-interview-labs/pull/140#pullrequestreview-6009330818) и [решение G5 #7072090020](https://github.com/Melchiorius/backend-interview-labs/pull/140#issuecomment-7072090020). PR #140 действительно объединён squash-коммитом `69d61c427e7efcab89a55652830050ef16041c8e`. Эти исторические решения не являются ревью нынешней доработки.

**Фактическая проверка D-A16b (02).** На Debian OpenJDK/javac 21.0.12.1 временные Probe.java и Extra.java отдельно скомпилированы с `javac --release 21 -encoding UTF-8 -Xlint:all -Werror -XDrawDiagnostics`, затем выполнены в отдельных JVM. Все четыре вызова завершились с exit 0; оба потока компилятора и stderr JVM пусты. Получено 18 строк stdout: 17 от Probe и одна от Extra. Семь строк подтверждают проверки точных классов исключений, в том числе идентичности причины или объекта исключения в соответствующих случаях; остальные одиннадцать содержат другие наблюдения и метки. Это не 18 тестов. SHA-256 stdout Probe `5f0d604c0fc24c17796ea4aee2d49c357170a61cdb90a390855733c9379b2340`, Extra `96555584442d4ba1e2736bc67d0376cf695ead0bc4937609dd08d72ffea4c15d`. Отдельные 33 контекстуальные отрицательные компиляции дали exit 1 и тематические ключи; Warn.java с fallthrough дал exit 0 с предупреждением, отдельная JVM дала 2:1. Исторические 24 отрицательные компиляции G1, новые 33 и 24 маркера EXPECTED REJECTION полного CI относятся к разным наборам проверок, их не суммируют. Полные временные исходники, команды, результаты и контрольные суммы — [приложение проверки 02](https://github.com/Melchiorius/backend-interview-labs/issues/378#issuecomment-7240616158).

Общий runner проверен ролью 02 без инструментирования: пять восстановленных входных файлов с точными Git blob SHA и режимами, не полный клон. Три запуска (исходное окружение, LC_ALL=C/TZ=Pacific/Honolulu, LC_ALL=C.utf8/TZ=Asia/Tokyo) завершились с exit 0 и пустым внешним stderr. Одна компиляция javac --release 21 -Xlint:all двух исходников, проверка отсутствия stderr компилятора, две отдельные JVM, два diff -u с эталонами, вывод фактических трасс и маркера; сам скрипт не проверяет отдельно JVM stderr, количество строк или SHA-256. Внешняя сверка подтвердила S11/111 и P20/279 байт с эталонами, одинаковый полный stdout32/473 байта, SHA-256 `e929cf66fe84455c8bab9469e49a5759d5bb4ff7b941c5611d0c363c39c6efed`. В отдельном Probe при этих окружениях измерены en_US/Etc/UTC/UTF-8, en_US/Pacific/Honolulu/UTF-8, en/Asia/Tokyo/UTF-8; это не измерения внутри JVM runner. Доступны только C, C.utf8, POSIX. Полный replay исторических negatives/M01 и компиляция всех встроенных фрагментов не проводились.

**Независимая практическая проверка D-A16b (03).** Адаптированный временный Boundary.java скомпилирован на OpenJDK 21.0.12.1 командой `javac --release 21 -encoding UTF-8 -Xlint:all -Werror -XDrawDiagnostics -d classes Boundary.java` и запущен отдельной JVM. Компиляция и запуск завершились с exit 0; оба потока компилятора и stderr JVM пусты. Получено 17 строк stdout (323 байта, SHA-256 `53b43b09237fb5c80dd9353db5f06eb7aa15dd1ac5705164d1901f2781f400cc`): семь меток проверок точных классов исключений, включая идентичность причины или объекта в соответствующих случаях, и десять других наблюдений и меток, в том числе параметры окружения. Отсутствие ожидаемого исключения или неверный класс либо причина приводили к провалу проверки. Это не 17 тестов. Подтверждены порядок вычисления селектора и guard, эффекты изменения элемента массива (`array=0:1`) и вызова метода (`method=1`), вывод generic-типа и совпадение с null. В этой проверке получены и проверены `short=a` и `count=n`: наблюдаемое число вызовов accessor не является переносимой гарантией. Исходник адаптирован для проверки механизмов, а не для дословного повторения всех фрагментов статьи.

Роль 03 проверила 33 различных изолированных отрицательных исходника. Первая попытка остановилась после пяти компиляций: ожидаемый диагностический ключ для FinalBinding был записан неверно. После исправления ожидания все 33 случая выполнены повторно в рамках полного прогона, поэтому фактически было 38 вызовов javac, из них пять повторных, а не 38 различных случаев. В итоговом прогоне каждая компиляция дала exit 1, пустой stdout и одну ошибку; для некоторых случаев программа проверки проверяла только код завершения, а тематические диагностические ключи сверены вручную по выведенным результатам. В частности, четыре варианта записи во внешнее поле в guard дали `compiler.err.cannot.assign.not.declared.guard`. Это наблюдение той же версии javac, а не новый вывод о черновике JLS или доказательство ошибки компилятора. Независимая проверка охватила доминирование type pattern над record pattern без generics, запрет сочетания безусловного pattern с default, допустимость отдельной case null и ограничения порядка объединённой метки. Отрицательные варианты с generic dominance и сочетанием безусловного pattern с case null, default проверены именно ролью 02. Отдельный Warn.java у роли 03 скомпилирован без -Werror с exit 0 и предупреждением `compiler.warn.possible.fall-through.into.case`; JVM завершилась с exit 0, результат `2:1` проверен. Это не отрицательная компиляция и не строгая компиляция без предупреждений. Исторические 24 отрицательные компиляции G1, 33 случая роли 02, 33 различных случая роли 03 (38 вызовов) и 24 маркера CI относятся к разным проверкам и не суммируются.

Роль 03 также отдельно выполнила три запуска неизменённого runner без инструментирования на пяти восстановленных входных файлах с проверенными Git blob SHA и режимами (не полный клон). В исходном окружении, LC_ALL=C/TZ=Pacific/Honolulu и LC_ALL=C.utf8/TZ=Asia/Tokyo получены exit 0 и пустой внешний stderr. Во всех трёх запусках S11/111 и P20/279 байт совпали с полными эталонами, весь stdout32/473 байта — с ожидаемыми трассами и маркером. Внешние проверки подтвердили число строк, размеры и SHA-256 (для полного stdout `e929cf66fe84455c8bab9469e49a5759d5bb4ff7b941c5611d0c363c39c6efed`); внутри скрипта нет отдельной проверки stderr JVM, количества строк или SHA-256. Настройки locale/zone/charset измерены в отдельном Boundary при тех же окружениях, а не внутри JVM runner; внутренний stderr отдельно не захватывался. Полные исходники, команды, диагностика и результаты — [отчёт независимой проверки 03](https://github.com/Melchiorius/backend-interview-labs/pull/379#issuecomment-7240803547).

**NOT RUN:** N01–N24 в GitHub CI, независимая компиляция всех Markdown snippets, real JPMS module-path, binary evolution after separate compilation, JMH/JIT/GC, расширенные внутренние механизмы enum, другие JDK/OS, GUI и ссылочные якоря.

## Самопроверка

1. При `!(o instanceof String s) || s.isEmpty()` справа вычислится метод, если o равен 67? А если o — "x"?
2. В switch по Integer сначала `case Integer i when i > 0 -> 1`, затем `case 67 -> 2`, затем `case Integer i -> 3`. Что вернётся для 67? Что произойдёт, если удалить первый guard?
3. Объявлены record R(String s) и Pair(Object a,Object b). Сравните результаты сопоставления R(null) с R(String s) и Pair(null,67) с Pair(String s,var b).
4. Accessor record бросает AssertionError во время matching; guard после успешного type match бросает IllegalStateException. Какие исключения увидит вызывающий код?

> [!note]- Ответы
> 1. Для 67 — нет, левый операнд true. Для "x" — да, s связана, результат false (§2).
> 2. Результат 1; без guard следующая константа доминируется и компиляция отклоняется (§5).
> 3. Первый pattern совпадёт и свяжет s с null, второй не совпадёт (§8).
> 4. MatchException с причиной AssertionError в первом случае, исходный IllegalStateException во втором (§9.1).
