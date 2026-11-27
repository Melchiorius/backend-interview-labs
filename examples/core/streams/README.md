# CORE-STREAMS + CORE-STREAM-ADV — Java 21, два связанных запуска

**Один учебный runnable** состоит из двух автономных Java-исходников в общей папке, проверяемых одной командой. [StreamPipelineProbes.java](src/StreamPipelineProbes.java) демонстрирует **P01–P33** из §6 принятого [G1-плана](../../../docs/plans/CORE-STREAMS-ADV.md). [StreamG1Clarifications.java](src/StreamG1Clarifications.java) отдельно проверяет **C01–C11** из §5a, добавленные после исправления обязательных P0-требований ST-19 и ST-20. **44 подписанные строки — два Java-процесса и две последовательности наблюдений, а не 44 независимых теста.** Выражения и ожидаемый вывод исходно взяты из G1; в Java добавлены три комментария `// Theory/Research` в начале файлов, а в P19 позднее стабилизирована печать через отдельный `TreeMap`. Все остальные выражения и байты эталона сохранены.

Связанные статьи: [CORE-STREAMS](../../../knowledge/01-java-core/streams.md) и [CORE-STREAM-ADV](../../../knowledge/01-java-core/stream-advanced.md); из обеих статей можно перейти обратно к этому примеру. **Файлы примера:** [StreamPipelineProbes.java — P01–P33](src/StreamPipelineProbes.java), [StreamG1Clarifications.java — C01–C11](src/StreamG1Clarifications.java), [эталон полного stdout](expected.stdout), [скрипт проверки](verify.sh).

## Запуск из корня репозитория

Требуется JDK **21** с `javac` и `java` в `PATH`, Bash и `diff` (например, Git Bash на Windows). Из корня:

```sh
bash examples/core/streams/verify.sh
```

Либо вручную:

```sh
mkdir -p examples/core/streams/build/classes
javac --release 21 -d examples/core/streams/build/classes \
  examples/core/streams/src/StreamPipelineProbes.java \
  examples/core/streams/src/StreamG1Clarifications.java
java -cp examples/core/streams/build/classes StreamPipelineProbes
java -cp examples/core/streams/build/classes StreamG1Clarifications
```

Скрипт удаляет **только** каталог `examples/core/streams/build/`, компилирует оба файла, запускает их **в порядке P, затем C**, записывает их общий stdout в `build/actual.stdout` и строго сравнивает с [expected.stdout](expected.stdout) через `diff -u`. При ошибке компиляции, выполнения либо несовпадении stdout процесс завершается ненулевым кодом. В случае успеха допечатывается отдельная **строка скрипта, не входящая в эталон**: `CHECK PASSED: stdout matches expected.stdout`. Отдельный Gradle-модуль, JUnit и Mockito не требуются.

## Полный эталон Java stdout — 44 строки

```text
P01=0
P02=[2, 4, 6]
P03=3
P04=[a, b, c]
P05=[1, 2, 3]
P06=[4, 6]
P07=7
P08=true
P09=3,peek=0
P10=[a, null]
P11=UnsupportedOperationException
P12=2
P13=NullPointerException
P14=IllegalStateException
P15={a=2, b=1}
P16=NullPointerException
P17=NullPointerException
P18={even=[2, 4], odd=[1, 3]}
P19={false=[1, 3], true=[2, 4]}
P20=10
P21=[1, 2, 3]
P22=IllegalStateException
P23=before=
P24=after=AB
P25=1234
P26=500500
P27=fallback
P28=[1, 2]
P29=[3, 4]
P30=a,b
P31=IllegalStateException
P32=0
P33=[1, 2, 3, 4]
C01=Optional[6]
C02=Optional.empty
C03=6
C04=6
C05=OptionalInt[6]
C06=Optional[1],OptionalInt[1]
C07=true
C08=false
C09=true
C10=true
C11=67
```

## P01–P33: устройство pipeline, terminal и collectors

| Наблюдения | Почему получен результат и что он не доказывает |
|---|---|
| **P01–P03** | Вызов промежуточного `map` только строит lazy pipeline; после терминального `toList()` mapper фактически вызван три раза. Побочные эффекты mapper/peek нельзя считать обязательными для всех terminal и оптимизаций. |
| **P04–P06** | `flatMap` разворачивает внутренние Stream, `distinct/sorted` устраняют дубликаты и упорядочивают, а `filter` + `limit` дают два первых подходящих элемента. |
| **P07–P09** | В ordered parallel-потоке `findFirst` выбирает первый элемент по encounter order, а `findAny` не имеет фиксированного значения: сравнивается только `isPresent`. `count()` на SIZED-источнике возвращает здесь 3 и может вычислить размер без вызова `peek`. Эталон намеренно фиксирует наблюдение `peek=0` на проверенном JDK 21, а не универсальный контракт API: на другой допустимой реализации строгая сверка stdout может не пройти, хотя результат `count()` корректен. Такого отказа на другой JVM не наблюдали; скрипт сверки не ослаблен. |
| **P10–P13** | `Stream.toList()` вернул список с `null`, который **нельзя изменять**; попытка `add` выбросила `UnsupportedOperationException`. `Collectors.toList()` не обещает mutability, конкретный тип и thread safety. `Collectors.toUnmodifiableList()` отвергает `null`. |
| **P14–P17** | Дублирующийся ключ без merge policy в `toMap` → `IllegalStateException`, с `Integer::sum` → значения 2 и 1. Для стабильной печати P15 используется **отдельный TreeMap**, поскольку порядок обычного `toMap` не гарантирован. Тот же приём применяется в P18 (groupingBy) и P19 (partitioningBy): сортировка ключей относится только к представлению при печати, а не к типу или порядку обхода Map, возвращённой коллектором. `toUnmodifiableMap` запрещает `null` ключи/значения; `toMap` с null mapped value дал NPE на проверенном JDK, не нужно приписывать всем Map collectors один контракт. |
| **P18–P21** | `groupingBy` и `partitioningBy` дают разные группы; в P18 `TreeMap` только стабилизирует отображение. `IntStream.sum()` — примитивная редукция, `boxed()` превращает элементы в объекты. |
| **P22–P24** | Второй terminal для потреблённого Stream в этом случае выбрасывает ISE; обнаружение reuse **не гарантируется** во всех комбинациях intermediate stages. Успешный `count()` не вызвал `onClose`; лишь явный `close()` вызвал обработчики A→B. Terminal не гарантирует закрытия внешнего IO-ресурса. |
| **P25–P26** | `forEachOrdered` соблюдает encounter order вывода; это **не** означает последовательное исполнение upstream callback в одном потоке. Parallel sum использует ассоциативное сложение, но не обещает speedup, размер пула или планирование потоков. |
| **P27–P33** | Пустой `Optional` и `orElse`, ordered префиксы `takeWhile/dropWhile`, `joining`, исключение mapper **при terminal**, `Stream.ofNullable(null)` и конечный `iterate`. P31 — **перехваченная runtime-ошибка**, а не compile-negative checked case. |

## C01–C11: обязательные уточнения G1 (ST-20 и ST-19)

| Наблюдения | Механизм |
|---|---|
| **C01–C02** | `Stream.reduce(BinaryOperator)` **без identity** возвращает `Optional<T>`; для пустого Stream — `Optional.empty`. |
| **C03–C04** | `reduce(identity, accumulator)` возвращает значение; трёхаргументный overload дополнительно использует combiner. Для суммы 0 — нейтральный identity, а сложение ассоциативно. `reduce(10, Integer::sum)` не задаёт нейтральный элемент. |
| **C05–C06** | `IntStream.reduce(IntBinaryOperator)` возвращает `OptionalInt`; `Stream.findFirst` — `Optional<T>`, примитивный `IntStream.findFirst` — `OptionalInt`. |
| **C07–C10** | `anyMatch/allMatch/noneMatch` возвращают **примитивный boolean**, не Optional. На пустом Stream значения `false/true/true` (vacuous truth). |
| **C11** | `orElse(67)` — метод Optional, не метод у boolean; для пустого findFirst берётся fallback. |

## Негативные случаи и границы проверки

В [§7 G1-плана](../../../docs/plans/CORE-STREAMS-ADV.md) есть **N01–N17 — 17 изолированных заведомо некомпилируемых Java-проб**; в §5a — отдельный **CheckedFunctionProbe** (checked IOException нельзя выбросить напрямую из lambda `Stream.map(Function)`, поскольку `Function.apply` не объявляет `throws IOException`). Они **не входят** в положительную компиляцию этого `verify.sh`; нельзя смешивать их с P31, где ошибка runtime. Для negative-компиляций сверять **категорию** diagnostics, а не только ненулевой exit или точный текст javac.

**Не проверены и не гарантируются** данным runnable: реальный `Files.lines`/IO, автоматическое освобождение внешних ресурсов при terminal, JMH/performance, deterministic scheduler и callbacks parallel, скорость выполнения, число worker threads, обязательная гонка или её проявление при использовании общего `ArrayList`, другие JDK/компиляторы, Windows PowerShell без Bash, Obsidian/IntelliJ GUI, все отдельные negative N01–N17. Программа не является тестом thread safety. P09 не означает, что `peek` всегда пропускается.
