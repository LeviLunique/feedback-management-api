package br.com.fiap.feedback.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;

/**
 * Disponibiliza o relogio da aplicacao para injecao.
 *
 * <p>Todas as datas sao geradas em UTC (SPEC 4.1) e, por vir do container, o
 * relogio pode ser substituido por um fixo nos testes.
 */
@ApplicationScoped
public class ProdutorDeRelogio {

    @Produces
    @ApplicationScoped
    public Clock relogio() {
        return Clock.systemUTC();
    }
}
