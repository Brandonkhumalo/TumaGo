from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('TumaGo_Server', '0009_partner_balance_transactions'),
    ]

    operations = [
        migrations.AddField(
            model_name='delivery',
            name='picked_up',
            field=models.BooleanField(default=False),
        ),
    ]
