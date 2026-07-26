package br.com.fiap.feedback.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.duble.EnviadorDeEmailFake;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Caso de uso RF-03: conteudo do e-mail de feedback critico (SPEC 6).
 */
class NotificarFeedbackCriticoTest {

    private static final Instant DATA_ENVIO = Instant.parse("2026-07-26T15:30:45Z");

    private EnviadorDeEmailFake enviador;
    private NotificarFeedbackCritico notificarFeedbackCritico;

    @BeforeEach
    void preparar() {
        enviador = new EnviadorDeEmailFake();
        notificarFeedbackCritico = new NotificarFeedbackCritico(enviador);
    }

    private Avaliacao avaliacaoCritica(String descricao) {
        return new Avaliacao(UUID.randomUUID(), descricao, 1, DATA_ENVIO);
    }

    @Test
    @DisplayName("envia um e-mail com o assunto de urgencia")
    void enviaEmailComAssuntoDeUrgencia() {
        notificarFeedbackCritico.executar(avaliacaoCritica("Audio inaudivel"));

        assertThat(enviador.unico().assunto()).isEqualTo(NotificarFeedbackCritico.ASSUNTO);
    }

    @Test
    @DisplayName("corpo traz descricao, urgencia e data de envio")
    void corpoTrazDadosExigidos() {
        notificarFeedbackCritico.executar(avaliacaoCritica("Audio inaudivel"));

        String corpo = enviador.unico().corpoHtml();
        assertThat(corpo).contains("Audio inaudivel");
        assertThat(corpo).contains("CRITICA");
        assertThat(corpo).contains("26/07/2026 15:30:45 UTC");
    }

    @Test
    @DisplayName("neutraliza HTML vindo do texto do estudante")
    void neutralizaHtmlDoEstudante() {
        notificarFeedbackCritico.executar(
                avaliacaoCritica("<script>alert('xss')</script> aula ruim"));

        String corpo = enviador.unico().corpoHtml();
        assertThat(corpo).doesNotContain("<script>");
        assertThat(corpo).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("envia exatamente um e-mail por avaliacao")
    void enviaUmEmailPorAvaliacao() {
        notificarFeedbackCritico.executar(avaliacaoCritica("Primeira"));
        notificarFeedbackCritico.executar(avaliacaoCritica("Segunda"));

        assertThat(enviador.enviados()).hasSize(2);
    }
}
