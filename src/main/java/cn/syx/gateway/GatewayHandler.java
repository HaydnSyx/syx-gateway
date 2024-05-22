package cn.syx.gateway;

import cn.syx.gateway.other.LoadBalancer;
import cn.syx.gateway.other.RoundRibbonLoadBalancer;
import cn.syx.gateway.other.ServiceMeta;
import cn.syx.registry.client.SyxRegistryClient;
import cn.syx.registry.client.model.SyxRegistryInstanceMeta;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class GatewayHandler {

    @Autowired
    private SyxRegistryClient registryClient;

    private static LoadBalancer<SyxRegistryInstanceMeta> lb = new RoundRibbonLoadBalancer<>();

    public Mono<ServerResponse> handle(ServerRequest request) {
        String service = request.path().substring(4);

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

        Mono<String> req = request.bodyToMono(String.class);
        return req.flatMap(e -> invokeFromRegistry(e, instanceMeta.toUrl()));
    }

    @NotNull
    private static Mono<ServerResponse> invokeFromRegistry(String request, String url) {
        WebClient client = WebClient.create(url);
        Mono<String> body = client.post()
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .toEntity(String.class)
                .map(ResponseEntity::getBody);
        return body.flatMap(GatewayHandler::parseBody);
    }

    @NotNull
    private static Mono<ServerResponse> parseBody(String body) {
        return ServerResponse.ok()
                .header("Content-Type", "application/json")
                .bodyValue(body);
    }
}
