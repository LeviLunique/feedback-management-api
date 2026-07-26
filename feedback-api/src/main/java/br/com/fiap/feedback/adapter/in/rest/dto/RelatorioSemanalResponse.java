package br.com.fiap.feedback.adapter.in.rest.dto;

import br.com.fiap.feedback.domain.RelatorioSemanal;
import br.com.fiap.feedback.domain.Urgencia;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Consolidado semanal devolvido pela API (SPEC 5.3).
 *
 * <p>As chaves do mapa por dia sao convertidas para texto {@code yyyy-MM-dd},
 * garantindo um JSON estavel independentemente da serializacao de datas.
 */
@Schema(name = "RelatorioSemanal", description = "Consolidado das avaliacoes da semana")
public record RelatorioSemanalResponse(
        LocalDate inicio,
        LocalDate fim,
        double mediaNotas,
        int totalAvaliacoes,
        Map<String, Long> avaliacoesPorDia,
        Map<Urgencia, Long> avaliacoesPorUrgencia,
        List<AvaliacaoResponse> itens) {

    public static RelatorioSemanalResponse de(RelatorioSemanal relatorio) {
        Map<String, Long> porDia = new LinkedHashMap<>();
        relatorio.avaliacoesPorDia().forEach((dia, quantidade) -> porDia.put(dia.toString(), quantidade));

        return new RelatorioSemanalResponse(
                relatorio.inicio(),
                relatorio.fim(),
                relatorio.mediaNotas(),
                relatorio.totalAvaliacoes(),
                porDia,
                relatorio.avaliacoesPorUrgencia(),
                relatorio.itens().stream().map(AvaliacaoResponse::de).toList());
    }
}
