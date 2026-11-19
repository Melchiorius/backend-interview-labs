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

**Java Core:** [Типы и значения](knowledge/01-java-core/types.md) (подробная статья с [проверяемым примером](examples/core/TypesAndVariablesDemo.java)), [Преобразования типов](knowledge/01-java-core/conversions.md) ([самопроверка](examples/core/ConversionsDemo.java)), [Передача аргументов](knowledge/01-java-core/parameters.md) ([самопроверка](examples/core/ParametersDemo.java)), [Классы и объекты](knowledge/01-java-core/classes.md) ([проверяемый пример](examples/core/ClassesAndObjectsDemo.java)), [Модификаторы доступа и инкапсуляция](knowledge/01-java-core/access.md) ([многофайловый Java 21 пример](examples/core/access/README.md)). Общий [запускаемый пример](examples/core/README.md) не требует Gradle: `java examples/core/ValuesAndReferencesDemo.java` (JDK 21).

**Первый готовый материал по многопоточности:** [Semaphore — теория](knowledge/03-concurrency/semaphore.md) и [упражнение с JUnit/Mockito](exercises/concurrency/semaphore-limited-service/README.md). Эталон прошёл отдельные тесты GitHub Actions. Исходники и компактная диаграмма проверены пользователем в IntelliJ. Запуск общих `.run`-конфигураций и отображение в Obsidian остаются отдельными проверками. Отсутствие других статей не означает отсутствие тем в каталоге.

Не размещайте в репозитории секреты, пароли или конфиденциальные рабочие данные.
