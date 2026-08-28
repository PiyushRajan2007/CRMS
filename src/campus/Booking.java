package campus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Booking {
  private static int nextId = 1;
  private final String id;
  private final User user;
  private final Resource resource;
  private final LocalDateTime createdAt;
  private boolean active = true;

  public Booking(User user, Resource resource) {
    this.id = "B" + nextId++;
    this.user = user;
    this.resource = resource;
    this.createdAt = LocalDateTime.now();
  }

  public Booking(String id, User user, Resource resource, boolean active, LocalDateTime createdAt) {
    this.id = id;
    this.user = user;
    this.resource = resource;
    this.active = active;
    this.createdAt = createdAt;
    updateNextId(id);
  }

  private static void updateNextId(String id) {
    try {
      nextId = Math.max(nextId, Integer.parseInt(id.substring(1)) + 1);
    } catch (RuntimeException ignored) {
      // Invalid persisted IDs are rejected by StorageService.
    }
  }

  public String getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public Resource getResource() {
    return resource;
  }

  public boolean isActive() {
    return active;
  }

  public void close() {
    active = false;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public Details details() {
    return new Details();
  }

  public class Details {
    public String summary() {
      return id + " | " + user.getName() + " -> " + resource.getName() + " | "
          + createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
  }

  @Override
  public String toString() {
    return details().summary() + (active ? " | active" : " | cancelled");
  }
}
