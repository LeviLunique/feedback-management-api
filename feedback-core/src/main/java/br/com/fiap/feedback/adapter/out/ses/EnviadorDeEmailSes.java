package br.com.fiap.feedback.adapter.out.ses;

import br.com.fiap.feedback.domain.EnviadorDeEmail;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

/**
 * Envia e-mails aos administradores via Amazon SES (SPEC 6).
 *
 * <p>Remetente e destinatarios vem de configuracao ({@code SENDER_EMAIL} e
 * {@code ADMIN_EMAILS}), nunca do codigo, para atender a exigencia de nao ter
 * dados de ambiente versionados.
 */
@ApplicationScoped
public class EnviadorDeEmailSes implements EnviadorDeEmail {

    private static final String UTF_8 = "UTF-8";

    private final SesClient cliente;
    private final String remetente;
    private final List<String> destinatarios;

    public EnviadorDeEmailSes(
            SesClient cliente,
            @ConfigProperty(name = "app.ses.remetente") String remetente,
            @ConfigProperty(name = "app.ses.administradores") String administradores) {
        this.cliente = cliente;
        this.remetente = remetente;
        this.destinatarios = separar(administradores);
    }

    @Override
    public void enviarParaAdministradores(String assunto, String corpoHtml) {
        cliente.sendEmail(SendEmailRequest.builder()
                .source(remetente)
                .destination(Destination.builder().toAddresses(destinatarios).build())
                .message(Message.builder()
                        .subject(conteudo(assunto))
                        .body(Body.builder().html(conteudo(corpoHtml)).build())
                        .build())
                .build());
    }

    private static Content conteudo(String texto) {
        return Content.builder().charset(UTF_8).data(texto).build();
    }

    static List<String> separar(String enderecos) {
        return Arrays.stream(enderecos.split(","))
                .map(String::strip)
                .filter(endereco -> !endereco.isEmpty())
                .toList();
    }
}
