import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.api.deps import get_current_user, get_admin_user
from app.core.exceptions import NotFound, Forbidden
from app.models.measurement import Measurement, VerificationStatus
from app.models.project import Project
from app.models.user import User
from app.schemas.measurement import MeasurementCreate, MeasurementUpdate, MeasurementVerify, MeasurementOut
from app.services.billing import BillingService

router = APIRouter(prefix="/projects/{project_id}/measurements", tags=["measurements"])


def _get_project(project_id: str, db: Session, user: User) -> Project:
    project = db.get(Project, uuid.UUID(project_id))
    if not project:
        raise NotFound("Project not found")
    if project.owner_id != user.id and not user.is_admin:
        raise Forbidden()
    return project


@router.post("/", response_model=MeasurementOut, status_code=201)
def create_measurement(
    project_id: str,
    payload: MeasurementCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    project = _get_project(project_id, db, current_user)
    measurement = Measurement(
        project_id=project.id,
        measurement_type=payload.measurement_type,
        value=payload.value,
        unit=payload.unit,
        measurement_date=payload.measurement_date,
        metadata_=payload.metadata_,
        notes=payload.notes,
    )
    db.add(measurement)
    db.flush()

    billing = BillingService(db)
    billing.charge_for_measurement(current_user, measurement)

    db.commit()
    db.refresh(measurement)
    return measurement


@router.get("/", response_model=list[MeasurementOut])
def list_measurements(
    project_id: str,
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    _get_project(project_id, db, current_user)
    return (
        db.query(Measurement)
        .filter(Measurement.project_id == uuid.UUID(project_id))
        .offset(skip)
        .limit(limit)
        .all()
    )


@router.get("/{measurement_id}", response_model=MeasurementOut)
def get_measurement(
    project_id: str,
    measurement_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    _get_project(project_id, db, current_user)
    m = db.get(Measurement, uuid.UUID(measurement_id))
    if not m or m.project_id != uuid.UUID(project_id):
        raise NotFound("Measurement not found")
    return m


@router.patch("/{measurement_id}", response_model=MeasurementOut)
def update_measurement(
    project_id: str,
    measurement_id: str,
    payload: MeasurementUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    _get_project(project_id, db, current_user)
    m = db.get(Measurement, uuid.UUID(measurement_id))
    if not m or m.project_id != uuid.UUID(project_id):
        raise NotFound("Measurement not found")
    for field, value in payload.model_dump(exclude_none=True).items():
        setattr(m, field, value)
    db.commit()
    db.refresh(m)
    return m


@router.post("/{measurement_id}/verify", response_model=MeasurementOut)
def verify_measurement(
    project_id: str,
    measurement_id: str,
    payload: MeasurementVerify,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_admin_user),
):
    m = db.get(Measurement, uuid.UUID(measurement_id))
    if not m or m.project_id != uuid.UUID(project_id):
        raise NotFound("Measurement not found")
    m.verification_status = payload.status
    m.verifier_id = current_user.id
    m.verified_at = datetime.now(timezone.utc)
    if payload.notes:
        m.notes = payload.notes

    if payload.status == VerificationStatus.VERIFIED:
        billing = BillingService(db)
        project = db.get(Project, m.project_id)
        billing.charge_for_verification(project.owner, m)

    db.commit()
    db.refresh(m)
    return m
