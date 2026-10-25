package campus;

// A Room inherits Resource features and fulfills the Bookable contract.
public class Room extends BookableResource {
  private final int capacity;

  public Room(String id, String name, int capacity) {
    super(id, name, "Room");
    if (capacity <= 0)
      throw new IllegalArgumentException("Capacity must be positive.");
    this.capacity = capacity;
  }

  public int getCapacity() {
    return capacity;
  }

  @Override
  public String toString() {
    return super.toString() + " | capacity=" + capacity + " | " + (isAvailable() ? "available" : "booked");
  }
}
