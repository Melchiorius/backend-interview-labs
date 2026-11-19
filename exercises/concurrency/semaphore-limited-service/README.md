# EX-CON-SEM-01 — ограниченный внешний сервис

**Практика к статье:** [Semaphore](../../../knowledge/03-concurrency/semaphore.md). **Рекомендуемая глубина:** Middle / Middle+.

## Задача

Есть интерфейс `Gateway`: вызов `execute()` выполняет одну внешнюю операцию и может завершиться исключением.

Реализуйте `LimitedService`, у которого:
- конструктор `LimitedService(int maxConcurrent, Gateway gateway)` отвергает `maxConcurrent <= 0` (`IllegalArgumentException`) и `null` gateway (`NullPointerException`);
- `String call() throws Exception` делегирует `gateway.execute()`, но одновременно допускает **не более `maxConcurrent`** вызовов;
- при исключении gateway разрешение возвращается;
- если поток прерывается во время ожидания разрешения, вызов завершается `InterruptedException` без выполнения gateway и без лишнего `release()`.

**Что изучаем дополнительно:** `InterruptedException` — способ сообщить об отмене ожидания; `finally` выполняется при нормальном и исключительном выходе; JUnit проверяет утверждения `assertEquals`, `assertThrows`, а Mockito предоставляет тестовый `Gateway`. В тестах применяются `CountDownLatch` (счётчик событий с ожиданием), `ExecutorService` (пул запуска задач) и таймауты, чтобы проверять одновременные вызовы. Более подробные статьи о них будут добавлены позднее.

## Где код

- [Заготовка](src/main/java/edu/labs/semaphore/LimitedService.java)
- [Контракт внешней зависимости](src/main/java/edu/labs/semaphore/Gateway.java)
- [JUnit/Mockito тесты](src/test/java/edu/labs/semaphore/LimitedServiceTest.java)
- [Эталон для самостоятельного сравнения](src/reference/java/edu/labs/semaphore/LimitedService.java)

## Запуск из корня репозитория

Требуется JDK 21. Gradle Wrapper уже находится в корне. Первая загрузка зависимостей может потребовать интернет.

**Windows — проверить свою заготовку (ожидаются красные тесты до решения):**

```powershell
.\gradlew.bat -p exercises/concurrency/semaphore-limited-service test
```

**Linux/macOS:**

```sh
./gradlew -p exercises/concurrency/semaphore-limited-service test
```

**Проверить эталон, не изменяя заготовку:**

```powershell
.\gradlew.bat -p exercises/concurrency/semaphore-limited-service test -Preference=true
```

На Linux/macOS замените `.\gradlew.bat` на `./gradlew`. Выбор `-Preference=true` переключает исходники на `src/reference/java`, оставляя те же тесты. В IntelliJ можно создать Gradle Run Configuration с указанной командой задачи. Эта задача — отдельный вложенный Gradle-проект, независимый от проверки других лабораторий.

## Что проверяется

Конструктор и делегирование, освобождение разрешения при исключении, ограничение числа одновременно выполняемых операций, реакция на прерывание ожидания. Тест на конкуренцию использует контролируемые барьеры, но не доказывает отсутствие всех возможных гонок при каждом расписании потоков. При красном тесте посмотрите текст assertion и вернитесь к [разделу о `acquire()` и `finally`](../../../knowledge/03-concurrency/semaphore.md).

**Важно:** заготовка специально бросает `UnsupportedOperationException`: это приглашение реализовать поведение. Основная CI-сборка корневого проекта не должна считать её корректной реализацией.
