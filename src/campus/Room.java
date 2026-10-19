package campus;

// A Room inherits Resource features and fulfills the Bookable contract.
public class Room extends Resource implements Bookable {
  private final int capacity;
  private boolean available = true;

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
  public boolean isAvailable() {
    return available;
  }

  @Override
  public void setAvailable(boolean available) {
    this.available = available;
  }

  @Override
  public String toString() {
    return super.toString() + " | capacity=" + capacity + " | " + (available ? "available" : "booked");
  }
}
