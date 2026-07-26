package br.com.fiap.feedback.application;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.FeedbackRepository;
import br.com.fiap.feedback.domain.PublicadorDeFeedbackCritico;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Instant;

/**
 * Caso de uso RF-01: recebe uma avaliacao de estudante, valida as regras de
 * dominio e a persiste. Quando a avaliacao e critica, anuncia o fato para que a
 * notificacao aos administradores seja disparada (RF-03).
 *
 * <p>A decisao de notificar pertence ao caso de uso, e nao ao controller: e
 * regra de negocio e precisa valer para qualquer porta de entrada.
 *
 * <p>O relogio e injetado para que a data de envio seja deterministica nos
 * testes.
 */
@ApplicationScoped
public class ReceberAvaliacao {

    private final FeedbackRepository repositorio;
    private final PublicadorDeFeedbackCritico publicador;
    private final Clock relogio;

    public ReceberAvaliacao(
            FeedbackRepository repositorio,
            PublicadorDeFeedbackCritico publicador,
            Clock relogio) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    public Avaliacao executar(String descricao, Integer nota) {
        Avaliacao avaliacao = Avaliacao.nova(descricao, nota, Instant.now(relogio));
        repositorio.salvar(avaliacao);
        if (avaliacao.exigeNotificacaoImediata()) {
            publicador.publicar(avaliacao);
        }
        return avaliacao;
    }
}
