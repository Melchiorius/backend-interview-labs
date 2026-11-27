# CORE-RESOURCES — try-with-resources, порядок закрытия и подавленные исключения в Java 21

**Версия:** Java SE 21. **Исследовательский контракт:** [принятый план CORE-EXCEPTIONS/RESOURCES](../../docs/plans/CORE-EXCEPTIONS-RESOURCES.md) (ER-01–ER-30, Issue [#100](https://github.com/Melchiorius/backend-interview-labs/issues/100)); авторское [Issue #103](https://github.com/Melchiorius/backend-interview-labs/issues/103). Эта статья продолжает [CORE-EXCEPTIONS](exceptions.md): прежде чем разбираться, почему закрытие может создать ещё одно исключение, важно различать `throw`, `throws`, propagation, `getCause()` и `finally`.

**Как читать примеры.** Объявления классов и интерфейсов помещайте в отдельные исходники с импортами перед объявлениями; методы вызывайте из main в том же пакете, а отдельные операторы — внутри метода. В примерах с Traced, LogResource и Count используйте соответствующий вспомогательный класс, определённый в статье. Повторяющиеся имена из независимых примеров не склеивайте. Для checked-вызовов объявите указанный throws или обработайте исключение; намеренно неиспользуемые ресурсные переменные могут вызвать предупреждение javac, поэтому учебные примеры с такими предупреждениями компилируются без -Werror. Отрицательные варианты — отдельные компиляции.

## Проблема: работу и очистку нужно диагностировать одновременно

Представим обработчик заказа: он открывает два ресурса, пытается записать данные, затем закрывает оба. Если запись завершилась неудачно и закрытие второго ресурса тоже бросило исключение, **какую ошибку должен увидеть вызывающий**? Если наружу выйдет только «не удалось закрыть соединение», мы потеряем причину, из-за которой сама операция не была выполнена.

`try-with-resources` (TWR) — языковая конструкция для ресурсов, реализующих `AutoCloseable`. Она задаёт порядок закрытия и, в стандартной ситуации, **сохраняет первичную причину** отдельно от ошибок cleanup. Это принципиально отличается от простого `finally { resource.close(); }`, где новая ошибка закрытия может заменить прежнюю. Нормативная основа — [JLS 21 §14.20.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3).

## AutoCloseable и Closeable: две разные декларации close (ER-11, ER-18, ER-23)

~~~java
interface AutoCloseable {
    void close() throws Exception;
}

// Смысл контракта java.io.Closeable:
interface Closeable extends AutoCloseable {
    void close() throws java.io.IOException;
}
~~~

Эта схема **иллюстрирует объявления методов с их throws-контрактами**, а не предлагает копировать объявления стандартных библиотечных интерфейсов в проект. Действительные API: [AutoCloseable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AutoCloseable.html) и [java.io.Closeable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Closeable.html).

`AutoCloseable` допускает общий `Exception` в контракте `close()`. `Closeable` — более специальный интерфейс с `IOException`. **Но конкретный ресурс может сужать throws вплоть до пустого списка**. Компилятор анализирует выбранное объявление метода по статическому типу ресурса, а не «всегда заставляет ловить Exception, раз тип реализует AutoCloseable».

~~~java
class PlainResource implements AutoCloseable {
    @Override public void close() { /* checked exceptions нет */ }
}
class CloseExamples {
    static void noCheckedNeeded() {
        try (PlainResource resource = new PlainResource()) {
            System.out.println("work");
        }
    }
}
~~~

Здесь `PlainResource.close()` не объявляет checked, и сам по себе TWR не требует `throws Exception`. А если написать `AutoCloseable resource = new PlainResource(); try (resource) { ... }`, статический тип ссылки — `AutoCloseable`, поэтому в показанном простом TWR нужно обработать checked-исключение из `close()` (например, `catch (Exception e)`) или объявить `throws Exception` у окружающего метода, несмотря на фактический объект `PlainResource`. Компилятор проверяет объявленный throws-контракт по статическому типу, а при выполнении вызывается реализация `close()` фактического объекта. Throws clause не входит в сигнатуру метода в смысле JLS §8.4.2. См. [CORE-INTERFACES](interfaces.md), [CORE-INHERITANCE](inheritance.md), [JLS §11.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2).

**Идемпотентность:** `AutoCloseable` сам по себе **не обещает** безопасного многократного `close()` и не запрещает runtime-ошибки; контракт `Closeable` требует, чтобы закрытие уже закрытого объекта не имело эффекта. Прикладная реализация может иметь дополнительные условия. Не следует бездумно закрывать один и тот же объект вручную и в TWR, не прочитав API конкретного ресурса.

**Рекомендации API, не гарантии вызова close.** Документация [AutoCloseable.close()](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AutoCloseable.html#close()) советует реализации выбирать более узкий checked-контракт либо не бросать исключений, по возможности освобождать ресурс и отмечать его закрытым до бросания ошибки, избегать InterruptedException и исключений, подавление которых нарушает корректную работу программы. InterruptedException связан с политикой прерывания: см. [CORE-EXCEPTIONS](exceptions.md), раздел о флаге прерывания. Это рекомендации реализации, а не обещание успешного освобождения или обязательной идемпотентности AutoCloseable. В конечной пробе собственный Closeable с проверкой closed изменил состояние один раз при двух вызовах, а другой AutoCloseable сначала установил closed=true и затем бросил IOException. Эти наблюдения иллюстрируют выбранные реализации, не доказывают свойства всех ресурсов.

**Видимость:** реализация публичного метода интерфейса требует совместимого доступа, поэтому `class R implements AutoCloseable { void close() {} }` (package-private) **не компилируется**. [JLS §9.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4).

## Самый простой TWR и кто вызывает close (ER-12)

~~~java
class LogResource implements AutoCloseable {
    private final String name;
    LogResource(String name) {
        this.name = name;
        System.out.println("open " + name);
    }
    void use() { System.out.println("use " + name); }
    @Override public void close() {
        System.out.println("close " + name);
    }
}

class Order {
    static void run() {
        try (LogResource a = new LogResource("A");
             LogResource b = new LogResource("B")) {
            a.use();
            b.use();
        }
    }
}
~~~

Вывод при вызове `Order.run()`:

~~~text
open A
open B
use A
use B
close B
close A
~~~

Алгоритм: **объявления ресурсов инициализируются слева направо** (A, затем B), тело работает только после успешной инициализации всех ресурсов, затем вызовы закрытия идут **в обратном порядке** (B, затем A). Инициализация здесь — не создание класса JVM, а **вычисление ресурса и получение ссылки на конкретный экземпляр**. Не путайте с [CORE-INIT](initialization.md).

Для одного ресурса `try (R r = ...) { body; }` по завершении защищённого блока вызывает `r.close()`, если результат ресурса не `null`, включая стандартные случаи исключения из тела. Если у API недопустимо повторное закрытие, TWR не делает его автоматически безопасным: **владение ресурсом** — отдельный вопрос проекта.

[Норма JLS §14.20.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3). Реальный вывод показанного кода — **объяснённое ожидаемое наблюдение**, а не заявление, что этот Markdown-фрагмент уже прогнан на CI.

### Один объект в двух записях заголовка

~~~java
class Count implements AutoCloseable {
    int closes;
    public void close() { closes++; }
}
// Внутри метода:
Count a = new Count();
Count b = a;
try (a; b) {
    System.out.println(a == b); // true
}
System.out.println(a.closes); // 2
~~~

TWR регистрирует записи заголовка, а не множество уникальных объектов: две ненулевые ссылки на один экземпляр означают две попытки close в обратном порядке при обычном выходе. Дедупликации по identity нет. Обёртка также может закрывать вложенный ресурс в своём close; если оба зарегистрированы, возможны повторные вызовы. Определите владельца и контракт повторного закрытия заранее; не добавляйте второй close ради формальной страховки.

## Ошибка работы плюс две ошибки close: primary и suppressed (ER-13, ER-19, ER-25)

Для явного наблюдения сделаем ресурс, который при закрытии регистрирует событие и может бросить `IOException`:

~~~java
import java.io.IOException;

final class Traced implements AutoCloseable {
    private final String name;
    private final StringBuilder events;
    private final boolean failOnClose;

    Traced(String name, StringBuilder events, boolean failOnClose) {
        this.name = name;
        this.events = events;
        this.failOnClose = failOnClose;
        events.append("open-").append(name).append(';');
    }

    @Override public void close() throws IOException {
        events.append("close-").append(name).append(';');
        if (failOnClose) throw new IOException("close-" + name);
    }
}
~~~

Это один **вспомогательный класс**, используемый в следующих примерах; предполагается, что он присутствует в том же пакете. Сценарий: оба ресурса создаются успешно, тело бросает `IOException("work")`, оба `close()` тоже бросают.

~~~java
import java.io.IOException;

class BodyAndCloseFailures {
    static void run() {
        StringBuilder log = new StringBuilder();
        try (Traced a = new Traced("A", log, true);
             Traced b = new Traced("B", log, true)) {
            log.append("body;");
            throw new IOException("work");
        } catch (IOException primary) {
            System.out.println(log);
            System.out.println(primary.getMessage()); // work
            for (Throwable suppressed : primary.getSuppressed()) {
                System.out.println(suppressed.getMessage());
            }
        }
    }
}
~~~

Ожидаемая последовательность:

~~~text
open-A;open-B;body;close-B;close-A;
work
close-B
close-A
~~~

Сначала созданы A и B; затем тело создало **primary** `IOException("work")`. При выходе вызывается `B.close()` и бросает `IOException("close-B")`. Поскольку primary уже существует, эта ошибка добавляется к нему через **suppressed**, но не заменяет его. Затем вызывается `A.close()`: `"close-A"` становится **вторым suppressed**. Внешний `catch` получает primary `"work"`, у которого `getSuppressed()` содержит два других Throwable **в порядке их появления при обратном закрытии**.

**Важная граница:** suppressed — не другое название `getCause()`. Поле cause обычно описывает *почему была создана новая ошибка* (например, исключение базы, обёрнутое сервисом), а suppressed — **дополнительные ошибки cleanup** при распространении уже существующей primary. Обе связи могут сосуществовать. Вывод и правила: [JLS §14.20.3.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3.1), [Throwable.getSuppressed API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html#getSuppressed()).

### Что именно делает трансляция TWR

Для одной объявленной ресурсной переменной JLS §14.20.3.1 задаёт следующую схему (имена служебных переменных условны, R и acquire/body — заполнители, не самостоятельный Java-файл):

~~~java
final R r = acquire();
Throwable primary = null;
try {
    body();
} catch (Throwable t) {
    primary = t;
    throw t;
} finally {
    if (r != null) {
        if (primary != null) {
            try {
                r.close();
            } catch (Throwable closeFailure) {
                primary.addSuppressed(closeFailure);
            }
        } else {
            r.close();
        }
    }
}
~~~

Инициализатор находится до внутреннего try. Несколько записей переводятся во вложенные TWR: первый ресурс — внешний, последующие — внутренние. Поэтому ошибки при создании внутреннего ресурса и при его закрытии могут стать первичными для внешнего. Важен catch(Throwable), не catch(Exception): даже AssertionError из close при наличии отличного от него primary может быть подавлен. Однако сам addSuppressed тоже может завершиться исключением.

~~~java
import java.io.IOException;
class SameFailure {
    static class R implements AutoCloseable {
        final IOException failure;
        R(IOException failure) { this.failure = failure; }
        public void close() throws IOException { throw failure; }
    }
    static void run() {
        IOException same = new IOException("same");
        IOException outer = new IOException("outer");
        try (R a = new R(outer); R b = new R(same)) {
            throw same;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getCause() == same);
            System.out.println(e.getSuppressed()[0] == outer);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
~~~

Здесь тело и внутренний close бросают **один и тот же объект**. API Throwable запрещает self-suppression: addSuppressed(same) на same бросает IllegalArgumentException. Она заменяет прежнюю причину при выходе из внутреннего TWR; внешний close затем добавляет свою отдельную ошибку к новой primary. На OpenJDK 21.0.12.1 фактически наблюдались causeSame=true и outer=true. Тип IAE — норма API; наличие именно same в cause этой IAE — наблюдение данной реализации, не обещание контракта. Если тело успешно, но оба close бросают один общий IOException, возникает аналогичная self-suppression при внешнем закрытии; в пробе снова IAE, causeSame=true.

Если primary создан с отключённой suppression, допустимое отличное исключение из close игнорируется, primary продолжает распространяться, список остаётся пустым. Отключение не отменяет проверку self/null; см. [CORE-EXCEPTIONS](exceptions.md) и [Throwable.addSuppressed](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html#addSuppressed(java.lang.Throwable)). Отдельная проба с primary-подклассом Exception, вызвавшим super("disabled", null, false, true), подтвердила сохранение его identity и длину 0. Проба с AssertionError из close подтвердила сохранение этого объекта в suppressed обычного IOException. Таким образом, сохранение primary и полнота списка не безусловны: правила обычных примеров предполагают различные исключения, нормальное завершение регистрации и включённую suppression.

## Тело завершилось успешно, но оба вызова `close()` завершились ошибкой (ER-14, ER-25)

В следующем коде **нет ошибки тела**, но есть два падающих закрытия:

~~~java
import java.io.IOException;
class TwoCloseFailures {
    static void run() {
        StringBuilder log = new StringBuilder();
        try (Traced a = new Traced("A", log, true);
             Traced b = new Traced("B", log, true)) {
            log.append("body;");
        } catch (IOException primary) {
            System.out.println(primary.getMessage()); // close-B
            System.out.println(primary.getSuppressed()[0].getMessage()); // close-A
        }
    }
}
~~~

Закрытие идёт B → A, значит **первая ошибка закрытия** `close-B` становится primary, а более поздняя `close-A` добавляется к ней как suppressed. Если падает только один `close()` при нормальном теле, его ошибка будет primary; если у неё уже есть suppressed, они сохраняются. Не нужно выдумывать отсутствующую ошибку работы или считать список обязательно пустым.

Это не гарантирует, что любой `close()` всегда кидает `IOException`: в демонстрационном классе так запрограммировано флагом `failOnClose`. Сама спецификация TWR задаёт **приоритет уже возникших ошибок**, не обещает, что ресурсы способны всегда закрыться безошибочно.

### Вложенные suppressed и уже существующие связи
В схеме root — первичная ошибка, prior — ранее добавленная подавленная ошибка, child — новая ошибка закрытия, deep — подавленная ошибка самого child. Стрелки показывают связи между объектами исключений по ссылкам, а не порядок выполнения.

~~~text
root
  cause -> cause
  suppressed[0] -> prior
  suppressed[1] -> child
      suppressed[0] -> deep
~~~

Такое дерево получилось в конечной пробе: root уже имел cause и prior до входа в TWR, тело бросало root, close бросал child с уже добавленным deep. После закрытия root.getSuppressed() содержит [prior, child], child.getSuppressed() — [deep]. Существующие элементы не очищаются, новые допустимые добавляются в конец по порядку регистрации, вложенные списки не распрямляются в общий массив. Cause остаётся независимой связью. При единственной ошибке close (тот же child и успешное тело) наружу вышел child со своим прежним deep. Это дерево конкретного примера, не требование ацикличности произвольных связей Throwable или полного стека.

## Ошибка инициализации второго ресурса (ER-15)

Тонкость, которую легко пропустить: тело `try` ещё **не началось**, но первый ресурс **уже открыт**. Если создание B бросает исключение, Java должна попытаться закрыть ранее полученный A.

~~~java
import java.io.IOException;

class InitFailure {
    static Traced failCreatingB(StringBuilder log) throws IOException {
        log.append("init-fail;");
        throw new IOException("init-B");
    }

    static void run(boolean closeAAlsoFails) {
        StringBuilder log = new StringBuilder();
        try (Traced a = new Traced("A", log, closeAAlsoFails);
             Traced b = failCreatingB(log)) {
            log.append("body;"); // никогда не выполняется в этом сценарии
        } catch (IOException primary) {
            System.out.println(log);
            System.out.println(primary.getMessage()); // init-B
            System.out.println(primary.getSuppressed().length);
        }
    }
}
~~~

При `closeAAlsoFails=false` лог: `open-A;init-fail;close-A;`, primary = `init-B`, suppressed = **0**. При `closeAAlsoFails=true` лог по-прежнему заканчивается `close-A;`, primary **остаётся** `init-B`, но suppressed включает `close-A`.

Иными словами, **порядок частичной инициализации — слева направо, cleanup успешных предыдущих ресурсов — обратно**. Если бы B успешно создался, а C не создался, после ошибки C закрывались бы B, затем A. Новому ресурсу, инициализация которого так и не вернула объект, вызывать `close()` не на чем. Норма: [JLS §14.20.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3).

### Неудачная обёртка и граница регистрации

~~~java
import java.io.IOException;
class Wrapper implements AutoCloseable {
    Wrapper(Count inner) throws IOException {
        throw new IOException("wrapper");
    }
    public void close() { throw new AssertionError(); }
}
// Внутри метода, используя Count из примера выше:
Count first = new Count();
try (var w = new Wrapper(first)) {
    throw new AssertionError("body");
} catch (IOException e) {
    System.out.println(first.closes); // 0
}
Count second = new Count();
try (second; var w = new Wrapper(second)) {
    throw new AssertionError("body");
} catch (IOException e) {
    System.out.println(second.closes); // 1
}
first.close(); // очистка тестового объекта владельцем
~~~

В первой форме инициализатор обёртки не вернул ресурс, а first не был зарегистрирован. TWR не умеет обнаруживать внутренние объекты, приобретённые как аргументы конструктора (например, new Wrapper(openInner())), или заранее открытые вне заголовка. Во второй форме second зарегистрирован отдельно и его close вызывается даже при ошибке следующей записи. В реальной реализации, которая берёт владение, неудачная инициализация должна быть failure-atomic: освобождать уже приобретённое при ошибке либо сохранять ответственность у вызывающего по явному контракту. При успешной обёртке, которая каскадно закрывает underlying, две записи могут вызвать повторное закрытие (см. пример alias). Не регистрируйте обе ссылки без проверки контракта владения и повторного close.

## null, внешняя ссылка и effectively final (ER-16)

Ресурсное выражение может вычислиться в `null`, если это допустимо по типам; TWR **не вызывает** на нём `close()` и не бросает NPE просто из-за попытки закрыть null.

~~~java
class ExistingResourceCases {
    static void nullResource() {
        LogResource missing = null;
        try (missing) {
            System.out.println("body"); // close(null) не вызывается
        }
    }

    static void existingResource() {
        LogResource alreadyCreated = new LogResource("E");
        try (alreadyCreated) {
            alreadyCreated.use();
        }
        // close E уже вызван ровно однажды данным TWR
    }
}
~~~

В Java 21 разрешены два вида записи ресурса: объявление локальной переменной с инициализатором (ровно один declarator, разделение ресурсов через ;), доступное с Java 7, либо VariableAccess — ExpressionName или FieldAccess, добавленный в Java 9 для уже существующих переменных. Внешняя переменная (включая параметр) должна быть final или effectively final и definitely assigned до использования; поле должно быть final и definitely assigned. Тип ресурса должен быть подтипом AutoCloseable. Объявленная в заголовке переменная неявно final, её область видимости включает последующие записи и тело try, но не внешний catch/finally. См. [JLS §14.20.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3), [CORE-FUNCTIONS](functions.md) (захват ссылок и effectively final).

~~~java
class Reassignment {
    static void invalid() {
        LogResource r = new LogResource("first");
        r = new LogResource("second");
        // try (r) {}   // ошибка компиляции: r не effectively final
    }
}
~~~

Переопределённая ссылка перестаёт быть effectively final, даже если ресурсы по отдельности реализуют AutoCloseable. Это не означает, что внутреннее состояние **самого объекта** должно быть неизменяемым. `final` ограничивает переназначение **переменной**, а не делает объект «immutable». О различиях типов/ссылок: [CORE-TYPES](types.md) и [CORE-PARAMS](parameters.md).

~~~java
class Holder {
    final Count field = new Count();
}
class FieldCase {
    static int evaluations;
    static Holder receiver(Holder h) {
        evaluations++;
        return h;
    }
    static void run() {
        Holder h = new Holder();
        try (receiver(h).field) {
            System.out.println(evaluations); // 1
        }
        System.out.println(h.field.closes); // 1
    }
}
~~~

Для внешнего VariableAccess трансляция сначала сохраняет вычисленную ссылку в новой final-переменной. Receiver выражения receiver(h).field вычисляется один раз при регистрации, не ещё раз при close. Простой h.field также допустим. Произвольное выражение try(get()) не является VariableAccess, но try(var r = get()) допустимо при совместимом типе и обработке checked. Реальные отдельные отказы javac: non-final field и переназначенный parameter (expr.effectively.final.var), неинициализированная переменная (var.might.not.have.been.initialized), присваивание объявленной в заголовке (try.resource.may.not.be.assigned), ссылка на неё в finally (cant.resolve.location), голый вызов (expr.needs.var), объявление без = и два declarator через запятую (синтаксическая диагностика). Вывод `1` для счётчика в `FieldCase.run()` предполагает один вызов при начальном `evaluations == 0`; повторные вызовы без сброса накапливают значение. В фактической временной пробе счётчик `eval` явно сброшен в 0: поле и receiver дали по одному close, receiver вычислен один раз. Внешняя переменная сохраняет свою обычную область видимости; close не обнуляет её автоматически.

**Практическая ловушка:** внешний объект уже существовал до TWR, но TWR его **закроет**; другие ссылки на этот же объект после блока могут указывать на закрытое состояние. Это вопрос *владения временем жизни*, не «временной копии» и не передачи объекта по значению.

## Расширенная форма TWR: `catch` и `finally` выполняются после попыток закрытия (ER-17)

Форма `try (resource) { body } catch (...) { ... } finally { ... }` сочетает автоматическое закрытие с обычными обработчиками. Важно правильно назвать последовательность:

~~~java
class Extended {
    static void run() {
        StringBuilder log = new StringBuilder();
        try (Traced a = new Traced("A", log, false)) {
            log.append("body;");
        } catch (java.io.IOException e) {
            log.append("catch;");
        } finally {
            log.append("finally;");
        }
        System.out.println(log); // open-A;body;close-A;finally;
    }
}
~~~

Сначала выполняются ресурсные инициализаторы, затем (при их успехе) тело, затем предусмотренные трансляцией попытки закрытия уже зарегистрированных ненулевых ресурсов. После завершения внутреннего TWR внешний catch (при подходящем исключении) и finally работают по правилам обычного try. Если B не создался, ошибка инициализации может попасть во внешний catch после попытки закрытия A. Если тело успешно, но close упал, catch может получить именно ошибку закрытия. В конечной пробе close выставил флаг попытки/закрытия своей реализации и бросил IOException; catch и finally наблюдали этот флаг. Это факт вызова и завершения попытки, не гарантия освобождения реального ресурса. Если close не завершится, последующий управляющий поток может не наступить. «Catch обрабатывает только ошибки body» неверно. [JLS §14.20.3.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3.2).

**Граница у примера:** `Traced.close()` статически объявляет `IOException`, поэтому даже если данный экземпляр создан с `false` и фактически не бросает, catch IOException остаётся допустимым — компилятор учитывает **объявленный throws-контракт**, не исследует флаг экземпляра.

## Почему TWR не равно finally { close(); } (ER-10, ER-13)

В обычной конструкции:

~~~java
import java.io.IOException;

class Manual {
    static void work() throws IOException {
        Traced r = new Traced("A", new StringBuilder(), true);
        try {
            throw new IOException("work");
        } finally {
            r.close(); // бросит IOException("close-A") и может затмить "work"
        }
    }
}
~~~

Если `finally` сам завершается резко новым исключением, по общему правилу abrupt completion наружу может уйти **новое** исключение, прежнее **не будет добавлено автоматически** как suppressed. В TWR языковая трансляция сохраняет `"work"` primary, а `"close-A"` — suppressed. В этом разница между **исключением из тела и исключением при cleanup**, а не просто синтаксический сахар для вызова `close()`. При желании реализовать такую логику вручную нужны собственные конструкции сохранения primary; TWR снижает вероятность ошибки автора, но не может решить бизнес-транзакцию.

Аналогично: если в теле TWR вычисляется `return 67;`, затем close бросает исключение, то нормального возврата **не будет**, потому что очистка не завершилась нормально. Если close прошёл успешно, ранее вычисленный результат возвращается. [JLS §14.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.2), [§14.20.3.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3.1).

### Return, break и continue

~~~java
static int value(Count r) {
    try (r) { return 67; }
}
// Внутри метода:
Count a = new Count();
for (int i = 0; i < 1; i++) {
    try (a) { break; }
}
Count b = new Count();
for (int i = 0; i < 1; i++) {
    try (b) { continue; }
}
System.out.println(a.closes + "," + b.closes); // 1,1
~~~

При нормальном close сохраняется уже подготовленная причина abrupt completion (return, break, continue). Если причина не throw, служебная primary остаётся null; при исключении close его ошибка заменяет прежнюю причину. В отдельных конечных пробах return дал 67 и счётчик1; при падающем close вместо return распространился тот же IOException("close"). Для break и continue получены счётчики1,1; при падающем close каждый переход заменён IOException. Не существует объекта Throwable для подавления самого return/break/continue; никакого автоматического suppressed для этих причин. Если была причина throw, действует другая ветвь трансляции выше. См. [CORE-EXCEPTIONS](exceptions.md) о причинах R/S и checked-анализе finally.

## Проверяемое исключение из `close()`: какое объявление close учитывает компилятор (ER-18, ER-23)

При использовании TWR компилятор проверяет возможные checked исключения **как из инициализации, так и из тела и закрытия**. Типовая ошибка: «в теле нет throw, значит ничего не нужно ловить».

~~~java
class CheckedClose {
    static final class General implements AutoCloseable {
        @Override public void close() throws Exception { /* API контракт */ }
    }

    // static void bad() {
    //     try (General r = new General()) {}
    // } // compile error: unreported Exception из close()

    static void declared() throws Exception {
        try (General r = new General()) {}
    }
}
~~~

Декларация `General.close() throws Exception` создаёт checked-обязательство. Но класс `PlainResource` из первого примера с `close()` без checked **не** требует `throws Exception`. И классы, реализующие `Closeable`, сужают допустимую декларацию до `IOException` или более узких типов.

Если реализатор `AutoCloseable.close()` пытается сузить видимость, код не компилируется; если переопределяющий метод пытается **расширить** checked `throws` относительно контракта интерфейса, компилятор тоже отвергнет его. В данном случае `AutoCloseable.close() throws Exception` достаточно широк, однако потомки конкретных классов могут иметь **более узкий** родительский контракт. См. [CORE-EXCEPTIONS](exceptions.md) и [CORE-INHERITANCE](inheritance.md).

### Статическая обязанность даже при null и составном типе

~~~java
static void allowed() throws Exception {
    AutoCloseable r = null;
    try (r) {
        System.out.println("body");
    }
}
~~~

Удаление throws Exception здесь отвергается: javac учитывает объявление close статического типа AutoCloseable, даже если во время выполнения ссылка заведомо null и close не будет вызван. Проверяется не конкретное значение, а тип ресурса и возможные checked-исключения из close согласно JLS §11.2.2.

~~~java
import java.io.IOException;
import java.sql.SQLException;
interface A extends AutoCloseable {
    void close() throws IOException;
}
interface B extends AutoCloseable {
    void close() throws SQLException;
}
class Both implements A, B {
    int closes;
    public void close() { closes++; }
}
class Intersection {
    static <T extends A & B> void use(T r) {
        try (r) { System.out.println(r != null); }
    }
}
~~~

У T учитываются совместимые унаследованные методы: checked-исключение должно быть разрешено обоими контрактами close. Для несвязанных IOException и SQLException общего разрешённого checked-типа нет, поэтому use не требует checked throws из close. В пробе вызов с Both дал один close. Это конкретный совместимый набор абстрактных методов, не правило объединять throws всех bounds в один широкий список, не гарантия отсутствия unchecked и не вывод о поведении любого объекта. См. [CORE-INTERFACES](interfaces.md), [JLS §15.12.2.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2.5), [JLS §11.2.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.2). Отдельные отрицательные компиляции подтвердили недопустимость package-private close и расширение throws Exception при реализации Closeable.close.

## Стек вызовов, cause, suppressed: три разных диагностических вопроса (ER-19, ER-25)

| Диагностика | На какой вопрос отвечает | Пример |
|---|---|---|
| Stack trace | Снимок стека, обычно при создании Throwable, а не история всех последующих throw | Стек `IOException("work")`; повторный throw сам не обновляет его |
| `getCause()` | Какая **предыдущая ошибка была обёрнута** в новую | `IllegalStateException("service", io)` |
| `getSuppressed()` | Какие **дополнительные ошибки** были зарегистрированы при сохранении основной причины | `work` + `close-B`/`close-A` |

Один Throwable может иметь и cause, и suppressed. `getSuppressed()` возвращает массив; сами Throwable-объекты сохраняют собственные stack trace/cause. Но suppressed не обязаны быть исключительно «файловыми ошибками закрытия»: API предоставляет публичный `addSuppressed()` и позволяет добавлять их вручную. При отключённой suppression на уровне `Throwable` добавление не будет наблюдаться в обычном списке, поэтому нельзя обещать универсальную полноту diagnostics при любом subclass/конструкторе. [Throwable API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html).

Как показано в [CORE-EXCEPTIONS](exceptions.md), повторный throw того же объекта не обновляет trace автоматически; fillInStackTrace/setStackTrace могут менять записываемый стек, а writableStackTrace=false даёт пустой массив и отключает запись. Не обещайте точное место throw, полную цепочку кадров или все вторичные ошибки в любом Throwable.

**Почему не нужно подавлять все исключения cleanup?** Даже если primary более важен, подавленные сведения позволяют увидеть вторичные сбои, которые объясняют утечки/невозможность корректно завершить операцию. Передача только `primary.getMessage()` в мониторинг теряет диагностический контекст. Это инженерный аргумент за вывод полной цепочки с suppressed, не языковое требование конкретного логгера.

## Где заканчиваются гарантии TWR (ER-24, ER-29)

- TWR контролирует **языковые вызовы `close()`**, но не гарантирует, что процесс продолжит работу при `Runtime.halt`, завершении ОС, аппаратном отказе или другом прекращении исполнения. Конкретный `AutoCloseable.close()` может сам завершиться неудачно.
- `close()` **не тождественен** `commit()` или `rollback()`. В JDBC ресурс `Connection` или `Statement` имеет собственный API и транзакционный контекст; судьба незавершённой транзакции при закрытии зависит от конкретного контракта и реализации. Нельзя выводить успешный commit из факта успешного close. Подробный курс DB-JDBC будет отдельным, пока без вымышленной локальной ссылки.
- Для `InputStream`, `Reader` и других IO-ресурсов существуют дополнительные гарантии и ошибки операций; одного интерфейса `AutoCloseable` недостаточно, чтобы сделать выводы о буферизации, flush, файловой системе и долговечности записи. Это граница будущей темы IO-FILES.
- TWR не устанавливает потокобезопасность, не передаёт владение между потоками, не освобождает автора от решения, **кто отвечает за время жизни** ресурса, и не защищает от постороннего повторного close.
- `finally` и TWR не становятся механизмом автоматической отмены задач или транзакционных компенсирующих операций. [CORE-EXCEPTIONS](exceptions.md) обсуждает распространение Throwable, но не подменяет Spring transaction management.

## Карта соответствия принятому G1 ER-01–ER-30

| ID · приоритет | Обоснование и раздел |
|---|---|
| ER-01–ER-08 · P0 | [CORE-EXCEPTIONS](exceptions.md): checked/unchecked, анализ, catch, propagation, cause, custom |
| ER-09–ER-10 · P0 | [CORE-EXCEPTIONS](exceptions.md) и здесь «Почему TWR не равно finally» |
| ER-11 · P0 | `AutoCloseable` vs `Closeable`: throws, idempotence, visibility |
| ER-12 · P0 | Init A→B, close B→A |
| ER-13 · P0 | Body failure primary, две ошибки close suppressed |
| ER-14 · P0 | Только close failures: первый при обратном закрытии primary |
| ER-15 · P0 | Ошибка инициализации B закрывает уже созданный A |
| ER-16 · P0 | null, effectively final, внешняя ссылка |
| ER-17 · P0 | Extended TWR: catch/finally после попыток закрытия |
| ER-18 · P0 | Close-throws checked и override; [CORE-EXCEPTIONS](exceptions.md) |
| ER-19 · P1 | Cause vs suppressed; ручное добавление и отключение suppression (ссылка на API) |
| ER-20–ER-22 · P1 | [CORE-EXCEPTIONS](exceptions.md): override, throw null, Error/interruption |
| ER-23 · P1 | Частные `close()` без checked или с узким throws |
| ER-24 · P1 | Границы при прекращении исполнения, невозможность гарантировать cleanup |
| ER-25 · P1 | Nested primary/suppressed, порядок и диагностика |
| ER-26 · P1 | [CORE-EXCEPTIONS](exceptions.md): checked в ctor/finally/lambda |
| ER-27 · P2 | [CORE-EXCEPTIONS](exceptions.md): precise rethrow как углубление |
| ER-28 · P2 | [CORE-INIT](initialization.md): class initialization errors |
| ER-29 · P2 | IO/JDBC, close не commit/rollback — обозначенная граница |
| ER-30 · P2 | Spring exception translation/transactions — обозначенная граница [CORE-EXCEPTIONS](exceptions.md) |

## Источники, проверяемость и переходы

**Нормативная основа:** [JLS 21 §11.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2), [§14.20.2–14.20.3.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3), [Throwable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html), [AutoCloseable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AutoCloseable.html), [Closeable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Closeable.html). [Реестр исследовательских источников](../../docs/SOURCE_REGISTRY.md) отделяет нормативные документы от независимо найденных интервью-вопросов. Требования к примерам — [EXERCISE_AND_TESTING_STANDARD](../../docs/EXERCISE_AND_TESTING_STANDARD.md).

Короткие самостоятельные фрагменты (включая `Traced`, от которого зависят последующие демонстрации) читаются **совместно**, но их нельзя без подготовки скопировать в один файл с несколькими публичными объявлениями и импортами. **Отдельные N01–N20** из [G1 §6](../../docs/plans/CORE-EXCEPTIONS-RESOURCES.md) — двадцать самостоятельных ожидаемых ошибок компиляции, а не часть позитивного сценария. **Проверяемая общая трасса:** [CORE-EXCEPTIONS + CORE-RESOURCES — README, Java source, P01–P20 stdout и verify.sh](../../examples/core/exceptions-resources/README.md). На конкретных `R implements AutoCloseable` и общем журнале P09–P13 показывают порядок open A→B / close B→A, body primary `work` с suppressed `[close-B, close-A]`, close-only primary `close-B` и ошибку `init-B`, после которой уже созданный A закрывается. P14–P16 показывают null, effectively-final и extended `finally`. **20 наблюдений — одна программа, не 20 независимых тестов.** Негативные N01–N20 из G1 остаются отдельными. Согласование и фактические команды автора runnable: [Issue #104](https://github.com/Melchiorius/backend-interview-labs/issues/104).

**Исторические авторские проверки роли 02 при первоначальной публикации (OpenJDK/javac 21.0.12.1, Linux):** 20/20 отдельных N01–N20 из G1 §6 заново проверены на ожидаемые **категории** ошибок компиляции командой `javac --release 21`; это временные исходники, не CI.

Дополнительно локально собран отдельный компактный smoke с `Traced implements AutoCloseable` и общим `StringBuilder`, выполнены `javac --release 21` и `java`: **10 строк одного запуска**, включая порядок `open-A;open-B;body;close-B;close-A;`, primary `work` плюс suppressed `[close-B, close-A]`, primary `close-B` плюс suppressed `[close-A]`, `init-B` плюс suppressed `[close-A]`, `null`, existing resource и extended `finally`. Это **авторская независимая компоновка**, не автоматическое извлечение всех блоков этой статьи и не обещание P01–P20 §5.

Пробная компиляция того же smoke с `-Xlint:all -Werror` **не прошла** из-за предупреждений `[try]` о намеренно неиспользуемых в теле ресурсных ссылках и `[finally]` о замещающем abrupt completion; под обычным `--release 21` компиляция и запуск прошли. Такие warnings полезны для обучения и не являются ложной «ошибкой TWR».

**NOT RUN при первоначальной авторской проверке:** автоматическая компиляция всех Markdown snippets, GUI Obsidian/IntelliJ, другие JDK/JVM, живой JDBC, bytecode/JIT/perf и реальная файловая система.

**Историческая проверка роли 03 при первоначальной публикации.** Общий `ExceptionResourceProbes.java`: Debian OpenJDK/javac 21.0.12.1, `bash examples/core/exceptions-resources/verify.sh`, успешные `javac --release 21`/`java`/`diff -u` и **20/20 строк P01–P20**. Это фактическая проверка исходных code/stdout/script blobs роли 03, а не автоматический прогон Markdown snippets. Первоначальная публикация прошла через [PR #105](https://github.com/Melchiorius/backend-interview-labs/pull/105); история его CI относится к тому HEAD, а не к новым проверкам ниже.

**Дополнительные проверки D-A12b (первоначальная доработка статьи, роль 02).** В [задаче #339](https://github.com/Melchiorius/backend-interview-labs/issues/339) и [фактическом отчёте 02 #7212873432](https://github.com/Melchiorius/backend-interview-labs/issues/339#issuecomment-7212873432) приведены полные исходники, команды и результаты на Debian OpenJDK/javac 21.0.12.1.

- `Probe.java`: `javac --release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror`, затем JVM с явными UTF-8 параметрами. Оба кода завершения 0; stdout компилятора и stderr обоих процессов пусты, stdout JVM — 20 строк.
- `Warning.java`: те же параметры без -Werror, компиляция и JVM завершились с кодом 0. Одно предупреждение try.resource.not.referenced, stdout JVM — body.
- 11 изолированных отрицательных исходников: те же параметры без -Werror, каждый завершился с кодом 1, stdout пуст. Broad дополнительно предупреждает о потенциальном InterruptedException, Two — синтаксические ошибки; JVM не запускалась.

Это отдельные конечные проверки N14–22, не исторические 20 негативов G1, не пробы A12a (15 авторских/17 независимых) и не 24 ожидаемых отклонения других проверок CI. Все Markdown-блоки автоматически не исполнялись.

**Регрессия общего runner при первоначальной доработке D-A12b (роль 02).** На трёх точных восстановленных входах (исходник, скрипт, эталон; не полный клон) с LC_ALL=C.UTF-8, после сверки Git SHA и mode, выполнены bash verify.sh и два cmp (actual.stdout против эталона, печатный вывод против эталона с маркером), exits0/0/0, внешний stderr пуст. 20 строк/539 байт actual и fixture совпадают, SHA-256 c52f0f1b8a79641887f362ae7abc311e41925cea1de404664fd0673bb1a8b9f8; stdout21 строка/584 байта, SHA-256 ffd02db971b4ad8d35a37a24ea466ae31cd14f74d7ff25b61b258621d2651a78, последняя строка CHECK PASSED: stdout matches expected.stdout. Скрипт сам выполняет javac --release 21, одну JVM, diff -u и печать; не проверяет отдельно strict lint, stderr или hash. Это одна программа/20 наблюдений. Это результаты первоначального локального прогона 02, а не повторный запуск при редактуре G3. Новые временные пробы не включены в runner и CI.

**Отдельная интеграционная проверка N23 (роль 03).** В [отчёте 03 #7216580176](https://github.com/Melchiorius/backend-interview-labs/pull/340#issuecomment-7216580176) и README приведены отдельные уточнения измеряемых свойств исходной трассы. На OpenJDK/javac 21.0.12.1 Negative.java с checked-вызовом без обработки отвергнут компилятором (один отрицательный исходник, без -Werror, код 1); Positive.java с обработкой и проверками идентичности и отсутствия cause до и после addSuppressed строго скомпилирован с -Werror и выполнен (один положительный исходник, коды 0/0, три строки stdout, 93 байта). Роль 03 также отдельно выполнила неизменённый runner на трёх точных восстановленных входах: исходные 20 строк и маркер совпали. Это самостоятельные временные проверки, не новые строки P01–P20, не исполнение этих проб в CI и не окончательная приёмка пакета.

Не проверены все inline-блоки, GUI/внешние ссылки, другие JVM/ОС, живой IO/JDBC, стресс, принудительное завершение процесса.

**Где тот же принцип нужен Stream API:** [CORE-STREAM-ADV: `BaseStream.close()`, `onClose()` и suppressed-ошибки обработчиков](stream-advanced.md). Терминальная операция Stream сама по себе не обязана закрывать поток; если источник удерживает внешний ресурс, ответственность за его закрытие остаётся у вызывающего кода. Сравните этот контракт с try-with-resources выше; это не обещание автоматического `commit`/`rollback` или завершения внешнего IO.

**Читать дальше:** [CORE-EXCEPTIONS](exceptions.md) → **эта статья** → [CORE-INTERFACES](interfaces.md) → [CORE-INIT](initialization.md) → [Stream.close/onClose](stream-advanced.md); позднее IO-FILES / DB-JDBC и SPR-TX после появления этих материалов.

**Граница потоков ввода и безопасности:** [CORE-SERIAL](serialization.md) разбирает `ObjectOutputStream`/`ObjectInputStream` только на собственных доверенных `ByteArray`-потоках, специальные `readObject`/`InvalidObjectException` и запрет на недоверенную нативную десериализацию.
