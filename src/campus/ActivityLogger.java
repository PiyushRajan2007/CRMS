package campus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Thread-safe in-memory activity log; persistence is handled by StorageService. */
public class ActivityLogger {
  private final List<String> entries = new ArrayList<>();

  public synchronized void log(String action, String detail) {
    entries.add(LocalDateTime.now() + " | " + action + " | " + detail);
  }

  public synchronized List<String> entries() {
    return List.copyOf(entries);
  }

  public synchronized void load(List<String> saved) {
    entries.clear();
    entries.addAll(saved);
  }
}
