package bm.tracker.gpstracker.queue;

import bm.tracker.gpstracker.model.GpsMessage;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Component;

@Component
public class MessageQueue {
  private final Queue<GpsMessage> queue = new ConcurrentLinkedQueue<>();

  public void enqueue(GpsMessage msg) {
    if (msg != null) {
      queue.add(msg);
    }
  }

  public GpsMessage poll() {
    return queue.poll();
  }

  public boolean isEmpty() {
    return queue.isEmpty();
  }

  public int size() {
    return queue.size();
  }
}
