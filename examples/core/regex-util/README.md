# CORE-REGEX + CORE-UTIL — воспроизводимые Java 21 примеры

Этот лабораторный комплект поддерживает **две самостоятельные статьи**: [Регулярные выражения](../../../knowledge/01-java-core/regex.md) и [Утилиты JDK](../../../knowledge/01-java-core/utilities.md). Все положительные исполняемые тела и stdout-эталоны перенесены из [принятого G1 §6](../../../docs/plans/CORE-REGEX-UTIL.md) без изменения логики. В каждом исходнике перед первым `import` добавлен лишь соответствующий root-relative комментарий `// Theory:`.

**Два отдельных JVM-процесса, 33 + 38 = 71 подписанное наблюдение stdout — не 71 JUnit-тест.** Не создаём фиктивный Gradle/JUnit/Mockito-модуль.

## Воспроизведение

Нужны JDK **21** (`javac` и `java`), Bash, `mktemp`, `diff`, `wc`, `sha256sum` и `cut`. Из корня Git-репозитория:

~~~bash
bash examples/core/regex-util/verify.sh
~~~

`verify.sh` работает с `set -euo pipefail`, временным каталогом `mktemp -d` и `trap` на очистку. Он проверяет **одну компиляцию двух исходников** через `javac --release 21 -encoding UTF-8 -Xlint:all`: exit=0, **пустой stderr и ни одного warning**. Затем запускает две **отдельные JVM** с `-Dfile.encoding=UTF-8` и `-cp`, требует exit=0/stderr=0 и для каждой трассы выполняет **полный `diff -u`**, точное число строк и SHA-256 как фактического stdout, так и committed fixture относительно нормативного G1. `.class` и промежуточные `.stdout` создаются только в temp и не сохраняются в Git. Любое несовпадение завершает проверку ошибкой.

Для отдельного исследования одной программы (из корня репозитория):

~~~bash
tmp="$(mktemp -d)"
trap 'rm -rf -- "$tmp"' EXIT
javac --release 21 -encoding UTF-8 -Xlint:all -d "$tmp" \
  examples/core/regex-util/src/RegexProbe.java \
  examples/core/regex-util/src/UtilProbe.java
java -Dfile.encoding=UTF-8 -cp "$tmp" RegexProbe
# Или отдельно:
java -Dfile.encoding=UTF-8 -cp "$tmp" UtilProbe
~~~

| Сценарий | Полный Java21 source | Эталон |
| --- | --- | --- |
| RG01–RG33, 33 строки | [src/RegexProbe.java](src/RegexProbe.java) | [regex.expected.stdout](regex.expected.stdout) |
| UT01–UT38, 38 строк | [src/UtilProbe.java](src/UtilProbe.java) | [util.expected.stdout](util.expected.stdout) |

## RegexProbe: разница между наблюдением и механизмом

**RG01–RG06 — два языка и три режима совпадения.** `String regex = "\\d+"` сначала интерпретируется *компилятором Java* как строка `\d+`, которую затем интерпретирует уже *движок Pattern*. `Matcher.find()` находит фрагмент внутри строки, `matches()` требует совпадения всего региона, `lookingAt()` — префикса. Поэтому `RG02=true`, `RG03=false`, `RG04/05=true`; `RG06=[67@1:3, 8@4:5]` — позиции в строке, индексы UTF-16. `RG07=IllegalStateException` показывает, что `Matcher.group()` нельзя использовать до успешного сопоставления.

**RG08–RG12 — группы, квантификаторы и нулевая ширина.** В историческом RG08 шаблон `(?<word>[a-z]+)-(?<n>\\d+)` в Java-литерале сопоставляется со строкой `abc-67`: `matches()` даёт true, `group("word")` — `abc`, `group(2)` (группа с именем `n`) — `67`, итог `RG08=true:abc:67`; noncapturing group не увеличивает `groupCount()` (`RG09=0`). Greedy `a.+b` захватывает `a1b2b`, reluctant `a.+?b` — `a1b`, possessive `a.++b` не откатывается и в этом входе не находит совпадение. Именно на `a1b2b` получены `RG10=a1b2b:a1b` и `RG11=false`; это не измерение стоимости поиска. Zero-width lookahead отмечает **два** положения, а не потребляет два символа (`RG12=[0, 1]`).

**RG13–RG19 и RG24–RG33 — контекст и границы.** `MULTILINE` изменяет смысл `^`/`$`, `DOTALL` — поведение точки на переносе строки; `CASE_INSENSITIVE`, `LITERAL`, `COMMENTS` — разные флаги, а не общий «режим regex». В `RG25/26` арабско-индийская цифра не сопоставляется с базовым `\\d`, но сопоставляется при `UNICODE_CHARACTER_CLASS`. `RG27=0:0` — эффект `reset()`; `RG28=true:false` получен на строке `xxword`, шаблоне `\\bword` (Java-литерал) и `region(2,6)`: при непрозрачных границах начало региона допускает границу слова, при `useTransparentBounds(true)` предшествующий `x` вне региона учитывается и совпадение исчезает. Это конкретный пример, а не все варианты границ региона. `RG31=3:4` — `String.split` по умолчанию удаляет хвостовые пустые элементы; `split(",", -1)` сохраняет. `RG32=2:2` — совпадение с emoji длиной два UTF-16 code units; не считать `char` и Unicode code point одним и тем же.

**RG18–RG23 — quoting vs replacement grammar.** `Pattern.quote` делает метасимволы буквальными **в regex**, `Matcher.quoteReplacement` экранирует специальную грамматику **строки замены** (в G1 `RG21=$\\literal`); это разные API. `replaceAll` с `$2:$1` переставляет захваченные группы, `RG20=67:user`. `Pattern.compile("[")` **компилируется как Java source**, но при исполнении бросает `PatternSyntaxException` (`RG22`); недопустимая ссылка `$9` в replacement на этом входе приводит к **runtime IndexOutOfBoundsException** (`RG23`), не к ошибке `javac`. В отдельном опыте ниже ссылка `$9` при отсутствии совпадения не приводит к обработке замены.

**Ограничение ReDoS:** некоторые regex и специально подобранные входы могут вызывать дорогостоящий backtracking. Данная программа **не измеряет** время, memory/CPU consumption, худшую сложность, не реализует гарантированный timeout и не подтверждает линейность на произвольном вводе. Нельзя экстраполировать успешное выполнение 33 строк на безопасность недоверенного regex или больших входов.

## UtilProbe: наблюдаемые API-контракты

**UT01–UT12 — Objects.** `Objects.deepEquals` сравнивает два содержательно одинаковых `int[]` по элементам (`UT02=true`), а `Objects.equals` для тех же двух массивов — по ссылочной идентичности (`UT03=false`). `Objects.hashCode(67)=67` и varargs `Objects.hash(67)=98` различаются. `requireNonNull` со `Supplier<String>` вызывает поставщика сообщения *только* на null (счётчик `UT07=0` → `UT08=1:lazy`); `checkIndex` и `checkFrom*Index` выбрасывают `IndexOutOfBoundsException` при неподходящих границах.

**UT13–UT20 — Comparator.** `Comparator.comparing(Person::name).thenComparingInt(Person::age)` задаёт лексикографический порядок: `Ada:20`, затем `Ada:67`, затем `Bob:30`; `reversed()` меняет весь компаратор. `nullsFirst`/`nullsLast` работают на **внешнем элементе**, но сами не устраняют null из ключа внутри `Comparator.comparing` (`UT18=NullPointerException`). `TreeSet` определяет эквивалентность компаратором: `"A"` и `"a"` могут занять одну позицию (`UT19=1`), даже когда `String.equals` возвращает false; `Comparator.nullsFirst(null)` сравнивает все не-null значения как равные (`UT20=2`). Это демонстрация именно этих контрактов, **не полное покрытие COL-SORTED**.

**UT21–UT28 — Arrays.** `Arrays.equals` для примитивных массивов и `Arrays.deepEquals` для вложенных массивов отличаются от обычного `Arrays.equals(Object[], Object[])` на массивах, содержащих `int[]`. `Arrays.mismatch` возвращает первый несовпадающий индекс (`UT24=1`), `Arrays.compare` — знак лексикографического сравнения (`UT25=-1`). Только **после сортировки** `Arrays.binarySearch` имеет осмысленный контракт; в примере `UT27=1`, `UT28=-3` означает отрицательный код точки вставки по правилу `-(insertionPoint)-1`. При *несортированном* вводе полезный результат **не гарантируется**, хотя Java source законен.

**UT29–UT38 — views и копии.** `Arrays.asList(words)` — backed view фиксированного размера: изменения массива видны списку (`UT29=[Z, B]`), `view.set` меняет исходный массив (`UT30=[Z, Y]`), а `view.add` бросает `UnsupportedOperationException`. `Arrays.asList(new int[]{1,67})` — список **одного элемента `int[]`**, не двух `Integer` (`UT32=1`). `List.of` запрещает null и изменения структуры. `Collections.unmodifiableList(source)` — **живая неизменяемая обёртка** над мутируемым источником, `List.copyOf(source)` — **структурный snapshot** (`UT35=[B]:[A]`); но копия **поверхностная**, поэтому изменяемый `StringBuilder` меняется и внутри неё (`UT36=A67`). `UT38` — сортировка слов компаратором длины, затем по естественному порядку; не доказательство производительности сортировки.

## 14 compiler-negative: читать и разбирать отдельно от runtime

В [G1 §7](../../../docs/plans/CORE-REGEX-UTIL.md) есть **14 исторических отдельных исходников и ожидаемых диагностических категорий** — 6 Regex и 8 Util. Они ранее **реально проверены независимо ролью 03 на `javac 21.0.12.1 -XDrawDiagnostics`** (14/14), но **этот positive runner и его CI их не выполняют**. Это не новые временные проверки D-A23a и не 24 ожидаемых отказа компиляции других пакетов в текущем CI. Для каждой ошибки важно не только `exit != 0`, но и **правильная диагностическая причина**. Ниже дана карта для самостоятельного разбора; G1 содержит полные исходные однострочные программы и точные категории:

| Negative | Содержательная причина | Ожидаемая `compiler.err.*` |
| --- | --- | --- |
| RG_N01, RG_N02 | Недопустимые Java string escapes `\d` и `\q` в исходнике (одна обратная косая черта); например, `String p = "\d+";` и `String p = "\q";`. Интерпретация Pattern здесь даже не начинается | `illegal.esc.char` |
| RG_N03 | `String.matches` принимает `String`, не `Pattern` | `cant.apply.symbol` |
| RG_N04 | `group(int)` принадлежит `Matcher`, не `Pattern` | `cant.resolve.location.args` |
| RG_N05 | У `Matcher.group` нет перегрузки с двумя `int` | `cant.apply.symbols` |
| RG_N06 | `Matcher.group()` возвращает `String`, не `int` | `prob.found.req` |
| UT_N01 | `Arrays.deepEquals` принимает `Object[]`, не примитивный `int[]` напрямую | `cant.apply.symbol` |
| UT_N02 | `Arrays.binarySearch(int[], int)` нельзя вызвать с ключом `String` | `cant.apply.symbols` |
| UT_N03 | `Objects.checkIndex` принимает целочисленную длину, не `String` | `cant.apply.symbols` |
| UT_N04 | `Comparator.compare` обязан возвращать `int`, а не `boolean` | `prob.found.req` |
| UT_N05 | `Arrays.asList(new int[]{...})` даёт `List<int[]>`, поэтому `add(67)` не соответствует типу | `cant.apply.symbols` |
| UT_N06 | `Comparator.nullsFirst` принимает `Comparator`, не строку | `cant.apply.symbol` |
| UT_N07 | `Arrays.sort` принимает массив, не `List` | `cant.apply.symbols` |
| UT_N08 | `Objects.requireNonNull` в варианте Supplier требует `Supplier<String>` (сообщение), не числа | `cant.apply.symbols` |

Два характерных **семантических контраста**: `String p = "\\d+";` в реальном Java source корректен, но `String p = "\d+";` содержит запрещённую для Java последовательность `\d`; при этом `Pattern.compile("[")` абсолютно корректно с точки зрения `javac` и падает только **в runtime**. Не всякая одиночная обратная косая черта перед regex-метасимволом вызывает ошибку Java: `"\s"` — допустимый Java-литерал пробела, а `"\b"` — символ backspace (U+0008). Для regex whitespace и границы слова в Java-литерале нужны `"\\s"` и `"\\b"` соответственно. Аналогично `Objects.requireNonNull(null, () -> "required")` компилируется и бросает `NullPointerException` при выполнении, а `() -> 67` в этой перегрузке **не компилируется**. Точные ключи `javac` — наблюдение OpenJDK 21.0.12.1, не обещание неизменного локализованного текста или категории в других версиях.

## Граница доказанного

Принятые исходники G1 и фактические positive stdout воспроизводились на **Debian Linux / OpenJDK 21.0.12.1**, строго UTF-8 + LF. Другие JDK vendors/OS/Unicode versions не проверены; Unicode-class и UTF-16 наблюдения следует интерпретировать в пределах данного JDK, не как универсальный будущий Unicode table. В CI есть только описанные **2 positive JVM**; исторические 14 isolated compiler-negative и новые временные проверки ниже, автоматическое извлечение и компиляция всех inline snippets теории, benchmark/timeouts/ReDoS performance, гарантии на произвольные входы, COL-SORTED/CORE-I18N/SERIAL, P2, другие JDK/OS и реальная Obsidian/IDE GUI/anchors/HTTP навигация **не входят в этот runner и его CI**. Другие JDK/OS, измерения производительности и GUI не проверялись. 

**Проверки D-A23a.** В [отчёте автора 02](https://github.com/Melchiorius/backend-interview-labs/issues/333#issuecomment-7210941583) приведены отдельные временные `RemediationProbe` и `ExerciseProbe` (две строгие компиляции, два успешных запуска, 31 и 5 строк stdout), один исходник с недопустимым Java escape и три запуска исходного runner. Это новые проверки конечного набора примеров, а не повтор исторических 14 отрицательных исходников, не автоматическая компиляция всех блоков статьи и не CI.

При интеграции D-A23a роль 03 независимо восстановила пять точных файлов пакета (не полный клон) и выполнила `bash -n` и три полных запуска неизменённого `verify.sh` на Debian OpenJDK/javac 21.0.12.1. При `LC_ALL=C` временные PATH-обёртки передавали одному абсолютному бинарнику Java `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8`; для en_US и tr_TR также переопределяли язык и страну, для default — нет. JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS и _JAVA_OPTIONS сняты. Измеренные локали en_US/en_US/tr_TR, stdout и default charset — UTF-8. Все три запуска завершились с кодом 0 и пустым stderr. Внутренние сравнения, число строк 33/38 и SHA-256 обеих трасс совпали с G1. Каждый полный stdout runner — одна строка (106 байт), побайтно одинаковая между запусками; 71 строка проверяется внутри скрипта, а не печатается им. SHA и режимы пяти файлов сверены до и после проверок.

В отдельных временных исходниках 03 заново скомпилировала ровно RG_N01 и RG_N02 из G1: оба дали exit 1 с `compiler.err.illegal.esc.char` и пустым stdout. Остальные 12 исторических отрицательных исходников не повторялись. Временная программа `Contrast` прошла строгую компиляцию и три запуска в тех же конфигурациях (exit 0, stderr пуст): проверены допустимый regex для цифр, различие Java-пробела/backspace и regex whitespace/границы слова, `PatternSyntaxException` при компиляции шаблона "[" в работающей программе и различие замены `$9` с совпадением (IndexOutOfBoundsException) и без совпадения (исходная строка `x`) для `replaceFirst` и `replaceAll`. Эти пробы не входят в исторические RG/UT-эталоны или CI. Проверки ограничены конкретными входами, тремя конфигурациями одного JDK/ОС; локально не выполнялись полный Gradle/JUnit, clean clone, автоматическая компиляция всех листингов, стресс/benchmark ReDoS или другие платформы.
