# CORE-CONTROL — как выполняется программа: условия, циклы и switch (Java 21)

**Главный вопрос:** какой оператор Java выполняется следующим, сколько раз вычисляется условие, что прекращают `break` и `continue`, и чем `switch statement` отличается от `switch expression`? Это связная теория **Java SE 21 без preview**, не список операторов и их полного precedence. Общее устройство [типов и переменных](types.md), [исключений](exceptions.md), [enum](enums.md) полезно знать заранее. Углублённый pattern matching, `when`, record deconstruction, exhaustiveness и dominance — самостоятельная [CORE-PATTERNS](patterns.md), здесь лишь минимально необходимая граница.

**Основание:** [принятый G1 CORE-ENUMS + CORE-CONTROL](../../docs/plans/CORE-ENUMS-CONTROL.md), Issue [#146](https://github.com/Melchiorius/backend-interview-labs/issues/146), матрица **C-01–C-29 = 19 P0 / 6 P1 / 4 P2**. Норма: JLS Java21 [§14 statements](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html), [§15.28 switch expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.28), [§16 definite assignment](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html), [§14.22 reachability](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.22). Отдельные snippets не считаются проверенными проектной CI лишь потому, что их аналоги есть в исследовательском G1.

## 1. if/else и dangling else: важен синтаксис, а не отступ (C-01)

Условие `if` должно иметь тип boolean или Boolean с соответствующим unboxing; числовое `67` не является истинным значением, как в некоторых других языках. Проверка выполняется перед соответствующей ветвью. При `if (condition) A else B` выполняется **ровно одна** из ветвей, если условие вычислено нормально. Если выражение бросит исключение, ни одна ветвь не начнётся.

~~~java
class DanglingElseDemo {
    static String choose(boolean outer, boolean inner) {
        String result = "none";
        if (outer)
            if (inner) result = "inner-true";
            else result = "inner-false";
        return result;
    }
    public static void main(String[] args) {
        System.out.println(choose(false, true)); // none
        System.out.println(choose(true, false)); // inner-false
    }
}
~~~

**Разбор:** `else` относится к ближайшему предшествующему `if`, у которого ещё нет `else`. При outer=false весь внутренний оператор пропускается; при outer=true/inner=false исполняется внутренняя else-ветвь. Отступы не меняют дерево разбора. Чтобы привязать `else` к внешнему if, сделайте границы очевидными фигурными скобками.

~~~java
class ExplicitBracesDemo {
    static String choose(boolean outer, boolean inner) {
        if (outer) {
            if (inner) return "both";
        } else {
            return "outer-false";
        }
        return "other";
    }
}
~~~

На собеседовании полезно сначала мысленно восстановить вложенную структуру, а потом говорить о выполнении. Иначе ответ «outer=false значит inner-false» возникнет из неверного предположения о принадлежности `else`.

## 2. while и do-while: ноль итераций или хотя бы одна (C-02)

`while (test) body` проверяет test **до** каждой итерации. Если test изначально false, тело не исполняется вообще. `do body while (test);` выполняет тело **до первой проверки**: если тело завершается обычным образом (без break/return/exception), оно исполняется хотя бы один раз.

~~~java
class WhileOrder {
    public static void main(String[] args) {
        int w = 0;
        while (w < 3) { w++; }
        System.out.println(w);       // 3

        int d = 0;
        do { d++; } while (d < 0);
        System.out.println(d);       // 1

        int untouched = 67;
        while (untouched < 0) untouched++;
        System.out.println(untouched); // 67
    }
}
~~~

**Трассировка while:** w=0, test 0<3 → true, тело →1, затем test 1<3 → true, тело →2, затем →3, test 3<3 → false и выход. **Трассировка do:** тело увеличивает d с 0 до 1, test 1<0 → false, выход. Это принципиально разное число вызовов side-effectful test. Для `while(true)` прекращение может происходить через `break`, `return` либо исключение, а не только изменением boolean-условия.

## 3. Basic for: init, test, body, update и тонкости continue (C-03)

Обычный `for (init; condition; update) body` работает в определённом порядке: **init один раз → condition → body → update → condition → ...**. При первой false-condition тело вообще не выполняется. Пустое condition у basic for эквивалентно `true`. Не путайте update-часть после тела с условием перед телом.

~~~java
class ForOrder {
    public static void main(String[] args) {
        int sum = 0;
        for (int i = 0; i < 4; i++) {
            if (i == 1) continue;
            if (i == 3) break;
            sum += i;
        }
        System.out.println(sum); // 2: i=0 и i=2
    }
}
~~~

**Разбор:** i=0 → sum+=0, update i=1. i=1 → continue **не обрывает весь цикл**: управление проходит к update i++ и очередной проверке condition. i=2 → sum+=2. i=3 → break немедленно завершает цикл **без последующего update**. При использовании списка вызовов с побочными эффектами в `update` это отличие может быть наблюдаемым.

**Scope:** переменная, объявленная `for (int i=... )`, имеет определённую область видимости внутри заголовка и тела цикла, но после цикла обращаться к ней нельзя. Переменная, объявленная до for, продолжает существовать после него. Значение `i` — primitive, его локальное изменение не меняет автоматически другие переменные.

## 4. Enhanced for: значение элемента и ссылочный объект — разные вещи (C-04)

Цикл `for (T element : source)` обходит массив или объект, реализующий `Iterable`. При каждой итерации **переменная цикла** получает очередное значение элемента. В массиве `int[]` это копия числа; присваивание переменной не меняет сам массив. Для массива ссылок копируется ссылка на объект: его состояние можно изменить через эту ссылку, но переназначение переменной не заменит элемент массива.

~~~java
import java.util.Arrays;
import java.util.List;

class EnhancedForDemo {
    public static void main(String[] args) {
        int[] numbers = {1, 2, 3};
        for (int item : numbers) item += 67;
        System.out.println(Arrays.toString(numbers)); // [1, 2, 3]

        int sum = 0;
        for (int item : List.of(1, 2, 3)) sum += item;
        System.out.println(sum); // 6

        StringBuilder[] refs = {new StringBuilder("A")};
        for (StringBuilder sb : refs) sb.append("B");
        System.out.println(refs[0]); // AB
    }
}
~~~

**Трассировка:** временная переменная item принимает 1, 2, 3, но не является «ссылкой на слот int[]»; значит +=67 меняет только item. Переменная sb, напротив, хранит ссылку на тот же StringBuilder, который находится в массиве, поэтому append изменяет достижимый объект. Это не то же самое, что присвоить sb новый StringBuilder. Не сводите enhanced for к «всегда копия объекта» — в Java копируется **значение ссылки**.

**Ограничение:** поведение Iterator для произвольной коллекции и модификация её структуры во время обхода — тема [коллекционных контрактов](../../docs/CURRICULUM.md), а не гарантия именно enhanced for. Здесь достаточно понимать, когда и откуда берётся очередной элемент.

## 5. break, continue, метки и вложенные циклы (C-05, C-06, C-18)

Без метки `break` завершает ближайший содержащий его **loop или switch statement**; `continue` пропускает оставшееся тело текущей итерации ближайшего **loop** и переходит к следующему цикловому шагу. С меткой `break label` завершает соответствующий **помеченный оператор**, если он является допустимой окружающей областью. Для `continue label` цель должна быть именно **помеченным циклом**, а не произвольным блоком.

~~~java
class LabeledBreakDemo {
    static int count() {
        int total = 0;
        outer: for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (j == 1) continue;   // только внутренний цикл
                if (i == 2) break outer; // весь внешний for
                total++;
            }
        }
        return total;
    }
    public static void main(String[] args) {
        System.out.println(count()); // 6
    }
}
~~~

**Ручная проверка G1 C09=6:** при i=0 и i=1 каждый внутренний цикл увеличивает счётчик для j=0,2,3: 3+3. При i=2 и j=0 выполняется `break outer`, больше прибавлений нет. Если ошибочно считать `continue` переходом на следующую *внешнюю* итерацию, получится другое число — это и есть типовая ошибка при чтении кода.

~~~java
class LabeledContinueDemo {
    static int count() {
        int total = 0;
        next: for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 1) continue next;
                total++;
            }
        }
        return total;
    }
    public static void main(String[] args) {
        System.out.println(count()); // 3
    }
}
~~~

Здесь при каждом i счётчик увеличится только для j=0, а `continue next` немедленно перейдёт к следующей итерации **внешнего** for, включая его update. Это реальное отличие от `continue;` в первом примере.

**Компиляционные ограничения:** `break;` вне цикла/switch statement (N18), `continue;` вне цикла (N19), `break absent;` без доступной метки (N21) и `continue block;` для обычного помеченного блока (N20) **не компилируются**. Это не `IllegalStateException` во время выполнения: javac знает о неправильной цели до запуска.

## 6. Порядок вычисления и короткое замыкание && / || (C-07, C-16)

Java вычисляет выражения в определённом порядке по спецификации: в частности, условный AND `&&` вычисляет правую часть **лишь если** левая true, условный OR `||` — лишь если левая false. Такие «пропущенные» операнды могут содержать изменения состояния, и тогда результат заметен.

~~~java
class ShortCircuitDemo {
    static int andTrace() {
        int x = 0, y = 0;
        if (++x > 0 && ++y > 0) { }
        if (x > 2 && ++y > 0) { }
        return x * 10 + y;
    }
    public static void main(String[] args) {
        System.out.println(andTrace()); // 11
        int[] counter = {0};
        if (true || ++counter[0] > 0) { }
        System.out.println(counter[0]); // 0
    }
}
~~~

**C13=11:** первая левая проверка увеличивает x до 1, затем `&&` действительно вычисляет правую и увеличивает y до 1. Во второй проверке `x>2` ложна, инкремент y пропускается; `x*10+y = 11`. **C21=0:** `true || ...` не вычисляет правую часть. Если на интервью ответить «обе части вычисляются, потому что оператор бинарный», вы перепутаете `&&/||` с другими операторами. Полная таблица операторов, битовые `&/|`, precedence и conversions — отдельная будущая тема `CORE-OPERATORS`.

**Selector у switch вычисляется один раз** перед сопоставлением с case labels. Это не список независимых `if (selector()==case)`, где выражение selector вызывается заново для каждой ветви.

~~~java
class SelectorOnceDemo {
    static String select(int[] calls) {
        return switch (++calls[0]) {
            case 1 -> "first";
            default -> "other";
        };
    }
    public static void main(String[] args) {
        int[] calls = {0};
        System.out.println(select(calls) + ":" + calls[0]); // first:1
    }
}
~~~

Если selector вызывался бы дважды, его side effect мог бы изменять результат. По Java21 выражение switch вычисляется однократно; далее определяется matching label.

## 7. Legacy switch statement с colon: fall-through не «падает по значениям» (C-08, C-10)

**Классический switch statement** над примитивным int и обычными константными case labels может быть **неисчерпывающим**. Если ни одна метка не подходит и нет default, выполнение продолжится после switch. В colon-форме выполняются операторы после выбранной метки, и если нет завершающего управления, выполнение **переходит к соседнему case body** — даже если его метка не соответствует selector.

~~~java
class LegacySwitchDemo {
    @SuppressWarnings("fallthrough") // намеренно, чтобы показать trace без javac lint warning
    static String legacy(int n) {
        StringBuilder b = new StringBuilder();
        switch (n) {
            case 1: b.append("one");
            case 2: b.append("two"); break;
            default: b.append("other");
        }
        return b.toString();
    }
    static String withoutDefault(int n) {
        String value = "unchanged";
        switch (n) { case 1: value = "one"; break; }
        return value;
    }
    public static void main(String[] args) {
        System.out.println(legacy(1));         // onetwo
        System.out.println(legacy(2));         // two
        System.out.println(withoutDefault(67)); // unchanged
    }
}
~~~

**C03=onetwo:** выбрана метка case 1, после append("one") нет break, поэтому выполняется append("two") под case 2. Это **не** означает, что selector внезапно стал равен 2. **C05=two:** selector 2, вход сразу в case 2, затем break. **C06=unchanged:** n=67 не совпал ни с одной меткой, отсутствие default допустимо именно для такого legacy statement; значение осталось прежним.

Аннотация `@SuppressWarnings("fallthrough")` здесь подавляет предупреждение выбранного компилятора при учебном намеренном fall-through, но **не** отменяет семантику языка и не должна быть общей практикой «скрывать предупреждения».

## 8. Arrow switch statement: без fall-through, но это ещё не expression (C-09, C-23, C-24)

Синтаксис стрелок есть и у switch **statement**, и у switch **expression**. В statement стрелочная ветвь может вызывать метод типа `void`, менять переменную или выполняться как блок; **в отличие от expression**, statement не обязан порождать значение, которое присваивается переменной.

~~~java
class ArrowStatementDemo {
    static String classify(int n) {
        StringBuilder b = new StringBuilder();
        switch (n) {
            case 1 -> b.append("one");
            case 2 -> b.append("two");
            default -> b.append("other");
        }
        return b.toString();
    }
    static void report(int n) {
        switch (n) {
            case 1 -> System.out.println("one");
            default -> System.out.println("other");
        }
    }
    public static void main(String[] args) {
        System.out.println(classify(1)); // one, не onetwo
    }
}
~~~

**C04=one**: после выполнения стрелочной ветви case 1 в соседнюю ветвь не проваливаемся. Важно: **arrow-форма сама по себе не делает switch enhanced switch statement** и не требует автоматически всех констант. Языковой критерий enhanced включает определённые виды label/selector; подробный контраст §10. Попытка написать внутри *switch expression*, ожидающего int, ветку `case 1 -> System.out.println(n)` даёт compiler-negative **N26**: void expression не даёт нужного значения результата. Смешивание colon и arrow case labels в **одном switch block** компилятор отвергает (**N25**).

## 9. Switch expression: значение, exhaustiveness и yield (C-11, C-12, C-19)

Когда нужно **вычислить результат** по конечному набору состояний, используйте switch expression: `String text = switch (stage) { ... };`. Он обязан быть **exhaustive** — компилятор должен видеть покрытие допустимых значений. Для enum это может быть перечисление всех констант без default, как в примере ниже. Если написать switch expression над int с одним case и без default, javac отвергнет его (N16).

~~~java
enum Stage { NEW, RUNNING, DONE }

class YieldDemo {
    static String evaluate(Stage stage) {
        return switch (stage) {
            case NEW -> "new";
            case RUNNING -> {
                int code = 67;
                yield "run:" + code;
            }
            case DONE -> "done";
        };
    }
    static int colonYield(int n) {
        return switch (n) {
            case 1:
                yield 67;
            default:
                yield 0;
        };
    }
    public static void main(String[] args) {
        System.out.println(evaluate(Stage.RUNNING)); // run:67
        System.out.println(evaluate(Stage.DONE));    // done
        System.out.println(colonYield(1));            // 67
    }
}
~~~

**C07/C08:** переключение по Stage exhaustiveness доказано тремя константами NEW/RUNNING/DONE. В arrow block для RUNNING `yield "run:"+code` возвращает **значение из switch expression**, а не из окружающего метода. **C16:** в colon-форме switch expression значение тоже отдаётся через `yield 67`. Внутри выражения это **не** старый `break 67` из исторических preview-версий языка.

**Нельзя пересекать границу switch expression оператором break/continue:** G1 N30 — `break;` в switch-expression body с попыткой выйти наружу, N31 — `continue;` без допустимого loop в этой конструкции; компилятор отвергнет эти изолированные исходники. Не путайте с `break` классического switch **statement**, который законно завершает сам statement. `yield` вне switch expression — compiler-negative N17. `throw` как резко завершающее выполнение выражение/блок имеет отдельную семантику: подробная модель abrupt completion разобрана в [CORE-EXCEPTIONS](exceptions.md).

## 10. Null selector, enhanced statement и набор допустимых типов (C-14, C-15, C-20–C-22)

В Java21 switch над ссылочным типом может **явно** обрабатывать null через `case null`. Если null selector не обрабатывается подходящей null-меткой, будет **NullPointerException**; обычный `default` не служит универсальным null-handler. Это **runtime exception**, не компиляционная ошибка. Для простого legacy enum switch без case null это особенно заметно.

~~~java
class NullSwitchDemo {
    static String classify(String value) {
        return switch (value) {
            case null -> "missing";
            case "A" -> "A";
            default -> "other";
        };
    }
    static String noNullHandler(String value) {
        return switch (value) {
            case "A" -> "A";
            default -> "other";
        };
    }
    public static void main(String[] args) {
        System.out.println(classify(null)); // missing
        try {
            noNullHandler(null);
        } catch (NullPointerException ex) {
            System.out.println("NPE");      // NPE
        }
    }
}
~~~

**Усиленный оператор `switch` (`enhanced switch statement`) в Java 21:** согласно [JLS §14.11.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.2), он возникает, если **хотя бы одно** из условий выполнено: (1) тип выражения выбора не входит в традиционный набор `char`, `byte`, `short`, `int` и их обёрток, `String`, enum; (2) есть метка `case` с шаблоном сопоставления (`pattern`); (3) есть метка `case null`. Поэтому даже оператор `switch` над `String` с `case null` относится к усиленной форме. **Такой оператор обязан охватывать все допустимые варианты**. G1 N24 с одной меткой `case String s -> ...` для `Object` без `default` или полного покрытия не компилируется; обычный `switch` над `int` из §7 без `default` по-прежнему допустим. Наличие `default` само по себе не означает обработку `null` — для неё нужна соответствующая метка. Подробнее о шаблонах и их покрытии — [CORE-PATTERNS](patterns.md).

**Поддерживаемые типы selector в Java21 (C-20 P1):** классические `byte/short/char/int` и их обёртки, `String`, enum; плюс допустимые reference types для pattern switch с правилами §14.11. Java21 без preview **не** позволяет просто написать `switch(long)`, `switch(boolean)`, `switch(float)` или `switch(double)`. Это отличается от возможностей отдельных более поздних preview. Не переносите возможности новых релизов в Java SE21 задним числом.

**String switch (C-21 P1):** метки сопоставляют значения строк по определённой языкoм семантике, а не сравнивают их адреса `==`. Однако default всё равно не обязан перехватывать null; нужен отдельный `case null`, если это ожидаемый вход. Глубокий pattern switch по String и guards уже есть в CORE-PATTERNS.

**Квалифицированные enum case (C-22 P1):** в Java21 `case Status.NEW` **компилируется** — реальный положительный результат G1 EnumProbe **E26**. Его нельзя включать в список compile-negative как «N27». В некоторых более старых заметках по Java приведён иной запрет: здесь ориентируемся на [JLS Java21 §14.11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11) и на успешную пробу конкретного `--release 21`.

## 11. Статические проверки компилятора: reachability и definite assignment (C-13, C-17, C-25)

Есть два разных вопроса, которые компилятор проверяет **до исполнения**: *можно ли достичь оператора* и *обязательно ли переменная получает значение перед чтением*.

~~~java
class DefiniteAssignmentGood {
    static int value(boolean chooseFirst) {
        int result;
        if (chooseFirst) {
            result = 67;
        } else {
            result = 0;
        }
        return result;
    }
}
~~~

Здесь любой путь, доходящий до return, присвоил `result` значение, значит чтение корректно. Если переменная присваивалась лишь внутри `if (b)` **без else**, путь b=false оставил бы её неинициализированной: javac отклоняет *всю программу* (G1 N23). Это не runtime «случайное значение в стеке».

**Reachability:** безусловный `return 67;` завершает выполнение метода. Любой последующий исполняемый оператор в том же блоке после него недостижим и отвергается компилятором: в G1 N22 это отдельная корректно оформленная *отрицательная* проба `return 67; System.out.println(1);`. Это **статическая диагностика**, а не предположение о том, что JVM «никогда туда не дойдёт». Положительный вариант с ранним возвратом из одной ветви:

~~~java
class ReachabilityPositive {
    static int answer(boolean ok) {
        if (ok) return 67;
        return 0;
    }
}
~~~

Требования к меткам switch тоже статические: два `case 1` в одном switch (N14), два `default` (N28) или `case dynamicVariable`, где требуется constant expression (N15), — compiler errors. Употребление `return` и `throw` ведёт к abrupt completion, но не заменяет объяснение типов исключений: [CORE-EXCEPTIONS](exceptions.md).

## 12. Предметная программа G1: C01–C21 без догадок

В [принятом плане G1 §7.2](../../docs/plans/CORE-ENUMS-CONTROL.md) описана самостоятельная Java 21-программа `ControlProbe.java` с **21 подписанной строкой вывода**. Сейчас доступны [сам исходник](../../examples/core/enums-control/src/ControlProbe.java), [ожидаемый вывод](../../examples/core/enums-control/control.expected.stdout) и [инструкция к общему запускаемому примеру](../../examples/core/enums-control/README.md). Вывод этой программы проверяется отдельно от 30 строк `EnumProbe`; Java-фрагменты, встроенные в статью, не становятся автоматически выполненными проверками.

| Метки G1 | Дословные значения | Причина |
|---|---|---|
| C01–C02 | `none` / `inner-false` | dangling else §1 |
| C03–C06 | `onetwo` / `one` / `two` / `unchanged` | colon fall-through, arrow statement, legacy без default §§7–8 |
| C07–C08 | `run:67` / `done` | исчерпывающий switch expression с `yield` §9 |
| C09–C12 | `6` / `3` / `1` / `6` | labelled break, while/do, enhanced for §§2–5 |
| C13–C15 | `11` / `null-explicit` / `NPE` | short circuit, case null, runtime null §6/10 |
| C16–C18 | `67` / `2` / `[1, 2, 3]` | colon yield, continue, копия primitive element §3/4/9 |
| C19–C21 | `first:1` / `3` / `0` | selector once, labelled continue, OR skips RHS §5/6 |

**Читательская проверка:** C09=6 можно получить ручным счётом в двух первых i-итерациях; C20=3 — по одному увеличению в каждой внешней итерации; C13=11 — по двум коротким замыканиям; C03=onetwo — без ошибочного «selector сменился». Это 21 stdout-строка **одного Java-процесса**, а в связанной программе enum — 30 строк **второго** процесса; **51 наблюдение ≠ 51 unit-тест**.

## 13. P2: где мы намеренно останавливаемся (C-26–C-29)

**C-26 P2 — вложенные try/finally, сложный DA и reachability:** спецификация имеет дополнительные ограничения для abrupt completion и definite assignment сквозь finally и вложенные циклы. Проверенные N22/N23 *не* доказывают все эти комбинации. Расширенный набор compiler-contrasts не выполнялся.

**C-27 P2 — separate compilation/binary evolution:** изменение набора permitted/enum-констант после компиляции старого клиента может влиять на фактическое поведение exhaustive switch. **Двухверсионный JVM эксперимент не проводился**. Не выдавайте P20 из [CORE-PATTERNS](patterns.md) за доказательство бинарной эволюции enum.

**C-28 P2 — pattern switch:** специализированные правила `when`, record patterns, nested null, `MatchException` и dominance уже изложены в [CORE-PATTERNS](patterns.md). Здесь нужна граница между базовым управляющим механизмом и отдельной развитой темой, а не декларация повторного полного покрытия.

**C-29 P2 — JPMS, JVM internals, JIT/GC/JMH, alternative JDK:** не выполнены модульный запуск, измерения производительности, байткод-анализ и сравнения реализаций. **CORE-OPERATORS, CORE-SYNTAX и COL-CONTRACTS остаются отдельными curricular ID**, хотя их частные сюжеты пересекаются с условными выражениями и обходом коллекций.

## Карта покрытия C-01–C-29

| ID | Приоритет | Объяснение, трасса, compile-negative или честная граница |
|---|---|---|
| C-01 | P0 | §1 dangling else, G1 C01/C02 |
| C-02 | P0 | §2 while-before vs do-after, C10/C11 |
| C-03 | P0 | §3 basic for init/test/body/update, continue/break |
| C-04 | P0 | §4 enhanced for local copy, C12/C18 |
| C-05 | P0 | §5 break nearest vs labelled outer, C09/N18/N21 |
| C-06 | P0 | §5 continue loop/label, C17/C20/N19/N20 |
| C-07 | P0 | §6 selector evaluated once, C19 |
| C-08 | P0 | §7 colon fall-through C03/C05, suppress intentional lint |
| C-09 | P0 | §8 arrow statement, C04, no fall-through |
| C-10 | P0 | §7 legacy statement no default, C06/N16 contrast |
| C-11 | P0 | §9 exhaustive expression / enum, C07/C08/N16 |
| C-12 | P0 | §9 yield arrow-block/colon form C07/C16, N17 |
| C-13 | P0 | §11 duplicate/default/nonconstant labels N14/N15/N28 |
| C-14 | P0 | §10 case null vs default, C14/C15 runtime NPE |
| C-15 | P0 | §10 enhanced statement exhaustive, N24 |
| C-16 | P0 | §6 short circuit &&/|| and side effects, C13/C21 |
| C-17 | P0 | §11 unreachable/definite assignment compiler errors N22/N23 |
| C-18 | P0 | §5 labels, legal loop target, N20/N21 |
| C-19 | P0 | §9 break/continue cannot exit switch expression, N30/N31 |
| C-20 | P1 | §10 Java21 selector types; long/boolean/float/double unsupported |
| C-21 | P1 | §10 String selector compares values, null handling |
| C-22 | P1 | §10 qualified case Status.NEW legal Java21 E26, N27 excluded |
| C-23 | P1 | §8 void arrow statement vs invalid expression result N26 |
| C-24 | P1 | §8 mixed colon/arrow forbidden, N25 |
| C-25 | P1 | §11 return/throw abrupt completion; exceptions backlink |
| C-26 | P2 | §13 complicated DA/finally NOT RUN |
| C-27 | P2 | §13 binary evolution / separate compilation NOT RUN |
| C-28 | P2 | §13 CORE-PATTERNS already covered elsewhere; no duplication |
| C-29 | P2 | §13 JPMS/JVM/JMH/JIT/GC/preview/other JDK NOT RUN |

## Первоисточники, доказательства и границы

**Нормативные спецификации Java21:** [JLS §14.9/§14.12–14.14](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html), [JLS §14.7/§14.15–14.17](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html), [JLS §14.11 switch](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11), [JLS §14.22 reachability](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.22), [§15.28 switch expression](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.28), [§16 definite assignment](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html), [§15.7/§15.23/§15.24 evaluation/short circuit](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html). [Oracle Java21 switch guide](https://docs.oracle.com/en/java/javase/21/language/switch-expressions-and-statements.html) поясняет, не заменяет JLS. C1–C4 из [G1 §3](../../docs/plans/CORE-ENUMS-CONTROL.md) — реальные вопросы, **не** нормативные гарантии.

**Подтверждение G1 и независимая проверка 04:** на OpenJDK `javac/java 21.0.12.1` оба полных исходника собирались через `javac --release 21 -Xlint:all`, их вывод **30+21 строк** совпал с эталонами. Отдельно проверены **31 изолированный отрицательный пример компиляции** N01–N26/N28–N32 по ожидаемым диагностическим категориям; N11 — намеренная синтаксическая ошибка `enum extends` с каскадным сообщением о конце файла, N27 исключён как допустимый `case Status.NEW`. **Интегрированный G2 CI** [#44978108366](https://github.com/Melchiorius/backend-interview-labs/actions/runs/44978108366) на HEAD `c6c4bfe822130db220aeffd390798a52e2cc9992` успешно скомпилировал два опубликованных Java-файла и сравнил два вывода по [скрипту проверки](../../examples/core/enums-control/verify.sh). Новый HEAD требует нового CI. Все отдельные Java-блоки статьи **не** компилируются автоматически, 31 отрицательный пример **не** входит в штатный CI.

**NOT RUN:** компиляция всех отдельных Java-блоков этой статьи в CI, 31 отрицательный пример в CI, отдельные курсы CORE-OPERATORS, CORE-SYNTAX и COL-CONTRACTS, многопоточные проверки, бинарная эволюция enum после раздельной компиляции, внутренние механизмы Reflection/Serialization, реальный запуск JPMS, GC/JIT/JMH, другие JDK/ОС, GUI Obsidian/IntelliJ и Markdown-якоря. До G5 реестры `COVERAGE/CURRICULUM` не изменяются.
