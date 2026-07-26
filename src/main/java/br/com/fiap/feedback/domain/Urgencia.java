package br.com.fiap.feedback.domain;

/**
 * Classificacao de urgencia derivada da nota da avaliacao.
 *
 * <p>A escala de notas e o mapeamento para urgencia formam uma unica regra de
 * negocio, por isso vivem juntos neste enum.
 */
public enum Urgencia {

    CRITICA,
    ALTA,
    MEDIA,
    BAIXA;

    public static final int NOTA_MINIMA = 0;
    public static final int NOTA_MAXIMA = 10;

    /**
     * Aplica a regra de classificacao: 0-2 critica, 3-5 alta, 6-7 media, 8-10 baixa.
     *
     * @throws IllegalArgumentException se a nota estiver fora da escala
     */
    public static Urgencia daNota(int nota) {
        if (!ehNotaValida(nota)) {
            throw new IllegalArgumentException(
                    "nota deve estar entre " + NOTA_MINIMA + " e " + NOTA_MAXIMA + ", recebida: " + nota);
        }
        if (nota <= 2) {
            return CRITICA;
        }
        if (nota <= 5) {
            return ALTA;
        }
        if (nota <= 7) {
            return MEDIA;
        }
        return BAIXA;
    }

    public static boolean ehNotaValida(int nota) {
        return nota >= NOTA_MINIMA && nota <= NOTA_MAXIMA;
    }

    /**
     * Indica se a avaliacao deve acionar a notificacao imediata aos
     * administradores (RF-03).
     */
    public boolean exigeNotificacaoImediata() {
        return this == CRITICA;
    }
}
