# CORE-GENERICS + CORE-ERASURE — исследовательский план Java SE 21 (G1)

**Статус:** G1 ACCEPTED координатором 00 в [Issue #80](https://github.com/Melchiorius/backend-interview-labs/issues/80#issuecomment-7051633981) после [независимого G1 PASS 04](https://github.com/Melchiorius/backend-interview-labs/pull/81#pullrequestreview-5999669867); [PR #81](https://github.com/Melchiorius/backend-interview-labs/pull/81) опубликован squash `101ef992c218452c4da9b41d6e5b9de01e309af0`. **Основание:** Issue #80, родитель #33; исторический BASE `a9cf928e5d95d511bbca0468e07827886a0b2832`, проверенный G1 HEAD `6799d700ab20a2511344bf9911f7a2d98007ba5b`. **Версия:** JLS/API Java SE 21; пробы исследователя — OpenJDK/javac 21.0.12.1, Debian. Это **принятый G1-план, не Implemented/Verified учебные статьи**; G2 — очередь Issues #82/#83.

## 1. Обещание читателю и решение о двух статьях

**Главный вопрос:** почему `List<Integer>` нельзя передать как `List<Number>`; что означают `? extends`, `? super` и PECS на реальном API; почему unsafe generics-код иногда *компилируется с предупреждением*, но приводит к `ClassCastException`; как erasure влияет на runtime, overload и overriding?

**Рекомендация 01: две отдельные связанные статьи, один согласованный план, без механического дублирования.**
- **CORE-GENERICS — статическая типизация:** параметры типа класса/метода, границы, инвариантность, wildcards/PECS, producer/consumer, capture helper, generic methods/inference/diamond и контраст с ковариантными массивами. Для читателя гарантия: объяснить, что компилятор *может* разрешить и запретить, и предсказать результат основных сценариев.
- **CORE-ERASURE — граница compile-time/runtime:** правила JLS §4.6/§4.7, reifiable/non-reifiable, raw/unchecked, heap pollution, `@SafeVarargs`, ограничения generic arrays и `instanceof` через checked cast compatibility (Java 16+), `new T()`, same-erasure name clash, override/bridge. Для читателя гарантия: показать место возникновения runtime cast и почему ошибка может появиться **после** успешного javac.
- **Общий мост:** `List<? extends Number>` читается как `Number`, но не принимает произвольный `Integer`; `List<? super Integer>` принимает `Integer`, но без дополнительных знаний возвращает `Object`; `Collections.copy(List<? super T>, List<? extends T>)` связывает направления. Здесь речь не про списки как реализацию структур данных и не про полное устройство JVM.
- **Форма примеров после G1:** план §4 содержит один самодостаточный positive `GenericsProbes.java` с 20 наблюдениями, 22 независимых отрицательных компиляции и 4 самостоятельных lint-сценария. Роль 03 должна обосновать один runnable example либо короткие inline-кейсы; **не** создавать два Gradle-проекта по количеству статей, не включать negative snippets в success source set.
- **Фразам-заблуждениям — явные контрпримеры:** «все generics существуют только во время компиляции» (runtime class/erased casts/метаданные методов остаются), «? extends делает коллекцию immutable» (можно читать и иногда добавлять `null`, изменяемость объекта зависит от реализации), «? super при чтении даёт T» (возвращает Object), «SafeVarargs исправляет unsafe тело» (нет), «массивы инвариантны» (ковариантны, runtime ArrayStoreException), «bridge метода никогда не бывает» (наблюдаем на javac21).

**Границы:** семантика generics — JLS/API Java 21; точный текст diagnostic и конкретное количество методов, синтезированных javac, — реализация. Нет гарантии, что любой unchecked приводит к CCE; исключение возникает только при нарушении реально проверяемого ожидания типа/приведения. Не превращать краткое упоминание erasure в руководство по classfile/invokedynamic/HotSpot, JPA или reflection API.

## 2. Нормативные источники категории A (точные Java 21)

Сверены с реестровыми ID [OFF-JLS21 и OFF-API21](../SOURCE_REGISTRY.md). Не путать настоящие правила JLS с подсказками интервью-каталогов.

- [JLS §4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.4) — type variables/bounds; multiple bounds: class or type variable в первом положении, порядок важен для стирания.
- [§4.5, §4.5.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.5.1) — parameterized types и `?` / `? extends` / `? super`; [§4.10.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.10.2) — subtyping и инвариантность отдельных parameterizations.
- [§4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.6) — erasure: `G<A>` → `G`, type variable → erasure *leftmost bound*, method signature/return types; [§4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.7) — reifiable types (в том числе `List<?>`, но не `List<String>`).
- [§4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.8) — raw types и ограниченный legacy interoperability; [§4.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.2) — heap pollution и его пути.
- [JLS §5.1.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.9) — unchecked conversion/warning; [§5.1.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.10) — capture conversion (в JLS дан пример `Collections.reverse(List<?>)` с helper). [§5.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.2) — assignment contexts/inference и conversion.
- [JLS §8.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.2), [§8.4.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.4) — generic классы и методы; [§8.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.2) — сигнатуры/name clash; [§8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3) — overriding/return compatibility и предупреждения при unsafe overriding. Фактические bridge methods в `javac` можно наблюдать через Reflection API, но **не** приписывать JLS фиксированное число classfile methods.
- [JLS §9.6.4.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.7), [Java 21 @SafeVarargs](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/SafeVarargs.html) — обещание автора о varargs безопасности и условия применения, а не проверка тела/исправление heap pollution. `-Xlint:varargs` может указать опасный код внутри аннотированного метода.
- [JLS §10.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-10.html#jls-10.5) — array store checks и `ArrayStoreException` при ковариантности; [§15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2) — type-comparison `instanceof`: левое выражение должно быть **checked cast compatible** с типом справа по [§5.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.5); прежнее общее требование reifiability для типа справа снято в Java 16. Нереifiable parameterization всё ещё может быть запрещён в конкретной паре типов, если narrowing cast был бы unchecked; [§15.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.9) — class instance creation, отсутствие `new T()`.
- [Java 21 Collections.copy](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#copy(java.util.List,java.util.List)) — норма API: `<T> void copy(List<? super T> dest, List<? extends T> src)`; размер dest должен быть достаточен, модификация — поддерживаться. Не утверждать, что `Collections.copy` растит список назначения.
- [Java 21 List](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html) — element contract; [Class](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#getDeclaredMethods()), [Method.isBridge](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#isBridge()) — наблюдение bridge через Reflection (не необходимость bytecode inspection).
- [Java 21 Collections.emptyList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#emptyList()), [ArrayList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html), [Arrays](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html) — inference/array context при использовании.

**Не выдавать популярный mnemonic «PECS» за термин JLS:** спецификация задаёт ограничения чтения/записи и capture conversion, API `Collections.copy` служит конкретной проверкой. Допустимость `add(null)` на `List<? extends Number>` — типовое правило; действительно успешная вставка зависит ещё от runtime-контракта выбранной коллекции.

## 3. Независимые вопросы категории C — не нормативные источники

Использованы существующие записи [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), без дублирования реестра:

- **Q-HRUDAY** — [Core Java Interview Question Bank, глава 11 Generics](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md#chapter-11---generics): 89 вопросов; bounds, generic methods, variance/PECS, raw/unchecked, heap pollution, erasure. Наличие проверено в файле GitHub, ответы не считать проверенной нормой.
- **Q-IN28** — [Java Interview Questions, Q178–184](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/readme.md): введение, generic class/method, ограничения, bounds; терминология «generics for superclass» требует разделения *bounded type variable* и wildcard super.
- **Q-CHERNYSHOV** — [русский перечень §20 «Records и Дженерики»](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/ru/README.md): вопросы №11–25 про erasure, PECS, arrays, raw, bridge, inheritance. По фактическому списку это отдельный независимый набор вопросов; полнота ответов не подтверждена.
- **Q-TEAMLEAD** — [java-interview-questions](https://github.com/teamlead/java-interview-questions): вопросы «How Java understands Generics», «PECS principle», static generic method, generics history. Частотная категоризация автора не верифицирована, не определяет приоритет P0.
- **Q-BAELDUNG-CORE** — существует в реестре; полный ретроспективный тематический аудит текста после G2 отложен, не заявлять проверку каждого его ответа.

Четыре независимо доступных C-источника выявляют вопросы; никакого количественного рейтинга рынка интервью из их пересечений не выводить. Нормы и возможные ошибки в ответах сверяются по A.

## 4. Матрица G1, P0/P1/P2: критерии покрытия до статей

`P01–P20` — 20 строк **одной** успешно скомпилированной программы §5; `N01–N22` — 22 **отдельных** compile-negative файла §6; `W01–W04` — отдельные warning-проверки §7. P0 — обязательный для обещания читателю механизм, не «уровень Junior/Middle/Senior».

| ID | Приоритет, интервью-вопрос → ожидаемый корректный ответ | Норма и доказательство | Ответная статья |
|---|---|---|---|
| GE-01 | **P0**. Что такое `Box<T>` и generic method? Типы проверяются при компиляции, `Box<Integer>.get()` даёт Integer без явного cast у клиента. | §8.1.2, §8.4.4; P01 | GENERICS |
| GE-02 | **P0**. Что даёт `T extends Number & Comparable<T>`? Доступ к двум контрактам, class bound первым; erasure использует leftmost bound. | §4.4, §4.6; P02/P10, N12 | GENERICS + ERASURE |
| GE-03 | **P0**. Почему `List<Integer>` не подтип `List<Number>`? Инвариантность предотвращает запись несовместимого значения через alias; тип-аргументы не ковариантны. | §4.10.2; N01 и P15 | GENERICS |
| GE-04 | **P0**. Разница `List<Object>`, `List<?>`, `List<? extends Object>`? Первое не принимает List<String> присваиванием; wildcard допускает список неизвестного элементного типа, не давая произвольно записать значение. | §4.5.1, §4.10.2; P15/P20, N21/N22 | GENERICS |
| GE-05 | **P0**. Что разрешено для `? extends Number`? Прочитать Number, нельзя добавить Integer; `null` допускается статически, но поведение конкретного списка отдельно. | §4.5.1, §5.1.10; P05, N02 | GENERICS |
| GE-06 | **P0**. Что разрешено для `? super Integer`? Добавить Integer, но прочитать без дополнительных сведений можно лишь Object; не путать с bound на T. | §4.5.1; P06, N03 | GENERICS |
| GE-07 | **P0**. Где PECS реально нужен? `Collections.copy(dest,src)`: producer src extends, consumer dest super; dest должен существовать и быть достаточно длинным. | Collections.copy API; P03/P04/P19 | GENERICS |
| GE-08 | **P0**. Что такое wildcard capture? `List<?>` иногда передаётся generic helper, который удерживает один captured T между get/set; не значит разрешить добавление чужого типа. | §5.1.10; P07/N16 | GENERICS |
| GE-09 | **P0**. Почему массивы ковариантны и где происходит `ArrayStoreException`, в отличие от инвариантных generics? | §10.5; P14/N20 | GENERICS, граница CORE-ARRAYS |
| GE-10 | **P0**. Что стирается и что остаётся? `List<String>`/`List<Integer>` могут иметь одну runtime implementation class; bound T стирается к Number, сигнатура generic method — к Object. | §4.6; P08/P10/P11 | ERASURE |
| GE-11 | **P0**. Почему `instanceof List<?>` и даже `typed instanceof ArrayList<String>` допустимы при статическом типе `List<String>`, а `Object o; o instanceof List<String>` — нет? Решает checked cast compatibility (§5.5), а не один лишь факт reifiability. Runtime проверяет erased class (`ArrayList`), не восстанавливает и не проверяет произвольный type argument `String`. | §4.7, §5.5, §15.20.2; P09/P16/P21/N09/N10 | ERASURE |
| GE-12 | **P0**. Почему не `new T()`, `T.class`, `new T[2]` или `new List<String>[2]`? При runtime отсутствует требуемая reified компонентная информация/конструкторная сущность. | §4.6/4.7, §15.9/§10; N05–N08 | ERASURE |
| GE-13 | **P0**. Что делают raw types, unchecked warnings и cast? Небезопасный alias может поместить Integer в List<String>; компилятор предупреждает, а ClassCastException возникает при извлечении как String, не обязательно при вставке. | §4.8, §5.1.9, §4.12.2; P12, W01/W04 | ERASURE |
| GE-14 | **P0**. Что такое heap pollution и почему `@SafeVarargs` не гарантирует безопасность? Можно подменить varargs-array через Object[], привести к CCE; `-Xlint:varargs` замечает опасный код и с аннотацией. | §4.12.2, §9.6.4.7; P13, W02/W03 | ERASURE |
| GE-15 | **P0**. Почему два overload метода, отличающиеся только `List<String>` и `List<Integer>`, не компилируются? Одинаковые erasure signatures. | §4.6/§8.4.2; N11 | ERASURE |
| GE-16 | **P0**. Почему generic overriding работает после erasure? Compiler может сгенерировать bridge для сохранения полиморфизма; `isBridge()` — наблюдение конкретного javac21. | §8.4.8.3, Method.isBridge; P17/N19 | ERASURE |
| GE-17 | **P0**. Generic method inference/diamond/boxing: зачем `Collections.emptyList()` и `new ArrayList<>()` могут вывести Integer и почему `List<int>` запрещён? | §5.2, §15.12, §4.5; P18, N04 | GENERICS |
| GE-18 | **P1**. Можно ли создать `List<?>[]`, если нельзя `List<String>[]`? Первое имеет reifiable component type; это не позволяет без unchecked получить массив List<String>. | §4.7, §10; P16, N08/N15 | ERASURE |
| GE-19 | **P1**. Почему `extends` не гарантирует immutability, `super` не значит «любой supertype при чтении» и `add(null)` особый случай? | §4.5.1, List API; P05/P06/N02 | GENERICS |
| GE-20 | **P1**. Может ли generic class наследовать Throwable? Запрещено; нельзя трактовать type parameter как runtime exception class. | §8.1.2, §11; N18 | ERASURE |
| GE-21 | **P1**. Почему static member generic class не может объявлять поле с type variable T? T относится к параметризации экземпляра, а static field — к классу. | §8.1.2; N13 | GENERICS |
| GE-22 | **P1**. Где reflection может увидеть метаданные generic declarations, хотя runtime object не становится отдельным List<String>? Не смешивать `getClass()` с `getGenericReturnType()`. | §4.6, Method API; P10/P11 и будущий REFLECTION | ERASURE |
| GE-23 | **P1**. Чем отличается unchecked cast/unchecked call/varargs warning от compile-time error? Строгий `-Xlint:unchecked -Xlint:varargs` и категории, warning сам по себе не делает программу безопасной. | §4.8, §5.1.9; W01–W04/N11 | ERASURE |
| GE-24 | **P1**. Когда @SafeVarargs допустим и что проверяет компилятор? static/final/constructor/private instance по Java 21; аннотация подавляет часть предупреждений, не доказывая safety. | §9.6.4.7, API @SafeVarargs; W03 | ERASURE |
| GE-25 | **P1**. Почему generic constructors/diamond, generic varargs и overload/inference нельзя заменить лозунгом «тип выведется всегда»? Контекст, bounds и reifiability могут препятствовать применению. | §8.8.4, §15.12, §5.2; P18/N12 | GENERICS |
| GE-26 | **P2**. Recursive bounds, F-bounded polymorphism, intersection types — отдельное углубление, не обязательно для простого PECS. | §4.4, §4.9 | GENERICS, дополнительный блок |
| GE-27 | **P2**. Реальная форма bridge в bytecode и binary compatibility, descriptors/Signature attributes — проверять по JVMS и конкретному javac. | §4.6, JVMS21 | JVM-BYTECODE |
| GE-28 | **P2**. Параметризованные Repository/DAO API, type-safe caches, framework proxy — контракты конкретных библиотек, не гарантия JLS. | API отдельно после G1 | DB-JPA / SPR-DI |
| GE-29 | **P2**. Что reflection ParameterizedType видит в declarations и чего не знает об обычном runtime ArrayList? | Reflection API | CORE-REFLECTION |
| GE-30 | **P2**. Deep inference с intersection types, lambdas, overloaded generic methods и wildcards. | §15.12/§18 | CORE-FUNCTIONS / CORE-GENERICS advanced |

## 5. Positive Java 21: полный исходник и фактический stdout

**Один** `GenericsProbes.java` компилировался и запускался в отдельном временном каталоге:
```sh
mkdir -p out
javac --release 21 -d out positive/GenericsProbes.java
java -cp out GenericsProbes
# Отдельный lint запуск именно этого source:
javac --release 21 -Xlint:unchecked -Xlint:varargs -d out positive/GenericsProbes.java
```

**Полный листинг** (положительный в смысле компиляции; P12/P13/P14 специально ловят runtime exceptions; внутри остаются два ожидаемых lint warning):

```java
import java.lang.reflect.*;
import java.util.*;

public class GenericsProbes {
    static final class Box<T> {
        private final T value;
        Box(T value) { this.value = value; }
        T get() { return value; }
    }
    static final class NumberBox<T extends Number> {
        private final T value;
        NumberBox(T value) { this.value = value; }
        T get() { return value; }
    }
    static <T extends Number & Comparable<T>> int compare(T a,T b) { return a.compareTo(b); }
    static <T> void copyOne(List<? super T> target, List<? extends T> source) {
        target.set(0,source.get(0));
    }
    static void captureFirst(List<?> items) { captureHelper(items); }
    private static <T> void captureHelper(List<T> items) { items.set(1,items.get(0)); }
    static <T> T identity(T value) { return value; }
    static class Parent<T> { T get() { return null; } }
    static final class Child extends Parent<String> {
        @Override String get() { return "child"; }
    }
    @SafeVarargs static String unsafeFirst(List<String>... lists) {
        Object[] alias = lists;
        alias[0] = List.of(67);
        return lists[0].get(0);
    }
    public static void main(String[] args) throws Exception {
        Box<Integer> box = new Box<>(67);
        System.out.println("P01=" + box.get());
        System.out.println("P02=" + compare(67,68));
        List<Integer> src = List.of(67,68);
        List<Number> dst = new ArrayList<>(List.of(0,0));
        Collections.copy(dst,src);
        System.out.println("P03=" + dst);
        copyOne(dst,src);
        System.out.println("P04=" + dst.get(0));
        List<? extends Number> producer = new ArrayList<Integer>(List.of(67));
        Number num = producer.get(0);
        producer.add(null);
        System.out.println("P05=" + num + "," + producer.size());
        List<? super Integer> consumer = new ArrayList<Number>();
        consumer.add(67);
        Object readBack = consumer.get(0);
        System.out.println("P06=" + readBack + "," + readBack.getClass().getSimpleName());
        List<String> items = new ArrayList<>(List.of("a","b"));
        captureFirst(items);
        System.out.println("P07=" + items);
        System.out.println("P08=" + (new ArrayList<String>().getClass()==new ArrayList<Integer>().getClass()));
        Object generic = new ArrayList<String>();
        System.out.println("P09=" + (generic instanceof List<?>));
        System.out.println("P10=" + NumberBox.class.getDeclaredMethod("get").getReturnType().getSimpleName());
        System.out.println("P11=" + GenericsProbes.class.getDeclaredMethod("identity",Object.class).getReturnType().getSimpleName());
        try {
            List<String> typed = new ArrayList<>();
            List raw = typed;
            raw.add(67);
            String mustBeString = typed.get(0);
            System.out.println("P12=" + mustBeString);
        } catch (ClassCastException ex) { System.out.println("P12=ClassCastException"); }
        try { unsafeFirst(List.of("a")); System.out.println("P13=no-error"); }
        catch (ClassCastException ex) { System.out.println("P13=ClassCastException"); }
        try {
            Number[] values = new Integer[1];
            values[0]=3.14;
            System.out.println("P14=no-error");
        } catch (ArrayStoreException ex) { System.out.println("P14=ArrayStoreException"); }
        List<String> words=List.of("x");
        List<?> anything=words; List<? extends Object> upper=words;
        System.out.println("P15=" + anything.get(0) + "," + upper.get(0));
        List<?>[] wildcardArray = new List<?>[1];
        wildcardArray[0] = List.of(67);
        System.out.println("P16=" + wildcardArray[0].get(0));
        System.out.println("P17=" + Child.class.getDeclaredMethods().length + "," +
           Arrays.stream(Child.class.getDeclaredMethods()).filter(Method::isBridge).count() + "," + new Child().get());
        List<Integer> empty = Collections.emptyList();
        List<Integer> made = new ArrayList<>(); made.add(67);
        System.out.println("P18=" + empty.size() + "," + made.get(0));
        List<Number> destination = new ArrayList<>(List.of(0,0));
        Collections.copy(destination,List.of(67,68));
        System.out.println("P19=" + destination.get(1));
        List<Integer> ints = List.of(67);
        List<?> general = ints;
        System.out.println("P20=" + (general instanceof List<?>) + "," + general.get(0));
    }
}
```

**Фактический stdout OpenJDK 21.0.12.1 — 20 из 20 строк, именно этот результат проверен:**
```text
P01=67
P02=-1
P03=[67, 68]
P04=67
P05=67,2
P06=67,Integer
P07=[a, a]
P08=true
P09=true
P10=Number
P11=Object
P12=ClassCastException
P13=ClassCastException
P14=ArrayStoreException
P15=x,x
P16=67
P17=2,1,child
P18=0,67
P19=68
P20=true,67
```

**P21 — отдельная положительная минипроба Java 21 для GE-11 (не входит в исходные P01–P20):**

```java
import java.util.ArrayList;
import java.util.List;

class P21_InstanceofParameterized {
    public static void main(String[] args) {
        List<String> typed = new ArrayList<>();
        System.out.println(typed instanceof ArrayList<String>);
    }
}
```

Команда: `javac --release 21 -d out P21_InstanceofParameterized.java && java -cp out P21_InstanceofParameterized`. **Фактический stdout OpenJDK 21.0.12.1:**

```text
true
```

Пара к N09: `typed` имеет статический тип `List<String>`, поэтому сужение до `ArrayList<String>` checked cast compatible и компилируется; `Object` → `List<String>` потребовал бы unchecked narrowing cast, поэтому type-comparison `instanceof` не проходит compile-time проверку. В runtime положительная проверка видит класс `ArrayList` после erasure; она не проверяет фактический параметр элемента `String`.

**Смысл измерения:** 1 successful compilation + 1 Java launch + 20 observations в основном `GenericsProbes.java`, а **не** 20 самостоятельных тестовых классов. Дополнительный P21 — отдельная успешная компиляция и запуск, отдельно от исходных 20 наблюдений. `P17=2,1,child` отражает конкретный `javac` 21 и Reflection; количество/расположение bridge не обещано неизменным для всех компиляторов. `P05` вставляет `null` в изменяемый `ArrayList`, а не в immutable `List.of`. `P12/13/14` — контролируемые runtime failures, перехваченные в коде.

**При `-Xlint:unchecked -Xlint:varargs` у этого positive source — 2 warnings:** `[varargs]` из опасного присвоения `Object[] alias = lists` в методе с `@SafeVarargs` и `[unchecked]` из вызова `raw.add(67)`. Строки/путь в диагностике зависят от файла и javac; здесь утверждаются категории и фактическое число для выбранного JDK, не универсальные тексты JLS.

## 6. Negative: 22 отдельные исходные программы, каждая javac FAIL

В отдельном каталоге сохранить строки таблицы как `negative/<ID>.java`. Запускать **по одной**: `javac --release 21 -d out negative/<ID>.java`. Имена top-level классов здесь уникальные; N19 содержит дополнительный класс `GParent`, поэтому нельзя механически объединять с другими примерами. В 22 самостоятельных вызовах `javac` получено **22/22** ожидаемых категорий отказа, exit nonzero; точные тексты/номера строк не считать JLS-гарантией.

| ID | Полный исходный текст отдельного файла | Ожидаемая категория |
|---|---|---|
| `N01_Invariance` | `import java.util.*; class N01_Invariance { List<Number> n = new ArrayList<Integer>(); }` | Несовместимые инвариантные generic-типы |
| `N02_ExtendsAdd` | `import java.util.*; class N02_ExtendsAdd { void m(List<? extends Number> list) { list.add(67); } }` | Cannot add Integer to неизвестный captured subtype |
| `N03_SuperGetInteger` | `import java.util.*; class N03_SuperGetInteger { void m(List<? super Integer> list) { Integer x = list.get(0); } }` | Возвращается unknown supertype, не Integer |
| `N04_PrimitiveArgument` | `import java.util.*; class N04_PrimitiveArgument { List<int> numbers; }` | Type argument обязан быть reference type |
| `N05_NewTypeVariable` | `class N05_NewTypeVariable<T> { T build() { return new T(); } }` | `new T()` недопустим |
| `N06_TypeVariableClassLiteral` | `class N06_TypeVariableClassLiteral<T> { Class<?> c = T.class; }` | `T.class` недопустим |
| `N07_NewTypeVariableArray` | `class N07_NewTypeVariableArray<T> { T[] make() { return new T[2]; } }` | Generic array creation |
| `N08_NewParameterizedArray` | `import java.util.*; class N08_NewParameterizedArray { List<String>[] v = new List<String>[2]; }` | Non-reifiable array creation |
| `N09_InstanceofParameterized` | `import java.util.*; class N09_InstanceofParameterized { boolean m(Object o) { return o instanceof List<String>; } }` | Compile-time error: `Object` → `List<String>` не checked cast compatible (потребовалось бы unchecked narrowing); это частный отрицательный случай, не универсальный запрет параметризованного типа справа |
| `N10_ParameterizedClassLiteral` | `import java.util.*; class N10_ParameterizedClassLiteral { Class<?> c = List<String>.class; }` | Неверный class literal для parameterized type |
| `N11_ErasureNameClash` | `import java.util.*; class N11_ErasureNameClash { void m(List<String> s) {} void m(List<Integer> n) {} }` | Two declarations have same erased signature |
| `N12_BoundOrder` | `class N12_BoundOrder<T extends Comparable<T> & Number> {}` | Class Number не может идти после interface bound |
| `N13_StaticUsesTypeParameter` | `class N13_StaticUsesTypeParameter<T> { static T value; }` | Instance type variable недоступен в static field |
| `N14_NewWildcard` | `import java.util.*; class N14_NewWildcard { Object v = new ArrayList<? extends Number>(); }` | Нельзя instantiate wildcard type arguments |
| `N15_IncompatibleGenericArray` | `import java.util.*; class N15_IncompatibleGenericArray { List<String>[] values = new List<?>[2]; }` | Нет implicit conversion массива wildcard в массив List<String> |
| `N16_WildcardAdd` | `import java.util.*; class N16_WildcardAdd { void m(List<?> items) { items.add("x"); } }` | Неизвестный captured element type |
| `N17_DoubleGenericInterface` | `class N17_DoubleGenericInterface implements Comparable<String>,Comparable<Integer> { public int compareTo(String x) { return 0; } public int compareTo(Integer x) { return 0; } }` | Generic interface с несовместимыми type arguments дважды |
| `N18_GenericThrowable` | `class N18_GenericThrowable<T> extends Exception {}` | Generic subclass Throwable запрещён |
| `N19_GenericOverrideReturn` | `class GParent<T> { T get() { return null; } } class N19_GenericOverrideReturn extends GParent<String> { Object get(){return "x";} }` | Object return не совместим с родительским String при override |
| `N20_ArrayWrongDirection` | `class N20_ArrayWrongDirection { Integer[] ints = new Number[1]; }` | Нельзя присвоить массив надтипа в переменную массива подтипа |
| `N21_UnboundedWildcardRead` | `import java.util.*; class N21_UnboundedWildcardRead { void m(List<?> xs) { String x = xs.get(0); } }` | Unknown captured type при чтении |
| `N22_UnboundedWildcardAddAll` | `import java.util.*; class N22_UnboundedWildcardAddAll { void m(List<String> dst,List<?> src){dst.addAll(src);} }` | `Collection<?>` не гарантирует String-элементы |

**N09 не означает, что `instanceof List<String>` запрещён всегда:** в нём источник имеет статический тип `Object`, поэтому checked cast compatibility отсутствует. Положительный парный случай P21 использует источник `List<String>` и цель `ArrayList<String>`.

**Не называть compile errors то, что компилируется с unchecked warning:** raw-вставка, unchecked cast, unsafe varargs в обычной конфигурации `javac`, и `@SafeVarargs`-метод с опасным телом — другой класс ошибок, специально вынесенный в §7.

## 7. Предупреждения, heap pollution и @SafeVarargs: 4 отдельные программы

**Команда для каждого файла:** `javac --release 21 -Xlint:unchecked -Xlint:varargs -d out warnings/<ID>.java`, затем `java -cp out <ID>`. Это **четыре самостоятельных успешных компиляции с предупреждениями** и четыре запуска. Ниже — полные исходники и точные категории предупреждений, проверенные на указанном javac.

**W01 — unchecked cast, 1 warning [unchecked], runtime CCE:**
```java
import java.util.*;
class W01_RawCast {
    static String get() {
        List raw = List.of(67);
        List<String> strings = (List<String>) raw;
        return strings.get(0);
    }
    public static void main(String[] args) {
        try { get(); System.out.println("W01=no-error"); }
        catch (ClassCastException ex) { System.out.println("W01=ClassCastException"); }
    }
}
```

**W02 — generic varargs, 2 warnings [unchecked] (declaration + call), runtime normal:**
```java
import java.util.*;
class W02_GenericVarargs {
    static void accept(List<String>... args) {
        System.out.println("W02=" + args.length);
    }
    public static void main(String[] args) { accept(List.of("x")); }
}
```

**W03 — @SafeVarargs скрывает часть unchecked warnings, но 1 [varargs] warning внутри тела, runtime CCE:**
```java
import java.util.*;
class W03_SafeVarargsBody {
    @SafeVarargs static String unsafe(List<String>... lists) {
        Object[] alias = lists;
        alias[0] = List.of(67);
        return lists[0].get(0);
    }
    public static void main(String[] args) {
        try { unsafe(List.of("x")); System.out.println("W03=no-error"); }
        catch (ClassCastException ex) { System.out.println("W03=ClassCastException"); }
    }
}
```

**W04 — raw insertion, 1 [unchecked] warning, runtime CCE при последующем чтении:**
```java
import java.util.*;
class W04_UncheckedInsertion {
    static void pollute(List<String> typed) {
        List raw = typed;
        raw.add(67);
    }
    public static void main(String[] args) {
        List<String> typed = new ArrayList<>();
        pollute(typed);
        try { String value = typed.get(0); System.out.println("W04=" + value); }
        catch (ClassCastException ex) { System.out.println("W04=ClassCastException"); }
    }
}
```

**Фактический журнал:**

| Case | javac exit | Diagnostic category / count | Фактический stdout |
|---|---|---|---|
| W01 | 0 | `[unchecked]`, 1 | `W01=ClassCastException` |
| W02 | 0 | `[unchecked]`, 2 | `W02=1` |
| W03 | 0 | `[varargs]`, 1 | `W03=ClassCastException` |
| W04 | 0 | `[unchecked]`, 1 | `W04=ClassCastException` |

**Строгая граница:** эти 5 отдельных compiler-warning observations не означают 5 разных ошибок языка. Отсутствие предупреждения по умолчанию, аннотация `@SafeVarargs` и успешный выход javac не являются формальной гарантией типовой безопасности программы. `ClassCastException` наблюдается лишь в указанных нарушающих контракт примерах на OpenJDK 21.

## 8. Честный учёт проверок, соседние темы и Gates

**Проверки роли 01 (реально выполнены):** OpenJDK 21.0.12.1 / javac 21.0.12.1; positive: 1 успешный javac + 1 запуск + 20 точно записанных stdout-строк; negative: 22 **раздельные** javac-компиляции с ожидаемыми ошибками; warning cases: 4 успешные lint-компиляции, **5 предупреждений** и 4 запуска; positive файл отдельно проверен с `-Xlint:unchecked -Xlint:varargs` → два warning. Все полные источники, имена классов, команды и результаты записаны в §5–7. Temporary source files не помещены в репозиторий, но их полное содержимое входит в Markdown для восстановления; ни Gradle, ни JUnit по теме не запускались локально.

**Not run:** локальные Gradle/JUnit/Mockito проекта, точный GitHub CI для PR **до его открытия**, Obsidian/IntelliJ GUI, другой javac/JVM, JPA/Spring generics behavior, reflection generic-signature metadata тест, `javap -v`/ASM, JVM bridge binary-compatibility тест, property-based type inference suite, performance/JIT/JMH. CI после создания PR отдельно проверять по точному HEAD и **не** выдавать существующие Java Core steps за запуск Markdown-проб.

**Реально опубликованные зависимости по BASE main** (ссылки из каталога `docs/plans/`):
- [CORE-TYPES](../../knowledge/01-java-core/types.md), [CORE-CONVERSIONS](../../knowledge/01-java-core/conversions.md) и [CORE-PARAMS](../../knowledge/01-java-core/parameters.md) — reference/primitive, boxing, assignment; не повторять весь вводный курс.
- [CORE-CLASSES](../../knowledge/01-java-core/classes.md), [CORE-INHERITANCE](../../knowledge/01-java-core/inheritance.md) — class, bounds, overriding и dispatch; глубокие правила overriding здесь ограничить erasure/bridge.
- [CORE-OBJECT](../../knowledge/01-java-core/object.md), [CORE-EQUALITY](../../knowledge/01-java-core/equality.md) — типы объектов/контракты, не подменять generics-факты hash/equals.
- [CORE-INIT](../../knowledge/01-java-core/initialization.md) — граница static/instance initialization, не копировать unrelated lifecycle.
- [Принятый G1-план CORE-NESTED/FUNCTIONS](CORE-NESTED-FUNCTIONS.md) — **план, не опубликованные статьи**, корректная граница inference/lambdas; [план CORE-INTERFACES/COMPOSITION](CORE-INTERFACES-COMPOSITION.md) — исследования, не документация готовности.
- [CURRICULUM](../CURRICULUM.md), [COVERAGE](../COVERAGE.md), [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md) — актуальные curricular IDs/статусы, не свидетельство опубликованной темы.

**Будущие ID без фиктивных Markdown-ссылок:** CORE-GENERICS, CORE-ERASURE (две цели текущего исследования), CORE-ARRAYS, COL-CONTRACTS, CORE-REFLECTION, JVM-BYTECODE, CORE-FUNCTIONS (G1 принят, статьи ещё нет), CORE-STREAMS, DB-JPA, SPR-DI, CORE-IMMUTABLE, TEST-DESIGN. На момент G2 заново проверить фактический main; если файл опубликован, дать настоящую локальную ссылку, иначе краткое объяснение на месте/запись пробела.

**Действия по ролям:**
- **04 — независимое G1 review по точному HEAD:** проверить P0 GE-01–17, текст нормативных ссылок и локальные target paths, E/negative/warning сценarii; особое внимание GE-05/06/08 (extends/super/capture), GE-10/11/12 (erasure, checked cast compatibility for `instanceof`, reifiable arrays), GE-13/14 (unchecked/SafeVarargs), GE-15/16 (clash/bridge), GE-09 (array covariance). При блокере — конкретный GE/N/W и норма. Авторские успешные пробы и зелёный CI ≠ независимое review.
- **00 — решение по G1 *после* предметного review:** согласовать две статьи и матрицу 17 P0 / 8 P1 / 5 P2, границы между PECS и erasure; ACCEPTED либо CHANGES с ID/критерием. **До явного решения 00 роли 02/03 по этой паре не открывать.**
- **02 (после G1):** CORE-GENERICS — PECS через списки и copy, инвариантность и реальные типовые ограничения; CORE-ERASURE — unsafe/runtime/bounds/bridge с объяснением, где именно тип теряется, а где остаётся runtime check. Базовые правила нельзя считать объяснёнными одной таблицей.
- **03 (после G1):** изолированные compile-negative cases и lint cases нельзя класть в обычный runnable success source set; создать только оправданный самостоятельный пример, если демонстрации §5–7 недостаточно. Mockito/JUnit без содержательной причины не требовать.
- **05 (G3):** проверить живой русский язык, P0 ↔ статьи, локальные ссылки, недублирование существующих тем, реальную среду Obsidian только при фактическом GUI-тесте.
- **06 (G6):** синхронизация COV/CURRICULUM только после публикации/решения 00, не после создания исследовательского PR.

**Параллельная программа:** CORE-NESTED/FUNCTIONS (#78/#79) и CORE-INTERFACES/COMPOSITION (#75/#77) продолжают свои gates, текущий G1 их не задерживает. **PR плана не merge; COVERAGE не изменять; учебные статьи за 02 не писать.**
