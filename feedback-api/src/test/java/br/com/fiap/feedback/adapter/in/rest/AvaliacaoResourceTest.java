package br.com.fiap.feedback.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato do endpoint POST /api/v1/avaliacao (SPEC 5.1).
 */
@QuarkusTest
class AvaliacaoResourceTest {

    private static final String ENDPOINT = "/api/v1/avaliacao";

    @Test
    @DisplayName("registra avaliacao valida e responde 201 com o recurso criado")
    void registraAvaliacaoValida() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "descricao": "Aula muito bem explicada", "nota": 9 }
                        """)
                .when().post(ENDPOINT)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("descricao", equalTo("Aula muito bem explicada"))
                .body("nota", equalTo(9))
                .body("urgencia", equalTo("BAIXA"))
                .body("dataEnvio", notNullValue());
    }

    @Test
    @DisplayName("classifica nota baixa como CRITICA")
    void classificaNotaBaixaComoCritica() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "descricao": "Audio com falhas graves", "nota": 1 }
                        """)
                .when().post(ENDPOINT)
                .then()
                .statusCode(201)
                .body("urgencia", equalTo("CRITICA"));
    }

    @Test
    @DisplayName("serializa a data de envio em ISO-8601 UTC")
    void serializaDataEnvioEmIso8601() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "descricao": "Conteudo adequado", "nota": 7 }
                        """)
                .when().post(ENDPOINT)
                .then()
                .statusCode(201)
                .body("dataEnvio", matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$"));
    }

    @Test
    @DisplayName("ignora urgencia enviada pelo cliente e usa a derivada da nota")
    void ignoraUrgenciaEnviadaPeloCliente() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "descricao": "Tentando forjar urgencia", "nota": 10, "urgencia": "CRITICA" }
                        """)
                .when().post(ENDPOINT)
                .then()
                .statusCode(201)
                .body("urgencia", equalTo("BAIXA"));
    }

    @Test
    @DisplayName("rejeita nota acima da escala com corpo de erro padrao")
    void rejeitaNotaAcimaDaEscala() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "descricao": "Nota invalida", "nota": 11 }
                        """)
                .when().post(ENDPOINT)
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("erro", notNullValue())
                .body("mensagens", hasSize(1))
                .body("mensagens", hasItem(containsString("nota")));
    }

    @Test
    @DisplayName("rejeita nota abaixo da escala")
    void rejeitaNotaAbaixoDaEscala() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "descricao": "Nota invalida", "nota": -1 }
                        """)
                .when().post(ENDPOINT)
                .then()
                .statusCode(400)
                .body("mensagens", hasItem(containsString("nota")));
    }

    @Test
    @DisplayName("rejeita requisicao sem nota")
    void rejeitaRequisicaoSemNota() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "descricao": "Faltou a nota" }
                        """)
                .when().post(ENDPOINT)
                .then()
                .statusCode(400)
                .body("mensagens", hasItem(containsString("nota")));
    }

    @Test
    @DisplayName("rejeita descricao vazia")
    void rejeitaDescricaoVazia() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "descricao": "   ", "nota": 5 }
                        """)
                .when().post(ENDPOINT)
                .then()
                .statusCode(400)
                .body("mensagens", hasItem(containsString("descricao")));
    }

    @Test
    @DisplayName("acumula as mensagens quando ha mais de um erro")
    void acumulaMensagensDeErro() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "descricao": "", "nota": 42 }
                        """)
                .when().post(ENDPOINT)
                .then()
                .statusCode(400)
                .body("mensagens", hasSize(2));
    }

    @Test
    @DisplayName("rejeita corpo vazio sem estourar erro interno")
    void rejeitaCorpoVazio() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .when().post(ENDPOINT)
                .then()
                .statusCode(400)
                .body("mensagens", hasSize(2));
    }

    @Test
    @DisplayName("nao vaza detalhes internos no corpo de erro")
    void naoVazaDetalhesInternos() {
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        { "descricao": "Nota invalida", "nota": 99 }
                        """)
                .when().post(ENDPOINT)
                .then()
                .statusCode(400)
                .body(allOf(
                        not(containsString("br.com.fiap")),
                        not(containsString("Exception"))));
    }
}
