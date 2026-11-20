# CORE-INTERFACES + CORE-COMPOSITION — исследовательская карта Java SE 21 (G1)

**Статус:** исследование роли 01, **G1 ACCEPTED** решением 00 в [Issue #64](https://github.com/Melchiorius/backend-interview-labs/issues/64#issuecomment-7049387322) после независимого [review 04 #5998196425](https://github.com/Melchiorius/backend-interview-labs/pull/66#pullrequestreview-5998196425) по HEAD `274ced773bee55d9ecdc89924e0e8b370cc815ea`. [PR #66](https://github.com/Melchiorius/backend-interview-labs/pull/66) опубликован в `main` squash SHA `9fa271902ff6a3b9fb487f85e39d8682e8304113`. **Это только принятая исследовательская постановка:** статьи CORE-INTERFACES и CORE-COMPOSITION и их будущий runnable **ещё не Implemented/Verified**, G2/G3/G4/G5 не объявляются пройденными. **Issue:** [#64](https://github.com/Melchiorius/backend-interview-labs/issues/64), родитель [#33](https://github.com/Melchiorius/backend-interview-labs/issues/33). **Исходный BASE `main`:** `fb8cf28604260fb5453f82f07317ddb6557c6442` (20.11.2026, исторический). **Локальный JDK автора 01:** `openjdk 21.0.12.1` / `javac 21.0.12.1`, `javac --release 21`. Точный исходный HEAD G1 приведён выше; G1-проверки 01 не подменяют будущий G2. Актуальные статусы есть в [COVERAGE](../COVERAGE.md).

## 1. Главный вопрос, обещание, решение об архитектуре

**Вопрос читателя:** чем интерфейс отличается от абстрактного класса и как Java 21 выбирает реализацию при наследовании нескольких интерфейсов? Когда лучше наследовать поведение, а когда хранить объект и делегировать ему работу — и какие дополнительные обязательства возникают при этом выборе?

**Рекомендация роли 01: две самостоятельные связанные статьи, один общий исследовательский план и один обоснованный интеграционный runnable example (при согласовании 02/03).**

1. **`CORE-INTERFACES`** — **нормативная** семантика: интерфейс как тип и контракт; `extends`/`implements`; abstract/default/static/private методы и public static final поля; inheritance и разрешение conflicts, `Interface.super`; class wins, abstract/default interaction; `Object`-methods; SAM/marker как обзор; отличие от abstract class. Читатель способен предсказать компиляцию и output. **Не** писать большой урок DI и шаблонов проектирования.
2. **`CORE-COMPOSITION`** — **архитектурное решение**: объект хранит другой объект, явно передаёт операции (delegation), стратегия за интерфейсом, constructor injection, заменяемость зависимостей, стоимость forwarding/lifecycle, инварианты и LSP/IS-A/HAS-A как эвристики. Java-контракт определяет, что код **делает**, но не доказывает, что дизайн всегда лучше. **Не** повторять JLS-правила `default` и `Interface.super`: привести минимальный договор-тип и ссылку на первую статью.
3. **Общий мост:** один предметный сценарий `Policy`/`Service`/`Decorator` демонстрирует API интерфейса и передачу ответственности, **без дублирования** длинных примеров. Возможен один multi-file example с двумя независимыми entry points, либо короткие inline примеры, если роль 03 покажет, что отдельный модуль не оправдан. Не вводить обязательное упражнение/Mockito ради галочки.
4. **Навигация:** после фактической публикации создать работающие двусторонние ссылки между двумя статьями, а не дважды объяснять одну и ту же механику. Предлагаемый порядок `CORE-CLASSES → CORE-ACCESS → CORE-INHERITANCE → CORE-INTERFACES → CORE-COMPOSITION`. Отсутствующие публикации остаются ID без Markdown-ссылок.

**Граница доверия:** JLS 21 регулирует синтаксис, типы, доступность, поиск метода, overriding/default resolution, инициализацию. Формулировки «всегда предпочитай композицию», «интерфейсы автоматически обеспечивают слабую связанность», «DI без Spring всегда лучше» — *не* нормы JLS; применимость зависит от контрактов, частоты изменений, требований к идентичности, жизненному циклу, производительности и эксплуатации. LSP/принципы SOLID — критерии дизайна, а не правило компилятора.

## 2. Нормативные источники A (Java SE 21, конкретные разделы)

Используются записи `OFF-JLS21` и `OFF-API21` из [реестра](../SOURCE_REGISTRY.md); версии зафиксированы. Нормативный статус имеет первоисточник, не сторонний ответ на собеседовании.

- **[JLS 21 §9.1, §9.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.1.3)** — `interface` как reference type, `extends` нескольких superinterfaces, циклы запрещены; обзор `sealed` **вне** P0.
- **[JLS 21 §9.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.2)** — члены интерфейса и отношение к публичным методам `Object`.
- **[JLS 21 §9.3, §9.3.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.3)** — поля интерфейса неявно `public static final`, обязательная инициализация, различие final-ссылки и изменяемого содержимого; отсутствие instance-state/constructor.
- **[JLS 21 §9.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4)** — `public abstract` по умолчанию у обычных abstract методов, тела `default`, `static`, `private` (включая `private static`); несовместимые комбинации; `default` не может override-equivalent не-private метод `Object`.
- **[JLS 21 §9.4.1.1–1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4.1.3)** — overriding/наследование, конфликт унаследованных `default/default` и `abstract/default` с override-equivalent сигнатурами, совместимость методов.
- **[JLS 21 §9.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.5)** — member types внутри интерфейса неявно `public static`, нельзя объявлять `private/protected`.
- **[JLS 21 §9.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8)** — functional interface: interface, **не объявленный `sealed`**, с одной логической abstract-операцией. Несколько унаследованных abstract-методов с override-equivalent signatures могут логически представлять одну операцию. При подсчёте не учитываются public instance methods `Object`; методы `default`, `static` и `private` не добавляют abstract-операций. Sealed interface с одной abstract-операцией не является functional interface и не может быть target type для lambda или method reference. `@FunctionalInterface` проверяет этот контракт, но не создаёт его. Особые generics/erasure оставить CORE-GENERICS/CORE-FUNCTIONS.
- **[JLS 21 §8.1.1.1, §8.1.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.5)** — abstract class с состоянием/конструкторами, один superclass и несколько superinterfaces через `implements`, обязательство конкретного класса реализовать abstract methods.
- **[JLS 21 §8.4.8, §8.4.8.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.4)** — наследование методов классов и интерфейсов, class-method precedence, abstract override и требование согласовать return/throws/access.
- **[JLS 21 §15.12.1–4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12)** — compile-time method selection, особая форма `InterfaceName.super.m()` (только подходящая непосредственная superinterface, без redundant-пути), runtime invocation. Не объяснять всё фразой «самый правый интерфейс».
- **[JLS 21 §12.4.1–2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.1)** — инициализация superinterfaces, объявляющих default methods (включая компилятором созданные synthetic default), distinct from direct interface initialization. Полная модель уже в CORE-INIT.
- **[Java SE 21 `Runnable`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runnable.html), [`java.util.Comparator`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html), [`java.util.function.Function`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/Function.html), [`Objects.requireNonNull`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html#requireNonNull(java.lang.Object))** — конкретные стандартные интерфейсы и проверка обязательной зависимости; интерфейсный API не является нормой «всегда constructor injection».

**Точность формулировок:** (а) `default`-метод интерфейса — не «дополнительная static функция», он участвует в наследовании и dispatch, но имеет конфликты; (б) `static` методы интерфейса не наследуются реализующим классом; (в) `private` методы разрешены внутри интерфейса, но не образуют публичный API; (г) наличие default method не означает автоматическое решение diamond; (д) абстрактный метод *класса* может препятствовать выбору default-метода интерфейса, см. N03; (е) интерфейс не хранит instance fields, но `public static final int[]` допускает изменение элементов массива.

## 3. Независимые вопросы C — не авторитет JLS

Все источники уже перечислены в [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), поэтому не создавать повторные записи. Фактически просмотрены файлы/списки вопросов GitHub:
- **`Q-IN28`** — [JavaInterviewQuestionsAndAnswers](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/readme.md): вопросы 36–46 о multiple inheritance, interface, extends, abstract class/interface; №216+ как мост с functional interface. Упрощения версии Java 8 не принимать без JLS 21 сверки.
- **`Q-HRUDAY`** — [Core Java Interview Question Bank](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md): вопросы про default/private, class-vs-interface, `composition vs inheritance`, marker/SAM; проверены наличие и темы вопросов, а не истинность каждого авторского ответа.
- **`Q-CHERNYSHOV`** — [русский список Java Backend Q&A](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/ru/README.md): §18 OOP/SOLID, вопросы 10–12 — composition, inheritance, delegation; §18 вопросы 7, 16 — интерфейсная сегрегация и DI/DIP. Это про дизайн, не компиляционные гарантии.
- **`Q-TEAMLEAD`** — [java-interview-questions](https://github.com/teamlead/java-interview-questions): interface vs abstract class, multiple inheritance, modifiers интерфейса, `FunctionalInterface`, применение SOLID и композиции. Авторская частотность не валидирована, уровни P0/P1/P2 из неё не выводятся.

**Не подтверждено:** репрезентативность этих четырёх банков для всех интервью, статистика частоты, независимость происхождения каждого вопроса. Условия JLS/Java API не выводятся из согласия нескольких интервью-ответов. Перед G4 повторить аудит уже готовых статей по оставшейся C-подборке (например, Q-BAELDUNG-CORE) и зафиксировать новые gaps, если есть.

## 4. Матрица вопросов и ожидаемых ответов до текста

`P0` — обязательная полнота обещания; `P1` — важные ограничения/углубления; `P2` — специальные границы, не уровень Junior/Middle/Senior. Метки `P01–P16` — 16 проверенных строк stdout §5.1; `N01–N19` — 19 раздельных ожидаемых ошибок компиляции §5.2.

| ID / приоритет | Вопрос и ожидаемая смысловая гарантия/рекомендация | Норма/источник | Контрпример / будущий раздел |
|---|---|---|---|
| IC-01 **P0** | Что такое интерфейс в Java 21? Это тип и контракт, а не «класс без реализации»; интерфейс может содержать default/static/private methods. | §9.1, §9.4 | P01/P05; «Тип и контракт», CORE-INTERFACES |
| IC-02 **P0** | Чем `extends` отличается от `implements`? Интерфейс может наследовать несколько интерфейсов, класс — расширять только один superclass и реализовывать несколько interfaces. | §8.1.5, §9.1.3 | P01/P12, N07; CORE-INHERITANCE — не дублировать |
| IC-03 **P0** | Что означает неявный `public abstract`, какие исключения? Abstract interface method не превращается в package-private; default/static/private имеют тело; private и public — разные ограничения. | §9.4, §9.4.3 | P05/P06, N05/N14/N16/N19; CORE-INTERFACES |
| IC-04 **P0** | Есть ли у интерфейса поля экземпляра? Нет; поля неявно `public static final`, их надо инициализировать. `final`-ссылка на массив не делает массив неизменяемым. | §9.3 | P07/P08, N06/N15/N18; интерфейсные константы |
| IC-05 **P0** | Как вызывается экземплярный метод через интерфейсную ссылку? Статический тип определяет допустимые сигнатуры, объект — переопределяющую реализацию. | §15.12, §8.4.8 | P01; дать ссылку на CORE-INHERITANCE |
| IC-06 **P0** | Кто выигрывает при совпадении унаследованного class method и interface default? Конкретный class method имеет приоритет; **abstract method в superclass** также требует собственной реализации, не «заменяется» default. | §8.4.8.4 | P02, N03; «Class precedence» |
| IC-07 **P0** | Что происходит с двумя конкурирующими default-методами из независимых interfaces? Требуется явное разрешение override; наследование двух путей к одному и тому же default не равно независимому конфликту. | §9.4.1.3, §8.4.8.4 | P03, N01; «Diamond» |
| IC-08 **P0** | А если из независимых интерфейсов пришли abstract и default той же сигнатуры? Нельзя автоматически отдать победу default; реализующий concrete class должен разрешить конфликт. | §9.4.1.3, §8.4.8.4 | N02 и P13; «Abstract/default» |
| IC-09 **P0** | Что если более специфический subinterface переопределяет default родителя? При наследовании учитывается более специфическое объявление; не выбирать по порядку `implements`. | §9.4.1, §8.4.8 | P04, N12 |
| IC-10 **P0** | Что делает `InterfaceName.super.m()`? Явное обращение к default реализации допустимой прямой superinterface; нельзя ссылаться на произвольного предка или на redundant interface. | §15.12.1 | P03, N11/N12 |
| IC-11 **P0** | Можно ли объявить default `toString/equals` в interface? Нет: override-equivalent с non-private `Object`-method; при этом abstract `equals(Object)` сам по себе разрешён. | §9.4.1.2, §9.8 | P10 (abstract Object-method), N04/N17 |
| IC-12 **P0** | Чем interface отличается от abstract class? У abstract class допустимы instance state/constructor/protected/template method, но superclass только один; интерфейсное наследование множественное. | §8.1.1.1, §8.1.5, §9.3 | P09, N03, N07; CORE-INTERFACES |
| IC-13 **P0** | Почему functional interface должен быть не объявлен `sealed` и иметь одну логическую abstract-операцию? При подсчёте не учитываются public instance methods `Object`, а методы `default`, `static` и `private` не добавляют abstract-операций. Sealed interface с одной abstract-операцией не является functional interface и не может быть SAM target для lambda или method reference. `@FunctionalInterface` проверяет контракт, но не создаёт его. | §9.8 | P10/P16, N13; CORE-FUNCTIONS подробности |
| IC-14 **P0** | Что такое composition и delegation в коде? Сервис **содержит** `Policy` и передаёт выполнение, не наследует реализацию; это проектный выбор, а не наследственная иерархия. | Java-типизация §8.1.5; дизайн C | P14, 2 разных Policy; CORE-COMPOSITION |
| IC-15 **P0** | Что даёт constructor injection? Явные зависимости, выбор реализации при создании и контроль `null`/lifetime; не обещать автоматическую потокобезопасность и заменяемость без соблюдения контракта. | API Objects.requireNonNull; дизайн | P14; сценарий замены зависимости |
| IC-16 **P0** | Как decorator/delegation меняют поведение без extends конкретной реализации? Wrapper хранит delegate, может добавить поведение; нужно осознанно определять forwarding и контракты. | Дизайн, не JLS-норма | P15; «Цена делегирования» |
| IC-17 **P1** | Что скрыто за `private` и `static` interface members? Static method нужно вызвать через имя интерфейса, private helper только внутри; класс их не наследует. | §9.4, §15.12 | P05/P06, N09/N10 |
| IC-18 **P1** | Могут ли интерфейсы быть без методов (marker), а обычный класс реализовать несколько одинаковых abstract-сигнатур одной реализацией? Да при совместимости. | §9.4.1, §9.8 | P11/P12; не вводить marker как обязательную рекомендацию |
| IC-19 **P1** | Чем interface default отличается от runtime overriding и compile-time method selection? Привязка «всегда вызывается первый/последний default» неверна. | §15.12, §8.4.8 | P02/P04; ссылка CORE-INHERITANCE |
| IC-20 **P1** | Как interface initialization связано с default и static constants? При инициализации class учитываются superinterfaces с default methods; compile-time constants не равны active initialization. | §12.4.1–2 | Граница CORE-INIT; не повторять большой протокол |
| IC-21 **P1** | Когда важны return-type compatibility, checked throws и abstract redeclaration для interface methods? Override обязан соблюдать совместимость; не всякий diamond разрешается одинаковым именем. | §8.4.8.3/4, §9.4.1 | P13, N02; отдельный P1 контрпример при G2 |
| IC-22 **P1** | Какие затраты у композиции? Boilerplate forwarding, ответственность за lifecycle/identity/exception propagation, возможные нарушения invariants; не считать wrapper автоматически substitutable. | Дизайн, C | P15; таблица trade-offs |
| IC-23 **P1** | Почему IS-A/HAS-A и LSP — не механические тесты компилятора? Наследование требует поведенческой подстановки, композиция — согласованного публичного API; оба могут быть ошибочны. | Дизайн SOLID/LSP C, не JLS | Решение с критериями, CORE-COMPOSITION |
| IC-24 **P1** | Как проверить код с внедрённой зависимостью без Mockito? Предать test fake/stub как реализацию интерфейса, проверить эффект без framework; но testability не гарантирует корректность контракта. | Дизайн/TEST-DESIGN | Показать простой fake при G2, не навязывать exercise |
| IC-25 **P2** | Как `sealed` interface/permits меняет допустимые реализации? Правила Java 21 существуют, но подробный анализ отдельно. | §9.1.1.4 | CORE-SEALED |
| IC-26 **P2** | Как generics, bridge/erasure и covariant returns усложняют интерфейсные конфликты? | §9.4.1.2, §8.4.8.3 | CORE-GENERICS/CORE-ERASURE |
| IC-27 **P2** | Как лямбды, method references и intersection types взаимодействуют с SAM? | §9.8–9.9, §15.27 | CORE-FUNCTIONS |
| IC-28 **P2** | Как добавление abstract/default методов влияет на source/binary compatibility интерфейсов? Новые defaults могут вызвать конфликт; нельзя обещать «добавлять default всегда безопасно». | JLS 21 §13, §9.4.1 | API evolution отдельное исследование |
| IC-29 **P2** | Как Spring DI/AOP, proxies, reflection и framework lifecycle влияют на design выбора? Зависит от конкретных версий, не гарантируется JLS. | OFF-SPRING при будущем исследовании | SPR-DI, TEST-DESIGN, DESIGN-ARCH |
| IC-30 **P2** | Дает ли final поле-делегат safe publication/thread safety? Нет автоматической безопасности всех операций и побочных эффектов; JMM отдельная тема. | JLS §17, API | CON-JMM, concurrency |

## 5. Репрезентативные compile/run проверки Java 21

**Важно:** проверялись отдельные случаи в среде OpenJDK 21.0.12.1; JLS/API — основание нормативных утверждений. `javac`-ошибки не являются runtime errors, а совпадение stdout на одном JDK не доказывает производительность, безопасность потоков или превосходство композиции в продакшене.

### 5.1 Positive: полноценный `InterfaceCompositionProbes.java`

Сохранить следующий листинг в файл `InterfaceCompositionProbes.java` и выполнить:
```sh
mkdir -p out
javac --release 21 -d out InterfaceCompositionProbes.java
java -cp out InterfaceCompositionProbes
```

Исходник зафиксирован в плане специально **для воспроизводимости G1**; это не публикация новой Java-статьи/упражнения. Встроенная программа содержит несколько вложенных интерфейсов и классов для независимости от пакетов проекта.

```java
public class InterfaceCompositionProbes {
  interface Processor { String run(); }
  static final class Fast implements Processor { public String run() { return "fast"; } }
  static final class Safe implements Processor { public String run() { return "safe"; } }

  interface Welcome { default String text() { return "interface"; } }
  static class ClassWins { public String text() { return "class"; } }
  static final class Mixed extends ClassWins implements Welcome { }

  interface Left { default String message() { return "L"; } }
  interface Right { default String message() { return "R"; } }
  static final class Both implements Left, Right {
    @Override public String message() { return Left.super.message() + Right.super.message(); }
  }

  interface ParentDefault { default String value() { return "parent"; } }
  interface ChildDefault extends ParentDefault { @Override default String value() { return "child"; } }
  static final class MoreSpecific implements ParentDefault, ChildDefault { }

  interface Helpers {
    private String prefix() { return "private:"; }
    private static int twice(int value) { return value * 2; }
    default String label() { return prefix() + twice(67); }
    static String origin() { return "interface-static"; }
  }
  static final class HelpersImpl implements Helpers { }

  interface Constants { int VALUE = 67; int[] ARRAY = {67}; }

  static abstract class ParentWithState {
    protected final int base;
    ParentWithState(int base) { this.base = base; }
    abstract int score();
  }
  static final class Concrete extends ParentWithState {
    Concrete(int base) { super(base); }
    @Override int score() { return base + 1; }
  }

  @FunctionalInterface
  interface Transform {
    int apply(int x);
    boolean equals(Object o); // публичный метод Object: не новый SAM
    default int doubleApply(int x) { return apply(x) * 2; }
    static int identity(int x) { return x; }
  }

  interface Marker {}
  static final class Marked implements Marker {}
  interface First { String name(); }
  interface Second { String name(); }
  static final class OneImplementation implements First, Second {
    public String name() { return "one"; }
  }

  interface DefaultBase { default String get() { return "default"; } }
  interface Required extends DefaultBase { @Override String get(); }
  static final class OverrideRequired implements Required {
    public String get() { return "required"; }
  }

  interface Policy { int adjust(int input); }
  static final class AddOne implements Policy { public int adjust(int input) { return input + 1; } }
  static final class DoubleIt implements Policy { public int adjust(int input) { return input * 2; } }
  static final class Service { // композиция + constructor injection, без extends Policy
    private final Policy policy;
    Service(Policy policy) { this.policy = java.util.Objects.requireNonNull(policy); }
    int execute(int number) { return policy.adjust(number); }
  }

  static final class DecoratedPolicy implements Policy {
    private final Policy delegate;
    DecoratedPolicy(Policy delegate) { this.delegate = delegate; }
    @Override public int adjust(int input) { return delegate.adjust(input) + 10; }
  }

  public static void main(String[] args) {
    Processor p = new Fast();
    System.out.println("P01=" + p.run() + "," + new Safe().run());
    System.out.println("P02=" + new Mixed().text());
    System.out.println("P03=" + new Both().message());
    System.out.println("P04=" + new MoreSpecific().value());
    System.out.println("P05=" + new HelpersImpl().label());
    System.out.println("P06=" + Helpers.origin());
    System.out.println("P07=" + Constants.VALUE + "," + Constants.ARRAY[0]);
    Constants.ARRAY[0] = 68;
    System.out.println("P08=" + Constants.ARRAY[0]);
    System.out.println("P09=" + new Concrete(67).score());
    Transform transformer = x -> x + 1;
    System.out.println("P10=" + transformer.apply(67) + "," + transformer.doubleApply(67));
    System.out.println("P11=" + (new Marked() instanceof Marker));
    System.out.println("P12=" + new OneImplementation().name());
    System.out.println("P13=" + new OverrideRequired().get());
    System.out.println("P14=" + new Service(new AddOne()).execute(67) + "," + new Service(new DoubleIt()).execute(67));
    System.out.println("P15=" + new Service(new DecoratedPolicy(new AddOne())).execute(67));
    System.out.println("P16=" + Transform.identity(67));
  }
}
```

**Фактический stdout на OpenJDK 21.0.12.1** (все 16 строк; значения равны ожидаемым):
```text
P01=fast,safe
P02=class
P03=LR
P04=child
P05=private:134
P06=interface-static
P07=67,67
P08=68
P09=68
P10=68,136
P11=true
P12=one
P13=required
P14=68,134
P15=78
P16=67
```

**Проверено:** `javac --release 21` успешно завершился; `java` вывел ровно эти 16 строк. Это один успешно скомпилированный независимый набор + 16 отдельных наблюдений stdout, **не** 16 независимых компиляций. Начальное значение `Constants.ARRAY[0]` изменилось несмотря на final-ссылку, что не нарушает JLS. P14/P15 показывают работающий API, но **не** измеряют качество дизайна.

### 5.2 Negative: 19 отдельных ожидаемых ошибок `javac`

**Правило воспроизведения:** каждый фрагмент одной строки сохранить **в отдельный** `<ID>.java` (фрагменты не объединять, так как имена `A`/`B`/`Api` повторяются); выполнить `javac --release 21 -d out <ID>.java` с JDK 21. Если компилятор завершился успешно, тест провалился. Сверять **причину**, не дословную реализацию диагностики.

| ID | Полный Java-фрагмент для отдельного файла | Ожидаемая причина отказа |
|---|---|---|
| N01_DoubleDefault | `interface A {default String m(){return "A";}} interface B {default String m(){return "B";}} class N01_DoubleDefault implements A,B {}` | Два конфликтующих независимых default |
| N02_DefaultVsAbstract | `interface A {void m();} interface B {default void m(){}} class N02_DefaultVsAbstract implements A,B {}` | Abstract/default conflict, нужен override |
| N03_AbstractSuperclass | `abstract class Parent {abstract String m();} interface Def {default String m(){return "default";}} class N03_AbstractSuperclass extends Parent implements Def {}` | Abstract superclass method не удовлетворён default |
| N04_DefaultObject | `interface N04_DefaultObject {default String toString(){return "wrong";}}` | default override-equivalent с `Object.toString` |
| N05_ProtectedMethod | `interface N05_ProtectedMethod {protected void m();}` | `protected` недопустим для метода интерфейса |
| N06_UninitializedField | `interface N06_UninitializedField {int state;}` | Неинициализированное final поле интерфейса |
| N07_MissingImpl | `interface Api {void run();} class N07_MissingImpl implements Api {}` | Concrete class не реализовал abstract method |
| N08_WeakerVisibility | `interface Api {void run();} class N08_WeakerVisibility implements Api {void run(){}}` | Попытка снизить public access |
| N09_InterfaceStaticImpl | `interface Api {static void ping(){}} class Imp implements Api {} class N09_InterfaceStaticImpl {void test(){Imp.ping();}}` | Static interface method не наследуется классом |
| N10_PrivateOutside | `interface Api {private static void secret(){}} class N10_PrivateOutside {void test(){Api.secret();}}` | Нет доступа к private helper |
| N11_NonDirectSuper | `interface A {default void m(){}} interface B extends A {} class N11_NonDirectSuper implements B {void test(){A.super.m();}}` | A не direct superinterface |
| N12_RedundantSuper | `interface A {default void m(){}} interface B extends A {} class N12_RedundantSuper implements A,B {void test(){A.super.m();}}` | A direct, но redundant через более специфический B |
| N13_NotFunctional | `@FunctionalInterface interface N13_NotFunctional {void a();void b();}` | Две несвязанные abstract operations |
| N14_PrivateAbstract | `interface N14_PrivateAbstract {private abstract void m();}` | Недопустимая комбинация private abstract |
| N15_AssignInterfaceField | `interface A {int N=67;} class N15_AssignInterfaceField {void f(){A.N=68;}}` | Нельзя присвоить static final |
| N16_DefaultWithoutBody | `interface N16_DefaultWithoutBody {default void m();}` | У default метода обязано быть тело |
| N17_DefaultEquals | `interface N17_DefaultEquals {default boolean equals(Object o){return true;}}` | default override-equivalent с Object.equals |
| N18_PrivateField | `interface N18_PrivateField {private int counter=67;}` | Не существует private instance field в интерфейсе |
| N19_FailDefaultStatic | `interface N19_FailDefaultStatic {static default void m(){}}` | `static` и `default` взаимоисключающи |

**Фактическая проверка:** все **19/19** отдельных команд `javac --release 21` вернули ошибку ожидаемого класса на OpenJDK 21.0.12.1. Это **19 независимых отрицательных компиляций**, в отличие от одного положительного запуска с 16 stdout assertions. Точный текст ошибок `javac`, их число и номера строк не входят в нормативную гарантию JLS. Все исходники доступны в этой таблице, но отдельными файлами в Git не коммитились.

## 6. Архитектурная таблица: выбирать по контракту, не по лозунгу

| Ситуация | Суперкласс/наследование | Композиция/делегирование | Неочевидная стоимость |
|---|---|---|---|
| Действительно общее изменяемое состояние и обязательный template lifecycle | Абстрактный класс может хранить защищённое состояние, вызывать общий код | Объекты могут делиться ответственностью без общей иерархии | Наследник связан с порядком вызовов родителя; у wrapper больше plumbing |
| Несколько вариантов одного алгоритма | Подклассы могут override, но изменения затрагивают иерархию | `Service(Policy)` выбирает реализацию при сборке объекта | Нужно поддерживать контракт Policy, передавать ошибки/ресурсы, сохранять identity/lifetime |
| Добавление диагностики вокруг алгоритма | Override с вызовом `super` возможен, если extension point доступен | `DecoratedPolicy` делегирует и дополняет результат | Wrapper не всегда прозрачная замена: `equals`, `toString`, синхронизация и contract evolution могут требовать forwarding |
| Объект обязан быть подтипом API, потребители полагаются на `instanceof` | Реализация interface даёт нужное subtype; extends базового класса — другое обязательство | Внутренний delegate **сам по себе не делает** внешний Service подтипом Policy | Может понадобиться явное `implements Policy` и полный forwarding; избегать ложного «HAS-A значит IS-A» |
| Изменение контракта зависимостей | Наследование может облегчить shared implementation, но усилить fragile-base coupling | Интерфейс позволяет заменить Policy при конструировании | Не всякая новая реализация поведенчески substitutable, несмотря на компиляцию |

Критерии при выборе: семантика подстановки (LSP), необходимость собственного состояния/инварианта, кто владеет зависимостью и закрывает её ресурсы, ожидаемое изменение требований, стоимость публичного forwarding API, допустимость замены в тесте, требования к идентичности и backward compatibility. **Не** объявлять P14/P15 «доказательством SOLID» или производственного преимущества без конкретных измерений/ограничений.

## 7. Реально опубликованные зависимости и отсутствующие темы

По GitHub `main` @ `fb8cf28604260fb5453f82f07317ddb6557c6442` проверены существующие пути:

- [CORE-CLASSES](../../knowledge/01-java-core/classes.md) — объекты, конструкторы, состояние, передача объекта, инварианты.
- [CORE-ACCESS](../../knowledge/01-java-core/access.md) — `protected`/package/private, интерфейсные методы, interface member types, доступ через два пакета. Здесь не повторять всю нормативную таблицу.
- [CORE-INHERITANCE](../../knowledge/01-java-core/inheritance.md) — overriding, static/instance dispatch, `super`, class inheritance. Ссылка обязательна при значимом обсуждении dispatch.
- [CORE-INIT](../../knowledge/01-java-core/initialization.md) — superclass-before-subclass и default superinterface initialization, включая synthetic default. Не копировать 800-строчный разбор, дать отдельную ссылку.
- [CORE-TYPES](../../knowledge/01-java-core/types.md) — reference types, casts и статический тип выражения; напомнить по одной проверяемой строчке, без пересказа.
- [CURRICULUM](../CURRICULUM.md), [COVERAGE](../COVERAGE.md), [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md) — нормативные входы, статусы и карта будущих статей.

**Не существующие в исследованном main ID, поэтому без Markdown-ссылок:** `CORE-FUNCTIONS`, `CORE-GENERICS`, `CORE-ERASURE`, `CORE-SEALED`, `CORE-IMMUTABLE`, `SPR-DI`, `TEST-DESIGN`, `DESIGN-ARCH`, `CON-JMM`, `CORE-REFLECTION`, `CORE-MODULES`. Перед редактурой 02/05 заново сверить существование каждой цели; только потом ставить локальные ссылки. Не считать покрытыми Spring bean scopes, DI framework, mocking, interface proxies, JMM, generics erasure, module access или lambda capture только по данной статье.

## 8. Исторические критерии передачи ролям и принятое решение G1

- **00:** решить, одобрены ли **две статьи / единый план / один интеграционный пример**, все IC-01–IC-16 P0 и честные границы IC-17–IC-30. Проверить BASE/HEAD и результат ревью, затем явно ACCEPTED/CHANGES REQUESTED; G1 не приравнивается к публикации или merge.
- **04:** независимое содержательное review по точному HEAD: JLS §§9.4.1.3/8.4.8.4/15.12/9.8, class abstract vs interface default, Object-method prohibition, redundant Interface.super, static/private, const reference vs mutation. При необходимости повторить P01–P16 и выборку N01–N19 отдельно; зелёный workflow **не** заменяет проверки этих Markdown-listings. Вердикт с указанием конкретных блокеров.
- **02 (только после G1):** написать два самостоятельных связных текста. Для CORE-INTERFACES раскрыть IC-01–IC-13 с достаточными «что выведет/почему не компилируется»; для CORE-COMPOSITION раскрыть IC-14–IC-16 и применимые P1, преимущества и затраты на одном понятном сценарии. Не перепечатывать оба раза интерфейсную механику и не превращать методологические советы в JLS.
- **03 (только после G1):** при содержательном смысле сделать один компактный multi-file reusable example: package `api` (Policy), `impl` (две реализации и wrapper), `client` (выбор policy и проверка поведения). Негативные интерфейсные случаи держать изолированно, чтобы основной source set компилировался; если для этого достаточно inline кода §5, не создавать механически отдельное Gradle-упражнение. JUnit/Mockito только при оправданном контракте.
- **05:** проверить читаемость, отсутствие «написано ради статей» и достаточность объяснений; работать с локальными Obsidian-ссылками только на существующие цели, фактическую IDE-проверку обозначить отдельно.
- **06:** после публикации/решения 00 синхронизировать действующие файлы и статус `COVERAGE` на фактическом main, не объявлять G6 по одному зелёному CI.

**Что реально выполнялось:** локальная `javac --release 21` + `java` для §5.1 (один positive source / 16 точных строк) и 19 изолированных negative `javac` сценариев §5.2. **Not run:** локальная Gradle/JUnit/Mockito сборка репозитория, IntelliJ/Obsidian GUI preview/клики, property-based/большой набор JDK, concurrency stress, Spring, API binary compatibility, performance benchmark, интеграционный example роли 03. CI для нового PR проверять отдельно **по его будущему HEAD** и записывать точный scope: существующий workflow не компилирует java-листинги внутри этого плана, пока они не вынесены в source files.

**Итог G6 (статусная приписка, без изменения исследовательской матрицы IC-01–IC-30):** предложенные ролью 01 две самостоятельные связанные статьи под единым планом приняты 00 как G1 по HEAD `274ced773bee55d9ecdc89924e0e8b370cc815ea` после independent review 04. PR #66 squash-merged в `main` как `9fa271902ff6a3b9fb487f85e39d8682e8304113`. Локальные 16 positive stdout + 19 negative `javac` остаются проверками исследовательского плана, **не** репозиторным CI harness. Предmerge [Actions #44792885119](https://github.com/Melchiorius/backend-interview-labs/actions/runs/44792885119) `completed/success` проверял только существующий workflow на HEAD плана, не IC snippets. Отдельные статьи CORE-INTERFACES/CORE-COMPOSITION и их runnable **ещё не опубликованы**; принимать G1 за G2/G5 или повышать `COVERAGE` до Implemented/Verified нельзя.