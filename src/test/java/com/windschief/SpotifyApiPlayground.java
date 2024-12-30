package com.windschief;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.windschief.spotify.model.AlbumsResponse;
import com.windschief.spotify.model.SearchResponse;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class SpotifyApiPlayground {
        private static final String ACCESS_TOKEN = "access_token";
        private static final String SPOTIFY_API_BASE_URL = "https://api.spotify.com/v1";
        private static final ObjectMapper mapper = new ObjectMapper();

        public static void main(String[] args) throws Exception {
                HttpClient client = HttpClient.newHttpClient();

                testSearch(client, "artist:Miles Davis year:1959", "track", "US", 10);
                getArtistAlbums("0kbYTNQb4Pb1rPbbaF0pT4", client);
                getUserPlaylists(client);
        }

        private static void testSearch(HttpClient client,
                        String query, String type, String market, int limit) throws Exception {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = String.format("%s/search?q=%s&type=%s&market=%s&limit=%d",
                                SPOTIFY_API_BASE_URL, encodedQuery, type, market, limit);

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                                .GET()
                                .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                SearchResponse searchResponse = mapper.readValue(response.body(), SearchResponse.class);

                searchResponse.tracks().items().forEach(track -> System.out.printf("Track: %s by %s%n",
                                track.name(),
                                track.artists().get(0).id()));
        }

        private static void getArtistAlbums(String artistId, HttpClient client) throws Exception {
                String url = String.format("%s/artists/%s/albums?include_groups=album,single",
                                SPOTIFY_API_BASE_URL, artistId);

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                                .GET()
                                .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                AlbumsResponse albumsResponse = mapper.readValue(response.body(), AlbumsResponse.class);

                albumsResponse.items().forEach(
                                album -> System.out.printf("Album: %s (%s)%n", album.name(), album.release_date()));
        }

        private static void getUserPlaylists(HttpClient client) throws Exception {
                String url = String.format("%s/me/playlists", SPOTIFY_API_BASE_URL);

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                                .GET()
                                .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println(response.body());
        }
}