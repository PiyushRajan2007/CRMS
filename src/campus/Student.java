package campus;

public class Student extends User {
  public Student(String id, String name) {
    super(id, name);
  }

  @Override
  public void showPermissions() {
    System.out.println("Student: rooms and books; limit 2 active bookings.");
  }

  @Override
  public int getBookingLimit() {
    return 2;
  }

  @Override
  public boolean canBook(Resource resource) {
    return resource instanceof Room && !(resource instanceof Lab) || resource instanceof Book;
  }
}
