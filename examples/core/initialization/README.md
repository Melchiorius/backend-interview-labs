# CORE-INIT — порядок инициализации в Java 21

Один автономный пример показывает **два связанных этапа** создания объекта: инициализацию классов/интерфейсов и инициализацию полей/конструкторов конкретного экземпляра. Основной исходник намеренно оставлен одним файлом: вложенные типы позволяют увидеть причинно-следственный порядок в одном запуске, не создавая лишний source set или Gradle-модуль.

**Основной исходник:** [src/InitializationDemo.java](src/InitializationDemo.java)  
**Полный ожидаемый stdout основного примера:** [expected.stdout](expected.stdout)  
**Проверка точного вывода:** [verify.sh](verify.sh)

Теоретические предпосылки: [CORE-INIT — порядок инициализации](../../../knowledge/01-java-core/initialization.md), [CORE-CLASSES — поля и конструкторы](../../../knowledge/01-java-core/classes.md), [CORE-INHERITANCE — переопределение и динамический вызов](../../../knowledge/01-java-core/inheritance.md). Нормативная основа — JLS 21 §§4.12.4–4.12.5, 8.3.2, 8.6–8.8, 9.4, 12.4.1–12.4.2 и 12.5. [План CORE-INIT](../../../docs/plans/CORE-INIT.md), [JLS §15.9.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9.4), [§11.2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.3).

## Запуск из корня репозитория

Требуется JDK 21 (java и javac). Сначала можно проверить версии:

~~~sh
java -version
javac -version
~~~

Ручная компиляция и запуск:

~~~sh
javac --release 21 -d examples/core/initialization/build/classes examples/core/initialization/src/InitializationDemo.java
java -cp examples/core/initialization/build/classes InitializationDemo
~~~

Ожидаемый stdout программы полностью сохранён в expected.stdout и приведён ниже. Для автоматического сравнения фактического вывода с сохранённым файлом выполните:

~~~sh
bash examples/core/initialization/verify.sh
~~~

Скрипт удаляет и пересоздаёт только локальный examples/core/initialization/build/, компилирует исходник с --release 21, запускает InitializationDemo, сравнивает stdout через diff -u и завершает работу с ненулевым кодом при любом расхождении. После исходного вывода скрипт отдельно проверяет два случая неявного super() (см. B3 ниже), затем печатает исходный финальный маркер CHECK PASSED: stdout matches expected.stdout. Каталог build/ уже исключён существующим .gitignore.

PowerShell — те же команды из корня:

~~~powershell
javac --release 21 -d examples/core/initialization/build/classes examples/core/initialization/src/InitializationDemo.java
java -cp examples/core/initialization/build/classes InitializationDemo
~~~

## Полный ожидаемый stdout основного примера

~~~text
== 1. Compile-time constant ==
Child.CONSTANT = 67
== 2. Inherited non-constant static field ==
Parent.static field
Parent.BOXED field
Parent.static block
Child.BOXED = 67
== 3. Child class initialization ==
RootDefault.<clinit>
LeafDefault.<clinit>
Child.static field
Child.static block
Child.OWN = 68
== 3a. Direct interface initialization ==
QuietChild.<clinit>
QuietChild.MARK = 1
== 4. Instance construction ==
Parent.instance field
Parent.instance block
Parent.constructor: before virtual call
Child.override: childValue=0, childLabel=null
Parent.constructor: after virtual call
Child.instance field
Child.instance reference field
Child.instance block
Child(int) constructor body: seed=67
Child() constructor body
== 5. Final object state ==
state=67/68/ready
~~~

## Как читать последовательность

### 1. Constant variable не запускает инициализацию

Parent.CONSTANT — static final int, инициализированный константным выражением. Поле унаследовано Child, поэтому Child.CONSTANT допустимо. Значение 67 подставляется при компиляции: до первой строки с Child.CONSTANT не появляется ни одного маркера Parent или Child.

### 2. Унаследованное поле запускает класс, который его объявляет

Child.BOXED — не constant variable: тип Integer не подходит под определение JLS §4.12.4, хотя поле static final. Поле объявлено в Parent, а не в Child; обращение через имя Child инициализирует **Parent**, но не Child. Поэтому видны Parent.static field, Parent.BOXED field и Parent.static block, а маркеров Child ещё нет. Статические поля и блоки одного класса выполняются в текстовом порядке; constant variables обрабатываются отдельным ранним шагом процедуры §12.4.2.

### 3. При инициализации класса учитываются интерфейсы с default methods

Обращение к Child.OWN, объявленному непосредственно в Child, запускает инициализацию Child. Суперкласс Parent уже инициализирован. Далее виден рекурсивный обход релевантных superinterfaces в postorder: сначала RootDefault, затем LeafDefault. Оба объявляют default-метод. QuietParent и QuietChild не объявляют default methods, поэтому не инициализируются лишь из-за того, что Child implements QuietChild.

Отдельный доступ к QuietChild.MARK затем инициализирует непосредственно QuietChild; маркера QuietParent нет. Инициализация интерфейса сама по себе не означает инициализацию его superinterfaces. В примере нет compiler-generated synthetic default methods, поэтому специальная оговорка JLS §12.4.1 не меняет наблюдаемый порядок.

### 4. Все поля объекта получают default values до кода конструкторов

При new Child() память объекта получает начальные значения всех instance fields, в том числе объявленных в Child: childValue == 0, childLabel == null. Затем исполняется цепочка от суперкласса к подклассу:

1. Инициализатор поля Parent.parentValue, затем instance initializer block Parent.
2. Тело конструктора Parent. Он вызывает переопределяемый observe(), поэтому фактический объект Child приводит к Child.observe(). Инициализаторы Child ещё не исполнялись, и override печатает 0/null.
3. После возврата из конструктора Parent выполняются инициализаторы полей Child в порядке объявления, затем instance initializer block Child.
4. Выполняется остаток тела `Child(int)`: этот конструктор вызван из `Child()` через `this(67)` и сам вызывает `super()` для `Parent`.
5. Возвращаемся в Child() после this(67) и выполняем остаток его тела.

Child.instance field, Child.instance reference field и Child.instance block встречаются ровно по одному разу: делегирование this(67) не запускает instance initializers повторно. В финале объект имеет parentValue=67, childValue=68, childLabel="ready".

## B3 — неявный super() с нулём фактических аргументов

Отдельный [ImplicitSuperDemo.java](src/ImplicitSuperDemo.java) показывает два контраста в static nested ordinary classes. У Parent есть только package-private конструктор Parent(int... xs), который сохраняет длину массива. Explicit имеет явно объявленный пустой конструктор без явного super(); Implicit не объявляет конструкторов. Объемлющий класс создаёт оба объекта, отдельно проверяет count == 0 и печатает длины в порядке Explicit, Implicit. Это не побайтный повтор верхнеуровневого листинга статьи, а проверка того же механизма с другим layout; доступ package-private не зависит от разрешённого private-доступа внутри одного top-level типа.

Изолированный запуск из корня репозитория:

~~~sh
mkdir -p examples/core/initialization/build/implicit-super-classes
javac --release 21 -XDrawDiagnostics -d examples/core/initialization/build/implicit-super-classes examples/core/initialization/src/ImplicitSuperDemo.java
java -cp examples/core/initialization/build/implicit-super-classes ImplicitSuperDemo
~~~

Ожидаемый stdout самостоятельного примера:

~~~text
0
0
~~~

Исходный InitializationDemo и 29 строк expected.stdout не изменены (29 строк — не число тестов). Runner компилирует новый пример в отдельный каталог, сравнивает его две строки с отдельным временным эталоном, выводит их после исходного stdout и до прежнего финального маркера. Две отдельные проверки с AssertionError — не JUnit.

**Нормативная граница.** Ноль фактических аргументов не равен пустому списку формальных параметров: [JLS 21 §8.8.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7), [§8.8.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7.1), [§15.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2). Для default constructor [§8.8.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.9) формулирует условие о доступном конструкторе суперкласса, принимающем ноль аргументов и не имеющем секции throws. Наблюдаемый javac 21.0.12.1 принимает обе varargs-цепочки; отдельная временная проба также принимает default subclass при Parent() throws IllegalStateException, тогда как checked IOException отвергается. Буквальное условие no throws clause строже последнего наблюдения с unchecked-only throws; [§11.2.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html#jls-11.2.3) отдельно регулирует checked exceptions. Это наблюдение конкретного компилятора, не объявление спецификации ошибочной и не гарантия переносимости. Подробности и самостоятельные временные фрагменты — в связанной статье CORE-INIT, не в tracked negative harness. Private constructor negative проверялся в двух разных верхнеуровневых классах.

## Почему основной пример ограничен одним сценарием

Этот runnable демонстрирует связное ядро L1/I1/I2 из плана CORE-INIT: active use и declaring type, constant vs non-constant static member, superclass/interface/class ordering, default values, superclass-first instance initialization, this(...) и virtual dispatch во время construction. Отдельные сценарии для recursive class initialization, initialization failure, Class.forName/ClassLoader, multi-thread deadlock и отрицательных компиляций намеренно не добавлены в этот файл: они имеют отдельные механизмы и требуют независимых изолированных запусков. Их место — самостоятельные фрагменты статьи, а не искусственное усложнение центрального примера.

**Историческая проверка основного примера:** OpenJDK 21.0.12.1 / javac 21.0.12.1; bash examples/core/initialization/verify.sh — exit code 0, полный stdout совпал с expected.stdout, скрипт вывел CHECK PASSED: stdout matches expected.stdout. Другие JDK-поставщики/версии и визуальная проверка Obsidian/IntelliJ — not run.

**B3 — фактическая локальная проверка роли 03:** Debian OpenJDK/javac 21.0.12.1, LC_ALL=C. Baseline/final bash -n и полный bash runner exit 0, stderr пуст. Исходный actual.stdout совпал побайтно с baseline и expected.stdout; полный runner 30→32 строки, прежний вывод после удаления двух новых строк идентичен. Изолированные compile/run ImplicitSuperDemo exit 0/0, stdout 0/0, compiler stdout/stderr и java stderr пусты. Семь временных отрицательных компиляций exit 1 с ожидаемыми причинами (неприменимость, неоднозначность, private access, checked explicit/default, first statement, cyclic this); две временные положительные compile/run exit 0/0, stdout checked=67 и unchecked=67. Все временные пробы отдельно, не tracked и не часть CI runner. Git blob hashes сверены для 3/3 исходных и 4/4 итоговых исполняемых/input файлов, runner mode100644 сохранён. Gradle/JUnit/clean clone/GUI локально не запускались; CI проверяется отдельно на точном SHA.
