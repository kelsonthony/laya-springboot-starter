# Progress

## T1: Typed HTTP client, complete
Gate: ./mvnw verify, Java 21, 18 tests passed, zero failed or skipped.

| Requirement | Assertion evidence | Outcome |
| --- | --- | --- |
| LAYA-1 request shape | src/test/java/io/github/kelsonthony/laya/LayaClientTests.java:29 content().json(..., STRICT) | Exact state/questions and omitted default model |
| LAYA-1 typed values | src/test/java/io/github/kelsonthony/laya/LayaClientTests.java:40 result.choice("team").choice() | billing, urgency .95, severity 1.75, 123 input tokens |
| LAYA-1 validation | src/test/java/io/github/kelsonthony/laya/QuestionTests.java:27 assertThatThrownBy | Invalid questions rejected |
| LAYA-1 snapshots | src/test/java/io/github/kelsonthony/laya/QuestionTests.java:15 hasSize(1) | Caller changes do not alter criteria; null preserved |
| LAYA-2 response errors | src/test/java/io/github/kelsonthony/laya/LayaClientTests.java:102 isInstanceOf(RestClientException.class) | Invalid body, types, numbers and names rejected |
| LAYA-2 HTTP errors | src/test/java/io/github/kelsonthony/laya/LayaClientTests.java:83 getStatusCode().value() | Exact 401/422/429/500, Retry-After 5, one attempt |
| LAYA-2 extensions | src/test/java/io/github/kelsonthony/laya/LayaClientTests.java:51 routing() | Routing retained, unknown fields accepted |

Every test maps to LAYA-1 or LAYA-2. Tests assert protocol and returned values, not only call counts.

## T2: Auto-configuration, complete
Gate: ./mvnw verify, Java 21, 32 tests passed, zero failed or skipped.

| Requirement | Assertion evidence | Outcome |
| --- | --- | --- |
| LAYA-3 defaults | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:29 hasSingleBean | Starts without credentials/network, localhost:8000, model null |
| LAYA-3 wire defaults | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:39 headerDoesNotExist / jsonPath | No Authorization or model by default |
| LAYA-3 auth precedence | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:54 header | Bearer explicit wins over fallback |
| LAYA-3 fallback | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:70 header | LAYA_API_KEY supplies Bearer fallback |
| LAYA-3 blank key | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:84 headerDoesNotExist | Explicit blank suppresses fallback |
| LAYA-3 invalid config | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:96 hasFailed | Bad root URL and blank model fail startup |
| LAYA-4 backoff | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:110 doesNotHaveBean | Disabled, custom and nonservlet contexts respected |
| LAYA-4 customization | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:39 header | X-Community interceptor retained, JSON protocol verified |

Every context test maps to LAYA-3 or LAYA-4; one mock expectation forbids accidental startup HTTP.

## T2a: Preserve official null routing diagnostics, complete
Local real-inference smoke found routing.workflow and routing.detection legitimately null. Preserve null metadata in an unmodifiable shallow snapshot. Gate: starter install and example verify, 33 + 3 tests passed. LAYA-2 regression assertion: src/test/java/io/github/kelsonthony/laya/LayaClientTests.java:113 containsEntry("workflow", null).

## T3: Community example, docs and CI, complete
Gate: ./mvnw install (33 tests), ./mvnw -f examples/support-triage/pom.xml clean verify (3 tests), Python smoke with the official server (PASS). Zero test failures or skips.

| Requirement | Evidence | Outcome |
| --- | --- | --- |
| LAYA-5 MVC result | examples/support-triage/src/test/java/io/github/kelsonthony/laya/example/TriageApplicationTests.java:39 jsonPath("$.department").value("financeiro") | All five fields asserted, upstream request state/types checked |
| LAYA-5 invalid ticket | examples/support-triage/src/test/java/io/github/kelsonthony/laya/example/TriageApplicationTests.java:47 status().isBadRequest() | Missing or empty message returns 400 before HTTP |
| LAYA-5 real inference | scripts/smoke.py:47 triage["department"] == "financeiro" | Official model and packaged Java application return financeiro; values within protocol ranges |
| LAYA-6 docs/build | README.md:1; README.en.md:1; LICENSE:1; .github/workflows/build.yml:1 | Two README files, credits, wrapper, Apache license and nine CI combinations |

Tests reverse-map to LAYA-5; documentation review maps to LAYA-6. The source-level tests and real packaged-JAR smoke are separate evidence.

## T4: Custom converter evidence, complete
Gate: starter clean install and example clean verify, 36 + 3 tests passed, zero failures/skips. LAYA-4 assertion src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:53 returns noul .8 from application/x-laya-test via the retained custom converter.

## T5: Official request semantics, complete
Upstream serve.py rejects null state (400) and null score levels (422). LayaRequest now rejects both before HTTP. LAYA-1 assertions src/test/java/io/github/kelsonthony/laya/QuestionTests.java:26 and :31 assert exact exceptions; unchanged factory tests prove only shallow snapshot preservation, not valid HTTP evaluation. Gate: starter clean install and example clean verify, 36 + 3 tests passed, zero failures/skips. README null-description guidance narrowed to choice/noul.

## T6: Supported GitHub build actions, complete
First remote run 37679661739 passed all nine Java/Boot combinations. Its annotations reported deprecated Node20-based actions. Replaced checkout/setup-java with verified current official releases v7.0.1/v6.0.1. Gate: initial matrix PASS and official action manifest inspection; final remote matrix will verify this configuration.
