package campus;

// Interface-based abstraction exposes searchable behaviour without implementation details.
public interface Searchable {
  boolean matches(String query);
}
