"""Add partner self-service fields and PartnerDevice model.

- PartnerCompany: password, phone_number, max_device_slots, setup_poll_url, unique contact_email
- PartnerDevice: links partner to CustomUser accounts for device logins
- PartnerTransaction: add device_purchase type
"""
from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion
import uuid


class Migration(migrations.Migration):

    dependencies = [
        ("TumaGo_Server", "0010_delivery_picked_up"),
    ]

    operations = [
        # --- PartnerCompany new fields ---
        migrations.AddField(
            model_name="partnercompany",
            name="password",
            field=models.CharField(blank=True, max_length=128),
        ),
        migrations.AddField(
            model_name="partnercompany",
            name="phone_number",
            field=models.CharField(blank=True, max_length=20),
        ),
        migrations.AddField(
            model_name="partnercompany",
            name="max_device_slots",
            field=models.IntegerField(default=10),
        ),
        migrations.AddField(
            model_name="partnercompany",
            name="setup_poll_url",
            field=models.URLField(blank=True, max_length=500),
        ),
        # Make contact_email unique for partner login
        migrations.AlterField(
            model_name="partnercompany",
            name="contact_email",
            field=models.EmailField(max_length=254, unique=True),
        ),

        # --- PartnerDevice model ---
        migrations.CreateModel(
            name="PartnerDevice",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("label", models.CharField(max_length=100)),
                ("is_active", models.BooleanField(default=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                (
                    "partner",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="devices",
                        to="TumaGo_Server.partnercompany",
                    ),
                ),
                (
                    "user",
                    models.OneToOneField(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="partner_device",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
            options={
                "indexes": [
                    models.Index(fields=["partner", "is_active"], name="idx_partner_device_active"),
                ],
            },
        ),
    ]
