# JVM Lab

Лаборатория по устройству JVM и поведению Java-приложений во время выполнения.

## Темы

1. Runtime data areas: heap, stacks, metaspace и program counter.
2. Создание объектов и ссылки.
3. Class loading, linking и initialization.
4. ClassLoader и области видимости классов.
5. JIT compilation и оптимизации.
6. Garbage Collection и компромиссы throughput/latency.
7. Memory leaks и OutOfMemoryError.
8. Диагностика: heap dump, thread dump и базовые инструменты JVM.

## Принцип

Отделять требования спецификаций от особенностей конкретной JVM. Если пример зависит от HotSpot или настроек GC, это должно быть явно указано.
