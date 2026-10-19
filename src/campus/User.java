package campus;

// Abstract base class: common user data is encapsulated, while roles supply behaviour.
public abstract class User {
  private final String id;
  private String name;

  protected User(String id, String name) {
    if (id == null || id.isBlank() || name == null || name.isBlank())
      throw new IllegalArgumentException("User ID and name cannot be empty.");
    this.id = id;
    this.name = name;
  }

  public final String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public abstract void showPermissions();

  public abstract int getBookingLimit();

  public abstract boolean canBook(Resource resource);

  @Override
  public String toString() {
    return id + " - " + name + " (" + getClass().getSimpleName() + ")";
  }
}
