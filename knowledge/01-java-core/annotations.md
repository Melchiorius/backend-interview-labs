# CORE-ANNOTATIONS — метаданные в Java 21: объявление, хранение и чтение

**Назначение.** Аннотация — типизированные метаданные о декларации или использовании типа, а не автоматически исполняемый обработчик. Читатель этой статьи должен уметь предсказать, что проверит компилятор, что окажется в class-файле и что увидит Java Reflection. Норма — [принятый G1 Java 21](../../docs/plans/CORE-ANNOTATIONS-REFLECTION.md), [JLS §9.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6), [§9.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7) и [AnnotatedElement Java SE 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AnnotatedElement.html). Исследовательский план — источник G1 evidence; это отдельная учебная статья G2.

**Адресная матрица:** AN-01–AN-17 — 17 P0, AN-18–AN-22 — 5 P1, AN-23–AN-25 — 3 P2, всего **25/25**. В паре с [CORE-REFLECTION](reflection.md) — 53/53. Соседние темы: [интерфейсы и SAM](interfaces.md), [функциональные интерфейсы](functions.md), [наследование классов](inheritance.md), [поля и видимость](access.md), [generics](generics.md), [records](records.md). Аннотации — не замена всем этим контрактам.

## 1. Объявляем тип аннотации: элементы, значения, default (AN-01, AN-02, AN-03, AN-14)

Предположим, архитектура приложения требует маркировать методы как операции определённого уровня доступа. Аннотация описывает **форму данных**, которые автор кода обязан сообщить; отдельная библиотека, компилятор или собственный reflection-код может затем эти данные прочитать.

~~~java
import java.lang.annotation.*;

enum Importance { HIGH, LOW }

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Operation {
    String value();                     // обязательный элемент
    int retries() default 3;           // необязательный элемент
    Importance level() default Importance.HIGH;
    Class<?> resultType() default String.class;
    int[] ports() default {67, 68};
}

class Service {
    @Operation(value = "find", retries = 2, ports = {67})
    String find() { return "found"; }
}
~~~

Синтаксис `@interface` объявляет **annotation interface**; запись `String value();` — **элемент** (метод без параметров), а не поле экземпляра или callback. Обязательному элементу без `default` надо дать значение при использовании. Для единственного элемента с именем `value` допускается сокращённая форма `@Operation("find")`, если остальные элементы имеют значения по умолчанию. `default` — характеристика объявления аннотации, а не вычисляемый конструктор экземпляра.

Допустимые возвращаемые типы элементов по JLS §9.6.1: примитив, `String`, `Class` (в том числе параметризованный Class), enum, annotation interface либо **одномерный массив** таких типов. Например, `int[]` и `Class<?>[]` подходят, а `Object`, `List<String>`, `String[][]`, `void` и параметр метода — нет. Отсюда compiler-negative G1 `AN_N03` (двумерный массив), `AN_N12` (Object), `AN_N13` (void). **Это ошибки объявления элемента**, а не ошибки reflection или `ClassCastException`.

Значения элементов подчиняются [JLS §9.7.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7.1): для примитивов и строк — допустимые константные выражения, для enum — именованные enum constants, для `Class` — class literal, для вложенной аннотации — annotation expression, для массива — соответствующие значения. `Integer.parseInt("2")` и `new String("x")` не станут «константами» только потому, что дадут известный результат: это `AN_N04/AN_N09`, compiler-negative. `@Operation` без обязательного `value` — `AN_N02`; `@Operation(value = null)` — не способ представить отсутствие: G1 `AN_N07` отклоняется компилятором.

В принятом `AnnotationsProbe` метаданные `Details` содержат `int[] numbers() default {6,7}`, `Class<?> kind() default String.class`, enum `HIGH` и строковый default. Runtime-строки **AN11–AN14**: `default` / `6:7` / `String` / `HIGH`. Это значения элементов, не доказательство запуска аннотации.

## 2. Где аннотация разрешена: @Target и TYPE_USE (AN-04, AN-16; AN-20 P1)

~~~java
import java.lang.annotation.*;
import java.lang.reflect.Field;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@interface NonEmptyText {}

class Order {
    @NonEmptyText String label;
}

class AnnotationLookup {
    static boolean typeAnnotated() throws NoSuchFieldException {
        Field f = Order.class.getDeclaredField("label");
        return f.getAnnotatedType().isAnnotationPresent(NonEmptyText.class);
    }
}
~~~

`@Target` задаёт контексты применения. `ElementType.TYPE` — декларация класса/интерфейса/enum/annotation, `FIELD` — декларация поля, `METHOD` — метод, `PARAMETER` — параметр, `TYPE_USE` — использование типа. Эти понятия **не совпадают**. В данном случае `@NonEmptyText` применяется к *типу поля* `String`, а не обязательно к декларации `Field`. Для аннотации, предназначенной только для `TYPE_USE`, `f.getAnnotatedType().isAnnotationPresent(NonEmptyText.class)` читает нужные метаданные, а `f.getAnnotation(NonEmptyText.class)` может вернуть `null`. Принятый G1 демонстрирует именно это: **AN17=true, AN18=true** (вторая строка — результат сравнения с null).

**Ловушка:** выражение `@Target(ElementType.TYPE_USE) @interface A {} @A class SomeClass {}` в G1 **успешно компилировалось на Java 21**. Нельзя объявлять его заведомо invalid: правила применения в декларационных контекстах тоньше упрощённого мнемонического «только перед названием используемого типа». Точная норма — [JLS §9.7.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7.4); ошибка `AN_N01`, напротив, реальна для `@Target(METHOD)` на декларации класса.

Если `@Target` отсутствует, аннотацию можно применять в тех declaration contexts, которые разрешает JLS по умолчанию; это **не** равнозначно автоматическому разрешению всех возможных `TYPE_USE` (AN-18 P1). Generic type arguments, массивные измерения, local variables, records и type parameters — дополнительные P1 сценарии; единственный `Field.getAnnotatedType()` не проверяет их все. [AnnotatedType API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AnnotatedType.html).

## 3. SOURCE, CLASS, RUNTIME: хранение ≠ исполнение (AN-05, AN-06, AN-15; AN-18 P1)

`@Retention` задаёт минимальную гарантию жизненного цикла метаданных:

| Режим | Смысл для Java 21 | Runtime reflection |
|---|---|---|
| `RetentionPolicy.SOURCE` | Аннотация обрабатывается как информация исходника, в обычный class-файл не записывается | Не видна |
| `RetentionPolicy.CLASS` | Записывается в class-файл, но не требуется доступность через runtime reflection | Не видна стандартным `getAnnotation` |
| `RetentionPolicy.RUNTIME` | Хранится и доступна соответствующим runtime API | Может быть видна там, где тип/target и API совпали |

~~~java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Live {}
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@interface BinaryOnly {}
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@interface SourceOnly {}

@Live @BinaryOnly @SourceOnly
class AnnotatedService {}
~~~

Отсюда для `AnnotatedService.class.getAnnotation(Live.class)` ожидается ненулевой объект аннотации, но для `BinaryOnly.class`/`SourceOnly.class` в соответствующем lookup — отсутствие. G1 реально зафиксировал **AN01=parent** для RUNTIME `Family`, **AN09=true** для `SourceMark==null` и **AN10=true** для `ClassMark==null`; оба последних — *значения boolean сравнения*, а не строки ошибок компиляции.

Если `@Retention` не указан, по [JLS §9.6.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.2) применяется **CLASS** (AN-18 P1). Метаданные с `CLASS` могут использовать инструменты обработки байткода — но **нет проведённого `javap -v` опыта**, подтверждающего конкретный class-file layout этой статьи.

**Отдельно:** даже `@Retention(RUNTIME)` не запускает annotation processor и не выполняет бизнес-логику. Annotation processing (JSR 269) — отдельный процессуальный API компилятора; сами мета-аннотации не являются event listeners и не сканируют classpath. В G1 **не запускался** реальный JSR 269 processor (AN-22 P1, AN-23 P2). Условное «фреймворк автоматически найдёт @Live» зависит от явно реализованного кода фреймворка.

## 4. Инспекция через AnnotatedElement: direct, declared, inherited (AN-06, AN-07, AN-08)

Простейший рабочий маршрут — узнать `Class` и запросить объявленную на нём метку:

~~~java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@interface Family {
    String value();
}

@Family("parent")
class Parent {}
class Child extends Parent {}

@Family("interface")
interface MarkerInterface {}
class ImplementsMarker implements MarkerInterface {}
~~~

Для `Child.class.getAnnotation(Family.class)` результатом станет аннотация `parent`, **поскольку** `Family` отмечена `@Inherited` и лежит на *суперклассе*. Но `Child.class.getDeclaredAnnotation(Family.class)` даст `null`: у самого Child метки нет. G1 **AN02=parent, AN03=true**. `ImplementsMarker.class.getAnnotation(Family.class)` даёт `null`: `@Inherited` не превращает метку интерфейса в аннотацию реализующего класса (AN04=true).

`@Inherited` работает для **аннотаций классов в цепочке superclass**, а не «аннотаций любых методов, полей, implemented interface». Если аннотация **не** имеет `@Inherited`, даже при фактическом наследовании метода нельзя из этого выводить наследование самой метки. G1 child класса без `@Inherited` на `Tag` — **AN05=true**, т.е. `getAnnotation(Tag.class)==null`.

Различие [AnnotatedElement.getAnnotation/getDeclaredAnnotation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AnnotatedElement.html) — источник практической ошибки: `getAnnotation` может увидеть унаследованную присутствующую аннотацию, `getDeclaredAnnotation` ищет непосредственно прикреплённую. При этом `isAnnotationPresent(A.class)` опирается на семантику присутствия, а не на «уже был вызван scanner». G1 **AN20=true** для наличия `Details` на Parent. Для отдельных методов и полей логика inheritance зависит от контракта соответствующего AnnotatedElement; нельзя механически переносить class-inheritance на Method.

## 5. Повторяемые аннотации и содержащий тип (AN-09, AN-10; AN-21 P1)

Повторение `@Tag("alpha") @Tag("beta")` требует **контейнерной** аннотации, способной содержать массив нужного типа:

~~~java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Tags.class)
@interface Tag { String value(); }

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Tags { Tag[] value(); }

@Tag("alpha")
@Tag("beta")
class Task {}
~~~

Компилятор может представлять несколько непосредственно повторённых `Tag` через содержащую `Tags`. Поэтому запросы **различаются**:

| Java reflection запрос | Результат для Task в этом примере |
|---|---|
| `Task.class.getDeclaredAnnotation(Tag.class)` | `null` — напрямую объявленного одиночного Tag нет |
| `Task.class.getDeclaredAnnotationsByType(Tag.class)` | Массив из **2** элементов |
| `Task.class.getAnnotation(Tags.class)` | Ненулевой container с `value().length==2` |
| `Task.class.getAnnotationsByType(Tag.class)` | Массив из **2** элементов, учитывая правила presence/inheritance |

В G1 **AN06=2 / AN07=2 / AN08=true / AN22=alpha** — отсюда и контрпример к ложному утверждению «`getDeclaredAnnotation(Tag)==null` означает, что тега нет». Дополнительные distinctions `getAnnotationsByType` и `getDeclaredAnnotationsByType` раскрыты в [Javadoc AnnotatedElement](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AnnotatedElement.html). Поведение при одновременном `@Repeatable` и `@Inherited` требует согласованной политики для *самой повторяемой* аннотации и container; не обещайте, что примеры без `@Inherited` автоматически работают одинаково для всех subclass chains (AN-21 P1).

Ошибки повторения могут быть **статическими**: неповторяемую метку нельзя просто повторить (G1 `AN_N05`), неправильный контейнер с `String[] value()` вместо `Tag[]` — `AN_N06`; оба дают тематические diagnostics, а не `getAnnotation()==null`.

## 6. Стандартные аннотации: проверяемый контракт, не runtime hooks (AN-11, AN-12, AN-13; AN-19 P1)

| Аннотация | Фактический эффект / предел |
|---|---|
| `@Override` | Компилятор проверяет, что метод действительно переопределяет/реализует подходящий метод; неверное применение → ошибка javac |
| `@FunctionalInterface` | Компилятор проверяет функциональность интерфейса (контракт SAM), но лямбда возможна и без этой метки |
| `@Deprecated(since="21", forRemoval=false)` | Сообщает о нежелательности использования API; javac/javadoc и конкретные инструменты могут использовать метаданные |
| `@SuppressWarnings("deprecation")` | Предлагает подавить поддерживаемые компилятором предупреждения в пределах допустимой области, но не исправляет программу и не запрещает исключения |
| `@Documented` | Директива инструментам документации включать аннотацию в generated docs, **если** документация генерируется и аннотация применима |
| `@SafeVarargs` | Обещание автора о безопасном использовании varargs из непереопределяемого контракта; специальная компиляторная проверка по JLS, не blanket safety |
 
~~~java
@FunctionalInterface
interface Op { int apply(int value); }

class ParentAction {
    String name() { return "base"; }
}
class ChildAction extends ParentAction {
    @Override
    String name() { return "child"; }
}

@Deprecated(since = "21", forRemoval = false)
class LegacyAction {}

class ApiExample {
    @SuppressWarnings("deprecation")
    static Object legacy() { return new LegacyAction(); }
}
~~~

G1 **AN19=68** после `value -> value + 1` с аргументом 67. При этом `@FunctionalInterface` не трансформирует выполнение лямбды; невалидный двухабстрактный интерфейс с меткой отклоняется компилятором `AN_N11`. G1 `AN16=true` — `getDeclaredAnnotation(Override.class)==null` для метода; это **не** означает, что `@Override` игнорируется компилятором: его назначение статическое (и retention SOURCE). Неверное `@Override` — `AN_N10` с диагностикой `method.does.not.override.superclass`.

**Исправление замечания G1-N2 (AN-13):** исследовательский Java-source действительно показывал `@Documented` и `@Override`, но **не выполнял** `javadoc` и не содержал самостоятельного trace `@Deprecated/@SuppressWarnings`. Поэтому примеры выше — **нормативные и поясняющие**, а не выдуманный результат запуска инструментов. Параметры `@Deprecated.since/forRemoval` и `@SuppressWarnings` имеют Java 21 нормативный смысл; реальное поведение ключей suppression зависит от javac и флагов предупреждений. **NOT RUN здесь:** отдельный `javadoc` / `javac -Xlint:deprecation` / `javap -v` опыт; если они будут запущены позднее, укажите точные команду/вывод/HEAD, не переносите G1 stdout.

Аннотация `@Documented` сама относится к объявлению annotation interface: это не флаг `@Retention(RUNTIME)` и не JSR269 processor. Чтобы показать фактический HTML output, нужно отдельное реальное выполнение javadoc с входными исходниками — учебное отличие принципиально.

## 7. Compiler-negative ≠ runtime absence (AN-17)

В принятом G1 рецензент 04 воспроизвёл **19/19 изолированных javac-negative** для суммарной пары тем. Из них на аннотации приходятся `AN_N01–N15, AN_N19–N20`, а ещё два — на необработанные checked exception reflection. Это **исторический local G1/04**, не запуск каждого snippet из этой статьи в штатном GitHub CI.

| Контрпример | Что именно отклоняет компилятор |
|---|---|
| `@Target(METHOD)` применён к классу | Target not applicable — `AN_N01` |
| Обязательный элемент пропущен | Missing default/value — `AN_N02` |
| `String[][] value()` / `Object value()` / `void value()` | Invalid element type — `AN_N03/12/13` |
| `Integer.parseInt("2")` / `new String("x")` как annotation value | Not allowable annotation constant — `AN_N04/09` |
| Повторение неповторяемой аннотации / неверный container | `AN_N05/06` |
| `null` / несоответствующий numeric literal / duplicate named values | `AN_N07/08/14/15` |
| Некорректные `@Override` / `@FunctionalInterface` | `AN_N10/11` |
| `@Retention` или `@Inherited` использованы на обычном классе | `AN_N19/20` |

Но `@Retention(CLASS)` на **самой аннотации** и `getAnnotation()==null` во время исполнения — полностью корректны и не являются javac-negative. Аналогично `@Target(TYPE_USE)` на классе в исследованном случае — **compile-positive**, см. §2. Категории диагностик привязаны к G1 изолированным исходникам, а не к необязательному отдельному negative runner.

## 8. Воспроизводимая трасса принятого G1 (23 stdout)

Полный [опубликованный AnnotationsProbe.java](../../examples/core/annotations-reflection/src/AnnotationsProbe.java) воспроизводит [G1 §6.1](../../docs/plans/CORE-ANNOTATIONS-REFLECTION.md), а [точный stdout fixture (23 строки)](../../examples/core/annotations-reflection/annotations.expected.stdout) закрепляет ожидаемые результаты. Исторически программу отдельно исполняла 04 на OpenJDK 21.0.12.1 с `javac --release 21 -Xlint:all` без предупреждений. Теперь [общий Java21/JPMS runnable](../../examples/core/annotations-reflection/README.md) и [verify.sh](../../examples/core/annotations-reflection/verify.sh) действительно опубликованы: AN probe занимает **один из четырёх самостоятельных JVM-процессов**, а 23 строки — не 23 JUnit-теста.

| Наблюдения | Что доказывают в границах примера |
|---|---|
| AN01–AN05 | Видимость `Family` на классе/child и отсутствие наследования через interface/non-Inherited Tag |
| AN06–AN08, AN22 | Repeatable container и различие `ByType` от single `getDeclaredAnnotation` |
| AN09/AN10 | SOURCE/CLASS метки не видны через runtime Class.getAnnotation |
| AN11–AN14 | Default String/int[]/Class/enum annotation elements |
| AN15/AN16 | Declaration annotation на методе vs SOURCE-`@Override` runtime null |
| AN17/AN18 | Type-use annotated Field type vs declaration annotation Field |
| AN19/AN20/AN23 | SAM lambda, isAnnotationPresent, annotationType identity |
| AN21 | Отсутствие declared Tag на child |

Исторические исследования JPMS и reflection представлены в отдельной [CORE-REFLECTION](reflection.md); **не** делайте вывод, что всякий reflection lookup автоматически читает CLASS retention.

## 9. Матрица адресного покрытия 25/25

| ID | P | Раздел и проверяемый механизм |
|---|---|---|
| AN-01 | P0 | §1: `@interface` как annotation interface и элементы без аргументов |
| AN-02 | P0 | §1: разрешённые annotation element types и `AN_N03/12/13` |
| AN-03 | P0 | §1: required/default, compile constants и отрицательные примеры |
| AN-04 | P0 | §2: `@Target` vs `TYPE_USE`, правильный positive/negative |
| AN-05 | P0 | §3: SOURCE / CLASS / RUNTIME по JLS и AN09/10 |
| AN-06 | P0 | §3–4: getAnnotation/isAnnotationPresent runtime annotation |
| AN-07 | P0 | §4: declared vs inherited lookup, AN02/03 |
| AN-08 | P0 | §4: `@Inherited` только по class-superclass, AN04/05 |
| AN-09 | P0 | §5: @Repeatable container type, AN06/07 и AN_N06 |
| AN-10 | P0 | §5: getAnnotationsByType vs getDeclaredAnnotation и container |
| AN-11 | P0 | §6: @Override static checking, AN16/AN_N10 |
| AN-12 | P0 | §6: @FunctionalInterface/SAM, AN19/AN_N11 |
| AN-13 | P0 | §6: @Deprecated/@SuppressWarnings/@Documented и честный N2 |
| AN-14 | P0 | §1: enum/Class/array-valued elements, AN12–14 |
| AN-15 | P0 | §3: RUNTIME ≠ JSR269 processing |
| AN-16 | P0 | §2: AnnotatedType vs Field annotation, AN17/18 |
| AN-17 | P0 | §7: compiler-negative ≠ runtime absent metadata |
| AN-18 | P1 | §2–3: отсутствующие @Target/@Retention и CLASS default |
| AN-19 | P1 | §6: javadoc/@Documented и @Deprecated/@SuppressWarnings — tools NOT RUN |
| AN-20 | P1 | §2: TYPE_USE generics/records/arrays beyond single field NOT RUN |
| AN-21 | P1 | §5: inherited repeatable/container semantics, отдельная matrix NOT RUN |
| AN-22 | P1 | §3: JSR269 processors/source retention НЕ исполнялись |
| AN-23 | P2 | §3: реальный annotation processor/сборочные генерации NOT RUN |
| AN-24 | P2 | §3: corrupted metadata/enum/error recovery NOT RUN |
| AN-25 | P2 | §3: библиотеки Spring/AOP/ORM annotation semantics NOT RUN |

## 10. Нормативные источники и честные ограничения

**A:** [JLS21 §9.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6), [§9.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7), [§9.6.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4), [AnnotatedElement API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AnnotatedElement.html), [@Repeatable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/annotation/Repeatable.html), [@Inherited](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/annotation/Inherited.html), [@Deprecated](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Deprecated.html), [@SuppressWarnings](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/SuppressWarnings.html), [@Documented](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/annotation/Documented.html). **C:** реальные формулировки вопросов [Q-HRUDAY](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md), [Q-TEAMLEAD](https://github.com/teamlead/java-interview-questions/blob/main/README.md), [Q-CHERNYSHOV-RU](https://github.com/Shell26/Java-Developer/blob/master/core.md) мотивируют обучение, **не** гарантии JLS и не частотную статистику.

**Исторически выполнено G1/04:** один `AnnotationsProbe` с **23/23 stdout**, один `ReflectionProbe` с **34/34 stdout**, два отдельных настоящих JPMS module-path запуска по **3/3** (всего **4 JVM / 63 подписанные stdout строки ≠ 63 JUnit-теста**), отдельно **19/19** тематических compiler-negative по `compiler.err.*` **вне обычного CI**. Это исходная исследовательская база, не означает запуск всех иллюстраций этого Markdown автоматически.

**После технической интеграции:** [полный runner](../../examples/core/annotations-reflection/README.md) опубликован с **10 Java21 source** (две classpath-программы и восемь файлов двух настоящих JPMS named-module вариантов), [четырьмя stdout-эталонами](../../examples/core/annotations-reflection/README.md) и [verify.sh](../../examples/core/annotations-reflection/verify.sh). [Технический CI #45020360437](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45020360437) завершился **SUCCESS** на *своём* exact technical HEAD `5c1bb8cbf2fc46d7c9229df9989ce73ca1f4a048`. **[Итоговый CI #45021178223](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45021178223) завершился SUCCESS** на последующем author HEAD `b46335704bfce17b198f64aec68a9ff701bae3ef`: Gradle/JUnit, **3 компиляции** `javac --release 21 -Xlint:all` (classpath + 2 module-path варианта), **4 JVM и 4 точных `diff -u`**. Этот успешный запуск относится **только к указанному SHA**; после редакторских изменений требуется новый CI, результат которого фиксируется в [PR #172](https://github.com/Melchiorius/backend-interview-labs/pull/172). **NOT RUN в этих positive CI:** 19 isolated G1 compiler-negative, компиляция *всех* inline Java snippets статьи, JSR269 processor, `javap -v` class-file investigation, `javadoc` HTML generation, отдельный `javac -Xlint:deprecation` trace, иные JDK/vendor/OS, альтернативные JPMS graphs, classloader/agents, Spring/AOP/ORM, JMH/JIT/GC, GUI/HTTP/Obsidian anchors и P2. Ни один из этих пунктов не объявляется выполненным из-за публикации runner.
