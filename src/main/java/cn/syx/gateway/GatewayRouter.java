package cn.syx.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@Component
public class GatewayRouter {

    @Autowired
    private HelloHandler helloHandler;
    @Autowired
    private GatewayHandler gatewayHandler;

    @Bean
    public RouterFunction<?> helloRoute() {
        return RouterFunctions.route(RequestPredicates.GET("/hello"), request -> helloHandler.handle(request));
    }

    @Bean
    public RouterFunction<?> gwRoute() {
        return RouterFunctions.route(RequestPredicates.GET("/gw").or(RequestPredicates.POST("/gw/**")), request -> gatewayHandler.handle(request));
    }
}
