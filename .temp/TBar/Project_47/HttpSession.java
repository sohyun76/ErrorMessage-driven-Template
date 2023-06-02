package io.github.hapjava.server.impl.connections;

import io.github.hapjava.accessories.HomekitAccessory;
import io.github.hapjava.server.HomekitAuthInfo;
import io.github.hapjava.server.impl.HomekitRegistry;
import io.github.hapjava.server.impl.http.HomekitClientConnection;
import io.github.hapjava.server.impl.http.HttpRequest;
import io.github.hapjava.server.impl.http.HttpResponse;
import io.github.hapjava.server.impl.jmdns.JmdnsHomekitAdvertiser;
import io.github.hapjava.server.impl.json.AccessoryController;
import io.github.hapjava.server.impl.json.CharacteristicsController;
import io.github.hapjava.server.impl.pairing.PairVerificationManager;
import io.github.hapjava.server.impl.pairing.PairingManager;
import io.github.hapjava.server.impl.pairing.PairingUpdateController;
import io.github.hapjava.server.impl.responses.InternalServerErrorResponse;
import io.github.hapjava.server.impl.responses.NotFoundResponse;
import java.io.IOException;
import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpSession {

  private volatile PairingManager pairingManager;
  private volatile PairVerificationManager pairVerificationManager;
  private volatile AccessoryController accessoryController;
  private volatile CharacteristicsController characteristicsController;

  private final HomekitAuthInfo authInfo;
  private final HomekitRegistry registry;
  private final SubscriptionManager subscriptions;
  private final HomekitClientConnection connection;
  private final JmdnsHomekitAdvertiser advertiser;

  private static final Logger logger = LoggerFactory.getLogger(HttpSession.class);

  public HttpSession(
      HomekitAuthInfo authInfo,
      HomekitRegistry registry,
      SubscriptionManager subscriptions,
      HomekitClientConnection connection,
      JmdnsHomekitAdvertiser advertiser) {
    this.authInfo = authInfo;
    this.registry = registry;
    this.subscriptions = subscriptions;
    this.connection = connection;
    this.advertiser = advertiser;
  }

  public HttpResponse handleRequest(HttpRequest request) throws IOException {
    switch (request.getUri()) {
      case "/pair-setup":
        return handlePairSetup(request);

      case "/pair-verify":
        return handlePairVerify(request);

      default:
        if (registry.isAllowUnauthenticatedRequests()) {
          return handleAuthenticatedRequest(request);
        } else {
          logger.warn("Unrecognized request for " + request.getUri());
          return new NotFoundResponse();
        }
    }
  }

  public HttpResponse handleAuthenticatedRequest(HttpRequest request) throws IOException {
    advertiser.setDiscoverable(
        false); // brigde is already bound and should not be discoverable anymore
    try {
      switch (request.getUri()) {
        case "/accessories":
          return getAccessoryController().listing();

        case "/characteristics":
          switch (request.getMethod()) {
            case PUT:
              return getCharacteristicsController().put(request, connection);

            default:
              logger.warn("Unrecognized method for " + request.getUri());
              return new NotFoundResponse();
          }

        case "/pairings":
          return new PairingUpdateController(authInfo, advertiser).handle(request);

        default:
          if (request.getUri().startsWith("/characteristics?")) {
            return getCharacteristicsController().get(request);
          }
          logger.warn("Unrecognized request for " + request.getUri());
          return new NotFoundResponse();
      }
    } catch (Exception e) {
      logger.warn("Could not handle request", e);
      return new InternalServerErrorResponse(e);
    }
  }

  private HttpResponse handlePairSetup(HttpRequest request) {
    if (pairingManager == null) {
      synchronized (HttpSession.class) {
        if (pairingManager == null) {
          pairingManager = new PairingManager(authInfo, registry);
        }
      }
    }
    try {
      return pairingManager.handle(request);
    } catch (Exception e) {
      logger.warn("Exception encountered during pairing", e);
      return new InternalServerErrorResponse(e);
    }
  }

  private HttpResponse handlePairVerify(HttpRequest request) {
    if (pairVerificationManager == null) {
      synchronized (HttpSession.class) {
        if (pairVerificationManager == null) {
          pairVerificationManager = new PairVerificationManager(authInfo, registry);
        }
      }
    }
    try {
      return pairVerificationManager.handle(request);
    } catch (Exception e) {
      logger.warn("Exception encountered while verifying pairing", e);
      return new InternalServerErrorResponse(e);
    }
  }

  private synchronized AccessoryController getAccessoryController() {
    if (accessoryController == null) {
      accessoryController = new AccessoryController(registry);
    }
    return accessoryController;
  }

  private synchronized CharacteristicsController getCharacteristicsController() {
    if (characteristicsController == null) {
      characteristicsController = new CharacteristicsController(registry, subscriptions);
    }
    return characteristicsController;
  }

  public static class SessionKey {
    private final InetAddress address;
    private final HomekitAccessory accessory;

    public SessionKey(InetAddress address, HomekitAccessory accessory) {
      this.address = address;
      this.accessory = accessory;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SessionKey) {
        return address.equals(((SessionKey) obj).address)
            && accessory.equals(((SessionKey) obj).accessory);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      int hash = 1;
      hash = hash * 31 + address.hashCode();
      hash = hash * 31 + accessory.hashCode();
      return hash;
    }
  }
}
