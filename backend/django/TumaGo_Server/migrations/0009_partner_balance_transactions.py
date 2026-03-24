import django.db.models.deletion
import uuid
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('TumaGo_Server', '0008_customuser_ban_fields'),
    ]

    operations = [
        # Add balance fields to PartnerCompany
        migrations.AddField(
            model_name='partnercompany',
            name='balance',
            field=models.DecimalField(decimal_places=2, default=0.00, max_digits=12),
        ),
        migrations.AddField(
            model_name='partnercompany',
            name='setup_fee_paid',
            field=models.BooleanField(default=False),
        ),
        migrations.AddField(
            model_name='partnercompany',
            name='commission_rate',
            field=models.DecimalField(decimal_places=2, default=20.00, max_digits=5),
        ),

        # Create PartnerTransaction model
        migrations.CreateModel(
            name='PartnerTransaction',
            fields=[
                ('id', models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ('transaction_type', models.CharField(choices=[('deposit', 'Deposit'), ('delivery_charge', 'Delivery Charge'), ('refund', 'Refund'), ('setup_fee', 'Setup Fee'), ('adjustment', 'Adjustment')], max_length=20)),
                ('amount', models.DecimalField(decimal_places=2, max_digits=12)),
                ('balance_after', models.DecimalField(decimal_places=2, max_digits=12)),
                ('description', models.TextField(blank=True)),
                ('fare', models.DecimalField(blank=True, decimal_places=2, max_digits=10, null=True)),
                ('system_commission', models.DecimalField(blank=True, decimal_places=2, max_digits=10, null=True)),
                ('driver_share', models.DecimalField(blank=True, decimal_places=2, max_digits=10, null=True)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('partner', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name='transactions', to='TumaGo_Server.partnercompany')),
                ('delivery_request', models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name='transactions', to='TumaGo_Server.partnerdeliveryrequest')),
                ('driver', models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name='partner_earnings', to=settings.AUTH_USER_MODEL)),
            ],
            options={
                'ordering': ['-created_at'],
            },
        ),
        migrations.AddIndex(
            model_name='partnertransaction',
            index=models.Index(fields=['partner', 'transaction_type'], name='idx_partner_txn_type'),
        ),
    ]
