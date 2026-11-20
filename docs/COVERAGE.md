# Реестр покрытия и реализации курса

**Дата сверки:** 2026-11-20. **База:** фактический `main` @ `86e67b756395373b34cbd9c4c8a7c7435c7d090e` после публикации CORE-INHERITANCE через [PR #43](https://github.com/Melchiorius/backend-interview-labs/pull/43). Шесть статей Java Core опубликованы; исследовательский план CORE-INHERITANCE принят в PR #42, статья прошла G3/G4 до merge, G5 опубликован PR #43. Компактная Mermaid-схема Semaphore проверена пользователем в IntelliJ; реальная навигация и визуальное отображение **в Obsidian не проверялись**. Это живой реестр **материалов базы знаний**, не оценка личных знаний пользователя и не журнал учебной траектории.

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
| Maven migration PR #4 | **Closed without merge** | Gradle остаётся текущей системой сборки |
| Mockito | Included in standalone pilot exercise (PR #6); not a root-wide dependency | Explore broader tests when suitable |
| knowledge/ Obsidian vault | Статья Semaphore в main; Mermaid Preview **проверен пользователем в IntelliJ** | Отдельно проверить локальную навигацию в Obsidian |
| examples/, exercises/, solutions/ | `exercises/` существует для Semaphore; `examples/core/` содержит автономные JDK 21 демонстрации; отдельный `solutions/` не создавался | Reference kept in `src/reference/java` inside exercise |
| .run/ shared IntelliJ configs | Файлы созданы в PR #6; source roots исправлены в PR #7, пользователь подтвердил работу IDE с исходниками | Отдельно проверить запуск обеих конфигураций .run |
| Изолированные команды запуска exercise + student/reference | Эталон подтверждён в CI, `includeBuild` и три source set (`common`, `main`, `reference`) приняты в PR #7 | Студенческую команду проверить локально |
| CI matrix/sharding/distributed test execution | Planned | После локального пилота; не утверждать что есть |

## Тематическое покрытие (каталог в CURRICULUM.md)
| Раздел | Что есть | Пробелы/расширения | Следующая конкретная единица |
|---|---|---|---|
| 01 Java Core | Карта 42 возможных тематических единиц принята в PR #11. **Implemented в `main`: шесть статей** — [CORE-TYPES](../knowledge/01-java-core/types.md), [CORE-CONVERSIONS](../knowledge/01-java-core/conversions.md), [CORE-PARAMS](../knowledge/01-java-core/parameters.md), [CORE-CLASSES](../knowledge/01-java-core/classes.md), [CORE-ACCESS](../knowledge/01-java-core/access.md) и [CORE-INHERITANCE](../knowledge/01-java-core/inheritance.md). У каждой есть связанный Java 21 пример; для CORE-ACCESS и CORE-INHERITANCE опубликованы многофайловые примеры с изолированными отрицательными случаями. | P0-пробелы исходного аудита адресованы в трёх переработанных ранних статьях; это не означает абсолютную полноту для интервью. Остальные ID каталога не объявляются реализованными. Навигация в Obsidian ещё не проверена; CORE-INIT, CORE-OBJECT, CORE-INTERFACES и CORE-COMPOSITION остаются отдельными тематическими границами. | [Итог предыдущего аудита](JAVA_CORE_POST_REVIEW_2026-11-19.md); CORE-ACCESS опубликован PR #29, CORE-INHERITANCE — PR #43. Следующие содержательные приоритеты и смысл завершения Java Core определяет 00. |
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
| Статья Semaphore | CON-SEM | Реализована в main; текст и цветовая схема обновлены в PR #8/#9; компактный рендер проверен пользователем в IntelliJ, локальный Obsidian ещё не проверен | Термины, семантика, примеры, ловушки, источники и ссылки |
| Готовый пример | EXAMPLE-CON-SEM | Reference implementation in exercise; GitHub Actions reference job SUCCESS (2026-11-19, run 44518768253) | Код компилируется, запущен, объяснён |
| Самостоятельная задача | EX-CON-SEM-01 | Exercise committed in PR #6 | README, заготовка и реалистичные ограничения |
| JUnit + Mockito | TEST-CON-SEM | JUnit/Mockito tests executed successfully against reference in CI runs 44518768253 and 44519250051 | Проверены разрешения, ошибки, interruption; Mockito только для внешней зависимости |
| Эталон | SOL-CON-SEM-01 | Reference compiled and tests passed in CI run 44518768253 | Отдельно от student; проверен CI |
| Фокусированный Gradle/IDE запуск | RUN-CON-SEM | Nested Gradle project; isolated CI command passed; shared .run configs committed but IDE not verified | Одна команда/конфигурация; нет сборки независимых exercises |
| Интеграционная задача | MERGE-CON-01 | Planned (not required for pilot) | Сопоставление Semaphore/Lock/BlockingQueue/Executor |

## CORE-ACCESS — опубликовано после PR #29 (G6-сверка)

- **Implemented в `main`:** [статья](../knowledge/01-java-core/access.md), [пример и инструкция](../examples/core/access/README.md), [скрипт `verify.sh`](../examples/core/access/verify.sh), шесть positive Java-исходников в `examples/core/access/src/` (три `api`, три `client`) и восемь независимых negative Java-исходников в `examples/core/access/negative/`. Факт публикации проверен по squash-коммиту **`68a70d22308bbc944d57528a4ec5a8aa599d4644`** в `main`, а также по доступности статьи, README, скрипта и CI-конфигурации. Исследовательская матрица AC-01–AC-20 — в [плане](plans/CORE-ACCESS.md), принятом через PR #23.
- **История интеграции:** [PR #29](https://github.com/Melchiorius/backend-interview-labs/pull/29) объединён в `main`; исходные PR #26 и #27 закрыты **без отдельного merge**, поскольку их результаты включены в #29. Issues #24 и #25 закрыты. Для интегрированного PR роли 05 и 04 зафиксировали **G3 PASS / G4 FINAL PASS** на HEAD `ab64c5f62fffd196d5990df5137859ad0f3b9945` перед squash-merge. Не приравнивать завершение GitHub PR к самостоятельному доказательству полноты темы.
- **Verified — только технические проверки:** [GitHub Actions run #44612581812](https://github.com/Melchiorius/backend-interview-labs/actions/runs/44612581812) завершился **completed/success** для исходного интегрированного HEAD `ab64c5f62fffd196d5990df5137859ad0f3b9945`. Конфигурация CI включает Gradle/JUnit, standalone Java Core и `bash examples/core/access/verify.sh`, который компилирует/запускает positive пример и отдельно ожидает ошибки `javac` для восьми negative случаев. В исходном G4 причины всех восьми ошибок были проверены предметно; сам скрипт проверяет ненулевой код и наличие диагностики, а не её семантическую причину. Этот результат **не является новым независимым CI-запуском на squash SHA** и не доказывает все гарантии JLS.
- **Границы навигационной проверки:** по протоколу G4 на исходном HEAD проверены Markdown-ссылки **100/100** и пути Java Theory **14/14**; эти значения перенесены из результатов ревью и **не перепроверены заново** при текущей статусной редактуре. Визуальный просмотр и реальные переходы в **Obsidian/IntelliJ — not run**. Не утверждать, что локальный vault полностью проверен.
- **Остающийся объём Java Core:** публикация `CORE-ACCESS` не означает готовности всех 42 тематических единиц каталога. Приоритеты следующих тем утверждает 00; глубокие вопросы JPMS, наследования, reflection и других соседних тем не входят автоматически в доказанное покрытие CORE-ACCESS.

## CORE-INHERITANCE — опубликовано после PR #43 (G6-сверка)

- **Implemented в `main`:** [статья Java 21](../knowledge/01-java-core/inheritance.md), [README многофайлового примера](../examples/core/inheritance/README.md), [`verify.sh`](../examples/core/inheritance/verify.sh), три положительных исходника в `examples/core/inheritance/src/` и 12 отдельных отрицательных исходников в `examples/core/inheritance/negative/`. Включены также [общий README примеров](../examples/core/README.md), [исследовательский план](plans/CORE-INHERITANCE.md) и [ретроспективный аудит черновика](plans/CORE-INHERITANCE_POST_DRAFT_AUDIT.md). Наличие в `main` проверено по squash-коммиту **`86e67b756395373b34cbd9c4c8a7c7435c7d090e`** и фактическому Git tree.
- **История gates и интеграции:** G1 принят 00 после независимого G1-review PASS 04 по PR #42; G3 PASS роли 05 и G4 PASS роли 04 зафиксированы на едином предmerge HEAD **`be19d7cac5da78e4d0f3610df4199d23b4f9a49d`**. PR #43 объединён в `main` squash-коммитом **`86e67b756395373b34cbd9c4c8a7c7435c7d090e`**. PR #44 закрыт без самостоятельного merge, поскольку его код вошёл в #43; PR #46 был объединён только в ветку #43, не напрямую в `main`. Issues #38–#41 закрыты после публикации.
- **Verified — проверки до merge, не после него:** GitHub Actions [run #44667504650](https://github.com/Melchiorius/backend-interview-labs/actions/runs/44667504650) — `completed/success` на предmerge HEAD `be19d7cac5da78e4d0f3610df4199d23b4f9a49d`; run включает Gradle/JUnit, standalone Java Core, CORE-ACCESS и CORE-INHERITANCE positive/negative checks. В независимом G4-review 04 восстановил точные 16 файлов (3 positive, 12 negative и `verify.sh`) с совпадающими Git blob SHA и выполнил `bash examples/core/inheritance/verify.sh`: exit 0, восемь ожидаемых строк stdout и 12/12 отдельных отрицательных компиляций с ожидаемыми причинами. Отдельный компактный `Dispatch.java`, приведённый в статье, проверен как 6/6 ожидаемых строк. Эти результаты относятся к **предmerge HEAD**, а не к новому post-merge запуску.
- **Post-merge CI:** для squash SHA `86e67b756395373b34cbd9c4c8a7c7435c7d090e` GitHub Actions workflow runs не обнаружены; combined status также не содержит проверок. Поэтому post-merge CI **не заявляется**. В G6 не запускались локально Java/Gradle/JUnit и не выполнялась визуальная проверка Obsidian/IntelliJ.
- **Проверка локальной навигации в G6:** в восьми ключевых Markdown-документах (`README.md`, `docs/CURRICULUM.md`, `docs/COVERAGE.md`, оба плана CORE-INHERITANCE, статья, общий README примеров и README примера наследования) проверены **120 относительных Markdown-ссылок / 49 уникальных файловых целей**; все 49 целей существуют в текущем Git tree `main`. Это статическая проверка путей через GitHub, не проверка реальных кликов, рендера или vault в Obsidian/IntelliJ. Ранее G3 05 сообщил 150/150 ссылок в 11 документах, а G4 04 независимо сообщил 147/147 в своей выборке 11 документов; это отдельные выборки, их числа не суммируются.
- **Границы содержательного покрытия:** по G4-review IH-01–IH-14 (P0) проверены как объяснённые в статье; IH-15–IH-21 (P1) раскрыты в заявленной глубине. IH-22–IH-25 (P2) оставлены явными границами, а CORE-INIT, CORE-OBJECT, CORE-INTERFACES, CORE-COMPOSITION, generics/erasure, sealed/records и JVM bytecode/JIT не объявляются отдельными реализованными статьями. Публикация CORE-INHERITANCE не означает готовность всех 42 возможных тематических единиц Java Core.
- **Ограничение `verify.sh`:** проверка характерных английских фрагментов диагностики подходит к проверенному OpenJDK 21.0.12.1, но текст `javac` зависит от компилятора и локали и не является гарантией JLS. При смене окружения диагностику следует адаптировать, не скрывая реальную ошибку.

## Проверка содержательной полноты

- **Implemented** и **Verified** описывают наличие материала и факт технической проверки, но не означают «полное покрытие интервью-вопросов». Для такого заявления требуются предварительная матрица вопросов, исследование спецификации/интервью-источников и повторный аудит готовой статьи.
- Непокрытые P0 фиксировать явным статусом **Requires expansion**, даже если файл опубликован и пример прошёл CI. Счётчик статей и тестов не заменяет проверку полноты.
- [Ретроспективный аудит Java Core (2026-11-19)](JAVA_CORE_COVERAGE_AUDIT_2026-11-19.md) — текущий список содержательных пробелов и критерии доработки.

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
