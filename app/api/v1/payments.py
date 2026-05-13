import hashlib
import hmac
from fastapi import APIRouter, Depends, Request, Header
from sqlalchemy.orm import Session
from app.database import get_db
from app.api.deps import get_current_user
from app.core.exceptions import BadRequest
from app.config import get_settings
from app.models.user import User
from app.schemas.payment import (
    MpesaPaymentRequest,
    PayPalPaymentRequest,
    FlutterwavePaymentRequest,
    PaymentOut,
)
from app.services.mpesa import MpesaService
from app.services.paypal import PayPalService
from app.services.flutterwave import FlutterwaveService
from app.services.billing import BillingService

router = APIRouter(prefix="/payments", tags=["payments"])
settings = get_settings()


# ── M-Pesa ──────────────────────────────────────────────────────────────────

@router.post("/mpesa/stk-push", response_model=PaymentOut, status_code=201)
def mpesa_stk_push(
    payload: MpesaPaymentRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    service = MpesaService(db)
    payment = service.initiate_stk_push(
        user=current_user,
        amount=payload.amount,
        phone_number=payload.phone_number,
        description=payload.description,
        invoice_id=payload.invoice_id,
    )
    return payment


@router.post("/mpesa/callback")
async def mpesa_callback(request: Request, db: Session = Depends(get_db)):
    body = await request.json()
    service = MpesaService(db)
    service.process_callback(body)
    return {"ResultCode": 0, "ResultDesc": "Success"}


# ── PayPal ───────────────────────────────────────────────────────────────────

@router.post("/paypal/create-order", response_model=PaymentOut, status_code=201)
def paypal_create_order(
    payload: PayPalPaymentRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    service = PayPalService(db)
    payment = service.create_order(
        user=current_user,
        amount=payload.amount,
        currency=payload.currency,
        description=payload.description,
        invoice_id=payload.invoice_id,
    )
    return payment


@router.post("/paypal/capture/{payment_id}", response_model=PaymentOut)
def paypal_capture(
    payment_id: str,
    paypal_order_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    service = PayPalService(db)
    payment = service.capture_order(
        payment_id=payment_id,
        paypal_order_id=paypal_order_id,
        user=current_user,
    )
    return payment


@router.post("/paypal/webhook")
async def paypal_webhook(request: Request, db: Session = Depends(get_db)):
    body = await request.json()
    service = PayPalService(db)
    service.process_webhook(body)
    return {"status": "ok"}


# ── Flutterwave ──────────────────────────────────────────────────────────────

@router.post("/flutterwave/initiate", response_model=PaymentOut, status_code=201)
def flutterwave_initiate(
    payload: FlutterwavePaymentRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    service = FlutterwaveService(db)
    payment = service.initiate_payment(
        user=current_user,
        amount=payload.amount,
        currency=payload.currency,
        email=payload.email or current_user.email,
        phone_number=payload.phone_number or current_user.phone_number,
        description=payload.description,
        invoice_id=payload.invoice_id,
    )
    return payment


@router.post("/flutterwave/webhook")
async def flutterwave_webhook(
    request: Request,
    verif_hash: str | None = Header(None, alias="verif-hash"),
    db: Session = Depends(get_db),
):
    if verif_hash != settings.flutterwave_webhook_secret:
        raise BadRequest("Invalid webhook signature")
    body = await request.json()
    service = FlutterwaveService(db)
    service.process_webhook(body)
    return {"status": "ok"}


@router.get("/flutterwave/callback")
def flutterwave_callback(
    status: str,
    tx_ref: str,
    transaction_id: str | None = None,
    db: Session = Depends(get_db),
):
    if status == "successful" and transaction_id:
        service = FlutterwaveService(db)
        service.verify_and_complete(tx_ref=tx_ref, transaction_id=transaction_id)
    return {"status": status, "tx_ref": tx_ref}


# ── All payments ─────────────────────────────────────────────────────────────

@router.get("/", response_model=list[PaymentOut])
def list_payments(
    skip: int = 0,
    limit: int = 50,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    from app.models.payment import Payment
    return (
        db.query(Payment)
        .filter(Payment.user_id == current_user.id)
        .order_by(Payment.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )
