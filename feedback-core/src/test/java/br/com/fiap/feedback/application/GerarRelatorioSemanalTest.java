package br.com.fiap.feedback.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.RelatorioSemanal;
import br.com.fiap.feedback.duble.RepositorioDeFeedbackEmMemoria;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Caso de uso RF-04: recorte semanal do relatorio.
 *
 * <p>2026-07-26 e um domingo; a semana ISO correspondente vai de segunda
 * 2026-07-20 a domingo 2026-07-26.
 */
class GerarRelatorioSemanalTest {

    private static final Instant DOMINGO = Instant.parse("2026-07-26T12:00:00Z");

    private RepositorioDeFeedbackEmMemoria repositorio;
    private GerarRelatorioSemanal gerarRelatorioSemanal;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioDeFeedbackEmMemoria();
        gerarRelatorioSemanal =
                new GerarRelatorioSemanal(repositorio, Clock.fixed(DOMINGO, ZoneOffset.UTC));
    }

    private Avaliacao registrar(int nota, String dia) {
        Avaliacao avaliacao = new Avaliacao(
                UUID.randomUUID(), "Avaliacao de " + dia, nota, Instant.parse(dia + "T10:00:00Z"));
        repositorio.salvar(avaliacao);
        return avaliacao;
    }

    @Test
    @DisplayName("sem referencia, cobre a semana corrente de segunda a domingo")
    void semReferenciaCobreSemanaCorrente() {
        RelatorioSemanal relatorio = gerarRelatorioSemanal.executar(null);

        assertThat(relatorio.inicio()).isEqualTo(LocalDate.parse("2026-07-20"));
        assertThat(relatorio.fim()).isEqualTo(LocalDate.parse("2026-07-26"));
    }

    @Test
    @DisplayName("com referencia no meio da semana, cobre a semana inteira")
    void comReferenciaNoMeioDaSemana() {
        RelatorioSemanal relatorio = gerarRelatorioSemanal.executar(LocalDate.parse("2026-07-23"));

        assertThat(relatorio.inicio()).isEqualTo(LocalDate.parse("2026-07-20"));
        assertThat(relatorio.fim()).isEqualTo(LocalDate.parse("2026-07-26"));
    }

    @Test
    @DisplayName("referencia na segunda-feira mantem a propria semana")
    void referenciaNaSegundaMantemAPropriaSemana() {
        RelatorioSemanal relatorio = gerarRelatorioSemanal.executar(LocalDate.parse("2026-07-20"));

        assertThat(relatorio.inicio()).isEqualTo(LocalDate.parse("2026-07-20"));
        assertThat(relatorio.fim()).isEqualTo(LocalDate.parse("2026-07-26"));
    }

    @Test
    @DisplayName("inclui somente as avaliacoes da semana referenciada")
    void incluiSomenteAvaliacoesDaSemana() {
        registrar(5, "2026-07-19");
        Avaliacao dentro = registrar(8, "2026-07-22");
        registrar(5, "2026-07-27");

        RelatorioSemanal relatorio = gerarRelatorioSemanal.executar(null);

        assertThat(relatorio.itens()).containsExactly(dentro);
        assertThat(relatorio.totalAvaliacoes()).isEqualTo(1);
    }

    @Test
    @DisplayName("agrega notas e urgencias da semana")
    void agregaNotasEUrgenciasDaSemana() {
        registrar(10, "2026-07-20");
        registrar(0, "2026-07-21");

        RelatorioSemanal relatorio = gerarRelatorioSemanal.executar(null);

        assertThat(relatorio.mediaNotas()).isEqualTo(5.0);
        assertThat(relatorio.avaliacoesPorDia()).hasSize(2);
    }

    @Test
    @DisplayName("semana anterior tem recorte proprio")
    void semanaAnteriorTemRecorteProprio() {
        Avaliacao daSemanaPassada = registrar(6, "2026-07-15");

        RelatorioSemanal relatorio = gerarRelatorioSemanal.executar(LocalDate.parse("2026-07-19"));

        assertThat(relatorio.inicio()).isEqualTo(LocalDate.parse("2026-07-13"));
        assertThat(relatorio.fim()).isEqualTo(LocalDate.parse("2026-07-19"));
        assertThat(relatorio.itens()).containsExactly(daSemanaPassada);
    }

    @Test
    @DisplayName("semana sem dados devolve relatorio vazio")
    void semanaSemDadosDevolveRelatorioVazio() {
        RelatorioSemanal relatorio = gerarRelatorioSemanal.executar(LocalDate.parse("2020-01-01"));

        assertThat(relatorio.vazio()).isTrue();
        assertThat(relatorio.totalAvaliacoes()).isZero();
    }
}
