package campus;

/** Shared availability state for every resource that can be reserved. */
public abstract class BookableResource extends Resource implements Bookable {
  private boolean available = true;

  protected BookableResource(String id, String name, String category) {
    super(id, name, category);
  }

  @Override
  public synchronized boolean isAvailable() {
    return available;
  }

  @Override
  public synchronized void setAvailable(boolean available) {
    this.available = available;
  }
}
