# Повторная проверка первых трёх статей Java Core

**Дата:** 2026-11-19. **Версия требований:** Java 21. **Исходный список пробелов:** [JAVA_CORE_COVERAGE_AUDIT_2026-11-19.md](JAVA_CORE_COVERAGE_AUDIT_2026-11-19.md). **Методика:** [AGENTS.md](../AGENTS.md#исследование-полноты-до-написания-статьи-обязательно). **Источники:** [SOURCE_REGISTRY.md](SOURCE_REGISTRY.md).

## Что было устранено

| Исходный P0 | Теперь где раскрыто | Чем проверено |
|---|---|---|
| TYPES: диапазоны и `char` | [CORE-TYPES](../knowledge/01-java-core/types.md#восемь-примитивных-типов) | Сверено с JLS 21 §4.2; UTF-16 пример Java 21 |
| TYPES: литералы и суффиксы | [CORE-TYPES](../knowledge/01-java-core/types.md#литералы-как-компилятор-понимает-записанное-число) | JLS 21 §3.10, отрицательные примеры компиляции |
| TYPES: категории и области видимости переменных | [CORE-TYPES](../knowledge/01-java-core/types.md#какие-бывают-переменные-и-почему-это-важно) | JLS 21 §§4.12 и 16; Java 21 демо |
| TYPES: `var` | [CORE-TYPES](../knowledge/01-java-core/types.md#var-тип-выводится-но-не-меняется) | JLS 21 §14.4, ограничения описаны |
| CONVERSIONS: контексты присваивания, вызова и cast | [CORE-CONVERSIONS](../knowledge/01-java-core/conversions.md#пять-контекстов-преобразований) | JLS 21 §5 |
| CONVERSIONS: константное сужение | [CORE-CONVERSIONS](../knowledge/01-java-core/conversions.md#константные-выражения-где-cast-не-требуется) | JLS 21 §5.2; runnable JDK 21 |
| CONVERSIONS: распаковка `Integer == int` | [CORE-CONVERSIONS](../knowledge/01-java-core/conversions.md#один-integer-другой-int-уже-другой) | JLS 21 §§5.1.8, 15.21; JDK 21 демо |
| CONVERSIONS: overload и `null` | [CORE-CONVERSIONS](../knowledge/01-java-core/conversions.md#как-преобразования-влияют-на-перегрузку) | JLS 21 §15.12; явно отделена неоднозначность компиляции |
| CONVERSIONS: переполнение до присваивания | [CORE-CONVERSIONS](../knowledge/01-java-core/conversions.md#числовое-продвижение-и-переполнение-до-присваивания) | JLS 21 §5.6; JDK 21 демо |
| PARAMS: массивы | [CORE-PARAMS](../knowledge/01-java-core/parameters.md#массивы-тоже-передаются-по-значению) | Java 21: изменение элемента vs переназначение |
| PARAMS: `String`, `Integer` | [CORE-PARAMS](../knowledge/01-java-core/parameters.md#неизменяемые-string-и-integer-объект-не-изменился) | Java 21: concat и unbox/rebox |
| PARAMS: `null` | [CORE-PARAMS](../knowledge/01-java-core/parameters.md#что-происходит-с-null) | Различие NPE во время выполнения и неоднозначности компиляции |

**Вывод:** все 12 тематических групп P0 из исходного аудита получили содержательные объяснения. Это не утверждение о том, что покрыты *все* возможные вопросы для собеседования, все главы JLS или что прошла внешняя экспертная оценка. Источники и адреса P0 находятся в [планах отдельных статей](plans/CORE-TYPES.md), [CORE-CONVERSIONS](plans/CORE-CONVERSIONS.md) и [CORE-PARAMS](plans/CORE-PARAMS.md).

## Что по-прежнему оставлено на расширение

| Тема | Осталось за границей этих статей | Где раскрывать |
|---|---|---|
| Типы | Более подробные scope/shadowing, `var` и лямбды, объектный жизненный цикл | `CORE-SYNTAX`, `CORE-FUNCTIONS`, `JVM-MEMORY` |
| Числа | IEEE 754, `BigDecimal`, `NaN`, `-0.0`, saturation, unsigned API | `CORE-NUMBERS` |
| Преобразования | Условный оператор `?:` с обёртками, overload при лямбдах и generics, bridge methods | `CORE-OPERATORS`, `CORE-GENERICS`, `CORE-ERASURE` |
| Строки | Строковые литералы, String Pool, интернирование и конкатенация в глубину | `CORE-STRINGS` |
| Параметры | Effectively final и захват переменных, generic varargs, dispatch/override, параметры и GC | `CORE-FUNCTIONS`, `CORE-GENERICS`, `CORE-INHERITANCE`, `JVM-MEMORY` |
| Общая практика | При запуске на машине читателя проверить markdown-ссылки, рендерер и JDK source-file mode | локальная проверка IntelliJ и Obsidian |

Эти **P1/P2-хвосты не считаются закрытыми** самим наличием упоминания. При появлении соответствующей статьи менять ID на действующую локальную ссылку и повторять аудит старых материалов.

## Проверки кода

- Локальный OpenJDK 21.0.12.1: `TypesAndVariablesDemo.java`, `ConversionsDemo.java`, `ParametersDemo.java` запускались независимо; каждый завершился `CHECK PASSED`.
- После каждого PR из серии GitHub Actions должен подтверждать работу всей сборки и накопленных Java-примеров из `examples/core/`.
- Для отрицательных примеров («не компилируется») синтаксически недопустимые вызовы оставлены **комментариями** в Markdown/Java, поэтому сборка не может доказать, что каждый такой случай действительно отвергается компилятором. Формальные ответы сверены с JLS 21. При необходимости позднее добавить отдельный набор compile-fail тестов, не мешающих основной сборке.
- Проверка всех страниц в Obsidian, точной компоновки в IntelliJ и независимый внешний литературный review на настоящий момент не проводились.

## Критерий следующих статей

Прежде чем создавать `CORE-CLASSES`, сначала составить план [по шаблону исследования](../AGENTS.md#исследование-полноты-до-написания-статьи-обязательно), взяв источники из [реестра](SOURCE_REGISTRY.md). Писать статью только после фиксации P0 и границ; тестировать нужные граничные случаи, а не каждый тривиальный пример. Успешный CI подтверждает исполняемость кода, но не абсолютную полноту учебного содержания.
