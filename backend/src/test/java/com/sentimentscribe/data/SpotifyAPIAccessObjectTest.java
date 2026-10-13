package com.sentimentscribe.data;

import com.sentimentscribe.config.SpotifyProperties;
import com.sentimentscribe.domain.SongRecommendation;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SpotifyAPIAccessObjectTest {

    @Test
    public void testJSONtoSongRecommendation_AllFieldsPresent() {
        JSONObject artist = new JSONObject().put("name", "Artist Name");
        JSONArray artists = new JSONArray().put(artist);
        JSONArray images = new JSONArray().put(new JSONObject().put("url", "http://image/cover.jpg"));
        JSONObject album = new JSONObject()
                .put("release_date", "2020-05-01")
                .put("images", images);
        JSONObject externalUrls = new JSONObject().put("spotify", "http://spotify/track");

        JSONObject track = new JSONObject()
                .put("name", "Song Name")
                .put("artists", artists)
                .put("album", album)
                .put("external_urls", externalUrls)
                .put("popularity", "87");

        SpotifyAPIAccessObject dao = new SpotifyAPIAccessObject(List.of("happy"),
                new SpotifyProperties("", ""));
        SongRecommendation rec = dao.JSONtoSongRecommendation(track);

        assertEquals("2020", rec.getReleaseYear());
        assertEquals("http://image/cover.jpg", rec.getImageUrl());
        assertEquals("Song Name", rec.getSongName());
        assertEquals("Artist Name", rec.getArtistName());
        assertEquals("87/100", rec.getPopularityScore());
        assertEquals("http://spotify/track", rec.getExternalUrl());
    }

    @Test
    public void testJSONtoSongRecommendation_MissingOptionalFields() {
        JSONObject track = new JSONObject()
                .put("name", "Unknown Song");

        SpotifyAPIAccessObject dao = new SpotifyAPIAccessObject(List.of("sad"),
                new SpotifyProperties("", ""));
        SongRecommendation rec = dao.JSONtoSongRecommendation(track);

        assertEquals("Unknown", rec.getReleaseYear());
        assertEquals("", rec.getImageUrl());
        assertEquals("Unknown Song", rec.getSongName());
        assertEquals("Unknown", rec.getArtistName());
        assertEquals("Unknown", rec.getPopularityScore());
        assertEquals("", rec.getExternalUrl());
    }

    @Test
    public void testFetchSongRecommendations_RealAPI_WhenCredentialsPresent() throws Exception {
        String clientId = System.getenv("SPOTIFY_CLIENT_ID");
        String clientSecret = System.getenv("SPOTIFY_CLIENT_SECRET");

        if (clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty()) {
            return;
        }

        SpotifyAPIAccessObject dao = new SpotifyAPIAccessObject(
                List.of("adventure", "friendship"),
                new SpotifyProperties(clientId, clientSecret));
        List<SongRecommendation> recs = dao.fetchSongRecommendations();
        assertNotNull(recs);
        assertFalse(recs.isEmpty());
        for (SongRecommendation rec : recs) {
            assertNotNull(rec.getSongName());
            assertNotNull(rec.getArtistName());
            System.out.println("Song: " + rec.getSongName() + " & Artist: " + rec.getArtistName());
        }
    }

    @Test
    public void searchUsesSupportedPageSize() throws Exception {
        JSONObject track = searchTrack("supported", 87);
        List<SongRecommendation> songs = fetchFromSearch(new JSONArray().put(track), true, List.of());
        assertEquals(List.of("supported"), songs.stream().map(SongRecommendation::getSongId).toList());
    }

    @Test
    public void searchKeepsTracksWithoutPopularityAndStillFiltersKnownLowScores() throws Exception {
        JSONArray items = new JSONArray()
                .put(searchTrack("missing", null))
                .put(searchTrack("null", JSONObject.NULL))
                .put(searchTrack("low", 10))
                .put(searchTrack("high", 87))
                .put(searchTrack("excluded", 90));
        List<SongRecommendation> songs = fetchFromSearch(items, false, List.of("excluded"));
        assertEquals(List.of("missing", "null", "high"), songs.stream().map(SongRecommendation::getSongId).toList());
        assertEquals("Unknown", songs.getFirst().getPopularityScore());
        assertEquals("Unknown", songs.get(1).getPopularityScore());
        assertEquals("87/100", songs.get(2).getPopularityScore());
    }

    private static JSONObject searchTrack(String id, Object popularity) {
        JSONObject track = new JSONObject().put("id", id).put("name", "Song " + id)
                .put("artists", new JSONArray().put(new JSONObject().put("name", "Artist " + id)))
                .put("album", new JSONObject().put("release_date", "2020-01-01"))
                .put("external_urls", new JSONObject().put("spotify", "https://open.spotify.com/track/" + id));
        if (popularity != null) track.put("popularity", popularity);
        return track;
    }

    @SuppressWarnings("unchecked")
    private static List<SongRecommendation> fetchFromSearch(JSONArray items, boolean checkLimit,
                                                           List<String> excludeIds) throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(200);
            if (request.uri().getPath().equals("/api/token")) {
                when(response.body()).thenReturn("{\"access_token\":\"nonsecret-test-token\"}");
            } else {
                if (checkLimit) assertTrue(request.uri().getQuery().contains("limit=10&"),
                        "Spotify search requests must fit the current 10-result maximum");
                when(response.body()).thenReturn(new JSONObject()
                        .put("tracks", new JSONObject().put("items", items)).toString());
            }
            return response;
        });
        try (MockedStatic<HttpClient> factory = mockStatic(HttpClient.class)) {
            factory.when(HttpClient::newHttpClient).thenReturn(client);
            return new SpotifyAPIAccessObject(List.of("happy"), new SpotifyProperties("fixture-id", "fixture-secret"))
                    .fetchSongRecommendations(excludeIds);
        }
    }
}

