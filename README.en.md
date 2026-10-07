# Laya Spring Boot Starter

Community integration for [official Laya](https://github.com/NandhaKishorM/laya), inspired by [Dan Vega's Jev starter](https://github.com/danvega/jev-spring-boot-starter). Independently implemented; not an official SDK. Apache-2.0.

Spring Boot 4, Java 17+, Spring MVC, RestClient and Jackson 3. The starter calls a self-hosted Python `laya-serve` server over `POST /v1/systemone`; it does not run inference in the JVM. Boot 3 and WebFlux are outside this version's scope.

## Install

`0.1.0-SNAPSHOT` is **not on Maven Central**. Clone this repository and run `./mvnw clean install`, then add:

```xml
<dependency>
  <groupId>io.github.kelsonthony</groupId>
  <artifactId>laya-springboot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Your application provides `spring-boot-starter-webmvc`. Start Laya in a separate Python 3.10+ virtual environment:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install "laya[serve]"
LAYA_HOST=127.0.0.1 LAYA_DEVICE=cpu LAYA_PRELOAD=0 laya-serve
```

The first inference downloads and loads its checkpoint. Default server URL: `http://localhost:8000`. Authentication is optional; set the same `LAYA_API_KEY` in the server and Java application when using it.

Inject `io.github.kelsonthony.laya.LayaClient`:

```java
var result = laya.evaluate("I was charged twice", Map.of(
    "team", Question.choice("Which team?", "billing", "support"),
    "urgent", Question.noul("Does this need urgent attention?"),
    "severity", Question.score("Severity?", "Minor", "Moderate", "Blocking")));
String team = result.choice("team").choice();
double urgency = result.noul("urgent").noul();
double severity = result.score("severity").score();
```

Import `io.github.kelsonthony.laya.Question` and `java.util.Map`. `choice` returns a label and probabilities; `noul` returns probability of yes; `score` is a weighted zero-based level index. `confidence()` for choice/score is Laya's entropy-based measure; `answerConfidence()` exposes the optional calibrated probability. The application chooses its thresholds.

## Configure

| Property | Default |
| --- | --- |
| `laya.enabled` | true |
| `laya.base-url` | http://localhost:8000 |
| `laya.api-key` | absent, fallback LAYA_API_KEY |
| `laya.model` | absent, server auto-routing |

An explicit blank key disables authentication; a blank model is invalid. Use `english`, `multilingual` or `typed-decisions` for explicit routing. A per-call `new LayaRequest(state, questions, "multilingual")` overrides the configured model.

```yaml
spring:
  http:
    clients:
      connect-timeout: 5s
      read-timeout: 120s
  threads:
    virtual:
      enabled: true
```

Virtual threads require Java 21+. The starter retains Boot's HTTP builder configuration, performs no startup HTTP calls and backs off for a custom `LayaClient`, disabled integration or nonservlet application. Calls are synchronous and never automatically retried. HTTP errors preserve status, body and `Retry-After`; malformed responses raise `RestClientException`; missing names or wrong named accessors raise `IllegalArgumentException`.

## Example and verification

```bash
./mvnw clean install
./mvnw -f examples/support-triage/pom.xml clean verify
./mvnw -f examples/support-triage/pom.xml spring-boot:run
python3 scripts/smoke.py
```

Start the official Laya server separately before running the smoke. The example accepts `POST /triage` with `{"message":"Fui cobrado duas vezes e preciso de um reembolso."}` and returns department, confidence, urgency, severity and model. Empty/missing messages return 400. Maven tests use deterministic HTTP mocks; the optional smoke runs actual inference. CI covers Java 17/21/25 and Boot 4.0.0/4.0.8/4.1.1.

Read the [Portuguese README](README.md) for the full API, attribution, exact upstream commits and configuration guidance. See [CONTRIBUTING.md](CONTRIBUTING.md), [LICENSE](LICENSE), the [official Laya Java project](https://github.com/NandhaKishorM/laya/tree/main/laya-java) and the [official model](https://huggingface.co/convaiinnovations/laya).
