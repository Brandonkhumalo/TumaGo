from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("TumaGo_Server", "0012_partner_business_info"),
    ]

    operations = [
        migrations.AddField(
            model_name="partnercompany",
            name="is_suspended",
            field=models.BooleanField(default=False),
        ),
        migrations.AddField(
            model_name="partnercompany",
            name="suspended_until",
            field=models.DateTimeField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name="partnercompany",
            name="ban_reason",
            field=models.TextField(blank=True, default=""),
        ),
        migrations.AddField(
            model_name="partnercompany",
            name="is_permanently_banned",
            field=models.BooleanField(default=False),
        ),
    ]
