import uuid
from datetime import datetime
from pydantic import BaseModel, ConfigDict
from app.models.project import ProjectType, ProjectStatus


class ProjectCreate(BaseModel):
    name: str
    description: str | None = None
    project_type: ProjectType = ProjectType.OTHER
    country: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    area_hectares: float | None = None
    methodology: str | None = None
    standard: str | None = None


class ProjectUpdate(BaseModel):
    name: str | None = None
    description: str | None = None
    project_type: ProjectType | None = None
    status: ProjectStatus | None = None
    country: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    area_hectares: float | None = None
    methodology: str | None = None
    standard: str | None = None


class ProjectOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    owner_id: uuid.UUID
    name: str
    description: str | None
    project_type: ProjectType
    status: ProjectStatus
    country: str | None
    latitude: float | None
    longitude: float | None
    area_hectares: float | None
    methodology: str | None
    standard: str | None
    created_at: datetime
    updated_at: datetime
