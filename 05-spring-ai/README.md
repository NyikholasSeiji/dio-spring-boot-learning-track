# API de Orçamento com Spring AI (comandos de voz)

Projeto final da trilha Spring Boot da DIO (módulo 05), evoluído por mim no desafio de projeto.

## O que o projeto faz

É uma API de controle de gastos. Você pode registrar e consultar transações pela API REST **ou por voz**:

1. O cliente envia um arquivo de áudio (ex.: "gastei 80 reais no mercado").
2. O áudio vira texto (OpenAI Whisper, via `TranscriptionModel`).
3. A IA (`ChatClient` + `gpt-4o-mini`) entende a intenção do comando.
4. Por **Tool Calling**, a IA chama um caso de uso real da aplicação (salvar, listar ou resumir gastos).
5. O caso de uso grava/consulta no MySQL.
6. A resposta final é convertida em áudio MP3 (`TextToSpeechModel`) e devolvida ao cliente.

```
áudio ──> transcrição ──> ChatClient ──> @Tool (caso de uso) ──> MySQL
                                   └──> texto final ──> voz (MP3)
```

## Melhorias que implementei

| Melhoria | Onde |
|---|---|
| **Validação antes de salvar**: descrição obrigatória (até 255 caracteres), valor maior que zero e categoria obrigatória. A regra fica no domínio, então vale para a API REST e para a IA. Na API retorna `400` com mensagem clara; na IA, o erro volta para o modelo, que explica o problema ao usuário. | `domain/Transaction.java`, `domain/InvalidTransactionException.java`, `infrastructure/http/ApiExceptionHandler.java` |
| **Nova consulta + nova Tool: resumo dos gastos** (quantidade e total por categoria e total geral). Disponível em `GET /transactions/summary` e como tool `summarize-expenses` ("quanto eu gastei no total?"). | `application/SummarizeExpensesUseCase.java`, `application/output/ExpenseSummaryOutput.java`, `infrastructure/http/response/ExpenseSummaryResponse.java` |
| **Correção de bug nos valores**: o valor é salvo em centavos, mas a resposta não dividia por 100 (8000 centavos aparecia como `8000.0` em vez de `80.0`). | `application/output/Money.java`, `application/output/TransactionOutput.java` |
| **Respostas melhores da IA**: prompt de sistema explica que valores são em centavos, pede para não inventar dados faltantes e responder em até duas frases (a resposta vira áudio). | `resources/prompts/system-message.st` |
| **Validação do upload**: áudio vazio em `/transactions/ai` retorna `400`. | `infrastructure/http/TransactionController.java` |
| **Testes unitários** dos fluxos principais (sem precisar de OpenAI nem banco). | `src/test/java/dio/budgeting/{domain,application,infrastructure}` |

A arquitetura em camadas foi mantida: o controller e a IA chamam os **mesmos casos de uso**, e a validação mora no domínio.

## Tecnologias

- Java 25, Spring Boot 4, Gradle
- Spring AI 2 (OpenAI): `ChatClient`, Tool Calling (`@Tool`), `TranscriptionModel` (Whisper), `TextToSpeechModel`
- Spring Web, Spring Data JPA, MySQL (Docker Compose)
- JUnit 5, AssertJ, MockMvc

## Como executar

Pré-requisitos: **JDK 25**, **Docker** (o Spring Boot sobe o MySQL do `compose.yml` sozinho) e uma **chave da OpenAI**.

Linux/macOS:

```bash
cd 05-spring-ai
export OPENAI_API_KEY="sua_chave_aqui"
./gradlew bootRun
```

Windows (PowerShell):

```powershell
cd 05-spring-ai
$env:OPENAI_API_KEY="sua_chave_aqui"
.\gradlew.bat bootRun
```

A API sobe em `http://localhost:8080`.

## Como testar

### Testes automatizados

```bash
./gradlew test
```

Roda os testes unitários (validação, resumo, valores em reais e endpoints REST com MockMvc), sem precisar de chave nem banco.
Os testes de integração com a OpenAI (`*IT.java`) só rodam se `OPENAI_API_KEY` estiver definida.

### Endpoints REST

O arquivo [`requests.http`](requests.http) tem todas as requisições prontas (IntelliJ / VS Code REST Client). Com `curl`:

```bash
# Criar transação (valor em centavos: 8000 = R$ 80,00)
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Compras no mercado", "category": "GROCERIES", "amount": 8000}'
# {"id":"...","category":"GROCERIES","description":"Compras no mercado","amount":80.0}

# Validação: valor zero
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Compras no mercado", "category": "GROCERIES", "amount": 0}'
# 400 {"title":"Transação inválida","status":400,"detail":"O valor da transação deve ser maior que zero.", ...}

# Listar por categoria (GROCERIES, PHARMA ou AUTO)
curl http://localhost:8080/transactions/GROCERIES

# Resumo dos gastos (novo)
curl http://localhost:8080/transactions/summary
# {"categories":[{"category":"GROCERIES","count":1,"total":80.0},{"category":"PHARMA","count":0,"total":0.0},{"category":"AUTO","count":0,"total":0.0}],"count":1,"total":80.0}
```

### Fluxo principal por voz

Use um dos áudios de exemplo em `src/test/resources/audio` (ou grave o seu):

```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@src/test/resources/audio/recording-1.m4a" \
  -o resposta.mp3
```

Abra o `resposta.mp3` para ouvir a confirmação. Depois grave algo como "quanto eu gastei no total?" para a IA usar a nova tool `summarize-expenses`.

## O que aprendi

- Como o Spring AI conecta um modelo de linguagem à aplicação com `ChatClient`, prompt de sistema e Tool Calling.
- Que a IA deve chamar os **mesmos casos de uso** da API REST: assim a regra de negócio (como a validação) vale para os dois caminhos.
- Que, quando uma tool lança exceção, o Spring AI devolve a mensagem para o modelo, que consegue explicar o erro ao usuário.
- Que o prompt de sistema faz muita diferença (unidade dos valores, tamanho da resposta, não inventar dados).
- Como testar a lógica sem depender da OpenAI, usando um repositório em memória e MockMvc.

## Referências

- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/index.html)
- [ChatClient API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Tools API](https://docs.spring.io/spring-ai/reference/api/tools.html)
- [Audio Transcriptions API](https://docs.spring.io/spring-ai/reference/api/audio/transcriptions.html)
- [Audio Speech API](https://docs.spring.io/spring-ai/reference/api/audio/speech.html)
- Conceitos de arquitetura da trilha: [README principal](../README.md)
