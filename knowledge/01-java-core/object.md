# CORE-OBJECT — `java.lang.Object`, идентичность и базовые методы в Java 21

**Версия:** Java SE 21. **Задача:** [Issue #62](https://github.com/Melchiorius/backend-interview-labs/issues/62). **Исследовательская основа:** принятый план [CORE-OBJECT-EQUALITY](https://github.com/Melchiorius/backend-interview-labs/blob/eede792ea16005e9087f4bc8eca5617c73c79761/docs/plans/CORE-OBJECT-EQUALITY.md) из PR #61 @ `eede792ea16005e9087f4bc8eca5617c73c79761`. План пока находится в открытом PR #61 и не предполагается существующим в текущем `main`; поэтому дана ссылка на точный принятый commit.

## Зачем нужен общий базовый класс

Любой объект Java, включая массив, можно передать туда, где ожидается `Object`. Это не значит, что все объекты одинаковы: их реальные классы, поля и методы остаются разными. `Object` задаёт минимальный общий контракт **объектов классов**: идентичность, описание, хеширование, доступ к runtime-классу и исторические или низкоуровневые механизмы.

У класса без явного `extends` непосредственный суперкласс — `Object`; он находится на вершине иерархии классов и **сам не имеет суперкласса**. Массивы тоже являются объектами, хотя их типы создаются JVM по специальным правилам. Интерфейс не «расширяет Object» через `extends Object`: интерфейсы имеют собственные правила объявления и наследования; отдельные публичные методы `Object` отражаются в правилах интерфейсных типов. См. [JLS 21 §8.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.4) и [§9.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.2).

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

Здесь важно различать **identity** — «это один и тот же объект?» — и **value equality** — «считаем ли мы эти два объекта эквивалентными по выбранным данным?». Первый вопрос решает ссылочное `==`; второй решает реализация `equals` конкретного типа. Подробный контракт, ошибки переопределения и последствия для коллекций разобраны в самостоятельной [CORE-EQUALITY](equality.md).

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

В исходном `Object` `x.equals(y)` эквивалентен `x == y`. Если тип решает сравнивать по полям, он **сам** переопределяет `equals(Object)` и должен согласовать с ним `hashCode()`. Равные в смысле `equals` объекты обязаны иметь одинаковый hash; одинаковые hash **не делают** объекты равными. Нет требования, что hash — адрес памяти, постоянный номер или уникальное значение.

Здесь достаточно этой границы. Как реализовать value object, не нарушить симметрию/транзитивность, не потерять поиск ключа после изменения состояния и согласовать сортировку, подробно изложено в [CORE-EQUALITY](equality.md). Первоисточник — [Object.equals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#equals(java.lang.Object)) и [Object.hashCode](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#hashCode()).

## getClass(): фактический класс, а не тип переменной

`getClass()` — метод `public final`, который возвращает объект `Class`, представляющий фактический runtime-класс получателя. У типа результата есть дополнительные generic-уточнения в Java API; для этого примера важно, что статический тип переменной не меняет класс самого объекта.

```java
Object reference = new String("java");
System.out.println(reference.getClass() == String.class); // true
System.out.println(reference instanceof CharSequence);    // true
```

Результат `getClass()` — объект `Class`, представляющий определение типа. Имя класса само по себе недостаточно для runtime-идентичности: defining class loader тоже играет роль. Поэтому `getClass()` полезен для отражения (reflection) и строгой проверки фактического типа, но **не** является скрытой проверкой значения.

В реализации `equals` выбор `getClass()` против `instanceof` — вопрос модели: запрещаем ли сравнение с любыми подклассами или допускаем полиморфную эквивалентность? Не существует универсального ответа без семантики конкретного типа. Разбор реальных нарушений симметрии см. в [CORE-EQUALITY](equality.md). Источники: [Object.getClass](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#getClass()), [JLS 21 §15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2).

## toString(): представление для чтения, не идентификатор

В базовой реализации `Object.toString()` формирует строку из имени runtime-класса, символа `@` и шестнадцатеричного представления результата `hashCode()`. Но почти любой тип может переопределить этот метод; `String.toString()`, например, возвращает само строковое значение. Поэтому формат класса с `@` — описание **базового метода**, а не обязательный формат каждого `toString`.

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

Изначальный механизм `Object.clone()` проверяет, реализует ли runtime-класс маркерный интерфейс `Cloneable`; если нет, выбрасывает `CloneNotSupportedException`. `Cloneable` **не** объявляет метод `clone()` и сам по себе **не** делает его публичным. Для обычного экземпляра такой `clone` выполняет поверхностное копирование полей: значения примитивов копируются, значения ссылочных полей по-прежнему указывают на те же вложенные объекты.

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

Предпочтение конструктору копирования, статической фабрике или явному преобразованию в новый value object — **совет по проектированию**, а не требование JLS. Источники: [Object.clone](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#clone()), [Cloneable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Cloneable.html), [CloneNotSupportedException](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/CloneNotSupportedException.html).

## finalize(): не средство освобождения ресурсов

`finalize()` в Java 21 **deprecated for removal**. Финализация асинхронна и недетерминирована: нет гарантии, что программа дождётся её до завершения процесса, и нельзя полагаться на порядок таких вызовов. Она также может задержать освобождение объектов и породить риски безопасности, если объект становится доступным повторно.

Для ресурса, который надо закрыть вовремя (файл, сокет, соединение), используйте явное управление временем жизни, обычно `AutoCloseable` и `try-with-resources`:

```java
try (var stream = java.nio.file.Files.newInputStream(path)) {
    // Работа с открытым потоком.
}
// close() вызывается при выходе из try в установленном порядке.
```

Здесь `path` — заранее существующая переменная типа `Path`: это поясняющий фрагмент, а не готовая программа. Garbage collector управляет памятью, но не даёт детерминированного срока закрытия внешнего ресурса. Источники: [Object.finalize](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#finalize()), [AutoCloseable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/AutoCloseable.html), [JLS 21 §14.20.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.3).

## wait/notify/notifyAll: объект как монитор, а не просто «сигнал»

У каждого объекта есть монитор, и методы `wait`, `notify` и `notifyAll` связаны с условным ожиданием на этом мониторе. Их методы `final`, их нельзя переопределять в подклассе. Вызывать их следует, **владея соответствующим монитором**; иначе возникает `IllegalMonitorStateException`.

При `wait` поток временно освобождает монитор объекта и ждёт уведомления, прерывания либо другого разрешённого условия пробуждения. `notify` не передаёт монитор немедленно пробуждённому потоку: тот ещё должен вновь получить владение монитором. Возможны и *spurious wakeups*, поэтому ожидание условия оформляют циклом, а не одним `if`.

```java
synchronized (lock) {
    while (!ready) {
        lock.wait();
    }
    // Условие ready проверено при владении монитором lock.
}
```

Это **фрагмент** внутри метода с доступными переменными `lock` и `ready`; код должен учитывать `InterruptedException` и согласованный протокол изменения `ready`. Полную многопоточную программу здесь намеренно не строим: блокировки, happens-before, interruption и корректный протокол пробуждения — отдельная будущая тема `CON-MONITOR`. См. [Object.wait](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#wait()), [notify](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#notify()), [notifyAll](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#notifyAll()) и [JLS 21 §17.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.2).

## Границы и проверка понимания

- **`Object` не гарантирует value equality.** Это исходная identity-семантика метода `equals`; конкретные классы могут задать другое отношение. [CORE-EQUALITY](equality.md) объясняет нормативный контракт и последствия в коллекциях.
- **Рефлексия ≠ равенство:** `getClass()` определяет фактический тип, а не совпадение полей.
- **Identity hash ≠ уникальность:** коллизии допустимы и здесь; никакого адреса памяти или устойчивого идентификатора API не обещает.
- **`clone` ≠ глубокое копирование:** в исходном механизме ссылочные поля остаются общими.
- **`finalize` ≠ `close`:** не управляйте сроком освобождения ресурса через сборщик мусора.
- **Мониторные методы ≠ готовый протокол конкуренции:** без синхронизации условий можно получить ошибки и потерянные уведомления.
- **Специальные карты:** `IdentityHashMap` использует ссылочную идентичность ключей и нарушает общий смысл сравнения ключей через `equals`; это P2-граница, не обычное поведение `HashMap`. См. [Java 21 IdentityHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/IdentityHashMap.html).
- **Value-based classes и record:** их соглашения не делают весь Java-мир единым «значенческим» механизмом. Для массивов, record и сортировки — [CORE-EQUALITY](equality.md); полноценный курс по record — будущая тема `CORE-RECORDS`.
- **Область ответственности:** виртуальный вызов и конструкторы — [CORE-INHERITANCE](inheritance.md) и [CORE-INIT](initialization.md). Полный контракт потоковой публикации — будущая `CON-JMM`, а внутренние структуры hash collections — `COL-HASH`.

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

**Исследовательские и интервью-ориентиры:** принятый [G1-план](https://github.com/Melchiorius/backend-interview-labs/blob/eede792ea16005e9087f4bc8eca5617c73c79761/docs/plans/CORE-OBJECT-EQUALITY.md) и [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md), включая Q-HRUDAY, Q-CHERNYSHOV, Q-TEAMLEAD, Q-IN28 и Q-BAELDUNG-COL. Интервью-подборки помогают обнаруживать пробелы, **но не доказывают нормативное поведение, статистическую частоту вопросов или полноту**.

**Проверяемость:** показанные ожидаемые результаты являются предсказаниями по спецификации; фактические запуск и CI конкретной интегрированной версии документируются в авторском PR. Отдельные небольшие фрагменты помечены как фрагменты; они не являются автономными файлами. Визуальный Obsidian/IntelliJ и специальные class-loader/мониторные сценарии не заявлены проверенными.
