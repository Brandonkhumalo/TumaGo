import django.db.models.deletion
import uuid
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('TumaGo_Server', '0008_customuser_ban_fields'),
    ]

    operations = [
        # Create PartnerCompany model (with all fields including balance)
        migrations.CreateModel(
            name='PartnerCompany',
            fields=[
                ('id', models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ('name', models.CharField(max_length=200)),
                ('api_key', models.CharField(db_index=True, max_length=64, unique=True)),
                ('api_secret', models.CharField(max_length=128)),
                ('webhook_url', models.URLField(blank=True)),
                ('is_active', models.BooleanField(default=True)),
                ('rate_limit', models.IntegerField(default=100)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('contact_email', models.EmailField(max_length=254)),
                ('balance', models.DecimalField(decimal_places=2, default=0.00, max_digits=12)),
                ('setup_fee_paid', models.BooleanField(default=False)),
                ('commission_rate', models.DecimalField(decimal_places=2, default=20.00, max_digits=5)),
            ],
        ),

        # Create PartnerDeliveryRequest model
        migrations.CreateModel(
            name='PartnerDeliveryRequest',
            fields=[
                ('id', models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ('partner', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name='delivery_requests', to='TumaGo_Server.partnercompany')),
                ('partner_reference', models.CharField(max_length=200)),
                ('trip_request', models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name='partner_delivery', to='TumaGo_Server.triprequest')),
                ('delivery', models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name='partner_delivery', to='TumaGo_Server.delivery')),
                ('status', models.CharField(choices=[('pending', 'Pending'), ('matching', 'Matching'), ('driver_assigned', 'Driver Assigned'), ('picked_up', 'Picked Up'), ('delivered', 'Delivered'), ('cancelled', 'Cancelled')], default='pending', max_length=30)),
                ('pickup_contact', models.CharField(blank=True, max_length=20)),
                ('dropoff_contact', models.CharField(blank=True, max_length=20)),
                ('package_description', models.TextField(blank=True)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
            ],
        ),
        migrations.AddIndex(
            model_name='partnerdeliveryrequest',
            index=models.Index(fields=['partner', 'status'], name='idx_partner_delivery_status'),
        ),
        migrations.AddIndex(
            model_name='partnerdeliveryrequest',
            index=models.Index(fields=['partner', 'partner_reference'], name='idx_partner_ref'),
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
