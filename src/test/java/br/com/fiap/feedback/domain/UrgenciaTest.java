package br.com.fiap.feedback.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Regra de urgencia derivada da nota (SPEC 4.3).
 */
class UrgenciaTest {

    @ParameterizedTest(name = "nota {0} resulta em urgencia {1}")
    @CsvSource({
            "0, CRITICA",
            "1, CRITICA",
            "2, CRITICA",
            "3, ALTA",
            "4, ALTA",
            "5, ALTA",
            "6, MEDIA",
            "7, MEDIA",
            "8, BAIXA",
            "9, BAIXA",
            "10, BAIXA"
    })
    void derivaUrgenciaDaNota(int nota, Urgencia esperada) {
        assertThat(Urgencia.daNota(nota)).isEqualTo(esperada);
    }

    @ParameterizedTest(name = "nota {0} fora da escala e rejeitada")
    @ValueSource(ints = {-1, 11, 100})
    void rejeitaNotaForaDaEscala(int nota) {
        assertThatThrownBy(() -> Urgencia.daNota(nota))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("apenas a urgencia CRITICA aciona notificacao")
    void apenasCriticaAcionaNotificacao() {
        assertThat(Urgencia.CRITICA.exigeNotificacaoImediata()).isTrue();
        assertThat(Urgencia.ALTA.exigeNotificacaoImediata()).isFalse();
        assertThat(Urgencia.MEDIA.exigeNotificacaoImediata()).isFalse();
        assertThat(Urgencia.BAIXA.exigeNotificacaoImediata()).isFalse();
    }
}
