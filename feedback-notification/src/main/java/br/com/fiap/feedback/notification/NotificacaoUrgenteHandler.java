package br.com.fiap.feedback.notification;

import br.com.fiap.feedback.application.NotificarFeedbackCritico;
import br.com.fiap.feedback.evento.EventoDeFeedbackCritico;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Named;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Funcao {@code urgent-notification-fn}: consome o topico SNS de feedbacks
 * criticos e dispara o e-mail aos administradores (RF-03).
 *
 * <p>Responsabilidade unica: traduzir o evento do SNS para o caso de uso. Toda
 * a regra de conteudo do e-mail vive em {@link NotificarFeedbackCritico}.
 */
@Named("urgentNotification")
public class NotificacaoUrgenteHandler implements RequestHandler<SNSEvent, String> {

    private static final Logger LOG = Logger.getLogger(NotificacaoUrgenteHandler.class);

    private final NotificarFeedbackCritico notificarFeedbackCritico;
    private final ObjectMapper objectMapper;

    public NotificacaoUrgenteHandler(
            NotificarFeedbackCritico notificarFeedbackCritico, ObjectMapper objectMapper) {
        this.notificarFeedbackCritico = notificarFeedbackCritico;
        this.objectMapper = objectMapper;
    }

    @Override
    public String handleRequest(SNSEvent evento, Context contexto) {
        List<SNSEvent.SNSRecord> registros = evento == null ? null : evento.getRecords();
        if (registros == null || registros.isEmpty()) {
            LOG.warn("Evento SNS sem registros; nada a notificar.");
            return "0 notificacoes";
        }

        int notificadas = 0;
        for (SNSEvent.SNSRecord registro : registros) {
            notificar(registro.getSNS().getMessage());
            notificadas++;
        }
        LOG.infof("Notificacoes de feedback critico enviadas: %d", notificadas);
        return notificadas + " notificacoes";
    }

    private void notificar(String mensagem) {
        try {
            EventoDeFeedbackCritico evento =
                    objectMapper.readValue(mensagem, EventoDeFeedbackCritico.class);
            notificarFeedbackCritico.executar(evento.paraAvaliacao());
        } catch (Exception falha) {
            // Relanca para que o Lambda registre a falha e o SNS possa reentregar.
            throw new IllegalStateException("Falha ao processar evento de feedback critico", falha);
        }
    }
}
