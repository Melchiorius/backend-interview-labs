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
