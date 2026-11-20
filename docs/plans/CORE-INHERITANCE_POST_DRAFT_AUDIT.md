# CORE-INHERITANCE — ретроспективная проверка покрытия G2

**Дата:** 20.11.2026. **Роль:** 02 — Knowledge Author. [Исследовательский план](CORE-INHERITANCE.md) / Issue #37 / merged PR #42. [Черновик статьи](../../knowledge/01-java-core/inheritance.md) / Issue #38. Версия: Java SE 21.

**Это самопроверка после черновика, не независимый verdict 04 и не публикация.** P0/P1/P2 — приоритет раскрытия темы, а не оценка кандидата.

## P0: точное соответствие IH-01–IH-14

| ID | Раздел статьи | Собственный ответ и проверяемый случай |
|---|---|---|
| IH-01 | «Иерархия, extends и границы наследования» | Один прямой superclass; Object без superclass, обычный класс без extends наследует Object; private/package members, конструкторы не наследуются |
| IH-02 | «Одна переменная — два важных типа» | Upcast `Document ref = new Report()`, declared/runtime type, нельзя вызвать только Sub-метод через Base |
| IH-03 | «Overloading и overriding — различие» | Signature/subsignature, return-only overload — ошибка |
| IH-04 | «Главный пример» | `Base ref = new Derived()`, выбранный метод экземпляра исполняет override `derived:object` |
| IH-05 | «Главный пример» | `ref.describe("X")` vs `((Derived)ref).describe("X")` → `derived:object/derived:string`, overload compile-time |
| IH-06 | «Почему static методы не переопределяются» | `Base.kind()/Derived.kind()` → `base:static/derived:static`; static/instance conflict — ошибка |
| IH-07 | «Поля тоже не переопределяются» | `ref.code` и `((Derived)ref).code` → `67/68`; static/instance field hiding |
| IH-08 | «this, super и приведение» | `super.describe("X")` → `base:object`; cast не отключает override; `super.value` → 67 |
| IH-09 | «private и package-private» | Private метод Base исполняется из Base; package-private Base в другом пакете не override |
| IH-10 | «final» | Нельзя extends final class, override final instance и скрыть final static |
| IH-11 | «Нельзя уменьшать доступность» | protected → public допустимо, protected → package-private запрещено |
| IH-12 | «Ковариантный возвращаемый тип» | Number → Integer возможно; Object вместо Number запрещён; result-only overload запрещён |
| IH-13 | «Checked throws» | IOException → FileNotFoundException допустимо, обратная замена запрещена; unchecked отдельно |
| IH-14 | «@Override: проверка, а не переключатель» | Несовпадающий параметр в `@Override` отвергается, корректный override не зависит от аннотации |

**Итог авторской сверки:** 14/14 P0 с собственными объяснениями, механизмом и показанным примером/контрпримером. Это не утверждение о полной независимой нормативной проверке; 04 может выявить уточнения.

## P1: достаточное собственное раскрытие, не фиктивные ссылки

| ID | Раздел | Что установлено и где граница |
|---|---|---|
| IH-15 | «abstract и интерфейсы» | Abstract классы, реализация, abstract final — ошибка; детали CORE-INTERFACES |
| IH-16 | «Перегрузки в наследовании и неоднозначный null» | null при String/Integer — ambiguity; явный cast разрешает |
| IH-17 | «private и package-private» | Вызов private Base не меняется при таком же публичном методе Child |
| IH-18 | «Опасность виртуального вызова из конструктора» | Вызов override до инициализации поля Sub печатает 0; детали CORE-INIT |
| IH-19 | «Наследование, LSP и хрупкий базовый класс» | Нарушение MessageStore.save в SilentStore, контраст OptionalSaver через композицию, fragile superclass hook; **дизайн, не JLS** |
| IH-20 | «Иерархия» + «abstract и интерфейсы» | Один class-superclass, много interfaces, class method приоритетнее default, конфликт defaults |
| IH-21 | «Почему static методы...» + «final» | Запрет static/instance с той же сигнатурой, final override/hiding |

## P2: сознательно оставленные границы

- **IH-22** — generics/erasure/bridge и bytecode → будущие CORE-ERASURE и JVM-BYTECODE.
- **IH-23** — sealed/record ограничения → CORE-SEALED, CORE-RECORDS.
- **IH-24** — JVM vtable/JIT и оптимизации не гарантируются JLS; измерений нет.
- **IH-25** — Object.equals/hashCode/toString и контракт proxy/ORM → CORE-OBJECT, CORE-EQUALITY и темы фреймворков.

## Ретроспективное сравнение с иными интервью-источниками

Исследование 01 использовало Q-IN28, Q-TEAMLEAD и Q-CHERNYSHOV. Для независимой проверки пропусков после текста дополнительно рассмотрены:

1. [Baeldung — Java Structure and Initialization Interview Questions](https://www.baeldung.com/java-classes-initialization-questions) — перегрузка/переопределение, static hiding и особенности конструкторов.
2. [GeeksforGeeks — Java Interview Questions](https://www.geeksforgeeks.org/java/java-interview-questions/) — extends, static, composition, fragile superclass, ограничение наследования.
3. [Baeldung — Java Type System Interview Questions](https://www.baeldung.com/java-type-system-interview-questions) — Object и реализации интерфейсов.

Все найденные **в рассмотренной выборке** ключевые вопросы уже отражены в IH-01–IH-21. **Новых P0-пробелов в этой выборке не выявлено**, абсолютная полнота интервью неизвестна. Эти C-источники не являются нормативными: спорные гарантии проверяются по JLS 21, не по их ответам. Частота вопросов не оценивалась.

## Фактические проверки 02 на JDK 21

Окружение: `openjdk 21.0.12.1` и `javac 21.0.12.1`. Во временном каталоге `/tmp/inheritance-02-check` вне Git-репозитория выполнены отдельные `javac --release 21 -d <out> <sources>` и, для работающих вариантов, `java -cp <out> Main`.

**Семь успешно компилированных и запущенных вариантов:**

- `Dispatch`: шесть строк `derived:object`, `derived:string`, `base:static/derived:static`, `67/68`, `base:object/derived:object`, `67`.
- `Covariant`: `Integer:68`; narrower checked throws.
- `PrivateDispatch`: `base-private/child-public`.
- `Init`: `0`.
- `FieldMix`: `67/68` при static/instance field hiding.
- `PackageMain`: `true`, `true`, `67`, `class` (Object/abstract/default method).
- `client.Main` в двух пакетах: `base` для package-private parent method.

**Двенадцать отдельных ожидаемых отказов компиляции:** `BadAmbiguity`, `BadFinal`, `BadHiding`, `BadOverride`, `BadReturn`, `BadThrows`, `BadFinalStatic`, `BadAccess`, `BadReturnOnly`, `BadStaticOther`, `BadMultipleExtends` и `BadPackageOverride`. Для каждого проверен ненулевой результат `javac` и наличие диагностики по намеренно запрещённому случаю. Это изолированные эквивалентные пробы — **не исходники будущего примера 03**.

**Не выполнялось ролью 02 локально:** `bash examples/core/inheritance/verify.sh` на новом интегрированном HEAD (ожидается проверка CI и/или 03); локальные Gradle/JUnit/Mockito; визуализация и переходы в Obsidian/IntelliJ; автоматическая проверка всех внешних URL. Успех отдельных проб не доказывает полноту JLS.

## Дальнейшие ворота

- [Issue #39](https://github.com/Melchiorius/backend-interview-labs/issues/39), роль 03: пример из [PR #44](https://github.com/Melchiorius/backend-interview-labs/pull/44) уже включён в общую ветку PR #43; подтвердить проверки на новом интегрированном HEAD.
- [Issue #40](https://github.com/Melchiorius/backend-interview-labs/issues/40), роль 05: редактура текста, имён, навигации, понятности участников.
- [Issue #41](https://github.com/Melchiorius/backend-interview-labs/issues/41), роль 04: независимый G4 после интеграции и G3, по конкретному SHA.
- Роль 00 решает merge. Роль 06 после merge сверяет COVERAGE. **Статус CORE-INHERITANCE не повышать до публикации.**
