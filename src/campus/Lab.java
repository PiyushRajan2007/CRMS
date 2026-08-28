package campus;

public class Lab extends Room {
  private final String equipment;

  public Lab(String id, String name, int capacity, String equipment) {
    super(id, name, capacity);
    this.equipment = equipment;
  }

  public String getEquipment() {
    return equipment;
  }

  @Override
  public String toString() {
    return super.toString() + " | equipment=" + equipment;
  }
}
