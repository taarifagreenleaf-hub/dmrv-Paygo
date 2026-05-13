import uuid
from datetime import datetime, timezone, timedelta
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.config import get_settings
from app.core.exceptions import InsufficientCredits
from app.models.billing import UsageRecord, UsageType, Invoice, InvoiceStatus, CreditLedger
from app.models.measurement import Measurement
from app.models.user import User
from app.schemas.billing import UsageSummary

settings = get_settings()


class BillingService:
    def __init__(self, db: Session):
        self.db = db

    # ── Credit management ────────────────────────────────────────────────────

    def add_credits(self, user: User, amount: float, description: str, payment_id: uuid.UUID | None = None):
        user.credit_balance = float(user.credit_balance) + amount
        entry = CreditLedger(
            user_id=user.id,
            amount=amount,
            balance_after=float(user.credit_balance),
            description=description,
            payment_id=payment_id,
        )
        self.db.add(entry)

    def _deduct_credits(self, user: User, amount: float, description: str):
        balance = float(user.credit_balance)
        if balance < amount:
            raise InsufficientCredits()
        user.credit_balance = balance - amount
        entry = CreditLedger(
            user_id=user.id,
            amount=-amount,
            balance_after=float(user.credit_balance),
            description=description,
        )
        self.db.add(entry)

    # ── Metered billing ──────────────────────────────────────────────────────

    def _total_measurements_this_month(self, user: User) -> int:
        now = datetime.now(timezone.utc)
        start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        return (
            self.db.query(func.sum(UsageRecord.quantity))
            .filter(
                UsageRecord.user_id == user.id,
                UsageRecord.usage_type == UsageType.MEASUREMENT,
                UsageRecord.created_at >= start,
            )
            .scalar()
            or 0
        )

    def charge_for_measurement(self, user: User, measurement: Measurement):
        used = self._total_measurements_this_month(user)
        if used < settings.free_tier_measurements:
            unit_price = 0.0
        else:
            unit_price = settings.price_per_measurement

        record = UsageRecord(
            user_id=user.id,
            measurement_id=measurement.id,
            usage_type=UsageType.MEASUREMENT,
            quantity=1,
            unit_price=unit_price,
            total_amount=unit_price,
        )
        self.db.add(record)

        if unit_price > 0:
            self._deduct_credits(user, unit_price, f"Measurement #{measurement.id}")

    def charge_for_verification(self, user: User, measurement: Measurement):
        unit_price = settings.price_per_verification
        record = UsageRecord(
            user_id=user.id,
            measurement_id=measurement.id,
            usage_type=UsageType.VERIFICATION,
            quantity=1,
            unit_price=unit_price,
            total_amount=unit_price,
        )
        self.db.add(record)
        self._deduct_credits(user, unit_price, f"Verification of measurement #{measurement.id}")

    def charge_for_report(self, user: User, description: str = "Report generation"):
        unit_price = settings.price_per_report
        record = UsageRecord(
            user_id=user.id,
            usage_type=UsageType.REPORT,
            quantity=1,
            unit_price=unit_price,
            total_amount=unit_price,
        )
        self.db.add(record)
        self._deduct_credits(user, unit_price, description)

    # ── Invoice generation ───────────────────────────────────────────────────

    def generate_invoice(self, user: User) -> Invoice:
        now = datetime.now(timezone.utc)
        period_start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        period_end = now

        # Unbilled usage records
        unbilled = (
            self.db.query(UsageRecord)
            .filter(
                UsageRecord.user_id == user.id,
                UsageRecord.invoice_id.is_(None),
                UsageRecord.created_at >= period_start,
                UsageRecord.created_at <= period_end,
            )
            .all()
        )

        subtotal = sum(r.total_amount for r in unbilled)
        tax = round(subtotal * 0.0, 4)  # 0% tax — adjust per jurisdiction
        total = subtotal + tax

        invoice_number = f"INV-{now.strftime('%Y%m')}-{uuid.uuid4().hex[:8].upper()}"
        invoice = Invoice(
            user_id=user.id,
            invoice_number=invoice_number,
            status=InvoiceStatus.OPEN,
            subtotal=subtotal,
            tax=tax,
            total=total,
            currency="USD",
            period_start=period_start,
            period_end=period_end,
            due_date=now + timedelta(days=30),
        )
        self.db.add(invoice)
        self.db.flush()

        for record in unbilled:
            record.invoice_id = invoice.id

        self.db.commit()
        self.db.refresh(invoice)
        return invoice

    # ── Usage summary ─────────────────────────────────────────────────────────

    def get_usage_summary(self, user: User) -> UsageSummary:
        def count_type(usage_type: UsageType) -> int:
            return (
                self.db.query(func.sum(UsageRecord.quantity))
                .filter(UsageRecord.user_id == user.id, UsageRecord.usage_type == usage_type)
                .scalar()
                or 0
            )

        total_cost = (
            self.db.query(func.sum(UsageRecord.total_amount))
            .filter(UsageRecord.user_id == user.id)
            .scalar()
            or 0.0
        )

        used_this_month = self._total_measurements_this_month(user)
        free_remaining = max(0, settings.free_tier_measurements - used_this_month)

        return UsageSummary(
            total_measurements=count_type(UsageType.MEASUREMENT),
            total_verifications=count_type(UsageType.VERIFICATION),
            total_reports=count_type(UsageType.REPORT),
            total_api_calls=count_type(UsageType.API_CALL),
            total_cost_usd=float(total_cost),
            current_balance_usd=float(user.credit_balance),
            free_tier_remaining=free_remaining,
        )
