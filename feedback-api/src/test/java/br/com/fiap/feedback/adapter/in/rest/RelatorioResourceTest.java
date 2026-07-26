package br.com.fiap.feedback.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato do endpoint GET /api/v1/relatorios/semanal (SPEC 5.3).
 */
@QuarkusTest
class RelatorioResourceTest {

    private static final String RELATORIO = "/api/v1/relatorios/semanal";
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
    @DisplayName("devolve o consolidado da semana corrente")
    void devolveConsolidadoDaSemanaCorrente() {
        registrar("Entra no relatorio da semana", 7);

        LocalDate hoje = LocalDate.now(ZoneOffset.UTC);
        LocalDate segunda = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        given()
                .when().get(RELATORIO)
                .then()
                .statusCode(200)
                .body("inicio", equalTo(segunda.toString()))
                .body("fim", equalTo(segunda.plusDays(6).toString()))
                .body("totalAvaliacoes", greaterThanOrEqualTo(1))
                .body("mediaNotas", notNullValue());
    }

    @Test
    @DisplayName("expoe as quatro faixas de urgencia e a contagem por dia")
    void exponeFaixasDeUrgenciaEContagemPorDia() {
        registrar("Avaliacao para o agrupamento", 4);
        String hoje = LocalDate.now(ZoneOffset.UTC).toString();

        given()
                .when().get(RELATORIO)
                .then()
                .statusCode(200)
                .body("avaliacoesPorUrgencia", hasKey("CRITICA"))
                .body("avaliacoesPorUrgencia", hasKey("ALTA"))
                .body("avaliacoesPorUrgencia", hasKey("MEDIA"))
                .body("avaliacoesPorUrgencia", hasKey("BAIXA"))
                .body("avaliacoesPorDia", hasKey(hoje));
    }

    @Test
    @DisplayName("itens trazem descricao, urgencia e data de envio")
    void itensTrazemDadosExigidos() {
        String id = registrar("Item detalhado do relatorio", 2);

        given()
                .when().get(RELATORIO)
                .then()
                .statusCode(200)
                .body("itens.id", hasItem(id))
                .body("itens.descricao", hasItem("Item detalhado do relatorio"))
                .body("itens.urgencia", hasItem("CRITICA"))
                .body("itens.dataEnvio", notNullValue());
    }

    @Test
    @DisplayName("semana antiga devolve relatorio zerado")
    void semanaAntigaDevolveRelatorioZerado() {
        given()
                .queryParam("referencia", "2018-03-14")
                .when().get(RELATORIO)
                .then()
                .statusCode(200)
                .body("inicio", equalTo("2018-03-12"))
                .body("fim", equalTo("2018-03-18"))
                .body("totalAvaliacoes", equalTo(0))
                .body("mediaNotas", equalTo(0.0f));
    }

    @Test
    @DisplayName("rejeita referencia em formato invalido")
    void rejeitaReferenciaEmFormatoInvalido() {
        given()
                .queryParam("referencia", "26-07-2026")
                .when().get(RELATORIO)
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("mensagens", hasItem(containsString("referencia")));
    }
}
