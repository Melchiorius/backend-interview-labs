# CORE-INHERITANCE — runnable Java 21 example

**Назначение:** один многофайловый пример, в котором один объект наследника используется через ссылку базового типа. Это не самостоятельное упражнение, новой системы сборки не требуется.

**Теория:** [Классы](../../../knowledge/01-java-core/classes.md), [доступ](../../../knowledge/01-java-core/access.md), [типы ссылок](../../../knowledge/01-java-core/types.md), [перегрузки](../../../knowledge/01-java-core/conversions.md), [параметры](../../../knowledge/01-java-core/parameters.md); [исследовательский план CORE-INHERITANCE](../../../docs/plans/CORE-INHERITANCE.md). Статья CORE-INHERITANCE готовится по Issue #38: на несуществующий файл пока не ссылаемся.

## Участники и механизм

В `src/base/Base.java` класс Base объявляет describe(Object), статический kind(), поле code = 67, final-методы, Number result(), read() throws IOException, private и package-private методы. В `src/client/Derived.java` из другого пакета наследник переопределяет describe(Object), вводит describe(String), скрывает kind() и поле code = 68, уточняет результат до Integer и checked throws до FileNotFoundException. В `src/client/InheritanceDemo.java` переменная Base ref и переменная Derived derived указывают **на один объект Derived**.

Для `ref.describe("67")` компилятор выбирает доступный по Base метод describe(Object), а при выполнении запускается его override в Derived. Поэтому `derived:object`. Для `derived.describe("67")` компилятор видит перегрузку describe(String) и выбирает её: `derived:string`. Приведение `((Base)this)` не отключает виртуальный dispatch, в отличие от специального вызова `super.describe(...)`. Статические методы и поля выбираются по compile-time типу, не по фактическому классу объекта. Вызов static через переменную показан как ловушка, а не рекомендуемый стиль.

Приватный метод Base не переопределяется одноимённым методом Derived. Package-private метод Base из чужого пакета также не переопределяется: это два отдельных объявления. Пример показывает допустимое сужение return Number → Integer и checked IOException → FileNotFoundException.

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
CHECK PASSED
~~~

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

## Проверки и границы

В локальном тестовом дереве на OpenJDK/javac **21.0.12.1** действительно выполнено `bash examples/core/inheritance/verify.sh`: exit **0**, `CHECK PASSED`, 12 ожидаемых `EXPECTED REJECTION`, `ALL EXPECTED OUTCOMES OBSERVED`. CI для опубликованного HEAD нужно отдельно подтвердить, Obsidian/IntelliJ — **not run**. Успешная проверка нескольких фрагментов не доказывает всех правил JLS.

Порядок инициализации и опасный virtual call из конструктора относятся к будущему CORE-INIT; interface default-конфликты — CORE-INTERFACES, LSP и композиция — CORE-COMPOSITION. Полный байткод, bridge/generics и JIT не включены.
