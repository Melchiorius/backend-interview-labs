# CORE-EXCEPTIONS + CORE-RESOURCES — один запускаемый пример Java 21

**Зачем этот пример:** короткие фрагменты показывают `catch`, `finally` или один ресурс по отдельности, но хуже объясняют **связную последовательность** при двух ресурсах и нескольких одновременно возникающих исключениях. Здесь все операции воспроизводятся на одном `AutoCloseable` (`R`), общем `StringBuilder`-журнале и одном `main`: видно, кто успел открыться, когда выполняется `close()`, какое исключение остаётся *primary* и какие попадают в `getSuppressed()`. Контраст `finally` и try-with-resources (TWR) — смысл примера, а не упражнение ради наличия файлов.

Связанные статьи: [CORE-EXCEPTIONS](../../../knowledge/01-java-core/exceptions.md) и [CORE-RESOURCES](../../../knowledge/01-java-core/resources.md). Исходное исследование с нормативными ссылками, отдельными отрицательными примерами и полными пояснениями: [G1-план CORE-EXCEPTIONS/RESOURCES](../../../docs/plans/CORE-EXCEPTIONS-RESOURCES.md). Обе статьи находятся в том же дереве репозитория; относительные ссылки разрешаются от данного README.

Файлы: [ExceptionResourceProbes.java](src/ExceptionResourceProbes.java), [expected.stdout](expected.stdout), [verify.sh](verify.sh). **Одна компиляция и один запуск дают 20 подписанных строк P01–P20** — это не 20 независимых тестовых программ. Java-операторы и наблюдаемые строки из §5 принятого G1-плана сохранены; добавлены только три комментария `// Theory/Research:` в заголовке.

## Запуск из корня репозитория

Требуется установленный **JDK 21** (`javac` и `java` в `PATH`). В Bash / Git Bash на Windows:

```sh
bash examples/core/exceptions-resources/verify.sh
```

Либо вручную из корня репозитория:

```sh
mkdir -p examples/core/exceptions-resources/build/classes
javac --release 21 -d examples/core/exceptions-resources/build/classes examples/core/exceptions-resources/src/ExceptionResourceProbes.java
java -cp examples/core/exceptions-resources/build/classes ExceptionResourceProbes
```

`verify.sh` удаляет **только свой** каталог `examples/core/exceptions-resources/build/`, компилирует `--release 21`, запускает класс и сравнивает **весь stdout** с [эталоном](expected.stdout) с помощью `diff -u`. Ошибка компиляции, запуска или несовпадение приводят к ненулевому exit code. После 20 строк Java-вывода скрипт допечатывает собственную строку `CHECK PASSED: stdout matches expected.stdout` (она **не входит** в файл эталона). Отдельный Gradle-модуль, JUnit и Mockito не нужны.

## Полный ожидаемый stdout Java-программы

```text
P01=true,true
P02=io
P03=service,IOException,io
P04=67
P05=68
P06=IllegalStateException,finally,0
P07=unchecked
P08=IOException
P09=open-A;open-B;body;close-B;close-A;
P10=open-A;open-B;body;close-B;close-A;primary-work,supp-[close-B, close-A]
P11=open-A;open-B;body;close-B;close-A;primary-close-B,supp-[close-A]
P12=open-A;init-fail;close-A;primary-init-B,supp-0
P13=open-A;init-fail;close-A;primary-init-B,supp-[close-A]
P14=body;
P15=open-E;body;close-E;
P16=open-A;body;close-A;finally;
P17=close-X,supp-0
P18=body
P19=auto
P20=other
```

## Механизмы и причинно-следственная связь

| Наблюдение | Что фактически происходит |
|---|---|
| `P01–P03` | P01 выводит результаты двух `instanceof`: `IOException` относится к `Exception`, `AssertionError` — к `Error`. Это наблюдение об иерархии, а не проверка правил компиляции checked exceptions. По JLS `IOException` — checked, `AssertionError` — unchecked. P02 выводит сообщение перехваченного `IOException`. В `wrappedOperation()` перехваченный объект передаётся конструктору `IllegalStateException` как причина; P03 выводит сообщение обёртки, класс и сообщение причины, но не сравнивает ссылки на объекты. |
| `P04–P06` | В `finallyValue()` вычисленное возвращаемое значение 67 уже выбрано до последующей записи `n=68`. В `finallyOverrideReturn()` новый `return 68` **затеняет** исходный `return 67`. В `finallyOverrideThrow()` новый `IllegalStateException("finally")` заменяет первоначальный `IOException("work")`; старый не становится автоматически suppressed (`0`). Так писать обычно не следует: теряется исходная причина. |
| `P07–P08` | Unchecked `IllegalArgumentException` перехватывается как `RuntimeException`; корректный multi-catch `IOException | IllegalStateException` обрабатывает checked-исключение. |
| `P09` | Два ресурса успешно создаются по порядку **A → B**, затем выполняется `body`, закрытие — **B → A**. Последовательность записана в едином журнале. |
| `P10` | Тело выбрасывает `IOException("work")`, оба `close()` тоже падают. **Primary = work**, suppressed идут в порядке фактического закрытия: `close-B`, `close-A`. |
| `P11` | Тело выполнилось успешно, но оба `close()` падают. **Primary = close-B**, ошибка `close-A` становится suppressed. |
| `P12–P13` | Создание A прошло, создание B (`failedInit`) выбрасывает `init-B`. Тело не начинается, B не существует, но ранее созданный **A закрывается**. Если `A.close()` успешен — suppressed нет; если падает — `close-A` suppressed у primary `init-B`. |
| `P14` | `try(none)` с `none == null` законен. Тело выполняется, вызов `close()` пропущен. |
| `P15` | Существующую **effectively-final** локальную переменную `existing` можно использовать как ресурс TWR. Ссылка не переназначается; объект закрывается. Это не гарантия безопасного повторного закрытия. |
| `P16` | Внешний `finally` выполняется после попыток закрыть зарегистрированные ненулевые ресурсы TWR (при обычном выполнении или разматывании стека). В данном сценарии `close()` возвращается нормально, а журнал показывает `body;close-A;finally;`. Запись `close-A` фиксирует вызов метода, а не гарантированное освобождение реального ресурса. |
| `P17–P19` | `close-X` может стать primary даже при пустом теле; `AutoCloseable.close()` может объявлять `Exception` шире `Closeable.close()`, и проверка checked exceptions применяется к вызову TWR. |
| `P20` | P20 выводит только сообщение первого suppressed-исключения (`other`). По контракту `Throwable` добавление suppressed не меняет cause, но эта строка не проверяет ни идентичность причины, ни её отсутствие до и после вызова. |

**Важные ограничения:** `finally` не гарантируется при `Runtime.halt`, kill процесса и других ситуациях, когда выполнение JVM прекращается без нормального разматывания стека; TWR не гарантирует успешность `close()` и не делает `close()` идемпотентным. `AutoCloseable.close()` не означает JDBC commit/rollback или освобождение произвольных внешних ресурсов вне его контракта. `R` здесь — специально созданный ресурс с предсказуемым поведением, **не** реальный файловый поток, соединение JDBC или компонент Spring. Отсутствие исключения в одном наблюдении не является доказательством общей exception safety.

## Отрицательные компиляции и границы проверки

В [§6 G1-плана](../../../docs/plans/CORE-EXCEPTIONS-RESOURCES.md) находятся **N01–N20**, двадцать *самостоятельных* специально некомпилируемых Java-исходников. Они не входят в positive `ExceptionResourceProbes.java` и не проверяются этим `verify.sh`/CI. При ручном переносе multi-catch из Markdown-таблицы `\\|` — это экранирование вертикальной черты для Markdown: фактический Java source должен содержать **`|`**, не `\\|`. Требуется сверять **категорию** ошибки (например, unhandled checked, catch order, effectively-final, несовместимый ресурс), а не только ненулевой exit и не точный текст diagnostic, зависящий от версии `javac`.

**Что проверяет пример:** локальную компиляцию на JDK 21 и детерминированный stdout одного запуска. **Что не следует считать выполненным по этому скрипту:** индивидуальную проверку всех N01–N20, реальный IO/JDBC, Windows PowerShell без Bash, визуальный Obsidian/IntelliJ, тесты shutdown/kill, другие JDK/JVM, многопоточность, benchmark. Исходный пример и статьи опубликованы в PR [#105](https://github.com/Melchiorius/backend-interview-labs/pull/105). Исторический stdout сохранён без изменений. Отдельные проверки статической обязанности обработать или объявить checked exception и сохранения либо отсутствия cause при `addSuppressed()` относятся к доработке [#340](https://github.com/Melchiorius/backend-interview-labs/pull/340); их временные исходники и результаты фиксируются в отчёте инженера, а не входят в P01–P20 или CI этого runner. Актуальные статусы публикации и приёмки отражаются в `COVERAGE/CURRICULUM` и решениях координатора; успешный runner сам по себе не подтверждает полноту материала.
