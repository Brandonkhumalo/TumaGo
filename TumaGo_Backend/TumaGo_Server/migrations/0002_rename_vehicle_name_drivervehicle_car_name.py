# Generated by Django 5.2.1 on 2025-07-02 15:27

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('TumaGo_Server', '0001_initial'),
    ]

    operations = [
        migrations.RenameField(
            model_name='drivervehicle',
            old_name='vehicle_name',
            new_name='car_name',
        ),
    ]
