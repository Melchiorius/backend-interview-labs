# CORE-ASSERT — внутренние инварианты, JVM-флаги и обязательная валидация в Java 21

**Граница материала:** Java SE 21, без preview. Основание — [принятый исследовательский план G1](../../docs/plans/CORE-ASSERT-OPTIONAL.md), [поручение G2 #162](https://github.com/Melchiorius/backend-interview-labs/issues/162). Карта ниже: **AS-01–AS-11 (11 P0), AS-12–AS-17 (6 P1), AS-18–AS-20 (3 P2)**. Это самостоятельная статья о механизме `assert`; [CORE-OPTIONAL](optional.md) описывает допустимое отсутствие результата. Уже опубликованные [исключения](exceptions.md), [инициализация](initialization.md), [операторы](operators.md) и [передача аргументов](parameters.md) раскрывают соседние механизмы, но не заменяют отдельный контракт assertions.

Представьте метод, который завершил внутреннее вычисление и ожидает, что индекс всегда неотрицателен. Разработчик может написать `assert index >= 0;`, чтобы искать нарушения **предположения о собственном коде** во время диагностики. Но если этот же метод принимает недоверенное значение извне, такая проверка **не защищает контракт метода**: в обычном режиме JVM assertions по умолчанию отключены, поэтому соответствующее выражение не выполняется. Это различие — исходная точка ответа на собеседовании.

## 1. Что делает `assert`, и почему это не JUnit (AS-01, AS-02, AS-05, AS-10)

У языка есть две формы **оператора** (не метода):

~~~java
assert condition;
assert condition : detail;
~~~

По [JLS Java SE 21 §14.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.10) `condition` (Expression1) должна иметь тип `boolean` или `Boolean`; сообщение (Expression2) может иметь любой **не-`void`** тип. Деталь может быть числом, строкой или объектом; преобразование в текст сообщения выполняет соответствующий конструктор `AssertionError`. Запись `assert 67;` — ошибка компиляции **A-N01**, а `assert false : System.out.println("oops");` — ошибка компиляции **A-N02**, поскольку `println` возвращает `void`.

Вся семантика зависит от того, **включены ли assertions для данного класса**:

| Режим и значение условия | Expression1 | Expression2 | Результат |
|---|---|---|---|
| Disabled | Не вычисляется | Не вычисляется | Оператор ничего не делает |
| Enabled, условие `true` | Вычисляется один раз | Не вычисляется | Управление идёт дальше |
| Enabled, условие `false` | Вычисляется один раз | Вычисляется, если указано | Выбрасывается `AssertionError` |

Исключение: если вычисление `condition` или `detail` само резко завершается (например, бросает исключение), соответствующее исключение распространяется **вместо нормального завершения оператора**; нельзя утверждать, что *всякое* `assert false : expression` обязательно породит `AssertionError` при бросающем `expression`. `AssertionError` — потомок `Error`, а не `Exception`. [AssertionError API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AssertionError.html), [иерархия исключений](exceptions.md).

~~~java
class Invariant {
    static int count;
    static boolean check() {
        count++;
        return false;
    }
    static String explain() { return "condition violated"; }

    static void run() {
        try {
            assert check() : explain();
            System.out.println("continued");
        } catch (AssertionError error) {
            System.out.println(error.getMessage());
        }
        System.out.println("checks=" + count);
    }
}
~~~

При запуске JVM **без** `-ea` вызов `check()` вообще не происходит: будет `continued` и `checks=0`. При **включённых** assertions `check()` вызовется, вернёт `false`, вычислится `explain()`, возникнет `AssertionError`, и после указанного локального `catch` будут `condition violated` и `checks=1`. `catch` нужен лишь для **учебного измерения**; ловить все `Error` и пытаться продолжать работу приложения — не рекомендуемый контракт обработки ошибок.

## 2. Правила включения: не свойство исходного кода (AS-03, AS-04; AS-14 P1)

Для обычной пользовательской программы в новом JVM-процессе assertions по умолчанию **disabled**. Их можно включить или выключить при **запуске** через опции launcher [Java 21 `java`](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html):

~~~bash
java AssertProbe
java -ea AssertProbe
java -da AssertProbe
java -ea:AssertProbe AssertProbe
java -da -ea:AssertProbe AssertProbe
java -ea -da:AssertProbe AssertProbe
java -esa AssertProbe
java -ea -dsa AssertProbe
~~~

`-ea` (то же `-enableassertions`) — включение для пользовательских классов; `-da` (`-disableassertions`) — выключение. Суффикс `:имя.Класса` ограничивает настройку классом, суффикс пакета с `...` — пакетом и его подпакетами, при этом есть отдельная форма для безымянного пакета. **Порядок применения нескольких совпадающих установок имеет значение.** Проверяя последовательность, запускайте **новую JVM для каждого набора флагов**, а не меняйте аргументы в одном процессе.

`-esa` / `-dsa` управляют assertions **системных классов**, а не просто являются синонимами `-ea` / `-da`. В принятом G1 запуск `java -esa AssertProbe` сохранил обычный пользовательский вывод **A0**; это не эксперимент с исполнением assertions *внутри* системных классов. Сочетание `java -ea -dsa AssertProbe` сохранило пользовательский вывод **A1**. Тонкое class/package matching, взаимодействие со своими загрузчиками классов и JPMS — не проверенный здесь runtime.

**Доказательство требует два разных процесса.** В [G1 §6.1](../../docs/plans/CORE-ASSERT-OPTIONAL.md) полная программа `AssertProbe.java` формирует две разные группы по **15 подписанных строк** каждая — A0 (обычный запуск) и A1 (`-ea`). Это две конфигурации, а не два метода JUnit или один процесс с динамическим переключателем. После публикации технического runnable фактическую ссылку на него можно добавить в эту статью; пока исходник и stdout находятся в принятом плане G1, а не в отдельной опубликованной директории.

## 3. Последовательность вычислений и опасный побочный эффект (AS-02, AS-06, AS-15 P1, AS-16 P1)

Рассмотрим выражение `assert ++counter == 1;`. В режиме disabled `counter` не изменится; в enabled — увеличится. В G1 это ровно `AS05=0` в A0 и `AS05=1` в A1. Такая программа меняет прикладное состояние в зависимости от диагностической настройки, поэтому **изменение необходимого состояния внутри `assert` — антипример**, даже если во время разработки тесты проходят.

Вторая тонкость — сообщение. Оно вычисляется **только при enabled + ложном условии**:

~~~java
class SideEffects {
    static int conditionCalls, messageCalls;
    static boolean predicate(boolean ok) {
        conditionCalls++;
        return ok;
    }
    static String description() {
        messageCalls++;
        return "detail";
    }
    static void exercise() {
        assert predicate(true) : description();
        assert predicate(false) : description();
    }
}
~~~

Если assertions выключены, счётчики остаются `0:0`. Если включены, первое условие инкрементирует `conditionCalls`, но `description()` не вызывается. Второе условие инкрементирует `conditionCalls` ещё раз, затем вызывается `description()` и возникает `AssertionError`. **Без перехвата выполнение `exercise()` прервётся**, итог счётчиков перед прерыванием — `2:1`. Именно этот порядок иллюстрируют G1 `AS02/AS04`.

С `Boolean` условием возможно **распаковывание null**: `Boolean flag = null; assert flag;` при включённой проверке приводит к `NullPointerException` во время Expression1, а при выключенной — даже распаковка не исполняется (AS-13 P1, [JLS §5.1.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.8)). В G1 **не было отдельного null-Boolean эксперимента**. Автор 02 дополнительно выполнил временную Java21-пробу `assert flag;` с `Boolean flag=null`: при обычном запуске результат `ok`, при `-ea` — перехваченный runtime `NPE`. Эта проверка **не является частью 15+15 G1 stdout**, технического runner или проектного CI.

## 4. Обязательная проверка входных данных — не `assert` (AS-07, AS-08, AS-09, AS-11, AS-16)

Нарушение внутреннего инварианта и неправильный **аргумент публичной операции** — разные ситуации. Для обязательной проверки используйте обычный контроль потока и API, которые работают независимо от флагов JVM:

~~~java
import java.util.Objects;

class Request {
    private boolean ready;

    static String username(String name) {
        return Objects.requireNonNull(name, "name");
    }
    static int age(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("negative-age");
        }
        return age;
    }
    String execute() {
        if (!ready) {
            throw new IllegalStateException("not-ready");
        }
        return "ready";
    }
}
~~~

`NullPointerException` при `null` — контракт конкретной проверки `requireNonNull`; `IllegalArgumentException` осмыслен для **недопустимого параметра**, `IllegalStateException` — для операции, запрещённой **текущим состоянием**. Язык не угадывает бизнес-контракт автоматически: в каждом API нужно явно решить, какое отсутствие или нарушение запрещено. В G1 `AS09=NPE:value`, `AS10=IAE:negative-age`, `AS11=ISE:not-ready` **одинаковы при `java` и `java -ea`**.

С точки зрения безопасности проверять авторизацию выражением `assert userHasPermission();` **недопустимо**: отключение assertions просто пропускает проверку. Это опасность пропущенной валидации, **не** отдельное тестирование security системы и не рекомендация запускать эксплуатационный опыт.

## 5. Почему `Objects.requireNonNull` с сообщением бывает eager или lazy (AS-08; AS-17 P1)

У `Objects.requireNonNull` есть три важные перегрузки Java 21:

~~~java
Objects.requireNonNull(value);
Objects.requireNonNull(value, "value must be present");
Objects.requireNonNull(value, () -> computeMessage());
~~~

Если передан обычный аргумент типа `String`, **выражение для него вычисляется до вызова метода**. Поэтому `Objects.requireNonNull(good, expensiveMessage())` вызовет `expensiveMessage()` даже при ненулевом `good`. Если передан `Supplier<String>`, передаётся сам объект-поставщик, но его `get()` вызывается только при `value == null`. Создание лямбды/ссылки как аргумента **не то же самое**, что вызов поставщика.

В G1 при ненулевом объекте `AS06=1` после String-message, `AS07=1` после Supplier-message, а при `null` `AS08=NPE:required-message:2`. Это воспроизводимая трасса счётчика вызовов, **не** доказательство производительности. Кроме того, `requireNonNullElse` и `requireNonNullElseGet` возвращают допустимый ненулевой fallback, но их null-политика отличается от `Optional.orElse`: `requireNonNullElse(null,null)` не может вернуть `null`, а `Optional.<String>empty().orElse(null)` может ([Objects API Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html)). Нюансы null-supplier и null-default рассматриваются как P1, а не автоматически пройденный набор runtime-тестов.

## 6. Сообщение `AssertionError` и исключения при вычислении (AS-05; AS-15 P1)

`assert false;` при enabled создаёт `AssertionError` без detail; при локальном `catch (AssertionError ex)` вызов `ex.getMessage()` обычно возвращает `null` — этот случай подтверждён G1 строкой `AS12=AssertionError:true`. `assert false : "expected-67";` даёт сообщение `expected-67` (G1 `AS03=AssertionError:expected-67`).

Если при вычислении Expression2 вызванная функция бросает исключение, до создания `AssertionError` с detail дело не доходит; здесь действует обычная семантика резкого завершения выражения. Сообщения и цепочки причин у `AssertionError` зависят от переданного detail и конструктора; не делайте из единичного примера blanket-вывод, что любая строка/Throwable всегда форматируется одинаково.

## 7. Особый случай инициализации класса: нормативное правило без эксперимента (AS-12 P1)

JLS §14.10 и [§12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2) предусматривают **специальное правило**: assertion, исполняемый **до завершения инициализации собственного класса**, считается включённым, даже если обычная assertion policy для класса disabled. Это предотвращает нежелательную семантику при рекурсивной/циклической инициализации.

Это утверждение **про нормативную семантику конкретного случая**, а не «`-da` игнорируется всегда» или «после загрузки флаг можно менять без ограничений». В G1 не строился и не исполнялся отдельный cyclic-initialization probe, не исследовались произвольные загрузчики, JVMTI, агенты и JPMS. Пример `AssertProbe` не проверяет этот corner case. [CORE-INIT](initialization.md) описывает обычный порядок инициализации, но не закрывает специальную P2-тему.

## 8. От `AssertionError` к `Optional.empty()`: выбор API-контракта (AS-11)

| Ситуация | Типичный контракт | Почему |
|---|---|---|
| Внутреннее предположение после корректного вычисления нарушено | `assert invariant;` как диагностическая проверка | Может быть выключена; не является обязательной защитой |
| Параметр обязан быть ненулевым | `Objects.requireNonNull(parameter, "parameter")` | Обязательная NPE независимо от `-ea` |
| Аргумент нарушает объявленные ограничения | `throw new IllegalArgumentException(...)` | Явная ошибка вызова |
| Текущее состояние не допускает операцию | `throw new IllegalStateException(...)` | Явная ошибка состояния |
| Поиск корректно не нашёл результат | `Optional.empty()` по контракту поиска | Отсутствие — нормальный исход, а не нарушение инварианта |

Пустой `Optional` не должен маскировать нарушение обязательной валидации. И наоборот, `AssertionError` — неподходящий способ сообщить о допустимом отсутствии записи. Разбор возвращаемого контейнера — [CORE-OPTIONAL](optional.md).

## 9. Трасса принятого G1: контроль соответствия двух JVM режимов

| Маркер | Запуск без `-ea` (A0) | Запуск с `-ea` (A1) | Причина |
|---|---|---|---|
| AS01 | `false` | `true` | Присваивание в условии пропущено / выполнено |
| AS02 | `0:0` | `1:0` | Условие true, detail не нужен |
| AS03 | `skipped` | `AssertionError:expected-67` | False assertion; сообщение только при enabled |
| AS04 | `0:0` | `2:1` | Счётчики условия и сообщения |
| AS05 | `0` | `1` | Вредный side effect |
| AS06/AS07 | `1` / `1` | `1` / `1` | String eager, Supplier lazy |
| AS08 | `NPE:required-message:2` | То же | Supplier вызван при null |
| AS09/AS10/AS11 | `NPE:value` / `IAE:negative-age` / `ISE:not-ready` | То же | Mandatory validation |
| AS12 | `skipped` | `AssertionError:true` | No-detail error |
| AS13/AS14 | `67` / `ready` | То же | Успешные обычные методы |
| AS15 | `false` | `true` | Desired assertion status класса |

Две группы по 15 подписанных строк взяты из [принятого G1 §6.1](../../docs/plans/CORE-ASSERT-OPTIONAL.md). В G1 исследователь и независимый рецензент 04 собирали полноценный `AssertProbe.java` на OpenJDK 21.0.12.1 с `javac --release 21 -Xlint:all` и сравнивали stdout в **двух отдельных JVM-процессах**. Дополнительно 04 сверил семь scoped конфигураций с надлежащими A0/A1; это историческое G1 evidence, **не** запуск автором 02 и **не** project CI данной статьи.

## 10. Компиляционные контрпримеры — отдельно от runtime

У G1 только **два** `assert` compiler-negative: `A-N01: assert 67;` (не `boolean/Boolean`) и `A-N02: assert false : System.out.println("message");` (`void` detail). В изолированных полных файлах они дали `javac` диагностические категории `int → boolean` и `void not allowed` на Java 21, не в CI проекта.

`assert false;` **компилируется**; при enabled и достижении оператора это runtime `AssertionError`. `Boolean b=null; assert b;` тоже **компилируется**; при enabled может дать runtime `NullPointerException`. Не смешивайте статическую ошибку типов, ошибку времени выполнения и выключенную проверку.

## 11. Проверка полноты и границы

| ID | Приоритет | Где объяснён механизм и его evidence |
|---|---|---|
| AS-01 | P0 | §1: оператор и skip обеих Expressions, AS01–AS03 |
| AS-02 | P0 | §1, §3: две формы/detail только при false; A-N02 |
| AS-03 | P0 | §2: `-ea/-da`, class-target, A0/A1, AS15 |
| AS-04 | P0 | §2: `-esa/-dsa` и system vs user |
| AS-05 | P0 | §1, §6: `AssertionError extends Error` и detail, AS03/12 |
| AS-06 | P0 | §3: side effect внутри условного assert, AS05 |
| AS-07 | P0 | §4: обязательная проверка аргумента/authorization, AS09 |
| AS-08 | P0 | §5: `requireNonNull` String eager / Supplier lazy, AS06–08 |
| AS-09 | P0 | §4: выбор IAE/ISE и runtime evidence AS10/11 |
| AS-10 | P0 | §1, §10: `boolean/Boolean` vs void detail A-N01/02 |
| AS-11 | P0 | §8: assert/invariant vs допустимое отсутствие Optional |
| AS-12 | P1 | §7: ранняя инициализация, JLS-only; NOT RUN |
| AS-13 | P1 | §3: Boolean null/unboxing, JLS; local probe NOT RUN |
| AS-14 | P1 | §2: scoped flags и порядок; G1 семь scoped launches |
| AS-15 | P1 | §3, §6: message expression и резкое завершение |
| AS-16 | P1 | §4: security precondition anti-pattern |
| AS-17 | P1 | §5: requireNonNullElse/ElseGet, API contract |
| AS-18 | P2 | §7: classloader assertion policy / JPMS NOT RUN |
| AS-19 | P2 | §1: test frameworks/JUnit distinct, separate topic NOT RUN |
| AS-20 | P2 | §7: JIT/GC, производительность NOT RUN |

**Нормативные первоисточники:** [JLS §14.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.10), [§12.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2), [§5.1.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.8); [Java 21 launcher](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html), [AssertionError API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AssertionError.html), [Objects API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html). Интервью-вопросы C1/C2/C3 из [G1 §2](../../docs/plans/CORE-ASSERT-OPTIONAL.md) мотивируют выбор темы, **не** доказывают языковые гарантии или частоту вопросов.

**Что проверено:** 02 использовала **принятое G1 и результаты 04**, а не повторно исполняла каждую короткую Java-программу из текста. Отдельный временный `NullAndAssertProbe.java` был собран на OpenJDK21.0.12.1 командой `javac --release 21 -Xlint:all` без предупреждений; два независимых запуска `java` и `java -ea` подтвердили пропуск/выполнение распаковки null в `assert` (`ok`/`NPE`). Временный файл не публиковался как runnable. При последующей технической интеграции ссылаться на GitHub CI можно только после его фактического completed/SUCCESS **на новом точном HEAD**. **NOT RUN:** отдельная компиляция всех inline Java-блоков статьи, 11 отрицательных примеров в обычном CI, early/cyclic class initialization, реальный assert системного кода при `-esa`, JPMS/classloaders, JIT/GC/JMH, разные JDK/OS, IDE/Obsidian GUI и якоря; P2 не завершены. Не менять `COVERAGE`/`CURRICULUM` до G5.
