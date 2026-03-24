from django.urls import path

from TumaGo import settings
from django.conf.urls.static import static
from .views.UserViews import views, userViews, otpViews
from .views.DriverViews import authViews, driverViews
from .views.PaymentViews import paymentViews

urlpatterns = [
    # OTP (Android app registrations only — WhatsApp skips OTP)
    path('otp/send/', otpViews.send_otp, name='send_otp'),
    path('otp/verify/', otpViews.verify_otp_view, name='verify_otp'),

    path('signup/', views.signup, name='signup'),
    path('update/user/profile/', views.update_profile, name='update_profile'),
    path('user/Data/', userViews.GetUserData, name='User_Data'),
    path('sync/time/', views.sync_time, name='Sync_Time'),
    path('trip_Expense/', userViews.GetTripExpenses, name='Trip_Expense'),
    path('cancel/delivery/', userViews.cancel_delivery, name='cancel_delivery'),
    path('cancel/trip/', userViews.cancel_trip_request, name='cancel_trip_request'),
    path('rate/driver/', userViews.rate_driver, name='rate_driver'),
    path('track/delivery/', userViews.track_delivery, name='track_delivery'),

    path('login/', views.login, name='login'),
    path('verify_token/', views.VerifyToken, name='VerifyToken'),
    path('logout/', views.logout, name='logout'),
    path('delete/account/', authViews.delete_account, name='delete_account'),
    path('delivery/request/', driverViews.RequestDelivery, name='Delivery_Request'),
    path('reset_password/', authViews.change_password, name='change_password'), #Both apps
    path('get/deliveries/', driverViews.get_deliveries, name='get_user_or_driver_deliveries'),
    path('verifyTerms/', views.check_terms, name='Check_User_Terms'),
    path('accept/terms/', views.accept_terms, name='Accept_terms'),

    #all users
    path('allUsers/', views.get_all_users, name='All_Users'),
    path('allDeliveries/', views.get_all_deliveries, name='All_deliveries'),
    path('delete/', views.delete_all_users, name='All_Users'),

    #Driver
    path('driver/signup/', authViews.driver_register, name='driver_signup'),
    path('driver/data/', driverViews.get_driver_data, name='driver_data'),
    path('save-fcm-token/', authViews.save_fcm_token, name='save_fcm_token'),
    path('driver/offline/', authViews.driver_offline, name='driver_offline'),
    path('add/vehicle/', authViews.driver_vehicle, name='add_driver_vehicle'),
    path('accept/trip/', driverViews.AcceptTrip, name="Accept_trip"),
    path('end_trip/', driverViews.end_trip, name='end_trip'),
    path('add/license/', authViews.upload_license, name='upload_license'),
    path('driver/delivery_info/', driverViews.getDriver_Finances, name='driver_delivery_info'),

    # Payments (Paynow)
    path('payment/initiate/', paymentViews.initiate_payment, name='initiate_payment'),
    path('payment/status/', paymentViews.check_payment_status, name='check_payment_status'),
    path('payment/callback/', paymentViews.paynow_callback, name='paynow_callback'),
    path('payment/driver-balance/', paymentViews.get_driver_balance, name='driver_balance'),
    path('payment/pay-driver/', paymentViews.pay_driver, name='pay_driver'),
] + static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)

# Serve media files during development
'''if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)'''