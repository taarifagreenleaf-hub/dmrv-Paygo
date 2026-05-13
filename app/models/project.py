import uuid
from datetime import datetime, timezone
from sqlalchemy import String, Text, DateTime, ForeignKey, Enum as SAEnum, Float
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
import enum
from app.database import Base


class ProjectType(str, enum.Enum):
    REFORESTATION = "reforestation"
    SOIL_CARBON = "soil_carbon"
    RENEWABLE_ENERGY = "renewable_energy"
    METHANE_CAPTURE = "methane_capture"
    BLUE_CARBON = "blue_carbon"
    COOKSTOVES = "cookstoves"
    OTHER = "other"


class ProjectStatus(str, enum.Enum):
    DRAFT = "draft"
    ACTIVE = "active"
    UNDER_VERIFICATION = "under_verification"
    VERIFIED = "verified"
    ARCHIVED = "archived"


class Project(Base):
    __tablename__ = "projects"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    owner_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    project_type: Mapped[ProjectType] = mapped_column(SAEnum(ProjectType), default=ProjectType.OTHER)
    status: Mapped[ProjectStatus] = mapped_column(SAEnum(ProjectStatus), default=ProjectStatus.DRAFT)
    country: Mapped[str | None] = mapped_column(String(100), nullable=True)
    latitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    longitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    area_hectares: Mapped[float | None] = mapped_column(Float, nullable=True)
    methodology: Mapped[str | None] = mapped_column(String(255), nullable=True)
    standard: Mapped[str | None] = mapped_column(String(100), nullable=True)  # e.g. VCS, Gold Standard
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    owner: Mapped["User"] = relationship("User", back_populates="projects")
    measurements: Mapped[list["Measurement"]] = relationship(
        "Measurement", back_populates="project", cascade="all, delete-orphan"
    )
