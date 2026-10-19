package campus;

// Custom checked exception for a booking lookup failure.
public class BookingNotFoundException extends Exception {
  private static final long serialVersionUID = 1L;

  public BookingNotFoundException(String message) {
    super(message);
  }
}
