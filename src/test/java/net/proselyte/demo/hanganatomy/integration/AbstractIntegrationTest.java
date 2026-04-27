package net.proselyte.demo.hanganatomy.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.util.UUID;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import net.proselyte.demo.hanganatomy.client.PartnerPayload;
import net.proselyte.demo.hanganatomy.repository.PartnerSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    protected static final UUID DASHBOARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("hanganatomy")
            .withUsername("hanganatomy")
            .withPassword("hanganatomy");

    @RegisterExtension
    static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
            .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PartnerSnapshotRepository partnerSnapshotRepository;

    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("hang.partner.base-url", () -> WIREMOCK.baseUrl() + "/partner-api/v1");
        registry.add("management.otlp.tracing.endpoint", () -> "http://127.0.0.1:1/v1/traces");
        registry.add("logging.file.name", () -> "build/test-logs/hang-anatomy-test.json.log");
        registry.add("hang.admin.db-timeout", () -> "2s");
        registry.add("hang.health.readiness-timeout", () -> "2s");
    }

    @BeforeEach
    void resetWireMock() {
        WIREMOCK.resetAll();
        stubPartnerNormal();
    }

    protected void stubPartnerNormal() {
        WIREMOCK.stubFor(get(urlMatching("/partner-api/v1/dashboard/.*"))
                .willReturn(okJson("""
                        {
                          "externalRef": "partner-001",
                          "partnerMode": "normal",
                          "score": 87.5,
                          "recommendation": "ok",
                          "payload": {
                            "externalRef": "partner-001",
                            "partnerMode": "normal",
                            "score": 87.5,
                            "recommendation": "ok"
                          }
                        }
                        """)));
    }

    protected void stubPartnerSlow(int delayMillis) {
        WIREMOCK.stubFor(get(urlMatching("/partner-api/v1/dashboard/.*"))
                .willReturn(aResponse()
                        .withFixedDelay(delayMillis)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "externalRef": "partner-slow",
                                  "partnerMode": "slow-partner",
                                  "score": 10.0,
                                  "recommendation": "slow",
                                  "payload": {
                                    "externalRef": "partner-slow",
                                    "partnerMode": "slow-partner",
                                    "score": 10.0,
                                    "recommendation": "slow"
                                  }
                                }
                                """)));
    }

    protected void storeSnapshot(UUID dashboardId) {
        partnerSnapshotRepository.saveSnapshot(
                dashboardId,
                new PartnerPayload(
                        "snapshot",
                        "snapshot-ref",
                        "snapshot",
                        12.0d,
                        "cached",
                        objectMapper.createObjectNode()
                                .put("partnerMode", "snapshot")
                                .put("score", 12.0d)
                                .put("recommendation", "cached")
                )
        );
    }

    protected RestClient localRestClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }
}
