import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.pagerduty.martin.PagerDutyClient;
import com.pagerduty.martin.Config;

public class Test {

  private static String createdIncidentID = null;
  private static boolean success = false;
  private static String message = null;

  public static String randomString() {
    int leftLimit = 48; // numeral '0'
    int rightLimit = 122; // letter 'z'
    int targetStringLength = 10;
    Random random = new Random();

    String generatedString = random.ints(leftLimit, rightLimit + 1)
      .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
      .limit(targetStringLength)
      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
      .toString();

    return generatedString;
}
  public static String servicePostBody(String name, String ep) {
    return "{" +
      "\"service\": {" +
        "\"type\": \"service\", " +
        "\"name\": \"" + name + "\", " +
        "\"escalation_policy\": {" +
          "\"type\": \"escalation_policy\", " +
          "\"id\": \"" + ep + "\"" +
        "}," +
        "\"incident_urgency_rule\": {" +
          "\"type\": \"constant\", " +
          "\"urgency\": \"low\"" +
        "}" +
      "}" +
    "}";
  }

  public static String epPostBody(String name, String user) {
    return "{" +
      "\"escalation_policy\": {" +
        "\"type\": \"escalation_policy\"," +
        "\"name\": \"" + name + "\"," +
        "\"escalation_rules\": [" +
          "{" +
            "\"escalation_delay_in_minutes\": 30," +
            "\"targets\": [" +
              "{" +
                "\"id\": \"" + user + "\"," +
                "\"type\": \"user_reference\"" +
              "}" +
            "]" +
          "}" +
        "]" +
      "}" +
    "}";
  }

  public static String webhookSubscriptionPostBody(String url, String serviceID) {
    return "{" +
      "\"webhook_subscription\": {" +
        "\"delivery_method\": {" +
          "\"type\": \"http_delivery_method\"," +
          "\"url\": \"" + url + "\"" +
        "}," +
        "\"events\": [" +
          "\"incident.triggered\"" +
        "]," +
        "\"filter\": {" +
          "\"id\": \"" + serviceID + "\"," +
          "\"type\": \"service_reference\"" +
        "}," +
        "\"type\": \"webhook_subscription\"" +
      "}" +
    "}";
  }

  public static String incidentPostBody(String serviceID) {
    return "{" +
      "\"incident\": {" +
        "\"type\": \"incident\"," +
        "\"title\": \"The server is on fire.\"," +
        "\"service\": {" +
          "\"id\": \"" + serviceID + "\"," +
          "\"type\": \"service_reference\"" +
        "}," +
        "\"urgency\": \"high\"" +
      "}" +
    "}";
  }

  public static String incidentResolveBody(String incidentID) {
    return "{" +
      "\"incidents\": [" +
        "{" +
          "\"id\": \"" + incidentID + "\"," +
          "\"type\": \"incident_reference\"," +
          "\"status\": \"resolved\"" +
        "}" +
      "]" +
    "}";
  }

  public static String getUserID(PagerDutyClient pdc, String email) {
    String userID = null;
    try {
      String users = pdc.get("users", "query=" + URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8.toString()));
      JsonObject r = new Gson().fromJson(users, JsonObject.class);
      JsonArray userArray = r.get("users").getAsJsonArray();
      if (userArray.size() == 0) {
        System.out.println("no user found");
      } else {
        for (JsonElement u : userArray) {
          JsonObject user = u.getAsJsonObject();
          if (user.get("email").getAsString().equals(email)) {
            userID = user.get("id").getAsString();
            break;
          }
        }
      }
    } catch (Exception e) {
      System.out.println("Failed to list PagerDuty users for email " + email + ". Try again.");
    }
    return userID;
  }

  public static String createEP(PagerDutyClient pdc, String fromEmail, String name, String userID) {
    String epID = null;
    try {
        String epPostBody = epPostBody(name, userID);
        String epRespStr = pdc.post("escalation_policies", epPostBody, fromEmail);
        JsonObject r = new Gson().fromJson(epRespStr, JsonObject.class);
        epID = r.get("escalation_policy").getAsJsonObject().get("id").getAsString();
    } catch (Exception e) {
      System.out.println("Failed to create PagerDuty escalation policy. Try again.");
    }
    return epID;
  }

  public static String createService(PagerDutyClient pdc, String fromEmail, String name, String epID) {
    String serviceID = null;
    try {
      String servicePostBody = servicePostBody(name, epID);
      String serviceRespStr = pdc.post("services", servicePostBody, fromEmail);
      JsonObject r = new Gson().fromJson(serviceRespStr, JsonObject.class);
      serviceID = r.get("service").getAsJsonObject().get("id").getAsString();
    } catch (Exception e) {
      System.out.println("Failed to create PagerDuty service. Try again.");
    }
    return serviceID;
  }

  public static String createIncident(PagerDutyClient pdc, String fromEmail, String serviceID) {
    String incidentID = null;
    try {
      String incidentPostBody = incidentPostBody(serviceID);
      String incidentRespStr = pdc.post("incidents", incidentPostBody, fromEmail);
      JsonObject r = new Gson().fromJson(incidentRespStr, JsonObject.class);
      incidentID = r.get("incident").getAsJsonObject().get("id").getAsString();
    } catch (Exception e) {
      System.out.println("Failed to create PagerDuty incident. Try again.");
    }
    return incidentID;
  }

  public static boolean resolveIncident(PagerDutyClient pdc, String fromEmail, String incidentID) {
    try {
      String incidentResolveBody = incidentResolveBody(incidentID);
      String incidentRespStr = pdc.put("incidents", incidentResolveBody, fromEmail);
      new Gson().fromJson(incidentRespStr, JsonObject.class);
    } catch (Exception e) {
      System.out.println(e);
      System.out.println("Failed to resolve PagerDuty incident. Try again.");
      return false;
    }
    return true;
  }

  public static String createWebhookSubscription(PagerDutyClient pdc, String url, String serviceID) {
    String webhookSubscriptionID = null;
    try {
      String webhookSubscriptionPostBody = webhookSubscriptionPostBody(url, serviceID);
      String respStr = pdc.post("webhook_subscriptions", webhookSubscriptionPostBody, null);
      JsonObject r = new Gson().fromJson(respStr, JsonObject.class);
      webhookSubscriptionID = r.get("webhook_subscription").getAsJsonObject().get("id").getAsString();
    } catch (Exception e) {
      System.out.println(e);
      System.out.println("Failed to create PagerDuty webhook subscription. Try again.");
    }
    return webhookSubscriptionID;
  }

  public static void cleanup(PagerDutyClient pdc, String fromEmail, String incidentID, String serviceID, String epID, HttpServer server) {
    try {
      if (incidentID != null) {
        System.out.print("* Resolving incident " + incidentID + "... ");
        resolveIncident(pdc, fromEmail, incidentID);
        System.out.println("done.");
      }
      if (serviceID != null) {
        System.out.print("* Deleting service " + serviceID + "... ");
        pdc.delete("services/" + serviceID);
        System.out.println("done.");
      }
      if (epID != null) {
        System.out.print("* Deleting escalation policy " + epID + "... ");
        pdc.delete("escalation_policies/" + epID);
        System.out.println("done.");
      }
      if (server != null) {
        System.out.print("* Stopping webhook listener... ");
        server.stop(0);
        System.out.println("done.");
      }
    } catch (Exception e) {}
  }

  public static void main(String[] args) throws IOException {
    Config config = new Config();
    System.out.println("\nStarting test...");
    String randomString = randomString();
    HttpServer server = HttpServer.create(new InetSocketAddress(config.listenPort), 0);
    HttpContext context = server.createContext("/");
    context.setHandler(Test::handleRequest);
    server.start();
    System.out.println("* Webhook listener started on port " + config.listenPort);

    PagerDutyClient pdc = new PagerDutyClient(config.restToken);

    System.out.print("* Getting your PagerDuty user ID... ");
    String userID = getUserID(pdc, config.fromEmail);
    if (userID == null) {
      System.out.println("No PagerDuty user found for email " + config.fromEmail + ". Try again.");
      System.exit(1);
    }
    System.out.println("Got user id " + userID);

    System.out.print("* Creating escalation policy \"Probe EP " + randomString + "\"... ");
    String epID = createEP(pdc, config.fromEmail, "Probe EP " + randomString, userID);
    if (epID == null) System.exit(1);
    System.out.println("Created escalation policy " + epID);

    System.out.print("* Creating service \"Probe Service " + randomString + "\"... ");
    String serviceID = createService(pdc, config.fromEmail, "Probe Service " + randomString, epID);
    if (serviceID == null) {
      cleanup(pdc, null, null, null, epID, null);
      System.exit(1);
    }
    System.out.println("Created service " + serviceID);

    System.out.print("* Creating webhook subscription... ");
    String webhookSubscriptionID = createWebhookSubscription(pdc, config.webhookURL, serviceID);
    if (webhookSubscriptionID == null) {
      cleanup(pdc, null, null, serviceID, epID, null);
      System.exit(1);
    }
    System.out.println("Created webhook subscription " + webhookSubscriptionID);

    System.out.print("* Creating incident... ");
    String incidentID = createIncident(pdc, config.fromEmail, serviceID);
    if (incidentID == null) {
      System.out.println("Failed to create an incident in PagerDuty. Try again.");
      cleanup(pdc, null, null, serviceID, epID, null);
      System.exit(1);
    }
    Test.createdIncidentID = incidentID;
    System.out.println("Created incident " + incidentID);
    Test.message = "No webhook sent to " + config.webhookURL + " was received on port " + config.listenPort + " on this host.";
    System.out.print("Waiting 10 seconds for webhook delivery... ");
    try {
      Thread.sleep(10000);
    } catch (Exception e) {}
    if (Test.success) {
      System.out.println("\n\nAll set! Network connection to PagerDuty is working fine.\n\n");
    } else {
      System.out.println("\n\nUh oh. There seems to be a problem: " + Test.message + " Please fix this and try again.\n\n");
    }
    System.out.println("Cleaning up...");
    cleanup(pdc, config.fromEmail, incidentID, serviceID, epID, server);
  }

  private static void handleRequest(HttpExchange exchange) throws IOException {
    if ("POST".equals(exchange.getRequestMethod())) {
      BufferedReader in = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
      String inputLine;
      StringBuffer reqBody = new StringBuffer();

      while ((inputLine = in.readLine()) != null) {
        reqBody.append(inputLine);
      }
      in.close();
      try {
        JsonObject r = new Gson().fromJson(reqBody.toString(), JsonObject.class);
        String webhookEvent = r.get("event").getAsJsonObject()
            .get("event_type").getAsString();
        String webhookIncidentID = r.get("event").getAsJsonObject()
            .get("data").getAsJsonObject()
            .get("id").getAsString();  
        System.out.println("Got an " + webhookEvent + " webhook for incident " + webhookIncidentID);
        if (webhookIncidentID.equals(Test.createdIncidentID)) {
          Test.success = true;
        } else {
          Test.message += "Got unexpected webhook values. The webhook body received was " + reqBody.toString();
        }
      } catch (Exception e) {
        Test.message += "Failed to parse POST body - exception: " + e + "\n" +
            "The webhook body received was" + reqBody.toString();
      }
    }
    String response = "Hi there!";
    exchange.sendResponseHeaders(200, response.getBytes().length);// response code and length
    OutputStream os = exchange.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }
}