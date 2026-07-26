package br.com.fiap.feedback.adapter.in.rest;

import br.com.fiap.feedback.adapter.in.rest.dto.AvaliacaoResponse;
import br.com.fiap.feedback.adapter.in.rest.dto.NovaAvaliacaoRequest;
import br.com.fiap.feedback.application.ReceberAvaliacao;
import br.com.fiap.feedback.domain.Avaliacao;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Endpoint de entrada de avaliacoes (SPEC 5.1).
 */
@Path("/api/v1/avaliacao")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Avaliacoes", description = "Recebimento de feedbacks das aulas")
public class AvaliacaoResource {

    private final ReceberAvaliacao receberAvaliacao;

    public AvaliacaoResource(ReceberAvaliacao receberAvaliacao) {
        this.receberAvaliacao = receberAvaliacao;
    }

    @POST
    @Operation(
            summary = "Registra uma avaliacao",
            description = "Persiste o feedback do estudante e classifica a urgencia a partir da nota.")
    @APIResponse(responseCode = "201", description = "Avaliacao registrada")
    @APIResponse(responseCode = "400", description = "Payload invalido")
    public Response registrar(NovaAvaliacaoRequest requisicao) {
        // Corpo ausente equivale a todos os campos ausentes: vira erro de
        // validacao do dominio (400) em vez de NullPointerException (500).
        NovaAvaliacaoRequest recebida =
                requisicao == null ? new NovaAvaliacaoRequest(null, null) : requisicao;

        Avaliacao avaliacao = receberAvaliacao.executar(recebida.descricao(), recebida.nota());
        return Response.status(Response.Status.CREATED)
                .entity(AvaliacaoResponse.de(avaliacao))
                .build();
    }
}
