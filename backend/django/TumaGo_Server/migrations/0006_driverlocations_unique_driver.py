from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ("TumaGo_Server", "0005_driverbalance_payment"),
    ]

    operations = [
        migrations.AlterField(
            model_name="driverlocations",
            name="driver",
            field=models.OneToOneField(
                on_delete=django.db.models.deletion.CASCADE,
                related_name="driver_locations",
                to=settings.AUTH_USER_MODEL,
            ),
        ),
    ]
