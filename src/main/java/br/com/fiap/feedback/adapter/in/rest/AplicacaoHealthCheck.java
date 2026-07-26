package br.com.fiap.feedback.adapter.in.rest;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * Expoe em {@code /q/health} o nome e a versao da aplicacao em execucao.
 *
 * <p>Permite confirmar rapidamente qual build esta ativa em cada funcao Lambda,
 * apoiando o requisito de monitoramento (RF-06).
 */
@Liveness
@ApplicationScoped
public class AplicacaoHealthCheck implements HealthCheck {

    static final String NOME_DO_CHECK = "aplicacao";

    private final String nomeDaAplicacao;
    private final String versaoDaAplicacao;

    public AplicacaoHealthCheck(
            @ConfigProperty(name = "quarkus.application.name") String nomeDaAplicacao,
            @ConfigProperty(name = "quarkus.application.version") String versaoDaAplicacao) {
        this.nomeDaAplicacao = nomeDaAplicacao;
        this.versaoDaAplicacao = versaoDaAplicacao;
    }

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named(NOME_DO_CHECK)
                .up()
                .withData("nome", nomeDaAplicacao)
                .withData("versao", versaoDaAplicacao)
                .build();
    }
}
