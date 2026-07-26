package br.com.fiap.feedback.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Garante os contratos de monitoramento e documentacao usados pela colecao
 * Postman e pelos alarmes de infraestrutura.
 */
@QuarkusTest
class HealthEndpointTest {

    @Test
    @DisplayName("GET /q/health responde 200 com status UP")
    void healthRespondeUp() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks.name", hasItem(AplicacaoHealthCheck.NOME_DO_CHECK));
    }

    @Test
    @DisplayName("GET /q/openapi publica a especificacao da API")
    void openApiEstaPublicada() {
        given()
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }
}
