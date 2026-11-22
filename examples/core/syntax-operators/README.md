# CORE-SYNTAX + CORE-OPERATORS — общий runnable Java 21

Два автономных исходника и настоящая двухфайловая компиляция `package/static import` из [принятого плана G1 §6](../../../docs/plans/CORE-SYNTAX-OPERATORS.md). Подробная теория: [CORE-SYNTAX](../../../knowledge/01-java-core/syntax.md) и [CORE-OPERATORS](../../../knowledge/01-java-core/operators.md). **44 подписанных строки stdout трёх JVM-процессов — не 44 unit/JUnit-теста.**

## Команда запуска

Нужны Java/Javac 21, Bash, mktemp, diff (Linux/macOS или Git Bash); из корня проекта:

~~~bash
bash examples/core/syntax-operators/verify.sh
~~~

Скрипт создаёт временную директорию `mktemp -d` и удаляет её по `trap`. Он **одной командой** `javac --release 21 -Xlint:all -d` компилирует четыре .java в неё; любые warning/diagnostic в stderr или ошибка компиляции приводят к FAIL. Затем выполняет **три отдельных `java -cp` процесса**, требует пустой stderr и сравнивает **три полных stdout** с отдельными эталонами через `diff -u`. При успехе: `CHECK PASSED: Java 21 syntax/operators/package; 14+27+3 stdout lines (three JVM processes)`. Никаких .class/actual.stdout в Git-дереве не остаётся.

## Файлы и ожидаемые наблюдения

| Java21 исходники | Отдельный эталон | Количество |
| --- | --- | --- |
| [src/SyntaxProbe.java](src/SyntaxProbe.java) | [syntax.expected.stdout](syntax.expected.stdout) | SY01–SY14 (14) |
| [src/OperatorsProbe.java](src/OperatorsProbe.java) | [operators.expected.stdout](operators.expected.stdout) | OP01–OP27 (27) |
| [src/p/Helper.java](src/p/Helper.java) + [src/demo/PackageProbe.java](src/demo/PackageProbe.java) | [package.expected.stdout](package.expected.stdout) | PK01–PK03 (3) |

## Что объясняет SyntaxProbe

SY01=7:67:8 показывает local shadowing поля класса; SY02=14 — параметр и `this.balance`; SY03=7 — работающий `import static Math.max`; SY04=2 и SY09=67 — импорт типа и fully qualified class. SY05=68 — захват effectively-final локальной переменной в лямбде; SY06=no и SY07=yes — definite assignment по обеим ветвям if/else. SY08=Java21 — обычный `var` local; SY10=true, SY11=SyntaxProbe, SY12=true — обычный Java21 main/обращение к классу; SY13=7 и SY14=5 — scope вложенного блока.

## Что объясняет OperatorsProbe

- **OP01=7 vs OP02=9:** приоритет и скобки задают группировку, но не отменяют порядок вычисления операндов слева направо.
- **OP03=67:68, OP04=69:69, OP05=4:** post-/pre-increment; `x=x++` оставляет старую величину.
- **OP06=-32768 / OP07=-2147483648 / OP08=-2:-1:** `short +=1` с неявным сужением, переполнение int, деление с усечением к нулю и остаток. OP09–OP11 — masked shift distance.
- **OP12=false:0, OP14=true:0** показывают lazy `&&` и `||`; **OP13=false:1, OP15=true:1** — eager boolean `&` и `|`.
- **OP16=3x vs OP17=x12:** порядок строковой конкатенации; OP18=1, OP19=1.0:Double — правила типа `?:` и numeric promotion.
- **OP20=4:** `a += (a=3)` использует ранее сохранённое значение LHS=1. **OP21=67:LR:** аргументы метода в Java гарантированно вычисляются слева направо, а не случайно.
- **OP22=false** для `NaN == NaN`, **OP23=-1** для `Double.compare(-0.0, 0.0)`, OP24=67:0 — вычисляется только выбранная ветка `?:`.
- **OP25=3:3** — ассоциативность присваивания, **OP26=2:1** — целочисленные деление и остаток.
- **OP27=ArithmeticException:1** — деление на ноль вызывает исключение **во время выполнения** после side effect. `int n=1/0` в Java21 — compile-positive; `-Xlint:all` может выдать warning `[divzero]`, поэтому полный G1-позитив получает zero динамически.

## Почему package-пример двухфайловый

`src/p/Helper.java` объявляет `package p;` и публичный тип `Helper`; `src/demo/PackageProbe.java` объявляет `package demo;`, использует обычный `import p.Helper` и два `import static`. **PK01=hello:67** подтверждает импорт метода и значения, **PK02=67** — доступ по имени импортированного типа, **PK03=2** — fully-qualified `java.util.List`. Обе compilation units компилируются совместно с `-d`; третья JVM запускает `demo.PackageProbe` по полному имени. Одним исходником без зависимостей этот контракт не воспроизвести.

## Границы проверки

Это позитивный runner: 4 Java source, 3 JVM и 3 stdout fixtures, без Gradle/JUnit/Mockito-модуля. **33 изолированные compile-negative** (32 отдельных и двухфайловый SY-N15) исследованы в G1 отдельно по диагностическим ключам, но *не входят* в обычный runner/CI; как и проверка всех snippets Markdown, иных JDK/OS, JPMS/JMM/JIT/JMH/GC/performance, визуального поведения Obsidian/IntelliJ, Markdown anchors/HTTP-ссылок.
