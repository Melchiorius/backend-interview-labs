# Java Core — карта статей, примеров и направлений углубления

**Статус:** проект содержания, не перечень готовых файлов. Основная версия — Java 21. Этот документ уточняет укрупнённые категории `CORE-*` из [CURRICULUM.md](CURRICULUM.md), не заменяет [COVERAGE.md](COVERAGE.md) и не задаёт обязательный порядок прохождения.

## Принципы декомпозиции

- **Статья** — самостоятельная единица объяснения с конкретным вопросом и достаточной глубиной, а не одна страница на каждый метод языка. Близкие короткие темы объединять, если разделение лишь создаёт навигационный шум.
- **Встроенный пример** — короткий код в Markdown для объяснения правила. **Отдельный запускаемый пример** нужен при нескольких взаимодействующих типах, демонстрации ошибки/диагностики или если читателю важно воспроизвести поведение. **Упражнение** добавлять там, где самостоятельная реализация приносит пользу; не на каждую статью.
- **«Хвосты»** в таблицах — именованные направления расширения и будущие связи. Пока материал по целевому ID не создан, это **не Markdown-ссылка**. После появления статьи добавить локальную ссылку Obsidian и обратную навигацию, не оставлять битые ссылки.
- Не дублировать большие темы из Collections, Concurrency, JVM, I/O, SQL и Spring: Java Core даёт необходимую основу и направляет к соответствующим разделам.
- Никаких обязательных слоёв Middle/Middle+/Senior или отдельных тестов на каждый синтаксический пример. Важно не количество файлов, а полнота объяснения и точность границ.

## Предлагаемые статьи

### 01. Язык и значения

| ID / статья | Что раскрыть | Пример (по умолчанию внутри статьи) | Продолжение / «хвост» |
|---|---|---|---|
| `CORE-SYNTAX` — **Структура Java-программы и область видимости** | Компиляционная единица, package/import, main, блоки, локальные переменные, области видимости и shadowing | Короткие фрагменты: видимость переменной в блоках | → JVM-BYTECODE: компиляция, source-file launch, jshell |
| `CORE-TYPES` — **Примитивные и ссылочные типы** | 8 примитивов, значения ссылок, null, переменные и присваивание, default values, границы и переполнение | Код: присваивание примитива и ссылки, изменение одного объекта через две ссылки | → JVM-MEMORY: представление значений, escape analysis |
| `CORE-OPERATORS` — **Выражения, операторы и вычисление** | Приоритет, short-circuit, условный оператор, инкремент, последовательность вычисления, целочисленное деление | Код с побочными эффектами, объяснение каждого шага | → JLS: тонкости evaluation order |
| `CORE-CONVERSIONS` — **Приведения типов и перегрузка на границах** | Widening/narrowing, boxing/unboxing, числовое продвижение, null-unboxing, выбор перегрузки | Код: почему выбран один из overload и где возник NPE | → CORE-GENERICS: стирание и совместимость типов; COL-CONTRACTS: boxing |
| `CORE-PARAMS` — **Передача аргументов и изменение объектов** | Всегда передача значения; значение ссылки не означает передачу переменной по ссылке; mutable/immutable | Код: reassign параметра против изменения поля объекта | → CORE-IMMUTABLE; CON-RACE: общее изменяемое состояние |
| `CORE-CONTROL` — **Управляющие конструкции** | if/for/while/switch, break/continue, switch expressions, yield, проверка полноты | Код: обычный switch и выражение switch | → CORE-PATTERNS: pattern switch; ALG-COMPLEXITY |

### 02. Объекты и классы

| ID / статья | Что раскрыть | Пример (по умолчанию внутри статьи) | Продолжение / «хвост» |
|---|---|---|---|
| `CORE-CLASSES` — **Классы, объекты, поля и методы** | Создание, конструкторы, this, инициализация экземпляра, static vs instance, методы и свойства состояния | Пример: простой счёт с проверкой инварианта | → JVM-LOADING: инициализация классов; CON-JMM: безопасная публикация |
| `CORE-ACCESS` — **Инкапсуляция и видимость** | public/protected/package-private/private, пакеты, минимальная область доступа, API против реализации | Код: package-private + наследование через другой пакет | → CORE-MODULES: exports/opens; SPR-DI |
| `CORE-OBJECT` — **Контракт java.lang.Object** | Object, toString, equals, hashCode, getClass, базовые контракты; wait/notify — только упоминание | Код: объект-значение с equals/hashCode и неправильный вариант | → COL-HASH; CON-MONITOR |
| `CORE-INHERITANCE` — **Наследование и полиморфизм** | extends, overriding, dynamic dispatch, super, final, скрытие полей, перегрузка vs переопределение | Код: Base/Derived, static/instance, перегрузка и реальный тип | → CORE-SEALED: ограничение иерархий; SPR-AOP |
| `CORE-INTERFACES` — **Интерфейсы и абстрактные классы** | Контракт, default/static/private методы интерфейса, множественная реализация, выбор абстракции | Код: разные реализации одного сервиса и default methods | → CORE-FUNCTIONS: функциональные интерфейсы; SPR-DI |
| `CORE-COMPOSITION` — **Композиция, делегирование, зависимости** | «Имеет» против «является», отношение объектов, иммутабельные зависимости, декомпозиция API | Код: OrderService и PriceCalculator, подмена зависимости в тесте | → SPR-DESIGN; TEST-DESIGN; DESIGN-ARCH |
| `CORE-NESTED` — **Вложенные и локальные классы** | static nested, inner, local, anonymous, захват переменных, implicit outer reference | Код: inner против static nested, anonymous Runnable | → CORE-FUNCTIONS; JVM-MEMORY |
| `CORE-INIT` — **Порядок инициализации** | Поля, блоки и конструкторы, this()/super(), static initialization, опасности вызова overridable метода | Код: печать порядка выполнения без навязывания внутренней модели JVM | → JVM-LOADING: loading/linking/initialization; CORE-INHERITANCE |

### 03. Строки, массивы, значения

| ID / статья | Что раскрыть | Пример (по умолчанию внутри статьи) | Продолжение / «хвост» |
|---|---|---|---|
| `CORE-ARRAYS` — **Массивы** | Размеры, индексы, ссылочные элементы, covariance, ArrayStoreException, копирование, varargs | Код: shallow copy массива ссылок и covariance | → COL-SEQUENTIAL; CORE-GENERICS: arrays vs generics |
| `CORE-STRINGS` — **String и текст** | Неизменяемость String, equals против ==, concat, Unicode code points и UTF-16, split/replace | Код: code point против char, substring и сравнение строк | → IO-BYTES: кодировки; JVM-MEMORY: string pool |
| `CORE-BUILDERS` — **StringBuilder и форматирование** | Когда конкатенация достаточна, накопление текста, StringBuilder/StringBuffer, форматирование | Код: построение строки в цикле, корректность и читаемость | → JVM-JIT: оптимизации конкатенации; CON-RACE: thread safety |
| `CORE-ENUMS` — **Enum как тип** | Константы и поведение, поля/методы, values/valueOf, EnumSet/EnumMap | Код: статус заказа с допустимыми переходами | → COL-CONTRACTS; CORE-SEALED |
| `CORE-RECORDS` — **Record и объект-значение** | Компоненты, конструктор с проверками, автоматически создаваемые методы, неглубокая неизменяемость | Код: record с mutable List и копирование через List.copyOf | → CORE-IMMUTABLE; CORE-OBJECT |
| `CORE-IMMUTABLE` — **Неизменяемость и защитные копии** | Состояние vs ссылка, final, defensive copies, unmodifiable vs immutable, инварианты | Код: неправильный и правильный immutable value object | → CON-JMM: final fields, publication; COL-CONTRACTS |

### 04. Контракты объектов и типизация

| ID / статья | Что раскрыть | Пример (по умолчанию внутри статьи) | Продолжение / «хвост» |
|---|---|---|---|
| `CORE-EQUALITY` — **Равенство, хеширование и порядок** | equals/hashCode, симметрия, транзитивность, compareTo/Comparator consistency; mutable key | Код: ключ HashMap до/после изменения поля и корректный тип | → COL-HASH; COL-SORTED |
| `CORE-GENERICS` — **Обобщённые типы** | Type parameters, invariance, wildcards, PECS, generic methods, ограничения | Код: копирование List<? extends T> → List<? super T> | → COL-CONTRACTS; CORE-ERASURE |
| `CORE-ERASURE` — **Стирание типов и ограничения generics** | Erasure, raw types, unchecked casts, heap pollution, bridge methods на уровне API | Код: varargs + heap pollution, предупреждения компилятора | → JVM-BYTECODE: bridge methods; CORE-REFLECTION: ParameterizedType |
| `CORE-SEALED` — **Sealed-иерархии** | sealed/permits/non-sealed/final, exhaustiveness и границы закрытой модели | Код: sealed Result + switch pattern matching | → CORE-PATTERNS; DESIGN-ARCH |
| `CORE-PATTERNS` — **Pattern matching в Java 21** | instanceof patterns, record patterns, pattern matching for switch, null handling, guarded patterns | Код: обработка вариантов Result без цепочки кастов | → CORE-SEALED; CORE-CONTROL |

### 05. Ошибки и ресурсы

| ID / статья | Что раскрыть | Пример (по умолчанию внутри статьи) | Продолжение / «хвост» |
|---|---|---|---|
| `CORE-EXCEPTIONS` — **Исключения и управление ошибками** | Throwable/Error/Exception/RuntimeException, checked/unchecked, причины, propagation, catch ordering | Код: layered exception, сохранение cause, логирование на границе | → SPR-WEB: обработка ошибок; DIST-FAIL |
| `CORE-RESOURCES` — **try-with-resources** | AutoCloseable, гарантии закрытия, suppressed exceptions, порядок закрытия, исключение из close | Код: два ресурса, throw в work и close, getSuppressed() | → IO-FILES; DB-JDBC; CORE-EXCEPTIONS |
| `CORE-ASSERT` — **Проверка входных данных и assert** | IllegalArgumentException vs IllegalStateException, Objects.requireNonNull, assert не для проверки внешнего ввода | Код: инвариант конструктора и валидатор | → TEST-JUNIT; SPR-WEB |
| `CORE-OPTIONAL` — **Optional и отсутствие значения** | of/ofNullable/empty, map/flatMap/orElse/orElseGet, API return types и anti-patterns | Код: вычисление fallback в orElse против orElseGet | → CORE-STREAMS; DB-ORM |

### 06. Функциональный стиль и потоки данных

| ID / статья | Что раскрыть | Пример (по умолчанию внутри статьи) | Продолжение / «хвост» |
|---|---|---|---|
| `CORE-FUNCTIONS` — **Лямбды и функциональные интерфейсы** | SAM, lambda, method reference, effectively final, области видимости, исключения | Код: Predicate/Function/Consumer и capture | → CORE-STREAMS; CON-FUTURE |
| `CORE-STREAMS` — **Stream API: построение и обработка** | source/intermediate/terminal, lazy evaluation, map/filter/flatMap, once-only stream, collectors | Код: pipeline для заказов с несколькими стадиями | → COL-CONTRACTS; CORE-STREAM-ADV |
| `CORE-STREAM-ADV` — **Stream: корректность, побочные эффекты и производительность** | Collector, grouping, toMap collision, null semantics, stateful ops, parallel streams, encounter order | Код: toMap с duplicate keys, неверное использование side effects | → CON-FORKJOIN; JVM-JIT; ALG-COMPLEXITY |

### 07. Стандартные API общего назначения

| ID / статья | Что раскрыть | Пример (по умолчанию внутри статьи) | Продолжение / «хвост» |
|---|---|---|---|
| `CORE-NUMBERS` — **Числа и точность** | int/long overflow, floating point, NaN, infinity, BigDecimal (scale/equals/compareTo), Math | Код: 0.1 + 0.2 и BigDecimal.valueOf, округление денег | → DB-REL: DECIMAL; JVM-BYTECODE: IEEE 754 |
| `CORE-DATETIME` — **Дата и время (java.time)** | Instant/LocalDate/LocalDateTime/ZonedDateTime/Duration/Period, ZoneId, DST | Код: момент времени vs локальное время, переход часов | → DB-JDBC: TIMESTAMP; DIST-FAIL: распределённое время |
| `CORE-REGEX` — **Регулярные выражения** | Pattern/Matcher, escaping в строковом литерале, группы, find/matches, ограничение сложности | Код: разбор простого формата, экранирование слешей | → IO-BYTES; безопасность: ReDoS |
| `CORE-UTIL` — **Objects, Comparator и утилиты JDK** | Objects.*, Comparator composition, Arrays utilities, API contracts | Код: null-safe comparator и Objects.equals | → COL-SORTED; CORE-EQUALITY |
| `CORE-I18N` — **Locale и форматирование** | Locale, NumberFormat, DateTimeFormatter, интернационализация и сравнение с кодировкой | Код: отображение числа/даты для двух локалей | → CORE-DATETIME; IO-BYTES |

### 08. Метаданные, модули и динамика

| ID / статья | Что раскрыть | Пример (по умолчанию внутри статьи) | Продолжение / «хвост» |
|---|---|---|---|
| `CORE-ANNOTATIONS` — **Аннотации** | @Target/@Retention, repeatable/inherited, source/class/runtime, compile vs runtime processing | Код: своя runtime annotation + чтение | → CORE-REFLECTION; SPR-AOP; TEST-JUNIT |
| `CORE-REFLECTION` — **Reflection API** | Class, fields/methods/constructors, invocation, type erasure, security boundaries | Код: безопасный инспектор свойств без изменения private fields | → JVM-LOADING; CORE-MODULES |
| `CORE-MODULES` — **JPMS и пакеты как граница доступа** | module-info, requires/exports/opens, module path vs classpath | Код: два минимальных модуля с экспортированным API | → JVM-LOADING; SPR-DI: reflection |
| `CORE-SERIAL` — **Сериализация объектов и форматы данных** | Serializable, serialVersionUID, риски десериализации, почему часто нужны явные DTO/JSON | Небольшой разбор контрактов, не делать опасный публичный десериализатор | → IO-BYTES; SPR-WEB; безопасность десериализации |
| `CORE-CLASSLOADER` — **Class, ClassLoader и загрузка классов — обзор** | Class literals, Class.forName, classpath, контекстный loader без дублирования JVM internals | Код: узнать loader/название класса; факты загрузки через рефлексию | → JVM-LOADING: полная модель делегирования, linkage, initialization |

## Где действительно нужны отдельные Java-примеры

**Начальный набор сценариев** (не отдельный файл на каждую таблицу выше):

| Сценарий | Основные статьи | Что воспроизводит | Формат |
|---|---|---|---|
| Присваивание значений и передача параметров | CORE-TYPES, CORE-PARAMS, CORE-IMMUTABLE | Локальная ссылка, изменение объекта, повторное присваивание и защитная копия | Небольшой runnable example |
| Инициализация и полиморфизм | CORE-CLASSES, CORE-INIT, CORE-INHERITANCE, CORE-INTERFACES | Порядок создания объектов, выбор перегрузки/override, вызовы в конструкторе | Runnable example с выводом и объяснением |
| Равенство и коллекции | CORE-OBJECT, CORE-EQUALITY, COL-HASH | Потеря доступности изменённого ключа, контракт equals/hashCode | Самостоятельный пример + несколько JUnit проверок |
| Неизменяемый объект-значение | CORE-RECORDS, CORE-IMMUTABLE | Неглубокая неизменяемость record, defensive copies | Упражнение с тестами |
| Generics без магии | CORE-GENERICS, CORE-ERASURE | PECS, компиляторные ограничения, raw types и heap pollution | Несколько маленьких примеров; отрицательные компиляционные тесты — только если реально нужны |
| Обработка ошибок и закрытие ресурсов | CORE-EXCEPTIONS, CORE-RESOURCES | Причина исключения, подавленные исключения, порядок close | Runnable example + JUnit |
| Обработка данных через Stream | CORE-FUNCTIONS, CORE-STREAMS, CORE-STREAM-ADV | Ленивая цепочка, grouping, duplicate keys, side effects | Runnable example, факультативная практика |
| Числа и дата | CORE-NUMBERS, CORE-DATETIME | Денежное округление, разница локального времени и Instant, DST | Два небольших изолированных примера |
| Модель предметной области | CORE-ENUMS, CORE-SEALED, CORE-PATTERNS | Допустимые состояния, sealed Result и exhaustiveness | Runnable example |
| Метаданные и модули | CORE-ANNOTATIONS, CORE-REFLECTION, CORE-MODULES | Объявить и прочитать аннотацию, различить exports и opens | Маленький многомодульный пример, только после теории |

Отдельные исходники помещать рядом с упражнением или в `examples/` по необходимости; их точная структура не закрепляется до пилота. Каждый вынесенный пример снабжается ссылкой на все непосредственно необходимые статьи и имеет одну понятную команду запуска.

## Углубления, которые нельзя потерять

- **Семантика языка / JLS:** порядок вычисления выражений, совместимость методов, overload resolution, inference, covariance массивов, bridge methods и сигнатуры на уровне байткода. Базовые факты — в Java Core; формальная спецификация и байткод — через `JVM-BYTECODE`.
- **Объектная модель / JVM:** ссылки и достижимость, layout объектов, escape analysis, string pool, загрузка, linking и initialization классов. Java Core описывает наблюдаемое поведение, но не выдаёт детали HotSpot за гарантии языка.
- **Безопасность и корректность API:** mutable keys, defensive copies, serialization pitfalls, reflection/module access, regex complexity, API compatibility. Углубления раскрывать там, где они объясняют реальную ошибку.
- **Современная Java:** records, sealed types, switch patterns в Java 21, изменения API в следующих выпусках — только после проверки версии и статуса.
- **Взаимодействие с другими разделами:** JMM и safe publication, collections, HTTP/JSON/DTO, JDBC, Spring proxies/DI, streams vs parallelism, локали и кодировки. Их большие объяснения не дублировать здесь.

## Навигация и приоритет разработки

Для первой очереди разумно собирать **связные группы**, не все файлы сразу:

1. Типы → присваивание/приведение → передача параметров (`CORE-TYPES`, `CORE-CONVERSIONS`, `CORE-PARAMS`) — базовые ошибочные представления.
2. Класс → объект → инкапсуляция → наследование → интерфейс → композиция (`CORE-CLASSES`, `CORE-ACCESS`, `CORE-INHERITANCE`, `CORE-INTERFACES`, `CORE-COMPOSITION`) — самостоятельная модель объектов.
3. Контракты `Object`, строки, массивы, равенство, неизменяемость и `record` — база для Collections и Concurrent.
4. Исключения и ресурсы; generics и стирание — база для рабочего API.
5. Лямбды, Optional, Stream; современные языковые конструкции; дополнительные библиотеки стандартного API.
6. Аннотации, reflection, модули, сериализация — после предпосылок; продвинутые детали остаются в хвостах.

**Первая статья для реализации:** `CORE-TYPES` вместе с коротким примером «две переменные ссылаются на один объект». По мере публикации каждую строку этой карты уточнять: фактический путь, подтверждённый запуск, добавленные связи, а не формальная галочка «изучено».

## Критерий готовности будущей статьи

Читатель без преподавателя понимает кто что делает, что хранится/передаётся, какое правило действительно гарантирует Java, когда оно полезно, как воспроизвести пример и где продолжить тему. Термины — естественный русский; схемы компактны, подписаны и не заменяют объяснение. Ссылки ведут только к существующим локальным файлам.
