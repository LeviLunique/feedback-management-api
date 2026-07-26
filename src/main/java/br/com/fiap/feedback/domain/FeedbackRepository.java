package br.com.fiap.feedback.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Porta de saida para persistencia de avaliacoes.
 *
 * <p>O dominio declara o que precisa; a tecnologia concreta (DynamoDB) fica em
 * {@code adapter.out}, mantendo a inversao de dependencia.
 */
public interface FeedbackRepository {

    void salvar(Avaliacao avaliacao);

    /**
     * Avaliacoes recebidas entre {@code inicio} e {@code fim}, ambos inclusive,
     * considerando o dia em UTC do envio, ordenadas da mais antiga para a mais
     * recente.
     */
    List<Avaliacao> buscarPorPeriodo(LocalDate inicio, LocalDate fim);
}
