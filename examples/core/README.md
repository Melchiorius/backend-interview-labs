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

[CORE-GENERICS + CORE-ERASURE — автономный Java 21 пример](generics-erasure/README.md) связывает границы `T extends`, PECS, `Collections.copy`, wildcard capture, erasure/reflection, raw и varargs heap pollution с `ClassCastException`, ковариантный массив с `ArrayStoreException` и bridge при overriding. Один `GenericsProbes.java`, полный G1 stdout P01–P20, ожидаемые lint warnings и `bash examples/core/generics-erasure/verify.sh`; без отдельного Gradle-проекта.
