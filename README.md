# Laya Spring Boot Starter

[![Build](https://github.com/kelsonthony/laya-springboot-starter/actions/workflows/build.yml/badge.svg)](https://github.com/kelsonthony/laya-springboot-starter/actions/workflows/build.yml)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)

Integre decisões tipadas do **[Laya](https://github.com/NandhaKishorM/laya)** a aplicações Spring MVC. Adicione a dependência, execute seu servidor Laya e injete `LayaClient`.

**Spring Boot 4 · Java 17+ · Spring MVC · RestClient · Jackson 3**

[English quickstart](README.en.md) · [Exemplo de triagem](examples/support-triage) · [Como contribuir](CONTRIBUTING.md)

Projeto independente da comunidade, inspirado no [Jev Spring Boot Starter de Dan Vega](https://github.com/danvega/jev-spring-boot-starter). A implementação deste repositório é própria. Não é um SDK oficial nem tem afiliação com os autores do Laya ou do Jev.

## O que ele faz

O [servidor HTTP oficial do Laya](https://github.com/NandhaKishorM/laya#self-hosting-http-server-jev-compatible) oferece `POST /v1/systemone`, compatível com o protocolo de decisões do Jev. Este starter conecta o Spring Boot a esse servidor. O modelo roda no processo Python do Laya; o starter Java faz chamadas HTTP síncronas e não baixa nem carrega pesos na JVM.

| Pergunta | Uso | Resultado |
| --- | --- | --- |
| `Question.choice(...)` | Escolher uma opção | Rótulo, probabilidades e confiança |
| `Question.score(...)` | Avaliar níveis ordenados | Pontuação ponderada, legenda e probabilidades |
| `Question.noul(...)` | Avaliar uma afirmação sim/não | Probabilidade de sim, de 0 a 1 |

Em uma escala com três níveis, `score` fica entre 0 e 2 e pode ser fracionário. `confidence` em `choice`/`score` representa a medida de entropia do Laya; `answerConfidence()` expõe `answer_confidence`, quando disponível. Sua aplicação escolhe os limiares e valida a qualidade do modelo com seus dados.

## Começar

### 1. Instalar o starter localmente

A versão `0.1.0-SNAPSHOT` **ainda não está publicada no Maven Central**. Instale no seu repositório Maven local:

```bash
git clone https://github.com/kelsonthony/laya-springboot-starter.git
cd laya-springboot-starter
./mvnw clean install
```

No macOS com Java instalado pelo Homebrew, se necessário:

```bash
export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
```

Adicione à sua aplicação Spring Boot 4:

```xml
<dependency>
    <groupId>io.github.kelsonthony</groupId>
    <artifactId>laya-springboot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Sua aplicação fornece `spring-boot-starter-webmvc`. O starter fornece `spring-boot-starter-restclient` e não inicia servidor web por conta própria. Spring Boot 3 e WebFlux não são suportados nesta versão.

### 2. Executar o servidor oficial

Python 3.10+ em um ambiente virtual separado:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install "laya[serve]"
LAYA_HOST=127.0.0.1 LAYA_DEVICE=cpu LAYA_PRELOAD=0 laya-serve
```

O endereço padrão do starter é `http://localhost:8000`. O primeiro pedido baixa e carrega o checkpoint escolhido; pode demorar mais que os seguintes. Configure um timeout adequado ou faça o preload conforme a [documentação oficial](https://github.com/NandhaKishorM/laya). Para GPU, dispositivos e recursos avançados, siga o projeto oficial.

Autenticação é opcional para o servidor local. Se definir `LAYA_API_KEY` no servidor, configure a mesma chave na aplicação Java. Não use a chave hospedada do TypeSafe Jev como se fosse uma credencial do Laya.

### 3. Injetar o cliente

```java
import io.github.kelsonthony.laya.LayaClient;
import io.github.kelsonthony.laya.Question;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class AtendimentoService {
    private final LayaClient laya;

    public AtendimentoService(LayaClient laya) {
        this.laya = laya;
    }

    public String equipe(String mensagem) {
        var resposta = laya.evaluate(Map.of("message", mensagem), Map.of(
            "equipe", Question.choice("Qual equipe deve atender?", Map.of(
                "financeiro", "Pagamentos, cobranças e reembolsos",
                "suporte", "Problemas técnicos e acesso"))));
        return resposta.choice("equipe").choice();
    }
}
```

Não precisa de anotação de habilitação nem de component scanning do pacote do starter. A configuração automática se aplica a aplicações servlet, não chama HTTP no startup e recua se você fornecer seu próprio `LayaClient`.

## Três decisões em uma chamada

```java
var resposta = laya.evaluate("Não consigo receber pagamentos e a loja está parada", Map.of(
    "urgente", Question.noul("O problema exige atenção urgente?"),
    "equipe", Question.choice("Qual equipe deve atender?", "financeiro", "integracoes", "suporte"),
    "gravidade", Question.score("Qual a gravidade?", "Leve", "Moderado", "Operação bloqueada")));

double urgencia = resposta.noul("urgente").noul();
String equipe = resposta.choice("equipe").choice();
double confianca = resposta.choice("equipe").confidence();
double gravidade = resposta.score("gravidade").score();
```

`answers()` contém todas as respostas. Os acessores por nome verificam o tipo e lançam `IllegalArgumentException` para nome ausente ou tipo errado. `model()` informa o modelo retornado pelo servidor; `routing()` contém os diagnósticos de roteamento, quando presentes. `usage()` expõe `inputTokens()` e `outputTokens()`.

As respostas preservam também `answerConfidence()`, `action()`, `abstention()` e `lowConfidence()`, quando enviados pelo Laya. Campos extras desconhecidos são ignorados. Esta versão não oferece parâmetros de abstinência por pedido; os metadados são úteis quando o servidor está configurado para produzi-los.

O estado e as instruções aceitam valores serializáveis em JSON: texto, records, mapas e listas. Descrições de critérios `choice` e `noul` podem ser nulas. O pedido rejeita estado nulo e níveis nulos em `score` antes da chamada HTTP. As coleções são copiadas superficialmente; mantenha os valores aninhados imutáveis durante a chamada.

## Configuração

| Propriedade | Padrão | Função |
| --- | --- | --- |
| `laya.enabled` | `true` | Ativa a configuração automática |
| `laya.base-url` | `http://localhost:8000` | Raiz HTTP; não inclua `/v1/systemone` |
| `laya.api-key` | Ausente; fallback `LAYA_API_KEY` | Bearer token opcional |
| `laya.model` | Ausente | Delega ao roteador do servidor |

```yaml
laya:
  base-url: http://localhost:8000
  # api-key: ${LAYA_API_KEY}
  # model: multilingual
spring:
  http:
    clients:
      connect-timeout: 5s
      read-timeout: 120s
```

Use `LAYA_BASE_URL`, `LAYA_API_KEY`, `LAYA_MODEL` e `LAYA_ENABLED` para configurar pelo ambiente. Uma chave definida explicitamente em `laya.api-key` vence o fallback; uma chave explicitamente vazia desativa a autenticação. Não defina `laya.model` vazio: omita a propriedade para roteamento automático. Os aliases oficiais incluem `english`, `multilingual` e `typed-decisions`; veja a lista atual no Laya.

Um modelo pode ser escolhido só para uma chamada:

```java
import io.github.kelsonthony.laya.LayaRequest;

var resposta = laya.evaluate(new LayaRequest(estado, perguntas, "multilingual"));
```

O starter clona o `RestClient.Builder` do Spring Boot, preservando conversores, interceptadores, transporte e observabilidade. Os timeouts acima valem para os clientes HTTP configurados pelo Boot. Para ajustes compartilhados, use `RestClientCustomizer`; para ajustes exclusivos, forneça um bean `LayaClient` com seu próprio `RestClient`.

Com Java 21+, sua aplicação pode ativar virtual threads:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

As chamadas continuam síncronas na thread do chamador. O starter não altera o threading da aplicação. Consulte [virtual threads no Spring Boot](https://docs.spring.io/spring-boot/reference/features/spring-application.html#features.spring-application.virtual-threads).

## Erros

- Falhas HTTP propagam `RestClientResponseException`, preservando status, corpo e cabeçalhos como `Retry-After`.
- Falhas de conexão propagam `ResourceAccessException`.
- Respostas vazias, malformadas, com nomes/tipos incompatíveis ou números ausentes geram `RestClientException`. Valores ausentes não viram zero.
- Cada avaliação faz uma tentativa; não há retry automático.

O aplicativo decide a recuperação apropriada. Erros de inferência no exemplo não são convertidos em decisões inventadas.

## Exemplo local e testes

```bash
./mvnw clean install
./mvnw -f examples/support-triage/pom.xml clean verify
./mvnw -f examples/support-triage/pom.xml spring-boot:run
```

Com `laya-serve` ativo em outro terminal:

```bash
curl -sS http://127.0.0.1:8080/triage \
  -H 'Content-Type: application/json' \
  -d '{"message":"Fui cobrado duas vezes e preciso de um reembolso."}'
```

O retorno contém `department`, `confidence`, `urgency`, `severity` e `model`. Mensagem vazia ou ausente retorna HTTP 400. As pontuações são produzidas pelo modelo e variam; o exemplo não exige um valor de confiança fixo.

```bash
python3 scripts/smoke.py --laya-url http://localhost:8000 --app-url http://127.0.0.1:8080
```

O smoke valida a API oficial, os três tipos de decisão via aplicação Java e uma mensagem inválida. Não substitua o servidor por um mock ao relatar um teste de inferência real. Os testes Maven usam HTTP simulado para serem determinísticos e não baixam modelos. A CI verifica Java 17/21/25 e Spring Boot 4.0.0/4.0.8/4.1.1, incluindo o build do exemplo.

## Créditos e referências

- **[NandhaKishorM/laya](https://github.com/NandhaKishorM/laya)**: projeto oficial, modelos, servidor e contrato HTTP. Veja também o [modelo no Hugging Face](https://huggingface.co/convaiinnovations/laya) e o [Java oficial](https://github.com/NandhaKishorM/laya/tree/main/laya-java), que possui uma proposta própria de runtime/cliente.
- **[danvega/jev-spring-boot-starter](https://github.com/danvega/jev-spring-boot-starter)**: inspiração para a experiência de integração com Spring MVC e `RestClient`. O código do starter Jev não foi copiado; no momento da referência, o repositório não declarava licença.
- **[Apache Maven Wrapper](https://github.com/apache/maven-wrapper)**: scripts de build sob Apache-2.0, com os avisos originais preservados.

Referências conferidas nos commits Laya [`3cf26cb`](https://github.com/NandhaKishorM/laya/commit/3cf26cbcb18725dbc2d127bb8bb2c4c43243ae63) e Jev [`1f5d3d7`](https://github.com/danvega/jev-spring-boot-starter/commit/1f5d3d7bb7238c7d84703762b06ce72282cebfc9).

## Licença

[Apache License 2.0](LICENSE). Copyright 2026 Kelson Anthony. Contribuições da comunidade são bem-vindas.
