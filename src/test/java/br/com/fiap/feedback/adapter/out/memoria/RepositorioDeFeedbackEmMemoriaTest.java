package br.com.fiap.feedback.adapter.out.memoria;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.domain.Avaliacao;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RepositorioDeFeedbackEmMemoriaTest {

    private static final Instant AGORA = Instant.parse("2026-07-26T12:00:00Z");

    private final RepositorioDeFeedbackEmMemoria repositorio = new RepositorioDeFeedbackEmMemoria();

    @Test
    @DisplayName("guarda as avaliacoes na ordem em que sao recebidas")
    void guardaAvaliacoesNaOrdemDeChegada() {
        Avaliacao primeira = Avaliacao.nova("Primeira", 3, AGORA);
        Avaliacao segunda = Avaliacao.nova("Segunda", 7, AGORA);

        repositorio.salvar(primeira);
        repositorio.salvar(segunda);

        assertThat(repositorio.todas()).containsExactly(primeira, segunda);
    }

    @Test
    @DisplayName("comeca vazio")
    void comecaVazio() {
        assertThat(repositorio.todas()).isEmpty();
    }
}
