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

## B4a — локаль по умолчанию и воспроизводимый вывод

Метод без параметров [`String.toUpperCase()`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#toUpperCase()) использует локаль по умолчанию при вызове метода, а не при создании ссылки на метод конкретного объекта (bound). Исходное слово `hi` даёт `HI` при `en_US`, но `Hİ` (U+0048 U+0130) при `tr_TR`. Причина — разные правила преобразования регистра, а не кодировка. Выбор `abc` сохраняет ссылку на метод с целевым типом `Supplier<String>`, чей метод `get()` не принимает аргументов, и даёт `ABC` в проверенных конфигурациях. Сам метод `toUpperCase()` остаётся зависимым от локали по умолчанию.

Для нейтрального технического преобразования регистра строки явно передавайте [`Locale.ROOT`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Locale.html#ROOT) в [`toUpperCase(Locale)`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#toUpperCase(java.util.Locale)). Сравните число параметров единственного абстрактного метода (SAM) целевого интерфейса:

~~~java
String text = "hi";
Supplier<String> technical = () -> text.toUpperCase(Locale.ROOT);
Function<Locale, String> boundWithLocale = text::toUpperCase;
BiFunction<String, Locale, String> unboundWithLocale = String::toUpperCase;
// technical.get(), boundWithLocale.apply(Locale.ROOT)
// и unboundWithLocale.apply(text, Locale.ROOT) дают HI.
~~~

Для этих вариантов нужны импорты `java.util.Locale` и `java.util.function.*`. В первом случае используется лямбда, а не ссылка на метод: `get()` принимает ноль аргументов. Во втором — ссылка на метод конкретного объекта (bound): `apply(Locale)` принимает один аргумент, локаль. В третьем — ссылка с передаваемым получателем (unbound): `apply` принимает два аргумента, строку и локаль. Для текста, предназначенного человеку, локаль выбирают с учётом языка пользователя, не обязательно `Locale.ROOT`. Ни программа, ни проверочный скрипт не меняют и не фиксируют глобальную локаль.

**Фактическая локальная проверка роли 03.** [Отчёт о шести полных запусках](https://github.com/Melchiorius/backend-interview-labs/pull/285#issuecomment-7160192368) относится к технической версии `c2c7f0e1b8a0ccb6bb8b07ab78a4dd56dd3c5f02`. Исходную и исправленную версии проверили на Debian OpenJDK/javac 21.0.12.1 при `LC_ALL=C`, каждую в трёх конфигурациях JVM.

Для испытаний временную обёртку команды `java` помещали первой в `PATH`. Она передавала параметры JVM одному и тому же исполняемому файлу по абсолютному пути `/usr/lib/jvm/java-21-openjdk-amd64/bin/java` перед остальными аргументами команды. Во всех случаях задавали `-Dstdout.encoding=UTF-8`. Для запуска с локалью по умолчанию других параметров локали не было; для `en_US` добавляли `-Duser.language=en -Duser.country=US`, для `tr_TR` — `-Duser.language=tr -Duser.country=TR`. Переменные `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS` и `_JAVA_OPTIONS` были сняты. Обёртки находились вне репозитория.

Отдельная программа, запущенная в тех же условиях, подтвердила значения `Locale.getDefault()`: `en_US`, `en_US`, `tr_TR` соответственно. Во всех трёх конфигурациях `System.out.charset()`, `stdout.encoding`, `file.encoding` и кодировка по умолчанию JVM — `UTF-8`; `native.encoding` — `ANSI_X3.4-1968` при `LC_ALL=C`. Это проверка конфигураций JVM, а не системных локалей разных ОС.

В таблице указан код завершения проверочного скрипта и значение P10, полученное из сохранённого вывода самой Java-программы (`build/actual.stdout`), в том числе при неуспешном сравнении.

| Конфигурация JVM | Исходная версия: код завершения / P10 | Исправленная версия: код завершения / P10 |
|---|---|---|
| По умолчанию (фактически `en_US`) | 0 / `67,HI,2,67` | 0 / `67,ABC,2,67` |
| `en_US` | 0 / `67,HI,2,67` | 0 / `67,ABC,2,67` |
| `tr_TR` | 1 / `67,Hİ,2,67` | 0 / `67,ABC,2,67` |

Во всех шести запусках поток ошибок (`stderr`) был пуст, а вывод Java-программы состоял из 17 строк. В исходной версии при `tr_TR` команда `diff -u` обнаружила единственное расхождение в P10 (`HI` вместо фактического `Hİ`); скрипт завершился с кодом 1, напечатав 11 строк отчёта о различии, без маркера успеха. Во всех трёх запусках исправленной версии сравнение завершилось с кодом 0. Скрипт напечатал 18 строк: ровно 17 строк из `expected.stdout` и исходный маркер `CHECK PASSED: stdout matches expected.stdout`. Эти три вывода совпали побайтно.

Остальные 16 строк, другие поля P10 и результаты P11/P12 сохранены. Проверочный скрипт не изменился: режим Git `100755` и хэш содержимого прежние. Хэши трёх файлов (исходник Java, эталон вывода и скрипт) сверены для каждой версии — 3 из 3. Прямые команды компиляции и запуска без параметров локали JVM также дали точный эталон исправленной версии. Команды, хэши и результаты измерений приведены в [отчёте 03](https://github.com/Melchiorius/backend-interview-labs/pull/285#issuecomment-7160192368).

Полная сборка Gradle, тесты JUnit, проверка в заново клонированном репозитории и графическом интерфейсе локально ролью 03 не выполнялись. Отдельное свидетельство — [интеграционный CI #45599538553](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45599538553) для прежнего HEAD `98084b761925636a0db23637f89a105793304b93`: все 34 шага успешны, в том числе проверка этого скрипта. Это не отдельный прогон в трёх локалях и не результат CI для последующих версий. Проверки не доказывают инвариантность произвольных строк Unicode для всех локалей, ОС или версий JDK.

## Негативные случаи и границы

В [§6 исследовательского плана](../../../docs/plans/CORE-NESTED-FUNCTIONS.md) содержатся **N01–N20 — двадцать независимых намеренно некомпилируемых Java-фрагментов** с объяснением причин: отсутствие enclosing instance, нарушение effectively-final, неверный target type, checked exception, локальный `static class`, конфликт перегрузок и т. д. **Они не входят в положительную компиляцию**. Не следует считать, что этот `verify.sh` проверяет их; точный текст javac diagnostics не является универсальной нормой JLS. Вынесение всех двадцати в отдельный committed CI-harness или обязательный exercise без дополнительной пользы не требуется.

**Не гарантируются JLS:** новая identity объекта лямбды при каждом вычислении, конкретное имя/класс лямбды, число выделений памяти, определённый механизм classfile/HotSpot/invokedynamic или производительность по сравнению с anonymous class. `AtomicInteger` здесь — детерминированный счётчик **последовательных** вызовов, а не демонстрация concurrency. Этот runnable не проверяет thread safety и не является benchmark.

Нормативные ориентиры: JLS Java SE 21 §8.1.3, §14.3, §15.9.5, §15.13, §15.27 и соответствующие функциональные API. **Точный локальный JDK, команды, результаты CI и ограничения проверки фиксируются в PR после фактического запуска.** Другие JDK/компиляторы, визуальный Obsidian/IntelliJ, все негативные случаи N01–N20 в рамках этого `verify.sh` — **not run**.
