package edu.labs.semaphore;

// Theory: knowledge/03-concurrency/semaphore.md
// Model: external work guarded by a Semaphore.
@FunctionalInterface
public interface Gateway {
    String execute() throws Exception;
}
