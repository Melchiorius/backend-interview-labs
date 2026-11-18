# Concurrency Lab

Лаборатория по многопоточности и конкурентному программированию на Java.

## Темы

1. Thread, Runnable и жизненный цикл потока.
2. Race condition и data race.
3. Java Memory Model: visibility, ordering и happens-before.
4. synchronized, monitor и reentrancy.
5. volatile и его границы.
6. Lock, ReentrantLock и Condition.
7. Atomic classes, CAS и ABA.
8. Deadlock, livelock и starvation.
9. ExecutorService, ThreadPoolExecutor и rejection policies.
10. ConcurrentHashMap и конкурентные операции.
11. Interrupt, cancellation и graceful shutdown.

## Требования к экспериментам

Многопоточный тест не должен полагаться на случайный удачный порядок выполнения. Для каждого эксперимента описывать:
- какие потоки участвуют;
- какие данные разделяются;
- какие действия конфликтуют;
- какие исходы допустимы;
- какое свойство проверяется;
- почему тест действительно проверяет это свойство.

Для тестирования concurrency использовать подходящие синхронизаторы и инструменты. Не считать один успешный прогон доказательством отсутствия гонки.
