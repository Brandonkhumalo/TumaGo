import django.db.models.deletion
import uuid
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("TumaGo_Server", "0012_partner_business_info"),
    ]

    operations = [
        # DriverWallet — prepaid balance for accepting deliveries
        migrations.CreateModel(
            name="DriverWallet",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("balance", models.DecimalField(decimal_places=2, default=0.00, max_digits=10)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("driver", models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name="wallet", to=settings.AUTH_USER_MODEL)),
            ],
        ),
        # WalletTransaction — audit trail for wallet changes
        migrations.CreateModel(
            name="WalletTransaction",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("transaction_type", models.CharField(choices=[("topup", "Top-up"), ("commission", "Commission Deduction"), ("refund", "Commission Refund"), ("transfer_in", "Transfer In (from owed)"), ("transfer_out", "Transfer Out (to owed)")], max_length=20)),
                ("amount", models.DecimalField(decimal_places=2, max_digits=10)),
                ("balance_after", models.DecimalField(decimal_places=2, max_digits=10)),
                ("description", models.TextField(blank=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("driver", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="wallet_transactions", to=settings.AUTH_USER_MODEL)),
                ("delivery", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name="wallet_transactions", to="TumaGo_Server.delivery")),
            ],
            options={
                "ordering": ["-created_at"],
            },
        ),
        migrations.AddIndex(
            model_name="wallettransaction",
            index=models.Index(fields=["driver", "transaction_type"], name="idx_wallet_txn_type"),
        ),
        # CommissionRefundRequest — driver cancellation refund review
        migrations.CreateModel(
            name="CommissionRefundRequest",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("amount", models.DecimalField(decimal_places=2, max_digits=10)),
                ("reason", models.TextField()),
                ("status", models.CharField(choices=[("pending", "Pending"), ("approved", "Approved"), ("denied", "Denied")], db_index=True, default="pending", max_length=10)),
                ("admin_notes", models.TextField(blank=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("reviewed_at", models.DateTimeField(blank=True, null=True)),
                ("driver", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="refund_requests", to=settings.AUTH_USER_MODEL)),
                ("delivery", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="refund_requests", to="TumaGo_Server.delivery")),
                ("reviewed_by", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name="reviewed_refunds", to=settings.AUTH_USER_MODEL)),
            ],
            options={
                "ordering": ["-created_at"],
            },
        ),
        migrations.AddIndex(
            model_name="commissionrefundrequest",
            index=models.Index(fields=["driver", "status"], name="idx_refund_driver_status"),
        ),
        migrations.AddIndex(
            model_name="commissionrefundrequest",
            index=models.Index(fields=["status", "created_at"], name="idx_refund_pending"),
        ),
    ]
