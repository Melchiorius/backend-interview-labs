# CORE-ARRAYS + CORE-STRINGS/BUILDERS — два Java 21 запуска

**Один учебный набор** связывает поведение массивов и строк без создания отдельной задачи для студента. Оба исходника взяты из [принятого G1-плана, §6](../../../docs/plans/CORE-ARRAYS-STRINGS.md): [ArraysG1.java](src/ArraysG1.java) даёт A01–A24, а [StringsG1.java](src/StringsG1.java) — S01–S28. Исполняемые операторы не изменены: в начала Java-файлов добавлены только комментарии `// Theory` и `// Research`. **52 строки — наблюдения двух Java-процессов, не 52 независимых теста.**

Сначала теория: [CORE-ARRAYS](../../../knowledge/01-java-core/arrays.md) и [CORE-STRINGS с CORE-BUILDERS](../../../knowledge/01-java-core/strings.md). Этот README помогает воспроизвести связь механизмов и разобрать вывод; подробные определения, контрпримеры, ссылки на JLS/API и все AR/TX/BL находятся в статьях.

## Как запустить

Нужны **JDK 21**, Bash и утилита `diff` (в Windows можно использовать Git Bash). Из **корня репозитория**:

```sh
java -version
javac -version
bash examples/core/arrays-strings/verify.sh
```

Для раздельного ручного запуска:

```sh
mkdir -p examples/core/arrays-strings/build/classes
javac --release 21 -d examples/core/arrays-strings/build/classes examples/core/arrays-strings/src/ArraysG1.java examples/core/arrays-strings/src/StringsG1.java
java -cp examples/core/arrays-strings/build/classes ArraysG1
java -cp examples/core/arrays-strings/build/classes StringsG1
```

Скрипт [verify.sh](verify.sh) пересоздаёт **только** `examples/core/arrays-strings/build/` (каталог исключён из Git через `**/build/`), выполняет **одну компиляцию `javac --release 21` двух source**, затем **два отдельных `java`**, сравнивает **весь stdout** каждого запуска через два `diff -u` с [arrays.expected.stdout](arrays.expected.stdout) и [strings.expected.stdout](strings.expected.stdout). При расхождении, ошибке компиляции или запуска завершается ненулевым кодом. После успешных сравнений печатает 52 строки Java и дополнительное сообщение `CHECK PASSED: both stdout files match expected` — это сообщение **скрипта**, не часть эталона.

## Полный stdout ArraysG1 (24 строки)

```text
A01=[0, 0],[null, null]
A02=2,true
A03=ArrayStoreException
A04=true
A05=2,2,1
A06=true,true,67
A07=true,true
A08=[4, 0, 0]
A09=[1, 1, 2, 3]
A10=true
A11=false,true
A12=[[1], [2]]
A13=[z, q],[z, q]
A14=UnsupportedOperationException
A15=1,3
A16=2
A17=ArrayIndexOutOfBoundsException
A18=NegativeArraySizeException
A19=NullPointerException
A20=ArrayStoreException
A21=ClassCastException
A22=67
A23=NullPointerException
A24=1
```

## Полный stdout StringsG1 (28 строк)

```text
S01=a,ab
S02=true,false
S03=true,true
S04=68x,x671
S05=4,3
S06=true,128512
S07=3,true
S08=3
S09=3,1
S10=true,true,false
S11=[a, b]
S12=[a, b],[a, b, , ]
S13=a|null
S14=null
S15=NullPointerException
S16=a!c,!!!
S17=false
S18=[ab67],6
S19=true,true
S20=false,true
S21=null
S22=NullPointerException
S23=xy
S24=3,0,0
S25=67
S26=bc
S27=StringIndexOutOfBoundsException
S28=StringIndexOutOfBoundsException
```

## Почему получаются эти результаты

| Метки | Наблюдение и механизм |
|---|---|
| A01–A04 | У `int[]` и `String[]` разные значения элементов по умолчанию. Массив ссылок ковариантен: `String[]` можно видеть как `Object[]`, но реальный компонент `String` проверяется **при записи**; помещение `Integer` вызывает `ArrayStoreException`. Это не делает `List<String>` ковариантным к `List<Object>`. |
| A05–A07 | `int[][]` может быть jagged. `clone()` и `Arrays.copyOf` для внешнего массива создают **новый внешний массив**, но вложенные `int[]` остаются теми же объектами. Изменение вложенного элемента заметно через обе ссылки. |
| A08–A09 | `Arrays.copyOf` для `int[]` дополняет недостающие позиции нулями; `System.arraycopy` корректно обрабатывает перекрывающиеся области внутри одного массива. Это не рекурсивная копия. |
| A10–A12 | `Arrays.equals` у вложенных массивов сравнивает ссылки на компоненты, `Arrays.deepEquals` — вложенное содержимое, `deepToString` его отображает. Это не универсальный алгоритм обхода циклического графа объектов. |
| A13–A15 | `Arrays.asList(source)` возвращает fixed-size **backed view**: `set` и изменения исходного массива видны с обеих сторон, `add` вызывает `UnsupportedOperationException`. `Arrays.asList(int[])` имеет один элемент типа `int[]`, тогда как `Arrays.stream(int[])` — `IntStream` из его чисел. |
| A16–A20 | `Arrays.stream(String[])` обрабатывает элементы; ошибки границ и создания массива различаются (`ArrayIndexOutOfBoundsException`, `NegativeArraySizeException`, `NullPointerException`). В A20 `System.arraycopy` перехватывает `ArrayStoreException` на несовместимом элементе, но **не проверяет содержимое destination после ошибки**. |
| A21–A24 | `@SafeVarargs` здесь **заведомо лживый**: запись через массив varargs позволяет подменить `List<String>` списком с Integer, и `ClassCastException` возникает на чтении как `String`. Это контрпример, не образец кода. `List<?>[]` допустим благодаря reifiable wildcard; `List.of` отвергает null; `clone` массива сохраняет длину. |
| S01–S04 | `String` immutable: `concat` возвращает другой результат, исходный литерал не меняется. `equals` сравнивает содержимое, `==` — ссылки; литералы/constant expression и `intern()` демонстрируют идентичность, но **не обещают местоположение String pool или число аллокаций**. При `+` важны порядок и типы операндов. |
| S05–S08 | Строка `A😀Z` занимает 4 **UTF-16 code units**, но имеет 3 **Unicode code points**. Суррогатная пара занимает два `char`; `codePointAt`, `codePointCount`, `offsetByCodePoints` и `substring` решают разные задачи. Ни одно наблюдение не равно подсчёту графем. |
| S09–S12 | `trim` и `strip` используют разные критерии пробелов; `split` принимает **регулярное выражение** (поэтому `\\.` в Java literal обозначает буквальную точку), а limit `0` убирает хвостовые пустые строки; `-1` сохраняет их. |
| S13–S17 | `String.join` допускает null-элемент с текстовым представлением `null`; `(Object)null` для `String.valueOf` возвращает строку `"null"`, а `(char[])null` вызывает `NullPointerException`. Более тонкая перегрузка **`String.valueOf(null)` без cast выбирает `char[]`** и тоже вызывает NPE — этот прямой вызов не включён в 28 строк. `replace` работает с литералами, `replaceAll` — с regex; Unicode-композиционно эквивалентные строки без нормализации не обязаны быть равными. |
| S18–S24 | `StringBuilder` изменяемый: `append/insert` меняют содержимое. `ensureCapacity` увеличивает **вместимость, а не длину**; `setLength` дополняет нулевыми `char`. У `StringBuilder` не переопределён value-based `equals`; `String.contentEquals` проверяет содержимое. `append((String)null)` добавляет `"null"`, а `append((char[])null)` выбрасывает NPE. `StringBuffer` синхронизирует отдельные методы, **не** произвольную составную операцию. |
| S25–S28 | `String.format(Locale.ROOT, ...)`, `substring` и граничные ошибки `charAt/substring` завершают демонстрацию; форматирование с другими locale не проверялось. |

## Уточнения, которые важно не смешивать с положительным примером

**Частичное копирование `System.arraycopy`.** По API при несовместимом элементе массива ссылок возможен уже скопированный совместимый префикс. Отдельный локальный опыт с `Object[]{"ok", 67}` и `String[]{"initial", "initial"}` после перехваченного `ArrayStoreException` дал `[ok, initial]` на OpenJDK 21.0.12.1. В A20 показан только класс ошибки, поэтому результат частичного копирования **не является** одной из A01–A24 и не добавлен в эталон.

**Компиляционно-отрицательные случаи.** [§7 G1-плана](../../../docs/plans/CORE-ARRAYS-STRINGS.md) содержит **N01–N24** — 24 изолированных Java-фрагмента, которые не должны компилироваться. `verify.sh` их **не** запускает и не выдаёт падения `javac` за `ArrayStoreException`/NPE в работающем процессе. Роль 02 отдельно проверила ожидаемые категории диагностики; это не проверка в данном CI. Для автоматизации N01–N24 понадобится отдельный harness с проверкой **причины** ошибки, не только ненулевого exit status.

**Границы воспроизводимости.** Проверяется Java 21 source + фактический stdout двух программ, а не реальный IO и кодировки, grapheme segmentation, Unicode normalization, JMH/HotSpot allocation/GC, многопоточный стресс StringBuffer, GUI Obsidian/IntelliJ, альтернативные JVM/JDK, отрицательные Markdown-snippets или все 51 curricular ID. Никакая производительность `StringBuilder` данным опытом не измерялась. Для общего доказательства публикации необходим CI после интеграции примера в [авторский PR #124](https://github.com/Melchiorius/backend-interview-labs/pull/124); до G5 `COVERAGE/CURRICULUM` не изменяются.
