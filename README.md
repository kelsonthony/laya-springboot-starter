# Laya Spring Boot Starter

[![Build](https://github.com/kelsonthony/laya-springboot-starter/actions/workflows/build.yml/badge.svg)](https://github.com/kelsonthony/laya-springboot-starter/actions/workflows/build.yml)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)

Bring typed decisions from **[Laya](https://github.com/NandhaKishorM/laya)** to Spring MVC applications. Add the dependency, run your Laya server, and inject `LayaClient`.

**Spring Boot 4 · Java 17+ · Spring MVC · RestClient · Jackson 3**

[Português (Brasil)](README.pt-BR.md) · [Support triage example](examples/support-triage) · [Contributing](CONTRIBUTING.md)

An independent community project inspired by [Dan Vega's Jev Spring Boot Starter](https://github.com/danvega/jev-spring-boot-starter). This repository contains an original implementation. It is not an official SDK and is not affiliated with the authors of Laya or Jev.

## What it does

The [official Laya HTTP server](https://github.com/NandhaKishorM/laya#self-hosting-http-server-jev-compatible) exposes `POST /v1/systemone`, compatible with Jev's decision protocol. This starter connects Spring Boot to that server. Inference runs in Laya's Python process; the Java starter makes synchronous HTTP calls and does not download or load model weights into the JVM.

| Question | Purpose | Result |
| --- | --- | --- |
| `Question.choice(...)` | Select a named option | Label, probabilities, and confidence |
| `Question.score(...)` | Evaluate ordered levels | Weighted score, legend, and probabilities |
| `Question.noul(...)` | Evaluate a yes/no statement | Probability of yes, between 0 and 1 |

A three-level `score` ranges from 0 to 2 and can be fractional. For `choice` and `score`, `confidence` is Laya's entropy-based measure; `answerConfidence()` exposes `answer_confidence` when available. Your application chooses its thresholds and evaluates model quality on its own data.

## Getting started

### 1. Install the starter locally

Version `0.1.0-SNAPSHOT` is **not yet published to Maven Central**. Install it into your local Maven repository:

```bash
git clone https://github.com/kelsonthony/laya-springboot-starter.git
cd laya-springboot-starter
./mvnw clean install
```

On macOS with a Homebrew-installed JDK, set `JAVA_HOME` if needed:

```bash
export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
```

Add the dependency to your Spring Boot 4 application:

```xml
<dependency>
    <groupId>io.github.kelsonthony</groupId>
    <artifactId>laya-springboot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Your application provides `spring-boot-starter-webmvc`. The starter provides `spring-boot-starter-restclient` and does not start a web server by itself. Spring Boot 3 and WebFlux are outside this version's scope.

### 2. Run the official server

Use Python 3.10+ in a separate virtual environment:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install "laya[serve]"
LAYA_HOST=127.0.0.1 LAYA_DEVICE=cpu LAYA_PRELOAD=0 laya-serve
```

The starter defaults to `http://localhost:8000`. The first request downloads and loads the selected checkpoint and can take longer than subsequent requests. Configure an appropriate timeout or preload the model as described in the [official documentation](https://github.com/NandhaKishorM/laya). Follow the official project for GPU, device, and advanced runtime settings.

Authentication is optional for the local server. If you set `LAYA_API_KEY` on the server, configure the same key in your Java application. A hosted TypeSafe Jev API key is not a Laya server credential.

### 3. Inject the client

```java
import io.github.kelsonthony.laya.LayaClient;
import io.github.kelsonthony.laya.Question;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class SupportService {
    private final LayaClient laya;

    public SupportService(LayaClient laya) {
        this.laya = laya;
    }

    public String department(String message) {
        var response = laya.evaluate(Map.of("message", message), Map.of(
            "department", Question.choice("Which team should handle this?", Map.of(
                "billing", "Payments, charges, and refunds",
                "support", "Technical issues and account access"))));
        return response.choice("department").choice();
    }
}
```

No enable annotation or starter package component scanning is required. Auto-configuration applies to servlet applications, performs no HTTP calls during startup, and backs off when you provide your own `LayaClient`.

## Three decisions in one request

```java
var response = laya.evaluate("Payments are failing and my store cannot accept orders", Map.of(
    "urgent", Question.noul("Does this issue need urgent attention?"),
    "team", Question.choice("Which team should handle this?", "billing", "integrations", "support"),
    "severity", Question.score("How severe is this issue?", "Minor", "Moderate", "Blocking")));

double urgency = response.noul("urgent").noul();
String team = response.choice("team").choice();
double confidence = response.choice("team").confidence();
double severity = response.score("severity").score();
```

`answers()` contains all named answers. Named accessors check the answer type and throw `IllegalArgumentException` for a missing name or incorrect type. `model()` reports the model returned by the server; `routing()` contains routing diagnostics when available. `usage()` exposes `inputTokens()` and `outputTokens()`.

Answers also retain `answerConfidence()`, `action()`, `abstention()`, and `lowConfidence()` when supplied by Laya. Unknown additional fields are ignored. This version does not expose per-request abstention settings; these metadata fields are useful when the server is configured to produce them.

State and instructions accept JSON-serializable values, including text, records, maps, and lists. Choice/noul descriptions may be null. Requests reject null state and null score levels before HTTP. Collections are shallow snapshots; keep nested caller-owned values immutable while a request is in use.

## Configuration

| Property | Default | Purpose |
| --- | --- | --- |
| `laya.enabled` | `true` | Enable auto-configuration |
| `laya.base-url` | `http://localhost:8000` | HTTP root; omit `/v1/systemone` |
| `laya.api-key` | Absent; `LAYA_API_KEY` fallback | Optional bearer token |
| `laya.model` | Absent | Delegate checkpoint selection to the server |

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

Use `LAYA_BASE_URL`, `LAYA_API_KEY`, `LAYA_MODEL`, and `LAYA_ENABLED` for environment-based configuration. An explicitly configured `laya.api-key` wins over the fallback; an explicitly blank key disables authentication. Do not set an empty `laya.model`: omit the property for automatic routing. Official model aliases include `english`, `multilingual`, and `typed-decisions`; consult Laya for its current model list.

Override the model for a single request:

```java
import io.github.kelsonthony.laya.LayaRequest;

var response = laya.evaluate(new LayaRequest(state, questions, "multilingual"));
```

The starter clones Spring Boot's `RestClient.Builder`, retaining message converters, interceptors, HTTP transport, and observation configuration. The timeout properties above apply to Boot-configured HTTP clients application-wide. Use `RestClientCustomizer` for shared settings, or provide a `LayaClient` bean with your own `RestClient` for client-specific settings.

On Java 21+, your application can enable virtual threads:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Calls remain synchronous on the caller's thread. The starter does not change application-wide threading settings. See [Spring Boot virtual threads](https://docs.spring.io/spring-boot/reference/features/spring-application.html#features.spring-application.virtual-threads).

## Errors

- HTTP failures propagate `RestClientResponseException`, preserving status, body, and headers such as `Retry-After`.
- Connection failures propagate `ResourceAccessException`.
- Empty or malformed responses, mismatched names/types, and missing required numbers raise `RestClientException`. Missing values never silently become zero.
- Each evaluation makes one attempt; there is no automatic retry policy.

Your application decides how to recover. The example does not convert inference errors into fabricated decisions.

## Local example and tests

```bash
./mvnw clean install
./mvnw -f examples/support-triage/pom.xml clean verify
./mvnw -f examples/support-triage/pom.xml spring-boot:run
```

With `laya-serve` running in another terminal:

```bash
curl -sS http://127.0.0.1:8080/triage \
  -H 'Content-Type: application/json' \
  -d '{"message":"Fui cobrado duas vezes e preciso de um reembolso."}'
```

The sample ticket is Portuguese and means "I was charged twice and need a refund." The example's questions and department labels are also Portuguese to demonstrate multilingual inference. The response contains `department`, `confidence`, `urgency`, `severity`, and `model`; this ticket is expected to select `financeiro` (billing). Empty or missing messages return HTTP 400. Scores come from the model and can vary; the example does not require a fixed confidence value.

```bash
python3 scripts/smoke.py --laya-url http://localhost:8000 --app-url http://127.0.0.1:8080
```

The smoke checks the official API, all three decision types through the Java application, and an invalid ticket. Use the real server when reporting an inference test. Maven tests use deterministic HTTP mocks and do not download models. CI verifies Java 17/21/25 and Spring Boot 4.0.0/4.0.8/4.1.1, including the example build. See [local verification evidence](docs/verification.md).

## Credits and references

- **[NandhaKishorM/laya](https://github.com/NandhaKishorM/laya)**: the official project, models, server, and HTTP contract. See the [Hugging Face model](https://huggingface.co/convaiinnovations/laya) and the [official Java project](https://github.com/NandhaKishorM/laya/tree/main/laya-java), which provides its own runtime/client approach.
- **[danvega/jev-spring-boot-starter](https://github.com/danvega/jev-spring-boot-starter)**: inspiration for the Spring MVC and `RestClient` integration experience. No Jev starter code was copied; the repository did not declare a license at the referenced revision.
- **[Apache Maven Wrapper](https://github.com/apache/maven-wrapper)**: build scripts licensed under Apache-2.0, with their original notices retained.

References checked at Laya commit [`3cf26cb`](https://github.com/NandhaKishorM/laya/commit/3cf26cbcb18725dbc2d127bb8bb2c4c43243ae63) and Jev commit [`1f5d3d7`](https://github.com/danvega/jev-spring-boot-starter/commit/1f5d3d7bb7238c7d84703762b06ce72282cebfc9).

## License

[Apache License 2.0](LICENSE). Copyright 2026 Kelson Anthony. Community contributions are welcome.
