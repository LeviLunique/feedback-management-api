package br.com.fiap.feedback.adapter.in.rest;

import br.com.fiap.feedback.adapter.in.rest.dto.ListaDeAvaliacoesResponse;
import br.com.fiap.feedback.application.ConsultarAvaliacoes;
import br.com.fiap.feedback.domain.Urgencia;
import br.com.fiap.feedback.domain.ValidacaoDeDominioException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Consulta de avaliacoes para analise dos administradores (SPEC 5.2).
 *
 * <p>Os parametros chegam como texto e sao convertidos aqui para que entradas
 * malformadas produzam o mesmo corpo de erro padrao do restante da API, em vez
 * da resposta generica do framework.
 */
@Path("/api/v1/avaliacoes")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Avaliacoes", description = "Recebimento de feedbacks das aulas")
public class ConsultaDeAvaliacoesResource {

    private final ConsultarAvaliacoes consultarAvaliacoes;

    public ConsultaDeAvaliacoesResource(ConsultarAvaliacoes consultarAvaliacoes) {
        this.consultarAvaliacoes = consultarAvaliacoes;
    }

    @GET
    @Operation(
            summary = "Lista avaliacoes por periodo",
            description = "Sem filtros, devolve os ultimos 7 dias. Datas no formato yyyy-MM-dd.")
    @APIResponse(responseCode = "200", description = "Avaliacoes encontradas")
    @APIResponse(responseCode = "400", description = "Parametros invalidos")
    public ListaDeAvaliacoesResponse listar(
            @QueryParam("dataInicio") String dataInicio,
            @QueryParam("dataFim") String dataFim,
            @QueryParam("urgencia") String urgencia) {

        return ListaDeAvaliacoesResponse.de(consultarAvaliacoes.executar(
                paraData(dataInicio, "dataInicio"),
                paraData(dataFim, "dataFim"),
                paraUrgencia(urgencia)));
    }

    private static LocalDate paraData(String valor, String parametro) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.strip());
        } catch (DateTimeParseException formatoInvalido) {
            throw new ValidacaoDeDominioException(
                    List.of(parametro + " deve estar no formato yyyy-MM-dd"));
        }
    }

    private static Urgencia paraUrgencia(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Urgencia.valueOf(valor.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException desconhecida) {
            throw new ValidacaoDeDominioException(
                    List.of("urgencia deve ser um de: " + List.of(Urgencia.values())));
        }
    }
}
