package br.com.fiap.feedback.domain;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Consolidado das avaliacoes de um periodo (RF-04 / SPEC 5.3).
 *
 * <p>A agregacao e uma funcao pura sobre a lista de avaliacoes, o que a torna
 * testavel sem infraestrutura e reutilizavel pelo endpoint e pela funcao
 * agendada.
 *
 * @param avaliacoesPorDia contem apenas os dias que tiveram avaliacoes
 * @param avaliacoesPorUrgencia contem sempre as quatro faixas, inclusive zeradas,
 *     para que o administrador leia a distribuicao completa de um relance
 */
public record RelatorioSemanal(
        LocalDate inicio,
        LocalDate fim,
        double mediaNotas,
        int totalAvaliacoes,
        Map<LocalDate, Long> avaliacoesPorDia,
        Map<Urgencia, Long> avaliacoesPorUrgencia,
        List<Avaliacao> itens) {

    public static RelatorioSemanal de(LocalDate inicio, LocalDate fim, List<Avaliacao> avaliacoes) {
        return new RelatorioSemanal(
                inicio,
                fim,
                mediaDasNotas(avaliacoes),
                avaliacoes.size(),
                contarPorDia(avaliacoes),
                contarPorUrgencia(avaliacoes),
                List.copyOf(avaliacoes));
    }

    public boolean vazio() {
        return totalAvaliacoes == 0;
    }

    private static double mediaDasNotas(List<Avaliacao> avaliacoes) {
        double media = avaliacoes.stream().mapToInt(Avaliacao::nota).average().orElse(0.0);
        return Math.round(media * 100.0) / 100.0;
    }

    private static Map<LocalDate, Long> contarPorDia(List<Avaliacao> avaliacoes) {
        return avaliacoes.stream().collect(Collectors.groupingBy(
                avaliacao -> LocalDate.ofInstant(avaliacao.dataEnvio(), ZoneOffset.UTC),
                TreeMap::new,
                Collectors.counting()));
    }

    private static Map<Urgencia, Long> contarPorUrgencia(List<Avaliacao> avaliacoes) {
        Map<Urgencia, Long> porUrgencia = new EnumMap<>(Urgencia.class);
        for (Urgencia urgencia : Urgencia.values()) {
            porUrgencia.put(urgencia, 0L);
        }
        porUrgencia.putAll(avaliacoes.stream().collect(
                Collectors.groupingBy(Avaliacao::urgencia, Collectors.counting())));
        return porUrgencia;
    }
}
