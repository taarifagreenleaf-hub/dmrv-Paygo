def test_register_and_login(client):
    resp = client.post("/api/v1/auth/register", json={
        "email": "alice@example.com",
        "full_name": "Alice K",
        "password": "secret123",
    })
    assert resp.status_code == 201
    data = resp.json()
    assert data["email"] == "alice@example.com"
    assert data["credit_balance"] == 0.0

    resp = client.post("/api/v1/auth/login", json={
        "email": "alice@example.com",
        "password": "secret123",
    })
    assert resp.status_code == 200
    assert "access_token" in resp.json()


def test_duplicate_email(client):
    payload = {"email": "bob@example.com", "full_name": "Bob", "password": "pw"}
    client.post("/api/v1/auth/register", json=payload)
    resp = client.post("/api/v1/auth/register", json=payload)
    assert resp.status_code == 400


def test_wrong_password(client):
    client.post("/api/v1/auth/register", json={
        "email": "carol@example.com", "full_name": "Carol", "password": "right"
    })
    resp = client.post("/api/v1/auth/login", json={
        "email": "carol@example.com", "password": "wrong"
    })
    assert resp.status_code == 401


def test_get_me(client):
    client.post("/api/v1/auth/register", json={
        "email": "dave@example.com", "full_name": "Dave", "password": "pw"
    })
    token = client.post("/api/v1/auth/login", json={
        "email": "dave@example.com", "password": "pw"
    }).json()["access_token"]

    resp = client.get("/api/v1/users/me", headers={"Authorization": f"Bearer {token}"})
    assert resp.status_code == 200
    assert resp.json()["email"] == "dave@example.com"
