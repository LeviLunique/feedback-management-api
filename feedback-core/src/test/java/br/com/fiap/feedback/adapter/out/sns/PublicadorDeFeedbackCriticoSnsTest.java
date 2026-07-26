package br.com.fiap.feedback.adapter.out.sns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.fiap.feedback.domain.Avaliacao;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integracao real contra o SNS do LocalStack iniciado pelos Dev Services.
 */
@QuarkusTest
class PublicadorDeFeedbackCriticoSnsTest {

    @Inject
    PublicadorDeFeedbackCriticoSns publicador;

    @Test
    @DisplayName("publica avaliacao critica no topico")
    void publicaAvaliacaoCritica() {
        Avaliacao critica = new Avaliacao(
                UUID.randomUUID(), "Aula sem audio do inicio ao fim", 0, Instant.parse("2026-07-26T10:00:00Z"));

        assertThatCode(() -> publicador.publicar(critica)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("publica descricao com caracteres especiais sem quebrar o JSON")
    void publicaDescricaoComCaracteresEspeciais() {
        Avaliacao critica = new Avaliacao(
                UUID.randomUUID(),
                "Aspas \" barra \\ e acento: avaliacao pessima",
                1,
                Instant.parse("2026-07-26T11:00:00Z"));

        assertThatCode(() -> publicador.publicar(critica)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("extrai o nome do topico a partir do ARN")
    void extraiNomeDoTopicoDoArn() {
        assertThat(PreparadorDeTopicoLocal.nomeDoTopico(
                        "arn:aws:sns:us-east-1:123456789012:feedback-critico"))
                .isEqualTo("feedback-critico");
    }
}
