# Feedback Management API

Plataforma serverless de feedback de aulas para o Tech Challenge FIAP (Fase 4): estudantes
enviam avaliações, administradores recebem notificações automáticas de itens críticos e um
relatório semanal consolidado por e-mail.

> **Status**: em desenvolvimento incremental. Toda a regra de negócio está pronta — registro e
> consulta de avaliações com persistência em DynamoDB, classificação de urgência, notificação
> automática de feedbacks críticos e relatório semanal por e-mail. Faltam a infraestrutura como
> código, o pipeline de deploy e o build nativo.

## Stack e requisitos
- Java 21 (bytecode `--release 21`; compila com JDK 21 ou superior), Maven 3.9+ (wrapper `mvnw` incluído)
- Quarkus 3.37 compilado nativo com GraalVM (Mandrel) para AWS Lambda
- AWS: Lambda, API Gateway (HTTP API), DynamoDB, SNS, SES, EventBridge Scheduler, CloudWatch
- IaC com AWS SAM (`infra/template.yaml`) e deploy automatizado via GitHub Actions (OIDC)
- Docker (Testcontainers, Newman e build nativo em container)
- OpenAPI/Swagger UI em `/q/swagger-ui` (dev)

## Arquitetura

Três funções serverless, cada uma com responsabilidade única:

| Função | Trigger | Responsabilidade |
|---|---|---|
| `feedback-intake-fn` | API Gateway | Receber, validar e persistir avaliações; publicar evento crítico; servir consultas |
| `urgent-notification-fn` | SNS `feedback-critico` | Enviar e-mail de urgência aos administradores (SES) |
| `weekly-report-fn` | EventBridge (cron semanal) | Agregar a semana e enviar o relatório por e-mail (SES) |

Avaliações são persistidas no DynamoDB; notas 0–2 são classificadas como urgência `CRITICA`
e disparam a notificação.

O código segue arquitetura hexagonal (ports & adapters): `domain` concentra as regras de
negócio sem dependência de framework, `application` orquestra os casos de uso e
`adapter.in` / `adapter.out` isolam HTTP, DynamoDB, SNS e SES.

Cada função serverless é um módulo Maven com seu próprio artefato de deploy:

| Módulo | Função | Papel |
|---|---|---|
| `feedback-core` | — | Domínio, casos de uso e adaptadores de saída, compartilhados |
| `feedback-api` | `feedback-intake-fn` | Recursos REST atrás do API Gateway |
| `feedback-notification` | `urgent-notification-fn` | Consome o tópico SNS e envia o e-mail de urgência |
| `feedback-report` | `weekly-report-fn` | Agrega a semana e envia o relatório por e-mail |

A separação é imposta pelo Quarkus, que não permite a extensão HTTP e um handler Lambda
customizado no mesmo artefato — e coincide com a responsabilidade única exigida no desafio.

## Como executar

### Dev local
```bash
./mvnw install -DskipTests
./mvnw -pl feedback-api quarkus:dev
```
Aplicação em `http://localhost:8080` (health em `/q/health`, Swagger em `/q/swagger-ui`).
O primeiro comando publica o `feedback-core` no repositório local; depois disso basta o segundo.

O Quarkus sobe automaticamente um container (LocalStack) via Dev Services e provisiona tabela,
tópico e identidades de e-mail — não é preciso nenhuma credencial AWS para desenvolver ou
rodar os testes, só o Docker ativo.

### Testes e qualidade
```bash
./mvnw verify
```
Executa os testes unitários e de integração de todos os módulos e aplica o gate de cobertura
do JaCoCo por módulo: o build **falha** se a cobertura de linhas ficar abaixo de 80%.
Relatórios em `<módulo>/target/site/jacoco/index.html`.

Os testes de integração usam Testcontainers com LocalStack — nenhuma chamada atinge a AWS.

### Deploy na AWS

Toda a infraestrutura está declarada em [infra/template.yaml](infra/template.yaml) (AWS SAM):
tabela DynamoDB com índice, tópico SNS, API HTTP com throttling, as três funções Lambda com
uma role de menor privilégio cada, o agendamento semanal e os grupos de log.

```bash
export AWS_PROFILE=fiap
export SENDER_EMAIL="seu-email-verificado@exemplo.com"
export ADMIN_EMAILS="seu-email-verificado@exemplo.com"

./mvnw package -Dnative                       # gera os function.zip nativos
cd infra && sam deploy \
  --parameter-overrides "SenderEmail=$SENDER_EMAIL AdminEmails=$ADMIN_EMAILS"
```

O `samconfig.toml` já fixa nome da stack, região e capabilities — os e-mails entram pela linha
de comando justamente para não ficarem versionados.

Para conferir o que mudaria antes de aplicar, acrescente `--no-execute-changeset`.
Ao final da demonstração, `sam delete` remove a stack inteira.

Saídas úteis após o deploy (`ApiUrl`, `NomeDaTabela`, `TopicoDeFeedbackCritico`) aparecem no
terminal e podem ser consultadas com:

```bash
aws cloudformation describe-stacks --stack-name feedback-management --query 'Stacks[0].Outputs'
```

### Variáveis de ambiente principais
- `AWS_PROFILE` — perfil autenticado via SSO (`aws sso login --profile fiap`)
- `SENDER_EMAIL` — remetente verificado no SES
- `ADMIN_EMAILS` — destinatários das notificações e do relatório semanal

Em execução na AWS, `FEEDBACK_TABLE`, `FEEDBACK_CRITICO_TOPIC_ARN` e `AWS_REGION` são injetadas
automaticamente pelo CloudFormation e pelo próprio Lambda — não precisam ser definidas à mão.

## Endpoints

Já disponíveis:

| Endpoint | Descrição |
|---|---|
| `POST /api/v1/avaliacao` | Registra avaliação `{ "descricao": string, "nota": 0..10 }` e retorna a urgência derivada |
| `GET /api/v1/avaliacoes` | Lista avaliações do período (`dataInicio`, `dataFim`, `urgencia` — todos opcionais) |
| `GET /api/v1/relatorios/semanal` | Consolidado da semana (`referencia` opcional): média, totais por dia e por urgência |
| `GET /q/health` | Health check com nome e versão da build ativa |
| `GET /q/openapi` | Especificação OpenAPI 3 da API |
| `GET /q/swagger-ui` | Swagger UI (apenas em dev) |

Exemplo de registro de avaliação:

```bash
curl -X POST http://localhost:8080/api/v1/avaliacao \
  -H 'Content-Type: application/json' \
  -d '{"descricao":"Aula com audio ruim","nota":2}'
```

```json
{
  "id": "02d67b53-7f9a-43da-8c51-3b16e7ea25ac",
  "descricao": "Aula com audio ruim",
  "nota": 2,
  "urgencia": "CRITICA",
  "dataEnvio": "2026-07-26T16:21:10.527129Z"
}
```

A urgência é sempre derivada da nota pelo servidor — notas 0–2 viram `CRITICA`, 3–5 `ALTA`,
6–7 `MEDIA` e 8–10 `BAIXA` — e nunca é aceita do cliente. Payloads inválidos retornam `400`
com o corpo padrão `{ "status", "erro", "mensagens": [...] }`, acumulando todos os erros.

A consulta sem filtros devolve os últimos 7 dias, no formato `{ "itens": [...], "total": n }`.
Datas usam `yyyy-MM-dd` e o período é limitado a 366 dias:

```bash
curl "http://localhost:8080/api/v1/avaliacoes?dataInicio=2026-07-20&dataFim=2026-07-26&urgencia=CRITICA"
```

O relatório semanal cobre de segunda a domingo — sem `referencia`, a semana corrente:

```bash
curl "http://localhost:8080/api/v1/relatorios/semanal"
```

```json
{
  "inicio": "2026-07-20", "fim": "2026-07-26",
  "mediaNotas": 5.0, "totalAvaliacoes": 2,
  "avaliacoesPorDia": { "2026-07-26": 2 },
  "avaliacoesPorUrgencia": { "CRITICA": 1, "ALTA": 0, "MEDIA": 0, "BAIXA": 1 },
  "itens": [ { "id": "...", "descricao": "...", "nota": 1, "urgencia": "CRITICA", "dataEnvio": "..." } ]
}
```

## Postman
Coleção em `postman/feedback-management-api.postman_collection.json` com testes de asserção
para os endpoints já implementados — cada entrega adiciona os seus. Importe a coleção junto
com o environment `postman/feedback-management-api.environment.json`.

### Testes automatizados via Newman (Docker)
```bash
./mvnw quarkus:dev   # em um terminal
./scripts/run-postman.sh
```
- Por padrão roda contra `http://host.docker.internal:8080` (app local).
- Para rodar contra a AWS: `BASE_URL="https://<api-id>.execute-api.us-east-1.amazonaws.com" ./scripts/run-postman.sh`.
- Usa a imagem `postman/newman:6-alpine`, com builds nativos para amd64 e arm64 (Apple Silicon).
  Para trocar a imagem: `NEWMAN_IMAGE=postman/newman:alpine ./scripts/run-postman.sh`.

## Monitoramento
- Logs estruturados (JSON) no CloudWatch Logs, retenção de 14 dias.
- Alarmes de erro por função e 5XX do API Gateway com notificação por e-mail.
- Dashboard CloudWatch com invocações, erros e duração p95.
- Health check em `/q/health` expõe o nome e a versão da build ativa em cada função.

## Segurança e governança
- IAM com menor privilégio por função: uma role por Lambda, sem `*` em ação ou recurso.
  A função de relatório, por exemplo, tem apenas `dynamodb:Query` — não consegue alterar dados.
- Envio de e-mail restrito por condição `ses:FromAddress`, impedindo a função de se passar por
  outro remetente mesmo que o código tente.
- TLS obrigatório no API Gateway, criptografia em repouso no DynamoDB e no SNS,
  Point-in-Time Recovery na tabela e throttling de requisições.
- Log de acesso do API Gateway sem corpo de requisição: o texto do aluno não vai para o log.
- Nenhuma credencial no código: configuração por variáveis de ambiente e parâmetros da stack.
- Detalhes e limitações conhecidas: [docs/seguranca.md](docs/seguranca.md)

## Troubleshooting
- **Build nativo falhando local**: use o build em container — `./mvnw package -Dnative` (o profile
  `native` já ativa `quarkus.native.container-build`, dispensando o GraalVM instalado na máquina).
- **SES não envia e-mail**: conta em sandbox — verifique remetente e destinatários (`aws ses verify-email-identity`).
- **Testcontainers sem Docker**: garanta o Docker Desktop ativo antes do `./mvnw verify`.
