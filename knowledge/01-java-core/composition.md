# CORE-COMPOSITION — композиция, делегирование и выбор между контрактами в Java 21

**Java SE 21 · G2 / Issue [#70](https://github.com/Melchiorius/backend-interview-labs/issues/70).** Основа: [G1-план CORE-INTERFACES-COMPOSITION](../../docs/plans/CORE-INTERFACES-COMPOSITION.md), принятый 00 в [Issue #64](https://github.com/Melchiorius/backend-interview-labs/issues/64#issuecomment-7049387322). Эта статья — **архитектурные последствия и работающий код**, а не перечень нормативных правил разрешения default: для этих правил есть [CORE-INTERFACES](interfaces.md).

## Проблема, которую не решает одно наследование

Предположим, сервис получает число 67 и должен обработать его по выбранной политике. Сегодня политика прибавляет единицу, завтра умножает на два, для отдельного клиента требуется журналирование или дополнительное ограничение. Прямое наследование от конкретной реализации вынудило бы сервис унаследовать больше, чем ему нужно: чужое поведение, иногда состояние и зависимости, а также обязательства поддерживать семантику суперкласса. При добавлении нескольких независимых вариантов дерево наследования может перестать выражать реальные сочетания.

**Композиция** означает, что один объект **хранит ссылки на другие объекты** и использует их для выполнения своей работы. **Делегирование** — более конкретная операция: владелец передаёт часть выполнения другому объекту, который выступает delegate. Это не отдельная конструкция синтаксиса Java; компилятор видит поля, конструкторы, вызовы методов и интерфейсные типы.

Связанный вопрос не «композиция или наследование всегда лучше», а **какой публичный контракт должен зависеть от какого**, кто владеет объектами, кто отвечает за результат и какие изменения ожидаются. Архитектурные критерии вроде IS-A/HAS-A и принципа подстановки Лисков (LSP) — рекомендации, **не** проверки, выполняемые `javac`.

## Центральный пример: Strategy + constructor injection + Decorator

Ниже один **самодостаточный диагностический пример** в файле `CompositionWalkthrough.java`. Он нужен для понимания архитектуры без отдельного Gradle-модуля. Роль 03 готовит единый интеграционный multi-file пример для двух статей в [Issue #71](https://github.com/Melchiorius/backend-interview-labs/issues/71); не путайте один inline-файл с её проектом.

```java
import java.util.Objects;

public final class CompositionWalkthrough {
    private CompositionWalkthrough() {}

    interface Policy {
        int apply(int input);
    }

    static final class AddOne implements Policy {
        @Override public int apply(int input) {
            return input + 1;
        }
    }

    static final class DoubleIt implements Policy {
        @Override public int apply(int input) {
            return input * 2;
        }
    }

    static final class AddTenDecorator implements Policy {
        private final Policy delegate;

        AddTenDecorator(Policy delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override public int apply(int input) {
            return delegate.apply(input) + 10;
        }
    }

    static final class Service {
        private final Policy policy;

        Service(Policy policy) {
            this.policy = Objects.requireNonNull(policy, "policy");
        }

        int execute(int input) {
            return policy.apply(input);
        }
    }

    static final class RecordingPolicyFake implements Policy {
        int lastInput = -1;
        int calls;

        @Override public int apply(int input) {
            calls++;
            lastInput = input;
            return 900;
        }
    }

    public static void main(String[] args) {
        System.out.println("add=" + new Service(new AddOne()).execute(67));
        System.out.println("double=" + new Service(new DoubleIt()).execute(67));
        System.out.println("decorated=" +
                new Service(new AddTenDecorator(new AddOne())).execute(67));

        RecordingPolicyFake fake = new RecordingPolicyFake();
        System.out.println("fake=" + new Service(fake).execute(67));
        System.out.println("observed=" + fake.lastInput + ",calls=" + fake.calls);
        if (fake.lastInput != 67 || fake.calls != 1) {
            throw new AssertionError("forwarding failed");
        }

        try {
            new Service(null);
        } catch (NullPointerException ex) {
            System.out.println("guard=" + ex.getMessage());
        }

        try {
            new Service(new AddTenDecorator(n -> {
                throw new IllegalStateException("rejected:" + n);
            })).execute(67);
        } catch (IllegalStateException ex) {
            System.out.println("failure=" + ex.getMessage());
        }

        System.out.println("CHECK PASSED");
    }
}
```

Команды с JDK 21 из каталога файла:

```sh
javac --release 21 CompositionWalkthrough.java
java CompositionWalkthrough
```

Полный ожидаемый stdout:

```text
add=68
double=134
decorated=78
fake=900
observed=67,calls=1
guard=policy
failure=rejected:67
CHECK PASSED
```

### Пошаговое объяснение

**1. Контракт задаётся как `Policy.apply(int)`.** Это публичная операция, а не готовая реализация расчёта. Два класса `AddOne` и `DoubleIt` реализуют одну сигнатуру, хотя делают разные вещи. Java-проверка интерфейсного контракта описана в [CORE-INTERFACES](interfaces.md); сама по себе эта проверка ещё не доказывает поведенческую взаимозаменяемость.

**2. `Service` содержит `Policy`, но не наследуется от `AddOne`.** Поле `private final Policy policy` создаёт явную связь *HAS-A*: сервис **имеет** объект политики. Тело `execute(int)` только передаёт аргумент в `policy.apply(input)`. Это и есть delegating call. По статическому типу `Policy` компилятор проверяет допустимость вызова, а во время выполнения виртуальный вызов выбирает реализацию фактического объекта.

**3. Две строки `add=68` и `double=134`.** Клиент создаёт два объекта `Service`, передав каждому свою реализацию. Исходник класса `Service` для этого не изменяется. На входе 67 одна политика прибавляет 1, другая умножает на 2. Важно: зависимость выбрана **при построении конкретного объекта**, а не переключается магически во всех существующих сервисах.

**4. Строка `decorated=78`.** `AddTenDecorator` сам реализует `Policy`, но хранит другую `Policy`. Он сначала вызывает `delegate.apply(67)` — результат `68` от `AddOne` — и затем прибавляет 10. Декоратор заменим **по типу** на любую политику. Если при этом договор требует, например, «проверка не должна увеличивать значение», такой декоратор уже может нарушить **поведенческий** контракт, несмотря на успешную компиляцию.

**5. Строки `fake=900` и `observed=67,calls=1`.** Вместо настоящей политики передан ручной **тестовый двойник** `RecordingPolicyFake`. Он возвращает заранее заданное число и записывает вход/количество вызовов. Проверяется, что `Service` действительно делегировал аргумент 67 ровно один раз. Двойник одновременно выполняет роль stub (заданный ответ) и записывает обращения; название fake здесь учебное и не обязано совпадать со строгой классификацией test doubles.

**6. Строка `guard=policy`.** В конструкторе используется `Objects.requireNonNull(policy, "policy")`. Поэтому пустая зависимость отвергается сразу, а не приводит позже к неясной ошибке в `execute()`. Это контракт стандартного [Java SE 21 Objects.requireNonNull](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html#requireNonNull(java.lang.Object,java.lang.String)), но не часть языкового синтаксиса constructor injection.

**7. Строка `failure=rejected:67`.** Политика-лямбда выбрасывает `IllegalStateException`. `AddTenDecorator` и `Service` не перехватывают исключение, поэтому оно распространяется до внешнего `catch`. Это демонстрирует конкретную политику **прозрачной передачи исключений**, но не общую гарантию любого decorator: wrapper может подавлять, оборачивать, повторять или преобразовывать ошибки — и должен явно отвечать за смысл этого изменения.

**8. `CHECK PASSED`.** Проверка означает только отсутствие срабатывания assertion для одного сценария на одном запуске. Она не доказывает корректность всех политик, отсутствие data races, полную LSP-подстановку или производительность.

## Почему это Strategy, а не только вызов поля

Условный алгоритм «сделать расчёт» меняется за счёт объекта, реализующего один контракт. Это **Strategy**: точка изменения поведения вынесена в отдельные взаимозаменяемые по типу алгоритмы. `Service` зависит от интерфейса, а клиент принимает решение, какой объект передать в конструктор.

Вместо:

```java
class Service {
    int execute(int input, boolean doubleIt) {
        return doubleIt ? input * 2 : input + 1;
    }
}
```

мы передаём поведение через `Policy`. В первом варианте при добавлении каждого нового вида политики растёт ветвление внутри сервиса. Во втором меняются реализации, но добавляется ответственность за организацию зависимостей. Для **двух навсегда фиксированных действий** простой условный оператор иногда понятнее. Strategy оправдана не названием паттерна, а реально независимой эволюцией политик и требованиями проверяемости.

## Constructor injection: что гарантируется, а что нет

В классе `Service` обязательная зависимость передана через конструктор. Это делает состояние «без Policy» явным недопустимым случаем и позволяет создать сервис с выбранной реализацией до первого запроса.

Однако из того, что поле `final`, **не следует**, что:

- реализация `Policy` неизменяема или потокобезопасна;
- объект `Policy` больше никем не используется или не модифицируется;
- разные реализации соблюдают одинаковое смысловое поведение;
- весь `Service` безопасно опубликован в других потоках;
- внедряемый объект будет автоматически закрыт при завершении использования.

`final` не позволяет присвоить другую ссылку в поле после конструирования, но не замораживает состояние объекта, на который она указывает. Тонкие гарантии final-field semantics и безопасной публикации относятся к JMM ([JLS 21 §17.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.5)), а не к самому слову «инъекция». В реальных системах нужно явно определить ownership, lifetime и synchronization.

**Constructor injection** — лишь одна техника; зависимости также могут создаваться фабрикой, передаваться аргументом метода или назначаться иным способом. Java не требует использовать Spring или контейнер. И напротив, наличие Spring не заменяет проектного решения о том, кто отвечает за жизненный цикл ресурса.
