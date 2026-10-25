package campus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ResourceManagerTest {
  private static final LocalDateTime START = LocalDateTime.of(2030, 1, 1, 10, 0);

  private ResourceManager manager(Student student, Room room) {
    University university = new University("Test");
    university.addResource(room);
    ResourceManager manager = new ResourceManager(university);
    manager.registerUser(student);
    manager.registerUser(new Student("S2", "Second Student"));
    manager.registerUser(new Admin("A1", "Administrator"));
    return manager;
  }

  @Test
  void permissionRulesAreEnforced() throws Exception {
    Room room = new Room("R1", "Room", 4);
    ResourceManager manager = manager(new Student("S1", "Student"), room);
    University university = new University("Test");
    university.addResource(new Lab("L1", "Lab", 4, "Computers"));
    ResourceManager books = new ResourceManager(university);
    Student student = new Student("S1", "Student");
    books.registerUser(student);
    assertThrows(InvalidUserException.class, () -> books.bookResource(student, university.getResources().get(0)));
    assertTrue(manager.getActivityLogger().entries().isEmpty());
  }

  @Test
  void bookingLimitAndDoubleBookingAreEnforced() throws Exception {
    Room room = new Room("R1", "Room", 4);
    ResourceManager manager = manager(new Student("S1", "Student"), room);
    User student = manager.findUser("S1");
    manager.bookResource(student, room, START, START.plusHours(1));
    assertThrows(RoomNotAvailableException.class,
        () -> manager.bookResource(manager.findUser("S2"), room, START.plusMinutes(30), START.plusHours(2)));
    University university = new University("Test");
    Room first = new Room("R1", "First", 4);
    Room second = new Room("R2", "Second", 4);
    Room third = new Room("R3", "Third", 4);
    university.addResource(first); university.addResource(second); university.addResource(third);
    manager = new ResourceManager(university);
    manager.registerUser(new Student("S1", "Student"));
    User registeredStudent = manager.findUser("S1");
    manager.bookResource(registeredStudent, first, START, START.plusHours(1));
    manager.bookResource(registeredStudent, second, START, START.plusHours(1));
    ResourceManager limitedManager = manager;
    assertThrows(InvalidUserException.class,
        () -> limitedManager.bookResource(registeredStudent, third, START, START.plusHours(1)));
  }

  @Test
  void onlyOwnerOrAdminCanCancel() throws Exception {
    Room room = new Room("R1", "Room", 4);
    ResourceManager manager = manager(new Student("S1", "Student"), room);
    manager.bookResource(manager.findUser("S1"), room, START, START.plusHours(1));
    assertThrows(InvalidUserException.class, () -> manager.cancelBooking(manager.findUser("S2"), "B1"));
    manager.cancelBooking(manager.findUser("A1"), "B1");
    assertTrue(manager.getBookings().get(0).isActive() == false);
  }

  @Test
  void loadedBookingPreservesPersistenceState() throws Exception {
    Room room = new Room("R1", "Room", 4);
    ResourceManager manager = manager(new Student("S1", "Student"), room);
    Booking saved = new Booking("B42", manager.findUser("S1"), room, true, START, START, START.plusHours(1), null);
    manager.addLoadedBooking(saved);
    assertEquals("B42", manager.getBookings().get(0).getId());
    assertTrue(!room.isAvailable());
  }
}
