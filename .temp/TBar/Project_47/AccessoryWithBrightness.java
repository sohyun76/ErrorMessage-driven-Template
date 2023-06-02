package io.github.hapjava.accessories.optionalcharacteristic;

import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import java.util.concurrent.CompletableFuture;

/**
 * Accessory with brightness values.
 *
 * @author Andy Lintner
 */
public interface AccessoryWithBrightness {

  /**
   * Retrieves the current brightness of the light
   *
   * @return a future that will contain the brightness, expressed as an integer between 0 and 100.
   */
  CompletableFuture<Integer> getBrightness();

  /**
   * Sets the current brightness of the light
   *
   * @param value the brightness, on a scale of 0 to 100, to set
   * @return a future that completes when the brightness is changed
   * @throws Exception when the brightness cannot be set
   */
  CompletableFuture<Void> setBrightness(Integer value) throws Exception;

  /**
   * Subscribes to changes in the brightness of the light.
   *
   * @param callback the function to call when the state changes.
   */
  void subscribeBrightness(HomekitCharacteristicChangeCallback callback);

  /** Unsubscribes from changes in the brightness of the light. */
  void unsubscribeBrightness();
}
