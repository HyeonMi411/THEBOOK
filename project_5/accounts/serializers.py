from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from rest_framework import serializers

User = get_user_model()


class UserResponseSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ["id", "email", "nickname", "mobile", "role", "provider", "profile_image", "date_joined"]


class SignupSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, validators=[validate_password])

    class Meta:
        model = User
        fields = ["email", "password", "nickname", "mobile", "profile_image"]

    def create(self, validated_data):
        password = validated_data.pop("password")
        user = User(
            username=validated_data["email"],  # Django 내부 username 필드는 email로 채움
            provider="local",
            **validated_data,
        )
        user.set_password(password)
        user.save()
        return user


class LoginSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField()
    provider = serializers.CharField(default="local")
