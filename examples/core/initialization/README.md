# CORE-INIT — порядок инициализации в Java 21

Один автономный пример показывает **два связанных этапа** создания объекта: инициализацию классов/интерфейсов и инициализацию полей/конструкторов конкретного экземпляра. Исходник намеренно оставлен одним файлом: вложенные типы позволяют увидеть причинно-следственный порядок в одном запуске, не создавая лишний source set или Gradle-модуль.

**Исходник:** [src/InitializationDemo.java](src/InitializationDemo.java)  
**Полный ожидаемый stdout:** [expected.stdout](expected.stdout)  
**Проверка точного вывода:** [verify.sh](verify.sh)

Теоретические предпосылки: [CORE-INIT — порядок инициализации](../../../knowledge/01-java-core/initialization.md), [CORE-CLASSES — поля и конструкторы](../../../knowledge/01-java-core/classes.md), [CORE-INHERITANCE — переопределение и динамический вызов](../../../knowledge/01-java-core/inheritance.md). Нормативная основа — JLS 21 §§4.12.4–4.12.5, 8.3.2, 8.6–8.8, 9.4, 12.4.1–12.4.2 и 12.5.

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

Скрипт удаляет и пересоздаёт только локальный examples/core/initialization/build/, компилирует исходник с --release 21, запускает InitializationDemo, сравнивает stdout через diff -u и завершает работу с ненулевым кодом при любом расхождении. После вывода программы скрипт печатает CHECK PASSED: stdout matches expected.stdout. Каталог build/ уже исключён существующим .gitignore.

PowerShell — те же команды из корня:

~~~powershell
javac --release 21 -d examples/core/initialization/build/classes examples/core/initialization/src/InitializationDemo.java
java -cp examples/core/initialization/build/classes InitializationDemo
~~~

## Полный ожидаемый stdout

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
4. Выполняется остаток тела Child(int), вызванного через super().
5. Возвращаемся в Child() после this(67) и выполняем остаток его тела.

Child.instance field, Child.instance reference field и Child.instance block встречаются ровно по одному разу: делегирование this(67) не запускает instance initializers повторно. В финале объект имеет parentValue=67, childValue=68, childLabel="ready".

## Почему пример ограничен одним сценарием

Этот runnable демонстрирует связное ядро L1/I1/I2 из плана CORE-INIT: active use и declaring type, constant vs non-constant static member, superclass/interface/class ordering, default values, superclass-first instance initialization, this(...) и virtual dispatch во время construction. Отдельные сценарии для recursive class initialization, initialization failure, Class.forName/ClassLoader, multi-thread deadlock и отрицательных компиляций намеренно не добавлены в этот файл: они имеют отдельные механизмы и требуют независимых изолированных запусков. Их место — самостоятельные фрагменты статьи, а не искусственное усложнение центрального примера.

**Фактическая проверка автора:** OpenJDK 21.0.12.1 / javac 21.0.12.1; bash examples/core/initialization/verify.sh — exit code 0, полный stdout совпал с expected.stdout, скрипт вывел CHECK PASSED: stdout matches expected.stdout. Другие JDK-поставщики/версии и визуальная проверка Obsidian/IntelliJ — not run.
