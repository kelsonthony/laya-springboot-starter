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
