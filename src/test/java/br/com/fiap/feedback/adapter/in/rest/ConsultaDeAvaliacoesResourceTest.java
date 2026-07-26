package br.com.fiap.feedback.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato do endpoint GET /api/v1/avaliacoes (SPEC 5.2).
 */
@QuarkusTest
class ConsultaDeAvaliacoesResourceTest {

    private static final String CONSULTA = "/api/v1/avaliacoes";
    private static final String REGISTRO = "/api/v1/avaliacao";

    private String registrar(String descricao, int nota) {
        return given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"descricao\": \"" + descricao + "\", \"nota\": " + nota + "}")
                .when().post(REGISTRO)
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    @DisplayName("lista a avaliacao recem-registrada com itens e total")
    void listaAvaliacaoRecemRegistrada() {
        String id = registrar("Consulta deve encontrar esta avaliacao", 6);

        given()
                .when().get(CONSULTA)
                .then()
                .statusCode(200)
                .body("itens.id", hasItem(id))
                .body("total", greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("total corresponde a quantidade de itens devolvidos")
    void totalCorrespondeAQuantidadeDeItens() {
        registrar("Coerencia entre total e itens", 7);

        int total = given().when().get(CONSULTA).then().statusCode(200).extract().path("total");
        given()
                .when().get(CONSULTA)
                .then()
                .body("itens", hasSize(total));
    }

    @Test
    @DisplayName("filtra por urgencia")
    void filtraPorUrgencia() {
        String idCritica = registrar("Falha grave de audio na aula", 0);
        registrar("Aula excelente", 10);

        given()
                .queryParam("urgencia", "CRITICA")
                .when().get(CONSULTA)
                .then()
                .statusCode(200)
                .body("itens.id", hasItem(idCritica))
                .body("itens.urgencia", everyItem(equalTo("CRITICA")));
    }

    @Test
    @DisplayName("aceita urgencia em minusculas")
    void aceitaUrgenciaEmMinusculas() {
        given()
                .queryParam("urgencia", "critica")
                .when().get(CONSULTA)
                .then()
                .statusCode(200)
                .body("itens.urgencia", everyItem(equalTo("CRITICA")));
    }

    @Test
    @DisplayName("periodo sem dados devolve lista vazia")
    void periodoSemDadosDevolveListaVazia() {
        given()
                .queryParam("dataInicio", "2018-01-01")
                .queryParam("dataFim", "2018-01-05")
                .when().get(CONSULTA)
                .then()
                .statusCode(200)
                .body("itens", hasSize(0))
                .body("total", equalTo(0));
    }

    @Test
    @DisplayName("respeita o periodo informado")
    void respeitaPeriodoInformado() {
        String id = registrar("Registrada no dia corrente", 5);
        String hoje = LocalDate.now(ZoneOffset.UTC).toString();

        given()
                .queryParam("dataInicio", hoje)
                .queryParam("dataFim", hoje)
                .when().get(CONSULTA)
                .then()
                .statusCode(200)
                .body("itens.id", hasItem(id));
    }

    @Test
    @DisplayName("rejeita data em formato invalido")
    void rejeitaDataEmFormatoInvalido() {
        given()
                .queryParam("dataInicio", "26/07/2026")
                .when().get(CONSULTA)
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("mensagens", hasItem(containsString("dataInicio")));
    }

    @Test
    @DisplayName("rejeita urgencia desconhecida")
    void rejeitaUrgenciaDesconhecida() {
        given()
                .queryParam("urgencia", "URGENTISSIMA")
                .when().get(CONSULTA)
                .then()
                .statusCode(400)
                .body("mensagens", hasItem(containsString("urgencia")));
    }

    @Test
    @DisplayName("rejeita periodo com inicio depois do fim")
    void rejeitaPeriodoInvertido() {
        given()
                .queryParam("dataInicio", "2026-07-20")
                .queryParam("dataFim", "2026-07-10")
                .when().get(CONSULTA)
                .then()
                .statusCode(400)
                .body("mensagens", hasItem(containsString("dataInicio")));
    }

    @Test
    @DisplayName("rejeita periodo longo demais")
    void rejeitaPeriodoLongoDemais() {
        given()
                .queryParam("dataInicio", "2020-01-01")
                .queryParam("dataFim", "2026-12-31")
                .when().get(CONSULTA)
                .then()
                .statusCode(400)
                .body("mensagens", hasItem(containsString("periodo")));
    }
}
