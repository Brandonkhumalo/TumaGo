from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("TumaGo_Server", "0013_partner_suspension"),
    ]

    operations = [
        migrations.CreateModel(
            name="TermsContent",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("app_type", models.CharField(choices=[("client", "Client App"), ("driver", "Driver App")], max_length=10)),
                ("content", models.TextField()),
                ("version", models.PositiveIntegerField(default=1)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
            ],
            options={
                "ordering": ["-version"],
                "unique_together": {("app_type", "version")},
            },
        ),
        migrations.AddField(
            model_name="termsandconditions",
            name="accepted_version",
            field=models.PositiveIntegerField(default=0),
        ),
    ]
