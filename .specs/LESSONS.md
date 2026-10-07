# LESSONS - auto-maintained by scripts/lessons.py

> Machine-owned. Do NOT hand-edit. Changes are overwritten on the next `lessons.py` write.
> Canonical state lives in `.specs/lessons.json`. Edit lessons only via the script.
> promote_threshold=2 distinct features · window_days=45 · quarantine_threshold=2

## Confirmed (load these at Specify/Design)

Corroborated across multiple features. Safe to apply as guidance.

_none_

## Candidates (under observation - do NOT load as guidance yet)

Seen once or not yet corroborated. Tracked, not trusted.

### L-001 - Valide a semântica dos pedidos no handler oficial do Laya, incluindo estado nulo e níveis nulos de score, antes de aceitar apenas a serialização JSON.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `client` · harmful: 0
- features: starter
- evidence: .specs/features/starter/validation.md:128 (client)
- last seen: 2026-10-07T20:07:30Z

### L-002 - Prove a preservação de conversores personalizados com um tipo de conteúdo que os conversores padrão não suportam.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `autoconfigure` · harmful: 0
- features: starter
- evidence: .specs/features/starter/validation.md:129 (autoconfigure)
- last seen: 2026-10-07T20:07:30Z

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
