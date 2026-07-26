package br.com.fiap.feedback.adapter.out.dynamodb;

/**
 * Nomes de tabela, indice e atributos do DynamoDB (SPEC 4.2).
 *
 * <p>Centralizados para que o adaptador e a criacao da tabela local nao se
 * desalinhem.
 */
final class TabelaDeFeedback {

    static final String INDICE_POR_DIA = "gsi-data";

    static final String ID = "id";
    static final String DESCRICAO = "descricao";
    static final String NOTA = "nota";
    static final String DATA_ENVIO = "dataEnvio";
    static final String DATA_ENVIO_DIA = "dataEnvioDia";
    static final String URGENCIA = "urgencia";

    private TabelaDeFeedback() {
    }
}
