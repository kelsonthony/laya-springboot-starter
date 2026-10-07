#!/usr/bin/env python3
"""Run against the official laya-serve and the running Java example, never a mock."""
import argparse
import json
import math
import os
from urllib.error import HTTPError
from urllib.request import Request, urlopen


def send(url, body, key=None, timeout=600):
    headers = {"Content-Type": "application/json"}
    if key:
        headers["Authorization"] = "Bearer " + key
    request = Request(url, data=json.dumps(body).encode(), headers=headers)
    with urlopen(request, timeout=timeout) as response:
        return json.load(response)


def probability(value):
    assert isinstance(value, (int, float)) and not isinstance(value, bool)
    assert math.isfinite(value) and 0 <= value <= 1


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--laya-url", default="http://localhost:8000")
    parser.add_argument("--app-url", default="http://127.0.0.1:8080")
    args = parser.parse_args()
    message = "Fui cobrado duas vezes e preciso de um reembolso."
    result = send(args.laya_url.rstrip("/") + "/v1/systemone", {
        "state": {"message": message},
        "questions": {"department": {"type": "choice", "instructions": "Qual equipe deve atender?", "criteria": {
            "financeiro": "Cobranças, pagamentos e reembolsos", "suporte": "Problemas técnicos"}}},
        "model": "multilingual"
    }, os.environ.get("LAYA_API_KEY"))
    assert result["answers"]["department"]["type"] == "choice"
    assert result["answers"]["department"]["choice"] == "financeiro", result
    assert result["usage"]["input_tokens"] > 0
    assert result["usage"]["output_tokens"] == 0
    assert result["model"]
    probability(result["answers"]["department"]["confidence"])

    triage_url = args.app_url.rstrip("/") + "/triage"
    triage = send(triage_url, {"message": message})
    assert set(triage) == {"department", "confidence", "urgency", "severity", "model"}, triage
    assert triage["department"] == "financeiro", triage
    probability(triage["confidence"])
    probability(triage["urgency"])
    assert isinstance(triage["severity"], (int, float)) and math.isfinite(triage["severity"])
    assert 0 <= triage["severity"] <= 2
    assert triage["model"]
    try:
        send(triage_url, {"message": " "})
        raise AssertionError("Empty message must return HTTP 400")
    except HTTPError as error:
        assert error.code == 400, error.code
    print(json.dumps({"status": "PASS", "official_response": result, "java_triage": triage}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
