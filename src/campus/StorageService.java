package campus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class StorageService {
  private final File directory = new File("data");

  public StorageService() {
    directory.mkdirs();
  }

  public void save(ResourceManager manager) throws IOException {
    try (FileWriter users = new FileWriter(new File(directory, "users.txt"));
        FileWriter resources = new FileWriter(new File(directory, "resources.txt"));
        FileWriter bookings = new FileWriter(new File(directory, "bookings.txt"));
        FileWriter logs = new FileWriter(new File(directory, "logs.txt"))) {
      for (User user : manager.getUsers())
        users.write(
            user.getId() + "|" + user.getName() + "|" + user.getClass().getSimpleName() + System.lineSeparator());
      for (Resource resource : manager.searchResource(""))
        resources.write(resourceRecord(resource) + System.lineSeparator());
      for (Booking booking : manager.getBookings())
        bookings.write(booking.getId() + "|" + booking.getUser().getId() + "|" + booking.getResource().getId() + "|"
            + booking.isActive() + "|" + booking.getCreatedAt() + System.lineSeparator());
      logs.write("Saved at " + java.time.LocalDateTime.now() + System.lineSeparator());
    }
  }

  public boolean hasData() {
    return new File(directory, "users.txt").exists() && new File(directory, "resources.txt").exists()
        && new File(directory, "bookings.txt").exists();
  }

  public void load(ResourceManager manager, University university) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(new File(directory, "users.txt")))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 3)
          throw new IOException("Invalid user record.");
        User user = switch (parts[2]) {
          case "Student" -> new Student(parts[0], parts[1]);
          case "Faculty" -> new Faculty(parts[0], parts[1]);
          case "Admin" -> new Admin(parts[0], parts[1]);
          default -> throw new IOException("Invalid user type.");
        };
        manager.registerUser(user);
      }
    }
    try (BufferedReader reader = new BufferedReader(new FileReader(new File(directory, "resources.txt")))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 4)
          throw new IOException("Invalid resource record.");
        Resource resource = switch (parts[0]) {
          case "Room" -> new Room(parts[1], parts[2], Integer.parseInt(parts[3]));
          case "Lab" -> new Lab(parts[1], parts[2], Integer.parseInt(parts[3]), parts.length > 4 ? parts[4] : "");
          case "Book" -> new Book(parts[1], parts[2], parts[3]);
          default -> throw new IOException("Invalid resource type.");
        };
        university.addResource(resource);
      }
    } catch (NumberFormatException exception) {
      throw new IOException("Invalid resource capacity.", exception);
    }
    try (BufferedReader reader = new BufferedReader(new FileReader(new File(directory, "bookings.txt")))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 5)
          throw new IOException("Invalid booking record.");
        User user = manager.findUser(parts[1]);
        Resource resource = manager.findResource(parts[2]);
        if (resource == null)
          throw new IOException("Saved booking refers to an unknown resource.");
        manager.addLoadedBooking(new Booking(parts[0], user, resource, Boolean.parseBoolean(parts[3]),
            LocalDateTime.parse(parts[4])));
      }
    } catch (Exception exception) {
      if (exception instanceof IOException ioException)
        throw ioException;
      throw new IOException("Invalid booking record.", exception);
    }
  }

  private String resourceRecord(Resource resource) {
    if (resource instanceof Lab lab)
      return "Lab|" + lab.getId() + "|" + lab.getName() + "|" + lab.getCapacity() + "|" + lab.getEquipment();
    if (resource instanceof Room room)
      return "Room|" + room.getId() + "|" + room.getName() + "|" + room.getCapacity();
    Book book = (Book) resource;
    return "Book|" + book.getId() + "|" + book.getName() + "|" + book.getAuthor();
  }

  public void printSavedBookings() throws IOException {
    File file = new File(directory, "bookings.txt");
    if (!file.exists()) {
      System.out.println("No saved bookings yet.");
      return;
    }
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null)
        System.out.println(line);
    }
  }
}
