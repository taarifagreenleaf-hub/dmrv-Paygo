"""
PayPal REST API v2 integration.
Uses Orders API to create and capture payments.
Docs: https://developer.paypal.com/docs/api/orders/v2/
"""
import uuid
from datetime import datetime, timezone
import httpx
from sqlalchemy.orm import Session
from app.config import get_settings
from app.core.exceptions import PaymentError, NotFound, Forbidden
from app.models.payment import Payment, PaymentProvider, PaymentStatus
from app.models.user import User
from app.services.billing import BillingService

settings = get_settings()

_SANDBOX_BASE = "https://api-m.sandbox.paypal.com"
_PROD_BASE = "https://api-m.paypal.com"


class PayPalService:
    def __init__(self, db: Session):
        self.db = db
        self.base_url = _SANDBOX_BASE if settings.paypal_env == "sandbox" else _PROD_BASE

    def _get_access_token(self) -> str:
        resp = httpx.post(
            f"{self.base_url}/v1/oauth2/token",
            data={"grant_type": "client_credentials"},
            auth=(settings.paypal_client_id, settings.paypal_client_secret),
            timeout=10,
        )
        resp.raise_for_status()
        return resp.json()["access_token"]

    def _headers(self) -> dict:
        return {
            "Authorization": f"Bearer {self._get_access_token()}",
            "Content-Type": "application/json",
        }

    def create_order(
        self,
        user: User,
        amount: float,
        currency: str,
        description: str,
        invoice_id: uuid.UUID | None = None,
    ) -> Payment:
        payment = Payment(
            user_id=user.id,
            invoice_id=invoice_id,
            provider=PaymentProvider.PAYPAL,
            status=PaymentStatus.PENDING,
            amount=amount,
            currency=currency,
            description=description,
        )
        self.db.add(payment)
        self.db.flush()

        try:
            resp = httpx.post(
                f"{self.base_url}/v2/checkout/orders",
                json={
                    "intent": "CAPTURE",
                    "purchase_units": [
                        {
                            "reference_id": str(payment.id),
                            "description": description,
                            "amount": {
                                "currency_code": currency,
                                "value": f"{amount:.2f}",
                            },
                        }
                    ],
                    "application_context": {
                        "return_url": f"{settings.flutterwave_redirect_url}/paypal/success",
                        "cancel_url": f"{settings.flutterwave_redirect_url}/paypal/cancel",
                    },
                },
                headers=self._headers(),
                timeout=15,
            )
            resp.raise_for_status()
            data = resp.json()
            payment.provider_reference = data["id"]  # PayPal Order ID
            payment.provider_payload = data
            payment.status = PaymentStatus.PROCESSING
        except Exception as exc:
            payment.status = PaymentStatus.FAILED
            payment.failure_reason = str(exc)
            self.db.commit()
            raise PaymentError(f"PayPal order creation failed: {exc}") from exc

        self.db.commit()
        self.db.refresh(payment)
        return payment

    def capture_order(self, payment_id: str, paypal_order_id: str, user: User) -> Payment:
        payment = self.db.get(Payment, uuid.UUID(payment_id))
        if not payment:
            raise NotFound("Payment not found")
        if payment.user_id != user.id:
            raise Forbidden()
        if payment.provider_reference != paypal_order_id:
            raise PaymentError("Order ID mismatch")

        try:
            resp = httpx.post(
                f"{self.base_url}/v2/checkout/orders/{paypal_order_id}/capture",
                headers=self._headers(),
                timeout=15,
            )
            resp.raise_for_status()
            data = resp.json()

            capture = data["purchase_units"][0]["payments"]["captures"][0]
            payment.provider_transaction_id = capture["id"]
            payment.provider_payload = data
            payment.status = PaymentStatus.COMPLETED
            payment.completed_at = datetime.now(timezone.utc)

            billing = BillingService(self.db)
            billing.add_credits(
                user,
                payment.amount,
                f"PayPal payment {capture['id']}",
                payment_id=payment.id,
            )
        except Exception as exc:
            payment.status = PaymentStatus.FAILED
            payment.failure_reason = str(exc)
            self.db.commit()
            raise PaymentError(f"PayPal capture failed: {exc}") from exc

        self.db.commit()
        self.db.refresh(payment)
        return payment

    def process_webhook(self, body: dict):
        event_type = body.get("event_type", "")
        resource = body.get("resource", {})

        if event_type == "PAYMENT.CAPTURE.COMPLETED":
            txn_id = resource.get("id")
            # Match by provider_transaction_id if capture already happened via capture endpoint
            payment = (
                self.db.query(Payment)
                .filter(
                    Payment.provider == PaymentProvider.PAYPAL,
                    Payment.provider_transaction_id == txn_id,
                )
                .first()
            )
            if payment and payment.status != PaymentStatus.COMPLETED:
                payment.status = PaymentStatus.COMPLETED
                payment.completed_at = datetime.now(timezone.utc)
                payment.provider_payload = body
                self.db.commit()

        elif event_type == "PAYMENT.CAPTURE.DENIED":
            order_id = resource.get("supplementary_data", {}).get("related_ids", {}).get("order_id")
            if order_id:
                payment = (
                    self.db.query(Payment)
                    .filter(
                        Payment.provider == PaymentProvider.PAYPAL,
                        Payment.provider_reference == order_id,
                    )
                    .first()
                )
                if payment:
                    payment.status = PaymentStatus.FAILED
                    payment.failure_reason = "PayPal capture denied"
                    self.db.commit()
