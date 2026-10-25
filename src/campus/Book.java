package campus;

// Inheritance plus an interface let a book be handled as a generic resource.
public class Book extends BookableResource {
  private final String author;

  public Book(String id, String name, String author) {
    super(id, name, "Book");
    this.author = author;
  }

  public String getAuthor() {
    return author;
  }

  @Override
  public String toString() {
    return super.toString() + " | author=" + author + " | " + (isAvailable() ? "available" : "borrowed");
  }
}
