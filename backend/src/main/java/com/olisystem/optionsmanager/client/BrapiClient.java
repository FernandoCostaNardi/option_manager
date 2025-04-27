package com.olisystem.optionsmanager.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class BrapiClient {
  private final String token;

  public BrapiClient(String token) {
    this.token = token;
  }

  public JSONObject getQuoteJson(String ticker) throws Exception {
    String url = buildBrapiUrl(ticker);
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setRequestMethod("GET");

    int status = connection.getResponseCode();
    if (status == 200) {
      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      StringBuilder content = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        content.append(line);
      }
      in.close();
      connection.disconnect();
      return new JSONObject(content.toString());
    } else {
      connection.disconnect();
      throw new Exception("Erro ao buscar informações do ativo via HTTP: " + status);
    }
  }

  private String buildBrapiUrl(String ticker) {
    return "https://brapi.dev/api/quote/" + ticker + "?token=" + token;
  }
}
