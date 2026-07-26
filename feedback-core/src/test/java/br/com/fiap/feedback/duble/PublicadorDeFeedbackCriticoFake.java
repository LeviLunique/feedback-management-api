package br.com.fiap.feedback.duble;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.PublicadorDeFeedbackCritico;
import java.util.ArrayList;
import java.util.List;

/**
 * Registra o que seria publicado no topico, permitindo verificar <em>quais</em>
 * avaliacoes disparam notificacao sem depender do SNS.
 */
public class PublicadorDeFeedbackCriticoFake implements PublicadorDeFeedbackCritico {

    private final List<Avaliacao> publicadas = new ArrayList<>();

    @Override
    public void publicar(Avaliacao avaliacao) {
        publicadas.add(avaliacao);
    }

    public List<Avaliacao> publicadas() {
        return List.copyOf(publicadas);
    }
}
