from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('TumaGo_Server', '0007_triprequest_cancelled'),
    ]

    operations = [
        migrations.AddField(
            model_name='customuser',
            name='is_banned',
            field=models.BooleanField(default=False, db_index=True),
        ),
        migrations.AddField(
            model_name='customuser',
            name='banned_until',
            field=models.DateTimeField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name='customuser',
            name='ban_reason',
            field=models.TextField(blank=True, default=''),
        ),
    ]
