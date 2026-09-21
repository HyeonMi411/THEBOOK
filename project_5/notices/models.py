from django.conf import settings
from django.db import models


class Notice(models.Model):
    """project_3의 SBOARD2에 대응."""
    title = models.CharField(max_length=255)
    content = models.TextField()
    attachment = models.FileField(upload_to="uploads/notices/", null=True, blank=True)
    hit = models.IntegerField(default=0)
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    created_by = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.SET_NULL, null=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self):
        return self.title
