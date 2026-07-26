package br.com.fiap.feedback.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Agregacao exigida pelo relatorio semanal (RF-04 / SPEC 5.3).
 */
class RelatorioSemanalTest {

    private static final LocalDate INICIO = LocalDate.parse("2026-07-20");
    private static final LocalDate FIM = LocalDate.parse("2026-07-26");

    private static Avaliacao avaliacao(int nota, String dia) {
        return new Avaliacao(UUID.randomUUID(), "Avaliacao de " + dia, nota, Instant.parse(dia + "T10:00:00Z"));
    }

    @Test
    @DisplayName("calcula a media das notas")
    void calculaMediaDasNotas() {
        RelatorioSemanal relatorio = RelatorioSemanal.de(INICIO, FIM,
                List.of(avaliacao(10, "2026-07-20"), avaliacao(5, "2026-07-21")));

        assertThat(relatorio.mediaNotas()).isEqualTo(7.5);
    }

    @Test
    @DisplayName("arredonda a media para duas casas decimais")
    void arredondaMediaParaDuasCasas() {
        RelatorioSemanal relatorio = RelatorioSemanal.de(INICIO, FIM,
                List.of(avaliacao(1, "2026-07-20"), avaliacao(2, "2026-07-21"), avaliacao(2, "2026-07-22")));

        assertThat(relatorio.mediaNotas()).isEqualTo(1.67);
    }

    @Test
    @DisplayName("conta o total de avaliacoes")
    void contaTotalDeAvaliacoes() {
        RelatorioSemanal relatorio = RelatorioSemanal.de(INICIO, FIM,
                List.of(avaliacao(8, "2026-07-20"), avaliacao(3, "2026-07-21")));

        assertThat(relatorio.totalAvaliacoes()).isEqualTo(2);
    }

    @Test
    @DisplayName("agrupa a quantidade de avaliacoes por dia")
    void agrupaQuantidadePorDia() {
        RelatorioSemanal relatorio = RelatorioSemanal.de(INICIO, FIM, List.of(
                avaliacao(8, "2026-07-20"),
                avaliacao(3, "2026-07-20"),
                avaliacao(5, "2026-07-22")));

        assertThat(relatorio.avaliacoesPorDia()).containsExactly(
                entry(LocalDate.parse("2026-07-20"), 2L),
                entry(LocalDate.parse("2026-07-22"), 1L));
    }

    @Test
    @DisplayName("agrupa por urgencia sempre com as quatro faixas")
    void agrupaPorUrgenciaComTodasAsFaixas() {
        RelatorioSemanal relatorio = RelatorioSemanal.de(INICIO, FIM,
                List.of(avaliacao(1, "2026-07-20"), avaliacao(9, "2026-07-21"), avaliacao(10, "2026-07-21")));

        assertThat(relatorio.avaliacoesPorUrgencia())
                .containsOnlyKeys(Urgencia.CRITICA, Urgencia.ALTA, Urgencia.MEDIA, Urgencia.BAIXA)
                .contains(
                        entry(Urgencia.CRITICA, 1L),
                        entry(Urgencia.ALTA, 0L),
                        entry(Urgencia.MEDIA, 0L),
                        entry(Urgencia.BAIXA, 2L));
    }

    @Test
    @DisplayName("preserva os itens do periodo")
    void preservaItensDoPeriodo() {
        Avaliacao primeira = avaliacao(4, "2026-07-20");
        Avaliacao segunda = avaliacao(6, "2026-07-21");

        RelatorioSemanal relatorio = RelatorioSemanal.de(INICIO, FIM, List.of(primeira, segunda));

        assertThat(relatorio.itens()).containsExactly(primeira, segunda);
    }

    @Test
    @DisplayName("registra o periodo coberto")
    void registraPeriodoCoberto() {
        RelatorioSemanal relatorio = RelatorioSemanal.de(INICIO, FIM, List.of());

        assertThat(relatorio.inicio()).isEqualTo(INICIO);
        assertThat(relatorio.fim()).isEqualTo(FIM);
    }

    @Test
    @DisplayName("semana sem avaliacoes produz relatorio zerado, nao erro")
    void semanaSemAvaliacoes() {
        RelatorioSemanal relatorio = RelatorioSemanal.de(INICIO, FIM, List.of());

        assertThat(relatorio.totalAvaliacoes()).isZero();
        assertThat(relatorio.mediaNotas()).isZero();
        assertThat(relatorio.avaliacoesPorDia()).isEmpty();
        assertThat(relatorio.avaliacoesPorUrgencia()).containsValues(0L, 0L, 0L, 0L);
        assertThat(relatorio.itens()).isEmpty();
        assertThat(relatorio.vazio()).isTrue();
    }

    @Test
    @DisplayName("relatorio com avaliacoes nao e vazio")
    void relatorioComAvaliacoesNaoEhVazio() {
        assertThat(RelatorioSemanal.de(INICIO, FIM, List.of(avaliacao(7, "2026-07-20"))).vazio()).isFalse();
    }
}
