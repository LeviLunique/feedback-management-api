package br.com.fiap.feedback.adapter.out.sns;

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;

/**
 * Cria o topico de feedbacks criticos no ambiente local de dev e teste.
 *
 * <p>Em producao o topico pertence a infraestrutura como codigo (SAM), por isso
 * este componente nao existe no perfil {@code prod}.
 */
@UnlessBuildProfile("prod")
@ApplicationScoped
public class PreparadorDeTopicoLocal {

    private static final Logger LOG = Logger.getLogger(PreparadorDeTopicoLocal.class);

    private final SnsClient cliente;
    private final String topicoArn;

    public PreparadorDeTopicoLocal(
            SnsClient cliente,
            @ConfigProperty(name = "app.sns.topico-feedback-critico") String topicoArn) {
        this.cliente = cliente;
        this.topicoArn = topicoArn;
    }

    void aoIniciar(@Observes StartupEvent evento) {
        String nome = nomeDoTopico(topicoArn);
        // CreateTopic e idempotente: devolve o ARN existente se ja houver topico.
        cliente.createTopic(CreateTopicRequest.builder().name(nome).build());
        LOG.infof("Topico local '%s' disponivel.", nome);
    }

    static String nomeDoTopico(String arn) {
        return arn.substring(arn.lastIndexOf(':') + 1);
    }
}
