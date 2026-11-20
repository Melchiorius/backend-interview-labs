# CORE-EQUALITY — равенство, хэши и ключи коллекций (Java 21)

Один автономный пример соединяет **правильный immutable value key** и четыре намеренно проблемных ситуации: коллизия хэшей без равенства, нарушение `equals`/`hashCode`, асимметрия равенства при наследовании и изменение ключа после вставки. Последняя часть показывает, почему `HashSet` и `TreeSet` могут по-разному понимать «один элемент». Это готовая демонстрация, **не самостоятельное упражнение**.

Всё запускается из одного [Java-исходника](src/EqualityDemo.java), без Gradle-подпроекта и внешних зависимостей. Полный ожидаемый вывод хранится отдельно в [expected.stdout](expected.stdout), а [verify.sh](verify.sh) проверяет его побайтно. Это **не** репозиторная реализация всех 14 проб из исследовательского плана и не тест всех возможных коллекций/JVM.

**Теория для этого примера:** [CORE-EQUALITY — контракт equals/hashCode, ключи и порядок](../../../knowledge/01-java-core/equality.md), [CORE-OBJECT — identity и базовые методы](../../../knowledge/01-java-core/object.md), [CORE-TYPES — типы и ссылки](../../../knowledge/01-java-core/types.md), [CORE-CLASSES](../../../knowledge/01-java-core/classes.md), [CORE-INHERITANCE](../../../knowledge/01-java-core/inheritance.md). Теперь статьи и Java-пример физически находятся в одном интегрированном дереве. Исследовательская основа: [принятый G1 PR #61 @ eede792e](https://github.com/Melchiorius/backend-interview-labs/blob/eede792ea16005e9087f4bc8eca5617c73c79761/docs/plans/CORE-OBJECT-EQUALITY.md) (не предполагается объединённым в main). Нормативные требования — Java SE 21 [Object](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html), [Map](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html), [Comparable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Comparable.html), [Comparator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html), [BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html).

## Точные команды из корня репозитория

Установить JDK 21 с командами `javac` и `java` в `PATH`. Компиляция и запуск (Linux/macOS, Windows PowerShell или Git Bash):

```sh
javac --release 21 -d examples/core/equality/build/classes examples/core/equality/src/EqualityDemo.java
java -cp examples/core/equality/build/classes EqualityDemo
```

Каталог `build/` уже игнорируется корневым `.gitignore`. Для независимого запуска **с проверкой точного stdout** из корня (Bash, включая Git Bash в Windows):

```sh
java -version
javac -version
bash examples/core/equality/verify.sh
```

Скрипт пересоздаёт только `examples/core/equality/build/`, выполняет `javac --release 21`, запускает программу, сравнивает `expected.stdout` с `build/actual.stdout` через `diff -u` и **завершается с ненулевым exit code при расхождении**. Если сравнение успешно, печатает весь stdout, затем собственную строку `CHECK PASSED: stdout matches expected.stdout` — последняя строка скрипта **не входит** в сохранённый вывод Java-программы.

## Полный ожидаемый stdout Java-программы

```text
== Identity and correct immutable key ==
Different references: true
Equal values and matching hashes: true/true
Unequal hash-collision keys in HashSet: 2
HashMap retrieves colliding keys: left/right
== Deliberately broken keys (OpenJDK 21 observation) ==
Broken hash: equal/hash-equal=true/false
Broken hash: HashMap lookup=null
Asymmetric equals: parent->child/child->parent=true/false
Mutable key: lookup before/after=stored/null
== Hash equality vs sorted equality ==
BigDecimal equals/compareToZero=false/true
BigDecimal HashSet/TreeSet=2/1
Case-insensitive String HashSet/TreeSet=2/1
Nested arrays shallow/deep=false/true
Objects.equals(null, null)=true
CHECK PASSED
```

## Механизмы и ограничения

**1. Правильный value key.** `ValueKey` — `final`, оба значимых поля (`id`, `code`) неизменяемы; `equals(Object)` сравнивает те же данные, из которых вычисляется `hashCode()`. Разные ссылки `new ValueKey(67,"FB")` равны по значению. Отдельно проверены рефлексивность, симметрия, транзитивность и неравенство с `null`. `"FB"` и `"Ea"` имеют одинаковый `String.hashCode`, поэтому для выбранной формулы `ValueKey.hashCode` получаем **коллизию** `ValueKey(67,"FB")` / `ValueKey(67,"Ea")`, но эти значения **не равны**. Оба хранятся в `HashSet` и независимо извлекаются из `HashMap`. Контракт требует `equals ⇒ same hash`, но **не** `same hash ⇒ equals`.

**2. Намеренное нарушение hash-контракта.** `BadHashKey.equals` сравнивает только `id`, а `BadHashKey.hashCode` возвращает отдельный `salt`. Получается `new BadHashKey(67,1).equals(new BadHashKey(67,2)) == true`, хотя хэши 1 и 2. В конкретном запуске **OpenJDK 21** `HashMap.get` по второму ключу возвращает `null`. Нельзя выводить из этого, что любая реализация `Map` обязана совершить точно такую же ошибку поиска: **причина — нарушение контракта со стороны типа ключа**; отсутствие результата — наблюдение в указанной реализации. Восстанавливать правильность нужно через согласованный `equals`/`hashCode`, а не через подбор `HashMap`.

**3. Нарушение симметрии при наследовании.** `ParentKey.equals` принимает любой `ParentKey` по `id`, тогда как `ChildKey.equals` требует `ChildKey` и дополнительно проверяет `label`. Поэтому `parent.equals(child) == true`, а `child.equals(parent) == false`. Оба `hashCode` здесь совпадают, но это **не исправляет** несоблюдение симметрии. Такой способ расширять value equality у наследника нельзя считать корректным общим рецептом.

**4. Изменяемый ключ.** `MutableKey` имеет поле `id`, которое участвует и в `equals`, и в `hashCode`. После `put(mutable, "stored")` меняем `id: 67 → 68`. В данном запуске OpenJDK 21 поиск возвращает `null`, хотя запись была добавлена. Контракт [`Map`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html) прямо оговаривает, что поведение при изменении ключа способом, влияющим на equality, **не специфицировано**. Не утверждайте, что после каждой мутации на любой JVM `get` обязательно вернёт `null`: возможен другой наблюдаемый эффект. Практический вывод — использовать стабильные ключи, а не полагаться на особенности bucket layout.

**5. Hash equality и sorted equality — не одно и то же.** `new BigDecimal("2.0").equals(new BigDecimal("2.00"))` даёт `false`, так как учитывается `scale`, а `compareTo(...) == 0` — `true`. В `HashSet` сохраняются оба элемента (`size=2`), в `TreeSet` с естественным порядком — один (`size=1`). Аналогичный пример с `String.CASE_INSENSITIVE_ORDER`: `"a"` и `"A"` различаются через `String.equals`, но компаратор сравнивает их как равные. Это намеренно несогласованный с `equals` порядок: соответствующие отсортированные коллекции при нём не соблюдают общий контракт `Set`/`Map`.

**6. Дополнительные маленькие контрасты.** `Arrays.equals` для двумерных `int[][]` на верхнем уровне сравнивает ссылки на вложенные массивы, поэтому здесь `false`; `Arrays.deepEquals` сравнивает содержимое рекурсивно, поэтому `true`. `Objects.equals(null,null)` — `true` без `NullPointerException`.

## Границы воспроизводимости

Локальная проверка роли 03: **OpenJDK 21.0.12.1 / `javac 21.0.12.1`**, точные команды 

```sh
javac --release 21 -d examples/core/equality/build/classes examples/core/equality/src/EqualityDemo.java
java -cp examples/core/equality/build/classes EqualityDemo
bash examples/core/equality/verify.sh
```

завершились с exit code **0**, `diff -u` подтвердил точное совпадение ожидаемого stdout; команда `java` не использует флаг `-ea`, поскольку все проверки вызывают собственный `check` с явным `AssertionError`. Не проверялись другие JDK и альтернативные реализации `Map`, визуальное отображение Obsidian/IntelliJ, property-based testing, многопоточные изменения ключей и детали внутреннего устройства `HashMap`. Отсутствуют отрицательные файлы с `javac`-ошибками: эти ошибки **семантические**, классы специально компилируются и должны быть исполняемыми.
