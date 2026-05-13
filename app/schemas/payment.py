import uuid
from datetime import datetime
from pydantic import BaseModel, ConfigDict
from app.models.payment import PaymentProvider, PaymentStatus


class MpesaPaymentRequest(BaseModel):
    amount: float
    phone_number: str  # Format: 254XXXXXXXXX
    description: str = "dMRV PayGo Credit Top-Up"
    invoice_id: uuid.UUID | None = None


class PayPalPaymentRequest(BaseModel):
    amount: float
    currency: str = "USD"
    description: str = "dMRV PayGo Credit Top-Up"
    invoice_id: uuid.UUID | None = None


class FlutterwavePaymentRequest(BaseModel):
    amount: float
    currency: str = "USD"
    email: str | None = None
    phone_number: str | None = None
    description: str = "dMRV PayGo Credit Top-Up"
    invoice_id: uuid.UUID | None = None


class PaymentOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    user_id: uuid.UUID
    invoice_id: uuid.UUID | None
    provider: PaymentProvider
    status: PaymentStatus
    amount: float
    currency: str
    provider_reference: str | None
    provider_transaction_id: str | None
    description: str | None
    failure_reason: str | None
    completed_at: datetime | None
    created_at: datetime


class MpesaCallbackPayload(BaseModel):
    Body: dict


class PayPalWebhookPayload(BaseModel):
    event_type: str
    resource: dict


class FlutterwaveWebhookPayload(BaseModel):
    event: str
    data: dict
