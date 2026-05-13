from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.api.deps import get_current_user
from app.models.billing import UsageRecord, Invoice, CreditLedger
from app.models.user import User
from app.schemas.billing import UsageRecordOut, InvoiceOut, CreditLedgerOut, UsageSummary
from app.services.billing import BillingService

router = APIRouter(prefix="/billing", tags=["billing"])


@router.get("/usage", response_model=list[UsageRecordOut])
def list_usage(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return (
        db.query(UsageRecord)
        .filter(UsageRecord.user_id == current_user.id)
        .order_by(UsageRecord.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )


@router.get("/usage/summary", response_model=UsageSummary)
def usage_summary(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    billing = BillingService(db)
    return billing.get_usage_summary(current_user)


@router.get("/invoices", response_model=list[InvoiceOut])
def list_invoices(
    skip: int = 0,
    limit: int = 50,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return (
        db.query(Invoice)
        .filter(Invoice.user_id == current_user.id)
        .order_by(Invoice.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )


@router.get("/invoices/{invoice_id}", response_model=InvoiceOut)
def get_invoice(
    invoice_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    import uuid
    from app.core.exceptions import NotFound, Forbidden
    invoice = db.get(Invoice, uuid.UUID(invoice_id))
    if not invoice:
        raise NotFound("Invoice not found")
    if invoice.user_id != current_user.id and not current_user.is_admin:
        raise Forbidden()
    return invoice


@router.post("/invoices/generate", response_model=InvoiceOut, status_code=201)
def generate_invoice(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    billing = BillingService(db)
    invoice = billing.generate_invoice(current_user)
    return invoice


@router.get("/credits", response_model=list[CreditLedgerOut])
def credit_history(
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return (
        db.query(CreditLedger)
        .filter(CreditLedger.user_id == current_user.id)
        .order_by(CreditLedger.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )
