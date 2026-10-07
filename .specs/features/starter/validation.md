# Validation: Laya Spring Boot Starter — PASS

**Result**: PASS
**Date**: 2026-10-07
**Spec**: `.specs/features/starter/spec.md`
**Diff range**: new repository, inclusive commits `4b6f77d..e3e051a` (initial root commit included).
**Verifier**: fresh independent sub-agent, author != verifier. Production sources and tests reviewed read-only; only this report written in the real repository.

All six acceptance criteria match their defined outcomes. Two findings from the initial review were corrected and re-verified: missing converter-retention evidence and acceptance of null score levels that the official server rejects. No material gaps remain.

## Task completion

The simple-tier plan is embedded in spec.md rather than a separate tasks.md. Its three execution steps and their gates are complete; progress.md records implementation evidence.

| Task | Status | Evidence |
| --- | --- | --- |
| Typed HTTP client | Complete | Request/answer DTOs, validation, 17 client + 4 question tests |
| Boot auto-configuration | Complete | Imports metadata and 15 context tests |
| Example, docs, CI | Complete | Executable example build, 3 MVC tests, independently rerun official smoke |

## Spec-anchored acceptance criteria

Paths below are relative to the repository. Assertion expressions are actual test expressions, not inferred coverage.

| AC | Spec-defined outcome | File:line + assertion evidence | Result |
| --- | --- | --- | --- |
| 1 | Exact POST /v1/systemone, state and named choice/score/noul questions | src/test/java/io/github/kelsonthony/laya/LayaClientTests.java:27 `requestTo("http://localhost:8000/v1/systemone")`; :28 `method(HttpMethod.POST)`; :29 `content().json(..., STRICT)` includes null choice descriptions | PASS |
| 1 | Exact typed values, model and token usage | src/test/java/io/github/kelsonthony/laya/LayaClientTests.java:40 `choice().isEqualTo("billing")`; :44 `noul().isEqualTo(0.95)`; :46 `score().isEqualTo(1.75)`; :48 `model().isEqualTo("convaiinnovations/laya-multilingual")`; :49 `inputTokens().isEqualTo(123L)`; :50 `outputTokens().isZero()` | PASS |
| 1 | Optional per-request model overrides configured default | src/test/java/io/github/kelsonthony/laya/LayaClientTests.java:60 `jsonPath("$.model").value("multilingual")`; :69 `jsonPath("$.model").value("english")` | PASS |
| 1 | Null choice/noul descriptions preserved, invalid questions and null state/score levels rejected before HTTP | src/test/java/io/github/kelsonthony/laya/QuestionTests.java:14 `get("billing").isNull()`; :20 noul criteria `hasSize(2)`; :25 `new LayaRequest(...null score...)` throws `IllegalArgumentException` with exact message; :30 null state throws exact message; :35-43 duplicate/blank/empty/unknown inputs throw | PASS |
| 2 | Malformed answers, missing numbers, wrong names and wrong types raise RestClientException | src/test/java/io/github/kelsonthony/laya/LayaClientTests.java:90-98 parameter set includes invalid JSON, missing noul, out-of-range noul, wrong name/type and missing input_tokens; :101-102 `assertThatThrownBy(...).isInstanceOf(RestClientException.class)` | PASS |
| 2 | Unknown fields accepted; routing and null diagnostics accessible | src/test/java/io/github/kelsonthony/laya/LayaClientTests.java:20-23 fixture has unknown fields; :51 `routing().containsEntry("reason", "language")`; :112 `containsEntry("workflow", null).containsEntry("detection", null)`; :113 mutation throws UnsupportedOperationException | PASS |
| 2 | HTTP 401/422/429/500 and Retry-After preserved, no retry | src/test/java/io/github/kelsonthony/laya/LayaClientTests.java:76 exact four statuses; :83 `getStatusCode().value().isEqualTo(status)`; :84 `getFirst("Retry-After").isEqualTo("5")`; :87 `server.verify()` with one request expectation | PASS |
| 3 | Servlet startup supplies exactly one client, localhost default, model absent, no startup calls | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:28 `hasSingleBean(LayaClient.class)`; :29 base URL `isEqualTo("http://localhost:8000")`; :30 model `isNull()`; :35 mock server has no expectations until context startup completes, so premature HTTP would fail | PASS |
| 3 | Default wire omits Authorization/model | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:38 `headerDoesNotExist("Authorization")`; :39 `jsonPath("$.model").doesNotExist()` | PASS |
| 3 | Explicit key wins; environment fallback works; blank explicit key disables auth | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:64 `header("Authorization", "Bearer explicit")`; :79 `header("Authorization", "Bearer fallback")`; :92 `headerDoesNotExist("Authorization")` | PASS |
| 3 | Invalid URLs and blank model fail startup | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:100 five invalid URLs; :103 `hasFailed()`; :104 root IllegalArgumentException; :111 `hasRootCauseMessage("laya.model must not be blank; omit it for automatic routing")` | PASS |
| 4 | Disabled/custom/nonservlet contexts back off | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:116 `doesNotHaveBean(LayaClient.class)`; :121 `hasSingleBean(...).hasBean("custom")`; :126 nonservlet `doesNotHaveBean(...)` | PASS |
| 4 | Retain builder interceptors and custom message converters | src/test/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfigurationTests.java:38 `header("X-Community", "laya")`; :51 custom `application/x-laya-test` response; :52-53 decoded `noul().isEqualTo(0.8)` proves nondefault converter survived | PASS |
| 5 | Built example /triage returns all five exact values supplied by Laya | examples/support-triage/src/test/java/io/github/kelsonthony/laya/example/TriageApplicationTests.java:39 `status().isOk()` and department financeiro; :40 confidence .9 and urgency .8; :41 severity 1.7 and model laya-rl-agent | PASS |
| 5 | Empty/missing ticket rejected before HTTP | examples/support-triage/src/test/java/io/github/kelsonthony/laya/example/TriageApplicationTests.java:47 and :53 `status().isBadRequest()`; :48 and :54 `server.verify()` with no outbound expectation | PASS |
| 5 | Actual official inference separated from deterministic mocks | scripts/smoke.py:38 official choice `== "financeiro"`; :39 positive input tokens; :46 exact triage keys; :47 department financeiro; :48-51 finite probability/score ranges; :57 empty input `error.code == 400`; docs/verification.md:7 identifies upstream/runtime; :14 explicitly distinguishes mocks from weights | PASS |
| 6 | Portuguese README, English quickstart, license, contribution guide, wrapper, CI and credits | README.md:6 official Laya and :12 Jev credit; :30 explicitly not Maven Central; README.en.md:3 upstream credits and :9 SNAPSHOT install; LICENSE:2 Apache License; CONTRIBUTING.md:10-16 build/protocol guidelines; .github/workflows/build.yml:13-25 Java/Boot matrix and both build gates; mvnw:1 and .mvn/wrapper/maven-wrapper.properties:1 wrapper present | PASS |

No spec-precision gaps. Static documentation criteria use direct artifact inspection; runtime criteria use executable assertions.

## Build-level gates

Independent commands, with `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`:

- `./mvnw verify`: exit 0, BUILD SUCCESS, 36 tests, 0 failures/errors/skips.
- `./mvnw -f examples/support-triage/pom.xml verify`: exit 0, BUILD SUCCESS, 3 tests, 0 failures/errors/skips, executable JAR repackaged.
- `python3 scripts/smoke.py --laya-url http://127.0.0.1:18000 --app-url http://127.0.0.1:18080`: exit 0, PASS; official multilingual model and Java example both returned financeiro. Java confidence .9976, urgency .1767, severity 1.475, model laya-rl-agent.

Smoke used the live official server and the parent's already-running packaged example. The subsequently rebuilt final example has the added request-boundary checks; the valid smoke request exercises the same production decision path. The parent will rerun packaged smoke after restart before publishing.

Gate logs: `/tmp/laya-verifier-final.log`, `/tmp/laya-verifier-example.log`. Original repository test count: 0 (new repository). Final count: 39 across starter/example. No deleted, weakened or skipped assertions. Initial review had 33 + 3; fixes added three tests.

Local execution proves Java 21 / Boot 4.0.8. Java 17/25 and other Boot releases are configured in CI; their successful remote execution is not inferred from local results.

## Discrimination sensor

Final sensor ran in detached `/tmp/laya-verifier-sensor` at e3e051a. Each mutation was discarded before the next. Only `test` goals ran; no mutated JAR was installed into shared Maven storage. Earlier exploratory sensor results at e666e2f were superseded by this stable run.

| Mutation | Source | Focused test command | Outcome |
| --- | --- | --- | --- |
| Remove returned-name guard (`if (false)`) | src/main/java/io/github/kelsonthony/laya/LayaClient.java:34 | `./mvnw -q -Dtest=LayaClientTests test` | Killed, exit 1; 17 tests, one failure. Wrong-name fixture yields NPE instead of required RestClientException, and exact exception-type assertion detects it. |
| Remove Bearer header side effect | src/main/java/io/github/kelsonthony/laya/autoconfigure/LayaAutoConfiguration.java:32 | `./mvnw -q -Dtest=LayaAutoConfigurationTests test` | Killed, exit 1; 15 tests, two failures: explicit/environment auth expected Authorization header but it was null. |
| Remove null score-level request guard (`if (false)`) | src/main/java/io/github/kelsonthony/laya/LayaRequest.java:19 | `./mvnw -q -Dtest=QuestionTests test` | Killed, exit 1; 4 tests, one failure: expected code to raise a throwable. |

**Sensor depth**: lightweight, three behavior mutations. **Result**: 3 injected, 3 killed, 0 survived.

Isolation: real `git status --porcelain` was empty immediately before and after the final sensor; `cmp /tmp/laya-sensor-baseline.txt /tmp/laya-sensor-after.txt` exited 0. Scratch worktree was removed. No stash used. Logs: `/tmp/laya-final-mutant-{names,auth,score}.log`.

## Code quality and edge cases

| Check | Result |
| --- | --- |
| Scoped implementation, no unrelated changes or runtime embedding | PASS |
| No automatic retries or startup model calls | PASS |
| Boot builder cloned; custom client backoff respected | PASS |
| Defensive collection snapshots preserve legal null descriptions | PASS |
| Exact protocol values/statuses targeted by assertions | PASS |
| Domain requirements each mapped; MVC happy, empty and missing cases covered | PASS |
| Each test maps to AC1-5; static artifacts map to AC6 | PASS |
| CONTRIBUTING.md:10-16 build and protocol guidelines followed | PASS |

Response constructors require numeric decision values and nonnegative present usage counts; missing values cannot silently become primitive zero. Choice/score confidence and returned rubric/probabilities are checked. Unknown extension fields are ignored, while routing snapshots retain official null diagnostic fields. Invalid configuration rejects startup, auth logging is redacted, and HTTP failure metadata remains with Spring exceptions.

## Resolved findings and lessons

1. AC1: official serve.py rejects null score levels at lines 682-690 and null state at 648-653. Final LayaRequest rejects both before HTTP, with exact-message tests; docs/spec clarify that nullable descriptions apply to choice/noul. Existing factory snapshot assertions remain intact and no longer imply wire validity.
2. AC4: interceptor evidence alone did not prove custom converters survived. Commit 12807be added a nonstandard-content-type converter test; final gate passes.

Grounded reusable lessons were sent to the orchestrator for project-local recording: verify request validity against the actual upstream handler, and prove custom converter retention with a content type unsupported by default converters. The delegated verifier writes only this report; the orchestrator owns lessons bookkeeping.

## Requirement traceability

LAYA-1 through LAYA-6: independently verified. Spec already marks them verified; no source/spec mutation performed by verifier. No further fix tasks remain. Interactive UAT is unnecessary for this backend library and sample API.

## Summary

**Overall**: Ready. Six of six ACs matched. Both build gates passed with 39 tests and no skips. Three of three mutants killed. Official local inference smoke passed. Public GitHub creation/push and remote CI confirmation remain the orchestrator's delivery steps.

## Delivery check by the author

The final packaged JAR was restarted after all changes; the exact documented smoke command passed again on 2026-10-07. Department financeiro, confidence 1.0, urgency .1767, severity 1.475, model laya-rl-agent. This additional check does not replace the independent review above. Both resolved findings were recorded as project-local candidate lessons through the skill's lessons script.
