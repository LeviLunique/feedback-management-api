package br.com.fiap.feedback.application;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.EnviadorDeEmail;
import br.com.fiap.feedback.domain.RelatorioSemanal;
import br.com.fiap.feedback.domain.Urgencia;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Caso de uso RF-04: envia o relatorio semanal por e-mail aos administradores.
 *
 * <p>O conteudo cobre exatamente o que o enunciado pede: descricao, urgencia e
 * data de envio de cada avaliacao, quantidade por dia, quantidade por urgencia e
 * media das notas.
 */
@ApplicationScoped
public class EnviarRelatorioSemanal {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private final GerarRelatorioSemanal gerarRelatorioSemanal;
    private final EnviadorDeEmail enviadorDeEmail;

    public EnviarRelatorioSemanal(
            GerarRelatorioSemanal gerarRelatorioSemanal, EnviadorDeEmail enviadorDeEmail) {
        this.gerarRelatorioSemanal = gerarRelatorioSemanal;
        this.enviadorDeEmail = enviadorDeEmail;
    }

    public RelatorioSemanal executar(LocalDate referencia) {
        RelatorioSemanal relatorio = gerarRelatorioSemanal.executar(referencia);
        enviadorDeEmail.enviarParaAdministradores(assunto(relatorio), corpo(relatorio));
        return relatorio;
    }

    static String assunto(RelatorioSemanal relatorio) {
        return "Relatorio semanal de feedbacks - %s a %s"
                .formatted(DIA.format(relatorio.inicio()), DIA.format(relatorio.fim()));
    }

    private static String corpo(RelatorioSemanal relatorio) {
        return """
                <html><body style="font-family: Arial, sans-serif;">
                  <h2>Relatorio semanal de feedbacks</h2>
                  <p>Periodo: <strong>%s</strong> a <strong>%s</strong></p>
                  <p>Total de avaliacoes: <strong>%d</strong> &middot; Media das notas: <strong>%s</strong></p>
                  <h3>Avaliacoes por dia</h3>
                  %s
                  <h3>Avaliacoes por urgencia</h3>
                  %s
                  <h3>Avaliacoes recebidas</h3>
                  %s
                </body></html>
                """
                .formatted(
                        DIA.format(relatorio.inicio()),
                        DIA.format(relatorio.fim()),
                        relatorio.totalAvaliacoes(),
                        relatorio.mediaNotas(),
                        porDia(relatorio.avaliacoesPorDia()),
                        porUrgencia(relatorio.avaliacoesPorUrgencia()),
                        itens(relatorio));
    }

    private static String porDia(Map<LocalDate, Long> avaliacoesPorDia) {
        if (avaliacoesPorDia.isEmpty()) {
            return "<p>Nenhuma avaliacao no periodo.</p>";
        }
        return tabela(avaliacoesPorDia.entrySet().stream()
                .map(entrada -> linha(DIA.format(entrada.getKey()), String.valueOf(entrada.getValue())))
                .collect(Collectors.joining()));
    }

    private static String porUrgencia(Map<Urgencia, Long> avaliacoesPorUrgencia) {
        return tabela(avaliacoesPorUrgencia.entrySet().stream()
                .map(entrada -> linha(entrada.getKey().name(), String.valueOf(entrada.getValue())))
                .collect(Collectors.joining()));
    }

    private static String itens(RelatorioSemanal relatorio) {
        if (relatorio.vazio()) {
            return "<p>Nenhuma avaliacao no periodo.</p>";
        }
        String cabecalho = "<tr><th align=\"left\">Descricao</th><th align=\"left\">Urgencia</th>"
                + "<th align=\"left\">Data de envio</th></tr>";
        String linhas = relatorio.itens().stream()
                .map(EnviarRelatorioSemanal::linhaDaAvaliacao)
                .collect(Collectors.joining());
        return tabela(cabecalho + linhas);
    }

    private static String linhaDaAvaliacao(Avaliacao avaliacao) {
        return "<tr><td>%s</td><td>%s</td><td>%s</td></tr>"
                .formatted(
                        escapar(avaliacao.descricao()),
                        avaliacao.urgencia(),
                        DATA_HORA.format(avaliacao.dataEnvio()));
    }

    private static String tabela(String conteudo) {
        return "<table cellpadding=\"6\" border=\"1\" style=\"border-collapse: collapse;\">"
                + conteudo + "</table>";
    }

    private static String linha(String chave, String valor) {
        return "<tr><td>%s</td><td>%s</td></tr>".formatted(chave, valor);
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
