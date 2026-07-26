package br.com.fiap.feedback.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Avaliacao enviada por um estudante sobre uma aula (RF-01).
 *
 * <p>A urgencia nao e um campo armazenado: ela e sempre derivada da nota
 * (SPEC 4.3), o que torna impossivel um cliente forja-la ou o registro ficar
 * inconsistente com a propria nota.
 *
 * @param id identificador gerado pelo servidor
 * @param descricao texto do feedback, sem espacos nas bordas
 * @param nota inteiro de 0 a 10
 * @param dataEnvio instante do recebimento, em UTC
 */
public record Avaliacao(UUID id, String descricao, int nota, Instant dataEnvio) {

    public static final int TAMANHO_MAXIMO_DA_DESCRICAO = 2000;

    private static final String DESCRICAO_OBRIGATORIA = "descricao e obrigatoria";
    private static final String DESCRICAO_MUITO_LONGA =
            "descricao deve ter no maximo " + TAMANHO_MAXIMO_DA_DESCRICAO + " caracteres";
    private static final String NOTA_OBRIGATORIA = "nota e obrigatoria";
    private static final String NOTA_FORA_DA_ESCALA =
            "nota deve estar entre " + Urgencia.NOTA_MINIMA + " e " + Urgencia.NOTA_MAXIMA;
    private static final String DATA_ENVIO_OBRIGATORIA = "dataEnvio e obrigatoria";
    private static final String ID_OBRIGATORIO = "id e obrigatorio";

    public Avaliacao {
        List<String> erros = new ArrayList<>();
        if (id == null) {
            erros.add(ID_OBRIGATORIO);
        }
        erros.addAll(validarDescricao(descricao));
        if (!Urgencia.ehNotaValida(nota)) {
            erros.add(NOTA_FORA_DA_ESCALA);
        }
        if (dataEnvio == null) {
            erros.add(DATA_ENVIO_OBRIGATORIA);
        }
        lancarSeHouverErros(erros);

        descricao = descricao.strip();
    }

    /**
     * Cria uma nova avaliacao, gerando o identificador e validando os dados
     * informados pelo cliente.
     *
     * @param nota aceita {@code null} para reportar a ausencia como erro de
     *     validacao, em vez de estourar {@code NullPointerException}
     */
    public static Avaliacao nova(String descricao, Integer nota, Instant dataEnvio) {
        if (nota == null) {
            // Neste ponto ja existe ao menos o erro da nota ausente, entao o
            // lancamento e incondicional.
            List<String> erros = new ArrayList<>(validarDescricao(descricao));
            erros.add(NOTA_OBRIGATORIA);
            if (dataEnvio == null) {
                erros.add(DATA_ENVIO_OBRIGATORIA);
            }
            throw new ValidacaoDeDominioException(erros);
        }
        return new Avaliacao(UUID.randomUUID(), descricao, nota, dataEnvio);
    }

    public Urgencia urgencia() {
        return Urgencia.daNota(nota);
    }

    public boolean exigeNotificacaoImediata() {
        return urgencia().exigeNotificacaoImediata();
    }

    private static List<String> validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return List.of(DESCRICAO_OBRIGATORIA);
        }
        if (descricao.strip().length() > TAMANHO_MAXIMO_DA_DESCRICAO) {
            return List.of(DESCRICAO_MUITO_LONGA);
        }
        return List.of();
    }

    private static void lancarSeHouverErros(List<String> erros) {
        if (!erros.isEmpty()) {
            throw new ValidacaoDeDominioException(erros);
        }
    }
}
