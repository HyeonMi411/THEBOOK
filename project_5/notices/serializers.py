from rest_framework import serializers
from .models import Notice


class NoticeSerializer(serializers.ModelSerializer):
    userNickname = serializers.CharField(source="created_by.nickname", read_only=True, default=None)

    class Meta:
        model = Notice
        fields = ["id", "title", "content", "attachment", "hit", "ip_address", "created_at", "userNickname"]
        read_only_fields = ["id", "hit", "ip_address", "created_at"]
