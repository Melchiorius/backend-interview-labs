# Реестр покрытия и реализации курса

**Дата сверки:** 2026-11-19. **База:** состояние после PR #6; факт CI проверки указан отдельно от неподтверждённого локального UX. Это живой реестр **материалов базы знаний**, не оценка личных знаний пользователя и не журнал учебной траектории.

## Легенда
- **Implemented**: файл/код существует в main.
- **Verified**: подтверждено успешной сборкой/тестами или прямой проверкой; указывать где и когда.
- **Discussed**: ранее объяснялось, но структурированный курс/практика не доказаны.
- **Planned**: задумано, в репозитории ещё отсутствует.
- **Unknown**: статус не проверен; НЕ приравнивать к отсутствию или изученности.

Учёт нужен для **фактических материалов**, а не для подсчёта формально заполненных полей. По реально начатой теме отмечать: существующую статью и её рабочую ссылку, полезную глубину (где применимо), крупные примеры/упражнения (если нужны), проверку запуска и известные пробелы. Не требовать отдельных примера, теста, IDE-конфигурации или трёх уровней от каждой темы. Личный статус «Освоено» не вести; «обсуждали» не означает, что файл создан.

## Подтверждено в существующем проекте
| Компонент | Реальный статус | Дальнейшее действие |
|---|---|---|
| AGENTS.md, README.md, docs/ROADMAP.md, docs/STUDY_METHOD.md, docs/TOPIC_TEMPLATE.md, docs/PROGRESS.md | Implemented | Согласовать с новой архитектурой и поддерживать |
| Java 21, Gradle multi-project: labs/concurrency, collections, jvm, async | Implemented | Не переделывать ради архитектурной красоты |
| `SynchronizedCounter` и `BoundedBlockingQueue` | Implemented | Связать с теорией, примерами и exercise |
| JUnit Jupiter 5.11.4 и тесты двух классов | Implemented; ранее четыре теста успешно прошли в CI и CLI | Расширить тестирование и учебные объяснения |
| Gradle Wrapper 8.8 | Implemented (после PR #3) | Проверять доступность на чистом clone |
| Gradle CLI и IntelliJ Sync после переустановки IDE | Verified пользователем в ноябре 2026 | Не смешивать с проверкой будущих упражнений |
| Maven migration PR #4 | **OPEN, not merged** | Не включать в курс; судьба PR отдельно решается владельцем |
| Mockito | Included in standalone pilot exercise (PR #6); not a root-wide dependency | Explore broader tests when suitable |
| knowledge/ Obsidian vault | Pilot Semaphore Markdown article exists in PR #6; Obsidian UI not verified | Check local navigation in Obsidian |
| examples/, exercises/, solutions/ | `exercises/` exists for Semaphore pilot; standalone `examples/` and `solutions/` not created | Reference kept in `src/reference/java` inside exercise |
| .run/ shared IntelliJ configs | Implemented for pilot in PR #6; IDE unverified | Проверить переносимость на чистом clone |
| Изолированные команды запуска exercise + student/reference | Reference Gradle command verified in GitHub Actions; student run pending local check | Gradle source roots fixed in PR #7 with composite includeBuild + permanent reference source set; verify IntelliJ reload locally |
| CI matrix/sharding/distributed test execution | Planned | После локального пилота; не утверждать что есть |

## Тематическое покрытие (каталог в CURRICULUM.md)
| Раздел | Что есть | Пробелы/расширения | Следующая конкретная единица |
|---|---|---|---|
| 01 Java Core | Отдельные темы обсуждались, структурированный контент не подтверждён | generics, контракты, ООП, streams, exceptions, reflection | CORE-TYPES + CORE-GENERICS |
| 02 Collections | Обсуждались HashMap, ArrayList, ConcurrentHashMap; lab-каркас | статьи, runnable examples, задачи и тесты | COL-HASH |
| 03 Concurrency | SynchronizedCounter, BoundedBlockingQueue, JUnit; обсуждались JMM/volatile/locks/futures | CountDownLatch, CyclicBarrier, Phaser, RW locks, CAS nuances, ForkJoin, virtual threads, JMM-система | **CON-SEM + EX-CON-SEM-01 implemented in PR #6**, затем CON-JMM |
| 04 JVM | lab-каркас, обсуждали GC/heap/stack | bytecode, classloading, JIT, профилировка | JVM-MEMORY |
| 05 I/O & Networking | Не подтверждено наличие учебных файлов | I/O, NIO, TCP/HTTP, backpressure | IO-BYTES |
| 06 Databases | Не подтверждено наличие учебных файлов | SQL, indexing, MVCC, transactions, ORM | DB-TX |
| 07 Spring | Не подтверждено наличие учебных файлов | DI, proxy/AOP, transactions, tests | SPR-DI |
| 08 Distributed Systems | Присутствуют в roadmap как System Design | Kafka, partial failures, outbox, retry | DIST-FAIL |
| 09 Testing & Tooling | JUnit тесты, Gradle и GitHub CI ранее успешно работали | Mockito, тест-дизайн, concurrency-tests, отдельные IDE run configs | TEST-JUNIT + TEST-MOCKITO |
| 10 Algorithms & Design | Есть roadmap System Design | системная декомпозиция, задачи, архитектурные кейсы | DESIGN-REQ |

## Пилотная вертикаль — Semaphore (PR #6)
| Артефакт | ID | Статус | Критерий завершения |
|---|---|---|---|
| Статья Semaphore | CON-SEM | Существует в main; ясность русских объяснений и схема переработаны в отдельном PR, визуальная проверка Obsidian ещё нужна | Термины, семантика, примеры, ловушки, источники и ссылки |
| Готовый пример | EXAMPLE-CON-SEM | Reference implementation in exercise; GitHub Actions reference job SUCCESS (2026-11-19, run 44518768253) | Код компилируется, запущен, объяснён |
| Самостоятельная задача | EX-CON-SEM-01 | Exercise committed in PR #6 | README, заготовка и реалистичные ограничения |
| JUnit + Mockito | TEST-CON-SEM | JUnit/Mockito tests executed successfully against reference in CI runs 44518768253 and 44519250051 | Проверены разрешения, ошибки, interruption; Mockito только для внешней зависимости |
| Эталон | SOL-CON-SEM-01 | Reference compiled and tests passed in CI run 44518768253 | Отдельно от student; проверен CI |
| Фокусированный Gradle/IDE запуск | RUN-CON-SEM | Nested Gradle project; isolated CI command passed; shared .run configs committed but IDE not verified | Одна команда/конфигурация; нет сборки независимых exercises |
| Интеграционная задача | MERGE-CON-01 | Planned (not required for pilot) | Сопоставление Semaphore/Lock/BlockingQueue/Executor |

## Как избегать ложного статуса
- `CURRICULUM.md` отвечает на вопрос **«какие знания должны быть доступны в принципе?»**; этот файл — **«что уже существует и что стоит дополнить?»**. Полезная статья может быть опубликована до создания всех связанных тем. При появлении новых статей возвращаться к старым и добавлять ссылки; статус связей обновлять по факту.
- На раннем этапе достаточно сводки по разделам и отдельных строк для реально выполняемых работ (как пилот). При появлении десятков статей можно развернуть список по ID, **без обязательной строки на каждую пока не начатую подтему**. Главное — не путать отсутствие строки с подтверждённой готовностью.
- Неработающие пути не публиковать как действующие ссылки. Если отдельного материала нет, уместно краткое объяснение в текущей статье и запись о пробеле здесь.

## Глубина и расширение
- Для созданного материала отмечать **действительно раскрытую глубину**, а не автоматически требовать M, M+ и S для каждого ID. Отсутствие факультативной специализированной детали — возможное будущее расширение, не блокер базового текста.
- Наличие `M` не доказывает наличия `M+`; наличие кода не доказывает раскрытия теории. По мере разработки детализация идёт до каждого идентификатора и каждого слоя.
- Полезная редкая тема сохраняется в каталоге даже если пока существует только название и предложение по содержанию. Не сокращать карту ради ощущения завершённости.
- Реестр хранит **состояние знаний как контента** и инфраструктуры; частные результаты учеников в репозиторий не записываются.

## Регламент обновления
При каждом PR с учебным содержанием указывать:
1. Основание — ID темы/задачи и зависимости.
2. Файлы теории, runnable example, exercise, solution, tests, .run, ссылки.
3. Фактические команды и результаты тестов (если не запускались — **not run**).
4. Открытые вопросы и следующую интеграционную задачу.
5. Изменение текущего статуса в этом реестре и при необходимости в PROGRESS.md.
6. Изменения глубины охвата — отдельно для M/M+/S; не хранить личные оценки освоения в курсе.

Не объявлять материал «реализованным» только из-за наличия строки в каталоге.
