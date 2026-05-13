from fastapi import APIRouter
from app.api.v1 import auth, users, projects, measurements, billing, payments

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(auth.router)
api_router.include_router(users.router)
api_router.include_router(projects.router)
api_router.include_router(measurements.router)
api_router.include_router(billing.router)
api_router.include_router(payments.router)
