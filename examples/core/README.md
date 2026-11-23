# Java Core — воспроизводимые примеры

Примеры здесь не являются обязательными упражнениями. Это короткие самостоятельные программы, которые полезно запускать и изменять, чтобы сопоставлять результат с текстом статьи.

## Типы, переменные и значения

[TypesAndVariablesDemo.java](TypesAndVariablesDemo.java) относится к [CORE-TYPES](../../knowledge/01-java-core/types.md). Он сравнивает копирование значений и ссылок, инициализацию полей и массивов, `var`, `final`, переполнение `int` и Unicode.

Из корня репозитория, JDK 21:

```powershell
java examples/core/TypesAndVariablesDemo.java
```

Пример заканчивается строкой `CHECK PASSED`, иначе выбрасывает `AssertionError`. Запуск этого примера отдельно проверяется в GitHub Actions.

## Преобразования, упаковка, перегрузки

[ConversionsDemo.java](ConversionsDemo.java) дополняет [CORE-CONVERSIONS](../../knowledge/01-java-core/conversions.md): проверяет константное сужение, составное присваивание, кэш `Integer`, разницу `Integer == Integer` и `Integer == int`, `null` при распаковке, выбор перегрузки, переполнение и String Pool.

Из корня репозитория с JDK 21:

```powershell
java examples/core/ConversionsDemo.java
```

Программа заканчивается строкой `CHECK PASSED`. Некомпилируемые фрагменты специально оставлены **закомментированными**, а не включены в программу. Пример также запускается в GitHub Actions.

## Передача аргументов, массивов, varargs

[ParametersDemo.java](ParametersDemo.java) сопровождает [CORE-PARAMS](../../knowledge/01-java-core/parameters.md). Он проверяет передачу примитива и ссылки, изменение объекта и массива, `swap` параметров, `String` и `Integer`, `varargs`, выбор перегрузки по объявленному типу и порядок вычисления аргументов.

**Из корня репозитория (JDK 21):**

```powershell
java examples/core/ParametersDemo.java
```

Результат заканчивается строкой `CHECK PASSED`, иначе выбрасывается `AssertionError`. Пример включён в CI.

## Классы, состояние и конструкторы

[ClassesAndObjectsDemo.java](ClassesAndObjectsDemo.java) связан со статьёй [CORE-CLASSES](../../knowledge/01-java-core/classes.md). Здесь два разных объекта счёта, две ссылки на один экземпляр, перегруженные конструкторы, `static`-счётчик и проверка инвариантов (ошибочные данные и переполнение).

Из корня репозитория на JDK 21:

```powershell
java examples/core/ClassesAndObjectsDemo.java
```

Программа должна завершиться строкой `CHECK PASSED`. Дополнительная проверка через `javac` также поддерживается; изолированные примеры не требуют Gradle. Запуск примера добавлен в GitHub Actions.

## Значения и ссылки

[ValuesAndReferencesDemo.java](ValuesAndReferencesDemo.java) относится к трём статьям:

- [Типы и значения](../../knowledge/01-java-core/types.md)
- [Преобразования типов](../../knowledge/01-java-core/conversions.md)
- [Передача аргументов](../../knowledge/01-java-core/parameters.md)

**Из корня репозитория, JDK 21:**

```powershell
java examples/core/ValuesAndReferencesDemo.java
```

Ожидаемые последние строки:

```text
After append: AB
Inside replace: C
After replace: AB
After reassigning second: AB
CHECK PASSED
```

Программа завершится с ошибкой `AssertionError`, если ожидаемое поведение нарушено. Для запуска не нужны Gradle, сторонние зависимости и подключение к интернету после установки JDK.

Код намеренно оставлен в одном файле: разносить его по классам или создавать отдельный Gradle-модуль ради демонстрации трёх операций нет смысла.

## Правила доступа между пакетами

[CORE-ACCESS — многофайловый Java 21 пример](access/README.md) демонстрирует `protected` между пакетами, отличие экземплярного доступа от `protected static`, специальные правила конструктора и отдельно проверяемые ошибки компиляции. Готового упражнения нет; независимые команды описаны в README примера.

## Наследование, перегрузка и переопределение

[CORE-INHERITANCE — многофайловый пример Java 21](inheritance/README.md) различает compile-time overload и runtime override, скрытие статических методов и полей, `super`, ковариантные результаты и checked `throws`. Двенадцать отрицательных файлов компилируются отдельно, без создания обязательного упражнения.

## Порядок инициализации классов и объектов

[CORE-INIT — автономный пример Java 21](initialization/README.md) показывает class initialization (constant variable, унаследованное static-поле, superinterfaces с default methods) и instance initialization (default values, superclass constructor, virtual dispatch, делегирование this-конструктора и инициализаторы подкласса). Один исходник, полный ожидаемый stdout и проверка diff; отдельный Gradle-модуль не требуется.

## Равенство, хэши и ключи коллекций

[CORE-EQUALITY — воспроизводимый пример Java 21](equality/README.md) разбирает правильный immutable value key, коллизию hash без равенства, нарушения `equals/hashCode`, асимметрию при наследовании, изменяемый ключ и различие `HashSet`/`TreeSet`. Один Java-файл, полный ожидаемый stdout и команда `bash examples/core/equality/verify.sh`; отдельного упражнения и Gradle-подпроекта нет.

## Интерфейсы и композиция

[CORE-INTERFACES + CORE-COMPOSITION — runnable Java 21](interfaces-composition/README.md) показывает интерфейсную `Policy`, две реализации, декоратор с явным делегированием, `Service(Policy)`, а также независимые случаи разрешения конфликтующих `default` и приоритета метода класса. Воспроизводимый stdout и отдельный запуск `bash examples/core/interfaces-composition/verify.sh`; без Gradle-модуля и обязательного упражнения.

## Вложенные классы, захват переменных и лямбды

[CORE-NESTED + CORE-FUNCTIONS — автономный Java 21 пример](nested-functions/README.md): static nested / inner / local / anonymous, `Outer.this`, effectively-final capture, `this` у anonymous и lambda, четыре вида method references и ранний NPE bound reference. Один исходник, полный stdout из 17 строк и `bash examples/core/nested-functions/verify.sh`; без Gradle-подпроекта и обязательного упражнения.

## Generics, PECS и erasure

[CORE-GENERICS + CORE-ERASURE — автономный Java 21 пример](generics-erasure/README.md) дополняет две статьи: [CORE-GENERICS](../../knowledge/01-java-core/generics.md) (bounds, invariance, PECS/capture) и [CORE-ERASURE](../../knowledge/01-java-core/erasure.md) (raw/unchecked, runtime casts, reifiability, bridge). Пример связывает эти механизмы, включая Java 21 `instanceof`, `Collections.copy`, `ClassCastException` при намеренном raw heap pollution и `ArrayStoreException` у ковариантного массива. Один исходник, полный stdout (18 строк) и точный `bash examples/core/generics-erasure/verify.sh`; без отдельного Gradle-проекта.

## Исключения и управление ресурсами

[CORE-EXCEPTIONS + CORE-RESOURCES — один Java 21 runnable](exceptions-resources/README.md) демонстрирует `finally` и потерю первичного исключения, цепочку `getCause()`, TWR с двумя ресурсами, primary/suppressed exceptions, partial initialization, existing effectively-final resource и порядок `close` перед внешним `finally`. Один источник, точный stdout P01–P20 и `bash examples/core/exceptions-resources/verify.sh`, без Gradle-модуля и отдельного упражнения.

## Stream API: pipeline, collectors и reduce

[CORE-STREAMS + CORE-STREAM-ADV — один воспроизводимый Java 21 пример](streams/README.md) объединяет два G1-сценария: StreamPipelineProbes P01–P33 и StreamG1Clarifications C01–C11. Один `bash examples/core/streams/verify.sh` компилирует оба исходника и сравнивает полный stdout (44 наблюдения двух запусков). Лень вычислений, `peek/count`, `toList`/коллекторы, `reduce` без/с identity, Optional и parallel без обещаний scheduler/JMH.

## Массивы, строки и буферы

[CORE-ARRAYS + CORE-STRINGS/BUILDERS — два Java 21 запуска](arrays-strings/README.md) связывает типы и ковариантность массивов, копирование и `Arrays.asList` с UTF-16/code points, строковыми перегрузками и мутабельными `StringBuilder`/`StringBuffer`. Один `bash examples/core/arrays-strings/verify.sh` компилирует два G1-класса и сравнивает 24 A-строки и 28 S-строк с двумя эталонами. Без отдельного Gradle/JUnit-модуля.

## Records и наблюдаемая неизменяемость

[CORE-RECORDS + CORE-IMMUTABLE — общий Java 21 runnable](records-immutability/README.md) сопоставляет generated equality/constructors record и намеренный антипример copy invariant с владением ссылками, `List.copyOf` snapshot, `Collections.unmodifiableList` live view и defensive copy-in/out. Один `bash examples/core/records-immutability/verify.sh` запускает две программы G1 и сравнивает R01–R16 / I01–I10 с эталонами, без отдельного Gradle/JUnit-модуля.

## Sealed-иерархии и pattern matching (Java 21)

[CORE-SEALED + CORE-PATTERNS — общий runnable](sealed-patterns/README.md) дополняет статьи [CORE-SEALED](../../knowledge/01-java-core/sealed.md) и [CORE-PATTERNS](../../knowledge/01-java-core/patterns.md): разрешённые прямые и косвенные наследники, exhaustive `switch`, flow scope, `when`, null selectors/components и runtime `MatchException`. Две самостоятельные программы, 11+20 подписанных строк stdout, один `bash examples/core/sealed-patterns/verify.sh`; без Gradle/JUnit-модуля.

## Перечисления и управление потоком Java 21

[CORE-ENUMS + CORE-CONTROL — общий runnable](enums-control/README.md) дополняет статьи [CORE-ENUMS](../../knowledge/01-java-core/enums.md) и [CORE-CONTROL](../../knowledge/01-java-core/control-flow.md). Две самостоятельные программы воспроизводят полные positive-примеры G1: E01–E30 и C01–C21. Один запуск `bash examples/core/enums-control/verify.sh` компилирует оба исходника на Java 21, запускает два независимых JVM-процесса и сравнивает 30+21 строку stdout с отдельными эталонами; без Gradle/JUnit-модуля.

## Синтаксис и операторы Java 21

[CORE-SYNTAX + CORE-OPERATORS — общий runnable](syntax-operators/README.md) дополняет [CORE-SYNTAX](../../knowledge/01-java-core/syntax.md) и [CORE-OPERATORS](../../knowledge/01-java-core/operators.md): shadowing/scope, реальный package/static-import, precedence/evaluation order, side effects, overflow и short-circuit. Четыре Java21 исходника, три JVM-процесса, 14+27+3 строки stdout; запуск: `bash examples/core/syntax-operators/verify.sh`. Без отдельного Gradle/JUnit-модуля.

## Assertions и Optional (Java 21)

[CORE-ASSERT + CORE-OPTIONAL — воспроизводимый runner](assert-optional/README.md) дополняет [CORE-ASSERT](../../knowledge/01-java-core/assertions.md) и [CORE-OPTIONAL](../../knowledge/01-java-core/optional.md). Один AssertProbe запускается в двух независимых JVM (default и `-ea`) с разными stdout-эталонами по 15 строк; отдельный OptionalProbe показывает eager/lazy fallback, map/flatMap и runtime exceptions (36 строк). Запуск: `bash examples/core/assert-optional/verify.sh`. 66 подписанных наблюдений трёх процессов, не 66 unit-тестов; без отдельного Gradle-модуля.

## Аннотации, Reflection и JPMS (Java 21)

[CORE-ANNOTATIONS + CORE-REFLECTION — воспроизводимый runnable](annotations-reflection/README.md) связывает [Annotations](../../knowledge/01-java-core/annotations.md) и [Reflection](../../knowledge/01-java-core/reflection.md) с двумя полными Java21-программами и реальными named modules `m.target`/`m.client` в двух вариантах: `exports api` vs `exports api; opens api to m.client`. Один `bash examples/core/annotations-reflection/verify.sh` выполняет 3 компиляции и 4 JVM-процесса с 4 полными сравнениями stdout (**23+34+3+3=63 наблюдения**, не JUnit-тесты); без нового Gradle-модуля.

## JPMS и ClassLoader (Java 21)

[CORE-MODULES + CORE-CLASSLOADER — воспроизводимый runner](modules-classloader/README.md) дополняет [статью о модулях](../../knowledge/01-java-core/modules.md) и [статью о загрузчиках классов](../../knowledge/01-java-core/classloaders.md). Один скрипт `bash examples/core/modules-classloader/verify.sh` воспроизводит два самостоятельных named-module graphs (exports vs opens), `requires static` с отсутствующим/разрешённым модулем, два defining `ChildFirstTokenLoader`, `ClassCastException` и реальную временную runtime-копию без superclass: **4 javac, 6 JVM, 6 точных stdout fixtures, 49 строк** — не JUnit-тестов.

## Числа и дата/время (Java 21)

[CORE-NUMBERS + CORE-DATETIME — два самостоятельных Java21 runnable](numbers-datetime/README.md) дополняет [теорию чисел](../../knowledge/01-java-core/numbers.md) и [`java.time`](../../knowledge/01-java-core/datetime.md). Скрипт `bash examples/core/numbers-datetime/verify.sh` выполняет одну чистую компиляцию, две независимые JVM и два полных `diff -u` со сверкой **33+38=71 подписанной строки stdout** и G1 SHA-256; DST-трасса зависит от версии tzdb. Без нового Gradle/JUnit-модуля.

## Регулярные выражения и утилиты Java 21

[CORE-REGEX + CORE-UTIL — два самостоятельных runnable](regex-util/README.md) связывает [теорию регулярных выражений](../../knowledge/01-java-core/regex.md) и [утилиты JDK](../../knowledge/01-java-core/utilities.md) с двумя полными Java21 программами: `RegexProbe` (33 RG) и `UtilProbe` (38 UT). Команда `bash examples/core/regex-util/verify.sh` выполняет **одну чистую компиляцию и две отдельные JVM**, сравнивает оба полных stdout и их G1 SHA-256 (**71 наблюдение, не 71 JUnit-тест**). Отдельные 14 compiler-negative описаны в [G1 §7](../../docs/plans/CORE-REGEX-UTIL.md) и не подменяются runtime exception-примерами.
