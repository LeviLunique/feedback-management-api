package br.com.fiap.feedback.report;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.application.EnviarRelatorioSemanal;
import br.com.fiap.feedback.application.GerarRelatorioSemanal;
import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.EnviadorDeEmail;
import br.com.fiap.feedback.domain.FeedbackRepository;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Funcao weekly-report-fn: recorte e disparo do relatorio agendado.
 *
 * <p>2026-07-27 e uma segunda-feira, o dia em que o agendamento roda; a semana
 * reportada deve ser a anterior, de 2026-07-20 a 2026-07-26.
 */
class RelatorioSemanalHandlerTest {

    private static final Instant SEGUNDA = Instant.parse("2026-07-27T08:00:00Z");

    private RepositorioFake repositorio;
    private EnviadorFake enviador;
    private RelatorioSemanalHandler handler;

    /** Repositorio minimo em memoria, para nao depender do DynamoDB. */
    private static final class RepositorioFake implements FeedbackRepository {
        private final List<Avaliacao> avaliacoes = new ArrayList<>();

        @Override
        public void salvar(Avaliacao avaliacao) {
            avaliacoes.add(avaliacao);
        }

        @Override
        public List<Avaliacao> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
            return avaliacoes.stream()
                    .filter(avaliacao -> {
                        LocalDate dia = LocalDate.ofInstant(avaliacao.dataEnvio(), ZoneOffset.UTC);
                        return !dia.isBefore(inicio) && !dia.isAfter(fim);
                    })
                    .sorted(Comparator.comparing(Avaliacao::dataEnvio))
                    .toList();
        }
    }

    private static final class EnviadorFake implements EnviadorDeEmail {
        private final List<String> corpos = new ArrayList<>();

        @Override
        public void enviarParaAdministradores(String assunto, String corpoHtml) {
            corpos.add(corpoHtml);
        }
    }

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioFake();
        enviador = new EnviadorFake();
        Clock relogio = Clock.fixed(SEGUNDA, ZoneOffset.UTC);
        handler = new RelatorioSemanalHandler(
                new EnviarRelatorioSemanal(new GerarRelatorioSemanal(repositorio, relogio), enviador),
                relogio);
    }

    private void registrar(String descricao, int nota, String dia) {
        repositorio.salvar(new Avaliacao(
                UUID.randomUUID(), descricao, nota, Instant.parse(dia + "T10:00:00Z")));
    }

    @Test
    @DisplayName("reporta a semana anterior, e nao a que esta comecando")
    void reportaSemanaAnterior() {
        String resultado = handler.handleRequest(new ScheduledEvent(), null);

        assertThat(resultado).contains("2026-07-20").contains("2026-07-26");
    }

    @Test
    @DisplayName("envia um e-mail com as avaliacoes da semana encerrada")
    void enviaEmailComAvaliacoesDaSemanaEncerrada() {
        registrar("Avaliacao da semana encerrada", 3, "2026-07-22");

        handler.handleRequest(new ScheduledEvent(), null);

        assertThat(enviador.corpos).hasSize(1);
        assertThat(enviador.corpos.get(0)).contains("Avaliacao da semana encerrada");
    }

    @Test
    @DisplayName("ignora avaliacoes fora da semana reportada")
    void ignoraAvaliacoesForaDaSemana() {
        registrar("Da semana que comeca", 5, "2026-07-27");
        registrar("Da semana encerrada", 5, "2026-07-24");

        handler.handleRequest(new ScheduledEvent(), null);

        String corpo = enviador.corpos.get(0);
        assertThat(corpo).contains("Da semana encerrada");
        assertThat(corpo).doesNotContain("Da semana que comeca");
    }

    @Test
    @DisplayName("semana sem avaliacoes ainda envia relatorio")
    void semanaSemAvaliacoesAindaEnviaRelatorio() {
        String resultado = handler.handleRequest(new ScheduledEvent(), null);

        assertThat(enviador.corpos).hasSize(1);
        assertThat(resultado).contains("0 avaliacoes");
    }

    @Test
    @DisplayName("informa a quantidade de avaliacoes no retorno")
    void informaQuantidadeNoRetorno() {
        registrar("Primeira", 4, "2026-07-21");
        registrar("Segunda", 6, "2026-07-23");

        assertThat(handler.handleRequest(new ScheduledEvent(), null)).contains("2 avaliacoes");
    }
}
