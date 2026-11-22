# Backend Interview Labs — база знаний Java Backend

Самостоятельная база знаний по Java и backend-разработке: точные, компактные Markdown-статьи со схемами, короткими фрагментами кода и, где это необходимо, большими Java-примерами и упражнениями с JUnit/Mockito. Материалы рассчитаны на использование **без преподавателя или ИИ**.

## Как пользоваться
1. Выберите тему в [каталоге](docs/CURRICULUM.md) либо посмотрите [необязательные маршруты для нескольких проходов](docs/ROADMAP.md).
2. Прочитайте основное определение и объяснение. При необходимости перейдите по ссылке к связанному понятию; дополнительные Obsidian-callout и специализированные статьи можно оставить на потом.
3. Короткий код уже находится в статье. Большой пример или задача, если они созданы, доступны по ссылке.
4. Для самостоятельной задачи откройте её README и запустите **только её** тесты указанной командой. Вернитесь к теории и уточните непонятное.

**Откройте корневую директорию клонированного репозитория как Obsidian vault.** Навигация ведёт к локальным файлам этой же версии проекта; GitHub служит только для хранения версий, PR и получения обновлений. Диаграммы Mermaid в Markdown Preview IntelliJ отображаются при включённом плагине Mermaid (Settings → Plugins); если вместо схемы виден блок `flowchart`, проверьте этот плагин. Компактный вариант схемы Semaphore проверен в IntelliJ; отображение в Obsidian и переходы между локальными файлами проверяются отдельно.

## Текущая техническая база
- Java 21; основной `main` на момент сверки использует Gradle multi-project.
- JUnit Jupiter уже используется в начальных лабораториях; Mockito подключён в отдельном пилотном упражнении PR #6; в корневых модулях не объявляется обязательной зависимостью.
- Исходный код известных лабораторий: `labs/concurrency/`, `labs/collections/`, `labs/jvm/`, `labs/async/`.
- Для первых лабораторий тесты запускаются через Gradle Wrapper (из корня репозитория):

Windows:
```powershell
.\gradlew.bat :labs:concurrency:test
```

macOS / Linux:
```sh
./gradlew :labs:concurrency:test
```

JDK 21 требуется. Первая загрузка Gradle/библиотек может требовать интернет. Эти команды относятся к модулю `labs:concurrency`; самостоятельное упражнение Semaphore запускается по отдельной инструкции ниже. Их актуальную работоспособность на чистом клоне нужно проверять отдельно.

## Где что искать
- [CURRICULUM.md](docs/CURRICULUM.md) — карта всех запланированных тем.
- [COVERAGE.md](docs/COVERAGE.md) — реально существующие материалы и пробелы.
- [SOURCE_REGISTRY.md](docs/SOURCE_REGISTRY.md) — каталог проверенных отправных точек: спецификации, документация и банки вопросов для аудита полноты будущих статей.
- [ROADMAP.md](docs/ROADMAP.md) — рекомендуемые первый, второй и последующие проходы.
- [STUDY_METHOD.md](docs/STUDY_METHOD.md) — короткая справка самостоятельному читателю.
- [AGENTS.md](AGENTS.md) — требования для авторов и ассистентов.
- [ROLE_MODEL.md](docs/ROLE_MODEL.md) и [WORKFLOW.md](docs/WORKFLOW.md) — организация работы ролей 00–06 и последовательность проверки материалов.
- [Стартовые инструкции для ролей 00–06](docs/roles/README.md) — отдельные тексты для запуска новых чатов с первыми поручениями.
- [EXERCISE_AND_TESTING_STANDARD.md](docs/EXERCISE_AND_TESTING_STANDARD.md) — будущая независимая проверка упражнений.

**Java Core:** [Типы и значения](knowledge/01-java-core/types.md) (подробная статья с [проверяемым примером](examples/core/TypesAndVariablesDemo.java)), [Преобразования типов](knowledge/01-java-core/conversions.md) ([самопроверка](examples/core/ConversionsDemo.java)), [Передача аргументов](knowledge/01-java-core/parameters.md) ([самопроверка](examples/core/ParametersDemo.java)), [Классы и объекты](knowledge/01-java-core/classes.md) ([проверяемый пример](examples/core/ClassesAndObjectsDemo.java)), [Модификаторы доступа и инкапсуляция](knowledge/01-java-core/access.md) ([многофайловый Java 21 пример](examples/core/access/README.md)), [Наследование и переопределение](knowledge/01-java-core/inheritance.md) ([многофайловый Java 21 пример](examples/core/inheritance/README.md)), [Порядок инициализации](knowledge/01-java-core/initialization.md) ([автономный Java 21 пример](examples/core/initialization/README.md)). Общий [запускаемый пример](examples/core/README.md) не требует Gradle: `java examples/core/ValuesAndReferencesDemo.java` (JDK 21).

**Объекты и равенство Java 21:** [java.lang.Object, identity и основные методы](knowledge/01-java-core/object.md); [equals/hashCode, контракты и коллекции](knowledge/01-java-core/equality.md) с [воспроизводимым Java-примером](examples/core/equality/README.md).

**Интерфейсы и композиция Java 21:** [интерфейсные контракты и `default`-методы](knowledge/01-java-core/interfaces.md), [композиция, делегирование и выбор архитектуры](knowledge/01-java-core/composition.md) с [общим многофайловым примером](examples/core/interfaces-composition/README.md).

**Вложенные классы и функциональные выражения Java 21:** [static nested, inner, local и anonymous классы](knowledge/01-java-core/nested-classes.md), [SAM, лямбды и ссылки на методы](knowledge/01-java-core/functions.md) с [одним общим запускаемым примером](examples/core/nested-functions/README.md) и сравнением 17 наблюдений.

**Обобщённые типы и стирание Java 21:** [параметры типов, инвариантность, PECS и wildcard](knowledge/01-java-core/generics.md), [стирание типов, raw/unchecked и проверки при выполнении](knowledge/01-java-core/erasure.md) с [одним 18-строчным дидактическим runnable](examples/core/generics-erasure/README.md).

**Исключения и управление ресурсами Java 21:** [иерархия `Throwable`, checked/unchecked, распространение и `finally`](knowledge/01-java-core/exceptions.md), [try-with-resources, порядок закрытия и suppressed-исключения](knowledge/01-java-core/resources.md) с [единым 20-строчным запускаемым примером](examples/core/exceptions-resources/README.md).

**Stream API Java 21:** [источники, промежуточные и терминальные операции, Optional](knowledge/01-java-core/streams.md), [редукции, Collector, parallel и закрытие Stream](knowledge/01-java-core/stream-advanced.md), а также [один воспроизводимый пример с двумя Java-программами](examples/core/streams/README.md) — 44 наблюдения P01–P33 и C01–C11 за два запуска.

**Массивы, строки и буферы Java 21:** [типы массивов, ковариантность, копирование и `Arrays.asList`](knowledge/01-java-core/arrays.md), [String, UTF-16, Unicode и StringBuilder/StringBuffer](knowledge/01-java-core/strings.md) с [общим запускаемым примером на двух Java-программах](examples/core/arrays-strings/README.md) — A01–A24 и S01–S28 (52 наблюдения за два запуска).

**Record и неизменяемость в Java 21:** [компоненты record, конструкторы, `equals` и copy invariant](knowledge/01-java-core/records.md), [владение изменяемым состоянием, defensive copy, `List.copyOf` и live view](knowledge/01-java-core/immutability.md) с [общим запускаемым примером Records + Immutable](examples/core/records-immutability/README.md) — R01–R16 и I01–I10 (26 наблюдений двух запусков).

**Sealed types и pattern matching Java 21:** [разрешённые прямые подтипы, `permits`, `final/sealed/non-sealed`](knowledge/01-java-core/sealed.md) и [flow scoping, `switch`, `when`, record patterns и `MatchException`](knowledge/01-java-core/patterns.md). [Общий запускаемый пример Sealed + Patterns](examples/core/sealed-patterns/README.md) содержит два полноценных Java 21 исходника G1: S01–S11 (11 строк) и P01–P20 (20 строк) — 31 наблюдение двух процессов, не 31 JUnit-тест.

**Enums и управление потоком Java 21:** [enum, конструкторы, `name/ordinal/toString`, идентичность констант и `EnumSet`/`EnumMap`](knowledge/01-java-core/enums.md), [условия, циклы, метки, `switch` и `yield`](knowledge/01-java-core/control-flow.md) и [общий запускаемый пример](examples/core/enums-control/README.md). Две программы дают 30+21 подписанную строку вывода двух отдельных JVM-процессов, а не 51 JUnit-тест.

**Синтаксис и операторы Java 21:** [структура исходного файла, пакеты, импорты, области видимости и `var`](knowledge/01-java-core/syntax.md) и [приоритет, порядок вычисления, `++`/`+=`, переполнение, сдвиги, булевы и условные операторы](knowledge/01-java-core/operators.md). Самостоятельные статьи с матрицей 26+30 вопросов по принятому G1; четыре G1-исходника дают 14+27+3 наблюдения трёх Java-процессов при отдельном запуске, не 44 JUnit-теста. Опубликован [общий runnable Syntax + Operators](examples/core/syntax-operators/README.md): четыре Java 21 исходника, три stdout-эталона и проверка 14+27+3 строк трёх JVM-процессов, а не 44 JUnit-теста. Запуск: `bash examples/core/syntax-operators/verify.sh`.

**Первый готовый материал по многопоточности:** [Semaphore — теория](knowledge/03-concurrency/semaphore.md) и [упражнение с JUnit/Mockito](exercises/concurrency/semaphore-limited-service/README.md). Эталон прошёл отдельные тесты GitHub Actions. Исходники и компактная диаграмма проверены пользователем в IntelliJ. Запуск общих `.run`-конфигураций и отображение в Obsidian остаются отдельными проверками. Отсутствие других статей не означает отсутствие тем в каталоге.

Не размещайте в репозитории секреты, пароли или конфиденциальные рабочие данные.
