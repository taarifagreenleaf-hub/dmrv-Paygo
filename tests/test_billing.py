import pytest


def _register_login(client, email="billing@example.com", password="pw"):
    client.post("/api/v1/auth/register", json={
        "email": email, "full_name": "Test User", "password": password
    })
    token = client.post("/api/v1/auth/login", json={
        "email": email, "password": password
    }).json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


def test_usage_summary_empty(client):
    headers = _register_login(client, "sum@example.com")
    resp = client.get("/api/v1/billing/usage/summary", headers=headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["total_measurements"] == 0
    assert data["current_balance_usd"] == 0.0
    assert data["free_tier_remaining"] == 50


def test_generate_invoice_empty(client):
    headers = _register_login(client, "inv@example.com")
    resp = client.post("/api/v1/billing/invoices/generate", headers=headers)
    assert resp.status_code == 201
    data = resp.json()
    assert data["total"] == 0.0
    assert data["status"] == "open"


def test_credit_history_empty(client):
    headers = _register_login(client, "cred@example.com")
    resp = client.get("/api/v1/billing/credits", headers=headers)
    assert resp.status_code == 200
    assert resp.json() == []
