package campus;

// Inheritance and runtime polymorphism: Admin supplies its own User behaviour.
public class Admin extends User {
  public Admin(String id, String name) {
    super(id, name);
  }

  @Override
  public void showPermissions() {
    System.out.println("Admin: full campus resource access; limit 20 active bookings.");
  }

  @Override
  public int getBookingLimit() {
    return 20;
  }

  @Override
  public boolean canBook(Resource resource) {
    return true;
  }
}
