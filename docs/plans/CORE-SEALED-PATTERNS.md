# CORE-SEALED + CORE-PATTERNS — исследовательский план G1 (Java SE 21)

**Статус:** `G1 PROPOSED / NOT ACCEPTED`. **Роль:** 01 — Research & Coverage. Основание — [Issue #135](https://github.com/Melchiorius/backend-interview-labs/issues/135) и [поручение 00 #7071316617](https://github.com/Melchiorius/backend-interview-labs/issues/135#issuecomment-7071316617); программа #33. **BASE/main при создании ветки:** `b6eaba9b35e22bb5aaa62e12e5ae426443466ba1` после G6 Records/Immutable (Issue #134); точный PR HEAD фиксируется в GitHub PR/адресном комментарии. **Норма:** Java SE/JLS 21 без preview. **Локальная проба:** OpenJDK `javac 21.0.12.1` и `java 21.0.12.1`, Linux; `javac --release 21`. Содержимое документа — рабочая карта ДО статей, **не публикация G2**.

## 1. Обещание читателю, границы и архитектура

После двух связанных тем кандидат должен объяснить, какие именно **прямые** подтипы допускает `sealed`, как работают `permits` и `final|sealed|non-sealed`, почему «закрытая» иерархия не тождественна неизменяемости или невозможности всех косвенных наследников, а также предсказывать, **компилируется ли** `instanceof`/`switch` с pattern labels, что печатает программа и где возникает `NullPointerException` либо `MatchException`.

**Решение о G2: две самостоятельные содержательные статьи, один совместный runnable только если оправдан обучающей пользой.**
- `CORE-SEALED`: ограничения типа и прямых наследников, право reopen у `non-sealed`, named/unnamed module, implicit permits, связи с records/interfaces. Будущий путь `knowledge/01-java-core/sealed.md` — **НЕ существующая ссылка**.
- `CORE-PATTERNS`: `instanceof` и flow-scoping, `switch` expression/statement, dominance, exhaustiveness, null, `when` guards, record patterns/deconstruction и границы Java 21. Будущий путь `knowledge/01-java-core/patterns.md` — **НЕ существующая ссылка**.
- Один возможный `examples/core/sealed-patterns/` с **двумя отдельно запускаемыми** программами ниже и проверяемым stdout (31 строка суммарно в двух JVM-процессах), а при педагогической необходимости — negative компиляции отдельными `.java`. Создание G2 runnable — только 02/03 после `G1 ACCEPTED` 00. Не считать 31 stdout строкой «31 тестом». Специальное LMS-упражнение не требуется по формальному признаку.

**Не охватываем и не повышаем статус:** `CORE-ENUMS` (отдельная не реализованная тема; здесь только сравнение с sealed exhaustiveness), полную систему `CORE-REFLECTION`, `CORE-SERIAL`, отдельный `CON-JMM`, bytecode/JVMS, hot reload, реальную бинарную эволюцию после separate compilation, модули JPMS как учебный курс, JIT/GC/JMH, framework integration и unnamed patterns / unnamed variables JEP 443 preview (в `--release 21` без preview недоступны). `record patterns` Java 21 *стабильны*, а «примитивные type patterns для switch long» **не** стандарт Java 21.

## 2. Аудит опубликованного main и навигация

После публикации [PR #132](https://github.com/Melchiorius/backend-interview-labs/pull/132) и документарного G6 #134 доступны **23 самостоятельные статьи Java Core, 24 из 42 curricular ID**, что подтверждают актуальные [COVERAGE](../COVERAGE.md) и [CURRICULUM](../CURRICULUM.md). Новые `CORE-SEALED` и `CORE-PATTERNS` **не Implemented**. Уже раскрытые фрагменты нельзя выдавать за самостоятельный полный курс двух тем:

| Действительно существующая статья (проверен путь main) | Что уже есть | Где новая глубина |
|---|---|---|
| [CORE-INHERITANCE](../../knowledge/01-java-core/inheritance.md) | наследование, `final`, overriding, требования подтипов | `sealed` и `non-sealed`, explicit/implicit `permits`, границы прямых наследников |
| [CORE-INTERFACES](../../knowledge/01-java-core/interfaces.md) | interfaces, SAM; запомнить запрет `sealed` functional interface | `sealed` интерфейсы, прямые реализации / подинтерфейсы, reopen |
| [CORE-RECORDS](../../knowledge/01-java-core/records.md) | implicit final records, компоненты, accessors | record patterns, вложенное deconstruction и ошибочные match-ветви |
| [CORE-IMMUTABLE](../../knowledge/01-java-core/immutability.md) | `final` ссылки, состояние, invariants | `sealed` не гарантирует immutable; допустимый mutable `non-sealed` наследник |
| [CORE-CLASSES](../../knowledge/01-java-core/classes.md) | классы, конструкторы, поля | объявление sealed hierarchy и его ограничения |
| [CORE-TYPES](../../knowledge/01-java-core/types.md) | типы, `null`, `instanceof` в общих чертах | pattern variables, scope, null dispatch |
| [CORE-EQUALITY](../../knowledge/01-java-core/equality.md) | identity/value equality | record patterns не означают equality |
| [CORE-GENERICS](../../knowledge/01-java-core/generics.md) | параметры типов и bounds | generic sealed hierarchy; ограничения pattern applicability |
| [CORE-ARRAYS](../../knowledge/01-java-core/arrays.md) | массивы и ковариантность | не подменять record patterns массивными patterns, которых нет |

Дополнительный [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md) указывает `CORE-SEALED` и `CORE-PATTERNS` как будущие тематические ID. [Технический стандарт](../EXERCISE_AND_TESTING_STANDARD.md) не требует отдельного упражнения на каждую статью. Отсутствующую `CORE-ENUMS` упоминать как ID без фиктивной локальной ссылки. Проверка абсолютного scope ссылок: все относительные file targets таблицы и нормативных документов выше существуют в фактическом GitHub main; не заявляется проверка anchor fragments или Obsidian GUI.

## 3. Нормативные и интервью-источники

Приоритет [SOURCE_REGISTRY](../SOURCE_REGISTRY.md): **A** — JLS/API Java 21, **B** — объясняющая литература/JEP, **C** — независимые интервью-вопросы, **не нормативные ответы**.

**A — первичные разделы, сверенные именно для SE21 (не текущая JLS других выпусков):**

| Метка | Версия / конкретный источник | Контракт |
|---|---|---|
| A1 | [JLS 21 `8.1.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.1.2), [`8.1.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.6) | `sealed` class, `final/non-sealed`, модификаторы, класс `permits`, named/unnamed module |
| A2 | [JLS 21 `9.1.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.1.1.4), [`9.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.1.4) | sealed interface, permitted direct class/interface, implicit permits |
| A3 | [JLS 21 `14.11.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.1), [`14.11.1.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.1.1), [`14.11.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.1.2) | switch labels, dominance, exhaustive blocks, null/guard selection |
| A4 | [JLS 21 `14.11.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.2), [`14.11.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.11.3), [`15.28](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.28) | enhanced statement/expression, runtime exceptions |
| A5 | [JLS 21 `14.30.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.30.1), [`14.30.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.30.2), [`14.30.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.30.3) | type/record patterns, component accessors, null matching, unconditional/dominance |
| A6 | [JLS 21 `15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2), [`6.3.1–6.3.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3) | instanceof pattern, flow-sensitive scope, switch bindings |
| A7 | [JLS 21 `8.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10), [`9.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8) | records implicitly final; sealed interface не функциональный даже с одним SAM |
| A8 | [JLS 21 `13](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html), [JEP 409](https://openjdk.org/jeps/409), [JEP 440](https://openjdk.org/jeps/440), [JEP 441](https://openjdk.org/jeps/441) | binary compatibility **только граница и контекст**; реальные separate-compilation эксперименты не выполнены |

Дополнительные официальные объяснения **B** (не подменяют норму): [Java 21 Sealed Classes](https://docs.oracle.com/en/java/javase/21/language/sealed-classes-and-interfaces.html), [Java 21 Pattern Matching for switch](https://docs.oracle.com/en/java/javase/21/language/pattern-matching-switch.html).

**C — реальные независимые вопросы, сверенные с источниками:**
- **C1 / Q-HRUDAY (из существующего реестра):** [Core Java Interview Question Bank, ``20.6–20.7](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md). Проверены конкретные формулировки «Difference between final, sealed, and non-sealed», «What is the permits clause?», «Can interfaces be sealed?», «Pattern Matching for instanceof», «What are guarded patterns?», «What are null patterns?»; **этот банк не является доказательством норм JLS и не измеряет частоту вопросов**.
- **C2 / Q-CHERNYSHOV (из реестра):** [OOP/Composition and Inheritance, вопрос о sealed Java 17+](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/eng/18.%20OOP%20and%20SOLID/10.%20What%20is%20composition%20and%20inheritance.md). Вопрос «Why did Sealed Classes ... fundamentally change ... inheritance?» подтверждён. Источник содержит реализационные/JIT-обобщения, которые **не** принимаем без отдельной проверки.
- **C3 / новый независимый кандидат для реестра:** [Modern Java 17–21 Interview Questions, StackWise](https://tutorials.jitinkayyala.com/interview/modern-java/): «What are sealed classes and why are they useful?»; «Explain pattern matching for switch with an example». Дополняет пробелы банка по Java 21, но не устанавливает нормативный контракт.
- **B дополнительно:** [Baeldung — Sealed Classes and Interfaces](https://www.baeldung.com/java-sealed-classes-interfaces), [Baeldung — Pattern Matching for switch](https://www.baeldung.com/java-switch-pattern-matching), [Pattern matching for instanceof](https://www.baeldung.com/java-pattern-matching-Instanceof). У этих материалов есть объясняющие примеры, но при расхождении приоритет A.

Никаких выводов «на собеседованиях всегда спрашивают» и показателей частотности из трёх банков не делаем. Обновление самого `SOURCE_REGISTRY` целесообразно после решения 00 о расширении scope, **данный G1 PR остаётся однофайловым**.

## 4. Матрица P0 — sealed hierarchy

Каждая строка P0 имеет нормативную опору A, конкретную позитивную метку `Sxx` или изолированную отрицательную `Nxx`, границу и будущий ответ в статье. `Sxx` обозначают строки `SealedProbe`, `N24` — отдельная двухфайловая compile-negative по unnamed module; `M01` — отдельно проверенная компиляция named module. Уровни P0/P1/P2 — **приоритет внутри темы**, не уровни тайтла Middle/Senior.

| ID / P | Вопрос и механизм | A / интервью | Доказательная проба и граница |
|---|---|---|---|
| S-01 P0 | `sealed class` vs `sealed interface`: контроль прямых подтипов, а не всех транзитивных экземпляров | A1,A2 / C1,C2 | `S01,S06`; `N01`; статья CORE-SEALED `1 |
| S-02 P0 | `permits` перечисляет **разрешённые прямые** наследования/реализации | A1,A2 / C1 | `N01,N04,N06,N21`; не путать с косвенным наследником |
| S-03 P0 | Неявный `permits` при canonical name в той же compilation unit | A1,A2 / C1 | `S04,S05` + `AutoPermit`; local/anonymous не выводятся автоматически |
| S-04 P0 | Допустимый direct child должен быть `final|sealed|non-sealed` явно или неявно | A1,A2 / C1 | `N02,N22`; record implicitly final `S08` |
| S-05 P0 | `non-sealed` вновь разрешает косвенным наследникам свободное расширение | A1 / C1,C2 | `S03,S09` и `Later extends Pending`; не «открыть исходный Root» прямому непермитированному классу |
| S-06 P0 | Unnamed module — same package; named module — same module, допускается другой package | A1,A2 / C1 | `N24` и `M01`; не заявлять реальный multi-module runtime |
| S-07 P0 | `final` запрещает всех наследников класса, `sealed` допускает контролируемых прямых | A1 / C1 | `S06,N05`; окончательность/иммутабельность разные свойства |
| S-08 P0 | Record — implicit final и допустимая реализация sealed interface | A1,A2,A7 / C1 | `S01,S08`; enum лишь нормативное сопоставление, не курс CORE-ENUMS |
| S-09 P0 | Sealed generic interface может иметь generic record как permitted child | A2,A7 / C1 | `S10` (GenericResult и GenericOk); inference см. [generics](../../knowledge/01-java-core/generics.md) |
| S-10 P0 | Sealed interface с одним abstract method **не** функциональный интерфейс для lambda | A7 / C1 | `N07`; уже есть вводный SAM в [interfaces](../../knowledge/01-java-core/interfaces.md) |
| S-11 P0 | `sealed` никак не гарантирует неизменяемость состояния объекта | A1,A7 / C2 | `S11=1` после mutable `Pending.increment`; [immutability](../../knowledge/01-java-core/immutability.md) |
| S-12 P0 | `permits` без `sealed`, неверный direct subtype, лишний `non-sealed` запрещены | A1,A2 / C1 | `N03,N04,N20`; не сводить все ошибки к одному сообщению javac |

## 5. Матрица P0 — patterns / switch / record patterns

| ID / P | Вопрос и механизм | A / интервью | Доказательная проба и граница |
|---|---|---|---|
| PM-01 P0 | `instanceof T t` одновременно проверяет тип и связывает переменную без явного cast | A5,A6 / C1 | `P10,P11`, `N16` — недопустимый primitive input |
| PM-02 P0 | Flow scope после `if (!(x instanceof T t)) return` | A6 / C1 | `P10,P11`; не любая последующая строка видит `t` |
| PM-03 P0 | `instanceof &&` позволяет обращаться к binding справа; `||` — нет | A6 / C1 | `P19,N15`; область видимости определяет definite match |
| PM-04 P0 | `switch` expression даёт значение; enhanced switch statement требует exhaustiveness | A3,A4 / C1,C3 | `P14,P15` и `N17`; не переносить требование на старый не-enhanced statement |
| PM-05 P0 | Exhaustive switch по sealed Result без `default` | A3,A4 / C3 | `S01,S02` и `N09`; проверять все разрешённые ветви |
| PM-06 P0 | `non-sealed` не отменяет exhaustiveness, если есть `case Pending p`, покрывающий его потомков | A1,A3,A5 / C2,C3 | `S03` показывает `Later` обработан ветвью Pending; отсутствующая ветвь `N09` |
| PM-07 P0 | Dominance: более широкий unguarded pattern выше делает узкий недостижимым | A3,A5 / C1 | `N08`; порядок ветвей имеет значение |
| PM-08 P0 | `case null` обрабатывает null явно и находится вне обычной проверки типа/record pattern | A3,A4,A5 / C1 | `P01`; `N11` duplicate-null |
| PM-09 P0 | `default` сам по себе не перехватывает null; `case null, default` перехватывает | A3,A4 / C1 | `P16=NPE` и `P18=caught`; различать null selector и null component |
| PM-10 P0 | `when` guard срабатывает **после** type match и позволяет специализированную ветвь до общей | A3,A5 / C1 | `P02,P03,P04,P05`, `N12` — старый preview `&&` синтаксис |
| PM-11 P0 | Гардированная ветвь обычно не даёт полного покрытия типа | A3 / C1 | `N10,N23`, с исключением константного `true`; не объявлять любую guard исчерпывающей |
| PM-12 P0 | `record pattern` проверяет тип record и извлекает компоненты | A5,A7 / C3 | `P07,P08,P09`; это не equals сравнение двух объектов |
| PM-13 P0 | Вложенный record pattern, binding и inferred `var` в деконструкции | A5,A6 / C3 | `P12,P13` + `N13,N14` arity/type failures |
| PM-14 P0 | `null` в record component может не совпадать с nested record pattern | A5 / C3 | `P13` false (instanceof), `P20=MatchException` (exhaustive switch runtime); различать случаи |
| PM-15 P0 | Ошибка accessor во время record deconstruction заворачивается в `MatchException` с cause | A5 / C3 | `P17=MatchException:IllegalStateException`; runtime, не компиляция |
| PM-16 P0 | `MatchException` при exhaustiveness mismatch — не эквивалентен compiler `not.exhaustive` | A3,A4,A5 / C3 | `P20` runtime vs `N09,N23` compile; binary evolution отдельно not run |
| PM-17 P0 | Область/уникальность pattern binding и повторяющиеся case labels | A3,A6 / C1 | `N15,N18,N11`; воспроизвести семантическую ошибку, не EOF |
| PM-18 P0 | Тип selector и Java21 syntax, `long` без preview не поддерживается | A3,A4,A6 / C1,C3 | `N19,N16`; не приписывать Java21 primitive patterns нового выпуска |

## 6. P1 и P2 — точные хвосты, не ложное закрытие

| ID | Приоритет | Что проверить и где граница |
|---|---|---|
| S-13 | P1 | Допустимый `sealed abstract class`, цепочка `sealed -> sealed -> final`; `SealedProbe.State` + JLS A1 |
| S-14 | P1 | Перечисление `permits` с доступностью/ canonical name, невозможность local/anonymous в implicit permits; A1,A2, проверять перед G2 |
| S-15 | P1 | Сопоставление sealed интерфейса и enum: у enum специальные правила implicit final/sealed; отдельный `CORE-ENUMS` **не Implemented** |
| S-16 | P1 | Выбор sealed для domain result vs `final` для запрета расширения; наличие generic hierarchy `S10` |
| S-17 | P1 | Named module cross-package compilation (`M01`) vs полноценная система JPMS; A1,A2, только ограниченный опыт |
| PM-19 | P1 | Различие legacy `switch` statement (не требует exhaustive) vs enhanced (`N17`); A4 |
| PM-20 | P1 | `when` guard с side effects и order selector/guard; JLS A3, полноценный runtime trace отдельно при G2 |
| PM-21 | P1 | `case null, default` combined label и взаимодействие с unconditional type pattern; A3, `P18` |
| PM-22 | P1 | Type vs record patterns, applicability/unconditional и dominance по generic types; A5, нужна адресная проверка G2 |
| PM-23 | P1 | Порядок выбрасывания `MatchException` из accessor и причины; A5, `P17`, не превращать в общее «всегда MatchException» |
| PM-24 | P1 | Различие enum exhaustive switch vs sealed exhaustive switch при изменении программы; A3,A4; не полноценный CORE-ENUMS |
| S-18 | P2 | JVM `IncompatibleClassChangeError` при бинарной эволюции sealed hierarchy: JLS `13`; **not run** отдельный classpath/separate compilation |
| S-19 | P2 | Reflection `getPermittedSubclasses`, modules/JPMS real launch; **not run** |
| S-20 | P2 | JIT devirtualization и измерения производительности: **not run**, никаких оптимизационных гарантий |
| PM-25 | P2 | Separate compilation старого exhaustive switch и нового permitted subtype, runtime `MatchException`: **not run** |
| PM-26 | P2 | Record pattern nested generic inference, wildcard и теория pattern applicability; полнота отдельно |
| PM-27 | P2 | Unnamed patterns/variables JEP 443 preview в JDK21 и primitive switch preview следующих версий: **вне Java 21 standard** |
| PM-28 | P2 | Реальный JPMS/JVM-classfile/JMH/GC/JIT/разные JDK+OS, метрики, перегрузки фреймворков: **not run** |

**Итого матрица:** **30 P0 (S-01..S-12, PM-01..PM-18), 11 P1 (5+6), 7 P2 (3+4)**. Для каждого P0 обозначен конкретный JLS/API источник и positive/negative. Во время G4 нужно повторно пройти её **строка → место статьи → корректный пример → существенный контрпример**, а не считать этот план доказанной completeness статей.

## 7. Проверенные positive full-source программы (только в G1 Markdown)

**Инструмент и команды реально выполнены** в чистом временном каталоге, не в рабочем Gradle-проекте:

~~~sh
java -version
javac -version
javac --release 21 -Xlint:all SealedProbe.java PatternsProbe.java
java SealedProbe
java PatternsProbe
~~~

`javac --release 21 -Xlint:all` вернул exit 0 и не выдал warnings. Вывод двух процессов ниже проверен по строкам. Файлы Java как артефакты репозитория **не** созданы.

### 7.1 SealedProbe.java — 11 строк stdout

~~~java
public class SealedProbe {
    sealed interface Result permits Ok, Err, Pending {}
    record Ok(int value) implements Result {}
    record Err(String message) implements Result {}
    non-sealed static class Pending implements Result {
        int count;
        void increment() { count++; }
    }
    static final class Later extends Pending {}

    sealed interface AutoPermit {}
    record Token(String text) implements AutoPermit {}
    final static class Marker implements AutoPermit {}

    sealed abstract static class State permits Ready, Stopped {}
    static final class Ready extends State {}
    static final class Stopped extends State {}

    sealed interface GenericResult<T> permits GenericOk {}
    record GenericOk<T>(T value) implements GenericResult<T> {}

    static String describe(Result result) {
        return switch (result) {
            case Ok(int value) -> "ok:" + value;
            case Err(String message) -> "err:" + message;
            case Pending p -> "pending";
        };
    }

    static String implicit(AutoPermit value) {
        return switch (value) {
            case Token(String text) -> "token:" + text;
            case Marker marker -> "marker";
        };
    }

    public static void main(String[] args) {
        System.out.println("S01=" + describe(new Ok(67)));
        System.out.println("S02=" + describe(new Err("bad")));
        System.out.println("S03=" + describe(new Later()));
        System.out.println("S04=" + implicit(new Token("A")));
        System.out.println("S05=" + implicit(new Marker()));
        System.out.println("S06=" + (new Ready() instanceof State));
        System.out.println("S07=" + Result.class.isSealed());
        System.out.println("S08=" + (new Ok(1) instanceof java.lang.Record));
        System.out.println("S09=" + (new Later() instanceof Pending));
        GenericResult<String> g = new GenericOk<>("generic");
        System.out.println("S10=" + ((GenericOk<?>) g).value());
        Pending mutable = new Pending();
        mutable.increment();
        System.out.println("S11=" + mutable.count);
    }
}
~~~

**Реальный stdout OpenJDK 21.0.12.1, 11 строк:**

~~~text
S01=ok:67
S02=err:bad
S03=pending
S04=token:A
S05=marker
S06=true
S07=true
S08=true
S09=true
S10=generic
S11=1
~~~

`S03` существенен: `Later` не является прямым разрешённым подтипом `Result`, но подпадает под `case Pending p` как наследник `non-sealed Pending`. `S11` показывает, что sealed-декларация не запрещает изменение поля.

### 7.2 PatternsProbe.java — 20 строк stdout

~~~java
public class PatternsProbe {
    record Point(int x, int y) {}
    record Envelope(Point point) {}
    sealed interface Answer permits Yes, No {}
    record Yes(Point point) implements Answer {}
    record No(String cause) implements Answer {}

    static String classify(Object input) {
        return switch (input) {
            case null -> "null";
            case String s when s.length() > 3 -> "long:" + s;
            case String s -> "short:" + s;
            case Integer n when n > 0 -> "positive:" + n;
            case Integer n -> "nonpositive:" + n;
            default -> "other";
        };
    }

    static String answer(Answer input) {
        return switch (input) {
            case Yes(Point(int x, int y)) when x > 0 -> "yes+:" + (x + y);
            case Yes(Point(int x, int y)) -> "yes:" + (x + y);
            case No(String cause) -> "no:" + cause;
        };
    }

    static String flow(Object input) {
        if (!(input instanceof String s)) return "not-string";
        return "string:" + s.length();
    }

    static String deconstruct(Object input) {
        if (input instanceof Envelope(Point(var x, var y)))
            return "point:" + (x + y);
        return "not-point";
    }

    static String countStatement(Object input) {
        StringBuilder b = new StringBuilder();
        switch (input) {
            case null -> b.append("null");
            case Integer n when n > 0 -> b.append("positive");
            case Integer n -> b.append("nonpositive");
            default -> b.append("other");
        }
        return b.toString();
    }

    static String nullOrDefault(Object value) {
        return switch (value) { case null, default -> "caught"; };
    }

    static boolean flowAnd(Object value) {
        return value instanceof String s && s.length() > 3;
    }

    static String failRecordAccessor(Object value) {
        return switch(value) {
            case Broken(int n) -> "ok:" + n;
            default -> "other";
        };
    }

    record Broken(int number) {
        @Override public int number() {
            throw new IllegalStateException("broken-accessor");
        }
    }

    public static void main(String[] args) {
        System.out.println("P01=" + classify(null));
        System.out.println("P02=" + classify("abcd"));
        System.out.println("P03=" + classify("ab"));
        System.out.println("P04=" + classify(67));
        System.out.println("P05=" + classify(-1));
        System.out.println("P06=" + classify(2.5));
        System.out.println("P07=" + answer(new Yes(new Point(4, 5))));
        System.out.println("P08=" + answer(new Yes(new Point(0, 5))));
        System.out.println("P09=" + answer(new No("down")));
        System.out.println("P10=" + flow("seven"));
        System.out.println("P11=" + flow(67));
        System.out.println("P12=" + deconstruct(new Envelope(new Point(6, 7))));
        System.out.println("P13=" + deconstruct(new Envelope(null)));
        System.out.println("P14=" + countStatement(67));
        System.out.println("P15=" + countStatement(null));
        try {
            System.out.println("P16=" + switch ((Object) null) {
                case String s -> s;
                default -> "default";
            });
        } catch (NullPointerException ex) {
            System.out.println("P16=NPE");
        }
        try { failRecordAccessor(new Broken(1)); }
        catch (MatchException ex) {
            System.out.println("P17=MatchException:" +
                ex.getCause().getClass().getSimpleName());
        }
        System.out.println("P18=" + nullOrDefault(null));
        System.out.println("P19=" + flowAnd("abcde"));
        try { answer(new Yes(null)); }
        catch (MatchException ex) {
            System.out.println("P20=MatchException");
        }
    }
}
~~~

**Реальный stdout OpenJDK 21.0.12.1, 20 строк:**

~~~text
P01=null
P02=long:abcd
P03=short:ab
P04=positive:67
P05=nonpositive:-1
P06=other
P07=yes+:9
P08=yes:5
P09=no:down
P10=string:5
P11=not-string
P12=point:13
P13=not-point
P14=positive
P15=null
P16=NPE
P17=MatchException:IllegalStateException
P18=caught
P19=true
P20=MatchException
~~~

**Самые важные различия:** `P16` — `default` при selector=null не отменяет NPE; `P18` — отдельная combined метка `case null, default`. `P13`: при `instanceof` с вложенной record pattern ссылка на `Point` равная null просто не совпадает. `P20`: исчерпывающий по типу `Answer` `switch` реально получает `Yes(null)`, ни одна record pattern не срабатывает, поэтому `MatchException` — **не compiler error**. `P17`: accessor `Broken.number()` бросает `IllegalStateException`, pattern matching выбрасывает `MatchException` с исходной причиной, как описано в JLS 21 `14.30.2. Не говорить, что всякий null компонента обязан выдавать `MatchException` независимо от контекста.

## 8. Изолированные compile-negative

**Метод проверки:** каждый `Nxx` сохраняется в отдельный файл `Nxx_Name.java` с **полным приведённым source**, затем выполняется `javac --release 21 -XDrawDiagnostics Nxx_Name.java` в изолированном каталоге; необходимы ненулевой статус и **именно тематический diagnostic key**. В таблице source с несколькими top-level package-private типами остаётся самостоятельной полной compilation unit. **Не склеивать** строки в один файл. В `N05` возможны две закономерные diagnostics, обе относятся к одному запрещённому сочетанию модификаторов. `N12` — **намеренная** проверка недопустимого в Java21 старого preview `case ... && ...` на уровне grammar, а не случайный неполный файл. N24 — отдельный двухфайловый package-specific набор. Эти пробы **не входят в CI** проекта и не являются JUnit.

| № / файл | Полный исходник самостоятельного файла (строки разделены `⏎`) | Фактическая причина / диагностический ключ javac 21.0.12.1 |
|---|---|---|
| 01 / `N01_IllegalDirect.java` | `sealed interface Root01 permits Allowed01 {} ⏎ final class Allowed01 implements Root01 {} ⏎ final class N01_IllegalDirect implements Root01 {}` | `compiler.err.cant.inherit.from.sealed` |
| 02 / `N02_MissingModifier.java` | `sealed interface Root02 permits N02_MissingModifier {} ⏎ class N02_MissingModifier implements Root02 {}` | `compiler.err.non.sealed.sealed.or.final.expected` |
| 03 / `N03_NonSealedNoParent.java` | `non-sealed class N03_NonSealedNoParent {}` | `compiler.err.non.sealed.with.no.sealed.supertype` |
| 04 / `N04_PermitsNotDirect.java` | `sealed interface Root04 permits Allowed04, N04_PermitsNotDirect {} ⏎ non-sealed class Allowed04 implements Root04 {} ⏎ final class N04_PermitsNotDirect extends Allowed04 {}` | `compiler.err.invalid.permits.clause` / `doesnt.extend.sealed` |
| 05 / `N05_SealedFinal.java` | `sealed final class N05_SealedFinal permits Child05 {} ⏎ final class Child05 extends N05_SealedFinal {}` | `compiler.err.illegal.combination.of.modifiers` (и ожидаемая вторичная `cant.inherit.from.final`) |
| 06 / `N06_ExtraChild.java` | `sealed class Root06 permits Allowed06 {} ⏎ final class Allowed06 extends Root06 {} ⏎ final class N06_ExtraChild extends Root06 {}` | `compiler.err.cant.inherit.from.sealed` |
| 07 / `N07_SealedLambda.java` | `sealed interface Root07 permits Impl07 { void f(); } ⏎ final class Impl07 implements Root07 {public void f(){}} ⏎ class N07_SealedLambda { Root07 value = () -> {}; }` | `compiler.misc.not.a.functional.intf` |
| 08 / `N08_Dominated.java` | `class N08_Dominated { static int f(Object v) { return switch(v) {case Object o -> 1; case String s -> 2;};} }` | `compiler.err.pattern.dominated` |
| 09 / `N09_NonExhaustive.java` | `sealed interface Root09 permits Good09,Bad09 {} ⏎ record Good09(int n) implements Root09 {} ⏎ record Bad09(String s) implements Root09 {} ⏎ class N09_NonExhaustive { static int f(Root09 v) {return switch(v) {case Good09 g -> 1;};} }` | `compiler.err.not.exhaustive` |
| 10 / `N10_GuardOnly.java` | `class N10_GuardOnly { static int f(Object v) {return switch(v) {case String s when s.length() > 0 -> 1;};} }` | `compiler.err.not.exhaustive` |
| 11 / `N11_DuplicateNull.java` | `class N11_DuplicateNull { static int f(Object v) {return switch(v) {case null -> 1; case null -> 2; default -> 3;};} }` | `compiler.err.duplicate.case.label` |
| 12 / `N12_OldGuardSyntax.java` | `class N12_OldGuardSyntax { static int f(Object v) {return switch(v) {case String s && s.length() > 0 -> 1; default -> 0;};} }` | `compiler.err.expected2: :, ->` и parser diagnostics **точно на устаревшем `&&`**; синтаксический контрпример |
| 13 / `N13_RecordArity.java` | `record Pair13(int a,int b) {} ⏎ class N13_RecordArity { static int f(Object v) {return switch(v) {case Pair13(int a) -> a; default -> 0;};} }` | `compiler.err.incorrect.number.of.nested.patterns` |
| 14 / `N14_RecordComponentType.java` | `record Pair14(int a,int b) {} ⏎ class N14_RecordComponentType { static int f(Object v) {return switch(v) {case Pair14(String a,int b) -> 1; default -> 0;};} }` | `compiler.misc.inconvertible.types: int, String` |
| 15 / `N15_FlowOr.java` | `class N15_FlowOr { static boolean f(Object v) {return v instanceof String s || s.length() > 0;} }` | `compiler.err.cant.resolve.location` (переменная `s` вне scope) |
| 16 / `N16_PrimitiveInstanceof.java` | `class N16_PrimitiveInstanceof { static boolean f(int v) {return v instanceof String s;} }` | `compiler.err.type.found.req` / required reference |
| 17 / `N17_EnhancedStatement.java` | `class N17_EnhancedStatement { static void f(Object v) {switch(v) {case String s -> System.out.println(s);}} }` | `compiler.err.not.exhaustive.statement` |
| 18 / `N18_DuplicateBinding.java` | `record Pair18(int a,int b) {} ⏎ class N18_DuplicateBinding { static int f(Object v) {return switch(v) {case Pair18(int x,int x) -> x; default -> 0;};} }` | `compiler.err.match.binding.exists` |
| 19 / `N19_LongSwitch.java` | `class N19_LongSwitch { static int f(long v) {return switch(v) {case 67L -> 1; default -> 0;};} }` | `compiler.err.selector.type.not.allowed: long` |
| 20 / `N20_PermitsWithoutSealed.java` | `class N20_PermitsWithoutSealed permits Good20 {} ⏎ final class Good20 extends N20_PermitsWithoutSealed {}` | `compiler.err.invalid.permits.clause` / class is not sealed |
| 21 / `N21_UnlistedInterface.java` | `sealed interface Root21 permits Good21 {} ⏎ final class Good21 implements Root21 {} ⏎ class N21_UnlistedInterface implements Root21 {}` | `compiler.err.cant.inherit.from.sealed` |
| 22 / `N22_NonSealedFurtherChild.java` | `sealed interface Root22 permits N22_NonSealedFurtherChild {} ⏎ class N22_NonSealedFurtherChild implements Root22 { }` | `compiler.err.non.sealed.sealed.or.final.expected` |
| 23 / `N23_OnlyGuardedSealed.java` | `sealed interface Root23 permits Good23, Bad23 {} ⏎ record Good23(int n) implements Root23 {} ⏎ record Bad23(String s) implements Root23 {} ⏎ class N23_OnlyGuardedSealed { static int f(Root23 v) {return switch(v) {case Good23 g when g.n()>0 -> 1; case Bad23 b -> 2;};} }` | `compiler.err.not.exhaustive` |

**N01–N23 реально исполнены:** `23/23` отдельные invocations `javac --release 21 -XDrawDiagnostics` завершились ненулевым exit code и выдали показанные ключи. Диагностическая орфография/позиции — **деталь версии javac**, а не текстовая гарантия JLS. Ошибочный первоначальный кандидат `String value; value instanceof String s` **не** включён как «обязательная ошибка»: на используемом JDK 21 эта запись **компилируется**, поэтому заменена валидным контрпримером `N16` с primitive `int`. Нельзя подгонять утверждения под старые ответы интервью-банков.

**N24: запрет разных packages внутри *unnamed module* (полные два файла, отдельно от N01–N23).** Структура:

~~~text
p/Root.java
q/Bad.java
~~~

`p/Root.java`:
~~~java
package p;
public sealed interface Root permits q.Bad {}
~~~

`q/Bad.java`:
~~~java
package q;
public final class Bad implements p.Root {}
~~~

В каталоге, содержащем `p/` и `q/`, реально выполнено:

~~~sh
javac --release 21 -XDrawDiagnostics p/Root.java q/Bad.java
~~~

Результат: **exit != 0**, `compiler.err.class.in.unnamed.module.cant.extend.sealed.in.diff.package: p.Root`; это именно ограничение unnamed module, не отсутствие класса.

**M01: позитивный контраст — те же разные packages в одном *named module* (отдельный набор 3 файла).** `module-info.java`:

~~~java
module sealed.pilot { exports p; }
~~~

`p/Root.java`:
~~~java
package p;
public sealed interface Root permits q.Good {}
~~~

`q/Good.java`:
~~~java
package q;
public final class Good implements p.Root {}
~~~

Команда `javac --release 21 -Xlint:all -d out module-info.java p/Root.java q/Good.java` **реально прошла exit 0 без предупреждений** на JDK 21.0.12.1. **Не запускались** отдельная JVM с `--module-path`, взаимодействие разных named modules, reflection APIs или hot reload; проверен только компиляционный контракт совместного module.

Таким образом, **24/24 negative cases** (N01–N23 и N24) и **1 дополнительный named-module positive** получили локальные фактические результаты. Никаких недоказанных «все будущие реализации JVM ведут себя как этот локальный javac» не заявляем.

## 9. Подготовка G2 и чек-лист G3/G4

**Для роли 02:** связный текст на русском, объясняющий действующих лиц (selector expression, matching pattern, accessor, compiler), затем один конкретный кодовый trace на каждый P0; явная маркировка `compile error` / `runtime exception` / `непредсказанное поведением будущей сборки`. Для `switch` заранее объяснить, что означает «exhaustive» на основании permits, а не использовать слово как синоним «есть default». Не смешивать «record pattern не matched при null» и «null selector»: случаи ``P13, P16, P20` отвечают на **разные** вопросы. Не обещать, что `non-sealed` ломает любой исчерпывающий switch: конкретный `case Pending p` покрывает потомков; наоборот, один `case Ok` при наличии `Err/Pending` не exhaustive.

**Для роли 03:** при необходимости одного общего runnable две автономные Java-программы/два stdout эталона `11+20`; сохранять `P17/P20` как объясняемые runtime failures, а `N01–N24` как **отдельный** источник negative diagnostics. Если negative включаются в проверку, каждый должен компилироваться из своего собственного полного файла с ожидаемым category+exit и без загрязнения успешного общего javac. Не создавать ритуальный Gradle подпроект/тест только ради счётчика.

**Для 05/04:** проверить естественный русский, спецификацию Java 21 без включённого preview, все исходные JLS ссылки; контрпримеры ``N08,N09,N10,N15,N17,N23,N24`; `P17` и `P20`; существование всех локальных относительных ссылок. Результат G4 соотносить с интегрированным exact HEAD. Доказательства G1 local probes **не** подменяют позднейшую CI/рецензию G2. Обновление `COVERAGE/CURRICULUM` строго после G5/G6.

**Не проверено и не заявляется как успешное:** проектная Gradle/JUnit/CI-сборка Java-кода внутри G1 Markdown (действующая CI не компилирует fenced code автоматически); разные JDK vendors/versions и OS; реальные Obsidian/IntelliJ клики и Mermaid preview; полная семантика JPMS/совместная загрузка модулей; separate compilation/binary evolution (`IncompatibleClassChangeError`, не выполнена); общий тестовый стенд Java/JMH/JIT/GC/системы Reflection; исчерпывающий курс CORE-ENUMS и любые preview Java 22+.

## 10. Промежуточный вердикт G1 и передача

Исследовательский результат: **G1 PROPOSED, NOT ACCEPTED**; рекомендованы **две самостоятельные статьи** и **один оправданный runnable**. Есть **30 P0 / 11 P1 / 7 P2**, два Java 21 full-source с реально измеренными **31 stdout строкой**, **23 изолированных отдельных negative .java**, плюс **N24** межпакетный unnamed-module negative и **M01** named-module compilation positive. В этом PR **только** данный Markdown. Отсутствует заявление, что G2 либо весь Java Core завершён.

### Передача эстафеты
Кому: **04 — Independent Reviewer**.
Что сделать: по **точному HEAD исследовательского PR** провести независимое G1-review JLS Java21, каждого P0/контрпримера, `non-sealed` reopen и sealed exhaustiveness, `case null`/guard/dominance, `P17/P20` vs compiler negatives, package/module restrictions, source-level limits и соответствия примеров stdout/diagnostics. Вынести `PASS` или `CHANGES` с конкретным блокером и требуемым исправлением; при изменении HEAD провести повторное review. После PASS передать **00** для `G1 ACCEPTED/BLOCKED` и решения о merge; **до решения 00 не начинать G2**.
Передать: оператор пересылает **прямую ссылку на отдельно опубликованный адресный GitHub-комментарий PR** с точным полным HEAD SHA, не общий URL PR и не воображаемую ссылку.
