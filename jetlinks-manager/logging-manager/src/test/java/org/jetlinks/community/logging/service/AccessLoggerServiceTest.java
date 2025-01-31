package org.jetlinks.community.logging.service;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.elastic.search.index.ElasticIndex;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
class AccessLoggerServiceTest {

    @Test
    void getAccessLogger() throws Exception {
        ElasticSearchService elasticSearchService = Mockito.mock(ElasticSearchService.class);
        AccessLoggerService service = new AccessLoggerService();
        Class<? extends AccessLoggerService> serviceClass = service.getClass();
        Field searchService = serviceClass.getDeclaredField("searchService");
        searchService.setAccessible(true);
        searchService.set(service,elasticSearchService);
        Mockito.when(elasticSearchService.queryPager(Mockito.any(ElasticIndex.class),Mockito.any(QueryParam.class),Mockito.any(Class.class)))
            .thenReturn(Mono.just(PagerResult.of(1,new ArrayList<>())));
        assertNotNull(service);
        service.getAccessLogger(new QueryParam())
            .map(PagerResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }
}