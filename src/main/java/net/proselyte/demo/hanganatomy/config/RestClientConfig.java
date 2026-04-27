package net.proselyte.demo.hanganatomy.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient partnerRestClient(RestClient.Builder restClientBuilder, HangDemoProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.partner().connectTimeout());
        requestFactory.setReadTimeout(properties.partner().readTimeout());

        return restClientBuilder
                .baseUrl(properties.partner().baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService partnerExecutor() {
        return Executors.newFixedThreadPool(16, Thread.ofPlatform().name("partner-exec-", 0).factory());
    }

    @Bean(destroyMethod = "shutdown")
    ScheduledExecutorService timeoutScheduler() {
        return Executors.newScheduledThreadPool(2, Thread.ofPlatform().name("timeout-scheduler-", 0).factory());
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService budgetExecutor() {
        return Executors.newCachedThreadPool(Thread.ofPlatform().name("budget-exec-", 0).factory());
    }
}
