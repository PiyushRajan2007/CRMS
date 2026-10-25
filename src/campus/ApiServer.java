package campus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;


public class ApiServer {
  private final University university;
  private final ResourceManager manager;
  private final StorageService storage = new StorageService();

  public ApiServer() {
    Main main = new Main();
    university = main.getUniversity();
    manager = main.getResourceManager();
  }

  public void start() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    server.createContext("/api/", this::handle);
    server.setExecutor(Executors.newFixedThreadPool(8));
    server.start();
    System.out.println("Campus API listening on http://localhost:8080");
  }

  private void handle(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(204, -1);
      exchange.close();
      return;
    }

    try {
      Object response = route(exchange);
      sendJson(exchange, 200, response);
    } catch (Exception exception) {
      int status = exception instanceof IllegalArgumentException ? 400 : 500;
      if (exception instanceof MethodNotAllowedException)
        status = 405;
      if (exception instanceof InvalidUserException || exception instanceof RoomNotAvailableException
          || exception instanceof BookingNotFoundException)
        status = 400;
      sendJson(exchange, status, error(exception));
    } finally {
      exchange.close();
    }
  }

  private Object route(HttpExchange exchange) throws Exception {
    String path = exchange.getRequestURI().getPath();
    String method = exchange.getRequestMethod();
    if ("GET".equalsIgnoreCase(method)) {
      return switch (path) {
        case "/api/resources" -> resources(university.getResources());
        case "/api/search" -> {
          String query = queryParameter(exchange.getRequestURI().getRawQuery(), "q");
          yield resources(manager.searchResource(query == null ? "" : query));
        }
        case "/api/bookings" -> bookings();
        case "/api/users" -> users();
        case "/api/logs" -> logs();
        default -> throw new IllegalArgumentException("Unknown endpoint: " + path);
      };
    }

    if ("POST".equalsIgnoreCase(method)) {
      Map<String, String> body = readJsonObject(exchange);
      return switch (path) {
        case "/api/book" -> book(required(body, "userId"), required(body, "resourceId"), body.get("startAt"), body.get("endAt"));
        case "/api/cancel" -> cancel(required(body, "userId"), required(body, "bookingId"));
        case "/api/save" -> save();
        case "/api/demo-concurrency" -> demoConcurrency();
        default -> throw new IllegalArgumentException("Unknown endpoint: " + path);
      };
    }

    throw new MethodNotAllowedException("Use GET, POST, or OPTIONS.");
  }

  private List<Map<String, Object>> resources(List<Resource> resourceList) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Resource resource : resourceList) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", resource.getId());
      item.put("name", resource.getName());
      item.put("type", resource.getClass().getSimpleName());
      item.put("available", resource instanceof Bookable bookable && bookable.isAvailable());
      if (resource instanceof Room room)
        item.put("capacity", room.getCapacity());
      if (resource instanceof Lab lab)
        item.put("equipment", lab.getEquipment());
      if (resource instanceof Book book)
        item.put("author", book.getAuthor());
      result.add(item);
    }
    return result;
  }

  private Object book(String userId, String resourceId, String startText, String endText) throws Exception {
    User user = manager.findUser(userId);
    Resource resource = manager.findResource(resourceId);
    Booking booking;
    if (startText == null && endText == null) {
      booking = manager.bookResource(user, resource);
    } else {
      if (startText == null || endText == null)
        throw new IllegalArgumentException("Both startAt and endAt are required for a time-slot booking.");
      booking = manager.bookResource(user, resource, LocalDateTime.parse(startText), LocalDateTime.parse(endText));
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("ok", true);
    result.put("bookingId", booking.getId());
    result.put("userId", booking.getUser().getId());
    result.put("resourceId", booking.getResource().getId());
    result.put("active", booking.isActive());
    result.put("startAt", booking.getStartAt().toString());
    result.put("endAt", booking.getEndAt().toString());
    if (booking.getDueAt() != null)
      result.put("dueAt", booking.getDueAt().toString());
    return result;
  }

  private Object cancel(String userId, String bookingId) throws Exception {
    manager.cancelBooking(manager.findUser(userId), bookingId);
    return Map.of("ok", true, "bookingId", bookingId, "cancelled", true);
  }

  private List<Map<String, Object>> bookings() {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Booking booking : manager.getBookings()) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", booking.getId());
      item.put("userId", booking.getUser().getId());
      item.put("resourceId", booking.getResource().getId());
      item.put("active", booking.isActive());
      item.put("createdAt", booking.getCreatedAt().toString());
      item.put("startAt", booking.getStartAt() == null ? null : booking.getStartAt().toString());
      item.put("endAt", booking.getEndAt() == null ? null : booking.getEndAt().toString());
      item.put("dueAt", booking.getDueAt() == null ? null : booking.getDueAt().toString());
      item.put("fine", booking.getFine());
      result.add(item);
    }
    return result;
  }

  private List<Map<String, Object>> users() {
    List<Map<String, Object>> result = new ArrayList<>();
    for (User user : manager.getUsers()) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", user.getId());
      item.put("role", user.getClass().getSimpleName());
      item.put("bookingLimit", user.getBookingLimit());
      result.add(item);
    }
    return result;
  }

  private List<String> logs() throws IOException {
    return Files.readAllLines(Path.of("data", "logs.txt"), StandardCharsets.UTF_8);
  }

  private Object save() throws IOException {
    storage.save(manager);
    return Map.of("ok", true, "saved", true);
  }

  private List<Map<String, Object>> demoConcurrency() throws Exception {
    Resource candidate = manager.findResource("R101");
    if (!(candidate instanceof Room room))
      throw new IllegalStateException("The option-8 demo requires room R101.");
    manager.resetResourceForDemo(room);

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Map<String, Object>> events = java.util.Collections.synchronizedList(new ArrayList<>());
    Runnable attempt = () -> {
      ready.countDown();
      try {
        start.await();
        User user = manager.findUser(Thread.currentThread().getName());
        manager.bookRoom(user, room);
        events.add(demoEvent(Thread.currentThread().getName(), "booked", false, true));
      } catch (RoomNotAvailableException exception) {
        events.add(demoEvent(Thread.currentThread().getName(), "rejected", true, false));
      } catch (Exception exception) {
        events.add(demoEvent(Thread.currentThread().getName(), "error", false, false));
      }
    };

    Thread first = new Thread(attempt, "S1001");
    Thread second = new Thread(attempt, "S1002");
    first.start();
    second.start();
    ready.await();
    start.countDown();
    try {
      first.join();
      second.join();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw exception;
    }
    events.sort(Comparator.comparing(event -> (String) event.get("timestamp")));
    return events;
  }

  private Map<String, Object> demoEvent(String thread, String action, boolean blocked, boolean winner) {
    Map<String, Object> event = new LinkedHashMap<>();
    event.put("thread", thread);
    event.put("action", action);
    event.put("timestamp", LocalDateTime.now().toString());
    event.put("blocked", blocked);
    event.put("winner", winner);
    return event;
  }

  private static Map<String, Object> error(Exception exception) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("ok", false);
    result.put("error", exception.getClass().getSimpleName());
    result.put("message", exception.getMessage() == null ? "" : exception.getMessage());
    return result;
  }

  private static String required(Map<String, String> body, String name) {
    String value = body.get(name);
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("Missing or empty field: " + name);
    return value;
  }

  private static Map<String, String> readJsonObject(HttpExchange exchange) throws IOException {
    byte[] bytes = exchange.getRequestBody().readAllBytes();
    return new JsonObjectParser(new String(bytes, StandardCharsets.UTF_8)).parse();
  }

  private static String queryParameter(String rawQuery, String key) {
    if (rawQuery == null)
      return null;
    for (String parameter : rawQuery.split("&")) {
      int separator = parameter.indexOf('=');
      String name = separator < 0 ? parameter : parameter.substring(0, separator);
      if (URLDecoder.decode(name, StandardCharsets.UTF_8).equals(key))
        return URLDecoder.decode(separator < 0 ? "" : parameter.substring(separator + 1), StandardCharsets.UTF_8);
    }
    return null;
  }

  private static void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
    byte[] body = toJson(value).getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(status, body.length);
    exchange.getResponseBody().write(body);
  }

  private static String toJson(Object value) {
    if (value == null)
      return "null";
    if (value instanceof String string)
      return quote(string);
    if (value instanceof Number || value instanceof Boolean)
      return value.toString();
    if (value instanceof Map<?, ?> map) {
      List<String> fields = new ArrayList<>();
      for (Map.Entry<?, ?> entry : map.entrySet())
        fields.add(quote(String.valueOf(entry.getKey())) + ":" + toJson(entry.getValue()));
      return "{" + String.join(",", fields) + "}";
    }
    if (value instanceof Iterable<?> iterable) {
      List<String> items = new ArrayList<>();
      for (Object item : iterable)
        items.add(toJson(item));
      return "[" + String.join(",", items) + "]";
    }
    return quote(value.toString());
  }

  private static String quote(String value) {
    StringBuilder escaped = new StringBuilder("\"");
    for (int i = 0; i < value.length(); i++) {
      char character = value.charAt(i);
      switch (character) {
        case '"' -> escaped.append("\\\"");
        case '\\' -> escaped.append("\\\\");
        case '\b' -> escaped.append("\\b");
        case '\f' -> escaped.append("\\f");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        default -> {
          if (character < 0x20)
            escaped.append(String.format("\\u%04x", (int) character));
          else
            escaped.append(character);
        }
      }
    }
    return escaped.append('"').toString();
  }

  private static final class JsonObjectParser {
    private final String input;
    private int index;

    JsonObjectParser(String input) {
      this.input = input;
    }

    Map<String, String> parse() {
      skipWhitespace();
      expect('{');
      Map<String, String> values = new LinkedHashMap<>();
      skipWhitespace();
      if (take('}')) {
        skipWhitespace();
        ensureEnd();
        return values;
      }
      while (true) {
        skipWhitespace();
        String key = parseString();
        skipWhitespace();
        expect(':');
        skipWhitespace();
        String value = parseString();
        values.put(key, value);
        skipWhitespace();
        if (take('}'))
          break;
        expect(',');
      }
      skipWhitespace();
      ensureEnd();
      return values;
    }

    private String parseString() {
      expect('"');
      StringBuilder value = new StringBuilder();
      while (index < input.length()) {
        char character = input.charAt(index++);
        if (character == '"')
          return value.toString();
        if (character < 0x20)
          throw invalidJson();
        if (character != '\\') {
          value.append(character);
          continue;
        }
        if (index == input.length())
          throw invalidJson();
        char escape = input.charAt(index++);
        switch (escape) {
          case '"', '\\', '/' -> value.append(escape);
          case 'b' -> value.append('\b');
          case 'f' -> value.append('\f');
          case 'n' -> value.append('\n');
          case 'r' -> value.append('\r');
          case 't' -> value.append('\t');
          case 'u' -> value.append(parseUnicodeEscape());
          default -> throw invalidJson();
        }
      }
      throw invalidJson();
    }

    private char parseUnicodeEscape() {
      if (index + 4 > input.length())
        throw invalidJson();
      try {
        char decoded = (char) Integer.parseInt(input.substring(index, index + 4), 16);
        index += 4;
        return decoded;
      } catch (NumberFormatException exception) {
        throw invalidJson();
      }
    }

    private boolean take(char expected) {
      if (index < input.length() && input.charAt(index) == expected) {
        index++;
        return true;
      }
      return false;
    }

    private void expect(char expected) {
      if (!take(expected))
        throw invalidJson();
    }

    private void skipWhitespace() {
      while (index < input.length() && Character.isWhitespace(input.charAt(index)))
        index++;
    }

    private void ensureEnd() {
      if (index != input.length())
        throw invalidJson();
    }

    private IllegalArgumentException invalidJson() {
      return new IllegalArgumentException("Invalid JSON request body.");
    }
  }

  private static final class MethodNotAllowedException extends Exception {
    MethodNotAllowedException(String message) {
      super(message);
    }
  }

  public static void main(String[] args) throws IOException {
    new ApiServer().start();
  }
}
