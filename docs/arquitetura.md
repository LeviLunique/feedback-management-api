# Arquitetura da solução

Decisões de arquitetura da Feedback Management API, o modelo de nuvem escolhido e os
componentes envolvidos.

## 1. Problema e forma da solução

Estudantes avaliam aulas; administradores precisam saber **imediatamente** de problemas graves e
receber um consolidado periódico. São três responsabilidades com perfis de carga muito diferentes:

- **Ingestão**: picos concentrados no fim das aulas, ociosidade o resto do tempo.
- **Notificação**: rara, mas urgente.
- **Relatório**: uma vez por semana.

Um único processo sempre ligado atenderia os três casos pagando por capacidade ociosa quase o
tempo todo. Daí a escolha por **FaaS**.

## 2. Modelo de nuvem

**Nuvem pública AWS**, com computação **serverless (FaaS)** e serviços gerenciados (BaaS):

| Critério | Por que se encaixa |
|---|---|
| Escala a zero | Sem avaliações, o custo de computação é zero — decisivo com créditos limitados |
| Pagamento por uso | Cobra por invocação e duração, não por hora de servidor reservado |
| Sem gestão de servidores | Nada de patch de SO, dimensionamento ou balanceador |
| Isolamento por função | Cada responsabilidade escala, falha e é monitorada isoladamente |

O contraponto honesto é o **cold start**, que o binário nativo GraalVM mitiga (detalhe na §6).

## 3. Componentes

```
                    ┌──────────────────────────────────────────────────┐
  Estudante         │                      AWS                          │
     │              │                                                   │
     │ POST         │   ┌─────────────┐      ┌──────────────────────┐  │
     │ /avaliacao   │   │ API Gateway │─────▶│  feedback-intake-fn  │  │
     ├─────────────────▶│  (HTTP API) │      │       (Lambda)       │  │
     │              │   │  throttling │      └──────────┬───────────┘  │
  Administrador     │   └─────────────┘                 │              │
     │ GET          │                        ┌──────────▼───────────┐  │
     │ /avaliacoes  │                        │  DynamoDB (feedback) │  │
     │ /relatorios  │                        │   PK id + gsi-data   │  │
     └─────────────────────────────────────▶ └──────────┬───────────┘  │
                    │                                   │ (urgência    │
                    │                        ┌──────────▼──────────┐   │
                    │                        │   SNS feedback-      │  │
                    │                        │      critico         │  │
                    │                        └──────────┬───────────┘  │
                    │                                   │              │
                    │                     ┌─────────────▼───────────┐  │
                    │                     │ urgent-notification-fn  │  │
                    │                     │        (Lambda)         │  │
                    │                     └─────────────┬───────────┘  │
                    │   ┌──────────────┐                │              │
                    │   │  EventBridge │     ┌──────────▼───────────┐  │
                    │   │  Scheduler   │────▶│   weekly-report-fn   │──┼──▶ SES ──▶ e-mail
                    │   │ (seg, 08:00) │     │       (Lambda)       │  │      do admin
                    │   └──────────────┘     └──────────────────────┘  │
                    └──────────────────────────────────────────────────┘
```

| Componente | Serviço | Papel |
|---|---|---|
| Porta de entrada | API Gateway HTTP API | Recebe HTTPS, aplica throttling e roteia para a função |
| Ingestão/consulta | Lambda `feedback-intake-fn` | Valida, persiste, publica críticos, serve consultas |
| Persistência | DynamoDB | Armazena avaliações; índice `gsi-data` para consultas por período |
| Desacoplamento | SNS `feedback-critico` | Separa o registro do envio de e-mail |
| Notificação | Lambda `urgent-notification-fn` | Formata e envia o alerta de urgência |
| Agendamento | EventBridge Scheduler | Dispara o relatório toda segunda às 08:00 |
| Relatório | Lambda `weekly-report-fn` | Agrega a semana encerrada e envia o consolidado |
| E-mail | SES | Entrega as mensagens aos administradores |
| Observabilidade | CloudWatch + X-Ray | Logs, métricas, alarmes, painel e rastreamento |

**HTTP API em vez de REST API**: mais barato e com menor latência; não precisamos de chaves de
API, modelos de request nem transformações que só a REST API oferece.

## 4. Padrão arquitetural: hexagonal (ports & adapters)

O domínio fica no centro, sem saber que existe AWS:

```
feedback-core
├── domain/        Avaliacao, Urgencia, RelatorioSemanal + portas (interfaces)
├── application/   casos de uso que orquestram domínio e portas
└── adapter/out/   implementações das portas: DynamoDB, SNS, SES
feedback-api          → adapter/in/rest  (adaptador de entrada HTTP)
feedback-notification → handler SNS      (adaptador de entrada por evento)
feedback-report       → handler agendado (adaptador de entrada por tempo)
```

As dependências sempre apontam para dentro. `ReceberAvaliacao` depende de `FeedbackRepository`
e `PublicadorDeFeedbackCritico` — interfaces do próprio domínio — e não do SDK da AWS. Por isso
os testes de caso de uso rodam em milissegundos com dublês em memória, e o adaptador de DynamoDB
pode ser trocado sem tocar em regra de negócio.

**Não foi usado**: CQRS, Event Sourcing, camada de serviços anêmica ou repositório genérico.
Nenhum deles resolve um problema que este sistema tenha.

## 5. Um módulo Maven por função

Cada função é um artefato próprio. Isso **não foi escolha estética**: o Quarkus recusa o build
quando a extensão HTTP convive com um handler Lambda customizado —

> *Multiple handler classes. You have a custom handler class and the AWS Lambda HTTP extension.
> Please remove one of them from your deployment.*

O efeito colateral é positivo e alinhado ao enunciado: a separação de artefatos materializa a
responsabilidade única. Cada função tem seu próprio deploy, sua própria role e seu próprio
ciclo de vida.

## 6. Quarkus com GraalVM nativo

As funções são compiladas em **binário nativo** (GraalVM/Mandrel) e executam no runtime
`provided.al2023` sobre **arm64 (Graviton)**.

O motivo é o cold start. Uma Lambda em JVM precisa iniciar a máquina virtual, carregar classes e
inicializar o framework a cada partida a frio — segundos. O binário nativo resolve boa parte
disso em tempo de compilação: o Quarkus faz descoberta de beans, leitura de configuração e
construção do modelo REST durante o build, não na inicialização.

Custo dessa escolha: o build é lento (minutos por função) e reflexão precisa ser declarada.
Como o deploy é infrequente e o cold start afeta todo estudante que avalia uma aula, a troca
compensa.

## 7. Modelagem de dados

Tabela única `feedback`:

| Atributo | Tipo | Papel |
|---|---|---|
| `id` | S | Chave de partição (UUID) |
| `descricao` | S | Texto do feedback |
| `nota` | N | 0 a 10 |
| `dataEnvio` | S | ISO-8601 UTC |
| `dataEnvioDia` | S | `yyyy-MM-dd` — partição do índice |
| `urgencia` | S | Derivada da nota; gravada apenas para leitura no console |

O índice `gsi-data` tem `dataEnvioDia` como partição e `dataEnvio` como ordenação. Como chave de
partição exige valor exato, consultas por período percorrem **um dia por vez** — sete `Query`
para uma semana, em vez de um `Scan` na tabela inteira.

**A urgência não é estado**: `Avaliacao.urgencia()` sempre a calcula a partir da nota. Isso torna
impossível um registro ficar inconsistente consigo mesmo e atende por construção a regra de que
a urgência nunca é aceita do cliente. O atributo gravado serve só para inspeção visual da tabela.

## 8. Fluxo de uma avaliação crítica

1. Estudante envia `POST /api/v1/avaliacao` com nota 1.
2. `feedback-intake-fn` valida, gera id e data, deriva a urgência `CRITICA` e grava no DynamoDB.
3. Como a urgência exige notificação, publica o evento no tópico SNS — **e responde `201` ao
   estudante sem esperar o e-mail**.
4. O SNS invoca `urgent-notification-fn`, que monta o e-mail com descrição, urgência e data.
5. O SES entrega a mensagem ao administrador.

O tópico entre os passos 3 e 4 é o que permite a API responder rápido e um problema no envio ser
reprocessado sem afetar o registro da avaliação.

## 9. Escolhas registradas

| Decisão | Alternativa descartada | Motivo |
|---|---|---|
| DynamoDB | RDS/Aurora | RDS não escala a zero e exige VPC, encarecendo e atrasando o cold start |
| SNS entre ingestão e e-mail | Enviar e-mail na própria ingestão | Deixaria o estudante esperando o SES e acoplaria falhas |
| Filtro de urgência na aplicação | `FilterExpression` no DynamoDB | O filtro é aplicado após a leitura e **não reduz RCU** — mesmo custo, mais complexidade |
| Consulta dia a dia no GSI | `Scan` com filtro | `Scan` lê a tabela inteira e cresce com o volume |
| AWS SAM | Terraform/CDK | Menos peças para um projeto pequeno de Lambda; o SAM já gera roles e permissões |
| Três funções | Uma função com rotas internas | O enunciado exige responsabilidade única por componente |

## 10. Limitações conhecidas

- Endpoints públicos, sem autenticação — decisão registrada no PRD; a mitigação é o throttling.
- SES em sandbox: só envia para endereços verificados.
- A consulta de avaliações deveria exigir perfil de administrador em produção real.
- Sem paginação nas consultas: adequado ao volume do desafio, insuficiente para escala real.
