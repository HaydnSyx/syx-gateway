package cn.syx.gateway;

import cn.syx.gateway.other.LoadBalancer;
import cn.syx.gateway.other.RoundRibbonLoadBalancer;
import cn.syx.gateway.other.ServiceMeta;
import cn.syx.registry.client.SyxRegistryClient;
import cn.syx.registry.client.model.SyxRegistryInstanceMeta;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component("gatewayWebHandler")
public class GatewayWebHandler implements WebHandler {

    @Autowired
    private SyxRegistryClient registryClient;

    private static LoadBalancer<SyxRegistryInstanceMeta> lb = new RoundRibbonLoadBalancer<>();

    @NotNull
    @Override
    public Mono<Void> handle(ServerWebExchange exchange) {
        log.info("");
        String service = exchange.getRequest().getPath().value().substring(4);

        ServiceMeta meta = ServiceMeta.builder()
                .namespace("default")
                .env("dev")
                .group("app")
                .name(service)
                .version("1.0.0")
                .build();
        List<SyxRegistryInstanceMeta> instanceMetas = registryClient.fetchAll(meta.toPath());

        SyxRegistryInstanceMeta instanceMeta = lb.choose(instanceMetas);
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
