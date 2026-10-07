# Local verification

PASS on 2026-10-07, macOS ARM64, OpenJDK 21.0.12.1, Spring Boot 4.0.8.

- Starter: 36 tests passed, zero failures/errors/skips.
- MVC example: 3 tests passed, zero failures/errors/skips; executable JAR built.
- Official Laya source at 3cf26cbcb18725dbc2d127bb8bb2c4c43243ae63, version 0.4.0, Python 3.12, torch 2.14.1, CPU, four inference threads.
- Real multilingual checkpoint downloaded from the official model repository.
- Official laya-serve at 127.0.0.1:18000 and packaged Java app at 127.0.0.1:18080.
- `python3 scripts/smoke.py --laya-url http://127.0.0.1:18000 --app-url http://127.0.0.1:18080`: PASS.

See [local-smoke.json](local-smoke.json) for the synthetic ticket and actual results. Java triage returned department `financeiro`, confidence `0.9976`, urgency `0.1767`, severity `1.475` and model `laya-rl-agent`. These measured scores are not promised for other inputs or future checkpoints.

Maven tests use deterministic HTTP mocks and do not prove model quality. The optional smoke used the actual official server and inference weights. Java 17/25 and other Boot versions are covered by the GitHub Actions matrix, separately from this local run.
