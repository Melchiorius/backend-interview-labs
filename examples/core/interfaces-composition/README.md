# CORE-INTERFACES + CORE-COMPOSITION — один Java 21 runnable

Один автономный многофайловый пример показывает **две разные задачи**: (1) правила интерфейсного API — abstract/default/private, разрешение конфликтов двух default и приоритет реализации класса; (2) композицию — сервис хранит интерфейсную зависимость и явно делегирует ей работу. Вместо двух несвязанных проектов используется **одна** исходная группа из восьми Java-файлов с реальными пакетами `api`, `impl`, `client`. Дополнительный самостоятельный пример B2c проверяет private-доступ отдельно, не изменяя исходную композицию. Это готовый пример, не ученическая заготовка и не доказательство «композиция всегда лучше наследования».

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

Исходная часть скрипта очищает **только** `examples/core/interfaces-composition/build/`, выполняет `javac --release 21`, запускает точку входа и сравнивает `expected.stdout` с `build/actual.stdout` через `diff -u`. Любое несовпадение выдаёт ненулевой exit code; при успехе скрипт печатает полный исходный вывод Java-программы, выполняет отдельный блок B2c и завершает работу дополнительной контрольной строкой `CHECK PASSED: stdout matches expected.stdout` (эта дополнительная строка **не** входит в сохранённый stdout). Каталог `build/` игнорируется Git через существующий `.gitignore`; нет отдельного Gradle-подпроекта, JUnit или Mockito.

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

## B2c — private helper вложенного интерфейса

Отдельный [InterfacePrivateAccessDemo.java](src/client/InterfacePrivateAccessDemo.java) в пакете client содержит public вложенный интерфейс Policy с abstract apply и private **instance** helper(). Объемлющий top-level класс создаёт lambda, вызывает helper через receiver типа Policy, проверяет результат и печатает `nestPrivate=policy`. Доступ разрешён из тела объемлющего top-level типа; private helper не наследуется и не является публичным API. Это не исходный `api.Policy` с private static format. Исходные восемь Java-файлов, семь проверок, десять строк и expected.stdout не менялись. Добавлена одна независимая assertion, не JUnit.

Изолированный положительный запуск из корня репозитория:

~~~sh
mkdir -p examples/core/interfaces-composition/build/private-classes
javac --release 21 -XDrawDiagnostics -d examples/core/interfaces-composition/build/private-classes examples/core/interfaces-composition/src/client/InterfacePrivateAccessDemo.java
java -cp examples/core/interfaces-composition/build/private-classes client.InterfacePrivateAccessDemo
~~~

Ожидаемый вывод самостоятельной программы:

~~~text
nestPrivate=policy
~~~

Два намеренно некомпилируемых файла проверяются отдельно:

| Файл | Выражение | Причина |
|---|---|---|
| [BadExternalPrivateAccess.java](negative/BadExternalPrivateAccess.java) | `policy.helper()` | Receiver и публичный вложенный тип доступны, но посторонний top-level класс не имеет private-доступа |
| [BadTypeQualifiedInstance.java](negative/BadTypeQualifiedInstance.java) | `Policy.helper()` | В объемлющем классе private-доступ разрешён, но instance-метод вызывается через имя типа без receiver |

После положительного запуска (создайте выходные каталоги заранее):

~~~sh
mkdir -p examples/core/interfaces-composition/build/negative/BadExternalPrivateAccess examples/core/interfaces-composition/build/negative/BadTypeQualifiedInstance
javac --release 21 -XDrawDiagnostics -cp examples/core/interfaces-composition/build/private-classes -d examples/core/interfaces-composition/build/negative/BadExternalPrivateAccess examples/core/interfaces-composition/negative/BadExternalPrivateAccess.java
javac --release 21 -XDrawDiagnostics -cp examples/core/interfaces-composition/build/private-classes -d examples/core/interfaces-composition/build/negative/BadTypeQualifiedInstance examples/core/interfaces-composition/negative/BadTypeQualifiedInstance.java
~~~

На javac 21.0.12.1 оба случая дали exit 1, пустой stdout и одну ожидаемую ошибку: `compiler.err.report.access: helper(), private, client.InterfacePrivateAccessDemo.Policy` и `compiler.err.non-static.cant.be.ref: kindname.method, helper()` соответственно. Причины различны, не отсутствие типа. JLS 21 [§6.6.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6.1), [§9.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4.1), [§15.12.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.3). Конкретные диагностические ключи и аргументы относятся к javac, не гарантия формата JLS.

Runner сохраняет исходное сравнение expected.stdout, отдельно компилирует и запускает B2c в private-classes, сравнивает его одну строку с отдельным временным эталоном и проверяет каждую negative-компиляцию по exit=1, пустому stdout, категории/аргументам и одной ошибке. Строка nestPrivate=policy, EXPECTED REJECTION и диагностики печатаются **после** десяти исходных строк, но **до** последнего маркера CHECK PASSED: stdout matches expected.stdout. Они не входят в исходный expected.stdout.

## Что здесь намеренно не воспроизводится

Отдельные **некомпилируемые** случаи из плана (default/default без override, abstract/default, недопустимый `InterfaceName.super`, override-equivalent `Object`-метод) должны оставаться самостоятельными короткими примерами в статье CORE-INTERFACES: они не входят в рабочую компиляцию и не выдают общий failure `javac` за точное доказательство JLS. Специализированные наследование конфликтующих generics, sealed interfaces, Spring DI, mutable lifecycle, concurrency и продакшен-тестирование — за пределами этого runnable.

**Историческая локальная проверка исходного сценария роли 03:** OpenJDK `21.0.12.1` / `javac 21.0.12.1`, из корня запущено `bash examples/core/interfaces-composition/verify.sh` — exit code `0`, все **10 строк** stdout совпали с `expected.stdout`, после сравнения выведено `CHECK PASSED: stdout matches expected.stdout`. Сохранённые Java-файлы, `verify.sh` и `expected.stdout` сверены с опубликованными Git blob SHA (10/10 совпадений). Для другого поставщика JDK, UI Obsidian/IntelliJ и локального Gradle/JUnit — **not run**. GitHub Actions нужно проверять отдельно на **фактическом SHA конкретного PR**: успешный запуск предыдущего HEAD не доказывает прохождение CI после новых изменений.

**B2c — новая локальная проверка:** Debian OpenJDK/javac 21.0.12.1. Baseline/final `LC_ALL=C bash -n` и полный runner exit 0, stderr runner пуст. Исходный 10-line actual.stdout совпал побайтно с baseline и expected.stdout; прежние 11 строк полного runner (включая финальный маркер) сохранены, новый блок добавил 7 строк. Standalone compile/run exit 0/0, два изолированных negative javac exit 1 с указанными причинами. Проверены 10/10 исходных и 13/13 итоговых исполняемых Git blob; runner mode 100755 сохранён. Локальный Gradle/JUnit, полный clean clone и GUI не запускались; exact CI проверяется отдельно.
