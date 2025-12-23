package com.sentimentscribe.config;

import com.sentimentscribe.data.NLPAnalysisDataAccessObject;
import com.sentimentscribe.data.NLPKeywordExtractor;
import com.sentimentscribe.data.RecommendationAPIAccessObject;
import com.sentimentscribe.persistence.postgres.PostgresDiaryEntryRepositoryAdapter;
import com.sentimentscribe.persistence.postgres.PostgresVerifyPasswordDataAccessObject;
import com.sentimentscribe.persistence.postgres.StoragePathGenerator;
import com.sentimentscribe.persistence.postgres.repo.DiaryEntryJpaRepository;
import com.sentimentscribe.persistence.postgres.repo.UserJpaRepository;
import com.sentimentscribe.usecase.save_entry.SaveEntryKeywordExtractor;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

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
    @Profile("postgres")
    public StoragePathGenerator storagePathGenerator() {
        return new StoragePathGenerator();
    }

    @Bean
    @Profile("postgres")
    public PostgresDiaryEntryRepositoryAdapter postgresDiaryEntryRepositoryAdapter(
            DiaryEntryJpaRepository diaryEntryRepository,
            UserJpaRepository userJpaRepository,
            SaveEntryKeywordExtractor keywordExtractor,
            StoragePathGenerator storagePathGenerator) {
        return new PostgresDiaryEntryRepositoryAdapter(
                diaryEntryRepository,
                userJpaRepository,
                keywordExtractor,
                storagePathGenerator
        );
    }

    @Bean
    @Profile("postgres")
    public PostgresVerifyPasswordDataAccessObject postgresVerifyPasswordDataAccessObject(
            UserJpaRepository userJpaRepository) {
        return new PostgresVerifyPasswordDataAccessObject(userJpaRepository);
    }

    @Bean
    public RecommendationAPIAccessObject recommendationAPIAccessObject(NLPAnalysisDataAccessObject analysisDao,
                                                                       SpotifyProperties spotifyProperties,
                                                                       TmdbProperties tmdbProperties) {
        return new RecommendationAPIAccessObject(analysisDao, spotifyProperties, tmdbProperties);
    }
}
