package ru.yandux.practicum.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;


@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "app.security.users[0].username=ivan",
                "app.security.users[0].password=ivan",
                "app.security.users[0].roles[0]=USER",
                "app.security.users[1].username=anna",
                "app.security.users[1].password=anna",
                "app.security.users[1].roles[0]=ADMIN",
                "app.security.users[1].roles[1]=USER"
        })
@AutoConfigureWebTestClient
class GatewaySecurityConfigTest {
    private static final String REGULAR_USER_LOGIN = "ivan";
    private static final String ADMIN_USER_LOGIN = "anna";
    private static final String ORDERS_PATH = "/api/orders";
    private static final String PRODUCTS_PATH = "/api/products";
    private static final String UNKNOWN_PATH = "/api/unknown";
    private static final String ANY_PATH = "/**";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void catalogGet_isPublic() {
        // TODO: проверьте, что GET /api/products доступен без учётных данных

        webTestClient.get()
                .uri(PRODUCTS_PATH)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void orderCreate_withoutCredentials_isUnauthorized() {
        // TODO: проверьте, что POST /api/orders без учётных данных возвращает 401

        webTestClient.post()
                .uri(ORDERS_PATH)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void productWrite_withUserCredentials_isForbidden() {
        // TODO: проверьте, что USER не может выполнять write-операцию с каталогом

        webTestClient.post()
                .uri(PRODUCTS_PATH)
                .header("Authorization", basic(REGULAR_USER_LOGIN, REGULAR_USER_LOGIN))
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.patch()
                .uri(PRODUCTS_PATH + "/1")
                .header("Authorization", basic(REGULAR_USER_LOGIN, REGULAR_USER_LOGIN))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void productWrite_withAdminCredentials_passesSecurity() {
        // TODO: проверьте, что ADMIN проходит security-проверку для write-операции

        webTestClient.post()
                .uri(PRODUCTS_PATH)
                .header("Authorization", basic(ADMIN_USER_LOGIN, ADMIN_USER_LOGIN))
                .exchange()
                .expectStatus().isOk();

        webTestClient.patch()
                .uri(PRODUCTS_PATH + "/1")
                .header("Authorization", basic(ADMIN_USER_LOGIN, ADMIN_USER_LOGIN))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void unknownRoute_withAdminCredentials_isForbidden() {
        // TODO: проверьте, что неизвестный маршрут закрыт даже для ADMIN

        webTestClient.post()
                .uri(UNKNOWN_PATH)
                .header("Authorization", basic(ADMIN_USER_LOGIN, ADMIN_USER_LOGIN))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void corsPreflight_isPublic() {
        // TODO: проверьте, что OPTIONS /api/orders не блокируется security-слоем

        webTestClient.options()
                .uri(ORDERS_PATH)
                .exchange()
                .expectStatus().isOk();
    }

    private String basic(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class TestBackendConfig {

        @Bean
        RouterFunction<ServerResponse> testBackendRoutes() {
            return route()
                    .GET(PRODUCTS_PATH + ANY_PATH, request -> ServerResponse.ok().build())
                    .GET(ORDERS_PATH + ANY_PATH, request -> ServerResponse.ok().build())
                    .POST(PRODUCTS_PATH + ANY_PATH, request -> ServerResponse.ok().build())
                    .POST(ORDERS_PATH + ANY_PATH, request -> ServerResponse.ok().build())
                    .PATCH(PRODUCTS_PATH + ANY_PATH, request -> ServerResponse.ok().build())
                    .OPTIONS(ORDERS_PATH + ANY_PATH, request -> ServerResponse.ok().build())
                    .build();
        }
    }
}