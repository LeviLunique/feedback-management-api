package br.com.fiap.feedback.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.Urgencia;
import br.com.fiap.feedback.domain.ValidacaoDeDominioException;
import br.com.fiap.feedback.duble.RepositorioDeFeedbackEmMemoria;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Caso de uso RF-01: receber e persistir uma avaliacao.
 *
 * <p>Usa o repositorio em memoria como duble: e uma implementacao real da
 * porta, deterministica e sem dependencia de infraestrutura.
 */
class ReceberAvaliacaoTest {

    private static final Instant AGORA = Instant.parse("2026-07-26T15:30:00Z");

    private RepositorioDeFeedbackEmMemoria repositorio;
    private ReceberAvaliacao receberAvaliacao;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioDeFeedbackEmMemoria();
        receberAvaliacao = new ReceberAvaliacao(repositorio, Clock.fixed(AGORA, ZoneOffset.UTC));
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
}
