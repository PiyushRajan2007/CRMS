package campus;

// Multilevel inheritance: a Lab is a specialized Room and Resource.
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
  public ResourceType getResourceType() {
    return ResourceType.LAB;
  }

  @Override
  public String toString() {
    return super.toString() + " | equipment=" + equipment;
  }
}
