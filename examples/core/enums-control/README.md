# CORE-ENUMS + CORE-CONTROL — общий Java 21 runnable

Пакет дополняет две самостоятельные статьи:

- [CORE-ENUMS](../../../knowledge/01-java-core/enums.md) — перечисления как закрытые типы с именованными экземплярами, identity, конструкторами, поведением, EnumSet и EnumMap.
- [CORE-CONTROL](../../../knowledge/01-java-core/control-flow.md) — условия, циклы, метки, switch statement, switch expression и yield.

Исходники воспроизводят полные positive-программы из принятого исследовательского плана G1, §7. Они показывают совместную работу enum и switch, но не заменяют теорию и не превращают все наблюдения в независимые unit-тесты.

## Состав пакета

| Файл | Назначение |
|---|---|
| src/EnumProbe.java | Полная программа G1 E01–E30; 30 подписанных строк stdout. |
| src/ControlProbe.java | Полная программа G1 C01–C21; 21 подписанная строка stdout. |
| enum.expected.stdout | Точный независимый эталон первого JVM-процесса. |
| control.expected.stdout | Точный независимый эталон второго JVM-процесса. |
| verify.sh | Компиляция, два отдельных запуска JVM и два строгих сравнения diff -u. |

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
- E11–E14: EnumSet/EnumMap обходят ключи в порядке объявления enum; EnumMap допускает null как значение и сохраняет различие между отсутствующим ключом и ключом с null.
- E15–E19, E28–E29: EnumSet.add(null) и EnumMap.put(null, ...) бросают NullPointerException, тогда как contains(null)/remove(null) у EnumSet и containsKey(null) у EnumMap возвращают false; valueOf различает неизвестное имя и null.
- E20: enum-константа может содержать изменяемое состояние. Сам факт enum singleton не делает это состояние immutable или потокобезопасным.
- E21–E24: у константы с собственным телом getClass() отличается от getDeclaringClass(); Class.isSealed() показывает различие между обычным enum и enum с constant-specific body.
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

## Границы проверки

Скрипт проверяет только две positive-программы и точное совпадение stdout на установленной JDK 21. Исследовательские compile-negative N01–N26/N28–N32 из G1 §8 намеренно не входят в обычный runner/CI. Он не проверяет другие версии JDK/JVM, ОС, JMM/thread-safety, binary evolution enum, JPMS, JIT/GC, производительность или benchmark. Эти свойства нельзя выводить из двух stdout-трассировок.
