from datetime import datetime, timezone


def _reg(client, email):
    client.post("/api/v1/auth/register", json={"email": email, "full_name": "U", "password": "pw"})
    token = client.post("/api/v1/auth/login", json={"email": email, "password": "pw"}).json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


def _make_admin(db, email):
    from app.models.user import User
    user = db.query(User).filter(User.email == email).first()
    user.is_admin = True
    db.commit()


def _project(client, headers):
    return client.post("/api/v1/projects/", json={"name": "Test Project"}, headers=headers).json()["id"]


MEASUREMENT_PAYLOAD = {
    "measurement_type": "carbon_sequestration",
    "value": 12.5,
    "unit": "tCO2e",
    "measurement_date": "2024-06-01T08:00:00Z",
    "notes": "sensor reading",
    "metadata": {"device_id": "DEV-001"},
}


def test_create_measurement(client):
    h = _reg(client, "meas_create@example.com")
    pid = _project(client, h)

    resp = client.post(f"/api/v1/projects/{pid}/measurements/", json=MEASUREMENT_PAYLOAD, headers=h)
    assert resp.status_code == 201
    data = resp.json()
    assert data["value"] == 12.5
    assert data["unit"] == "tCO2e"
    assert data["verification_status"] == "pending"


def test_list_measurements(client):
    h = _reg(client, "meas_list@example.com")
    pid = _project(client, h)

    client.post(f"/api/v1/projects/{pid}/measurements/", json=MEASUREMENT_PAYLOAD, headers=h)
    client.post(f"/api/v1/projects/{pid}/measurements/", json={**MEASUREMENT_PAYLOAD, "value": 7.0}, headers=h)

    resp = client.get(f"/api/v1/projects/{pid}/measurements/", headers=h)
    assert resp.status_code == 200
    assert len(resp.json()) == 2


def test_get_single_measurement(client):
    h = _reg(client, "meas_get@example.com")
    pid = _project(client, h)

    mid = client.post(f"/api/v1/projects/{pid}/measurements/", json=MEASUREMENT_PAYLOAD, headers=h).json()["id"]

    resp = client.get(f"/api/v1/projects/{pid}/measurements/{mid}", headers=h)
    assert resp.status_code == 200
    assert resp.json()["id"] == mid


def test_update_measurement(client):
    h = _reg(client, "meas_update@example.com")
    pid = _project(client, h)

    mid = client.post(f"/api/v1/projects/{pid}/measurements/", json=MEASUREMENT_PAYLOAD, headers=h).json()["id"]

    resp = client.patch(
        f"/api/v1/projects/{pid}/measurements/{mid}",
        json={"notes": "updated note", "value": 99.9},
        headers=h,
    )
    assert resp.status_code == 200
    assert resp.json()["notes"] == "updated note"
    assert resp.json()["value"] == 99.9


def test_billing_free_tier(client):
    """First 50 measurements have zero cost; usage summary reflects them."""
    h = _reg(client, "meas_billing@example.com")
    pid = _project(client, h)

    client.post(f"/api/v1/projects/{pid}/measurements/", json=MEASUREMENT_PAYLOAD, headers=h)

    summary = client.get("/api/v1/billing/usage/summary", headers=h).json()
    assert summary["total_measurements"] == 1
    assert summary["free_tier_remaining"] == 49
    assert summary["current_balance_usd"] == 0.0


def test_verify_measurement_admin_only(client, db):
    from app.models.user import User
    owner_email = "meas_owner@example.com"
    owner = _reg(client, owner_email)
    admin_email = "meas_admin@example.com"
    admin_h = _reg(client, admin_email)
    _make_admin(db, admin_email)

    # give the owner enough credit to cover the $0.50 verification fee
    owner_user = db.query(User).filter(User.email == owner_email).first()
    owner_user.credit_balance = 5.0
    db.commit()

    pid = _project(client, owner)
    mid = client.post(f"/api/v1/projects/{pid}/measurements/", json=MEASUREMENT_PAYLOAD, headers=owner).json()["id"]

    # non-admin cannot verify
    resp = client.post(f"/api/v1/projects/{pid}/measurements/{mid}/verify",
                       json={"status": "verified"}, headers=owner)
    assert resp.status_code == 403

    # admin can verify
    resp = client.post(f"/api/v1/projects/{pid}/measurements/{mid}/verify",
                       json={"status": "verified", "notes": "looks good"}, headers=admin_h)
    assert resp.status_code == 200
    assert resp.json()["verification_status"] == "verified"


def test_measurement_404_wrong_project(client):
    h = _reg(client, "meas_404@example.com")
    pid = _project(client, h)
    mid = client.post(f"/api/v1/projects/{pid}/measurements/", json=MEASUREMENT_PAYLOAD, headers=h).json()["id"]

    other_pid = _project(client, h)
    resp = client.get(f"/api/v1/projects/{other_pid}/measurements/{mid}", headers=h)
    assert resp.status_code == 404
