package org.jetlinks.community.network.manager.service;

import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.gateway.supports.DeviceGatewayProperties;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceGatewayConfigServiceTest {

    @Test
    void getProperties() {
        DeviceGatewayService gatewayService = Mockito.mock(DeviceGatewayService.class);


        DeviceGatewayConfigService service = new DeviceGatewayConfigService(gatewayService);
        assertNotNull(service);

        DeviceGatewayEntity entity = new DeviceGatewayEntity();
        entity.setId("test");
        entity.setNetworkId("test");
        Mockito.when(gatewayService.findById(Mockito.anyString()))
            .thenReturn(Mono.just(entity));

        service.getProperties("test")
            .map(DeviceGatewayProperties::getNetworkId)
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        Mockito.when(gatewayService.findById(Mockito.anyString()))
            .thenReturn(Mono.empty());
        service.getProperties("test")
            .map(DeviceGatewayProperties::getNetworkId)
            .as(StepVerifier::create)
            .expectError(NotFoundException.class)
            .verify();
    }
}