package com.pagerduty.martin;

import java.util.Scanner;
import java.net.URL;
import java.net.MalformedURLException;

public class Config {
  public String webhookURL;
  public String webhookPath;
  public int listenPort;
  public String restToken;
  public String fromEmail;
  public Scanner in;

  public Config() {
    this.in = new Scanner(System.in);
    this.prompt();
  }

  public boolean promptForWebhookURL() {
    try {
      System.out.print("What URL do you want PagerDuty to send webhooks to? ");
      String webhookURL = this.in.nextLine();
      URL url = new URL(webhookURL);
      String path = url.getPath();
      this.webhookURL = webhookURL;
      this.webhookPath = path;
      return true;
    } catch (MalformedURLException e) {
      return false;
    }
  }

  public boolean promptForPort() {
    try {
      System.out.print("What port do you want to listen for webhooks on? ");
      int port = this.in.nextInt();
      this.listenPort = port;
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean promptForToken() {
    try {
      System.out.print("Enter a PagerDuty REST API token: ");
      String token = this.in.nextLine();
      this.restToken = token;
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean promptForEmail() {
    try {
      System.out.print("Enter your PagerDuty login email: ");
      String email = this.in.nextLine();
      this.fromEmail = email;
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void prompt() {
    while (!promptForToken());
    while (!promptForEmail());
    while (!promptForWebhookURL());
    while (!promptForPort());
  }
}