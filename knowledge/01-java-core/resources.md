# CORE-RESOURCES — try-with-resources, порядок закрытия и подавленные исключения в Java 21

**Версия:** Java SE 21. **Исследовательский контракт:** [принятый план CORE-EXCEPTIONS/RESOURCES](../../docs/plans/CORE-EXCEPTIONS-RESOURCES.md) (ER-01–ER-30, Issue [#100](https://github.com/Melchiorius/backend-interview-labs/issues/100)); авторское [Issue #103](https://github.com/Melchiorius/backend-interview-labs/issues/103). Эта статья продолжает [CORE-EXCEPTIONS](exceptions.md): прежде чем разбираться, почему закрытие может создать ещё одно исключение, важно различать `throw`, `throws`, propagation, `getCause()` и `finally`.

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

Эта схема **иллюстрирует сигнатуры**, а не предлагает копировать объявления стандартных библиотечных интерфейсов в проект. Действительные API: [AutoCloseable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AutoCloseable.html) и [java.io.Closeable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Closeable.html).

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

Здесь `PlainResource.close()` не объявляет checked, и сам по себе TWR не требует `throws Exception`. А если написать `AutoCloseable resource = new PlainResource(); try (resource) { ... }`, статический тип ссылки — `AutoCloseable`, и checked-обязательство из его `close()` может понадобиться обработать, несмотря на фактический объект `PlainResource`. Не путайте виртуальный dispatch конкретной реализации и compile-time checking *объявленной* сигнатуры. См. [CORE-INTERFACES](interfaces.md), [CORE-INHERITANCE](inheritance.md), [JLS §11.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2).

**Идемпотентность:** `AutoCloseable` сам по себе **не обещает** безопасного многократного `close()` и не запрещает runtime-ошибки; контракт `Closeable` требует, чтобы закрытие уже закрытого объекта не имело эффекта. Прикладная реализация может иметь дополнительные условия. Не следует бездумно закрывать один и тот же объект вручную и в TWR, не прочитав API конкретного ресурса.

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

Закрытие идёт B → A, значит **первая ошибка закрытия** `close-B` становится primary, а более поздняя `close-A` добавляется к ней как suppressed. Если падает только один `close()`, его ошибка будет primary, а suppressed-список обычно пуст — **не нужно выдумывать отсутствующую ошибку работы**.

Это не гарантирует, что любой `close()` всегда кидает `IOException`: в демонстрационном классе так запрограммировано флагом `failOnClose`. Сама спецификация TWR задаёт **приоритет уже возникших ошибок**, не обещает, что ресурсы способны всегда закрыться безошибочно.

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

Начиная с Java 9 (включая Java 21) в заголовке TWR допустима уже объявленная локальная переменная или параметр, если он **final** или **effectively final**, точно определён при входе и имеет тип, совместимый с `AutoCloseable`. Необязательно заново объявлять `R r = ...` *внутри* скобок.

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

**Практическая ловушка:** внешний объект уже существовал до TWR, но TWR его **закроет**; другие ссылки на этот же объект после блока могут указывать на закрытое состояние. Это вопрос *владения временем жизни*, не «временной копии» и не передачи объекта по значению.

## Расширенная форма TWR: `catch` и `finally` выполняются после закрытия (ER-17)

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

Сначала отрабатывают ресурсные инициализаторы, затем тело, затем закрытие, **и только после этого** внешний `catch` (если есть подходящее исключение) и `finally`. Если B не создался, ошибка инициализации может попасть во внешний catch **после** закрытия A. Если тело успешно, но `close()` упал, catch может получить именно ошибку закрытия. «Catch обрабатывает только ошибки body» — неверно. [JLS §14.20.3.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3.2).

**Граница у примера:** `Traced.close()` статически объявляет `IOException`, поэтому даже если данный экземпляр создан с `false` и фактически не бросает, catch IOException остаётся допустимым — компилятор учитывает **сигнатуру**, не исследует флаг экземпляра.

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

## Проверяемое исключение из `close()`: какую сигнатуру учитывает компилятор (ER-18, ER-23)

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

## Стек вызовов, cause, suppressed: три разных диагностических вопроса (ER-19, ER-25)

| Диагностика | На какой вопрос отвечает | Пример |
|---|---|---|
| Stack trace | В какой точке было создано/брошено исключение и через какие вызовы оно прошло | Стек `IOException("work")` |
| `getCause()` | Какая **предыдущая ошибка была обёрнута** в новую | `IllegalStateException("service", io)` |
| `getSuppressed()` | Какие **дополнительные ошибки** были зарегистрированы при сохранении основной причины | `work` + `close-B`/`close-A` |

Один Throwable может иметь и cause, и suppressed. `getSuppressed()` возвращает массив; сами Throwable-объекты сохраняют собственные stack trace/cause. Но suppressed не обязаны быть исключительно «файловыми ошибками закрытия»: API предоставляет публичный `addSuppressed()` и позволяет добавлять их вручную. При отключённой suppression на уровне `Throwable` добавление не будет наблюдаться в обычном списке, поэтому нельзя обещать универсальную полноту diagnostics при любом subclass/конструкторе. [Throwable API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html).

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
| ER-17 · P0 | Extended TWR: catch/finally после закрытия |
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

**Авторские проверки роли 02 (OpenJDK/javac 21.0.12.1, Linux):** 20/20 отдельных N01–N20 из G1 §6 заново проверены на ожидаемые **категории** ошибок компиляции командой `javac --release 21`; это временные исходники, не CI. Дополнительно локально собран отдельный компактный smoke с `Traced implements AutoCloseable` и общим `StringBuilder`, выполнены `javac --release 21` и `java`: **10 строк одного запуска**, включая порядок `open-A;open-B;body;close-B;close-A;`, primary `work` плюс suppressed `[close-B, close-A]`, primary `close-B` плюс suppressed `[close-A]`, `init-B` плюс suppressed `[close-A]`, `null`, existing resource и extended `finally`. Это **авторская независимая компоновка**, не автоматическое извлечение всех блоков этой статьи и не обещание P01–P20 §5. Пробная компиляция того же smoke с `-Xlint:all -Werror` **не прошла** из-за предупреждений `[try]` о намеренно неиспользуемых в теле ресурсных ссылках и `[finally]` о замещающем abrupt completion; под обычным `--release 21` компиляция и запуск прошли. Такие warnings полезны для обучения и не являются ложной «ошибкой TWR». **NOT RUN:** автоматическая компиляция всех Markdown snippets, GUI Obsidian/IntelliJ, другие JDK/JVM, живой JDBC, bytecode/JIT/perf и реальная файловая система. Проверка общего `ExceptionResourceProbes.java` ролью 03: Debian OpenJDK/javac 21.0.12.1, `bash examples/core/exceptions-resources/verify.sh`, успешные `javac --release 21`/`java`/`diff -u` и **20/20 строк P01–P20**. Это фактическая проверка исходных code/stdout/script blobs роли 03, а не автоматический прогон Markdown snippets. CI на окончательном интегрированном HEAD фиксируется в PR #105 отдельно.

**Читать дальше:** [CORE-EXCEPTIONS](exceptions.md) → **эта статья** → [CORE-INTERFACES](interfaces.md) → [CORE-INIT](initialization.md); позднее IO-FILES / DB-JDBC и SPR-TX после появления этих материалов.
