import uuid
from datetime import datetime
from pydantic import BaseModel, ConfigDict
from app.models.billing import UsageType, InvoiceStatus


class UsageRecordOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    user_id: uuid.UUID
    measurement_id: uuid.UUID | None
    usage_type: UsageType
    quantity: int
    unit_price: float
    total_amount: float
    invoice_id: uuid.UUID | None
    created_at: datetime


class InvoiceOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    user_id: uuid.UUID
    invoice_number: str
    status: InvoiceStatus
    subtotal: float
    tax: float
    total: float
    currency: str
    period_start: datetime
    period_end: datetime
    due_date: datetime | None
    paid_at: datetime | None
    notes: str | None
    created_at: datetime
    usage_records: list[UsageRecordOut] = []


class CreditLedgerOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    user_id: uuid.UUID
    amount: float
    balance_after: float
    description: str
    payment_id: uuid.UUID | None
    created_at: datetime


class UsageSummary(BaseModel):
    total_measurements: int
    total_verifications: int
    total_reports: int
    total_api_calls: int
    total_cost_usd: float
    current_balance_usd: float
    free_tier_remaining: int
