// Theory: knowledge/02-collections/concurrent.md
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
class N02 {
    int probe() {
        BlockingQueue<Integer> q = new ArrayBlockingQueue<>(1);
        return q.take();
    }
}
