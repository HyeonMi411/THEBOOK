from rest_framework.permissions import BasePermission, SAFE_METHODS


class IsAdminOrReadOnly(BasePermission):
    """project_3의 '등록/수정/삭제는 ROLE_ADMIN 전용' 정책과 동일."""

    def has_permission(self, request, view):
        if request.method in SAFE_METHODS:
            return True
        return bool(request.user and request.user.is_authenticated and request.user.role == "ROLE_ADMIN")
