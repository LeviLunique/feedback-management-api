package br.com.fiap.feedback.adapter.in.rest.dto;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.Urgencia;
import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Representacao de uma avaliacao devolvida pela API (SPEC 5.1).
 */
@Schema(name = "Avaliacao", description = "Avaliacao registrada")
public record AvaliacaoResponse(
        UUID id,
        String descricao,
        int nota,
        @Schema(description = "Derivada da nota pelo servidor") Urgencia urgencia,
        @Schema(description = "Instante do recebimento, em UTC") Instant dataEnvio) {

    public static AvaliacaoResponse de(Avaliacao avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.id(),
                avaliacao.descricao(),
                avaliacao.nota(),
                avaliacao.urgencia(),
                avaliacao.dataEnvio());
    }
}
