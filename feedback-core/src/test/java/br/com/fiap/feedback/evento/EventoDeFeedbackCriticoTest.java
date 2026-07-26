package br.com.fiap.feedback.evento;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.Urgencia;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O evento e o contrato entre a funcao de ingestao e a de notificacao: se a
 * ida e volta pelo JSON perder um campo, a notificacao sai errada.
 */
class EventoDeFeedbackCriticoTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final Avaliacao CRITICA = new Avaliacao(
            UUID.randomUUID(), "Aula sem audio", 1, Instant.parse("2026-07-26T15:30:45Z"));

    @Test
    @DisplayName("converte a avaliacao preservando os dados exigidos pelo e-mail")
    void converteAvaliacao() {
        EventoDeFeedbackCritico evento = EventoDeFeedbackCritico.de(CRITICA);

        assertThat(evento.id()).isEqualTo(CRITICA.id());
        assertThat(evento.descricao()).isEqualTo("Aula sem audio");
        assertThat(evento.nota()).isEqualTo(1);
        assertThat(evento.urgencia()).isEqualTo(Urgencia.CRITICA);
        assertThat(evento.dataEnvio()).isEqualTo(CRITICA.dataEnvio());
    }

    @Test
    @DisplayName("sobrevive a ida e volta pelo JSON do topico")
    void sobreviveIdaEVoltaPeloJson() throws Exception {
        String json = objectMapper.writeValueAsString(EventoDeFeedbackCritico.de(CRITICA));

        EventoDeFeedbackCritico lido = objectMapper.readValue(json, EventoDeFeedbackCritico.class);

        assertThat(lido.paraAvaliacao()).isEqualTo(CRITICA);
    }

    @Test
    @DisplayName("reconstroi a avaliacao com a urgencia derivada da nota")
    void reconstroiAvaliacaoComUrgenciaDerivada() {
        Avaliacao reconstruida = EventoDeFeedbackCritico.de(CRITICA).paraAvaliacao();

        assertThat(reconstruida.urgencia()).isEqualTo(Urgencia.CRITICA);
        assertThat(reconstruida.exigeNotificacaoImediata()).isTrue();
    }
}
