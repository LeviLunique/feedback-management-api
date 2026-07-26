package br.com.fiap.feedback.application;

import br.com.fiap.feedback.domain.FeedbackRepository;
import br.com.fiap.feedback.domain.RelatorioSemanal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Caso de uso RF-04: consolida as avaliacoes de uma semana.
 *
 * <p>A semana segue o padrao ISO, de segunda a domingo, para que o recorte seja
 * o mesmo no endpoint de consulta e no envio agendado.
 */
@ApplicationScoped
public class GerarRelatorioSemanal {

    private final FeedbackRepository repositorio;
    private final Clock relogio;

    public GerarRelatorioSemanal(FeedbackRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    /**
     * @param referencia qualquer dia da semana desejada; {@code null} usa o dia corrente
     */
    public RelatorioSemanal executar(LocalDate referencia) {
        LocalDate dia = referencia != null ? referencia : LocalDate.now(relogio);
        LocalDate inicio = dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fim = inicio.plusDays(6);

        return RelatorioSemanal.de(inicio, fim, repositorio.buscarPorPeriodo(inicio, fim));
    }
}
