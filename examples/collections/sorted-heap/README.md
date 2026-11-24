# Сортированные коллекции и приоритетная очередь: проверки Java 21

Теория: [сортированные коллекции](../../../knowledge/02-collections/sorted.md) и [приоритетная очередь](../../../knowledge/02-collections/priority-queue.md).
Предпосылки: [контракты](../../../knowledge/02-collections/contracts.md), [hash-based коллекции](../../../knowledge/02-collections/hash-collections.md), [последовательные коллекции](../../../knowledge/02-collections/sequential.md), [итераторы](../../../knowledge/02-collections/iteration.md), [equals/hashCode](../../../knowledge/01-java-core/equality.md), [generics](../../../knowledge/01-java-core/generics.md), [неизменяемость](../../../knowledge/01-java-core/immutability.md).
Основание: [план C3](../../../docs/plans/COL-SORTED-HEAP.md).

## Запуск и контракт

[SortedHeapProbe.java](src/SortedHeapProbe.java) — самостоятельный пример без зависимостей и входных данных. Проверка check сравнивает ожидаемое и фактическое значения, throwsType проверяет тип исключения; расхождения вызывают AssertionError независимо от -ea. stdout содержит 51 строку ([эталон](expected.stdout)). Это не 51 JUnit-тест и не полное покрытие всей матрицы.

Нужны OpenJDK 21 (javac и java одного JDK), Bash, diff, grep, mktemp и стандартные Unix-утилиты. Из корня checkout:

```bash
bash examples/collections/sorted-heap/verify.sh
```

Из каталога примера: `./verify.sh`. Для отдельного позитивного запуска: `java src/SortedHeapProbe.java`. Runner использует временный каталог, javac --release 21 -Xlint:all, java, полный diff -u, затем отдельно компилирует N01/N02. Временные файлы удаляются; exit 0 означает прохождение всех проверок.

## Разбор

| Сценарий | Матрица | Механизм |
|---|---|---|
| R01 BigDecimal | CT-03/04 | 1.0 и 1.00 различаются scale и equals, но compareTo возвращает 0. TreeSet хранит один элемент, HashSet два. Порядок HashSet не проверяется. Несогласованный с equals порядок допустим для sorted semantics, но нарушает общий Set/Map equality contract. |
| R02 Overflow | CT-01/02, CP-02 | MAX_VALUE - (-1) переполняется до MIN_VALUE; обратное вычитание также MIN_VALUE. Оба знака отрицательные, что нарушает антисимметрию знака. Integer.compare даёт противоположные знаки. С ошибочным comparator сортировка не запускается. |
| R03 Navigation/range | CT-05/06/07 | В порядке ключей 10,20,30 для 20 lower/floor/ceiling/higher равны 10/20/20/30. Диапазон [10,30) исключает 30; put через view меняет parent, parent update виден в range. Вставка 30 через range запрещена. |
| R04 Reverse/snapshot | CT-08/12 | descendingMap/reversed меняют порядок обхода и остаются backed views; удаление/вставка отражаются в parent. TreeMap putFirst/putLast запрещены, порядок задаётся сравнением. firstEntry — snapshot отображения, setValue не поддерживается; не deep copy mutable value. |
| R05 PQ | CP-01/02/05/07 | Natural head — минимум; reverseOrder даёт максимум. Извлекать по приоритету через poll/remove(), не считать iterator отсортированным. remove(Object) ищет конкретный равный элемент, а remove() удаляет голову. |
| R06 PQ boundaries | CP-03/04/06 | peek/poll на пустой очереди возвращают null, element/remove бросают NSE, offer(null) — NPE. Дубликаты допустимы. Две разные Task с priority=1 проверяются по приоритету и принадлежности множеству, не по относительному порядку. |
| R07 Comparator equivalence | CT-03/04/10/14 | Сравнение строк только по длине делает ab/cd/xy эквивалентными. TreeSet отвергает повтор, TreeMap заменяет значение. Natural TreeSet отвергает null; nullsFirst позволяет null в выбранном TreeSet. |
| R08 Mutable ordering/priority | CT-11, CP-13 | Объяснение ниже, не фиксированный runtime результат. |

### Изменяемые ключи и приоритеты (R08)

Дерево размещает ключ по результатам сравнений при вставке; куча восстанавливает инвариант при операциях offer/poll. Если изменить поле, участвующее в сравнении уже сохранённого объекта, коллекция не получает уведомления об изменении и не перестраивается автоматически. Например, задача с priority=67 может стать priority=1, но это не даёт обещания немедленно увидеть её в head. У TreeMap/TreeSet поиск по изменённому ключу также не должен считаться надёжным. Не закрепляем результат такой мутации как Java API guarantee. Выбирайте immutable ordering key либо удаляйте элемент **до** изменения сравниваемых полей, затем вставляйте снова. Если инвариант уже нарушен, требуется корректное перестроение из достоверного набора элементов, а не обещание успешного remove мутированного ключа.

## Compiler-negative

- [N01](negative/N01.java): TreeMap<Integer,String> не принимает String в качестве ключа (CT-18).
- [N02](negative/N02.java): TreeSet не имеет get(int) (CT-05).

На javac 21.0.12.1 оба завершаются exit=1. N01: compiler.err.cant.apply.symbols с вложенной compiler.misc.inconvertible.types (String/Integer); N02: compiler.err.cant.resolve.location.args (get/int). Runner проверяет статус и диагностическую категорию, не весь текст ошибки. Файлы намеренно некомпилируемы; не включать их в позитивную компиляцию.

## Границы

Никаких проверок неопределённого порядка итератора PQ, tie placement, формы дерева/кучи, latency, GC, памяти, private reflection или гонок. CT-09/CP-07 — оценки сложности из API с учётом цены сравнения, не benchmark; CT-13/CP-08/11 — механизмы реализации, не наблюдаемые гарантии формы структуры. PQ не потокобезопасна. См. Java SE 21 [TreeMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html), [TreeSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html), [Comparator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html), [PriorityQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html).
