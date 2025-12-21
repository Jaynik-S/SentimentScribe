package com.sentimentscribe.web;

import com.sentimentscribe.domain.MovieRecommendation;
import com.sentimentscribe.domain.SongRecommendation;
import com.sentimentscribe.service.RecommendationService;
import com.sentimentscribe.service.ServiceResult;
import com.sentimentscribe.usecase.get_recommendations.GetRecommendationsOutputData;
import com.sentimentscribe.web.dto.ErrorResponse;
import com.sentimentscribe.web.dto.MovieRecommendationResponse;
import com.sentimentscribe.web.dto.RecommendationRequest;
import com.sentimentscribe.web.dto.RecommendationResponse;
import com.sentimentscribe.web.dto.SongRecommendationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationsController {

    private final RecommendationService recommendationService;

    public RecommendationsController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ResponseEntity<?> recommend(@RequestBody RecommendationRequest request) {
        ServiceResult<GetRecommendationsOutputData> result = recommendationService.recommend(request.text());
        if (!result.success()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(result.errorMessage()));
        }
        GetRecommendationsOutputData data = result.data();
        List<SongRecommendationResponse> songs = data.getSongRecommendations()
                .stream()
                .map(RecommendationsController::toSongResponse)
                .toList();
        List<MovieRecommendationResponse> movies = data.getMovieRecommendations()
                .stream()
                .map(RecommendationsController::toMovieResponse)
                .toList();
        return ResponseEntity.ok(new RecommendationResponse(data.getKeywords(), songs, movies));
    }

    private static SongRecommendationResponse toSongResponse(SongRecommendation song) {
        return new SongRecommendationResponse(
                song.getReleaseYear(),
                song.getImageUrl(),
                song.getSongName(),
                song.getArtistName(),
                song.getPopularityScore(),
                song.getExternalUrl()
        );
    }

    private static MovieRecommendationResponse toMovieResponse(MovieRecommendation movie) {
        return new MovieRecommendationResponse(
                movie.getReleaseYear(),
                movie.getImageUrl(),
                movie.getMovieTitle(),
                movie.getMovieRating(),
                movie.getOverview()
        );
    }
}
