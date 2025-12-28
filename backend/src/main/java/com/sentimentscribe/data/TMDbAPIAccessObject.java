package com.sentimentscribe.data;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.sentimentscribe.config.TmdbProperties;
import com.sentimentscribe.domain.MovieRecommendation;
import org.json.JSONArray;
import org.json.JSONObject;

public class TMDbAPIAccessObject {
    private final String apiKey;
    private static int limit = 4;
    private final List<String> terms;

    public TMDbAPIAccessObject(List<String> terms, TmdbProperties properties) {
        this.terms = terms;
        this.apiKey = properties.apiKey();
    }
    
    private List<String> getKeywordIds(List<String> terms) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("TMDb API key is not configured.");
        }
        List<String> ids = new ArrayList<>();
        HttpClient client = HttpClient.newHttpClient();

        for (String t : terms) {
            // System.out.print((t));
            String url = String.format(
                    "https://api.themoviedb.org/3/search/keyword?api_key=%s&query=%s&page=1",
                    apiKey,
                    URLEncoder.encode(t, StandardCharsets.UTF_8)
            );

            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                JSONArray results = new JSONObject(res.body()).optJSONArray("results");
                if (results != null && results.length() > 0) {
                    String id = String.valueOf(results.getJSONObject(0).optInt("id"));
                    ids.add(id);
                }
            }
            else {
                throw new Exception("TMDb keyword search failed: " + res.statusCode());
            }
        }
        return ids;
    }

    private List<JSONObject> discoverMovies(List<String> ids,
                                            Set<String> excludeIds)
            throws Exception {
        if (ids == null || ids.isEmpty()) return List.of();

        HttpClient client = HttpClient.newHttpClient();
        List<JSONObject> collected = new ArrayList<>();
        Set<String> seenTitles = new HashSet<>();
        Set<String> seenIds = new HashSet<>();

        if (excludeIds != null && !excludeIds.isEmpty()) {
            seenIds.addAll(excludeIds);
        }

        List<String> queries = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                queries.add(ids.get(i) + "," + ids.get(j));
            }
        }
        queries.add(String.join("|", ids));

        int maxPages = 3;

        for (String keywordStr : queries) {
            if (collected.size() >= limit) break;
            String encoded = URLEncoder.encode(keywordStr, StandardCharsets.UTF_8);

            for (int page = 1; page <= maxPages && collected.size() < limit; page++) {
                String url = String.format(
                        "https://api.themoviedb.org/3/discover/movie?api_key=%s&with_keywords=%s&include_adult=false" +
                                "&sort_by=vote_average.desc&vote_count.gte=350&language=en-US&page=%d",
                        apiKey,
                        encoded,
                        page
                );

                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() < 200 || res.statusCode() >= 300) {
                    throw new Exception("TMDb discover failed: " + res.statusCode());
                }

                JSONArray results = new JSONObject(res.body()).optJSONArray("results");
                if (results == null || results.length() == 0) {
                    break;
                }

                for (int i = 0; i < results.length(); i++) {
                    if (collected.size() >= limit) break;

                    JSONObject movie = results.getJSONObject(i);
                    String title = movie.optString("title", "").trim();
                    String id = String.valueOf(movie.optInt("id", 0));

                    if (id.isEmpty() || "0".equals(id)) {
                        continue;
                    }

                    if (excludeIds != null && excludeIds.contains(id)) {
                        continue;
                    }

                    // Ensure we don't collect movies that share the same title
                    if (title.isEmpty() || seenTitles.contains(title) || seenIds.contains(id)) {
                        continue;
                    }

                    seenTitles.add(title);
                    seenIds.add(id);
                    collected.add(movie);
                }
            }
        }

        return collected.subList(0, Math.min(collected.size(), limit));
    }
    
    public MovieRecommendation JSONtoMovieRecommendation(JSONObject movie) {
        String movieId = String.valueOf(movie.optInt("id", 0));
        String title = movie.optString("title", "-");
        String year = movie.optString("release_date", "-");
        year = year.isEmpty() ? "—" : year.substring(0, Math.min(4, year.length()));
        String voteAvg = movie.optString("vote_average", "—");
        voteAvg = voteAvg + "/10";
        String overview = movie.optString("overview", "");
        String posterPath = movie.optString("poster_path", null);
        String posterUrl = (posterPath == null || posterPath.isEmpty())
                ? "-"
                : "https://image.tmdb.org/t/p/original" + posterPath;
        System.out.println(title + " " + year + " " + voteAvg);
        return new MovieRecommendation(movieId, year, posterUrl, title, voteAvg, overview);
    }
    
    public List<MovieRecommendation> fetchMovieRecommendations() throws Exception {
        return fetchMovieRecommendations(List.of());
    }

    public List<MovieRecommendation> fetchMovieRecommendations(List<String> excludeMovieIds) throws Exception {
        try {
            List<String> ids = getKeywordIds(terms);
            Set<String> exclude = excludeMovieIds == null
                    ? new HashSet<>()
                    : new HashSet<>(excludeMovieIds);
            List<JSONObject> movies = discoverMovies(ids, exclude);
            List<MovieRecommendation> movieList = new ArrayList<>();
            for (JSONObject movie : movies) {
                movieList.add(JSONtoMovieRecommendation(movie));
            }
            return movieList;
        }
        catch (Exception error) {
            throw new Exception("Failed to fetch movie recommendations: ", error);
        }
    }

//    public static void main(String[] args) {
//        List<String> terms = List.of("adventure", "friendship", "heroism");
//        TMDbAPIAccessObject DAO = new TMDbAPIAccessObject(terms, new TmdbProperties("api-key"));
//        try {
//            List<MovieRecommendation> recommendations = DAO.fetchMovieRecommendations();
//            for (MovieRecommendation rec : recommendations) {
//                System.out.println(rec.getMovieTitle());
//                System.out.println(rec.getReleaseYear());
//                System.out.println(rec.getMovieRating());
//                System.out.println(rec.getImageUrl());
//                System.out.println();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}

