from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('TumaGo_Server', '0009_partner_balance_transactions'),
    ]

    operations = [
        # Add picked_up field to Delivery
        migrations.AddField(
            model_name='delivery',
            name='picked_up',
            field=models.BooleanField(default=False),
        ),

        # Add idx_user_role index to CustomUser
        migrations.AddIndex(
            model_name='customuser',
            index=models.Index(fields=['role'], name='idx_user_role'),
        ),

        # Add idx_trip_pending index to TripRequest
        migrations.AddIndex(
            model_name='triprequest',
            index=models.Index(fields=['accepted', 'cancelled'], name='idx_trip_pending'),
        ),

        # Create SESSuppressionList model
        migrations.CreateModel(
            name='SESSuppressionList',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('email', models.EmailField(db_index=True, max_length=254, unique=True)),
                ('reason', models.CharField(choices=[('bounce', 'Bounce'), ('complaint', 'Complaint')], max_length=10)),
                ('bounce_type', models.CharField(blank=True, max_length=50)),
                ('diagnostic', models.TextField(blank=True)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
            ],
            options={
                'verbose_name': 'SES Suppression Entry',
                'verbose_name_plural': 'SES Suppression List',
            },
        ),
    ]
