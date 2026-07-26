package br.com.fiap.feedback.evento;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.Urgencia;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;
import java.util.UUID;

/**
 * Contrato da mensagem trafegada no topico SNS entre a funcao de ingestao e a
 * funcao de notificacao.
 *
 * <p>Vive no modulo compartilhado justamente por ser um contrato entre duas
 * funcoes: alterar um lado sem o outro quebraria a integracao.
 *
 * <p><strong>{@code @RegisterForReflection} e obrigatorio aqui.</strong> O Quarkus
 * registra automaticamente para reflexao apenas os tipos alcancaveis pela camada
 * REST; este evento e serializado direto pelo {@code ObjectMapper}, entao no
 * binario nativo o Jackson nao encontraria seus componentes e falharia com
 * "No serializer found". Em JVM funciona sem a anotacao, o que torna a falha
 * invisivel fora do build nativo.
 */
@RegisterForReflection
public record EventoDeFeedbackCritico(
        UUID id, String descricao, int nota, Urgencia urgencia, Instant dataEnvio) {

    public static EventoDeFeedbackCritico de(Avaliacao avaliacao) {
        return new EventoDeFeedbackCritico(
                avaliacao.id(),
                avaliacao.descricao(),
                avaliacao.nota(),
                avaliacao.urgencia(),
                avaliacao.dataEnvio());
    }

    public Avaliacao paraAvaliacao() {
        return new Avaliacao(id, descricao, nota, dataEnvio);
    }
}
