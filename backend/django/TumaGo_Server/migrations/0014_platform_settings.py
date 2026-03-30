from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("TumaGo_Server", "0013_wallet_commission_system"),
    ]

    operations = [
        migrations.CreateModel(
            name="PlatformSettings",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("scooter_price_per_km", models.DecimalField(decimal_places=2, default=0.50, max_digits=6)),
                ("scooter_base_fee", models.DecimalField(decimal_places=2, default=0.20, max_digits=6)),
                ("van_price_per_km", models.DecimalField(decimal_places=2, default=1.10, max_digits=6)),
                ("van_base_fee", models.DecimalField(decimal_places=2, default=0.30, max_digits=6)),
                ("truck_price_per_km", models.DecimalField(decimal_places=2, default=2.30, max_digits=6)),
                ("truck_base_fee", models.DecimalField(decimal_places=2, default=0.50, max_digits=6)),
                ("cash_commission_pct", models.DecimalField(decimal_places=2, default=15.00, max_digits=5)),
                ("online_commission_pct", models.DecimalField(decimal_places=2, default=20.00, max_digits=5)),
                ("updated_at", models.DateTimeField(auto_now=True)),
            ],
            options={
                "verbose_name": "Platform Settings",
                "verbose_name_plural": "Platform Settings",
            },
        ),
    ]
