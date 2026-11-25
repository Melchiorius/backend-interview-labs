# CORE-NESTED + CORE-FUNCTIONS — вложенные классы и лямбды в Java 21

**Один автономный запускаемый пример**, а не обязательное упражнение. Он связывает ситуации, которые легко перепутать на интервью: чем `static nested` отличается от `inner`, какой `this` видят локальные и анонимные классы, какие локальные значения захватывает лямбда и *когда* вычисляется получатель ссылки на метод. Наблюдения `P01–P17` основаны на §5 [принятого исследовательского плана](../../../docs/plans/CORE-NESTED-FUNCTIONS.md). Исторический план использует `hi` в P10; исправленный runnable B4a использует `abc` и `ABC`, не меняя смысл ссылки на метод. **17 строк — один запуск программы**, не 17 независимых тестов.

**Файлы:** [NestedFunctionsProbes.java](src/NestedFunctionsProbes.java), [полный эталон expected.stdout](expected.stdout), [проверочный verify.sh](verify.sh). Единственная точка входа — `NestedFunctionsProbes.main` (default package), никаких внешних зависимостей, JUnit, Mockito или нового Gradle-модуля нет.

Для понимания примера полезны [CORE-INTERFACES](../../../knowledge/01-java-core/interfaces.md) — SAM и `@FunctionalInterface` — и [CORE-CLASSES](../../../knowledge/01-java-core/classes.md) — объекты и поля. Две основные статьи находятся в том же дереве репозитория: [CORE-NESTED — вложенные классы и захват](../../../knowledge/01-java-core/nested-classes.md) и [CORE-FUNCTIONS — SAM, лямбды и ссылки на методы](../../../knowledge/01-java-core/functions.md). Первая подробно объясняет P01–P08/P15–P16, вторая — P09–P14/P17; это распределение тем, а не два запуска программы. Из каждой статьи есть обратная ссылка на этот README и исходник.

## Запуск из корня репозитория

Нужен **JDK 21** с `javac` и `java` в `PATH`. Из корня клонированного репозитория (первые две команды подходят также для Windows PowerShell):

```sh
javac --release 21 -d examples/core/nested-functions/build/classes examples/core/nested-functions/src/NestedFunctionsProbes.java
java -cp examples/core/nested-functions/build/classes NestedFunctionsProbes
```

Для проверки точного полного вывода (Bash, включая Git Bash на Windows):

```sh
java -version
javac -version
bash examples/core/nested-functions/verify.sh
```

`verify.sh` очищает **только** `examples/core/nested-functions/build/`, компилирует `javac --release 21`, запускает программу и сохраняет `build/actual.stdout`. `diff -u` сравнивает **все** байты с [expected.stdout](expected.stdout). Любое несовпадение или ошибка программы приводит к ненулевому exit code. При успехе скрипт печатает сохранённые 17 строк программы и **дополнительную** строку `CHECK PASSED: stdout matches expected.stdout`; последняя не является частью Java stdout. Каталог `build/` исключён существующим `.gitignore`.

## Полный ожидаемый stdout Java-программы

```text
P01=68
P02=67
P03=70
P04=71
P05=73
P06=138
P07=false,true
P08=68
P09=68
P10=67,ABC,2,67
P11=true,true
P12=1,3
P13=int
P14=io
P15=67
P16=67,67
P17=68
```

## Разбор результата

| Строки | Механизм |
|---|---|
| `P01–P02` | `static Nested` создаётся без внешнего экземпляра и самостоятельно возвращает **68**. `Inner` создаётся с `this.new Inner()` и через `NestedFunctionsProbes.this.value` читает **67** у конкретного внешнего объекта. Результат `Nested.get()` не является чтением внешнего поля. |
| `P03–P05` | **Java 21** разрешает `static`-поле и `static`-инициализатор в обычных inner, local и anonymous classes. Инициализация увеличивает **69→70**, **70→71** и **72→73**. Важно: `static class Local` *по-прежнему не компилируется*, поскольку это модификатор **самого локального класса**, а не его static-член. |
| `P06` | Локальный `Local` читает effectively-final `captured=67` и `Local.marker=71`; сумма **138**. |
| `P07` | `this` внутри анонимного `Supplier` обозначает его собственный объект, поэтому сравнение с внешним объектом — **false**. У лямбды `this` лексически относится к внешнему `NestedFunctionsProbes` — **true**. |
| `P08` | Ссылка `box` захвачена без переназначения локальной переменной, но содержимое массива можно изменить с 67 на **68**. Захват effectively-final ссылки не делает объект immutable или thread-safe. |
| `P09–P10` | `IntUnaryOperator` задаёт lambda target type. Далее четыре вида method reference: `String::valueOf` (static), `"abc"::toUpperCase` (bound), `String::length` (unbound, получатель передаётся как аргумент SAM) и `Widget::new` (constructor). Получается `67,ABC,2,67`. |
| `P11–P12` | Выражение `missing::trim` выбрасывает NPE **в момент создания bound reference**. Лямбда `() -> missing.trim()` создаётся и бросает NPE лишь при `get()`. `receiver(calls)::toUpperCase` вызывает фабрику получателя **один раз**, поэтому после двух `get()` счётчик 1; лямбда `() -> receiver(calls).toUpperCase()` ещё дважды вызывает фабрику, итог **3**. |
| `P13–P14` | Приведение lambda к `IntUnaryOperator` разрешает перегрузку `choose`; `Checked.run()` объявляет `IOException`, поэтому соответствующая лямбда может бросить это checked exception. У `Runnable.run()` такого объявления нет. |
| `P15–P17` | Локальный `record LocalRecord` не захватывает переменные метода (он implicitly static); anonymous/lambda оба читают effectively-final `captured`; переобъявленный публичный `equals(Object)` из `Object` не добавляет вторую функциональную операцию `Transform`. |

## B4a — default locale и технический stdout

No-arg [String.toUpperCase()](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#toUpperCase()) использует default locale при вызове метода, а не при создании bound reference. Исходное слово hi даёт HI при en_US, но Hİ (U+0048 U+0130) при tr_TR. Выбор abc сохраняет bound reference с SAM Supplier<String> без аргументов и даёт ABC в проверенных конфигурациях. Это не превращает toUpperCase() в locale-independent API.

Для нейтрального технического преобразования произвольной строки явно передавайте [Locale.ROOT](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Locale.html#ROOT) в [toUpperCase(Locale)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#toUpperCase(java.util.Locale)). Сравните SAM-арности:

~~~java
String text = "hi";
Supplier<String> technical = () -> text.toUpperCase(Locale.ROOT);
Function<Locale, String> boundWithLocale = text::toUpperCase;
BiFunction<String, Locale, String> unboundWithLocale = String::toUpperCase;
// technical.get(), boundWithLocale.apply(Locale.ROOT)
// и unboundWithLocale.apply(text, Locale.ROOT) дают HI.
~~~

Это альтернативы с import java.util.Locale и java.util.function.*: первая — lambda, вторая — bound reference с одним аргументом SAM, третья — unbound с receiver и locale. Для человекочитаемого текста выбор локали определяется языком пользователя, а не обязательно ROOT. Никакой глобальной смены или фиксации локали в runnable/runner нет.

**Фактическая локальная проверка 03:** Debian OpenJDK/javac 21.0.12.1, LC_ALL=C; шесть полных запусков baseline/final с временными PATH-local java wrappers (один абсолютный java, флаги JVM перед аргументами), без JAVA_TOOL_OPTIONS/JDK_JAVA_OPTIONS/_JAVA_OPTIONS. Default — только -Dstdout.encoding=UTF-8, en_US и tr_TR дополнительно -Duser.language=en/tr -Duser.country=US/TR. Отдельная Java probe под теми же wrappers подтвердила actual Locale.getDefault() = en_US/en_US/tr_TR, System.out.charset() и stdout.encoding = UTF-8 во всех трёх. file.encoding и default charset UTF-8, native.encoding ANSI_X3.4-1968 (LC_ALL=C). Это JVM-конфигурации, не проверка всех OS/locales.

| JVM config | Baseline exit / P10 | Final exit / P10 |
|---|---|---|
| Default (actual en_US) | 0 / 67,HI,2,67 | 0 / 67,ABC,2,67 |
| en_US | 0 / 67,HI,2,67 | 0 / 67,ABC,2,67 |
| tr_TR | 1 / 67,Hİ,2,67 | 0 / 67,ABC,2,67 |

Во всех шести запусках stderr пуст; baseline tr_TR завершился на diff (без CHECK PASSED), все final stdout содержат точные 17 строк expected.stdout и исходный финальный маркер, побайтно одинаковы. 16 non-P10 lines и другие поля P10 сохранены, P11/P12 неизменны. Исходный runner mode100755 и blob сохранены; source/fixture/runner blobs сверены (3/3 baseline, 3/3 final). Прямые команды compile/run без JVM locale flags также дали точный final fixture. Команды, параметры wrappers и хэши — в technical PR. Full Gradle/JUnit/clean clone/GUI locally NOT RUN; exact CI отдельно. Не утверждаем инвариантность произвольного Unicode или всех локалей/JDK.

## Негативные случаи и границы

В [§6 исследовательского плана](../../../docs/plans/CORE-NESTED-FUNCTIONS.md) содержатся **N01–N20 — двадцать независимых намеренно некомпилируемых Java-фрагментов** с объяснением причин: отсутствие enclosing instance, нарушение effectively-final, неверный target type, checked exception, локальный `static class`, конфликт перегрузок и т. д. **Они не входят в положительную компиляцию**. Не следует считать, что этот `verify.sh` проверяет их; точный текст javac diagnostics не является универсальной нормой JLS. Вынесение всех двадцати в отдельный committed CI-harness или обязательный exercise без дополнительной пользы не требуется.

**Не гарантируются JLS:** новая identity объекта лямбды при каждом вычислении, конкретное имя/класс лямбды, число выделений памяти, определённый механизм classfile/HotSpot/invokedynamic или производительность по сравнению с anonymous class. `AtomicInteger` здесь — детерминированный счётчик **последовательных** вызовов, а не демонстрация concurrency. Этот runnable не проверяет thread safety и не является benchmark.

Нормативные ориентиры: JLS Java SE 21 §8.1.3, §14.3, §15.9.5, §15.13, §15.27 и соответствующие функциональные API. **Точный локальный JDK, команды, результаты CI и ограничения проверки фиксируются в PR после фактического запуска.** Другие JDK/компиляторы, визуальный Obsidian/IntelliJ, все негативные случаи N01–N20 в рамках этого `verify.sh` — **not run**.
