package campus;

// Abstraction provides a shared resource base; private fields protect its state.
public abstract class Resource implements Searchable {
  private final String id;
  private final String name;
  private final String category;

  protected Resource(String id, String name, String category) {
    if (id == null || id.isBlank() || name == null || name.isBlank())
      throw new IllegalArgumentException("Resource ID and name cannot be empty.");
    this.id = id;
    this.name = name;
    this.category = category;
  }

  public final String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  @Override
  public boolean matches(String query) {
    if (query == null)
      return false;
    String value = query.toLowerCase();
    return id.toLowerCase().contains(value) || name.toLowerCase().contains(value)
        || category.toLowerCase().contains(value);
  }

  @Override
  public String toString() {
    return id + " - " + name + " [" + category + "]";
  }
}
