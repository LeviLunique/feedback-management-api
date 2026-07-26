package br.com.fiap.feedback.domain;

/**
 * Porta de saida para persistencia de avaliacoes.
 *
 * <p>O dominio declara o que precisa; a tecnologia concreta (DynamoDB) fica em
 * {@code adapter.out}, mantendo a inversao de dependencia.
 */
public interface FeedbackRepository {

    void salvar(Avaliacao avaliacao);
}
