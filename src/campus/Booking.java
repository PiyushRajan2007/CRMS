package campus;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

// Encapsulation keeps booking state private; Details is a non-static inner class.
public class Booking {
  private static int nextId = 1;
  private final String id;
  private final User user;
  private final Resource resource;
  private final LocalDateTime createdAt;
  private final LocalDateTime startAt;
  private final LocalDateTime endAt;
  private final LocalDateTime dueAt;
  private boolean active = true;

  public Booking(User user, Resource resource) {
    this(user, resource, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
  }

  public Booking(User user, Resource resource, LocalDateTime startAt, LocalDateTime endAt) {
    this.id = "B" + nextId++;
    this.user = user;
    this.resource = resource;
    this.createdAt = LocalDateTime.now();
    if (startAt == null || endAt == null || !endAt.isAfter(startAt))
      throw new IllegalArgumentException("Booking end time must be after its start time.");
    this.startAt = startAt;
    this.endAt = endAt;
    this.dueAt = resource instanceof Book ? startAt.plusDays(7) : null;
  }

  public Booking(String id, User user, Resource resource, boolean active, LocalDateTime createdAt) {
    this(id, user, resource, active, createdAt, null, null, resource instanceof Book ? createdAt.plusDays(7) : null);
  }

  public Booking(String id, User user, Resource resource, boolean active, LocalDateTime createdAt,
      LocalDateTime startAt, LocalDateTime endAt, LocalDateTime dueAt) {
    this.id = id;
    this.user = user;
    this.resource = resource;
    this.active = active;
    this.createdAt = createdAt;
    this.startAt = startAt;
    this.endAt = endAt;
    this.dueAt = dueAt;
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

  public LocalDateTime getStartAt() { return startAt; }
  public LocalDateTime getEndAt() { return endAt; }
  public LocalDateTime getDueAt() { return dueAt; }

  public boolean overlaps(LocalDateTime start, LocalDateTime end) {
    if (startAt == null || endAt == null)
      return true;
    return start.isBefore(endAt) && end.isAfter(startAt);
  }

  public long getFine() {
    if (!(resource instanceof Book) || dueAt == null || !LocalDateTime.now().isAfter(dueAt))
      return 0;
    long overdueDays = Math.max(1, Duration.between(dueAt, LocalDateTime.now()).toDays());
    long rate = switch (user.getRole()) {
      case STUDENT -> 5;
      case FACULTY -> 2;
      case ADMIN -> 0;
    };
    return overdueDays * rate;
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
