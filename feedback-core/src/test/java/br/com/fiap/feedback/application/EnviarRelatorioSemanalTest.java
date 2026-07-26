package br.com.fiap.feedback.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.duble.EnviadorDeEmailFake;
import br.com.fiap.feedback.duble.RepositorioDeFeedbackEmMemoria;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Caso de uso RF-04: conteudo do e-mail do relatorio semanal (SPEC 6).
 */
class EnviarRelatorioSemanalTest {

    private static final Instant DOMINGO = Instant.parse("2026-07-26T12:00:00Z");

    private RepositorioDeFeedbackEmMemoria repositorio;
    private EnviadorDeEmailFake enviador;
    private EnviarRelatorioSemanal enviarRelatorioSemanal;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioDeFeedbackEmMemoria();
        enviador = new EnviadorDeEmailFake();
        Clock relogio = Clock.fixed(DOMINGO, ZoneOffset.UTC);
        enviarRelatorioSemanal = new EnviarRelatorioSemanal(
                new GerarRelatorioSemanal(repositorio, relogio), enviador);
    }

    private void registrar(String descricao, int nota, String dia) {
        repositorio.salvar(new Avaliacao(
                UUID.randomUUID(), descricao, nota, Instant.parse(dia + "T10:00:00Z")));
    }

    @Test
    @DisplayName("envia um e-mail com o assunto do relatorio e o periodo")
    void enviaEmailComAssuntoEPeriodo() {
        registrar("Aula boa", 8, "2026-07-22");

        enviarRelatorioSemanal.executar(null);

        assertThat(enviador.unico().assunto())
                .contains("Relatorio semanal")
                .contains("20/07/2026")
                .contains("26/07/2026");
    }

    @Test
    @DisplayName("corpo traz media, total e contagens por dia e por urgencia")
    void corpoTrazAgregados() {
        registrar("Aula boa", 8, "2026-07-22");
        registrar("Aula pessima", 0, "2026-07-22");

        enviarRelatorioSemanal.executar(null);

        String corpo = enviador.unico().corpoHtml();
        assertThat(corpo).contains("4.0");
        assertThat(corpo).contains("22/07/2026");
        assertThat(corpo).contains("CRITICA");
        assertThat(corpo).contains("BAIXA");
    }

    @Test
    @DisplayName("corpo lista descricao, urgencia e data de envio de cada avaliacao")
    void corpoListaItens() {
        registrar("Microfone falhando", 2, "2026-07-21");

        enviarRelatorioSemanal.executar(null);

        String corpo = enviador.unico().corpoHtml();
        assertThat(corpo).contains("Microfone falhando");
        assertThat(corpo).contains("CRITICA");
        assertThat(corpo).contains("21/07/2026");
    }

    @Test
    @DisplayName("neutraliza HTML vindo do texto do estudante")
    void neutralizaHtmlDoEstudante() {
        registrar("<img src=x onerror=alert(1)>", 5, "2026-07-21");

        enviarRelatorioSemanal.executar(null);

        String corpo = enviador.unico().corpoHtml();
        assertThat(corpo).doesNotContain("<img src=x");
        assertThat(corpo).contains("&lt;img");
    }

    @Test
    @DisplayName("semana sem avaliacoes ainda gera relatorio, informando a ausencia")
    void semanaSemAvaliacoesAindaGeraRelatorio() {
        enviarRelatorioSemanal.executar(null);

        assertThat(enviador.enviados()).hasSize(1);
        assertThat(enviador.unico().corpoHtml()).containsIgnoringCase("nenhuma avaliacao");
    }

    @Test
    @DisplayName("aceita referencia explicita de semana")
    void aceitaReferenciaExplicita() {
        registrar("Da semana passada", 7, "2026-07-15");

        enviarRelatorioSemanal.executar(LocalDate.parse("2026-07-19"));

        assertThat(enviador.unico().corpoHtml()).contains("Da semana passada");
    }
}
