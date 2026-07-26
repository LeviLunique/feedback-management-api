package br.com.fiap.feedback.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.feedback.application.NotificarFeedbackCritico;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Funcao urgent-notification-fn: traducao do evento SNS para o caso de uso.
 */
class NotificacaoUrgenteHandlerTest {

    private static final String EVENTO_CRITICO = """
            {
              "id": "7b1f0c8e-0000-4000-9000-1234567890ab",
              "descricao": "Aula sem audio do inicio ao fim",
              "nota": 1,
              "urgencia": "CRITICA",
              "dataEnvio": "2026-07-26T15:30:45Z"
            }
            """;

    private EnviadorDeEmailFake enviador;
    private NotificacaoUrgenteHandler handler;

    @BeforeEach
    void preparar() {
        enviador = new EnviadorDeEmailFake();
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        handler = new NotificacaoUrgenteHandler(new NotificarFeedbackCritico(enviador), objectMapper);
    }

    private static SNSEvent eventoCom(String... mensagens) {
        SNSEvent evento = new SNSEvent();
        evento.setRecords(List.of(mensagens).stream().map(mensagem -> {
            SNSEvent.SNS sns = new SNSEvent.SNS();
            sns.setMessage(mensagem);
            SNSEvent.SNSRecord registro = new SNSEvent.SNSRecord();
            registro.setSns(sns);
            return registro;
        }).toList());
        return evento;
    }

    @Test
    @DisplayName("envia e-mail para o feedback critico recebido")
    void enviaEmailParaFeedbackCritico() {
        String resultado = handler.handleRequest(eventoCom(EVENTO_CRITICO), null);

        assertThat(enviador.enviados()).hasSize(1);
        assertThat(resultado).isEqualTo("1 notificacoes");
    }

    @Test
    @DisplayName("e-mail traz descricao, urgencia e data de envio")
    void emailTrazDadosExigidos() {
        handler.handleRequest(eventoCom(EVENTO_CRITICO), null);

        EnviadorDeEmailFake.EmailEnviado email = enviador.enviados().get(0);
        assertThat(email.assunto()).isEqualTo(NotificarFeedbackCritico.ASSUNTO);
        assertThat(email.corpoHtml()).contains("Aula sem audio do inicio ao fim");
        assertThat(email.corpoHtml()).contains("CRITICA");
        assertThat(email.corpoHtml()).contains("26/07/2026 15:30:45 UTC");
    }

    @Test
    @DisplayName("processa todos os registros de um evento em lote")
    void processaTodosOsRegistrosDoLote() {
        String resultado = handler.handleRequest(eventoCom(EVENTO_CRITICO, EVENTO_CRITICO), null);

        assertThat(enviador.enviados()).hasSize(2);
        assertThat(resultado).isEqualTo("2 notificacoes");
    }

    @Test
    @DisplayName("evento sem registros nao envia e-mail")
    void eventoSemRegistrosNaoEnviaEmail() {
        SNSEvent vazio = new SNSEvent();
        vazio.setRecords(List.of());

        String resultado = handler.handleRequest(vazio, null);

        assertThat(enviador.enviados()).isEmpty();
        assertThat(resultado).isEqualTo("0 notificacoes");
    }

    @Test
    @DisplayName("evento nulo nao quebra a funcao")
    void eventoNuloNaoQuebra() {
        assertThat(handler.handleRequest(null, null)).isEqualTo("0 notificacoes");
        assertThat(enviador.enviados()).isEmpty();
    }

    @Test
    @DisplayName("mensagem malformada falha para que o SNS possa reentregar")
    void mensagemMalformadaFalha() {
        assertThatThrownBy(() -> handler.handleRequest(eventoCom("{ nao e json valido"), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("feedback critico");

        assertThat(enviador.enviados()).isEmpty();
    }
}
