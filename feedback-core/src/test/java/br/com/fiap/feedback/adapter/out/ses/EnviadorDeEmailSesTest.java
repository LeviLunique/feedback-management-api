package br.com.fiap.feedback.adapter.out.ses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integracao real contra o SES do LocalStack iniciado pelos Dev Services.
 */
@QuarkusTest
class EnviadorDeEmailSesTest {

    @Inject
    EnviadorDeEmailSes enviador;

    @Test
    @DisplayName("envia e-mail HTML aos administradores")
    void enviaEmailAosAdministradores() {
        assertThatCode(() -> enviador.enviarParaAdministradores(
                        "[URGENTE] Feedback critico recebido",
                        "<html><body><p>Corpo de teste</p></body></html>"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("separa multiplos destinatarios ignorando espacos")
    void separaMultiplosDestinatarios() {
        assertThat(EnviadorDeEmailSes.separar(" um@example.com , dois@example.com "))
                .containsExactly("um@example.com", "dois@example.com");
    }

    @Test
    @DisplayName("descarta entradas vazias na lista de destinatarios")
    void descartaEntradasVazias() {
        assertThat(EnviadorDeEmailSes.separar("um@example.com,,  ,dois@example.com"))
                .containsExactly("um@example.com", "dois@example.com");
    }

    @Test
    @DisplayName("aceita um unico destinatario")
    void aceitaUnicoDestinatario() {
        assertThat(EnviadorDeEmailSes.separar("unico@example.com"))
                .containsExactly("unico@example.com");
    }
}
