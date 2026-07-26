package br.com.fiap.feedback.adapter.out.dynamodb;

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Cria a tabela de feedback no DynamoDB local usado em dev e teste.
 *
 * <p>Em producao a tabela pertence a infraestrutura como codigo (SAM), por isso
 * este componente nao existe no perfil {@code prod}: aplicacao nunca deve criar
 * a propria infraestrutura em nuvem.
 */
@UnlessBuildProfile("prod")
@ApplicationScoped
public class PreparadorDeTabelaLocal {

    private static final Logger LOG = Logger.getLogger(PreparadorDeTabelaLocal.class);

    private final DynamoDbClient cliente;
    private final String tabela;

    public PreparadorDeTabelaLocal(
            DynamoDbClient cliente,
            @ConfigProperty(name = "app.dynamodb.tabela") String tabela) {
        this.cliente = cliente;
        this.tabela = tabela;
    }

    void aoIniciar(@Observes StartupEvent evento) {
        try {
            cliente.createTable(CreateTableRequest.builder()
                    .tableName(tabela)
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(
                            definicao(TabelaDeFeedback.ID),
                            definicao(TabelaDeFeedback.DATA_ENVIO_DIA),
                            definicao(TabelaDeFeedback.DATA_ENVIO))
                    .keySchema(chave(TabelaDeFeedback.ID, KeyType.HASH))
                    .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                            .indexName(TabelaDeFeedback.INDICE_POR_DIA)
                            .keySchema(
                                    chave(TabelaDeFeedback.DATA_ENVIO_DIA, KeyType.HASH),
                                    chave(TabelaDeFeedback.DATA_ENVIO, KeyType.RANGE))
                            .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                            .build())
                    .build());

            cliente.waiter().waitUntilTableExists(
                    DescribeTableRequest.builder().tableName(tabela).build());
            LOG.infof("Tabela local '%s' criada com o indice '%s'.",
                    tabela, TabelaDeFeedback.INDICE_POR_DIA);
        } catch (ResourceInUseException jaExiste) {
            LOG.debugf("Tabela local '%s' ja existe; nada a fazer.", tabela);
        }
    }

    private static AttributeDefinition definicao(String atributo) {
        return AttributeDefinition.builder()
                .attributeName(atributo)
                .attributeType(ScalarAttributeType.S)
                .build();
    }

    private static KeySchemaElement chave(String atributo, KeyType tipo) {
        return KeySchemaElement.builder().attributeName(atributo).keyType(tipo).build();
    }
}
