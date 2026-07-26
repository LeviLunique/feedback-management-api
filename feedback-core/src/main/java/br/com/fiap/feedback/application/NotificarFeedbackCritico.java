package br.com.fiap.feedback.application;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.EnviadorDeEmail;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.format.DateTimeFormatter;

/**
 * Caso de uso RF-03: avisa os administradores sobre um feedback critico.
 *
 * <p>O e-mail carrega exatamente os dados exigidos pelo enunciado: descricao,
 * urgencia e data de envio.
 */
@ApplicationScoped
public class NotificarFeedbackCritico {

    /** Assunto exigido pelo enunciado para o aviso de urgencia (SPEC 6). */
    public static final String ASSUNTO = "[URGENTE] Feedback critico recebido";

    private static final DateTimeFormatter FORMATO_DA_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'UTC'").withZone(java.time.ZoneOffset.UTC);

    private final EnviadorDeEmail enviadorDeEmail;

    public NotificarFeedbackCritico(EnviadorDeEmail enviadorDeEmail) {
        this.enviadorDeEmail = enviadorDeEmail;
    }

    public void executar(Avaliacao avaliacao) {
        enviadorDeEmail.enviarParaAdministradores(ASSUNTO, corpoDoEmail(avaliacao));
    }

    private static String corpoDoEmail(Avaliacao avaliacao) {
        return """
                <html><body style="font-family: Arial, sans-serif;">
                  <h2>Feedback critico recebido</h2>
                  <p>Uma avaliacao exige atencao imediata dos administradores.</p>
                  <table cellpadding="6" style="border-collapse: collapse;">
                    <tr><td><strong>Descricao</strong></td><td>%s</td></tr>
                    <tr><td><strong>Urgencia</strong></td><td>%s</td></tr>
                    <tr><td><strong>Data de envio</strong></td><td>%s</td></tr>
                    <tr><td><strong>Nota</strong></td><td>%d</td></tr>
                  </table>
                </body></html>
                """
                .formatted(
                        escapar(avaliacao.descricao()),
                        avaliacao.urgencia(),
                        FORMATO_DA_DATA.format(avaliacao.dataEnvio()),
                        avaliacao.nota());
    }

    /**
     * Neutraliza HTML vindo do texto do estudante, que e entrada nao confiavel.
     */
    private static String escapar(String texto) {
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
