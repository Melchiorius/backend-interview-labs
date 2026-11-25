# CORE-ENUMS + CORE-CONTROL — общий Java 21 runnable

Пакет дополняет две самостоятельные статьи:

- [CORE-ENUMS](../../../knowledge/01-java-core/enums.md) — перечисления как закрытые типы с именованными экземплярами, identity, конструкторами, поведением, EnumSet и EnumMap.
- [CORE-CONTROL](../../../knowledge/01-java-core/control-flow.md) — условия, циклы, метки, switch statement, switch expression и yield.

Исходники основаны на полных положительных программах принятого исследовательского плана G1, §7. В B4b исправлено техническое форматирование enum: вместо преобразования регистра с локалью по умолчанию используется Locale.ROOT; оба эталона сохранены. Они показывают совместную работу enum и `switch`, но не заменяют теорию и не превращают наблюдения в независимые модульные тесты.

## Состав пакета

| Файл | Назначение |
|---|---|
| [src/EnumProbe.java](src/EnumProbe.java) | Программа E01–E30 на основе G1 с исправлением B4b; 30 подписанных строк stdout. |
| [src/ControlProbe.java](src/ControlProbe.java) | Полная программа G1 C01–C21; 21 подписанная строка stdout. |
| [enum.expected.stdout](enum.expected.stdout) | Точный независимый эталон первого JVM-процесса. |
| [control.expected.stdout](control.expected.stdout) | Точный независимый эталон второго JVM-процесса. |
| [verify.sh](verify.sh) | Компиляция, два отдельных запуска JVM и два строгих сравнения diff -u. |

**30 + 21 = 51 наблюдение в выводе двух JVM-процессов, а не 51 JUnit-тест.** Пакет не требует Gradle, JUnit, Mockito или сетевого доступа; нужна установленная JDK 21.

## Запуск

Из корня репозитория на Linux/macOS или в Git Bash:

~~~bash
bash examples/core/enums-control/verify.sh
~~~

Скрипт использует javac --release 21 -Xlint:all, собирает class-файлы во временный каталог mktemp, удаляет его через trap, запускает EnumProbe и ControlProbe в отдельных JVM-процессах и сравнивает полный stdout каждого процесса с соответствующим эталоном. Скрипт завершается ошибкой при любом предупреждении/диагностике javac, stderr JVM или несовпадении stdout.

Ожидаемый результат:

~~~text
CORE-ENUMS + CORE-CONTROL: PASS (30 + 21 stdout lines; 2 JVM processes)
~~~

## Что наблюдать

### EnumProbe

- E01–E04: порядок values(), name(), ordinal() и переопределённый toString() — разные контракты; ordinal() не является бизнес-идентификатором.
- E05–E10: valueOf, явный business code 67, разрешённый переход состояния и поведение констант, реализующих интерфейс.
- E11–E14: итерация по элементам EnumSet и ключам EnumMap идёт в порядке объявления enum; EnumMap допускает null как значение и сохраняет различие между отсутствующим ключом и ключом с null.
- E15–E19, E28–E29: EnumSet.add(null) и EnumMap.put(null, ...) бросают NullPointerException, тогда как contains(null)/remove(null) у EnumSet и containsKey(null) у EnumMap возвращают false; valueOf различает неизвестное имя и null.
- E20: каждая enum-константа — единственный экземпляр соответствующего значения в рамках загрузки её типа, но внутри неё могут быть изменяемые поля; это само по себе не обеспечивает неизменяемости или потокобезопасности.
- E21–E24: у константы с собственным телом класса `getClass()` отличается от `getDeclaringClass()`; `Class.isSealed()` показывает различие между обычным enum и enum с телом класса конкретной константы.
- E25–E27, E30: identity enum, допустимость квалифицированной enum-константы в Java 21 switch, независимость массива, возвращённого values(), от последующего вызова и различие name()/toString().

### ControlProbe

- C01–C02: else относится к ближайшему незавершённому if.
- C03–C06: классический switch допускает fall-through и отсутствие default; arrow rule не проваливается в следующую ветку.
- C07–C08, C16: switch expression должен дать значение; yield возвращает результат из блока или colon-группы.
- C09, C17, C20: break/continue, включая метку внешнего цикла, управляют разными областями выполнения.
- C10–C12: while проверяет условие до тела, do/while — после, enhanced for проходит элементы коллекции.
- C13, C21: && и || вычисляют правый операнд только при необходимости.
- C14–C15: Java 21 допускает case null в данном switch; без такой ветки null-selector вызывает runtime NullPointerException.
- C18: изменение локальной переменной enhanced-for не заменяет элементы массива.
- C19: selector switch вычисляется один раз.

## B4b — нейтральное техническое форматирование

В E01 метод toString() формирует строку status-paid с помощью name().toLowerCase(Locale.ROOT). Метод [name()](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Enum.html#name()) возвращает точное имя текущего объявления; toString() можно переопределить, а valueOf ищет имя, не отображаемую строку. No-arg [toLowerCase()](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#toLowerCase()) использует локаль по умолчанию: при tr_TR слово PAID даёт paıd (U+0131 вместо U+0069). Явный [Locale.ROOT](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Locale.html#ROOT) в [toLowerCase(Locale)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#toLowerCase(java.util.Locale)) подходит для нейтрального технического формата, но не обязательно для текста, предназначенного пользователю. Ни ordinal(), ни произвольный toString() не являются стабильным бизнес-идентификатором; явный код или сопоставление требуют собственного контракта совместимости. Локаль JVM не меняется и не фиксируется в программе или runner.

Историческое наблюдение и исходный G1-вариант зафиксированы в [A17.2](https://github.com/Melchiorius/backend-interview-labs/blob/3f5648eddc98f0308328224d840e9d3933c6bb3a/docs/audits/java-core-full-2026-11-24/A17.2-CORE-CONTROL.md#L71-L72); [согласованный контракт B4b](https://github.com/Melchiorius/backend-interview-labs/issues/289#issuecomment-7163012185) сохраняет оба эталона, ControlProbe и runner. Исторические планы/аудит не переписаны.

**Фактическая локальная проверка 03:** Debian OpenJDK/javac 21.0.12.1, LC_ALL=C, временные PATH-local java wrappers с одним абсолютным бинарником java и явным -Dstdout.encoding=UTF-8. Для en_US/tr_TR дополнительно заданы user.language и user.country; default без этих флагов. Переменные JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS, _JAVA_OPTIONS сняты. Отдельная Java-проба подтвердила фактические Locale.getDefault() en_US/en_US/tr_TR, System.out.charset() и stdout.encoding UTF-8 во всех трёх, file.encoding и default charset UTF-8, native.encoding ANSI_X3.4-1968. Это три конфигурации JVM, не матрица локалей ОС.

| JVM config | Baseline runner | Final runner |
|---|---|---|
| Default (en_US) | exit 0 | exit 0 |
| en_US | exit 0 | exit 0 |
| tr_TR | exit 1, E01 status-paıd | exit 0 |

Все шесть runner stderr пусты. Исходный tr_TR отказал на первом enum diff: оба JVM-процесса уже завершились, но сравнение control fixture не достигнуто, финального маркера нет. В исправленном варианте все три запуска печатают исходный маркер после обоих строгих сравнений. Runner удаляет временные файлы через trap, поэтому полный stdout E/C дополнительно сохранён **в отдельных прямых запусках**, не извлечён из runner: совместная javac --release 21 -Xlint:all компиляция каждой версии и два java процесса для каждой конфигурации (12 запусков, все exit 0, stderr пуст). Все 21 строки control совпали с эталоном в шести конфигурациях; единственный исходный enum diff в tr_TR — E01. Все три final вывода 30+21 совпали побайтно с эталонами и между конфигурациями. 29 остальных E-строк неизменны. Команда bash verify.sh из корня без locale flags также прошла. Сверены 5/5 исходных и 5/5 итоговых исполняемых/input Git blob SHA и modes; runner 100755 и оба эталона побайтно сохранены. Подробные команды, результаты и точные SHA приведены в [техническом отчёте PR #290](https://github.com/Melchiorius/backend-interview-labs/pull/290), относящемся к [Issue #289](https://github.com/Melchiorius/backend-interview-labs/issues/289). Обычный CI выводит только маркер, не 51 сырую строку. Полный Gradle/JUnit/clean clone/GUI локально не выполнялись; не заявляется универсальная переносимость на все JDK/ОС/локали.

## Границы проверки

Скрипт проверяет две положительные программы и точное совпадение вывода на установленной JDK 21. Исследовательские отрицательные примеры компиляции N01–N26/N28–N32 из G1 §8 намеренно не входят в обычный скрипт/CI. Скрипт не подтверждает поведение других JDK/JVM/ОС, JMM, бинарную эволюцию enum, JPMS, JIT/GC, производительность или результаты измерений.
