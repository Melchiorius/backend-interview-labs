# CORE-SYNTAX — как компилятор Java 21 понимает файлы, имена и области видимости

**Область:** Java SE 21 **без preview**. Основание — [принятый G1-план CORE-SYNTAX + CORE-OPERATORS](../../docs/plans/CORE-SYNTAX-OPERATORS.md), Issue [#154](https://github.com/Melchiorius/backend-interview-labs/issues/154). Карта: **SY-01–SY-26: 16 P0 / 7 P1 / 3 P2**. Здесь объясняется, *где объявлено имя и какую сущность оно означает*. [CORE-OPERATORS](operators.md) посвящён тому, *как выражение вычисляется*. [Общий runnable Java 21](../../examples/core/syntax-operators/README.md) содержит [SyntaxProbe](../../examples/core/syntax-operators/src/SyntaxProbe.java), [p/Helper](../../examples/core/syntax-operators/src/p/Helper.java) и [demo/PackageProbe](../../examples/core/syntax-operators/src/demo/PackageProbe.java), [SY stdout](../../examples/core/syntax-operators/syntax.expected.stdout) и [PK stdout](../../examples/core/syntax-operators/package.expected.stdout). См. также [типы](types.md), [классы](classes.md), [доступ](access.md), [управление потоком](control-flow.md), [лямбды](functions.md).

Представим два файла: в одном объявлен вспомогательный тип `p.Helper`, во втором — `demo.PackageProbe`. Прежде чем JVM сможет запустить метод, компилятор должен разобрать каждый файл, установить принадлежность пакету, разрешить имена типов и членов, проверить видимость и типы. `import` помогает найти тип или статический член по короткому имени, но **не запускает код и не гарантирует загрузку класса**. Именно эти механизмы чаще всего путают при ответах «почему не компилируется».

**Воспроизведение.** Из корня репозитория с JDK 21 и Bash: `bash examples/core/syntax-operators/verify.sh`. Новые фрагменты относятся к [временным Boundary.java, Launch.java, пакетным и отдельным отрицательным файлам](https://github.com/Melchiorius/backend-interview-labs/issues/357#issuecomment-7225976462); полные исходники, команды и результаты по ссылке. Они не входят в общий runner. Не склеивайте самостоятельные файлы или фрагменты методов в один исходник.

## 1. От текста файла до единицы компиляции (SY-01, SY-02, SY-06, SY-14, SY-15; SY-20 P1)

JLS различает обычную (`ordinary compilation unit`) и модульную (`modular compilation unit`) единицы компиляции. Схема обычной: необязательная PackageDeclaration, затем ноль или больше ImportDeclaration и TopLevelClassOrInterfaceDeclaration; допустимы аннотации перед package и пустые верхнеуровневые объявления `;`. Модульная единица содержит импорты и объявление модуля (обычно module-info.java), а не обычный набор классов. JPMS здесь не разбирается. Запись `import` **после** объявления класса не является ещё одним оператором внутри программы: это нарушение грамматики. [JLS §7.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.3).

~~~java
// Файл Example.java, без package: unnamed package
import java.util.List;

public class Example {
    public static void main(String[] args) {
        System.out.println(List.of("a", "b").size()); // 2
    }
}
~~~

Имя файла для публичного верхнеуровневого класса должно соответствовать `Example.java` при обычной файловой компиляции `javac`. Два разных `public` верхнеуровневых типа в одном файле также не пройдут этот контроль (G1 отрицательные случаи SY-N09/SY-N10). Несколько **непубличных** верхнеуровневых типов в одном файле возможны, но их доступность и организация файлов — отдельный вопрос; не следует объявлять любую такую форму ошибкой. Формальные ограничения и зависимость от системы хранения исходников изложены в [JLS §7.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.6).

Если `package` отсутствует, тип относится к *неименованному пакету*; если записано `package p;`, полное имя верхнеуровневого типа будет, например, `p.Helper`. Структура каталогов обычно соответствует компонентам имени пакета, но **пакет не тождественен class loader**: `package` — языковая декларация; процесс загрузки/разрешения JVM и JPMS здесь не исследован. Нельзя ожидать, что тип из неназванного пакета станет доступен из именованного пакета простым импортом.

Принадлежность именованному пакету задаётся декларацией, а не каталогом. В совместной компиляции `pos/src/p/Names.java` (public Names, пустое объявление и package-private Sibling), `pos/src/elsewhere/Other.java` (тоже package p, использует Sibling), `pos/src/p/package-info.java` (`@Deprecated package p;`) и `pos/src/q/Use.java` явно переданы javac. Компиляция успешна; это не гарантия автоматического поиска исходников при произвольной структуре каталогов. Система хранения и параметры source path влияют на поиск и требования к имени файла. Отдельная -Xlint:all компиляция дала exit 0 и два предупреждения (auxiliary class в другом файле и отсутствие эффекта Deprecated для пакета), не ошибки языка. См. [JLS §7.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.4.1).

**Обычная точка входа Java 21** для запуска класса: `public static void main(String[] args)` (эквивалентная допустимая varargs-форма `String... args` также поддерживается). Метод не является обязательной частью каждого класса: библиотечные классы могут не иметь `main`. Отсутствие подходящего метода обнаруживается при *попытке запуска* класса, а не автоматически при компиляции произвольного библиотечного типа. `static` позволяет лаунчеру вызвать точку входа без создания экземпляра класса. Не смешивайте это с preview-синтаксисом unnamed classes и instance main из других режимов. [JDK 21 java launcher](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html).

Подходящий метод может быть **унаследованным**: в Launch.java Parent имеет `public static void main(String... args)`, Child extends Parent не объявляет main; `java -cp classes Child test` напечатал 1. NoMain и NonStaticMain (public void main(String[] args)) успешно скомпилированы, но при запуске получили exit 1, соответственно main method not found и main method is not static. Перегрузка main(int) может быть обычным корректным методом, но не выбранной точкой входа. Это стандартный class-file запуск Java 21 без preview; не обобщение на другие режимы.

Лексический разбор начинается ещё раньше: Unicode escapes обрабатываются до токенизации, затем распознаются идентификаторы, ключевые слова, литералы и разделители. `var` в Java 21 — контекстное ключевое слово в предусмотренных грамматикой позициях, в других может быть идентификатором: `int var=67;` напечатал 67. TypeIdentifier исключает var, поэтому `class var {}` не компилируется. Один символ `_` — зарезервированное ключевое слово, не обычный идентификатор без preview. `true`, `false`, `null` — литералы, не ключевые слова. VarType и Underscore отдельно отклонены javac. См. [JLS §3.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.8), [§3.9](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.9). Не помещайте в комментарии случайные `\u000a` как «просто текст»: Unicode-перевод способен изменить поток символов ещё до выделения комментария. Об этом см. [JLS §3.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.3) и `SY-22` ниже.

## 2. Импорт — сокращение имени, не разрешение доступа и не загрузка (SY-03, SY-04, SY-05, SY-16; SY-18, SY-23 P1)

Импорт типа `import java.util.List;` позволяет использовать простое `List` вместо квалифицированного `java.util.List`. Импорт **не** создаёт в классе поле и не «подключает объект». Указание `java.util.Map.of(...)` сразу через полное имя вообще не требует импорта. Каждая единица компиляции неявно импортирует доступные публичные типы java.lang по требованию (как `import java.lang.*;`); доступные типы своего пакета тоже можно использовать по простым именам. Импорты действуют только в данной единице, не во всех файлах пакета и не внутри других import-деклараций. [JLS §7.5](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.5).

~~~java
import java.util.List;
import static java.lang.Math.max;

class ImportDemo {
    static int calculate() {
        List<Integer> values = List.of(6, 7);
        return max(values.get(0), values.get(1)); // 7
    }
    static int qualified() {
        return java.util.Map.of("key", 67).get("key"); // 67
    }
}
~~~

`import static java.lang.Math.max;` делает короткое имя **статического метода** доступным для поиска. Аналогично можно импортировать статическое поле, но не поле экземпляра: `import static java.awt.Point.x;` в G1 SY-N17 отклоняется, потому что `Point.x` — нестатическое поле. Для обычного импорта класса и статического импорта члена существуют отдельные правила, не взаимозаменяемые.

Single-type import использует каноническое имя `java.util.Map.Entry`, не бинарное `java.util.Map$Entry`. Обычный on-demand import может указывать тип, не только пакет: `import java.util.Map.*;` импортирует доступные member types, в том числе Entry. Single-static import импортирует все доступные статические члены данного имени, включая перегрузки и статические member types. В Boundary с import java.util.Map.* и import static java.util.Map.Entry.* тип Entry и метод comparingByKey разрешились; `Comparator<Entry<String,Integer>> cmp = comparingByKey();` предоставил целевой тип, вывод 67,−1. В q.Use single-static imports p.Names.pick и p.Names.Member дали `package=67,ok,2,p.Names$Member` (две перегрузки и вложенный тип). Тип из unnamed package нельзя импортировать; совместная компиляция Unnamed.java и q/UnnamedUse.java с простым именем Unnamed в named package отклонена cant.resolve.location, хотя оба файла переданы javac. См. [JLS §6.7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.7), [интерфейсы](interfaces.md).

Особенно важно различать следующие ситуации:

- `import java.util.*; import java.awt.*;` и последующий `List` приводят к **неоднозначности использования** простого имени: два потенциально подходящих типа (SY-N11). Явное `java.util.List` снимает неоднозначность.
- `import java.util.List; import java.awt.List;` — коллизия двух явных single-type imports одного простого имени (SY-N13). Даже без создания объекта компилятор обязан отклонить конфликтующие декларации.
- Два `import static` с одинаково применимыми членами разных типов могут сделать вызов неоднозначным (SY-N12); это не тот же механизм, что повторное объявление локальной переменной.

Single-type import java.util.List разрешает List при import java.awt.*; повторный идентичный single-type import игнорируется (Boundary). Два разных single-type imports одного простого имени отклонены даже без использования (ImportConflict). Для статических методов сначала учитывается затенение: по [JLS §6.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.4.1) single-static import метода затеняет во всей единице компиляции метод с тем же именем и сигнатурой из static-import-on-demand. Другая перегрузка этим правилом не затеняется. Затем [§15.12.2.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2.1) предписывает рассматривать незатенённые потенциально применимые методы из импортов, далее проверяются применимость и наиболее специфичный метод. Не следует отождествлять эту норму с фактическим поведением любого компилятора. В q.StaticChoice импортированы p.Names.pick(int/String) и q.StaticChoiceAlt.pick(long); pick(67) и pick(68L) выбрали int и long, вывод 67,68. StaticAmbig с Math.abs(int) и StrictMath.abs(int) отклонён ref.ambiguous. См. [JLS §7.5.1–4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.5.1), [§15.12.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2).

**Отдельная проверка single-static import против wildcard.** В [полных временных исходниках и результатах](https://github.com/Melchiorius/backend-interview-labs/pull/358#issuecomment-7226127484) p.A.pick(int) возвращает 67, q.B.pick(int) — 68, q.B.pick(long) — 69. При `import static p.A.pick; import static q.B.*;` по указанным правилам JLS ожидаются 67 для pick(1) и 69 для pick(1L). Однако на проверенном javac 21.0.12.1 программа Choice с обоими неквалифицированными вызовами **отклонена**: для pick(1L) диагностируется cant.apply.symbol у p.A.pick(int), с возможной потерей точности long → int. Это расхождение с нормативным ожиданием, не успешный вывод 67/69. В отдельном Shadow с теми же импортами вызовы `pick(1), q.B.pick(1L), q.B.pick(1)` дали 67,69,68; в Wild только с import static q.B.* вызов pick(1L) дал 69. В BothSingle с двумя явными static imports вызов pick(1) отклонён ref.ambiguous. Предыдущие StaticChoice/StaticAmbig тоже используют два single-static imports, а не wildcard. Для однозначного обращения к нужному методу в таком случае укажите имя класса. Не утверждаем, что наблюдаемая особенность присуща всем версиям javac или другим компиляторам; причина расхождения и конкретный upstream bug ID не установлены.

`import java.util.*` **не** импортирует автоматически подпакеты `java.util.concurrent`. Наличие импортируемого имени тоже не отменяет контроль доступности: `package-private` тип другого пакета не становится `public` после `import`. В §7 ниже приведён именно двухфайловый, а не условный, контрпример.

## 3. Области видимости и связывание имён (SY-07–SY-10; SY-17 P1)

Область видимости — участок программы, где конкретная декларация может обозначаться именем согласно правилам языка. Внутри метода доступность локального имени определяется блоками; scope не равен времени жизни объекта или физическому размещению переменной. Если переменная объявлена в `{...}`, после закрытия этого блока её имя снаружи **недоступно**. Соседний блок может объявить переменную с тем же именем, но вложенный блок, перекрывающий область уже объявленной локальной, **не вправе** повторно объявлять её тем же именем.

~~~java
class ScopeDemo {
    private int balance = 9;

    int sum(int balance) {
        int computed;
        {
            int increment = 5;
            computed = balance + increment; // параметр 4 -> 9
        }
        // System.out.println(increment); // SY-N03: вне блока
        return computed + this.balance;      // 9 + поле 9 = 18
    }
}
~~~

Параметр `balance` **затеняет поле** `this.balance`; указание `this.` позволяет выбрать поле явно. Затенить поле локальной переменной законно. Но два перекрывающихся локальных объявления `int n=1; int n=2;` — **ошибка повторного объявления** SY-N01. Вопрос о затенении нельзя отвечать фразой «вложенный блок всегда разрешает то же имя».

**Имя до объявления** и **имя вне блока** — разные позиции поиска; ошибка возникает, если нет другой подходящей декларации. В SY-N02 вызов `System.out.println(n);` стоит перед `int n=67;`: пока соответствующее локальное имя недоступно; в SY-N03 `n` уже было объявлено, но его область видимости завершилась. Квалифицированной записи наподобие `this.n` для локальной переменной не существует.

**SY-10: собственный initializer уже в scope.** При поле `int n=67;` внутри метода `int n=n;` связывает правое n с локальной, не с полем; OwnInitializer отклонён var.might.not.have.been.initialized (DA). До локального объявления имя может разрешиться в поле: Boundary.scope() напечатал pre=67 до `int n=7;`. Missing без поля дал cant.resolve.location (не найдено имя), а не DA. `int n=this.n;` явно выбирает поле. Scope локальной в блоке начинается с собственного initializer, охватывает последующие declarators той же декларации и оставшуюся часть блока. Обычный вложенный блок и lambda не вправе переобъявить перекрывающееся локальное имя: BlockRedeclare/LambdaRedeclare отклонены already.defined. JLS §6.4.1 допускает исключение для деклараций внутри вложенного класса/интерфейса: в Boundary local class Local, его метод f() с `int n=67;` допустим, внешний n=7, вывод nested=67:7. См. [JLS §6.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.3), [вложенные классы](nested-classes.md), [lambda](functions.md).

Термины JLS также не следует сводить к одному слову «скрытие»:

| Термин | О чём речь | Как различать |
|---|---|---|
| **Shadowing — затенение** | Декларация с подходящим именем в определённой области видимости мешает обратиться к другой декларации по простому имени | Локальный `balance` и поле `balance`: для поля можно написать `this.balance` |
| **Hiding — сокрытие унаследованного члена** | Член подкласса скрывает определённый член суперкласса по правилам JLS §8.3 и §8.4.8.2 | Для `static` методов — *method hiding*, а не динамическое переопределение |
| **Obscuring — заслонение имени** | В синтаксическом контексте имя переменной может заслонить имя типа или пакета, заставляя иначе классифицировать запись | Это вопрос *классификации имени*, не просто две локальные переменные |

Obscuring: при `int java=67;` выражение `java.util.List.of(67)` пытается обратиться к члену переменной java типа int; Obscure отклонён cant.deref. В позиции типа `java.util.List<String> typeContext=List.of("ok");` имя классифицируется иначе, Boundary напечатал obscure=67,ok. Даже квалифицированное имя в выражении не всегда обходит заслонение. Hiding: если B extends A объявляет static f() той же сигнатуры, что доступный унаследованный static A.f(), вызов B.f() выбирает скрывающий метод, A.f() — исходный; это не виртуальный dispatch. Не всякий одноимённый недоступный член наследуется. См. [интерфейсы](interfaces.md).

Подробнее: [JLS §6.4.1–§6.4.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.4.1), [наследование](inheritance.md), [модификаторы доступа](access.md).

## 4. Определённость присваивания — отдельно от существования имени (SY-11)

После объявления `int result;` имя существует в допустимой области, но **прочитать** его можно только если формальные правила definite assignment устанавливают присваивание до этой точки. Это анализ структуры программы по JLS, не произвольный анализ всех фактических runtime путей. Это *definite assignment* (`DA`), а не автоматическая инициализация локальных переменных нулём.

~~~java
class AssignmentFlow {
    static int choose(boolean flag) {
        int result;
        if (flag) result = 67;
        else result = 0;
        return result; // любое выполнение присвоило result
    }
}
~~~

Контрпример SY-N04 — `int n; if (condition) n=67; return n;`: при `condition=false` путь до `return` не содержит присваивания. Компилятор отклоняет такой код до запуска. Поля класса имеют значения по умолчанию; переносить этот механизм на локальные переменные нельзя. Детали [JLS §16](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html).

В Boundary.da() `int v; if(true)v=67; return v;` допустим и дал 67; `boolean b=true; int n; if(b)n=67; return n;` (DAConstant) отклонён, хотя человек знает b. if(true) содержит constant expression, изменяемая b — нет. У blank final дополнительно требуется definite unassignment перед присваиванием: BlankFinal с двумя последовательными присваиваниями отклонён var.might.already.be.assigned. См. [инициализация](initialization.md), [управление потоком](control-flow.md).

## 5. `var`: вывод статического типа, а не динамическое «что угодно» (SY-12; SY-21 P1)

`var words = java.util.List.of("Java", "21");` объявляет **локальную** переменную, тип которой выводится компилятором из инициализатора. Он остаётся статически определённым после компиляции; это не «поле любого типа» и не аналог JavaScript `var`. В G1 SY08 вызов `words.get(0)+words.get(1)` даёт `Java21`.

~~~java
class LocalInference {
    static int parse() {
        var value = 67; // компилятор вывел int
        // value = "text"; // несовместимое присваивание
        return value;
    }
}
~~~

Отрицательные случаи G1 отделяют ограничения: `var n;` (SY-N05) — нет инициализатора; `var n=null;` (SY-N06) — нельзя вывести самостоятельный тип из null; `var n=67;` на уровне **поля** (SY-N07) — здесь запрещено; `var a=1,b=2;` одной декларацией (SY-N08) — нельзя. Удачный пример с `var` **не** доказывает эти отрицания без отдельного `javac`. Подробнее о типах и преобразованиях — [CORE-TYPES](types.md), [CORE-CONVERSIONS](conversions.md), [JLS §14.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.4.1).

**SY-21: самостоятельный initializer var не получает assignment target type от объявления.** `List<String> typed=new ArrayList<>();` использует целевой тип, `var loose=new ArrayList<>();` выводит `ArrayList<Object>`. В Boundary typed.add("x"), loose.add(67) допустимы, loose.get(0) доступен как Object. `var x=()->67;`, `var x=String::length;`, `var x={67};` не имеют нужного target type (VarLambda/VarRef/VarArray отклонены отдельно). `var x=(String)null;` допустим (String), `var x=null;` нет. Выведенный тип может не иметь записываемого имени: `var anon=new Object(){int f(){return 67;}};` позволил вызвать anon.f(), результат 67. В общем случае учитывается проекция синтетических type variables (§14.4.1), не просто runtime-класс объекта.

Var допустим в local declaration, basic-for initializer, enhanced-for variable и resource declaration; отдельная форма lambda `(var a,var b)->a+b` требует functional target. Смешивать var с inferred identifier или explicit type нельзя (VarMixed). В Boundary all-var lambda дала 67, enhanced-for сумму 67, try(var resource = new ByteArrayInputStream(...)) чтение 67. VarField/VarReturn/VarParam (обычный параметр метода) отклонены restricted.type.not.allowed.here. См. [generics](generics.md), [lambda](functions.md), [ресурсы](resources.md).

## 6. Захват локальных переменных и `effectively final` (SY-13)

Лямбда, созданная внутри метода, может использовать локальную переменную из окружающей области, только если эта переменная `final` или **effectively final** и definitely assigned к месту захвата. У local с initializer и параметра без final запрещены assignment operator и ++/--; даже присваивание того же значения нарушает effectively final. Для local без initializer каждое присваивание должно происходить там, где переменная definitely unassigned и not definitely assigned; ++/-- запрещены. Это ограничение относится к *переменной*, а не запрещает изменять содержимое отдельного объекта, на который она ссылается.

~~~java
import java.util.function.IntSupplier;

class CaptureDemo {
    static int value() {
        int seed = 67;
        IntSupplier next = () -> seed + 1;
        return next.getAsInt(); // 68
    }
}
~~~

Если после создания лямбды вставить `seed=68;` или сделать другое переназначение `seed`, получится ошибка SY-N14: переменная уже не effectively final. В Boundary.capture(boolean) переменная v объявлена без initializer и присваивается в двух взаимоисключающих ветвях, затем захватывается IntSupplier: результаты 67 и 68. CaptureSame/CaptureInc отклонены non.effectively.final.var, CaptureDA — var.might.not.have.been.initialized. Мутация объекта через захваченную ссылку не равна присваиванию самой переменной. Единая модель EF/DA — [вложенные классы](nested-classes.md). Более полная модель lambda scope и method references — [CORE-FUNCTIONS](functions.md), [JLS §15.27.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27.2).

## 7. Настоящая multi-file компиляция: `package` и `static import` (SY-02, SY-03, SY-19, SY-23)

В отличие от иллюстрации, где имя `Helper` «должно где-то существовать», ниже **обе компиляционные единицы показаны целиком**. Один исходник объявляет тип в пакете `p`, второй обращается к его публичным членам из `demo`.

**Файл `p/Helper.java`:**

~~~java
package p;
public final class Helper {
    private Helper() {}
    public static final int VALUE = 67;
    public static String greet() { return "hello"; }
}
~~~

**Файл `demo/PackageProbe.java`:**

~~~java
package demo;
import p.Helper;
import static p.Helper.greet;
import static p.Helper.VALUE;

public class PackageProbe {
    public static void main(String[] args) {
        System.out.println("PK01=" + greet() + ":" + VALUE);
        System.out.println("PK02=" + Helper.VALUE);
        System.out.println("PK03=" + java.util.List.of(6, 7).size());
    }
}
~~~

Сохранить файлы в таком дереве, из каталога **над** `p/` и `demo/` выполнить (для Java 21):

~~~bash
mkdir -p out
javac --release 21 -Xlint:all -d out p/Helper.java demo/PackageProbe.java
java -cp out demo.PackageProbe
~~~

Ожидаются **три** строки: `PK01=hello:67`, `PK02=67`, `PK03=2`. Первый вывод использует импорт статических метода и поля; второй — обычный импорт типа; третий — полностью квалифицированное имя без импорта. Компилятор видит оба файла в одной команде, а `-d out` кладёт `p.Helper` и `demo.PackageProbe` в соответствующую пакетную структуру. Эти исходники и три stdout-строки **локально проверены в G1**, но не следует считать любой текст, вставленный из статьи, автоматически включённым в CI.

**Контрпример SY-N15 (тоже два файла):** `p/Hidden.java` с `package p; class Hidden { public static int value=67; }` и `q/UseHidden.java` с `package q; import p.Hidden; public class UseHidden { Hidden ref; }`. Ошибка именно в том, что `Hidden` — *непубличный верхнеуровневый тип другого пакета*; импорт не повышает доступность. Два файла участвуют в компиляции, поэтому это не проблема «отсутствующего класса». Независимый G1-рецензент получил `compiler.err.not.def.public.cant.access` при `javac --release 21 -XDrawDiagnostics`. Имена классов и пример завершёнными исходниками см. в [G1 §7.1](../../docs/plans/CORE-SYNTAX-OPERATORS.md).

## 8. Почему код не компилируется: диагностические категории (SY-04, SY-06, SY-09–SY-16; SY-18)

Правильная последовательность ответа на собеседовании — определить, **на какой стадии и какая норма нарушена**, а не повторять «Java запрещает». Эти исследовательские отрицательные проверки G1 изолированы от успешной программы:

| Случай | Что нарушено | Что *не* является объяснением |
|---|---|---|
| SY-N01 | Два локальных имени в перекрывающейся области | «Потому что переменные в Java глобальны» |
| SY-N02 / SY-N03 | Имя до объявления / имя вне области видимости | «DA запрещает любое объявление без значения» |
| SY-N04 | Чтение локального без доказанного присваивания | «Поля не получают default» |
| SY-N05–N08 | Недопустимая форма локального вывода `var` | «`var` — динамическая переменная» |
| SY-N09/N10 | `public` верхнеуровневый тип и соответствие имени файла | «Нельзя иметь больше одного объявления вообще» |
| SY-N11/N12/N13 | Неоднозначный импорт / коллизия имён | «`import` делает класс полем объекта» |
| SY-N14 | Захват переназначаемой локальной | «Лямбда не умеет читать внешние значения» |
| SY-N16 | `import` после декларации типа: намеренное нарушение грамматики | «В файле отсутствует закрывающая скобка» |
| SY-N17 | `import static` нестатического поля `Point.x` | «Импорты не разрешают поля вообще» |

**Никакой отдельный отрицательный исходник не входит в обычный успешный runnable.** В G1 независимый рецензент подтвердил **16 single-file SY-negatives** и отдельно **один двухфайловый SY-N15** по тематическим диагностическим категориям `javac 21`. Спецификация требует ошибок по смыслу, но не фиксирует текст `compiler.err.*` для всех компиляторов и IDE. Чтобы повторить отрицательную компиляцию, нужен отдельный полный исходник с точным именем файла: [G1 §7](../../docs/plans/CORE-SYNTAX-OPERATORS.md).

## 9. Лексические тонкости и границы (SY-22 P1; SY-24–SY-26 P2)

**Unicode escapes (SY-22 P1):** последовательности Unicode escape переводятся до того, как лексический анализатор выделяет комментарии или литералы; поэтому текст, внешне похожий на «закомментированный», может превратиться в новую строку. Не формируйте случайные невалидные учебные исходники из пробельных/управляющих escape: важен порядок этапов, зафиксированный [JLS §3.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.3). Не всякий обратный слеш начинает Unicode escape: §3.3 определяет eligibility по предыдущему результату перевода, в частности чётности последовательности непосредственно предшествующих обратных слешей (или предшествующему символу, полученному из Unicode escape). Только допустимый слеш с одним или несколькими u и четырьмя hex-цифрами образует escape; результат не подвергается повторному рекурсивному переводу. В Boundary строка `// translated newline: \u000a System.out.println("unicode="+67);` после перевода содержит перевод строки, затем оператор println. Фактический вывод unicode=67; это безопасный положительный пример, не проверка всех вариантов eligibility или parser errors.

**Не исследовано / не реализовано как отдельная тема:**

- **SY-24 P2:** JPMS `module-path`/`exports`, особенности различных компиляторов и launch modes, preview unnamed class/instance `main` в JDK 21.
- **SY-25 P2:** загрузка и инициализация классов JVM, Reflection, annotation processors, JShell, IDE/Obsidian runtime.
- **SY-26 P2:** расширенная область видимости pattern variables и flow scoping — [CORE-PATTERNS](patterns.md), не второй курс в этой статье.

Отличайте «описана языковая норма» от «мы протестировали runtime и среду разработки».

## 10. Принятая G1-трасса `SyntaxProbe` и самопроверка

В [принятом G1 §6.1](../../docs/plans/CORE-SYNTAX-OPERATORS.md) есть полный `SyntaxProbe.java` и самостоятельный эталон — **14 строк вывода одного процесса**, отличного от `OperatorsProbe` и `PackageProbe`. Сейчас опубликованы [исходник](../../examples/core/syntax-operators/src/SyntaxProbe.java), [эталон](../../examples/core/syntax-operators/syntax.expected.stdout) и [проверочный скрипт](../../examples/core/syntax-operators/verify.sh) трёх отдельных JVM. Значения из локальной проверки G1:

| Строка | Значение | Почему |
|---|---|---|
| SY01 | `7:67:8` | локальное `value=7` затеняет статическое поле `67`; вложенное вычисляет `8` |
| SY02 | `14` | параметр `balance=5` + `this.balance=9` |
| SY03 | `7` | `import static Math.max` |
| SY04 | `2` | `List.of("x","y").size()` |
| SY05 | `68` | `effectively final` захват `67` |
| SY06 / SY07 | `no` / `yes` | обе ветви присваивают локальный результат |
| SY08 | `Java21` | тип списка выведен через `var` |
| SY09 | `67` | полностью квалифицированный `java.util.Map` |
| SY10 | `true` | запуск без аргументов, `args.length==0` |
| SY11 | `SyntaxProbe` | `.class.getSimpleName()` |
| SY12 | `true` | `"hello" instanceof String` |
| SY13 / SY14 | `7` / `5` | переменная вложенного блока и внешняя переменная |

**Условия важны:** SY10=true только при запуске без аргументов; иначе ожидается другое значение. Это наблюдение не описывает все варианты `main` или launcher. Пробный `PackageProbe` даёт 3 строки третьего процесса, см. §7. Исследовательский G1 §6 прошёл на OpenJDK 21.0.12.1 с `javac --release 21 -Xlint:all`, три процесса и три `diff -u`; исходники из G1 сами по себе не часть CI репозитория. На [Actions #45006826872](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45006826872) выполнен технический HEAD `9d377ccccfec54153e2ee62a49ee9198b45ba120`, на [Actions #45007263735](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45007263735) — интегрированный author HEAD `aa668e5ac39a4b5b320f13ffd135e38b7fbae8f1`. Затем [Actions #45007640817](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45007640817) завершился **SUCCESS** на авторском HEAD `b898bfae718844f394d56401c1e9f0637b575798`, включая Gradle/JUnit и Java 21 runner. Эти запуски относятся только к названным SHA; [PR #156](https://github.com/Melchiorius/backend-interview-labs/pull/156) — историческая публикация; для нового HEAD проверка выполняется отдельно, исторические CI не наследуются.

### Самопроверка: предскажите компиляцию

Каждый случай — независимый фрагмент. Сначала решите, компилируется ли он и какая сущность обозначена именем. Полные исходники для воспроизведения — [приложение](https://github.com/Melchiorius/backend-interview-labs/issues/357#issuecomment-7225976462); не объединяйте их.

1. В instance-методе класса с полем n=67 объявлено `int n=n;`.
2. В методе `int n; if(true)n=67; return n;` и отдельно конструкция с `boolean b=true; if(b)n=67;` вместо if(true).
3. При `import java.util.List; import java.awt.*;` используется List; отдельно `import java.util.List; import java.awt.List;` даже без использования.
4. `var x=(String)null;` и отдельно `var x=String::length;`.

### Объяснение самопроверки

1. Ошибка DA: правое n уже локальное, не поле (OwnInitializer). 2. Первый допустим, второй отклонён DAConstant, несмотря на значение b. 3. Single-type import разрешает List, два разных single-type imports конфликтуют (ImportConflict). 4. Cast даёт String, method reference без target не выводится (VarRef). Это фактические отдельные компиляции и Boundary, не новые JUnit-тесты.

## 11. Матрица полноты SY-01–SY-26

Статус P0/P1 — **приоритет объяснения**, а не результаты независимого G3/G4 и не автоматический перевод `COVERAGE` в Implemented. Каждая строка указывает на конкретный механизм и проверку/контрпример.

| ID | P | Раздел и проверяемый механизм |
|---|---|---|
| SY-01 | P0 | §1: compilation unit, `import` после типа недопустим SY-N16 |
| SY-02 | P0 | §1, §7: named/unnamed package, PK01; не classloader |
| SY-03 | P0 | §2, §7: type/static imports, SY03/PK01–PK03 и SY-N17 |
| SY-04 | P0 | §2, §8: import ambiguity, SY-N11/N12/N13 |
| SY-05 | P0 | §2: simple vs qualified, java.lang, SY09/PK03 |
| SY-06 | P0 | §1, §8: public top-level и filename SY-N09/N10 |
| SY-07 | P0 | §3: block scope, SY13/14 и SY-N03 |
| SY-08 | P0 | §3: parameter/local shadow field, SY01/02 |
| SY-09 | P0 | §3, §8: duplicate local SY-N01 отклонён |
| SY-10 | P0 | §3: имя до объявления, собственный initializer и DA, после блока, SY-N02/N03 и OwnInitializer |
| SY-11 | P0 | §4: definite assignment, SY06/07 и SY-N04 |
| SY-12 | P0 | §5: local `var`, SY08 vs SY-N05–N08 |
| SY-13 | P0 | §6: effectively-final capture, SY05 и SY-N14 |
| SY-14 | P0 | §1, §10: стандартный Java21 `main`; SY10 зависит от args |
| SY-15 | P0 | §1, §8: lexer/keywords и grammar import-before-types SY-N16 |
| SY-16 | P0 | §2, §8: instance field нельзя `import static`, SY-N17 |
| SY-17 | P1 | §3: shadowing / hiding / obscuring разведены |
| SY-18 | P1 | §2, §8: single/on-demand/static collisions, SY-N11–N13 |
| SY-19 | P1 | §7: 2-file package-private SY-N15 vs public PK01 |
| SY-20 | P1 | §1: top-level package-private и соответствие имени файла |
| SY-21 | P1 | §1, §5: `var` contextual keyword, TypeIdentifier и самостоятельный initializer |
| SY-22 | P1 | §9: Unicode escape eligibility и фактическое unicode=67 |
| SY-23 | P1 | §2, §7: import ≠ access; SY-N15 |
| SY-24 | P2 | §9: JPMS/preview/launch variants НЕ ПРОВЕРЕНО |
| SY-25 | P2 | §9: classloader/Reflection/IDE НЕ ПРОВЕРЕНО |
| SY-26 | P2 | §9: flow scope pattern variables — ссылка, НЕ ЗАКРЫТО |

## Источники и пределы доказанного

**Нормативные первоисточники Java SE 21:** [JLS §3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html), [JLS §6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html), [JLS §7](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html), [JLS §14](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html), [JLS §16](https://docs.oracle.com/javase/specs/jls/se21/html/jls-16.html); [JDK21 launcher](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html). Для независимых интервью-вопросов использованы источники C1/C2 из [G1 §2](../../docs/plans/CORE-SYNTAX-OPERATORS.md) как формулировки вопросов, **не** как авторитет о языке.

**Что подтверждено G1/04:** полные `SyntaxProbe` (14), `OperatorsProbe` (27), `PackageProbe` (3) были собраны на OpenJDK 21.0.12.1; их три эталона совпали с stdout, 33 отдельных compile-negative (16 SY single + 16 OP single + 1 SY-N15 two-file) проверены локально по релевантным diagnostic keys. **Это историческое локальное evidence G1/04, а не штатный CI.** Позднее [CI #45007263735](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45007263735) успешно проверил интеграцию на `aa668e5ac39a4b5b320f13ffd135e38b7fbae8f1`, а [CI #45007640817](https://github.com/Melchiorius/backend-interview-labs/actions/runs/45007640817) — опубликованную редакцию на `b898bfae718844f394d56401c1e9f0637b575798`. Эти CI проверяют **четыре committed positive Java source, три JVM-процесса и 14+27+3 stdout-строки**, а также штатные проверки репозитория; **не** 33 compiler-negative в CI, **не** каждый отдельный inline Java-фрагмент и **не** прохождение G3/G4/G5. Для нового SHA результаты проверок фиксируются отдельно в соответствующем PR; исторический PR #156 не подтверждает последующие изменения. Все новые inline code snippets не объявляются отдельно прогнанными. Obsidian/IntelliJ GUI, якоря Markdown, JPMS/runtime class loading, JMM/JIT/GC/JMH, альтернативные компиляторы/ОС и границы SY24–26 — **NOT RUN**. Текущий статический аудит относительных ссылок при публикации не равен GUI-проверке. Актуальная навигация: [COVERAGE](../../docs/COVERAGE.md) и [CURRICULUM](../../docs/CURRICULUM.md); матрица статьи не является итоговой приёмкой.

**Новые фактические проверки 02.** [Полные временные исходники и наблюдения](https://github.com/Melchiorius/backend-interview-labs/issues/357#issuecomment-7225976462) на Debian OpenJDK/javac 21.0.12.1: итоговая компиляция шести файлов с --release 21 -encoding UTF-8 -XDrawDiagnostics exit 0, stdout/stderr пусты; отдельная -Xlint:all компиляция exit 0 с двумя предупреждениями (не ошибка и не Werror). Boundary, Child, q.Use и отдельно q.StaticChoice успешно запущены, NoMain/NonStaticMain после успешной компиляции отклонены лаунчером exit 1. Отрицательные компиляции 22 однофайловых + один двухфайловый сценарий, все exit 1/stdout пуст, диагностики по ссылке. Первоначальная ошибка вывода generic comparator в цепочке исправлена в временном исходнике до успешных проверок; первые class-not-found запуски не засчитываются как проверка main. Исторические 33 отрицательные компиляции G1, новые 23 и 24 ожидаемых отклонения всего CI — разные наборы.

**Дополнительная проверка N05 (02).** [Исходники A/B/Choice/Shadow/Wild/BothSingle, команды, вывод и хеши](https://github.com/Melchiorius/backend-interview-labs/pull/358#issuecomment-7226127484): четыре отдельные компиляции с `--release 21 -encoding UTF-8 -XDrawDiagnostics -Xlint:all -Werror` на том же JDK, Shadow/Wild exit 0 с пустыми stdout/stderr, Choice/BothSingle exit 1 с пустым stdout и указанными выше диагностическими категориями. Два успешных JVM-запуска дали соответственно 67,69,68 и 69 (stderr пуст). Попытка запуска Choice после неуспешной компиляции дала ClassNotFoundException, не является самостоятельным наблюдением о main. Нормативное ожидание и наблюдение для mixed imports разделены. Это четыре новых компиляции, не повтор прежних 23 отрицательных сценариев или всего runner; следующие сведения о runner относятся к предыдущей фактической проверке 02.

Полный неизменённый runner выполнен на восьми восстановленных входах с совпадающими Git blob SHA/mode (не полном клоне): default POSIX, затем LC_ALL=C.utf8 с временной обёрткой diff, сохраняющей actual.stdout до очистки и вызывающей настоящий /usr/bin/diff с прежними аргументами. Оба exit 0, stderr пуст, инвентарь до/после совпал. Одна компиляция четырёх исходников с --release 21 -Xlint:all, отказ при stderr компилятора, три JVM с проверкой пустого stderr и три diff -u, очистка trap. Во втором запуске сохранены фактические SY14/130 байт, OP27/295, PK3/29, все cmp с fixture exit 0; их SHA-256 в приложении. Внешний stdout обоих запусков — только одна строка/91 байт, SHA-256 f70c4dd9462d21b082a7720352501eab01528985d36f2dfcad9a567f63c9275a: `CHECK PASSED: Java 21 syntax/operators/package; 14+27+3 stdout lines (three JVM processes)`. Это 44 наблюдения, не 44 теста и не печать всех 44 строк. Скрипт не использует Werror или явный encoding; установлены OS locales C/C.utf8/POSIX, альтернативные JVM/OS и все Markdown-фрагменты не проверены. Практический пример и эталоны не изменялись.
