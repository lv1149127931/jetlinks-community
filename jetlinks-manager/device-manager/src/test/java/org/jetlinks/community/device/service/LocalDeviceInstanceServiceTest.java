package org.jetlinks.community.device.service;

import org.hswebframework.ezorm.core.MethodReferenceColumn;
import org.hswebframework.ezorm.core.NestConditional;
import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.ezorm.rdb.mapping.defaults.DefaultReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.device.entity.DeviceStateInfo;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.response.DeviceDeployResult;
import org.jetlinks.community.device.response.DeviceDetail;
import org.jetlinks.core.defaults.DefaultDeviceOperator;
import org.jetlinks.core.device.*;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.supports.cluster.CompositeDeviceStateChecker;
import org.jetlinks.supports.config.InMemoryConfigStorage;
import org.jetlinks.supports.config.InMemoryConfigStorageManager;
import org.jetlinks.supports.test.InMemoryDeviceRegistry;
import org.jetlinks.supports.test.MockProtocolSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LocalDeviceInstanceServiceTest {
    public static final String ID_1 = "test001";

    void initEntity(DeviceInstanceEntity deviceInstanceEntity){
        Map<String, Object> map = new HashMap<>();
        map.put("test", "test");
        deviceInstanceEntity.setId(ID_1);
        deviceInstanceEntity.setConfiguration(map);
        deviceInstanceEntity.setProductId("test");
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setDeriveMetadata("" +
            "{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[]}");
        deviceInstanceEntity.setProductName("TCP测试");
        deviceInstanceEntity.setProductId("1236859833832701954");
        deviceInstanceEntity.setName("TCP-setvice");
        deviceInstanceEntity.setCreatorId("1199596756811550720");
        deviceInstanceEntity.setCreatorName("超级管理员");
    }
    void initProductEntity(DeviceProductEntity deviceProductEntity){
        deviceProductEntity.setId("test");
        deviceProductEntity.setName("test");
        deviceProductEntity.setState((byte) 1);
        deviceProductEntity.setMessageProtocol("test");
        deviceProductEntity.setProtocolName("test");
        deviceProductEntity.setTransportProtocol("TCP");
    }
    @Test
    void save() {
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        DefaultReactiveRepository<DeviceInstanceEntity,String> repository= new DefaultReactiveRepository(Mockito.mock(DatabaseOperator.class),"table",String.class,Mockito.mock(ResultWrapper.class)){
            @Override
            public Mono<SaveResult> save(Publisher data) {
                data.subscribe(new Subscriber() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscription.request(1L);
                    }

                    @Override
                    public void onNext(Object o) {
                        System.out.println(o);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.getMessage();
                    }

                    @Override
                    public void onComplete() {
                        onNext(new Object());
                    }
                });
                return Mono.just(SaveResult.of(1,0));
            }
        };
        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initEntity(deviceInstanceEntity);
        assertNotNull(service);
        service.save(Mono.just(deviceInstanceEntity))
            .map(SaveResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

    }

    @Test
    void resetConfiguration() {
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        Map<String, Object> map = new HashMap<>();
        map.put("test", "test");
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(ID_1);
        deviceInstanceEntity.setConfiguration(map);
        deviceInstanceEntity.setProductId("test");
        HashMap<String, Object> map1 = new HashMap<>();
        map1.put("test", "test");
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId("test");
        deviceProductEntity.setConfiguration(map1);

        Mockito.when(repository.findById(Mockito.any(String.class))).thenReturn(Mono.just(deviceInstanceEntity));
        Mockito.when(deviceProductService.findById(Mockito.any(String.class))).thenReturn(Mono.just(deviceProductEntity));

        DeviceOperator operator = InMemoryDeviceRegistry.create().register(deviceInstanceEntity.toDeviceInfo()).block();
        assertNotNull(operator);
        operator.setConfigs(deviceProductEntity.getConfiguration());

        Mockito.when(registry.getDevice(Mockito.any(String.class))).thenReturn(Mono.just(operator));

        ReactiveUpdate<DeviceInstanceEntity> update = Mockito.mock(ReactiveUpdate.class);
        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(MethodReferenceColumn.class))).thenReturn(update);
        Mockito.when(update.where(Mockito.any(MethodReferenceColumn.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));

        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);
        //返回的设备Configuration为空
        service.resetConfiguration(ID_1)
            .map(Map::size)
            .as(StepVerifier::create)
            .expectNext(0)
            .verifyComplete();

        //返回的设备Configuration为不为空
        map.put("test1", "test1");
        service.resetConfiguration(ID_1)
            .map(m -> m.get("test1"))
            .as(StepVerifier::create)
            .expectNext("test1")
            .verifyComplete();

        //产品的配置为空,不会清楚设备关于产品的配置
        map.put("test", "test");
        HashMap<String, Object> map2 = new HashMap<>();
        deviceProductEntity.setConfiguration(map2);
        service.resetConfiguration(ID_1)
            .map(m -> m.get("test"))
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();
    }

    @Test
    void deploy() {
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initEntity(deviceInstanceEntity);
        Mockito.when(repository.findById(Mockito.any(String.class))).thenReturn(Mono.just(deviceInstanceEntity));
        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry();
        Mono<DeviceOperator> register = inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo().addConfig("state", org.jetlinks.core.device.DeviceState.online));
        DeviceOperator deviceOperator = register.block();
        assertNotNull(deviceOperator);

        Mockito.when(registry.register(Mockito.any(DeviceInfo.class))).thenReturn(Mono.just(deviceOperator));
        ReactiveUpdate update = Mockito.mock(ReactiveUpdate.class);
        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.where()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.in(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Collection.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));

        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);

        service.deploy(ID_1)
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .map(DeviceDeployResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();


        service.deploy(ID_1)
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .map(DeviceDeployResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();


        deviceOperator.putState(org.jetlinks.core.device.DeviceState.unknown).subscribe();
        service.deploy(ID_1)
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .map(DeviceDeployResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        deviceOperator.putState(org.jetlinks.core.device.DeviceState.noActive).subscribe();
        service.deploy(ID_1)
            .switchIfEmpty(Mono.error(NotFoundException::new))
            .map(DeviceDeployResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        Mockito.when(update.execute()).thenReturn(Mono.error(new IllegalArgumentException()));
        service.deploy(ID_1)
            .map(DeviceDeployResult::getTotal)
            .as(StepVerifier::create)
            .expectNext(0)
            .verifyComplete();

    }


    @Test
    void cancelDeploy() {
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initEntity(deviceInstanceEntity);
        Mockito.when(repository.findById(Mockito.any(Mono.class))).thenReturn(Mono.just(deviceInstanceEntity));

        ReactiveUpdate update = Mockito.mock(ReactiveUpdate.class);
        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));

        Mockito.when(update.where()).thenReturn(update);
        Mockito.when(update.in(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Collection.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));

        Mockito.when(registry.unregisterDevice(Mockito.any(String.class))).thenReturn(Mono.empty());

        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);

        service.cancelDeploy(ID_1)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        service.unregisterDevice(ID_1)
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();

        service.unregisterDevice(Mono.just(ID_1))
            .as(StepVerifier::create)
            .expectNext(1)
            .verifyComplete();
    }

    DefaultDeviceOperator createDeviceOperator(CompositeDeviceStateChecker stateChecker){
        InMemoryConfigStorage inMemoryConfigStorage = new InMemoryConfigStorage();
        inMemoryConfigStorage.setConfig("test", "test");
        inMemoryConfigStorage.setConfig("state", 1);

        Mockito.when(stateChecker.checkState(Mockito.any(DeviceOperator.class))).thenReturn(Mono.just((byte) 1));

        InMemoryConfigStorageManager inMemoryConfigStorageManager = Mockito.mock(InMemoryConfigStorageManager.class);
        Mockito.when(inMemoryConfigStorageManager.getStorage(Mockito.anyString()))
            .thenReturn(Mono.just(inMemoryConfigStorage));

        DefaultDeviceOperator defaultDeviceOperator =
            new DefaultDeviceOperator(ID_1, new MockProtocolSupport(), inMemoryConfigStorageManager
                , new StandaloneDeviceMessageBroker(), new InMemoryDeviceRegistry(),
                new CompositeDeviceMessageSenderInterceptor(), stateChecker);

        defaultDeviceOperator.putState(org.jetlinks.core.device.DeviceState.online).subscribe(System.out::println);
        defaultDeviceOperator.getState().subscribe(System.out::println);
        defaultDeviceOperator.checkState().subscribe(System.out::println);
        return defaultDeviceOperator;
    }

    @Test
    void createDeviceDetail() {
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initEntity(deviceInstanceEntity);
        CompositeDeviceStateChecker stateChecker = Mockito.mock(CompositeDeviceStateChecker.class);
        DefaultDeviceOperator defaultDeviceOperator = createDeviceOperator(stateChecker);//创建设备

        Mockito.when(registry.getDevice(Mockito.any(String.class))).thenReturn(Mono.just(defaultDeviceOperator));
        ReactiveUpdate<DeviceInstanceEntity> update = Mockito.mock(ReactiveUpdate.class);

        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));


        DefaultConfigMetadata defaultConfigMetadata = new DefaultConfigMetadata();

        defaultConfigMetadata.add("test1", "test1", new StringType());

        Mockito.when(
            configMetadataManager
                .getDeviceConfigMetadata(Mockito.anyString())
        ).thenReturn(Flux.just(defaultConfigMetadata));

        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };

        assertNotNull(service);
        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        initProductEntity(deviceProductEntity);
        ArrayList<DeviceTagEntity> deviceTagEntities = new ArrayList<>();
        DeviceTagEntity deviceTagEntity = new DeviceTagEntity();
        deviceTagEntity.setId("test");
        deviceTagEntities.add(deviceTagEntity);
        service.createDeviceDetail(deviceProductEntity, deviceInstanceEntity, deviceTagEntities)
            .map(DeviceDetail::getState)
            .map(DeviceState::getValue)
            .as(StepVerifier::create)
            .expectNext("online")
            .verifyComplete();

        //设备已经离线 数据库状态为更新
        Mockito.when(stateChecker.checkState(Mockito.any(DeviceOperator.class))).thenReturn(Mono.just((byte) -1));
        defaultDeviceOperator.putState(org.jetlinks.core.device.DeviceState.offline).subscribe();
        service.createDeviceDetail(deviceProductEntity, deviceInstanceEntity, deviceTagEntities)
            .map(DeviceDetail::getState)
            .map(DeviceState::getValue)
            .as(StepVerifier::create)
            .expectNext("offline")
            .verifyComplete();

        //检查是出错
        Mockito.when(stateChecker.checkState(Mockito.any(DeviceOperator.class))).thenReturn(Mono.error(new NotFoundException()));
        service.createDeviceDetail(deviceProductEntity, deviceInstanceEntity, deviceTagEntities)
            .map(DeviceDetail::getState)
            .map(DeviceState::getValue)
            .as(StepVerifier::create)
            .expectNext("online")
            .verifyComplete();
        //状态不是未激活 并且设备为空
        deviceInstanceEntity.setState(DeviceState.online);
        Mockito.when(registry.getDevice(Mockito.any(String.class))).thenReturn(Mono.empty());
        service.createDeviceDetail(deviceProductEntity, deviceInstanceEntity, deviceTagEntities)
            .map(DeviceDetail::getState)
            .as(StepVerifier::create)
            .expectNext(DeviceState.notActive) //设备为空，修改状态为未激活
            .verifyComplete();

        //在最后抛出异常时
        deviceInstanceEntity.setState(DeviceState.online);
        Mockito.when(registry.getDevice(Mockito.any(String.class))).thenReturn(Mono.empty());
        Mockito.when(update.execute()).thenReturn(Mono.error(new Exception()));
        service.createDeviceDetail(deviceProductEntity, deviceInstanceEntity, deviceTagEntities)
            .map(DeviceDetail::getState)
            .as(StepVerifier::create)
            .expectNext(DeviceState.notActive) //设备为空，修改状态为未激活
            .verifyComplete();

        //状态为未激活 并且设备为空
        Mockito.when(registry.getDevice(Mockito.any(String.class))).thenReturn(Mono.empty());
        deviceInstanceEntity.setState(DeviceState.notActive);
        service.createDeviceDetail(deviceProductEntity, deviceInstanceEntity, deviceTagEntities)
            .map(DeviceDetail::getState)
            .as(StepVerifier::create)
            .expectNext(DeviceState.notActive)
            .verifyComplete();
    }

    @Test
    void getDeviceDetail() {
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager metadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initEntity(deviceInstanceEntity);
        CompositeDeviceStateChecker stateChecker = Mockito.mock(CompositeDeviceStateChecker.class);
        DefaultDeviceOperator defaultDeviceOperator = createDeviceOperator(stateChecker);
        Mockito.when(registry.getDevice(Mockito.any(String.class)))
            .thenReturn(Mono.just(defaultDeviceOperator));

        DefaultConfigMetadata defaultConfigMetadata = new DefaultConfigMetadata("test", "test");

        defaultConfigMetadata.add("test1", "test1", new StringType());
        ConfigMetadata configMetadata = defaultConfigMetadata;
        Mockito.when(
            metadataManager
                .getDeviceConfigMetadata(Mockito.anyString())
        ).thenReturn(Flux.just(configMetadata));
        ReactiveUpdate<DeviceInstanceEntity> update = Mockito.mock(ReactiveUpdate.class);
        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));


        Mockito.when(repository.findById(Mockito.anyString())).thenReturn(Mono.just(deviceInstanceEntity));

        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        deviceProductEntity.setId("test");
        deviceProductEntity.setName("test");
        deviceProductEntity.setState((byte) 1);
        Mockito.when(deviceProductService.findById(Mockito.anyString())).thenReturn(Mono.just(deviceProductEntity));

        ReactiveQuery<DeviceTagEntity> query = Mockito.mock(ReactiveQuery.class);
        DeviceTagEntity deviceTagEntity = new DeviceTagEntity();
        deviceTagEntity.setId("test");
        deviceTagEntity.setName("test");
        deviceTagEntity.setDeviceId(ID_1);
        Mockito.when(tagRepository.createQuery()).thenReturn(query);
        Mockito.when(query.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))).thenReturn(query);
        Mockito.when(query.fetch()).thenReturn(Flux.just(deviceTagEntity));
        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, metadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };

        assertNotNull(service);
        service.getDeviceDetail(ID_1)
            .map(DeviceDetail::getState)
            .map(s -> s.getValue())
            .as(StepVerifier::create)
            .expectNext("online")
            .verifyComplete();

        Mockito.when(registry.getDevice(Mockito.any(String.class)))
            .thenReturn(Mono.empty());
        service.getDeviceDetail(ID_1)
            .map(DeviceDetail::getState)
            .map(s -> s.getValue())
            .as(StepVerifier::create)
            .expectNext("notActive")
            .verifyComplete();

    }

    @Test
    void getDeviceState() {
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initEntity(deviceInstanceEntity);
        CompositeDeviceStateChecker stateChecker = Mockito.mock(CompositeDeviceStateChecker.class);
        DefaultDeviceOperator defaultDeviceOperator = createDeviceOperator(stateChecker);
        Mockito.when(registry.getDevice(Mockito.any(String.class))).thenReturn(Mono.just(defaultDeviceOperator));

        ReactiveUpdate<DeviceInstanceEntity> update = Mockito.mock(ReactiveUpdate.class);
        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(
            update.set(
                Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class))
        ).thenReturn(update);
        Mockito.when(update.where(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.execute()).thenReturn(Mono.just(1));

        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);
        service.getDeviceState(ID_1)
            .map(DeviceState::getValue)
            .as(StepVerifier::create)
            .expectNext("online")
            .verifyComplete();
        //获取设备为空
        Mockito.when(registry.getDevice(Mockito.any(String.class))).thenReturn(Mono.empty());
        service.getDeviceState(ID_1)
            .map(DeviceState::getValue)
            .as(StepVerifier::create)
            .expectNext("notActive")
            .verifyComplete();
    }

    @Test
    void syncStateBatch() {
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        HashMap<String, Object> map = new HashMap<>();
        map.put("test", "test");
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initEntity(deviceInstanceEntity);
        deviceInstanceEntity.setParentId("test002");
        DeviceInstanceEntity deviceInstanceEntity1 = new DeviceInstanceEntity();
        deviceInstanceEntity1.setId("test002");
        deviceInstanceEntity1.setConfiguration(map);
        deviceInstanceEntity1.setParentId(ID_1);

        CompositeDeviceStateChecker stateChecker = Mockito.mock(CompositeDeviceStateChecker.class);

        DefaultDeviceOperator defaultDeviceOperator = createDeviceOperator(stateChecker);
        defaultDeviceOperator.setConfig(DeviceConfigKey.isGatewayDevice, true).subscribe(System.out::println);
        //defaultDeviceOperator.getConfig(DeviceConfigKey.isGatewayDevice).subscribe(System.out::println);

        Mockito.when(registry.getDevice(Mockito.anyString())).thenReturn(Mono.just(defaultDeviceOperator));

        ReactiveUpdate<DeviceInstanceEntity> update = Mockito.mock(ReactiveUpdate.class);
        Mockito.when(repository.createUpdate()).thenReturn(update);
        Mockito.when(update.set(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.where())
            .thenReturn(update);
        Mockito.when(update.in(Mockito.any(StaticMethodReferenceColumn.class), Mockito.any(Collection.class)))
            .thenReturn(update);
        Mockito.when(update.not(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
            .thenReturn(update);
        Mockito.when(update.execute())
            .thenReturn(Mono.just(1));

        NestConditional<ReactiveUpdate<DeviceInstanceEntity>> reactiveUpdateNestConditional = Mockito.mock(NestConditional.class);
        Mockito.when(update.nest()).thenReturn(reactiveUpdateNestConditional);
        Mockito.when(reactiveUpdateNestConditional.accept(Mockito.any(StaticMethodReferenceColumn.class)
            , Mockito.anyString(), Mockito.any(Object.class)
        )).thenReturn(reactiveUpdateNestConditional);
        Mockito.when(reactiveUpdateNestConditional.or()).thenReturn(reactiveUpdateNestConditional);
        Mockito.when(reactiveUpdateNestConditional.isNull(Mockito.any(StaticMethodReferenceColumn.class)))
            .thenReturn(reactiveUpdateNestConditional);

        Mockito.when(reactiveUpdateNestConditional.end()).thenReturn(update);

        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };

        assertNotNull(service);
        List<String> list = new ArrayList<>();
        list.add(ID_1);
        list.add("test002");
        service.syncStateBatch(Flux.just(list), true)
            .map(List::iterator)
            .map(Iterator::next)
            .map(DeviceStateInfo::getState)
            .map(DeviceState::getValue)
            .as(StepVerifier::create)
            .expectNext("online")
            .verifyComplete();
        Mockito.when(registry.getDevice(Mockito.anyString())).thenReturn(Mono.empty());
        service.syncStateBatch(Flux.just(list), true)
            .map(List::iterator)
            .map(s -> s.next())
            .map(DeviceStateInfo::getState)
            .map(DeviceState::getValue)
            .as(StepVerifier::create)
            .expectNext("notActive")
            .verifyComplete();
    }

    @Test
    void readAndConvertProperty()  {
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initEntity(deviceInstanceEntity);
        InMemoryConfigStorage inMemoryConfigStorage = new InMemoryConfigStorage();
        inMemoryConfigStorage.setConfig(DeviceConfigKey.connectionServerId.getKey(), "12345");//连接服务id
        inMemoryConfigStorage.setConfig(DeviceConfigKey.productId.getKey(), "test");
        CompositeDeviceStateChecker stateChecker = Mockito.mock(CompositeDeviceStateChecker.class);
        Mockito.when(stateChecker.checkState(Mockito.any(DeviceOperator.class))).thenReturn(Mono.just((byte) 1));
        InMemoryConfigStorageManager inMemoryConfigStorageManager = Mockito.mock(InMemoryConfigStorageManager.class);
        Mockito.when(inMemoryConfigStorageManager.getStorage(Mockito.anyString()))
            .thenReturn(Mono.just(inMemoryConfigStorage));

        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("temperature", 45);
        ReadPropertyMessageReply readPropertyMessageReply = ReadPropertyMessageReply.create();
        readPropertyMessageReply.setProperties(hashMap);
        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.just(readPropertyMessageReply));

        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        initProductEntity(deviceProductEntity);

        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry();
        DeviceProductOperator block = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        //设置协议的key值 用来查找到MockProtocolSupport
        // 底层加载物理模型时会调用协议实例的 getMetadataCodec方法，加载物模型编解码器
        assertNotNull(block);
        block.setConfig(DeviceConfigKey.protocol.getKey(), "test").subscribe(System.out::println);
        inMemoryDeviceRegistry.register( deviceInstanceEntity.toDeviceInfo()).subscribe();

        DefaultDeviceOperator defaultDeviceOperator =
            new DefaultDeviceOperator(ID_1, new MockProtocolSupport(), inMemoryConfigStorageManager
                , standaloneDeviceMessageBroker, inMemoryDeviceRegistry
                , new CompositeDeviceMessageSenderInterceptor(), stateChecker);
        defaultDeviceOperator.setConfig(DeviceConfigKey.productId.getKey(),"test").subscribe();
        defaultDeviceOperator.setConfig(DeviceConfigKey.protocol, "test").subscribe();
        defaultDeviceOperator.setConfig("lst_metadata_time", 1L).subscribe();
        defaultDeviceOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe(System.out::println);

        defaultDeviceOperator.getMetadata()
            .map(e->e.getProperty("temperature").map(s1->s1.getValueType()))
            .map(s->s.get()).subscribe(System.out::println);
        Mockito.when(registry.getDevice(Mockito.anyString())).thenReturn(Mono.just(defaultDeviceOperator));

        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };

        assertNotNull(service);
        service.readAndConvertProperty(ID_1, "temperature")
            .map(DevicePropertiesEntity::getNumberValue)
            .as(StepVerifier::create)
            .expectNext(BigDecimal.valueOf(45.0))   // 获取的温度
            .verifyComplete();

        readPropertyMessageReply.setCode(ErrorCode.REQUEST_HANDLING.name());
        readPropertyMessageReply.setMessage("request_handling");
        service.readAndConvertProperty(ID_1, "test")
            .map(DevicePropertiesEntity::getStringValue)
            .onErrorResume(e -> Mono.just(e.getMessage()))
            .as(StepVerifier::create)
            .expectNext("request_handling")
            .verifyComplete();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.empty());
        service.readAndConvertProperty(ID_1, "test")
            .map(DevicePropertiesEntity::getStringValue)
            .onErrorResume(e->Mono.just(e.getMessage()))
            .as(StepVerifier::create)
            .expectNext("设备不存在")
            .verifyComplete();
    }

    @Test
    void readAndConverProperty1(){
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initEntity(deviceInstanceEntity);

        InMemoryConfigStorage inMemoryConfigStorage = new InMemoryConfigStorage();
        inMemoryConfigStorage.setConfig(DeviceConfigKey.connectionServerId.getKey(), "12345");//连接服务id
        inMemoryConfigStorage.setConfig(DeviceConfigKey.productId.getKey(), "test");
        CompositeDeviceStateChecker stateChecker = Mockito.mock(CompositeDeviceStateChecker.class);
        Mockito.when(stateChecker.checkState(Mockito.any(DeviceOperator.class))).thenReturn(Mono.just((byte) 1));
        InMemoryConfigStorageManager inMemoryConfigStorageManager = Mockito.mock(InMemoryConfigStorageManager.class);
        Mockito.when(inMemoryConfigStorageManager.getStorage(Mockito.anyString()))
            .thenReturn(Mono.just(inMemoryConfigStorage));

        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("temperature", 45);
        ReadPropertyMessageReply readPropertyMessageReply = ReadPropertyMessageReply.create();
        readPropertyMessageReply.setProperties(hashMap);
        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.just(readPropertyMessageReply));

        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        initProductEntity(deviceProductEntity);

        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry();
        DeviceProductOperator block = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        assertNotNull(block);
        //设置协议的key值 用来查找到MockProtocolSupport
        // 底层加载物理模型时会调用协议实例的 getMetadataCodec方法，加载物模型编解码器
        block.setConfig(DeviceConfigKey.protocol.getKey(), "test").subscribe(System.out::println);

        inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo().addConfig("state", org.jetlinks.core.device.DeviceState.online)).subscribe();

        DefaultDeviceOperator defaultDeviceOperator =
            new DefaultDeviceOperator(ID_1, new MockProtocolSupport(), inMemoryConfigStorageManager
                , standaloneDeviceMessageBroker, inMemoryDeviceRegistry
                , new CompositeDeviceMessageSenderInterceptor(), stateChecker);
        defaultDeviceOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe(System.out::println);

        Mockito.when(registry.getDevice(Mockito.anyString())).thenReturn(Mono.just(defaultDeviceOperator));


        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);

        readPropertyMessageReply.setSuccess(false);
        readPropertyMessageReply.setMessage("发送失败");
        service.readAndConvertProperty(ID_1, "test")
            .map(DevicePropertiesEntity::getStringValue)
            .onErrorResume(e -> Mono.just(e.getMessage()))
            .as(StepVerifier::create)
            .expectNext("发送失败")
            .verifyComplete();
    }

    @Test
    void writeProperties() {
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initEntity(deviceInstanceEntity);

        InMemoryConfigStorage inMemoryConfigStorage = new InMemoryConfigStorage();
        inMemoryConfigStorage.setConfig(DeviceConfigKey.connectionServerId.getKey(), "12345");//连接服务id
        inMemoryConfigStorage.setConfig(DeviceConfigKey.productId.getKey(), "test");
        CompositeDeviceStateChecker stateChecker = Mockito.mock(CompositeDeviceStateChecker.class);
        Mockito.when(stateChecker.checkState(Mockito.any(DeviceOperator.class))).thenReturn(Mono.just((byte) 1));
        InMemoryConfigStorageManager inMemoryConfigStorageManager = Mockito.mock(InMemoryConfigStorageManager.class);
        Mockito.when(inMemoryConfigStorageManager.getStorage(Mockito.anyString()))
            .thenReturn(Mono.just(inMemoryConfigStorage));

        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("test", "test");
        WritePropertyMessageReply writePropertyMessageReply = WritePropertyMessageReply.create();
        writePropertyMessageReply.setProperties(hashMap);

        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.just(writePropertyMessageReply));

        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        initProductEntity(deviceProductEntity);

        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry();
        DeviceProductOperator block = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        assertNotNull(block);
        //设置协议的key值 用来查找到MockProtocolSupport
        // 底层加载物理模型时会调用协议实例的 getMetadataCodec方法，加载物模型编解码器
        block.setConfig(DeviceConfigKey.protocol.getKey(), "test").subscribe(System.out::println);

        inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo().addConfig("state", org.jetlinks.core.device.DeviceState.online)).subscribe();

        DefaultDeviceOperator defaultDeviceOperator =
            new DefaultDeviceOperator(ID_1, new MockProtocolSupport(), inMemoryConfigStorageManager
                , standaloneDeviceMessageBroker, inMemoryDeviceRegistry
                , new CompositeDeviceMessageSenderInterceptor(), stateChecker);
        defaultDeviceOperator.updateMetadata("{\"events\":[{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}],\"functions\":[],\"tags\":[{\"id\":\"test\",\"name\":\"tag\",\"valueType\":{\"type\":\"int\",\"unit\":\"meter\"},\"expands\":{\"readOnly\":\"false\"}}]}").subscribe(System.out::println);

        Mockito.when(registry.getDevice(Mockito.anyString())).thenReturn(Mono.just(defaultDeviceOperator));
        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);

        Map<String, Object> map1 = new HashMap<>();
        map1.put("test", "test");
        service.writeProperties(ID_1, map1)
            .map(m -> m.get("test"))
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        Mockito.when(registry.getDevice(Mockito.anyString()))
            .thenReturn(Mono.empty());
        service.writeProperties(ID_1, map1)
            .map(s->s.get("test"))
            .onErrorResume(e->Mono.just(e.getMessage()))
            .as(StepVerifier::create)
            .expectNext("设备不存在")
            .verifyComplete();
    }

    @Test
    void invokeFunction() {
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);

        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initEntity(deviceInstanceEntity);
        deviceInstanceEntity.setDeriveMetadata("{\"events\":[],\"properties\":['test':'test'],\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}],\"tags\":[]}");

        InMemoryConfigStorage inMemoryConfigStorage = new InMemoryConfigStorage();
        inMemoryConfigStorage.setConfig(DeviceConfigKey.connectionServerId.getKey(), "12345");//连接服务id
        inMemoryConfigStorage.setConfig(DeviceConfigKey.productId.getKey(), "test");
        CompositeDeviceStateChecker stateChecker = Mockito.mock(CompositeDeviceStateChecker.class);
        Mockito.when(stateChecker.checkState(Mockito.any(DeviceOperator.class))).thenReturn(Mono.just((byte) 1));
        InMemoryConfigStorageManager inMemoryConfigStorageManager = Mockito.mock(InMemoryConfigStorageManager.class);
        Mockito.when(inMemoryConfigStorageManager.getStorage(Mockito.anyString()))
            .thenReturn(Mono.just(inMemoryConfigStorage));

        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("test", "test");
        FunctionInvokeMessageReply functionInvokeMessageReply = FunctionInvokeMessageReply.create();
        functionInvokeMessageReply.setFunctionId("fire_alarm");
        functionInvokeMessageReply.setOutput("未报警");

        StandaloneDeviceMessageBroker standaloneDeviceMessageBroker = Mockito.mock(StandaloneDeviceMessageBroker.class);
        Mockito.when(standaloneDeviceMessageBroker.send(Mockito.anyString(), Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(1));
        Mockito.when(standaloneDeviceMessageBroker
            .handleReply(Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
            .thenReturn(Flux.just(functionInvokeMessageReply));

        DeviceProductEntity deviceProductEntity = new DeviceProductEntity();
        initProductEntity(deviceProductEntity);

        InMemoryDeviceRegistry inMemoryDeviceRegistry = new InMemoryDeviceRegistry();
        DeviceProductOperator block = inMemoryDeviceRegistry.register(deviceProductEntity.toProductInfo()).block();
        assertNotNull(block);
        //设置协议的key值 用来查找到MockProtocolSupport
        // 底层加载物理模型时会调用协议实例的 getMetadataCodec方法，加载物模型编解码器
        block.setConfig(DeviceConfigKey.protocol.getKey(), "test").subscribe(System.out::println);

        inMemoryDeviceRegistry.register(deviceInstanceEntity.toDeviceInfo()).subscribe();

        DefaultDeviceOperator defaultDeviceOperator =
            new DefaultDeviceOperator(ID_1, new MockProtocolSupport(), inMemoryConfigStorageManager
                , standaloneDeviceMessageBroker, inMemoryDeviceRegistry
                , new CompositeDeviceMessageSenderInterceptor(), stateChecker);
        defaultDeviceOperator.setConfig(DeviceConfigKey.productId.getKey(),"test").subscribe();
        defaultDeviceOperator.setConfig(DeviceConfigKey.protocol, "test").subscribe();
        defaultDeviceOperator.setConfig("lst_metadata_time", 1L).subscribe();;
        defaultDeviceOperator.updateMetadata("{\"events\":[],\"properties\":[],\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}],\"tags\":[]}").subscribe(System.out::println);

        Mockito.when(registry.getDevice(Mockito.anyString())).thenReturn(Mono.just(defaultDeviceOperator));
        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };

        assertNotNull(service);
        Map<String, Object> map1 = new HashMap<>();
        map1.put("test", "test");
        service.invokeFunction(ID_1, "AuditCommandFunction", map1)
            .map(Object::toString)
            .as(StepVerifier::create)
            .expectNext("未报警")
            .verifyComplete();
    }

    @Test
    void checkCyclicDependency() {
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        HashMap<String, Object> map = new HashMap<>();
        map.put("test", "test");
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        initEntity(deviceInstanceEntity);
        deviceInstanceEntity.setDeriveMetadata("{\"events\":[],\"properties\":['test':'test'],\"functions\":[{\"id\":\"AuditCommandFunction\",\"name\":\"查岗\",\"async\":false,\"output\":{},\"inputs\":[{\"id\":\"outTime\",\"name\":\"超时时间\",\"valueType\":{\"type\":\"int\",\"unit\":\"minutes\"}}]}],\"tags\":[]}");

        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);
        service.checkCyclicDependency(deviceInstanceEntity)
            .as(StepVerifier::create)
            .expectSubscription()
            .verifyComplete();
    }

    @Test
    void testCheckCyclicDependency() {
        ReactiveRepository<DeviceInstanceEntity, String> repository = Mockito.mock(ReactiveRepository.class);
        DeviceRegistry registry = Mockito.mock(DeviceRegistry.class);
        LocalDeviceProductService deviceProductService = Mockito.mock(LocalDeviceProductService.class);
        DeviceConfigMetadataManager configMetadataManager = Mockito.mock(DeviceConfigMetadataManager.class);
        ReactiveRepository<DeviceTagEntity, String> tagRepository = Mockito.mock(ReactiveRepository.class);
        HashMap<String, Object> map = new HashMap<>();
        map.put("test", "test");
        DeviceInstanceEntity deviceInstanceEntity = new DeviceInstanceEntity();
        deviceInstanceEntity.setId(ID_1);
        deviceInstanceEntity.setConfiguration(map);
        deviceInstanceEntity.setProductId("test");
        deviceInstanceEntity.setState(DeviceState.online);
        deviceInstanceEntity.setParentId("test001");

        Mockito.when(repository.findById(Mockito.anyString()))
            .thenReturn(Mono.just(deviceInstanceEntity));
        LocalDeviceInstanceService service = new LocalDeviceInstanceService(registry, deviceProductService, configMetadataManager, tagRepository) {
            @Override
            public ReactiveRepository<DeviceInstanceEntity, String> getRepository() {
                return repository;
            }
        };
        assertNotNull(service);
        service.checkCyclicDependency(ID_1,"test002")
            .as(StepVerifier::create)
            .expectSubscription()
            .expectErrorMessage("error.cyclic_dependence")
            .verify();
    }
}