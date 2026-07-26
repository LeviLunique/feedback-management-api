package br.com.fiap.feedback.domain;

/**
 * Porta de saida para anunciar que uma avaliacao critica foi recebida (RF-03).
 *
 * <p>O dominio apenas anuncia o fato; quem reage a ele (e como) e decisao da
 * infraestrutura, hoje um topico SNS consumido por uma funcao dedicada.
 */
public interface PublicadorDeFeedbackCritico {

    void publicar(Avaliacao avaliacao);
}
