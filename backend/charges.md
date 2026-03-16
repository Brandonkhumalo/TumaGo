earnings = Decimal(delivery_cost)
        if driver_vehicle and driver_vehicle.lower() == "scooter":
            charges = Decimal("0.20")
        elif driver_vehicle and driver_vehicle.lower() == "van":
            charges = Decimal("0.30")
        elif driver_vehicle and driver_vehicle.lower() == "truck":
            charges = Decimal("0.50")
        else:
            charges = Decimal("0.10")

        finances = DriverFinances(earnings=earnings, charges=charges, driver=driver)
        finances.save()