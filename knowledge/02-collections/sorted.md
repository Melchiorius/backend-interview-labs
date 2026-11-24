# Сортированные коллекции Java 21: TreeMap и TreeSet

**Тема:** COL-SORTED. [План C3](../../docs/plans/COL-SORTED-HEAP.md). Предпосылки: [контракты](contracts.md), [хеш-коллекции](hash-collections.md), [равенство](../01-java-core/equality.md), [итераторы](iteration.md). Практика: [SortedHeapProbe](../../examples/collections/sorted-heap/README.md).

Сортированная карта полезна не только для вывода ключей в порядке возрастания. Например, если ключ — момент начала тарифа, то для произвольного момента можно найти ближайший предшествующий тариф без полного обхода. Хеш-карта решает другую задачу: поиск по равенству ключа, а не поиск соседей.

## Правила сравнения (CT-01, CT-02, CT-14)

Comparable задаёт естественный порядок объектов одного типа через compareTo; Comparator позволяет передать внешний порядок. Важен знак результата, а не его абсолютное значение. TreeMap.comparator() возвращает null при естественном порядке.

Для корректного Comparator необходимы:
- sign(compare(a,b)) = -sign(compare(b,a));
- если a > b и b > c, то a > c;
- если compare(a,b) == 0, знаки compare(a,c) и compare(b,c) должны совпадать для любого c.

Симметрия исключений также обязательна: если compare(a,b) бросает исключение, то compare(b,a) тоже должен его бросать. Аналогичные правила действуют для Comparable. Согласованность с equals желательна, но не обязательна для самого Comparator.

~~~java
Comparator<Integer> broken = (a,b) -> a-b;
System.out.println(broken.compare(Integer.MAX_VALUE,-1)); // MIN_VALUE
System.out.println(broken.compare(-1,Integer.MAX_VALUE)); // MIN_VALUE
Comparator<Integer> safe = Integer::compare;
~~~

Переполнение int даёт отрицательный знак в обоих направлениях. Такой компаратор нельзя применять для корректной сортировки. Integer.compare сравнивает значения без вычитания. Для обратного порядка используйте reversed() или Comparator.reverseOrder(), а не опасное вычитание.

~~~java
record Person(String name, int id) {}
Comparator<Person> order = Comparator.comparing(Person::name)
    .thenComparingInt(Person::id);
~~~

thenComparing применяется только при равенстве по первому критерию. Если id не делает пару уникальной, дополнительные лица всё ещё могут сравниваться как равные. Comparator.nullsFirst(Comparator.naturalOrder()) позволяет сравнивать null с обычными значениями; nullsLast помещает его после них.

## Уникальность по сравнению, не по equals (CT-03, CT-04)

~~~java
var a = new java.math.BigDecimal("1.0");
var b = new java.math.BigDecimal("1.00");
System.out.println(a.equals(b)); // false: различный scale
System.out.println(a.compareTo(b)); // 0
System.out.println(new TreeSet<>(List.of(a,b)).size()); // 1
System.out.println(new HashSet<>(List.of(a,b)).size()); // 2
~~~

TreeSet считает элемент уже существующим при compare==0. TreeMap аналогично считает ключи эквивалентными и заменяет значение существующего отображения. Это определённое поведение сортированной структуры, однако при несогласованности порядка с equals она не соблюдает общий контракт Set/Map.

~~~java
var m = new TreeMap<String,Integer>(
    Comparator.comparingInt(String::length));
m.put("ab",1);
System.out.println(m.put("cd",67)); // 1
System.out.println(m.size()); // 1
System.out.println(m.get("xy")); // 67
~~~

Если задача требует отдельную запись для каждой строки, одного сравнения длин недостаточно: добавьте thenComparing(Comparator.naturalOrder()). Обратная ошибка — equals считает объекты равными, а компаратор различает их — также нарушает согласованность.

## Навигация (CT-05, CT-06)

TreeMap упорядочивает ключи, TreeSet — элементы. Это не порядок вставки LinkedHashMap. TreeSet не имеет метода get(int) (отрицательный пример N02); для выбора элемента по рангу стандартное API дерева не предоставляет индексной операции. Фабрики TreeSet.of нет.

Для ключей 10,20,30 в естественном порядке:

| Метод TreeMap | Отношение к 20 | Результат |
|---|---|---|
| lowerKey | строго меньше | 10 |
| floorKey | меньше либо равен | 20 |
| ceilingKey | больше либо равен | 20 |
| higherKey | строго больше | 30 |

Отношения определяются компаратором, а не числовым смыслом ключа. Методы поиска соседей возвращают null при отсутствии результата. У карты с допустимым null-ключом это может быть неоднозначно: используйте entry-вариант, который возвращает null при отсутствии записи. firstKey/lastKey и TreeSet.first/last на пустой структуре бросают NoSuchElementException. firstEntry/lastEntry и pollFirstEntry/pollLastEntry пустой карты возвращают null. pollFirst/pollLast пустого множества тоже возвращают null.

## Диапазонные и обратные представления (CT-07, CT-08, CT-16)

~~~java
var m = new TreeMap<Integer,String>();
m.put(10,"a"); m.put(20,"b"); m.put(30,"c");
var range = m.subMap(10,true,30,false);
range.put(15,"middle");
System.out.println(m.get(15)); // middle
m.put(20,"changed");
System.out.println(range.get(20)); // changed
// range.put(30,"x"); // IllegalArgumentException
~~~

headMap(to,false) ограничивает верхнюю границу, tailMap(from,true) — нижнюю; аналогичные методы есть у NavigableSet. Двухаргументный subMap(from,to) включает from и исключает to. Представление связано с исходной картой, а не является копией. Вставка за пределами диапазона запрещена. Пустой допустимый диапазон — не ошибка: отсутствие элементов не означает некорректные границы. Если from > to по порядку карты или вложенное представление запрашивает запрещённую границу, возможен IllegalArgumentException.

descendingMap/descendingSet и reversed() в Java 21 предоставляют обратный порядок и связанное представление. При использовании descendingMap границы задаются уже относительно обратного компаратора: descending.subMap(30,true,10,false) допустим для исходных возрастающих ключей 10..30. Эквивалентность по нулю сравнения при развороте не меняется.

TreeMap.putFirst/putLast и TreeSet.addFirst/addLast бросают UnsupportedOperationException: произвольное позиционирование противоречит порядку сравнения. Удаление первого/последнего элемента при этом поддерживается.

## Снимки записей и изменяемые ключи (CT-11, CT-12)

~~~java
var m = new TreeMap<Integer,String>();
m.put(1,"old");
var entry = m.firstEntry();
m.put(1,"new");
System.out.println(entry.getValue()); // old
// entry.setValue("x"); // UnsupportedOperationException
~~~

Навигационные записи TreeMap (firstEntry, lastEntry, lowerEntry, floorEntry, ceilingEntry, higherEntry и poll-варианты) представляют снимок отображения на момент получения. Это не глубокая копия объектов ключа и значения. Запись из итератора entrySet отличается: её связь с картой ограничена контрактом Map.Entry; после изменения карты поведение такой записи не определено, кроме разрешённого собственного setValue.

Если ключ — объект с изменяемым полем rank, а сравнение опирается на rank, его положение в дереве рассчитано по старому значению. Изменение поля не уведомляет дерево. Нельзя обещать успешный get/remove или определённый результат обхода после нарушения инварианта. Удаляйте ключ до изменения, затем вставляйте снова, либо используйте неизменяемый ключ. Если инвариант уже нарушен, может требоваться перестроение из достоверных данных.

## null и типы (CT-10, CT-18)

Естественный порядок TreeMap/TreeSet не допускает null-ключи/элементы. Переданный компаратор может их разрешить; TreeMap при этом может хранить null-значения независимо от политики ключей.

~~~java
var set = new TreeSet<Integer>(
    Comparator.nullsFirst(Comparator.naturalOrder()));
set.add(null);
set.add(67);
System.out.println(set.first()); // null
~~~

Несовместимые ключи могут вызвать ClassCastException во время сравнения. Однако new TreeMap<Integer,String>().put("x","y") не компилируется (N01): generics обнаруживают ошибку раньше. Raw types обходят часть проверок компилятора и не обеспечивают взаимную сравнимость.

## Стоимость и устройство дерева (CT-09, CT-13, CT-17)

Javadoc TreeMap гарантирует логарифмическую стоимость containsKey/get/put/remove, TreeSet — add/remove/contains. Цена одного сравнения не обязательно постоянна: сравнение длинных строк может потребовать обхода общего префикса. В модели стоимости сравнений O(log n) сравнений с ценой C дают O(C log n).

В закреплённом OpenJDK 21 TreeMap реализован красно-чёрным деревом. Узлы хранят ключ, значение, ссылки left/right/parent и цвет. Поиск идёт налево или направо по сравнению; вставка сначала добавляет узел как в обычном дереве поиска. Затем повороты и перекрашивание восстанавливают баланс: красные узлы не имеют красных детей, а пути от узла до пустых листьев имеют одинаковое число чёрных узлов. Это ограничивает высоту O(log n). Поворот меняет локальные связи без изменения отсортированного порядка. TreeSet использует NavigableMap в качестве хранилища.

Конкретные повороты, цвет узлов и их размещение в памяти не являются обещанием API. Сериализация не гарантирует восстановления той же внутренней формы. Обход в порядке ключей определяется контрактом, механизм переходов по узлам — реализацией. Итераторы TreeMap/TreeSet обнаруживают несовместимые изменения по возможности; ConcurrentModificationException не обеспечивает синхронизацию.

## Выбор структуры (CT-15)

| Структура | Уникальность | Порядок | Типичная задача |
|---|---|---|---|
| HashMap/HashSet | equals и согласованный hashCode | не гарантирован | поиск точного соответствия, ожидаемо O(1) при хорошем хешировании |
| TreeMap/TreeSet | compare==0 | порядок сравнения | соседи, диапазоны, логарифмические операции |
| LinkedHashMap | equals/hashCode | заданный порядок обхода | предсказуемый обход, в том числе порядок доступа при соответствующей настройке |

Стоимость, политика null и требования к изменяемости ключа отличаются. Для повторного извлечения наиболее приоритетного элемента с дубликатами см. [PriorityQueue](priority-queue.md).

## Источники и проверяемость

A — Java SE 21: [Comparable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Comparable.html), [Comparator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html), [TreeMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html), [TreeSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html), [NavigableMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/NavigableMap.html), [NavigableSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/NavigableSet.html), [Map.Entry](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.Entry.html), [BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html). I — [TreeMap.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/TreeMap.java), [TreeSet.java](https://github.com/openjdk/jdk21/blob/890adb6410dab4606a4f26a942aed02fb2f55387/src/java.base/share/classes/java/util/TreeSet.java).

Матрица CT-01–18 размечена в заголовках. Интервью-источники C и их ограничения зафиксированы в принятом плане; повторная выборка ещё не завершена. Практика роли 03 интегрирована: 51 строка вывода и N01/N02 по её локальному отчёту; [технический CI](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45364703998) успешен на d95c974c88cfdc416bdcaf9ab9b7afac209d6bac. Отдельная компиляция всех фрагментов статьи, локальный запуск автором, GUI Obsidian и измерения — NOT RUN.
