package campus;

public class Faculty extends User {
  public Faculty(String id, String name) {
    super(id, name);
  }

  @Override
  public void showPermissions() {
    System.out.println("Faculty: rooms, labs, and books; limit 5 active bookings.");
  }

  @Override
  public int getBookingLimit() {
    return 5;
  }

  @Override
  public boolean canBook(Resource resource) {
    return resource instanceof Room || resource instanceof Book;
  }
}
