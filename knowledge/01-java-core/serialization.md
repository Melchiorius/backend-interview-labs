# CORE-SERIAL — сериализация Java 21: граф объектов, версии, инварианты и безопасность

**Что решаем.** Программа сохраняет объект и связанные с ним объекты в последовательности байтов, а затем восстанавливает граф. Нативная сериализация Java — не копирование участка памяти: поток хранит описания классов, значения полей и ссылки; при чтении выполняются специальные правила создания экземпляров. Два одинаково выглядящих объекта не обязаны иметь одинаковую идентичность, а повторное упоминание **одного** объекта внутри потока может сохранить разделяемую ссылку.

**Важное ограничение безопасности:** все приведённые примеры десериализуют **только байты, созданные тем же примером из собственных доверенных объектов** через `ByteArrayOutputStream` → `byte[]` → `ByteArrayInputStream`. Никаких публичных или сетевых десериализаторов, чтения внешних файлов, чужих сериализованных байтов, gadget payloads и попыток обойти фильтры. Нативную Java-десериализацию **не следует применять к недоверенным данным**: корректный локальный учебный запуск не доказывает безопасность production-системы.

Нормативные источники **Java SE 21 (A)**: [Serializable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Serializable.html), [ObjectOutputStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ObjectOutputStream.html), [ObjectInputStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ObjectInputStream.html), [ObjectInputFilter](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ObjectInputFilter.html), [@Serial](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Serial.html); [спецификация сериализации: архитектура](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/serial-arch.html), [вход](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/input.html), [протокол](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/protocol.html), [версии](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/version.html); [рекомендации Oracle по безопасному коду](https://www.oracle.com/java/technologies/javase/seccodeguide.html). Полные локально проверенные G1-программы и отрицательные компиляционные случаи — в [принятом плане G1](../../docs/plans/CORE-I18N-SERIAL.md) (§4, §6–§7). Действующие соседние темы: [CORE-RECORDS](records.md), [CORE-EXCEPTIONS](exceptions.md), [CORE-RESOURCES](resources.md), [CORE-IMMUTABLE](immutability.md), [CORE-REFLECTION](reflection.md), [CORE-CLASSES](classes.md) и [CORE-I18N](internationalization.md).

**Карта:** S-01–S-27 = **27 P0**; S28–S34 = **7 P1**; S35–S39 = **5 P2**. P2 — исследовательские границы, **не подтверждённое исполнение**. Количество пунктов не равно количеству тестов: исторический `SerializationProbe` напечатал **20 строк S01–S20**, не 20 JUnit-тестов. Запускаемый комплект G2 интегрирован: [CI #45184055313](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45184055313) completed/SUCCESS на авторском `95bc9a0b329491a1e8b9670d4636760c88a47d88`, отдельно от технического CI и его SHA.

**Практика G2:** [полный SerializationProbe.java](../../examples/core/i18n-serialization/src/SerializationProbe.java), [полный stdout-эталон (20 строк)](../../examples/core/i18n-serialization/serial.expected.stdout) и [инструкция с безопасным verify.sh](../../examples/core/i18n-serialization/README.md). Вместе с [I18nProbe.java](../../examples/core/i18n-serialization/src/I18nProbe.java) получаем **20+29=49 строк стандартного вывода двух отдельных JVM, не 49 JUnit-тестов**. Оба Java body побайтово совпадают с принятым G1 §6 после удаления одного комментария `// Theory:`; оба эталона совпадают с G1. [Technical Actions #45182432517](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45182432517) completed/SUCCESS **на `648924427813af86c790ea4aaac32ba529b0f86d`** (отдельный technical HEAD); интегрированный [CI #45184055313](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45184055313) completed/SUCCESS именно на авторском `95bc9a0b329491a1e8b9670d4636760c88a47d88`. Проверяются одна чистая Java 21 компиляция, **две JVM**, два полных сравнения stdout/числа строк/SHA-256. `SerializationProbe` принимает **только собственные доверенные локально записанные ByteArray-байты**, не читает внешние файлы/сеть/пользовательский ввод. `UNDECIDED` не эквивалентен запрету, `S10=InvalidClassException:filter` — следствие **REJECTED**, а не UID mismatch. **12 compiler-negative и 2 `@Serial` warning-only — только отдельная локальная проверка 03, не штатный CI**; production security/two-build UID/другие JDK NOT RUN.

## 1. Serializable, потоки и тип возвращаемого значения (S-01, S-02, S-13, S-27)

`Serializable` — **маркерный интерфейс** без требуемого метода `serialize()`. `ObjectOutputStream.writeObject` записывает сериализуемый объект и достижимое состояние его графа по правилам протокола; его возвращаемый тип — `void`. `ObjectInputStream.readObject` возвращает `Object`: нужный тип программы следует проверять отдельно, а не присваивать результат непосредственно `String` без приведения.

~~~java
import java.io.*;

record Message(String text, int count) implements Serializable { }

ByteArrayOutputStream bytes = new ByteArrayOutputStream();
try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
    out.writeObject(new Message("ready", 67));
}
byte[] ownBytes = bytes.toByteArray(); // создано самим примером
try (ObjectInputStream in = new ObjectInputStream(
        new ByteArrayInputStream(ownBytes))) {
    Object restored = in.readObject();
    if (!(restored instanceof Message message)) {
        throw new IOException("Unexpected trusted object type");
    }
    System.out.println(message.text() + ":" + message.count()); // ready:67
}
~~~

Это **учебный код для заранее доверенных данных**, не API для произвольного `byte[]` извне. Операции записи и чтения могут порождать `IOException` и `ClassNotFoundException`; `NotSerializableException` — разновидность `ObjectStreamException` и возникает **при исполнении**, когда граф не соответствует требованиям сериализации. `new Object()` нельзя записать обычным `writeObject`, хотя соответствующий Java source прекрасно компилируется. В отличие от этого `String x = in.readObject();` — **ошибка типов во время javac**, пока нет приведения.

## 2. serialVersionUID, дескрипторы классов и версия (S-03, S-04, S-23, S-24)

Дескриптор обычного `Serializable`-класса содержит информацию о его структуре и идентификаторе версии `serialVersionUID`. Явное объявление `private static final long serialVersionUID = 67L;` делает UID предсказуемым, но **не превращает произвольные изменения класса в совместимые**: допустимость добавления/удаления полей и изменение иерархии регулирует спецификация версионирования.

~~~java
import java.io.*;

class Entry implements Serializable {
    @Serial private static final long serialVersionUID = 67L;
    String name;
}
System.out.println(ObjectStreamClass.lookup(Entry.class).getSerialVersionUID());
// 67
~~~

При несовпадении несовместимых UID обычного класса и дескриптора потока чтение может закончиться `InvalidClassException`. **Отдельный two-build/two-version опыт не выполнялся**: не называем локальную проверку фильтра тестом версионной совместимости. Имя `InvalidClassException` также встречается при отклонении `ObjectInputFilter`; причина и контекст важнее одного класса исключения.

**У record специальные правила.** Serializable record использует компоненты и канонический конструктор; спецификация не накладывает на него те же требования совпадения UID, что на обычные классы. В [G1 §6.2](../../docs/plans/CORE-I18N-SERIAL.md) `ObjectStreamClass.lookup(Receipt.class).getSerialVersionUID()` дал `0`, а у обычного `Node` — `67`. Это наблюдение не доказывает, что любая смена record-компонентов остаётся совместимой.

## 3. transient и static: что оказывается в потоке (S-05, S-06)

По стандартным правилам сериализации значения **обычных полей экземпляра** участвуют в сохранении. Поле `transient` исключено из стандартного списка и при чтении получает значение по умолчанию (`null` для ссылки), если код восстановления не устанавливает его другим способом. Поле `static` принадлежит классу, поэтому **не является состоянием экземпляра в потоке**.

~~~java
import java.io.*;

class Session implements Serializable {
    @Serial private static final long serialVersionUID = 67L;
    static String category = "before";
    String name = "node";
    transient String token = "local-session";
}
// После записи собственного Session, смены category на "after" и чтения:
// restored.name == "node"; restored.token == null;
// Session.category == "after" — текущее значение класса.
~~~

Важно не сделать неверный вывод о безопасности: `transient` исключает поле из **стандартного** сохранения, но не запрещает программе явно вывести секрет в лог или записать его в другом hook-методе. Это не механизм аутентификации или полного стирания секрета.

В полном G1 `S04=true` означает восстановленный `transient session == null`; `S05=after` показывает, что static-поле после записи осталось текущим значением класса. Ни одно наблюдение не требует вызова конструктора `Session` при чтении.

## 4. Граф ссылок, циклы, повторная запись и идентичность (S-07–S-10, S-22)

`ObjectOutputStream` отслеживает уже записанные объекты с помощью **handles**. Когда два поля указывают на один и тот же экземпляр, поток может сохранить эту общность вместо создания независимых копий. Это также позволяет корректно восстанавливать **циклы**, например `root.next.next == root`.

~~~java
import java.io.*;
class Node implements Serializable {
    @Serial private static final long serialVersionUID = 67L;
    String name;
    Node next;
    Node(String name) { this.name = name; }
}
Node root = new Node("root"), child = new Node("child");
root.next = child;
child.next = root;
// В полном G1-примере root записывается дважды В ОДИН ObjectOutputStream:
// copy.next.next == copy        → true (цикл);
// first == second              → true (handle одного stream);
// first != root                → true (восстановлен новый объект).
~~~

Это схема графа, а не исполняемый фрагмент чтения: полноценная последовательность записи и чтения приведена в [G1 §6.2](../../docs/plans/CORE-I18N-SERIAL.md). **Не переносить** факт сохранения identity на два разных потока, разные процессы или `writeUnshared`: эти случаи имеют отдельные контракты.

Если достижимое поле содержит несериализуемый объект, запись может бросить `NotSerializableException`. Корневой класс, помеченный `Serializable`, **не делает автоматически Serializable весь достижимый граф**. Отдельный опыт с вложенным не-Serializable полем в G1 **NOT RUN**; G1 показывает Runtime `NotSerializableException` при прямой записи `new Object()`.

## 5. Конструкторы обычного класса и record — принципиально разные пути (S-15–S-17, S-26)

Обычный Serializable-класс не восстанавливается обычным вызовом собственного конструктора, как при `new`. Поля заполняются по правилам сериализации; при этом вызывается подходящий no-arg конструктор ближайшего несериализуемого предка, поэтому условия и доступность такого конструктора важны. Инвариант, проверенный **только в конструкторе самого Serializable-класса**, нельзя считать проверенным после `readObject`. Изолированная трасса конструктора такого предка в G1 **не исполнялась**.

Для `record`, напротив, поля компонентов восстанавливаются с **вызовом канонического конструктора**. Именно в нём нужно закреплять инварианты record.

~~~java
import java.io.Serializable;
record Receipt(String label, int value) implements Serializable {
    Receipt {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label");
        }
    }
}
// На десериализации доверенного Receipt вызывается canonical constructor;
// special readObject/writeObject у record не работают как у ordinary class.
~~~

В полном безопасном G1-примере счётчик конструктора record до записи равен 1, а после чтения восстановленного экземпляра — 2 (`S13=1`, `S14=2`). `S15=invoice:67` подтверждает значения компонентов, `S16=true` — новый экземпляр после чтения. Это отдельное поведение record, а не универсальное правило для всех Java-классов. Вызовы record-specific `readObject` и `writeObject` игнорируются механизмом сериализации компонентов; отдельная проверка hook-ignore **NOT RUN**.

## 6. private readObject и валидация инвариантов (S-14, S-25, S-27)

Для обычного сериализуемого класса допустим **специальный приватный** метод с точной сигнатурой `private void readObject(ObjectInputStream) throws IOException, ClassNotFoundException`. Он **не переопределяет интерфейс Serializable**; это соглашение механизма объектной сериализации. В методе нужно прочитать поля (например, `defaultReadObject()`), затем проверить инварианты.

~~~java
import java.io.*;

class CheckedEntry implements Serializable {
    @Serial private static final long serialVersionUID = 67L;
    String name;
    @Serial
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (name == null || name.isBlank()) {
            throw new InvalidObjectException("invalid name");
        }
    }
}
~~~

Зачем нужна повторная проверка: поток может содержать состояние, которое не прошло обычный конструктор класса. `InvalidObjectException` при некорректном состоянии возникает **во время чтения**, не при `javac`. В G1 `S12=InvalidObjectException:validation` получено на собственных локально записанных доверенных байтах, где тест специально сформировал объект с пустым именем. Это **проверка инварианта**, а не эксперимент с вредоносным payload.

Корректная сигнатура принципиальна. `@Serial` помогает компилятору сообщать о подозрительных декларациях специальных членов, но **не гарантирует**, что каждый неправильный `@Serial` всегда приводит к фатальной ошибке компиляции: см. §10.

## 7. Enum и собственная идентичность при чтении (S-18)

У `enum` сериализация следует специальной модели: константа записывается/восстанавливается по имени; при чтении используется существующая константа enum-класса, поэтому не создаётся ещё один самостоятельный экземпляр той же константы.

~~~java
enum Kind { ACTIVE }
// В G1 собственный Kind.ACTIVE записан в локальный byte[] и прочитан;
// restoredEnum == Kind.ACTIVE    → true (S17=true).
~~~

Это отличается от чтения обычного `Serializable`-объекта, где identity восстановленного объекта отличается от исходного. Serializable-контракт enum нельзя отождествлять с обычными полями класса; он следует особым правилам протокола.

## 8. ObjectInputFilter: REJECTED, ALLOWED, UNDECIDED и ограничения (S-11, S-12, S-20, S-21)

`ObjectInputFilter` позволяет принять решение по сведениям об обрабатываемом объекте/графе и ограничить ресурсы чтения. В частности, решение фильтра может быть `ALLOWED`, `REJECTED` или `UNDECIDED`. **UNDECIDED не означает автоматический запрет**: последующая политика/обработка важна. Фильтр **не является самодостаточной гарантией безопасности** и не заменяет архитектурный запрет на недоверенную нативную десериализацию.

~~~java
import java.io.*;
ObjectInputFilter denyNode = info ->
        info.serialClass() == Node.class
            ? ObjectInputFilter.Status.REJECTED
            : ObjectInputFilter.Status.UNDECIDED;
// Только для ObjectInputStream, читающего собственный локальный byte[]:
// in.setObjectInputFilter(denyNode);
// in.readObject(); // Node отвергнут: InvalidClassException (filter rejection).
~~~

В проверенной программе `S10=InvalidClassException:filter` получено именно из-за **REJECTED на доверенном локально созданном Node**, а не из-за UID mismatch. Другой фильтр в программе возвращал `UNDECIDED` при разрешённых ограничениях; это **не** положительный результат производственного allowlist, а демонстрация, что UNDECIDED нельзя прочитать как deny.

Даже ограничение глубины/количества ссылок/размера потоков не доказало для конкретного приложения отсутствие DoS, исполнение произвольного пользовательского кода в специальных методах или отсутствие gadget chains. **Недоверенный ввод и атаки не воспроизводились по соображениям безопасности.** Для внешних систем предпочтителен строго определённый DTO/wire-контракт с проверками типов, размеров и схемы, а не приём стороннего `ObjectInputStream`.

## 9. Native serialization не равна DTO/JSON и не является интерфейсным контрактом (S-19, S-20, S-21)

У Java-объектного потока свой протокол, связанный с метаданными классов и правилами версионирования. `JSON`, `Avro`, `Protobuf` и другие схемные форматы имеют собственные гарантии и ограничения; **Java Serialization не превращается в них** при переименовании `writeObject`. Выбор wire-протокола должен быть частью архитектуры: явная схема, совместимость, размер, кодировка, разрешённые типы и проверка доверенности данных.

Контрпример: разработчик принимает байты из HTTP-body и просто вызывает `new ObjectInputStream(...)`, считая, что потом проверит тип возвращённого объекта. Вредоносное поведение может начаться **во время** восстановления графа, то есть проверка после `readObject` уже слишком поздняя, чтобы считать сам процесс безопасным. Такой обработчик **не создаём**.

[CORE-IMMUTABLE](immutability.md) объясняет защитные копии и контроль инвариантов; [CORE-I18N](internationalization.md) — перевод Unicode-строк в bytes через Charset. Ни тот, ни другой материал не превращает Java native stream в безопасное внешнее API.

## 10. Ошибки javac, предупреждения @Serial и runtime — три группы (S-13, S-14, S-25, S-27)

| Ситуация | Что на самом деле происходит |
|---|---|
| `class C implements Externalizable { }` без обязательных методов | **Ошибка javac**: класс не реализовал контракт Externalizable |
| `@Override public void writeObject(...)` у Serializable | **Ошибка javac**: метод интерфейса не существует, переопределять нечего |
| `byte[] b = out.writeObject("x")` | **Ошибка javac**: `writeObject` возвращает `void` |
| `String x = in.readObject()` | **Ошибка javac** без явного приведения/проверки `Object` |
| `ObjectInputFilter f = "maxdepth=5"` | **Ошибка javac**: строка не реализует интерфейс фильтра |
| `new ObjectInputStream(new ByteArrayOutputStream())` | **Ошибка javac**: нужен `InputStream`, не `OutputStream` |
| `@Serial private static final String serialVersionUID = "67"` | **Предупреждение** при `javac -Xlint:serial`, exit=0 в G1/03 |
| `@Serial private int readObject(ObjectInputStream)` | **Предупреждение**, не обязательный compiler-negative, exit=0 в G1/03 |
| `writeObject(new Object())` | Компилируется; **NotSerializableException** во время выполнения |
| Проверка инварианта после `defaultReadObject()` | Компилируется; **InvalidObjectException** во время выполнения |
| Фильтр `REJECTED` | Компилируется; **InvalidClassException** при чтении (filter rejection) |
| Обычный UID несовместим с дескриптором класса | **InvalidClassException** при чтении, но two-version test **NOT RUN** |

В [принятом G1 §7](../../docs/plans/CORE-I18N-SERIAL.md) перечислены **шесть независимых S_N01–S_N06 compiler-negative**, по одному самодостаточному Java-файлу. Роль 03 независимо проверила их локально с `javac --release 21 -XDrawDiagnostics`: каждый завершился с exit 1 и соответствующей тематической `compiler.err.*` категорией. Ещё **шесть L_N01–L_N06** относятся к CORE-I18N. Два отдельно скомпилированных ошибочных `@Serial`-случая завершились **exit 0 с предупреждениями** `compiler.warn.long.SVUID` и `compiler.warn.serial.method.unexpected.return.type` (во втором также предупреждение об отсутствии UID). Эти ключи — наблюдение **OpenJDK 21.0.12.1**, не переносимый текст ошибки для всех компиляторов. **12 compiler-negative ≠ 2 warnings ≠ runtime failures**; такие случаи не входят в положительный runner, если не создан отдельный доказанный negative harness.

## 11. P1: следующие механизмы и честные ограничения (S28–S34)

**S28. Externalizable.** В отличие от маркерного `Serializable`, интерфейс `Externalizable` требует `writeExternal` и `readExternal`, а при обычном восстановлении важен публичный конструктор без аргументов. Компиляционный контрпример отсутствующих методов проверен; полноценный runtime round-trip **NOT RUN**.

**S29. Serialization proxy.** `writeReplace`/`readResolve` позволяют проектировать замещающее представление и проверять инварианты на явно определённой форме. Но они не дают автоматической защиты при произвольном недоверенном input; отдельный proxy-example **NOT RUN**.

**S30. ObjectInputValidation / JEP 415.** Post-validation callbacks и factory фильтров позволяют строить более сложную политику. [JEP 415](https://openjdk.org/jeps/415) описывает контекстно-специфические фильтры; готовая production configuration и глобальная политика **NOT RUN**.

**S31. @Serial warnings.** `@Serial` относится к определённым специальным членам. Два неверных случая были реально проверены отдельно; предупреждение при exit 0 **не равняется** compiler-negative. Другие версии javac могут формулировать диагностику иначе.

**S32. serialPersistentFields / миграции.** Явное описание сериализуемых полей и изменение структуры — сложный контракт совместимости, а не одна версия UID. Two-build compatibility matrix **NOT RUN**.

**S33. writeUnshared/readUnshared/reset.** Эти API меняют правила разделяемых handles. Нельзя переносить результаты `S08=true` для двух `writeObject` внутри одного потока на `writeUnshared`. Дополнительные tracing-тесты **NOT RUN**.

**S34. Несериализуемый предок.** Доступный no-arg конструктор ближайшего несериализуемого superclass влияет на восстановление, но отдельного runnable c constructor counter/изменённой иерархией **NOT RUN**.

## 12. Прослеживаемость G1: 27 P0 / 7 P1 / 5 P2

В таблице указан **раздел объяснения и граница доказательства**, не «номер теста». Реальные метки stdout `S01–S20` описаны в принятом [G1 §6.2](../../docs/plans/CORE-I18N-SERIAL.md) и воспроизводятся уже опубликованным [SerializationProbe.java](../../examples/core/i18n-serialization/src/SerializationProbe.java) с [20-строчным эталоном](../../examples/core/i18n-serialization/serial.expected.stdout). Его интегрированный [CI #45184055313](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45184055313) completed/SUCCESS на `95bc9a0b329491a1e8b9670d4636760c88a47d88`.

| ID | Раздел, механизм и/или граница |
|---|---|
| S-01 | §1 marker Serializable; graph default |
| S-02 | §1 trusted local ByteArray streams |
| S-03 | §2 explicit UID 67 |
| S-04 | §2 нормативный UID mismatch; two-build NOT RUN |
| S-05 | §3 transient → default null |
| S-06 | §3 static не в потоке |
| S-07 | §4 достижимый граф и child |
| S-08 | §4 циклы `root.next.next == root` |
| S-09 | §4 общий stream handle при повторной записи |
| S-10 | §4 восстановленная identity != оригинальная |
| S-11 | §8 фильтр и значения UNDECIDED/REJECTED |
| S-12 | §8 InvalidClassException:filter |
| S-13 | §1, §10 NotSerializableException runtime |
| S-14 | §6, §10 private readObject validation |
| S-15 | §5 parent noarg constructor; trace NOT RUN |
| S-16 | §5 record canonical constructor 1→2 |
| S-17 | §5 record components/new identity |
| S-18 | §7 enum identity |
| S-19 | §9 native object stream != DTO/JSON |
| S-20 | §8, §9 запрет недоверенного deserialization |
| S-21 | §8 UNDECIDED не deny и фильтр не доказательство безопасности |
| S-22 | §4 graph nested nonserializable; separate NOT RUN |
| S-23 | §2 record UID waiver; UID mismatch NOT RUN |
| S-24 | §2 UID ordinary/record (67/0) |
| S-25 | §6, §10 @Serial и специальные hooks |
| S-26 | §5 record hook-ignore; separate NOT RUN |
| S-27 | §1, §6, §10 readObject возвращает Object |
| S28 | §11 P1 Externalizable public noarg; runtime NOT RUN |
| S29 | §11 P1 proxy/writeReplace/readResolve; NOT RUN |
| S30 | §11 P1 ObjectInputValidation/JEP415; NOT RUN |
| S31 | §10–§11 P1 @Serial warning-only |
| S32 | §11 P1 serialPersistentFields/version migrations; NOT RUN |
| S33 | §11 P1 writeUnshared/readUnshared/reset; NOT RUN |
| S34 | §5, §11 P1 nonserializable superclass; trace NOT RUN |
| S35 | P2 hostile gadget chains **NOT CONSTRUCTED** |
| S36 | P2 public endpoints/untrusted serialized files **NOT CONSTRUCTED** |
| S37 | P2 cross-version/classloader compatibility matrix **NOT VERIFIED** |
| S38 | P2 JSON/Avro/Protobuf/Jackson integration **NOT VERIFIED** |
| S39 | P2 huge graph DoS/performance/JMH **NOT VERIFIED** |

## 13. Sources A, реальные интервью C, локальный G1 и NOT RUN

**A:** [Serializable API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Serializable.html), [ObjectInputStream API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ObjectInputStream.html), [ObjectInputFilter API21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ObjectInputFilter.html), [Serialization spec: serial-arch](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/serial-arch.html), [input](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/input.html), [versioning](https://docs.oracle.com/en/java/javase/21/docs/specs/serialization/version.html), [security](https://www.oracle.com/java/technologies/javase/seccodeguide.html). Эти документы определяют обязательный Java SE21 контракт; строка конкретной ошибки или специфическая сборка не заменяют их.

**C — реально прочитанные широкие вопросы:** [Q-HRUDAY, Serialization & Deserialization](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md), [Q-TEAMLEAD](https://github.com/teamlead/java-interview-questions/blob/main/README.md) и [Q-CHERNYSHOV](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/eng/README.md) охватывают `Serializable`, UID, `transient`, кастомизацию и общий риск десериализации. **Точные вопросы** о waiver UID для record, `UNDECIDED`, JEP 415 и constructor-counters в проверенных банках **не подтверждены (C GAP)**; эти механизмы разобраны по спецификации, а не выданы за цитаты реальных интервью.

**Граница проверенного G1:** на Debian GNU/Linux 13/OpenJDK 21.0.12.1 роль 01/04 выполнила [полный `SerializationProbe.java` §6.2](../../docs/plans/CORE-I18N-SERIAL.md): **20 stdout-строк S01–S20**, SHA-256 source `294a30fccdb899c1471d8fb38bd7ec74cf3aad32b493927dd47459d234d978a3`, output `fa00d160ce87c1c241f42948140086edd43636e3b30949e27de97affb6535608`. Позже роль 03 **независимо** воспроизвела полные исходники и 3/3 локальных positive runs, 12/12 отдельные compiler-negative и 2/2 предупреждения `@Serial` ([Issue #203, отчёт 03](https://github.com/Melchiorius/backend-interview-labs/issues/203#issuecomment-7101919732)). Отдельные локальные G1/03-прогоны **не являются** GitHub CI G2. Технический [PR #206](https://github.com/Melchiorius/backend-interview-labs/pull/206) уже интегрирован в авторскую ветку; [интегрированный CI #45184055313](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45184055313) completed/SUCCESS на `95bc9a0b329491a1e8b9670d4636760c88a47d88`. Следующие редакционные изменения создают новый HEAD и требуют отдельного CI; подтверждение старого SHA не переносится.

**NOT RUN автором 02:** дополнительный самостоятельный javac для всех встроенных фрагментов; 12 compiler-negative и 2 `@Serial` warning-only в позитивном GitHub CI (проверены отдельно ролью 03); иные JDK/vendors/OS и зависимость десериализации от двух несовместимых сборок; `Externalizable`/proxy/runtime hooks в отдельном расширенном тесте; ресурсоёмкие графы; production-hardening `ObjectInputFilter`; P1/P2 кроме отдельно зафиксированных наблюдений 03. **Намеренно НЕ ВЫПОЛНЯЛИСЬ** десериализация внешних/недоверенных файлов или HTTP/сетевых байтов, gadget payloads и обходы фильтров. Визуальные Obsidian/IDE и внешние HTTP/anchors также не проверялись. Изменять [CURRICULUM](../../docs/CURRICULUM.md) и [COVERAGE](../../docs/COVERAGE.md) до G5/G6 нельзя.
