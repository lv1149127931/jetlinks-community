package org.jetlinks.community.test.spring;

import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.authorization.ReactiveAuthenticationSupplier;
import org.jetlinks.community.elastic.search.timeseries.ElasticSearchTimeSeriesManager;
import org.jetlinks.community.micrometer.MeterRegistryManager;
import org.jetlinks.community.test.utils.ContainerUtils;
import org.jetlinks.community.test.web.TestAuthentication;
import org.jetlinks.community.timeseries.micrometer.TimeSeriesMeterRegistryConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@ImportAutoConfiguration(value = {
    TransactionAutoConfiguration.class,

    WebClientAutoConfiguration.class, RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class,
    R2dbcTransactionManagerAutoConfiguration.class,
    ReactiveElasticsearchRestClientAutoConfiguration.class,

})
@EnableTransactionManagement(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Testcontainers
@ComponentScan(value = {
    "org.jetlinks.community.logging",
    "org.jetlinks.community.elastic",

}, basePackageClasses = {
    MeterRegistryManager.class,
    TimeSeriesMeterRegistryConfiguration.class,
    ElasticSearchTimeSeriesManager.class,
}, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {

    })
}
)
public class TestJetLinksController {

    static {

        System.setProperty("spring.r2dbc.url", "r2dbc:h2:mem:///./data/h2db/jetlinks");
        System.setProperty("spring.r2dbc.username", "sa");

    }

    @Container
    static GenericContainer<?> redis = ContainerUtils.newRedis();

    @Container
    static GenericContainer<?> elasticSearch = ContainerUtils.newElasticSearch();


    @Autowired
    protected WebTestClient client;

    @BeforeAll
    static void initAll() {
        System.setProperty("spring.redis.port", String.valueOf(redis.getMappedPort(6379)));
        System.setProperty("spring.redis.host", "127.0.0.1");


        System.setProperty("spring.data.elasticsearch.client.reactive.endpoints",
            elasticSearch.getHost()+":"+elasticSearch.getMappedPort(9200));
        elasticSearch.start();
        System.setProperty("device.message.writer.time-series", "true");
//        System.setProperty("spring.data elasticsearch.client.reactive.endpoints","localhost:9200");
        System.setProperty("spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS", "true");
        System.setProperty("jetlinks.protocol.spi.enabled", "true");
    }

    @BeforeEach
    void init() {
        ReactiveAuthenticationHolder.setSupplier(new ReactiveAuthenticationSupplier() {
            @Override
            public Mono<Authentication> get() {
                TestAuthentication authentication = new TestAuthentication("test");
                initAuth(authentication);
                return Mono.just(authentication);
            }

            @Override
            public Mono<Authentication> get(String userId) {
                TestAuthentication authentication = new TestAuthentication(userId);
                initAuth(authentication);
                return Mono.just(authentication);
            }
        });
    }

    protected void initAuth(TestAuthentication authentication) {
        authentication.addPermission("test", "test");
        authentication.addDimension("test", "test");
        authentication.addDimension("org", "org");
    }
}
