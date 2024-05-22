package cn.syx.gateway;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class HelloHandler {

    public Mono<ServerResponse> handle(ServerRequest request) {
        String url = "http://localhost:6081/rpc";

        String req = """
                {
                  "service": "cn.syx.rpc.demo.api.UserService",
                  "methodSign": "findById(int)",
                  "args": [10000]
                }
                """;

        WebClient client = WebClient.create(url);
        Mono<String> result = client.post().header("Content-Type", "application/json")
                .bodyValue(req)
                .retrieve()
                .toEntity(String.class)
                .map(HttpEntity::getBody);
//        result.subscribe(System.out::println);

        return ServerResponse.ok().header("Content-Type", "application/json")
                .body(result, String.class);
    }
}
