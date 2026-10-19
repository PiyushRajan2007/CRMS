package campus;

// Abstraction: this interface defines the common booking contract.
public interface Bookable {
  boolean isAvailable();

  void setAvailable(boolean available);
}
