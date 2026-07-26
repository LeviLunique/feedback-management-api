package br.com.fiap.feedback.duble;

import br.com.fiap.feedback.domain.EnviadorDeEmail;
import java.util.ArrayList;
import java.util.List;

/**
 * Captura os e-mails que seriam enviados, para inspecao de assunto e conteudo.
 */
public class EnviadorDeEmailFake implements EnviadorDeEmail {

    /**
     * @param assunto linha de assunto
     * @param corpoHtml corpo em HTML
     */
    public record EmailEnviado(String assunto, String corpoHtml) {
    }

    private final List<EmailEnviado> enviados = new ArrayList<>();

    @Override
    public void enviarParaAdministradores(String assunto, String corpoHtml) {
        enviados.add(new EmailEnviado(assunto, corpoHtml));
    }

    public List<EmailEnviado> enviados() {
        return List.copyOf(enviados);
    }

    public EmailEnviado unico() {
        if (enviados.size() != 1) {
            throw new IllegalStateException("Esperado exatamente 1 e-mail, mas houve " + enviados.size());
        }
        return enviados.get(0);
    }
}
