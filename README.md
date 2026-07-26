# Feedback Management API

Plataforma serverless de feedback de aulas para o Tech Challenge FIAP (Fase 4): estudantes
enviam avaliações, administradores recebem notificações automáticas de itens críticos e um
relatório semanal consolidado por e-mail.

## Stack e requisitos
- Java 21, Maven 3.9+ (wrapper `mvnw` incluído)
- Quarkus 3.x compilado nativo com GraalVM (Mandrel) para AWS Lambda
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
e disparam a notificação. Detalhes, decisões e modelo de cloud: [docs/arquitetura.md](docs/arquitetura.md).

## Como executar

### Dev local
```bash
./mvnw quarkus:dev
```
Aplicação em `http://localhost:8080` (health em `/q/health`, Swagger em `/q/swagger-ui`).

### Testes e qualidade
```bash
./mvnw verify   # unit + integração (Testcontainers) + gate de cobertura JaCoCo >= 80%
```
Relatório de cobertura em `target/site/jacoco/index.html`.

### Deploy na AWS
```bash
sam build && sam deploy --guided   # primeira vez; depois: sam deploy
```
O push em `main` também dispara o deploy automatizado via GitHub Actions.

### Variáveis de ambiente principais
- `AWS_PROFILE`, `AWS_REGION` — credenciais/região do deploy
- `SENDER_EMAIL` — remetente verificado no SES
- `ADMIN_EMAILS` — destinatários das notificações e do relatório semanal

## Endpoints principais (base `/api/v1`)
- `POST /avaliacao` – registra avaliação `{ "descricao": string, "nota": 0..10 }`; retorna urgência derivada.
- `GET /avaliacoes?dataInicio=&dataFim=&urgencia=` – lista avaliações com filtros (análise dos admins).
- `GET /relatorios/semanal?referencia=` – agregado semanal (média, totais por dia e por urgência).
- Health: `/q/health`
- OpenAPI: `/q/openapi` · Swagger UI (dev): `/q/swagger-ui`

## Postman
Coleção pronta em `postman/feedback-management-api.postman_collection.json` com testes de
asserção para todos os endpoints. Importe a coleção e o environment
`postman/feedback-management-api.environment.json`.

### Testes automatizados via Newman (Docker)
```bash
./mvnw quarkus:dev   # em um terminal
./scripts/run-postman.sh
```
- Por padrão roda contra `http://host.docker.internal:8080` (app local).
- Para rodar contra a AWS: `BASE_URL="https://<api-id>.execute-api.us-east-1.amazonaws.com" ./scripts/run-postman.sh`.
- Em Apple Silicon, para eliminar o aviso de plataforma: `NEWMAN_PLATFORM=linux/arm64/v8 ./scripts/run-postman.sh`.

## Monitoramento
- Logs estruturados (JSON) no CloudWatch Logs, retenção de 14 dias.
- Alarmes de erro por função e 5XX do API Gateway com notificação por e-mail.
- Dashboard CloudWatch com invocações, erros e duração p95.
- Detalhes: [docs/monitoramento.md](docs/monitoramento.md)

## Segurança e governança
- IAM com menor privilégio por função (uma role por Lambda, sem `*`).
- Deploy do CI via OIDC (sem access keys de longa duração).
- TLS no API Gateway, criptografia em repouso no DynamoDB, throttling de requisições.
- Detalhes: [docs/seguranca.md](docs/seguranca.md)

## Troubleshooting
- **Build nativo falhando local**: use o build em container — `./mvnw package -Dnative -Dquarkus.native.container-build=true`.
- **SES não envia e-mail**: conta em sandbox — verifique remetente e destinatários (`aws ses verify-email-identity`).
- **Testcontainers sem Docker**: garanta o Docker Desktop ativo antes do `./mvnw verify`.
