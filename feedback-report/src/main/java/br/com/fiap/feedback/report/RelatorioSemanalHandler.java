package br.com.fiap.feedback.report;

import br.com.fiap.feedback.application.EnviarRelatorioSemanal;
import br.com.fiap.feedback.domain.RelatorioSemanal;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import jakarta.inject.Named;
import java.time.Clock;
import java.time.LocalDate;
import org.jboss.logging.Logger;

/**
 * Funcao {@code weekly-report-fn}: acionada pelo EventBridge Scheduler, envia o
 * relatorio semanal aos administradores (RF-04).
 *
 * <p>O agendamento roda na segunda-feira, entao o recorte e a semana que acabou
 * de terminar - e nao a que esta comecando, que estaria praticamente vazia.
 */
@Named("weeklyReport")
public class RelatorioSemanalHandler implements RequestHandler<ScheduledEvent, String> {

    private static final Logger LOG = Logger.getLogger(RelatorioSemanalHandler.class);

    private final EnviarRelatorioSemanal enviarRelatorioSemanal;
    private final Clock relogio;

    public RelatorioSemanalHandler(EnviarRelatorioSemanal enviarRelatorioSemanal, Clock relogio) {
        this.enviarRelatorioSemanal = enviarRelatorioSemanal;
        this.relogio = relogio;
    }

    @Override
    public String handleRequest(ScheduledEvent evento, Context contexto) {
        LocalDate referencia = LocalDate.now(relogio).minusWeeks(1);
        RelatorioSemanal relatorio = enviarRelatorioSemanal.executar(referencia);

        LOG.infof("Relatorio semanal de %s a %s enviado com %d avaliacoes.",
                relatorio.inicio(), relatorio.fim(), relatorio.totalAvaliacoes());
        return "Relatorio de %s a %s enviado com %d avaliacoes"
                .formatted(relatorio.inicio(), relatorio.fim(), relatorio.totalAvaliacoes());
    }
}
