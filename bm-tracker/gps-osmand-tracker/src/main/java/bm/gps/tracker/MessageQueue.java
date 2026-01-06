package bm.gps.tracker;

import bm.gps.MessageOsmand;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Component;

@Component
public class MessageQueue {
  private final Queue<MessageOsmand> queue = new ConcurrentLinkedQueue<>();

  public void enqueue(MessageOsmand msg) {
    if (msg != null) {
      queue.add(msg);
    }
  }

  public MessageOsmand poll() {
    return queue.poll();
  }

  public boolean isEmpty() {
    return queue.isEmpty();
  }

  public int size() {
    return queue.size();
  }
}
