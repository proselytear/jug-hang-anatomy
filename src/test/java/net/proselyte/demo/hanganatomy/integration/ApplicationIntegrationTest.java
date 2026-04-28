package net.proselyte.demo.hanganatomy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import net.proselyte.demo.hanganatomy.dto.AdminStatusResponse;
import net.proselyte.demo.hanganatomy.dto.DashboardResponse;
import net.proselyte.demo.hanganatomy.repository.DashboardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApplicationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DashboardRepository dashboardRepository;

    @Test
    void heavyEndpointReturnsResponse() {
        ResponseEntity<DashboardResponse> response = localRestClient().get()
                .uri("/api/v1/dashboard/{id}", DASHBOARD_ID)
                .retrieve()
                .toEntity(DashboardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(DASHBOARD_ID);
    }

    @Test
    void adminEndpointReturnsResponse() {
        ResponseEntity<AdminStatusResponse> response = localRestClient().get()
                .uri("/api/v1/admin/status")
                .retrieve()
                .toEntity(AdminStatusResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("GREEN");
    }

    @Test
    void slowPartnerStillReturnsLivePayloadWithoutTimeoutProtection() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");
        storeSnapshot(id);
        stubPartnerSlow(2500);

        ResponseEntity<DashboardResponse> response = localRestClient().get()
                .uri("/api/v1/dashboard/{id}", id)
                .retrieve()
                .toEntity(DashboardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().outcome()).isEqualTo("success");
        assertThat(response.getBody().partner().source()).isEqualTo("live");
    }

    @Test
    void flywayLoadsSeedData() {
        assertThat(dashboardRepository.sampleIds(10)).hasSize(10);
    }

    @Test
    void actuatorHealthAvailable() {
        ResponseEntity<String> response = localRestClient().get()
                .uri("/actuator/health")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
