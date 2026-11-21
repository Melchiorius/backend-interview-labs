# CORE-GENERICS + CORE-ERASURE — воспроизводимый пример Java 21

**Один запускаемый пример** объединяет статические ограничения generics с тем, где после стирания действительно срабатывают runtime-проверки. Файл [GenericsProbes.java](src/GenericsProbes.java) и **20 строк P01–P20** из [§5 принятого G1-плана](../../../docs/plans/CORE-GENERICS-ERASURE.md) сохранены без изменения программной логики и вывода. Это *одна компиляция и один запуск*, не 20 отдельных тестов. Небезопасный код присутствует намеренно для объяснения ошибок.

**Файлы:** [GenericsProbes.java](src/GenericsProbes.java), [expected.stdout](expected.stdout), [verify.sh](verify.sh). **Связанные статьи:** [CORE-GENERICS — типизация, инвариантность и PECS](../../../knowledge/01-java-core/generics.md) и [CORE-ERASURE — стирание, unsafe generics и runtime-проверки](../../../knowledge/01-java-core/erasure.md). Предпосылки: [типы](../../../knowledge/01-java-core/types.md), [передача аргументов и массивы](../../../knowledge/01-java-core/parameters.md), [наследование](../../../knowledge/01-java-core/inheritance.md), [интерфейсы](../../../knowledge/01-java-core/interfaces.md). Обе статьи расположены рядом с примером в том же дереве репозитория.

## Точные команды из корня репозитория

Нужны `java` и `javac` из **JDK 21**. Для Bash (Linux, macOS или Git Bash):

```sh
mkdir -p examples/core/generics-erasure/build/classes
javac --release 21 -Xlint:unchecked -Xlint:varargs -d examples/core/generics-erasure/build/classes examples/core/generics-erasure/src/GenericsProbes.java
java -cp examples/core/generics-erasure/build/classes GenericsProbes
bash examples/core/generics-erasure/verify.sh
```

Скрипт `verify.sh` пересоздаёт **только** `examples/core/generics-erasure/build/`, компилирует Java с включённой диагностикой двух важных категорий, запускает класс и сравнивает **весь stdout** с [эталоном](expected.stdout) через `diff -u`. При несоответствии или неуспешном запуске он возвращает ненулевой код. При успехе после 20 строк Java выводит дополнительную строку `CHECK PASSED: stdout matches expected.stdout`; это **строка скрипта**, не программы.

### Почему компилятор выдаёт предупреждения

На **OpenJDK/javac 21.0.12.1** при `-Xlint:unchecked -Xlint:varargs` исходный `GenericsProbes.java` компилируется успешно (exit 0) с **двумя ожидаемыми предупреждениями**: `[varargs]` для `Object[] alias = lists` в опасном методе с `@SafeVarargs` и `[unchecked]` для `raw.add(67)`. `@SafeVarargs` не делает тело метода безопасным; здесь оно намеренно нарушает generic-контракт. **Не применять `-Werror` к этому исследовательскому исходнику**: ожидаемая диагностика — часть опыта. Точные диагностические строки/пути/число предупреждений не являются требованием JLS; `verify.sh` не сравнивает `stderr` с нормативным эталоном.

## Полный ожидаемый stdout Java-программы

```text
P01=67
P02=-1
P03=[67, 68]
P04=67
P05=67,2
P06=67,Integer
P07=[a, a]
P08=true
P09=true
P10=Number
P11=Object
P12=ClassCastException
P13=ClassCastException
P14=ArrayStoreException
P15=x,x
P16=67
P17=2,1,child
P18=0,67
P19=68
P20=true,67
```

## Разбор механизмов

| Строки | Откуда берётся результат |
|---|---|
| `P01–P02` | `Box<Integer>` возвращает 67; `compare` с двумя bounds `Number & Comparable<T>` сравнивает 67 и 68, результат отрицателен. |
| `P03–P04` | `Collections.copy(List<? super T>, List<? extends T>)` переносит значения из `List<Integer>` в **заранее заполненный** `List<Number>`, не увеличивая размер destination. `copyOne` подтверждает направление чтения/записи. При этом `List<Integer>` не является `List<Number>`: инвариантность не отменяется. |
| `P05–P06` | В `List<? extends Number>` читаем `Number` и можем добавить `null` (изменяемый `ArrayList`, а не `List.of`); `List<? super Integer>` разрешает добавить Integer, но обычное чтение имеет статический тип `Object`, хотя фактический объект — Integer. |
| `P07` | `captureHelper(List<T>)` захватывает один неизвестный wildcard-тип и переносит элемент внутри *того же* списка. Это не разрешение добавлять произвольные значения в `List<?>`. |
| `P08–P09` | `ArrayList<String>` и `ArrayList<Integer>` имеют одинаковый runtime-класс; `Object instanceof List<?>` компилируется (reifiable wildcard), но не проверяет тип элементов. |
| `P10–P11` | `getReturnType()` показывает *erased* возвращаемый класс: `T extends Number` → `Number`; необязанный `T` → `Object`. Generic signature metadata при этом может оставаться доступным через Reflection. |
| `P12` | Raw alias позволяет записать Integer в `List<String>`. Вызов `add` завершается, а `ClassCastException` возникает **при чтении как String**, где требуется runtime cast. |
| `P13` | В `@SafeVarargs unsafeFirst` значение `List<Integer>` подменяет `List<String>` через `Object[] alias`; массив видит совместимый runtime-тип `List`, а следующая проверка String приводит к пойманному `ClassCastException`. Аннотация не исправляет небезопасное тело. |
| `P14` | `Number[] values = new Integer[1]` допустимо благодаря ковариантности, но `values[0] = 3.14` отвергается **runtime array store check** (реальный тип — Integer[]). |
| `P15–P16` | `List<?>` и `List<? extends Object>` читаются как Object; `List<?>[]` допустим как массив reifiable-типов. Это не делает допустимым `new List<String>[1]`. |
| `P17` | `Parent<String>.get()` и `Child.get()` демонстрируют переопределение после erasure и наблюдаемый bridge. **`2,1,child` — результат конкретного javac/OpenJDK 21**, а не JLS-гарантия числа/порядка методов Reflection. Эталон воспроизводит принятую G1-среду; другой компилятор может иметь иной допустимый bridge layout. |
| `P18–P20` | `Collections.emptyList()` пуст; `new ArrayList<>()` заполняется вручную; `Collections.copy` снова работает с уже созданной destination; `List<?>` допустим в `instanceof`, содержимое читается через Object. |

## Что проверяется отдельно

В G1-плане §5 есть дополнительная **P21** Java 21: `List<String> typed; typed instanceof ArrayList<String>` допустимо по **checked cast compatibility**. Она не добавлена к основному P01–P20 stdout, поскольку задача — сохранить первоначальный транскрипт. Контрпример `Object obj; obj instanceof List<String>` требует запрещённого unchecked narrowing cast.

**Не входят в этот runnable:** 22 отдельные ошибки компиляции N01–N22 из §6 и четыре lint-пробы W01–W04 из §7. Положительный source их не компилирует. Воспроизводимые ошибки не обязаны иметь один и тот же текст diagnostic на разных JDK.

**Этот `verify.sh` не проверяет:** реальные клики Obsidian/IntelliJ, альтернативные JDK, `javap -v`/точную структуру bridge, JVM/JIT/heap layout, 22 отдельных negative N01–N22, четыре самостоятельные lint-пробы W01–W04, concurrency и performance. Роль 02 отдельно сообщила о проверке **22 отрицательных случаев и четырёх lint-проб** на JDK 21.0.12.1; эти результаты не относятся к данному скрипту или CI. Корневой Gradle/JUnit запускается существующим GitHub CI, а не данным скриптом. `COVERAGE/CURRICULUM` и статьи роли 02 не изменяются.
