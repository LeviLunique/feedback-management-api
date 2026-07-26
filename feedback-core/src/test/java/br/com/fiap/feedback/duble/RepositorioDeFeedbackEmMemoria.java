package br.com.fiap.feedback.duble;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.FeedbackRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Duble em memoria da porta de persistencia, usado nos testes de unidade dos
 * casos de uso.
 *
 * <p>E uma implementacao real da porta (nao um mock), o que mantem os testes
 * deterministicos sem exigir container nem stubbing de chamadas.
 */
public class RepositorioDeFeedbackEmMemoria implements FeedbackRepository {

    private final List<Avaliacao> avaliacoes = new ArrayList<>();

    @Override
    public void salvar(Avaliacao avaliacao) {
        avaliacoes.add(avaliacao);
    }

    @Override
    public List<Avaliacao> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return avaliacoes.stream()
                .filter(avaliacao -> {
                    LocalDate dia = LocalDate.ofInstant(avaliacao.dataEnvio(), ZoneOffset.UTC);
                    return !dia.isBefore(inicio) && !dia.isAfter(fim);
                })
                .sorted(Comparator.comparing(Avaliacao::dataEnvio))
                .toList();
    }

    /**
     * Avaliacoes armazenadas, na ordem de chegada.
     */
    public List<Avaliacao> todas() {
        return List.copyOf(avaliacoes);
    }
}
