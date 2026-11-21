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

<text>Все значения, которые разрешено бросать выражением `throw`, принадлежат классу `Throwable` или его подклассам. Существенная часть иерархии:</text>

~~~text
Throwable
├── Error                         unchecked
│   └── AssertionError
└── Exception                     не обязательно checked!
    ├── IOException               checked
    └── RuntimeException          unchecked
        ├── IllegalArgumentException
        └── NullPointerException
~~~

**Checked** — наследники `Throwable`, **за исключением** `RuntimeException`, `Error` и их наследников. Поэтому предложение «любой `Exception` checked» ложно: `RuntimeException extends Exception`, но является unchecked. И `Error` — тоже throwable: выражение `catch (Error e)` *синтаксически и семантически допустимо*, хотя восстановление после многих системных ошибок не является разумной общей стратегией.

~~~java
static void sample() {
    try {
        throw new IllegalArgumentException("invalid 67"); // unchecked
    } catch (RuntimeException e) {
        System.out.println(e.getMessage());
    }
}
~~~

Необязательность `throws RuntimeException` **не означает**, что исключение безвредно, что метод гарантирует корректный результат или что вызывающий не должен учитывать ошибку. Язык отвечает за проверяемость типов; бизнес-контракт определяет политику обработки. Нормы: [JLS §11.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.1), [Throwable API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html), [Exception API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Exception.html).

## Три разных действия: throw, throws и catch (ER-02, ER-03)

- `throw expression;` **создаёт abrupt completion** с полученным throwable-значением.
- `throws IOException` в сигнатуре метода или конструктора **объявляет** возможность распространения исключения и участвует в статической проверке, но ничего не бросает самостоятельно.
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

**Почему ошибка возможна, несмотря на пустое тело?** Компилятор клиента исходит из **объявленной сигнатуры вызываемого метода**, а не доказывает, что его реализация всегда безопасна. Библиотека может поменять тело без изменения контракта. Если вызывающий метод тоже объявляет `throws IOException`, он передаёт обязанность обработки следующему уровню; это не устранение ошибки.

Аналогично компилятор анализирует checked-исключения **конструкторов**, **инициализаторов** и **операций закрытия ресурсов**. Объект с `AutoCloseable.close() throws Exception` может создать checked-обязанность даже если тело `try` безобидно — конкретная механика в [CORE-RESOURCES](resources.md).

~~~java
class Bad {
    // static void f() { throw new Object(); }
    // Ошибка компиляции: Object не наследует Throwable
}
~~~

`throw null;` — особый случай: выражение допустимо по правилам типов, но при выполнении приводит к `NullPointerException`; оно не создаёт исключение «с типом null». Подробнее в разделе P1 ниже. Нормы: [JLS §11.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2), [§14.18](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.18), [§8.4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.6).

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

Если переставить `catch (Exception)` перед `catch (IOException)`, компилятор отвергнет недостижимый второй обработчик. Есть и другой отказ: `catch (IOException e)` для блока, который статически не может выбросить checked `IOException` (при отсутствии иных допустимых оснований), обычно отвергается с категорией **exception is never thrown**. Не сводите это правило к «catch можно написать для любого класса Throwable на всякий случай»: отдельные исключения из анализа, например unchecked, рассматриваются иначе. [JLS §14.20.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.1), [§11.2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.3).

## Multi-catch и запрет изменять параметр (ER-06)

Если разные **несвязанные** типы исключений приводят к одной обработке, их можно объединить через `|`:

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

Здесь у двух альтернатив нет отношения подтипов. Вариант `catch (IOException | Exception e)` недопустим: первая альтернатива уже является подтипом второй, поэтому их перечисление неоднозначно и избыточно. Параметр multi-catch **не разрешается переназначать** (в отличие от некоторых параметров обычного catch); это ограничение не мешает вызвать метод на пойманном объекте. Не путайте `|` с логическим `||` и не оставляйте символ Markdown `\|` в копируемом Java-файле (табличное экранирование существует только в G1-плане). [JLS §14.20](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20).

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

**Custom exception:** создание `class MissingOrderException extends RuntimeException` или `extends Exception` выбирает *статический контракт для клиентов*, а не просто уровень серьёзности. Checked может быть полезен, если вызывающий ожидаемо умеет устранить ситуацию и контракт явно требует обработку. Unchecked часто описывает нарушение предусловия или невозможность продолжить конкретную операцию, но само по себе **не является указанием игнорировать** её. Слишком широкая checked-иерархия на границе модулей усложняет изменение API; безусловное превращение всех проблем в `RuntimeException` стирает различие решений.

**Инженерная практика, не норма JLS:** логировать исключение на уровне, где известны пользовательская операция, идентификатор запроса и политика ответа; избегать нескольких одинаковых stacktrace на каждом промежуточном уровне; не добавлять секреты в сообщения. Перехват ради логирования с `throw e;` допустим, но не всегда полезен. При восстановлении следует учитывать идемпотентность операции, иначе повтор при частичном успехе может навредить.

> [!note]- Ручное suppressed и отключение suppression (ER-19)
> `Throwable.addSuppressed(other)` добавляет отдельное исключение, наблюдаемое в `getSuppressed()`, но не изменяет `getCause()`. Исключение нельзя подавить само собой; конструктор `Throwable(String, Throwable, boolean, boolean)` позволяет подклассу отключить suppression. Тогда `getSuppressed()` не показывает переданные значения. Это **не** способ автоматически восстановить работу после сбоя; [Throwable API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html) определяет точный контракт.

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

При вызове `Parent p = new Child(); p.run();` компилятор рассматривает статически выбранный `Parent.run() throws IOException`, даже если реальная реализация `Child.run()` не объявляет checked-исключений. Подробный overriding: [CORE-INHERITANCE](inheritance.md), правила [JLS §8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3).

Конструктор с `throws IOException` тоже создаёт статическую обязанность на месте `new`. В `finally` checked-исключение учитывается при анализе вызывающего метода; `try` не «аннулирует» объявление только из-за того, что работа завершилась. Функциональный интерфейс задаёт контракт checked-исключений лямбды:

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

Это не означает, что лямбда **вообще не может** бросать checked: подходящий функциональный метод может объявлять нужный `throws`. См. [CORE-FUNCTIONS](functions.md), [JLS §15.27](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27).

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

**Границы исполнения:** `System.exit`, `Runtime.halt`, завершение процесса ОС, аппаратные аварии и некоторые фатальные ошибки не дают универсальной гарантии выполнить `finally` и `AutoCloseable.close()`. Даже если cleanup выполнен, это не обещает завершения внешнего IO или транзакции. См. [CORE-RESOURCES](resources.md).

## Что эта статья не подменяет (ER-27, ER-28, ER-30)

- **ER-27 / P2:** *precise rethrow* может уточнять checked-типы для фактически не переназначаемого catch-параметра; это отдельная часть [JLS §11.2.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.2). Не строим здесь полную матрицу disjunctive types и generalized exception analysis.
- **ER-28 / P2:** ошибка инициализации класса, `ExceptionInInitializerError` и erroneous class принципиально отличаются от ошибки обычного конструктора; изложение уже есть в [CORE-INIT](initialization.md), не дублируем полную JVM-модель.
- **ER-30 / P2:** exception translation во фреймворке, политики rollback, HTTP error contracts, structured logging/trace IDs требуют конкретной версии Spring и архитектуры. Это будущие SPR-WEB/SPR-TX, не нормы JLS и не результат этого G2.

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
| ER-24 · P1 | Пределы гарантий наконец-то завершения/закрытия |
| ER-25 · P1 | Вложенные primary/suppressed — [CORE-RESOURCES](resources.md) |
| ER-26 · P1 | Конструкторы, finally, Runnable/lambda checked |
| ER-27 · P2 | Precise rethrow — граница, ссылка на JLS |
| ER-28 · P2 | [CORE-INIT](initialization.md), без дублирования |
| ER-29 · P2 | [CORE-RESOURCES](resources.md): IO/JDBC как граница |
| ER-30 · P2 | Spring/HTTP/transaction: будущий материал |

## Источники и воспроизводимость

**Нормативные источники:** [JLS 21 Chapter 11](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html), [§14.18–14.20](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html), [§8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3); [Throwable API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html), [Exception API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Exception.html). Классификация независимых интервью-источников — [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md): формулировки вопросов не доказывают гарантии Java.

Приведённые в статье маленькие исходники — **отдельные фрагменты**, их нельзя без подготовки вставлять в один файл из-за объявлений классов и импортов. Комментированные compile-negative строки поясняют **ожидаемую категорию ошибки**. Полный G1 включает **20 независимых negative N01–N20**; документ [плана](../../docs/plans/CORE-EXCEPTIONS-RESOURCES.md) записывает отдельные реальные проверки роли 01, но это **не означает**, что эти фрагменты были исполнены из данной статьи или CI. Общее Java 21 runnable согласуется с 03 в [Issue #104](https://github.com/Melchiorius/backend-interview-labs/issues/104); локальная ссылка на исполняемый пример добавляется **после** его физического включения в то же Git-дерево. **Авторская проверка роли 02 на OpenJDK/javac 21.0.12.1 (Linux):** заново созданы 20 **отдельных** Java-исходников N01–N20 из §6 G1, с удалением только табличного экранирования `\|` там, где нужен multi-catch `|`. Для каждого отдельно выполнен `javac --release 21 -d ...`: **20/20** завершились с ожидаемой **категорией диагностической ошибки** (не просто nonzero exit), включая catch ordering, checked throws, multi-catch, effectively-final и generic Throwable. Это локальный авторский эксперимент, **не GitHub CI**. Также отдельно скомпонован и запущен один **авторский smoke** ключевых механизмов `finally`/TWR (10 строк наблюдений) с `javac --release 21` и `java`: нормальная компиляция и выполнение, в том числе `return 67→68` и `throw null→NullPointerException`. Это *не* извлечение всех фрагментов из Markdown и не §5 positive P01–P20. Пробный `javac -Xlint:all -Werror` для заведомо демонстрационных `return` в `finally` и неиспользуемых resource variables дал ожидаемые lint-warning и ошибку **warnings found and -Werror specified**; именно поэтому полный учебный сценарий нельзя объявлять совместимым с `-Werror` без изменения кода/подавления диагностики. **NOT RUN:** автоматическая компиляция всех inline-snippets обоих Markdown, GUI Obsidian/IntelliJ, другие JDK/JVM, байткод/JIT/performance.

**Порядок чтения:** [CORE-INHERITANCE](inheritance.md) → **эта статья** → [CORE-RESOURCES](resources.md) → [CORE-INIT](initialization.md).
