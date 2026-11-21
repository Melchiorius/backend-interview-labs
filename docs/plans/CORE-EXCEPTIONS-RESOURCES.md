# CORE-EXCEPTIONS + CORE-RESOURCES — исследовательская спецификация Java 21 (G1)

**Статус:** исследование роли **01 — Research & Coverage**, до написания статей, **G1 ещё не принят**. [Issue #100](https://github.com/Melchiorius/backend-interview-labs/issues/100), родитель #33, запуск координатора [#7063291017](https://github.com/Melchiorius/backend-interview-labs/issues/100#issuecomment-7063291017). **Исходный BASE main:** `8d74028df7937b8597bb606c529a7d2b6b82c329` (после PR #99); **версия норм:** Java SE 21 / JLS 21, без preview. **Авторский локальный прогон:** OpenJDK 21.0.12.1 / javac 21.0.12.1 (Debian), `--release 21`. HEAD исследовательской ветки и CI фиксируются в PR после коммита. **Это план, не учебная статья, не runnable module и не изменение COVERAGE.**

## 1. Главный вопрос, разделение на статьи и критерии

**Основная задача читателя:** предсказать, компилируется ли код, какая именно ошибка распространяется из обычного `try/catch/finally`, что происходит с первоначальным исключением и вложенными causes, какие ресурсы будут закрыты, в каком порядке и что окажется в `getSuppressed()`, если одновременно не удались работа и закрытие.

**Рекомендация роли 01: две отдельные связанные статьи, один исследовательский план и один объединённый воспроизводимый сценарий, если роль 03 после G1 докажет его педагогическую ценность.**

- **CORE-EXCEPTIONS** — `Throwable/Error/Exception/RuntimeException`, *checked/unchecked* (статический анализ вызовов, `throw`/`throws`), обработка и распространение, catch order, multi-catch, `finally`/abrupt completion, wrapping/cause, собственные исключения и уровни ответственности. Слова «любой Exception checked», «catch Exception ловит Error», «finally гарантированно выполнится при остановке JVM» запрещены как неточные.
- **CORE-RESOURCES** — `AutoCloseable`, `Closeable`, TWR, закрытие в обратном порядке, частичная инициализация, `null` и existing effectively-final resource reference, primary/suppressed при failures в теле и `close()`, последовательность `catch/finally` при extended TWR. Объяснять, **почему TWR сохраняет первичную ошибку иначе, чем `finally`**, а не просто «автоматически закрывает».
- **Мост:** одна проверочная Java-программа §5 с `IOException`, слоями исключений и ресурсом с контролируемым `close()`; §6 — изолированные отрицательные компиляции. Материал CORE-RESOURCES **не** дублировать как полноценный курс IO/JDBC, где есть собственные `close`-контракты, transactions и сетевые сбои.
- **Цель для G1:** 30 вопросов с явно помеченными P0/P1/P2, точными JLS/API ссылками, положительными/отрицательными доказательствами и границами. Уроки 02 пишутся только после явного G1 решения 00; 03 не обязан создавать упражнение ради наличия двух ID.

**Не смешивать виды гарантий:** JLS определяет поток управления и compile-time checking; API описывает `Throwable`/`AutoCloseable`/`Closeable`; архитектурный совет «оборачивать на границе слоя, логировать там, где есть контекст» — рекомендация, **не** норма языка. Даже успешно выполненный `close()` не означает успешный commit транзакции или отсутствие потерянных данных.

## 2. Источники A (нормативные, Java SE 21) и конкретные правила

**Реестровые ID:** [OFF-JLS21 / OFF-API21](../SOURCE_REGISTRY.md). Основные нормы:

- **[JLS 21 §11.1–§11.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.1)** — только `Throwable` и подклассы являются throwable; checked = `Throwable` и его наследники, *кроме* `RuntimeException` и `Error` и их подклассов. `Exception` — класс с checked и unchecked наследниками; `Error` технически catchable, но unchecked и обычно не предназначен для recovery. Отдельно asynchronous exceptions; не обещать закрытие ресурсов при прекращении процесса.
- **[JLS 21 §11.2.1–§11.2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2)** — compile-time exception analysis и catch/declare. Проверяется не то, что «фактически случится», а множество исключений, которое по статическому анализу может быть выброшено выражениями/методами/ресурсами, в том числе `close()`. Для checked `IOException` нужны подходящий handler либо `throws` (с оговорёнными правилами precise rethrow).
- **[JLS 21 §11.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.3)** — распространение исключения, abrupt completion, поиск обработчика.
- **[JLS 21 §14.18](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.18)** — `throw` принимает `Throwable`; [§8.4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.6) — `throws` декларация; [§8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3) — override **не расширяет** checked `throws` сверх правил родительского метода. На уровне вызывающей ссылки имеет значение статически выбранный контракт.
- **[JLS 21 §14.20](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20)** — формы `try/catch`/multi-catch/`finally`; [§14.20.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.1) — catch ordering, multi-catch alternatives и effectively final multi-catch parameter. `catch(IOException|Exception)` запрещён из-за subtype relationship.
- **[JLS 21 §14.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.2)** — `finally` выполняется при нормальном/резком завершении (в рамках исполняемой программы), **но если сам `finally` завершается резко, прежняя причина abrupt completion может быть отброшена**. Значение выражения `return` вычисляется до `finally`, а новый `return` из `finally` заменяет предыдущий. Не обещать запуск finally после `Runtime.halt/System.exit`, аварии JVM, убийства процесса или отказа питания.
- **[JLS 21 §14.20.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3)** — TWR: ресурсы инициализируются **слева направо**, закрываются **справа налево**; если инициализатор последнего ресурса бросил исключение, **ранее успешно созданные** ресурсы закрываются; `null` не закрывается; external final/effectively-final resource variable допустима в Java 21.
- **[JLS 21 §14.20.3.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3.1)** — точная трансляция TWR, primary throwable, `addSuppressed` для `close()`-ошибки, если уже была primary; если основная работа прошла нормально, ошибка первого провалившегося закрытия становится primary, поздние ошибки — suppressed. Количество/порядок в реальном случае виден в §5.
- **[JLS 21 §14.20.3.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3.2)** — extended TWR: `catch/finally` выполняются **после попыток закрытия** ресурсов, а catch может поймать сбой инициализации или закрытия.
- **[Java SE 21 Throwable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Throwable.html)** — `getCause()/initCause()`, `addSuppressed()/getSuppressed()`, стектрейс, suppression может быть disabled отдельными конструкторами; причиной не является автоматически suppressed. Не называть suppressed «цепочкой causes».
- **[Java SE 21 Exception](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Exception.html), [RuntimeException](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/RuntimeException.html), [Error](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Error.html)** — типовая иерархия и checked boundaries.
- **[Java SE 21 AutoCloseable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AutoCloseable.html), [java.io.Closeable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Closeable.html)** — `AutoCloseable.close() throws Exception`; `Closeable.close() throws IOException`. `AutoCloseable` **не требует** идемпотентности; `Closeable` предоставляет более узкий `throws` и свой контракт повторного закрытия. Нельзя делать вывод, что любой ресурс непременно бросит `Exception`: учитывается **объявленный тип конкретного `close`**.

## 3. Независимые источники C: карта реальных интервью-вопросов

Записи уже есть в [SOURCE_REGISTRY](../SOURCE_REGISTRY.md); новые реестровые ID не создаются. Фактически проверено наличие вопросов в GitHub-файлах:

- **Q-HRUDAY** — [Core Java Interview Questions List](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md), **Chapter 10 — Exception Handling, 94 questions**: иерархия, checked/unchecked, `finally`, custom exception, TWR/AutoCloseable, multi-catch и pitfalls. Проверено оглавление/тематические вопросы, **не** истинность каждого ответа.
- **Q-IN28** — [JavaInterviewQuestionsAndAnswers](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/readme.md), **Q91–108**: `finally` и пределы его работы, `try` forms, checked propagation, multi-catch, TWR, best practices. Формулировки из разных версий Java сверяются по JLS 21.
- **Q-CHERNYSHOV** — [Русский банк вопросов](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/ru/README.md), **§7 «Исключения» (29 вопросов)**: checked/unchecked, `Throwable`, `finally`, TWR/AutoCloseable/Closeable, wrapping, suppressed, порядок catch.
- **Q-TEAMLEAD** — [java-interview-questions](https://github.com/teamlead/java-interview-questions), найденные prompts по checked/unchecked, exception hierarchy, `finally`, перезаписи исключения, AutoCloseable/TWR. Повторы банка и метки сложности не равны независимой статистике реальных интервью.
- **Q-BAELDUNG-CORE** — [Baeldung Java interview](https://www.baeldung.com/java-interview-questions), существующая запись в реестре; **полный разбор всех его ответов не проводился**. При G4 повторно сверить конкретные пункты и зафиксировать gaps, а не считать их просмотренными.

**Ограничения:** ни один интервью-каталог не является нормативной спецификацией или доказательством частотности. У каждого банка нет проверенной репрезентативной выборки интервью; материалы могут повторять устаревшие ответы. Ответы о порядке закрытия, suppressed и `finally` выводятся **только** из JLS/API и воспроизводимых тестов.

## 4. Матрица 30 вопросов: 18 P0, 8 P1, 4 P2

`P01–P20` — метки 20 строк stdout **одного** положительного запуска (§5); `N01–N20` — двадцать **отдельных** отрицательных `javac` (§6), а не двадцать ошибок одного файла. Приоритеты отражают обязательность темы, **не** уровень должности.

| ID | Приоритет / что обязан объяснить автор | Норма / проверка | Владелец будущей статьи |
|---|---|---|---|
| ER-01 | **P0** Иерархия `Throwable → Error/Exception → RuntimeException`; checked определяется **исключением** подклассов RuntimeException/Error, не словом «Exception». | JLS §11.1, API; P01 | EXCEPTIONS |
| ER-02 | **P0** `throw`, `throws` и обработка checked: `throws` лишь объявляет возможность, catch перехватывает runtime объект; `throw` требует Throwable. | §11.2, §14.18; P02, N01/N06/N20 | EXCEPTIONS |
| ER-03 | **P0** Статическая проверка checked у **вызывающего** метода, конструктора и `close`; даже если тело вызываемого метода реально не бросает. | §11.2; N02/N10/N16 | EXCEPTIONS |
| ER-04 | **P0** Как работает propagation по стеку: ближайший подходящий catch; необработанное исключение не превращается в returned value; unchecked не освобождает от смыслового контракта. | §11.3; P02/P07 | EXCEPTIONS |
| ER-05 | **P0** Catch ordering: специфический перед общим, иначе unreachable checked catch; не все catch для checked допустимы без возможного throw. | §14.20.1; N03/N07/N19 | EXCEPTIONS |
| ER-06 | **P0** Multi-catch не допускает связанных subtype alternatives, parameter нельзя присваивать; допустимые несовместимые альтернативы делят обработчик. | §14.20; P08, N04/N05 | EXCEPTIONS |
| ER-07 | **P0** Wrapping/cause: `new IllegalStateException("service", cause)` сохраняет причину и контракт слоя; `getCause()` не равно `getSuppressed()`. | Throwable API; P03/P20 | EXCEPTIONS |
| ER-08 | **P0** Custom exceptions и error handling на границе: когда checked/unchecked оправдан, когда не «глотать» ошибку, логировать один раз на ответственном уровне; это инженерная рекомендация, не JLS. | API/JLS + дизайн | EXCEPTIONS |
| ER-09 | **P0** `finally` не теряет уже вычисленное значение простого return, если лишь меняет local variable; normal/abrupt completion. | §14.20.2; P04 | EXCEPTIONS |
| ER-10 | **P0** `return` либо `throw` в `finally` **заменяют** прежний return/exception; в отличие от TWR исходное исключение может пропасть. `finally` не обещан при завершённом процессе. | §14.20.2; P05/P06, N17 | EXCEPTIONS |
| ER-11 | **P0** Отличие `AutoCloseable` / `Closeable`: declared throws, условия идемпотентности, ресурс должен поддерживать нужный интерфейс, `close` должен быть public. | API; P18/P19, N09/N13 | RESOURCES |
| ER-12 | **P0** TWR инициализирует слева направо, закрывает справа налево после блока; `close` вызывается и при исключении. | §14.20.3; P09 | RESOURCES |
| ER-13 | **P0** Если body throws и close тоже throws, body exception — primary; исключения close попадают в `getSuppressed()` в порядке обратного закрытия. | §14.20.3.1; P10 | RESOURCES |
| ER-14 | **P0** Если body успешно, но два `close` падают, primary — исключение правого ресурса; следующее добавляется в suppressed; единственный close failure — primary. | §14.20.3.1; P11/P17 | RESOURCES |
| ER-15 | **P0** Если инициализация второго ресурса бросила исключение, уже созданный первый закрывается; его close failure подавляется в пользу initialization exception. | §14.20.3; P12/P13 | RESOURCES |
| ER-16 | **P0** `null` ресурс не вызывает `close`; existing final/effectively-final variable в TWR разрешена (Java 9+), переопределяемая переменная запрещена. | §14.20.3; P14/P15, N11/N15 | RESOURCES |
| ER-17 | **P0** Extended TWR: ресурсы уже закрыты **до** внешних `catch/finally`; catch может получить ошибку инициализации/close. | §14.20.3.2; P16, P12 | RESOURCES |
| ER-18 | **P0** Checked, объявленный `AutoCloseable.close()`, учитывается компилятором; `Closeable.close()` уже по throws; правила override нельзя расширять. | §11.2/§8.4.8.3/API; N10/N12 | RESOURCES + EXCEPTIONS |
| ER-19 | **P1** `suppressed` и cause — две отдельные связи; suppression вручную и её отключение на уровне Throwable. | Throwable API; P20 | EXCEPTIONS/RESOURCES |
| ER-20 | **P1** Точная граница overriding checked: subclass может сужать/не объявлять, но не расширять сверх родителя; вызывающего проверяют по статическому типу. | §8.4.8.3; N12, CORE-INHERITANCE | EXCEPTIONS |
| ER-21 | **P1** `throw null` компилируется, но приводит к NullPointerException; отличается от `throw new Object()`. | §14.18 | EXCEPTIONS; отдельный runtime пример |
| ER-22 | **P1** `Error` unchecked/catchable; прерывания и `InterruptedException` требуют корректного управления состоянием потока, не универсального `catch(Exception)`. | §11.1, Thread API | EXCEPTIONS; CON-INTERRUPT граница |
| ER-23 | **P1** Ресурс может объявлять собственный более узкий throws; не каждое TWR требует `throws Exception`. | §11.2, API; N10/N19 | RESOURCES |
| ER-24 | **P1** Какие события не гарантируют выполнение `finally`/`close`? Процесс может завершиться, выполнение потока не продолжиться; не смешивать JLS flow с OS kill/OutOfMemory. | JLS §14.20, runtime limits | EXCEPTIONS/RESOURCES |
| ER-25 | **P1** Вложенные primary/suppressed: порядок от двух close, смысл стектрейса; suppressed — не «потерянная» ошибка и не замена cause. | §14.20.3.1; P10/P11/P13 | RESOURCES |
| ER-26 | **P1** Распространение checked в конструкторах, `finally`, интерфейсах и lambdas; не все исключения можно выбрасывать из `Runnable.run()`. | §11.2, §8.8, §15.27; N16/N17, CORE-FUNCTIONS | EXCEPTIONS |
| ER-27 | **P2** Precise rethrow, финальность catch variable, disjunctive types и generalized exception analysis. | JLS §11.2.2, §14.20 | EXCEPTIONS: углубление |
| ER-28 | **P2** Исключение при class initialization и `ExceptionInInitializerError`, erroneous class; отличается от ошибки instance constructor. | JLS §12.4.2, CORE-INIT | ссылка на опубликованное, не повторять |
| ER-29 | **P2** Контракты `try-with-resources` для реальных streams/JDBC Connection, close/commit/rollback и suppressed, vendor-specific behavior. | IO/JDBC API, не только JLS | IO-FILES / DB-JDBC |
| ER-30 | **P2** Exception translation Spring, transaction rollback policies, HTTP error contracts, structured logging/trace IDs. | Конкретные Spring/API версии | SPR-WEB / SPR-TX |

**Содержание после G1:** первая статья раскрывает ER-01–ER-10 и необходимые P1, вторая — ER-11–ER-18, с явным сравнением primary/suppressed и `finally`. ER-19–ER-26 — полноценные ограничения либо честно отложенные вложенные разделы; ER-27–ER-30 не объявлять реализованными. В каждой статье нужны не только таблицы, но и связный механизм и разобранные исходники. Невозможно считать suppressed объяснённым простым `close()` без случая совместного throw.

## 5. Positive: один полный Java 21 source, 20 фактических stdout-строк

**Файл для воспроизведения:** `ExceptionResourceProbes.java`. Сохранить следующий листинг в отдельный файл и выполнить:

```sh
mkdir -p out
javac --release 21 -d out ExceptionResourceProbes.java
java -cp out ExceptionResourceProbes > actual.stdout
# В expected.stdout поместить 20 строк из блока ниже
diff -u expected.stdout actual.stdout
```

Автором 01 команды реально выполнены на **OpenJDK 21.0.12.1**. Это **одна** компиляция / **один** запуск / **20** подписанных наблюдений, а не 20 независимых compiled programs. Точное совпадение полученного stdout с приведённым результатом проверено.

```java
import java.io.*;
import java.util.*;

public class ExceptionResourceProbes {
  static final class R implements AutoCloseable {
    final String name;
    final StringBuilder log;
    final boolean broken;
    R(String name, StringBuilder log, boolean broken) {
      this.name = name; this.log = log; this.broken = broken;
      log.append("open-").append(name).append(';');
    }
    @Override public void close() throws IOException {
      log.append("close-").append(name).append(';');
      if (broken) throw new IOException("close-" + name);
    }
  }
  static R failedInit(StringBuilder log) throws IOException {
    log.append("init-fail;");
    throw new IOException("init-B");
  }
  static int finallyValue() {
    int n=67;
    try { return n; }
    finally { n=68; }
  }
  static int finallyOverrideReturn() {
    try { return 67; }
    finally { return 68; }
  }
  static String finallyOverrideThrow() {
    try { throw new IOException("work"); }
    finally { throw new IllegalStateException("finally"); }
  }
  static void checkedOperation() throws IOException { throw new IOException("io"); }
  static void wrappedOperation() {
    try { checkedOperation(); }
    catch (IOException e) { throw new IllegalStateException("service",e); }
  }
  public static void main(String[] args) throws Exception {
    System.out.println("P01=" + (new IOException() instanceof Exception) + "," + (new AssertionError() instanceof Error));
    try { checkedOperation(); } catch(IOException e) { System.out.println("P02=" + e.getMessage()); }
    try { wrappedOperation(); } catch(IllegalStateException e) { System.out.println("P03=" + e.getMessage() + "," + e.getCause().getClass().getSimpleName() + "," + e.getCause().getMessage()); }
    System.out.println("P04=" + finallyValue());
    System.out.println("P05=" + finallyOverrideReturn());
    try { finallyOverrideThrow(); } catch (Exception e) { System.out.println("P06=" + e.getClass().getSimpleName() + "," + e.getMessage() + "," + e.getSuppressed().length); }
    try { throw new IllegalArgumentException("unchecked"); } catch (RuntimeException e) { System.out.println("P07="+e.getMessage()); }
    try { throw new IOException("checked"); } catch (IOException | IllegalStateException e) {System.out.println("P08="+e.getClass().getSimpleName()); }
    StringBuilder log = new StringBuilder();
    try(R a = new R("A",log,false); R b = new R("B",log,false)) {log.append("body;");}
    System.out.println("P09=" + log);
    log = new StringBuilder();
    try(R a = new R("A",log,true); R b = new R("B",log,true)) {
      log.append("body;"); throw new IOException("work");
    } catch(IOException e) {System.out.println("P10="+log+"primary-"+e.getMessage()+",supp-"+Arrays.toString(Arrays.stream(e.getSuppressed()).map(Throwable::getMessage).toArray())); }
    log = new StringBuilder();
    try(R a = new R("A",log,true); R b = new R("B",log,true)) {log.append("body;");}
    catch(IOException e) {System.out.println("P11=" +log+"primary-"+e.getMessage()+",supp-"+Arrays.toString(Arrays.stream(e.getSuppressed()).map(Throwable::getMessage).toArray()));}
    log = new StringBuilder();
    try(R a = new R("A",log,false); R b = failedInit(log)) {log.append("body;");}
    catch(IOException e) {System.out.println("P12="+log+"primary-"+e.getMessage()+",supp-"+e.getSuppressed().length);}
    log = new StringBuilder();
    try(R a = new R("A",log,true); R b = failedInit(log)) {log.append("body;");}
    catch(IOException e) {System.out.println("P13="+log+"primary-"+e.getMessage()+",supp-"+Arrays.toString(Arrays.stream(e.getSuppressed()).map(Throwable::getMessage).toArray()));}
    log = new StringBuilder();
    R none = null;
    try(none) {log.append("body;");}
    System.out.println("P14="+log);
    log = new StringBuilder();
    R existing = new R("E",log,false);
    try(existing) {log.append("body;");}
    System.out.println("P15="+log);
    log = new StringBuilder();
    try(R a = new R("A",log,false)) {log.append("body;");}
    finally {log.append("finally;");}
    System.out.println("P16="+log);
    try(R a = new R("X",new StringBuilder(),true)) {}
    catch (IOException e) { System.out.println("P17="+e.getMessage()+",supp-"+e.getSuppressed().length); }
    AutoCloseable general = () -> { throw new Exception("auto"); };
    try(general) { System.out.println("P18=body"); }
    catch(Exception e) { System.out.println("P19="+e.getMessage()); }
    try { throw new IOException("x"); }
    catch (IOException e) { e.addSuppressed(new IllegalArgumentException("other")); System.out.println("P20="+e.getSuppressed()[0].getMessage()); }
  }
}
```

**Фактический stdout (OpenJDK 21.0.12.1, 20/20 строк):**

```text
P01=true,true
P02=io
P03=service,IOException,io
P04=67
P05=68
P06=IllegalStateException,finally,0
P07=unchecked
P08=IOException
P09=open-A;open-B;body;close-B;close-A;
P10=open-A;open-B;body;close-B;close-A;primary-work,supp-[close-B, close-A]
P11=open-A;open-B;body;close-B;close-A;primary-close-B,supp-[close-A]
P12=open-A;init-fail;close-A;primary-init-B,supp-0
P13=open-A;init-fail;close-A;primary-init-B,supp-[close-A]
P14=body;
P15=open-E;body;close-E;
P16=open-A;body;close-A;finally;
P17=close-X,supp-0
P18=body
P19=auto
P20=other
```

**Интерпретация:**
- `P04–P06`: обычный `finally` способен заменить возврат или исключение. В P06 старый `IOException("work")` **не стал** automatically suppressed.
- `P09–P13`: initialize A→B, close B→A. При ошибке body primary = `work`, suppressed = `close-B` затем `close-A`. Если body успешен, primary = `close-B`, suppressed = `close-A`. При ошибке инициализации B закрывается A; ошибка A.close становится suppressed к `init-B`.
- `P14/P15`: null value пропускает `close`, existing effectively-final local закрывается; однако уже закрытый ресурс нельзя **безусловно** считать повторно безопасным.
- `P16`: `finally` extended TWR наблюдает уже выполненное `close`. `P18/P19`: объявленное `AutoCloseable.close throws Exception` требует подходящего handler/throws. `P20`: ручное suppressed не стало cause.

## 6. Negative: 20 изолированных проверок javac Java 21

Каждый snippet следующей таблицы — **полный самостоятельный исходник** в собственном файле `negative/<ID>.java`. Выполнить **по одному файлу**, не склеивая примеры с повторяющимися/связанными именами; команда: `javac --release 21 -d out-negative negative/<ID>.java`. У автора на OpenJDK 21.0.12.1 **20/20 независимых компиляций завершились nonzero exit** по нужной категории. Точный английский diagnostic, число сообщений и позиции строк — детали `javac`, а не нормативный текст JLS.

| ID = имя файла без расширения | Полный source | Проверенная причина отказа |
|---|---|---|
| `N01_UnhandledChecked` | `import java.io.*; class N01_UnhandledChecked {void work(){ throw new IOException("x"); }}` | Unhandled checked IOException |
| `N02_ThrowsNotHandled` | `import java.io.*; class N02_ThrowsNotHandled {void f() throws IOException{} void g(){f();}}` | Объявленного `throws IOException` вызываемому методу достаточно для проверки вызывающего |
| `N03_WrongCatchOrder` | `import java.io.*; class N03_WrongCatchOrder {void g(){try{throw new IOException();}catch(Exception e){}catch(IOException e){}}}` | `IOException` уже перехвачено более общим catch |
| `N04_UnrelatedMultiCatch` | `import java.io.*; class N04_UnrelatedMultiCatch {void g(){try{throw new IOException();}catch(IOException \| Exception e){}}}` | Multi-catch не допускает связанные subclass alternatives |
| `N05_MultiCatchAssignment` | `import java.io.*; class N05_MultiCatchAssignment {void g(){try{if(System.nanoTime()<0)throw new IOException();else throw new IllegalArgumentException();}catch(IOException \| IllegalArgumentException e){e=new IllegalArgumentException();}}}` | Параметр multi-catch нельзя переназначить |
| `N06_NotThrowable` | `class N06_NotThrowable {void g(){throw new Object();}}` | `throw` требует Throwable |
| `N07_CatchNeverThrown` | `import java.io.*; class N07_CatchNeverThrown {void g(){try{int x=67;}catch(IOException e){}}}` | Checked IOException не может быть выброшено из try |
| `N08_NoCatchFinallyResources` | `class N08_NoCatchFinallyResources {void g(){try{int n=67;}}}` | Обычный try без catch/finally/resource specification недопустим |
| `N09_ResourceNotCloseable` | `class N09_ResourceNotCloseable {void g(){try(String s="x") {}}}` | `String` не AutoCloseable |
| `N10_CloseCheckedUnhandled` | `class N10_CloseCheckedUnhandled {static class R implements AutoCloseable {public void close() throws Exception {}} void g(){try(R r=new R()) {}}}` | Checked exception из `close` не обработан |
| `N11_ResourceReassigned` | `class N11_ResourceReassigned {static class R implements AutoCloseable{public void close(){}} void g(){R r=new R();r=new R();try(r){}}}` | External resource variable не effectively final |
| `N12_OverrideBroaderChecked` | `import java.io.*; class N12_OverrideBroaderChecked {static class Parent{void f() throws IOException{}} static class Child extends Parent{ @Override void f() throws Exception{}}}` | Override расширил checked throws |
| `N13_ResourceCloseVisibility` | `class N13_ResourceCloseVisibility {static class R implements AutoCloseable {void close(){}}}` | Нельзя уменьшать public visibility реализации AutoCloseable.close |
| `N14_MultiCatchNotEffectivelyFinal` | `import java.io.*; class N14_MultiCatchNotEffectivelyFinal {void g(){try{throw new IOException();}catch(IOException\|IllegalArgumentException e){e=null;}}}` | Второй пример запрета переназначения multi-catch parameter |
| `N15_TryNotInitialized` | `class N15_TryNotInitialized {static class R implements AutoCloseable {public void close(){}} void g(){R r;try(r){}}}` | External resource variable not definitely assigned |
| `N16_CheckedConstructor` | `import java.io.*; class N16_CheckedConstructor {N16_CheckedConstructor() throws IOException {} void g(){new N16_CheckedConstructor();}}` | Вызывающий конструктор должен обработать объявленный checked |
| `N17_CheckedInFinally` | `import java.io.*; class N17_CheckedInFinally {void g(){try{int a=67;}finally{throw new IOException();}}}` | Checked из finally учитывается в throws/handler |
| `N18_GenericThrowable` | `class N18_GenericThrowable<T> extends Exception {}` | Generic Throwable-subclass недопустим, отдельная граница CORE-ERASURE |
| `N19_CloseExceptionCatchUnreachable` | `import java.io.*; class N19_CloseExceptionCatchUnreachable {static class R implements AutoCloseable{public void close(){}} void g(){try(R r=new R()){}catch(IOException e){}}}` | Checked IOException из `close()`, который ничего checked не объявляет, не предполагается статически |
| `N20_ThrowsTypeNotThrowable` | `class N20_ThrowsTypeNotThrowable {void g() throws String{}}` | `throws` ожидает Throwable subtype |

**Повторная проверка после исправления B1 / review #6005825300:** все N01–N20 по отдельности реконструированы из приведённых однострочных Java sources с удалением только Markdown-экранирования `\|` → `|`; каждый из 20 вызовов `javac --release 21 -d out-negative negative/<ID>.java` завершился nonzero **именно по заявленной диагностической категории** (20/20), без `reached end of file while parsing` и иных ошибок синтаксического разбора исходника. Для N04 после добавления последней `}` результат: `Alternatives in a multi-catch statement cannot be related by subclassing`, потому что `IOException` — подкласс `Exception` (JLS 21 §14.20.1). Это отдельная локальная проверка роли 01 на OpenJDK 21.0.12.1; GitHub CI не исполняет код внутри Markdown.

**Предосторожность при копировании:** в Markdown ячейках multi-catch запись `\|` — экранирование вертикальной черты для разметки таблицы. Для Java source удалить только Markdown escape перед `|`: на диск должен попасть оператор `|`, а не обратная косая черта. Иначе будет отказ по неверной причине. Более надёжно извлекать содержимое между backticks Markdown-парсером, который убирает этот escape.

**Что не является compile error:** `throw null` компилируется (runtime NullPointerException); unchecked исключение допустимо без `throws`; `AutoCloseable.close()` может не объявлять checked, тогда вызывающему не обязательно писать `throws Exception`; TWR с existing effectively-final variable законен. Из этих контрпримеров не делать обобщение «try всегда требует catch».

## 7. Связи с опубликованными материалами и непокрытые границы

Реальное существование ссылочных целей проверено на `main @ 8d74028df7937b8597bb606c529a7d2b6b82c329`:

- [CORE-CLASSES](../../knowledge/01-java-core/classes.md) — constructor exceptions и side effects, но не error-handling guide.
- [CORE-INIT](../../knowledge/01-java-core/initialization.md) — `ExceptionInInitializerError`, erroneous class, исключение instance construction; сослаться, не копировать целый блок.
- [CORE-INHERITANCE](../../knowledge/01-java-core/inheritance.md) — override throws, статический контракт вызывающей ссылки; здесь подробно только новое error-handling следствие.
- [CORE-INTERFACES](../../knowledge/01-java-core/interfaces.md) — checked throws при совпадении интерфейсных методов, не повторять весь diamond/overriding.
- [CORE-PARAMS](../../knowledge/01-java-core/parameters.md) — порядок вычисления выражений и возникновение исключения при вычислении аргумента.
- [CORE-FUNCTIONS](../../knowledge/01-java-core/functions.md) — `Runnable`/lambda checked exception contract.
- [CORE-GENERICS](../../knowledge/01-java-core/generics.md), [CORE-ERASURE](../../knowledge/01-java-core/erasure.md) — generic Throwable restriction, erasure, не уходить в PECS.
- [Предварительный план Generics/Erasure](CORE-GENERICS-ERASURE.md) — историческая исследовательская спецификация, не замена опубликованных статей.
- [CURRICULUM](../CURRICULUM.md), [COVERAGE](../COVERAGE.md), [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md), [SOURCE_REGISTRY](../SOURCE_REGISTRY.md) — факт карты и статусов, не свидетельство готовой темы. Текущий COVERAGE может отставать от G5 Generics/Erasure до окончания G6 #97 — не исправлять в PR исследования.

**Будущие ID — пока без фиктивных локальных Markdown-ссылок:** CORE-EXCEPTIONS, CORE-RESOURCES (цели), IO-FILES, DB-JDBC, SPR-WEB, SPR-TX, DIST-FAIL, CON-INTERRUPT, CON-JMM, JVM-ERRORS и прочие темы соответствующих разделов. Для появившихся к G2/G4 материалов сначала сверять актуальный main, только потом добавлять ссылки. Не дублировать уравнивание TWR c DB transaction commit или полную практику закрытия Reactor/Spring reactive streams.

## 8. Статус проверки, открытые риски и требования к G1

**Фактически выполнено 01:** OpenJDK/javac 21.0.12.1; один positive source `ExceptionResourceProbes.java` — успешная компиляция с `--release 21`, один запуск и **20/20** подписанных строк expected stdout; **20 отдельных** negative `javac --release 21` — **20/20 nonzero exits** с проверкой *категории* ошибки, не только факта отказа. Нормативные JLS/API страницы и четыре независимых перечня вопросов прочитаны по указанным разделам. Полные sources и ожидаемые результаты воспроизводимы из данного Markdown; временные `.java` файлы не добавлялись в репозиторий.

**Не запускалось:** локальные Gradle/JUnit/Mockito по проекту, IntelliJ/Obsidian GUI preview и клики, альтернативные JDK/JVM, операционные тесты JVM shutdown/kill, реальный IO/JDBC/transaction rollback, property-based testing, performance/JMH, Spring exception translation и тесты будущего runnable примера 03. GitHub Actions для PR этого исследования проверяются **по точному новому HEAD** после открытия PR; даже зелёный существующий CI не компилирует исходники, находящиеся только внутри Markdown.

**Обязательное независимое G1 review 04 (до решения 00):**
1. Проверить ER-01–ER-18 и классификацию P1/P2, источники JLS/API и границы с опубликованными статьями.
2. Перепроверить ключевые наблюдения `P04–P06` (скрытая подмена `finally`), `P10–P13` (primary/suppressed и partial init), `P14–P16` (null/existing resource/extended finally), негативы N03–N05/N09–N13/N19; при необходимости воспроизвести на независимом каталоге.
3. Убедиться, что ни в тексте, ни в будущем примере не появятся ложные утверждения о безусловном `finally`, TWR закрытии при kill, подавлении primary plain finally, гарантированной идемпотентности AutoCloseable и автоматическом commit/rollback JDBC.
4. Дать мотивированный G1 verdict по **точному HEAD** с нормативными ссылками и IDs блокеров. Авторская локальная проверка и CI ранее существующих тестов — не независимое review.

**Действия следующего gate:** только после оценки 04 роль 00 явно решает `G1 ACCEPTED / BLOCKED`. После G1 00 поручает 02 писать одну/две связанные статьи и 03 — оправданный компактный runnable при реальной учебной пользе. 05 оценивает естественность русского текста и полноту механизмов, 06 синхронизирует навигацию и COVERAGE **после фактической публикации**, не сейчас. PR исследования содержит **только этот Markdown**; merge только 00.

### Передача эстафеты
Кому: 04 — Independent Reviewer
Что сделать: провести независимую предметную G1-проверку `docs/plans/CORE-EXCEPTIONS-RESOURCES.md` в новом PR на фактическом HEAD (указать точный SHA после открытия PR), особое внимание ER-09–ER-18 и P10–P16; отправить результат 00 для решения G1.
Передать: оператор пересылает прямую ссылку на опубликованный адресный GitHub-комментарий с точным PR/HEAD.