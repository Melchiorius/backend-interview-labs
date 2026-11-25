# CORE-INTERFACES + CORE-COMPOSITION — один Java 21 runnable

Один автономный многофайловый пример показывает **две разные задачи**: (1) правила интерфейсного API — abstract/default/private, разрешение конфликтов двух default и приоритет реализации класса; (2) композицию — сервис хранит интерфейсную зависимость и явно делегирует ей работу. Вместо двух несвязанных проектов используется **одна** исходная группа из восьми Java-файлов с реальными пакетами `api`, `impl`, `client`. Дополнительный самостоятельный пример B2c проверяет private-доступ отдельно, не изменяя исходную композицию. Это готовый пример, не ученическая заготовка и не доказательство «композиция всегда лучше наследования».

**Исходники:** [Policy.java](src/api/Policy.java), [LeftOrigin.java](src/api/LeftOrigin.java), [RightOrigin.java](src/api/RightOrigin.java), [AddTenPolicy.java](src/impl/AddTenPolicy.java), [DoublePolicy.java](src/impl/DoublePolicy.java), [IncrementingDecorator.java](src/impl/IncrementingDecorator.java), [Service.java](src/client/Service.java), [InterfaceCompositionDemo.java](src/client/InterfaceCompositionDemo.java). **Контрольные файлы:** [expected.stdout](expected.stdout), [verify.sh](verify.sh).

Уже опубликованные теоретические зависимости: [классы и конструкторы](../../../knowledge/01-java-core/classes.md), [наследование и overriding](../../../knowledge/01-java-core/inheritance.md), [правила доступа](../../../knowledge/01-java-core/access.md). [Исследовательский план CORE-INTERFACES + CORE-COMPOSITION](../../../docs/plans/CORE-INTERFACES-COMPOSITION.md) — карта IC-01–30 и ссылки на JLS 21. **Связанные опубликованные теоретические статьи:** [CORE-INTERFACES — правила Java 21](../../../knowledge/01-java-core/interfaces.md) и [CORE-COMPOSITION — архитектура и делегирование](../../../knowledge/01-java-core/composition.md). Первая объясняет абстрактные методы, методы по умолчанию, их конфликты и `Interface.super`; вторая — Strategy, внедрение зависимостей через конструктор, Decorator, владение и тестовые двойники. Примеры статей самостоятельны: их вывод не входит в десять строк `InterfaceCompositionDemo` и `expected.stdout`, которые также не следует путать с полным выводом скрипта.

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

В локальном `Resolved implements LeftOrigin, RightOrigin` два независимо объявленных метода по умолчанию имеют одну сигнатуру. Не существует правила «побеждает последний интерфейс»: класс явно реализует `origin()` и вызывает допустимые `LeftOrigin.super.origin()` и `RightOrigin.super.origin()`, соединяя `left|right`. В `ClassWins extends OriginBase implements LeftOrigin` унаследованный публичный экземплярный `OriginBase.origin()` с совместимой сигнатурой и контрактом получает приоритет перед методом по умолчанию интерфейса: результат `class`. **Это** языковые правила JLS §8.4.8.4, §9.4.1.3, §15.12; необходимость и удачность `Service(Policy)` — решение о дизайне, не гарантия компилятора.

`final` в `Service.policy` **не** означает автоматическую потокобезопасность `Policy`, не фиксирует побочные эффекты delegate и не доказывает соответствие LSP/SOLID. Декоратор меняет поведение, поэтому его подстановка корректна лишь для предметного контракта, допускающего новое преобразование; в общем случае decorator обязан осознанно сохранять нужные свойства API, возвращаемые значения и исключения. `Objects.requireNonNull` устанавливает только инвариант non-null ссылки, а не принадлежность, потокобезопасность или жизненный цикл зависимости.

## D-A09 — проверка результата, взаимодействия и договора

В основной программе `InterfaceCompositionDemo` сохранены семь исходных проверок. Старая проверка строки описания теперь называется проверкой форматированного результата: сама строка не доказывает количество вызовов. Новые проверки выполняются до вывода и не добавляют строк в `expected.stdout`. Десять строк эталона — не количество проверок и не полный вывод скрипта.

### Проверка результата и взаимодействия (N12)

Создаются два отдельных экземпляра `RecordingPolicyFake`. С первым `Service.execute(67)` вызывает `apply`; проверяются возвращённое число `900`, переданный аргумент `67` и ровно один вызов. Второй экземпляр используется только для `Service.explain(67)`: у политики вызывается `describe`, который обращается к `apply` и возвращает строку `input=67 -> output=900`. Проверяются строка, аргумент и ровно один вызов `apply`. Это последовательные проверки результата и взаимодействия, а не доказательство полного контракта и не тесты JUnit.

### Два варианта описания декоратора (N10)

`CustomPolicy.apply` возвращает вход без изменений, а `CustomPolicy.describe` возвращает `custom`. Прямой вызов и вызов через `Service.explain` дают `custom`.

Когда сервис вызывает `explain(67)` с политикой `IncrementingDecorator`, унаследованный метод по умолчанию `describe` вызывает `apply` самого декоратора. Тот один раз вызывает `apply(67)` делегата и прибавляет единицу: описание становится `input=67 -> output=68`. Счётчики отдельного экземпляра делегата подтверждают один вызов `apply` и отсутствие вызовов `describe`.

Учебный вложенный класс `ForwardingDescription` сохраняет ту же операцию `apply` (прибавление единицы к результату делегата), но явно перенаправляет `describe` делегату. С новым экземпляром `CustomPolicy` сервис получает `custom`; наблюдается один вызов `describe` с аргументом `67` и ни одного вызова `apply`. Это другой договор описания, не изменение основной реализации декоратора и не обещание, что описание отражает изменённый результат.

### Арифметика и исключения (N11)

Обычное сложение `int` при `Integer.MAX_VALUE + 1` даёт `Integer.MIN_VALUE`. В отличие от него `AddTenPolicy`, `DoublePolicy` и `IncrementingDecorator` с тождественной политикой (делегат возвращает вход) при аргументе `Integer.MAX_VALUE` выбрасывают `ArithmeticException`. Все три случая проверяются отдельно: отсутствие исключения или другой его тип приводит к отказу проверки.

Дополнительно проверяется, что через `Service.execute` и декоратор до клиента доходит тот же заранее созданный экземпляр `IllegalStateException`: делегат вызывается по одному разу, без повторных попыток. Оба конструктора отвергают `null`. Это контраст с обычной арифметикой в статье, а не обещание представимого результата на всём диапазоне `int`.

N13 — отдельный пример предусловия и постусловия клиента в CORE-COMPOSITION, не часть этой программы. N14: хранение ссылки — композиция объектов в широком смысле; исключительное владение в узком смысле UML — отдельное отношение. Обязанность вызвать `close()` не следует ни из HAS-A, ни из работы сборщика мусора. N15: исторические результаты G1 и B2c ниже не переименованы в результаты новых проверок.

### Локальные доказательства D-A09

Роль 03 выполнила проверки на Debian OpenJDK/javac 21.0.12.1 с `LC_ALL=C`; см. [адресный отчёт #7165101211](https://github.com/Melchiorius/backend-interview-labs/pull/296#issuecomment-7165101211) и первоначально проверенный коммит `3c9ee6183b1142bb806259702eb8cb67ec8218e6`. На исходной и итоговой версиях проверка синтаксиса `bash -n` и полный запуск `bash examples/core/interfaces-composition/verify.sh` из корня завершились с кодом 0, stderr пуст. Сверены Git blob SHA и режимы 13/13 исходных и 13/13 итоговых исполняемых и входных файлов. Скрипт и эталон не изменялись; последующая редактура Markdown не является новым локальным запуском.

Полный вывод скрипта в обоих запусках совпал побайтно и содержал 18 строк: 10 строк основной программы, `nestPrivate=policy`, два блока ожидаемых отказов B2c по три строки (маркер отказа, диагностика, `1 error`) и итоговый `CHECK PASSED: stdout matches expected.stdout`. Отдельные компиляция и запуск положительного примера B2c дали коды 0/0, две отрицательные компиляции — код 1, пустой stdout и по одной ошибке с ключами `report.access` и `non-static.cant.be.ref`.

### Отдельный мутационный эксперимент (N12)

Во временных копиях исходной и итоговой версий в `Policy.describe` перед `int result = apply(input);` добавили ровно один лишний вызов `apply(input);`. Исходная версия с этой мутацией прошла полный скрипт с кодом 0 и прежним stdout: проверка строки не обнаружила лишнее обращение. Итоговая версия с той же мутацией скомпилировалась с кодом 0, но запуск Java завершился с кодом 1, пустым stdout и `AssertionError: Policy.describe must call apply exactly once`. После удаления временной мутации итоговая версия снова прошла скрипт с тем же выводом; отслеживаемый Git файл `Policy.java` не менялся. Это доказательство обнаружения конкретного дефекта, не полный мутационный анализ и не проверка мутанта в CI.

### Отдельные пробы статических методов (N09, IC17)

Роль 03 проверила точные `Api.java` и `StaticScope.java` из статьи на [авторском коммите](https://github.com/Melchiorius/backend-interview-labs/blob/4eb7b28fe36aefe949bf5837fcc0f3ee6957a8ce/knowledge/01-java-core/interfaces.md): компиляция и запуск дали коды 0/0 и stdout `67,67` (доступ к простому имени внутри интерфейса и статический импорт в клиенте). Отдельный вызов `Api.kind()` дал коды 0/0 и stdout `67`. Статья иллюстрирует недопустимый вызов через имя реализующего класса формой `StaticScope.kind()`; в фактической отрицательной пробе 03 использован отдельный класс `Standard` без статического импорта. Вызов `Standard.kind()` отклонён с ключом `cant.resolve.location.args`, а `api.kind()` при статическом типе переменной `Api` — с `illegal.static.intf.meth.call`. Обе изолированные компиляции дали код 1, пустой stdout и по одной ошибке при корректном пути к скомпилированным классам. Это временные исходники, не часть общего скрипта; B2c проверяет закрытый доступ отдельно.

N01–N08/N13 — отдельные пробы роли 02, см. [отчёт автора](https://github.com/Melchiorius/backend-interview-labs/issues/295#issuecomment-7164948377); они не приписываются роли 03 или CI. Расхождение буквальной нормы и наблюдения для `default clone()` роль 03 повторно не проверяла. Полный Gradle/JUnit, чистое клонирование репозитория и графические интерфейсы локально не проверялись ролью 03.

[Исторический интеграционный CI #45636007674](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45636007674) завершился успешно на коммите `f46552389e75fba3e5fb4fa4d369ccca235adf80`. Обычный скрипт CI не запускает изолированные пробы N09 и эксперимент с мутантом. Этот результат относится только к указанной версии; для каждого нового HEAD необходим отдельный актуальный CI, а независимая проверка G4 обязательна.

## B2c — private helper вложенного интерфейса

Отдельный [InterfacePrivateAccessDemo.java](src/client/InterfacePrivateAccessDemo.java) в пакете `client` содержит вложенный `public`-интерфейс `Policy` с абстрактным методом `apply` и экземплярным `private`-методом `helper()`. Объемлющий верхнеуровневый класс создаёт лямбду, вызывает `helper()` на экземпляре через переменную типа `Policy`, проверяет результат и печатает `nestPrivate=policy`. Доступ разрешён из тела объемлющего верхнеуровневого типа; `private`-метод не наследуется и не является публичным API. Это не исходный `api.Policy` с методом `private static format`. При исправлении B2c исходные восемь Java-файлов, семь проверок, десять строк и `expected.stdout` не менялись. Именно при исправлении B2c была добавлена одна самостоятельная проверка с `AssertionError`, не тест JUnit. В D-A09 усилена только основная программа; эталон и самостоятельный блок B2c сохранены.

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
| [BadExternalPrivateAccess.java](negative/BadExternalPrivateAccess.java) | `policy.helper()` | Экземпляр и публичный вложенный тип доступны, но другой верхнеуровневый класс не имеет доступа к `private`-методу |
| [BadTypeQualifiedInstance.java](negative/BadTypeQualifiedInstance.java) | `Policy.helper()` | В объемлющем классе доступ к `private`-методу разрешён, но экземплярный метод вызывается через имя типа без экземпляра |

После положительного запуска (создайте выходные каталоги заранее):

~~~sh
mkdir -p examples/core/interfaces-composition/build/negative/BadExternalPrivateAccess examples/core/interfaces-composition/build/negative/BadTypeQualifiedInstance
javac --release 21 -XDrawDiagnostics -cp examples/core/interfaces-composition/build/private-classes -d examples/core/interfaces-composition/build/negative/BadExternalPrivateAccess examples/core/interfaces-composition/negative/BadExternalPrivateAccess.java
javac --release 21 -XDrawDiagnostics -cp examples/core/interfaces-composition/build/private-classes -d examples/core/interfaces-composition/build/negative/BadTypeQualifiedInstance examples/core/interfaces-composition/negative/BadTypeQualifiedInstance.java
~~~

**Фактическая локальная проверка роли 03** ([отчёт с командами и результатами](https://github.com/Melchiorius/backend-interview-labs/pull/275#issuecomment-7156019201), технический HEAD `05e810cf6e97b5a0051f744e2433f0964fe745c6`): на Debian OpenJDK/javac 21.0.12.1 обе изолированные компиляции дали код завершения 1, пустой stdout и одну ожидаемую ошибку: `compiler.err.report.access: helper(), private, client.InterfacePrivateAccessDemo.Policy` и `compiler.err.non-static.cant.be.ref: kindname.method, helper()` соответственно. Причины различны, не отсутствие типа. JLS 21 [§6.6.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6.1), [§9.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4.1), [§15.12.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.3). Конкретные диагностические ключи и аргументы относятся к javac, не гарантия формата JLS.

Runner сохраняет исходное сравнение expected.stdout, отдельно компилирует и запускает B2c в private-classes, сравнивает его одну строку с отдельным временным эталоном и проверяет каждую negative-компиляцию по exit=1, пустому stdout, категории/аргументам и одной ошибке. Строка nestPrivate=policy, EXPECTED REJECTION и диагностики печатаются **после** десяти исходных строк, но **до** последнего маркера CHECK PASSED: stdout matches expected.stdout. Они не входят в исходный expected.stdout.

## Что здесь намеренно не воспроизводится

Отдельные **некомпилируемые** случаи из плана (default/default без override, abstract/default, недопустимый `InterfaceName.super`, override-equivalent `Object`-метод) должны оставаться самостоятельными короткими примерами в статье CORE-INTERFACES: они не входят в рабочую компиляцию и не выдают общий failure `javac` за точное доказательство JLS. Специализированные наследование конфликтующих generics, sealed interfaces, Spring DI, mutable lifecycle, concurrency и продакшен-тестирование — за пределами этого runnable.

**Историческая локальная проверка исходного сценария роли 03:** OpenJDK `21.0.12.1` / `javac 21.0.12.1`, из корня запущено `bash examples/core/interfaces-composition/verify.sh` — exit code `0`, все **10 строк** stdout совпали с `expected.stdout`, после сравнения выведено `CHECK PASSED: stdout matches expected.stdout`. Сохранённые Java-файлы, `verify.sh` и `expected.stdout` сверены с опубликованными Git blob SHA (10/10 совпадений). Для другого поставщика JDK, UI Obsidian/IntelliJ и локального Gradle/JUnit — **not run**. GitHub Actions нужно проверять отдельно на **фактическом SHA конкретного PR**: успешный запуск предыдущего HEAD не доказывает прохождение CI после новых изменений.

**B2c — локальная проверка роли 03:** [отчёт #275](https://github.com/Melchiorius/backend-interview-labs/pull/275#issuecomment-7156019201), технический HEAD `05e810cf6e97b5a0051f744e2433f0964fe745c6`, Debian OpenJDK/javac 21.0.12.1. На исходной и итоговой версиях `LC_ALL=C bash -n` и полный запуск `verify.sh` завершились с кодом 0, stderr скрипта пуст. Исходные десять строк `actual.stdout` совпали побайтно с предыдущей версией и `expected.stdout`; прежние 11 строк вывода скрипта (включая финальный маркер) сохранены, новый блок добавил 7 строк. Отдельные компиляция и запуск положительного примера завершились с кодом 0; две изолированные отрицательные компиляции — с кодом 1 по указанным выше причинам. Проверены 10/10 исходных и 13/13 итоговых исполняемых и входных файлов по Git blob SHA; режим `verify.sh` 100755 сохранён. Роль 03 локально не запускала Gradle/JUnit, полный чистый клон и GUI. Это локальная проверка технической версии, а не результат CI. [Интеграционный CI #45569888313](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45569888313) завершился успешно на предыдущем интеграционном HEAD `05f1f3c725147428fa2ae40a9564550a9f47d9fb`; для каждой следующей редакции результат CI проверяется отдельно.
