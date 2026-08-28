package campus;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {
  private final Scanner scanner = new Scanner(System.in);
  private final University university;
  private final ResourceManager manager;
  private final StorageService storage = new StorageService();

  public Main() {
    University loadedUniversity = new University("Northbridge University");
    ResourceManager loadedManager = new ResourceManager(loadedUniversity);
    if (storage.hasData()) {
      try {
        storage.load(loadedManager, loadedUniversity);
      } catch (IOException | IllegalArgumentException exception) {
        System.out.println("Saved data could not be loaded. Starting with defaults.");
        loadedUniversity = new University("Northbridge University");
        loadedManager = new ResourceManager(loadedUniversity);
        addDefaults(loadedUniversity, loadedManager);
      }
    } else {
      addDefaults(loadedUniversity, loadedManager);
    }
    university = loadedUniversity;
    manager = loadedManager;
  }

  private void addDefaults(University campus, ResourceManager resourceManager) {
    campus.addResource(new Room("R101", "Quiet Study Room", 6));
    campus.addResource(new Room("R202", "Group Study Room", 12));
    campus.addResource(new Lab("L301", "Computer Lab", 30, "Java workstations"));
    campus.addResource(new Book("BK01", "Object-Oriented Design", "A. Sharma"));
    campus.addResource(new Book("BK02", "Effective Java", "J. Bloch"));
    resourceManager.registerUser(new Student("S1001", "Ada Student"));
    resourceManager.registerUser(new Student("S1002", "Grace Student"));
    resourceManager.registerUser(new Faculty("F2001", "Dr. Lin Faculty"));
    resourceManager.registerUser(new Admin("A3001", "Campus Admin"));
  }

  public static void main(String[] args) {
    new Main().run();
  }

  private void run() {
    System.out.println("=== " + university.getName() + " Resource Manager ===");
    boolean running = true;
    while (running) {
      printMenu();
      try {
        if (!scanner.hasNextLine()) {
          save();
          break;
        }
        switch (scanner.nextLine().trim()) {
          case "1" -> listResources();
          case "2" -> search();
          case "3" -> book();
          case "4" -> cancel();
          case "5" -> listBookings();
          case "6" -> save();
          case "7" -> permissions();
          case "8" -> concurrencyDemo();
          case "0" -> {
            save();
            running = false;
          }
          default -> System.out.println("Choose a listed option.");
        }
      } catch (Exception exception) {
        System.out.println("Error: " + exception.getMessage());
      }
    }
    System.out.println("Goodbye.");
  }

  private void printMenu() {
    System.out.println(
        "\n1 List resources | 2 Search | 3 Book | 4 Cancel | 5 Bookings | 6 Save | 7 Permissions | 8 Thread demo | 0 Exit");
  }

  private void listResources() {
    university.getResources().forEach(System.out::println);
  }

  private void search() {
    System.out.print("Search text: ");
    List<Resource> results = manager.searchResource(scanner.nextLine());
    results.forEach(System.out::println);
    if (results.isEmpty())
      System.out.println("No matches.");
  }

  private void book() throws Exception {
    System.out.print("User ID: ");
    User user = manager.findUser(scanner.nextLine());
    System.out.print("Resource ID: ");
    Resource resource = manager.findResource(scanner.nextLine());
    if (resource == null)
      throw new RoomNotAvailableException("Resource not found.");
    System.out.println("Created: " + manager.bookResource(user, resource));
  }

  private void cancel() throws Exception {
    System.out.print("User ID: ");
    User user = manager.findUser(scanner.nextLine());
    System.out.print("Booking ID: ");
    manager.cancelBooking(user, scanner.nextLine());
    System.out.println("Booking cancelled.");
  }

  private void listBookings() {
    manager.getBookings().forEach(System.out::println);
  }

  private void save() throws IOException {
    storage.save(manager);
    System.out.println("Saved to data/. Read-back:");
    storage.printSavedBookings();
  }

  private void permissions() {
    for (User user : manager.getUsers()) {
      System.out.print(user + " -> ");
      user.showPermissions();
    }
  }

  private void concurrencyDemo() throws Exception {
    Room room = (Room) manager.findResource("R101");
    manager.resetResourceForDemo(room);
    Runnable attempt = () -> {
      try {
        System.out.println(Thread.currentThread().getName() + " won: "
            + manager.bookRoom(manager.findUser(Thread.currentThread().getName()), room));
      } catch (Exception e) {
        System.out.println(Thread.currentThread().getName() + " lost: " + e.getMessage());
      }
    };
    Thread first = new Thread(attempt, "S1001");
    Thread second = new Thread(attempt, "S1002");
    first.start();
    second.start();
    first.join();
    second.join();
  }
}
