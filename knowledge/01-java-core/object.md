# CORE-OBJECT — `java.lang.Object`, идентичность и базовые методы в Java 21

**Версия:** Java SE 21. **Задача:** [Issue #62](https://github.com/Melchiorius/backend-interview-labs/issues/62). **Исследовательская основа:** [принятый G1-план CORE-OBJECT-EQUALITY](../../docs/plans/CORE-OBJECT-EQUALITY.md), PR #61, исходный проверенный HEAD `eede792ea16005e9087f4bc8eca5617c73c79761`; PR #61 позднее объединён в `main` squash `f868f26a782e7695569ed166d7b9d50a6e7bd0b3`. Эта статья вместе с [CORE-EQUALITY](equality.md) опубликована в [PR #67](https://github.com/Melchiorius/backend-interview-labs/pull/67) (G5 squash `4680b057aeabcd3e4d36e15b13b4db0d032c944c`). Сведения о реальных CI/G3/G4 и ограничениях см. в [COVERAGE](../../docs/COVERAGE.md).

## Зачем нужен общий базовый класс

Любой объект Java, включая массив, можно передать туда, где ожидается `Object`. Это не значит, что все объекты одинаковы: их реальные классы, поля и методы остаются разными. `Object` задаёт минимальный общий контракт **объектов классов**: идентичность, описание, хеширование, доступ к runtime-классу и исторические или низкоуровневые механизмы.

У обычного класса, объявленного через `class` без явного `extends`, непосредственный суперкласс — `Object`. У `record` непосредственный суперкласс — `java.lang.Record`, у `enum` — `java.lang.Enum<E>`; для анонимных классов действуют [особые правила JLS 21 §15.9.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9.5). Сам `Object` находится на вершине иерархии классов и **не имеет суперкласса**. Массивы тоже являются объектами, хотя их типы создаются JVM по специальным правилам. Интерфейс не «расширяет Object» через `extends Object`: интерфейсы имеют собственные правила объявления и наследования; отдельные публичные методы `Object` отражаются в правилах интерфейсных типов. См. [JLS 21 §8.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.4) и [§9.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.2).

```java
class Ticket {             // неявно extends Object
    int number = 67;
}

public class ObjectIntro {
    public static void main(String[] args) {
        Ticket first = new Ticket();
        Ticket alias = first;
        Ticket second = new Ticket();

        Object any = first; // upcast: объект не изменился

        System.out.println(first == alias);  // true
        System.out.println(first == second); // false
        System.out.println(any.getClass() == Ticket.class); // true
        System.out.println(first.equals(second)); // false: не переопределён equals
    }
}
```

**Что здесь происходит.** `first` и `alias` содержат ссылку на один объект; `second` — на другой объект с таким же значением поля. Исходный `Object.equals(Object)` сравнивает **идентичность** и для этих двух экземпляров возвращает `false`. Присваивание `first` переменной типа `Object` не меняет фактический класс экземпляра. Механизмы классов, upcast и наследования раскрыты в [CORE-CLASSES](classes.md), [CORE-TYPES](types.md) и [CORE-INHERITANCE](inheritance.md).

Здесь важно различать **identity** — «это один и тот же объект?» — и **value equality** — «считаем ли мы эти два объекта эквивалентными по выбранным данным?». Первый вопрос решает ссылочное `==`, когда типы операндов допускают такое сравнение (две `null`-ссылки тоже равны); второй решает реализация `equals` конкретного типа при ненулевом получателе вызова. Подробный контракт, ошибки переопределения и последствия для коллекций разобраны в самостоятельной [CORE-EQUALITY](equality.md).

**Практическое продолжение:** [разбор equals/hashCode](equality.md) и его [автономный пример Java 21](../../examples/core/equality/README.md) показывают отличие identity от value equality и последствия для коллекций. Это намеренно не дублируется в обзоре методов `Object`.

## Основные методы Object: что они обещают

| Метод | Назначение и важная граница | Можно переопределить? |
|---|---|---|
| `equals(Object)` | По умолчанию identity equality; конкретный класс может задать значение равенства | Да |
| `hashCode()` | Целочисленный hash по контракту; не уникальный ID объекта | Да |
| `toString()` | Текстовое описание для диагностики | Да |
| `getClass()` | Фактический runtime-класс объекта | **Нет, `final`** |
| `clone()` | Особый механизм создания копии; `protected`, не универсальный конструктор копирования | Да, при соблюдении контракта |
| `finalize()` | Устаревший callback финализации; **deprecated for removal** в Java 21 | Исторически можно, **использовать не следует** |
| `wait(...)`, `notify()`, `notifyAll()` | Взаимодействие через монитор объекта; не обычная система событий | **Нет, `final`** |

Имена и сигнатуры проверяются по [Java SE 21: java.lang.Object](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html). `Object` не предоставляет `compareTo` — это отдельный интерфейс `Comparable`, а не универсальное сравнение всех объектов.

### equals и hashCode: обзор, а не второй учебник по равенству

Если `x` — ненулевая ссылка и выполняется именно исходная реализация `Object.equals(Object)`, её результат совпадает с результатом сравнения ссылок `x == y`, **при условии, что такое сравнение допустимо по статическим типам операндов**. В общем случае вызов `equals` может выбрать переопределённую реализацию, а при `x == null` вызов метода экземпляра выбросит `NullPointerException`. Для ненулевого `x` корректный по контракту `equals` возвращает `false` на `x.equals(null)`. Если тип решает сравнивать по полям, он **сам** переопределяет `equals(Object)` и должен согласовать с ним `hashCode()`. Равные в смысле `equals` объекты обязаны иметь одинаковый hash; одинаковые hash **не делают** объекты равными. Нет требования, что hash — адрес памяти, постоянный номер или уникальное значение.

Например, используя класс `Ticket` из первого примера:

```java
Ticket a = new Ticket(), alias = a, b = new Ticket();
Ticket missing = null, absent = null;
System.out.println(a == alias && a.equals(alias)); // true
System.out.println(a == b);                        // false
System.out.println(a.equals(b));                   // false
System.out.println(missing == absent);             // true
System.out.println(a.equals(null));                // false
// missing.equals(absent); // при выполнении NPE, не ошибка компиляции

String left = new String("67"), right = new String("67");
System.out.println(left == right);                 // false
System.out.println(left.equals(right));            // true
Integer number = 67;
// System.out.println(left == number); // недопустимое сравнение ссылочных типов
System.out.println(left.equals(number));           // false: вызов допустим по типам
```

Сравнение двух ссылочных операндов `==` допускается, когда возможно преобразование приведением одного типа к другому (либо операнды имеют совместимые типы с учётом правил для `null`). Например, `String` и `Integer` в этом фрагменте несравнимы через `==`, даже если обе ссылки равны `null`. Метод `equals(Object)` принимает аргумент типа `Object`, поэтому допустимость его вызова и допустимость оператора `==` — разные вопросы. См. [JLS §15.21.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21.3), [§15.12.4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4.4), [CORE-TYPES](types.md).

Здесь достаточно этой границы. Как реализовать value object, не нарушить симметрию/транзитивность, не потерять поиск ключа после изменения состояния и согласовать сортировку, подробно изложено в [CORE-EQUALITY](equality.md). Первоисточник — [Object.equals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#equals(java.lang.Object)) и [Object.hashCode](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#hashCode()).

## getClass(): фактический класс, а не тип переменной

`getClass()` — метод `public final`, который возвращает объект `Class`, представляющий фактический runtime-класс получателя. Хотя декларация метода — `Class<?> getClass()`, тип выражения вызова по специальному правилу — `Class<? extends E>`, где `E` — стирание статического типа выражения-получателя. Это ограничение для компилятора, а не изменение фактического класса объекта.

```java
Object reference = new String("java");
System.out.println(reference.getClass() == String.class); // true
System.out.println(reference instanceof CharSequence);    // true
```

```java
Number n = 67;
Class<? extends Number> c = n.getClass(); // приведение не требуется
System.out.println(c == Integer.class);   // true
// Class<Integer> exact = n.getClass();    // ошибка компиляции
```

Объявленный тип выражения `n` — `Number`, поэтому компилятор не обещает `Class<Integer>`. В данном выполнении объект действительно имеет класс `Integer`. Для вызова на параметризованном типе используется стирание, а не сведения обо всех фактических аргументах типа. См. [CORE-ERASURE](erasure.md).

Результат `getClass()` — объект `Class`, представляющий определение типа. Для именованных классов одного binary name недостаточно для идентичности типа: определяющий загрузчик тоже играет роль ([CORE-CLASSLOADER](classloaders.md)). Поэтому `getClass()` полезен для рефлексии ([CORE-REFLECTION](reflection.md)) и строгой проверки фактического типа, но **не** является скрытой проверкой значения. Объект `Class`, представляющий класс, служит монитором его объявленного `static synchronized` метода: например, такой метод `Ticket` синхронизируется на `Ticket.class`, а не на каждом экземпляре `Ticket` и не на классе произвольного наследника. Это тот же монитор, что у `synchronized (Ticket.class)`.

В реализации `equals` выбор `getClass()` против `instanceof` — вопрос модели: запрещаем ли сравнение с любыми подклассами или допускаем полиморфную эквивалентность? Не существует универсального ответа без семантики конкретного типа. Разбор реальных нарушений симметрии см. в [CORE-EQUALITY](equality.md). Источники: [Object.getClass](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#getClass()), [JLS 21 §15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2).

## toString(): представление для чтения, не идентификатор

В базовой реализации `Object.toString()` формирует строку из имени runtime-класса, символа `@` и шестнадцатеричного представления результата `hashCode()`. Но почти любой тип может переопределить этот метод; `String.toString()`, например, возвращает само строковое значение. Поэтому формат класса с `@` — описание **базового метода**, а не обязательный формат каждого `toString`. Внутри базового `toString()` вызывается виртуальный `hashCode()`: если класс переопределяет только его и возвращает `67`, унаследованный `toString()` заканчивается на `@43` (шестнадцатеричное представление 67), а не обязательно использует identity hash. Класс `SameHash` ниже иллюстрирует такой случай.

```java
class Order {
    private final int id;

    Order(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Order{id=" + id + "}";
    }
}

System.out.println(new Order(67)); // Order{id=67}
```

Последняя строка — **фрагмент внутри метода**, а не отдельный компилируемый файл: объявления класса и вызов нужно поместить в подходящий Java-контекст. `println(Object)` использует строковое представление объекта; не путайте его с гарантированным именем, адресом или бизнес-идентификатором.

Если `toString()` включает пароль, токен, персональные данные или иной секрет, обычное логирование может их раскрыть. Разумно показывать только безопасные диагностические поля. Не стоит строить по `toString()` логику сравнения, протокол хранения или ключи распределённого кеша. Источник: [Object.toString](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#toString()).

## System.identityHashCode: hash без переопределённого метода

`System.identityHashCode(x)` возвращает hash, как если бы для `x` применили исходную `Object.hashCode()`, **даже если** класс переопределил `hashCode`. Для `null` этот метод возвращает `0`.

```java
class SameHash {
    @Override
    public int hashCode() {
        return 67;
    }
}

SameHash item = new SameHash();
System.out.println(item.hashCode());                  // 67
System.out.println(System.identityHashCode(null));   // 0
// Не угадываем конкретное число identityHashCode(item).
```

Identity hash не обязан быть уникальным между объектами, одинаковым между запусками программы или равным адресу объекта в памяти. Он не является идентификатором сущности в БД, криптографическим хешем или безопасным ключом постоянного хранения. Даже сравнение двух identity hash **не заменяет** `a == b`. См. [System.identityHashCode](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#identityHashCode(java.lang.Object)) и [Object.hashCode](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#hashCode()).

## clone() и Cloneable: историческая модель копирования

`Object.clone()` объявлен `protected`. Поэтому внешний код не может свободно вызвать `someObject.clone()` для любого экземпляра. Обычно для доступа к копированию наследник предоставляет собственный публичный метод; это должно быть осознанное API-решение.

Исходный механизм `Object.clone()` проверяет, реализует ли фактический класс маркерный интерфейс `Cloneable`; если нет, выбрасывает проверяемое исключение `CloneNotSupportedException` (его нужно обработать или объявить при вызове `super.clone()` с этой сигнатурой). `Cloneable` **не** объявляет метод `clone()` и сам по себе **не** делает его публичным. Для обычного экземпляра такой `clone` выполняет поверхностное копирование полей: значения примитивов копируются, значения ссылочных полей по-прежнему указывают на те же вложенные объекты.

```java
class Box implements Cloneable {
    int[] numbers = {67};

    @Override
    public Box clone() {
        try {
            return (Box) super.clone();
        } catch (CloneNotSupportedException impossibleHere) {
            throw new AssertionError(impossibleHere);
        }
    }
}

Box original = new Box();
Box copy = original.clone();
System.out.println(original == copy);         // false
System.out.println(original.numbers == copy.numbers); // true
```

В этом **фрагменте** оба `Box` разделяют один массив; изменение `copy.numbers[0]` затронет и `original.numbers[0]`. Это не независимое глубокое клонирование. Контракт `clone` имеет оговорки, особенно если класс переопределяет его нестандартно; не переносите автоматически поведение `Object.clone` на все методы с именем `clone`.

### Копирование экземпляра без повторного конструктора

При успешном выполнении исходного `Object.clone()` для обычного экземпляра создаётся отдельный объект того же класса с копиями значений полей; обычная цепочка конструкторов, инициализаторы экземплярных полей и блоки инициализации для этой копии не выполняются повторно:

```java
class Counted implements Cloneable {
    static int constructions, initializations;
    { initializations++; }
    Counted() { constructions++; }
    @Override public Counted clone() throws CloneNotSupportedException {
        return (Counted) super.clone();
    }
}
// В методе с throws CloneNotSupportedException:
Counted a = new Counted();
Counted b = a.clone();
System.out.println(a != b); // true
System.out.println(Counted.constructions + ":" + Counted.initializations); // 1:1
```

Вывод предполагает первый такой запуск с исходными нулевыми счётчиками. Он относится к вызову `super.clone()`, не к произвольному переопределению, которое может применять иную стратегию. Связь с обычным созданием объекта — [CORE-CLASSES](classes.md), [CORE-INIT](initialization.md).

### Массивы: публичный clone с типизированным результатом

У массива типа `T[]` метод `clone()` публичный, возвращает `T[]` и не объявляет проверяемых исключений ([JLS §10.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.7)). Массивы реализуют `Cloneable`; для такого вызова не требуется ни приведение результата, ни `catch CloneNotSupportedException`.

```java
int[] original = {67};
int[] copy = original.clone();
copy[0] = 68;
System.out.println(original[0]); // 67

int[][] matrix = {{67}};
int[][] outer = matrix.clone();
System.out.println(matrix == outer);       // false
System.out.println(matrix[0] == outer[0]); // true
outer[0][0] = 68;
System.out.println(matrix[0][0]);          // 68
```

Клонируется внешний массив, его элементы-ссылки на внутренние массивы копируются как значения. Для независимого состояния нужно явно выбрать политику копирования вложенных объектов. См. [CORE-ARRAYS](arrays.md) и [CORE-IMMUTABLE](immutability.md).

Предпочтение конструктору копирования, статической фабрике или явному преобразованию в новый value object — **совет по проектированию**, а не требование JLS. Источники: [Object.clone](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#clone()), [Cloneable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Cloneable.html), [CloneNotSupportedException](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/CloneNotSupportedException.html).

## finalize(): не средство освобождения ресурсов

`finalize()` в Java 21 **deprecated for removal**. Автоматическая финализация может быть отключена в JVM Java 21; даже если включена, вызов может откладываться неопределённо долго. JVM автоматически вызывает `finalize()` не более одного раза на объект; явный вызов доступного метода `finalize()` из Java-кода — отдельный обычный вызов, а не автоматическая финализация, и это ограничение на него не распространяется. Нет гарантии, что программа дождётся финализации до завершения процесса, и нельзя полагаться на порядок таких вызовов. Она также может задержать освобождение объектов и породить риски безопасности, если объект становится доступным повторно.

Не путайте `final` (ограничение присваивания, переопределения или наследования), `finally` (блок обработки завершения `try`) и `finalize()` (устаревший метод финализации). Для ресурса, который надо закрыть вовремя (файл, сокет, соединение), используйте явное управление временем жизни, обычно `AutoCloseable` и `try-with-resources`:

```java
try (var stream = java.nio.file.Files.newInputStream(path)) {
    // Работа с открытым потоком.
}
// close() вызывается при выходе из try в установленном порядке.
```

Здесь `path` — заранее существующая переменная типа `Path`: это поясняющий фрагмент, а не готовая программа. Сборщик мусора управляет памятью, но не даёт детерминированного срока закрытия внешнего ресурса; `Cleaner` также не обещает своевременный автоматический вызов очистки. Порядок закрытия и обработка исключений разобраны в [CORE-RESOURCES](resources.md). Источники: [Object.finalize](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#finalize()), [AutoCloseable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AutoCloseable.html), [JLS 21 §14.20.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3).

## wait/notify/notifyAll: объект как монитор, а не просто «сигнал»

У каждого объекта есть монитор и множество ожидающих потоков (wait set). Методы `wait`, `notify` и `notifyAll` объявлены `final`; для корректного вызова текущий поток должен владеть монитором именно объекта-получателя, например внутри `synchronized (lock)` для `lock.wait()`. Без владения этим монитором возникает `IllegalMonitorStateException` (при корректных аргументах). Владение монитором другого объекта не помогает.

Когда поток действительно переходит в ожидание через `wait`, он освобождает **все свои повторные захваты монитора получателя**. Другие мониторы, которыми он владеет, остаются захваченными. Перед нормальным возвратом из ожидания или выбросом `InterruptedException` после ожидания он снова получает монитор с прежней глубиной захвата. Если поток уже прерван до корректного вызова `wait`, при владении монитором и корректных аргументах выбрасывается `InterruptedException` без перехода в ожидание; статус прерывания очищается при выбрасывании этого исключения. `Thread.sleep(...)`, напротив, не освобождает удерживаемые мониторы.

| Вызов | Что происходит |
|---|---|
| `wait()` | Ожидание без ограничения времени |
| `wait(long millis)` | Ожидание с заданным тайм-аутом; 0 означает отсутствие временного ограничения |
| `wait(long millis, int nanos)` | Дополнительные наносекунды; оба аргумента 0 означают отсутствие временного ограничения |
| `notify()` | Выбирает произвольный один поток из wait set этого объекта, если множество не пусто |
| `notifyAll()` | Уведомляет все потоки из wait set этого объекта |

Отрицательные `millis` и `nanos` вне диапазона 0–999999 недопустимы (`IllegalArgumentException`). Тайм-аут не гарантирует возврата ровно в указанный момент: поток ещё должен вновь захватить монитор. Уведомление не передаёт монитор автоматически, не освобождает его у уведомляющего потока и не накапливается в качестве разрешения на будущее ожидание. После освобождения монитора уведомлённые потоки конкурируют за его получение, без гарантии очерёдности или справедливости.

Условие `ready` нужно проверять под тем же монитором, под которым другой поток изменяет его и вызывает подходящее уведомление. Пробуждение может произойти по уведомлению, тайм-ауту (при ограниченном ожидании), прерыванию или без явной причины (*spurious wakeup*); уведомление само по себе не означает истинности условия. Поэтому условие проверяют в цикле, а не в `if`:

```java
synchronized (lock) {
    while (!ready) {
        lock.wait();
    }
    // Условие ready проверено при владении монитором lock.
}
```

Это **фрагмент** внутри метода с доступными переменными `lock` и `ready`; исключение `InterruptedException` необходимо передать вызывающему коду или корректно обработать в соответствии с политикой отмены. Если `ready` уже истинно, `wait()` не вызывается. При ограниченном ожидании условие и оставшееся время нужно проверять повторно, не начиная полный тайм-аут заново. Полную многопоточную программу здесь намеренно не строим: блокировки, interruption и протокол уведомления — будущая тема `CON-MONITOR`, happens-before и публикация — `CON-JMM`. См. [Object.wait](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#wait()), [notify](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#notify()), [notifyAll](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#notifyAll()) и [JLS 21 §17.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.2), [§17.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.3), [Object.wait(long,int)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#wait(long,int)) и [Thread.sleep(long)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Thread.html#sleep(long)).

## Границы и проверка понимания

- **`Object` не гарантирует value equality.** Это исходная identity-семантика метода `equals`; конкретные классы могут задать другое отношение. [CORE-EQUALITY](equality.md) объясняет нормативный контракт и последствия в коллекциях.
- **Рефлексия ≠ равенство:** `getClass()` определяет фактический тип, а не совпадение полей.
- **Identity hash ≠ уникальность:** коллизии допустимы и здесь; никакого адреса памяти или устойчивого идентификатора API не обещает.
- **`clone` ≠ глубокое копирование:** в исходном механизме ссылочные поля остаются общими.
- **`finalize` ≠ `close`:** не управляйте сроком освобождения ресурса через сборщик мусора.
- **Мониторные методы ≠ готовый протокол конкуренции:** без синхронизации условий можно получить ошибки и потерянные уведомления.
- **Специальные карты:** `IdentityHashMap` использует ссылочную идентичность ключей и нарушает общий смысл сравнения ключей через `equals`; это P2-граница, не обычное поведение `HashMap`. См. [Java 21 IdentityHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/IdentityHashMap.html).
- **Value-based classes и record:** их соглашения не делают весь Java-мир единым «значенческим» механизмом. Для массивов, record и сортировки — [CORE-EQUALITY](equality.md); отдельная статья о record — [CORE-RECORDS](records.md).
- **Область ответственности:** виртуальный вызов и конструкторы — [CORE-INHERITANCE](inheritance.md) и [CORE-INIT](initialization.md). Полный контракт потоковой публикации — будущая `CON-JMM`, а внутренние структуры хеш-коллекций — [COL-HASH](../02-collections/hash-collections.md).

**Самопроверка.** Попробуйте сначала предсказать результат, затем объяснить механизм:

| Вопрос | Ключ к ответу |
|---|---|
| Две переменные `Ticket` равны `null`: чем отличаются `a == b` и `a.equals(b)`? | `true` и NPE; для метода нужен ненулевой получатель |
| Почему `Number n = 67; n.getClass()` не имеет статического типа `Class<Integer>`? | Стирание статического типа получателя, фактический класс отдельно |
| Почему унаследованный `toString()` может закончиться на `@43`? | Виртуальный `hashCode()` возвращает 67 |
| Копирует ли `clone()` у `int[][]` вложенные массивы? | Нет, внешний массив новый, ссылки на строки общие |
| Нужно ли обрабатывать checked exception при вызове `clone()` на массиве? | Нет; у `T[]` публичный `T[] clone()` без checked exceptions |
| Может ли автоматическая финализация быть отключена? | Да; даже включённая не гарантирует своевременный вызов |
| Какой монитор освобождает `lock.wait()` при реальном ожидании внутри двух вложенных synchronized-блоков? | Только монитор `lock`, все его захваты; другие мониторы остаются удержанными |
| Чем `notifyAll()` отличается от `notify()`? | Все ожидающие на этом объекте против произвольного одного; ни один вызов не передаёт монитор |

## Карта покрытия исследовательского плана

| ID | Приоритет | Где объясняется |
|---|---|---|
| OE-01 | P0 | «Зачем нужен общий базовый класс», identity и мост к equality |
| OE-13 | P1 | «Основные методы Object» |
| OE-15 | P1 | «toString» |
| OE-16 | P1 | «System.identityHashCode» |
| OE-19 | P1 | «wait/notify/notifyAll» |
| OE-20 | P1 | «clone» и «finalize» |
| OE-23 | P2 | Граница `IdentityHashMap` |
| OE-26 | P2 | Граница hash/toString и устойчивых идентификаторов |

Остальные P0 (OE-02–OE-12), P1 (OE-14, OE-17, OE-18) и P2 раскрываются в [CORE-EQUALITY](equality.md) и не дублируются в этом обзоре.

## Источники и роль доказательств

**Нормативные для Java 21:** [JLS §8.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.4), [§9.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.2), [§15.21](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21), [§17.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.2), [Java SE 21 Object](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html), [System.identityHashCode](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#identityHashCode(java.lang.Object)) и перечисленные выше API. Для интерфейсной семантики `Comparable`, Map и HashSet требуется API, не вывод из JLS.

**Исследовательские и интервью-ориентиры:** принятый [G1-план](../../docs/plans/CORE-OBJECT-EQUALITY.md) и [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md), включая Q-HRUDAY, Q-CHERNYSHOV, Q-TEAMLEAD, Q-IN28 и Q-BAELDUNG-COL. Интервью-подборки помогают обнаруживать пробелы, **но не доказывают нормативное поведение, статистическую частоту вопросов или полноту**.

**Проверяемость:** ожидаемые результаты в статье — объяснения по спецификации и API, а не отчёт о запуске всех фрагментов. В D-A08a отдельные временные пробы Java 21 проверили идентичность и `null`, тип `getClass`, копирование, ошибки владения монитором и аргументов, заранее прерванный поток и случай уже истинного условия; фактические исходники и результаты приведены в авторском отчёте. Они не добавлены в репозиторий и не выполняются обычным CI. Проба предварительного прерывания не воспроизводит реальное освобождение монитора и конкуренцию за его повторный захват; эти правила следуют из спецификации. Автоматическая финализация, сценарии с загрузчиками и планировщиком, визуальный Obsidian/IntelliJ здесь не проверялись. Защищённый пример equality проверяет свои собственные сценарии, а не все случаи `Object`. Фрагменты с объявлениями типов и выполняемыми строками размещайте в соответствующем классе и методе; повторяющиеся имена из разных примеров не объединяйте.
