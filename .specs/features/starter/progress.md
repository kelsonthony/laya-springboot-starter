# Progress

## T1: Typed HTTP client, complete
Gate: ./mvnw verify, Java 21, 18 tests passed, zero failed or skipped.

| Requirement | Assertion evidence | Outcome |
| --- | --- | --- |
| LAYA-1 request shape | LayaClientTests.java:33 content().json(..., STRICT) | Exact state/questions and omitted default model |
| LAYA-1 typed values | LayaClientTests.java:46 result.choice("team").choice() | billing, urgency .95, severity 1.75, 123 input tokens |
| LAYA-1 validation | QuestionTests.java:27 assertThatThrownBy | Invalid questions rejected |
| LAYA-1 snapshots | QuestionTests.java:15 hasSize(1) | Caller changes do not alter criteria; null preserved |
| LAYA-2 response errors | LayaClientTests.java:116 isInstanceOf(RestClientException.class) | Invalid body, types, numbers and names rejected |
| LAYA-2 HTTP errors | LayaClientTests.java:94 getStatusCode().value() | Exact 401/422/429/500, Retry-After 5, one attempt |
| LAYA-2 extensions | LayaClientTests.java:58 routing() | Routing retained, unknown fields accepted |

Every test maps to LAYA-1 or LAYA-2. Tests assert protocol and returned values, not only call counts.
