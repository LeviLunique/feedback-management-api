package br.com.fiap.feedback.adapter.in.rest.erro;

import br.com.fiap.feedback.domain.ValidacaoDeDominioException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduz violacoes de invariantes do dominio em HTTP 400 com o corpo de erro
 * padrao (SPEC 5.1).
 */
@Provider
public class ValidacaoDeDominioExceptionMapper implements ExceptionMapper<ValidacaoDeDominioException> {

    @Override
    public Response toResponse(ValidacaoDeDominioException excecao) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(RespostaDeErro.requisicaoInvalida(excecao.mensagens()))
                .build();
    }
}
