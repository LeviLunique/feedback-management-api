package br.com.fiap.feedback.adapter.out.memoria;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.FeedbackRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementacao em memoria da porta de persistencia.
 *
 * <p>Permite exercitar a API de ponta a ponta antes da entrega do adaptador
 * DynamoDB, que a substituira. Nao sobrevive ao ciclo de vida do processo.
 */
@ApplicationScoped
public class RepositorioDeFeedbackEmMemoria implements FeedbackRepository {

    private final List<Avaliacao> avaliacoes = new CopyOnWriteArrayList<>();

    @Override
    public void salvar(Avaliacao avaliacao) {
        avaliacoes.add(avaliacao);
    }

    /**
     * Avaliacoes armazenadas, na ordem de chegada.
     */
    public List<Avaliacao> todas() {
        return List.copyOf(avaliacoes);
    }
}
