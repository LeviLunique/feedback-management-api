package br.com.fiap.feedback.adapter.out.dynamodb;

import br.com.fiap.feedback.domain.Avaliacao;
import br.com.fiap.feedback.domain.FeedbackRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

/**
 * Adaptador de persistencia em Amazon DynamoDB (SPEC 4.2).
 *
 * <p>As consultas por periodo usam o indice {@code gsi-data}, cuja chave de
 * particao e o dia do envio. Como a chave de particao exige valor exato, o
 * periodo e percorrido dia a dia — o que evita {@code Scan} na tabela inteira.
 */
@ApplicationScoped
public class RepositorioDeFeedbackDynamoDb implements FeedbackRepository {

    private final DynamoDbClient cliente;
    private final String tabela;

    public RepositorioDeFeedbackDynamoDb(
            DynamoDbClient cliente,
            @ConfigProperty(name = "app.dynamodb.tabela") String tabela) {
        this.cliente = cliente;
        this.tabela = tabela;
    }

    @Override
    public void salvar(Avaliacao avaliacao) {
        cliente.putItem(PutItemRequest.builder()
                .tableName(tabela)
                .item(paraItem(avaliacao))
                .build());
    }

    @Override
    public List<Avaliacao> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
        List<Avaliacao> encontradas = new ArrayList<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            encontradas.addAll(consultarDia(dia));
        }
        encontradas.sort(Comparator.comparing(Avaliacao::dataEnvio));
        return List.copyOf(encontradas);
    }

    private List<Avaliacao> consultarDia(LocalDate dia) {
        QueryRequest requisicao = QueryRequest.builder()
                .tableName(tabela)
                .indexName(TabelaDeFeedback.INDICE_POR_DIA)
                .keyConditionExpression("#dia = :dia")
                .expressionAttributeNames(Map.of("#dia", TabelaDeFeedback.DATA_ENVIO_DIA))
                .expressionAttributeValues(Map.of(":dia", AttributeValue.fromS(dia.toString())))
                .build();

        List<Avaliacao> doDia = new ArrayList<>();
        cliente.queryPaginator(requisicao)
                .stream()
                .flatMap(pagina -> pagina.items().stream())
                .map(RepositorioDeFeedbackDynamoDb::deItem)
                .forEach(doDia::add);
        return doDia;
    }

    private static Map<String, AttributeValue> paraItem(Avaliacao avaliacao) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(TabelaDeFeedback.ID, AttributeValue.fromS(avaliacao.id().toString()));
        item.put(TabelaDeFeedback.DESCRICAO, AttributeValue.fromS(avaliacao.descricao()));
        item.put(TabelaDeFeedback.NOTA, AttributeValue.fromN(Integer.toString(avaliacao.nota())));
        item.put(TabelaDeFeedback.DATA_ENVIO, AttributeValue.fromS(avaliacao.dataEnvio().toString()));
        item.put(TabelaDeFeedback.DATA_ENVIO_DIA, AttributeValue.fromS(diaDe(avaliacao.dataEnvio())));
        // Valor derivado da nota, gravado apenas para inspecao da tabela no
        // console; na leitura a urgencia e sempre recalculada.
        item.put(TabelaDeFeedback.URGENCIA, AttributeValue.fromS(avaliacao.urgencia().name()));
        return item;
    }

    private static Avaliacao deItem(Map<String, AttributeValue> item) {
        return new Avaliacao(
                UUID.fromString(item.get(TabelaDeFeedback.ID).s()),
                item.get(TabelaDeFeedback.DESCRICAO).s(),
                Integer.parseInt(item.get(TabelaDeFeedback.NOTA).n()),
                Instant.parse(item.get(TabelaDeFeedback.DATA_ENVIO).s()));
    }

    static String diaDe(Instant dataEnvio) {
        return LocalDate.ofInstant(dataEnvio, ZoneOffset.UTC).toString();
    }
}
