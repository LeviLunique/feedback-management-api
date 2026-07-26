package br.com.fiap.feedback.application;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.FeedbackRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Instant;

/**
 * Caso de uso RF-01: recebe uma avaliacao de estudante, valida as regras de
 * dominio e a persiste.
 *
 * <p>O relogio e injetado para que a data de envio seja deterministica nos
 * testes.
 */
@ApplicationScoped
public class ReceberAvaliacao {

    private final FeedbackRepository repositorio;
    private final Clock relogio;

    public ReceberAvaliacao(FeedbackRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public Avaliacao executar(String descricao, Integer nota) {
        Avaliacao avaliacao = Avaliacao.nova(descricao, nota, Instant.now(relogio));
        repositorio.salvar(avaliacao);
        return avaliacao;
    }
}
