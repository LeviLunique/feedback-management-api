package br.com.fiap.feedback.notification;

import br.com.fiap.feedback.domain.EnviadorDeEmail;
import java.util.ArrayList;
import java.util.List;

/**
 * Captura os e-mails que o handler dispararia, sem tocar no SES.
 */
class EnviadorDeEmailFake implements EnviadorDeEmail {

    record EmailEnviado(String assunto, String corpoHtml) {
    }

    private final List<EmailEnviado> enviados = new ArrayList<>();

    @Override
    public void enviarParaAdministradores(String assunto, String corpoHtml) {
        enviados.add(new EmailEnviado(assunto, corpoHtml));
    }

    List<EmailEnviado> enviados() {
        return List.copyOf(enviados);
    }
}
