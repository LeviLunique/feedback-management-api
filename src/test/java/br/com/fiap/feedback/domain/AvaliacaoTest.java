package br.com.fiap.feedback.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Invariantes da entidade Avaliacao (SPEC 4.1 e 4.3).
 */
class AvaliacaoTest {

    private static final Instant AGORA = Instant.parse("2026-07-26T12:00:00Z");

    @Test
    @DisplayName("gera id, registra data de envio e deriva a urgencia")
    void criaAvaliacaoValida() {
        Avaliacao avaliacao = Avaliacao.nova("Aula muito boa", 9, AGORA);

        assertThat(avaliacao.id()).isNotNull();
        assertThat(avaliacao.descricao()).isEqualTo("Aula muito boa");
        assertThat(avaliacao.nota()).isEqualTo(9);
        assertThat(avaliacao.dataEnvio()).isEqualTo(AGORA);
        assertThat(avaliacao.urgencia()).isEqualTo(Urgencia.BAIXA);
    }

    @Test
    @DisplayName("gera identificadores distintos para cada avaliacao")
    void geraIdentificadoresDistintos() {
        Avaliacao primeira = Avaliacao.nova("Primeira", 5, AGORA);
        Avaliacao segunda = Avaliacao.nova("Segunda", 5, AGORA);

        assertThat(primeira.id()).isNotEqualTo(segunda.id());
    }

    @Test
    @DisplayName("remove espacos em branco das bordas da descricao")
    void removeEspacosDaDescricao() {
        Avaliacao avaliacao = Avaliacao.nova("   Aula boa   ", 8, AGORA);

        assertThat(avaliacao.descricao()).isEqualTo("Aula boa");
    }

    @Test
    @DisplayName("nota critica marca a avaliacao para notificacao imediata")
    void notaCriticaExigeNotificacao() {
        Avaliacao avaliacao = Avaliacao.nova("Audio impossivel de ouvir", 1, AGORA);

        assertThat(avaliacao.urgencia()).isEqualTo(Urgencia.CRITICA);
        assertThat(avaliacao.exigeNotificacaoImediata()).isTrue();
    }

    @ParameterizedTest(name = "descricao \"{0}\" e rejeitada")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void rejeitaDescricaoVazia(String descricao) {
        assertThatThrownBy(() -> Avaliacao.nova(descricao, 5, AGORA))
                .isInstanceOf(ValidacaoDeDominioException.class)
                .satisfies(erro -> assertThat(((ValidacaoDeDominioException) erro).mensagens())
                        .anyMatch(m -> m.contains("descricao")));
    }

    @Test
    @DisplayName("rejeita descricao acima do tamanho maximo")
    void rejeitaDescricaoMuitoLonga() {
        String longa = "x".repeat(Avaliacao.TAMANHO_MAXIMO_DA_DESCRICAO + 1);

        assertThatThrownBy(() -> Avaliacao.nova(longa, 5, AGORA))
                .isInstanceOf(ValidacaoDeDominioException.class);
    }

    @Test
    @DisplayName("aceita descricao exatamente no tamanho maximo")
    void aceitaDescricaoNoLimite() {
        String limite = "x".repeat(Avaliacao.TAMANHO_MAXIMO_DA_DESCRICAO);

        assertThat(Avaliacao.nova(limite, 5, AGORA).descricao()).hasSize(Avaliacao.TAMANHO_MAXIMO_DA_DESCRICAO);
    }

    @ParameterizedTest(name = "nota {0} e rejeitada")
    @ValueSource(ints = {-1, 11})
    void rejeitaNotaForaDaEscala(int nota) {
        assertThatThrownBy(() -> Avaliacao.nova("Descricao valida", nota, AGORA))
                .isInstanceOf(ValidacaoDeDominioException.class)
                .satisfies(erro -> assertThat(((ValidacaoDeDominioException) erro).mensagens())
                        .anyMatch(m -> m.contains("nota")));
    }

    @Test
    @DisplayName("rejeita nota ausente")
    void rejeitaNotaAusente() {
        assertThatThrownBy(() -> Avaliacao.nova("Descricao valida", null, AGORA))
                .isInstanceOf(ValidacaoDeDominioException.class)
                .satisfies(erro -> assertThat(((ValidacaoDeDominioException) erro).mensagens())
                        .anyMatch(m -> m.contains("nota")));
    }

    @Test
    @DisplayName("rejeita data de envio ausente")
    void rejeitaDataDeEnvioAusente() {
        assertThatThrownBy(() -> Avaliacao.nova("Descricao valida", 5, null))
                .isInstanceOf(ValidacaoDeDominioException.class);
    }

    @Test
    @DisplayName("rejeita nota e data de envio ausentes ao mesmo tempo")
    void rejeitaNotaEDataDeEnvioAusentes() {
        assertThatThrownBy(() -> Avaliacao.nova("Descricao valida", null, null))
                .isInstanceOf(ValidacaoDeDominioException.class)
                .satisfies(erro -> assertThat(((ValidacaoDeDominioException) erro).mensagens()).hasSize(2));
    }

    @Test
    @DisplayName("rejeita reconstituicao sem identificador")
    void rejeitaReconstituicaoSemIdentificador() {
        assertThatThrownBy(() -> new Avaliacao(null, "Descricao valida", 5, AGORA))
                .isInstanceOf(ValidacaoDeDominioException.class)
                .satisfies(erro -> assertThat(((ValidacaoDeDominioException) erro).mensagens())
                        .anyMatch(m -> m.contains("id")));
    }

    @Test
    @DisplayName("acumula todas as mensagens de erro de uma vez")
    void acumulaMensagensDeErro() {
        assertThatThrownBy(() -> Avaliacao.nova("", 42, AGORA))
                .isInstanceOf(ValidacaoDeDominioException.class)
                .satisfies(erro -> assertThat(((ValidacaoDeDominioException) erro).mensagens()).hasSize(2));
    }

    @Test
    @DisplayName("reconstitui avaliacao ja persistida preservando o id")
    void reconstituiAvaliacaoPersistida() {
        UUID id = UUID.randomUUID();

        Avaliacao avaliacao = new Avaliacao(id, "Aula boa", 7, AGORA);

        assertThat(avaliacao.id()).isEqualTo(id);
        assertThat(avaliacao.urgencia()).isEqualTo(Urgencia.MEDIA);
    }
}
