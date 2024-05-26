package cn.syx.gateway.core;

import cn.syx.registry.client.SyxRegistryClient;
import cn.syx.registry.core.model.RegistryInstanceMeta;
import cn.syx.registry.core.model.instance.RpcServiceMeta;
import cn.syx.toolbox.strategy.lb.LoadBalancer;
import cn.syx.toolbox.strategy.lb.LoadBalancerTool;
import cn.syx.toolbox.strategy.lb.impl.RoundRibbonLoadBalancer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component("gatewayWebHandler")
public class GatewayWebHandler implements WebHandler {

    private static final LoadBalancer<RegistryInstanceMeta> lb
            = LoadBalancerTool.getInstance().get(RoundRibbonLoadBalancer.class);

    @Autowired
    private SyxRegistryClient registryClient;

    @NotNull
    @Override
    public Mono<Void> handle(ServerWebExchange exchange) {
        log.info("");
        String service = exchange.getRequest().getPath().value().substring(4);

        RpcServiceMeta meta = RpcServiceMeta.builder()
                .env("dev")
                .namespace("default")
                .group("app")
                .name(service)
                .version("1.0.0")
                .build();
        List<RegistryInstanceMeta> instanceMetas = registryClient.fetchAll(meta.identity());
        RegistryInstanceMeta instanceMeta = lb.choose(instanceMetas);
        log.info("select instance: {}", instanceMeta);

        Flux<DataBuffer> request = exchange.getRequest().getBody();
        return request.flatMap(req -> invokeFromRegistry(exchange, req, instanceMeta.toUrl())).next();
    }

    @NotNull
    private static Mono<Void> invokeFromRegistry(ServerWebExchange exchange, DataBuffer body, String url) {
        WebClient client = WebClient.create(url);
        Mono<String> data = client.post()
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .toEntity(String.class)
                .mapNotNull(ResponseEntity::getBody);
        return data.flatMap(d -> parseData(exchange, d));
    }

    @NotNull
    private static Mono<Void> parseData(ServerWebExchange exchange, String data) {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(bytes)
        ));
    }
}
