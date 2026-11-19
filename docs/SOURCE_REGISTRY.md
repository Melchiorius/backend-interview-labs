# Реестр внешних источников для базы знаний Java Backend

**Дата первичной проверки:** 2026-11-19. **Назначение:** постоянный каталог для планирования статей и аудита охвата интервью-вопросов. **Это не библиотека скопированных текстов:** хранить URL, назначение, версии, ограничения и собственные выводы, а не массово импортировать чужие ответы.

## Принцип доверия

- **A — спецификация или первичная документация продукта.** Определяет гарантии в области своей компетенции: JLS — язык, JVMS — виртуальная машина, Java API — библиотечные контракты, PostgreSQL — PostgreSQL, Spring — Spring. Производитель не определяет гарантии другого продукта.
- **B — авторитетное объяснение, гайд или техническая практика.** Помогает понять принцип и найти контрпример; не заменяет контракт.
- **C — банк вопросов и интервью-подборка.** Используется для поиска пропущенных тем, типовых уточнений и формата собеседования. Само наличие вопроса и готового ответа не доказывает ни корректность ответа, ни частоту появления на реальных интервью.

`CORE` = Java Core, `COL` = Collections, `CON` = Concurrency, `JVM` = Internals, `IO` = I/O & HTTP, `DB` = SQL/ORM, `SPR` = Spring, `DIST` = Distributed, `TEST` = Tooling/Testing, `ALG` = algorithms, `DESIGN` = System Design.

**Проверено:** существование/назначение основной части ресурсов по веб-поиску, некоторые — чтением страниц; это **не** полный контент-аудит каждого сайта, всех подпунктов, лицензий и актуальности каждого факта. Ссылки с пометкой `current` могут меняться и должны быть закреплены под конкретную версию статьи. Если URL переехал, искать канонический документ того же издателя и фиксировать замену в этом реестре.

## A. Нормативная и официальная документация

| ID / источник | Разделы | Для чего полезен | Уровень | Ограничения |
|---|---|---|---|---|
| `OFF-JLS21` — [Java Language Specification 21](https://docs.oracle.com/javase/specs/jls/se21/html/index.html) | CORE, CON | Контракты языка: типы, перегрузка, выражения, исключения, generics, JMM | A | Java 21; нормативный текст |
| `OFF-JVMS21` — [Java Virtual Machine Specification 21](https://docs.oracle.com/javase/specs/jvms/se21/html/index.html) | JVM, CORE | Классы, bytecode, загрузка, исполнение, memory areas | A | Java 21; отличать требования JVM от реализации HotSpot |
| `OFF-API21` — [Java SE 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/index.html) | CORE, COL, CON, IO, JVM | Контракты конкретных методов и классов JDK | A | Java 21; проверять страницу конкретного класса/метода |
| `OFF-JDK21` — [JDK 21 Specs & Tools](https://docs.oracle.com/en/java/javase/21/docs/specs/index.html) | JVM, TEST, IO | Инструменты диагностики, JFR, jcmd, команды JDK | A | Java 21 |
| `OFF-DEVJAVA` — [dev.java Learn](https://dev.java/learn/) | CORE, COL, CON, IO | Официальные поясняющие уроки и примеры | B | Уроки могут описывать новые версии; отдельно сверять с JDK 21 |
| `OFF-JVMTUNING` — [JDK 21 JVM guide](https://docs.oracle.com/en/java/javase/21/vm/index.html) | JVM | JVM/HotSpot, диагностика и поведение реализации | B | Вендорская реализация Oracle/HotSpot ≠ норма JVMS |
| `OFF-POSTGRES` — [PostgreSQL: Concurrency Control](https://www.postgresql.org/docs/current/mvcc.html) | DB, DIST | MVCC, изоляция, блокировки, аномалии транзакций | A | current = меняется; закрепить версию сервера и соответствующий URL |
| `OFF-PG-EXPLAIN` — [PostgreSQL: EXPLAIN](https://www.postgresql.org/docs/current/sql-explain.html) | DB | Планы запросов, статистика, EXPLAIN vs ANALYZE | A | current, зависимость планировщика от версии и статистики |
| `OFF-PG-TX` — [PostgreSQL: Transaction Isolation](https://www.postgresql.org/docs/current/transaction-iso.html) | DB | Repeatable Read, Serializable, retry при конфликтах | A | PostgreSQL semantics ≠ универсальный SQL стандарт |
| `OFF-SPRING` — [Spring Framework Reference](https://docs.spring.io/spring-framework/reference/) | SPR, TEST | DI, IoC, AOP proxies, transactions, MVC | A | перед написанием фиксировать Spring Framework версию |
| `OFF-BOOT` — [Spring Boot Reference](https://docs.spring.io/spring-boot/reference/) | SPR, TEST, IO | Auto-configuration, properties, management, tests | A | версия Boot зависит от проекта; /reference/ может обновляться |
| `OFF-HIBERNATE` — [Hibernate ORM Documentation](https://hibernate.org/orm/documentation/) | DB, SPR | JPA, persistence context, fetching, N+1, transactions | A | выбирать user guide конкретной версии; поведение Hibernate ≠ стандарт JPA |
| `OFF-HIBERNATE66` — [Hibernate ORM 6.6 User Guide](https://docs.hibernate.org/orm/6.6/userguide/html_single/) | DB, SPR | Зафиксированный пример версионной документации ORM | A | только когда речь действительно о Hibernate 6.6 |
| `OFF-HTTP` — [RFC 9110 HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110) | IO, DESIGN, SPR | Методы HTTP, идемпотентность, статусы, семантика запросов | A | нормативный HTTP, не только Spring |
| `OFF-CACHE` — [RFC 9111 HTTP Caching](https://www.rfc-editor.org/rfc/rfc9111) | IO, DESIGN, DIST | Кэширование HTTP, валидаторы, freshness | A | различать ограничения спецификации и поведение конкретного CDN |
| `OFF-MDN` — [MDN HTTP Request Methods](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods) | IO, DESIGN, SPR | Удобные таблицы методов, safe/idempotent/cacheable | B | пояснение; спорные гарантии подтверждать RFC |
| `OFF-KAFKA` — [Apache Kafka Design](https://kafka.apache.org/44/design/design/) | DIST, SPR | Delivery guarantees, offsets, consumer groups, failure cases | A | ссылка на Kafka 4.4; для других версий выбирать другую документацию |
| `OFF-JUNIT` — [JUnit 5.11.4 User Guide](https://docs.junit.org/5.11.4/user-guide/index.html) | TEST | JUnit Jupiter annotations, lifecycle, extension model, assertions | A | соответствует текущей заявленной версии JUnit 5.11.4 |
| `OFF-MOCKITO` — [Mockito Core Javadoc](https://www.javadoc.io/doc/org.mockito/mockito-core) | TEST | Mock, stub, spy, verify, limitations | A | выбирать установленную версию Mockito; javadoc.io может показывать latest |
| `OFF-GRADLE` — [Gradle User Manual](https://docs.gradle.org/current/userguide/) | TEST, TOOL | Tasks, multi-project, composites, build cache, source sets | A | current может отличаться от используемого Gradle Wrapper |
| `OFF-JMH` — [OpenJDK JMH](https://github.com/openjdk/jmh) | JVM, COL, ALG | Микробенчмарки и ограничения измерений | B | пример инструмента, не источник всех performance-гарантий |
| `OFF-TESTCONTAINERS` — [Testcontainers Guides](https://testcontainers.com/guides/) | TEST, DB, SPR | Проверяемые интеграционные сценарии с реальной БД/брокером | B | практические гайды; версию библиотеки и контейнера фиксировать |
| `OFF-DOCKER` — [Docker Documentation](https://docs.docker.com/get-started/) | TEST, DIST | Контейнеры, образы, сеть, Compose | A | проверять текущую версию/изменения инструментов |
| `OFF-K8S` — [Kubernetes Concepts](https://kubernetes.io/docs/concepts/) | TEST, DIST, DESIGN | Pods, Deployments, Services, rollout, probes | A | зависит от версии Kubernetes |
| `OFF-SRE` — [Google SRE Books](https://sre.google/sre-book/table-of-contents/) | DESIGN, DIST, TEST | SLO/SLI, мониторинг, отказоустойчивость, эксплуатация | B | практическая модель Google, не универсальный стандарт |
| `OFF-SRE-WB` — [Google SRE Workbook](https://sre.google/workbook/index/) | DESIGN, DIST, TEST | Инциденты, практические кейсы надёжности | B | практические рекомендации и примеры |
| `OFF-AWS-REL` — [AWS Well-Architected Reliability](https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/) | DESIGN, DIST | Ошибки распределённых систем, retry, recovery, SPOF | B | сильно ориентирован на AWS, переносимость оценивать |

## B. Подборки интервью-вопросов и проверка типичных ловушек

| ID / источник | Разделы | Для чего полезен | Уровень | Ограничения |
|---|---|---|---|---|
| `Q-TEAMLEAD` — [teamlead/java-interview-questions](https://github.com/teamlead/java-interview-questions) | CORE, COL, CON, JVM, DB, SPR | Большой банк вопросов; заявлена частотность на основе публичных собеседований | C | частотность и классификацию считать данными автора, не валидированной статистикой |
| `Q-CHERNYSHOV` — [Java Developer Interview Q&A 500+](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers) | CORE, COL, CON, JVM, IO, DB, SPR, DIST, TEST, DESIGN | Широкий охват backend, три уровня глубины | C | проверять полноту и версии каждого ответа |
| `Q-HRUDAY` — [Core Java Interview Question Bank](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List) | CORE, COL, CON, JVM | Большие списки вопросов по String, wrappers, OOP, generics | C | полезен для выявления забытых вопросов; не нормативен |
| `Q-IVAN` — [Community Java Interview Preparation](https://github.com/ivanjayasooriya/java-interview-prep) | CORE, COL, CON, JVM, TEST, DB | Группы вопросов, задачи, варианты тестов | C | не покрывает всю серверную архитектуру |
| `Q-IN28` — [in28minutes interview questions](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers) | CORE, COL, CON, JVM | Традиционные Java-подвохи и базовые вопросы | C | возможна устаревшая терминология, требует JDK 21 сверки |
| `Q-BAELDUNG-CORE` — [Baeldung Java Interview Questions](https://www.baeldung.com/java-interview-questions) | CORE, COL, JVM | Популярные вопросы о языке и API | C | отдельные статьи обновляются; ответы проверять по JLS/API |
| `Q-BAELDUNG-CLASS` — [Baeldung Java Class Structure and Initialization Interview Questions](https://www.baeldung.com/java-classes-initialization-questions) | CORE | Конструкторы, static/instance, initializer blocks и частые ловушки о классах | C | интервью-каталог, ответы сверять с JLS 21 §§8 и 12; не смешивать с нормативным источником |
| `Q-BAELDUNG-COL` — [Baeldung Java Collections Interview Questions](https://www.baeldung.com/java-collections-interview-questions) | COL, CORE | Контракты и внутренние механизмы коллекций | C | HashMap реализации различаются по версиям |
| `Q-BAELDUNG-CON` — [Baeldung Java Concurrency Interview Questions](https://www.baeldung.com/java-concurrency-interview-questions) | CON | Потоки, синхронизация, coordination, Executor | C | JMM-гарантии сверять со спецификацией |
| `Q-BAELDUNG-SPR` — [Baeldung Spring Interview Questions](https://www.baeldung.com/spring-interview-questions) | SPR | DI, AOP, transaction, MVC, lifecycle | C | не путать версии Spring и Spring Boot |
| `Q-BAELDUNG-BOOT` — [Baeldung Spring Boot Interview Questions](https://www.baeldung.com/spring-boot-interview-questions) | SPR, TEST | Auto-configuration, configuration, testing | C | классификация по вопросам без гарантии полноты |
| `Q-SYSTEM-PRIMER` — [System Design Primer](https://github.com/donnemartin/system-design-primer) | DESIGN, DIST, DB, IO | Большая карта системного дизайна, кейсы и типичные tradeoffs | C | важные части могут устаревать; не использовать как стандарт |
| `Q-BACKEND-2026` — [Backend Interview Questions 2026](https://github.com/alitalhacoban/backend-interview-questions-2026) | CORE, DB, SPR, DIST, DESIGN | Широкий банк с microservices и архитектурой | C | молодой ресурс; объём и качество проверять выборочно |
| `Q-CHERNYSHOV-RU` — [Русскоязычные вопросы Java Developer](https://github.com/Shell26/Java-Developer) | CORE, COL, CON, JVM, SPR | Русские формулировки вопросов, дополнительные идеи для поиска | C | проверять актуальность и не переносить ошибки |

## C. Практические и объясняющие источники

| ID / источник | Разделы | Для чего полезен | Уровень | Ограничения |
|---|---|---|---|---|
| `B-DEVJAVA-LANGUAGE` — [dev.java Language](https://dev.java/learn/language/) | CORE | Объяснения базового языка по темам | B | не всегда совпадает версия с 21 |
| `B-DEVJAVA-CLASSES` — [dev.java Classes and Objects](https://dev.java/learn/language/oop/classes-objects/) | CORE | Официальные объяснения классов, полей, методов, конструкторов и объектов | B | страница может учитывать более поздние версии; проверять спорные правила по JLS 21 |
| `B-MDN-CACHE` — [MDN HTTP Caching](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Caching) | IO, DESIGN | Практические объяснения кэширования HTTP | B | RFC выше по авторитету |
| `B-SPRING-GUIDES` — [Spring Guides](https://spring.io/guides) | SPR, TEST, DB | Небольшие runnable сценарии Spring | B | версии зависимостей проверять отдельно |
| `B-HIBERNATE-EXAMPLES` — [Hibernate Documentation Landing](https://hibernate.org/orm/documentation/) | DB, SPR | Разборы persistence contexts/fetching/examples | B | приоритет versioned user guide |

## Минимальный набор для каждого раздела (без повторного поиска с нуля)

| Раздел | Обязательная основа | Контроль пропусков / дополняющие источники |
|---|---|---|
| Java Core | OFF-JLS21, OFF-API21, OFF-DEVJAVA | Q-TEAMLEAD + Q-HRUDAY + Q-BAELDUNG-CORE; отдельно Q-CHERNYSHOV |
| Collections | OFF-API21 (Collection/Map/iterator), OFF-JLS21 (generics), OFF-DEVJAVA | Q-BAELDUNG-COL + Q-IN28 + Q-TEAMLEAD |
| Concurrency/JMM | OFF-JLS21 §17, OFF-API21 java.util.concurrent, OFF-JVMS21 при JVM утверждениях | Q-BAELDUNG-CON + Q-CHERNYSHOV + Q-TEAMLEAD |
| JVM | OFF-JVMS21, OFF-JVMTUNING, OFF-JDK21 | Q-TEAMLEAD + Q-CHERNYSHOV; OFF-JMH для измерений |
| I/O/HTTP | OFF-API21 (NIO/network), OFF-HTTP, OFF-CACHE, OFF-MDN | Q-CHERNYSHOV + Q-BACKEND-2026 |
| SQL/PostgreSQL/JPA | OFF-POSTGRES, OFF-PG-TX, OFF-PG-EXPLAIN, OFF-HIBERNATE (версионный user guide) | Q-CHERNYSHOV + Q-BACKEND-2026 + практические EXPLAIN/SQL кейсы |
| Spring | OFF-SPRING, OFF-BOOT (фиксировать версии), OFF-HIBERNATE для ORM | Q-BAELDUNG-SPR + Q-BAELDUNG-BOOT + Q-CHERNYSHOV |
| Distributed systems | OFF-KAFKA (по Kafka), OFF-HTTP, OFF-AWS-REL, OFF-SRE | Q-SYSTEM-PRIMER + Q-BACKEND-2026 + Q-CHERNYSHOV |
| Testing/Tooling | OFF-JUNIT, OFF-MOCKITO, OFF-GRADLE, OFF-TESTCONTAINERS | Q-CHERNYSHOV + Q-IVAN + практические контрпримеры |
| Algorithms/System Design | OFF-HTTP, OFF-AWS-REL, OFF-SRE, OFF-JMH для измерений | Q-SYSTEM-PRIMER + Q-BACKEND-2026; реальные design tradeoffs |

## Повторно используемый процесс работы

1. **Начать с карты** [CURRICULUM.md](CURRICULUM.md) и существующей статьи. Определить тему и соседние ID, проверить [COVERAGE.md](COVERAGE.md).
2. **Из банка вопросов C** выбрать **2–3 тематически разных источника**. Собрать уникальные вопросы/уточнения на русском своими словами; объединять синонимы, не считать «каждый вопрос = отдельная статья». Вопросы маркировать `[Q-ID]` с ссылкой на конкретное место первоисточника по возможности.
3. **Из A** сделать нормативную матрицу ответа: точная версия, метод/раздел, что гарантируется, что не гарантируется, исключения и условия. Из B добавить полезную мотивацию, код и производственные последствия.
4. **Контрпримеры:** для каждой спорной гарантии подготовить `what prints?`, `compile or not?`, `failure scenario?`, `trade-off?`; запуск кода **не заменяет** чтение контракта. Если вопрос упоминается только в C, пометить `не подтверждён как типичный`; не поднимать приоритет по одной подборке.
5. **Матрица покрытия** для статьи: `question_id | источник вопроса | нормативный источник | P0/P1/P2 | раздел ответа | контрпример | существующие ссылки | будущие хвосты | статус`. Статусы: `covered`, `mentioned`, `deferred`, `missing`, `disputed`. **P0 = covered** перед заявлением о достаточности статьи.
6. **Повторный аудит:** после текста ещё раз пройти независимую подборку, которой не пользовались при планировании. Иначе слепые зоны плана будут сохранены.
7. **Актуализация:** при смене Java/версионного фреймворка, обнаружении ошибки или перед выпуском большой группы статей пересматривать соответствующие записи; источник, который уже есть в реестре, повторно не разыскивать. Проверять ссылку и версию, а не механически повторять весь поиск.
8. **Лицензии/авторство:** ссылаться на первоисточники, формулировать объяснения самостоятельно; не копировать чужие банки вопросов целиком. Для сохранения текстов/файлов отдельно проверять лицензии и условия доступа.

## Подходящие задачи для следующего расширения реестра

- Зафиксировать **точные версии** используемых Spring Boot, Hibernate, Kafka, PostgreSQL, Gradle, Mockito и обновить A-ссылки при появлении полноценных runnable-проектов. Не угадывать версии по названию технологии.
- Создать **индекс уникальных вопросов** как отдельные компактные карточки или таблицу (ID, категория, факт присутствия в источнике, полный ответ где расположен), **не** переносить сюда чужие тексты.
- Провести **выборочную проверку качества** каждого C-источника на 10–20 вопросах: неточности по JLS/API, дубли, старые Java 8 примеры, глубина, полезность для Middle/Middle+.
- Расширить источники по SQL стандартам, security, observability, network internals и алгоритмам, когда начнётся соответствующий раздел. Текущий реестр — **базовая библиография**, а не доказательство абсолютной полноты.

## Проверка источников — политика реестра

Первичный поиск выполнен 2026-11-19. Веб-страницы различаются по времени публикации и могут изменяться. Упоминания числа вопросов (`500+`, `1715`) являются заявлениями авторов соответствующих репозиториев и не подтверждены независимым пересчётом. Не выводить из них точную вероятность вопроса на собеседовании.
