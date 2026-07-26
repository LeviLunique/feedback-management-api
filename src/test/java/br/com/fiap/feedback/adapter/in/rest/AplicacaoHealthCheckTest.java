package br.com.fiap.feedback.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AplicacaoHealthCheckTest {

    private final AplicacaoHealthCheck healthCheck =
            new AplicacaoHealthCheck("feedback-management-api", "1.0.0-SNAPSHOT");

    @Test
    @DisplayName("reporta status UP")
    void reportaStatusUp() {
        HealthCheckResponse resposta = healthCheck.call();

        assertThat(resposta.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
        assertThat(resposta.getName()).isEqualTo(AplicacaoHealthCheck.NOME_DO_CHECK);
    }

    @Test
    @DisplayName("expoe nome e versao da aplicacao nos dados do check")
    void expoeNomeEVersaoDaAplicacao() {
        HealthCheckResponse resposta = healthCheck.call();

        assertThat(resposta.getData())
                .isPresent()
                .get()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .contains(entry("nome", "feedback-management-api"), entry("versao", "1.0.0-SNAPSHOT"));
    }
}
