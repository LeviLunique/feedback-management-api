package br.com.fiap.feedback.adapter.in.rest.erro;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Corpo de erro padrao da API (SPEC 5.1).
 *
 * <p>Expoe apenas mensagens de negocio, sem detalhes internos como classes ou
 * stack traces.
 */
@Schema(name = "Erro", description = "Corpo padrao de erro")
public record RespostaDeErro(int status, String erro, List<String> mensagens) {

    public static RespostaDeErro requisicaoInvalida(List<String> mensagens) {
        return new RespostaDeErro(400, "Requisicao invalida", mensagens);
    }
}
