# CORE-OBJECT + CORE-EQUALITY — исследовательский план Java 21 (до статьи)

**Статус:** G1 / роль 01, ожидает рассмотрения 00; не статья и не acceptance. **Дата исследования:** 20.11.2026. **Issue:** [#59](https://github.com/Melchiorius/backend-interview-labs/issues/59), родитель [#33](https://github.com/Melchiorius/backend-interview-labs/issues/33). **BASE `main`:** `261f195a199f61e16abed5b0eaf16605840907b3`. **Нормативная версия:** Java SE 21, JLS 21 и Java SE 21 API. **Локальная проверка:** OpenJDK 21.0.12.1, `javac --release 21`. В рамках G1 запрещено объявлять материал готовым, публиковать статью за 02 и повышать COVERAGE.

## 1. Вопрос читателя, решение об архитектуре и границы

**Вопрос:** почему ссылки на объекты могут различаться при равных значениях, как правильно определить `equals` / `hashCode`, как не разрушить эквивалентность при наследовании и как `HashMap` и `TreeMap` по-разному интерпретируют «одинаковые ключи»?

**Решение на согласование 00: две связанные самостоятельные статьи, один общий исследовательский план.** Сохранить два ID каталога, не делать два независимых повторяющихся объяснения:
- **`CORE-OBJECT`:** `Object` как superclass, отличие identity от value equality, *исходная* реализация `Object.equals` (identity) и назначение `hashCode` в одном абзаце; `getClass`, `toString`, `clone`, `finalize`, `wait/notify` — по границам. **Не** разворачивать свойства эквивалентности, сортировку, `HashMap`-сценарии и дизайн value objects.
- **`CORE-EQUALITY`:** подробный контракт `equals/hashCode`, `==` и null-safe `Objects.equals`, симметрия и транзитивность при наследовании, коллизии, изменяемый ключ, естественный порядок и компаратор, `BigDecimal` и массивы как контрпримеры. Ссылается на введение `Object`, не повторяет каталог методов `Object`.
- **Причина:** `Object` содержит также мониторные и исторические методы, мало связанные с правилом равенства; `CORE-EQUALITY`, напротив, непосредственно соединяет type semantics, collections и сравнение. Единая огромная статья либо раздует обзор `Object`, либо будет вынуждена поверхностно раскрыть `Map`/`SortedSet`. Две статьи с жёстким разделом ответственности позволяют самостоятельно изучать каждую без дублирования полного контракта.
- **Навигация после G2/G5:** обе записи `CORE-OBJECT` / `CORE-EQUALITY` в CURRICULUM указывают на **разные реальные файлы**; из обзора Object ссылка на Equality, из Equality — на Object. Для читателя нужен маршрут `CORE-TYPES → CORE-OBJECT → CORE-EQUALITY → COL-HASH/COL-SORTED`. В рамках G1 файлов статей и ссылок на несуществующие страницы не создавать.

**Границы:** язык `==` регулируется JLS; `equals/hashCode`, `compareTo/Comparator` — контракты API, не синтаксические правила JLS. `HashMap` и `TreeMap` рассматриваются для проверки смысловых последствий, не как отдельная статья об устройстве buckets/red-black trees/resize. `wait/notify` — только направление `CON-MONITOR`, `record` — граница `CORE-RECORDS`, defensive copies и mutable state — `CORE-IMMUTABLE`. «`equals` проверяет содержимое» не является универсальным правилом: исходный `Object.equals` сравнивает identity, а содержательную эквивалентность определяет контракт конкретного типа.

## 2. Источники — версия и иерархия доверия

### A. Первичные официальные нормы Java SE 21

- **[JLS §15.21, особенно §15.21.1–3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.21)** — численное, логическое и ссылочное `==` / `!=`, допустимость сравнения ссылочных типов, compile-time ошибки. Для boxing/unboxing — [JLS §5.1.7–8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.8). Контракт `equals` здесь **не определяется**.
- **[Java 21 Object](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html):** точные контракты [`equals(Object)`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#equals(java.lang.Object)), [`hashCode()`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#hashCode()), [`getClass()`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#getClass()), [`toString()`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#toString()), [`clone()`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#clone()), [`finalize()`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#finalize()), [`wait()`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#wait()) / [`notify()`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#notify()). `finalize()` в Java 21 — deprecated for removal, не предлагать как механизм освобождения ресурсов.
- **[Java 21 Objects](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html):** [`Objects.equals(a,b)`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html#equals(java.lang.Object,java.lang.Object)), [`deepEquals(a,b)`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Objects.html#deepEquals(java.lang.Object,java.lang.Object)), `hash` / `hashCode`. Null-safe helper не делает типам новый контракт равенства.
- **[Java 21 Map](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html)**, [`HashMap`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html), [`HashSet`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashSet.html):** семантика ключей определяется эквивалентностью, изменения ключей могут приводить к **unspecified behavior**; не обещать конкретный bucket или всегда `null` при повторном поиске. Спецификация `Map.containsKey` в терминах `equals` не требует физического вызова именно `key.equals(...)`.
- **[Comparable](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Comparable.html)**, [Comparator](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html), [SortedMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SortedMap.html), [TreeMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html), [TreeSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeSet.html):** отношение порядка и consistency with equals. TreeMap/TreeSet продолжают определённо работать при несогласованности, но в таком случае нарушают общий контракт Map/Set.
- **[BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html):** `equals` учитывает числовое значение **и scale**, а `compareTo` — численное сравнение; натуральный порядок не согласован с `equals`.
- **[Arrays](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html):** `Arrays.equals` для одномерных массивов; `Arrays.deepEquals` для вложенных массивов (reference-array верхнего уровня). Сам `array.equals` по умолчанию — identity.
- **[System.identityHashCode(Object)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#identityHashCode(java.lang.Object))**, [JLS §8.1.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.4) и [§8.4.8](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8), [Record API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html) и [JLS §8.10](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10) — identity hash, наследование/overriding и граница автоматического равенства record.

**Важная точность:** последовательные вызовы `hashCode` на одном объекте должны давать одинаковый результат в пределах описанной `Object` предпосылки о неизменности используемых для `equals` данных, но сам hash **не обязан** совпадать между запусками JVM, быть уникальным или отражать адрес памяти. `equals(a,b) ⇒ hash(a)==hash(b)`, обратное неверно. Правила сортировки не требуют универсального `compareTo==0 ⇔ equals` для всех классов: это **настойчиво рекомендуемая согласованность**, нарушение которой имеет последствия для сортированных коллекций.

### B/C. Вопросники для проверки пропусков, не источники языковых гарантий

Базовые записи существуют в [SOURCE_REGISTRY](../SOURCE_REGISTRY.md). Новые записи не нужны: каждая использованная подборка найдена в реестре, первичные нормы взяты из A.

- **`Q-HRUDAY`** — [Object Class, Chapter 4](https://github.com/Hruday-Boppa/Core-Java-Interview-Questions-List/blob/main/README.md#chapter-4---object-class): 58 вопросов, в том числе default `equals`, five properties, `hashCode`, mutable key, `Object` API, byte-array keys и массивы. Вопросы проверены на уровне оглавления/списка, а не как авторитетные ответы.
- **`Q-CHERNYSHOV`** — [русский список, §10 HashMap / equals / hashCode](https://github.com/ViacheslavChernyshov/java-interview-questions-and-answers/blob/master/ru/README.md): вопросы №6–14 о контракте и изменяемых ключах, №21 о TreeMap, а также §Collections №29–30 о Comparable/Comparator.
- **`Q-TEAMLEAD`** — [java-interview-questions](https://github.com/teamlead/java-interview-questions): прямые формулировки о `Object`, `equals/hashCode`, hash collision, потере ключа в HashMap и `TreeSet` vs `HashSet`. Авторы утверждают рейтинг по 600 публичным интервью; методика/частота не проверена и **не определяет** P0/P1/P2.
- **`Q-IN28`** — [JavaInterviewQuestionsAndAnswers, Q29–31](https://github.com/in28minutes/JavaInterviewQuestionsAndAnswers/blob/master/readme.md): `equals`, реализация `equals`, `hashCode`; Q142–143 Comparable/Comparator и Q152/Q164 — TreeSet/TreeMap.
- **`Q-BAELDUNG-COL`** — [Java Collections Interview Questions](https://www.baeldung.com/java-collections-interview-questions): отдельная подсказка про ключи HashMap/TreeMap; отдельные механические утверждения о buckets и tree bins требуют проверки под конкретную реализацию/версию, не становятся API-гарантией.

**Ограничения поиска:** не проверены статистика частот интервью, текущая полнота всех C-источников и независимость их происхождения. Матрица P0 отражает важность для обещанного читателю понимания, а не уровень Middle/Senior. Последующий аудит готовых статей обязан использовать ещё один независимый набор вопросов и сверить фактические объяснения, а не только существование файлов.

## 3. Матрица проверяемых вопросов P0/P1/P2

Указанные названия разделов — **будущая композиция статьи 02**, не результат G1. В колонке «проба» `E01–E14` — наблюдения одного запускаемого позитивного файла из §4; `N01–N05` — самостоятельные ожидаемые ошибки компиляции. Для каждого P0 — проверяемый положительный исход и/или отрицательный контрпример.

| ID / приоритет | Вопрос → ожидаемая проверяемая мысль | Нормативный источник | Проба и место |
|---|---|---|---|
| OE-01 **P0** | Чем identity объекта отличается от value equality? `==` на ссылках проверяет одну и ту же ссылочную сущность, исходный `Object.equals` — identity; тип может override `equals`. | JLS §15.21.3, Object.equals | E01, E14; `CORE-OBJECT` вводит, `CORE-EQUALITY` раскрывает |
| OE-02 **P0** | Что `==` сравнивает у примитивов, ссылок, boxed vs primitive; когда выражение вообще не компилируется? Типы операндов принципиальны, возможен unboxing; `String==Integer` — compile error. | JLS §15.21.1–3, §5.1.8 | E01, N01/N04/N05; `CORE-EQUALITY`, исходные conversions краткой ссылкой |
| OE-03 **P0** | Каковы пять свойств `equals` для non-null: reflexive, symmetric, transitive, consistent, `x.equals(null)==false`? Это эквивалентность, которую должен сохранять override. | Object.equals API 21 | E03/E05/E06, N02; `CORE-EQUALITY` |
| OE-04 **P0** | Как связаны `equals` и `hashCode`? Равные → одинаковый hash; одинаковый hash не доказывает равенство. «Переопределить только equals» обычно нарушает контракт. | Object.hashCode/equals API 21 | E03/E04/E07; `CORE-EQUALITY` |
| OE-05 **P0** | Почему `HashMap.get(new Key(...))` может не найти равный ключ при неверном hash? Разное значение hash у равных ключей — нарушение контракта; исход зависит от реализации, в явном случае E04 воспроизведён. | Map/HashMap, Object.hashCode | E04; `CORE-EQUALITY`; internals → COL-HASH |
| OE-06 **P0** | Что сломается при расширении `equals` в подклассе? `Parent.equals(Child)` и `Child.equals(Parent)` могут расходиться, а «близкие значения» могут разрушить транзитивность. | Object.equals, JLS §8.4.8 | E05/E06; `CORE-EQUALITY`, inheritance prerequisite |
| OE-07 **P0** | Как писать null-safe comparison? `Objects.equals(a,b)` использует `a.equals(b)` при non-null `a`; вызов `null.equals` нельзя делать. | Objects.equals API 21 | E02; `CORE-EQUALITY` |
| OE-08 **P0** | Почему равные hash не запрещают два ключа в HashSet? Hash collisions допустимы, дальше используется эквивалентность по ключу; не смешивать hash и уникальную identity. | Object.hashCode, Set/HashSet API | E07; `CORE-EQUALITY`; COL-HASH глубже |
| OE-09 **P0** | Почему изменяемый ключ опасен для HashMap? Изменение состояния, влияющего на равенство/hash, оставляет отображение с неопределённым с точки зрения Map контрактом поведением; конкретная потеря поиска — пример JDK 21, не универсальная гарантия. | Map API 21 mutable key note | E08; `CORE-EQUALITY`; CORE-IMMUTABLE |
| OE-10 **P0** | Чем `compareTo/Comparator` отличаются от `equals` и что означает «consistent with equals»? Sorted collections определяют равенство ключей через результат сравнения, не `equals`; при несовпадении нарушается общий Map/Set контракт. | Comparable, Comparator, SortedMap/TreeSet | E10/E11; `CORE-EQUALITY`; COL-SORTED |
| OE-11 **P0** | Почему `new BigDecimal("2.0")` и `("2.00")` не равны через `equals`, но равны по `compareTo`? Scale входит в `equals`; `HashSet` и `TreeSet` покажут разное число элементов. | BigDecimal, TreeSet API 21 | E09/E10; `CORE-EQUALITY` |
| OE-12 **P0** | Что ожидается от объектного контракта в value class? Поля, определяющие equality, согласованно участвуют в hash; только `equals(SomeType)` без override `equals(Object)` — перегрузка (¦N02¦). | Object.equals/hashCode, JLS §8.4.8 | E03, N02/N03; «Корректный базовый value object» |
| OE-13 **P1** | Какие базовые методы даёт `Object` и какие нельзя override? `getClass()` и мониторные методы final; `equals/hashCode/toString` можно override. | Object API | E14, обзор методов `CORE-OBJECT` |
| OE-14 **P1** | `getClass()` vs `instanceof` внутри `equals`: абсолютный запрет на сравнение с подклассами vs полиморфная проверка; универсального рецепта без семантического решения нет. | Object.equals/getClass, JLS §15.20.2 | E05, «Как поддержать симметрию»; CORE-INHERITANCE |
| OE-15 **P1** | Почему `toString()` — диагностическое представление, а не стабильный идентификатор? Формат у базового Object включает hash; не обещать постоянство между запусками или безопасность вывода чувствительных полей. | Object.toString API 21 | Нормативный разбор; при необходимости P1 mini-code |
| OE-16 **P1** | Чем `System.identityHashCode` отличается от переопределённого hashCode? Возвращает значение как у исходной Object-реализации; не уникальный ID и не адрес. | System.identityHashCode API 21 | E14 проверяет null → 0; не проверять конкретное число для объекта |
| OE-17 **P1** | Почему массивы со знакомыми элементами часто «не равны» через `Object.equals`? `Arrays.equals` сравнивает элементы, `Arrays.deepEquals` — вложенные массивы; `Objects.deepEquals` для массивов — отдельная граница. | Arrays API, Objects.deepEquals | E12; CORE-ARRAYS / CORE-EQUALITY |
| OE-18 **P1** | Как `compareTo` и Comparator задают порядок, в том числе signum и transitivity, и что сломает несправедливый comparator? Проверять контракт Comparator отдельно от equals. | Comparable/Comparator API | E11; глубже COL-SORTED |
| OE-19 **P1** | Зачем `Object.wait/notify/notifyAll` и почему нельзя вызвать произвольно без владения монитором? `IllegalMonitorStateException`; цикл ожидания, interruption и HB — CON-MONITOR, не CORE-OBJECT. | Object.wait/notify API | Только понятное назначение/ссылка на будущую CON-MONITOR |
| OE-20 **P1** | Что реально делает `clone` и почему `finalize` не годится для управления ресурсами? `clone` protected, `Cloneable` marker/shallow-copy conventions; `finalize` deprecated for removal в Java 21. | Object.clone/finalize API | Обзор/опасности в `CORE-OBJECT`, без самодельного lifecycle demo |
| OE-21 **P2** | Чем `Double.equals` отличается от `==` для NaN/±0.0; как это влияет на хеш/сортировку? | Double API, JLS §15.21 | Отложить в CORE-NUMBERS; не смешивать с identity |
| OE-22 **P2** | Что именно генерирует record для `equals/hashCode`? Компоненты массивов не становятся автоматически deep-equal; record shallow и зависит от типов компонентов. | Record API; JLS §8.10 | E13 как простой пример; полноценное CORE-RECORDS |
| OE-23 **P2** | Что изменится в Map/Set при использовании identity-based `IdentityHashMap` или byte[] key? Это специальная семантика/контракт API, не обычный HashMap. | IdentityHashMap, Map/Arrays | COL-HASH / CORE-ARRAYS |
| OE-24 **P2** | Как stable equality относится к mutable collections, кэшам и concurrent publication? Меняющееся поле и race — разные проблемы; JMM нельзя «доказать» equals-примером. | Map API, JLS §17 | CORE-IMMUTABLE / CON-JMM |
| OE-25 **P2** | Как framework proxy/ORM меняет допустимые choices `getClass`/`instanceof` и DB identity? Зависит от версии библиотеки, не гарантируется JLS. | Версия Hibernate/Spring — будущая проверка | DB-JPA / SPR-DI, не заявлять покрытым |
| OE-26 **P2** | Почему `Object.hashCode` / `toString` не являются гарантированными memory address, стабильным persistent ID или криптографическим хешем? | Object/System API 21 | CORE-OBJECT краткая граница, JVM details отдельно |

## 4. Контрпримеры и собственный протокол проверки Java 21

### 4.1. Положительная локальная проверка: один изолированный файл

**Исходник для воспроизведения:** сохранить в `EqualityProbes.java` и выполнить команды ниже. Этот файл существует **в исследовательском блоке плана**, не в исходниках проекта; его не надо включать в Gradle/CI как готовое упражнение. Печать по меткам E01–E14 позволяет однозначно сопоставить результаты с матрицей. Исходник проверен на OpenJDK 21.0.12.1.

```java
import java.math.BigDecimal;
import java.util.*;

public class EqualityProbes {
  static final class Value {
    final int id;
    Value(int id) { this.id = id; }
    @Override public boolean equals(Object x) {
      return x instanceof Value v && id == v.id;
    }
    @Override public int hashCode() { return Integer.hashCode(id); }
  }
  static final class BadHash {
    final int id, hash;
    BadHash(int id, int hash) { this.id=id; this.hash=hash; }
    @Override public boolean equals(Object x) {
      return x instanceof BadHash b && id==b.id;
    }
    @Override public int hashCode() { return hash; }
  }
  static class Parent {
    final int id;
    Parent(int id) { this.id=id; }
    @Override public boolean equals(Object x) {
      return x instanceof Parent p && id==p.id;
    }
    @Override public int hashCode() { return id; }
  }
  static final class Child extends Parent {
    Child(int id) { super(id); }
    @Override public boolean equals(Object x) {
      return x instanceof Child c && super.equals(c);
    }
  }
  static final class Near {
    final int n;
    Near(int n) { this.n=n; }
    @Override public boolean equals(Object x) {
      return x instanceof Near z && Math.abs(n-z.n)<=1;
    }
    @Override public int hashCode() { return 0; }
  }
  static final class MutableKey {
    int id;
    MutableKey(int id) {this.id=id;}
    @Override public boolean equals(Object x) {
      return x instanceof MutableKey k && id==k.id;
    }
    @Override public int hashCode() {return Integer.hashCode(id);}
  }
  static final class Collision {
    final int id;
    Collision(int id) {this.id=id;}
    @Override public boolean equals(Object x){
      return x instanceof Collision c && id==c.id;
    }
    @Override public int hashCode(){return 67;}
  }
  record Pair(int id, String name) { }

  public static void main(String[] args) {
    String a = new String("67"), b = new String("67");
    System.out.println("E01=" + (a==b) + "," + a.equals(b));
    System.out.println("E02=" + Objects.equals(null,null) + "," + Objects.equals(null,a) + "," + Objects.equals(a,b));
    Value v1=new Value(67), v2=new Value(67);
    System.out.println("E03=" + v1.equals(v2)+","+(v1.hashCode()==v2.hashCode()));
    BadHash h1=new BadHash(67,1), h2=new BadHash(67,2);
    Map<BadHash,String> broken=new HashMap<>();broken.put(h1,"stored");
    System.out.println("E04="+h1.equals(h2)+","+(h1.hashCode()==h2.hashCode())+","+broken.get(h2));
    Parent p=new Parent(67);Child c=new Child(67);
    System.out.println("E05="+p.equals(c)+","+c.equals(p));
    Near x=new Near(67),y=new Near(68),z=new Near(69);
    System.out.println("E06="+x.equals(y)+","+y.equals(z)+","+x.equals(z));
    Collision col1=new Collision(1),col2=new Collision(2);
    Set<Collision> cs=new HashSet<>();cs.add(col1);cs.add(col2);
    System.out.println("E07="+(col1.hashCode()==col2.hashCode())+","+col1.equals(col2)+","+cs.size());
    MutableKey key=new MutableKey(67);Map<MutableKey,String> map=new HashMap<>();map.put(key,"ok");
    boolean before=map.containsKey(key);key.id=68;boolean after=map.containsKey(key);
    System.out.println("E08="+before+","+after+","+map.size());
    BigDecimal d1=new BigDecimal("2.0"),d2=new BigDecimal("2.00");
    System.out.println("E09="+d1.equals(d2)+","+(d1.compareTo(d2)==0)+","+Set.of(d1,d2).size());
    System.out.println("E10="+new HashSet<>(List.of(d1,d2)).size()+","+new TreeSet<>(List.of(d1,d2)).size());
    Set<String> sorted=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);sorted.add("a");sorted.add("A");
    System.out.println("E11="+"a".equals("A")+","+sorted.size());
    int[] ar1={67},ar2={67};int[][] nested1={{67}}, nested2={{67}};
    System.out.println("E12="+ar1.equals(ar2)+","+Arrays.equals(ar1,ar2)+","+Arrays.equals(nested1,nested2)+","+Arrays.deepEquals(nested1,nested2));
    Pair pair1=new Pair(67,"X"),pair2=new Pair(67,"X");
    System.out.println("E13="+pair1.equals(pair2)+","+(pair1.hashCode()==pair2.hashCode()));
    Object obj=new Object();
    System.out.println("E14="+obj.equals(obj)+","+(obj.getClass()==Object.class)+","+(System.identityHashCode(null)==0));
  }
}
```

**Команды, реально запущенные в изолированном временном каталоге:** `javac --release 21 -d out EqualityProbes.java` и `java -cp out EqualityProbes`. При самостоятельном повторении создать каталог `out` или разрешить `javac -d` создать нужные каталоги. Результат сверялся точным сравнением со следующими четырнадцатью строками:

```text
E01=false,true
E02=true,false,true
E03=true,true
E04=true,false,null
E05=true,false
E06=true,true,false
E07=true,false,2
E08=true,false,1
E09=false,true,2
E10=2,1
E11=false,1
E12=false,true,false,true
E13=true,true
E14=true,true,true
```

**Результат исследования:** 14/14 ожидаемых строк — PASS на OpenJDK 21.0.12.1. Примеры E04 (неверный hash) и E08 (изменённый ключ) показывают конкретный эффект этой реализации; они **не являются гарантией `null` для каждого нарушающего контракт ключа на любой Map/JDK**. E13 демонстрирует обычный record со стабильными scalar components, но не означает deep equality для массивов в record.

### 4.2. Отрицательные компиляционные случаи

Каждый отдельный файл компилировать **отдельно**: `javac --release 21 -d out-neg N01_IncomparableReferences.java` и так далее. Все пять файлов отдельно проверены: `javac` вернул ошибку ожидаемого класса (5/5). Точный русский/английский текст диагностики — свойство компилятора, не нормативный текст JLS.

**N01 — unrelated reference types, compile error (JLS §15.21.3):**

```java
class N01_IncomparableReferences {
  void test() {
    String s = "67";
    Integer n = 67;
    boolean same = (s == n); // incomparable types
  }
}
```

**N02 — `equals(T)` не override `equals(Object)`:**

```java
class N02_OverloadIsNotOverride {
  @Override public boolean equals(N02_OverloadIsNotOverride other) {
    return true;
  } // method does not override or implement
}
```

**N03 — нельзя сузить `public` при override `Object.equals`:**

```java
class N03_WeakerAccess {
  @Override boolean equals(Object other) { return true; }
} // attempting to assign weaker access privileges
```

**N04 — primitive `int` нельзя dereference:**

```java
class N04_PrimitiveMethod {
  boolean test() { int n = 67; return n.equals(67); }
} // int cannot be dereferenced
```

**N05 — несовместимое `int == boolean`:**

```java
class N05_NumericVsBoolean {
  boolean test() { return 67 == true; }
} // incomparable types
```

**Не выдавать ошибки проектирования за ошибки компиляции:** неверный `equals/hashCode`, асимметрия `equals`, использование изменяемого ключа и несогласованный `Comparator` в большинстве случаев **компилируются**. Это смысловые/контрактные ошибки, а не диагноз `javac`. В статье 02 для них нужен объяснённый stdout/эффект.

### 4.3. Отчёт о выполнении и непроверенном

- **Локально выполнено:** `java -version`/`javac -version` → OpenJDK 21.0.12.1; позитивный многофрагментный `EqualityProbes.java` (14 проверенных строк) и пять отдельно скомпилированных отрицательных исходников (5 ожидаемых отказов).
- **Воспроизводимость:** полный позитивный исходник, точный stdout и пять негативных исходников воспроизведены в §4; сторонние временные пути без значения не требуются. Изолированная проверка не выполняет Gradle/JUnit.
- **Не запускалось:** Gradle/JUnit/Mockito в репозитории по CORE-OBJECT/EQUALITY, Obsidian/IntelliJ preview, независимое review 04, property-based проверка всех аксиом `equals`, тесты любого JVM кроме указанной OpenJDK 21.0.12.1, сценарии concurrency и JPA/proxies. CI нового исследовательского PR проверить **после его создания**; существующий зелёный CI не проверяет автоматически эти пробы.
- **Не утверждать:** что нарушения hash-контракта всегда приводят к одному конкретному результату, что hash равен memory address, что любая `equals` реализует value semantics, что `TreeMap` использует `hashCode` либо что сравнение `String` одинаково для `==` и `equals`.

## 5. Существующие зависимости и честные будущие границы

**Реально существующие файлы на зафиксированном BASE `main`:**

- [CORE-TYPES](../../knowledge/01-java-core/types.md) — примитивы/ссылки, присваивание и identity; в новых статьях не повторять всю модель типов.
- [CORE-CONVERSIONS](../../knowledge/01-java-core/conversions.md) — boxing/unboxing, `Integer == int`, строковое interning; новая статья добавляет `equals/hashCode` и contractual collections, не пересказывает conversions.
- [CORE-CLASSES](../../knowledge/01-java-core/classes.md) — `Object` как superclass и class members; подробный constructor lifecycle — вне scope.
- [CORE-INHERITANCE](../../knowledge/01-java-core/inheritance.md) — dispatch и overriding; в Equality нужно только обоснование симметрии/транзитивности при наследовании.
- [CORE-INIT](../../knowledge/01-java-core/initialization.md) — class/instance initialization, не нужна для равенства, кроме напоминания о mutability во время construction.
- [CURRICULUM](../CURRICULUM.md), [COVERAGE](../COVERAGE.md), [SOURCE_REGISTRY](../SOURCE_REGISTRY.md), [JAVA_CORE_PLAN](../JAVA_CORE_PLAN.md) — факт наличия, планирования, версионности и последующих связей.

**Пока отсутствующие отдельные статьи — только текстовые ID без фиктивных ссылок:** `CORE-RECORDS`, `CORE-IMMUTABLE`, `CORE-ARRAYS`, `COL-HASH`, `COL-SORTED`, `CON-MONITOR`, `CON-JMM`, `CORE-NUMBERS`, `DB-JPA`, `SPR-DI`. Отсутствие каждого не компенсируется декларацией о его готовности в этом плане. Особенно важны: `COL-HASH` для устройства HashMap, `COL-SORTED` для структур TreeMap/TreeSet, `CORE-IMMUTABLE` для дизайна стабильных ключей, `CON-MONITOR` для wait/notify. Если к моменту написания эти статьи появились в main, проверить наличие и заменить текстовые ID настоящими ссылками.

## 6. Передача ролям и критерии G1/последующих этапов

- **00:** подтвердить или пересмотреть рекомендацию **две статьи / один план**, 12 P0, 8 P1, 6 P2 и границы между ними. Если 00 считает одну большую статью предпочтительнее — указать, каким образом сохраняются оба curricular ID и отдельные обещания читателю. Только явное решение 00 после проверки означает G1 accepted. До него не открывать содержательное G2.
- **02 (только после принятия G1):** написать собственное объяснение каждого OE-01–OE-12, отдельно обозначить contract errors vs compile errors; не использовать `Equals` как «автоматически сравнить все поля». Для `CORE-OBJECT` не дублировать матрицу hash/sorted keys; для `CORE-EQUALITY` сделать контрпример потерянного ключа и объяснить ограничения гарантий. P1 раскрыть там, где это важно читателю; P2 — короткий точный хвост.
- **03 (только после принятия G1):** один многофайловый runnable example может оправдаться при показе правильного/неправильного value key и двух реализаций Map; однако не создавать отдельное упражнение/Gradle module «ради количества». Если используются негативные `javac` фрагменты, держать их отдельно от success source set. Подробное наполнение согласовать с 02; исходник §4 уже обеспечивает минимальную проверяемость для G1.
- **04:** независимо проверить нормативность контракта, особенно `Object.hashCode`, пять свойств `equals`, `Map` mutable-key unspecified, `TreeSet` consistency, `BigDecimal` scale, относительность `getClass` vs `instanceof`, E01–E14 и N01–N05. Если будет правка HEAD после проверки — нужен новый вердикт по затронутому участку.
- **05:** после G2 проверить естественный русский текст, недублирующее разделение статей, точные локальные ссылки и фактическое отображение в Obsidian/IDE; не выдавать текстовую ссылку на будущий ID за существующий файл.
- **06:** после merge и решения 00 сверить настоящие статьи, навигацию и статус `COVERAGE`, не поднимать «implemented/verified» после принятия **только** исследовательского плана.

**Рекомендация роли 01:** G1-план готов к проверке, не принят. Вопросы 02/03 до решения 00 не разблокированы; `COVERAGE.md` и программа курса этим PR не меняются.