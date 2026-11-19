# CORE-CONVERSIONS — план исследования и сверка покрытия

**Дата:** 2026-11-19. **Основание:** [ретроспективный аудит](../JAVA_CORE_COVERAGE_AUDIT_2026-11-19.md); [реестр источников](../SOURCE_REGISTRY.md); JLS 21 §§5.1–5.6, 15.12, 15.21, 15.26; Java SE 21 API `Integer.valueOf` и `String.intern`. Перечни вопросов [teamlead/java-interview-questions](https://github.com/teamlead/java-interview-questions), [Baeldung Java Interview Questions](https://www.baeldung.com/java-interview-questions) использованы для поиска ожидаемых уточнений; ответы устанавливаются через JLS/API.

**Обещание статьи:** объяснить, почему преобразование типов зависит от **контекста**, что гарантируется и где возможна потеря информации/ошибка; уметь разобрать типичные `what prints?` и `compiles?` задачи. **Границы:** полный формальный вывод типов и generic overload относится к `CORE-GENERICS`/`CORE-ERASURE`; String Pool подробно — `CORE-STRINGS`; IEEE 754 и деньги — `CORE-NUMBERS`.

| Вопрос / сценарий | Приоритет | Где объясняется | Статус |
|---|---|---|---|
| Преобразования в присваивании, вызове, cast, числовом/строковом контексте | P0 | Пять контекстов; константные выражения | covered |
| `byte b = 67` vs `takeByte(67)`; `final int` vs `int` | P0 | Константные выражения | covered |
| Widening/narrowing: byte 300, long/double точность, NaN/Infinity | P0/P1 | Расширение и сужение | covered |
| `byte + byte`, `+=`, overflow before widening, целочисленное деление | P0 | Числовое продвижение | covered |
| Boxing/unboxing, `Integer.valueOf`, гарантии −128…127 | P0 | Boxing/unboxing, кэш Integer | covered |
| `Integer == Integer` vs `Integer == int`, `null` NPE | P0 | Один Integer, другой int | covered |
| Overload: `long` vs `Integer`; `null` и несвязанные ссылки; varargs | P0 | Как преобразования влияют на перегрузку | covered, сложные случаи deferred |
| Ссылочные downcast/upcast, ClassCastException | P1 | Преобразования ссылок | covered, подробности deferred |
| `Long x = 67` не компилируется | P1 | Boxing/unboxing | covered |
| String Pool, `intern()`, compile-time folding vs runtime concat | P1 | Строковый контекст | covered (полная тема deferred) |
| Generic overload/inference/bridge methods | P2 | Углубление + CORE-GENERICS/ERASURE | deferred |

**Контрпримеры и проверки:** `takeByte(67)`, `byte small = small + 1`, `accept(null)`, `Long wrong = 67` должны отвергаться компилятором; `Integer a = 1000; a == 1000` проходит благодаря распаковке; `Integer a = 127, b = 127; a == b` гарантированно true; в `long wrong = Integer.MAX_VALUE + 1` сначала происходит переполнение `int`; ссылка `new String("Java")` отличается от интернированной. Исполняемый [ConversionsDemo.java](../../examples/core/ConversionsDemo.java) запущен локально на OpenJDK 21.0.12.1, `CHECK PASSED`; CI дополнительно проверяет JDK 21.

**Coverage gate:** P0 перечислены и имеют собственное объяснение с кодом. Проверка IDE/Obsidian остаётся отдельной. Никакого утверждения о покрытии всей JLS главы 5, всех версий JDK или реальной вероятности собеседовательного вопроса.
