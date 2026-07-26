package br.com.fiap.feedback.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.Urgencia;
import br.com.fiap.feedback.domain.ValidacaoDeDominioException;
import br.com.fiap.feedback.duble.RepositorioDeFeedbackEmMemoria;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Caso de uso RF-05: consultar avaliacoes por periodo e urgencia.
 */
class ConsultarAvaliacoesTest {

    private static final Instant HOJE = Instant.parse("2026-07-26T12:00:00Z");

    private RepositorioDeFeedbackEmMemoria repositorio;
    private ConsultarAvaliacoes consultarAvaliacoes;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioDeFeedbackEmMemoria();
        consultarAvaliacoes = new ConsultarAvaliacoes(repositorio, Clock.fixed(HOJE, ZoneOffset.UTC));
    }

    private Avaliacao registrar(String descricao, int nota, String dia) {
        Avaliacao avaliacao =
                new Avaliacao(UUID.randomUUID(), descricao, nota, Instant.parse(dia + "T10:00:00Z"));
        repositorio.salvar(avaliacao);
        return avaliacao;
    }

    @Test
    @DisplayName("sem filtros, devolve os ultimos 7 dias")
    void semFiltrosDevolveUltimosSeteDias() {
        Avaliacao dentro = registrar("Dentro da janela", 5, "2026-07-20");
        registrar("Fora da janela", 5, "2026-07-19");

        List<Avaliacao> encontradas = consultarAvaliacoes.executar(null, null, null);

        assertThat(encontradas).containsExactly(dentro);
    }

    @Test
    @DisplayName("inclui avaliacoes do proprio dia de hoje")
    void incluiAvaliacoesDeHoje() {
        Avaliacao hoje = registrar("Enviada hoje", 8, "2026-07-26");

        assertThat(consultarAvaliacoes.executar(null, null, null)).containsExactly(hoje);
    }

    @Test
    @DisplayName("respeita o periodo informado")
    void respeitaPeriodoInformado() {
        registrar("Antes", 5, "2026-06-01");
        Avaliacao dentro = registrar("Dentro", 5, "2026-06-15");
        registrar("Depois", 5, "2026-06-30");

        List<Avaliacao> encontradas = consultarAvaliacoes.executar(
                LocalDate.parse("2026-06-10"), LocalDate.parse("2026-06-20"), null);

        assertThat(encontradas).containsExactly(dentro);
    }

    @Test
    @DisplayName("filtra por urgencia")
    void filtraPorUrgencia() {
        Avaliacao critica = registrar("Critica", 1, "2026-07-25");
        registrar("Baixa", 9, "2026-07-25");

        List<Avaliacao> encontradas = consultarAvaliacoes.executar(null, null, Urgencia.CRITICA);

        assertThat(encontradas).containsExactly(critica);
    }

    @Test
    @DisplayName("devolve lista vazia quando nada se encaixa")
    void devolveListaVaziaQuandoNadaSeEncaixa() {
        registrar("Antiga", 5, "2020-01-01");

        assertThat(consultarAvaliacoes.executar(null, null, null)).isEmpty();
    }

    @Test
    @DisplayName("rejeita periodo com inicio depois do fim")
    void rejeitaPeriodoInvertido() {
        assertThatThrownBy(() -> consultarAvaliacoes.executar(
                        LocalDate.parse("2026-07-20"), LocalDate.parse("2026-07-10"), null))
                .isInstanceOf(ValidacaoDeDominioException.class)
                .satisfies(erro -> assertThat(((ValidacaoDeDominioException) erro).mensagens())
                        .anyMatch(m -> m.contains("dataInicio")));
    }

    @Test
    @DisplayName("rejeita periodo longo demais para evitar consulta ilimitada")
    void rejeitaPeriodoLongoDemais() {
        assertThatThrownBy(() -> consultarAvaliacoes.executar(
                        LocalDate.parse("2020-01-01"), LocalDate.parse("2026-07-26"), null))
                .isInstanceOf(ValidacaoDeDominioException.class)
                .satisfies(erro -> assertThat(((ValidacaoDeDominioException) erro).mensagens())
                        .anyMatch(m -> m.contains("periodo")));
    }

    @Test
    @DisplayName("aceita periodo de um unico dia")
    void aceitaPeriodoDeUmDia() {
        Avaliacao doDia = registrar("Do dia", 6, "2026-05-05");

        List<Avaliacao> encontradas = consultarAvaliacoes.executar(
                LocalDate.parse("2026-05-05"), LocalDate.parse("2026-05-05"), null);

        assertThat(encontradas).containsExactly(doDia);
    }
}
