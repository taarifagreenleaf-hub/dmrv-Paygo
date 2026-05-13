import uuid
from fastapi import Depends, Header
from sqlalchemy.orm import Session
from app.database import get_db
from app.core.security import decode_access_token
from app.core.exceptions import Unauthorized, Forbidden
from app.models.user import User


def get_current_user(
    authorization: str = Header(...),
    db: Session = Depends(get_db),
) -> User:
    if not authorization.startswith("Bearer "):
        raise Unauthorized()
    token = authorization[7:]
    user_id = decode_access_token(token)
    if not user_id:
        raise Unauthorized("Invalid or expired token")
    user = db.get(User, uuid.UUID(user_id))
    if not user or not user.is_active:
        raise Unauthorized("User not found or inactive")
    return user


def get_admin_user(current_user: User = Depends(get_current_user)) -> User:
    if not current_user.is_admin:
        raise Forbidden("Admin access required")
    return current_user
