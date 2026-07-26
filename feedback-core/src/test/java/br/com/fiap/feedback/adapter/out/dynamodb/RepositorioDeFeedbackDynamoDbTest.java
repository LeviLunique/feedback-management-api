package br.com.fiap.feedback.adapter.out.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.Urgencia;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integracao real contra o DynamoDB Local iniciado pelos Dev Services.
 *
 * <p>Cada teste usa uma faixa de datas propria e no passado, de modo a nao
 * colidir com os dados gravados pelos testes de API (que usam o dia corrente).
 */
@QuarkusTest
class RepositorioDeFeedbackDynamoDbTest {

    @Inject
    RepositorioDeFeedbackDynamoDb repositorio;

    private Avaliacao gravar(String descricao, int nota, String dia) {
        Avaliacao avaliacao =
                new Avaliacao(UUID.randomUUID(), descricao, nota, Instant.parse(dia + "T09:00:00Z"));
        repositorio.salvar(avaliacao);
        return avaliacao;
    }

    @Test
    @DisplayName("grava e recupera a avaliacao pelo periodo")
    void gravaERecuperaPorPeriodo() {
        Avaliacao gravada = gravar("Integracao basica", 4, "2024-03-10");

        List<Avaliacao> encontradas = repositorio.buscarPorPeriodo(
                LocalDate.parse("2024-03-10"), LocalDate.parse("2024-03-10"));

        assertThat(encontradas).contains(gravada);
    }

    @Test
    @DisplayName("preserva todos os campos na ida e volta")
    void preservaCamposNaIdaEVolta() {
        Avaliacao gravada = gravar("Conteudo com acentuacao: avaliacao", 1, "2024-04-01");

        Avaliacao lida = repositorio
                .buscarPorPeriodo(LocalDate.parse("2024-04-01"), LocalDate.parse("2024-04-01"))
                .stream()
                .filter(avaliacao -> avaliacao.id().equals(gravada.id()))
                .findFirst()
                .orElseThrow();

        assertThat(lida.descricao()).isEqualTo("Conteudo com acentuacao: avaliacao");
        assertThat(lida.nota()).isEqualTo(1);
        assertThat(lida.dataEnvio()).isEqualTo(gravada.dataEnvio());
        assertThat(lida.urgencia()).isEqualTo(Urgencia.CRITICA);
    }

    @Test
    @DisplayName("ignora avaliacoes fora do periodo consultado")
    void ignoraAvaliacoesForaDoPeriodo() {
        gravar("Antes do periodo", 5, "2024-05-01");
        Avaliacao dentro = gravar("Dentro do periodo", 5, "2024-05-03");
        gravar("Depois do periodo", 5, "2024-05-05");

        List<Avaliacao> encontradas = repositorio.buscarPorPeriodo(
                LocalDate.parse("2024-05-02"), LocalDate.parse("2024-05-04"));

        assertThat(encontradas).containsExactly(dentro);
    }

    @Test
    @DisplayName("percorre varios dias e ordena da mais antiga para a mais recente")
    void percorreVariosDiasEmOrdem() {
        Avaliacao primeira = gravar("Primeiro dia", 2, "2024-06-01");
        Avaliacao segunda = gravar("Segundo dia", 6, "2024-06-02");
        Avaliacao terceira = gravar("Terceiro dia", 9, "2024-06-03");

        List<Avaliacao> encontradas = repositorio.buscarPorPeriodo(
                LocalDate.parse("2024-06-01"), LocalDate.parse("2024-06-03"));

        assertThat(encontradas).containsExactly(primeira, segunda, terceira);
    }

    @Test
    @DisplayName("devolve lista vazia para periodo sem dados")
    void devolveListaVaziaParaPeriodoSemDados() {
        List<Avaliacao> encontradas = repositorio.buscarPorPeriodo(
                LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-03"));

        assertThat(encontradas).isEmpty();
    }

    @Test
    @DisplayName("calcula o dia da particao em UTC")
    void calculaDiaDaParticaoEmUtc() {
        assertThat(RepositorioDeFeedbackDynamoDb.diaDe(Instant.parse("2026-07-26T23:59:59Z")))
                .isEqualTo("2026-07-26");
        assertThat(RepositorioDeFeedbackDynamoDb.diaDe(Instant.parse("2026-07-27T00:00:00Z")))
                .isEqualTo("2026-07-27");
    }
}
