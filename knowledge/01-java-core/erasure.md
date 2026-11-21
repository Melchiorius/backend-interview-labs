# CORE-ERASURE — стирание типов, unchecked-код и границы runtime

**Java SE 21.** Основание — [принятый G1-план Generics/Erasure](../../docs/plans/CORE-GENERICS-ERASURE.md), Issue [#82](https://github.com/Melchiorius/backend-interview-labs/issues/82). Начинать следует со статьи [CORE-GENERICS](generics.md): она объясняет, какие связи типов компилятор устанавливает *до запуска*. Здесь разберём, какие из этих сведений остаются при выполнении, где появляются проверки и почему некоторые ошибки проявляются поздно.

## Почему типы параметров не становятся отдельными runtime-классами (GE-10)

~~~java
List<String> words = new ArrayList<>();
List<Integer> numbers = new ArrayList<>();
System.out.println(words.getClass() == numbers.getClass()); // true
~~~

Мы создали два объекта **одной реализации** `ArrayList`. Java проверяет при компиляции разные контракты ссылок `List<String>` и `List<Integer>`, но runtime-класс экземпляра — `ArrayList`, а не новый «ArrayList-of-String». Этот результат нельзя обобщать как «абсолютно все generic-сведения исчезают»: в декларациях методов и суперклассов могут оставаться generic **метаданные**; при доступе к значениям компилятор может вводить приведения типов. Строгое правило — [JLS 21 §4.6 (type erasure)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.6).

Упрощённая схема стирания:

| Исходная конструкция | Что важно после erasure |
|---|---|
| `List<String>` или `List<Integer>` | `List` (типовая параметризация не создаёт два runtime-класса) |
| `T` без явной границы | стирается к `Object` |
| `T extends Number` | стирается к `Number` |
| `T extends Number & Comparable<T>` | к первой, левой границе — `Number` |
| `<T> T identity(T t)` | соответствующая erased сигнатура использует `Object` |

Это **модель статических типов и сигнатур**, а не обещание точного Java-исходника, который генератор класса обязан физически записать. Нельзя выводить спецификацию `javac` или количество байткод-методов только из таблицы.

## Где берётся ClassCastException после правильного вызова generic-метода

У обычного клиента компилятор проверяет совместимость generic-выражений; если он знает, что вызов `get()` должен иметь тип `String`, для реального использования результата могут потребоваться проверки или приведения. Некорректный raw/unchecked-код способен нарушить обещание параметризованного типа.

~~~java
List<String> strings = new ArrayList<>();
List raw = strings;            // raw-ссылка: обход параметра String
raw.add(67);                  // компилируется с unchecked warning

try {
    String text = strings.get(0); // ClassCastException при использовании результата как String
} catch (ClassCastException expected) {
    System.out.println("Значение не String");
}
~~~

В момент `raw.add(67)` объект `Integer` попал в тот же список; сама запись не обязана бросать исключение. Ошибка обнаруживается, когда извлечённое значение должно соответствовать ожидаемому `String`. Это не означает, что *любой* unchecked warning обязательно приводит к исключению. Но предупреждение показывает: компилятор не смог обеспечить всю гарантию типизации. [JLS §4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.8), [§5.1.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.9).

`List raw` — **raw type**, используемый главным образом для совместимости с устаревшими API. Современный код, если элементный тип действительно неизвестен, обычно должен предпочесть `List<?>`: он разрешает безопасное чтение как `Object`, не давая свободно записывать произвольные элементы. Raw-ссылка разрешает больше небезопасных вызовов, но не делает программу более типобезопасной.

## Reifiable / non-reifiable: что runtime может проверить (GE-11, GE-12, GE-18)

**Reifiable** — тип, для которого runtime доступно достаточно информации, предусмотренной языком для соответствующих проверок. Примеры: `String`, `String[]`, raw `List`, `List<?>`. Параметризация `List<String>` — **non-reifiable**. См. [JLS §4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.7).

Однако неверно говорить «в Java невозможно `instanceof` с любым non-reifiable типом». С Java 16 ключевое правило [JLS §15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2) связано с **checked cast compatibility** ([§5.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.5)). Сравним:

~~~java
List<String> typed = new ArrayList<>();
boolean first = typed instanceof List<?>;             // корректно
boolean second = typed instanceof ArrayList<String>; // корректно в Java 21

Object object = new ArrayList<String>();
boolean third = object instanceof List<?>;           // корректно
// boolean fourth = object instanceof List<String>;  // ошибка компиляции
~~~

Во втором случае статический тип выражения уже ограничивает допустимую параметризацию при проверяемом приведении к `ArrayList<String>`. JVM при этом **не проверяет отдельные runtime type arguments** внутри списка: для проверки принадлежности достаточно класса объекта. В четвёртом случае статический `Object` не даёт такой гарантии; приведение к `List<String>` было бы unchecked, поэтому `instanceof` запрещён. Формула «справа всегда должен стоять reifiable тип» является **устаревшим обобщением** для Java 21.

Похожее ограничение объясняет, почему запрещены конструкции:

~~~java
class Factory<T> {
    // T item = new T();         // у T нет известного конструктора как у класса
    // Class<T> type = T.class;  // class literal переменной типа запрещён
    // T[] items = new T[2];    // нельзя создать массив с таким компонентным типом
}

// List<String>[] array = new List<String>[2]; // нельзя создать generic array
List<?>[] wildcardArrays = new List<?>[2];    // допустимо
~~~

В случае `new T()` есть также принципиальная проблема знания конкретного конструктора, а не только erasure. При массивах компонентный тип должен быть совместим с runtime проверками хранения. `List<?>[]` допустим по правилам reifiability, но не превращает произвольный массив в безопасный `List<String>[]`. [JLS §10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html), [§15.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9).

## Heap pollution: когда статическое обещание нарушено (GE-13, GE-14)

**Heap pollution** возникает, когда переменная параметризованного типа ссылается на объект, тип которого несовместим с её статическим обещанием. Она может возникнуть через raw/unchecked conversion и некоторые varargs-механизмы. Это не просто «ошибка в куче»: термин описывает нарушение типовой согласованности, а не физическую порчу памяти JVM. [JLS §4.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.2).

Рассмотрим generic varargs. Вызывающая сторона предоставляет массив с компонентным типом, который можно представить при runtime; через alias этого массива можно поместить несовместимый список:

~~~java
@SafeVarargs
static String unsafeFirst(List<String>... lists) {
    Object[] alias = lists;
    alias[0] = List.of(67);
    return lists[0].get(0);
}

try {
    unsafeFirst(List.of("ok")); // выбросит ClassCastException
} catch (ClassCastException expected) {
    System.out.println("Обещание List<String> нарушено");
}
~~~

`alias[0] = List.of(67)` помещает в массив значение, которое runtime-массив может принять как `List`. Но метод затем пытается извлечь `String` из объекта `List<Integer>`, потому что исходная параметризация обещала другой тип. В таком методе `@SafeVarargs` **не обеспечивает безопасность**: это обещание автора о корректности тела относительно потенциально небезопасных операций с varargs, а не runtime-защита и не исправление действий с alias.

Собранная программа может выдавать предупреждения даже при аннотации, если запустить дополнительные категории lint: `javac --release 21 -Xlint:unchecked -Xlint:varargs ...`. Нельзя фиксировать точный текст предупреждения как норму JLS; важны условия небезопасной операции. [JLS §9.6.4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.7), [API @SafeVarargs](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/SafeVarargs.html).

### Где допустима аннотация @SafeVarargs (GE-24)

В Java 21 она применима к static-методам, final instance-методам, конструкторам и private instance-методам с varargs. Компилятор проверяет допустимое место аннотации, **не доказывает** произвольную безопасность тела. Аннотировать переопределяемый публичный instance-метод нельзя. Не использовать `@SafeVarargs` просто ради подавления warning: сначала проверить, существует ли реальное загрязнение массива или небезопасная публикация его alias.

## Warning не равен ошибке компиляции (GE-23)

| Сценарий | Ожидаемая категория | Что это значит |
|---|---|---|
| `List raw = typed; raw.add(67);` | unchecked call warning для небезопасного вызова | `javac` допускает вызов, но типовая гарантия ослаблена |
| `List<String> list = raw;` | unchecked conversion warning | исходный raw не доказывает соответствие `String` |
| `(List<String>) object` при подходящем статическом типе выражения | возможно unchecked cast warning | приведение может быть разрешено, но аргумент типа не подтверждён runtime |
| `new List<String>[2]` | ошибка компиляции | массив с non-reifiable компонентным типом нельзя создать |
| Два `m(List<String>)` / `m(List<Integer>)` | ошибка компиляции | конфликт после стирания сигнатур |

Для изолированного опыта полезен `javac --release 21 -Xlint:unchecked -Xlint:varargs File.java`. **Это четыре разные проверки предупреждений W01–W04 принятого плана, а не часть общего success stdout.** Отсутствие warning не доказывает отсутствие логической ошибки; наличие warning не доказывает, что исключение обязательно возникнет.

## Почему overload только по generic-аргументу невозможен (GE-15)

~~~java
class Overloads {
    // void use(List<String> items) { }
    // void use(List<Integer> items) { }
}
~~~

Если раскомментировать оба метода, `javac` отвергнет объявления: оба имеют одинаковую **erasure-сигнатуру** `use(List)`. Это не просто правило «JVM игнорирует generics», а нормативное ограничение совместимости сигнатур и name clash. Перегрузка, различающаяся другими параметрами или действительно различными erased типами, может оставаться допустимой; выводить запрет на *любые* generic-overloads было бы неверно. [JLS §8.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.2), [§4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.6).

## Переопределение после стирания: откуда bridge (GE-16)

При работе с generic-базовым классом контракт переопределения проверяется на уровне Java-исходника, но после стирания типы параметров и возвращаемых значений могут отличаться. Компилятор вправе создавать **bridge method** для сохранения полиморфизма.

~~~java
class Parent<T> {
    T get() { return null; }
}

class Child extends Parent<String> {
    @Override String get() { return "child"; }
}

Parent<String> parent = new Child();
System.out.println(parent.get()); // child
~~~

В Java-коде `Child.get()` корректно переопределяет generic-контракт родителя. В объектном представлении стирание `Parent<T>.get()` даёт возвращаемый `Object`, тогда как `Child.get()` возвращает `String`. Для совместимости вызова через базовый тип компилятор может создать дополнительную посредническую реализацию. Стандарт языка требует соблюдения правил переопределения, но **не устанавливает универсального фиксированного числа synthetic/bridge методов** в classfile всех компиляторов.

Наличие bridge у конкретного `javac` можно наблюдать через reflection:

~~~java
for (var method : Child.class.getDeclaredMethods()) {
    if (method.isBridge()) {
        System.out.println("bridge: " + method.getName());
    }
}
~~~

Этот фрагмент зависит от представления конкретного скомпилированного класса; корректнее говорить «`javac 21` в данной проверке сгенерировал bridge», если проверка действительно выполнена, а не обещать одинаковый результат любой JVM. Обобщённые сигнатуры/вывод типов и правила overriding: [JLS §8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3), [Method.isBridge API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#isBridge()).

## Generic-метаданные через Reflection: что можно увидеть (GE-22, GE-29)

Erasure не стирает всякую информацию **об объявлениях**: reflection может возвращать generic return type и parameterized type **декларации метода**. Например, `Method.getGenericReturnType()` может сообщить, что возвращаемый тип метода `List<String>`. Но `new ArrayList<String>().getClass()` сообщает runtime-класс объекта и не восстанавливает «какой `String` аргумент был выбран при создании» исключительно из экземпляра.

~~~java
class Api {
    public List<String> names() { return List.of("a"); }
}

var method = Api.class.getMethod("names");
System.out.println(method.getGenericReturnType()); // java.util.List<java.lang.String> для javac21
~~~

Это не типовая проверка произвольных объектов в списке. Когда рефлексия вызвана к API, которого нет у конкретного Java-класса, или когда метаданные недоступны, соответствующие выводы требуют отдельного анализа. Для дальнейшего углубления — будущая CORE-REFLECTION; описание `ParameterizedType` и байткодных атрибутов не является задачей этой статьи.

## Другие ограничения параметризованных типов (GE-20)

~~~java
// class GenericProblem<T> extends Exception { } // ошибка компиляции
~~~

Обобщённый класс нельзя делать прямым или косвенным подклассом `Throwable` ([JLS §8.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.2)). Это связано с требованиями к исключениям при выполнении и отсутствием достаточной runtime-параметризации. Но обычный класс исключения и generic-метод, объявляющий тип исключения в throws-контракте с допустимыми ограничениями, — **не одно и то же**; не следует из запрета `GenericProblem<T>` выводить запрет на любые generic-связанные throws-объявления.

## Краткий разбор типичных неверных ответов

- «Generics исчезают полностью» — неверно: остаются runtime-классы, проверки/casts и метаданные *объявлений*.
- «Любой unchecked немедленно бросит ClassCastException» — неверно: исключение возможно в конкретной точке использования, а может не возникнуть.
- «`instanceof List<String>` всегда запрещён» — неверно для Java 21: критерий — checked cast compatibility с **левым статическим типом**.
- «`@SafeVarargs` обезопасит метод» — неверно: тело обязано быть действительно безопасным.
- «Раз `List<String>[]` нельзя, то вообще любые массивы списков нельзя» — неверно: `List<?>[]` допустим.
- «После стирания overriding ломается» — неверно: исходный контракт сохраняется, в том числе благодаря bridge там, где он необходим в реализации.
- «Статический тип `List<String>` — отдельный класс в JVM» — неверно: объект может иметь обычный runtime-класс `ArrayList`.

## Соответствие исследовательскому G1 и пределы статьи

| ID | Раскрытая тема |
|---|---|
| GE-02 (совместно с CORE-GENERICS), GE-10 | Erasure: первая граница, generic signature, runtime object |
| GE-11 | Java 21 `instanceof`: checked cast compatibility, отличие `List<?>` от `List<String>` |
| GE-12 | `new T()`, `T.class`, generic arrays и reifiability |
| GE-13 | Raw/unchecked, runtime cast и точка `ClassCastException` |
| GE-14 | Heap pollution и `@SafeVarargs` |
| GE-15 | Same-erasure name clash |
| GE-16 | Overriding и bridge |
| GE-18, GE-20, GE-22, GE-23, GE-24 (P1) | Массивы `List<?>`, запрет generic Throwable, reflection, lint, допустимость `@SafeVarargs` |
| GE-27, GE-28, GE-29 (P2) | Bytecode/Signature и `ParameterizedType`, framework DAO/proxy — очерчены границы без заявления полного разбора |

**Не входит в заявленный объём:** Java bytecode descriptors/Signature attributes, бинарная совместимость подробно, JIT/HotSpot memory layout и benchmark, фреймворковые прокси, generic DAO/JPA, достоверная информация о фактическом параметре типа произвольного объекта по его identity. Эти вопросы относятся к отдельным JVM/REFLECTION/DB/SPR темам (GE-27–29); самостоятельных статей ещё нет, фиктивных локальных ссылок не добавляем.

**Источники:** [JLS 21 §4.6–4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html), [§5.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.5), [§8.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4), [§9.6.4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.7), [§15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2), [API @SafeVarargs](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/SafeVarargs.html), [Method.isBridge](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#isBridge()). В [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) нормативные источники отделены от интервью-подборок.

**Воспроизводимый пример Java 21:** [README, исходник и полный stdout CORE-GENERICS + CORE-ERASURE](../../examples/core/generics-erasure/README.md). Принятый G1 `GenericsProbes.java` даёт **20 наблюдений P01–P20 одной программы**: raw alias → запись `Integer` → `ClassCastException` при чтении `String`; опасный `@SafeVarargs` с varargs heap pollution и пойманным `ClassCastException`; `ArrayStoreException` при ковариантном массиве; erased return types через Reflection; overriding generic parent и наблюдение bridge. При компиляции этого намеренно небезопасного учебного исходника на OpenJDK/javac 21 ожидаются предупреждения `[unchecked]` и `[varargs]`, поэтому здесь **не** используется `-Werror`. Значение **`P17=2,1,child` — наблюдение конкретного `javac` 21, а не универсальная гарантия JLS**. Дополнительный Java 21 случай `typed instanceof ArrayList<String>` из G1 P21 разбирается **выше в статье** и отдельно в G1-плане, но не выдаётся за строку runnable. Отрицательные N01–N22 и самостоятельные lint W01–W04 остаются отдельными пробами, а не частью одного success source set.

**Фрагменты в статье** показывают разные случаи и не составляют один общий Java-файл. Связанная статья — [CORE-GENERICS](generics.md).
