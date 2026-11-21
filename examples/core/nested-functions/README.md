# CORE-NESTED + CORE-FUNCTIONS — вложенные классы и лямбды в Java 21

**Один автономный запускаемый пример**, а не обязательное упражнение. Он связывает ситуации, которые легко перепутать на интервью: чем `static nested` отличается от `inner`, какой `this` видят локальные и анонимные классы, какие локальные значения захватывает лямбда и *когда* вычисляется получатель ссылки на метод. Все наблюдения `P01–P17` повторяют §5 [принятого исследовательского плана](../../../docs/plans/CORE-NESTED-FUNCTIONS.md). **17 строк — один запуск программы**, не 17 независимых тестов.

**Файлы:** [NestedFunctionsProbes.java](src/NestedFunctionsProbes.java), [полный эталон expected.stdout](expected.stdout), [проверочный verify.sh](verify.sh). Единственная точка входа — `NestedFunctionsProbes.main` (default package), никаких внешних зависимостей, JUnit, Mockito или нового Gradle-модуля нет.

Из опубликованной теории применимы [CORE-INTERFACES](../../../knowledge/01-java-core/interfaces.md) — SAM и `@FunctionalInterface` — и [CORE-CLASSES](../../../knowledge/01-java-core/classes.md) — объекты и поля. **Обе самостоятельные статьи уже включены в общий авторский PR:** [CORE-NESTED — вложенные классы и захват](../../../knowledge/01-java-core/nested-classes.md) и [CORE-FUNCTIONS — SAM, лямбды и method references](../../../knowledge/01-java-core/functions.md). Первая подробно объясняет P01–P08/P15–P16, вторая — P09–P14/P17; это распределение тем, а не два запуска программы. Из каждой статьи есть обратная ссылка на этот README и исходник.

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
P10=67,HI,2,67
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
| `P09–P10` | `IntUnaryOperator` задаёт lambda target type. Далее четыре вида method reference: `String::valueOf` (static), `"hi"::toUpperCase` (bound), `String::length` (unbound, получатель передаётся как аргумент SAM) и `Widget::new` (constructor). Получается `67,HI,2,67`. |
| `P11–P12` | Выражение `missing::trim` выбрасывает NPE **в момент создания bound reference**. Лямбда `() -> missing.trim()` создаётся и бросает NPE лишь при `get()`. `receiver(calls)::toUpperCase` вызывает фабрику получателя **один раз**, поэтому после двух `get()` счётчик 1; лямбда `() -> receiver(calls).toUpperCase()` ещё дважды вызывает фабрику, итог **3**. |
| `P13–P14` | Приведение lambda к `IntUnaryOperator` разрешает перегрузку `choose`; `Checked.run()` объявляет `IOException`, поэтому соответствующая лямбда может бросить это checked exception. У `Runnable.run()` такого объявления нет. |
| `P15–P17` | Локальный `record LocalRecord` не захватывает переменные метода (он implicitly static); anonymous/lambda оба читают effectively-final `captured`; переобъявленный публичный `equals(Object)` из `Object` не добавляет вторую функциональную операцию `Transform`. |

## Негативные случаи и границы

В [§6 исследовательского плана](../../../docs/plans/CORE-NESTED-FUNCTIONS.md) содержатся **N01–N20 — двадцать независимых намеренно некомпилируемых Java-фрагментов** с объяснением причин: отсутствие enclosing instance, нарушение effectively-final, неверный target type, checked exception, локальный `static class`, конфликт перегрузок и т. д. **Они не входят в положительную компиляцию**. Не следует считать, что этот `verify.sh` проверяет их; точный текст javac diagnostics не является универсальной нормой JLS. Вынесение всех двадцати в отдельный committed CI-harness или обязательный exercise без дополнительной пользы не требуется.

**Не гарантируются JLS:** новая identity объекта лямбды при каждом вычислении, конкретное имя/класс лямбды, число выделений памяти, определённый механизм classfile/HotSpot/invokedynamic или производительность по сравнению с anonymous class. `AtomicInteger` здесь — детерминированный счётчик **последовательных** вызовов, а не демонстрация concurrency. Этот runnable не проверяет thread safety и не является benchmark.

Нормативные ориентиры: JLS Java SE 21 §8.1.3, §14.3, §15.9.5, §15.13, §15.27 и соответствующие функциональные API. **Точный локальный JDK, команды, результаты CI и ограничения проверки фиксируются в PR после фактического запуска.** Другие JDK/компиляторы, визуальный Obsidian/IntelliJ, все негативные случаи N01–N20 в рамках этого `verify.sh` — **not run**.
