package br.com.fiap.feedback.application;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.FeedbackRepository;
import br.com.fiap.feedback.domain.Urgencia;
import br.com.fiap.feedback.domain.ValidacaoDeDominioException;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Caso de uso RF-05: lista avaliacoes por periodo e urgencia para apoiar a
 * analise dos administradores.
 */
@ApplicationScoped
public class ConsultarAvaliacoes {

    static final int DIAS_DO_PERIODO_PADRAO = 7;
    static final int MAXIMO_DE_DIAS = 366;

    private final FeedbackRepository repositorio;
    private final Clock relogio;

    public ConsultarAvaliacoes(FeedbackRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    /**
     * @param inicio primeiro dia do periodo; {@code null} usa os
     *     {@value #DIAS_DO_PERIODO_PADRAO} dias que terminam em {@code fim}
     * @param fim ultimo dia do periodo; {@code null} usa o dia corrente
     * @param urgencia filtro opcional; {@code null} devolve todas
     */
    public List<Avaliacao> executar(LocalDate inicio, LocalDate fim, Urgencia urgencia) {
        LocalDate fimEfetivo = fim != null ? fim : LocalDate.now(relogio);
        LocalDate inicioEfetivo =
                inicio != null ? inicio : fimEfetivo.minusDays(DIAS_DO_PERIODO_PADRAO - 1L);

        validarPeriodo(inicioEfetivo, fimEfetivo);

        List<Avaliacao> encontradas = repositorio.buscarPorPeriodo(inicioEfetivo, fimEfetivo);
        if (urgencia == null) {
            return encontradas;
        }
        return encontradas.stream().filter(avaliacao -> avaliacao.urgencia() == urgencia).toList();
    }

    private static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new ValidacaoDeDominioException(List.of("dataInicio nao pode ser posterior a dataFim"));
        }
        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        if (dias > MAXIMO_DE_DIAS) {
            throw new ValidacaoDeDominioException(
                    List.of("periodo nao pode exceder " + MAXIMO_DE_DIAS + " dias"));
        }
    }
}
