from django.urls import path

from TumaGo import settings
from django.conf.urls.static import static
from .views.UserViews import views, userViews, otpViews
from .views.DriverViews import authViews, driverViews
from .views.PaymentViews import paymentViews, walletViews
from .views.EmailViews import snsViews
from .views.PartnerViews import partner_views, partner_portal_views
from .views.AdminViews import admin_views

urlpatterns = [
    # Admin Dashboard API
    path('admin/login/', admin_views.admin_login, name='admin_login'),
    path('admin/overview/', admin_views.admin_overview, name='admin_overview'),
    path('admin/deliveries/metrics/', admin_views.admin_delivery_metrics, name='admin_delivery_metrics'),
    path('admin/financial/metrics/', admin_views.admin_financial_metrics, name='admin_financial_metrics'),
    path('admin/drivers/metrics/', admin_views.admin_driver_metrics, name='admin_driver_metrics'),
    path('admin/users/metrics/', admin_views.admin_user_metrics, name='admin_user_metrics'),
    path('admin/partners/metrics/', admin_views.admin_partner_metrics, name='admin_partner_metrics'),
    path('admin/system/health/', admin_views.admin_system_health, name='admin_system_health'),
    path('admin/users/list/', admin_views.admin_users_list, name='admin_users_list'),
    path('admin/deliveries/list/', admin_views.admin_deliveries_list, name='admin_deliveries_list'),
    path('admin/payments/list/', admin_views.admin_payments_list, name='admin_payments_list'),
    path('admin/partners/create/', admin_views.admin_create_partner, name='admin_create_partner'),
    path('admin/partners/<uuid:partner_id>/toggle/', admin_views.admin_toggle_partner, name='admin_toggle_partner'),
    path('admin/users/<uuid:user_id>/detail/', admin_views.admin_user_detail, name='admin_user_detail'),
    path('admin/users/<uuid:user_id>/ban/', admin_views.admin_ban_user, name='admin_ban_user'),
    path('admin/users/<uuid:user_id>/unban/', admin_views.admin_unban_user, name='admin_unban_user'),

    # OTP (Android app registrations only — WhatsApp skips OTP)
    path('otp/send/', otpViews.send_otp, name='send_otp'),
    path('otp/verify/', otpViews.verify_otp_view, name='verify_otp'),

    path('signup/', views.signup, name='signup'),
    path('update/user/profile/', views.update_profile, name='update_profile'),
    path('user/Data/', userViews.GetUserData, name='User_Data'),
    path('trip_Expense/', userViews.GetTripExpenses, name='Trip_Expense'),
    path('cancel/delivery/', userViews.cancel_delivery, name='cancel_delivery'),
    path('cancel/trip/', userViews.cancel_trip_request, name='cancel_trip_request'),
    path('rate/driver/', userViews.rate_driver, name='rate_driver'),
    path('track/delivery/', userViews.track_delivery, name='track_delivery'),

    path('login/', views.login, name='login'),
    path('refresh/', views.refresh_token, name='refresh_token'),
    path('verify_token/', views.VerifyToken, name='VerifyToken'),
    path('logout/', views.logout, name='logout'),
    path('delete/account/', authViews.delete_account, name='delete_account'),
    path('delivery/request/', driverViews.RequestDelivery, name='Delivery_Request'),
    path('reset_password/', authViews.change_password, name='change_password'), #Both apps
    path('get/deliveries/', driverViews.get_deliveries, name='get_user_or_driver_deliveries'),
    path('verifyTerms/', views.check_terms, name='Check_User_Terms'),
    path('accept/terms/', views.accept_terms, name='Accept_terms'),

    # Admin-only (requires IsAuthenticated + IsAdminUser)
    path('allUsers/', views.get_all_users, name='All_Users'),
    path('allDeliveries/', views.get_all_deliveries, name='All_deliveries'),

    #Driver
    path('driver/signup/', authViews.driver_register, name='driver_signup'),
    path('driver/data/', driverViews.get_driver_data, name='driver_data'),
    path('save-fcm-token/', authViews.save_fcm_token, name='save_fcm_token'),
    path('driver/offline/', authViews.driver_offline, name='driver_offline'),
    path('add/vehicle/', authViews.driver_vehicle, name='add_driver_vehicle'),
    path('accept/trip/', driverViews.AcceptTrip, name="Accept_trip"),
    path('mark_pickup/', driverViews.mark_pickup, name='mark_pickup'),
    path('end_trip/', driverViews.end_trip, name='end_trip'),
    path('add/license/', authViews.upload_license, name='upload_license'),
    path('driver/delivery_info/', driverViews.getDriver_Finances, name='driver_delivery_info'),

    # Payments (Paynow)
    path('payment/initiate/', paymentViews.initiate_payment, name='initiate_payment'),
    path('payment/status/', paymentViews.check_payment_status, name='check_payment_status'),
    path('payment/callback/', paymentViews.paynow_callback, name='paynow_callback'),
    path('payment/driver-balance/', paymentViews.get_driver_balance, name='driver_balance'),
    path('payment/pay-driver/', paymentViews.pay_driver, name='pay_driver'),

    # Driver Wallet
    path('driver/wallet/topup/', walletViews.wallet_topup, name='wallet_topup'),
    path('driver/wallet/topup/status/', walletViews.wallet_topup_status, name='wallet_topup_status'),
    path('driver/wallet/balance/', walletViews.wallet_balance, name='wallet_balance'),
    path('driver/wallet/transfer/', walletViews.wallet_transfer, name='wallet_transfer'),
    path('driver/wallet/transactions/', walletViews.wallet_transactions, name='wallet_transactions'),
    path('driver/wallet/refund-requests/', walletViews.wallet_refund_requests, name='wallet_refund_requests'),

    # Driver cancel delivery (with reason for refund review)
    path('driver/cancel/delivery/', userViews.driver_cancel_delivery, name='driver_cancel_delivery'),

    # B2B Partner API (X-API-Key auth)
    path('partner/delivery/request/', partner_views.partner_request_delivery, name='partner_request_delivery'),
    path('partner/delivery/<uuid:delivery_id>/status/', partner_views.partner_delivery_status, name='partner_delivery_status'),
    path('partner/delivery/<uuid:delivery_id>/cancel/', partner_views.partner_cancel_delivery, name='partner_cancel_delivery'),
    path('partner/deliveries/', partner_views.partner_list_deliveries, name='partner_list_deliveries'),
    path('partner/balance/', partner_views.partner_check_balance, name='partner_check_balance'),

    # Partner Self-Service Portal
    path('partner/portal/register/', partner_portal_views.partner_register, name='partner_register'),
    path('partner/portal/pay-setup/', partner_portal_views.partner_pay_setup, name='partner_pay_setup'),
    path('partner/portal/pay-setup/status/', partner_portal_views.partner_pay_setup_status, name='partner_pay_setup_status'),
    path('partner/portal/login/', partner_portal_views.partner_login, name='partner_login'),
    path('partner/portal/account/', partner_portal_views.partner_account, name='partner_account'),
    path('partner/portal/devices/', partner_portal_views.partner_create_device, name='partner_create_device'),
    path('partner/portal/devices/list/', partner_portal_views.partner_list_devices, name='partner_list_devices'),
    path('partner/portal/devices/<uuid:device_id>/toggle/', partner_portal_views.partner_toggle_device, name='partner_toggle_device'),
    path('partner/portal/devices/purchase/', partner_portal_views.partner_purchase_device_slots, name='partner_purchase_slots'),

    # Admin Partner Balance Management
    path('admin/partners/<uuid:partner_id>/deposit/', admin_views.admin_partner_deposit, name='admin_partner_deposit'),
    path('admin/partners/<uuid:partner_id>/transactions/', admin_views.admin_partner_transactions, name='admin_partner_transactions'),

    # Admin Commission Refund Reviews
    path('admin/refund-requests/', admin_views.admin_refund_requests, name='admin_refund_requests'),
    path('admin/refund-requests/<uuid:refund_id>/review/', admin_views.admin_review_refund, name='admin_review_refund'),
    path('admin/refund-requests/metrics/', admin_views.admin_refund_metrics, name='admin_refund_metrics'),

    # Admin Platform Settings (pricing + commission)
    path('admin/settings/', admin_views.admin_get_settings, name='admin_get_settings'),
    path('admin/settings/update/', admin_views.admin_update_settings, name='admin_update_settings'),

    # SES (email bounce/complaint handling via SNS)
    path('ses/notifications/', snsViews.ses_notifications, name='ses_notifications'),
] + static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)

# Serve media files during development
'''if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)'''