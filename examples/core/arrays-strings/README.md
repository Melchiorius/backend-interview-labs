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
| S01–S04 | `String` неизменяем: в S01 `concat("b")` возвращает строку с содержимым `ab`, исходная строка остаётся `a`. Это не обещание новой объектной идентичности при каждом `concat`: при пустом аргументе метод возвращает исходную строку. `equals` сравнивает содержимое, `==` — ссылки; литералы/constant expression и `intern()` демонстрируют идентичность, но **не обещают местоположение String pool или число аллокаций**. При `+` важны порядок и типы операндов. |
| S05–S08 | Строка `A😀Z` занимает 4 **UTF-16 code units**, но имеет 3 **Unicode code points**. Суррогатная пара занимает два `char`; `codePointAt`, `codePointCount`, `offsetByCodePoints` и `substring` решают разные задачи. Ни одно наблюдение не равно подсчёту графем. |
| S09–S12 | `trim` и `strip` используют разные критерии пробелов; `split` принимает **регулярное выражение** (поэтому `\\.` в Java-литерале задаёт регулярное выражение `\.` для буквальной точки), а limit `0` убирает хвостовые пустые строки; `-1` сохраняет их. |
| S13–S17 | `String.join` допускает null-элемент с текстовым представлением `null`; `(Object)null` для `String.valueOf` возвращает строку `"null"`, а `(char[])null` вызывает `NullPointerException`. Более тонкая перегрузка **`String.valueOf(null)` без cast выбирает `char[]`** и тоже вызывает NPE — этот прямой вызов не включён в 28 строк. `replace` работает с литералами, `replaceAll` — с regex; S17 показывает только неравенство `"\u00E9"` и `"e\u0301"`; нормализация NFC в этой программе не выполняется. |
| S18–S24 | `StringBuilder` изменяемый: `append/insert` меняют содержимое. `ensureCapacity` увеличивает **вместимость, а не длину**; `setLength` дополняет нулевыми `char`. У `StringBuilder` не переопределён value-based `equals`; `String.contentEquals` проверяет содержимое. `append((String)null)` добавляет `"null"`, а `append((char[])null)` выбрасывает NPE. S23 — только последовательный `append` и результат `xy`; это не проверка потокобезопасности. `StringBuffer` синхронизирует отдельные методы, **не** произвольную составную операцию. |
| S25–S28 | S25 использует `String.format(Locale.ROOT, "%d", 67)`; S26 показывает допустимый `substring`. S27/S28 сохраняют наблюдаемый на OpenJDK 21.0.12.1 конкретный класс `StringIndexOutOfBoundsException`; документированный контракт `String.charAt` и `String.substring` — `IndexOutOfBoundsException`. Сравнение конкретного имени класса в историческом эталоне строже контракта API. Отдельные проверки ниже принимают документированный суперкласс. |

## Уточнения, которые важно не смешивать с положительным примером

**Частичное копирование `System.arraycopy`.** По API при несовместимом элементе массива ссылок возможен уже скопированный совместимый префикс. При исходной публикации отдельный локальный опыт с `Object[]{"ok", 67}` и `String[]{"initial", "initial"}` после перехваченного `ArrayStoreException` дал `[ok, initial]` на OpenJDK 21.0.12.1. В A20 показан только класс ошибки, поэтому результат частичного копирования **не является** одной из A01–A24 и не добавлен в эталон. Независимый [отчёт 04 по D-A14a](https://github.com/Melchiorius/backend-interview-labs/pull/328#issuecomment-7206813897) отдельно подтверждает совместимый префикс в другом примере; [окончательное решение A14a](https://github.com/Melchiorius/backend-interview-labs/issues/327#issuecomment-7206974773) не меняется.

**Компиляционно-отрицательные случаи.** [§7 G1-плана](../../../docs/plans/CORE-ARRAYS-STRINGS.md) содержит **N01–N24** — 24 исторических изолированных Java-исходника, которые не должны компилироваться. При исходной публикации роль 02 отдельно проверила ожидаемые категории диагностики. Для автоматизации N01–N24 понадобится отдельный harness с проверкой **причины** ошибки, не только ненулевого exit status.

При доработке D-A14b роль 02 отдельно проверила четыре новых отрицательных исходника: `Join`, `Append` и `Insert` отклонены из-за неоднозначности перегрузок, `Label` — из-за требования строковой константы в `case`. У `Join` дополнительно было предупреждение. Это не исторические N01–N24 и не 24 ожидаемых отказа компиляции в CI при проверках доступа, наследования и вложенных классов.

`verify.sh` не запускает ни исторические N01–N24, ни четыре новых исходника 02 и не выдаёт падения `javac` за `ArrayStoreException`/NPE в работающем процессе. Ни один из этих двух локальных наборов отрицательных исходников не выполняется в CI.

**D-A14b — локальная проверка роли 03 при исходной интеграции (N36/N37).** На Debian OpenJDK/javac 21.0.12.1 во временном дереве, восстановленном из пяти точных файлов пакета (не полный клон), при `LC_ALL=C` выполнены `bash -n examples/core/arrays-strings/verify.sh` и три полных запуска исходного скрипта из корня. Временные PATH-обёртки над одним абсолютным бинарником Java передавали `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8` во всех случаях; для en_US и tr_TR дополнительно задавались `user.language` и `user.country`, для default эти параметры не задавались. Переменные JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS и _JAVA_OPTIONS сняты.

Отдельная Java-проба под теми же обёртками измерила фактические локали en_US/en_US/tr_TR и UTF-8 для stdout и default charset. Все три запуска скрипта завершились с кодом 0, stderr пуст (в том числе нет уведомлений о подхвате JVM options); оба `cmp` в каждом случае дали 0. Вывод массивов — 24 строки/421 байт, строк — 28/399, полный вывод скрипта — 53/867; все три полных вывода побайтно одинаковы. Проверены SHA и режимы всех пяти файлов до и после запуска.

S25 дал 67 в трёх конфигурациях: это проверка форматирования с явным Locale.ROOT, а не всех форматов, локалей, ОС или JDK. Результаты относятся к исходной интеграции D-A14b; после редакционной правки прозы локальные запуски не повторялись.

Отдельная временная программа `BoundaryProbe` (не часть runner или CI) скомпилирована `javac --release 21 -encoding UTF-8 -Xlint:all -Werror` без диагностик и выполнена во всех трёх конфигурациях с кодом 0 и пустым stderr. Проверки `charAt` и двух форм `substring` строки, а также `charAt`, `substring` и отрицательного значения аргумента `setLength` у StringBuilder перехватывают документированный `IndexOutOfBoundsException`, а не требуют конкретного подкласса; в этих запусках наблюдался StringIndexOutOfBoundsException.

Отдельно подтверждены: пустой `concat` возвращает исходную строку; raw equality=false, NFC equality=true для двух представлений é; совместимый префикс arraycopy `[ok, initial]` после исключения; последовательная модель двух проверок длины и двух append даёт `xx`; форматирование с Locale.ROOT даёт 67. Последняя модель с проверками длины не запускает потоки и не доказывает свойства конкурентного исполнения. Это конечный набор самостоятельных проверок, а не строки S01/S17/S23/A20 исторического вывода; результаты не добавлены в эталон.

Источники: [поручение 00 по N36/N37](https://github.com/Melchiorius/backend-interview-labs/pull/331#issuecomment-7207429441), [фактический отчёт 03 об интеграции D-A14b](https://github.com/Melchiorius/backend-interview-labs/pull/331#issuecomment-7207496299) с исходником, командами и выводом, [отдельный отчёт автора 02 D-A14b](https://github.com/Melchiorius/backend-interview-labs/issues/330#issuecomment-7207400320). В трёх запусках runner автора 02 с явным UTF-8 и конфигурациями default/en_US/tr_TR оба сравнения эталонов успешны, а stderr содержит только уведомления JVM о параметрах окружения; это отдельные запуски с другими способами задания параметров, не результаты роли 03. Фактические измеренные локали и пустой stderr выше относятся именно к запускам 03.

Исторические N01–N24 и новые временные пробы 02 не повторялись ролью 03; полный Gradle/JUnit, clean clone, GUI и многопоточный стресс локально не выполнялись.

**Границы воспроизводимости.** Исторический runnable проверяет исходники Java 21 и фактический stdout двух программ, а не реальный файловый ввод-вывод и произвольные кодировки, сегментацию графем, нормализацию Unicode, JMH/HotSpot allocation/GC, многопоточный стресс StringBuffer, GUI Obsidian/IntelliJ, альтернативные JVM/JDK, отрицательные Markdown-snippets или все 51 curricular ID. Никакая производительность `StringBuilder` данным опытом не измерялась.

При исходной публикации материалы были собраны в [PR #124](https://github.com/Melchiorius/backend-interview-labs/pull/124) с практикой из [PR #125](https://github.com/Melchiorius/backend-interview-labs/pull/125). После той интеграции [GitHub Actions #44949506937](https://github.com/Melchiorius/backend-interview-labs/actions/runs/44949506937) завершился **SUCCESS** на exact commit `162e9bc5fac7fe18e54d8634eeff618177ab323c`, включая шаг проверки этого runnable (24+28 строк).

Это историческое свидетельство конкретной версии: при последующих изменениях нужен отдельный CI на новом HEAD. Успех положительного примера не подтверждает компиляцию всех inline-сниппетов статей или N01–N24 и не служит свидетельством прохождения G3/G4/G5 текущей доработки D-A14b. Историческая формулировка о подготовке к публикации относилась к исходному циклу; текущие решения о статусах и `COVERAGE/CURRICULUM` принимаются отдельно.
