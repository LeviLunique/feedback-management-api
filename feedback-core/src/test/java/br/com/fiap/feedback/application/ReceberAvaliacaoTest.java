package br.com.fiap.feedback.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.Urgencia;
import br.com.fiap.feedback.domain.ValidacaoDeDominioException;
import br.com.fiap.feedback.duble.PublicadorDeFeedbackCriticoFake;
import br.com.fiap.feedback.duble.RepositorioDeFeedbackEmMemoria;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Caso de uso RF-01: receber e persistir uma avaliacao, anunciando as criticas
 * para notificacao (RF-03).
 *
 * <p>Usa dubles em memoria: sao implementacoes reais das portas, deterministicas
 * e sem dependencia de infraestrutura.
 */
class ReceberAvaliacaoTest {

    private static final Instant AGORA = Instant.parse("2026-07-26T15:30:00Z");

    private RepositorioDeFeedbackEmMemoria repositorio;
    private PublicadorDeFeedbackCriticoFake publicador;
    private ReceberAvaliacao receberAvaliacao;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioDeFeedbackEmMemoria();
        publicador = new PublicadorDeFeedbackCriticoFake();
        receberAvaliacao =
                new ReceberAvaliacao(repositorio, publicador, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("persiste a avaliacao recebida e a devolve")
    void persisteEDevolveAvaliacao() {
        Avaliacao avaliacao = receberAvaliacao.executar("Aula excelente", 9);

        assertThat(avaliacao.id()).isNotNull();
        assertThat(avaliacao.urgencia()).isEqualTo(Urgencia.BAIXA);
        assertThat(repositorio.todas()).containsExactly(avaliacao);
    }

    @Test
    @DisplayName("carimba a data de envio com o relogio da aplicacao")
    void carimbaDataDeEnvioComORelogio() {
        Avaliacao avaliacao = receberAvaliacao.executar("Aula boa", 8);

        assertThat(avaliacao.dataEnvio()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("classifica nota baixa como critica")
    void classificaNotaBaixaComoCritica() {
        Avaliacao avaliacao = receberAvaliacao.executar("Nao consegui assistir", 0);

        assertThat(avaliacao.urgencia()).isEqualTo(Urgencia.CRITICA);
        assertThat(avaliacao.exigeNotificacaoImediata()).isTrue();
    }

    @Test
    @DisplayName("nao persiste avaliacao invalida")
    void naoPersisteAvaliacaoInvalida() {
        assertThatThrownBy(() -> receberAvaliacao.executar("", 5))
                .isInstanceOf(ValidacaoDeDominioException.class);

        assertThat(repositorio.todas()).isEmpty();
    }

    @ParameterizedTest(name = "nota {0} anuncia feedback critico")
    @ValueSource(ints = {0, 1, 2})
    void anunciaFeedbackCritico(int nota) {
        Avaliacao avaliacao = receberAvaliacao.executar("Problema grave na aula", nota);

        assertThat(publicador.publicadas()).containsExactly(avaliacao);
    }

    @ParameterizedTest(name = "nota {0} nao anuncia feedback critico")
    @ValueSource(ints = {3, 5, 6, 7, 8, 10})
    void naoAnunciaQuandoNaoEhCritico(int nota) {
        receberAvaliacao.executar("Aula dentro do esperado", nota);

        assertThat(publicador.publicadas()).isEmpty();
    }

    @Test
    @DisplayName("nao anuncia quando a avaliacao e invalida")
    void naoAnunciaAvaliacaoInvalida() {
        assertThatThrownBy(() -> receberAvaliacao.executar("", 0))
                .isInstanceOf(ValidacaoDeDominioException.class);

        assertThat(publicador.publicadas()).isEmpty();
    }
}
