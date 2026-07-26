package br.com.fiap.feedback.adapter.in.rest;

import br.com.fiap.feedback.adapter.in.rest.dto.RelatorioSemanalResponse;
import br.com.fiap.feedback.application.GerarRelatorioSemanal;
import br.com.fiap.feedback.domain.ValidacaoDeDominioException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Relatorio semanal para analise dos administradores (SPEC 5.3).
 */
@Path("/api/v1/relatorios")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Relatorios", description = "Consolidados periodicos dos feedbacks")
public class RelatorioResource {

    private final GerarRelatorioSemanal gerarRelatorioSemanal;

    public RelatorioResource(GerarRelatorioSemanal gerarRelatorioSemanal) {
        this.gerarRelatorioSemanal = gerarRelatorioSemanal;
    }

    @GET
    @Path("/semanal")
    @Operation(
            summary = "Consolidado da semana",
            description = "Media das notas, totais por dia e por urgencia. Sem referencia, usa a "
                    + "semana corrente (segunda a domingo).")
    @APIResponse(responseCode = "200", description = "Relatorio gerado")
    @APIResponse(responseCode = "400", description = "Parametro invalido")
    public RelatorioSemanalResponse semanal(@QueryParam("referencia") String referencia) {
        return RelatorioSemanalResponse.de(gerarRelatorioSemanal.executar(paraData(referencia)));
    }

    private static LocalDate paraData(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.strip());
        } catch (DateTimeParseException formatoInvalido) {
            throw new ValidacaoDeDominioException(
                    List.of("referencia deve estar no formato yyyy-MM-dd"));
        }
    }
}
