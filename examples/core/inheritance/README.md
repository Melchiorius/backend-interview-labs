# CORE-INHERITANCE — запускаемый пример на Java 21

**Назначение:** один многофайловый пример, в котором один объект наследника используется через ссылку базового типа. Это не самостоятельное упражнение, новой системы сборки не требуется.

**Теория:** [Классы](../../../knowledge/01-java-core/classes.md), [доступ](../../../knowledge/01-java-core/access.md), [типы ссылок](../../../knowledge/01-java-core/types.md), [перегрузки](../../../knowledge/01-java-core/conversions.md), [параметры](../../../knowledge/01-java-core/parameters.md); [исследовательский план CORE-INHERITANCE](../../../docs/plans/CORE-INHERITANCE.md). Самостоятельная [статья CORE-INHERITANCE](../../../knowledge/01-java-core/inheritance.md) представлена вместе с этим примером на общей ветке PR #43; до публикации в `main` оба материала остаются черновиками.

## Участники и механизм

В [`base.Base`](src/base/Base.java) объявлены метод `describe(Object)`, статический `kind()`, поле `code = 67`, `final`-методы, `Number result()`, `read() throws IOException`, а также `private` и package-private методы. В [`client.Derived`](src/client/Derived.java) из другого пакета подкласс переопределяет `describe(Object)`, добавляет перегрузку `describe(String)`, скрывает `kind()` и поле `code` собственным значением `68`, уточняет результат до `Integer` и объявляет более узкое проверяемое исключение `FileNotFoundException`. В [`client.InheritanceDemo`](src/client/InheritanceDemo.java) переменные `Base ref` и `Derived derived` указывают **на один объект `Derived`**.

Для `ref.describe("67")` компилятор видит у статического типа `Base` метод `describe(Object)`, а при выполнении запускается его переопределение в объекте `Derived`. Результат — `derived:object`. Для `derived.describe("67")` компилятор видит дополнительную перегрузку `describe(String)` и выбирает её: `derived:string`. Приведение `((Base) this)` не отключает виртуальную диспетчеризацию, в отличие от специального вызова `super.describe(...)`. Статические методы и поля выбираются по статическому типу выражения, а не по фактическому классу объекта. Вызов static через переменную показан как ловушка, а не рекомендуемый стиль.

Приватный метод Base не переопределяется одноимённым методом Derived. Package-private метод Base из чужого пакета также не переопределяется: это два отдельных объявления. Пример показывает допустимое сужение return Number → Integer и checked IOException → FileNotFoundException.

### Независимые объявления static и instance (B2b)

В [IndependentBase](src/base/IndependentBase.java) объявлены private static final privateStatic(), private final экземплярный privateInstance() и package-private static final foreignStatic(). В [IndependentDeclarationsDemo](src/client/IndependentDeclarationsDemo.java), прямом public подклассе из другого пакета, одноимённые методы имеют противоположные модификаторы static/instance. Родительская и дочерняя ссылки указывают на один объект. В первой строке private-final значения идут в порядке: родительская обёртка для private static (67), дочерний instance (68), родительская обёртка для private instance (67), дочерний static через имя подкласса (68). Private методы здесь не переопределяются и не скрываются. Final ограничивает действительное переопределение/скрытие, а не независимое объявление.

Вторая строка foreign_static сравнивает вызов родительской обёртки callForeignStatic() (67) и дочернего instance foreignStatic() (68): исходный package-private static final недоступен прямому подклассу из другого пакета. Это независимое объявление, не override/hide. Не распространяйте этот пример на транзитивное переопределение или другие ограничения объявления. См. JLS 21 [§8.4.8.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.1), [§8.4.8.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.2), [§8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3).

Это исполняемые контрасты к отдельным учебным фрагментам статьи, а не их буквальное копирование: runner объединяет оба случая в одной прямой иерархии; порядок private static → instance, затем private instance → static отличается от порядка в статье. InheritanceDemo вызывает IndependentDeclarationsDemo.verify() перед CHECK PASSED; добавлено шесть вызовов check к прежним шестнадцати. Негативные BadStaticForInstance и BadInstanceForStatic остаются корректными: в них доступные методы родителя образуют запрещённый конфликт. Остальные десять отрицательных примеров также сохранены.

## Запуск из корня репозитория

Установить **JDK 21**, чтобы были доступны java и javac. Linux/macOS или Git Bash (Windows):

~~~sh
java -version
javac -version
bash examples/core/inheritance/verify.sh
~~~

Скрипт собирает рабочий пример, запускает client.InheritanceDemo и затем **по одному** проверяет 12 файлов из каталога negative/. Все результаты и stderr лежат в игнорируемом Git каталоге build/. Отрицательные файлы не входят в успешную компиляцию.

PowerShell — самостоятельный запуск положительного сценария:

~~~powershell
javac --release 21 -d examples/core/inheritance/build/classes -sourcepath examples/core/inheritance/src examples/core/inheritance/src/client/InheritanceDemo.java
java -cp examples/core/inheritance/build/classes client.InheritanceDemo
~~~

После положительной компиляции можно запустить отдельный отрицательный пример:

~~~powershell
javac --release 21 -cp examples/core/inheritance/build/classes -d examples/core/inheritance/build/negative examples/core/inheritance/negative/BadOverrideFinal.java
$LASTEXITCODE
~~~

Ожидается ненулевой exit code; убедитесь по сообщению компилятора, что запрещён именно override final-метода.

### Ожидаемый вывод

~~~text
Base reference + String argument: derived:object
Derived reference + String argument: derived:string
super vs cast: base:object / derived:object
static method: base:static / derived:static
field: 67 / 68 / 67
covariant + throws: 68 / derived:read
private/package: base:private / base:package
private-final=67:68:67:68
foreign_static=67:68
CHECK PASSED
~~~

Изолированный запуск только новых положительных контрастов (из корня, Linux/macOS/Git Bash):

~~~sh
mkdir -p examples/core/inheritance/build/standalone
javac --release 21 -XDrawDiagnostics -d examples/core/inheritance/build/standalone -sourcepath examples/core/inheritance/src examples/core/inheritance/src/client/IndependentDeclarationsDemo.java
java -cp examples/core/inheritance/build/standalone client.IndependentDeclarationsDemo
~~~

Ожидаются две новые строки private-final и foreign_static из вывода выше, без CHECK PASSED (это маркер общего InheritanceDemo).

## Преднамеренно некомпилируемые случаи

| Файл | Причина отказа |
|---|---|
| [BadExtendsFinal](negative/BadExtendsFinal.java) | Нельзя наследоваться от final String |
| [BadOverrideFinal](negative/BadOverrideFinal.java) | Нельзя переопределять final instance-метод |
| [BadHideFinalStatic](negative/BadHideFinalStatic.java) | Нельзя скрывать final static-метод |
| [BadStaticForInstance](negative/BadStaticForInstance.java) | static вместо inherited instance |
| [BadInstanceForStatic](negative/BadInstanceForStatic.java) | instance вместо inherited static |
| [BadOverrideOverload](negative/BadOverrideOverload.java) | @Override на новой перегрузке |
| [BadCovariantReturn](negative/BadCovariantReturn.java) | Object шире допустимого Number |
| [BadCheckedThrows](negative/BadCheckedThrows.java) | Объявлен более широкий checked Exception |
| [BadNarrowAccess](negative/BadNarrowAccess.java) | Снижение protected до package-private |
| [BadOverridePrivate](negative/BadOverridePrivate.java) | Чужой private-метод не override |
| [BadOverridePackage](negative/BadOverridePackage.java) | Чужой package-private из другого пакета не override |
| [BadReturnOnlyOverload](negative/BadReturnOnlyOverload.java) | По возвращаемому типу перегрузки нет |

Скрипт проверяет ненулевой exit code **и характерную причину ошибки** по английским диагностическим фрагментам OpenJDK 21. Точные строки диагностик javac — не гарантия JLS; на другой локали или компиляторе тестовый скрипт может потребовать адаптации. Ошибки сохраняются в build/negative/<имя>/stderr.log.

Для отдельной проверки категорий javac двух доступных конфликтов после общего runner:

~~~sh
javac --release 21 -XDrawDiagnostics -cp examples/core/inheritance/build/classes -d examples/core/inheritance/build/negative/BadStaticForInstance examples/core/inheritance/negative/BadStaticForInstance.java
javac --release 21 -XDrawDiagnostics -cp examples/core/inheritance/build/classes -d examples/core/inheritance/build/negative/BadInstanceForStatic examples/core/inheritance/negative/BadInstanceForStatic.java
~~~

На OpenJDK 21.0.12.1 фактически получены exit 1 и соответственно compiler.err.override.static / compiler.misc.cant.override и compiler.err.override.meth / compiler.misc.cant.override (static). Диагностики относятся к доступным методам Base, а не к недостающим зависимостям.

## Проверки и границы

В локальном тестовом дереве на OpenJDK/javac **21.0.12.1** действительно выполнено `bash examples/core/inheritance/verify.sh`: exit **0**, `CHECK PASSED`, 12 ожидаемых `EXPECTED REJECTION`, `ALL EXPECTED OUTCOMES OBSERVED`. CI для опубликованного HEAD нужно отдельно подтвердить, Obsidian/IntelliJ — **not run**. Успешная проверка нескольких фрагментов не доказывает всех правил JLS.

Порядок инициализации и опасный virtual call из конструктора относятся к будущему CORE-INIT; interface default-конфликты — CORE-INTERFACES, LSP и композиция — CORE-COMPOSITION. Полный байткод, bridge/generics и JIT не включены.
