def _register_login(client, email):
    client.post("/api/v1/auth/register", json={"email": email, "full_name": "U", "password": "pw"})
    token = client.post("/api/v1/auth/login", json={"email": email, "password": "pw"}).json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


def test_create_and_list_projects(client):
    headers = _register_login(client, "proj@example.com")
    resp = client.post("/api/v1/projects/", json={
        "name": "Reforestation Kenya",
        "project_type": "reforestation",
        "country": "KE",
        "area_hectares": 500.0,
    }, headers=headers)
    assert resp.status_code == 201
    project_id = resp.json()["id"]

    resp = client.get("/api/v1/projects/", headers=headers)
    assert resp.status_code == 200
    assert any(p["id"] == project_id for p in resp.json())


def test_update_project(client):
    headers = _register_login(client, "upd@example.com")
    pid = client.post("/api/v1/projects/", json={"name": "Old Name"}, headers=headers).json()["id"]

    resp = client.patch(f"/api/v1/projects/{pid}", json={"name": "New Name", "status": "active"}, headers=headers)
    assert resp.status_code == 200
    assert resp.json()["name"] == "New Name"
    assert resp.json()["status"] == "active"


def test_delete_project(client):
    headers = _register_login(client, "del@example.com")
    pid = client.post("/api/v1/projects/", json={"name": "To Delete"}, headers=headers).json()["id"]

    resp = client.delete(f"/api/v1/projects/{pid}", headers=headers)
    assert resp.status_code == 204

    resp = client.get(f"/api/v1/projects/{pid}", headers=headers)
    assert resp.status_code == 404
