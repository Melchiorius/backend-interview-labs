# CORE-EQUALITY — равенство, hashCode и порядок в Java 21

**Версия:** Java SE 21. **Issue:** [#62](https://github.com/Melchiorius/backend-interview-labs/issues/62). **Исследовательская основа:** [принятый G1-план CORE-OBJECT-EQUALITY](https://github.com/Melchiorius/backend-interview-labs/blob/eede792ea16005e9087f4bc8eca5617c73c79761/docs/plans/CORE-OBJECT-EQUALITY.md), OE-01–OE-26. Базовый обзор класса, в котором объявлены `equals` и `hashCode`, — [CORE-OBJECT](object.md).

## Сначала определим, что значит «одинаковые»

В Java есть несколько разных способов поставить вопрос о двух значениях. Они **не взаимозаменяемы**:

| Операция | На какой вопрос отвечает |
|---|---|
| `a == b` при числовых/boolean примитивах | Равны ли значения после предусмотренных преобразований? |
| `a == b` при двух ссылках | Указывают ли обе ссылки на один объект (или обе `null`)? |
| `a.equals(b)` | Считает ли **реализация метода у объекта `a`** значение `b` равным себе? |
| `Objects.equals(a,b)` | То же отношение `equals`, но с определённой обработкой `null` |
| `a.hashCode()` | Какой целочисленный hash выдаёт тип по своему контракту? Это **не тест равенства** |
| `a.compareTo(b)` или `Comparator.compare(a,b)` | Как эти значения упорядочены: меньше, эквивалентны для данного порядка или больше? |

Самое частое заблуждение: «Если поля совпадают, `equals` должен вернуть `true`». Нет. `Object.equals(Object)` по умолчанию сравнивает ссылки. Лишь конкретный тип принимает решение считать, например, два разных экземпляра `AccountKey("service", 67)` эквивалентными **по значениям**. Нормативные источники: [JLS 21 §15.21](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21), [Object.equals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#equals(java.lang.Object)), [Objects.equals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html#equals(java.lang.Object,java.lang.Object)) и [Comparable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Comparable.html).

## 1. Что в действительности сравнивает ==

**Числа.** Для совместимых числовых примитивов действует *binary numeric promotion*; типы могут быть расширены до общего числового типа. Например, `67 == 67L` — `true`. Для `float/double` действует численная семантика: `Double.NaN == Double.NaN` — `false`, `+0.0 == -0.0` — `true`. Это сравнение не устанавливает объектную эквивалентность `Double.equals`.

**Boolean.** `true == true` — `true`, но `int` с `boolean` сравнивать нельзя.

**Ссылки.** Две ссылки равны через `==`, если обе равны `null` либо обозначают один и тот же объект. Компилятор дополнительно требует допустимости сравниваемых ссылочных типов; нельзя сравнивать произвольные несовместимые ссылочные типы. `new String("java") == new String("java")` — `false`, а их `equals` возвращает `true`.

**Boxing/unboxing влияет на выбор операции.** Сравнение `Integer` с `int` обычно требует *unboxing*; если ссылка `null`, появляется `NullPointerException`. Если сравниваются две ссылки типа `Integer`, `==` проверяет identity, а не число. Не выводите из отдельных примеров кеширования обёрток универсальное свойство числового `==`.

```java
int primitive = 67;
Integer boxed = Integer.valueOf(67);
Integer missing = null;

System.out.println(boxed == primitive); // true: unboxing + числовое ==
System.out.println(boxed.equals(primitive)); // true: Integer.equals
// System.out.println(missing == primitive); // NullPointerException при исполнении
```

Эти строки — фрагмент внутри метода. У следующих **отдельных** фрагментов — ошибки компиляции:

```java
String s = "67";
Integer n = 67;
int primitive = 67;
// boolean a = s == n;              // compile-time error: incomparable reference types
// boolean b = 67 == true;          // compile-time error: int vs boolean
// boolean c = primitive.equals(n); // compile-time error: int cannot be dereferenced
```

Важно: закомментированные операторы намеренно не компилируются, если убрать комментарий. Это ошибки **типизации на этапе компиляции**, а не контрпримеры нарушенного `equals`. Нормативно: [JLS 21 §15.21.1–15.21.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21) и [§5.1.8 — Unboxing Conversion](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.8). Подробнее о типах и преобразованиях: [CORE-TYPES](types.md), [CORE-CONVERSIONS](conversions.md).

**Большая самостоятельная демонстрация роли 03:** [README и запуск CORE-EQUALITY](../../examples/core/equality/README.md), [EqualityDemo.java](../../examples/core/equality/src/EqualityDemo.java), [полный expected.stdout](../../examples/core/equality/expected.stdout). Там те же правила проверяются на другом именованном value key — `ValueKey(id,code)` — и намеренно сломанных ключах. В воспроизводимом выводе есть как гарантированные отношения, так и *наблюдаемый на OpenJDK 21* эффект неверного hash и изменяемого ключа; `verify.sh` сравнивает весь вывод. Ниже дан меньший самостоятельный `AccountKey` для подробного объяснения корректной реализации; это **разные классы и два разных учебных фрагмента**, не одна и та же программа.

## 2. Базовый корректный value object

Предположим, ключ счёта определяется **парой** «организация + номер счёта». Два отдельных экземпляра с одной парой должны считаться равными и одинаково работать как ключ карты. Мы явно выбираем правило равенства и не используем память или текстовое представление объекта как идентификатор.

Полный самостоятельный файл `ValueKeyDemo.java` (JDK 21):

```java
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class AccountKey {
    private final String tenant;
    private final long number;

    AccountKey(String tenant, long number) {
        this.tenant = Objects.requireNonNull(tenant);
        this.number = number;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AccountKey that)) {
            return false;
        }
        return number == that.number && tenant.equals(that.tenant);
    }

    @Override
    public int hashCode() {
        return 31 * tenant.hashCode() + Long.hashCode(number);
    }
}

public class ValueKeyDemo {
    public static void main(String[] args) {
        AccountKey first = new AccountKey("service", 67);
        AccountKey second = new AccountKey("service", 67);
        AccountKey third = new AccountKey("service", 68);

        System.out.println(first == second);
        System.out.println(first.equals(second));
        System.out.println(first.hashCode() == second.hashCode());

        Set<AccountKey> unique = new HashSet<>();
        unique.add(first);
        unique.add(second);
        unique.add(third);
        System.out.println(unique.size());

        Map<AccountKey, String> map = new HashMap<>();
        map.put(first, "found");
        System.out.println(map.get(second));
    }
}
```

Запуск из каталога с этим файлом:

```sh
javac --release 21 ValueKeyDemo.java
java ValueKeyDemo
```

Ожидаемый stdout:

```text
false
true
true
2
found
```

**Разберём каждый переход.** В первой строке `false`, поскольку `first` и `second` — два созданных объекта. Вторая строка `true`: `AccountKey.equals` сравнивает ту же пару значений, а `instanceof AccountKey that` проверяет тип без опасного cast. Третья строка `true`: одна и та же пара значений даёт один и тот же результат согласованного `hashCode`. `HashSet` оставляет два различных *значения* — счёт 67 и счёт 68. `HashMap.get(second)` находит запись, созданную по другому, но равному ключу `first`.

**Почему реализация надёжнее, чем множество публичных сеттеров?** `final`-класс не допускает подкласса с новым спорным полем равенства; оба поля `private final` и неизменяемы (`String` тоже неизменяем). Это удерживает equality/hash стабильными на время жизни ключа. Значение `31` в формуле — лишь детерминированная комбинация полей, не нормативное требование; коллизии возможны и допустимы.

**Граница:** этот пример показывает корректный ключ с двумя полями, но не задаёт универсального рецепта бизнес-идентичности. Если домен считает существенными другие поля либо допускает изменение идентификатора, нужно заново обосновать правило равенства. Равенство по value не автоматически «правильнее» равенства по identity.

## 3. Полный контракт equals и типичная ошибка overload

Переопределение `equals(Object)` обязано задавать отношение эквивалентности. [Object.equals, Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#equals(java.lang.Object)) формулирует пять условий:

1. **Reflexive:** для `x != null`, `x.equals(x)` — `true`.
2. **Symmetric:** для `x,y != null`, `x.equals(y)` и `y.equals(x)` согласованы.
3. **Transitive:** если `x.equals(y)` и `y.equals(z)`, то `x.equals(z)`.
4. **Consistent:** повторные сравнения дают тот же ответ, если данные, используемые в сравнении, не менялись.
5. **Non-null:** для `x != null`, `x.equals(null)` — `false`.

**Симметрия важна**, потому что `Objects.equals(a,b)` и `Objects.equals(b,a)` в нормальной реализации не должны сообщать разные отношения равенства. **Транзитивность важна**, потому что коллекция не должна видеть цепочку `a~b`, `b~c` и при этом отрицать `a~c`. **Consistency** не обязывает все объекты быть вечно неизменяемыми, но делает изменение полей равенства опасным во время использования ключом.

Сигнатура имеет значение. Это **не** override:

```java
class WrongKey {
    private final int id;

    WrongKey(int id) {
        this.id = id;
    }

    public boolean equals(WrongKey other) { // overload, не equals(Object)
        return other != null && id == other.id;
    }

    // @Override
    // public boolean equals(WrongKey other) { ... }
    // с @Override компилятор обнаружил бы отсутствие переопределяемого метода.
}
```

Если сравнивать через `Object` или вызвать `Objects.equals`, работает унаследованный `Object.equals(Object)`, то есть identity. Для пользовательского равенства нужна **точная** сигнатура `public boolean equals(Object other)` и `@Override`. Аннотация помогает компилятору ловить ошибку; она не меняет сами правила dispatch. См. [JLS 21 §8.4.8.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.1) и [§9.6.4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.4), [Object.equals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#equals(java.lang.Object)).

## 4. hashCode: обязательная связь, но не идентификатор

[Object.hashCode, Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#hashCode()) требует:

- при неизменности данных, участвующих в `equals`, последовательные вызовы на одном объекте во время выполнения программы дают согласованное значение;
- если `x.equals(y) == true`, то `x.hashCode() == y.hashCode()`;
- из `x.equals(y) == false` **не следует** требование разных хешей.

Последнее означает, что **коллизия** (одинаковый hash у неравных объектов) разрешена. Hash — способ сузить поиск для hash-based collection, а не окончательное доказательство равенства. Его нельзя объявлять уникальным идентификатором, адресом памяти или хешем, устойчивым между запусками процесса.

### Контрпример: разные объекты с одним hash — корректно

```java
final class CollisionKey {
    final int id;

    CollisionKey(int id) {
        this.id = id;
    }

    @Override public boolean equals(Object other) {
        return other instanceof CollisionKey that && id == that.id;
    }

    @Override public int hashCode() {
        return 67; // плохая производительность при большом числе ключей, но не ошибка контракта
    }
}

var set = new java.util.HashSet<CollisionKey>();
set.add(new CollisionKey(67));
set.add(new CollisionKey(68));
System.out.println(set.size()); // 2
```

Два ключа остаются разными, поскольку их `equals` возвращает `false`. Последние три строки здесь — фрагмент внутри метода. Константный hash даёт множество коллизий и обычно ухудшает эффективность, но **не** делает сравниваемые объекты равными.

### Контрпример: equals переопределён, hashCode унаследован

```java
final class BrokenKey {
    final int id;

    BrokenKey(int id) {
        this.id = id;
    }

    @Override public boolean equals(Object other) {
        return other instanceof BrokenKey that && id == that.id;
    }

    // hashCode НЕ переопределён: остался Object.hashCode()
}
```

Два разных `new BrokenKey(67)` будут равны по `equals`, однако исходная реализация `Object.hashCode` **не обязана** присваивать им одинаковый hash. Это и есть несоблюдение обязательного контракта. Если в данном запуске хеши случайно совпали, тип не стал корректным: равенство всё ещё не обеспечено реализацией hash.

**Почему ломается поиск в HashMap?** Hash-based map может сначала учитывать hash для поиска кандидатов и только затем равенство. При разных хешах двух равных ключей поиск по новому экземпляру способен не найти существующую запись. Это объяснение причины, но **не обещание**, что `map.get(new BrokenKey(67))` всегда вернёт `null` на каждой JVM. `Map.containsKey` API вообще не требует вызывать `equals` физически при каждом поиске. Источники: [Map](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html), [HashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html), [Object.hashCode](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#hashCode()). Подробное устройство bucket/tree bins/resize — будущая тема `COL-HASH`, не языковая гарантия Java.

## 5. null-safe сравнение: Objects.equals

Вызов `reference.equals(other)` требует ненулевой ссылки `reference`. Если она равна `null`, JVM бросит `NullPointerException`. `Objects.equals(a,b)` обрабатывает `null`, используя обычный `equals` при ненулевом первом аргументе:

```java
Object a = null;
Object b = null;
Object c = new String("67");
Object d = new String("67");

System.out.println(java.util.Objects.equals(a, b)); // true
System.out.println(java.util.Objects.equals(a, c)); // false
System.out.println(java.util.Objects.equals(c, d)); // true
System.out.println(c == d); // false: два разных new String
```

Последние результаты следуют из `String.equals`, а не из какого-то особого «глубокого» сравнения `Objects.equals`. Этот helper **не исправляет** неправильный `equals`, асимметрию или плохой hash-контракт. Для вложенных массивов есть **другая** операция `Objects.deepEquals`; см. отдельный раздел об массивах. Источник: [Objects.equals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html#equals(java.lang.Object,java.lang.Object)).

## 6. Наследование: симметрия и транзитивность не возникают автоматически

Представьте тип `ParentTag`, сравнивающий экземпляры по `id`, и подкласс `ChildTag`, добавляющий `color`. Если родитель признаёт любой подкласс по `instanceof ParentTag`, но потомок требует дополнительного свойства, возникает асимметрия.

```java
class ParentTag {
    final int id;

    ParentTag(int id) { this.id = id; }

    @Override public boolean equals(Object other) {
        return other instanceof ParentTag that && id == that.id;
    }

    @Override public int hashCode() { return Integer.hashCode(id); }
}

class ChildTag extends ParentTag {
    final String color;

    ChildTag(int id, String color) {
        super(id);
        this.color = color;
    }

    @Override public boolean equals(Object other) {
        return other instanceof ChildTag that
                && super.equals(that)
                && color.equals(that.color);
    }

    @Override public int hashCode() { return super.hashCode(); }
}

ParentTag parent = new ParentTag(67);
ChildTag child = new ChildTag(67, "red");
System.out.println(parent.equals(child)); // true
System.out.println(child.equals(parent)); // false
```

Один и тот же «факт равенства» зависит от направления вызова, значит **симметрия нарушена**. Вызов `parent.equals(child)` попадает в реализацию `ParentTag.equals`; `child.equals(parent)` — в реализацию `ChildTag.equals`. Это прямо связано с виртуальной диспетчеризацией, разобранной в [CORE-INHERITANCE](inheritance.md). Код компилируется: это **нарушение контракта API**, а не ошибка синтаксиса.

Транзитивность можно нарушить и без наследования. Например, равенство «числа отличаются не более чем на один» кажется симметричным, но не транзитивно:

```java
final class Near {
    final int number;
    Near(int number) { this.number = number; }

    @Override public boolean equals(Object other) {
        return other instanceof Near that
                && Math.abs((long) number - that.number) <= 1;
    }

    @Override public int hashCode() { return 67; }
}

Near x = new Near(67), y = new Near(68), z = new Near(69);
System.out.println(x.equals(y)); // true
System.out.println(y.equals(z)); // true
System.out.println(x.equals(z)); // false
```

Одинаковый hash здесь намеренно устраняет вторую независимую ошибку, чтобы было видно именно **нарушение транзитивности**.

### getClass() или instanceof — что «правильнее»?

`getClass() == other.getClass()` запрещает равенство между объектами разных runtime-классов; это может быть уместно, если каждый наследник представляет отдельное понятие. `instanceof SomeBase` разрешает рассматривать подкласс как тот же базовый тип; это может быть уместно, если подклассы **не меняют** равенство и подчиняются единому контракту.

Но ни `getClass`, ни `instanceof` не является универсальным лекарством. При изменяющей семантику equality иерархии иногда лучше **композиция** либо запрет наследования (`final` value class), как в `AccountKey`. Любой choice нужно обосновывать предметной областью, особенно при будущих ORM proxy. В этой статье не утверждается, как ведёт себя конкретная версия Hibernate: это отдельная будущая проверка `DB-JPA`. Нормативно: [Object.equals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#equals(java.lang.Object)), [Object.getClass](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#getClass()), [JLS 21 §15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2).

## 7. Изменяемый ключ в HashMap

Допустим, equality и hash зависят от изменяемого идентификатора:

```java
final class MutableKey {
    int id;
    MutableKey(int id) { this.id = id; }

    @Override public boolean equals(Object other) {
        return other instanceof MutableKey that && id == that.id;
    }

    @Override public int hashCode() { return Integer.hashCode(id); }
}

var map = new java.util.HashMap<MutableKey, String>();
MutableKey key = new MutableKey(67);
map.put(key, "stored");
key.id = 68;
System.out.println(map.get(key)); // наблюдение зависит от JDK/состояния карты
```

**Механизм ошибки:** запись была вставлена при значении ключа 67; затем поле, от которого зависит его hash/equality, изменилось. `Map` API прямо предупреждает: если значение объекта меняется способом, влияющим на `equals`, пока объект используется как ключ, **поведение карты не специфицировано**. Это не означает «Map всегда вернёт null», «запись физически исчезла» или «после возврата 67 всё гарантированно исправится». Конкретный вывод нужно **наблюдать в явно названном JDK**, а не выдавать за контракт. Для обычной карты `get(...) == null` к тому же не доказывает отсутствие записи: отображённое значение тоже может быть `null`.

**Практический вывод:** используйте ключи со стабильными для equality/hash полями на протяжении времени хранения. Если модель обязана меняться, отделяйте изменяемое состояние от identity-ключа. Это рекомендация проектирования, а не правило, по которому Java запрещает `mutable`-объекты в роли ключей. Источник: [Map API, предупреждение о mutable keys](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html), [HashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html). Полная модель неизменяемости — будущая тема `CORE-IMMUTABLE`.

## 8. Comparable и Comparator: порядок — отдельный контракт

`Comparable<T>.compareTo(other)` определяет **естественный порядок**, а `Comparator<T>.compare(a,b)` — отдельное явно переданное правило. Результат имеет смысл **знака**: отрицательное, ноль или положительное число. Он не обязан быть ровно `-1` или `1`.

Контракт сравнения требует согласованности знаков, транзитивности и правильной эквивалентности относительно третьего элемента. Например, если `a > b` и `b > c`, порядок не должен внезапно сделать `a < c`. Нельзя бездумно писать `return a - b` для произвольных `int`: переполнение может исказить порядок; безопаснее `Integer.compare(a,b)`.

**Consistent with equals** — отдельное понятие: `compare(a,b) == 0` тогда и только тогда, когда `a.equals(b)` (для сравниваемых элементов данного типа). Для `Comparable` такая согласованность настоятельно рекомендуется, но **не является обязательным универсальным законом**: важное исключение — `BigDecimal`. `Comparator`, не согласованный с `equals`, способен задавать корректный порядок сам по себе, но сортированная коллекция тогда иначе понимает дубликаты.

### Как это влияет на TreeSet и TreeMap

`HashSet/HashMap` используют equality/hash семантику своих ключей. `TreeSet/TreeMap` определяют эквивалентность ключей через **нулевой результат сравнения** по natural order либо переданному comparator. Для соответствия общему контракту `Set/Map` порядок должен быть согласован с `equals`; если он не согласован, спецификация предупреждает о нарушении общего контракта, хотя поведение самой сортированной структуры остаётся определённым.

```java
var byLength = java.util.Comparator.comparingInt(String::length);
var ordered = new java.util.TreeSet<String>(byLength);
ordered.add("AA");
ordered.add("BB");
System.out.println("AA".equals("BB")); // false
System.out.println(byLength.compare("AA", "BB")); // 0
System.out.println(ordered.size()); // 1: длина совпала
```

`Comparator` здесь **не нарушает собственный контракт**: он осмысленно упорядочивает строки по длине, но его эквивалентность не совпадает с `String.equals`. Другой пример — компаратор, нарушающий транзитивность, — уже является нарушением *самого* `Comparator` contract; результат сортировки с ним не стоит прогнозировать по спецификации.

Нормативно: [Comparable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Comparable.html), [Comparator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html), [TreeSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html), [TreeMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html), [SortedMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SortedMap.html). Структуры деревьев и сложность операций — будущая тема `COL-SORTED`.

## 9. BigDecimal: одно численное значение, два понятия равенства

`new BigDecimal("2.0")` и `new BigDecimal("2.00")` представляют одно численное значение, но имеют **разный scale**. У `BigDecimal.equals` учитываются и числовое значение, и scale; `compareTo` сравнивает их как числа.

```java
var a = new java.math.BigDecimal("2.0");
var b = new java.math.BigDecimal("2.00");

System.out.println(a.equals(b));    // false
System.out.println(a.compareTo(b)); // 0

var hashSet = new java.util.HashSet<java.math.BigDecimal>();
hashSet.add(a);
hashSet.add(b);
System.out.println(hashSet.size()); // 2

var treeSet = new java.util.TreeSet<java.math.BigDecimal>();
treeSet.add(a);
treeSet.add(b);
System.out.println(treeSet.size()); // 1
```

Это **не** поломка `BigDecimal`: его природный порядок документирован как несогласованный с `equals`. `HashSet` видит два неравных по `equals` значения, `TreeSet` — один класс эквивалентности по `compareTo==0`. Если бизнес-модель считает масштаб частью значения, следует явно обосновать тип коллекции или правило нормализации, а не просто заменить любую карту на TreeMap.

Нормативно: [BigDecimal.equals / compareTo](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html), [Comparable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Comparable.html), [TreeSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html).

## 10. Массивы: Object.equals, Arrays.equals, Arrays.deepEquals

Массив — объект Java, но его унаследованный `equals` не превращает набор элементов в value object. Два разных массива с одинаковыми элементами по умолчанию **не равны** через `array1.equals(array2)`.

```java
int[] first = {67};
int[] second = {67};

System.out.println(first == second);             // false
System.out.println(first.equals(second));        // false
System.out.println(java.util.Arrays.equals(first, second)); // true

int[][] nestedA = {{67}};
int[][] nestedB = {{67}};
System.out.println(java.util.Arrays.equals(nestedA, nestedB));     // false
System.out.println(java.util.Arrays.deepEquals(nestedA, nestedB)); // true
System.out.println(java.util.Objects.deepEquals(nestedA, nestedB)); // true
```

`Arrays.equals` у одномерных примитивных массивов сравнивает элементы соответствующей перегрузкой. Для `Object[]` сравнивает соответствующие элементы, используя их `equals`, поэтому два внутренних массива (сами отдельные объекты) дают `false`. `Arrays.deepEquals(Object[],Object[])` рекурсивно сравнивает вложенные массивы; `Objects.deepEquals` применяет подходящую проверку и допускает `null`. `Arrays.deepEquals` нельзя вызвать с одиночным `int[]`: соответствующий метод принимает `Object[]`.

**Ограничения:** «глубокое» сравнение — не произвольный обход объектных графов. Внутренние обычные объекты всё равно сравниваются согласно своим `equals`; сложные циклические самоссылочные массивы могут приводить к особым рискам, поэтому не используйте этот метод как универсальный графовый алгоритм. Для массивов-ключей HashMap обычная объектная identity остаётся существенной; без value wrapper содержимое `byte[]` само по себе не определяет равенство ключей.

Источники: [Arrays.equals/deepEquals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html), [Objects.deepEquals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html#deepEquals(java.lang.Object,java.lang.Object)), [Object.equals](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#equals(java.lang.Object)).

## 11. Точные границы P2: что здесь не обещается

- **Числа с плавающей точкой (OE-21):** примитивное `NaN == NaN` ложно, но `Double.equals` рассматривает канонизованные NaN как равные; примитивные `+0.0 == -0.0` истинно, а `Double.equals` их различает. Это поведение стандартизовано [JLS §15.21](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21) и [Double API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Double.html#equals(java.lang.Object)); полный разбор — будущая `CORE-NUMBERS`.
- **Record (OE-22):** `record Payload(byte[] value) {}` получает автоматически реализованные `equals/hashCode`, но массивный компонент сравнивается по собственному правилу массива (identity), не «deep». Например, два `new Payload(new byte[]{67})` не обязаны быть равны по содержимому вложенных массивов. Полный контракт record — [JLS 21 §8.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10) и [Record API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html); будущая `CORE-RECORDS`.
- **IdentityHashMap и массивные ключи (OE-23):** [IdentityHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/IdentityHashMap.html) сравнивает ссылки по `==` даже для типов с value `equals`; обычный `HashMap<byte[],V>` также видит два разных массива как неравных по их унаследованному `equals`. Это разные причины схожего результата, их не следует смешивать.
- **Изменяемые коллекции и concurrency (OE-24):** mutable-key проблема возникает и в одном потоке; data race или небезопасная публикация — **другая** проблема Java Memory Model. Не доказано, что `equals` потокобезопасен, лишь потому что один поток успешно выполнил тест. См. [JLS 21 §17](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html); дальнейшая тема `CON-JMM`.
- **ORM/proxy (OE-25):** замена runtime-типа на proxy может иметь последствия для `getClass`/`instanceof`, но ни JLS, ни Object API не определяют правила идентичности сущности БД. Конкретные Hibernate/Spring версии и жизненный цикл ORM — отдельная будущая тема `DB-JPA`, здесь не проверено.
- **Identity hash и сохранение (OE-26):** `Object.hashCode`, `System.identityHashCode` и базовый `toString` не гарантируют адрес памяти, уникальность, пригодность как persistent ID или криптографическую стойкость. Подробное введение — [CORE-OBJECT](object.md).

## 12. Что проверять на собеседовании вместо заучивания одной формулы

Хороший ответ разделяет три уровня: **какое отношение требуется предметной области**, **какой контракт API должен быть соблюдён** и **какой конкретный результат можно воспроизвести на указанном JDK**. Например, если два `AccountKey` равны по значениям, контракт требует одинаковых hash, но не уникальных hash; если ключ мутировал после вставки, стандарт Map не обещает конкретный результат; если TreeSet видит `compareTo==0`, это не автоматически значит `equals==true`.

При чтении незнакомого примера полезно последовательно определить статические типы выражения `==`, наличие boxing/unboxing, реальную сигнатуру метода `equals(Object)`, поля value equality, реализацию hash, возможность изменения этих полей и смысл результата `compare/compareTo`. Каждое из этих уточнений способно изменить вывод.

## Карта покрытия CORE-OBJECT + CORE-EQUALITY

Следующая таблица отражает распределение **всех** OE-01–OE-26 между двумя самостоятельными статьями. Приоритеты взяты из принятого исследования, не из статистической частоты вопросов.

| ID | Приоритет | Объяснение и проверяемый механизм |
|---|---|---|
| OE-01 | P0 | Введение [CORE-OBJECT](object.md) + §1 и §2: identity против value equality |
| OE-02 | P0 | §1: числовое/boolean/reference `==`, unboxing, compile-negative |
| OE-03 | P0 | §3: пять свойств equals, перегрузка vs override |
| OE-04 | P0 | §4: equals ⇒ same hash, коллизии и неправильный override |
| OE-05 | P0 | §4: почему поиск равного broken key может не сработать |
| OE-06 | P0 | §6: Parent/Child asymmetry и Near transitivity |
| OE-07 | P0 | §5: null safety через Objects.equals |
| OE-08 | P0 | §4: два неравных элемента с одной hash |
| OE-09 | P0 | §7: mutable Map key, unspecified behavior |
| OE-10 | P0 | §8: Comparable/Comparator vs equals, sorted collections |
| OE-11 | P0 | §9: BigDecimal scale, HashSet=2, TreeSet=1 |
| OE-12 | P0 | §2–§4: корректный immutable key, overload/override |
| OE-13 | P1 | [CORE-OBJECT](object.md): каталог методов Object |
| OE-14 | P1 | §6: getClass vs instanceof — выбор семантики |
| OE-15 | P1 | [CORE-OBJECT](object.md): toString — диагностика, не ID |
| OE-16 | P1 | [CORE-OBJECT](object.md): identityHashCode |
| OE-17 | P1 | §10: Arrays.equals/deepEquals, Objects.deepEquals |
| OE-18 | P1 | §8: compareTo/Comparator signum и контракт порядка |
| OE-19 | P1 | [CORE-OBJECT](object.md): wait/notify, владение монитором |
| OE-20 | P1 | [CORE-OBJECT](object.md): clone и finalize |
| OE-21 | P2 | §11: Double NaN/±0.0 |
| OE-22 | P2 | §11: record-компоненты, не deep array equality |
| OE-23 | P2 | §11 + [CORE-OBJECT](object.md): IdentityHashMap и byte[] |
| OE-24 | P2 | §11: mutability vs concurrency |
| OE-25 | P2 | §6 и §11: версии ORM/proxy — не проверены |
| OE-26 | P2 | §11 + [CORE-OBJECT](object.md): hash/toString не persistent ID |

## Связанные материалы и источники

**Локальные рабочие ссылки:** [CORE-OBJECT](object.md), [CORE-TYPES](types.md), [CORE-CONVERSIONS](conversions.md), [CORE-CLASSES](classes.md), [CORE-INHERITANCE](inheritance.md), [CORE-INIT](initialization.md). Об устройстве загрузки, специализации массивов, collections и JMM есть отдельные будущие темы: `COL-HASH`, `COL-SORTED`, `CORE-ARRAYS`, `CORE-IMMUTABLE`, `CORE-RECORDS`, `CON-JMM`. У отсутствующих статей не создаются фиктивные Markdown-ссылки.

**Нормативные:** [JLS 21 §15.21](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21), [§5.1.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.8), [§8.4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8), [Object](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html), [Objects](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html), [Map](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html), [Set](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Set.html), [HashSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashSet.html), [HashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html), [Comparable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Comparable.html), [Comparator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html), [TreeSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html), [TreeMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html), [BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html) и [Arrays](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html). Правила `==` определяет JLS, а `equals/hashCode/compareTo/Comparator` — контракты Java API.

**Источники для аудита интервью-пробелов, не нормы:** [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md), Q-HRUDAY, Q-CHERNYSHOV, Q-TEAMLEAD, Q-IN28 и Q-BAELDUNG-COL из [принятого исследовательского плана](https://github.com/Melchiorius/backend-interview-labs/blob/eede792ea16005e9087f4bc8eca5617c73c79761/docs/plans/CORE-OBJECT-EQUALITY.md). Дополнительное внешнее сравнение: [DevsCampus Java 21 interview questions](https://devscampus.com/interview-questions/java-21), где прямо поставлен вопрос о consistency between equality, hash and Comparable. Вопросники помогают найти пробел, но не устанавливают требования языка.

**Фактическая проверка:** точные команды и результаты авторских локальных проб и CI будут записаны в PR на конкретном HEAD. Для примера роли 03 из [Issue #63](https://github.com/Melchiorius/backend-interview-labs/issues/63) предусмотрена интеграция в единый кандидат до G3. Отрицательные компиляционные фрагменты специально отделены от положительных и пока не объявлены опубликованным harness. JVM, отличные от проверенной, property-based tests всех законов equals, ORM/JPA, visual Obsidian/IntelliJ — `not run`, если не указано иное.
