import uuid
from datetime import datetime
from pydantic import BaseModel, ConfigDict
from app.models.measurement import MeasurementType, VerificationStatus


class MeasurementCreate(BaseModel):
    measurement_type: MeasurementType
    value: float
    unit: str
    measurement_date: datetime
    metadata_: dict | None = None
    notes: str | None = None


class MeasurementUpdate(BaseModel):
    value: float | None = None
    unit: str | None = None
    measurement_date: datetime | None = None
    metadata_: dict | None = None
    notes: str | None = None


class MeasurementVerify(BaseModel):
    status: VerificationStatus
    notes: str | None = None


class MeasurementOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    project_id: uuid.UUID
    measurement_type: MeasurementType
    value: float
    unit: str
    measurement_date: datetime
    verification_status: VerificationStatus
    metadata_: dict | None
    notes: str | None
    verifier_id: uuid.UUID | None
    verified_at: datetime | None
    created_at: datetime
