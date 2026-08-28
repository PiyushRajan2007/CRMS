package campus;

public class Book extends Resource implements Bookable {
  private final String author;
  private boolean available = true;

  public Book(String id, String name, String author) {
    super(id, name, "Book");
    this.author = author;
  }

  public String getAuthor() {
    return author;
  }

  @Override
  public boolean isAvailable() {
    return available;
  }

  @Override
  public void setAvailable(boolean available) {
    this.available = available;
  }

  @Override
  public String toString() {
    return super.toString() + " | author=" + author + " | " + (available ? "available" : "borrowed");
  }
}
