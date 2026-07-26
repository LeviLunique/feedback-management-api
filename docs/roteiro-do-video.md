# Roteiro do vídeo de demonstração

Entregável obrigatório do Tech Challenge: vídeo mostrando a aplicação em funcionamento, as
funções serverless ativas e as configurações do projeto. Sugestão de 8 a 12 minutos.

Antes de gravar, confira: token do SSO válido (`aws sso login --profile fiap`), stack publicada,
identidade verificada no SES e assinatura do tópico de alertas confirmada.

---

## 1. Abertura (~1 min)

Apresente o problema em uma frase: *"plataforma de feedback de aulas em que estudantes avaliam e
administradores são avisados de problemas graves e recebem um relatório semanal"*.

Mostre o README e diga os números: três funções serverless, 140 testes, cobertura acima de 80%.

## 2. Modelo de nuvem e arquitetura (~2 min)

Abra [arquitetura.md](arquitetura.md) no diagrama da §3 e percorra o caminho de uma avaliação.

Explique **por que serverless**: as três responsabilidades têm cargas muito diferentes (picos no
fim das aulas, notificação rara, relatório semanal); um servidor sempre ligado pagaria ociosidade.
Cite escala a zero e pagamento por uso, importantes com créditos limitados.

Justifique duas escolhas concretas:
- **DynamoDB em vez de RDS**: RDS não escala a zero e exigiria VPC, encarecendo e atrasando o cold start.
- **SNS entre ingestão e e-mail**: a API responde ao estudante sem esperar o SES.

## 3. Código e qualidade (~2 min)

Mostre a estrutura hexagonal: `domain` sem nenhum import de AWS, `application` com os casos de uso,
`adapter` na borda.

Abra `Avaliacao.java` e destaque que **a urgência não é campo armazenado** — é derivada da nota,
o que torna impossível o cliente forjá-la ou o registro ficar inconsistente.

Rode `./mvnw verify` e mostre o gate de cobertura falhando o build se cair abaixo de 80%.

Explique por que são três módulos Maven: o Quarkus recusa a extensão HTTP junto com um handler
customizado, então cada função é um artefato — o que coincide com a responsabilidade única.

## 4. Console AWS: as funções ativas (~2 min)

No console, mostre em sequência:

- **Lambda**: as três funções, o runtime `provided.al2023` e a arquitetura `arm64`.
- **API Gateway**: a HTTP API e o throttling configurado.
- **DynamoDB**: a tabela, o índice `gsi-data`, criptografia em repouso e PITR ligados.
- **SNS**: os tópicos `feedback-critico` e `ops-alerts`.
- **EventBridge Scheduler**: o agendamento de segunda às 08:00.

Abra a role de uma função em **IAM** e mostre que não há `*`. Destaque que a função de relatório
tem apenas `dynamodb:Query` — não consegue alterar dados.

## 5. Aplicação funcionando (~2 min)

Com a URL da API publicada:

```bash
export BASE_URL="https://<api-id>.execute-api.us-east-1.amazonaws.com/v1"

# Avaliação positiva
curl -X POST "$BASE_URL/api/v1/avaliacao" -H 'Content-Type: application/json' \
  -d '{"descricao":"Aula muito bem explicada","nota":9}'

# Avaliação crítica: dispara a notificação
curl -X POST "$BASE_URL/api/v1/avaliacao" -H 'Content-Type: application/json' \
  -d '{"descricao":"Aula sem audio do inicio ao fim","nota":0}'

# Validação rejeitada
curl -X POST "$BASE_URL/api/v1/avaliacao" -H 'Content-Type: application/json' \
  -d '{"descricao":"","nota":42}'
```

Mostre que a nota 9 virou `BAIXA` e a nota 0 virou `CRITICA`, e que o payload inválido devolveu
`400` com **as duas mensagens de uma vez**.

**Abra a caixa de entrada e mostre o e-mail de urgência chegando**, com descrição, urgência e
data de envio — exatamente os dados exigidos.

Depois as consultas:

```bash
curl "$BASE_URL/api/v1/avaliacoes?urgencia=CRITICA"
curl "$BASE_URL/api/v1/relatorios/semanal"
```

## 6. Relatório semanal (~1 min)

O agendamento é semanal, então invoque a função na hora para demonstrar:

```bash
aws lambda invoke --function-name feedback-management-weekly-report-fn \
  --payload '{}' --cli-binary-format raw-in-base64-out /dev/stdout
```

Mostre o e-mail do relatório: média das notas, quantidade por dia, quantidade por urgência e a
lista com descrição, urgência e data de envio.

## 7. Monitoramento (~1,5 min)

- **Dashboard CloudWatch**: invocações, erros, duração p95 e capacidade do DynamoDB.
- **Logs**: abra o grupo da função de ingestão e mostre o log estruturado em JSON.
- **Alarmes**: os quatro alarmes e o tópico que envia e-mail.
- **X-Ray**: um trace ponta a ponta, do API Gateway até o DynamoDB.

## 8. Deploy automatizado e segurança (~1,5 min)

Mostre `infra/template.yaml` e explique que **toda** a infraestrutura nasce dele — nada é criado
pelo console.

Abra `.github/workflows/deploy.yml` e destaque a autenticação por **OIDC**: nenhuma access key
armazenada, e a confiança restrita a este repositório e ao branch `main`.

Feche com [seguranca.md](seguranca.md): criptografia em repouso e em trânsito, throttling, log de
acesso sem corpo da requisição e escape de HTML no conteúdo escrito pelo aluno.

## 9. Encerramento (~30 s)

Recapitule o atendimento aos requisitos e mencione as limitações conhecidas com honestidade
(endpoint público, SES em sandbox). Reconhecer limites demonstra domínio do trade-off.

---

## Roteiro de recuperação

Se algo falhar durante a gravação:

| Sintoma | Causa provável |
|---|---|
| `403` na API | Token do SSO expirado — não afeta a API, mas afeta os comandos `aws` |
| E-mail não chega | Endereço não verificado no SES, ou assinatura do tópico não confirmada |
| Alarme não notifica | Assinatura do tópico `ops-alerts` pendente de confirmação |
| Primeira chamada lenta | Cold start; repita a chamada e mostre a diferença — é um bom momento para falar do binário nativo |
