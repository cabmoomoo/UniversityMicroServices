
package UniversityMicroServices.DiscoveryService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class App {
  public static void main(String[] args) {
    ServiceRegister serviceRegister = new ServiceRegister();

    Javalin app = Javalin.create();
    app.get("/{service}", serviceRegister::getService);
    app.post("/{service}", serviceRegister::registerService);
    app.delete("/{service}", serviceRegister::removeService);
    app.get("/", serviceRegister::pingRegister);
    app.start(7000);

    Timer serviceEKG = new Timer("serviceEKG", true);
    serviceEKG.scheduleAtFixedRate(
      new TimerTask() {
        @Override
        public void run() {
          serviceRegister.serviceListEKG();
        }
      }, 
      30000, 
      30000
    );
  }
}

class ServiceRegister {
  private final Map<String, List<String>> serviceMap = new ConcurrentHashMap<>();
  private final Map<String, Integer> serviceIndex = new ConcurrentHashMap<>();
  private final ExecutorService threadPool = Executors.newFixedThreadPool(3);

  private final String ports = ":7000";

  /*
   * Endpoints
   */
  void registerService(Context ctx) {
    String serviceName = ctx.pathParam("service");
    String serviceAddress = ctx.ip() + this.ports;
    List<String> serviceList = this.serviceMap.get(serviceName);
    if (serviceList == null) {
      this.serviceMap.put(serviceName, new ArrayList<>());
      serviceList = this.serviceMap.get(serviceName);
    }
    if (!this.serviceIndex.containsKey(serviceName)) {
      serviceIndex.put(serviceName, 0);
    }
    if (!serviceList.contains(serviceAddress)) {
      serviceList.add(serviceAddress);
    }
  }

  void getService(Context ctx) {
    String serviceName = ctx.pathParam("service");
    try {
      List<String> serviceList = this.serviceMap.get(serviceName);
      int index = this.serviceIndex.get(serviceName);
      if (index > serviceList.size()) {
        index = 0;
      }
      String serviceIP = serviceList.get(index);
      ctx.result(serviceIP);
    } catch (NullPointerException | IndexOutOfBoundsException e) {
      ctx.status(500);
      ctx.result("Failed to find registered instance for service " + serviceName + "\n" + e.getMessage());
    }
  }

  void removeService(Context ctx) {
    String serviceName = ctx.pathParam("service");
    List<String> serviceList = this.serviceMap.get(serviceName);
    if (serviceList == null) {
      returnError(ctx, "No such service %s found".formatted(serviceName));
      return;
    }
    boolean success = serviceList.remove(ctx.ip() + this.ports);
    if (!success) {
      returnError(ctx, "Service %s exists, but IP %s was not already registered".formatted(serviceName, ctx.ip() + this.ports));
    }
  }

  void pingRegister(Context ctx) {
    StringBuilder result = new StringBuilder();
    result.append("Hi there, you've pinged the University Discovery Service without providing a service name. Here's what our registry looks like:\n");
    for (String serviceName : this.serviceMap.keySet()) {
      result.append(
        """
          %s:
        """.formatted(serviceName)
      );
      for (String serviceIP : this.serviceMap.get(serviceName)) {
        result.append(
        """
            %s,
        """.formatted(serviceIP)
        );
      }
    }
    result.append("Please note that all listed IPs will be relative to this Discovery Service. If those IPs look like local subnets, that probably means they are.");
    ctx.result(result.toString());
  }

  /*
   * Register Maintainence
   */
  boolean pingService(String IP) {
    try {
      URL url = new URI("http://" + IP).toURL();
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setConnectTimeout(2500);
      conn.setReadTimeout(5000);
      conn.connect();
      if (conn.getResponseCode() != 200) {
        return false;
      }
    } catch (IOException | URISyntaxException ex) {
      return false;
    }
    return true;
  }

  void serviceListEKG() {
    for (String serviceName : this.serviceMap.keySet()) {
      for (String serviceIP : this.serviceMap.get(serviceName)) {
        threadPool.execute(() -> {
          // Ping heartbeat 3 times
          int counter = 0;
          while (counter < 3) {
            boolean serviceAlive = pingService(serviceIP);
            if (serviceAlive) {
              System.out.println("Successfully pinged service " + serviceName + " at IP address " + serviceIP);
              return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
              return;
            }
            counter += 1;
          }
          System.out.println("Heartbeat failed for service " + serviceName + " at IP address " + serviceIP + ", removing...");
          synchronized (this.serviceIndex) {
            int currIndex = this.serviceIndex.get(serviceName);
            int deadServiceIndex = this.serviceMap.get(serviceName).indexOf(serviceIP);
            if (deadServiceIndex >= currIndex) {
              this.serviceIndex.put(serviceName, Math.max(0, currIndex-1));
            }
            this.serviceMap.get(serviceName).remove(deadServiceIndex);
          }
        });
      }
    }
  }

  /*
   * Util Methods
   */

  private void returnError(Context ctx, String msg) {
    ctx.status(500);
    ctx.result(msg);
  }

}