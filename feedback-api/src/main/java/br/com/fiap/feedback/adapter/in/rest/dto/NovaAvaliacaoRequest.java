package br.com.fiap.feedback.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Payload de entrada do POST /api/v1/avaliacao (SPEC 5.1).
 *
 * <p>Campos desconhecidos sao ignorados: a urgencia e sempre derivada da nota
 * pelo servidor e nunca aceita do cliente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "NovaAvaliacao", description = "Avaliacao enviada por um estudante")
public record NovaAvaliacaoRequest(
        @Schema(description = "Texto do feedback", examples = "Aula muito bem explicada", required = true)
        String descricao,
        @Schema(description = "Nota de 0 a 10", examples = "9", minimum = "0", maximum = "10", required = true)
        Integer nota) {
}
