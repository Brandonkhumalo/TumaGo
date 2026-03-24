from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('TumaGo_Server', '0006_driverlocations_unique_driver'),
    ]

    operations = [
        migrations.AddField(
            model_name='triprequest',
            name='cancelled',
            field=models.BooleanField(default=False),
        ),
    ]
