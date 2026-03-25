"""Add business information fields to PartnerCompany."""
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("TumaGo_Server", "0011_partner_selfservice_devices"),
    ]

    operations = [
        migrations.AddField(
            model_name="partnercompany",
            name="description",
            field=models.TextField(blank=True),
        ),
        migrations.AddField(
            model_name="partnercompany",
            name="address",
            field=models.CharField(blank=True, max_length=300),
        ),
        migrations.AddField(
            model_name="partnercompany",
            name="city",
            field=models.CharField(blank=True, max_length=100),
        ),
        migrations.AddField(
            model_name="partnercompany",
            name="contact_person_name",
            field=models.CharField(blank=True, max_length=200),
        ),
        migrations.AddField(
            model_name="partnercompany",
            name="contact_person_role",
            field=models.CharField(blank=True, max_length=100),
        ),
    ]
