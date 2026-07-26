package br.com.fiap.feedback.adapter.out.sns;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.PublicadorDeFeedbackCritico;
import br.com.fiap.feedback.evento.EventoDeFeedbackCritico;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Publica avaliacoes criticas em um topico SNS (SPEC 2.1).
 *
 * <p>O topico desacopla a ingestao do envio de e-mail: a API responde ao
 * estudante sem esperar o SES, e a funcao de notificacao pode falhar e ser
 * reprocessada sem afetar o registro da avaliacao.
 */
@ApplicationScoped
public class PublicadorDeFeedbackCriticoSns implements PublicadorDeFeedbackCritico {

    private final SnsClient cliente;
    private final ObjectMapper objectMapper;
    private final String topicoArn;

    public PublicadorDeFeedbackCriticoSns(
            SnsClient cliente,
            ObjectMapper objectMapper,
            @ConfigProperty(name = "app.sns.topico-feedback-critico") String topicoArn) {
        this.cliente = cliente;
        this.objectMapper = objectMapper;
        this.topicoArn = topicoArn;
    }

    @Override
    public void publicar(Avaliacao avaliacao) {
        cliente.publish(PublishRequest.builder()
                .topicArn(topicoArn)
                .subject("Feedback critico")
                .message(serializar(EventoDeFeedbackCritico.de(avaliacao)))
                .build());
    }

    private String serializar(EventoDeFeedbackCritico evento) {
        try {
            return objectMapper.writeValueAsString(evento);
        } catch (JsonProcessingException falha) {
            throw new IllegalStateException("Nao foi possivel serializar o evento de feedback critico", falha);
        }
    }
}
