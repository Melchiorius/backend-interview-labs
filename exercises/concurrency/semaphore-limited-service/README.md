# EX-CON-SEM-01 — ограниченный внешний сервис

**Теория:** [Semaphore](../../../knowledge/03-concurrency/semaphore.md) · **уровень:** Middle, нюансы Middle+.

## Условие

Интерфейс `Gateway` описывает внешний вызов `execute()`, который возвращает строку и может завершиться исключением.

Реализуйте класс `LimitedService`:

- `LimitedService(int maxConcurrent, Gateway gateway)` отклоняет `maxConcurrent <= 0` через `IllegalArgumentException` и `gateway == null` через `NullPointerException`.
- `String call() throws Exception` вызывает `gateway.execute()`, но **одновременно внутри `execute()` может находиться не более `maxConcurrent` вызовов**.
- При любом завершении `execute()` — нормальном или исключительном — полученное разрешение возвращается.
- Если поток прерван во время ожидания разрешения, `call()` завершается `InterruptedException`, не выполняя `execute()` и не возвращая разрешения, которого не получил.

**Нужные понятия:** `Semaphore.acquire/release` и `finally` разобраны в [статье](../../../knowledge/03-concurrency/semaphore.md). `InterruptedException` означает прерывание операции, которая допускает прерывание. Для тестов используются JUnit Jupiter (проверки результата), Mockito (подставной Gateway), `ExecutorService` (запуск рабочих задач) и `CountDownLatch` (ожидание наступления события). Отдельные подробные статьи о последних механизмах будут добавлены по мере развития базы.

## Файлы

- [Заготовка](src/main/java/edu/labs/semaphore/LimitedService.java)
- [Gateway — контракт внешней зависимости](src/common/java/edu/labs/semaphore/Gateway.java)
- [JUnit/Mockito-тесты](src/test/java/edu/labs/semaphore/LimitedServiceTest.java)
- [Эталонное решение](src/reference/java/edu/labs/semaphore/LimitedService.java) — можно открыть после собственной попытки

## Запуск конкретного задания

Из корня репозитория с **JDK 21**. Gradle Wrapper находится в корне; первая загрузка Gradle и зависимостей может требовать интернет.

**Windows — своя реализация:**

```powershell
.\gradlew.bat -p exercises/concurrency/semaphore-limited-service test
```

**Linux/macOS — своя реализация:**

```sh
./gradlew -p exercises/concurrency/semaphore-limited-service test
```

Заготовка пока **не реализована**, поэтому функциональные тесты должны завершаться ошибками. Не нужно менять тесты ради зелёного результата: реализуйте контракт в `src/main/java`.

**Эталон — те же тесты, но другая реализация (Windows):**

```powershell
.\gradlew.bat -p exercises/concurrency/semaphore-limited-service test -Preference=true
```

**Эталон (Linux/macOS):**

```sh
./gradlew -p exercises/concurrency/semaphore-limited-service test -Preference=true
```

Обе реализации зарегистрированы в Gradle как отдельные source sets: `main` — заготовка, `reference` — эталон. Параметр `-Preference=true` запускает те же тесты с эталонными классами вместо ученических. Другие лаборатории для этого запуска не компилируются, хотя нужные зависимости Gradle загрузит.

Если IntelliJ сообщает `Java file outside of source root`, после обновления проекта выполните **Gradle → Reload All Gradle Projects**: корневой `settings.gradle.kts` подключает упражнение через `includeBuild(...)`, а вложенный `build.gradle.kts` объявляет оба каталога Java как source sets. Ручная разметка папок не нужна.

Для IntelliJ в корне проекта есть общие конфигурации `.run/Semaphore - student tests.run.xml` и `.run/Semaphore - reference tests.run.xml`, без абсолютных путей. **Их фактический импорт в IDE ещё предстоит проверить**. Если IDE не подхватила их, используйте команды выше из терминала.

## Как интерпретировать тесты

Тесты проверяют аргументы конструктора, значение и делегирование, возврат разрешения после ошибки, ограничение одновременных обращений и прерывание ожидающего вызова. При неуспехе откройте отчёт JUnit и сопоставьте название теста с соответствующим контрактом выше и [объяснением семафора](../../../knowledge/03-concurrency/semaphore.md).

В конкурентном тесте потоки координируются через `CountDownLatch` и ограниченные ожидания. Даже многократный успешный тест **не доказывает корректность для всех возможных расписаний потоков**.

Это отдельный Gradle-проект с намеренно незавершённой заготовкой: стандартная корневая CI-сборка не должна запускать её как эталон.
