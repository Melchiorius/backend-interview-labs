# CORE-IMMUTABLE — неизменяемость объектов Java 21: владение ссылками, защитные копии и инварианты

**Область:** Java SE 21, без preview. Эта статья — о **наблюдаемом поведении объекта и его достижимого состояния**, а не об одной языковой конструкции. Зависимости: [типы и final](types.md), [ссылки и передача аргументов](parameters.md), [обычные классы и конструкторы](classes.md), [массивы и shallow copy](arrays.md), [равенство](equality.md). Для специального контракта record читайте отдельно [CORE-RECORDS](records.md). Сравнить оба подхода на фактически запускаемом Java 21-коде помогает [общий runnable Records + Immutable](../../examples/core/records-immutability/README.md): `RecordsProbe.java` R01–R16 и `ImmutableProbe.java` I01–I10, два запуска и два независимых эталона (26 наблюдений, **не 26 тестов**).

**Исследовательский G1:** [принятая матрица CORE-RECORDS + CORE-IMMUTABLE](../../docs/plans/CORE-RECORDS-IMMUTABLE.md), **I-01–I-19 = 12 P0 / 4 P1 / 3 P2**. Нормативные источники — [JLS 21 §4.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4), [JLS §8.3.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.1.2), [List.copyOf](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#copyOf(java.util.Collection)), [Collections.unmodifiableList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#unmodifiableList(java.util.List)), [Arrays.copyOf](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html). Здесь цель — проектирование корректной внешней модели, а **не** автоматическое доказательство safe publication в JMM, глубокого клонирования любых графов или производительности.

## 1. Что значит «неизменяемый объект» и где его граница (I-01, I-02)

**Наблюдаемая неизменяемость** означает: после успешного конструирования, используя доступные клиенту операции и ссылки, нельзя изменить состояние объекта, которое он обещает считать своим значением. Важно не только, присваиваются ли его поля заново, но и могут ли **другие ссылки** изменить *достижимые через эти поля объекты*.

Рассмотрим объект как граф ссылок, не как строку ключевых слов. Пусть у пользователя есть `List<String> source`, а созданный объект хранит ссылку на тот же список. Даже если поле списка объявлено `final`, пользователь всё ещё может выполнить `source.add("B")`. Внешнее состояние изменится, а метод объекта, читающий список, увидит обновление. Поле-то осталось с прежней ссылкой.

~~~java
import java.util.ArrayList;
import java.util.List;

final class LooksImmutable {
    private final List<String> names;

    LooksImmutable(List<String> names) {
        this.names = names;               // передана и сохранена та же ссылка
    }
    List<String> names() { return names; } // ссылка выходит наружу
}

class MutableThroughAlias {
    public static void main(String[] args) {
        List<String> source = new ArrayList<>(List.of("A"));
        LooksImmutable value = new LooksImmutable(source);
        source.add("B");
        System.out.println(value.names()); // [A, B], хотя поле final
        value.names().add("C");
        System.out.println(source);        // [A, B, C]
    }
}
~~~

**Кто что меняет:** `source` у вызывающего и `names` в объекте — **две ссылки на один изменяемый ArrayList**; после `add` изменяется объект списка, а не присваивание финальному полю. Выданный accessor создаёт ещё одну копию той же ссылки. `final` не означает *deep immutability* и не предотвращает изменение элемента списка или массива.

**Граница:** сам объект может быть *immutable относительно конкретного API*, даже если его реализация содержит внутренний изменяемый кэш при сохранении наблюдаемой семантики. Это требует собственного контракта и доказательства, а не лозунга «ни одного mutable поля». В этой статье разбираются простые значения, где неизменяемость устанавливается конструктором и защитой границ владения.

## 2. Constructor invariant и правило владения на входе (I-03, I-10)

**Инвариант** — условие, которому должен удовлетворять каждый *успешно сконструированный* объект и которое сохраняют его публичные операции. Например: массив содержит хотя бы одну оценку, список пользователей не содержит `null`, число кредитов не отрицательно. Значение нужно **проверить до публикации** корректного объекта.

Но одной проверки мало: если вы сохранили внешнюю изменяемую ссылку, вызывающий может изменить ранее проверенное значение **после** конструктора. В частности, `Objects.requireNonNull(list)` удостоверяет лишь, что сама ссылка не null; оно не делает список неизменяемым и не копирует элементы.

Для входного массива нужно определить: кто владеет исходной ссылкой? В типичном API вызывающий **сохраняет право изменять свой массив**, а значит конструктор не должен его просто запоминать.

~~~java
import java.util.Objects;

final class Scores {
    private final int[] values;

    Scores(int[] input) {
        Objects.requireNonNull(input, "input");
        if (input.length == 0) {
            throw new IllegalArgumentException("empty");
        }
        this.values = input.clone(); // COPY-IN: владеем собственным внешним массивом
    }

    int first() {
        return values[0];
    }

    int[] values() {
        return values.clone(); // COPY-OUT: не отдаём внутреннее хранилище
    }
}

class ScoresExample {
    public static void main(String[] args) {
        int[] input = {67};
        Scores scores = new Scores(input);
        input[0] = 1;                  // изменяем только внешний массив
        scores.values()[0] = 2;        // изменяем только копию, полученную из accessor
        System.out.println(scores.first()); // 67

        try {
            new Scores(new int[0]);
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage()); // empty
        }
    }
}
~~~

**Шаги по ссылкам:** конструктор проверяет длину исходного `input`, создаёт `clone` и сохраняет его в приватное финальное поле. После создания у клиента остаётся `input` — другая область хранения. При чтении `scores.values()` возвращается **ещё один новый массив**, не ссылка на внутренний `values`. Поэтому обе внешние модификации не меняют результат `first()`.

**Если опустить copy-in**, изменение `input` поменяет состояние объекта после проверки. **Если опустить copy-out**, клиент сможет получить внутренний массив из accessor и изменить его в обход методов. Для полного контроля mutable array нужны **обе стороны**. Но это только модель обычного `final class`; для record с array-компонентом дополнительно действует [record copy invariant](records.md), поэтому тот же шаблон нельзя переносить механически.

**Граница модели:** этот конструктор предполагает, что во время проверки и копирования массив не меняют конкурентно другие потоки. Если другие потоки уже используют одну ссылку и мутируют её без синхронизации, одной последовательности `check → clone` недостаточно для доказательства отсутствия гонки. Это отдельный сценарий JMM/публикации (§9).

## 3. Defensive copy-in и copy-out: у массивов копия внешней оболочки shallow (I-04, I-09)

`clone()` для массива создаёт новый **массив того же компонентного типа и длины**; `Arrays.copyOf(original, newLength)` создаёт новый массив с выбранной длиной, обрезая или дополняя значения. Однако для массива **ссылок** элементы копируются как **те же ссылки на вложенные объекты**, а не рекурсивно.

~~~java
import java.util.Arrays;

class ShallowCopyBoundary {
    public static void main(String[] args) {
        int[][] matrix = {{1, 2}};
        int[][] outer = matrix.clone();
        System.out.println(outer != matrix);         // true: внешние массивы разные
        System.out.println(outer[0] == matrix[0]);   // true: внутренняя строка общая

        matrix[0][0] = 67;
        System.out.println(outer[0][0]);            // 67: alias сохраняется

        int[] extended = Arrays.copyOf(new int[]{1}, 3);
        System.out.println(Arrays.toString(extended)); // [1, 0, 0]
    }
}
~~~

Для `int[]` одна копия массива изолирует его элементы, потому что элементы — **примитивные значения**. Для `int[][]`, `StringBuilder[]`, `List<MutableElement>` или `List<List<String>>` нужно **отдельно определить, какие вложенные объекты должны быть независимыми**, и копировать их согласно этому требованию. Многомерный массив — [массив массивов](arrays.md), а не непрерывная матрица по контракту языка.

Если поле объекта содержит `int[][]`, недостаточно вернуть `field.clone()` и объявить весь граф глубоко неизменяемым: вызывающий сможет изменить `returned[0][0]`, потому что внутренний массив разделяется с исходным. Аналогично, `Collections.unmodifiableList` может защищать операции списка, но не делает вложенный `StringBuilder` неизменяемым.

## 4. Список-обёртка и структурный snapshot — принципиально разные контракты (I-05, I-06, I-14)

Здесь часто путают три вещи:

1. **Изменяемый исходный список.** Его хозяин может выполнять `add/remove/set`.
2. **`Collections.unmodifiableList(source)`** — *unmodifiable view*, через который нельзя применять структурные мутации списка, но view **поддерживается исходным списком**: изменения source остаются видимы.
3. **`List.copyOf(source)`** — *unmodifiable snapshot* элементов source **на момент копирования**, при условии отсутствия null; последующие структурные изменения source не отражаются в результате. Это **не рекурсивная копия элементов**.

~~~java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ViewsVersusSnapshots {
    public static void main(String[] args) {
        List<String> source = new ArrayList<>(List.of("A"));
        List<String> view = Collections.unmodifiableList(source);
        List<String> snapshot = List.copyOf(source);

        source.add("B");
        System.out.println(source);    // [A, B]
        System.out.println(view);      // [A, B]: live backed view
        System.out.println(snapshot);  // [A]: структурный snapshot

        try {
            view.add("C");
        } catch (UnsupportedOperationException e) {
            System.out.println("view rejects add");
        }
        try {
            snapshot.add("C");
        } catch (UnsupportedOperationException e) {
            System.out.println("snapshot rejects add");
        }
    }
}
~~~

**Почему две разные причины:** `view.add` запрещён как модификатор через wrapper, но `source.add` всё ещё меняет *базовый список* и виден через view. У snapshot собственная семантика неизменяемой последовательности элементов: структурное изменение source не влияет на набор элементов snapshot.

**Важно не обещать новую identity:** API `List.copyOf` допускает оптимизацию и переиспользование уже подходящего неизменяемого списка. То, что в одном запуске `source == snapshot` было `false`, — наблюдение конкретного случая, **не гарантия**, что всегда создаётся новый экземпляр. Смысл copyOf — **immutable value/structural snapshot**, а не обязательная аллокация.

**Null:** `List.copyOf` запрещает null-элементы и null-вход; при таком входе возникает **runtime `NullPointerException`**. Для `Collections.unmodifiableList` требования иные: сама обёртка не делает прежние nullable-элементы невозможными. Различайте null-компонент record, который может быть законен, и контракт метода copyOf, который запрещает null в коллекции (§7).

~~~java
import java.util.Arrays;
import java.util.List;

class CopyOfNull {
    public static void main(String[] args) {
        try {
            List.copyOf(Arrays.asList("A", null));
        } catch (NullPointerException e) {
            System.out.println("NPE from copyOf");
        }
    }
}
~~~

Этот пример **компилируется**; NPE происходит в методе при выполнении, а не на этапе `javac`.

## 5. Mutable элементы и вложенные ссылки: unmodifiable не значит deeply immutable (I-07, I-13)

Структурно unmodifiable список может содержать изменяемые объекты. Если другой участник держит ссылку на вложенный элемент, он способен изменить его, даже когда `snapshot.add` запрещён. **List.copyOf копирует ссылки на элементы, а не сами элементы**.

~~~java
import java.util.List;

final class MutableCounter {
    int value;
    MutableCounter(int value) { this.value = value; }
}

class ShallowListElements {
    public static void main(String[] args) {
        MutableCounter element = new MutableCounter(1);
        List<MutableCounter> snapshot = List.copyOf(List.of(element));
        element.value = 67;
        System.out.println(snapshot.get(0).value); // 67
        // snapshot.add(element); // компилируется, но runtime UOE при выполнении
    }
}
~~~

**Как сделать содержимое действительно устойчивым:** определите, что является *значением* вложенного элемента. Если это собственный immutable value object, часто достаточно snapshot структурных ссылок (например, `List<String>`, поскольку String immutable). Если элемент — mutable DTO, нужно скопировать или преобразовать каждый элемент в устойчивое представление, **затем** построить unmodifiable коллекцию. Возврат `List.copyOf(mutables)` сам по себе не выполняет deep copy.

Второй пример — **самостоятельная единица компиляции**: для него повторно приведён минимальный `MutableCounter`. Копируйте и запускайте каждый из двух Java-блоков отдельно, а не объединяйте их в один файл (иначе два объявления `MutableCounter` конфликтуют).

~~~java
import java.util.List;

final class MutableCounter {
    int value;
    MutableCounter(int value) { this.value = value; }
}

record CounterValue(int count) {}

class CopyMutableElements {
    static List<CounterValue> freeze(List<MutableCounter> source) {
        return source.stream()
                .map(e -> new CounterValue(e.value))
                .toList();       // структурно unmodifiable List в Java 21
    }

    public static void main(String[] args) {
        MutableCounter element = new MutableCounter(1);
        List<CounterValue> frozen = freeze(List.of(element));
        element.value = 67;
        System.out.println(frozen.get(0).count()); // 1
    }
}
~~~

Здесь текстовое требование — **сохранить значение числа на момент последовательного преобразования**, а не «сделать копию произвольного объектного графа». `Stream.toList()` возвращает unmodifiable список по контракту Java 21, а `CounterValue(int)` хранит примитивное значение, поэтому внешняя мутация `MutableCounter` больше не влияет на него. Важно: если исходные mutable элементы параллельно изменяют в момент чтения, нужен согласованный протокол синхронизации; один `map` не даёт консистентный snapshot конкурирующего графа.

## 6. Пример безопасного record с List<String> и пределы record (I-11, I-12)

В record component fields неявно `final`, но поле типа `List<String>` может хранить изменяемый список. Если весь публичный контракт — **значения строк** и порядок, compact constructor может запомнить `List.copyOf`. Это решает утечку структуры для данного типа элементов, не превращая record в универсальный immutable container.

~~~java
import java.util.ArrayList;
import java.util.List;

record UnsafeNames(List<String> names) {}

record Names(List<String> names) {
    Names {
        names = List.copyOf(names); // copy-in/snapshot, Strings сами immutable
    }
}

class SafeRecordList {
    public static void main(String[] args) {
        List<String> source = new ArrayList<>(List.of("A"));
        UnsafeNames unsafe = new UnsafeNames(source);
        Names safe = new Names(source);

        source.add("B");
        System.out.println(unsafe.names()); // [A, B]
        System.out.println(safe.names());   // [A]

        try {
            safe.names().add("C");
        } catch (UnsupportedOperationException e) {
            System.out.println("no add");
        }
    }
}
~~~

**Обратите внимание:** accessor `names()` возвращает уже unmodifiable snapshot, хранящийся в компоненте, и при реконструкции `new Names(safe.names())` сохраняется стандартный record copy invariant (поскольку сравнение List сравнивает соответствующие элементы). Здесь мы **не** переопределяли accessor так, чтобы он каждый раз создавал новый array с reference equality.

Для record с `int[]` другой случай. Если `values()` возвращает `clone`, но стандартный `equals` сравнивает ссылки массивов, реконструированный объект оказывается **не равен** исходному. [Подробный антипример BadAccessor](records.md) показывает R13=false — это **нарушение record copy invariant**. Даже copy-in массива **не** освобождает от согласования `equals/hashCode` и accessors. Если нужен скрытый внутренний массив с copy-in/out, обычный `final class Scores` из §2 зачастую проще.

**Решение о выборе:** record удобен, когда API сознательно прозрачен и состояние компонент адекватно представляет значение. Если требуется скрыть внешнее состояние, добавить собственные поля, гарантировать защитные копии/специальное равенство или управлять сложным жизненным циклом, обычный класс может дать более ясный контракт.

## 7. Идентичность, равенство и стабильность ключей Map (I-08, I-15)

**Идентичность ссылки (`==`)**, **значение (`equals`)** и **способность изменять объект** — три разных свойства. Unmodifiable коллекция может быть тем же объектом после copyOf, если уже соответствует требованиям; это не делает логическое значение «той же ссылкой обязано быть всегда». Кроме того, два разных immutable value objects могут быть равны по содержимому и иметь одинаковые хэши.

В качестве ключа `HashMap` часто выбирают значение с **устойчивым `equals/hashCode`**. Если ключ содержит mutable список/массив и равенство или hash зависят от изменяемого содержимого, после модификации ключ может перестать находиться в той же хеш-структуре. Это не универсальный дефект всех record: в record с массивом generated hash обычно ориентируется на ссылочную идентичность массива, а не на изменяемые элементы. Но **при пользовательском содержательном hash по элементам** и внешнем alias риск реален.

~~~java
import java.util.HashMap;
import java.util.Map;

final class MutableKey {
    int n;
    MutableKey(int n) { this.n = n; }
    @Override public boolean equals(Object other) {
        return other instanceof MutableKey k && k.n == n;
    }
    @Override public int hashCode() { return Integer.hashCode(n); }
}

class KeyMutation {
    public static void main(String[] args) {
        MutableKey key = new MutableKey(67);
        Map<MutableKey, String> map = new HashMap<>();
        map.put(key, "value");
        key.n = 1; // теперь hashCode другой
        System.out.println(map.get(key)); // null для этой последовательности
    }
}
~~~

**Гарантия vs наблюдение:** HashMap использует стабильность результатов `hashCode`/`equals` ключа в пределах нахождения ключа в структуре. Нельзя менять состояние, влияющее на эти результаты, и ожидать корректного поиска; показанный `null` — результат конкретного сценария. Подробная теория контрактов — в [CORE-EQUALITY](equality.md).

## 8. Произвольный граф и циклические ссылки — почему deep copy не одна строчка (I-16)

Объекты образуют граф: один элемент может ссылаться на другого; два поля могут ссылаться на **один и тот же** вложенный объект; возможна циклическая связь. Если при копировании нужно сохранить такие свойства, обычное рекурсивное `clone` без таблицы уже скопированных объектов может нарушить aliasing либо не завершиться на цикле. Нужно заранее определить:

- Какие вершины графа являются значениями, а какие — идентичностями?
- Должны ли две исходные ссылки на один объект остаться двумя ссылками на одну **копию**?
- Что делать с циклом, внешними ресурсами, null и взаимными ссылками?
- Должна ли копия отражать состояние на один момент времени при конкурентных мутациях?

Решение этих задач зависит от доменной модели; нельзя обещать «универсальная глубокая неизменяемость после `List.copyOf`» или «`Arrays.deepEquals` сравнит любые циклические графы». В этой статье мы умеем **выделять границы владения и копировать выбранные изменяемые компоненты**, но не разрабатываем универсальный графовый copy algorithm. В [CORE-ARRAYS](arrays.md) отдельно рассмотрены shallow и ограниченность deep-операций массивов.

## 9. Потоковая публикация — самостоятельная гарантия, не вывод из final (I-17 · P2)

Неизменяемость внешнего API и **безопасная публикация между потоками** связаны, но не эквивалентны. Специальная семантика `final` полей в Java Memory Model важна для видимости состояния после корректного конструирования при определённых условиях. Однако нельзя доказать корректность произвольного достижимого mutable graph и запрет всех data races только указанием `private final` в примере.

Особенно опасны `this`, опубликованный во время выполнения конструктора, конкурентная мутация входного массива в момент defensive copy, либо callbacks, которые видят частично сконструированное состояние. В этих случаях надо исследовать корректную публикацию, happens-before и протокол доступа к общим mutable объектам.

**I-17 — осознанная P2-граница:** здесь нет независимого happens-before proof, JMM litmus/stress и анализа небезопасного escape. Из одного запуска на OpenJDK нельзя делать выводы о потокобезопасности всех применений. Подробная будущая тема — `CON-JMM`, а не ещё один абзац «final значит thread-safe».

## 10. Что компилируется, а что является runtime/protocol failure

| Ситуация | Стадия | Объяснение |
|---|---|---|
| `final int[] data = {67}; data[0] = 1;` | Компилируется | Final запрещает переназначить **ссылку**, не изменить элемент |
| `List.copyOf(Arrays.asList("A", null))` | **Runtime NPE** | Метод запрещает null-элементы |
| `List.copyOf(source).add("B")` | **Runtime UOE** | Unmodifiable результат |
| `Collections.unmodifiableList(source); source.add("B")` | Компилируется, view изменится | Wrapper не копирует source |
| `values.clone()` у `int[][]` | Компилируется, shallow | Вложенные строки разделены |
| `BadAccessor.values() { return values.clone(); }` | Компилируется, но нарушает record copy invariant | Копия массива по умолчанию сравнивается по ссылке |
| `new Scores(new int[0])` из §2 | **Runtime IllegalArgumentException** | Собственная проверка инварианта |
| `record Frozen(int[] a) {}` | Компилируется, объект не deeply immutable | Конечная ссылка на mutable array |

Компиляционно-отрицательные примеры N01–N16 из G1 относятся прежде всего к **языковым ограничениям record**. Проверка NPE/UOE/валидации в запущенной программе **не** доказывает, что код не компилируется, и не заменяет `javac -XDrawDiagnostics` с проверкой ожидаемой категории.

## Карта I-01–I-19: механизм, контрпример и честная граница

| ID | Приоритет | Раздел и пример / предел гарантии |
|---|---|---|
| I-01 | P0 | §1: наблюдаемая неизменяемость; mutable aliased List |
| I-02 | P0 | §1: `final` поле хранит неизменную ссылку, не mutable состояние |
| I-03 | P0 | §1/2: alias на входе (Unsafe/List) и защитное copy-in |
| I-04 | P0 | §2/3: alias на выходе и копия в accessor |
| I-05 | P0 | §4/6: `List.copyOf` snapshot ссылок и порядка |
| I-06 | P0 | §4: `Collections.unmodifiableList` live view |
| I-07 | P0 | §4/5: UOE у mutator vs mutable nested element |
| I-08 | P0 | §4/7: null/NPE; record null отдельно; identity не гарантирована |
| I-09 | P0 | §2/3: обе копии массива, clone/copyOf shallow |
| I-10 | P0 | §2/6: constructor invariant и ошибочная валидация |
| I-11 | P0 | §6: record не deeply immutable, snapshot List<String> |
| I-12 | P0 | §2/6: когда final class удобнее record |
| I-13 | P1 | §5: mutable elements, копия значений в CounterValue |
| I-14 | P1 | §4: view vs copyOf, identity optimization |
| I-15 | P1 | §7: равенство/hash key меняются при мутации |
| I-16 | P1 | §8: aliasing/cycles, нет универсального deep copy |
| I-17 | P2 | §9: JMM/final safe publication — **не доказано** |
| I-18 | P2 | **Граница:** DTO, JavaBeans/ORM/JPA и framework-specific contracts — отдельно |
| I-19 | P2 | **Граница:** allocation/JIT/GC/JMH performance — не измерено |

**Связанная новая статья:** [CORE-DATETIME](datetime.md) показывает неизменяемость `Instant`, `LocalDate` и `ZonedDateTime`: `plusDays` возвращает новое значение, а не меняет исходный объект. Это применение принципа value objects, но не новый эксперимент по потокобезопасности или JMM.

## Нормы, пример G1 и пределы проверки

**Первоисточники:** [List.copyOf](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#copyOf(java.util.Collection)), [Collections.unmodifiableList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#unmodifiableList(java.util.List)), [List.of](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#of()), [Arrays.clone/copyOf](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html), [JLS §4.12.4 final](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4), [JLS §17.5 final fields](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.5), [java.lang.Record](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html). В [SOURCE_REGISTRY](../../docs/SOURCE_REGISTRY.md) неофициальные интервью-источники используются для формулирования вопросов, **не** как доказательство Java API.

**Исследовательский G1** включает полностью проверенный на OpenJDK 21.0.12.1 `ImmutableProbe.java` с **I01–I10, 10 строк stdout**: `I01=[A, B]`, `I02=[A]`, `I03=[A, B]`, `I04=UOE`, `I05=NPE`, `I06=67`, `I07=67`, `I08=false` (наблюдение, **не** требование новой identity), `I09=[1, 0, 0]`, `I10=empty`. Вместе с `RecordsProbe.java` это **26 наблюдений двух отдельных запусков**, а не 26 unit-tests. [Оба исходника и эталона](../../examples/core/records-immutability/README.md) уже включены в авторскую ветку из [технического PR #133](https://github.com/Melchiorius/backend-interview-labs/pull/133). Роль 03 отдельно сверила опубликованные SHA и воспроизвела оба G1 stdout (16/16 и 10/10) на Debian OpenJDK 21.0.12.1 с нулём предупреждений. **CI объединённого HEAD** проверяется отдельно в [PR #132](https://github.com/Melchiorius/backend-interview-labs/pull/132); результат локального запуска не означает компиляции всех inline Java-фрагментов.

**NOT RUN без отдельного evidence:** автоматическое извлечение/компиляция *всех* фрагментов Java из статьи; отрицательные N01–N16 в CI; визуальный Obsidian/IntelliJ; другие JDK/JVM/OS, реальные concurrent mutation/JMM litmus, property-based проверки всех возможных graph aliases, reflection/serialization/ORM, Unicode/IO, benchmark/JMH/GC/JIT. Положительный запуск одного примера не заменяет нормативного доказательства глубокой неизменяемости или безопасной публикации.

**Продолжение про стандартные представления коллекций:** [CORE-UTIL](utilities.md) сопоставляет backed `Arrays.asList`, `Collections.unmodifiableList` как живую обёртку, `List.of` и shallow structural snapshot `List.copyOf`.
