# CORE-INIT — исследовательский план до написания статьи

**Статус:** G1, исследовательский план роли 01; 00 запросил исправления, 04 оставил обязательные замечания по прежнему HEAD. Повторное независимое review исправленного HEAD ожидается. **Дата:** 20.11.2026. **Issue:** [#49](https://github.com/Melchiorius/backend-interview-labs/issues/49), родитель [#33](https://github.com/Melchiorius/backend-interview-labs/issues/33). **Нормативная версия:** Java SE 21. **BASE main:** `0615515f27e6f44ccbdf27b039dbe39d0434709e`. Этот файл — исследовательский план, не статья и не доказательство прохождения G1/G2/G4.

## 1. Вопрос читателя и обещание будущей статьи

**Главный вопрос:** что именно происходит между `new Subclass(...)` и получением готового объекта? В каком порядке и при каких условиях Java инициализирует классы, поля и блоки, как связаны конструкторы `this(...)`/`super(...)`, почему виртуальный вызов из конструктора суперкласса может увидеть ещё неинициализированное состояние наследника и когда выполняется статическая инициализация класса?

**Обещание читателю:** после статьи читатель должен суметь предсказать порядок сообщений/значений для цепочки из нескольких классов; отличить загрузку, linking, class initialization, instance initialization и constructor body; объяснить активные триггеры и исключения для compile-time constants; определить, какие поля видит конструктор предка; отличить class-initialization failure от ошибки создания одного экземпляра; не смешивать рекурсивную инициализацию классов с `ClassCircularityError`; объяснить, какие гарантии даёт class initialization для статического состояния и где начинается отдельная тема CON-JMM.

**P0 — обязательное ядро:** активное использование и compile-time constants; superclass-first class initialization и порядок static fields/blocks; default values всех instance fields до инициализаторов; superclass-first instance initialization; constructor chaining и implicit `super()`; dynamic dispatch в ходе construction; ошибка class initialization.

**Терминология:** *loading* — получение/создание runtime-представления класса; *linking* — verification, preparation, resolution; *class initialization* — исполнение class/interface initialization method (`<clinit>` в терминах JVM); *instance initialization* — инициализация полей и instance initializer blocks конкретного объекта; *constructor body* — исполнение тела конструктора после обязательной цепочки вызовов и инициализаторов. Не называть class initialization «загрузкой класса».

## 2. Существующее покрытие — что повторять нельзя

### CORE-CLASSES

Опубликованная статья `knowledge/01-java-core/classes.md` (на BASE `0615515f27e6f44ccbdf27b039dbe39d0434709e`) уже объясняет:

- назначение конструктора, перегрузку конструкторов и различие конструктора и метода;
- default constructor: он существует, если класс не объявил ни одного конструктора; не путать его с написанным вручную no-args constructor;
- явные `this(...)`/`super(...)` как первый оператор конструктора **в Java 21**, делегирование через `this(...)`, запрет циклической цепочки;
- значения полей по умолчанию, простую последовательность создания одного класса, текстовый порядок instance field initializers/instance initializer blocks;
- различие static и instance членов, базовую идею static initialization, compile-time constants и отсутствие обязательной инициализации класса при чтении такой константы.

**Что CORE-INIT должен добавить, а не переписать:** точный нормативный порядок при наследовании из нескольких классов; факт, что все поля объекта, в том числе поля самого нижнего подкласса, получают default values до исполнения constructor/initializer code; static initialization по иерархии и интерфейсам с default methods; что `this(...)` приводит к однократному выполнению instance initializers; отсутствие доступного no-args конструктора суперкласса как compile-time blocker для implicit `super()`; значения полей подкласса при virtual call из конструктора предка; триггеры инициализации класса, recursive initialization и failure.

### CORE-INHERITANCE

Опубликованная статья `knowledge/01-java-core/inheritance.md` на том же BASE уже раскрывает `extends`, overriding, dynamic dispatch, `super`, `final` и различие compile-time/runtime. CORE-INIT не пересказывает общий полиморфизм: объясняет **только временную точку**, в которой override может быть вызван до выполнения инициализаторов подкласса, и последствия для состояния объекта.

### Другие зависимости и статусы

- `docs/CURRICULUM.md` и `docs/JAVA_CORE_PLAN.md` предусматривают самостоятельную тему CORE-INIT «Порядок инициализации»; это не означает, что статья уже существует.
- В `docs/COVERAGE.md` на BASE нет опубликованной/проверенной статьи CORE-INIT. План не повышает coverage.
- Реальные локальные ссылки: `classes.md`, `inheritance.md`. Для `CORE-OBJECT`, `CORE-INTERFACES`, `JVM-LOADING`, `CON-JMM` пока использовать обычные текстовые ID, если соответствующий материал отсутствует; не создавать фиктивные Obsidian-ссылки.

## 3. Нормативные источники — Java SE 21

### JLS 21 — язык и порядок исполнения

- **[§4.12.4 — final variables и constant variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.4):** constant variable — `final` переменная примитивного типа или `String`, инициализированная constant expression. `static final Integer` и `static final String = new String(...)` не являются constant variables.
- **[§4.12.5 — initial values of variables](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.12.5):** значения по умолчанию для полей и array components; не смешивать с definite assignment локальных переменных.
- **[§8.3.2 — field initialization](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.2) и [§8.3.3 — restrictions on field references in initializers](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.3.3):** порядок field initializers и ограничения forward references.
- **[§8.6 — instance initializers](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.6), [§8.7 — static initializers](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.7):** область и ограничения initializer blocks; последовательность деклараций в теле класса.
- **[§8.8.7 — constructor body](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7), [§8.8.7.1 — explicit constructor invocations](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.7.1), [§8.8.9 — default constructor](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.8.9):** правила `this(...)`/`super(...)`, implicit superclass constructor invocation и default constructor. Применять именно правила Java 21: explicit constructor invocation — первый оператор; не переносить более поздние preview/flexible constructor bodies.
- **[§9.3–9.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.3):** interface fields и default methods; для class initialization важно, какие superinterfaces объявляют default methods.
- **[§12.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.1.3):** запуск программы и инициализация initial class.
- **[§12.2 и §12.2.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.2):** loading и ClassCircularityError; не путать с циклом static initializers.
- **[§12.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.3):** linking: verification, preparation, resolution; время resolution может быть eager/lazy в разрешённых спецификацией пределах.
- **[§12.4.1 — when initialization occurs](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.1) и [§12.4.2 — detailed initialization procedure](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4.2):** активные триггеры, constant variables, superclass/superinterface ordering, initialization lock, recursion, erroneous state, `ExceptionInInitializerError` и последующие `NoClassDefFoundError`.
- **[§12.5 — creation of new class instances](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.5):** default initialization, superclass constructor chain, instance initializers, constructor body и динамический выбор переопределяемого метода во время конструирования.
- **[§16.8–16.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html#jls-16.9):** definite assignment blank `final` static/instance fields в initializers и constructors; использовать как границу compile-time контрпримеров.
- **[§17.4.5 — happens-before](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.4.5), [§17.5.1 — final field semantics](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.5.1):** минимальная опора для class-initialization synchronization и final-field guarantees. Полную модель JMM в CORE-INIT не переносить.

### JVMS 21 — только граница JVM-LOADING

- **[JVMS §5.3 — creation and loading](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.3)**; **[§5.4 — linking](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.4)**; **[§5.5 — initialization](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html#jvms-5.5)**.
- JVMS §5.5 описывает initialization через JVM-инструкции/класс-файлы и initialization lock. Source-level JLS правила остаются главным доказательством для учебной статьи; не выводить из конкретного bytecode универсальный алгоритм компилятора.

### Java SE 21 API

- [Class.forName(String)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#forName(java.lang.String)) — вызывает инициализацию класса.
- [Class.forName(String, boolean, ClassLoader)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html#forName(java.lang.String,boolean,java.lang.ClassLoader)) — `initialize=false` позволяет загрузить класс без запуска class initializer.
- [ClassLoader.loadClass(String)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html#loadClass(java.lang.String)) — эквивалентен `loadClass(name, false)`, не запускает class initialization.

**Нормативное предостережение:** JLS/JVMS не требуют от реализации использовать `Class` monitor в качестве initialization lock; mapping class/interface → lock выбирает JVM. Нельзя писать «JVM всегда делает `synchronized(Class.class)`». Аналогично не объявлять class init lock пользовательским monitor.

## 4. Интервью-источники C: проверка пробелов, не доказательства

Все источники ниже уже внесены в `docs/SOURCE_REGISTRY.md`; новых записей реестра не требуется.

- **`Q-BAELDUNG-CLASS` — [Java Class Structure and Initialization Interview Questions](https://www.baeldung.com/java-classes-initialization-questions), last updated 2024-01-08.** Проверены Q5 (constructor chaining), Q10 (instance/static initializer blocks), Q12 (static-field Singleton и thread safety). В Q10 сказано, что static initializer выполняется «during class loading» — это неточная терминология: нормативно речь о class initialization, которое отделено от loading/linking (JLS §§12.2–12.4). Также фразу «compiler copies initializer block into every constructor» не представлять как требуемую спецификацией реализацию.
- **`Q-BAELDUNG-CORE` — [Java Interview Questions](https://www.baeldung.com/java-interview-questions), last updated 2024-05-11.** Advanced Q5 (ClassLoader) и Q6 (static vs dynamic class loading) — полезная смежная граница JVM-LOADING; не заменяют JLS 12.2–12.4 и не являются полным вопросником по initialization.
- **`Q-TEAMLEAD` — [Java Interview Questions](https://github.com/teamlead/java-interview-questions).** В списке найдены прямые prompts: ошибка в class initialization block; исключение в constructor; зачем static initialization blocks; порядок constructors и initializer blocks с учётом иерархии; initialization block; default variable values (соответствующие записи списка, строки репозитория около 803, 881–885, 923, 928). Английские формулировки местами машинно искажены. Заявление репозитория об анализе 600 публичных интервью — авторская методика, не независимая статистическая гарантия.
- **`Q-CHERNYSHOV` — [Java Interview Questions and Answers, RU](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/ru/README.md).** Найдены adjacent prompts про Singleton, thread-safe Singleton и double-checked locking (раздел Design Patterns). В доступном списке не обнаружен отдельный вопрос про class-initialization order/failure; использовать этот источник только для проверки границы thread safety/publication, не засчитывать как прямое подтверждение полноты CORE-INIT.

**B-источник:** `B-DEVJAVA-CLASSES` из реестра — [dev.java Classes and Objects](https://dev.java/learn/language/oop/classes-objects/). Прямая загрузка страницы в текущем web-сеансе завершилась cache miss; содержание не использовалось для утверждений. Для норм Java 21 достаточно JLS/JVMS/API. Baeldung и банки вопросов — только prompts и объяснительный фон.

## 5. Точная модель class initialization

### 5.1. Триггеры и то, что НЕ является триггером

Для обычного класса `C` JLS §12.4.1 требует инициализации перед первым активным использованием, в частности: созданием экземпляра `new C`; вызовом `static`-метода, объявленного в `C`; присваиванием `static`-поля, объявленного в `C`; чтением `static`-поля, объявленного в `C`, если оно не является constant variable; а также при определённых reflective actions. Класс также инициализируется при инициализации его подкласса.

Не следует считать триггером: получение `C.class`; создание массива `new C[1]`; загрузку через `ClassLoader.loadClass`; `Class.forName(name, false, loader)`; чтение constant variable. `Class.forName(name)` — контраст, который инициирует инициализацию.

Ссылка `Sub.inheritedStatic` не означает, что инициализируется `Sub`: инициализируется **тип, который фактически объявляет resolved field**. `static final int K = 67` и `static final String S = "Java"` могут быть constant variables; `static final Integer K = 67` — нет.

### 5.2. Порядок class initialization

До инициализации класс загружен и связан (verification/preparation, resolution может быть отложен). Подготовка даёт static fields значения по умолчанию. В подробной процедуре JLS §12.4.2 constant variables получают constant values до инициализации суперкласса. Далее сначала инициализируется superclass. Затем для каждой непосредственно реализованной interface в порядке `implements` рекурсивно обходятся её superinterfaces слева направо; предки рассматриваются **до** возврата к самой interface (postorder). Инициализируются только те интерфейсы этого обхода, которые сами объявляют хотя бы один default method. После этого исполняются class variable initializers и static initializer blocks **в текстовом порядке объявления** как единая последовательность действий.

Не говорить «сначала все static-поля, потом все static-блоки»: порядок общий текстовый. Не говорить «при инициализации класса автоматически инициализируются все интерфейсы»: JLS выделяет superinterfaces с default methods; инициализация самого интерфейса не запускает автоматически инициализацию его superinterfaces. **Оговорка JLS §12.4.1:** synthetic default methods, сгенерированные компилятором, тоже могут повлиять на то, будет ли интерфейс считаться объявляющим default method; это не всегда видно из исходного Java-кода.

### 5.3. Recursion, synchronization и failure

- У каждого class/interface есть свой initialization lock; другие потоки ждут завершения уже выполняемой инициализации. Рекурсивный запрос **того же класса из того же потока** завершается нормально без повторного запуска initializer. Это допускает чтение ещё не присвоенного поля со значением по умолчанию.
- Если initializer завершается исключением `E`, которое не является `Error`, процедура заменяет его на `ExceptionInInitializerError(E)`. Если это `Error`, он передаётся без обязательной упаковки. Тип помечается erroneous; последующее активное использование приводит к `NoClassDefFoundError`. Сообщение/текст stack trace не является контрактом.
- Если инициализация superclass/superinterface завершается abrupt completion, инициализируемый тип тоже становится erroneous. Это не то же самое, что исключение в обычном static method, вызванном уже после успешной инициализации.
- Рекурсивная инициализация A↔B в одном потоке не тождественна циклической иерархии наследования. `ClassCircularityError` возникает при **загрузке/derivation класса**, если загружаемый класс или интерфейс оказывается собственным (прямым или косвенным) superclass/superinterface (JLS §12.2.1; JVMS §5.3.5). Это `LinkageError` по иерархии типов, но не ошибка фазы linking (§5.4). Структурно некорректный class file обычно ведёт к `ClassFormatError`, а не к `ClassCircularityError`. Цикл в Java source обычно отклоняет `javac` ещё при компиляции.

## 6. Точная модель instance initialization и constructor chaining

Для `new Sub(...)` class initialization необходимых типов предшествует созданию экземпляра. При создании объекта **все instance fields всего объекта, включая поля Sub, получают default values до выполнения initializer/constructor code**. Затем constructor invocation проходит вверх по цепочке `super(...)` до базового класса. Для каждого класса после возврата из constructor его superclass выполняются его instance field initializers и instance initializer blocks в текстовом порядке, а затем остаток тела его конструктора. Возврат по цепочке приводит к инициализации подкласса и затем к его телу конструктора.

`this(...)` делегирует другому конструктору того же класса. Путь делегирования должен дойти до конструктора, который вызывает `super(...)`/implicit `super()`; instance initializers класса выполняются **один раз**, не заново для каждого конструктора в цепочке. После возврата делегированного конструктора выполняется оставшаяся часть вызывающего constructor body.

В Java 21 явный `this(...)`/`super(...)` — первый оператор constructor body. Если конструктор не содержит explicit constructor invocation и не является constructor `Object`, применяется implicit `super()`. Если у прямого superclass нет доступного no-args constructor, такой constructor подкласса не компилируется. Не путать default constructor (неявно объявляется, только если конструкторов нет) с ручным no-args constructor; само определение default constructor уже дано в CORE-CLASSES.

**Dynamic dispatch during construction:** virtual call из constructor суперкласса может выбрать override подкласса. Однако initializer subclass ещё не выполнялся; его instance fields имеют default values. Это не специальное «отключение полиморфизма» при конструировании. Практическая рекомендация — не вызывать overridable методы из конструкторов, если контракт не рассчитан на частично сконструированный объект.

## 7. Матрица вопросов P0/P1/P2

Приоритеты отражают обязательность объяснения в будущей статье, а не статистическую частоту на интервью. В колонке «что уже есть» указано, что можно кратко связать ссылкой, а не копировать.

| ID / приоритет | Вопрос и ожидаемый ответ | Нормативная опора | Контрпример / место в будущей статье / связь |
|---|---|---|---|
| CI-01 **P0** | Чем loading, linking, class initialization, instance initialization и constructor body отличаются? Loading/linking не запускают автоматически все static initializers; class initialization исполняет class initializer; constructor строит конкретный объект. | JLS §§12.2–12.5; JVMS §§5.3–5.5 | Positive L1; «Фазы жизненного цикла»; граница JVM-LOADING |
| CI-02 **P0** | Что запускает class initialization и какие действия не запускают? `new`, вызов/запись/чтение соответствующего static member (с оговоркой constant variable), reflection; class literal/array creation/loadClass не равны active use. | JLS §12.4.1 | Positive L1/L2; `Class.forName` API; CORE-CLASSES |
| CI-03 **P0** | Почему чтение `static final int K=67` не инициализирует класс, а `static final Integer K=67` инициализирует? Constant variable ограничена final primitive/String + constant expression; `Integer` не подходит. | JLS §§4.12.4, 12.4.1 | Positive L2; negative/contrast L3; CORE-CLASSES уже упоминает константу — не переписывать определение |
| CI-04 **P0** | Какой класс инициализируется при обращении `Sub.inheritedStatic`? Тип, который фактически объявляет resolved field/method, а не обязательно тип из квалификатора. | JLS §12.4.1 | Positive L1; «Наследуемое static-поле»; CORE-INHERITANCE |
| CI-05 **P0** | В каком порядке выполняются superclass static initialization и static fields/blocks класса? Суперкласс раньше подкласса; поля и static blocks внутри одного класса — общий текстовый порядок; constant variables имеют отдельный ранний шаг процедуры. | JLS §§8.3.2, 8.7, 12.4.2 | Positive L1; «Порядок class initialization»; не дублировать одно-классовый порядок CORE-CLASSES |
| CI-06 **P0** | Инициализируются ли все интерфейсы при инициализации класса? Нет: superclass инициализируется раньше; затем учитывается рекурсивный postorder-обход superinterfaces в порядке объявления, и инициализируются интерфейсы, объявляющие default methods. Прямая инициализация интерфейса не запускает его предков. Учитывать оговорку JLS о compiler-generated synthetic default methods. | JLS §§9.3–9.4, 12.4.1–2 | Positive L1; «Интерфейсная граница»; CORE-INTERFACES |
| CI-07 **P0** | Когда получают default values поля объекта, включая поля подкласса? Все instance variables объекта — до instance initializers/constructors; `int=0`, reference=`null` и т. д. | JLS §§4.12.5, 12.5 | Positive I1/I2; «От выделения до конструктора»; CORE-CLASSES уже объясняет базовые значения |
| CI-08 **P0** | Как выполняются superclass и subclass instance initializers, `this(...)`, `super(...)` и constructor bodies? Superclass chain первой; затем instance field initializers/blocks конкретного класса в текстовом порядке; `this(...)` делегирует и не повторяет initializers. | JLS §§8.6, 8.8.7–8.8.9, 12.5 | Positive I1; N1–N3; «Цепочка конструкторов»; синтаксис уже есть в CORE-CLASSES |
| CI-09 **P0** | Почему override подкласса может вывести `0`, когда вызывается из constructor суперкласса? Dynamic dispatch действует и во время construction, но поле подкласса пока только default-initialized. | JLS §12.5; CORE-INHERITANCE | Positive I2; «Опасность virtual call в конструкторе»; не пересказывать общий override |
| CI-10 **P0** | Что происходит, если static initializer / static field initializer бросает исключение? Non-Error становится `ExceptionInInitializerError`; Error передаётся как Error; класс erroneous, последующее активное использование даёт `NoClassDefFoundError`. | JLS §12.4.2; API Class | Positive F1/F2; «Неуспешная class initialization» |
| CI-11 **P1** | Что происходит при рекурсивной инициализации A→B→A в том же потоке? Повторный запрос A завершается нормально без повторного выполнения; B может прочитать ещё default value A. Это не `ClassCircularityError`. | JLS §12.4.2 | Positive C1 (`2/1`); «Рекурсия initializer» |
| CI-12 **P1** | Почему class initialization thread-safe и что она гарантирует? Один поток выполняет инициализацию, другие ждут; lock уникален для class/interface, mapping реализации не фиксирован как `Class` monitor; после успешной init статическое состояние видно при последующем использовании по правилам синхронизации. | JLS §12.4.2; §17.4.5; JVMS §5.5 | Нормативный разбор; concurrency test `not run`; граница CON-JMM |
| CI-13 **P1** | Чем failure при `new Instance()` отличается от class-initialization failure? Исключение конструктора/instance initializer прерывает конкретное создание, но не переводит класс в erroneous state; следующий `new` может выполниться. | JLS §12.5; §12.4.2 | Positive F3; «Два вида неудачи» |
| CI-14 **P1** | Как проверить load-without-init и явную инициализацию через API? `ClassLoader.loadClass(name)` эквивалентен `loadClass(name,false)`; `Class.forName(name,false,loader)` не запускает init; `Class.forName(name)` и overload с `true` — запускают. | Java SE 21 API Class/ClassLoader; JLS §12.4.1 | Positive L3; «Проверяем через API»; не уходить в reflection framework |
| CI-15 **P1** | Может ли override подкласса, вызванный из constructor суперкласса, сам опубликовать `this` до выполнения subclass initializers? Да: например, override записывает `this` в `static volatile` поле или передаёт ссылку другому потоку; в этот момент поля подкласса ещё имеют default values. Это отдельный факт от same-thread dynamic dispatch: сам вызов override не является data race; межпоточная проблема появляется только при фактическом escape и конкурентном чтении. | JLS §12.5; §§17.4.5, 17.5.1 | Positive I2 — same-thread dispatch; дополнительный сценарий escape из override; граница CON-JMM |
| CI-16 **P1** | Почему forward reference поля иногда compile error, хотя поле получает default value? Ограничения §8.3.3 зависят от вида/квалификации ссылки; отдельный qualified access может увидеть default value; blank final дополнительно контролируется definite assignment. | JLS §§8.3.3, 16.8–16.9 | Negative N4; optional P2 контраст `C.field`; не повторять CORE-CLASSES целиком |
| CI-17 **P2** | Что именно означает `ClassCircularityError`? При загрузке/derivation класса обнаруживается, что загружаемый тип является собственным прямым или косвенным superclass/superinterface. Это не ошибка linking-фазы и не рекурсивное чтение static fields в initializer; Java source cycle обычно отклоняет `javac`. | JLS §12.2.1; JVMS §5.3.5 | Negative N5 — только compile-time cycle; runtime class-file cycle `not run` |
| CI-18 **P2** | Могут ли разные потоки получить deadlock из-за циклических class-initializer dependencies? Потенциально да, если потоки держат разные initialization locks и каждый ждёт другой класс; это не тождественно same-thread recursive case. | JLS §12.4.2 initialization locks; JVMS §5.5 | Теоретический two-thread контрпример; не обещать детерминированный запуск |
| CI-19 **P2** | Что меняется при нескольких class loaders? Runtime class identity включает defining loader; два определения одного binary name могут быть разными `Class` и иметь независимую инициализацию/static state. | JLS §12.2; JVMS §5.3; API ClassLoader | Boundary JVM-LOADING; не строить полноценную статью про delegation |
| CI-20 **P2** | Какие тонкости compile-time constants и бинарной совместимости остаются? Значение constant variable может быть встроено клиентским компилятором; изменение значения без перекомпиляции клиента может оставить старое значение в старом binary. | JLS §§4.12.4, 13.1, 13.4.9 | Optional P2; не смешивать с class initialization triggers |

## 8. Протокол положительных и отрицательных сценариев

**Правило для 02/03:** каждый ожидаемый результат — отдельный пример/исходный набор. Не помещать намеренно некомпилируемые строки в runnable source set. Для отрицательного случая фиксировать причину отказа компилятора, а не зависимый от версии текст диагностики.

### Значимые stdout-наблюдения, entry points и команды

Следующая таблица фиксирует смысловые stdout-assertions из авторского протокола, а не полный дословный transcript: временные исходники и сохранённые stdout-файлы в репозиторий не попали. Поэтому утверждение о прежнем exact comparison (7/7) остаётся записью автора; независимый читатель не может повторить его побайтно без восстановления исходников. Это **не** репозиторный test harness.

| Исходник / entry point | Значимые ожидаемые stdout-наблюдения |
|---|---|
| `ClassInitDemo` | Маркеры class initialization показывают superclass до subclass; интерфейс с default method и его соответствующий предок инициализируются по postorder-обходу; интерфейс без собственного default method не получает маркер только из-за `implements`; inherited static member приводит к инициализации declaring type. |
| `ConstantVsBoxedDemo` | Значение constant variable (`67`) читается без маркера инициализации; чтение boxed `Integer` сопровождается маркером class initialization. |
| `InstanceOrderDemo` | Override, вызванный из superclass constructor, наблюдает default value `0`; после subclass initializer поле имеет `68`; цепочка `this(...)` не повторяет instance initializers; в сценарии с двумя полями итоговое состояние — `67/68`. |
| `CircularInitDemo` | Для same-thread A↔B recursion зафиксировано `2/1`; повторный запрос инициализируемого A не запускает его initializer повторно. |
| `InitFailureDemo` | Для non-`Error`: `ExceptionInInitializerError`, затем при следующем active use `NoClassDefFoundError`; для `Error`: исходный `AssertionError`, затем `NoClassDefFoundError`. |
| `InstanceFailureDemo` | Первая попытка создания завершается `IllegalStateException`; следующая попытка создания экземпляра того же класса завершается успешно. |
| `ClassLoadingDemo` | После `ClassLoader.loadClass` и `Class.forName(name, false, loader)` маркер class initialization отсутствует; после `Class.forName(name, true, loader)` он появляется. |

**Команды повторения при наличии исходников.** В исходной временной сессии использовались имена файлов/entry points из таблицы; точные временные каталоги не сохранены. Ниже — однозначная форма команд для восстановления исходников в `src/` и `negative/` (каждый positive запускается отдельно в собственном output-каталоге):

```sh
mkdir -p out
javac --release 21 -d out/ClassInitDemo src/ClassInitDemo.java
java -cp out/ClassInitDemo ClassInitDemo
javac --release 21 -d out/ConstantVsBoxedDemo src/ConstantVsBoxedDemo.java
java -cp out/ConstantVsBoxedDemo ConstantVsBoxedDemo
javac --release 21 -d out/InstanceOrderDemo src/InstanceOrderDemo.java
java -cp out/InstanceOrderDemo InstanceOrderDemo
javac --release 21 -d out/CircularInitDemo src/CircularInitDemo.java
java -cp out/CircularInitDemo CircularInitDemo
javac --release 21 -d out/InitFailureDemo src/InitFailureDemo.java
java -cp out/InitFailureDemo InitFailureDemo
javac --release 21 -d out/InstanceFailureDemo src/InstanceFailureDemo.java
java -cp out/InstanceFailureDemo InstanceFailureDemo
javac --release 21 -d out/ClassLoadingDemo src/ClassLoadingDemo.java
java -cp out/ClassLoadingDemo ClassLoadingDemo
```

Пять отрицательных файлов компилировались раздельно командой вида `javac --release 21 -d out/negative negative/<EntryPoint>.java`; имена entry points: `CyclicInheritance`, `CyclicThis`, `ForwardRef`, `NoNoArgSuper`, `NotFirst`. При повторе каждый исходник должен давать ожидаемую категорию отказа из таблицы N1–N5; сами исходники и точные исходные пути не сохранены, поэтому этот блок — команда для воспроизведения после восстановления файлов, а не свидетельство доступного в репозитории harness.

### Positive — уже выполненные сценарии Java 21

| ID | Сценарий | Ожидаемый наблюдаемый результат |
|---|---|---|
| L1 | `ChildInit.CONSTANT`, затем `ChildInit.dynamic`, затем `ChildInit.own`; superclass/subclass static field+block; interface default method | Constant `67` печатается без init; обращение к inherited non-constant field инициализирует только declaring Parent; обращение к Child own field инициализирует Child; class init запускает RootDefault, который объявляет default method, но не ChildNoDefault, пока его поле не запрошено |
| L2 | `static final int`/`String` против `static final Integer` | primitive/String constants читаются без вывода static initializer; boxed Integer запускает init |
| I1 | Parent/Child instance field + initializer blocks, `Child()` → `this(67)` → `super()` | Parent field/block/constructor выполняются до Child field/block/constructor; initializer Child выполняется один раз; final state `67/68` |
| I2 | Parent constructor вызывает overridable method Child; Child field initializer задаёт `68` | Override видит `0` (default value), после возврата Parent constructor Child initializer устанавливает `68` |
| C1 | A.x initializer читает B.y, B.y initializer читает A.x в одном потоке | Вывод `2/1`; recursive request A завершается, B читает ещё default `A.x=0` |
| F1 | Static field initializer бросает `IllegalStateException` | Первое активное использование: `ExceptionInInitializerError` с cause `IllegalStateException`; следующее: `NoClassDefFoundError` |
| F2 | Static initializer helper бросает `AssertionError` | Первое активное использование передаёт `AssertionError`; следующее: `NoClassDefFoundError` |
| F3 | Instance initializer бросает только при первой попытке создать экземпляр | Первая `new` завершается `IllegalStateException`; класс не становится erroneous; вторая `new` проходит |
| L3 | `ClassLoader.loadClass`, `Class.forName(name,false,loader)`, затем `Class.forName(name,true,loader)` | После первых двух действий static initializer не печатает ничего; при `true` инициализация происходит |

### Negative — отдельные компиляционные пробы

| ID | Кодовая ситуация | Ожидаемый результат `javac --release 21` |
|---|---|---|
| N1 | Subclass не объявляет конструктор, superclass объявил только `Parent(int)` | Ошибка: implicit `super()` не находит применимый no-args constructor |
| N2 | `A(){ this(67); }`, `A(int x){ this(); }` | Ошибка: recursive constructor invocation |
| N3 | `System.out.println(...); this(67);` в теле constructor | Ошибка Java 21: explicit constructor invocation должен быть первым оператором |
| N4 | `int first = later; int later = 67;` | Ошибка illegal forward reference |
| N5 | `class A extends B {}; class B extends A {}` | Ошибка javac cyclic inheritance; это **не** runtime `ClassCircularityError` |

### Рекомендуемые короткие фрагменты для будущей статьи

```java
class Parent {
    Parent() { printValue(); }
    void printValue() { }
}

class Child extends Parent {
    int value = 67;
    @Override void printValue() { System.out.println(value); }
}

new Child(); // Parent constructor sees 0; after it returns, value becomes 67
```

```java
class A { static int x = B.y + 1; }
class B { static int y = A.x + 1; }
// First active use: A.x == 2, B.y == 1
```

Это исследовательские минимальные формы. В статье 02 нужно отдельно показать полную цепочку Parent → Child, порядок полей/блоков и constructor bodies; не выдавать два фрагмента выше за готовое учебное изложение.

## 9. Реально выполненные проверки

**Окружение локальных проб:** `java -version` → `openjdk version "21.0.12.1" 2026-08-18`; `javac -version` → `javac 21.0.12`. ОС тестового контейнера — Debian.

**Авторский локальный протокол (не репозиторный harness):**

- Для семи positive demo programs (`ClassInitDemo`, `ConstantVsBoxedDemo`, `InstanceOrderDemo`, `CircularInitDemo`, `InitFailureDemo`, `InstanceFailureDemo`, `ClassLoadingDemo`) автор зафиксировал компиляцию `javac --release 21`, отдельный запуск каждого entry point через `java -cp` и сравнение stdout: **7/7 ожидаемых исходов**.
- Пять отдельных negative sources (`CyclicInheritance`, `CyclicThis`, `ForwardRef`, `NoNoArgSuper`, `NotFirst`) передавались `javac --release 21` раздельно: **5/5 ожидаемых отказов компиляции**. Причины перечислены в N1–N5; текст диагностики не считается нормативным.
- Значимые наблюдения и команды для повторения приведены в §8. Полные временные исходники, точные временные пути и сохранённые stdout не сохранились; поэтому исходное exact comparison нельзя независимо воспроизвести по PR. Не выдавать эти 12 проб за committed tests или CI harness.

**GitHub Actions — фактический repository CI:** для предыдущего HEAD `0d2ca5830f438504cce3fb0809eaa298f54dc4f9` найден PR-triggered run [#44731444729 / run 154](https://github.com/Melchiorius/backend-interview-labs/actions/runs/44731444729), `completed / success`. Его фактический scope: Gradle/JUnit, standalone Java Core example, существующие CORE-ACCESS и CORE-INHERITANCE checks. **CORE-INIT probes из §8 в этот workflow не входят и этим CI не проверялись.** Результат run #154 нельзя переносить на новый commit после исправлений без отдельной проверки нового HEAD.

**Не выполнялось (`not run`):** локальный Gradle/JUnit/Mockito; runtime `ClassCircularityError` на class file с циклическим superclass/superinterface graph; многопоточный deadlock stress test и отдельный concurrency test initialization lock; IntelliJ/Obsidian visual preview. Независимое review 04 на прежнем HEAD выполнено и запросило изменения; повторное review исправленного HEAD ещё не состоялось.

## 10. Границы соседних тем

- **CORE-CLASSES:** не переписывать определение default constructor, `this()`/`super()` синтаксис, простые default values и single-class textual order; использовать как prerequisite, затем добавить hierarchy-specific semantics.
- **CORE-INHERITANCE:** не повторять общий dynamic dispatch; объяснить только вызов override из constructor и момент, когда поля наследника ещё default-initialized.
- **CORE-INTERFACES:** только правило инициализации superinterfaces с default methods; не разбирать полностью default-method resolution/conflicts.
- **CORE-OBJECT:** не уходить в методы `Object`, identity/equality, GC/finalization и полный жизненный цикл объекта.
- **JVM-LOADING:** не превращать план в руководство по classloader delegation, modules, verification/linking internals, bytecode, metaspace/unloading или HotSpot implementation. Для `ClassCircularityError` достаточно границы загрузки/derivation (JLS §12.2.1; JVMS §5.3.5); не относить его к ошибкам linking-фазы и не смешивать с `ClassFormatError`.
- **CON-JMM:** не переносить полную модель happens-before, safe publication patterns, final-field semantics и data races; дать короткую связь и точную ссылку на отдельную тему. Вызов override из конструктора — сначала language initialization order, а не самостоятельная concurrency bug.

## 11. Контракт передачи ролям и решение G1

- **00 — координатор:** оценить границы и приоритеты матрицы, отдельно решить, достаточно ли P0 и не чрезмерно ли CORE-INIT захватывает JVM-LOADING/CON-JMM. Только явное решение 00 принимает G1. До него 02/03 не разблокированы.
- **02 — автор после G1:** связный русский текст с одним центральным примером инициализации иерархии; объяснить наблюдаемые значения по шагам; каждый P0 покрыть ответом, ссылкой JLS 21 и positive/negative контрпримером; не дублировать CORE-CLASSES/INHERITANCE.
- **03 — examples engineer после G1:** рассмотреть один автономный runnable Java 21 пример, если он помогает проверить порядок. Не создавать Gradle/JUnit/exercise модуль ритуально. Негативные исходники держать отдельно от успешно компилируемых.
- **05 — editor после интеграции 02/03:** проверить точные термины «загрузка/связывание/инициализация/конструирование», ясность порядка и подписи вывода, локальные ссылки; не считать чтение Markdown визуальным review.
- **04 — independent reviewer после G3:** проверить интегрированный HEAD и особенно §12.4.1/12.4.2, §12.5, интерфейсный default-method нюанс, constant variables, EIIE/NCDFE, recursive init vs ClassCircularityError, `this(...)` один раз и Java 21 first-statement restriction. Проверки 01 не заменяют независимое review.
- **06 — Research Ops:** после merge отдельным G6 сверить main, ссылки и COVERAGE; не повышать CORE-INIT до `implemented/verified` по факту существования этого плана.

## 12. Остаточные вопросы и риски

1. **Interface default-method initialization:** статья должна точно воспроизвести recursive postorder enumeration superinterfaces по JLS 12.4.2; учитывать также оговорку JLS §12.4.1 о compiler-generated synthetic default methods, которые могут влиять на initialization, хотя их не видно в исходном коде.
2. **ConstantValue и superclass order:** в detailed procedure constant variables инициализируются на раннем шаге до инициализации superclass; этот нюанс не следует сглаживать общей схемой «всегда superclass полностью раньше любого static значения подкласса».
3. **Class initialization deadlock:** specification описывает отдельные locks и ожидание; межпоточный цикл зависимостей может зависнуть. Без контролируемого воспроизводимого теста не обещать конкретный stdout или тайминг.
4. **Runtime ClassCircularityError:** source-level циклическое наследование проверено только как compile-time error. Runtime-сценарий требует class file с циклическим графом superclass/superinterface и относится к loading/derivation, а не linking; такой class file/custom loader в локальных пробах не создавался и не запускался.
5. **`this` escape / final fields:** финальные поля имеют специальную семантику при корректном завершении constructor, но публикация ссылки до завершения construction меняет применимость гарантий; подробности должны оставаться в CON-JMM.
6. **Поздние версии Java:** гибкие тела конструкторов после Java 21 не применять. В статье явно закрепить `Java 21`; preview-функции и новые JLS не смешивать с текущей нормой.

**Вывод роли 01:** исследовательский объём определён; отдельный план создан до статьи. Авторский локальный протокол сообщает 7 positive + 5 negative Java 21 проб, но временные исходники/stdout не сохранены, поэтому независимая побайтная воспроизводимость не обеспечена. Repository CI #154 прошёл на предыдущем HEAD, но не проверял CORE-INIT. Исправления внесены после review 00/04; новый HEAD передан на повторную оценку. G1 остаётся **не принятым**, G2 не начат, COVERAGE не изменялся.