# Monitoramento e entrega contínua

Como a aplicação é observada em produção e como o código chega até lá, atendendo aos requisitos
de "aplicação monitorada" e "deploy automatizado dos componentes atualizáveis".

## 1. Logs

Cada função escreve logs estruturados em **JSON** (extensão `quarkus-logging-json`), o que
permite filtrar por campo no CloudWatch Logs Insights em vez de fazer busca por texto. Em
desenvolvimento o formato volta a ser legível, para não atrapalhar o dia a dia.

Cada função tem seu próprio grupo de logs, com **retenção de 14 dias** (parametrizável em
`RetencaoDeLogsEmDias`) — o suficiente para investigar incidentes sem acumular custo:

| Grupo | Conteúdo |
|---|---|
| `/aws/lambda/feedback-management-feedback-intake-fn` | Ingestão e consultas |
| `/aws/lambda/feedback-management-urgent-notification-fn` | Envio de alertas de urgência |
| `/aws/lambda/feedback-management-weekly-report-fn` | Geração do relatório semanal |
| `/aws/apigateway/feedback-management` | Acesso à API |

O log de acesso do API Gateway registra método, rota, status, latência e origem — **nunca o
corpo da requisição**, para que o texto escrito pelo aluno não seja replicado.

Consulta útil no Logs Insights:

```
fields @timestamp, level, message
| filter level = "ERROR"
| sort @timestamp desc
| limit 50
```

## 2. Alarmes

Quatro alarmes publicam no tópico SNS `feedback-management-ops-alerts`, que envia e-mail ao
endereço informado no parâmetro `AlertEmail`:

| Alarme | Dispara quando |
|---|---|
| `feedback-management-intake-erros` | A função de ingestão falha (≥ 1 erro em 5 min) |
| `feedback-management-notificacao-erros` | O aviso de feedback crítico não é enviado |
| `feedback-management-relatorio-erros` | O relatório semanal falha |
| `feedback-management-api-5xx` | A API responde com erro de servidor ao estudante |

Duas decisões que evitam ruído e pontos cegos:

**`TreatMissingData: notBreaching`** — sem invocações não há falha. Sem isso, um período sem
tráfego (comum de madrugada) dispararia alarme por falta de dados.

**`OKActions` habilitado** — você recebe e-mail também quando o problema se resolve. Um alarme
que só avisa o início deixa a dúvida se o incidente terminou.

> O tópico usa assinatura por e-mail: a AWS envia uma mensagem de confirmação no primeiro
> deploy, e os alarmes só chegam **depois do aceite**. Sem esse clique, os alarmes disparam mas
> ninguém é avisado.

## 3. Painel

O dashboard `feedback-management-monitoramento` reúne, em uma tela:

- **Invocações por função** — volume de uso de cada componente
- **Erros por função** — falhas, separadas por responsabilidade
- **Duração p95** — latência real percebida, sem o efeito de médias
- **DynamoDB: capacidade consumida** — leituras e gravações, útil para acompanhar custo
- **API Gateway** — requisições, erros de cliente (4xx), de servidor (5xx) e latência p95

A URL sai como output da stack (`PainelCloudWatch`) após o deploy.

## 4. Rastreamento

As funções sobem com `Tracing: Active` (AWS X-Ray), permitindo seguir uma requisição desde o
API Gateway até a chamada ao DynamoDB ou ao SNS e identificar onde o tempo foi gasto.

## 5. Entrega contínua

Dois workflows em `.github/workflows/`:

**`ci.yml`** roda em todo pull request e em qualquer branch que não seja `main`: build completo,
testes unitários e de integração e o gate de cobertura de 80%. Os testes de integração usam
Testcontainers com LocalStack, então o pipeline **não precisa de credenciais da AWS**. Os
relatórios de cobertura ficam disponíveis como artefato.

**`deploy.yml`** roda no merge para `main`: repete os testes, compila as três funções em binário
nativo GraalVM e publica a stack com SAM. Um `concurrency group` impede dois deploys simultâneos
sobre a mesma stack.

### Autenticação sem chaves

O deploy usa **OIDC**: o GitHub emite um token de identidade de curta duração e a AWS o troca por
credenciais temporárias. Nenhuma access key é armazenada no repositório.

A confiança é restrita no próprio template ([infra/github-oidc.yaml](../infra/github-oidc.yaml)):
somente **este repositório** e somente o branch **main** conseguem assumir a role. Um fork ou
outro branch não conseguem, mesmo com acesso ao workflow.

A permissão mais sensível do pipeline — criar roles IAM — está limitada ao prefixo
`feedback-management-*`, aproveitando o fato de o CloudFormation nomear os recursos com o nome
da stack. O pipeline não consegue criar ou alterar roles fora do projeto.

### Configuração no GitHub (uma vez)

Aplique o bootstrap da role:

```bash
aws cloudformation deploy \
  --template-file infra/github-oidc.yaml \
  --stack-name feedback-management-github-oidc \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides GitHubRepo=SEU-USUARIO/feedback-management-api
```

> A conta já possui o provedor OIDC do GitHub, por isso o parâmetro `CriarProvedorOidc` fica em
> `nao`. Em conta nova, use `sim` — criar um segundo provedor para o mesmo emissor falha.

Pegue o ARN da role no output e cadastre em *Settings → Secrets and variables → Actions*:

| Segredo | Conteúdo |
|---|---|
| `AWS_ROLE_ARN` | ARN da role de deploy |
| `SENDER_EMAIL` | Remetente verificado no SES |
| `ADMIN_EMAILS` | Destinatários das notificações e do relatório |
| `ALERT_EMAIL` | Destinatário dos alarmes operacionais |

Opcionalmente, a variável `AWS_REGION` (padrão `us-east-1`).
