package campus;

// Custom checked exception for invalid or unauthorized users.
public class InvalidUserException extends Exception {
  private static final long serialVersionUID = 1L;

  public InvalidUserException(String message) {
    super(message);
  }
}
