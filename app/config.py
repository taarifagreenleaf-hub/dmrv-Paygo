from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = "dMRV PayGo"
    app_env: str = "development"
    secret_key: str = "change-me-in-production"
    access_token_expire_minutes: int = 60
    algorithm: str = "HS256"

    database_url: str = "postgresql://dmrv_user:dmrv_pass@localhost:5432/dmrv_paygo"

    # M-Pesa
    mpesa_consumer_key: str = ""
    mpesa_consumer_secret: str = ""
    mpesa_shortcode: str = "174379"
    mpesa_passkey: str = ""
    mpesa_callback_url: str = ""
    mpesa_env: str = "sandbox"

    # PayPal
    paypal_client_id: str = ""
    paypal_client_secret: str = ""
    paypal_env: str = "sandbox"

    # Flutterwave
    flutterwave_secret_key: str = ""
    flutterwave_public_key: str = ""
    flutterwave_webhook_secret: str = ""
    flutterwave_redirect_url: str = ""

    # PayGo pricing (USD)
    price_per_measurement: float = 0.10
    price_per_verification: float = 0.50
    price_per_report: float = 1.00
    free_tier_measurements: int = 50


@lru_cache
def get_settings() -> Settings:
    return Settings()
