# Segurança e governança de acesso

Como a solução protege os dados dos alunos e controla quem pode fazer o quê, atendendo aos
requisitos de "configurações de segurança relacionadas aos dados de clientes" e "governança de
acesso" do Tech Challenge.

## 1. Identidade e acesso — menor privilégio por função

Cada função Lambda tem **sua própria role**, criada pelo SAM a partir das permissões declaradas
em [`infra/template.yaml`](../infra/template.yaml). Nenhuma política usa `*` em ações ou recursos.

| Função | Pode fazer | Sobre qual recurso |
|---|---|---|
| `feedback-intake-fn` | `dynamodb:PutItem`, `dynamodb:Query` | Apenas a tabela da stack e seu índice `gsi-data` |
| | `sns:Publish` | Apenas o tópico de feedbacks críticos da stack |
| `urgent-notification-fn` | `ses:SendEmail`, `ses:SendRawEmail` | Apenas a identidade do remetente verificado |
| `weekly-report-fn` | `dynamodb:Query` | Apenas a tabela e o índice — **sem permissão de escrita** |
| | `ses:SendEmail`, `ses:SendRawEmail` | Apenas a identidade do remetente verificado |

Dois pontos merecem destaque:

A função de relatório **não consegue alterar dados**, só ler. Se ela for comprometida ou tiver
um bug, o pior caso é um relatório errado — nunca a perda de avaliações.

O envio de e-mail é restrito por uma condição `ses:FromAddress`, que impede a função de enviar
mensagens se passando por outro remetente, mesmo que o código tente.

Além dessas permissões, o SAM anexa a `AWSLambdaBasicExecutionRole`, que dá apenas escrita nos
logs da própria função.

## 2. Proteção dos dados dos alunos

**Em trânsito**: o API Gateway HTTP API só aceita HTTPS; não existe listener em texto claro.
A comunicação entre Lambda, DynamoDB, SNS e SES usa os endpoints TLS da AWS.

**Em repouso**: a tabela DynamoDB tem `SSESpecification` habilitado e o tópico SNS usa a chave
gerenciada `alias/aws/sns`. Ambos sem custo adicional.

**Recuperação**: a tabela tem *Point-in-Time Recovery* ligado, permitindo restaurar qualquer
instante dos últimos 35 dias em caso de exclusão acidental.

**Nos logs**: o formato de log de acesso do API Gateway registra método, rota, status e latência
— **nunca o corpo da requisição**. O texto que o aluno escreveu não é replicado para o log.

**Contra injeção**: a descrição da avaliação é texto livre e, portanto, entrada não confiável.
Todo conteúdo do aluno passa por escape de HTML antes de entrar nos e-mails de urgência e de
relatório, evitando que uma avaliação maliciosa execute script na caixa de entrada do
administrador. Há testes automatizados cobrindo esse cenário.

## 3. Disponibilidade e custo

O API Gateway aplica *throttling* (padrão: 20 req/s com pico de 40), configurável por parâmetro.
Isso limita tanto abuso quanto o risco de uma rajada consumir os créditos de nuvem.

O DynamoDB é *on-demand*: escala a zero e não cobra capacidade ociosa.

## 4. Governança de acesso

**Nenhuma credencial no repositório.** Remetente e destinatários entram como parâmetros da
stack; a região e o nome da tabela chegam por variável de ambiente injetada pelo CloudFormation.
O `.gitignore` exclui `.env`, e não há chave de acesso em nenhum arquivo versionado.

**Desenvolvimento local** usa AWS IAM Identity Center (SSO): o token expira em poucas horas e
não existe chave de longa duração na máquina. Os testes automatizados sequer tocam a AWS —
rodam contra LocalStack via Testcontainers.

**Deploy automatizado** usará OIDC no GitHub Actions (entrega seguinte), com uma role assumida
por confiança no repositório. Também aqui, nenhuma chave de longa duração é armazenada.

**Rastreabilidade**: o CloudTrail registra por padrão as chamadas de API na conta, e cada função
tem seu grupo de logs próprio, com retenção de 14 dias.

**Infraestrutura só por código**: toda a stack nasce do `template.yaml`. Alterações manuais no
console são proibidas por gerarem *drift* — divergência entre o que está declarado e o que roda.
A aplicação também nunca cria a própria infraestrutura em nuvem: os componentes que provisionam
tabela, tópico e identidades existem apenas nos perfis de desenvolvimento e teste, anotados com
`@UnlessBuildProfile("prod")`.

## 5. Limitações conhecidas

**O endpoint de avaliação é público**, sem autenticação — decisão registrada no PRD, já que o
aluno avalia a aula sem login. A proteção é o throttling e a validação de payload. Em produção
real, o caminho seria um authorizer JWT no API Gateway.

**O SES opera em sandbox**: só envia para endereços verificados, o que é suficiente para a
demonstração. Sair do sandbox exige solicitação à AWS.

**A consulta de avaliações também é pública** nesta entrega. Como ela expõe feedbacks de alunos,
em produção deveria exigir perfil de administrador.
