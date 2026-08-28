package campus;

import java.util.ArrayList;
import java.util.List;

public class University {
  private final String name;
  private final List<Resource> resources = new ArrayList<>();

  public University(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void addResource(Resource resource) {
    if (resource == null)
      throw new IllegalArgumentException("Resource cannot be null.");
    if (findResource(resource.getId()) != null)
      throw new IllegalArgumentException("Duplicate resource ID: " + resource.getId());
    resources.add(resource);
  }

  private Resource findResource(String id) {
    for (Resource resource : resources)
      if (resource.getId().equalsIgnoreCase(id))
        return resource;
    return null;
  }

  public List<Resource> getResources() {
    return List.copyOf(resources);
  }
}
