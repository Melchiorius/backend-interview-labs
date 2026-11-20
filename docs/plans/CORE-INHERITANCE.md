# CORE-INHERITANCE — исследовательский план до написания статьи

**Статус:** G1 принят 20.11.2026 решением 00 после независимого G1-review PASS роли 04 по PR #42 (проверенный HEAD `0e72b6929006ed12fc4b4467c79e7c275fc6fc27`). Статья и Java-пример опубликованы через PR #43: G3 PASS роли 05 и G4 PASS роли 04 на интегрированном предmerge HEAD `be19d7cac5da78e4d0f3610df4199d23b4f9a49d`; G5 — squash merge в `main`, SHA `86e67b756395373b34cbd9c4c8a7c7435c7d090e`. **Дата:** 20.11.2026. **Issue:** [#37](https://github.com/Melchiorius/backend-interview-labs/issues/37); родитель [#33](https://github.com/Melchiorius/backend-interview-labs/issues/33). **Исходный base main при постановке G1:** `36b1050293aa7b9d5bc9a741ad9e1564e8e28e41` (историческая база, не текущий main). **Норма:** Java Language Specification SE 21; **локальное окружение для проб на этапе G1:** OpenJDK 21.0.12.1, `javac --release 21`. Это исследовательский план и запись об основании статьи, а не сама учебная статья.

## 1. Вопрос читателя, обещание и содержательные границы

**Главный вопрос:** почему `Base x = new Derived()` может вызвать метод `Derived`, но не его собственную перегрузку, не его `static`-метод и не его скрытое поле? Как это связано с настоящим наследованием, переопределением, выбором метода, `super`, `final`, допустимым результатом/исключениями и дизайном API?

**Обещание будущей статьи:** читатель самостоятельно предсказывает вывод и ошибки компиляции для `extends`, `overload`, `override`, `static`/field hiding, `super` и `final`; объясняет разделение *compile-time declaration* и *runtime method lookup*; знает важные границы access/constructor/abstract/return/throws; обосновывает, когда наследование отражает отношение типов, а когда предпочтительнее композиция. Отсутствие углублённой bytecode-теории не мешает объяснить наблюдаемую семантику языка.

**Внутри CORE-INHERITANCE:** single superclass + множественная реализация интерфейсов на обзорном уровне, «is-a» как проектная эвристика (не грамматика JLS), наследуемые члены, upcast и статический тип выражения, разрешение overload, dynamic dispatch override, раздельные static method hiding и field hiding, `super`, `final`, `@Override`, covariance/checked throws, кратко abstract, минимальные примеры LSP/fragile superclass.

**Вне детального scope:** полный порядок конструирования/инициализаторов и вызовов `super()`/`this()` — `CORE-INIT`; `equals/hashCode/toString/clone` — `CORE-OBJECT`/`CORE-EQUALITY`; интерфейсные default-конфликты — `CORE-INTERFACES`; bridge, erasure, raw/generics — `CORE-GENERICS`/`CORE-ERASURE`; sealed и records — `CORE-SEALED`/`CORE-RECORDS`; module/access specifics — готовая `CORE-ACCESS` и будущая `CORE-MODULES`; полная композиция — `CORE-COMPOSITION`; байткод/JIT/оптимизации — JVM-темы. Будущие ID писать текстом, без ссылок на несуществующие статьи.

**Терминология:** *перегрузка* (разные сигнатуры, compile-time selection), *переопределение* (override instance method), *скрытие* (static method/field hiding), *статический тип выражения* (тип, известный компилятору), *runtime class* (фактический класс объекта). `@Override` — компиляционная проверка соответствия правилам, а не переключатель runtime-dispatch. P0/P1/P2 — приоритет объяснения в статье, не грейд кандидата и не оценённая частота вопросов.

## 2. Нормативные источники — A, зафиксированная Java SE 21

- **`OFF-JLS21` [JLS 21 §8.1.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.1.2), [§8.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.4), [§8.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.2):** superclass/subclass, типы-члены, `final`/`sealed`, правила наследования.
- **[§8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3) и [§15.11.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.11.1):** field hiding, статический выбор поля, включая контраст static/instance.
- **[§8.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.2), [§8.4.8.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.1), [§8.4.8.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.2), [§8.4.8.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.3), [§8.4.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.9):** signature/subsignature, overriding, static hiding, требования совместимости, overloading.
- **[§8.4.3.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.3.3), [§8.4.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.5), [§8.4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.6):** final method, return-type-substitutability (включая covariance), checked exceptions.
- **[§15.12.1–15.12.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12):** compile-time поиск, применимость и наиболее специфичный overload; [§15.12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.4) — runtime evaluation, dynamic method lookup. Уточнять, что и в runtime-стадии есть специальные случаи `static`, `super`, `private`.
- **[§6.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6):** доступность классов/членов, package-private из другого пакета, `protected`; готовая тема CORE-ACCESS.
- **[§8.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8) и [§12.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.5):** конструкторы не наследуются и не override; опасность вызова overridable метода до инициализации поля подкласса (только предупреждение здесь).
- **[§8.1.1.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.1.1), [§8.4.3.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.3.1), [§9.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4):** abstract class/method, interface/default boundary.
- **`OFF-API21` [java.lang.Override](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Override.html):** назначение `@Override` (проверка компиляции), [java.lang.Object](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html) — корневой объектный контракт как граница с CORE-OBJECT.

**Чёткое разделение:** JLS задаёт семантику языка; JVMS определяет правила JVM, а `javac`/`javap`/HotSpot — конкретные инструменты/реализация. Нельзя объяснять выбор перегрузки таблицей виртуальных методов, объявлять vtable/JIT нормативным алгоритмом JLS или делать вывод о гарантиях из одного запуска.

## 3. Источники B/C: независимая проверка пропусков

### B — официальные и поясняющие материалы

- `OFF-DEVJAVA` / `B-DEVJAVA-LANGUAGE`: [dev.java Language](https://dev.java/learn/language/), [Classes and Objects](https://dev.java/learn/language/oop/classes-objects/). Объяснения помогают выстроить модель, но страницы развиваются: точные утверждения о Java 21 брать из спецификации.

### C — 3 независимых каталога вопросов, уже входящих в SOURCE_REGISTRY

- **`Q-IN28`** [in28minutes/JavaInterviewQuestionsAndAnswers](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers): вопросы **№32–36** (inheritance, overloading, overriding, ссылку на объект подкласса через superclass, multiple inheritance); **№43–46** (abstract); **№50–54** (constructor/super). Прямая тема вопросов проверена; интервью-ответы не нормативны.
- **`Q-TEAMLEAD`** [teamlead/java-interview-questions](https://github.com/teamlead/java-interview-questions): вопросы о возможности override `static`, динамическом полиморфизме, нескольких superclass, возможности переопределить конструктор и различии overriding/overloading. Авторская статистика частот не проверялась и не определяет P0.
- **`Q-CHERNYSHOV`** [java-interview-questions-and-answers](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers): раздел OOP & SOLID, вопросы **№60–61** (Liskov), **№65–67** (наследование против композиции и делегирование); **№24–25** (generics inheritance/bridge) как повод отделить P2. Это самостоятельный угол исследования *проектной* применимости, не источник правил override.

Выбор охватывает три различных ракурса: базовые контрастные вопросы, повторяющиеся практические вопросы, дизайн/контракты. В реестре есть все три источника, **новую запись SOURCE_REGISTRY не добавлять без нового пригодного источника**. Не поднимать вопрос в P0 по одному C-источнику. Ответы сформулировать самостоятельно. После статьи 02 выполнить повторный аудит по четвёртому C-источнику (например `Q-BAELDUNG-CORE` или `Q-HRUDAY`) без ложной статистики.

## 4. Нормативная таблица выбора поведения: три разных этапа

| Выражение / элемент | Компиляция | Во время выполнения | Подводный камень |
|---|---|---|---|
| `Base ref = new Sub(); ref.m(67)` с перегрузками | По *статическим типам* и применимости выбирается сигнатура/объявление (`§15.12.2`) | Для выбранного **instance**-метода выбирается допустимая переопределяющая реализация (`§15.12.4`) | Новая `m(Integer)`/`m(String)` только в `Sub` не становится доступной для поиска по `Base ref` |
| `ref.staticMethod()` | `static` выбирается через статический тип/найденное объявление | Нет виртуального dispatch | Вызов через объектную ссылку может компилироваться, но вводит в заблуждение; использовать `Base.staticMethod()` |
| `ref.field` | Поле ищется по compile-time типу выражения/квалификатору (§15.11) | Не полиморфно переопределяется | Сокрытое `Sub.field` не выбирается из-за runtime-класса объекта |
| `super.m()` / `super.field` | Применяются правила superclass и доступности | Вызов `super.m()` не выбирает переопределение самого `Sub` | Не путать `super.m()` с `((Base)this).m()`: cast не выключает virtual dispatch |
| `private`, package-private в другом пакете | Не существует соответствующего override для этих случаев | Создание одноимённого метода не подменяет исходный вызов | `@Override` обнаруживает ложное ожидание |

**Точность:** упрощение «вызовы методов всегда виртуальные» неверно. `static`, `private`, `super` и конструкторы требуют отдельного разбора. Для `final`-метода запрещён override/hiding, но отсутствие override само по себе не оправдывает предположение о конкретной оптимизации JIT.

## 5. Матрица P0/P1/P2 до текста

Каждая строка — вопрос, который будущая статья обязана уметь разъяснить в пределах приоритета. Кодовые ID относятся к §6; названия разделов в кавычках — **предлагаемые**, не опубликованные. Нормативную опору дают пункты §2, банк C служит для выявления вопросов.

| ID / класс | Вопрос → ожидаемый ответ | Источники | Пример/критерий и раздел будущей статьи; граница |
|---|---|---|---|
| IH-01 **P0** | Что наследует класс при `extends`? Одна прямая class-supertype, транзитивные члены при ограничениях §8.2; `private` недоступен как наследуемый; конструкторы **не наследуются**. | §8.1.4, §8.2, §8.8; IN28 32/36 | R13, N14; «Иерархия и члены», CORE-CLASSES/ACCESS |
| IH-02 **P0** | Можно ли хранить `new Sub()` в переменной `Base`? Да при subtype/upcast; доступные методы выбираются из compile-time типа. | §8.1.4, §5.1.5, §15.12; IN28 35 | R01–R02; «Два типа»; готовые CORE-TYPES/CONVERSIONS |
| IH-03 **P0** | Чем overriding отличается от overloading? Разные сигнатуры → overload; subsignature + правила → override; return type не создаёт overload. | §8.4.2, §8.4.8.1, §8.4.9; IN28 33/34 | R02, N06, N13; «Выбор сигнатуры»; CORE-CONVERSIONS |
| IH-04 **P0** | Почему `Base b = new Sub(); b.m()` выполняет реализацию `Sub`? После допустимого compile-time выбора для экземплярного метода выполняется dynamic method lookup. | §15.12.2, §15.12.4; TEAMLEAD dynamic polymorphism | R01; «Runtime dispatch» |
| IH-05 **P0** | Почему `Base b = new Sub(); b.m("x")` не выбирает новую перегрузку `Sub.m(String)`? Overload выбирает компилятор из методов, доступных через тип `Base`, а выбранное instance-объявление потом может быть override. | §15.12.1–4 | R02; «Overload vs override», не повторять всю таблицу конверсий |
| IH-06 **P0** | Переопределяются ли `static` методы? Нет, допустимо скрытие `static` другим `static`, привязка к compile-time типу. `static` ↔ instance с override-equivalent signature несовместимы. | §8.4.8.1–2, §15.12; TEAMLEAD static | R03, N04/N05; «Статические методы» |
| IH-07 **P0** | Переопределяются ли поля? Нет, поле скрывается; даже одноимённые поля разных типов и static/instance могут сосуществовать. | §8.3, §15.11 | R04, R16–17; «Field hiding»; CORE-ACCESS |
| IH-08 **P0** | Что значат `this.m()`, `super.m()` и `super.field`? Выбор `super` — обращение к реализации/полю суперкласса в разрешённом контексте; cast ссылки не эквивалентен `super`. | §15.11.2, §15.12, §8.4.8 | R04–05, N17; «super»; не пересказывать полный constructor chaining CORE-INIT |
| IH-09 **P0** | Можно ли override `private` или package-private из другого пакета? Нет, недоступное объявление не становится переопределённым; одноимённый метод — другое объявление. `@Override` полезен как контроль. | §6.6, §8.2, §8.4.8.1, API Override; CORE-ACCESS | R11–12, N11–12; «Граница доступа» |
| IH-10 **P0** | Что запретить с `final`? `final class` нельзя расширять; `final` instance-метод нельзя переопределить, `final static` нельзя скрыть. Отдельно `final` поле не означает `final` метод. | §8.1.1.2, §8.4.3.3; TEAMLEAD final | R09–10, N01–03; «final»; уже раскрыто final-переменной CORE-TYPES |
| IH-11 **P0** | Может ли override сузить доступ? Нет, нельзя снижать доступность; можно повысить при разрешённых условиях. | §8.4.8.3; CORE-ACCESS | N10; «Правила переопределения» |
| IH-12 **P0** | Что с возвращаемым типом? Для reference types возможно ковариантное уточнение (`Number` → `Integer`), несовместимое расширение нельзя; return-only overload — ошибка. | §8.4.5, §8.4.8.3 | R06, N07/N08/N13; «Return type»; сложные generics вынести в CORE-ERASURE |
| IH-13 **P0** | Можно ли добавить `throws IOException` при override метода с `throws FileNotFoundException`? Нет, нельзя расширять checked exceptions; сузить/убрать checked допустимо, unchecked — отдельно. | §8.4.6, §8.4.8.3 | R07–08, N09; «Throws»; полная иерархия исключений — CORE-EXCEPTIONS |
| IH-14 **P0** | Зачем `@Override` и что он гарантирует? Проверяет корректность переопределения/реализации, не заставляет VM выбирать метод и не применяется для overload. | API Override; §8.4.8.1, §9.6.4.4 | N06/N11/N12; «Compile-time safety» |
| IH-15 **P1** | Зачем abstract класс/метод? Нереализованный метод должен получить конкретную реализацию либо класс остаётся abstract; abstract class нельзя создавать через обычный `new` без реализации. | §8.1.1.1, §8.4.3.1 | R13, N15; кратко, глубокая модель — CORE-INTERFACES |
| IH-16 **P1** | Как комбинируются унаследованные и объявленные overloads? Override конкретной сигнатуры не отменяет другие; при ambiguity (`m(String)` vs `m(Integer)` с `null`) ошибка на compile-time. | §8.4.8, §8.4.9, §15.12.2 | R02, N18; «Границы overload»; CORE-CONVERSIONS |
| IH-17 **P1** | Зачем различать `private` вызов из superclass и одноимённый метод Sub? Private-вызов в Base не меняется из-за Sub, даже при runtime-классе Sub. | §8.4.8.3, §15.12.4 | R11; «Мнимое переопределение» |
| IH-18 **P1** | Почему вызов overridable метода из конструктора опасен? Динамически может попасть в реализацию ещё не инициализированного Sub; не объявлять это нормальным способом инициализации. | §12.5, §15.12.4 | R15 печатает `0`; подробно CORE-INIT/CON-JMM |
| IH-19 **P1** | Что выигрывает/теряет наследование относительно композиции? `is-a` требует сохранять поведенческий контракт; fragile base class/LSP — проектное рассуждение, не теорема JLS. | CHERNYSHOV OOP 60–61,65–67; §8.1.4 только как синтаксис | «Дизайн: отношение типов»; будущая CORE-COMPOSITION |
| IH-20 **P1** | Почему одновременно два родителя-класса нельзя, но два интерфейса реализовать можно? Синтаксис class extends один, interfaces via implements; при конфликтующих defaults требуется разрешение. | §8.1.4–5, §9.4 | N14/N16, R14; глубокое CORE-INTERFACES |
| IH-21 **P1** | В каких случаях метод нельзя override из-за `static`/instance или `final`? Нужна явная compile-time ошибка, не просто «будет вызван Base». | §8.4.8.1–3 | N02–05; «Почему код не компилируется» |
| IH-22 **P2** | Как erasure/bridge methods сохраняют override для generics? JVM/bytecode механика и name-clash требует отдельного разбора; не выдавать сгенерированный bridge за явный метод исходника. | §8.4.2, §8.4.8.3, JVMS 21 §4.6 по будущему плану | Хвост CORE-ERASURE/JVM-BYTECODE; CHERNYSHOV generics 24–25 |
| IH-23 **P2** | Что меняется у sealed/record классов? Ограничение наследников (`permits`), record final; детали отдельно. | §8.1.1.2, §8.10 | Хвост CORE-SEALED/CORE-RECORDS |
| IH-24 **P2** | Можно ли гарантировать таблицу vtable, `invokevirtual` для каждого вызова, inlining/JIT? Нельзя как универсальное объяснение JLS; bytecode и оптимизации — другие уровни. | JLS §15.12.4, JVMS 21; реализация HotSpot | Хвост JVM-BYTECODE/JVM-JIT; не заявлять измерений |
| IH-25 **P2** | Как overrides взаимодействуют с `Object.equals/hashCode/toString`, а также с контрактами proxy/ORM? Не смешивать правила JLS и API/framework. | OFF-API21 Object; версии Spring/Hibernate — позже | Хвост CORE-OBJECT/CORE-EQUALITY, SPR-DI, DB-JPA |

## 6. Протокол примеров «что выведет / почему не компилируется»

**Для 02:** короткие runnable фрагменты оставлять внутри статьи с ясными участниками; исходники с ошибками **не вставлять в тот же компилируемый файл**. Для 03: если будет отдельный пример, положительные и отрицательные варианты изолировать; команда CI для эталона не должна собирать намеренно некомпилируемый Java-файл.

### Сценарий A: compile-time overload + runtime override

```java
class Base { String m(Object arg) { return "base"; } }
class Sub extends Base {
    @Override String m(Object arg) { return "override"; }
    String m(String arg) { return "overload"; }
}
// в main:
Base b = new Sub();
System.out.println(b.m("X"));        // override
System.out.println(((Sub) b).m("X")); // overload
```

Компилятор видит у `b` метод `Base.m(Object)` и **сначала выбирает его сигнатуру**; реализацию затем ищут в объекте `Sub`. После явного приведения статический тип выражения уже `Sub`, и перегрузка `m(String)` выигрывает при compile-time разрешении. Проверка R02: `override/overload`. Это приоритет P0, а не подтверждение «overload динамический».

### Сценарий B: static method hiding и поле

```java
class Base {
    static String who() { return "B"; }
    int value = 67;
}
class Sub extends Base {
    static String who() { return "S"; }
    int value = 68;
}
// в main:
Base b = new Sub();
System.out.println(b.who());       // B (советуем Base.who())
System.out.println(b.value);       // 67
System.out.println(((Sub)b).value);// 68
```

`b.who()` допускается как синтаксис, но метод статический и не выбирается виртуально. Поле зависит от compile-time типа первичного выражения. Отдельно показать `super.value` / `super.m()`, чтобы не путать их с cast.

### Индекс локально проведённых проверок (все на OpenJDK 21.0.12.1)

**Фактическая команда для каждого отдельного сценария:** `javac --release 21 -d <temp/output> <список исходников сценария>`; только для успешной компиляции с ожидаемым stdout затем `java -cp <temp/output> Demo` или `client.Demo`. Исходники создавались во временных отдельных каталогах вне репозитория, не являются committed examples и не тестируют весь JLS.

| ID | Условие / наблюдаемый результат | Ожидаемый исход | Фактический результат |
|---|---|---|---|
| R01 | `Base b = new Sub(); b.m()` | Печать `Sub` | Совпало |
| R02 | Upcast + override и новая overload | `override/overload` | Совпало |
| R03 | `static` hiding по типу `Base` / `Sub` | `B/S` | Совпало |
| R04 | field hiding разных типов + `super.x` | `67/sub/67` | Совпало |
| R05 | `super.m()` внутри override | `base-sub` | Совпало |
| R06 | covariant `Number` → `Integer` | `Integer:68` | Совпало |
| R07 | `IOException` → `FileNotFoundException` | Компилируется | Совпало |
| R08 | checked `IOException` → unchecked `IllegalStateException` | Компилируется | Совпало |
| R09–R10 | Корректное использование `final class`, унаследованного `final` метода | Компилируется; `67` | Совпало |
| R11 | Private `Base.m`, отдельный публичный `Sub.m` | `base/sub` | Совпало |
| R12 | Package-private `Base.m` и одноимённый `Sub.m` из другого пакета | `base` при вызове из Base | Совпало |
| R13 | Конкретная реализация abstract-метода | `sub` | Совпало |
| R14 | Конкретный class-method vs interface default | `class` | Совпало |
| R15 | Вызов override в конструкторе до field initializer Sub | `0` | Совпало |
| R16–R17 | Сокрытие static поля экземплярным и наоборот | `67/sub`, `67/68` | Совпало |
| N01–N03 | `extends final class`; override `final` instance; hide `final static` | Ошибка компиляции | Совпало (по отдельности) |
| N04–N05 | static метод вместо instance и наоборот при одинаковой сигнатуре | Ошибка компиляции | Совпало |
| N06 | `@Override` на новую overload `(String)` вместо `(Object)` | Ошибка компиляции | Совпало |
| N07–N08 | Override с более широким или несовместимым reference return | Ошибка компиляции | Совпало |
| N09 | `throws FileNotFoundException` → `throws IOException` | Ошибка компиляции | Совпало |
| N10 | Снижение `protected` до package access при override | Ошибка компиляции | Совпало |
| N11–N12 | `@Override` для private метода и package-private метода другого пакета | Ошибка компиляции | Совпало |
| N13 | Overload только возвращаемым типом | Ошибка компиляции | Совпало |
| N14 | `class C extends A,B` | Ошибка компиляции | Совпало |
| N15 | `abstract final class` | Ошибка компиляции | Совпало |
| N16 | Два конфликтующих interface default метода без override | Ошибка компиляции | Совпало |
| N17 | `super.m()` в static context | Ошибка компиляции | Совпало |
| N18 | Две несравнимые перегрузки `(String)/(Integer)` и `m(null)` | Ошибка компиляции (ambiguity) | Совпало |

**Всего 35 независимых компиляций:** 17 положительных (в т. ч. фактический stdout где применимо) и 18 намеренно отрицательных — **35/35 соответствуют ожиданиям**. Диагностический текст `javac` зависит от инструмента и не нормируется JLS. Результаты не подменяют нормативный вывод. Перечень временных исходников и скрипт исполнения в Git не вносились.

### Дополнительные негативные контрасты, требуемые от статьи

- `@Override` поверх новой overload — compile error, не runtime exception (N06).
- `Base.method()` с возвращаемым типом `Number`, в `Sub` с более широким `Object` — compile error (N07); `Number` → `Integer` — допустимо (R06).
- Сужение/расширение *checked throws* следует отделить от unchecked exceptions и от исключения, фактически выброшенного **из тела**; синтаксический `throws` — не контракт, что метод обязательно бросит исключение.
- Два `default` конфликтуют (N16) — только короткий сигнал границы `CORE-INTERFACES`, не описание полного порядка Java интерфейсного наследования.

## 7. Зависимости и редакционные границы

**Существующие, проверенные файлы в main:**

- [CORE-CLASSES](../../knowledge/01-java-core/classes.md) — модель класса/объекта, конструкторы **не наследуются**, статические и экземплярные члены, допустимое состояние.
- [CORE-ACCESS](../../knowledge/01-java-core/access.md) — access, package-private, `protected`, private и два пакета; эта статья уже разбирает компиляционные правила доступа. Новая статья не должна её переписывать целиком.
- [CORE-TYPES](../../knowledge/01-java-core/types.md) — declared type/reference, `final` у переменной; у метода и класса другое назначение.
- [CORE-CONVERSIONS](../../knowledge/01-java-core/conversions.md) — JLS-контексты преобразований/overload; новая статья ограничится влиянием статического типа и override.
- [CORE-PARAMS](../../knowledge/01-java-core/parameters.md) — pass-by-value и выбор overload по compile-time типам; отсылка к будущему dispatch уже есть.
- [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), [CURRICULUM](../CURRICULUM.md), [COVERAGE](../COVERAGE.md), [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md) — процесс и карта статусов.

**Будущие ID, только обычным текстом, без ложных ссылок:** `CORE-INIT`, `CORE-OBJECT`, `CORE-EQUALITY`, `CORE-INTERFACES`, `CORE-COMPOSITION`, `CORE-GENERICS`, `CORE-ERASURE`, `CORE-SEALED`, `CORE-RECORDS`, `CORE-MODULES`, `CON-JMM`, `JVM-BYTECODE`, `JVM-JIT`. Их отсутствие фиксируется в COVERAGE после отдельной актуализации, не выдаётся за готовность.

## 8. Исторические требования ролей 00/02/03/05/04 и gates (этап планирования)

- **00 (историческая контрольная точка G1):** на этапе подготовки плана требовалось сверить scope и P0 IH-01–IH-14, дождаться независимого review 04 и только затем разблокировать 02/03. Это решение принято: G1-review 04 — PASS на HEAD `0e72b6929006ed12fc4b4467c79e7c275fc6fc27`, официальный G1 принят 00, исследовательский PR #42 опубликован в `main` коммитом `00e88f9ee6eb2c2e4069493aab316621adddc546`. Затем выполнены G2 02/03, G3 PASS 05, G4 PASS 04 и G5 merge PR #43 в `main` (`86e67b756395373b34cbd9c4c8a7c7435c7d090e`). Перечень ниже сохраняет исходные критерии работы ролей, но не является списком открытых поручений.
- **02:** построить связный самостоятельный русский материал по P0, объяснять три стадии «статический тип → выбор сигнатуры → реализация»; дать компактные работающие и отдельно намеренно некомпилируемые фрагменты; у каждого указать **что выведет и почему** или причину отказа. Не пересказывать целиком CORE-CONVERSIONS/ACCESS/CLASSES; P1 раскрыть или дать полезный точный хвост; P2 не объявлять покрытым.
- **03:** **рекомендован один обоснованный автономный пример**, сочетающий несколько классов, выбранный overload + override + static/field hiding и `super`. Содержательные отрицательные варианты хранить отдельно (не портить CI). JDK 21 `javac --release 21` + `java`; не требовать ритуального Gradle-подпроекта, JUnit/Mockito/ученического exercise. Согласовать с 02, какие короткие фрагменты остаются внутри статьи, а какие вынести.
- **05:** проверить, что выражения «вызов выбирается по классу объекта», «сигнатура выбирается по типу ссылки» не смешиваются и объяснены действующие лица/точка вызова; Obsidian ссылки только существующие; проверку визуализации документировать отдельно.
- **04:** независимо проверить точный HEAD и JLS 21 §8.4.8/15.12; особое внимание R02 (overload/override), R11/R12 (private/package), R15 (constructor), N07/N09 (return/throws), static-field hiding, `super` и корректность формулировок к `@Override`. Локальные 35 проверок 01 не выдавать за тесты 04. При доработке нового HEAD — новый verdict.
- **06:** исходное поручение G6 после публикации выполнено по Issue #47. `docs/COVERAGE.md` синхронизируется отдельным минимальным документарным PR с фактическим squash SHA, доказательствами pre-merge CI/G3/G4 и явным отсутствием post-merge CI; текущий статус материалов фиксируется в COVERAGE.

## 9. Остаточные риски и состояние проверки

- **Историческая база исследования:** при создании этого плана на `main` @ `36b1050293aa7b9d5bc9a741ad9e1564e8e28e41` были опубликованы пять самостоятельных статей Java Core, а `CORE-INHERITANCE` ещё отсутствовал. После G5 актуальный `main` @ `86e67b756395373b34cbd9c4c8a7c7435c7d090e` содержит шесть статей; статья и пример CORE-INHERITANCE опубликованы PR #43. Текущие Implemented/Verified и границы см. в [COVERAGE](../COVERAGE.md).
- **Выполнено локально:** `java -version`, `javac -version` показывают OpenJDK 21.0.12.1; 35 изолированных сценариев компиляции, часть из них с `java` и проверкой точного stdout. **Не выполнено:** Gradle/JUnit/Mockito/CI для данного плана (не меняется Java-код); визуальная проверка Obsidian (not run); независимое содержательное review (pending).
- **Не установлено:** полнота всех реальных собеседований; качество/частотность подсчётов C; производительность dispatch, детали конкретного JIT и реальное поведение при всех возможных системах модулей и сложных generics. Ничто из этого не повышает P0 автоматически.
- **Coverage gate после статьи:** пройти каждый IH-01–IH-14 с реальным разделом ответа и компиляционным/поведенческим контрпримером; P1 должен быть честно раскрыт либо явно отложен со смысловым минимумом; P2 обозначить; повторить аудит по независимой неиспользованной подборке. При обнаруженном P0-пробеле открывать адресное расширение, не объявлять «полноту Java Core» только по числу PR.
