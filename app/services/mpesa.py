"""
M-Pesa Daraja API integration.
Supports STK Push (Lipa Na M-Pesa Online) for credit top-ups.
Docs: https://developer.safaricom.co.ke/Documentation
"""
import base64
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

_SANDBOX_BASE = "https://sandbox.safaricom.co.ke"
_PROD_BASE = "https://api.safaricom.co.ke"


class MpesaService:
    def __init__(self, db: Session):
        self.db = db
        self.base_url = _SANDBOX_BASE if settings.mpesa_env == "sandbox" else _PROD_BASE

    def _get_access_token(self) -> str:
        credentials = base64.b64encode(
            f"{settings.mpesa_consumer_key}:{settings.mpesa_consumer_secret}".encode()
        ).decode()
        resp = httpx.get(
            f"{self.base_url}/oauth/v1/generate?grant_type=client_credentials",
            headers={"Authorization": f"Basic {credentials}"},
            timeout=10,
        )
        resp.raise_for_status()
        return resp.json()["access_token"]

    def _generate_password(self, timestamp: str) -> str:
        raw = f"{settings.mpesa_shortcode}{settings.mpesa_passkey}{timestamp}"
        return base64.b64encode(raw.encode()).decode()

    def initiate_stk_push(
        self,
        user: User,
        amount: float,
        phone_number: str,
        description: str,
        invoice_id: uuid.UUID | None = None,
    ) -> Payment:
        # Create a pending payment record first
        payment = Payment(
            user_id=user.id,
            invoice_id=invoice_id,
            provider=PaymentProvider.MPESA,
            status=PaymentStatus.PENDING,
            amount=amount,
            currency="KES",
            description=description,
        )
        self.db.add(payment)
        self.db.flush()

        timestamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
        try:
            token = self._get_access_token()
            payload = {
                "BusinessShortCode": settings.mpesa_shortcode,
                "Password": self._generate_password(timestamp),
                "Timestamp": timestamp,
                "TransactionType": "CustomerPayBillOnline",
                "Amount": int(amount),
                "PartyA": phone_number,
                "PartyB": settings.mpesa_shortcode,
                "PhoneNumber": phone_number,
                "CallBackURL": settings.mpesa_callback_url,
                "AccountReference": f"dmrv-{str(payment.id)[:8]}",
                "TransactionDesc": description[:20],
            }
            resp = httpx.post(
                f"{self.base_url}/mpesa/stkpush/v1/processrequest",
                json=payload,
                headers={"Authorization": f"Bearer {token}"},
                timeout=15,
            )
            resp.raise_for_status()
            data = resp.json()
            payment.provider_reference = data.get("CheckoutRequestID")
            payment.provider_payload = data
            payment.status = PaymentStatus.PROCESSING
        except Exception as exc:
            payment.status = PaymentStatus.FAILED
            payment.failure_reason = str(exc)
            self.db.commit()
            raise PaymentError(f"M-Pesa STK push failed: {exc}") from exc

        self.db.commit()
        self.db.refresh(payment)
        return payment

    def process_callback(self, body: dict):
        """Handle Daraja STK callback, update payment and add credits on success."""
        try:
            result = body["Body"]["stkCallback"]
            checkout_id = result["CheckoutRequestID"]
            result_code = result["ResultCode"]
        except (KeyError, TypeError):
            return

        payment = (
            self.db.query(Payment)
            .filter(
                Payment.provider == PaymentProvider.MPESA,
                Payment.provider_reference == checkout_id,
            )
            .first()
        )
        if not payment:
            return

        payment.provider_payload = body

        if result_code == 0:
            items = {
                item["Name"]: item.get("Value")
                for item in result.get("CallbackMetadata", {}).get("Item", [])
            }
            payment.provider_transaction_id = str(items.get("MpesaReceiptNumber", ""))
            payment.status = PaymentStatus.COMPLETED
            payment.completed_at = datetime.now(timezone.utc)

            user = self.db.get(User, payment.user_id)
            if user:
                # Convert KES to USD using a fixed rate placeholder (replace with live FX)
                kes_to_usd = 0.0077
                usd_amount = round(float(payment.amount) * kes_to_usd, 4)
                billing = BillingService(self.db)
                billing.add_credits(
                    user,
                    usd_amount,
                    f"M-Pesa top-up {payment.provider_transaction_id}",
                    payment_id=payment.id,
                )
        else:
            payment.status = PaymentStatus.FAILED
            payment.failure_reason = result.get("ResultDesc", "Unknown error")

        self.db.commit()
