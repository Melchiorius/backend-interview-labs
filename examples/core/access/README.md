# CORE-ACCESS — доступ между пакетами Java 21

**Назначение:** показать, *какое именно выражение компилируется* в двух настоящих пакетах. Это готовый запускаемый пример, **не упражнение с заготовкой**.

**Теоретическая основа:** [CORE-ACCESS — модификаторы доступа и инкапсуляция](../../../knowledge/01-java-core/access.md), [классы и инварианты](../../../knowledge/01-java-core/classes.md), [статический тип ссылки](../../../knowledge/01-java-core/types.md), [приведение типов](../../../knowledge/01-java-core/conversions.md). Нормативная карта и первоисточники — [исследование CORE-ACCESS](../../../docs/plans/CORE-ACCESS.md), прежде всего JLS 21 §6.6.2.1 (поля/методы) и §6.6.2.2 (конструкторы).

## Участники и смысл обращений

- [`api.Parent`](src/api/Parent.java) — базовый класс с `protected int value = 67` (поле экземпляра), `protected static int total = 67` (поле класса), package-private `packageValue`, защищённым конструктором и методом `touch()`.
- [`api.Neighbor`](src/api/Neighbor.java) — **не наследуется** от `Parent`, но находится в том же пакете. Поэтому может читать `value`, `packageValue`, `total` и вызывать `new Parent()`.
- [`client.Child`](src/client/Child.java) — подкласс в **другом** пакете. Может использовать `super()`, `this.value`, `otherChild.value`, `Parent.total`, но не `parent.value`, где `parent` имеет статический тип `Parent`.
- [`client.Sibling`](src/client/Sibling.java) — независимый подкласс `Parent` в пакете `client`, **не подкласс `Child`**. Общий предок не разрешает `Child` читать `sibling.value`.
- [`client.AccessDemo`](src/client/AccessDemo.java) — точка входа с проверками. Сам не наследуется от `Parent`, но создаёт `new Parent() {}`: анонимный подкласс с вызовом protected-конструктора по особому правилу JLS §6.6.2.2.
- [`api.InterfaceTypes`](src/api/InterfaceTypes.java) — типы-члены интерфейса неявно `public static` (JLS §9.5).

- [`api.Hidden`](src/api/Hidden.java) — package-private класс с public полем `value`, методом `read()` и статическим типом-членом `Visible`.
- [`api.Exported`](src/api/Exported.java) — public подкласс `Hidden`, не переобъявляющий эти члены. Клиент из другого пакета выбирает их через доступный тип `Exported` (JLS 21 [§6.6.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6.1), [§6.5.5.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.5.5.2), [§8.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.2)).

## Положительная проверка

Из **корня репозитория**, JDK 21 (Windows PowerShell, Linux/macOS):

```text
javac --release 21 -d examples/core/access/build/classes -sourcepath examples/core/access/src examples/core/access/src/client/AccessDemo.java
java -cp examples/core/access/build/classes client.AccessDemo
```

Ожидаются две строки (девять прежних и три дополнительные проверки выполняются до итогового маркера):

```text
public_inherited=67:67:Visible
CHECK PASSED
```

Клиент создаёт `Exported`, читает `e.value`, вызывает `e.read()`, создаёт `new Exported.Visible()` и проверяет тип и возвращаемую методом `label()` строку. Имя `Hidden` непосредственно недоступно из `client`, но это не запрещает обращение к унаследованным public-членам через доступный подкласс. `Hidden.Visible` и `Exported.Visible` не два разных вложенных класса. Пример не использует рефлексию и не переобъявляет члены.
 Через `-sourcepath` компилятор находит все зависимости по пакетам. Скомпилированные `.class` лежат в исключённом из Git каталоге `build/`. Gradle, JUnit и Mockito не нужны.

## Независимые отрицательные случаи

Каждый из десяти файлов каталога `negative/` содержит одно намеренно **недопустимое** обращение. Они никогда не включаются в общую успешную компиляцию. Проверять по одному, читать диагностику по смыслу, не сравнивать её точный текст с шаблоном.

| Файл | Запрещённое выражение | Объяснение |
|---|---|---|
| [`BadParentValue.java`](negative/BadParentValue.java) | `parent.value` | Статический тип квалификатора `Parent`, не текущий подкласс |
| [`BadParentCast.java`](negative/BadParentCast.java) | `((Parent) child).value` | Приведение меняет статический тип выражения на `Parent`, объект остаётся тем же |
| [`BadSibling.java`](negative/BadSibling.java) | `sibling.value` | `Sibling` и текущий класс — разные наследники `Parent` |
| [`BadPackageValue.java`](negative/BadPackageValue.java) | `packageValue` | package-private не доступен подклассу из `client` |
| [`BadNewParent.java`](negative/BadNewParent.java) | `new Parent()` | Protected-конструктор не разрешает обычное создание экземпляра вне `api` |
| [`BadStaticUnrelated.java`](negative/BadStaticUnrelated.java) | `Parent.total` | В постороннем классе `client` protected static недоступен |
| [`BadPrivateInterfaceMember.java`](negative/BadPrivateInterfaceMember.java) | `private class Member` в интерфейсе | §9.5 запрещает private тип-член интерфейса |
| [`BadProtectedInterfaceMember.java`](negative/BadProtectedInterfaceMember.java) | `protected interface Nested` в интерфейсе | §9.5 запрещает protected тип-член интерфейса |
| [`BadHiddenName.java`](negative/BadHiddenName.java) | `api.Hidden h;` | Сам package-private тип недоступен из другого пакета |
| [`BadHiddenVisible.java`](negative/BadHiddenVisible.java) | `api.Hidden.Visible v;` | public тип-член назван через недоступный квалифицирующий тип |

**Контраст instance и static:** в `Child` допустимо `Parent.total`, но не `parent.value` при параметре типа `Parent`. Ограничение на статический тип квалификатора из JLS §6.6.2.1 относится к **экземплярным** членам, а не к static. Это не делает static-член публичным.

**Контраст конструктора:** `Child()` использует `super()`; обычный `new Parent()` вне пакета запрещён даже внутри подкласса, а `new Parent() {}` создаёт анонимный подкласс по отдельному правилу.

После положительной компиляции выполните из корня один отрицательный случай:

```text
javac --release 21 -cp examples/core/access/build/classes -d examples/core/access/build/negative examples/core/access/negative/BadParentValue.java
```

Ожидается ненулевой код завершения и сообщение о запрещённом обращении **именно в соответствующей строке**. Для следующего случая замените имя файла. Падение из-за неверного пути или отсутствующего JDK не считается подтверждением ожидаемого поведения.

Для последовательного независимого запуска всех случаев (Linux/macOS или Git Bash в Windows):

```sh
bash examples/core/access/verify.sh
```

Скрипт запускает положительный пример, затем отдельный `javac` для **каждого** отрицательного файла, выводит фактическую диагностику и проверяет ненулевой exit code. Сверка причины ошибки с JLS остаётся содержательной, а не машинной проверкой точной локализованной строки ошибки. В PowerShell без Bash можно повторять указанную команду с разными именами файлов и проверять `$LASTEXITCODE` после каждой.

Для двух новых случаев после положительной компиляции можно дополнительно получить категории диагностик javac:

```sh
javac --release 21 -XDrawDiagnostics -cp examples/core/access/build/classes -d examples/core/access/build/negative/BadHiddenName examples/core/access/negative/BadHiddenName.java
javac --release 21 -XDrawDiagnostics -cp examples/core/access/build/classes -d examples/core/access/build/negative/BadHiddenVisible examples/core/access/negative/BadHiddenVisible.java
```

Создайте соответствующие выходные каталоги заранее (runner уже создаёт их). На javac 21.0.12.1 отдельно наблюдались exit=1 и `compiler.err.not.def.public.cant.access` (Hidden не public), `compiler.err.not.def.access.class.intf.cant.access` (Hidden.Visible назван через недоступный Hidden). Это разные причины, не отсутствие классов или синтаксическая ошибка. Точный текст/ключ диагностики зависит от компилятора. Полный runner сохраняет восемь прежних отрицательных случаев и проверяет десять отдельных компиляций; итоговый маркер — `ALL EXPECTED OUTCOMES OBSERVED`. Как и ранее, причины диагностики проверяются содержательно.

## Границы

Сценарии — обычная Java 21 без явных именованных модулей. JPMS, reflection, nestmates, полное overriding и runtime dispatch не исследуются в этом примере. Инварианты предметного API уже разобраны в [ClassesAndObjectsDemo.java](../ClassesAndObjectsDemo.java), не дублируются. Прохождение проверок **не доказывает всю спецификацию Java**.
