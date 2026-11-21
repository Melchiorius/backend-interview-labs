# CORE-INTERFACES + CORE-COMPOSITION — один Java 21 runnable

Один автономный многофайловый пример показывает **две разные задачи**: (1) правила интерфейсного API — abstract/default/private, разрешение конфликтов двух default и приоритет реализации класса; (2) композицию — сервис хранит интерфейсную зависимость и явно делегирует ей работу. Вместо двух несвязанных проектов используется **одна** группа из восьми Java-файлов с реальными пакетами `api`, `impl`, `client`. Это готовый пример, не ученическая заготовка и не доказательство «композиция всегда лучше наследования».

**Исходники:** [Policy.java](src/api/Policy.java), [LeftOrigin.java](src/api/LeftOrigin.java), [RightOrigin.java](src/api/RightOrigin.java), [AddTenPolicy.java](src/impl/AddTenPolicy.java), [DoublePolicy.java](src/impl/DoublePolicy.java), [IncrementingDecorator.java](src/impl/IncrementingDecorator.java), [Service.java](src/client/Service.java), [InterfaceCompositionDemo.java](src/client/InterfaceCompositionDemo.java). **Контрольные файлы:** [expected.stdout](expected.stdout), [verify.sh](verify.sh).

Уже опубликованные теоретические зависимости: [классы и конструкторы](../../../knowledge/01-java-core/classes.md), [наследование и overriding](../../../knowledge/01-java-core/inheritance.md), [правила доступа](../../../knowledge/01-java-core/access.md). [Исследовательский план CORE-INTERFACES + CORE-COMPOSITION](../../../docs/plans/CORE-INTERFACES-COMPOSITION.md) — карта IC-01–30 и ссылки на JLS 21. **Связанные самостоятельные теоретические статьи уже включены в этот интегрированный PR:** [CORE-INTERFACES — правила Java 21](../../../knowledge/01-java-core/interfaces.md) и [CORE-COMPOSITION — архитектура и делегирование](../../../knowledge/01-java-core/composition.md). Первая объясняет default/abstract/conflict/Interface.super; вторая — Strategy, constructor injection, Decorator, ownership и test fake. Примеры внутри каждой статьи самостоятельны, их stdout не следует смешивать с десятью строками этого многофайлового сценария.

## Участники

- Пакет **`api`**: `Policy` — `@FunctionalInterface` с ровно одним абстрактным `int apply(int value)`. `default describe(int)` предоставляет обычное наследуемое поведение и вызывает `apply` **один раз**. `private static format` помогает реализации default, но не выставлен в публичном API. `LeftOrigin` и `RightOrigin` объявляют независимые `default origin()`.
- Пакет **`impl`**: `AddTenPolicy` прибавляет 10, `DoublePolicy` умножает на 2. `IncrementingDecorator` **содержит** `Policy delegate`, обращается к нему через `delegate.apply(value)`, затем прибавляет 1; он не наследуется от `DoublePolicy` и может обернуть другую политику. `Math.addExact`/`multiplyExact` намеренно сигнализируют переполнение, а не обещают насыщение или обработку любых чисел.
- Пакет **`client`**: `Service` принимает `Policy` через конструктор, отвергает `null` через `Objects.requireNonNull`, хранит зависимость в `private final` поле и делегирует `execute` и `explain`. Класс `InterfaceCompositionDemo` создаёт три экземпляра **одного типа** `Service` с разной политикой и содержит два маленьких положительных JLS-контраста.

## Запуск из корня репозитория

Требуется установленный **JDK 21** с `javac`, `java` в `PATH`. Выполняйте в корневом каталоге репозитория, Linux/macOS/PowerShell:

```sh
javac --release 21 -d examples/core/interfaces-composition/build/classes -sourcepath examples/core/interfaces-composition/src examples/core/interfaces-composition/src/client/InterfaceCompositionDemo.java
java -cp examples/core/interfaces-composition/build/classes client.InterfaceCompositionDemo
```

Для независимой полной проверки (Bash, включая Git Bash на Windows):

```sh
java -version
javac -version
bash examples/core/interfaces-composition/verify.sh
```

Скрипт очищает **только** `examples/core/interfaces-composition/build/`, выполняет `javac --release 21`, запускает точку входа и сравнивает `expected.stdout` с `build/actual.stdout` через `diff -u`. Любое несовпадение выдаёт ненулевой exit code; при успехе скрипт печатает полный вывод Java-программы и дополнительную контрольную строку `CHECK PASSED: stdout matches expected.stdout` (эта дополнительная строка **не** входит в сохранённый stdout). Каталог `build/` игнорируется Git через существующий `.gitignore`; нет отдельного Gradle-подпроекта, JUnit или Mockito.

## Полный ожидаемый stdout Java-программы

```text
== Service with interchangeable policies ==
add ten: 77
double: 134
decorated double: 135
default describe: input=67 -> output=77
null dependency rejected: true
== Interface default resolution ==
two defaults: left|right
class wins: class
CHECK PASSED
```

## Почему вывод именно такой

`Service` не наследует ни `AddTenPolicy`, ни `DoublePolicy`. `Service.execute(67)` только передаёт вход текущей зависимости: первая реализация даёт 67 + 10 = 77, вторая — 67 × 2 = 134. Декоратор вызывает делегированную `DoublePolicy.apply(67)` и добавляет 1, поэтому получается 135. Публичный `default describe` интерфейса вызывает `apply` и форматирует строку `input=67 -> output=77` через скрытый helper; отсутствие override не мешает inherited default. `new Service(null)` вызывает `NullPointerException` в конструкторе и отмечается `true` только потому, что пример перехватывает этот ожидаемый случай.

В локальном `Resolved implements LeftOrigin, RightOrigin` два независимых default имеют одну сигнатуру. Не существует правила «побеждает последний интерфейс»: класс явно реализует `origin()` и вызывает допустимые `LeftOrigin.super.origin()` и `RightOrigin.super.origin()`, соединяя `left|right`. В `ClassWins extends OriginBase implements LeftOrigin` доступный конкретный `OriginBase.origin()` получает приоритет перед интерфейсным default: результат `class`. **Это** языковые правила JLS §8.4.8.4, §9.4.1.3, §15.12; необходимость и удачность `Service(Policy)` — решение о дизайне, не гарантия компилятора.

`final` в `Service.policy` **не** означает автоматическую потокобезопасность `Policy`, не фиксирует побочные эффекты delegate и не доказывает соответствие LSP/SOLID. Декоратор меняет поведение, поэтому его подстановка корректна лишь для предметного контракта, допускающего новое преобразование; в общем случае decorator обязан осознанно сохранять нужные свойства API, возвращаемые значения и исключения. `Objects.requireNonNull` устанавливает только инвариант non-null ссылки, а не принадлежность, потокобезопасность или жизненный цикл зависимости.

## Что здесь намеренно не воспроизводится

Отдельные **некомпилируемые** случаи из плана (default/default без override, abstract/default, недопустимый `InterfaceName.super`, override-equivalent `Object`-метод) должны оставаться самостоятельными короткими примерами в статье CORE-INTERFACES: они не входят в рабочую компиляцию и не выдают общий failure `javac` за точное доказательство JLS. Специализированные наследование конфликтующих generics, sealed interfaces, Spring DI, mutable lifecycle, concurrency и продакшен-тестирование — за пределами этого runnable.

**Фактическая локальная проверка роли 03:** OpenJDK `21.0.12.1` / `javac 21.0.12.1`, из корня запущено `bash examples/core/interfaces-composition/verify.sh` — exit code `0`, все **10 строк** stdout совпали с `expected.stdout`, после сравнения выведено `CHECK PASSED: stdout matches expected.stdout`. Сохранённые Java-файлы, `verify.sh` и `expected.stdout` сверены с опубликованными Git blob SHA (10/10 совпадений). Для другого поставщика JDK, UI Obsidian/IntelliJ и локального Gradle/JUnit — **not run**. GitHub Actions нужно проверять отдельно на **фактическом SHA конкретного PR**: успешный запуск предыдущего HEAD не доказывает прохождение CI после новых изменений.
