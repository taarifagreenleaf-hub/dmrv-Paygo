"""
Flutterwave v3 API integration.
Supports card, mobile money, and bank transfer payments.
Docs: https://developer.flutterwave.com/docs
"""
import uuid
from datetime import datetime, timezone
import httpx
from sqlalchemy.orm import Session
from app.config import get_settings
from app.core.exceptions import PaymentError
from app.models.payment import Payment, PaymentProvider, PaymentStatus
from app.models.user import User
from app.services.billing import BillingService

settings = get_settings()

_FLW_BASE = "https://api.flutterwave.com/v3"


class FlutterwaveService:
    def __init__(self, db: Session):
        self.db = db

    def _headers(self) -> dict:
        return {
            "Authorization": f"Bearer {settings.flutterwave_secret_key}",
            "Content-Type": "application/json",
        }

    def initiate_payment(
        self,
        user: User,
        amount: float,
        currency: str,
        email: str,
        phone_number: str | None,
        description: str,
        invoice_id: uuid.UUID | None = None,
    ) -> Payment:
        tx_ref = f"dmrv-{uuid.uuid4().hex}"
        payment = Payment(
            user_id=user.id,
            invoice_id=invoice_id,
            provider=PaymentProvider.FLUTTERWAVE,
            status=PaymentStatus.PENDING,
            amount=amount,
            currency=currency,
            description=description,
            provider_reference=tx_ref,
        )
        self.db.add(payment)
        self.db.flush()

        try:
            payload = {
                "tx_ref": tx_ref,
                "amount": amount,
                "currency": currency,
                "redirect_url": settings.flutterwave_redirect_url,
                "meta": {"payment_id": str(payment.id)},
                "customer": {
                    "email": email,
                    "name": user.full_name,
                    **({"phonenumber": phone_number} if phone_number else {}),
                },
                "customizations": {
                    "title": "dMRV PayGo",
                    "description": description,
                },
            }
            resp = httpx.post(
                f"{_FLW_BASE}/payments",
                json=payload,
                headers=self._headers(),
                timeout=15,
            )
            resp.raise_for_status()
            data = resp.json()

            if data.get("status") != "success":
                raise PaymentError(data.get("message", "Flutterwave error"))

            payment.provider_payload = data
            # The payment_link is returned in data["data"]["link"] — attach it
            payment.provider_transaction_id = data.get("data", {}).get("link")
            payment.status = PaymentStatus.PROCESSING
        except PaymentError:
            raise
        except Exception as exc:
            payment.status = PaymentStatus.FAILED
            payment.failure_reason = str(exc)
            self.db.commit()
            raise PaymentError(f"Flutterwave initiation failed: {exc}") from exc

        self.db.commit()
        self.db.refresh(payment)
        return payment

    def verify_and_complete(self, tx_ref: str, transaction_id: str):
        """Verify a transaction with Flutterwave and credit the user on success."""
        payment = (
            self.db.query(Payment)
            .filter(
                Payment.provider == PaymentProvider.FLUTTERWAVE,
                Payment.provider_reference == tx_ref,
            )
            .first()
        )
        if not payment or payment.status == PaymentStatus.COMPLETED:
            return

        try:
            resp = httpx.get(
                f"{_FLW_BASE}/transactions/{transaction_id}/verify",
                headers=self._headers(),
                timeout=15,
            )
            resp.raise_for_status()
            data = resp.json()
            tx = data.get("data", {})

            if (
                data.get("status") == "success"
                and tx.get("status") == "successful"
                and str(tx.get("tx_ref")) == tx_ref
            ):
                payment.provider_transaction_id = str(tx.get("id"))
                payment.provider_payload = data
                payment.status = PaymentStatus.COMPLETED
                payment.completed_at = datetime.now(timezone.utc)

                user = self.db.get(User, payment.user_id)
                if user:
                    # Use charged_amount in USD; Flutterwave returns charged currency
                    usd_amount = float(tx.get("amount", payment.amount))
                    billing = BillingService(self.db)
                    billing.add_credits(
                        user,
                        usd_amount,
                        f"Flutterwave payment {payment.provider_transaction_id}",
                        payment_id=payment.id,
                    )
            else:
                payment.status = PaymentStatus.FAILED
                payment.failure_reason = data.get("message", "Verification failed")
        except Exception as exc:
            payment.status = PaymentStatus.FAILED
            payment.failure_reason = str(exc)

        self.db.commit()

    def process_webhook(self, body: dict):
        event = body.get("event", "")
        data = body.get("data", {})

        if event == "charge.completed" and data.get("status") == "successful":
            tx_ref = data.get("tx_ref")
            transaction_id = str(data.get("id", ""))
            if tx_ref and transaction_id:
                self.verify_and_complete(tx_ref=tx_ref, transaction_id=transaction_id)
