package cn.syx.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

import java.util.Map;

@Slf4j
@Configuration
public class GatewayConfig {

    @Bean
    public ApplicationRunner runner(@Autowired ApplicationContext context) {
       return args -> {
           SimpleUrlHandlerMapping handlerMapping = context.getBean(SimpleUrlHandlerMapping.class);
           handlerMapping.setUrlMap(Map.of("/gw/**", "gatewayWebHandler"));
           handlerMapping.initApplicationContext();
           log.info("gateway runner done.");
       };
    }
}
