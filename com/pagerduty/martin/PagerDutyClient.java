package com.pagerduty.martin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PagerDutyClient {
  public String token;

  public PagerDutyClient(String token) {
    this.token = token;
  }

  public String request(String endpoint, String method, String data, String fromEmail, String queryStr) throws IOException {
    URL url = new URL("https://api.pagerduty.com/" + endpoint + (queryStr != null ? "?" + queryStr : ""));
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod(method);
    connection.setRequestProperty("Authorization", "Token token=" + this.token);
    connection.setRequestProperty("Accept", "application/vnd.pagerduty+json;version=2");

    if (fromEmail != null) {
      connection.setRequestProperty("From", fromEmail);
    }
    if ("POST".equals(method) || "PUT".equals(method)) {
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setDoOutput(true);
      OutputStream os = connection.getOutputStream();
      os.write(data.getBytes());
      os.flush();
      os.close();
    }

    int responseCode = connection.getResponseCode();
    if (responseCode >= 200 && responseCode < 300) { // success
      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer();

      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();
      return response.toString();
    } else {
      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer();

      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();
      throw new IOException("oops! Error " + responseCode + ", server response was " + response.toString());
    }
  }

  public String get(String endpoint, String queryStr) throws IOException {
    return this.request(endpoint, "GET", null, null, queryStr);
  }

  public String get(String endpoint) throws IOException {
    return this.get(endpoint, null);
  }

  public String post(String endpoint, String data, String fromEmail) throws IOException {
    return this.request(endpoint, "POST", data, fromEmail, null);
  }

  public String put(String endpoint, String data, String fromEmail) throws IOException {
    return this.request(endpoint, "PUT", data, fromEmail, null);
  }

  public String delete(String endpoint) throws IOException {
    return this.request(endpoint, "DELETE", null, null, null);
  }
}
