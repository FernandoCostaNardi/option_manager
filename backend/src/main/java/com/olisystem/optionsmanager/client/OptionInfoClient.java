package com.olisystem.optionsmanager.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OptionInfoClient {
  private final HttpClient httpClient;
  private static final String BASE_URL = "https://opcoes.net.br/";

  public OptionInfoClient() {
    this.httpClient = HttpClient.newHttpClient();
  }

  /** Busca o HTML da página da opção */
  public String fetchOptionHtml(String optionCode) throws Exception {
    String url = BASE_URL + optionCode;

    HttpRequest request = createRequest(url);
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    return handleResponse(response, url);
  }

  private HttpRequest createRequest(String url) {
    return HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("User-Agent", "Mozilla/5.0")
        .GET()
        .build();
  }

  private String handleResponse(HttpResponse<String> response, String originalUrl)
      throws Exception {
    int statusCode = response.statusCode();

    if (statusCode == 301 || statusCode == 302) {
      return handleRedirect(response, originalUrl);
    }

    if (statusCode != 200) {
      throw new Exception("Falha ao obter dados da opção. Código: " + statusCode);
    }

    return response.body();
  }

  private String handleRedirect(HttpResponse<String> response, String originalUrl)
      throws Exception {
    String location =
        response
            .headers()
            .firstValue("Location")
            .orElseThrow(() -> new Exception("Redirecionamento sem cabeçalho Location"));

    String fullRedirectUrl = buildFullRedirectUrl(location, originalUrl);

    HttpRequest redirectRequest = createRequest(fullRedirectUrl);
    HttpResponse<String> redirectResponse =
        httpClient.send(redirectRequest, HttpResponse.BodyHandlers.ofString());

    if (redirectResponse.statusCode() != 200) {
      throw new Exception(
          "Falha ao obter dados da opção após redirecionamento. Código: "
              + redirectResponse.statusCode());
    }

    return redirectResponse.body();
  }

  private String buildFullRedirectUrl(String location, String originalUrl) {
    if (location.startsWith("http")) {
      return location;
    }

    URI originalUri = URI.create(originalUrl);
    String scheme = originalUri.getScheme();
    String host = originalUri.getHost();
    int port = originalUri.getPort();
    String portPart = port != -1 ? ":" + port : "";

    if (!location.startsWith("/")) {
      location = "/" + location;
    }

    return scheme + "://" + host + portPart + location;
  }
}
