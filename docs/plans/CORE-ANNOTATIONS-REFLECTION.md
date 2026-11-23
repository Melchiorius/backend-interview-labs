# CORE-ANNOTATIONS + CORE-REFLECTION — G1 исследование Java SE 21

**Статус:** G1 PROPOSED / NOT ACCEPTED. **Роль:** 01 — Research & Coverage. **Основание:** [Issue #167](https://github.com/Melchiorius/backend-interview-labs/issues/167), [поручение 00 #7078386872](https://github.com/Melchiorius/backend-interview-labs/issues/167#issuecomment-7078386872), родитель #33.

**Позднейшая пометка о статусе (2026-11-23; не часть исходного G1):** сохранённое выше `G1 PROPOSED / NOT ACCEPTED` описывает этап подготовки этого плана. Позднее роль 00 формально приняла G1: [решение по PR #169](https://github.com/Melchiorius/backend-interview-labs/pull/169#issuecomment-7078606500) на проверенном G1 HEAD `6a08a490a701aafc85f85d07066d84807bd3be43`. Соответствующие учебные материалы G2 впоследствии опубликованы через [G5 PR #172](https://github.com/Melchiorius/backend-interview-labs/pull/172), merge commit `aa183b3b1bdba3c5eed8163e4b10aee82a043092`. Это разные этапы и SHA: исторический BASE, матрицы P0/P1/P2 и результаты исследований ниже не изменены.

**BASE/main при создании исследования:** `a05ac57f70d906d3679ff36be72470fc7edff06c` (G5 [PR #164](https://github.com/Melchiorius/backend-interview-labs/pull/164) опубликован). Отдельный G6 Issue #166/PR #168 — работа роли 06, возможно изменение base при открытии PR; фактический review HEAD/BASE уточняется в GitHub, а не угадывается. **Версия нормативных источников:** JLS/Javadoc Java SE 21 без preview. **Фактические локальные проверки:** OpenJDK `javac/java 21.0.12.1`, Debian Linux 13, `--release 21`.

## 1. Две темы, педагогическая архитектура и границы

**CORE-ANNOTATIONS** — объявления метаданных и правила применимости/хранения/наследования/повторения, различие compile-time consumers и runtime reflection. **CORE-REFLECTION** — исследование структуры классов и динамическая работа с членами, с отдельно проверяемыми правилами доступа Java21. Их объединяет сквозной сценарий «объявили `@Retention(RUNTIME)` → нашли через `AnnotatedElement`», но они **не** один механизм. **Рекомендация: две самостоятельные глубокие статьи**, будущие `knowledge/01-java-core/annotations.md` и `knowledge/01-java-core/reflection.md` (пока **отсутствуют**, не оформлять как действующие ссылки); один небольшой общий runnable с двумя main и отдельным, только при учебной необходимости, JPMS fixture. Создание G2 — только 02/03 после G1 acceptance 00.

**Вне scope / не Implemented по соседству:** `CORE-MODULES`, `CORE-CLASSLOADER`, `CORE-SERIAL`, `JVM-LOADING`, JPMS как самостоятельный курс, полный annotation processing (JSR269), Spring/AOP/ORM, dynamic proxies, MethodHandles/VarHandles, hidden classes, bytecode instrumentation, Java agents, полноценная runtime performance/security оценка, JMM/GC/JIT/JMH. Наблюдение двух JPMS-модулей (``6.3`) **ограничено конкретным** exports/opens API access, а не подтверждает весь CORE-MODULES. Не считать каждый `@Override` в старом материале полнотой CORE-ANNOTATIONS.

### 1.1 Аудит реального опубликованного main — уже есть / новая глубина

Фактически после G5 #164: **31 самостоятельная Java Core статья / 32 из 42 curricular ID имеют материал / 10 ещё не Implemented**. Статусные [CURRICULUM](../CURRICULUM.md) и [COVERAGE](../COVERAGE.md) могли отставать до G6 #166, но опубликованные [assertions](../../knowledge/01-java-core/assertions.md) и [optional](../../knowledge/01-java-core/optional.md) действительно присутствуют. Дерево начального BASE прочитано целиком, `truncated=false`. Существование каждой относительной ссылки ниже проверяется по Git tree; не притворяемся, что GUI Obsidian или markdown anchors проверены.

| Реальный путь опубликованного материала | Что уже разобрано | Отдельная глубина AN/RF |
|---|---|---|
| [classes](../../knowledge/01-java-core/classes.md) | классы, члены, конструкторы; reflection упомянута как альтернативный способ создания | объявления annotation element, Class/Method/Constructor, invocation и access failure |
| [access](../../knowledge/01-java-core/access.md) | Java visibility/private/protected/package | `canAccess/trySetAccessible` и JPMS `exports/opens` ≠ «private можно всегда обойти» |
| [interfaces](../../knowledge/01-java-core/interfaces.md) | `@FunctionalInterface` и SAM | объявления `@interface`, `@Target/Retention/Repeatable/Inherited` |
| [initialization](../../knowledge/01-java-core/initialization.md) | `Class.forName(name,false,loader)` vs initialization | API reflection и наблюдаемый static init — только мост, не новый JVM-LOADING курс |
| [generics](../../knowledge/01-java-core/generics.md) | inference, параметризация | `Type/ParameterizedType`, тип декларации vs конкретный generic argument объекта |
| [erasure](../../knowledge/01-java-core/erasure.md) | generic declarations vs runtime erasure, bridge и Reflection snippet | систематическое `getGenericReturnType` vs `getReturnType` и ограничения |
| [nested-classes](../../knowledge/01-java-core/nested-classes.md) | member/local/anonymous, nested access | **nestmates** изменяют пример проверки private access; `RF_NEST` |
| [records](../../knowledge/01-java-core/records.md) | элементы/constructor и аннотации record-компонентов (P1) | annotation target/retention и runtime API; не обещать полный RecordComponent |
| [enums](../../knowledge/01-java-core/enums.md) | enum constraints + reflection границы P2 | Class enum name и annotation enum-valued members, не полный CORE-ENUMS |
| [sealed](../../knowledge/01-java-core/sealed.md) | `Class.isSealed` и permits, reflection P2 | general Class API, не полный sealed/JPMS |
| [functions](../../knowledge/01-java-core/functions.md) | `@FunctionalInterface` semantic compile check | meta-annotations, SOURCE/CLASS/RUNTIME, runtime visibility |
| [exceptions](../../knowledge/01-java-core/exceptions.md) | propagation/cause/checked errors | `InvocationTargetException` wrapping, `IllegalAccessException`, `NoSuchMethodException`; не повторять весь Throwable |
| [assertions](../../knowledge/01-java-core/assertions.md) | runtime disabled vs enabled assert | annotation metadata не JUnit annotations; reflection не assertion |
| [optional](../../knowledge/01-java-core/optional.md) | absent vs contract error | вызовы reflection могут возвращать `null`/array либо бросать checked exceptions, не «все reflection APIs Optional» |

Нормативные документы: [AGENTS](../../AGENTS.md), [WORKFLOW](../WORKFLOW.md), [ROLE_MODEL](../ROLE_MODEL.md), [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md), [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), [EXERCISE_AND_TESTING_STANDARD](../EXERCISE_AND_TESTING_STANDARD.md). Никаких файлов статьи/реестров в G1 не менять.

## 2. Источники A/JLS/API21 и независимые интервью-вопросы C

**Источники A фиксированы на Java SE 21; C — только постановка вопроса, без гарантий и выдуманной статистики.**

| ID | Нормативный Java21 источник | Для чего именно |
|---|---|---|
| A1 | [JLS21 `9.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6), [`9.6.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.1), [`9.6.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.2) | `@interface`, допустимые types/constants/required/default elements |
| A2 | [JLS21 `9.6.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.3), [`9.6.4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4.8), [`9.7.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7.5) | Repeatable, containing interface, constraints, multiple annotation syntax |
| A3 | [JLS21 `9.6.4.1–9.6.4.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4), [`9.7.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7.4) | `@Target`, `@Retention`, `@Inherited`, declaration vs type-use |
| A4 | [JLS21 `9.6.4.4–9.6.4.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6.4) | `@Override/@SuppressWarnings/@Deprecated/@SafeVarargs/@FunctionalInterface`, compile-time roles |
| A5 | [Annotation meta API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/annotation/package-summary.html), [Retention](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/annotation/Retention.html), [Target](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/annotation/Target.html), [Repeatable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/annotation/Repeatable.html) | official meta-annotation contracts, values RUNTIME/SOURCE/CLASS |
| A6 | [AnnotatedElement 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AnnotatedElement.html), [Class 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html) | present vs directly/indirectly/inherited; getAnnotation/getDeclaredAnnotation/getAnnotationsByType; declared/public/inherited |
| A7 | [Field 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Field.html), [Method 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html), [Constructor 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Constructor.html) | reflective inspection/get/set/invoke/newInstance; checked/runtime failures |
| A8 | [AccessibleObject 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AccessibleObject.html), [InaccessibleObjectException 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/InaccessibleObjectException.html), [JLS21 `6.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6) | canAccess/trySetAccessible/setAccessible; access checks and JPMS opens/exports |
| A9 | [java.lang.reflect.Type 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Type.html), [ParameterizedType 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/ParameterizedType.html), [Modifier 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Modifier.html) | getReturnType (erased Class) vs getGenericReturnType (declared generic metadata), modifiers |
| A10 | [Class.forName API 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#forName(java.lang.String,boolean,java.lang.ClassLoader)), [JLS21 `12.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4) | загрузка/инициализация only in chosen overload, separate JVM evidence |
| A11 | [JPMS module declaration JLS21 `7.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.7), [Module 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Module.html), [AccessibleObject 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AccessibleObject.html) | named modules, exported public access vs opened package deep reflection, **только узкий verified module-path сценарий** |
| A12 | [InvocationTargetException 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/InvocationTargetException.html), [Parameter 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Parameter.html) | target exception cause and metadata; parameter names without `-parameters` не обещать |

**Независимые реальные вопросы C из существующего реестра:**
- **C1 Q-HRUDAY** — [README, Chapter 18 ``18.1/18.3/18.4/18.6](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md): “What is Reflection in Java?”, “How can constructors be accessed using Reflection?”, “Why must annotations have RetentionPolicy.RUNTIME for Reflection?”, “What restrictions exist in modern Java regarding reflective access?” Проверены строки разделов 18.1/18.3/18.4/18.6; ответы из банка **не** нормативны и `-Xlint`/JPMS требуют Javadoc.
- **C2 Q-TEAMLEAD** — [README](https://github.com/teamlead/java-interview-questions/blob/main/README.md): “How to create your own annotation”, “What is the main idea of reflection”. Это авторские формулировки; не приписываем им исчерпывающий Java21 API checklist.
- **C3 Q-CHERNYSHOV-RU** — [core.md](https://github.com/Shell26/Java-Developer/blob/master/core.md): «Что такое Reflection?» и «Как создать аннотацию?» — реальные заголовки, примеры/ответы перепроверять по JLS21.
- **C4 Q-HRUDAY (пересечение)** — `18.4` спрашивает `isAnnotationPresent` и retrieval values, `18.6` о модулях и инкапсуляции; несколько независимых банков дополняют формулировку, **не заменяют технический источник A**.

## 3. P0 CORE-ANNOTATIONS (17 обязательных вопросов)

`ANxx` — строка исполняемого `AnnotationsProbe` (``6.1`), `Nxx` — отдельный javac negative (``7`); каждому P0 — реальный C и точная норма A. Эти 17 строк нельзя заменить перечислением терминов в будущей статье.

| ID | Что уметь объяснить | A/C | Probe / контрпример / граница |
|---|---|---|---|
| AN-01 | `@interface` объявляет annotation interface; element — метод без параметров | A1 / C2,C3 | `AnnotationsProbe.Family`; `N12/N13` incorrect element type |
| AN-02 | Допустимые element types: primitive, String, Class, enum, annotation, их одномерные массивы; не arbitrary Object/List/2D array | A1 / C2 | `AN12–AN14; N03/N12/N13` |
| AN-03 | Required element vs `default`; значения не dynamic expressions | A1 / C2,C3 | `AN11` default; `N02/N04/N09` |
| AN-04 | `@Target` ограничивает contexts; declaration annotation vs TYPE_USE annotation | A3 / C2 | `AN17/AN18, N01`; не утверждать, что TYPE_USE обязательно равно FIELD |
| AN-05 | `SOURCE / CLASS / RUNTIME` — разные гарантии жизненного цикла/видимости | A3,A5 / C1,C3 | `AN09/AN10` invisible via runtime API; `AN01` visible |
| AN-06 | `getAnnotation` и `isAnnotationPresent` для RUNTIME declaration annotations | A6 / C1 | `AN01,AN20`; CLASS-retention не compile-negative |
| AN-07 | `getDeclaredAnnotation` отличается от inherited `getAnnotation` | A6 / C1 | `AN02=true parent, AN03=true absence declared` |
| AN-08 | `@Inherited` работает для class inheritance от annotated superclass, **не** для implemented interface или arbitrary method | A3,A6 / C1,C3 | `AN02/AN04/AN05` |
| AN-09 | `@Repeatable` требует корректный container c `value():Tag[]` и constraints | A2,A5 / C1,C2 | `AN06/AN07`, `N06` invalid container |
| AN-10 | `getAnnotationsByType` раскрывает повторения через container; `getDeclaredAnnotation(Tag)` может быть null | A2,A6 / C1 | `AN06=2,AN07=2,AN08=true,AN22=alpha` |
| AN-11 | `@Override` — проверка compiler, не runtime business annotation | A4 / C2,C3 | `AN16=true` null via runtime; `N10` |
| AN-12 | `@FunctionalInterface` — compiler-enforced SAM declaration, а не условие lambda | A4 / C2 | `AN19=68`, `N11` |
| AN-13 | `@Deprecated/@SuppressWarnings/@Documented` — разные эффекты, не «все runtime hooks» | A4,A5 / C2 | `@Documented Details`, `@Override Parent`; расширение deprecated/suppress P1 |
| AN-14 | Enum/Class/array-valued annotation elements — метаданные и типы/значения, не методы создания объектов | A1,A5 / C2 | `AN12–AN14` (numbers, Class, enum) |
| AN-15 | `@Retention(RUNTIME)` делает annotation видимой через поддерживающий API, **не запускает JSR269 processor** и не сканирует classpath | A3,A6 / C1 | `AN01/AN15`; processing/Javac API not run |
| AN-16 | Type-use можно прочитать через `Field.getAnnotatedType()`, это **не то же** что `Field.getAnnotation` | A3,A6 / C1 | `AN17=true,AN18=true` (отсутствует declaration annotation) |
| AN-17 | Nonrepeatable duplicate и invalid value/type/target — **compiler** failure, annotation absent at runtime — другой случай | A1–A3 / C2 | `N01–N09, AN09/AN10` |

## 4. P0 CORE-REFLECTION (18 обязательных вопросов)

| ID | Механизм | A/C | Probe / граница |
|---|---|---|---|
| RF-01 | `Foo.class` vs `obj.getClass` дают Class для runtime типа, не новый объект Foo | A6 / C1,C3 | `RF01=true` |
| RF-02 | `Class.forName(name,false,loader)` vs `forName(name)`: инициализация различна | A10 / C1 | `RF17b,RF18,RF17c`; classloader topic остаётся отдельно |
| RF-03 | `getMethods` охватывает public inherited, `getDeclaredMethods` — declared regardless access; не 1:1 «все видимые» | A6 / C1 | `RF02/03/04` |
| RF-04 | `getFields` vs `getDeclaredFields`, private declaration и inherited public | A6,A7 / C1 | `RF05–RF07` |
| RF-05 | `getConstructors` vs `getDeclaredConstructors`; различие списка и права invocation | A6,A7 / C1 | `RF08=1:2,RF13–RF15` |
| RF-06 | `getDeclaredMethod` может **найти** private, но не гарантирует право `invoke` | A7,A8 / C1,C3 | `RF04` и `RF19=IllegalAccessException` |
| RF-07 | `canAccess`/`trySetAccessible`, unnamed module доступ **контекстный**, не universal bypass | A8 / C1 | `RF09=false,RF10=true,RF20=true`; отдельный top-level ReflectTarget (не nestmate) |
| RF-08 | JPMS `exports` для публичного API vs `opens` для deep reflective access в named module | A8,A11 / C1 | `JM01..03` настоящие два JVM module-path запуска; `CORE-MODULES` отдельно |
| RF-09 | `Method.invoke`: Object return, boxing/unboxing/widening, checked failures | A7 / C1 | `RF12=72`; narrowing limits Javadoc |
| RF-10 | `Constructor.newInstance` реально создаёт object; public/declared и доступ | A7,A8 / C1 | `RF13=68,RF14=true,RF15=1` |
| RF-11 | Метод throws из target → `InvocationTargetException.getCause` | A7,A12 / C1 | `RF16=IllegalArgumentException:boom` (runtime) |
| RF-12 | `getMethod("missing")` → checked `NoSuchMethodException`, не negative javac | A6,A7 / C1 | `RF17` runtime; `RF_N17` unhandled checked compiler error |
| RF-13 | `IllegalAccessException` vs `InaccessibleObjectException` — разные runtime access outcomes | A7,A8 / C1 | `RF19` vs `RF31`; `RF30=false` |
| RF-14 | `getReturnType` returns raw erased Class, `getGenericReturnType` retains declaration Type | A7,A9 / C1 | `RF22=List,RF23=true,RF24=java.lang.String`; не значит `List<String>` reified |
| RF-15 | `Type`/`ParameterizedType` reflect method declaration, не inspect arbitrary concrete List elements | A9 / C1 | `RF23/RF24`; [erasure](../../knowledge/01-java-core/erasure.md) |
| RF-16 | Primitive `Class` and array `Class` objects, component type | A6 / C1,C3 | `RF25–RF28` |
| RF-17 | `getDeclared*` member ordering unspecified: sort before printable result | A6,A7 / C1 | `RF32=[add, fail, hidden, names]` after explicit sorted, не полагаться на reflection array order |
| RF-18 | AccessibleObject/JDK strong encapsulation: `String.value` inaccessible deep reflection; don't recommend access workaround | A8,A11 / C1 | `RF30=false,RF31=InaccessibleObjectException` in JDK21; JPMS scope narrow |

**Итого P0 35: 17 AN + 18 RF**. Для G4 reviewer должен сопоставить каждую строку P0 с полноценным G2 объяснением, а не только наличием API имени. Их источники C — подборки вопросов, а JLS/Javadoc A — техническая норма.

## 5. P1/P2 — хвост и реальные границы

| ID | Priority | Почему не закрываем без отдельной глубины |
|---|---|---|
| AN-18 | P1 | `@Retention` отсутствует → CLASS по умолчанию, `@Target` отсутствует → допустимые contexts по JLS; `AN10` показывает explicit CLASS |
| AN-19 | P1 | `@Documented` vs Javadoc tool, `@Deprecated(since,forRemoval)`, `@SuppressWarnings` ключи/область; без Javadoc tool run |
| AN-20 | P1 | TYPE_USE внутри generics, local/parameter/record components, `AnnotatedType` array; не подменять всю TYPE_USE систему одним field |
| AN-21 | P1 | Repeatable + `@Inherited` containing annotation constraints и getAnnotationsByType inheritance; проверить отдельно |
| AN-22 | P1 | Processor `javax.annotation.processing` / annotation SOURCE handling — **не запускали**, не выдавать наличие retention за processor invocation |
| RF-19 | P1 | `Parameter` names/`-parameters`, synthetic/bridge methods: дополнительные compiler flags/metadata без гарантий имени `arg0` |
| RF-20 | P1 | `getRecordComponents`, `getPermittedSubclasses`, `Class.isRecord/isSealed` — действующие API, но полнота records/sealed уже отдельно |
| RF-21 | P1 | Invocation varargs/boxing/widening, exception conversion, receiver/constructor edge; полный набор отдельно |
| RF-22 | P1 | `getDeclaredAnnotationsByType` vs `getAnnotationsByType` на inherited/repeatable; проверить runtime direct/indirect matrix |
| RF-23 | P1 | `trySetAccessible` vs `setAccessible(true)` на same module, module openness и nestmates; точные reason differences |
| AN-23 | P2 | JSR 269 actual annotation processor, generated files, Gradle/Maven integration, JNI/classfile annotations — not run |
| AN-24 | P2 | Binary/runtime corrupted annotations, `AnnotationFormatError`, missing classes/enum constant values, error recovery — not run |
| AN-25 | P2 | Proxy/AOP/Spring/ORM handling meta-annotations vs Java native reflection, library-specific semantics — not run |
| RF-24 | P2 | Separate classloader identity, dynamic loading, hidden classes, service loaders, JPMS full graph — not run |
| RF-25 | P2 | Reflection internals/JDK 21 implementation/performance/JIT/JMH/GC vs language contract — not run |
| RF-26 | P2 | Privileged access/security manager legacy, agents, instrumentation, bytecode manipulation — not run |
| RF-27 | P2 | Dynamic proxies, MethodHandles/VarHandles, annotation processing frameworks — not run |
| RF-28 | P2 | JMM/concurrency, serialization, modules as full independent course — not run |

**Итого: 35 P0 / 10 P1 / 8 P2.** Все P2 — not Implemented/неподтверждённые тестами подпункты, даже если есть минимальные мосты.

## 6. Полностью воспроизводимые positive Java21 программы

Исполнены на `javac/java 21.0.12.1`, Debian Linux 13, отдельно от GitHub CI. Команда `javac --release 21 -Xlint:all AnnotationsProbe.java ReflectionProbe.java` завершилась **exit=0, stderr пуст**, оба `java` дали **23+34=57** точных stdout lines; два `diff -u` по эталонам ниже **exit=0**. Программы содержатся **только** в Markdown, не как Java-файлы данного G1 PR.

### 6.1 AnnotationsProbe.java (полный исходник)

~~~java
import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AnnotationsProbe {
    enum Importance { HIGH, LOW }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Family { String value(); }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Repeatable(Tags.class)
    @interface Tag { String value(); }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface Tags { Tag[] value(); }

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.TYPE)
    @interface SourceMark {}

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    @interface ClassMark {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Details {
        String value() default "default";
        int[] numbers() default {6, 7};
        Class<?> kind() default String.class;
        Importance importance() default Importance.HIGH;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    @interface TypeMark {}

    @Family("parent")
    @Tag("alpha")
    @Tag("beta")
    @Details
    @SourceMark
    @ClassMark
    static class Parent {
        @Tag("method")
        public String work() { return "work"; }
        @Override public String toString() { return "parent"; }
    }
    static class Child extends Parent {}

    @Family("interface")
    interface FamilyInterface {}
    static class Implementor implements FamilyInterface {}

    static class FieldHolder { @TypeMark String text; }

    @FunctionalInterface
    interface Op { int apply(int a); }

    public static void main(String[] args) throws ReflectiveOperationException {
        System.out.println("AN01=" + Parent.class.getAnnotation(Family.class).value());
        System.out.println("AN02=" + Child.class.getAnnotation(Family.class).value());
        System.out.println("AN03=" + (Child.class.getDeclaredAnnotation(Family.class) == null));
        System.out.println("AN04=" + (Implementor.class.getAnnotation(Family.class) == null));
        System.out.println("AN05=" + (Child.class.getAnnotation(Tag.class) == null));
        System.out.println("AN06=" + Parent.class.getDeclaredAnnotationsByType(Tag.class).length);
        System.out.println("AN07=" + Parent.class.getAnnotation(Tags.class).value().length);
        System.out.println("AN08=" + (Parent.class.getDeclaredAnnotation(Tag.class) == null));
        System.out.println("AN09=" + (Parent.class.getAnnotation(SourceMark.class) == null));
        System.out.println("AN10=" + (Parent.class.getAnnotation(ClassMark.class) == null));
        Details details = Parent.class.getAnnotation(Details.class);
        System.out.println("AN11=" + details.value());
        System.out.println("AN12=" + details.numbers()[0] + ":" + details.numbers()[1]);
        System.out.println("AN13=" + details.kind().getSimpleName());
        System.out.println("AN14=" + details.importance());
        Method work = Parent.class.getDeclaredMethod("work");
        System.out.println("AN15=" + work.getDeclaredAnnotation(Tag.class).value());
        Method toStringMethod = Parent.class.getDeclaredMethod("toString");
        System.out.println("AN16=" + (toStringMethod.getDeclaredAnnotation(Override.class) == null));
        Field field = FieldHolder.class.getDeclaredField("text");
        System.out.println("AN17=" + field.getAnnotatedType().isAnnotationPresent(TypeMark.class));
        System.out.println("AN18=" + (field.getAnnotation(TypeMark.class) == null));
        Op op = value -> value + 1;
        System.out.println("AN19=" + op.apply(67));
        System.out.println("AN20=" + Parent.class.isAnnotationPresent(Details.class));
        System.out.println("AN21=" + (Child.class.getDeclaredAnnotationsByType(Tag.class).length == 0));
        System.out.println("AN22=" + Parent.class.getAnnotationsByType(Tag.class)[0].value());
        System.out.println("AN23=" + (Parent.class.getAnnotation(Details.class).annotationType() == Details.class));
    }
}
~~~

**Фактический stdout, 23 строки:**
~~~text
AN01=parent
AN02=parent
AN03=true
AN04=true
AN05=true
AN06=2
AN07=2
AN08=true
AN09=true
AN10=true
AN11=default
AN12=6:7
AN13=String
AN14=HIGH
AN15=method
AN16=true
AN17=true
AN18=true
AN19=68
AN20=true
AN21=true
AN22=alpha
AN23=true
~~~

### 6.2 ReflectionProbe.java (полный исходник, отдельный top-level ReflectTarget)

~~~java
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;

public class ReflectionProbe {
    static class Parent {
        public int inheritedField = 7;
        public String inheritedMethod() { return "parent"; }
    }
    static class Lazy {
        static { System.out.println("RF18=static-init"); }
    }
    static boolean containsMethod(Method[] methods, String name) {
        return Arrays.stream(methods).anyMatch(method -> method.getName().equals(name));
    }
    static boolean containsField(Field[] fields, String name) {
        return Arrays.stream(fields).anyMatch(field -> field.getName().equals(name));
    }
    public static void main(String[] args) throws ReflectiveOperationException {
        Class<?> type = ReflectTarget.class;
        ReflectTarget obj = new ReflectTarget(67);
        System.out.println("RF01=" + (type == obj.getClass()));
        System.out.println("RF02=" + containsMethod(type.getMethods(), "inheritedMethod"));
        System.out.println("RF03=" + containsMethod(type.getDeclaredMethods(), "inheritedMethod"));
        System.out.println("RF04=" + containsMethod(type.getDeclaredMethods(), "hidden"));
        System.out.println("RF05=" + containsField(type.getFields(), "inheritedField"));
        System.out.println("RF06=" + containsField(type.getDeclaredFields(), "inheritedField"));
        System.out.println("RF07=" + containsField(type.getDeclaredFields(), "secret"));
        System.out.println("RF08=" + type.getConstructors().length + ":" + type.getDeclaredConstructors().length);
        Field privateField = type.getDeclaredField("secret");
        System.out.println("RF09=" + privateField.canAccess(obj));
        System.out.println("RF10=" + privateField.trySetAccessible());
        System.out.println("RF11=" + privateField.getInt(obj));
        Method add = type.getDeclaredMethod("add", int.class);
        System.out.println("RF12=" + add.invoke(obj, 5));
        Constructor<?> publicConstructor = type.getConstructor(int.class);
        System.out.println("RF13=" + ((ReflectTarget) publicConstructor.newInstance(67)).add(1));
        Constructor<?> privateConstructor = type.getDeclaredConstructor();
        System.out.println("RF14=" + privateConstructor.trySetAccessible());
        System.out.println("RF15=" + ((ReflectTarget) privateConstructor.newInstance()).add(1));
        try {
            type.getMethod("fail").invoke(obj);
        } catch (InvocationTargetException error) {
            System.out.println("RF16=" + error.getCause().getClass().getSimpleName() + ":" + error.getCause().getMessage());
        }
        try {
            type.getMethod("missing");
        } catch (NoSuchMethodException error) {
            System.out.println("RF17=NoSuchMethodException");
        }
        Class<?> unloaded = Class.forName(Lazy.class.getName(), false, ReflectionProbe.class.getClassLoader());
        System.out.println("RF17b=" + (unloaded == Lazy.class));
        System.out.println("RF17c=" + Class.forName(Lazy.class.getName()).getSimpleName());
        Method hidden = type.getDeclaredMethod("hidden");
        try { hidden.invoke(obj); }
        catch (IllegalAccessException error) { System.out.println("RF19=IllegalAccessException"); }
        System.out.println("RF20=" + hidden.trySetAccessible());
        System.out.println("RF21=" + hidden.invoke(obj));
        Method names = type.getDeclaredMethod("names");
        System.out.println("RF22=" + names.getReturnType().getSimpleName());
        Type generic = names.getGenericReturnType();
        System.out.println("RF23=" + (generic instanceof ParameterizedType));
        System.out.println("RF24=" + ((ParameterizedType) generic).getActualTypeArguments()[0].getTypeName());
        System.out.println("RF25=" + int.class.isPrimitive());
        System.out.println("RF26=" + String[].class.isArray());
        System.out.println("RF27=" + String[].class.getComponentType().getSimpleName());
        System.out.println("RF28=" + (Class.forName("java.lang.String") == String.class));
        System.out.println("RF29=" + Modifier.isPrivate(privateField.getModifiers()));
        Field jdkPrivate = String.class.getDeclaredField("value");
        System.out.println("RF30=" + jdkPrivate.trySetAccessible());
        try { jdkPrivate.setAccessible(true); }
        catch (java.lang.reflect.InaccessibleObjectException error) {
            System.out.println("RF31=InaccessibleObjectException");
        }
        System.out.println("RF32=" + Arrays.stream(type.getDeclaredMethods()).map(Method::getName).sorted().toList());
    }
}

class ReflectTarget extends ReflectionProbe.Parent {
    private int secret;
    public String label = "Java";
    public ReflectTarget(int value) { secret = value; }
    private ReflectTarget() { secret = 0; }
    public int add(int number) { return secret + number; }
    private String hidden() { return "hidden:" + secret; }
    public void fail() { throw new IllegalArgumentException("boom"); }
    public List<String> names() { return List.of("A", "B"); }
}
~~~

**Фактический stdout, 34 строки** (порядок методов в последней строке **явно отсортирован**):
~~~text
RF01=true
RF02=true
RF03=false
RF04=true
RF05=true
RF06=false
RF07=true
RF08=1:2
RF09=false
RF10=true
RF11=67
RF12=72
RF13=68
RF14=true
RF15=1
RF16=IllegalArgumentException:boom
RF17=NoSuchMethodException
RF17b=true
RF18=static-init
RF17c=Lazy
RF19=IllegalAccessException
RF20=true
RF21=hidden:67
RF22=List
RF23=true
RF24=java.lang.String
RF25=true
RF26=true
RF27=String
RF28=true
RF29=true
RF30=false
RF31=InaccessibleObjectException
RF32=[add, fail, hidden, names]
~~~

**RF_NEST — важный обнаруженный контрпример:** первоначально `ReflectTarget` был `static class Target` внутри `ReflectionProbe`. Тогда `Field.canAccess(obj)` для приватного поля дал **true**, а `Method.invoke` приватного метода **не** выбросил `IllegalAccessException`: caller и target были *nestmates*. Эта версия была **реально скомпилирована и запущена**, после чего заменена отдельным **top-level** `ReflectTarget`, позволившим честно проверить `RF09=false`/`RF19=IllegalAccessException`. Обобщение «всегда false для private при reflection» неверно.

### 6.3 Реальный JPMS module-path: exports-only vs exports+opens

**Это действительно были 2 отдельных compilations и 2 отдельных JVM launches на `--module-path`**, не псевдо-module и не вывод из компиляции. Варианты имеют **одинаковые** файлы `api/Vault.java` и `client/Inspect.java` и **разные** `m.target/module-info.java`. Полные source для повторения:

~~~text
exportonly/src/m.target/module-info.java
exportonly/src/m.target/api/Vault.java
exportonly/src/m.client/module-info.java
exportonly/src/m.client/client/Inspect.java
opens/src/m.target/module-info.java
opens/src/m.target/api/Vault.java
opens/src/m.client/module-info.java
opens/src/m.client/client/Inspect.java
~~~

**exportonly/src/m.target/module-info.java:**
~~~java
module m.target { exports api; }
~~~

**opens/src/m.target/module-info.java:**
~~~java
module m.target { exports api; opens api to m.client; }
~~~

**Обе версии m.target/api/Vault.java:**
~~~java
package api;
public class Vault {
    public Vault() {}
    public int read() { return 67; }
    private int secret = 67;
}
~~~

**Обе версии m.client/module-info.java:**
~~~java
module m.client { requires m.target; }
~~~

**Обе версии m.client/client/Inspect.java:**
~~~java
package client;
import api.Vault;
import java.lang.reflect.Field;
public class Inspect {
    public static void main(String[] args) throws Exception {
        Vault vault = new Vault();
        System.out.println("JM01=" + Vault.class.getMethod("read").invoke(vault));
        Field field = Vault.class.getDeclaredField("secret");
        boolean enabled = field.trySetAccessible();
        System.out.println("JM02=" + enabled);
        try { System.out.println("JM03=" + field.getInt(vault)); }
        catch (IllegalAccessException e) { System.out.println("JM03=IllegalAccessException"); }
    }
}
~~~

**Фактические команды для каждого варианта**, запустить из его каталога:
~~~sh
javac --release 21 -Xlint:all -d mods --module-source-path src -m m.target,m.client
java --module-path mods -m m.client/client.Inspect
~~~

Обе компиляции `exit 0, stderr=empty` после явного `public Vault() {}`: без него `-Xlint:all` на exported class даёт предупреждение `[missing-explicit-ctor]`; этот **настоящий warning был устранён**, а не скрыт. У обоих запусков `exit 0` и разные детерминированные stdout:

**exports only** (3 строки):
~~~text
JM01=67
JM02=false
JM03=IllegalAccessException
~~~

**exports + opens** (3 строки):
~~~text
JM01=67
JM02=true
JM03=67
~~~

Обобщение: `exports` допускает публичный API, но **не является** правом deep reflection на private. `opens api to m.client` в этой конкретной module topology разрешает `trySetAccessible`. Это не эксперимент с `open module`, другого graph модуля, `--add-opens`, динамической загрузкой, privilege или security policy — их не тестировали.

### 6.4 Реальное повторение/хэши

~~~sh
java -version            # openjdk 21.0.12.1, Debian Linux 13
javac -version           # javac 21.0.12.1
javac --release 21 -Xlint:all AnnotationsProbe.java ReflectionProbe.java
java AnnotationsProbe > annotations.stdout
java ReflectionProbe > reflection.stdout
# После сохранения эталонов из соответствующих text blocks:
diff -u annotations.expected.stdout annotations.stdout
diff -u reflection.expected.stdout reflection.stdout
# Из каждого каталога exportonly/opens:
javac --release 21 -Xlint:all -d mods --module-source-path src -m m.target,m.client
java --module-path mods -m m.client/client.Inspect > actual.stdout
diff -u expected.stdout actual.stdout
~~~

Реально сравнили stdout повторных запусков `diff -u`: **4/4 exit 0**, compiler stderr empty. `sha256sum` stdout UTF-8+LF для воспроизводимого конкретного опыта:
- `annotations.stdout` = `058d779f3773df4d3dc5200d0f14020db8230394342b98be3cec58c020fa91c1`
- `reflection.stdout` = `46d5e3693aaa375861cbcd0dbaf7700c5223dd1f768073831a24ed93903ccc5c`
- `jpms/exportonly/actual.stdout` = `0b97996683f6cf0d2bed7954d55cf05f0e005162d636a32b272475b4833109a6`
- `jpms/opens/actual.stdout` = `aafdcc081b39266968505f1f909b0cf5a84ee57a5b8fee37c628444c110779a3`

**Итого** 57 строк от двух main и 6 строк от двух named-module runs — **63 stdout строки**, это **не 63 теста**. GitHub CI не компилирует fenced Java из данного G1 Markdown автоматически. Runtime exceptions отражены в stdout после локального catch; **не** `javac` negatives.

## 7. Изолированные компиляционно-отрицательные Java21 source

**19/19** самостоятельных полных `.java`, изолированные отдельные `javac --release 21 -XDrawDiagnostics -d out Filename.java` (exit **1** у каждого, тематический `compiler.err.*`), на OpenJDK 21.0.12.1. Ни одна ошибка не является отсутствующим `class` из другого файла, accidental EOF или путаницей с runtime. Первая релевантная категория приведена в таблице; полные source заключены в code span. `N03`/`N12`/`N13` проверяют **недопустимую декларацию element type**, `N04/N09` — не-constant annotation expression, `RF_N17/N18` — checked exceptions при статическом Java вызове API.

| ID / самостоятельный file | Полный source | Реальный diagnostic key |
|---|---|---|
| `AN_N01_TargetClass.java` | `import java.lang.annotation.*; @Target(ElementType.METHOD) @interface A01 {} @A01 class AN_N01_TargetClass {}` | `compiler.err.annotation.type.not.applicable` |
| `AN_N02_MissingValue.java` | `@interface A02 { String value(); } @A02 class AN_N02_MissingValue {}` | `compiler.err.annotation.missing.default.value` |
| `AN_N03_ArrayOfArray.java` | `@interface AN_N03_ArrayOfArray { String[][] value(); }` | `compiler.err.invalid.annotation.member.type` |
| `AN_N04_NonconstantDefault.java` | `@interface AN_N04_NonconstantDefault { int n() default Integer.parseInt("2"); }` | `compiler.err.expression.not.allowable.as.annotation.value` |
| `AN_N05_Duplicate.java` | `@interface A05 { String value(); } @A05("A") @A05("B") class AN_N05_Duplicate {}` | `compiler.err.duplicate.annotation.missing.container` |
| `AN_N06_BadRepeatable.java` | `import java.lang.annotation.*; @Repeatable(B06.class) @interface A06 {} @interface B06 { String[] value(); } class AN_N06_BadRepeatable {}` | `compiler.err.invalid.repeatable.annotation.value.return` |
| `AN_N07_NullValue.java` | `@interface A07 { String value(); } @A07(null) class AN_N07_NullValue {}` | `compiler.err.attribute.value.must.be.constant` |
| `AN_N08_WrongType.java` | `@interface A08 { int n(); } @A08(n=67L) class AN_N08_WrongType {}` | `compiler.err.prob.found.req (loss of precision long → int)` |
| `AN_N09_NewExpression.java` | `@interface A09 { String value(); } @A09(new String("x")) class AN_N09_NewExpression {}` | `compiler.err.expression.not.allowable.as.annotation.value` |
| `AN_N10_BadOverride.java` | `class AN_N10_BadOverride { @Override public void missing() {} }` | `compiler.err.method.does.not.override.superclass` |
| `AN_N11_BadFunctional.java` | `@FunctionalInterface interface AN_N11_BadFunctional { void a(); void b(); }` | `compiler.err.bad.functional.intf.anno.1` |
| `AN_N12_ObjectElement.java` | `@interface AN_N12_ObjectElement { Object value(); }` | `compiler.err.invalid.annotation.member.type` |
| `AN_N13_VoidElement.java` | `@interface AN_N13_VoidElement { void value(); }` | `compiler.err.invalid.annotation.member.type` |
| `AN_N14_DuplicateNamedValue.java` | `@interface A14 { int value(); } @A14(value=67, value=68) class AN_N14_DuplicateNamedValue {}` | `compiler.err.duplicate.annotation.member.value` |
| `AN_N15_DoubleToInt.java` | `@interface A15 { int value(); } @A15(1.5) class AN_N15_DoubleToInt {}` | `compiler.err.prob.found.req (loss of precision double → int)` |
| `RF_N17_UnhandledGetMethod.java` | `import java.lang.reflect.*; class RF_N17_UnhandledGetMethod { void f() { Method m = String.class.getDeclaredMethod("missing"); } }` | `compiler.err.unreported.exception.need.to.catch.or.throw (NoSuchMethodException)` |
| `RF_N18_UnhandledInvoke.java` | `import java.lang.reflect.*; class RF_N18_UnhandledInvoke { void f(Method method, Object o) { method.invoke(o); } }` | `compiler.err.unreported.exception.need.to.catch.or.throw (IllegalAccessException)` |
| `AN_N19_RetentionOnClass.java` | `import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) class AN_N19_RetentionOnClass {}` | `compiler.err.annotation.type.not.applicable` |
| `AN_N20_InheritedOnClass.java` | `import java.lang.annotation.*; @Inherited class AN_N20_InheritedOnClass {}` | `compiler.err.annotation.type.not.applicable` |

**Контроль ложного negative:** исходный кандидат `@Target(ElementType.TYPE_USE) @interface U16 {} @U16 class SomeClass {}` на **этом JDK21 компилируется (exit 0)**. Поэтому он НЕ включён в 19 compiler-negative. Нельзя просто считать такую комбинацию незаконной по интуитивной формулировке «TYPE_USE только после слова class»; JLS `9.7.4` и конкретная context applicability требуют точной проверки. Включаем в повторное G2 исследование контекстов, а не подгоняем число negatives.

**Контроль runtime vs compile:** `Class.getMethod("missing")` на существующем классе — **runtime NoSuchMethodException** (`RF17`); отказ `RF_N17` вызван совсем другим — *отсутствием catch/throws для checked exception*. `Method.invoke` выбрасывает `InvocationTargetException` **после** вызова target с причиной (`RF16`), а `RF_N18` — статическая ошибка необработанного checked `IllegalAccessException`. `@Retention(CLASS)`/`SOURCE` с runtime `getAnnotation()==null` не ошибка компиляции. **JPMS module-path exports-only `trySetAccessible=false` — runtime**, не compiler-negative.

## 8. Что должна сделать G2 и что реально НЕ проверено

**02 — Knowledge Author:** две связные статьи с trace каждого P0: annotation declaration/use → retention → runtime retrieval (отдельно declared/inherited/repeatable/type-use); Class → inspect → invoke → exceptions → access across modules. Показывать конкретный наблюдаемый код и объяснять *почему* он работает. Не обещать «Reflection гарантирует исходный порядок методов» или «любое private можно открыть», не превращать `CLASS` в runtime. Сопоставить URL A, точный Java21 контракт и отличие от вторичных C/блогов.

**03 — Examples & Exercises Engineer:** если нужен runnable, 2 autonomous main + optional exact JPMS two configurations, доказанные 23/34/3/3 stdout; проверка флагов `--release 21 -Xlint:all` и `java --module-path`, сравнение отдельных эталонов. Изоляция 19 compiler-negative с категориями; **не** смешивать их в один общий javac batch. Изменение private state через reflection в production не предлагать как «практический best practice».

**05 — Knowledge Editor:** естественный грамотный русский, читабельная цепочка зависимостей и реальные контрпримеры, без штампов/ложной глубины; проверить все ссылки как Obsidian local paths после будущей публикации. Развести SOURCE/CLASS/RUNTIME от source processing и JPMS exports vs opens. Не создавать ссылок на пока несуществующие `annotations.md` и `reflection.md` до G2.

**04 — Independent Reviewer:** независимо сверить 35 P0 и A/C, `getAnnotationsByType` vs `getDeclaredAnnotation` на контейнере, `@Inherited` не interface/method, `TYPE_USE` vs field annotation, `RF_NEST` и IllegalAccess, JDK21 strong encapsulation, **настоящие два module-path runs**, метод invoke target/cause, generic erasure, deterministic sorted method names; проверить 19 topical compile-negative, JDK/stdout и границы `not run`. CHANGES → новый HEAD, повторный review.

**Явно NOT RUN / не выдаём за проверенное:** annotation processor JSR269, javap -v class-file proof for CLASS annotations, complex type-use generic arrays, full Javadoc tool `@Documented` output, OS/JDK vendors кроме указанного Debian OpenJDK21, альтернативные компиляторы и модульные graphs, Java agents/instrumentation, compile/build/tests из markdown в GitHub CI, full Spring/ORM/proxy/serialization integration, GUI Obsidian/IDE/Markdown anchors, real performance/JMH/JIT/GC, JVM-LOADING и classloader identity graph, JMM/threads, preview Java21. Full Git tree статически проверен; HTTP/anchor checking не выполнялись.

**Статический link audit:** получить полный рекурсивный GitHub tree по фактическому BASE, `truncated=false`; сосчитать и проверить все уникальные `../` и `../../` Markdown paths из `docs/plans/`. Существование пути ≠ переход по якорю или GUI-rendered graph; не создавать ссылки на будущие файлы.

## 9. Статус и передача эстафеты

| Величина | Статус |
|---|---|
| A/JLS/API21 + реальные C, матрица **35 P0 / 10 P1 / 8 P2** | `G1 PROPOSED / NOT ACCEPTED` |
| Полный AnnotationsProbe.java (23 stdout), ReflectionProbe.java (34 stdout) | Verified local JDK21, `javac -Xlint:all` без warnings, stdout diff |
| 2 named-module export/opens launches, 3+3 stdout | Verified local JDK21 via actual `--module-path` |
| 19 isolated javac negatives, semantic key | Verified local, не GitHub Actions tests |
| Один ошибочно предполагаемый compiler-negative `TYPE_USE` | Выявлен **exit=0**, исключён |
| `knowledge/.../annotations.md` и `reflection.md` | **NOT Implemented** |
| Java sources/examples/status registries/Gradle/CI in this G1 branch | **no changes** |
| GitHub Actions CI exact PR HEAD | Зафиксировать отдельно после создания PR; CI **не компилирует Markdown snippets** |

### Передача эстафеты
Кому: **04 — Independent Reviewer**.
Что сделать: независимое G1-review **точного HEAD исследовательского PR**, 35 P0, JLS/API21, реальные вопросы C, 2 полных Java21 source 23/34 stdout, 2 разных реально запущенных JPMS scenarios 3/3 stdout, 19 compiler-negative, nestmate исправление, относительные ссылки и `not run`; выдать `PASS` или `CHANGES` с конкретными блокерами/строками/нормативными основаниями. После изменения HEAD повторить review; только после PASS передать **00** для G1 ACCEPTED/BLOCKED/merge; не запускать G2.
Передать: оператор пересылает **прямую ссылку на отдельный адресный опубликованный PR-комментарий**, а не URL PR body.
