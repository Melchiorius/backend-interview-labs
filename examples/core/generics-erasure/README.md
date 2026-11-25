# CORE-GENERICS + CORE-ERASURE — граница проверки типов в Java 21

**Один автономный runnable** связывает два уровня: *проверку generic-ограничений компилятором* и *фактические проверки типов во время выполнения*. Здесь используются те же `List` и небольшие generic-классы, чтобы на примерах показать PECS/wildcard capture, type erasure, raw heap pollution, checked `instanceof` и ковариантность массивов. Отдельного упражнения/Gradle-модуля нет.

**Исходники и проверки:** [GenericsErasureDemo.java](src/GenericsErasureDemo.java), [полный эталон expected.stdout](expected.stdout), [автономный verify.sh](verify.sh). Сценарий спроектирован по [принятому G1-плану GE-01–GE-30](../../../docs/plans/CORE-GENERICS-ERASURE.md), но **не является побайтной копией** исследовательского `GenericsProbes.java`: его 20 наблюдений/P21 и отдельные диагностические пробы сохранены в плане; основной пример здесь переработан ради читаемости и воспроизводимости без compiler-specific числа bridge-методов.

**Связанные теоретические статьи:** [CORE-GENERICS — границы, инвариантность, PECS и wildcard capture](../../../knowledge/01-java-core/generics.md) и [CORE-ERASURE — стирание, raw/unchecked, runtime casts и bridge](../../../knowledge/01-java-core/erasure.md). Этот runnable — одна дидактическая компоновка обоих механизмов; он **не заменяет** полные контрпримеры и нормативные пояснения статей. Предпосылки: [типы и значения](../../../knowledge/01-java-core/types.md), [наследование и overriding](../../../knowledge/01-java-core/inheritance.md), [массивы и передача ссылок](../../../knowledge/01-java-core/parameters.md).

Нормативные ориентиры (Java SE 21): [JLS §4.5.1 — wildcard](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.5.1), [§4.6 — erasure](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.6), [§4.7 — reifiable](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.7), [§4.12.2 — heap pollution](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.2), [§5.1.10 — capture conversion](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.10) и [`Collections.copy` API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#copy(java.util.List,java.util.List)).

## Команды из корня репозитория

Требуется JDK **21** с `javac` и `java` в `PATH`. В Bash (Linux/macOS/Git Bash на Windows):

```sh
mkdir -p examples/core/generics-erasure/build/classes
javac --release 21 -Xlint:all -Werror -d examples/core/generics-erasure/build/classes examples/core/generics-erasure/src/GenericsErasureDemo.java
java -cp examples/core/generics-erasure/build/classes GenericsErasureDemo
bash examples/core/generics-erasure/verify.sh
```

Скрипт `verify.sh` пересоздаёт **только** `examples/core/generics-erasure/build/`, компилирует с `--release 21` и строгим `-Xlint:all -Werror`, запускает программу и сравнивает **полный stdout** с `expected.stdout` через `diff -u`. При несовпадении завершается с ненулевым кодом. При успехе после полного stdout Java добавляет собственную строку `CHECK PASSED: stdout matches expected.stdout` — она **не входит** в эталон. В PowerShell можно создать папку `New-Item -ItemType Directory -Force examples/core/generics-erasure/build/classes` и выполнить те же `javac`/`java`, а для скрипта использовать Git Bash.

## Полный stdout Java-программы

```text
== Compile-time generic constraints ==
bounded NumberBox: 67
bounded compare: -1
PECS Collections.copy: [67, 68]
wildcard extends read/null: 67/2
wildcard super write/Object read: 67
wildcard capture helper: [a, a]
generic method inference: typed
== Erasure and runtime boundaries ==
same runtime List class: true
instanceof List<?>: true
Java 21 checked instanceof: true
erased bound/return: Number/Object
raw heap pollution: ClassCastException
covariant array store: ArrayStoreException
reifiable List<?>[] element: 67
override through Parent<String>: child
CHECK PASSED
```

Всего **18 строк одного запуска**, а не 18 независимых тестовых классов. Дополнительно исходник вызывает собственный `verify(...)` для проверки ключевых значений и выбрасывает `AssertionError` при нарушении ожиданий; флаг `-ea` не требуется.

## Что именно проверяется и почему

| Наблюдение | Причина и граница |
|---|---|
| `bounded NumberBox: 67`, `bounded compare: -1` | `NumberBox<T extends Number>` хранит значение типа `T`. В `compare(T extends Number & Comparable<T>)` доступны операции из обеих границ; `Integer.compareTo(68)` при 67 даёт отрицательный результат. Это **compile-time** гарантия доступных методов, не runtime-проверка параметра `T`. |
| `PECS Collections.copy: [67, 68]` | Источник — `List<Integer>`, получатель — заранее заполненный `List<Number>`. `Collections.copy` разрешает source как producer (`? extends T`), destination как consumer (`? super T`); он **не добавляет элементы и не расширяет** destination, поэтому он заранее имеет размер 2. Дополнительный `copyFirst` показывает те же ограничения в собственной сигнатуре, но проверяется на отдельном изменяемом получателе `[0, 0]`: после вызова ожидается `[67, 0]`. Исходный получатель `Collections.copy` независимо проверяется на `[67, 68]` и по-прежнему используется для вывода. |
| `wildcard extends read/null: 67/2` | `List<? extends Number>` даёт чтение как `Number`, но не разрешает записать произвольный `Integer` или `Number`; **`add(null)` компилируется**. В примере объект — изменяемый `ArrayList<Integer>`, поэтому размер становится 2. Это не доказательство, что все producer-коллекции допускают изменения: `List.of` был бы immutable. |
| `wildcard super write/Object read: 67` | В `List<? super Integer>` разрешена запись `Integer`; без дополнительных сведений статический тип результата `get` — **`Object`** (именно так объявлен `readBack`). Динамический объект здесь `Integer`, что не повышает гарантию типа сигнатуры. |
| `wildcard capture helper: [a, a]` | Метод `repeatFirst(List<?>)` вызывает generic-helper `<T> repeatFirstCaptured(List<T>)`: при одном захваченном неизвестном `T` допустимо прочитать первый элемент и записать его на вторую позицию того же списка. Это не разрешение вставлять любой новый `T` и не обход invariant type checking. |
| `generic method inference: typed` | `identity("typed")` выводит `T = String` по аргументу, возвращаемый результат статически используется как `String`. После erasure runtime-проверка типа при необходимости остаётся возможной. |
| `same runtime List class: true` | Два реально созданных `ArrayList<String>` и `ArrayList<Integer>` имеют один runtime-класс `ArrayList`. Это **не** означает, что объекты имеют одинаковое содержимое или что вся информация о generic-сигнатурах исчезает из Reflection metadata. |
| `instanceof List<?>: true` и `Java 21 checked instanceof: true` | `List<?>` — reifiable wildcard-параметризация. Кроме того, в Java 21 выражение `strings instanceof ArrayList<String>` компилируется, потому что статический `List<String>` допускает **checked cast compatible** narrowing до `ArrayList<String>`. Не обобщать это на `Object obj; obj instanceof List<String>`: он не компилируется. Во время выполнения JVM проверяет класс, **не** тип элементов. |
| `erased bound/return: Number/Object` | Reflection API возвращает raw `Class` для return type: `NumberBox<T extends Number>.get()` после erasure имеет return `Number`, generic `identity(T)` — `Object`. Это не отменяет существование generic signature metadata в classfile и Java Reflection. |
| `raw heap pollution: ClassCastException` | Метод `polluteThroughRaw` специально создаёт raw alias `List` для `List<String>` и добавляет `Integer(67)`. **Тип списка в Java не сменился:** нарушена гарантия `String`, которую компилятор предполагает при `polluted.get(0)`. Здесь при необходимости выполняется checkcast к `String`, он и вызывает **`ClassCastException` в строке чтения**, а не при `add`. Узкая `@SuppressWarnings({"rawtypes","unchecked"})` лишь подавляет ожидаемую диагностику внутри **намеренно некорректного** helper: это не исправление безопасности. |
| `covariant array store: ArrayStoreException` | `Number[] covariant = new Integer[1]` допустим благодаря runtime-проверяемой ковариантности массивов. Но присвоение `3.14` (= `Double`) не разрешено реальным массивом `Integer[]`. Это другой механизм, чем heap pollution generic-коллекции. |
| `reifiable List<?>[] element: 67` | Массив `List<?>[]` допустим как массив reifiable элементов; при чтении неизвестный тип элемента `List<?>` даёт `Object`. Это **не** разрешение `new List<String>[1]`. |
| `override through Parent<String>: child` | Вызов `Parent<String>.get()` на реальном `Child` выбирает переопределение `Child.get()`. На javac 21 такое переопределение может требовать **bridge method** на уровне байткода; конкретное число reflection-методов, их порядок или точная схема синтеза **не включены** в фиксированный stdout и не объявляются требованием JLS. |

## N21 — независимая проверка `copyFirst`

Ранее `copyFirst` вызывался для списка, который уже был заполнен `Collections.copy`. Проверка `[67, 68]` не обнаруживала пустое тело helper. Теперь сохранены все десять прежних проверок `verify(...)` и добавлена одиннадцатая: первый элемент отдельного списка должен измениться с 0 на 67, второй — остаться равным 0. Новая проверка выполняется до вывода, исходные 18 строк `expected.stdout` и скрипт не меняются.

В локальном дифференциальном эксперименте роли 03 (Debian OpenJDK/javac 21.0.12.1, `LC_ALL=C`) во временных копиях исходной и исправленной версий удалена только строка `target.set(0, source.get(0));`. Все четыре варианта прошли строгую компиляцию `javac --release 21 -Xlint:all -Werror` без диагностик. Исходная версия и её мутант, а также исправленная версия завершились с кодом 0 и одинаковым выводом скрипта (19 строк, 618 байт); исправленный мутант завершился с кодом 1 и `AssertionError: copyFirst must replace only the first element` до вывода программы. Мутанты не публикуются. Это проверка обнаружения конкретного дефекта, а не доказательство полной корректности `copyFirst` или результат CI. [Критерий N21 и поручение роли 03](https://github.com/Melchiorius/backend-interview-labs/pull/309#issuecomment-7172427294).

## Что не является частью runnable

В исследовательском плане §6 — **22** независимых некомпилируемых фрагмента, §7 — **4** отдельные lint/unchecked/varargs-программы. Их нельзя включать в общий successful `javac` вместе с этой программой. Основной пример **не проверяет** точные тексты компиляторских diagnostics, все generic bounds, name clash, `new T()`, запрещённое создание generic arrays, safety `@SafeVarargs` или все варианты heap pollution. Их объясняют статьи и отдельные пробы по G1. Конкретные warnings `-Xlint:rawtypes` и `-Xlint:unchecked` можно увидеть, убрав временно `@SuppressWarnings` с *намеренно небезопасного* helper в личной копии исходника, не превращая такой вариант в обязательный CI-файл.

**Границы проверки:** `verify.sh` компилирует и запускает только этот пример Java 21 с `javac --release 21 -Xlint:all -Werror`, сверяя все 18 строк stdout с `expected.stdout`. Это не JUnit suite, benchmark и не доказательство общей производительности или типовой безопасности произвольного API. **Скрипт не запускает** 22 отдельные compile-negative N01–N22 и четыре lint-пробы W01–W04 из G1-плана. Роль 02 отдельно сообщила, что воспроизвела эти 22 и 4 случая на OpenJDK/javac 21.0.12.1; это не CI и не проверки данного скрипта. Реальные клики Obsidian/IntelliJ, другие JVM, число/расположение bridge, classfile/JIT, concurrency/JMM и performance также не проверены этим скриптом. `COVERAGE.md` и `CURRICULUM.md` до публикации не меняются.
