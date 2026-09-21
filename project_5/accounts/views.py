import random
from datetime import timedelta

from django.contrib.auth import get_user_model
from django.core.mail import send_mail
from django.utils import timezone
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken

from .models import EmailVerificationCode
from .serializers import LoginSerializer, SignupSerializer, UserResponseSerializer

User = get_user_model()


def _issue_tokens_for(user):
    """
    project_3와 동일하게: accessToken은 role을 포함한 JWT.
    project_3는 refreshToken을 HttpOnly 쿠키로 분리했는데, 여기서는 데모 단순화를 위해
    응답 바디에 함께 내려줌 - 실제 서비스 연동 시 Set-Cookie로 옮기는 걸 권장.
    """
    refresh = RefreshToken.for_user(user)
    refresh["role"] = user.role
    refresh["nickname"] = user.nickname
    access = refresh.access_token
    access["role"] = user.role
    return str(access), str(refresh)


# ── 회원가입 ──────────────────────────────────────────────────
@api_view(["POST"])
@permission_classes([AllowAny])
def signup(request):
    email = request.data.get("email")
    # project_3에서 겪었던 것처럼, "이메일 인증을 마쳤는지"를 먼저 반드시 확인
    verified = EmailVerificationCode.objects.filter(
        email=email, verified=True, expires_at__gte=timezone.now()
    ).exists()
    if not verified:
        return Response({"error": "이메일 인증을 먼저 완료해주세요."}, status=400)

    serializer = SignupSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    user = serializer.save()
    return Response(UserResponseSerializer(user).data, status=status.HTTP_201_CREATED)


# ── 로그인 ────────────────────────────────────────────────────
@api_view(["POST"])
@permission_classes([AllowAny])
def login(request):
    serializer = LoginSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    data = serializer.validated_data

    try:
        user = User.objects.get(email=data["email"], provider=data.get("provider", "local"))
    except User.DoesNotExist:
        return Response({"error": "사용자를 찾을수 없습니다."}, status=404)

    # project_3 실제 운영 중 겪었던 버그: 탈퇴 여부를 비밀번호 검증보다 먼저 확인해야
    # "탈퇴한 계정입니다" 메시지를 정확히 보여줄 수 있음
    if user.deleted:
        return Response({"error": "탈퇴한 계정입니다."}, status=400)

    if not user.check_password(data["password"]):
        return Response({"error": "비밀번호 불일치"}, status=400)

    access, refresh = _issue_tokens_for(user)
    return Response({
        "accessToken": access,
        "refreshToken": refresh,
        "user": UserResponseSerializer(user).data,
    })


# ── 이메일 인증번호 발송/확인 ─────────────────────────────────
@api_view(["POST"])
@permission_classes([AllowAny])
def send_email_code(request):
    email = request.query_params.get("email") or request.data.get("email")
    if not email:
        return Response({"error": "email 파라미터가 필요합니다."}, status=400)

    code = f"{random.randint(0, 999999):06d}"
    EmailVerificationCode.objects.create(
        email=email, code=code, expires_at=timezone.now() + timedelta(minutes=5)
    )
    send_mail(
        subject="[BookStore] 이메일 인증번호",
        message=f"인증번호: {code} (5분간 유효)",
        from_email=None,
        recipient_list=[email],
        fail_silently=False,
    )
    return Response({"message": "인증번호를 발송했습니다."})


@api_view(["POST"])
@permission_classes([AllowAny])
def verify_email_code(request):
    email = request.query_params.get("email") or request.data.get("email")
    code = request.query_params.get("code") or request.data.get("code")

    record = (
        EmailVerificationCode.objects.filter(email=email, code=code, expires_at__gte=timezone.now())
        .order_by("-created_at")
        .first()
    )
    if not record:
        return Response({"error": "인증번호가 만료되었거나 올바르지 않습니다."}, status=400)

    record.verified = True
    record.expires_at = timezone.now() + timedelta(minutes=30)
    record.save()
    return Response({"message": "이메일 인증이 완료되었습니다."})


# ── 중복확인 ──────────────────────────────────────────────────
@api_view(["GET"])
@permission_classes([AllowAny])
def check_email(request):
    email = request.query_params.get("email")
    return Response(User.objects.filter(email=email).exists())


@api_view(["GET"])
@permission_classes([AllowAny])
def check_nickname(request):
    nickname = request.query_params.get("nickname")
    return Response(User.objects.filter(nickname=nickname).exists())


# ── 내 정보 / 탈퇴 ────────────────────────────────────────────
class MeView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response(UserResponseSerializer(request.user).data)

    def delete(self, request):
        # project_3와 동일한 소프트 삭제 정책 (주문/결제 이력을 보존하기 위함)
        user = request.user
        user.deleted = True
        user.is_active = False
        user.save()
        return Response({"message": "회원 탈퇴가 완료되었습니다."})


@api_view(["PATCH"])
@permission_classes([IsAuthenticated])
def update_nickname(request, user_id):
    if request.user.id != int(user_id):
        return Response({"error": "본인 계정만 수정할 수 있습니다."}, status=403)
    nickname = request.query_params.get("nickname") or request.data.get("nickname")
    request.user.nickname = nickname
    request.user.save()
    return Response(UserResponseSerializer(request.user).data)


@api_view(["PATCH"])
@permission_classes([IsAuthenticated])
def update_profile_image(request, user_id):
    if request.user.id != int(user_id):
        return Response({"error": "본인 계정만 수정할 수 있습니다."}, status=403)
    file = request.FILES.get("ufile")
    if not file:
        return Response({"error": "이미지 파일이 필요합니다."}, status=400)
    request.user.profile_image = file
    request.user.save()
    return Response(UserResponseSerializer(request.user).data)
