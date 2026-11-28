# CORE-RECORDS + CORE-IMMUTABLE — общий Java 21 runnable

**Один учебный набор двух Java-программ** связывает спецификационные механизмы record и отдельные случаи владения изменяемыми компонентами, aliasing и неизменяемых значений. Здесь **16 R-строк + 10 I-строк двух отдельных Java-процессов**, не 26 JUnit-тестов. Операторы взяты из принятого [G1 §5](../../../docs/plans/CORE-RECORDS-IMMUTABLE.md) без изменений; в заголовках source добавлены только комментарии Theory/Research.

Связанные статьи: [CORE-RECORDS](../../../knowledge/01-java-core/records.md) и [CORE-IMMUTABLE](../../../knowledge/01-java-core/immutability.md). Исходники: [RecordsProbe.java](src/RecordsProbe.java) и [ImmutableProbe.java](src/ImmutableProbe.java). Эталоны: [records.expected.stdout](records.expected.stdout) и [immutable.expected.stdout](immutable.expected.stdout).

## Запуск из корня репозитория

Нужны JDK **21**, Bash и `diff` (Windows: например, Git Bash). Команды:

```sh
java -version
javac -version
bash examples/core/records-immutability/verify.sh
```

Отдельные Java-программы можно запустить вручную:

```sh
mkdir -p examples/core/records-immutability/build/classes
javac --release 21 -Xlint:all -d examples/core/records-immutability/build/classes examples/core/records-immutability/src/RecordsProbe.java examples/core/records-immutability/src/ImmutableProbe.java
java -cp examples/core/records-immutability/build/classes RecordsProbe
java -cp examples/core/records-immutability/build/classes ImmutableProbe
```

Скрипт [verify.sh](verify.sh) удаляет **только** `examples/core/records-immutability/build/` (исключён из Git через `**/build/`), выполняет `javac --release 21 -Xlint:all` для двух исходников, **отвергает любое диагностическое сообщение javac на stderr**, затем запускает **два отдельных `java`** и сверяет оба полных stdout с эталонами посредством **двух `diff -u`**. При ошибке компиляции, предупреждении или отличии вывода выходит с ненулевым кодом. В конце успешного запуска напечатает ещё `CHECK PASSED: both stdout files match expected; javac emitted zero warnings` — это сообщение скрипта, не одна из 26 Java-строк.

## Полный stdout RecordsProbe (16 строк)

```text
R01=Ada:67
R02=true
R03=true
R04=true
R05=Bob
R06=age
R07=false
R08=true
R09=true
R10=true
R11=false
R12=ok
R13=false
R14=true
R15=person
R16=0
```

## Полный stdout ImmutableProbe (10 строк)

```text
I01=[A, B]
I02=[A]
I03=[A, B]
I04=UOE
I05=NPE
I06=67
I07=67
I08=false
I09=[1, 0, 0]
I10=empty
```

## Карта наблюдений

| Метки | Механизм и границы интерпретации |
|---|---|
| **R01–R06** | Canonical/compact constructor `Person` нормализует имя через `strip`, отвергает отрицательный возраст, дополнительный constructor делегирует через `this(...)`. `equals/hashCode/toString` генерируются из компонентов. R04 проверяет частное наблюдение вывода `toString`, а не обещание идентичной форматирующей строки на каждой реализации. |
| **R07–R09** | Generated `equals` для компонента `int[]` сравнивает ссылки, не содержимое: два массива `{1,2}` → `R07=false`; `Arrays.equals` → `R08=true`; одна и та же ссылка → `R09=true`. Record автоматически **не deep-immutable**. |
| **R10–R11** | У primitive `double` в generated record equality `NaN` равен `NaN`, но `-0.0` отличается от `+0.0`; не путать с простой арифметической операцией `==`. |
| **R12, R14–R16** | Generic record, nullable component, статическая константа `KIND`, вызов дополнительного конструктора с возрастом `0`. |
| **R13=false — намеренный АНТИПРИМЕР** | `BadAccessor` возвращает `values.clone()`; у реконструированной записи массив уже имеет **другую ссылку**, и стандартный `equals` отвергает копию. **Record copy invariant нарушен.** Так делать immutable record не рекомендуется; при необходимости защитного копирования массивов лучше спроектировать согласованное равенство или использовать обычный final class с copy-in/out. |
| **I01–I03** | `Unsafe` удерживает mutable List без copy-in, видит добавление `B`. `Safe` использует `List.copyOf`: snapshot ссылочной структуры на момент вызова. `Collections.unmodifiableList(source)` — **live backed view**, отражающий изменение `source`. |
| **I04–I05** | Модификация unmodifiable list вызывает `UnsupportedOperationException`; `List.copyOf` с null-элементом — `NullPointerException`. Это **runtime**, не Nxx compile-negative. |
| **I06** | Обычный `final class Numbers` использует **двусторонний defensive copy**: копирование при приёме массива и при выдаче `data()`. Изменения внешних копий не меняют `first()=67`. |
| **I07–I08** | `List.copyOf` не копирует изменяемые вложенные элементы: `MutableElement.n` меняется в списке. `I08=false` — наблюдение отдельных identity в этом примере, **не** обещание, что `List.copyOf` всегда создаёт новый объект. |
| **I09–I10** | `Arrays.copyOf` для primitive массива дополняет нулями; конструктор `Numbers` запрещает пустой массив (`IllegalArgumentException`). |

## Отдельные compile-negative и непроверенные границы

В [G1 §6](../../../docs/plans/CORE-RECORDS-IMMUTABLE.md) записаны **N01–N16**, изолированные Java 21 compile-negative единицы. Роли 04 и 02 ранее воспроизвели ожидаемые категории диагностики с `javac --release 21 -XDrawDiagnostics`, но **данный positive `verify.sh` их не запускает**; нет автоматического Nxx-harness и не следует путать Java compiler error с перехваченным runtime exception.

**Ничего в этом примере не доказывает** автоматически safe publication произвольного графа по JMM из-за `final`, глубокую неизменяемость `List.copyOf` или новую identity, неизменяемость `record` с mutable references, устойчивость к arbitrary reflection/serialization/ORM. Не проверены альтернативные JDK/OS, Obsidian/IntelliJ GUI, все Markdown code snippets, JMH/performance, GC/JIT, IO и concurrency stress. **Подтверждённое историческое свидетельство CI:** [GitHub Actions #44960925598](https://github.com/Melchiorius/backend-interview-labs/actions/runs/44960925598) завершился **completed/SUCCESS** на общем коммите `f6f38bb53606107b6f9cd5821eab95c5083b915d` [авторского PR #132](https://github.com/Melchiorius/backend-interview-labs/pull/132), включая два Java 21 запуска и строгую сверку 16+10 строк с эталонами через `verify.sh`. Этот результат относится **только к указанному SHA**; после любых последующих изменений требуется проверять CI нового HEAD. Ни запуск N01–N16 в CI, ни автоматическая компиляция всех Java-фрагментов статей, ни G3/G4/G5 из этого PASS не следуют. Для исходного PR #132 действовало историческое ограничение: до G5 `COVERAGE/CURRICULUM` не повышать.
