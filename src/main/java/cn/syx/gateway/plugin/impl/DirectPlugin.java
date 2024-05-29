package cn.syx.gateway.plugin.impl;

import cn.syx.gateway.plugin.GatewayPluginChain;
import cn.syx.gateway.plugin.AbstractGatewayPlugin;
import cn.syx.registry.client.SyxRegistryClient;
import cn.syx.registry.core.model.RegistryInstanceMeta;
import cn.syx.toolbox.base.StringTool;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DirectPlugin extends AbstractGatewayPlugin {

    private static final String NAME = "direct";

    private static final String URL_PREFIX = GATEWAY_PREFIX + "/" + NAME + "/";

    private static final LoadBalancer<RegistryInstanceMeta> lb
            = LoadBalancerTool.getInstance().get(RoundRibbonLoadBalancer.class);

    @Autowired
    private SyxRegistryClient registryClient;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean doSupport(ServerWebExchange exchange) {
        return exchange.getRequest().getPath().value().startsWith(URL_PREFIX);
    }

    @Override
    public Mono<Void> doHandle(ServerWebExchange exchange, GatewayPluginChain chain) {
        log.info("===> syx direct plugin start");
        String backend = exchange.getRequest().getQueryParams().getFirst("backend");

        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        exchange.getResponse().getHeaders().add("syx.gw.version", "v1.0.0");
        exchange.getResponse().getHeaders().add("syx.gw.plugin", name());

        Flux<DataBuffer> request = exchange.getRequest().getBody();
        return request.single().flatMap(req ->
                StringTool.isBlank(backend)
                        ? exchange.getResponse().writeWith(Mono.just(req)) // 返回原请求
                        : invokeFromRegistry(exchange, req, backend) // 按照backend路径请求
        ).then(chain.handle(exchange));
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
        log.info("parse data: {}", data);
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(bytes)
        ));
    }
}
