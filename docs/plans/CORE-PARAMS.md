# CORE-PARAMS — исследовательская матрица и итоговая проверка

**Дата:** 2026-11-19. **Объект:** `CORE-PARAMS` Java 21. **Контекст:** [общий аудит первых статей](../JAVA_CORE_COVERAGE_AUDIT_2026-11-19.md), [реестр внешних источников](../SOURCE_REGISTRY.md); JLS 21 §§4.12, 8.4.1, 15.7.4, 15.12.2, 15.12.4.2; [Baeldung Java Interview Questions](https://www.baeldung.com/java-interview-questions) (pass-by-value), [teamlead/java-interview-questions](https://github.com/teamlead/java-interview-questions) (смежные вопросы типов/объектов).

**Граница статьи:** гарантии передачи аргументов, локальных параметров, ссылок и массивов, возвращаемых значений, `null`, `varargs`, порядка вычисления и взаимодействия с перегрузкой. Объяснения потоко-безопасности, JMM, полного overload resolution, mutable/immutable модели объектов и GC — соседние темы.

| Содержание проверки | Приоритет | Раздел ответа | Статус |
|---|---|---|---|
| Разница аргумента и формального параметра; копирование значения | P0 | Что именно получает метод | covered |
| Примитив не меняет переменную вызывающего кода | P0 | Примитив | covered |
| Изменение общего mutable объекта vs переназначение параметра | P0 | Ссылка, swap | covered |
| Массив — объект; изменение элемента vs замена ссылки | P0 | Массивы | covered |
| String immutable: `s +=`, Integer immutable: `n++` | P0 | String и Integer | covered |
| `null`: передача, dereference, compile-time ambiguity | P0 | Что происходит с null | covered |
| `varargs`: создание массива при отдельных аргументах vs существующий массив | P1 | Varargs | covered |
| Аргументы вычисляются слева направо, до входа в метод | P1 | Порядок вычисления | covered |
| Перегрузка по compile-time типу vs dispatch по runtime объекту | P1 | Как выбирается перегрузка | covered, override deferred |
| Результат метода как способ заменить значение вызывающего кода | P1 | Как изменить значение | covered |
| `final` параметра / effectively final в лямбдах | P1 | final и хвост CORE-FUNCTIONS | covered / deferred |
| JVM stack/heap/GC, Concurrent publication | P2 | Углубления JVM-MEMORY, CON-RACE, CON-JMM | deferred |

**Контрпримеры:** `swap(a,b)` не заменяет ссылки вызывающего кода; `changeArray(arr)` меняет элемент, но `replaceArray(arr)` нет; `String` и `Integer` не мутируют на месте; `m(null)` не обязан бросать NPE, но может быть неоднозначен на этапе компиляции; перегрузка по `Object` выбирается из объявленного типа, даже если объект является `String`; аргументы с побочными эффектами оцениваются слева направо. Всё это воспроизводится в [ParametersDemo.java](../../examples/core/ParametersDemo.java) и разъясняется в статье.

**Проверка:** пример локально запущен с OpenJDK 21.0.12.1, `CHECK PASSED`; в GitHub Actions добавлен самостоятельный запуск примера. P0 покрыты, P1/P2 с явными границами и будущими ID без битых ссылок. Отображение в Obsidian и полный обзор всех банков вопросов остаются непроверенными; **не утверждать абсолютную полноту**.
