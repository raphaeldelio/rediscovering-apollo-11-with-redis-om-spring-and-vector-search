package dev.raphaeldelio.redisapollo.semanticcache;

import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.SearchStream;
import com.redis.om.spring.tuple.Fields;
import com.redis.om.spring.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SearchSemanticCacheService {

    private static final Logger logger = LoggerFactory.getLogger(SearchSemanticCacheService.class);
    private final EntityStream entityStream;
    private final SearchSemanticCacheRepository searchSemanticCacheRepository;

    public SearchSemanticCacheService(EntityStream entityStream, SearchSemanticCacheRepository searchSemanticCacheRepository) {
        this.entityStream = entityStream;
        this.searchSemanticCacheRepository = searchSemanticCacheRepository;
    }

    public List<Pair<SearchSemanticCache, Double>> getCacheResponse(byte[] embedding, boolean isQuestion) {
        SearchStream<SearchSemanticCache> stream = entityStream.of(SearchSemanticCache.class);
        return stream
                .filter(SearchSemanticCache$.EMBEDDED_QUERY.knn(1, embedding))
                .filter(SearchSemanticCache$.QUESTION.eq(isQuestion))
                .sorted(SearchSemanticCache$._EMBEDDED_QUERY_SCORE)
                .map(Fields.of(SearchSemanticCache$._THIS, SearchSemanticCache$._EMBEDDED_QUERY_SCORE))
                .collect(Collectors.toList());
    }

    public void cacheResponse(String query, String answer, boolean isQuestion) {
        SearchSemanticCache cache = new SearchSemanticCache(
                query,
                answer,
                isQuestion
        );
        cache.setId(UUID.randomUUID().toString());
        cache.setQuery(query);
        cache.setAnswer(answer);
        cache.setQuestion(isQuestion);
        searchSemanticCacheRepository.save(cache);
    }

    public void clearCache() {
        searchSemanticCacheRepository.deleteAll();
        logger.info("Cache cleared");
    }
}