package br.com.fiap.feedback.adapter.out.ses;

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.VerifyEmailIdentityRequest;

/**
 * Verifica remetente e destinatarios no SES local de dev e teste.
 *
 * <p>O SES so aceita enviar de/para enderecos verificados; em producao essa
 * verificacao e feita uma unica vez na conta AWS, fora da aplicacao.
 */
@UnlessBuildProfile("prod")
@ApplicationScoped
public class PreparadorDeIdentidadeLocal {

    private static final Logger LOG = Logger.getLogger(PreparadorDeIdentidadeLocal.class);

    private final SesClient cliente;
    private final List<String> enderecos;

    public PreparadorDeIdentidadeLocal(
            SesClient cliente,
            @ConfigProperty(name = "app.ses.remetente") String remetente,
            @ConfigProperty(name = "app.ses.administradores") String administradores) {
        this.cliente = cliente;
        List<String> todos = new ArrayList<>();
        todos.add(remetente);
        todos.addAll(EnviadorDeEmailSes.separar(administradores));
        this.enderecos = List.copyOf(todos);
    }

    void aoIniciar(@Observes StartupEvent evento) {
        enderecos.forEach(endereco -> cliente.verifyEmailIdentity(
                VerifyEmailIdentityRequest.builder().emailAddress(endereco).build()));
        LOG.infof("Identidades locais verificadas no SES: %d", enderecos.size());
    }
}
