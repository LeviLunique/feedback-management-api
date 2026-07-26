package br.com.fiap.feedback.domain;

import java.util.List;

/**
 * Sinaliza que uma ou mais invariantes do dominio foram violadas.
 *
 * <p>Carrega todas as mensagens de uma vez para que o cliente da API corrija
 * todos os problemas em uma unica tentativa.
 */
public class ValidacaoDeDominioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> mensagens;

    public ValidacaoDeDominioException(List<String> mensagens) {
        super(String.join("; ", mensagens));
        this.mensagens = List.copyOf(mensagens);
    }

    public List<String> mensagens() {
        return mensagens;
    }
}
