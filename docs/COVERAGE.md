# Реестр покрытия и реализации курса

**Дата сверки:** 2026-11-19. **База:** `main` на момент создания ветки `docs/course-architecture-and-coverage`. Это живой реестр **материалов базы знаний**, не оценка личных знаний пользователя и не журнал учебной траектории.

## Легенда
- **Implemented**: файл/код существует в main.
- **Verified**: подтверждено успешной сборкой/тестами или прямой проверкой; указывать где и когда.
- **Discussed**: ранее объяснялось, но структурированный курс/практика не доказаны.
- **Planned**: задумано, в репозитории ещё отсутствует.
- **Unknown**: статус не проверен; НЕ приравнивать к отсутствию или изученности.

Колонки **Контент**, **Покрытие глубины (M/M+/S)**, **Пример**, **Упражнение**, **Тест**, **IDE** должны обновляться независимо. Не заводить персональный столбец «Освоение»: это вне компетенции репозитория. «Обсуждали» не означает, что материал создан.

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
| Mockito | Planned, not yet configured | Подключить в тестовых зависимостях во время пилота |
| knowledge/ Obsidian vault | Planned | Создать карту, первые статьи, проверить ссылки |
| examples/, exercises/, solutions/ | Planned | Пилот EX-CON-SEM-01; выбрать размещение эталонов |
| .run/ shared IntelliJ configs | Planned | Проверить переносимость на чистом clone |
| Изолированные команды запуска exercise + student/reference | Planned | Проверить Gradle dependency isolation, CI |
| CI matrix/sharding/distributed test execution | Planned | После локального пилота; не утверждать что есть |

## Тематическое покрытие (каталог в CURRICULUM.md)
| Раздел | Что есть | Пробелы/расширения | Следующая конкретная единица |
|---|---|---|---|
| 01 Java Core | Отдельные темы обсуждались, структурированный контент не подтверждён | generics, контракты, ООП, streams, exceptions, reflection | CORE-TYPES + CORE-GENERICS |
| 02 Collections | Обсуждались HashMap, ArrayList, ConcurrentHashMap; lab-каркас | статьи, runnable examples, задачи и тесты | COL-HASH |
| 03 Concurrency | SynchronizedCounter, BoundedBlockingQueue, JUnit; обсуждались JMM/volatile/locks/futures | Semaphore, CountDownLatch, CyclicBarrier, Phaser, RW locks, CAS nuances, ForkJoin, virtual threads, JMM-система | **CON-SEM + EX-CON-SEM-01**, затем CON-JMM |
| 04 JVM | lab-каркас, обсуждали GC/heap/stack | bytecode, classloading, JIT, профилировка | JVM-MEMORY |
| 05 I/O & Networking | Не подтверждено наличие учебных файлов | I/O, NIO, TCP/HTTP, backpressure | IO-BYTES |
| 06 Databases | Не подтверждено наличие учебных файлов | SQL, indexing, MVCC, transactions, ORM | DB-TX |
| 07 Spring | Не подтверждено наличие учебных файлов | DI, proxy/AOP, transactions, tests | SPR-DI |
| 08 Distributed Systems | Присутствуют в roadmap как System Design | Kafka, partial failures, outbox, retry | DIST-FAIL |
| 09 Testing & Tooling | JUnit тесты, Gradle и GitHub CI ранее успешно работали | Mockito, тест-дизайн, concurrency-tests, отдельные IDE run configs | TEST-JUNIT + TEST-MOCKITO |
| 10 Algorithms & Design | Есть roadmap System Design | системная декомпозиция, задачи, архитектурные кейсы | DESIGN-REQ |

## Пилотная вертикаль (пока НЕ реализована)
| Артефакт | ID | Статус | Критерий завершения |
|---|---|---|---|
| Статья Semaphore | CON-SEM | Planned | Термины, семантика, примеры, ловушки, источники и ссылки |
| Готовый пример | EXAMPLE-CON-SEM | Planned | Код компилируется, запущен, объяснён |
| Самостоятельная задача | EX-CON-SEM-01 | Planned | README, заготовка и реалистичные ограничения |
| JUnit + Mockito | TEST-CON-SEM | Planned | Проверены разрешения, ошибки, interruption; Mockito только для внешней зависимости |
| Эталон | SOL-CON-SEM-01 | Planned | Отдельно от student; проверен CI |
| Фокусированный Gradle/IDE запуск | RUN-CON-SEM | Planned | Одна команда/конфигурация; нет сборки независимых exercises |
| Интеграционная задача | MERGE-CON-01 | Planned | Сопоставление Semaphore/Lock/BlockingQueue/Executor |

## Глубина и расширение
- Для каждого ID из CURRICULUM.md важно отслеживать, есть ли **содержательное покрытие M, M+ и S**, а не «сколько человек прошёл». Отсутствующий специализированный слой — открытый слот развития, а не блокер базовой статьи.
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
