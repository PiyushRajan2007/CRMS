package campus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Encapsulation centralizes rules; synchronized methods make booking thread-safe.
public class ResourceManager {
  private final University university;
  private final Map<String, User> users = new ConcurrentHashMap<>();
  private final List<Booking> bookings = new ArrayList<>();

  public ResourceManager(University university) {
    this.university = university;
  }

  public void registerUser(User user) {
    if (user == null || user.getId().isBlank())
      throw new IllegalArgumentException("User ID cannot be empty.");
    if (users.putIfAbsent(user.getId().toUpperCase(), user) != null)
      throw new IllegalArgumentException("Duplicate user ID: " + user.getId());
  }

  public User findUser(String id) throws InvalidUserException {
    if (id == null || id.isBlank())
      throw new InvalidUserException("User ID cannot be empty.");
    User user = users.get(id.toUpperCase());
    if (user == null)
      throw new InvalidUserException("No user found for ID: " + id);
    return user;
  }

  public Resource findResource(String id) {
    if (id == null || id.isBlank())
      return null;
    for (Resource resource : university.getResources())
      if (resource.getId().equalsIgnoreCase(id))
        return resource;
    return null;
  }

  public synchronized Booking bookRoom(User user, Room room) throws RoomNotAvailableException, InvalidUserException {
    validateUser(user);
    validateResource(room);
    checkPermission(user, room);
    if (!room.isAvailable())
      throw new RoomNotAvailableException(room.getName() + " is already booked.");
    if (activeBookings(user).size() >= user.getBookingLimit())
      throw new InvalidUserException("Booking limit reached for " + user.getName());
    room.setAvailable(false);
    Booking booking = new Booking(user, room);
    bookings.add(booking);
    return booking;
  }

  public synchronized Booking bookResource(User user, Resource resource)
      throws RoomNotAvailableException, InvalidUserException {
    validateUser(user);
    validateResource(resource);
    checkPermission(user, resource);
    if (!(resource instanceof Bookable bookable))
      throw new RoomNotAvailableException("This resource cannot be booked.");
    if (!bookable.isAvailable())
      throw new RoomNotAvailableException(resource.getName() + " is already unavailable.");
    if (activeBookings(user).size() >= user.getBookingLimit())
      throw new InvalidUserException("Booking limit reached for " + user.getName());
    bookable.setAvailable(false);
    Booking booking = new Booking(user, resource);
    bookings.add(booking);
    return booking;
  }

  public synchronized Booking waitForRoom(User user, Room room)
      throws InterruptedException, RoomNotAvailableException, InvalidUserException {
    while (!room.isAvailable())
      wait();
    return bookRoom(user, room);
  }

  public synchronized void cancelBooking(User requester, String bookingId)
      throws BookingNotFoundException, InvalidUserException {
    validateUser(requester);
    if (bookingId == null || bookingId.isBlank())
      throw new BookingNotFoundException("Booking ID cannot be empty.");
    for (Booking booking : bookings) {
      if (booking.getId().equalsIgnoreCase(bookingId) && booking.isActive()) {
        if (!(requester instanceof Admin) && !booking.getUser().getId().equalsIgnoreCase(requester.getId()))
          throw new InvalidUserException("You can cancel only your own bookings.");
        booking.close();
        if (booking.getResource() instanceof Bookable bookable)
          bookable.setAvailable(true);
        notifyAll();
        return;
      }
    }
    throw new BookingNotFoundException("Active booking not found: " + bookingId);
  }

  public List<Resource> searchResource(String query) {
    return recursiveSearch(university.getResources(), query, 0, new ArrayList<>());
  }

  public List<Resource> searchResource(String query, String category) {
    List<Resource> results = new ArrayList<>();
    for (Resource resource : searchResource(query))
      if (resource.getCategory().equalsIgnoreCase(category))
        results.add(resource);
    return results;
  }

  private List<Resource> recursiveSearch(List<Resource> resources, String query, int index, List<Resource> results) {
    if (index == resources.size())
      return results;
    if (resources.get(index).matches(query))
      results.add(resources.get(index));
    return recursiveSearch(resources, query, index + 1, results);
  }

  public synchronized List<Booking> getBookings() {
    return List.copyOf(bookings);
  }

  public synchronized void resetResourceForDemo(Resource resource) {
    for (Booking booking : bookings)
      if (booking.isActive() && booking.getResource() == resource)
        booking.close();
    if (resource instanceof Bookable bookable)
      bookable.setAvailable(true);
    notifyAll();
  }

  public List<User> getUsers() {
    return List.copyOf(users.values());
  }

  private List<Booking> activeBookings(User user) {
    List<Booking> result = new ArrayList<>();
    for (Booking booking : bookings)
      if (booking.isActive() && booking.getUser().getId().equals(user.getId()))
        result.add(booking);
    return result;
  }

  private void validateUser(User user) throws InvalidUserException {
    if (user == null || user.getId() == null || !users.containsKey(user.getId().toUpperCase()))
      throw new InvalidUserException("User is not registered.");
  }

  private void validateResource(Resource resource) throws RoomNotAvailableException {
    if (resource == null || findResource(resource.getId()) != resource)
      throw new RoomNotAvailableException("Resource is not registered.");
  }

  private void checkPermission(User user, Resource resource) throws InvalidUserException {
    if (!user.canBook(resource))
      throw new InvalidUserException(user.getClass().getSimpleName() + " cannot book "
          + resource.getClass().getSimpleName() + " resources.");
  }

  public synchronized void addLoadedBooking(Booking booking) throws RoomNotAvailableException {
    if (booking == null || booking.getUser() == null || booking.getResource() == null)
      throw new RoomNotAvailableException("Invalid saved booking.");
    for (Booking existing : bookings)
      if (existing.getId().equalsIgnoreCase(booking.getId()))
        throw new RoomNotAvailableException("Duplicate booking ID: " + booking.getId());
    bookings.add(booking);
    if (booking.isActive() && booking.getResource() instanceof Bookable bookable)
      bookable.setAvailable(false);
  }
}
