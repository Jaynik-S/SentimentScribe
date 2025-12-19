package com.moodverse.config;

import com.moodverse.data.DBNoteDataObject;
import com.moodverse.data.NLPAnalysisDataAccessObject;
import com.moodverse.data.NLPKeywordExtractor;
import com.moodverse.data.RecommendationAPIAccessObject;
import com.moodverse.data.VerifyPasswordDataAccessObject;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class AppConfig {

    @Bean
    public StanfordCoreNLP stanfordCoreNLP() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        return new StanfordCoreNLP(props);
    }

    @Bean
    public NLPAnalysisDataAccessObject nlpAnalysisDataAccessObject(StanfordCoreNLP pipeline) {
        return new NLPAnalysisDataAccessObject(pipeline);
    }

    @Bean
    public NLPKeywordExtractor nlpKeywordExtractor(NLPAnalysisDataAccessObject analysisDao) {
        return new NLPKeywordExtractor(analysisDao);
    }

    @Bean
    public DBNoteDataObject dbNoteDataObject() {
        return new DBNoteDataObject();
    }

    @Bean
    public VerifyPasswordDataAccessObject verifyPasswordDataAccessObject(AuthProperties authProperties) {
        return new VerifyPasswordDataAccessObject(authProperties);
    }

    @Bean
    public RecommendationAPIAccessObject recommendationAPIAccessObject(NLPAnalysisDataAccessObject analysisDao,
                                                                       SpotifyProperties spotifyProperties,
                                                                       TmdbProperties tmdbProperties) {
        return new RecommendationAPIAccessObject(analysisDao, spotifyProperties, tmdbProperties);
    }
}
