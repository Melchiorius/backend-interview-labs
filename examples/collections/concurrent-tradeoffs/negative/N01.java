// Theory: knowledge/02-collections/concurrent.md
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
class N01 {
    void probe() throws InterruptedException {
        Queue<Integer> q = new ArrayBlockingQueue<>(1);
        q.put(67);
    }
}
