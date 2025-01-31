package org.jetlinks.community.visualization.web;

import org.jetlinks.community.test.spring.TestJetLinksController;
import org.jetlinks.community.visualization.entity.DataVisualizationEntity;
import org.jetlinks.community.visualization.service.DataVisualizationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(DataVisualizationController.class)
class DataVisualizationControllerTest extends TestJetLinksController {
    private static final String BASE_URL="/visualization";
    private static final String ID="testId";

    @Test
    void getService() {
        DataVisualizationService service = new DataVisualizationController(new DataVisualizationService()).getService();
        assertNotNull(service);
    }

    @Test
    void getByTypeAndTarget() {
        assertNotNull(client);
        client.get()
            .uri(BASE_URL+"/type/target")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        DataVisualizationEntity entity = new DataVisualizationEntity();
        entity.setId(ID);
        entity.setType("type");
        entity.setTarget("target");
        entity.setName("name");
        client.patch()
            .uri(BASE_URL)
            .bodyValue(entity)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();

        client.get()
            .uri(BASE_URL+"/type/target")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}