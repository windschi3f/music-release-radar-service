package com.windschief;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SpotifyTokenRetrieval {
    private static final String CLIENT_ID = "your_client_id";
    private static final String CLIENT_SECRET = "your_client_secret";
    private static final String REDIRECT_URI = "http://localhost:8080/callback";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";

    public static void main(String[] args) throws Exception {
        String authorizationCode = getAuthorizationCode();
        if (authorizationCode != null) {
            String accessToken = getAccessToken(authorizationCode);
            System.out.println("Access Token: " + accessToken);
        }
    }

    private static String getAuthorizationCode() {
        String url = AUTH_URL + "?client_id=" + CLIENT_ID + "&response_type=code&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) + "&scope=user-read-private";
        System.out.println("Please open the following URL in your browser and authorize the application:");
        System.out.println(url);
        System.out.println("After authorization, you will be redirected to a URL. Please enter the 'code' parameter from that URL:");

        Scanner scanner = new Scanner(System.in);
        String redirectedUrl = scanner.nextLine();
        scanner.close();

        if (redirectedUrl.contains("code=")) {
            return redirectedUrl.split("code=")[1].split("&")[0];
        } else {
            System.out.println("Invalid URL or code parameter missing.");
            return null;
        }
    }

    private static String getAccessToken(String authorizationCode) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String form = buildForm(Map.of(
                "grant_type", "authorization_code",
                "code", authorizationCode,
                "redirect_uri", REDIRECT_URI,
                "client_id", CLIENT_ID,
                "client_secret", CLIENT_SECRET
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.body());
            return jsonNode.get("access_token").asText();
        } else {
            throw new RuntimeException("Failed to get access token: " + response.body());
        }
    }

    private static String buildForm(Map<String, String> data) {
        StringJoiner sj = new StringJoiner("&");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            sj.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sj.toString();
    }
}

