# Laya Spring Boot Starter

## Problem Statement
Spring MVC applications need a small typed HTTP integration with the official self-hosted Laya server, inspired by Dan Vega's Jev starter.

## Out of Scope
Embedding Python/ONNX in the JVM, hosted Jev credentials, publishing to Maven Central, automatic retries, batch inference and advanced per-request controls.

## Assumptions & Open Questions
| Assumption | Chosen default | Rationale |
| --- | --- | --- |
| Runtime | Spring Boot 4, Java 17+ | Matches the reference starter |
| Endpoint | http://localhost:8000 | Official laya-serve local address |
| Authentication | Optional laya.api-key / LAYA_API_KEY | Official server permits keyless local access |
| Model | Omit by default | Preserve official automatic routing |
| Attribution | Original implementation, Apache-2.0 | Jev repository has no license; do not copy its code |

Open questions: none. User authorized local creation, tests and public GitHub publication.

## User Stories
As a Spring developer I can inject a typed LayaClient and evaluate named decisions against my own Laya server.

Acceptance Criteria:
1. When evaluate receives valid named choice, score and noul questions, the client SHALL POST state, questions and optional model to /v1/systemone and return their exact typed values, model and usage. Null choice/noul criteria descriptions SHALL be preserved; invalid/empty questions, null score levels and null state SHALL be rejected before HTTP.
2. When the server sends malformed answers, missing numbers, wrong names or wrong types, the client SHALL raise RestClientException; unknown extension fields SHALL be tolerated and routing metadata SHALL remain accessible. HTTP 401, 422, 429 and 500 SHALL preserve their status and Retry-After without retrying.
3. When a servlet application starts, the starter SHALL provide one LayaClient without HTTP calls, use localhost:8000 and omit model and Authorization by default. A configured key SHALL produce Bearer authentication; explicit blank key SHALL disable it. Explicit key SHALL override LAYA_API_KEY. Invalid URLs or blank model SHALL fail startup.
4. When laya.enabled=false, a custom LayaClient exists, or the context is not servlet, the starter SHALL back off. The starter SHALL retain the Boot builder's interceptors and message converters.
5. When a user builds the example and posts a ticket to /triage, the example SHALL return department, confidence, urgency, severity and actual model from Laya. A documented local smoke SHALL distinguish official-model inference from mocked HTTP tests.
6. The repository SHALL contain an English primary README, Portuguese translation, English quickstart, Apache-2.0 license, contribution guide, Maven wrapper and GitHub CI, credit both upstream projects, and clearly state local SNAPSHOT installation rather than Maven Central availability.

## Requirement Traceability
| ID | Requirement | Status |
| --- | --- | --- |
| LAYA-1 | Typed evaluation and input validation | verified |
| LAYA-2 | Response validation and HTTP failures | verified |
| LAYA-3 | Defaults, auth and startup | verified |
| LAYA-4 | Backoff and Boot customization | verified |
| LAYA-5 | Working triage example | verified |
| LAYA-6 | Community documentation and build | verified |

## Execution Plan
1. Typed HTTP client: pom.xml, Maven wrapper, src/main/java/io/github/kelsonthony/laya/{Question,Answer,LayaRequest,LayaResponse,LayaClient}.java and client tests. Gate: ./mvnw verify. Commit: feat(client): adiciona cliente tipado para a API oficial do Laya.
2. Auto-configuration: autoconfigure classes, imports metadata and context tests. Gate: ./mvnw verify. Commit: feat(autoconfigure): integra o cliente ao Spring Boot.
3. Community example and docs: examples/support-triage, README files, LICENSE, CONTRIBUTING.md, CI and smoke script. Gate: ./mvnw install and example verify plus local smoke. Commit: feat(example): entrega exemplo de triagem e documentação comunitária.
