# CORE-EXCEPTIONS — исключения Java 21: типизация, распространение и управление ошибками

**Версия:** Java SE 21, без preview. **Исследовательская основа:** [G1 CORE-EXCEPTIONS/RESOURCES](../../docs/plans/CORE-EXCEPTIONS-RESOURCES.md) (ER-01–ER-30, Issue [#100](https://github.com/Melchiorius/backend-interview-labs/issues/100), G1 ACCEPTED ролью 00 после PR #102). **Авторское задание:** [Issue #103](https://github.com/Melchiorius/backend-interview-labs/issues/103). Эта статья объясняет, **какое исключение компилятор требует обработать**, **какой объект реально распространяется** и **какая причина может потеряться при `finally`**. Порядок закрытия, primary/suppressed и try-with-resources подробно разобраны в [CORE-RESOURCES](resources.md). Предпосылки: [наследование и overriding](inheritance.md), [классы и конструкторы](classes.md), [инициализация](initialization.md).

## Ошибка не является результатом метода (ER-04)

Представим метод чтения пользовательского файла. Его успешный результат — данные; неудача может иметь несколько причин: путь отсутствует, файл недоступен или байты имеют неожиданный формат. Вместо кодирования ошибки обычным значением Java позволяет методу завершиться **резко** (*abrupt completion*): исполнение текущего выражения прерывается и начинается поиск подходящего обработчика.

~~~java
import java.io.IOException;

class ReaderService {
    static String load() throws IOException {
        throw new IOException("file unavailable");
    }

    static void endpoint() {
        try {
            String data = load();
            System.out.println(data); // не выполняется при данном исключении
        } catch (IOException problem) {
            System.out.println(problem.getMessage()); // file unavailable
        }
    }
}
~~~

Здесь создан **конкретный объект** `IOException`. Он распространяется из `load()` к `endpoint()`; подходящий `catch` перехватывает именно этот объект. Если обработчика нет и исключение покидает границу потока, поведение определяет механизм необработанных исключений потока, а не «автоматический возврат null». Точная языковая модель: [JLS 21 §11.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.3).

Зачем это различие на собеседовании: фраза «ошибка поднимается вверх» недостаточна. Нужно назвать *где* объект создаётся, **какой статический контракт** компилятор проверил, *какие кадры вызова* покидает исполнение и *кто* может обоснованно принять решение о восстановлении или переводе ошибки в ответ.

## Иерархия Throwable и граница checked/unchecked (ER-01, ER-22)

Все значения, которые разрешено бросать выражением `throw`, принадлежат классу `Throwable` или его подклассам. Существенная часть иерархии:

~~~text
Throwable                         checked
├── Error                         unchecked
│   └── AssertionError
└── Exception                     checked (сам класс)
    ├── IOException               checked
    └── RuntimeException          unchecked
        ├── IllegalArgumentException
        └── NullPointerException
~~~

**Checked** — сам `Throwable` и все его подклассы, кроме `RuntimeException`, `Error` и их подклассов. Сам класс Exception checked, но его потомки могут быть unchecked. `catch (Exception e)` не перехватывает Error и прямого потомка Throwable вне ветви Exception. Классификация относится к классам, а выбор обработчика при выполнении зависит от фактического класса объекта. Error можно поймать, но не всякая ошибка допускает восстановление.

~~~java
static void sample() {
    try {
        throw new IllegalArgumentException("invalid 67"); // unchecked
    } catch (RuntimeException e) {
        System.out.println(e.getMessage());
    }
}
~~~

~~~java
class Direct extends Throwable {
    private static final long serialVersionUID = 1L;
}
class Hierarchy {
    static void demo() {
        try {
            try { throw new Direct(); }
            catch (Exception e) { throw new AssertionError(e); }
        } catch (Direct e) {
            System.out.println("Direct bypasses Exception");
        }
    }
}
~~~

В двух отдельных исходниках `void f() { throw new Throwable(); }` и `void f() { throw new Exception(); }` без обработки или throws javac диагностировал неперехваченный checked-тип. Внешний catch в примере выше необходим; AssertionError также минует catch(Exception), но не создаёт checked-обязанности.

Необязательность `throws RuntimeException` **не означает**, что исключение безвредно, что метод гарантирует корректный результат или что вызывающий не должен учитывать ошибку. Язык отвечает за проверяемость типов; бизнес-контракт определяет политику обработки. Нормы: [JLS §11.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.1), [Throwable API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html), [Exception API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Exception.html).

## Три разных действия: throw, throws и catch (ER-02, ER-03)

- `throw expression;` **создаёт abrupt completion** с полученным throwable-значением.
- `throws IOException` в объявлении метода или конструктора **объявляет** возможность распространения исключения и участвует в статической проверке, но ничего не бросает самостоятельно.
- `catch (IOException e)` получает управление, если из соответствующего `try` распространяется совместимый по типу объект. Это не перечень всех возможных ошибок метода.

Рассмотрим минимальный пример с зависимостью API:

~~~java
import java.io.IOException;
class Library {
    static void mayFail() throws IOException {
        // сегодня тело ничего не бросает
    }
}
class Client {
    static void request() {
        // Library.mayFail();  // ошибка компиляции: unreported IOException
    }
    static void requestHandled() {
        try {
            Library.mayFail();
        } catch (IOException e) {
            throw new IllegalStateException("request failed", e);
        }
    }
}
~~~

**Почему ошибка возможна, несмотря на пустое тело?** Компилятор клиента исходит из **статически выбранного объявления вызываемого метода**, а не доказывает, что его реализация всегда безопасна. Библиотека может поменять тело без изменения контракта. Если вызывающий метод тоже объявляет `throws IOException`, он передаёт обязанность обработки следующему уровню; это не устранение ошибки.

Аналогично компилятор анализирует checked-исключения **конструкторов**, **инициализаторов** и **операций закрытия ресурсов**. Объект с `AutoCloseable.close() throws Exception` может создать checked-обязанность даже если тело `try` безобидно — конкретная механика в [CORE-RESOURCES](resources.md).

~~~java
class Bad {
    // static void f() { throw new Object(); }
    // Ошибка компиляции: Object не наследует Throwable
}
~~~

`throw null;` — особый случай: выражение допустимо по правилам типов, но при выполнении приводит к `NullPointerException`; оно не создаёт исключение «с типом null». Подробнее в разделе P1 ниже. Нормы: [JLS §11.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2), [§14.18](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.18), [§8.4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.6).

**Статический тип и фактическое значение.** `throw null;` не требует checked-объявления; `IOException x = null; throw x;` требует обработать или объявить IOException, хотя при выполнении тоже возникает NullPointerException. Для обычного throw проверяются статический тип выражения и исключения его вычисления (особое правило precise rethrow отмечено ниже).

~~~java
import java.io.IOException;
class TypedNull {
    static void raise(IOException x) throws IOException { throw x; }
    static void demo() {
        try { raise(null); }
        catch (IOException e) { throw new AssertionError(e); }
        catch (NullPointerException e) {
            System.out.println("typed NPE");
        }
    }
}
~~~

Если вычисление выражения само завершилось исключением, до бросания его результата дело не доходит: метод `IOException make()` может сам бросить IllegalArgumentException, и именно она распространится при `throw make();`. Новый объект не обязателен: `throw saved;` бросает существующий Throwable. См. JLS §11.2.2 и §14.18.

## Распространение по цепочке вызовов (ER-04)

~~~java
import java.io.IOException;

class Layers {
    static void storage() throws IOException {
        throw new IOException("disk");
    }
    static String service() throws IOException {
        return "data: " + storageValue();
    }
    static String storageValue() throws IOException {
        storage();                 // дальнейший код этого метода не выполняется
        return "ok";
    }
    static void controller() {
        try {
            service();
        } catch (IOException e) {
            System.out.println("503: " + e.getMessage());
        }
    }
}
~~~

Последовательность: `storage()` создаёт `IOException("disk")` → `storageValue()` не перехватывает → `service()` не перехватывает → `controller()` получает объект в `catch`. Java при этом выполняет соответствующие `finally` при выходе из активных защищённых областей, если исполнение продолжается по стандартному пути unwinding. Ресурсы TWR закрываются по своим правилам **до** передачи ошибки внешнему обработчику. Ни один промежуточный метод не «возвращает исключение вместо String».

Обработчики одного обычного try защищают его блок, а не соседние catch или finally. Если catch бросает новый объект, поиск продолжается с внешних защищённых областей.

~~~java
try {
    try { throw new java.io.IOException(); }
    catch (java.io.IOException e) {
        throw new IllegalStateException("catch");
    } catch (RuntimeException e) {
        throw new AssertionError("не этот обработчик");
    }
} catch (IllegalStateException e) {
    System.out.println(e.getMessage()); // catch
}
~~~

Исключение из finally также минует catch этого же try. Для TWR есть дополнительные правила инициализации и закрытия ресурсов — см. CORE-RESOURCES. Когда обработчика нет, исключение достигает границы конкретного потока; при обычном завершении по исключению вызывается его uncaught exception handler. Пример внутри метода, объявляющего InterruptedException:

~~~java
Thread worker = new Thread(() -> { throw new AssertionError("worker"); });
worker.setUncaughtExceptionHandler((t, e) ->
        System.out.println("handler:" + e.getMessage()));
worker.start();
worker.join();
System.out.println("main continues");
~~~

Наблюдаем handler:worker, затем main continues после join. Завершение одного потока не означает автоматической остановки всех потоков. Это конечный пример с заданным обработчиком, не обещание формата stack trace или кода завершения процесса. [Thread API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html), JLS §11.3.

**Практический вывод:** перехватывайте ошибку там, где можно принять осмысленное решение: восстановиться, повторить операцию при подходящих условиях, преобразовать её в контракт верхнего слоя или завершить запрос. Широкий `catch (Exception ignored) {}` скрывает причину и не превращает неудачное действие в успешное.

## Почему порядок catch имеет значение (ER-05)

Обработчики одного `try` рассматриваются по порядку. Более **специфичный** тип должен предшествовать общему: иначе последующий обработчик для подтипа недостижим.

~~~java
import java.io.IOException;

class Handlers {
    static void example() {
        try {
            throw new IOException("io");
        } catch (IOException e) {
            System.out.println("specific");
        } catch (Exception e) {
            System.out.println("other");
        }
    }
}
~~~

Если переставить `catch (Exception)` перед `catch (IOException)`, второй обработчик запрещён: его тип уже покрыт первым. Отдельно проверяется возможность checked-исключения. По JLS §11.2.3 для checked-типа catch нужен хотя бы один статически возможный checked-тип из try, являющийся его подтипом или надтипом (включая тот же тип), **кроме Exception и надтипов Exception**. Пустой try с catch(Exception), catch(Throwable) или catch(Error) по отдельности допустим (Error unchecked), с catch(IOException) — ошибка. Учитываются также предыдущие обработчики и каждая альтернатива multi-catch. [JLS §14.20.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.1), [§11.2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.3).

| Статически возможный checked-тип | Порядок catch | Результат |
|---|---|---|
| IOException | FileNotFoundException, IOException | допустимо: возможны разные подтипы IOException |
| FileNotFoundException | FileNotFoundException, IOException | допустимо; javac предупредил о более широком catch |
| FileNotFoundException | IOException, FileNotFoundException | ошибка: подтип уже перехвачен |

~~~java
import java.io.*;
class CatchCases {
    static void broad() throws IOException { throw new FileNotFoundException(); }
    static void narrow() throws FileNotFoundException { throw new FileNotFoundException(); }
    static void legal() {
        try { broad(); }
        catch (FileNotFoundException e) { System.out.println("specific"); }
        catch (IOException e) { System.out.println("other io"); }
    }
    static void warning() {
        try { narrow(); }
        catch (FileNotFoundException e) { System.out.println("specific"); }
        catch (IOException e) { System.out.println("other io"); }
    }
}
~~~

Вторая функция с таким порядком принята javac 21 с `compiler.warn.unreachable.catch`; обратный порядок отвергнут с `compiler.err.except.already.caught`. Предупреждение не является запретом языка; -Werror может превратить предупреждение в отказ конкретной сборки.

## Multi-catch и запрет изменять параметр (ER-06)

Если разные типы исключений (ни один не подтип другого) приводят к одной обработке, их можно объединить через `|`:

~~~java
import java.io.IOException;
class Multi {
    static void work(boolean io) {
        try {
            if (io) throw new IOException("disk");
            throw new IllegalArgumentException("data");
        } catch (IOException | IllegalArgumentException e) {
            System.out.println(e.getMessage());
            // e = new IllegalArgumentException(); // ошибка: multi-catch parameter final
        }
    }
}
~~~

Для каждой альтернативы отдельно проверяются допустимость и предыдущие обработчики. `catch (IOException | Exception e)` запрещён из-за отношения подтипов. Объявленный тип параметра — наименьшая общая верхняя граница типов альтернатив (LUB), не обязательно просто Exception: может быть доступен метод общего интерфейса. Параметр multi-catch неявно final по объявлению, а не effectively final по использованию (JLS различает категории). Его нельзя переназначить, но объект не становится неизменяемым. Обычный catch без final допускает переназначение; это может изменить условия precise rethrow. [JLS §14.20](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20), [CORE-FUNCTIONS](functions.md).

~~~java
interface Coded { int code(); }
class A extends Exception implements Coded {
    private static final long serialVersionUID = 1L;
    public int code() { return 67; }
}
class B extends Exception implements Coded {
    private static final long serialVersionUID = 1L;
    public int code() { return 68; }
}
class Common {
    static void run(boolean first) {
        try {
            if (first) throw new A();
            throw new B();
        } catch (A | B e) {
            System.out.println(e.code()); // 67 или 68
        }
    }
}
~~~

В отдельной пробе присваивание e = new A() в обычном catch(A e) допустимо. Присваивание multi-catch отклонено с multicatch.parameter.may.not.be.assigned, альтернативы IOException и Exception — с multicatch.types.must.be.disjoint.

## Причина ошибки и оборачивание на границе слоя (ER-07, ER-08, ER-19)

В приложении техническая ошибка чтения может стать ошибкой операции сервиса. Важно не заменить исходную причину одной строкой сообщения:

~~~java
import java.io.IOException;

class PaymentService {
    static void read() throws IOException {
        throw new IOException("remote timeout");
    }
    static void process() {
        try {
            read();
        } catch (IOException cause) {
            throw new IllegalStateException("payment unavailable", cause);
        }
    }
    static void endpoint() {
        try {
            process();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());            // payment unavailable
            System.out.println(e.getCause().getMessage()); // remote timeout
        }
    }
}
~~~

`IllegalStateException` — **новый Throwable**, а `IOException` сохранён через `getCause()`. Это **цепочка причин**, не список suppressed. У TWR исключения от `close()` связываются иначе: `primary.getSuppressed()`, а не автоматически `getCause()`. См. [разбор в CORE-RESOURCES](resources.md).

### Повторный throw, stack trace и initCause

При обычном создании Throwable сохраняется информация о стеке в момент создания, а не обязательно в момент throw. Повторный throw того же объекта не обновляет trace автоматически; явный fillInStackTrace или setStackTrace может изменить его при разрешённой записи. Оборачивание создаёт другой объект с собственным контекстом и сохраняет ссылку на исходную причину.

~~~java
import java.io.IOException;
import java.util.Arrays;
class TraceExample {
    static IOException make() { return new IOException("source"); }
    static void relay(IOException e) throws IOException { throw e; }
    static void demo() {
        IOException saved = make();
        StackTraceElement[] trace = saved.getStackTrace();
        try { relay(saved); }
        catch (IOException e) {
            System.out.println(e == saved);
            System.out.println(Arrays.equals(trace, e.getStackTrace()));
            Throwable wrapped = new IllegalStateException("context", e);
            System.out.println(wrapped.getCause() == saved);
        }
    }
}
~~~

В конечной пробе сохранённый IOException после relay(saved) имел ту же идентичность и равный массив stack frames; наблюдаемый верхний метод — make, не relay. Это не гарантия полного стека или конкретных строк для всех JVM.

`initCause(cause)` разрешён не более одного раза для объекта с ещё не установленной причиной (например, созданного через Throwable() или Throwable(String)). Если конструктор уже принимает причину, даже null, либо initCause уже успешно вызван, повторное задание запрещено (IllegalStateException). Причина может быть null, но не сам объект (IllegalArgumentException для self-cause при доступной операции). Не определяем порядок ошибок при нескольких нарушенных предусловиях. Защищённый конструктор с writableStackTrace=false отключает запись стека: getStackTrace возвращает пустой массив, fillInStackTrace и setStackTrace не записывают стек. Это отдельная настройка от suppression. [Throwable API — initCause](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html#initCause(java.lang.Throwable)).

**Custom exception:** создание `class MissingOrderException extends RuntimeException` или `extends Exception` выбирает *статический контракт для клиентов*, а не просто уровень серьёзности. Checked может быть полезен, если вызывающий ожидаемо умеет устранить ситуацию и контракт явно требует обработку. Unchecked часто описывает нарушение предусловия или невозможность продолжить конкретную операцию, но само по себе **не является указанием игнорировать** её. Слишком широкая checked-иерархия на границе модулей усложняет изменение API; безусловное превращение всех проблем в `RuntimeException` стирает различие решений.

**Инженерная практика, не норма JLS:** логировать исключение на уровне, где известны пользовательская операция, идентификатор запроса и политика ответа; избегать нескольких одинаковых stacktrace на каждом промежуточном уровне; не добавлять секреты в сообщения. Перехват ради логирования с `throw e;` допустим, но не всегда полезен. При восстановлении следует учитывать идемпотентность операции, иначе повтор при частичном успехе может навредить.

> [!note]- Ручное suppressed и отключение suppression (ER-19)
> `Throwable.addSuppressed(other)` добавляет отдельное исключение к списку, не изменяя cause. Даже если suppression отключён, null запрещён (NullPointerException), собственный объект запрещён (IllegalArgumentException); только корректный отличный от себя объект игнорируется при отключении. `getSuppressed()` возвращает массив, изменение которого не влияет на последующие вызовы; это не глубокое копирование Throwable. Причина и suppressed могут сосуществовать, suppressed причины автоматически не наследуются. См. [Throwable API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html).

~~~java
class Quiet extends Exception {
    private static final long serialVersionUID = 1L;
    Quiet(Throwable cause) { super("quiet", cause, false, false); }
}
class Suppression {
    static void demo() {
        Throwable cause = new Exception("cause");
        Throwable other = new Exception("other");
        cause.addSuppressed(other);
        Throwable outer = new Exception("outer", cause);
        System.out.println(outer.getSuppressed().length); // 0
        outer.addSuppressed(other);
        Throwable[] copy = outer.getSuppressed();
        copy[0] = cause;
        System.out.println(outer.getSuppressed()[0] == other); // true
        System.out.println(outer.getCause() == cause); // true
        Quiet quiet = new Quiet(cause);
        quiet.addSuppressed(other);
        System.out.println(quiet.getSuppressed().length); // 0
        System.out.println(quiet.getStackTrace().length); // 0
    }
}
~~~

В отдельной проверке quiet.addSuppressed(quiet) вызвал IllegalArgumentException, quiet.addSuppressed(null) — NullPointerException. Границы автоматического подавления, включая self-suppression при TWR, рассматриваются отдельно в CORE-RESOURCES; здесь проверяется ручной API.

## Почему finally может изменить уже принятый результат (ER-09, ER-10)

Использование `finally` часто объясняют фразой «выполняется всегда». В обычном потоке исполнения оно выполняется при выходе из `try` или `catch` как с нормальным, так и с резким завершением. Но **само выражение завершения уже могло вычислиться**, и `finally` вправе заменить прежнюю причину abrupt completion.

~~~java
class FinallyCases {
    static int value() {
        int n = 67;
        try {
            return n;         // выражение return вычислилось: 67
        } finally {
            n = 68;           // меняет local variable, НЕ возвращённое значение
        }
    }

    static int overrideReturn() {
        try {
            return 67;
        } finally {
            return 68;        // подавляет ранее подготовленный return 67
        }
    }

    static void overrideException() {
        try {
            throw new IllegalArgumentException("original");
        } finally {
            throw new IllegalStateException("new"); // original теряется как primary
        }
    }
}
~~~

Для `value()` результат **67**, для `overrideReturn()` — **68**. Для `overrideException()` наружу уходит `IllegalStateException("new")`. Исходная `IllegalArgumentException("original")` **не добавляется автоматически** в `getSuppressed()` новой ошибки. Обычный `finally` не обеспечивает TWR-механику сохранения primary/suppressed — именно ради этой разницы сравните [CORE-RESOURCES](resources.md).

### Возврат ссылки и изменение объекта

~~~java
static StringBuilder valueRef() {
    StringBuilder b = new StringBuilder("67");
    try { return b; }
    finally {
        b.append("68");
        b = new StringBuilder("other");
    }
}
~~~

При нормальном завершении finally вызывающий получает ранее вычисленную ссылку, а не текущее значение локальной переменной b. Объект изменился, поэтому valueRef().toString() даёт 6768, не other. Если finally завершится резко, возврат может вообще не состояться. Связанный принцип — [CORE-PARAMS](parameters.md).

### Замещение причины и статическая проверка checked

Если try/catch завершился резко с причиной R, нормальное завершение finally сохраняет R; резкое завершение finally с причиной S заменяет R на S. Причиной может быть return, throw, break, continue и другие допустимые abrupt completions, а не только исключение. Если finally не завершится, прежняя передача управления тоже не продолжится.

~~~java
static int swallow() {
    try { throw new java.io.IOException(); }
    finally { return 67; }
}
static int breakReturn() {
    while (true) {
        try { return 67; }
        finally { break; }
    }
    return 68;
}
static int continueReturn() {
    for (int i = 0; i < 1; i++) {
        try { return 67; }
        finally { continue; }
    }
    return 68;
}
~~~

Намеренно плохие учебные примеры, не совет по обработке ошибок. Они компилируются с предупреждением javac `finally.cannot.complete` при -Xlint:all; результаты 67, 68, 68. По JLS §11.2.2 при **статически** невозможном нормальном завершении finally checked-исключения из try/catch не входят в множество исключений, распространяемых всем try statement. При условном `finally { if (b) throw new IllegalStateException(); }` нормальное завершение возможно: неперехваченный IOException из try всё ещё требует обработки/throws. Checked-исключения, возможные из самого finally, не исчезают: `try { throw new Exception(); } finally { throw new IOException(); }` требует обработать или объявить IOException, а не исходный Exception. Это статический анализ, не доказательство поведения конкретного входа.

**Два важных ограничения.** Во-первых, `return`/`throw` в `finally` практически всегда затрудняет диагностику и считается опасным инженерным приёмом. Во-вторых, `finally` не обещает исполнения **после прекращения JVM или процесса** (например, `Runtime.halt`, принудительного завершения ОС, аппаратного сбоя). Он описывает поток управления **в продолжающей исполняться программе**, а не гарантию сохранения данных. Норма: [JLS §14.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.2).

## Checked исключения в наследовании и функциональных контрактах (ER-20, ER-26)

Переопределяющий метод не может расширять набор **checked** исключений относительно разрешённого контрактом родительского метода. Более узкое или отсутствующее checked-объявление может быть допустимо; unchecked не ограничиваются таким же правилом.

~~~java
import java.io.IOException;
class Parent {
    void run() throws IOException {}
}
class Child extends Parent {
    @Override void run() {}              // корректно: сужение throws до пустого
    // @Override void run() throws Exception {} // ошибка компиляции
}
~~~

Для проверки неверного override замените существующий Child.run на закомментированную строку, а не добавляйте второй метод; иначе возникнет также ошибка дублирования. В отдельном исходнике с единственным неверным override javac выдал `compiler.err.override.meth.doesnt.throw`. В строгом смысле [JLS §8.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.2) throws clause не является частью сигнатуры метода: две декларации f() с разными throws не образуют перегрузку. Это подтверждено отдельной ошибкой already.defined.

При вызове `Parent p = new Child(); p.run();` компилятор рассматривает статически выбранный `Parent.run() throws IOException`, даже если реальная реализация `Child.run()` не объявляет checked-исключений. Подробный overriding: [CORE-INHERITANCE](inheritance.md), правила [JLS §8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3).

Конструктор с `throws IOException` тоже создаёт статическую обязанность на месте new. Для именованного класса checked-исключение не может покинуть статический инициализатор или инициализатор статического поля. Для инициализатора экземпляра (блока или поля) такое исключение допустимо только если есть хотя бы один явно объявленный конструктор и **каждый** конструктор явно объявляет этот checked-тип или его надтип. Для анонимного класса компилятор выводит подходящий throws генерируемого конструктора; вызов new всё равно требует обработки. См. JLS §11.2.3, [CORE-CLASSES](classes.md), [CORE-INIT](initialization.md).

~~~java
import java.io.IOException;
class InitExample {
    static int fail() throws IOException { throw new IOException("init"); }
    static class Named {
        int n = fail();
        Named() throws IOException {}
        Named(int x) throws Exception {}
    }
    static void demo() {
        try { new Named(); }
        catch (IOException e) { System.out.println("named:" + e.getMessage()); }
        try { new Object() { int n = fail(); }; }
        catch (IOException e) { System.out.println("anonymous:" + e.getMessage()); }
    }
}
~~~

Отдельные отрицательные исходники: static int x=fail() в именованном классе; instance int x=fail() без явного конструктора; второй явный конструктор без подходящего throws — все отклонены с unreported.exception.need.to.catch.or.throw. Это не повтор полной модели инициализации CORE-INIT.

Функциональный интерфейс задаёт контракт checked-исключений тела лямбды:

~~~java
import java.io.IOException;
class Lambdas {
    static void checked() throws IOException { throw new IOException("x"); }

    // Runnable bad = () -> checked(); // ошибка: Runnable.run() не объявляет IOException
    static Runnable safe() {
        return () -> {
            try { checked(); }
            catch (IOException e) { throw new IllegalStateException(e); }
        };
    }
}
~~~

Лямбда может бросать checked. Например, стандартный [Callable<V>](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Callable.html) объявляет call() throws Exception, в отличие от Runnable.run(). Вызов проверяется по статическому функциональному типу, не по конкретному телу.

~~~java
import java.io.IOException;
import java.util.concurrent.Callable;
class CheckedTask {
    static void demo() throws Exception {
        Callable<String> task = () -> { throw new IOException("call"); };
        try { task.call(); }
        catch (IOException e) { System.out.println(e.getMessage()); }
    }
}
~~~

Другие checked Exception здесь могут распространиться из demo по его объявлению. Создание lambda не исполняет её тело; проверка throws тела производится относительно целевого функционального типа. Однако вычисление receiver у связанной method reference, например `makeReceiver()::run`, происходит при создании ссылки и может само бросить checked IOException, даже если run() его не объявляет. Отдельная проба подтвердила IOException("creation") до вызова Runnable.run; Runnable с телом, бросающим IOException без обработки, отвергнут javac. См. [CORE-FUNCTIONS](functions.md), [CORE-GENERICS](generics.md) и [CORE-ERASURE](erasure.md) для ограничений типовых параметров, [JLS §15.27](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27), [JLS §11.2.1–11.2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.1).

## Почему throw null не эквивалентен throw new Object() (ER-21)

~~~java
class ThrowNull {
    static void unexpected() {
        throw null;           // компилируется; во время выполнения NullPointerException
    }

    // static void impossible() {
    //     throw new Object(); // не компилируется: Object не Throwable
    // }
}
~~~

У `null` нет созданного пользователем throwable-объекта; оператор `throw` при вычислении null приводит к `NullPointerException`. Следовательно, утверждение «throw требует явный new Throwable» неверно, а «throw new Object допустим, потому что Object — общий родитель» — тем более. Не проектируйте обработку ошибок вокруг `throw null`: это диагностический контрпример. [JLS §14.18](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.18).

## Error, InterruptedException и завершение процесса (ER-22, ER-24)

`Error` можно поймать, однако это не даёт разумного универсального способа продолжить работу после `OutOfMemoryError` или `StackOverflowError`. При встрече с системной ошибкой прежде всего анализируют возможность безопасной диагностики и остановки, а не добавляют глобальный `catch(Throwable)`. Это **рекомендация**, не запрет языка.

`InterruptedException` — checked и относится к политике отмены/прерывания. Если слой не может продолжить работу и вправе объявить `throws InterruptedException`, распространение исключения обычно сохраняет сигнал вызывающему. Если код **перехватывает и не пробрасывает** его, часто нужно восстановить флаг прерывания через `Thread.currentThread().interrupt()`, если это согласуется с контрактом. Нельзя советовать безусловное восстановление флага в любой точке, не учитывая, что прерываемые методы могут очищать его при бросании исключения и что вызывающий слой может иметь собственную политику. Подробная модель — будущая тема CON-INTERRUPT, пока без локальной ссылки. [Thread.interrupt API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html#interrupt()).

Минимальная локальная проба, предполагающая изначально непрерванный текущий поток и отсутствие внешних прерываний:

~~~java
Thread t = Thread.currentThread();
try {
    t.interrupt();
    System.out.println(t.isInterrupted()); // true
    try { Thread.sleep(0); }
    catch (InterruptedException e) {
        System.out.println(t.isInterrupted()); // false
        t.interrupt();
        System.out.println(t.isInterrupted()); // true
    }
    try { throw new InterruptedException("manual"); }
    catch (InterruptedException e) {
        System.out.println(t.isInterrupted()); // true
    }
} finally {
    Thread.interrupted(); // очищаем собственный флаг только в этой пробе
}
~~~

`sleep` по своему [контракту](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html#sleep(long)) при InterruptedException очищает interrupt status. Само бросание нового InterruptedException не является операцией очистки флага; в примере вручную брошенное исключение оставило true. isInterrupted читает флаг без очистки, статический interrupted читает и очищает. Если можно пробросить InterruptedException, часто это предпочтительнее поглощения; восстановление при переводе ошибки или невозможности пробросить согласуется с политикой вызывающего. Очистка в демонстрации нужна лишь для изоляции теста, не рецепт производственной обработки. Никаких предположений о задержке sleep(0) или планировщике.

**Границы исполнения:** `System.exit`, `Runtime.halt`, завершение процесса ОС, аппаратные аварии и некоторые фатальные ошибки не дают универсальной гарантии выполнить `finally` и `AutoCloseable.close()`. Даже если cleanup выполнен, это не обещает завершения внешнего IO или транзакции. См. [CORE-RESOURCES](resources.md).

## Что эта статья не подменяет (ER-27, ER-28, ER-30)

- **ER-27 / P2:** *precise rethrow* может уточнять checked-типы для фактически не переназначаемого catch-параметра; это отдельная часть [JLS §11.2.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.2). Не строим здесь полную матрицу disjunctive types и generalized exception analysis.
- **ER-28 / P2:** ошибка инициализации класса, `ExceptionInInitializerError` и erroneous class принципиально отличаются от ошибки обычного конструктора; изложение уже есть в [CORE-INIT](initialization.md), не дублируем полную JVM-модель.
- **ER-30 / P2:** exception translation во фреймворке, политики rollback, HTTP error contracts, structured logging/trace IDs требуют конкретной версии Spring и архитектуры. Это будущие SPR-WEB/SPR-TX: не нормы JLS и не предмет этой статьи.

## Карта ER-01–ER-30

| ID и приоритет | Где раскрыт вопрос |
|---|---|
| ER-01–ER-04 · P0 | Иерархия checked/unchecked; `throw/throws/catch`; static checking и propagation |
| ER-05–ER-06 · P0 | Catch ordering, multi-catch и compile-negative ловушки |
| ER-07–ER-08 · P0 | Cause/wrapping; custom exception и ответственность слоёв |
| ER-09–ER-10 · P0 | Вычисление `return` и замена результата/исключения в `finally` |
| ER-11–ER-17 · P0 | [CORE-RESOURCES](resources.md): API, TWR порядок, primary/suppressed, partial init, null/existing, extended |
| ER-18 · P0 | Static checked analysis; точный close-контракт — [CORE-RESOURCES](resources.md) |
| ER-19 · P1 | Ручное suppressed и его отличие от cause; [TWR-последовательность](resources.md) |
| ER-20–ER-22 · P1 | Override checked, `throw null`, Error и прерывания |
| ER-23 · P1 | Более узкий throws close — [CORE-RESOURCES](resources.md) |
| ER-24 · P1 | Пределы гарантий выполнения `finally` и закрытия ресурсов |
| ER-25 · P1 | Вложенные primary/suppressed — [CORE-RESOURCES](resources.md) |
| ER-26 · P1 | Конструкторы, finally, Runnable/lambda checked |
| ER-27 · P2 | Precise rethrow — граница, ссылка на JLS |
| ER-28 · P2 | [CORE-INIT](initialization.md), без дублирования |
| ER-29 · P2 | [CORE-RESOURCES](resources.md): IO/JDBC как граница |
| ER-30 · P2 | Spring/HTTP/transaction: будущий материал |

## Связанные контракты обязательной проверки и отсутствия

[CORE-ASSERT](assertions.md) отдельно показывает, почему `assert` при выключенных проверках пропускает условие и сообщение, почему `AssertionError` не заменяет `IllegalArgumentException` / `IllegalStateException` / `NullPointerException` при обязательной валидации. [CORE-OPTIONAL](optional.md) различает допустимое отсутствие результата и runtime NPE/NSEE при нарушениях конкретных контрактов `Optional`. Эта статья сохраняет собственную область checked/unchecked и распространения исключений.

**Прикладные runtime-исключения:** [CORE-NUMBERS](numbers.md) различает `Math.addExact` и точное `BigDecimal.divide` (оба могут бросать `ArithmeticException` во время исполнения) от `javac`-ошибок типов. [CORE-DATETIME](datetime.md) разбирает `DateTimeParseException`, `DateTimeException` для DST-gap и `UnsupportedTemporalTypeException`. Эти конкретные API-примеры не меняют базовую иерархию `Throwable` этой статьи.

## Источники и воспроизводимость

**Нормативные источники:** [JLS 21 Chapter 11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html), [§14.18–14.20](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html), [§8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3); [Throwable API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html), [Exception API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Exception.html). Классификация независимых интервью-источников — [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md): формулировки вопросов не доказывают гарантии Java.

Приведённые в статье маленькие исходники — **отдельные фрагменты**, их нельзя без подготовки вставлять в один файл из-за объявлений классов и импортов. Комментированные compile-negative строки поясняют **ожидаемую категорию ошибки**. Полный G1 включает **20 независимых negative N01–N20**; документ [плана](../../docs/plans/CORE-EXCEPTIONS-RESOURCES.md) записывает отдельные реальные проверки роли 01, но это **не означает**, что эти фрагменты были исполнены из данной статьи или CI. **Общий проверяемый пример Java 21:** [CORE-EXCEPTIONS + CORE-RESOURCES — запуск, полный stdout и объяснение P01–P20](../../examples/core/exceptions-resources/README.md). Исполняемый `ExceptionResourceProbes.java` объединяет `getCause()`, обычный `finally` с замещением причины, порядок закрытия TWR, primary/suppressed и ручное `addSuppressed()`; **20 строк — один запуск одной программы**, а не двадцать отдельных тестов. [Роль 03, Issue #104](https://github.com/Melchiorius/backend-interview-labs/issues/104) отдельно проверила `javac --release 21`, `java` и полный `diff -u` на OpenJDK 21.0.12.1; общий CI на интегрированном HEAD проверяется отдельно. Отдельные негативные N01–N20 не исполняются этим runnable. **Авторская проверка роли 02 на OpenJDK/javac 21.0.12.1 (Linux):** заново созданы 20 **отдельных** Java-исходников N01–N20 из §6 G1, с удалением только табличного экранирования `\|` там, где нужен multi-catch `|`. Для каждого отдельно выполнен `javac --release 21 -d ...`: **20/20** завершились с ожидаемой **категорией диагностической ошибки** (не просто nonzero exit), включая catch ordering, checked throws, multi-catch, effectively-final и generic Throwable. Это локальный авторский эксперимент, **не GitHub CI**. Также отдельно скомпонован и запущен один **авторский smoke** ключевых механизмов `finally`/TWR (10 строк наблюдений) с `javac --release 21` и `java`: нормальная компиляция и выполнение, в том числе `return 67→68` и `throw null→NullPointerException`. Это *не* извлечение всех фрагментов из Markdown и не §5 positive P01–P20. Пробный `javac -Xlint:all -Werror` для заведомо демонстрационных `return` в `finally` и неиспользуемых resource variables дал ожидаемые lint-warning и ошибку **warnings found and -Werror specified**; именно поэтому полный учебный сценарий нельзя объявлять совместимым с `-Werror` без изменения кода/подавления диагностики. **NOT RUN:** автоматическая компиляция всех inline-snippets обоих Markdown, GUI Obsidian/IntelliJ, другие JDK/JVM, байткод/JIT/performance.

**Дополнительные проверки D-A12a.** [Задача #336](https://github.com/Melchiorius/backend-interview-labs/issues/336) содержит отчёт о новых пробах на Debian OpenJDK/javac 21.0.12.1. Probe.java: строгая компиляция --release 21 -encoding UTF-8 -Xlint:all -Werror и JVM с явным UTF-8, exit 0/0, stderr пуст, 29 строк вывода. WarningCases.java: та же версия и -Xlint:all, но без -Werror, exit 0/0, пять предупреждений (четыре finally.cannot.complete и одно unreachable.catch), три строки stdout. 15 отдельно скомпилированных отрицательных исходников дали exit 1 с ожидаемой категорией javac; один также дал предупреждение finally, которое не является причиной языкового запрета. Это не повтор исторических 20 исходников G1 и не исполнение всех блоков Markdown. Пробы проверяют отдельные контрасты N01–13; в статье используются читаемые фрагменты, а не их дословный полный исходник.

Неизменённый общий runner выполнен на трёх точных восстановленных входных файлах (скрипт, Java-исходник и эталон), не на полном клоне. LC_ALL=C.UTF-8, exit 0, внешний stderr пуст, внутренний actual.stdout и эталон совпадают (20 строк, 539 байт, SHA-256 c52f0f1b8a79641887f362ae7abc311e41925cea1de404664fd0673bb1a8b9f8); stdout runner содержит эти 20 строк и маркер CHECK PASSED: stdout matches expected.stdout (21 строка, 584 байта). Скрипт делает javac --release 21, одну JVM и diff -u, не проверяет строго lint/stderr или хеши сам; хеши и stderr измерены снаружи. Новые временные пробы не входят в штатный CI. Не проверены другие JVM/ОС, тайминг и стресс потоков, принудительное завершение процесса, полная компиляция всех фрагментов, GUI и внешние ссылки. Полный разбор TWR остаётся в CORE-RESOURCES.

**Порядок чтения:** [CORE-INHERITANCE](inheritance.md) → **эта статья** → [CORE-RESOURCES](resources.md) → [CORE-INIT](initialization.md).

**Приложения к классификации ошибок:** [CORE-REGEX](regex.md) отделяет недопустимый Java string escape от runtime `PatternSyntaxException` и replacement failures; [CORE-UTIL](utilities.md) разбирает runtime `NullPointerException`/`IndexOutOfBoundsException`/`UnsupportedOperationException` и отдельные ошибки сигнатур при `javac`.

**Ошибки сериализации и локализованного ввода:** [CORE-SERIAL](serialization.md) различает `NotSerializableException`, `InvalidObjectException`, `InvalidClassException` из-за фильтра и несовместимости версии; [CORE-I18N](internationalization.md) показывает `ParseException`, `MissingResourceException` и отдельные ошибки типов при `javac`.
