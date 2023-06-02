package io.github.hapjava.server.impl;

import io.github.hapjava.accessories.HomekitAccessory;
import io.github.hapjava.characteristics.Characteristic;
import io.github.hapjava.services.Service;
import io.github.hapjava.services.impl.AccessoryInformationService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomekitRegistry {

  private static final Logger logger = LoggerFactory.getLogger(HomekitRegistry.class);

  private final String label;
  private final Map<Integer, HomekitAccessory> accessories;
  private final Map<HomekitAccessory, List<Service>> services = new HashMap<>();
  private final Map<HomekitAccessory, Map<Integer, Characteristic>> characteristics =
      new HashMap<>();
  private boolean isAllowUnauthenticatedRequests = false;

  public HomekitRegistry(String label) {
    this.label = label;
    this.accessories = new ConcurrentHashMap<>();
    reset();
  }

  public synchronized void reset() {
    characteristics.clear();
    services.clear();
    for (HomekitAccessory accessory : accessories.values()) {
      int iid = 0;
      List<Service> newServices;
      try {
        newServices = new ArrayList<>(2);
        newServices.add(new AccessoryInformationService(accessory));
        newServices.addAll(accessory.getServices());
      } catch (Exception e) {
        logger.warn("Could not instantiate services for accessory " + accessory.getName(), e);
        services.put(accessory, Collections.emptyList());
        continue;
      }
      Map<Integer, Characteristic> newCharacteristics = new HashMap<>();
      services.put(accessory, newServices);
      for (Service service : newServices) {
        iid++;
        for (Characteristic characteristic : service.getCharacteristics()) {
          newCharacteristics.put(++iid, characteristic);
        }
      }
      characteristics.put(accessory, newCharacteristics);
    }
  }

  public String getLabel() {
    return label;
  }

  public Collection<HomekitAccessory> getAccessories() {
    return accessories.values();
  }

  public List<Service> getServices(Integer aid) {
    return Collections.unmodifiableList(services.get(accessories.get(aid)));
  }

  public Map<Integer, Characteristic> getCharacteristics(Integer aid) {
    Map<Integer, Characteristic> characteristics = this.characteristics.get(accessories.get(aid));
    if (characteristics == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(characteristics);
  }

  public void add(HomekitAccessory accessory) {
    accessories.put(accessory.getId(), accessory);
  }

  public void remove(HomekitAccessory accessory) {
    accessories.remove(accessory.getId());
  }

  public boolean isAllowUnauthenticatedRequests() {
    return isAllowUnauthenticatedRequests;
  }

  public void setAllowUnauthenticatedRequests(boolean allow) {
    this.isAllowUnauthenticatedRequests = allow;
  }
}
