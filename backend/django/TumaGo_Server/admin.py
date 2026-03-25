import secrets
from django.contrib import admin
from .models import PartnerCompany, PartnerDeliveryRequest, PartnerDevice


@admin.register(PartnerCompany)
class PartnerCompanyAdmin(admin.ModelAdmin):
    list_display = ("name", "contact_email", "is_active", "rate_limit", "created_at")
    list_filter = ("is_active",)
    search_fields = ("name", "contact_email")
    readonly_fields = ("id", "api_key", "api_secret", "created_at")

    def save_model(self, request, obj, form, change):
        # Auto-generate api_key and api_secret on creation
        if not change:
            obj.api_key = f"tg_live_{secrets.token_hex(24)}"
            obj.api_secret = f"sk_{secrets.token_hex(32)}"
        super().save_model(request, obj, form, change)


@admin.register(PartnerDeliveryRequest)
class PartnerDeliveryRequestAdmin(admin.ModelAdmin):
    list_display = ("partner", "partner_reference", "status", "created_at")
    list_filter = ("status", "partner")
    search_fields = ("partner_reference",)
    readonly_fields = ("id", "created_at")


@admin.register(PartnerDevice)
class PartnerDeviceAdmin(admin.ModelAdmin):
    list_display = ("label", "partner", "user", "is_active", "created_at")
    list_filter = ("is_active", "partner")
    search_fields = ("label", "user__email")
    readonly_fields = ("id", "created_at")
