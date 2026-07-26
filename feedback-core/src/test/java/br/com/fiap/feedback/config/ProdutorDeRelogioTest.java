package br.com.fiap.feedback.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProdutorDeRelogioTest {

    @Test
    @DisplayName("produz relogio em UTC, como exige a data de envio")
    void produzRelogioEmUtc() {
        assertThat(new ProdutorDeRelogio().relogio().getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
